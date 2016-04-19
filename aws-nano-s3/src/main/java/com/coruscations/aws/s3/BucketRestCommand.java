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
import com.coruscations.aws.RestCommandResponse;

import javax.annotation.Nonnull;

abstract class BucketRestCommand<T extends RestCommandResponse> implements S3RestCommand<T> {

  protected final String bucketName;

  protected BucketRestCommand(String bucketName) {
    this.bucketName = validateBucketName(bucketName);
  }

  @Nonnull
  @Override
  public String getHost(Endpoint endpoint) {
    return endpoint.isAllowSubDomains() ?
           this.bucketName + "." + endpoint.getHost() : endpoint.getHost();
  }

  @Nonnull
  @Override
  public String getPath(Endpoint endpoint) {
    return endpoint.isAllowSubDomains() ? "/" : "/" + this.bucketName + "/";
  }

  @Nonnull
  protected String validateBucketName(String bucketName) {
    if (bucketName == null || bucketName.isEmpty()) {
      throw new IllegalArgumentException("Bucket name not set");
    }
    if (bucketName.indexOf('/') >= 0) {
      throw new IllegalArgumentException("Bucket names may not contain slashes: " + bucketName);
    }
    return bucketName;
  }
}
