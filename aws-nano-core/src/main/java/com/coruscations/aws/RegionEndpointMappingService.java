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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class RegionEndpointMappingService {

  private static final Logger LOG = Logger.getLogger(RegionEndpointMappingService.class.getName());

  private final ServiceLoader<RegionEndpointMapping> loader =
      ServiceLoader.load(RegionEndpointMapping.class);
  private final Map<String, RegionEndpointMapping> nameMappingMap;

  private RegionEndpointMappingService() {
    Map<String, RegionEndpointMapping> nameMappingMap = new HashMap<>();
    Iterator<RegionEndpointMapping> regionEndpointMappings = this.loader.iterator();
    while (regionEndpointMappings.hasNext()) {
      RegionEndpointMapping mapping = regionEndpointMappings.next();
      nameMappingMap.put(mapping.getAwsServiceName(), mapping);
    }
    this.nameMappingMap = Collections.unmodifiableMap(nameMappingMap);
  }

  public static RegionEndpointMappingService getInstance() {
    return RegionalEndpointMappingServiceHolder.INSTANCE;
  }

  public Endpoint getEndpoint(String awsService, String region) {
    Objects.requireNonNull(awsService, "AWS service name may not be null");
    Objects.requireNonNull(region, "Region may not be null");
    RegionEndpointMapping mapping = this.nameMappingMap.get(awsService);
    Objects.requireNonNull(mapping, "Unsupported service: " + awsService);
    return mapping.getEndpoint(region);
  }

  private static class RegionalEndpointMappingServiceHolder {

    private static final RegionEndpointMappingService INSTANCE =
        new RegionEndpointMappingService();
  }
}
