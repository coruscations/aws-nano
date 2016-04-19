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

public enum Region {
  US_WEST_1("us-west-1"),
  US_WEST_2("us-west-2"),

  EU("EU"),
  EU_WEST_1("eu-west-1"),
  EU_CENTRAL_1("eu-central-1"),

  AP_SOUTHEAST_1("ap-southeast-1"),
  AP_SOUTHEAST_2("ap-southeast-2"),
  AP_NORTHEAST_1("ap-northeast-1"),
  AP_NORTHEAST_2("ap-northeast-2"),

  SA_EAST_1("sa-east-1");

  private final String regionName;

  Region(String regionName) {
    this.regionName = regionName;
  }

  public String getRegionName() {
    return regionName;
  }
}
