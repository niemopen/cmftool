/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2025 The MITRE Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.niem.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContextTest {

    @Test
    void create_allComponents_noMapping_includesNamespaceDeclarationsOnlyWhenNoAliasNeeded() throws Exception {
        Property prop = property("foo", "urn:foo", "thing", false);
        FakeModel model = new FakeModel(setOf(prop), setOf(prop));

        JsonObject cxt = Context.create(model, new Mapping());

        assertNotNull(cxt);
        assertEquals("urn:foo", cxt.get("foo").getAsString());
        assertEquals(1, cxt.size());
    }

    @Test
    void create_withMappedProperty_addsMappedTermAndMappedNamespace() throws Exception {
        Property prop = property("foo", "urn:foo", "thing", false);
        FakeModel model = new FakeModel(setOf(prop), setOf(prop));
        StubMapping mapping = new StubMapping()
            .map("foo:thing", "bar:stuff")
            .prefix("bar", "urn:bar");

        JsonObject cxt = Context.create(model, mapping);

        assertEquals("urn:foo", cxt.get("foo").getAsString());
        assertEquals("urn:bar", cxt.get("bar").getAsString());
        assertEquals("foo:thing", cxt.get("bar:stuff").getAsString());
    }

    @Test
    void create_withNoPrefix_usesMappedLocalName() throws Exception {
        Property prop = property("foo", "urn:foo", "thing", false);
        FakeModel model = new FakeModel(setOf(prop), setOf(prop));
        StubMapping mapping = new StubMapping()
            .map("foo:thing", "bar:stuff")
            .prefix("bar", "urn:bar");

        JsonObject cxt = Context.create(model, mapping, null, true);

        assertEquals("urn:foo", cxt.get("foo").getAsString());
        assertEquals("urn:bar", cxt.get("bar").getAsString());
        assertEquals("foo:thing", cxt.get("stuff").getAsString());
    }

    @Test
    void create_withOrderedMappedProperty_writesListContainerForMappedTermAndOriginalQName() throws Exception {
        Property prop = property("foo", "urn:foo", "thing", true);
        FakeModel model = new FakeModel(setOf(prop), setOf(prop));
        StubMapping mapping = new StubMapping()
            .map("foo:thing", "bar:stuff")
            .prefix("bar", "urn:bar");

        JsonObject cxt = Context.create(model, mapping);

        JsonObject mapped = cxt.getAsJsonObject("bar:stuff");
        JsonObject original = cxt.getAsJsonObject("foo:thing");

        assertNotNull(mapped);
        assertEquals("foo:thing", mapped.get("@id").getAsString());
        assertEquals("@list", mapped.get("@container").getAsString());

        assertNotNull(original);
        assertEquals("@list", original.get("@container").getAsString());
    }

    @Test
    void create_withOrderedMappedPropertyAndNoPrefix_writesLocalNameListTermAndOriginalQName() throws Exception {
        Property prop = property("foo", "urn:foo", "thing", true);
        FakeModel model = new FakeModel(setOf(prop), setOf(prop));
        StubMapping mapping = new StubMapping()
            .map("foo:thing", "bar:stuff")
            .prefix("bar", "urn:bar");

        JsonObject cxt = Context.create(model, mapping, null, true);

        JsonObject local = cxt.getAsJsonObject("stuff");
        JsonObject original = cxt.getAsJsonObject("foo:thing");

        assertNotNull(local);
        assertEquals("foo:thing", local.get("@id").getAsString());
        assertEquals("@list", local.get("@container").getAsString());

        assertNotNull(original);
        assertEquals("@list", original.get("@container").getAsString());
    }

    @Test
    void create_singleMessage_usesMessageComponents() throws Exception {
        Property prop = property("foo", "urn:foo", "thing", false);
        FakeModel model = new FakeModel(Set.of(), setOf(prop));
        ObjectProperty msg = objectProperty("msg", "urn:msg", "Message", false);

        JsonObject cxt = Context.create(model, new Mapping(), msg);

        assertEquals("urn:foo", cxt.get("foo").getAsString());
        assertEquals(1, cxt.size());
    }

    @Test
    void create_noPrefix_duplicateLocalNames_throwsCMFException() {
        Property p1 = property("foo", "urn:foo", "thing", false);
        Property p2 = property("baz", "urn:baz", "thing", false);
        FakeModel model = new FakeModel(setOf(p1, p2), setOf(p1, p2));

        assertThrows(CMFException.class, () -> Context.create(model, new Mapping(), null, true));
    }

    @Test
    void create_noPrefix_localNameMatchingNamespacePrefix_throwsCMFException() {
        Property p1 = property("foo", "urn:foo", "bar", false);
        Property p2 = property("bar", "urn:bar", "item", false);
        FakeModel model = new FakeModel(setOf(p1, p2), setOf(p1, p2));

        assertThrows(CMFException.class, () -> Context.create(model, new Mapping(), null, true));
    }

    @Test
    void create_missingMappedPrefixUri_throwsCMFException() {
        Property prop = property("foo", "urn:foo", "thing", false);
        FakeModel model = new FakeModel(setOf(prop), setOf(prop));
        StubMapping mapping = new StubMapping()
            .map("foo:thing", "bar:stuff");

        assertThrows(CMFException.class, () -> Context.create(model, mapping));
    }

    @Test
    void create_conflictingNamespaceBindings_throwsCMFException() {
        Property p1 = property("dup", "urn:one", "item1", false);
        Property p2 = property("dup", "urn:two", "item2", false);
        FakeModel model = new FakeModel(setOf(p1, p2), setOf(p1, p2));

        assertThrows(CMFException.class, () -> Context.create(model, new Mapping()));
    }

    @Test
    void create_conflictingGeneratedTerms_throwsCMFException() {
        Property p1 = property("foo", "urn:foo", "one", false);
        Property p2 = property("bar", "urn:bar", "two", false);
        FakeModel model = new FakeModel(setOf(p1, p2), setOf(p1, p2));
        StubMapping mapping = new StubMapping()
            .map("foo:one", "baz:same")
            .map("bar:two", "baz:same")
            .prefix("baz", "urn:baz");

        assertThrows(CMFException.class, () -> Context.create(model, mapping));
    }

    @Test
    void create_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Context.create(null));
    }

    @Test
    void create_nullMessage_throwsNullPointerException() {
        FakeModel model = new FakeModel(Set.of(), Set.of());
        assertThrows(NullPointerException.class, () -> Context.create(model, new Mapping(), (ObjectProperty) null));
    }

    @Test
    void write_wrapsContextInAtContext() throws Exception {
        JsonObject res = new JsonObject();
        res.addProperty("foo", "urn:foo");
        res.addProperty("bar:stuff", "foo:thing");

        StringWriter out = new StringWriter();
        Context.write(res, out);

        JsonObject root = JsonParser.parseString(out.toString()).getAsJsonObject();
        JsonObject cxt = root.getAsJsonObject("@context");

        assertNotNull(cxt);
        assertEquals("urn:foo", cxt.get("foo").getAsString());
        assertEquals("foo:thing", cxt.get("bar:stuff").getAsString());
    }

    private static Property property(String prefix, String uri, String localName, boolean ordered) {
        Property p = new Property(new Namespace(prefix, uri), localName);
        p.setIsOrdered(ordered);
        return p;
    }

    private static ObjectProperty objectProperty(String prefix, String uri, String localName, boolean ordered) {
        ObjectProperty p = new ObjectProperty(new Namespace(prefix, uri), localName);
        p.setIsOrdered(ordered);
        return p;
    }

    @SafeVarargs
    private static <T> Set<T> setOf(T... values) {
        Set<T> s = new LinkedHashSet<>();
        for (T v : values) s.add(v);
        return s;
    }

    private static final class FakeModel extends Model {
        private final Set<Component> componentS;
        private final Set<Component> messageComponentS;

        FakeModel(Set<Component> componentS, Set<Component> messageComponentS) {
            this.componentS = new LinkedHashSet<>(componentS);
            this.messageComponentS = new LinkedHashSet<>(messageComponentS);
        }

        @Override
        public Set<Component> componentSet() {
            return new LinkedHashSet<>(componentS);
        }

        @Override
        public Set<Component> messageComponents(Set<ObjectProperty> msgPropS) {
            return new LinkedHashSet<>(messageComponentS);
        }
    }

    private static final class StubMapping extends Mapping {
        private final Map<String, String> qnMap = new HashMap<>();
        private final Map<String, String> preMap = new HashMap<>();

        StubMapping map(String fromQ, String toQ) {
            qnMap.put(fromQ, toQ);
            return this;
        }

        StubMapping prefix(String prefix, String uri) {
            preMap.put(prefix, uri);
            return this;
        }

        @Override
        public String qnToMappedQ(String fromQ) {
            return qnMap.getOrDefault(fromQ, fromQ);
        }

        @Override
        public String prefixToURI(String prefix) {
            return preMap.get(prefix);
        }
    }
}
