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
import com.coruscations.aws.RestCommandResponse;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

@ParametersAreNonnullByDefault
class BucketGet extends BucketRestCommand<BucketGet.Response> {

  private static final Logger LOG = Logger.getLogger(BucketGet.class.getName());

  private static final String QUERY_DELIMITER = "delimiter";
  private static final String QUERY_ENCODING_TYPE = "encoding-type";
  private static final String QUERY_MARKER = "marker";
  private static final String QUERY_MAX_KEYS = "max-keys";
  private static final String QUERY_PREFIX = "prefix";

  private final String bucketName;
  @Nullable
  private final Character delimiter;
  @Nullable
  private final String prefix;
  @Nullable
  private final String encodingType;
  @Nullable
  private final String marker;
  @Nullable
  private final Integer maxKeys;

  public BucketGet(String bucketName, @Nullable Character delimiter, @Nullable String prefix,
                   @Nullable String encodingType, @Nullable String marker,
                   @Nullable Integer maxKeys) {
    super(bucketName);
    this.bucketName = bucketName;
    this.delimiter = delimiter;
    this.prefix = prefix;
    this.encodingType = encodingType;
    this.marker = marker;
    this.maxKeys = maxKeys;
  }

  @Nonnull
  @Override
  public HttpMethod getMethod() {
    return HttpMethod.GET;
  }

  @Override
  public void addParameters(HttpURLConnectionBuilder builder, Endpoint endpoint) {
    appendQueryParameter(builder, QUERY_DELIMITER, this.delimiter);
    appendQueryParameter(builder, QUERY_ENCODING_TYPE, this.encodingType);
    appendQueryParameter(builder, QUERY_MARKER, this.marker);
    appendQueryParameter(builder, QUERY_MAX_KEYS, this.maxKeys);
    appendQueryParameter(builder, QUERY_PREFIX, this.prefix);
  }

  private void appendQueryParameter(HttpURLConnectionBuilder builder, String key, Object value) {
    if (value != null) {
      builder.addQueryParameter(key, String.valueOf(value));
    }
  }

