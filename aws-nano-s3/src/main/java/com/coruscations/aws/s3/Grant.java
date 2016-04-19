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

import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Grant {

  public enum Permission {
    FULL_CONTROL("x-amz-grant-full-control"),
    READ("x-amz-grant-read"),
    WRITE("x-amz-grant-write"),
    READ_ACP( "x-amz-grant-read-acp"),
    WRITE_ACP("x-amz-grant-write-acp");

    final String headerName;

    Permission(String headerName) {
      this.headerName = headerName;
    }
  }

  final Grantee grantee;
  final Permission permission;

  public Grant(Grantee grantee, Permission permission) {
    assert grantee != null;
    assert permission != null;
    this.grantee = grantee;
    this.permission = permission;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Grant grant = (Grant) o;
    return Objects.equals(grantee, grant.grantee) &&
           permission == grant.permission;
  }

  @Override
  public int hashCode() {
    return Objects.hash(grantee, permission);
  }
}
