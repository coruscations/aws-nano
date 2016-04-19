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

import static java.lang.String.format;

public class Constants {

  // HTTP Environment Keys
  public static final String USER_AGENT = "USER_AGENT";
  public static final String SCHEME = "s3";

  public static final String JAVA_VERSION = System.getProperty("java.version");

  // Defaults
  // TODO: Add package version, in addition to java version
  public static final String DEFAULT_USER_AGENT = format(
      "NanoAWSClient/1.0 (compatible; Java/%s; +https://github.com/coruscations/aws-nano)",
      JAVA_VERSION == null ? "[UNKNOWN]" : JAVA_VERSION);
}
