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

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

class XMLWriterTest {

    @Test
    void writeXmlDocumentProducesExpectedPrettyOutput() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <r:Root xmlns:z="urn:z" b="2" xmlns="urn:def" a10="10" xmlns:a="urn:a" a2="2" xmlns:r="urn:r">
              <child z="1">text</child>
              <!--c-->
              <?pi data?>
            </r:Root>
            """);

        var out = new StringWriter();
        XMLWriter.writeXML(dom, out);

        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <r:Root
              xmlns="urn:def"
              xmlns:a="urn:a"
              xmlns:r="urn:r"
              xmlns:z="urn:z"
              a2="2"
              a10="10"
              b="2">
              <child z="1">text</child>
              <!--c-->
              <?pi data?>
            </r:Root>
            """, out.toString());
    }

    @Test
    void writeXmlElementAsStandaloneCopiesAncestorNamespaces() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <root xmlns="urn:def" xmlns:p="urn:p">
              <p:child attr="v"/>
            </root>
            """);

        var child = (Element) dom.getDocumentElement()
            .getElementsByTagNameNS("urn:p", "child")
            .item(0);

        var out = new StringWriter();
        XMLWriter.writeXML(child, out);

        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <p:child
              xmlns="urn:def"
              xmlns:p="urn:p"
              attr="v"/>
            """, out.toString());
    }

    @Test
    void writeXmlEscapesAttributeAndTextContent() throws Exception {
        var dom = newDocument();
        var root = dom.createElement("root");
        dom.appendChild(root);

        root.setAttribute("attr", "a&b<\"c\n\r\t");
        root.appendChild(dom.createTextNode("x<y & z\r"));

        var out = new StringWriter();
        XMLWriter.writeXML(dom, out);
        var text = out.toString();

        assertTrue(text.contains("attr=\"a&amp;b&lt;&quot;c&#xA;&#xD;&#x9;\""));
        assertTrue(text.contains(">x&lt;y &amp; z&#xD;</root>"));
    }

    @Test
    void nodeToTextSerializesDocumentElementAndComment() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <root xmlns="urn:test"><child>value</child></root>
            """);

        var docText = XMLWriter.nodeToText(dom);
        var elemText = XMLWriter.nodeToText(dom.getDocumentElement());

        Comment comment = dom.createComment("hello");
        var commentText = XMLWriter.nodeToText(comment);

        assertTrue(docText.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"));
        assertTrue(elemText.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"));
        assertEquals("<!--hello-->\n", commentText);
    }

    @Test
    void writeXmlToFileWritesUtf8Content(@TempDir Path tempDir) throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <root><child>value</child></root>
            """);

        var outFile = tempDir.resolve("out.xml").toFile();
        XMLWriter.writeXML(dom, outFile);

        var text = Files.readString(outFile.toPath(), StandardCharsets.UTF_8);
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
              <child>value</child>
            </root>
            """, text);
    }

    private static Document parseXml(String xml) throws Exception {
        var db = ParserBootstrap.docBuilder();
        return db.parse(new InputSource(new java.io.StringReader(xml)));
    }

    private static Document newDocument() throws Exception {
        return ParserBootstrap.docBuilder().newDocument();
    }
}
