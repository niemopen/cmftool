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
package org.mitre.niem.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

class XMLDocumentTest {

    private static final String SIMPLE_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <root xmlns="urn:root" xmlns:a="urn:a" attr="value">
          <a:child xmlns:b="urn:b" id="c1">text</a:child>
        </root>
        """;

    private static final String XSD_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                   targetNamespace="http://example.com/ns"
                   xmlns="http://example.com/ns"
                   elementFormDefault="qualified">
          <xs:element name="Root" type="xs:string"/>
        </xs:schema>
        """;

    @Test
    void constructsFromFile(@TempDir Path tempDir) throws Exception {
        var xmlFile = writeFile(tempDir, "sample.xml", SIMPLE_XML);

        var xd = new XMLDocument(xmlFile.toFile());

        assertNotNull(xd.dom());
        assertNotNull(xd.documentElement());
        assertEquals("root", xd.documentElement().getLocalName());
        assertEquals(xmlFile.toFile().getCanonicalFile(), xd.docFile());
        assertEquals(xmlFile.toFile().getCanonicalFile().toURI(), xd.docURI());
    }

    @Test
    void constructsFromInputStreamWithoutUri() throws Exception {
        try (var is = new ByteArrayInputStream(SIMPLE_XML.getBytes(StandardCharsets.UTF_8))) {
            var xd = new XMLDocument(is);

            assertNotNull(xd.dom());
            assertEquals("root", xd.documentElement().getLocalName());
            assertNull(xd.docFile());
            assertNull(xd.docURI());
        }
    }

    @Test
    void constructsFromInputStreamWithUri() throws Exception {
        var uri = URI.create("file:/virtual/sample.xml");

        try (var is = new ByteArrayInputStream(SIMPLE_XML.getBytes(StandardCharsets.UTF_8))) {
            var xd = new XMLDocument(is, uri);

            assertNotNull(xd.dom());
            assertEquals("root", xd.documentElement().getLocalName());
            assertNull(xd.docFile());
            assertEquals(uri, xd.docURI());
        }
    }

    @Test
    void constructsFromInputSourceUsingSystemId() throws Exception {
        var src = new InputSource(new StringReader(SIMPLE_XML));
        src.setSystemId("file:/virtual/from-input-source.xml");

        var xd = new XMLDocument(src);

        assertNotNull(xd.dom());
        assertEquals("root", xd.documentElement().getLocalName());
        assertNull(xd.docFile());
        assertEquals(URI.create("file:/virtual/from-input-source.xml"), xd.docURI());
    }

    @Test
    void constructsFromInputSourceWithExplicitUri() throws Exception {
        var src = new InputSource(new StringReader(SIMPLE_XML));
        var uri = URI.create("file:/virtual/explicit.xml");

        var xd = new XMLDocument(src, uri);

        assertNotNull(xd.dom());
        assertEquals("root", xd.documentElement().getLocalName());
        assertEquals(uri, xd.docURI());
    }

    @Test
    void namespaceDeclarationsFindsRootAndNestedDeclarations() throws Exception {
        var xd = new XMLDocument(new ByteArrayInputStream(SIMPLE_XML.getBytes(StandardCharsets.UTF_8)));
        var decls = xd.namespaceDeclarations();

        assertEquals(3, decls.size());

        assertEquals("", decls.get(0).prefix());
        assertEquals("urn:root", decls.get(0).ns());
        assertEquals(0, decls.get(0).depth());

        assertEquals("a", decls.get(1).prefix());
        assertEquals("urn:a", decls.get(1).ns());
        assertEquals(0, decls.get(1).depth());

        assertEquals("b", decls.get(2).prefix());
        assertEquals("urn:b", decls.get(2).ns());
        assertEquals(1, decls.get(2).depth());
    }

    @Test
    void evalForBooleanStringAndNodesWorks() throws Exception {
        var xd = new XMLDocument(new ByteArrayInputStream(SIMPLE_XML.getBytes(StandardCharsets.UTF_8)));
        Element root = xd.documentElement();

        assertTrue(XMLDocument.evalForBoolean(root, "count(*) = 1"));
        assertFalse(XMLDocument.evalForBoolean(root, "count(*) = 2"));

        assertEquals("value", XMLDocument.evalForString(root, "@attr"));
        assertEquals("text", XMLDocument.evalForString(root, "//*[local-name()='child']/text()"));

        var nodes = XMLDocument.evalForNodes(root, "//*[local-name()='child']");
        assertNotNull(nodes);
        assertEquals(1, nodes.getLength());
        assertEquals("child", nodes.item(0).getLocalName());
    }

    @Test
    void qnameHelpersWork() {
        assertEquals("PersonName", XMLDocument.qnToName("nc:PersonName"));
        assertEquals("nc", XMLDocument.qnToPrefix("nc:PersonName"));

        assertEquals("PersonName", XMLDocument.qnToName("PersonName"));
        assertEquals("", XMLDocument.qnToPrefix("PersonName"));

        assertEquals("nc:PersonName", XMLDocument.makeQN("nc", "PersonName"));
        assertEquals("PersonName", XMLDocument.makeQN("", "PersonName"));
        assertEquals("PersonName", XMLDocument.makeQN(" ", "PersonName"));
    }

    @Test
    void makeUriWorksForSlashHashAndUrn() {
        assertEquals("http://example.com/ns/Thing", XMLDocument.makeURI("http://example.com/ns", "Thing"));
        assertEquals("http://example.com/ns/Thing", XMLDocument.makeURI("http://example.com/ns/", "Thing"));
        assertEquals("http://example.com/ns#Thing", XMLDocument.makeURI("http://example.com/ns#", "Thing"));
        assertEquals("urn:example:Thing", XMLDocument.makeURI("urn:example", "Thing"));
    }

    @Test
    void getXmlDocumentElementNamespaceReturnsNamespace(@TempDir Path tempDir) throws Exception {
        var xmlFile = writeFile(tempDir, "doc.xml", SIMPLE_XML);

        assertEquals("urn:root", XMLDocument.getXMLDocumentElementNamespace(xmlFile.toFile()));
        assertEquals("urn:root", XMLDocument.getXMLDocumentElementNamespace(xmlFile.toString()));
    }

    @Test
    void getXsdTargetNamespaceReturnsTargetNamespace(@TempDir Path tempDir) throws Exception {
        var xsdFile = writeFile(tempDir, "schema.xsd", XSD_XML);

        assertEquals("http://example.com/ns", XMLDocument.getXSDTargetNamespace(xsdFile.toFile()));
        assertEquals("http://example.com/ns", XMLDocument.getXSDTargetNamespace(xsdFile.toString()));
    }

    @Test
    void getXsdTargetNamespaceReturnsEmptyForNonSchema(@TempDir Path tempDir) throws Exception {
        var xmlFile = writeFile(tempDir, "not-schema.xml", SIMPLE_XML);

        assertEquals("", XMLDocument.getXSDTargetNamespace(xmlFile.toFile()));
    }

    @Test
    void invalidXPathReturnsDefaultValues() throws Exception {
        var xd = new XMLDocument(new ByteArrayInputStream(SIMPLE_XML.getBytes(StandardCharsets.UTF_8)));
        var root = xd.documentElement();

        assertFalse(XMLDocument.evalForBoolean(root, "//*["));
        assertEquals("", XMLDocument.evalForString(root, "//*["));
        assertNull(XMLDocument.evalForNodes(root, "//*["));
    }

    private static Path writeFile(Path dir, String name, String content) throws Exception {
        var path = dir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }
}

