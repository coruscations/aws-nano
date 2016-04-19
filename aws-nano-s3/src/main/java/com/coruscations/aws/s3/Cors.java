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

import com.coruscations.aws.HttpMethod;
import com.grack.nanojson.JsonStringWriter;

import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import static java.util.Collections.singleton;

@ParametersAreNonnullByDefault
public class Cors {

  private static final Logger LOG = Logger.getLogger(Cors.class.getName());

  private static final int MAX_RULES = 100;

  private Collection<Rule> rules = new LinkedHashSet<>();

  public Cors() {
  }

  public Cors(@Nonnull XMLEventReader reader) throws XMLStreamException {
    String id = null;
    Collection<String> allowedOrigins = new LinkedHashSet<>();
    Collection<HttpMethod> allowedMethods = new LinkedHashSet<>();
    int maxAgeSeconds = Integer.MIN_VALUE;
    Collection<String> exposeHeaders = new LinkedHashSet<>();

    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      int next = event.getEventType();
      if (next == XMLStreamConstants.START_ELEMENT) {
        StartElement startElement = event.asStartElement();
        String elementName = startElement.getName().getLocalPart();
        switch (elementName) {
          case "CORSRule":
            if (!allowedMethods.isEmpty() && !allowedMethods.isEmpty()) {
              addRule(id, allowedOrigins, allowedMethods, maxAgeSeconds, exposeHeaders);
            }
            id = null;
            allowedOrigins = new LinkedHashSet<>();
            allowedMethods = new LinkedHashSet<>();
            maxAgeSeconds = Integer.MIN_VALUE;
            exposeHeaders = new LinkedHashSet<>();
            break;
          case "ID":
            id = reader.getElementText();
            break;
          case "AllowedOrigin":
            allowedOrigins.add(reader.getElementText());
            break;
          case "AllowedMethod":
            allowedMethods.add(HttpMethod.valueOf(reader.getElementText()));
            break;
          case "MaxAgeSeconds":
            maxAgeSeconds = Integer.valueOf(reader.getElementText());
            break;
          case "ExposeHeader":
            allowedMethods.add(HttpMethod.valueOf(reader.getElementText()));
            break;
          case "CORSConfiguration":
            // Ignore these
            break;
          default:
            LOG.log(Level.FINE, "Unknown element in list buckets response: {0}", elementName);
            break;
        }
      }
    }
    if (!allowedMethods.isEmpty() && !allowedMethods.isEmpty()) {
      addRule(id, allowedOrigins, allowedMethods, maxAgeSeconds, exposeHeaders);
    }
  }

  public void addRule(String allowedOrigin, HttpMethod allowedMethod) {
    addRule(null, allowedOrigin, allowedMethod, Integer.MIN_VALUE, null);
  }

  public void addRule(String allowedOrigin, HttpMethod allowedMethod, int maxAgeSeconds) {
    addRule(null, allowedOrigin, allowedMethod, maxAgeSeconds, null);
  }

  public void addRule(String allowedOrigin, HttpMethod allowedMethod, int maxAgeSeconds,
                      @Nullable String exposeHeader) {
    addRule(null, allowedOrigin, allowedMethod, maxAgeSeconds, exposeHeader);
  }

  public void addRule(@Nullable String id, String allowedOrigin, HttpMethod allowedMethod,
                      int maxAgeSeconds, @Nullable String exposeHeader) {
    if (this.rules.size() + 1 == MAX_RULES) {
      throw new IllegalStateException("Max rules count exceeded");
    }
    this.rules.add(new Rule(id, singleton(allowedOrigin), singleton(allowedMethod), maxAgeSeconds,
                            singleton(exposeHeader)));
  }


  public void addRule(Collection<String> allowedOrigins,
                      Collection<HttpMethod> allowedMethods) {
    addRule(null, allowedOrigins, allowedMethods, Integer.MIN_VALUE, null);
  }

  public void addRule(Collection<String> allowedOrigins, Collection<HttpMethod> allowedMethods,
                      int maxAgeSeconds) {
    addRule(null, allowedOrigins, allowedMethods, maxAgeSeconds, null);

  }

  public void addRule(Collection<String> allowedOrigins,
                      Collection<HttpMethod> allowedMethods, int maxAgeSeconds,
                      @Nullable Collection<String> exposeHeaders) {
    addRule(null, allowedOrigins, allowedMethods, maxAgeSeconds, exposeHeaders);
  }

  public void addRule(@Nullable String id, Collection<String> allowedOrigins,
                      Collection<HttpMethod> allowedMethods, int maxAgeSeconds,
                      @Nullable Collection<String> exposeHeaders) {
    if (this.rules.size() + 1 == MAX_RULES) {
      throw new IllegalStateException("Max rules count exceeded");
    }
    this.rules.add(new Rule(id, allowedOrigins, allowedMethods, maxAgeSeconds, exposeHeaders));
  }

  protected String toRequestXml() {
    atLeastOne(this.rules, "rule");
    XMLEventFactory factory = XMLEventFactory.newFactory();
    StringWriter sw = new StringWriter();
    XMLEventWriter writer = null;
    try {
      writer = XMLOutputFactory.newInstance().createXMLEventWriter(sw);
      writer.add(factory.createStartDocument());
      writer.add(factory.createStartElement(null, null, "CORSConfiguration"));
      for (Rule rule : this.rules) {
        writer.add(factory.createStartElement(null, null, "CORSRule"));

        addCollection(factory, writer, rule.allowedOrigins, "AllowedOrigin");
        addCollection(factory, writer, rule.allowedMethods.stream().map(HttpMethod::name)
            .collect(Collectors.toSet()), "AllowedMethod");

        if (rule.maxAgeSeconds > Integer.MIN_VALUE) {
          writer.add(factory.createStartElement(null, null, "MaxAgeSeconds"));
          writer.add(factory.createCharacters(String.valueOf(rule.maxAgeSeconds)));
          writer.add(factory.createEndElement(null, null, "MaxAgeSeconds"));
        }

        addCollection(factory, writer, rule.exposeHeaders, "ExposeHeader");

        writer.add(factory.createEndElement(null, null, "CORSRule"));
      }
      writer.add(factory.createEndElement(null, null, "CORSConfiguration"));
      writer.add(factory.createEndDocument());
      writer.flush();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("Cannot serialize");
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (XMLStreamException e) {
          LOG.log(Level.WARNING, "Failed to close xml writer", e);
        }
      }
    }
    return sw.toString();
  }

  private void addCollection(XMLEventFactory factory, XMLEventWriter writer,
                             Collection<String> collection, String elementName)
      throws XMLStreamException {
    for (String allowedOrigin : collection) {
      writer.add(factory.createStartElement(null, null, elementName));
      writer.add(factory.createCharacters(allowedOrigin));
      writer.add(factory.createEndElement(null, null, elementName));
    }
  }


  protected JsonStringWriter writeJSON(JsonStringWriter writer) {
    JsonStringWriter configurationJson = writer.array("CORSConfiguration");
    for (Rule rule : this.rules) {
      JsonStringWriter ruleJson = configurationJson.object("CORSRule");
      if (rule.id != null) {
        ruleJson.value("ID", rule.id);
      }
      ruleJson.array("AllowedOrigin", rule.allowedOrigins);
      ruleJson.array("AllowedMethod", rule.allowedMethods.stream().map(HttpMethod::name)
          .collect(Collectors.toSet()));
      if (rule.maxAgeSeconds > Integer.MIN_VALUE) {
        ruleJson.value("MaxAgeSeconds", rule.maxAgeSeconds);
      }
      ruleJson.array("ExposeHeader", rule.exposeHeaders);
      configurationJson = ruleJson.end();
    }
    return configurationJson.end();
  }

  private static <T> Collection<T> atLeastOne(Collection<T> collection, String name) {
    if (collection == null || collection.isEmpty()) {
      throw new IllegalArgumentException("At least one " + name + " must be specified");
    }
    return collection;
  }

  @ParametersAreNonnullByDefault
  public static class Rule {

    @Nullable
    final String id;
    final Collection<String> allowedOrigins;
    final Collection<HttpMethod> allowedMethods;
    final int maxAgeSeconds;
    @Nullable
    final Collection<String> exposeHeaders;

    public Rule(String id, Collection<String> allowedOrigins, Collection<HttpMethod> allowedMethods,
                int maxAgeSeconds, @Nullable Collection<String> exposeHeaders) {
      this.id = id;
      this.allowedOrigins = atLeastOne(allowedOrigins, "allowed origin");
      this.allowedMethods = atLeastOne(allowedMethods, "allowed method");
      this.maxAgeSeconds = maxAgeSeconds;
      this.exposeHeaders = exposeHeaders == null || exposeHeaders.isEmpty() ? null : exposeHeaders;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Rule rule = (Rule) o;
      return maxAgeSeconds == rule.maxAgeSeconds &&
             Objects.equals(id, rule.id) &&
             Objects.equals(allowedOrigins, rule.allowedOrigins) &&
             Objects.equals(allowedMethods, rule.allowedMethods) &&
             Objects.equals(exposeHeaders, rule.exposeHeaders);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, allowedOrigins, allowedMethods, maxAgeSeconds, exposeHeaders);
    }
  }
}
