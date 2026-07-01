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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;

import static org.junit.jupiter.api.Assertions.*;

class ContextTest {

    private static final String NC_NS = "http://example.org/nc/";
    private static final String J_NS = "http://example.org/j/";
    private static final String MSG_NS = "http://example.org/msg/";

    @Test
    void readerConstructorReadsWrappedContext() throws Exception {
        var json = """
            {
              "@context": {
                "nc": "http://example.org/nc/",
                "msg:name": "nc:PersonName"
              }
            }
            """;

        var ctx = new Context(new StringReader(json));
        JsonObject obj = ctx.jsonObject();

        assertEquals(NC_NS, obj.get("nc").getAsString());
        assertEquals("nc:PersonName", obj.get("msg:name").getAsString());
    }

    @Test
    void readerConstructorReadsBareContextObject() throws Exception {
        var json = """
            {
              "nc": "http://example.org/nc/",
              "msg:name": "nc:PersonName"
            }
            """;

        var ctx = new Context(new StringReader(json));
        JsonObject obj = ctx.jsonObject();

        assertEquals(NC_NS, obj.get("nc").getAsString());
        assertEquals("nc:PersonName", obj.get("msg:name").getAsString());
    }

    @Test
    void readerConstructorRejectsInvalidJson() {
        var ex = assertThrows(CMFException.class, () -> new Context(new StringReader("not json")));
        assertTrue(ex.getMessage().startsWith("can't read context:"));
    }

    @Test
    void modelConstructorAddsNamespacePrefixesWithoutMappedTerms() throws Exception {
        var model = new Model();
        var nc = addNamespace(model, "nc", NC_NS);

        var personName = new Property(nc, "PersonName");
        model.addProperty(personName);

        var ctx = new Context(model);
        JsonObject obj = ctx.jsonObject();

        assertEquals(NC_NS, obj.get("nc").getAsString());
        assertFalse(obj.has("nc:PersonName"));
        assertFalse(obj.has("xs"));
    }

    @Test
    void modelConstructorWithMappingAddsMappedTermDefinition() throws Exception {
        var model = new Model();
        var nc = addNamespace(model, "nc", NC_NS);

        var personName = new Property(nc, "PersonName");
        model.addProperty(personName);

        var map = new Mapping();
        map.assignPrefix("nc", NC_NS);
        map.assignPrefix("msg", MSG_NS);
        map.addMapping(personName.qname(), "msg:name");

        var ctx = new Context(model, map);
        JsonObject obj = ctx.jsonObject();

        assertEquals(NC_NS, obj.get("nc").getAsString());
        assertEquals(MSG_NS, obj.get("msg").getAsString());
        assertEquals("nc:PersonName", obj.get("msg:name").getAsString());
    }

    @Test
    void orderedPropertyCreatesListContainerEntries() throws Exception {
        var model = new Model();
        var nc = addNamespace(model, "nc", NC_NS);

        var aliases = new Property(nc, "PersonAlias");
        aliases.setIsOrdered(true);
        model.addProperty(aliases);

        var map = new Mapping();
        map.assignPrefix("nc", NC_NS);
        map.assignPrefix("msg", MSG_NS);
        map.addMapping(aliases.qname(), "msg:aliases");

        var ctx = new Context(model, map);
        JsonObject obj = ctx.jsonObject();

        assertTrue(obj.has("msg:aliases"));
        JsonObject mapped = obj.getAsJsonObject("msg:aliases");
        assertEquals(aliases.uri(), mapped.get("@id").getAsString());
        assertEquals("@list", mapped.get("@container").getAsString());

        assertTrue(obj.has(aliases.uri()));
        JsonObject byUri = obj.getAsJsonObject(aliases.uri());
        assertEquals("@list", byUri.get("@container").getAsString());
    }

    @Test
    void writeWrapsContextInAtContextObject() throws Exception {
        var ctx = new Context(new StringReader("""
            {
              "nc": "http://example.org/nc/"
            }
            """));

        var out = new StringWriter();
        ctx.write(out);

        JsonObject written = JsonParser.parseString(out.toString()).getAsJsonObject();
        assertTrue(written.has("@context"));
        assertEquals(
            NC_NS,
            written.getAsJsonObject("@context").get("nc").getAsString()
        );
    }

