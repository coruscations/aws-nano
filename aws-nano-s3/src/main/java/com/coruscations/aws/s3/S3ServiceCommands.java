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

import java.io.IOException;
import java.util.logging.Logger;

class S3ServiceCommands extends S3Commands {

  private static final Logger LOG = Logger.getLogger(S3ServiceCommands.class.getName());

  S3ServiceCommands(ConfigurationProvider configurationProvider) {
    super(configurationProvider);
  }

  BucketsGet.Response listBuckets() throws IOException {
    return execute(new BucketsGet());
  }
}
