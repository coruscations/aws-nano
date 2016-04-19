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

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Objects;

class BinaryCollator extends Collator implements Serializable {

  private static final long serialVersionUID = 5842603444471921631L;

  private transient Charset charset;

  BinaryCollator(Charset charset) {
    this.charset = charset;
  }

  @Override
  public int compare(String source, String target) {
    return compareBytes(source.getBytes(this.charset), target.getBytes(this.charset));
  }

  @Override
  public CollationKey getCollationKey(String source) {
    return new BinaryCollationKey(this.charset, source);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.charset);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    BinaryCollator that = (BinaryCollator) o;
    return Objects.equals(charset, that.charset);
  }

  private static int compareBytes(byte[] source, byte[] target) {
    for (int i = 0; i < source.length; i++) {
      if (i >= target.length) {
        return 1;
      }
      byte s = source[i];
      byte t = target[i];
      if (s == t) {
        continue;
      }
      return s - t;
    }
    if (target.length > source.length) {
      return -1;
    }
    return 0;
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.writeObject(this.charset.name());
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.charset = Charset.forName((String) in.readObject());
  }

  static class BinaryCollationKey extends CollationKey {

    private final Charset charset;

    private byte[] bytes;

    BinaryCollationKey(Charset charset, String str) {
      super(str);
      this.charset = charset;
    }

    @Override
    public int compareTo(CollationKey otherKey) {
      return compareBytes(this.bytes, otherKey.toByteArray());
    }

    @Override
    public byte[] toByteArray() {
      byte[] bytes = this.bytes;
      if (bytes == null) {
        // This is not really thread safe, but it doesn't matter; worse-case is double-computation
        //   of the utf8 bytes, which is usually cheaper than any locks.
        bytes = getSourceString().getBytes(this.charset);
        this.bytes = bytes;
      }
      return bytes;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BinaryCollationKey that = (BinaryCollationKey) o;
      return Objects.equals(charset, that.charset) &&
             Objects.equals(getSourceString(), that.getSourceString());
    }

    @Override
    public int hashCode() {
      return Objects.hash(charset, getSourceString());
    }
  }
}
