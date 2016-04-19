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

import com.coruscations.aws.HttpURLConnectionBuilder;
import com.coruscations.aws.s3.Grant.Permission;
import com.grack.nanojson.JsonStringWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Acl {

  private static final Logger LOG = Logger.getLogger(Acl.class.getName());

  private static final String CANNED_ACL_HEADER = "x-amz-acl";

  private final BucketCannedAcl cannedAcl;
  private final List<Grant> grants;

  public Acl(BucketCannedAcl cannedAcl) {
    this.cannedAcl = cannedAcl;
    this.grants = Collections.emptyList();
  }

  public Acl(Grant... grants) {
    this.cannedAcl = null;
    this.grants = Collections.unmodifiableList(Arrays.asList(grants));
  }

  public Acl(Collection<Grant> grants) {
    this.cannedAcl = null;
    this.grants = Collections.unmodifiableList(new ArrayList<>(grants));
  }

  public BucketCannedAcl getCannedAcl() {
    return this.cannedAcl;
  }

  public List<Grant> getGrants() {
    return this.grants;
  }

  public void addHeaders(HttpURLConnectionBuilder builder) {
    if (this.cannedAcl != null) {
      builder.addHeader(CANNED_ACL_HEADER, this.cannedAcl.getAclName(), true);
    }
    EnumMap<Permission, Set<Grantee>> permissionGranteeMap = new EnumMap<>(Permission.class);
    for (Grant grant : this.grants) {
      Permission permission = grant.permission;
      Set<Grantee> grantees = permissionGranteeMap.get(permission);
      if (grantees == null) {
        grantees = new LinkedHashSet<>();
        permissionGranteeMap.put(permission, grantees);
      }
      grantees.add(grant.grantee);
    }

    for (Permission permission : permissionGranteeMap.keySet()) {
      Set<Grantee> grantees = permissionGranteeMap.get(permission);
      if (grantees.size() == 1) {
        builder.addHeader(permission.headerName, grantees.iterator().next().toString(), true);
        continue;
      }
      builder.addHeader(permission.headerName,
                        grantees.stream().map(Grantee::toString).collect(Collectors.joining(", ")),
                        true);
    }
  }

  protected JsonStringWriter writeJSON(JsonStringWriter writer) {
    JsonStringWriter aclJson = writer.object("AccessControlList");
    for (Grant grant : this.grants) {
      JsonStringWriter granteeJson = aclJson.object("Grant").object("Grantee");
      switch (grant.grantee.type) {
        case EMAIL_ADDRESS:
          granteeJson.value("EmailAddress", grant.grantee.item);
          break;
        case ID:
          granteeJson.value("ID", grant.grantee.item);
          if (grant.grantee.displayName != null) {
            granteeJson.value("DisplayName", grant.grantee.displayName);
          }
          break;
        case URI:
          granteeJson.value("URI", grant.grantee.item);
          break;
      }
      aclJson = granteeJson.end().value("Permission", grant.permission.name()).end();
    }
    return aclJson.end();
  }
}
