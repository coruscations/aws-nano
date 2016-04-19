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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

@RunWith(Parameterized.class)
public class HttpURLConnectionBuilderFactoryTest extends TestLogging {

  private static final Logger LOG = Logger.getLogger(
      HttpURLConnectionBuilderFactoryTest.class.getName());

  // TODO: Move to properties
  private static final String ACCESS_KEY = "AKIDEXAMPLE";
  private static final String SECRETE_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

  private static final String REQUEST_EXT = "req";
  private static final String CANONICAL_REQUEST_EXT = "creq";
  private static final String REQUEST_STRING_TO_SIGN_EXT = "sts";
  private static final String AUTHORIZATION_HEADER_EXT = "authz";
  private static final String SIGNED_REQUEST_EXT = "sreq";

  private static final String[] PARAM_NAMES =
      {"Test Name", "Request File", "Canonical Request File", "String to Sign File",
       "Authorization File", "Signed Request File"};

  private static final Pattern REQUEST_LINE_PATTERN =
      Pattern.compile("^\\W*(\\w+) (\\S+)(?: .*)? (\\S+)$");
  private static final Pattern REQUEST_HEADER_PATTERN = Pattern.compile("^(\\S+):(.*)$");
  private static final Pattern REQUEST_HEADER_CONTINUATION_PATTERN = Pattern.compile("^( [^:]+)$");
  //  private static final Pattern REQUEST_PARAM_PATTERN = Pattern.compile("^(\\S+)=(\\S+)$");

  private final TestRestCommand testRestCommand;

