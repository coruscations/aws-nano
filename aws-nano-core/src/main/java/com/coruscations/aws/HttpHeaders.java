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

public interface HttpHeaders {

  String AUTHORIZATION = "Authorization";
  String CONTENT_TYPE = "Content-Type";
  String DATE = "Date";
  String HOST = "Host";
  String USER_AGENT = "User-Agent";

  String X_AMZ_CONTENT_SHA256 = "x-amz-content-sha256";
  String X_AMZ_DATE = "X-Amz-Date";
}