    @Test
    void selectedMessageRootsLimitIncludedComponents() throws Exception {
        var model = new Model();
        var nc = addNamespace(model, "nc", NC_NS);
        var j = addNamespace(model, "j", J_NS);

        var root = new ObjectProperty(nc, "RootMessage");
        model.addProperty(root);

        var extra = new Property(j, "OtherThing");
        model.addProperty(extra);

        var ctx = new Context(model, null, Set.of(root));
        JsonObject obj = ctx.jsonObject();

        assertTrue(obj.has("nc"));
        assertFalse(obj.has("j"));
    }

    @Test
    void noPrefixContextUsesMappedLocalNameAsTerm() throws Exception {
        var model = new Model();
        var nc = addNamespace(model, "nc", NC_NS);

        var personName = new Property(nc, "PersonName");
        model.addProperty(personName);

        var map = new Mapping();
        map.assignPrefix("nc", NC_NS);
        map.assignPrefix("msg", MSG_NS);
        map.addMapping(personName.qname(), "msg:name");

        var ctx = new Context(model, map, null, true);
        JsonObject obj = ctx.jsonObject();

        assertEquals(NC_NS, obj.get("nc").getAsString());
        assertEquals(MSG_NS, obj.get("msg").getAsString());
        assertEquals("nc:PersonName", obj.get("name").getAsString());
        assertFalse(obj.has("msg:name"));
    }

    @Test
    void noPrefixContextRejectsDuplicateLocalNames() throws Exception {
        var model = new Model();
        var nc = addNamespace(model, "nc", NC_NS);
        var j = addNamespace(model, "j", J_NS);

        model.addProperty(new Property(nc, "Name"));
        model.addProperty(new Property(j, "Name"));

        var ex = assertThrows(CMFException.class, () -> new Context(model, null, null, true));
        assertTrue(ex.getMessage().contains("have same local name"));
    }
    @Test
    void expandExpandsMappedTermToFullIri() throws Exception {
        var ctx = new Context(new StringReader("""
        {
          "@context": {
            "nc": "http://example.org/nc/",
            "name": "nc:PersonName"
          }
        }
        """));

        assertEquals("http://example.org/nc/PersonName", ctx.expand("name"));
    }

    @Test
    void expandExpandsCompactIriToFullIri() throws Exception {
        var ctx = new Context(new StringReader("""
        {
          "@context": {
            "nc": "http://example.org/nc/"
          }
        }
        """));

        assertEquals("http://example.org/nc/PersonName", ctx.expand("nc:PersonName"));
    }

    @Test
    void expandExpandsObjectTermDefinitionUsingAtId() throws Exception {
        var ctx = new Context(new StringReader("""
        {
          "@context": {
            "nc": "http://example.org/nc/",
            "aliases": {
              "@id": "nc:PersonAlias",
              "@container": "@list"
            }
          }
        }
        """));

        assertEquals("http://example.org/nc/PersonAlias", ctx.expand("aliases"));
    }

    @Test
    void expandReturnsAbsoluteIriUnchangedWhenNotInContext() throws Exception {
        var ctx = new Context(new StringReader("""
        {
          "@context": {
            "nc": "http://example.org/nc/"
          }
        }
        """));

        var iri = "http://other.example.org/test/Thing";
        assertEquals(iri, ctx.expand(iri));
    }

    @Test
    void expandReturnsUnknownTermUnchangedWhenNoMappingExists() throws Exception {
        var ctx = new Context(new StringReader("""
        {
          "@context": {
            "nc": "http://example.org/nc/"
          }
        }
        """));

        assertEquals("unknownTerm", ctx.expand("unknownTerm"));
    }

    @Test
    void expandRejectsNullArgument() throws Exception {
        var ctx = new Context(new StringReader("""
        {
          "@context": {
            "nc": "http://example.org/nc/"
          }
        }
        """));

        var ex = assertThrows(NullPointerException.class, () -> ctx.expand(null));
        assertEquals("termOrCompactIRI must not be null", ex.getMessage());
    }

    private Namespace addNamespace(Model model, String prefix, String uri) throws CMFException {
        var ns = new Namespace(prefix, uri);
        model.addNamespace(ns);
        return ns;
    }
}
