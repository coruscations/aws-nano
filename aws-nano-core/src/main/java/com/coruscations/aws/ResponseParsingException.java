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

@SuppressWarnings("serial")
public class ResponseParsingException extends IOException {

  private final String requestString;

  public ResponseParsingException(String requestString) {
    this.requestString = requestString;
  }

  public ResponseParsingException(String requestString, String message) {
    super(message);
    this.requestString = requestString;
  }

  public ResponseParsingException(String requestString, String message, Throwable cause) {
    super(message, cause);
    this.requestString = requestString;
  }

  public ResponseParsingException(String requestString, Throwable cause) {
    super(cause);
    this.requestString = requestString;
  }

  public String getRequestString() {
    return this.requestString;
  }
}
