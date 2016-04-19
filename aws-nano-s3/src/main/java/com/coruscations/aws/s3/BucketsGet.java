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
import com.coruscations.aws.Parser;
import com.grack.nanojson.JsonStringWriter;
import com.grack.nanojson.JsonWriter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

class BucketsGet implements S3RestCommand<BucketsGet.Response> {

  private static final Logger LOG = Logger.getLogger(BucketsGet.class.getName());

  @Nonnull
  @Override
  public HttpMethod getMethod() {
    return HttpMethod.GET;
  }

  @Nonnull
  @Override
  public String getPath(Endpoint endpoint) {
    return "/";
  }

  @Nonnull
  @Override
  public Parser<Response> getResponseParser() {
    return  (responseCode, headers, reader) -> {

      if (reader == null) {
        return new Response(responseCode, headers, null, null, null);
      }

      String ownerId = null;
      String ownerDisplayName = null;

      Collection<BucketsGet.Bucket> buckets = new LinkedList<>();

      String bucketName = null;
      DateTimeFormatter dateParser = DateTimeFormatter.ISO_DATE_TIME;
      OffsetDateTime bucketCreationDate = null;

      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();
        int next = event.getEventType();
        if (next == XMLStreamConstants.START_ELEMENT) {
          StartElement startElement = event.asStartElement();
          String elementName = startElement.getName().getLocalPart();
          switch (elementName) {
            case "ID":
              ownerId = reader.getElementText();
              break;
            case "DisplayName":
              ownerDisplayName = reader.getElementText();
              break;
            case "Bucket":
              if (bucketName != null) {
                buckets.add(new BucketsGet.Bucket(bucketName, bucketCreationDate));
              }
              bucketName = null;
              bucketCreationDate = null;
              break;
            case "Name":
              bucketName = reader.getElementText();
              break;
            case "CreationDate":
              String dateString = reader.getElementText();
              bucketCreationDate = dateString == null ? null :
                                   dateParser.parse(dateString, OffsetDateTime::from);
              break;
            case "ListAllMyBucketsResult":
            case "Owner":
            case "Buckets":
              // Ignore these
              break;
            default:
              LOG.log(Level.FINE, "Unknown element in list buckets response: {0}", elementName);
              break;
          }
        }
      }
      // Add the last bucket
      if (bucketName != null) {
        buckets.add(new BucketsGet.Bucket(bucketName, bucketCreationDate));
      }
      return new Response(responseCode, headers, ownerId, ownerDisplayName, buckets);
    };
  }

  class Response extends OwnerRestCommandResponse {

    private final Collection<Bucket> buckets;

    private Response(int responseCode, @Nonnull Map<String, List<String>> headers, String ownerId,
                     String ownerDisplayName, Collection<Bucket> buckets)
        throws XMLStreamException {
      super(responseCode, headers, ownerId, ownerDisplayName);
      this.buckets = Collections.unmodifiableCollection(buckets);
    }

    private StringBuilder writeQuoted(StringBuilder sb, String val) {
      return sb.append('"').append(val).append('"');
    }

    public Collection<Bucket> getBuckets() {
      return this.buckets;
    }

    @Override
    public String toString() {
      JsonStringWriter bucketArray = writeOwner(JsonWriter.string().object()).array("Buckets");
      this.buckets.forEach(b -> bucketArray.object().value("Name", b.name)
          .value("CreationDate", b.creationDate.toString()).end());
      return bucketArray.end().end().done();
    }
  }

  static class Bucket {

    private final String name;
    private final OffsetDateTime creationDate;

    Bucket(String name, OffsetDateTime creationDate) {
      this.name = name;
      this.creationDate = creationDate;
    }

    public String getName() {
      return this.name;
    }

    public OffsetDateTime getCreationDate() {
      return this.creationDate;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Bucket bucket = (Bucket) o;
      return Objects.equals(name, bucket.name) &&
             Objects.equals(creationDate, bucket.creationDate);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, creationDate);
    }
  }
}