  public HttpURLConnectionBuilderFactoryTest(TestRestCommand testRestCommand) {
    this.testRestCommand = testRestCommand;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> parameters() throws URISyntaxException, IOException {
    URL url = HttpURLConnectionBuilderFactoryTest.class.getResource("/aws4_testsuite.zip");
    Collection<Object[]> parameterList = new LinkedList<>();
    try (FileSystem fs = FileSystems.newFileSystem(new URI("jar:" + url.toString()), emptyMap())) {
      Files.newDirectoryStream(fs.getPath("/aws4_testsuite")).forEach(dir -> {
        try {
          // Scan directories containing other directories
          if (Files.list(dir).anyMatch(d -> Files.isDirectory(d))) {
            Files.newDirectoryStream(dir).forEach(subDir -> scanDir(parameterList, subDir));
            return;
          }
        } catch (IOException e) {
          throw new IllegalStateException("Cannot list sub-directory", e);
        }
        // Just parse directories
        scanDir(parameterList, dir);
      });
    }
    return parameterList;
  }

  private static void scanDir(Collection<Object[]> parameterList, Path dir) {
    // Skip anything that isn't a directory
    if (!Files.isDirectory(dir)) {
      LOG.log(Level.FINE, "Skipping non-directory: {0}", dir);
      return;
    }
    String dirName = dir.getFileName().toString();
    final String testName = dirName.charAt(dirName.length() - 1) == '/' ?
                            dirName.substring(0, dirName.length() - 1) : dirName;
    try {
      Map<String, String> map = Files.list(dir).collect(HashMap::new, (m, p) -> {
        String filename = p.getFileName().toString();
        int lastDot = filename.lastIndexOf(".");
        if (lastDot < 0) {
          LOG.log(Level.INFO, "Ignoring file missing dot in {0}: {1}",
                  new Object[]{testName, filename});
          return;
        }
        if (!Objects.equals(testName, filename.substring(0, lastDot))) {
          LOG.log(Level.INFO, "Ignoring file with unknown prefix in {0}: {1}",
                  new Object[]{testName, filename});
          return;
        }
        String ext = filename.substring(lastDot + 1);
        try {
          m.put(ext, new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
        } catch (IOException e) {
          throw new IllegalStateException("Cannot read file", e);
        }
      }, Map::putAll);
      if (map.values().stream().anyMatch(Objects::isNull)) {
        LOG.log(Level.WARNING, "Ignoring {1} because it is incomplete.", testName);
        return;
      }
      parameterList.add(new Object[]{new TestRestCommand(testName, map.get(REQUEST_EXT),
                                                         map.get(CANONICAL_REQUEST_EXT),
                                                         map.get(REQUEST_STRING_TO_SIGN_EXT),
                                                         map.get(AUTHORIZATION_HEADER_EXT),
                                                         map.get(SIGNED_REQUEST_EXT))});
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Failed to load directory: " + dir.toString(), e);
    }
  }

  @Test
  public void testCreateCanonicalRequest()
      throws URISyntaxException, NoSuchAlgorithmException, MalformedURLException {
    StandardHttpURLConnectionBuilder builder =
        (StandardHttpURLConnectionBuilder) this.testRestCommand.createRestRequestBuilder();
    Assert.assertEquals(this.testRestCommand.testName + ": Canonical request did not match",
                        this.testRestCommand.canonicalRequest, builder.createCanonicalRequest());
  }

  @Test
  public void testCreateRequestStringToSign()
      throws URISyntaxException, NoSuchAlgorithmException, MalformedURLException {
    StandardHttpURLConnectionBuilder builder =
        (StandardHttpURLConnectionBuilder) this.testRestCommand.createRestRequestBuilder();
    String requestStringToSign =
        builder.createRequestStringToSign(this.testRestCommand.canonicalRequest);
    Assert.assertEquals(this.testRestCommand.testName + ": Request string to sign did not match",
                        this.testRestCommand.requestStringToSign, requestStringToSign);
  }

  @Test
  public void testCreateAuthorization()
      throws URISyntaxException, NoSuchAlgorithmException, MalformedURLException,
             InvalidKeyException {
    StandardHttpURLConnectionBuilder builder =
        (StandardHttpURLConnectionBuilder) this.testRestCommand.createRestRequestBuilder();
    String authorization = builder.createAuthorization(this.testRestCommand.requestStringToSign);
    Assert.assertEquals(this.testRestCommand.testName + ": Authorization did not match",
                        this.testRestCommand.authorizationHeader, authorization);
  }

  private static class TestRestCommand implements RestCommand<EmptyRestCommandResponse> {

    private final String testName;
    private final String request;
    private final String canonicalRequest;
    private final String requestStringToSign;
    private final String authorizationHeader;
    private final String signedRequest;

    private final HttpMethod method;
    private final String host;
    private final String path;
    private final String httpVersion;
    private final Map<String, List<String>> queryParameters;
    private final Map<String, List<String>> headers;
    private final String body;

    private TestRestCommand(String testName, String request, String canonicalRequest,
                            String requestStringToSign, String authorizationHeader,
                            String signedRequest)
        throws MalformedURLException {
      this.testName = testName;
      this.request = request;
      this.canonicalRequest = canonicalRequest;
      this.requestStringToSign = requestStringToSign;
      this.authorizationHeader = authorizationHeader;
      this.signedRequest = signedRequest;

      // Parse the request
      String[] lines = request.split("\n");
      Matcher requestLineMatcher = REQUEST_LINE_PATTERN.matcher(lines[0]);
      if (!requestLineMatcher.matches()) {
        throw new IllegalArgumentException("Invalid request (" + testName + "):\n" + request);
      }
      this.method = HttpMethod.valueOf(requestLineMatcher.group(1));

      String pathAndQuery = requestLineMatcher.group(2);
      int questionIndex = pathAndQuery.indexOf('?');
      this.path = questionIndex >= 0 ? pathAndQuery.substring(0, questionIndex) : pathAndQuery;
      this.queryParameters = computeQueryParameters(questionIndex > 0 &&
                                                    questionIndex != pathAndQuery.length() - 1 ?
                                                    pathAndQuery.substring(questionIndex + 1) :
                                                    null);

      this.httpVersion = requestLineMatcher.group(3);

      Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      List<String> bodyLines = new LinkedList<>();
      parseRemainingRequestLines(testName, lines, headers, bodyLines);
      this.headers = unmodifiableMap(headers);
      this.body = bodyLines.stream().collect(Collectors.joining("\n"));

      String host = headers.get("Host") == null || headers.get("Host").isEmpty() ? null :
                    headers.get("Host").iterator().next();
      if (host == null) {
        throw new IllegalArgumentException("Request missing host (" + testName + "):\n" + request);
      }
      this.host = host;
    }

    private HttpURLConnectionBuilder createRestRequestBuilder() {
      Map<String, ?> env = createEnvironment();
      ConfigurationProvider configurationProvider = new ConfigurationProvider(env);
      HttpURLConnectionBuilderFactory
          factory = new HttpURLConnectionBuilderFactory(configurationProvider);
      HttpURLConnectionBuilder builder = factory.createHttpURLConnectionBuilder(this);
      return builder;
    }

    private Map<String, ?> createEnvironment() {
      Map<String, Object> env = new HashMap<>();
      env.put(AwsCredentialProperty.AWS_ACCESS_KEY_ID.getEnvName(), ACCESS_KEY);
      env.put(AwsCredentialProperty.AWS_SECRET_KEY.getEnvName(), SECRETE_KEY);
      // Hack to prevent these from being set
      env.put(HttpHeaders.CONTENT_TYPE, new Object());
      env.put(HttpHeaders.USER_AGENT, new Object());
      return env;
    }

    private Map<String, List<String>> computeQueryParameters(String query) {
      Map<String, List<String>> queryParameters = new HashMap<>();
      if (query != null) {
        String partialName = "";
        for (String queryComponent : query.split("&")) {
          String[] nameVal = queryComponent.split("=");
          if (nameVal.length == 1 && !queryComponent.endsWith("=")) {
            partialName += nameVal[0] + "&";
            continue;
          }
          String name = partialName + (nameVal.length < 1 || nameVal[0] == null ? "" : nameVal[0]);
          partialName = "";
          String value = nameVal.length < 2 || nameVal[1] == null ? "" : nameVal[1];
          List<String> values = queryParameters.get(name);
          if (values == null) {
            values = new LinkedList<>();
            queryParameters.put(name, values);
          }
          values.add(value);
        }
        // Crude hack, but works
        if (partialName.length() > 0) {
          partialName = partialName.substring(0, partialName.length() - 1);
          List<String> values = queryParameters.get(partialName);
          if (values == null) {
            values = new LinkedList<>();
            queryParameters.put(partialName, values);
          }
          values.add("");
        }
      }
      return queryParameters;
    }

    private void parseRemainingRequestLines(String testName, String[] lines,
                                            Map<String, List<String>> headers,
                                            List<String> bodyLines) {
      boolean parseBody = false;
      String lastHeader = null;
      for (int i = 1; i < lines.length; i++) {
        String line = lines[i];
        if (line.trim().isEmpty()) {
          parseBody = true;
          continue;
        }
        if (parseBody) {
          bodyLines.add(line);
        }
        Matcher headerMatcher = REQUEST_HEADER_PATTERN.matcher(line);
        if (headerMatcher.matches()) {
          lastHeader = headerMatcher.group(1);
          List<String> values = headers.get(lastHeader);
          if (values == null) {
            values = new LinkedList<>();
            headers.put(lastHeader, values);
          }
          values.add(headerMatcher.group(2));
          continue;
        }
        Matcher headerContinuationMatcher = REQUEST_HEADER_CONTINUATION_PATTERN.matcher(line);
        if (headerContinuationMatcher.matches()) {
          if (lastHeader == null) {
            LOG.log(Level.WARNING, "Ignoring header continuation line ({0}): '{1}''",
                    new Object[]{testName, line});
            continue;
          }
          headers.get(lastHeader).add(line);
        }
      }
    }

    @Override
    public String toString() {
      return this.testName;
    }

    @Nonnull
    @Override
    public String getServiceName() {
      return "service";
    }

    @Nonnull
    @Override
    public HttpMethod getMethod() {
      return this.method;
    }

    @Nonnull
    @Override
    public String getHost(Endpoint endpoint) {
      return this.host;
    }

    @Nonnull
    @Override
    public String getPath(Endpoint endpoint) {
      return this.path;
    }

    @Nonnull
    @Override
    public String createBody(Endpoint endpoint) {
      return this.body;
    }

    @Override
    public void addParameters(HttpURLConnectionBuilder builder, Endpoint endpoint) {
      this.queryParameters.entrySet().forEach(
          e -> e.getValue().forEach(v -> builder.addQueryParameter(e.getKey(), v)));
    }

    @Override
    public void addHeaders(HttpURLConnectionBuilder builder, Endpoint endpoint) {
      this.headers.entrySet().forEach(e -> {
        // All the headers in the example are signed
        e.getValue().forEach(v -> builder.addHeader(e.getKey(), v, true));
      });
    }

    @Nonnull
    @Override
    public Parser<EmptyRestCommandResponse> getResponseParser() {
      return EmptyRestCommandResponse.getResponseParser();
    }
  }
}