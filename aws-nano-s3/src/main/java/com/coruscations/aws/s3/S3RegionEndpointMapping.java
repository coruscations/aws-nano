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

import com.coruscations.aws.Endpoint;
import com.coruscations.aws.RegionEndpointMapping;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class S3RegionEndpointMapping implements RegionEndpointMapping {

  private final Map<String, Endpoint> regionEndpointMap;

  public S3RegionEndpointMapping() {
    Endpoint.Scheme[] schemes = Endpoint.Scheme.values();
    Map<String, Endpoint> regionEndpointMap = new HashMap<>();
    addEndpoint(regionEndpointMap, "us-east-1", schemes);
    addEndpoint(regionEndpointMap, "us-west-1", schemes);
    addEndpoint(regionEndpointMap, "us-west-2", schemes);
    addEndpoint(regionEndpointMap, "eu-west-2", schemes);
    addEndpoint(regionEndpointMap, "eu-central-1", schemes);
    addEndpoint(regionEndpointMap, "eu-central-1", schemes);
    addEndpoint(regionEndpointMap, "ap-northeast-1", schemes);
    addEndpoint(regionEndpointMap, "ap-northeast-2", schemes);
    addEndpoint(regionEndpointMap, "ap-southeast-1", schemes);
    addEndpoint(regionEndpointMap, "ap-southeast-2", schemes);
    addEndpoint(regionEndpointMap, "sa-east-1", schemes);
    this.regionEndpointMap = regionEndpointMap;
  }

  private void addEndpoint(Map<String, Endpoint> map, String region, Endpoint.Scheme[] schemes) {
    String hostname = "us-east-1".equals(region) ? "s3.amazonaws.com" :
                  "s3-" + region + ".s3.amazonaws.com";
    map.put(region, new Endpoint(region, schemes, hostname, true, true));
  }

  @Nonnull
  @Override
  public String getAwsServiceName() {
    return S3RestCommand.S3_SERVICE_NAME;
  }

  @Nullable
  @Override
  public Endpoint getEndpoint(String region) {
    return this.regionEndpointMap.get(region);
  }
}
