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

import com.coruscations.aws.ErrorResponse;
import com.coruscations.aws.Parser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

class S3ErrorResponse extends ErrorResponse {

  private static final Logger LOG = Logger.getLogger(S3ErrorResponse.class.getName());

  private final String code;
  private final String message;
  private final String resource;
  private final String requestId;
  private final String awsAccessKey;
  private final String canonicalRequest;
  private final byte[] canonicalRequestBytes;
  private final String stringToSign;
  private final byte[] stringToSignBytes;
  private final String signatureProvided;

  static Parser<S3ErrorResponse> getParser() {
    return (responseCode, headers, reader) -> {

      if (reader == null) {
        return new S3ErrorResponse(responseCode, headers, null, null, null, null, null, null, null,
                                   null, null, null);
      }

      String code = null;
      String message = null;
      String resource = null;
      String requestId = null;

      // Signature debugging
      String awsAccessKey = null;
      String canonicalRequest = null;
      byte[] canonicalRequestBytes = null;
      String stringToSign = null;
      byte[] stringToSignBytes = null;
      String signatureProvided = null;

      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();
        int next = event.getEventType();
        if (next == XMLStreamConstants.START_ELEMENT) {
          StartElement startElement = event.asStartElement();
          String elementName = startElement.getName().getLocalPart();
          switch (elementName) {
            case "Code":
              code = reader.getElementText();
              break;
            case "Message":
              message = reader.getElementText();
              break;
            case "Resource":
              resource = reader.getElementText();
              break;
            case "RequestId":
              requestId = reader.getElementText();
              break;
            case "AWSAccessKeyId":
              awsAccessKey = reader.getElementText();
              break;
            case "StringToSign":
              stringToSign = reader.getElementText();
              break;
            case "StringToSignBytes":
              String[] stringToSignByteStrings = reader.getElementText().trim().split("\\s+");
              stringToSignBytes = new byte[stringToSignByteStrings.length];
              for (int i = 0; i < stringToSignByteStrings.length; i++) {
                stringToSignBytes[i] = Byte.valueOf(stringToSignByteStrings[i], 16);
              }
              break;
            case "CanonicalRequest":
              canonicalRequest = reader.getElementText();
              break;
            case "CanonicalRequestBytes":
              String[] canonicalRequestByteStrings = reader.getElementText().trim().split("\\s+");
              canonicalRequestBytes = new byte[canonicalRequestByteStrings.length];
              for (int i = 0; i < canonicalRequestByteStrings.length; i++) {
                canonicalRequestBytes[i] = Byte.valueOf(canonicalRequestByteStrings[i], 16);
              }
              break;
            case "SignatureProvided":
              signatureProvided = reader.getElementText();
              break;
            case "Error":
              // Ignore
              break;
            default:
              LOG.log(Level.FINER, "Unknown element in list buckets response: {0}", elementName);
              break;
          }
        } else if (next == XMLStreamConstants.END_ELEMENT &&
                   "Error".equals(event.asEndElement().getName().getLocalPart())) {
          // Done; if we go to the end of the document, it closes.
          break;
        }
      }
      return new S3ErrorResponse(responseCode, headers, code, message, resource, requestId,
                                 awsAccessKey, canonicalRequest, canonicalRequestBytes,
                                 stringToSign, stringToSignBytes, signatureProvided);
    };
  }

  private S3ErrorResponse(int responseCode, Map<String, List<String>> headers, String code,
                          String message, String resource, String requestId,
                          String awsAccessKey, String canonicalRequest,
                          byte[] canonicalRequestBytes, String stringToSign,
                          byte[] stringToSignBytes, String signatureProvided) {
    super(responseCode, headers);
    this.code = code;
    this.message = message;
    this.resource = resource;
    this.requestId = requestId;
    this.awsAccessKey = awsAccessKey;
    this.canonicalRequest = canonicalRequest;
    this.canonicalRequestBytes = canonicalRequestBytes;
    this.stringToSign = stringToSign;
    this.stringToSignBytes = stringToSignBytes;
    this.signatureProvided = signatureProvided;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public String getResource() {
    return resource;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getAwsAccessKey() {
    return awsAccessKey;
  }

  public String getCanonicalRequest() {
    return canonicalRequest;
  }

  public byte[] getCanonicalRequestBytes() {
    return canonicalRequestBytes;
  }

  public String getStringToSign() {
    return stringToSign;
  }

  public byte[] getStringToSignBytes() {
    return stringToSignBytes;
  }

  public String getSignatureProvided() {
    return signatureProvided;
  }

  @Override
  public String toString() {
    if (this.canonicalRequest == null) {
      return String.format("%s: (%d: %s) %s [%s]", this.resource, getResponseCode(), this.code,
                           this.message, this.requestId);
    }
    return String.format("%s: (%d: %s) %s [%s]\n" +
                         "Key: %s\n" +
                         "Canonical Request:\n%s\n" +
                         "CanonicalRequestBytes:%s\n" +
                         "StringToSign:\n%s\n" +
                         "StringToSignBytes: %s\n" +
                         "SignatureProvided: %s\n",
                         this.resource, getResponseCode(), this.code, this.message, this.requestId,
                         this.awsAccessKey, this.canonicalRequest,
                         Arrays.toString(this.canonicalRequestBytes), this.stringToSign,
                         Arrays.toString(this.stringToSignBytes), this.signatureProvided);
  }
}
