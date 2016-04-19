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

import com.coruscations.aws.Commands;
import com.coruscations.aws.ConfigurationProvider;

import java.util.logging.Logger;

abstract class S3Commands extends Commands<S3ErrorResponse> {

  private static final Logger LOG = Logger.getLogger(S3Commands.class.getName());

  S3Commands(ConfigurationProvider configurationProvider) {
    super(configurationProvider, S3ErrorResponse.getParser());
  }
}
