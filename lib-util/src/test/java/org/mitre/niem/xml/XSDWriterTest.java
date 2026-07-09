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
package org.mitre.niem.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

class XSDWriterTest {

    @Test
    void writeXmlOrdersSchemaRootAttributesAndNamespaceDeclarations() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                version="1.0"
                xmlns:ex="http://example.com/test"
                elementFormDefault="qualified"
                targetNamespace="http://example.com/test"
                xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element name="Root" type="xs:string"/>
            </xs:schema>
            """);

        var out = new StringWriter();
        new XSDWriter().writeXML(dom, out);

        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema
              targetNamespace="http://example.com/test"
              xmlns:ex="http://example.com/test"
              xmlns:xs="http://www.w3.org/2001/XMLSchema"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              elementFormDefault="qualified"
              version="1.0">
              <xs:element name="Root" type="xs:string"/>
            </xs:schema>
            """, out.toString());
    }

    @Test
    void writeXmlEnsuresXsNamespaceOnSchemaRoot() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <schema xmlns="http://www.w3.org/2001/XMLSchema"
                    targetNamespace="http://example.com/test">
              <element name="Root" type="string"/>
            </schema>
            """);

        var out = new StringWriter();
        new XSDWriter().writeXML(dom, out);
        var text = out.toString();

        assertTrue(text.contains("xmlns:xs=\"http://www.w3.org/2001/XMLSchema\""));
        assertTrue(text.contains("targetNamespace=\"http://example.com/test\""));
    }

    @Test
    void writeXmlOrdersXsElementAttributes() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element substitutionGroup="ex:Base"
                          maxOccurs="2"
                          type="xs:string"
                          name="Root"
                          minOccurs="0"
                          xmlns:ex="http://example.com/test"/>
            </xs:schema>
            """);

        var out = new StringWriter();
        new XSDWriter().writeXML(dom, out);

        assertTrue(out.toString().contains(
            "<xs:element xmlns:ex=\"http://example.com/test\" name=\"Root\" type=\"xs:string\" minOccurs=\"0\" maxOccurs=\"2\" substitutionGroup=\"ex:Base\"/>"
        ));
    }

    @Test
    void writeXmlOrdersXsImportAttributes() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:import id="i1"
                         schemaLocation="ext.xsd"
                         namespace="http://example.com/ext"/>
            </xs:schema>
            """);

        var out = new StringWriter();
        new XSDWriter().writeXML(dom, out);

        assertTrue(out.toString().contains(
            "<xs:import namespace=\"http://example.com/ext\" schemaLocation=\"ext.xsd\" id=\"i1\"/>"
        ));
    }

    @Test
    void writeStandaloneElementCopiesInScopeNamespacesAndAppliesXsdOrdering() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                       xmlns:ex="http://example.com/test"
                       targetNamespace="http://example.com/test">
              <xs:element type="ex:SomeType" name="Root"/>
            </xs:schema>
            """);

        var elem = (Element) dom.getDocumentElement()
            .getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "element")
            .item(0);

        var out = new StringWriter();
        new XSDWriter().writeXML(elem, out);

        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:element
              xmlns:ex="http://example.com/test"
              xmlns:xs="http://www.w3.org/2001/XMLSchema"
              name="Root"
              type="ex:SomeType"/>
            """, out.toString());
    }

    @Test
    void nodeToTextSerializesDocumentAndElement() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element name="Root" type="xs:string"/>
            </xs:schema>
            """);

        var writer = new XSDWriter();
        var docText = writer.nodeToText(dom);
        var elemText = writer.nodeToText(dom.getDocumentElement());

        assertTrue(docText.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"));
        assertTrue(docText.contains("<xs:schema"));
        assertTrue(elemText.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"));
        assertTrue(elemText.contains("<xs:schema"));
    }

    @Test
    void writeXmlToFileWritesUtf8Content(@TempDir Path tempDir) throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element name="Root" type="xs:string"/>
            </xs:schema>
            """);

        var outFile = tempDir.resolve("schema.xsd").toFile();
        new XSDWriter().writeXML(dom, outFile);

        var text = Files.readString(outFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(text.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"));
        assertTrue(text.contains("<xs:element name=\"Root\" type=\"xs:string\"/>"));
    }

    private static Document parseXml(String xml) throws Exception {
        var db = ParserBootstrap.docBuilder();
        return db.parse(new InputSource(new StringReader(xml)));
    }
}
