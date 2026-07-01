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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.niem.xml.XMLDocument.makeURI;

class MappingTest {

    private static final String SRC_PREFIX = "src";
    private static final String SRC_NS = "http://example.com/src/";
    private static final String TGT_PREFIX = "tgt";
    private static final String TGT_NS = "http://example.com/tgt/";

    @Test
    void assignPrefixStoresAssignment() throws Exception {
        var map = new Mapping();

        map.assignPrefix(SRC_PREFIX, SRC_NS);

        assertEquals(SRC_NS, map.prefixToURI(SRC_PREFIX));
        assertNull(map.prefixToURI("missing"));
    }

    @Test
    void assignPrefixRejectsDifferentPrefixForSameUri() throws Exception {
        var map = new Mapping();
        map.assignPrefix("a", SRC_NS);

        var ex = assertThrows(CMFException.class, () -> map.assignPrefix("b", SRC_NS));
        assertTrue(ex.getMessage().contains("uri already mapped"));
    }

    @Test
    void assignPrefixRejectsDifferentUriForSamePrefix() throws Exception {
        var map = new Mapping();
        map.assignPrefix("a", "http://example.com/one/");

        var ex = assertThrows(CMFException.class, () -> map.assignPrefix("a", "http://example.com/two/"));
        assertTrue(ex.getMessage().contains("prefix already assigned"));
    }

    @Test
    void addMappingPopulatesAllLookupMethods() throws Exception {
        var map = basicMap();

        map.addMapping("src:PersonName", "tgt:name");

        var fromU = makeURI(SRC_NS, "PersonName");
        var toU = makeURI(TGT_NS, "name");

        assertTrue(map.isMappedQ("src:PersonName"));
        assertTrue(map.isMappedU(fromU));

        assertEquals("tgt:name", map.qnToTargetQN("src:PersonName"));
        assertEquals("tgt:name", map.uriToTargetQN(fromU));
        assertEquals(TGT_NS, map.uriToTargetNSU(fromU));
        assertEquals(toU, map.uriToTargetURI(fromU));

        assertEquals("src:PersonName", map.targetQtoSourceQN("tgt:name"));
        assertEquals(fromU, map.targetUToSourceURI(toU));

        assertFalse(map.isMappedQ("src:Other"));
        assertNull(map.qnToTargetQN("src:Other"));
        assertNull(map.uriToTargetQN(makeURI(SRC_NS, "Other")));
        assertNull(map.uriToTargetNSU(makeURI(SRC_NS, "Other")));
        assertNull(map.uriToTargetURI(makeURI(SRC_NS, "Other")));
        assertNull(map.targetQtoSourceQN("tgt:other"));
        assertNull(map.targetUToSourceURI(makeURI(TGT_NS, "other")));
    }

