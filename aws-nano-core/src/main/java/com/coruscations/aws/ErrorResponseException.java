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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNullableByDefault;

@ParametersAreNullableByDefault
@SuppressWarnings("serial")
public class ErrorResponseException extends IOException {

  private final String requestString;
  private final ErrorResponse errorResponse;

  public ErrorResponseException(String requestString, ErrorResponse errorResponse) {
    super(errorResponse == null ? null : errorResponse.toString());
    this.requestString = requestString;
    this.errorResponse = errorResponse;
  }

  public ErrorResponseException(String requestString, ErrorResponse errorResponse, String message) {
    super(message);
    this.requestString = requestString;
    this.errorResponse = errorResponse;
  }

  public ErrorResponseException(String requestString, ErrorResponse errorResponse, String message,
                                Throwable cause) {
    super(message, cause);
    this.requestString = requestString;
    this.errorResponse = errorResponse;
  }

  public ErrorResponseException(String requestString, ErrorResponse errorResponse,
                                Throwable cause) {
    super(errorResponse == null ? null : errorResponse.toString(), cause);
    this.requestString = requestString;
    this.errorResponse = errorResponse;
  }

  @Nullable
  public String getRequestString() {
    return this.requestString;
  }

  @Nullable
  public ErrorResponse getErrorResponse() {
    return this.errorResponse;
  }
}
