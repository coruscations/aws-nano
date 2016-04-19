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
import com.coruscations.aws.HttpMethod;
import com.coruscations.aws.HttpURLConnectionBuilder;
import com.coruscations.aws.Parser;
import com.grack.nanojson.JsonWriter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

@ParametersAreNonnullByDefault
class BucketGetAcl extends BucketRestCommand<BucketGetAcl.Response> {

  private static final Logger LOG = Logger.getLogger(BucketGetAcl.class.getName());

  public BucketGetAcl(String bucketName) {
    super(bucketName);
  }

  @Nonnull
  @Override
  public HttpMethod getMethod() {
    return HttpMethod.GET;
  }

  @Override
  public void addParameters(HttpURLConnectionBuilder builder, Endpoint endpoint) {
    builder.addQueryParameter("acl", "");
  }

  @Override
  public Parser<Response> getResponseParser() {
    return (responseCode, headers, reader) -> {

      if (reader == null) {
        return new Response(responseCode, headers, null, null, null);
      }

      String ownerId = null;
      String ownerDisplayName = null;

      Grant.Permission permission = null;
      Grantee.Type type = null;
      String item = null;
      String displayName = null;

      boolean inAcl = false;
      boolean inOwner = false;

      List<Grant> grants = new LinkedList<>();

      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();
        int next = event.getEventType();
        if (next == XMLStreamConstants.START_ELEMENT) {
          StartElement startElement = event.asStartElement();
          String elementName = startElement.getName().getLocalPart();
          switch (elementName) {
            case "ID":
              if (inOwner) {
                ownerId = reader.getElementText();
              } else if (inAcl) {
                item = reader.getElementText();
                type = Grantee.Type.ID;
              }
              break;
            case "DisplayName":
              if (inOwner) {
                ownerDisplayName = reader.getElementText();
              } else if (inAcl) {
                displayName = reader.getElementText();
              }
              break;
            case "Grant":
              if (type != null && item != null) {
                Grantee grantee = new Grantee(type, item, displayName);
                if (permission != null) {
                  grants.add(new Grant(grantee, permission));
                }
              }
              permission = null;
              type = null;
              item = null;
              displayName = null;
              break;
            case "URI":
              item = reader.getElementText();
              type = Grantee.Type.URI;
              break;
            case "EmailAddress":
              item = reader.getElementText();
              type = Grantee.Type.URI;
              break;
            case "Permission":
              String permissionText = reader.getElementText();
              permission = Grant.Permission.valueOf(permissionText);
              break;
            case "Owner":
              inOwner = true;
              inAcl = false;
              break;
            case "AccessControlList":
              inOwner = false;
              inAcl = true;
              break;
            case "Grantee":
            case "AccessControlPolicy":
              // Ignore these
              break;
            default:
              LOG.log(Level.FINE, "Unknown element in list buckets response: {0}", elementName);
              break;
          }
        }
      }
      if (type != null && item != null) {
        Grantee grantee = new Grantee(type, item, displayName);
        if (permission != null) {
          grants.add(new Grant(grantee, permission));
        }
      }
      return new Response(responseCode, headers, ownerId, ownerDisplayName, new Acl(grants));
    };
  }

  class Response extends OwnerRestCommandResponse {

    private final Acl acl;

    private Response(int responseCode, @Nonnull Map<String, List<String>> headers, String ownerId,
                     String ownerDisplayName, Acl acl)
        throws XMLStreamException {
      super(responseCode, headers, ownerId, ownerDisplayName);
      this.acl = acl;
    }

    public Acl getAcl() {
      return this.acl;
    }

    @Override
    public String toString() {
      return acl.writeJSON(writeOwner(JsonWriter.string().object())).done();
    }
  }

}
