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

import com.coruscations.aws.EmptyRestCommandResponse;
import com.coruscations.aws.Endpoint;
import com.coruscations.aws.HttpHeaders;
import com.coruscations.aws.HttpMethod;
import com.coruscations.aws.HttpURLConnectionBuilder;
import com.coruscations.aws.Parser;

import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
class BucketPut extends BucketRestCommand<EmptyRestCommandResponse> {

  private static final Logger LOG = Logger.getLogger(BucketPut.class.getName());

  private final Acl acl;

  public BucketPut(String bucketName, Acl acl) {
    super(bucketName);
    this.acl = acl;
  }

  @Nonnull
  @Override
  public HttpMethod getMethod() {
    return HttpMethod.PUT;
  }

  @Override
  public String createBody(Endpoint endpoint) {
    // The default region does not need a location constraint.
    String region = endpoint.getRegion();
    if (isDefaultRegion(region)) {
      return "";
    }
    return String.format("<CreateBucketConfiguration xmlns=\"%s\">\n" +
                         "  <LocationConstraint>%s</LocationConstraint>\n" +
                         "</CreateBucketConfiguration>",
                         XMLNS, region);
  }

  protected boolean isDefaultRegion(String region) {
    return "us-east-1".equals(region);
  }

  @Override
  public void addHeaders(HttpURLConnectionBuilder builder, Endpoint endpoint) {
    if (this.acl != null) {
      this.acl.addHeaders(builder);
    }
    if (isDefaultRegion(endpoint.getRegion())) {
      return;
    }
    builder.addHeader(HttpHeaders.CONTENT_TYPE, "text/plain", true);
  }

  @Nonnull
  @Override
  public Parser<EmptyRestCommandResponse> getResponseParser() {
    return EmptyRestCommandResponse.getResponseParser();
  }
}