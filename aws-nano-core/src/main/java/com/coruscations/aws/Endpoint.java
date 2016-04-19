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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

public class Endpoint {

  private final String region;
  private final List<Scheme> allowedSchemes;
  private final String host;
  private final boolean allowSubDomains;
  private final boolean requireContentHashHeader;

  @ParametersAreNonnullByDefault
  public Endpoint(String region, String urlSpec, boolean allowSubDomains,
                  boolean requireContentHashHeader)
      throws URISyntaxException {
    this.region = region;
    URI url = new URI(urlSpec);
    this.allowedSchemes = Collections.singletonList(Scheme.valueOf(url.getScheme().toUpperCase()));
    this.host = url.getAuthority(); // Use authority because we want the port, too, if non-default
    this.allowSubDomains = allowSubDomains;
    this.requireContentHashHeader = requireContentHashHeader;
  }

  @ParametersAreNonnullByDefault
  public Endpoint(String region, Scheme[] allowedSchemes, String host, boolean allowSubDomains,
                  boolean requireContentHashHeader) {
    this(region, Arrays.asList(allowedSchemes), host, allowSubDomains, requireContentHashHeader);
  }

  @ParametersAreNonnullByDefault
  public Endpoint(String region, List<Scheme> allowedSchemes, String host,
                  boolean allowSubDomains, boolean requireContentHashHeader) {
    this.region = region;
    this.allowedSchemes = Collections.unmodifiableList(allowedSchemes);
    this.host = host;
    this.allowSubDomains = allowSubDomains;
    this.requireContentHashHeader = requireContentHashHeader;
  }

  public String getRegion() {
    return this.region;
  }

  public List<Scheme> getAllowedSchemes() {
    return this.allowedSchemes;
  }

  public Scheme getPreferredScheme() {
    return this.allowedSchemes.get(0);
  }

  public String getHost() {
    return this.host;
  }

  public boolean isAllowSubDomains() {
    return this.allowSubDomains;
  }

  public boolean isRequireContentHashHeader() {
    return this.requireContentHashHeader;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Endpoint endpoint = (Endpoint) o;
    return Objects.equals(region, endpoint.region) &&
           Objects.equals(allowedSchemes, endpoint.allowedSchemes) &&
           Objects.equals(host, endpoint.host);
  }

  @Override
  public int hashCode() {
    return Objects.hash(region, allowedSchemes, host);
  }

  public enum Scheme {
    HTTP("http"), HTTPS("https");

    private final String urlScheme;

    Scheme(String urlScheme) {
      this.urlScheme = urlScheme;
    }

    public String getUrlScheme() {
      return urlScheme;
    }
  }
}
