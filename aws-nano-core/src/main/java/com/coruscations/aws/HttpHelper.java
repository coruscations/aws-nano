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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

public class HttpHelper<T extends ErrorResponse> {

  private static final Logger LOG = Logger.getLogger(HttpHelper.class.getName());

  private static final Pattern CHARSET_PATTERN = Pattern.compile("charset\\s*=\\s*(\\S+)\\s*;?");
  private static final Pattern QUOTED_CHARSET_PATTERN = Pattern.compile(
      "charset=\"[^\"]*\"\\s*;?");

  private static Map<String, Charset> cachedCharsets = new ConcurrentHashMap<>();

  private final Parser<T> errorResponseParser;

  public HttpHelper(Parser<T> errorResponseParser) {
    this.errorResponseParser = errorResponseParser;
  }

  public Charset getCharset(String contentType) {
    if (contentType == null || contentType.isEmpty()) {
      return StandardCharsets.UTF_8;
    }
    Charset found = cachedCharsets.get(contentType);
    if (found != null) {
      return found;
    }
    found = getCharset(contentType, CHARSET_PATTERN);
    if (found != null) {
      return found;
    }
    found = getCharset(contentType, QUOTED_CHARSET_PATTERN);
    if (found != null) {
      return found;
    }
    return StandardCharsets.UTF_8;
  }

  private Charset getCharset(String contentType, Pattern pattern) {
    Matcher matcher = pattern.matcher(contentType);
    if (matcher.find()) {
      String charsetName = matcher.group(1);
      try {
        Charset charset = Charset.forName(charsetName);
        cachedCharsets.put(contentType, charset);
        return charset;
      } catch (IllegalArgumentException e) {
        LOG.log(Level.FINE, "Invalid charset name \"{0}\" in: ",
                new Object[]{charsetName, contentType});
      }
    }
    return null;
  }

  public String requestToString(HttpURLConnection connection) {
    try {
      connection.disconnect();
      StringBuilder sb = new StringBuilder();
      connection.getRequestProperties().forEach((name, values) -> {
        sb.append(name);
        values.forEach((value) -> {
          if (value != null) {
            sb.append(':').append(value);
          }
          sb.append('\n');
        });
      });
      return sb.substring(0, sb.length() - 1);
    } catch (Exception e) {
      LOG.fine("Failed to serialize connection for debugging.");
      return null;
    }
  }

  public <S> S processRequest(HttpURLConnection connection,
                              XMLInputFactory xmlInputFactory,
                              Parser<S> parser)
      throws IOException {
    int responseCode = connection.getResponseCode();
    Map<String, List<String>> headers = connection.getHeaderFields();
    Charset charset = getCharset(connection.getContentType());
    T responseError = null;
    try (InputStream is = getInputStream(connection, charset)) {
      switch (responseCode) {
        case HttpURLConnection.HTTP_OK:
        case HttpURLConnection.HTTP_CREATED:
        case HttpURLConnection.HTTP_ACCEPTED:
        case HttpURLConnection.HTTP_NO_CONTENT:
          return parseWithParser(responseCode, headers, is, charset, xmlInputFactory, parser);
        default:
          responseError = parseWithParser(responseCode, headers, is, charset, xmlInputFactory,
                                          this.errorResponseParser);
      }
      // Call disconnect here to get the connection to disconnect;
      //   cannot be done after the input stream is closed.
      connection.disconnect();
    } catch (Exception e) {
      throw new ResponseParsingException(requestToString(connection),
                                         "Failed to parse error response.", e);
    }
    throw new ErrorResponseException(requestToString(connection), responseError);
  }

  private InputStream getInputStream(HttpURLConnection connection, Charset charset)
      throws IOException {
    // Java is really stupid sometimes!!!
    InputStream is = connection.getResponseCode() < 400 ?
                     connection.getInputStream() : connection.getErrorStream();
    if (LOG.isLoggable(Level.FINE)) {
      if (is == null) {
        LOG.fine("Null response from: " + connection.getURL());
      } else {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int len;
        while ((len = is.read(buf)) >= 0) {
          if (len > 0) {
            baos.write(buf, 0, len);
          }
        }
        byte[] responseBytes = baos.toByteArray();
        String response = new String(responseBytes, charset);
        LOG.fine("Response from: " + connection.getURL() + "\n" + response);
        is = new ByteArrayInputStream(responseBytes);
      }
    }
    return is;
  }

  private <S> S parseWithParser(int responseCode, Map<String, List<String>> headers,
                                InputStream responseStream, Charset charset,
                                XMLInputFactory xmlInputFactory, Parser<S> parser)
      throws XMLStreamException {
    XMLEventReader reader = null;
    try {
      reader = responseStream == null ? null :
               xmlInputFactory.createXMLEventReader(responseStream, charset.name());
      return parser.parse(responseCode, headers, reader);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (XMLStreamException e) {
          // This is very rare, so just create a logger for it by cheating
          Logger log = Logger.getLogger(new Exception().getStackTrace()[1].getClassName());
          log.log(Level.FINE, "Failed to close reader", e);
        }
      }
    }
  }
}
