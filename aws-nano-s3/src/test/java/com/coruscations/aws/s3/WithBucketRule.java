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

import com.coruscations.aws.ConfigurationProvider;
import com.coruscations.aws.ErrorResponseException;

import org.junit.rules.ExternalResource;

import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.security.SecureRandom;

import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

public class WithBucketRule extends ExternalResource {

  private final S3BucketCommands s3BucketCommands;
  private final String bucketName;
  private final boolean requireEmpty;

  private boolean createdByRule = false;

  public WithBucketRule(ConfigurationProvider configurationProvider) {
    this(configurationProvider, "test-" + new BigInteger(50, new SecureRandom()).toString(32),
         true);
  }

  public WithBucketRule(ConfigurationProvider configurationProvider, String bucketName,
                        boolean requireEmpty) {
    this.s3BucketCommands = new S3BucketCommands(configurationProvider);
    this.bucketName = bucketName;
    this.requireEmpty = requireEmpty;
  }

  @Override
  protected void before() throws Throwable {
    boolean bucketExists = checkBucketExists();
    if (bucketExists) {
      if (this.requireEmpty) {
        checkBucketEmpty();
      }
      return;
    }
    createBucket();
  }

  private void createBucket() {
    try {
      this.s3BucketCommands.make(this.bucketName, null);
      this.createdByRule = true;
      return;
    } catch (Exception e) {
      assumeNoException("Error creating bucket " + this.bucketName, e);
    }
  }

  private boolean checkBucketEmpty() {
    BucketGet.Response response;
    try {
      response = this.s3BucketCommands.ls(this.bucketName, null, null, null, null, null);
    } catch (Exception e) {
      assumeNoException("Exception confirming bucket empty " + this.bucketName, e);
      return true;
    }
    assumeTrue("Bucket is not empty", response.getItems().isEmpty());
    return false;
  }

  private boolean checkBucketExists() {
    boolean bucketExists = false;
    try {
      this.s3BucketCommands.checkAccess(this.bucketName);
      bucketExists = true;
    } catch (ErrorResponseException e) {
      S3ErrorResponse response = (S3ErrorResponse) e.getErrorResponse();
      int responseCode = response == null ? -1 : response.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_NOT_FOUND) {
        assumeNoException("Unexpected response (" + responseCode + ": " +
                          (response == null ? "[none]" : response.getMessage()) +
                          ") checking for bucket: " + this.bucketName, e);
      }
    } catch (Exception e) {
      assumeNoException("Exception checking access to " + this.bucketName, e);
    }
    return bucketExists;
  }

  @Override
  protected void after() {
    // Only remove if we created it and it still exists
    if (!this.createdByRule || !checkBucketExists()) {
      return;
    }
    try {
      this.s3BucketCommands.remove(this.bucketName);
    } catch (Exception e) {
      assumeNoException("Exception removing bucket " + this.bucketName, e);
      return;
    }
  }

  public String getBucketName() {
    return this.bucketName;
  }
}
