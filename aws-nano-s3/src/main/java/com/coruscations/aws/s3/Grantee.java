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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Grantee {

  public enum Type {
    EMAIL_ADDRESS("emailAddress"), ID("id"), URI("uri");

    final String headerName;

    Type(String headerName) {
      this.headerName = headerName;
    }

    public String getHeaderName() {
      return headerName;
    }
  }

  final Type type;
  final String item;

  @Nullable
  final String displayName;

  public Grantee(Type type, String item, @Nullable String displayName) {
    assert type != null;
    assert item != null;
    this.type = type;
    this.item = item;
    this.displayName = displayName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Grantee grantee = (Grantee) o;
    return type == grantee.type && Objects.equals(item, grantee.item);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, item);
  }

  @Override
  public String toString() {
    return this.type.getHeaderName() + "=\"" + this.item + "\"";
  }
}
