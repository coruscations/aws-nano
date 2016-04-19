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

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface HttpURLConnectionBuilder {

  /**
   * Add a single query parameter; note that re-adding the same key will result in multiple values for the same key in the same order added.
   * @param key Key (not encoded)
   * @param val Value (not encoded)
   * @return The builder for chaining
   */
  HttpURLConnectionBuilder addQueryParameter(String key, String val);

  /**
   * Add a single header; note that re-adding the same key will result in multiple values for the same key in the same order added.
   * @param key Key (not encoded)
   * @param val Value (not encoded)
   * @return The builder for chaining
   */
  HttpURLConnectionBuilder addHeader(String key, String val, boolean isSigned);

  String getCanonicalRequest();

  String getRequestStringToSign();

  String getAuthorization();

  HttpURLConnection build() throws IOException;
}
