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

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;

public class S3ServiceCommandsIT extends S3CommandIT {

  private static final Logger LOG = Logger.getLogger(S3ServiceCommandsIT.class.getName());

  private static final ConfigurationProvider CONFIGURATION_PROVIDER =
      new ConfigurationProvider(createEnvironment());

  @ClassRule
  public static final TestRule MINIO =
      new MinioRule(CONFIGURATION_PROVIDER, S3RestCommand.S3_SERVICE_NAME);

  private final S3ServiceCommands s3ServiceCommands =
      new S3ServiceCommands(CONFIGURATION_PROVIDER);

  @Test
  public void listBuckets() throws IOException {
    BucketsGet.Response response = this.s3ServiceCommands.listBuckets();
    assertSuccessResponse(response);
    assertNotNull("Missing owner display name", response.getOwnerDisplayName());
    assertNotNull("Missing owner id", response.getOwnerId());
    assertNotNull("Missing headers", response.getHeaders());
  }
}