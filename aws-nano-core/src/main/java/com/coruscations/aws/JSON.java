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

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

abstract class JSON {

  private static final JSON json = createJSON();

  public static JSON getJSON() {
    return json;
  }

  private static JSON createJSON() {
    return new NanoJSON();
  }

  protected abstract Object parse(String json);
  protected abstract Object getObject(Object parsed, String name);
  protected abstract Object[] getArray(Object parsed, String name);
  protected abstract String getString(Object parsed, String name);
//  protected abstract int getInt(Object parsed, String name);
//  protected abstract long getLong(Object parsed, String name);
//  protected abstract float getFloat(Object parsed, String name);
//  protected abstract double getDouble(Object parsed, String name);
}

class NanoJSON extends JSON {

  private JsonParser.JsonParserContext<Object> parser = JsonParser.any();

  @Override
  protected Object parse(String json) {
    try {
      return this.parser.from(json);
    } catch (JsonParserException e) {
      throw new IllegalArgumentException("Invalid JSON", e);
    }
  }

  @Override
  protected Object getObject(Object parsed, String name) {
    return toJsonObject(parsed).get(name);
  }

  @Override
  protected Object[] getArray(Object parsed, String name) {
    JsonArray array = toJsonObject(parsed).getArray(name);
    return array.toArray(new Object[array.size()]);
  }

  @Override
  protected String getString(Object parsed, String name) {
    return toJsonObject(parsed).getString(name);
  }

  private JsonObject toJsonObject(Object parsed) {
    if (parsed instanceof JsonObject) {
      return (JsonObject) parsed;
    }
    throw new IllegalArgumentException("Invalid object for path lookup; did you forget to parse?");
  }
}
