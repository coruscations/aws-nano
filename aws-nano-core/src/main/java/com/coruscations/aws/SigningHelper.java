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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

class SigningHelper {

  public static final String SHA_256_ALGORITHM = "SHA-256";
  public static final String HMAC_SHA_256_ALGORITHM = "HmacSHA256";
  public static final String SHA256_EMPTY_STRING_HASH =
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  public static String hash(String text, MessageDigest messageDigest) {
    byte[] hash = messageDigest.digest(text.getBytes());
    return hashBytesToString(hash, 64);
  }

  public static String hashBytesToString(byte[] hash, int length) {
    BigInteger bi = new BigInteger(1, hash);
    String result = bi.toString(16);
    // Zero pad in case the first characters end up being chopped because they are zero
    while (result.length() < length) {
      result = "0" + result;
    }
    return result;
  }

  static MessageDigest getMessageDigest(String algorithm) {
    try {
      return MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException("Invalid algorithm: " + algorithm);
    }
  }

  static Mac getMac(String algorithm) {
    try {
      return Mac.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException("Invalid algorithm: " + algorithm);
    }
  }
}
