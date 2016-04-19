/*
 * Copyright (c) 2016 Michael K. Werle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.coruscations.aws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static com.coruscations.aws.Constants.DEFAULT_USER_AGENT;
import static java.lang.String.format;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

@ParametersAreNonnullByDefault
class StandardHttpURLConnectionBuilder implements HttpHeaders, HttpURLConnectionBuilder {

  private static final Logger LOG = Logger.getLogger(
      StandardHttpURLConnectionBuilder.class.getName());

  private static final byte[] AWS4_REQUEST = "aws4_request".getBytes(StandardCharsets.UTF_8);
  private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
      .appendValue(YEAR, 4).appendValue(MONTH_OF_YEAR, 2).appendValue(DAY_OF_MONTH, 2)
      .appendLiteral('T').appendValue(HOUR_OF_DAY, 2).appendValue(MINUTE_OF_HOUR, 2)
      .appendValue(SECOND_OF_MINUTE, 2).appendLiteral('Z').toFormatter();

  private static final Collator US_ASCII_BINARY_COLLATOR =
      new BinaryCollator(StandardCharsets.US_ASCII);
  private static final Collator UTF8_BINARY_COLLATOR = new BinaryCollator(StandardCharsets.UTF_8);

  private static final String CONTENT_TYPE_AMZ_JSON_1_0 = "application/x-amz-json-1.0";

  private final MessageDigest sha256 = SigningHelper.getMessageDigest(
      SigningHelper.SHA_256_ALGORITHM);
  private final Mac hmacSha256 = SigningHelper.getMac(SigningHelper.HMAC_SHA_256_ALGORITHM);

  private final ConfigurationProvider configurationProvider;

  private String serviceName;
  private HttpMethod method;
  private Endpoint endpoint;
  private String path;
  private String bodyHash = SigningHelper.SHA256_EMPTY_STRING_HASH;

  private final Map<String, List<String>> headers = new TreeMap<>(UTF8_BINARY_COLLATOR);
  private final Set<String> signedHeaders = new TreeSet<>(UTF8_BINARY_COLLATOR);

  private final List<String[]> query = new LinkedList<>();

  private String host;

  private String canonicalRequest;
  private String requestStringToSign;
  private String authorization;

  StandardHttpURLConnectionBuilder(ConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public void setMethod(HttpMethod method) {
    this.method = method;
  }

  private Endpoint getEndpoint() {
    return this.endpoint == null ? this.configurationProvider.getEndpoint(this.serviceName) :
           this.endpoint;
  }

  public void setEndpoint(Endpoint endpoint) {
    this.endpoint = endpoint;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setBody(String body) {
    this.bodyHash = body == null || body.isEmpty() ? SigningHelper.SHA256_EMPTY_STRING_HASH :
                    SigningHelper.hash(body, this.sha256);
  }

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public HttpURLConnectionBuilder addQueryParameter(String key, String val) {
    this.query.add(new String[]{key, val});
    return this;
  }

  @Override
  public HttpURLConnectionBuilder addHeader(String key, String val, boolean isSigned) {
    List<String> list = this.headers.get(key);
    if (list == null) {
      list = new LinkedList<>();
      this.headers.put(key, list);
    }
    list.add(val);
    if (isSigned) {
      this.signedHeaders.add(key);
    }
    return this;
  }

  public HttpURLConnection build() throws IOException {
    try {
      initializeHeaders();
      this.canonicalRequest = createCanonicalRequest();
      this.requestStringToSign = createRequestStringToSign(this.canonicalRequest);
      this.authorization = createAuthorization(this.requestStringToSign);
      this.headers.put(AUTHORIZATION, Collections.singletonList(this.authorization));

      // TODO: Pool connections???
      URL url = new URL(createRequestUrl());
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod(this.method.name());
      // Set the headers
      this.headers.entrySet().stream()
          .forEach(e -> e.getValue().stream()
              .forEach(val -> con.setRequestProperty(e.getKey(), val)));
      return con;
    } catch (InvalidKeyException | NoSuchAlgorithmException | URISyntaxException e) {
      throw new IOException("Failed to create signed request", e);
    }
  }

  void initializeHeaders() {
    // Set required headers
    normalizeHost();
    normalizeDateHeader();

    // These defaults can be removed setting non-string values in the environment.
    addDefaultConditionally(USER_AGENT, DEFAULT_USER_AGENT);

    // Add the content hash unless suppressed because AWS expects it pretty much all the time.
    if (getEndpoint().isRequireContentHashHeader() &&
        !this.headers.containsKey(X_AMZ_CONTENT_SHA256)) {
      this.headers.put(X_AMZ_CONTENT_SHA256, Collections.singletonList(this.bodyHash));
      this.signedHeaders.add(X_AMZ_CONTENT_SHA256);
    }
  }

  String createCanonicalRequest() throws NoSuchAlgorithmException, URISyntaxException {
    // Create the formatted request
    String canonicalRequest = format("%s\n%s\n%s\n%s\n%s\n%s", this.method, canonicalPath(),
                                     canonicalQueryString(), canonicalHeaders(),
                                     canonicalSignedHeaders(), this.bodyHash);
    LOG.log(Level.FINER, "Canonical request:\n{0}", canonicalRequest);
    return canonicalRequest;
  }

  String createRequestStringToSign(String canonicalRequest) {
    String canonicalRequestHash = SigningHelper.hash(canonicalRequest, this.sha256);
    String formattedDateTime = getFormattedDateTime();
    String formattedDate = formattedDateTime.substring(0, formattedDateTime.indexOf('T'));
    String requestStringToSign = format("AWS4-HMAC-SHA256\n%s\n%s/%s/%s/aws4_request\n%s",
                                        formattedDateTime, formattedDate, getEndpoint().getRegion(),
                                        this.serviceName, canonicalRequestHash);
    LOG.log(Level.FINER, "Request string to sign:\n{0}", requestStringToSign);
    return requestStringToSign;
  }

  String createAuthorization(String stringToSign)
      throws InvalidKeyException {
    String keyString = "AWS4" + this.configurationProvider.getSecretKey();
    byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);

    this.hmacSha256.init(new SecretKeySpec(keyBytes, SigningHelper.HMAC_SHA_256_ALGORITHM));
    String formattedDateTime = getFormattedDateTime();
    String formattedDate = formattedDateTime.substring(0, formattedDateTime.indexOf('T'));
    byte[] signedDate = this.hmacSha256.doFinal(formattedDate.getBytes(StandardCharsets.UTF_8));

    this.hmacSha256.init(new SecretKeySpec(signedDate, SigningHelper.HMAC_SHA_256_ALGORITHM));
    byte[] signedRegion =
        this.hmacSha256.doFinal(getEndpoint().getRegion().getBytes(StandardCharsets.UTF_8));

    this.hmacSha256.init(new SecretKeySpec(signedRegion, SigningHelper.HMAC_SHA_256_ALGORITHM));
    byte[] signedService = this.hmacSha256.doFinal(serviceName.getBytes(StandardCharsets.UTF_8));

    this.hmacSha256.init(new SecretKeySpec(signedService, SigningHelper.HMAC_SHA_256_ALGORITHM));
    byte[] signingKey = this.hmacSha256.doFinal(AWS4_REQUEST);

    this.hmacSha256.init(new SecretKeySpec(signingKey, SigningHelper.HMAC_SHA_256_ALGORITHM));
    String signature = SigningHelper.hashBytesToString(
        this.hmacSha256.doFinal(stringToSign.getBytes()), 64);

    String authorization = format("AWS4-HMAC-SHA256 Credential=%s/%s/%s/%s/aws4_request, " +
                                  "SignedHeaders=%s, Signature=%s",
                                  this.configurationProvider.getAccessKey(), formattedDate,
                                  getEndpoint().getRegion(), serviceName, canonicalSignedHeaders(),
                                  signature);
    LOG.log(Level.FINER, "Authorization: {0}", authorization);
    return authorization;
  }

  String createRequestUrl() {
    Endpoint endpoint = getEndpoint();
    StringBuilder urlBuilder = new StringBuilder(endpoint.getPreferredScheme().getUrlScheme());
    urlBuilder.append("://");
    urlBuilder.append(this.host == null ? endpoint.getHost() : this.host);
    urlBuilder.append(this.path);
    if (this.query.size() > 0) {
      boolean started = false;
      for (String[] q : this.query) {
        if (!started) {
          urlBuilder.append('?');
          started = true;
        } else {
          urlBuilder.append('&');
        }
        urlBuilder.append(q[0]);
        if (q.length > 1 && !q[1].isEmpty()) {
          urlBuilder.append('=').append(q[1]);
        }
      }
    }
    return urlBuilder.toString();
  }

  private void normalizeHost() {
    addDefaultHeader(HOST, this.host == null ? getEndpoint().getHost() : this.host);
  }

  private void addDefaultConditionally(String header, String defaultValue) {
    String value = singleHeader(header);
    if (value == null) {
      Object o = this.configurationProvider.getEnv().get(header);
      if (o instanceof String) {
        value = (String) o;
      } else if (o == null) {
        value = defaultValue;
      }
      if (value != null) {
        this.headers.put(header, Collections.singletonList(value));
      }
    }
  }

  @Nullable
  private String getFormattedDateTime() {
    List<String> date = this.headers.get(DATE);
    if (date != null && date.size() > 0) {
      return date.iterator().next();
    }
    date = this.headers.get(X_AMZ_DATE);
    if (date != null && date.size() > 0) {
      return date.iterator().next();
    }
    return null;
  }

  @Nonnull
  private String normalizeDateHeader() {
    String dateHeader = DATE;
    String formattedDateTime = singleHeader(DATE);
    if (formattedDateTime == null) {
      formattedDateTime = singleHeader(X_AMZ_DATE);
      if (formattedDateTime == null) {
        OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
        formattedDateTime = now.format(DATE_FORMAT);
        addDefaultHeader(DATE, formattedDateTime);
      } else {
        dateHeader = X_AMZ_DATE;
        this.signedHeaders.remove(DATE);
        this.headers.remove(DATE);
      }
    } else {
      this.headers.remove(X_AMZ_DATE);
      this.signedHeaders.remove(X_AMZ_DATE);
    }
    this.signedHeaders.add(dateHeader);
    return formattedDateTime;
  }

  private void addDefaultHeader(String key, String value) {
    if (this.headers.get(key) == null) {
      this.headers.put(key, Collections.singletonList(value));
      this.signedHeaders.add(key);
    }
  }

  @Nullable
  private String singleHeader(String name) {
    List<String> list = this.headers.get(name);
    if (list == null) {
      return null;
    }
    if (list.size() == 1) {
      return list.get(0);
    }
    if (list.isEmpty()) {
      this.headers.remove(name);
      this.signedHeaders.remove(name);
      return null;
    }
    String value = list.get(0);
    if (list.size() > 1) {
      LOG.log(Level.WARNING, "Removing duplicate header values for: {0}", name);
      this.headers.put(name, Collections.singletonList(value));
    }
    return value;
  }

  private String canonicalPath() {
    if (this.path == null || this.path.isEmpty()) {
      return "/";
    }
    String canonicalPath = Paths.get(this.path).normalize().toString();
    if (canonicalPath.length() > 1 && this.path.endsWith("/")) {
      canonicalPath = canonicalPath + "/";
    }
    return canonicalPath.startsWith("/") ? canonicalPath : "/" + canonicalPath;
  }

  private String canonicalQueryString() {
    if (this.query.isEmpty()) {
      return "";
    }
    Map<String, SortedSet<String>> sorted = new TreeMap<>(US_ASCII_BINARY_COLLATOR);
    for (String[] keyVal : this.query) {
      String key = EncodingHelper.awsEncodeURLComponent(keyVal[0], false);
      SortedSet<String> values = sorted.get(key);
      if (values == null) {
        values = new TreeSet<>(US_ASCII_BINARY_COLLATOR);
        sorted.put(key, values);
      }
      values.add(EncodingHelper.awsEncodeURLComponent(keyVal[1], false));
    }

    StringBuilder sb = new StringBuilder();
    for (String key : sorted.keySet()) {
      for (String value : sorted.get(key)) {
        sb.append(key).append('=').append(value).append('&');
      }
    }
    // Chop last "&"
    return sb.substring(0, sb.length() - 1);
  }

  private String canonicalHeaders() {
    if (this.signedHeaders.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (String signedHeader : this.signedHeaders) {
      String lowerHeader = signedHeader.toLowerCase();
      List<String> values = this.headers.get(signedHeader);
      if (values == null) {
        throw new IllegalStateException("Signed header \"" + signedHeader + "\" not set.");
      }
      sb.append(lowerHeader).append(':');
      if (values.size() == 1) {
        String value = values.get(0).trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.indexOf(' ') > 0) {
          // Squash inside spaces
          value = Arrays.stream(value.substring(1, value.length() - 1).split("\\s+"))
              .collect(Collectors.joining(" ", "\"", "\""));
        }
        sb.append(value);
      } else if (values.size() > 1) {
        sb.append(values.stream().map(String::trim).collect(Collectors.joining(",")));
      }
      sb.append('\n');
    }
    return sb.toString();
  }

  private String canonicalSignedHeaders() {
    return this.signedHeaders.stream().map(String::toLowerCase).collect(Collectors.joining(";"));
  }

  @Override
  public String getCanonicalRequest() {
    return this.canonicalRequest;
  }

  @Override
  public String getRequestStringToSign() {
    return this.requestStringToSign;
  }

  @Override
  public String getAuthorization() {
    return this.authorization;
  }
}
