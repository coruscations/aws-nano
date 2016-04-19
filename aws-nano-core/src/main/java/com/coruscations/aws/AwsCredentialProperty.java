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

import java.util.Objects;

import javax.annotation.Nonnull;

public enum AwsCredentialProperty {
  AWS_ACCESS_KEY_ID("ACCESS_KEY", "aws.accessKeyId",
                    new String[]{"AWS_ACCESS_KEY_ID", "AWS_ACCESS_KEY"},
                    "aws_access_key_id", "AccessKeyId"),
  AWS_SECRET_KEY("SECRET_KEY", "aws.secretKey",
                 new String[]{"AWS_SECRET_ACCESS_KEY", "AWS_SECRET_KEY"},
                 "aws_secret_access_key", "SecretAccessKey"),
  AWS_CONFIG_FILE("AWS_CONFIG_FILE", null, new String[]{"AWS_CONFIG_FILE"}, null, null),
  AWS_CREDENTIAL_PROFILES_FILE("AWS_CREDENTIAL_PROFILES_FILE", null,
                               new String[]{"AWS_CREDENTIAL_PROFILES_FILE"}, null, null),
  AWS_ENDPOINT("AWS_ENDPOINT", null, new String[]{"AWS_ENDPOINT"}, "endpoint", null),
  AWS_DEFAULT_PROFILE("AWS_DEFAULT_PROFILE", null, new String[]{"AWS_DEFAULT_PROFILE"}, null, null),
  AWS_DEFAULT_REGION("AWS_DEFAULT_REGION", null, new String[]{"AWS_DEFAULT_REGION"},
                     "region", null),
  IAM_ROLE_NAME("IAM_ROLE_NAME", null, new String[]{"IAM_ROLE_NAME"}, null, null);

  private final String envName;
  private final String systemPropertyName;
  private final String[] systemEnvNames;
  private final String iniFileName;
  private final String metadataName;

  AwsCredentialProperty(@Nonnull  String envName, String systemPropertyName, String[] systemEnvNames,
                        String iniFileName, String metadataName) {
    this.envName = envName;
    this.systemPropertyName = systemPropertyName;
    this.systemEnvNames = systemEnvNames;
    this.iniFileName = iniFileName;
    this.metadataName = metadataName;
  }

  public String getEnvName() {
    return envName;
  }

  public String getSystemPropertyName() {
    return systemPropertyName;
  }

  public String[] getSystemEnvNames() {
    return systemEnvNames;
  }

  public String getIniFileName() {
    return iniFileName;
  }

  public String getMetadataName() {
    return metadataName;
  }

  public static AwsCredentialProperty fromIniFileName(@Nonnull String name) {
    for (AwsCredentialProperty awsCredentialProperty : AwsCredentialProperty.values()) {
      String iniName = awsCredentialProperty.getIniFileName();
      if (Objects.equals(iniName, name)) {
        return awsCredentialProperty;
      }
    }
    return null;
  }
}
