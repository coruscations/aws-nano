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

package com.coruscations.aws.s3.fs;

import com.coruscations.aws.Constants;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
class FSKey implements Comparable<FSKey> {

  private final String accessKey;
  private final String secretKey;
  private final String endpoint;
  private final String[] prefixes;
  private final Type type;

  private final long createdMilli;
  private final int createdNano;

  FSKey(String accessKey, String secretKey, String endpoint, @Nullable String prefix) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.endpoint = endpoint;
    this.prefixes = prefix == null || prefix.isEmpty() ?
                    new String[0] : prefix.substring(1).split("/");
    Instant now = Instant.now();
    this.createdMilli = now.toEpochMilli();
    this.createdNano = now.getNano();
    switch (this.prefixes.length) {
      case 0:
        this.type = Type.ENDPOINT;
        break;
      case 1:
        this.type = Type.BUCKET;
        break;
      default:
        this.type = Type.PARTIAL_BUCKET;
        break;
    }
  }

  URI toURI(boolean includeUserInfo) throws URISyntaxException {
    return new URI(String.format(includeUserInfo ? "%1$s//%2$s:%3$s@%4$s/%5$s" : "%1$s//%4$s/%5$s",
                                 Constants.SCHEME, this.accessKey, this.secretKey, this.endpoint,
                                 String.join("/", this.prefixes)));
  }

  String getAccessKey() {
    return this.accessKey;
  }

  String getSecretKey() {
    return this.secretKey;
  }

  String getEndpoint() {
    return this.endpoint;
  }

  String[] getPrefixes() {
    return this.prefixes;
  }

  long getCreatedMilli() {
    return this.createdMilli;
  }

  int getCreatedNano() {
    return this.createdNano;
  }

  @Override
  public int compareTo(FSKey o) {
    int i = this.endpoint.compareTo(o.endpoint);
    if (i != 0) {
      return i;
    }

    // Return sorted by path with the most specific path first
    for (int j = 0; j < this.prefixes.length; j++) {
      if (o.prefixes.length == j) {
        return -1;
      }
      i = this.prefixes[j].compareTo(o.prefixes[j]);
      if (i != 0) {
        return i;
      }
    }
    // The prefix up to a matching length compared equally, other sorts first if it's longer.
    if (this.prefixes.length < o.prefixes.length) {
      return 1;
    }

    if (this.createdMilli == o.createdMilli) {
      return o.createdNano - this.createdNano;
    }
    return this.createdMilli < o.createdMilli ? -1 : 1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FSKey fsKey = (FSKey) o;
    return Objects.equals(this.accessKey, fsKey.accessKey) &&
           Objects.equals(this.secretKey, fsKey.secretKey) &&
           Objects.equals(this.endpoint, fsKey.endpoint) &&
           Arrays.equals(this.prefixes, fsKey.prefixes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.accessKey, this.secretKey, this.endpoint, this.prefixes);
  }

  enum Type { ENDPOINT, BUCKET, PARTIAL_BUCKET }
}
