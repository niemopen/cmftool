/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
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
package org.mitre.niem.cmf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.niem.xml.XMLDocument.makeURI;

import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

public class MappingTest {

    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

    @Test
    void writeThenReadRoundTrip() throws Exception {
        var map = new Mapping();
        map.assignPrefix("nc", "http://example.com/nc#");
        map.assignPrefix("msg", "http://example.com/msg#");

        map.addMapping("nc:PersonName", "msg:name");
        map.addMapping("nc:PersonAge", "msg:age");

        var sw = new StringWriter();
        map.write(sw);

        var map2 = Mapping.read(new StringReader(sw.toString()));

        assertTrue(map2.isMappedQ("nc:PersonName"));
        assertTrue(map2.isMappedQ("nc:PersonAge"));

        assertEquals("msg:name", map2.qnToMappedQ("nc:PersonName"));
        assertEquals("msg:age", map2.qnToMappedQ("nc:PersonAge"));

        assertEquals("nc:PersonName", map2.mappedQNtoQ("msg:name"));
        assertEquals("nc:PersonAge", map2.mappedQNtoQ("msg:age"));

        assertEquals(
            makeURI("http://example.com/msg#", "name"),
            map2.uriToMappedU(makeURI("http://example.com/nc#", "PersonName"))
        );
        assertEquals(
            makeURI("http://example.com/nc#", "PersonName"),
            map2.mappedURItoU(makeURI("http://example.com/msg#", "name"))
        );
    }

    @Test
    void addMappingIgnoresXsdSourceQName() throws Exception {
        var map = new Mapping();
        map.assignPrefix("xs", XSD_NS);
        map.assignPrefix("msg", "http://example.com/msg#");

        map.addMapping("xs:string", "msg:stringType");

        assertFalse(map.isMappedQ("xs:string"));
        assertEquals("xs:string", map.qnToMappedQ("xs:string"));
        assertNull(map.mappedQNtoQ("msg:stringType"));
        assertEquals(makeURI(XSD_NS, "string"), map.uriToMappedU(makeURI(XSD_NS, "string")));
        assertNull(map.mappedURItoU(makeURI("http://example.com/msg#", "stringType")));

        var sw = new StringWriter();
        map.write(sw);
        assertFalse(sw.toString().contains("xs:string"));
        assertFalse(sw.toString().contains("msg:stringType"));
    }

    @Test
    void readIgnoresXsdSourceMappings() throws Exception {
        var text = """
            PREFIX xs   http://www.w3.org/2001/XMLSchema
            PREFIX nc   http://example.com/nc#
            PREFIX msg  http://example.com/msg#

            xs:string      msg:stringType
            nc:PersonName  msg:name
            """;

        var map = Mapping.read(new StringReader(text));

        assertFalse(map.isMappedQ("xs:string"));
        assertEquals("xs:string", map.qnToMappedQ("xs:string"));
        assertNull(map.mappedQNtoQ("msg:stringType"));

        assertTrue(map.isMappedQ("nc:PersonName"));
        assertEquals("msg:name", map.qnToMappedQ("nc:PersonName"));
        assertEquals("nc:PersonName", map.mappedQNtoQ("msg:name"));
    }

    @Test
    void readAllowsInlineComments() throws Exception {
        var text = """
            # file comment
            PREFIX nc   http://example.com/nc#   # source namespace
            PREFIX msg  http://example.com/msg#  # target namespace

            nc:PersonName  msg:name   # mapping comment
            nc:PersonAge   msg:age    # another mapping comment
            """;

        var map = Mapping.read(new StringReader(text));

        assertEquals("msg:name", map.qnToMappedQ("nc:PersonName"));
        assertEquals("msg:age", map.qnToMappedQ("nc:PersonAge"));
        assertEquals("nc:PersonName", map.mappedQNtoQ("msg:name"));
        assertEquals("nc:PersonAge", map.mappedQNtoQ("msg:age"));
    }

    @Test
    void readRejectsMappingWithUndeclaredSourcePrefix() {
        var text = """
            PREFIX msg  http://example.com/msg#
            nc:PersonName  msg:name
            """;

        var ex = assertThrows(
            CMFException.class,
            () -> Mapping.read(new StringReader(text))
        );

        assertTrue(ex.getMessage().contains("Line 2"));
        assertTrue(ex.getMessage().contains("Undeclared source prefix"));
    }

    @Test
    void readRejectsMappingWithUndeclaredTargetPrefix() {
        var text = """
            PREFIX nc  http://example.com/nc#
            nc:PersonName  msg:name
            """;

        var ex = assertThrows(
            CMFException.class,
            () -> Mapping.read(new StringReader(text))
        );

        assertTrue(ex.getMessage().contains("Line 2"));
        assertTrue(ex.getMessage().contains("Undeclared target prefix"));
    }

    @Test
    void readRejectsConflictingPrefixDeclarations() {
        var text = """
            PREFIX nc  http://example.com/nc#
            PREFIX nc  http://example.com/other#
            """;

        var ex = assertThrows(
            CMFException.class,
            () -> Mapping.read(new StringReader(text))
        );

        assertTrue(ex.getMessage().contains("Line 2"));
        assertTrue(ex.getMessage().contains("prefix already assigned"));
    }

    @Test
    void readRejectsConflictingMappings() {
        var text = """
            PREFIX nc   http://example.com/nc#
            PREFIX msg  http://example.com/msg#
            nc:PersonName  msg:name
            nc:PersonName  msg:fullName
            """;

        var ex = assertThrows(
            CMFException.class,
            () -> Mapping.read(new StringReader(text))
        );

        assertTrue(ex.getMessage().contains("Line 4"));
        assertTrue(ex.getMessage().contains("already mapped"));
    }

    @Test
    void readIgnoresBomBlankLinesAndComments() throws Exception {
        var text = "\uFEFF# leading bom comment\n"
            + "\n"
            + "PREFIX nc   http://example.com/nc#\n"
            + "PREFIX msg  http://example.com/msg#\n"
            + "\n"
            + "# heading\n"
            + "nc:PersonName  msg:name\n";

        var map = Mapping.read(new StringReader(text));

        assertEquals("msg:name", map.qnToMappedQ("nc:PersonName"));
        assertEquals("nc:PersonName", map.mappedQNtoQ("msg:name"));
    }

    @Test
    void unmappedLookupsBehaveAsDocumented() throws Exception {
        var map = new Mapping();
        map.assignPrefix("nc", "http://example.com/nc#");
        map.assignPrefix("msg", "http://example.com/msg#");
        map.addMapping("nc:PersonName", "msg:name");

        assertEquals("nc:NoMapping", map.qnToMappedQ("nc:NoMapping"));
        assertEquals("http://example.com/other#Thing", map.uriToMappedU("http://example.com/other#Thing"));
        assertNull(map.mappedQNtoQ("msg:nope"));
        assertNull(map.mappedURItoU("http://example.com/msg#nope"));
    }
}
