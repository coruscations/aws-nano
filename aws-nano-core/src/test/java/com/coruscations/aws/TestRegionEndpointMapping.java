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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestRegionEndpointMapping implements RegionEndpointMapping {

  private final Map<String, Endpoint> regionEndpointMap;

  public TestRegionEndpointMapping() {
    this.regionEndpointMap = Arrays.stream(
        new String[]{"us-east-1", "us-west-1", "us-west-2", "eu-west-2", "eu-central-1",
                     "eu-central-1", "ap-northeast-1", "ap-northeast-2", "ap-southeast-1",
                     "ap-southeast-2", "sa-east-1"})
        .collect(HashMap<String, Endpoint>::new,
                 (map, region) -> map.put(region,
                                          new Endpoint(region,
                                                       new Endpoint.Scheme[]{Endpoint.Scheme.HTTP},
                                                       "localhost", false, false)),
                 HashMap::putAll);
  }

  @Nonnull
  @Override
  public String getAwsServiceName() {
    return "service";
  }

  @Nullable
  @Override
  public Endpoint getEndpoint(String region) {
    return this.regionEndpointMap.get(region);
  }
}
