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

import java.util.logging.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class HttpURLConnectionBuilderFactory {

  private static final Logger LOG = Logger.getLogger(HttpURLConnectionBuilderFactory.class.getName());

  private final ConfigurationProvider configurationProvider;

  public HttpURLConnectionBuilderFactory(ConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
  }

  HttpURLConnectionBuilder createHttpURLConnectionBuilder(RestCommand<?> restCommand) {
    StandardHttpURLConnectionBuilder builder =
        new StandardHttpURLConnectionBuilder(this.configurationProvider);
    String serviceName = restCommand.getServiceName();
    builder.setServiceName(serviceName);
    builder.setMethod(restCommand.getMethod());
    Endpoint endpoint = this.configurationProvider.getEndpoint(serviceName);
    builder.setHost(restCommand.getHost(endpoint));
    restCommand.addHeaders(builder, endpoint);

    builder.setPath(restCommand.getPath(endpoint));
    restCommand.addParameters(builder, endpoint);

    builder.setBody(restCommand.createBody(endpoint));

    builder.initializeHeaders();
    return builder;
  }
}