  @Nonnull
  @Override
  public Parser<Response> getResponseParser() {
    return (responseCode, headers, reader) -> {

      if (reader == null) {
        return new Response(responseCode, headers);
      }

      String name = null;
      String encodingType = null;
      String delimiter = null;
      String marker = null;
      String nextMarker = null;
      long maxKeys = 1000;
      boolean truncated = false;

      List<BucketItem> items = new LinkedList<>();
      List<CommonPrefix> commonPrefixes = new LinkedList<>();

      String key = null;
      String lastModified = null;
      String eTag = null;
      String size = null;
      String storageClass = null;

      // Shared by ListBucketResult and CommonPrefix
      String prefix = null;

      // Shared by Contents and CommonPrefix
      String ownerId = null;
      String ownerDisplayName = null;

      boolean isContents = false;
      boolean isCommonPrefix = false;

      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();
        int next = event.getEventType();
        if (next == XMLStreamConstants.START_ELEMENT) {
          StartElement startElement = event.asStartElement();
          String elementName = startElement.getName().getLocalPart();
          switch (elementName) {
            case "Contents":
              if (isCommonPrefix) {
                commonPrefixes.add(new CommonPrefix(prefix, ownerId, ownerDisplayName));
                prefix = null;
                ownerId = null;
                ownerDisplayName = null;
              } else if (key != null) {
                items.add(new BucketItem(key, lastModified, eTag, size, storageClass, ownerId,
                                         ownerDisplayName));
              }
              key = null;
              lastModified = null;
              eTag = null;
              size = null;
              storageClass = null;
              ownerId = null;
              ownerDisplayName = null;
              isContents = true;
              isCommonPrefix = false;
              break;
            case "CommonPrefixes":
              if (isContents) {
                items.add(new BucketItem(key, lastModified, eTag, size, storageClass, ownerId,
                                         ownerDisplayName));
                key = null;
                lastModified = null;
                eTag = null;
                size = null;
                storageClass = null;
                ownerId = null;
                ownerDisplayName = null;
              } else if (prefix != null) {
                commonPrefixes.add(new CommonPrefix(prefix, ownerId, ownerDisplayName));
              }
              prefix = null;
              ownerId = null;
              ownerDisplayName = null;
              isCommonPrefix = true;
              isContents = false;
              break;
            case "Delimiter":
              delimiter = reader.getElementText();
              break;
            case "DisplayName":
              ownerDisplayName = reader.getElementText();
              break;
            case "Encoding-Type":
              encodingType = reader.getElementText();
              break;
            case "ETag":
              eTag = reader.getElementText();
              break;
            case "ID":
              ownerId = reader.getElementText();
              break;
            case "IsTruncated":
              truncated = Boolean.valueOf(reader.getElementText());
              break;
            case "Key":
              key = reader.getElementText();
              break;
            case "LastModified":
              lastModified = reader.getElementText();
              break;
            case "Marker":
              marker = reader.getElementText();
              break;
            case "MaxKeys":
              String maxKeysText = reader.getElementText();
              maxKeys = maxKeysText == null || maxKeysText.isEmpty() ?
                        1000L : Long.valueOf(maxKeysText);
              break;
            case "Name":
              name = reader.getElementText();
              break;
            case "NextMarker":
              nextMarker = reader.getElementText();
              break;
            case "Prefix":
              prefix = reader.getElementText();
              break;
            case "Size":
              size = reader.getElementText();
              break;
            case "StorageClass":
              storageClass = reader.getElementText();
              break;
            case "Owner":
            case "ListBucketResult":
              // Ignore
              break;
            default:
              LOG.log(Level.FINE, "Unknown element in list bucket response: {0}", elementName);
              break;
          }
        }
      }
      if (isContents && key != null) {
        items.add(new BucketItem(key, lastModified, eTag, size, storageClass, ownerId,
                                 ownerDisplayName));
      }
      if (isCommonPrefix && prefix != null) {
        commonPrefixes.add(new CommonPrefix(prefix, ownerId, ownerDisplayName));
      }
      return new Response(responseCode, headers, name, prefix, delimiter, encodingType, marker,
                          nextMarker, maxKeys, truncated, items, commonPrefixes);
    };
  }

  public static class Response extends RestCommandResponse {

    private final String name;
    private final String prefix;
    private final String delimiter;
    private final String encodingType;
    private final String marker;
    private final String nextMarker;
    private final long maxKeys;
    private final boolean truncated;
    private final List<BucketItem> items;
    private final List<CommonPrefix> commonPrefixes;

    public Response(int responseCode, @Nonnull Map<String, List<String>> headers) {
      this(responseCode, headers, null, null, null, null, null, null, -1, false, null, null);
    }

    public Response(int responseCode, @Nonnull Map<String, List<String>> headers, String name,
                    String prefix, String delimiter, String encodingType, String marker,
                    String nextMarker, long maxKeys, boolean truncated, List<BucketItem> items,
                    List<CommonPrefix> commonPrefixes) {
      super(responseCode, headers);
      this.name = name;
      this.prefix = prefix;
      this.delimiter = delimiter;
      this.encodingType = encodingType;
      this.marker = marker;
      this.nextMarker = nextMarker;
      this.maxKeys = maxKeys;
      this.truncated = truncated;
      this.items = items;
      this.commonPrefixes = commonPrefixes;
    }

    @Override
    public String toString() {
      // TODO: implement
      return null;
    }

    public String getName() {
      return name;
    }

    public String getPrefix() {
      return prefix;
    }

    public String getDelimiter() {
      return delimiter;
    }

    public String getEncodingType() {
      return encodingType;
    }

    public String getMarker() {
      return marker;
    }

    public String getNextMarker() {
      return nextMarker;
    }

    public long getMaxKeys() {
      return maxKeys;
    }

    public boolean isTruncated() {
      return truncated;
    }

    public List<BucketItem> getItems() {
      return items;
    }

    public List<CommonPrefix> getCommonPrefixes() {
      return commonPrefixes;
    }
  }

  public static class BucketItem {

    private static final DateTimeFormatter LAST_MODIFIED_FORMATTER =
        DateTimeFormatter.ISO_DATE_TIME;

    private final String key;
    private final OffsetDateTime lastModified;
    private final String eTag;
    private final long size;
    private final StorageClass storageClass;
    private final String ownerId;
    private final String ownerDisplayName;

    public BucketItem(String key, String lastModified, String eTag, String size,
                      String storageClass, String ownerId, String ownerDisplayName) {
      this(key, LAST_MODIFIED_FORMATTER.parse(lastModified, OffsetDateTime::from), eTag,
           Long.valueOf(size), StorageClass.valueOf(storageClass), ownerId, ownerDisplayName);
    }

    public BucketItem(String key, OffsetDateTime lastModified, String eTag, long size,
                      StorageClass storageClass, String ownerId, String ownerDisplayName) {
      this.key = key;
      this.lastModified = lastModified;
      this.eTag = eTag;
      this.size = size;
      this.storageClass = storageClass;
      this.ownerId = ownerId;
      this.ownerDisplayName = ownerDisplayName;
    }

    public String getKey() {
      return key;
    }

    public OffsetDateTime getLastModified() {
      return lastModified;
    }

    public String geteTag() {
      return eTag;
    }

    public long getSize() {
      return size;
    }

    public StorageClass getStorageClass() {
      return storageClass;
    }

    public String getOwnerId() {
      return ownerId;
    }

    public String getOwnerDisplayName() {
      return ownerDisplayName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BucketItem that = (BucketItem) o;
      return size == that.size &&
             Objects.equals(key, that.key) &&
             Objects.equals(lastModified, that.lastModified) &&
             Objects.equals(eTag, that.eTag) &&
             Objects.equals(storageClass, that.storageClass) &&
             Objects.equals(ownerId, that.ownerId) &&
             Objects.equals(ownerDisplayName, that.ownerDisplayName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, lastModified, eTag, size, storageClass, ownerId, ownerDisplayName);
    }
  }

  public static class CommonPrefix {

    private final String prefix;
    private final String ownerId;
    private final String ownerDisplayName;

    public CommonPrefix(String prefix, String ownerId, String ownerDisplayName) {
      this.prefix = prefix;
      this.ownerId = ownerId;
      this.ownerDisplayName = ownerDisplayName;
    }

    public String getPrefix() {
      return prefix;
    }

    public String getOwnerId() {
      return ownerId;
    }

    public String getOwnerDisplayName() {
      return ownerDisplayName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CommonPrefix that = (CommonPrefix) o;
      return Objects.equals(prefix, that.prefix) &&
             Objects.equals(ownerId, that.ownerId) &&
             Objects.equals(ownerDisplayName, that.ownerDisplayName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(prefix, ownerId, ownerDisplayName);
    }
  }
}
