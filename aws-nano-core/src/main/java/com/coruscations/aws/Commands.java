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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;

public class Commands<T extends ErrorResponse> {

  private static final Logger LOG = Logger.getLogger(Commands.class.getName());

  private static DateTimeFormatter AMZ_DATE_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

  private final HttpURLConnectionBuilderFactory httpURLConnectionBuilderFactory;
  private final ConfigurationProvider configurationProvider;

  private final HttpHelper<T> httpHelper;

  private final XMLInputFactory xmlInputFactory;

  protected Commands(ConfigurationProvider configurationProvider, Parser<T> errorParser) {
    this.configurationProvider = configurationProvider;
    this.httpURLConnectionBuilderFactory = new HttpURLConnectionBuilderFactory(
        configurationProvider);
    // TODO: Figure out how to allow configuration
    this.xmlInputFactory = XMLInputFactory.newFactory();
    this.httpHelper = new HttpHelper<>(errorParser);
  }

  protected <R extends RestCommandResponse> R execute(RestCommand<R> command) throws IOException {
    HttpURLConnectionBuilder builder =
        this.httpURLConnectionBuilderFactory.createHttpURLConnectionBuilder(command);
    HttpURLConnection connection = builder.build();
    try {
      return this.httpHelper.processRequest(connection, this.xmlInputFactory,
                                            command.getResponseParser());
    } catch (ErrorResponseException e) {
      if (LOG.isLoggable(Level.FINE)) {
        StringBuilder sb = new StringBuilder();
        sb.append("Canonical failed request:\n").append(builder.getCanonicalRequest());
        sb.append("\nRequest string to sign:\n").append(builder.getRequestStringToSign());
        sb.append("\nAuthorization: ").append(builder.getAuthorization()).append('\n');
        ErrorResponse errorResponse = e.getErrorResponse();
        if (errorResponse == null) {
          sb.append("No response\n");
        } else {
          Map<String, List<String>> responseHeaders = errorResponse.getHeaders();
          sb.append(responseHeaders.get(null)).append('\n');
          responseHeaders.forEach((name, values) ->
                                      values.forEach(value -> {
                                        if (name != null) {
                                          sb.append(name).append(": ")
                                              .append(value).append('\n');
                                        }
                                      }));

          sb.append(errorResponse.toString());
        }
        LOG.fine(sb.toString());
      }
      throw e;
    }
  }

  public HttpURLConnectionBuilderFactory getHttpURLConnectionBuilderFactory() {
    return this.httpURLConnectionBuilderFactory;
  }

  public ConfigurationProvider getConfigurationProvider() {
    return this.configurationProvider;
  }

  public String getRegion() {
    return this.configurationProvider.getRegion();
  }

  public HttpHelper<T> getHttpHelper() {
    return this.httpHelper;
  }

  public XMLInputFactory getXmlInputFactory() {
    return this.xmlInputFactory;
  }
}
