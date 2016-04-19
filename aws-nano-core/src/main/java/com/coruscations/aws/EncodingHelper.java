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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class EncodingHelper {

  static final Pattern URL_COMPONENT_ENCODING_PATTERN;
  static {
    URL_COMPONENT_ENCODING_PATTERN =
        Pattern.compile(format("%s|%s|%s|%s", Pattern.quote("+"), Pattern.quote("*"),
                               Pattern.quote("%7E"), Pattern.quote("%2F")));
  }

  /**
   * Borrowed from the AWS Java SDK, which is released under the Apache 2 licnese
   */
  static String awsEncodeURLComponent(final String value, final boolean path) {
    if (value == null) {
      return "";
    }

    try {
      String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.name());

      Matcher matcher = URL_COMPONENT_ENCODING_PATTERN.matcher(encoded);
      StringBuffer buffer = new StringBuffer(encoded.length());

      while (matcher.find()) {
        String replacement = matcher.group(0);

        if ("+".equals(replacement)) {
          replacement = "%20";
        } else if ("*".equals(replacement)) {
          replacement = "%2A";
        } else if ("%7E".equals(replacement)) {
          replacement = "~";
        } else if (path && "%2F".equals(replacement)) {
          replacement = "/";
        }

        matcher.appendReplacement(buffer, replacement);
      }

      matcher.appendTail(buffer);
      return buffer.toString();

    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }
}
