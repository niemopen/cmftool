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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;

class XMLSchemaDocumentTest {

    private static final String XSD = """
        <?xml version="1.0" encoding="UTF-8"?>
        <xs:schema
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:xml="http://www.w3.org/XML/1998/namespace"
            xmlns:app="http://example.com/app"
            targetNamespace="http://example.com/app"
            xml:lang="en"
            version="1.2">

          <xs:annotation>
            <xs:documentation>Schema level documentation</xs:documentation>
          </xs:annotation>

          <xs:import namespace="http://example.com/ext" schemaLocation="ext.xsd">
            <xs:annotation>
              <xs:documentation xml:lang="fr">Import documentation</xs:documentation>
            </xs:annotation>
          </xs:import>

          <xs:element name="Root" type="xs:string"/>

        </xs:schema>
        """;

    private static final String XML_LANG_SAMPLE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <xs:schema
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:xml="http://www.w3.org/XML/1998/namespace"
            xml:lang="en">
          <xs:annotation>
            <xs:documentation>doc 1</xs:documentation>
            <xs:documentation xml:lang="fr">doc 2</xs:documentation>
          </xs:annotation>
          <xs:element name="Root">
            <xs:annotation>
              <xs:documentation>element doc</xs:documentation>
            </xs:annotation>
          </xs:element>
        </xs:schema>
        """;

    @Test
    void constructsFromFile(@TempDir Path tempDir) throws Exception {
        var xsdFile = writeFile(tempDir, "sample.xsd", XSD);

        var xsd = new XMLSchemaDocument(xsdFile.toFile());

        assertNotNull(xsd.dom());
        assertNotNull(xsd.documentElement());
        assertEquals("schema", xsd.documentElement().getLocalName());
        assertEquals(xsdFile.toFile().getCanonicalFile(), xsd.docFile());
        assertEquals(xsdFile.toFile().getCanonicalFile().toURI(), xsd.docURI());
    }

    @Test
    void constructsFromInputStreamWithoutUri() throws Exception {
        try (var in = new ByteArrayInputStream(XSD.getBytes(StandardCharsets.UTF_8))) {
            var xsd = new XMLSchemaDocument(in);

            assertNotNull(xsd.dom());
            assertEquals("schema", xsd.documentElement().getLocalName());
            assertNull(xsd.docFile());
            assertNull(xsd.docURI());
        }
    }

    @Test
    void constructsFromInputStreamWithUri() throws Exception {
        var uri = URI.create("file:/virtual/sample.xsd");

        try (var in = new ByteArrayInputStream(XSD.getBytes(StandardCharsets.UTF_8))) {
            var xsd = new XMLSchemaDocument(in, uri);

            assertNotNull(xsd.dom());
            assertEquals("schema", xsd.documentElement().getLocalName());
            assertNull(xsd.docFile());
            assertEquals(uri, xsd.docURI());
        }
    }

    @Test
    void constructsFromInputSource() throws Exception {
        var src = new InputSource(new StringReader(XSD));
        src.setSystemId("file:/virtual/from-input-source.xsd");

        var xsd = new XMLSchemaDocument(src);

        assertNotNull(xsd.dom());
        assertEquals("schema", xsd.documentElement().getLocalName());
        assertNull(xsd.docFile());
        assertEquals(URI.create("file:/virtual/from-input-source.xsd"), xsd.docURI());
    }

    @Test
    void constructsFromInputSourceWithExplicitUri() throws Exception {
        var src = new InputSource(new StringReader(XSD));
        var uri = URI.create("file:/virtual/explicit.xsd");

        var xsd = new XMLSchemaDocument(src, uri);

        assertNotNull(xsd.dom());
        assertEquals("schema", xsd.documentElement().getLocalName());
        assertEquals(uri, xsd.docURI());
    }

    @Test
    void returnsSchemaAttributes() throws Exception {
        var xsd = new XMLSchemaDocument(
            new ByteArrayInputStream(XSD.getBytes(StandardCharsets.UTF_8)));

        assertEquals("http://example.com/app", xsd.targetNamespace());
        assertEquals("en", xsd.language());
        assertEquals("1.2", xsd.version());

        assertSame(xsd.targetNamespace(), xsd.targetNamespace());
        assertSame(xsd.language(), xsd.language());
        assertSame(xsd.version(), xsd.version());
    }

    @Test
    void returnsSchemaLevelDocumentation() throws Exception {
        var xsd = new XMLSchemaDocument(
            new ByteArrayInputStream(XSD.getBytes(StandardCharsets.UTF_8)));

        List<LanguageString> docs = xsd.documentation();

        assertNotNull(docs);
        assertEquals(1, docs.size());
        assertNotNull(docs.get(0));
    }

    @Test
    void returnsImportElements() throws Exception {
        var xsd = new XMLSchemaDocument(
            new ByteArrayInputStream(XSD.getBytes(StandardCharsets.UTF_8)));

        List<XMLSchemaImport> imports = xsd.importElements();

        assertNotNull(imports);
        assertEquals(1, imports.size());
        assertNotNull(imports.get(0));
    }

    @Test
    void getDocumentationReturnsImmediateAnnotationDocumentation() throws Exception {
        var xsd = new XMLSchemaDocument(
            new ByteArrayInputStream(XML_LANG_SAMPLE.getBytes(StandardCharsets.UTF_8)));

        var root = xsd.documentElement();
        var docs = XMLSchemaDocument.getDocumentation(root);

        assertNotNull(docs);
        assertEquals(2, docs.size());
        assertNotNull(docs.get(0));
        assertNotNull(docs.get(1));
    }

    @Test
    void getDocumentationOnElementFindsElementDocumentation() throws Exception {
        var xsd = new XMLSchemaDocument(
            new ByteArrayInputStream(XML_LANG_SAMPLE.getBytes(StandardCharsets.UTF_8)));

        var nodes = XMLDocument.evalForNodes(xsd.documentElement(), "/*/*[local-name()='element']");
        var element = (org.w3c.dom.Element) nodes.item(0);

        var docs = XMLSchemaDocument.getDocumentation(element);

        assertNotNull(docs);
        assertEquals(1, docs.size());
        assertNotNull(docs.get(0));
    }

    @Test
    void getXmlLangFindsInheritedAndExplicitLanguage() throws Exception {
        var xsd = new XMLSchemaDocument(
            new ByteArrayInputStream(XML_LANG_SAMPLE.getBytes(StandardCharsets.UTF_8)));

        var root = xsd.documentElement();
        var docNodes = XMLDocument.evalForNodes(root, "//*[local-name()='documentation']");

        var inheritedDoc = (org.w3c.dom.Element) docNodes.item(0);
        var explicitDoc = (org.w3c.dom.Element) docNodes.item(1);

        assertEquals("en", XMLSchemaDocument.getXMLLang(inheritedDoc));
        assertEquals("fr", XMLSchemaDocument.getXMLLang(explicitDoc));
    }

    @Test
    void getXmlLangDefaultsToEnUsWhenNoLanguageInScope() throws Exception {
        String noLangXsd = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:annotation>
                <xs:documentation>doc</xs:documentation>
              </xs:annotation>
            </xs:schema>
            """;

        var xsd = new XMLSchemaDocument(
            new ByteArrayInputStream(noLangXsd.getBytes(StandardCharsets.UTF_8)));

        var docNodes = XMLDocument.evalForNodes(xsd.documentElement(), "//*[local-name()='documentation']");
        var doc = (org.w3c.dom.Element) docNodes.item(0);

        assertEquals("en-US", XMLSchemaDocument.getXMLLang(doc));
    }

    @Test
    void getLanguageStringReturnsNonNullValue() throws Exception {
        var xsd = new XMLSchemaDocument(
            new ByteArrayInputStream(XML_LANG_SAMPLE.getBytes(StandardCharsets.UTF_8)));

        var docNodes = XMLDocument.evalForNodes(xsd.documentElement(), "//*[local-name()='documentation']");
        var doc = (org.w3c.dom.Element) docNodes.item(0);

        var ls = XMLSchemaDocument.getLanguageString(doc);

        assertNotNull(ls);
    }

    private static Path writeFile(Path dir, String name, String content) throws Exception {
        var path = dir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }
}
