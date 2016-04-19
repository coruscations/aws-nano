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

package com.coruscations.aws.s3;

import com.coruscations.aws.Endpoint;
import com.coruscations.aws.HttpMethod;
import com.coruscations.aws.HttpURLConnectionBuilder;
import com.coruscations.aws.Parser;
import com.coruscations.aws.RestCommandResponse;
import com.grack.nanojson.JsonWriter;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.stream.XMLStreamException;

@ParametersAreNonnullByDefault
class BucketGetCors extends BucketRestCommand<BucketGetCors.Response> {

  private static final Logger LOG = Logger.getLogger(BucketGetCors.class.getName());

  protected BucketGetCors(String bucketName) {
    super(bucketName);
  }

  @Nonnull
  @Override
  public HttpMethod getMethod() {
    return HttpMethod.GET;
  }

  @Override
  public void addParameters(HttpURLConnectionBuilder builder, Endpoint endpoint) {
    builder.addQueryParameter("cors", "");
  }

  @Nonnull
  @Override
  public Parser<Response> getResponseParser() {
    return (responseCode, headers, reader) -> new Response(responseCode, headers,
                                                           reader == null ? null :
                                                           new Cors(reader));
  }

  class Response extends RestCommandResponse {

    private final Cors cors;

    private Response(int responseCode, @Nonnull Map<String, List<String>> headers, Cors cors)
        throws XMLStreamException {
      super(responseCode, headers);
      this.cors = cors;
    }

    @Override
    public String toString() {
      return this.cors.writeJSON(JsonWriter.string()).done();
    }
  }

}
