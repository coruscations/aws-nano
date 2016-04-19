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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class BinaryCollatorSortTest extends TestLogging {

  public final Charset charset;
  public final String source;
  public final String target;
  public final int result;

  public BinaryCollatorSortTest(Charset charset, String source, String target, int result) {
    this.charset = charset;
    this.source = source;
    this.target = target;
    this.result = result;
  }


  @Parameterized.Parameters(name = "{index}: [{0}] compare({1}, {2}) = {3}")
  public static Collection<Object[]> parameters() throws URISyntaxException, IOException {
    return Arrays.<Object[]>asList(
        new Object[]{StandardCharsets.US_ASCII, "a", "a", 0},
        new Object[]{StandardCharsets.US_ASCII, "a", "b", "a".charAt(0) - "b".charAt(0)},
        new Object[]{StandardCharsets.US_ASCII, "b", "a", "b".charAt(0) - "a".charAt(0)},
        new Object[]{StandardCharsets.US_ASCII, "a", "aa", -1},
        new Object[]{StandardCharsets.US_ASCII, "aa", "a", 1},
        new Object[]{StandardCharsets.UTF_8, "a", "a", 0},
        new Object[]{StandardCharsets.UTF_8, "a", "b", "a".charAt(0) - "b".charAt(0)},
        new Object[]{StandardCharsets.UTF_8, "b", "a", "b".charAt(0) - "a".charAt(0)},
        new Object[]{StandardCharsets.UTF_8, "a", "aa", -1},
        new Object[]{StandardCharsets.UTF_8, "aa", "a", 1}
    );
  }


  @Test
  public void testCompare() {
    assertEquals(this.result, new BinaryCollator(this.charset).compare(this.source, this.target));
  }
}