    @Test
    void addMappingIgnoresXsdSourceNamespace() throws Exception {
        var map = new Mapping();
        map.assignPrefix("xs", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        map.assignPrefix(TGT_PREFIX, TGT_NS);

        map.addMapping("xs:string", "tgt:string");

        var xsdStringU = makeURI(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string");
        var tgtStringU = makeURI(TGT_NS, "string");

        assertFalse(map.isMappedQ("xs:string"));
        assertFalse(map.isMappedU(xsdStringU));
        assertNull(map.qnToTargetQN("xs:string"));
        assertNull(map.uriToTargetQN(xsdStringU));
        assertNull(map.targetQtoSourceQN("tgt:string"));
        assertNull(map.targetUToSourceURI(tgtStringU));
    }

    @Test
    void addMappingRejectsInvalidSourceQName() throws Exception {
        var map = basicMap();

        var ex = assertThrows(CMFException.class, () -> map.addMapping("not-a-qname", "tgt:name"));
        assertTrue(ex.getMessage().contains("Invalid source QName"));
    }

    @Test
    void addMappingRejectsInvalidTargetQName() throws Exception {
        var map = basicMap();

        var ex = assertThrows(CMFException.class, () -> map.addMapping("src:PersonName", "not-a-qname"));
        assertTrue(ex.getMessage().contains("Invalid source QName"));
    }

    @Test
    void addMappingRejectsUndeclaredSourcePrefix() throws Exception {
        var map = new Mapping();
        map.assignPrefix(TGT_PREFIX, TGT_NS);

        var ex = assertThrows(CMFException.class, () -> map.addMapping("src:PersonName", "tgt:name"));
        assertTrue(ex.getMessage().contains("Undeclared source prefix"));
    }

    @Test
    void addMappingRejectsUndeclaredTargetPrefix() throws Exception {
        var map = new Mapping();
        map.assignPrefix(SRC_PREFIX, SRC_NS);

        assertThrows(CMFException.class, () -> map.addMapping("src:PersonName", "tgt:name"));
    }

    @Test
    void addMappingRejectsConflictingSourceRemap() throws Exception {
        var map = basicMap();
        map.addMapping("src:PersonName", "tgt:name");

        var ex = assertThrows(CMFException.class, () -> map.addMapping("src:PersonName", "tgt:fullName"));
        assertTrue(ex.getMessage().contains("already mapped"));
    }

    @Test
    void addMappingRejectsConflictingTargetReuse() throws Exception {
        var map = basicMap();
        map.addMapping("src:PersonName", "tgt:name");

        var ex = assertThrows(CMFException.class, () -> map.addMapping("src:GivenName", "tgt:name"));
        assertTrue(ex.getMessage().contains("already mapped"));
    }

    @Test
    void writeOutputsPrefixesAndMappingsButOmitsXsdPrefix() throws Exception {
        var map = new Mapping();
        map.assignPrefix("xs", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        map.assignPrefix(SRC_PREFIX, SRC_NS);
        map.assignPrefix(TGT_PREFIX, TGT_NS);

        map.addMapping("src:Alpha", "tgt:alpha");
        map.addMapping("src:BetaType", "tgt:betaType");

        var out = new StringWriter();
        map.write(out);
        var text = out.toString();

        assertTrue(text.contains("PREFIX src"));
        assertTrue(text.contains("PREFIX tgt"));
        assertFalse(text.contains("PREFIX xs"));

        assertTrue(text.contains("# FromQName"));
        assertTrue(text.contains("src:Alpha"));
        assertTrue(text.contains("tgt:alpha"));
        assertTrue(text.contains("src:BetaType"));
        assertTrue(text.contains("tgt:betaType"));
    }

    @Test
    void readParsesPrefixesMappingsCommentsAndBom() throws Exception {
        var text =
            "\uFEFFPREFIX src " + SRC_NS + "\n" +
            "PREFIX tgt " + TGT_NS + "   # target namespace\n" +
            "\n" +
            "# mapping lines\n" +
            "src:PersonName    tgt:name\n";

        var map = Mapping.read(new StringReader(text));

        var fromU = makeURI(SRC_NS, "PersonName");
        var toU = makeURI(TGT_NS, "name");

        assertEquals(SRC_NS, map.prefixToURI("src"));
        assertEquals(TGT_NS, map.prefixToURI("tgt"));
        assertEquals("tgt:name", map.qnToTargetQN("src:PersonName"));
        assertEquals("src:PersonName", map.targetQtoSourceQN("tgt:name"));
        assertEquals("tgt:name", map.uriToTargetQN(fromU));
        assertEquals(fromU, map.targetUToSourceURI(toU));
    }

    @Test
    void readRejectsInvalidSyntaxWithLineNumber() {
        var text =
            "PREFIX src " + SRC_NS + "\n" +
            "this is not valid\n";

        var ex = assertThrows(CMFException.class, () -> Mapping.read(new StringReader(text)));
        assertTrue(ex.getMessage().contains("Line 2"));
        assertTrue(ex.getMessage().contains("invalid mapping syntax"));
    }

    @Test
    void readWrapsPrefixAssignmentErrorWithLineNumber() {
        var text =
            "PREFIX a http://example.com/one/\n" +
            "PREFIX b http://example.com/one/\n";

        var ex = assertThrows(CMFException.class, () -> Mapping.read(new StringReader(text)));
        assertTrue(ex.getMessage().contains("Line 2"));
    }

    @Test
    void readWrapsMappingErrorWithLineNumber() {
        var text =
            "PREFIX src " + SRC_NS + "\n" +
            "src:PersonName tgt:name\n";

        var ex = assertThrows(CMFException.class, () -> Mapping.read(new StringReader(text)));
        assertTrue(ex.getMessage().contains("Line 2"));
    }

    @Test
    void writeAndReadRoundTripPreservesMappings() throws Exception {
        var original = basicMap();
        original.addMapping("src:PersonName", "tgt:name");
        original.addMapping("src:GivenName", "tgt:givenName");

        var out = new StringWriter();
        original.write(out);

        var roundTripped = Mapping.read(new StringReader(out.toString()));

        var personNameU = makeURI(SRC_NS, "PersonName");
        var givenNameU = makeURI(SRC_NS, "GivenName");

        assertEquals(SRC_NS, roundTripped.prefixToURI("src"));
        assertEquals(TGT_NS, roundTripped.prefixToURI("tgt"));

        assertEquals("tgt:name", roundTripped.qnToTargetQN("src:PersonName"));
        assertEquals("tgt:givenName", roundTripped.qnToTargetQN("src:GivenName"));

        assertEquals("tgt:name", roundTripped.uriToTargetQN(personNameU));
        assertEquals("tgt:givenName", roundTripped.uriToTargetQN(givenNameU));
    }

    @Test
    void readFileReadsMappingFile(@TempDir Path tempDir) throws IOException, CMFException {
        var file = tempDir.resolve("mapping.txt");
        var text =
            "PREFIX src " + SRC_NS + "\n" +
            "PREFIX tgt " + TGT_NS + "\n" +
            "src:PersonName tgt:name\n";

        Files.writeString(file, text);

        var map = Mapping.readFile(file.toFile());

        assertEquals(SRC_NS, map.prefixToURI("src"));
        assertEquals(TGT_NS, map.prefixToURI("tgt"));
        assertEquals("tgt:name", map.qnToTargetQN("src:PersonName"));
    }

    private static Mapping basicMap() throws CMFException {
        var map = new Mapping();
        map.assignPrefix(SRC_PREFIX, SRC_NS);
        map.assignPrefix(TGT_PREFIX, TGT_NS);
        return map;
    }
}



