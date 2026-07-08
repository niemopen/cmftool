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

import static javax.xml.XMLConstants.NULL_NS_URI;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.niem.xml.XMLDocument.makeURI;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MappingTest {

    private static final String NC_NS = "http://example.com/nc/";
    private static final String SX_NS = "http://example.com/simple/";
    private static final String ALT_NS = "http://example.com/alt/";

    private static Mapping newMapping() throws CMFException {
        return new Mapping();
    }

    private static Mapping newMappingWithNcAndSx() throws CMFException {
        var map = new Mapping();
        map.assignPrefix("nc", NC_NS);
        map.assignPrefix("sx", SX_NS);
        return map;
    }

    private static String ncUri(String localName) {
        return makeURI(NC_NS, localName);
    }

    @Test
    void assignPrefixAllowsConsistentReassignment() throws Exception {
        var map = newMapping();

        assertDoesNotThrow(() -> map.assignPrefix("nc", NC_NS));
        assertDoesNotThrow(() -> map.assignPrefix("nc", NC_NS));
    }

    @Test
    void assignPrefixRejectsNullOrBlankValues() throws Exception {
        var map = newMapping();

        var ex1 = assertThrows(CMFException.class, () -> map.assignPrefix(null, NC_NS));
        assertTrue(ex1.getMessage().contains("blank or null prefix"));

        var ex2 = assertThrows(CMFException.class, () -> map.assignPrefix(" ", NC_NS));
        assertTrue(ex2.getMessage().contains("blank or null prefix"));

        var ex3 = assertThrows(CMFException.class, () -> map.assignPrefix("nc", null));
        assertTrue(ex3.getMessage().contains("blank or null URI"));

        var ex4 = assertThrows(CMFException.class, () -> map.assignPrefix("nc", " "));
        assertTrue(ex4.getMessage().contains("blank or null URI"));
    }

    @Test
    void assignPrefixRejectsConflictingAssignments() throws Exception {
        var map = newMapping();
        map.assignPrefix("nc", NC_NS);

        var ex1 = assertThrows(CMFException.class, () -> map.assignPrefix("nc", ALT_NS));
        assertTrue(ex1.getMessage().contains("prefix already assigned"));

        var map2 = newMapping();
        map2.assignPrefix("nc", NC_NS);

        var ex2 = assertThrows(CMFException.class, () -> map2.assignPrefix("alt", NC_NS));
        assertTrue(ex2.getMessage().contains("uri already mapped"));
    }

    @Test
    void addMappingWithPrefixedTargetStoresExpectedRecord() throws Exception {
        var map = newMappingWithNcAndSx();

        map.addMapping("nc:PersonSurName", "sx:lname");

        var rec = map.uriToMapRec(ncUri("PersonSurName"));
        assertNotNull(rec);
        assertEquals("nc:PersonSurName", rec.sourceQN());
        assertEquals("sx", rec.prefix());
        assertEquals("lname", rec.localName());
        assertEquals("sx:lname", rec.qname());
        assertEquals("sx:lname", rec.targetArgument());
        assertEquals(makeURI(SX_NS, "lname"), rec.uri());
        assertEquals(SX_NS, rec.namespace());
        assertTrue(map.isMappedU(ncUri("PersonSurName")));
    }

    @Test
    void addMappingWithUnprefixedTargetUsesNullNamespace() throws Exception {
        var map = newMappingWithNcAndSx();

        map.addMapping("nc:personNameInitialIndicator", "isInitial");

        var rec = map.uriToMapRec(ncUri("personNameInitialIndicator"));
        assertNotNull(rec);
        assertEquals("nc:personNameInitialIndicator", rec.sourceQN());
        assertEquals("", rec.prefix());
        assertEquals("isInitial", rec.localName());
        assertEquals("isInitial", rec.qname());
        assertEquals("isInitial", rec.targetArgument());
        assertEquals("isInitial", rec.uri());
        assertEquals(NULL_NS_URI, rec.namespace());
    }

    @Test
    void addMappingAllowsReaddingSameMapping() throws Exception {
        var map = newMappingWithNcAndSx();

        assertDoesNotThrow(() -> map.addMapping("nc:PersonSurName", "sx:lname"));
        assertDoesNotThrow(() -> map.addMapping("nc:PersonSurName", "sx:lname"));

        var rec = map.uriToMapRec(ncUri("PersonSurName"));
        assertNotNull(rec);
        assertEquals("sx:lname", rec.targetArgument());
    }

    @Test
    void addMappingRejectsDifferentTargetForSameSource() throws Exception {
        var map = newMappingWithNcAndSx();
        map.assignPrefix("alt", ALT_NS);
        map.addMapping("nc:PersonSurName", "sx:lname");

        var ex = assertThrows(
            CMFException.class,
            () -> map.addMapping("nc:PersonSurName", "alt:otherName")
        );

        assertTrue(ex.getMessage().contains("already mapped"));
    }

    @Test
    void addMappingRejectsSameTargetForDifferentSource() throws Exception {
        var map = newMappingWithNcAndSx();
        map.addMapping("nc:PersonSurName", "sx:lname");

        var ex = assertThrows(
            CMFException.class,
            () -> map.addMapping("nc:PersonGivenName", "sx:lname")
        );

        assertTrue(ex.getMessage().contains("already mapped"));
    }

    @Test
    void addMappingRejectsInvalidOrUndeclaredNames() throws Exception {
        var map = newMappingWithNcAndSx();

        var ex1 = assertThrows(CMFException.class, () -> map.addMapping("badQName", "sx:lname"));
        assertTrue(ex1.getMessage().contains("Invalid source QName"));

        var ex2 = assertThrows(CMFException.class, () -> map.addMapping("nc:PersonName", "bad:name:again"));
        assertTrue(ex2.getMessage().contains("Invalid target name"));

        var ex3 = assertThrows(CMFException.class, () -> map.addMapping("zz:PersonName", "sx:lname"));
        assertTrue(ex3.getMessage().contains("Undeclared source prefix"));

        var ex4 = assertThrows(CMFException.class, () -> map.addMapping("nc:PersonName", "zz:lname"));
        assertTrue(ex4.getMessage().contains("Undeclared target prefix"));
    }

    @Test
    void uriToMapRecReturnsNullForUnknownSourceUri() throws Exception {
        var map = newMappingWithNcAndSx();

        assertFalse(map.isMappedU(ncUri("Unknown")));
        assertNull(map.uriToMapRec(ncUri("Unknown")));
    }

    @Test
    void writeAndReadRoundTripPreservesMappings() throws Exception {
        var map = newMappingWithNcAndSx();
        map.addMapping("nc:PersonSurName", "sx:lname");
        map.addMapping("nc:personNameInitialIndicator", "isInitial");

        var out = new StringWriter();
        map.write(out);

        var reparsed = Mapping.read(new StringReader(out.toString()));

        var rec1 = reparsed.uriToMapRec(ncUri("PersonSurName"));
        assertNotNull(rec1);
        assertEquals("sx", rec1.prefix());
        assertEquals("lname", rec1.localName());
        assertEquals("sx:lname", rec1.qname());
        assertEquals(makeURI(SX_NS, "lname"), rec1.uri());
        assertEquals(SX_NS, rec1.namespace());

        var rec2 = reparsed.uriToMapRec(ncUri("personNameInitialIndicator"));
        assertNotNull(rec2);
        assertEquals("", rec2.prefix());
        assertEquals("isInitial", rec2.localName());
        assertEquals("isInitial", rec2.qname());
        assertEquals("isInitial", rec2.uri());
        assertEquals(NULL_NS_URI, rec2.namespace());
    }

    @Test
    void writeOmitsXmlSchemaNamespacePrefixLine() throws Exception {
        var map = newMapping();
        map.assignPrefix("xsd", W3C_XML_SCHEMA_NS_URI);
        map.assignPrefix("nc", NC_NS);
        map.assignPrefix("sx", SX_NS);
        map.addMapping("nc:PersonSurName", "sx:lname");

        var out = new StringWriter();
        map.write(out);
        var text = out.toString();

        assertTrue(text.contains("PREFIX nc"));
        assertTrue(text.contains("PREFIX sx"));
        assertFalse(text.contains(W3C_XML_SCHEMA_NS_URI));
    }

    @Test
    void readSupportsBomCommentsAndInlineComments() throws Exception {
        String text =
            "\uFEFFPREFIX nc " + NC_NS + "\n" +
            "PREFIX sx " + SX_NS + "   # target namespace\n" +
            "\n" +
            "# comment line\n" +
            "nc:PersonSurName sx:lname # inline comment\n" +
            "nc:personNameInitialIndicator isInitial\n";

        var map = Mapping.read(new StringReader(text));

        var rec1 = map.uriToMapRec(ncUri("PersonSurName"));
        assertNotNull(rec1);
        assertEquals("sx:lname", rec1.targetArgument());

        var rec2 = map.uriToMapRec(ncUri("personNameInitialIndicator"));
        assertNotNull(rec2);
        assertEquals("isInitial", rec2.targetArgument());
        assertEquals(NULL_NS_URI, rec2.namespace());
    }

    @Test
    void readWrapsSemanticErrorsWithLineNumber() {
        String text =
            "PREFIX nc " + NC_NS + "\n" +
            "nc:PersonName sx:lname\n";

        var ex = assertThrows(CMFException.class, () -> Mapping.read(new StringReader(text)));
        assertTrue(ex.getMessage().contains("Line 2"));
        assertTrue(ex.getMessage().contains("Undeclared target prefix"));
    }

    @Test
    void readRejectsInvalidSyntaxWithLineNumber() {
        String text =
            "PREFIX nc " + NC_NS + "\n" +
            "this is not valid mapping syntax\n";

        var ex = assertThrows(CMFException.class, () -> Mapping.read(new StringReader(text)));
        assertTrue(ex.getMessage().contains("Line 2"));
        assertTrue(ex.getMessage().contains("invalid mapping syntax"));
    }

    @Test
    void readFileReadsFromDisk(@TempDir Path tempDir) throws IOException, CMFException {
        String text =
            "PREFIX nc " + NC_NS + "\n" +
            "PREFIX sx " + SX_NS + "\n" +
            "nc:PersonSurName sx:lname\n";

        Path file = tempDir.resolve("mapping.txt");
        Files.writeString(file, text, StandardCharsets.UTF_8);

        var map = Mapping.readFile(file.toFile());

        var rec = map.uriToMapRec(ncUri("PersonSurName"));
        assertNotNull(rec);
        assertEquals("sx:lname", rec.targetArgument());
        assertEquals(makeURI(SX_NS, "lname"), rec.uri());
    }
}
