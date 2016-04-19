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

import com.coruscations.aws.RestCommandResponse;
import com.grack.nanojson.JsonStringWriter;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

abstract class OwnerRestCommandResponse extends RestCommandResponse {

  private final String ownerId;
  private final String ownerDisplayName;

  protected OwnerRestCommandResponse(int responseCode, @Nonnull
      Map<String, List<String>> headers, String ownerId, String ownerDisplayName) {
    super(responseCode, headers);
    this.ownerId = ownerId;
    this.ownerDisplayName = ownerDisplayName;
  }

  public String getOwnerId() {
    return this.ownerId;
  }

  public String getOwnerDisplayName() {
    return this.ownerDisplayName;
  }

  protected JsonStringWriter writeOwner(JsonStringWriter writer) {
    return writer.object("Owner").value("Id", this.ownerId)
        .value("DisplayName", this.ownerDisplayName).end();
  }
}
