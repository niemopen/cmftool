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

import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class XMLWriterTest {

    private static final String CMF_NS = "https://docs.oasis-open.org/niemopen/ns/specification/cmf/1.0/";
    private static final String STRUCTURES_NS = "https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/";
    private static final String XSI_NS = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
    private static final String XMLNS_NS = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

    @Test
    void writesDefaultNamespaceDeclarationFirstOnRoot() throws Exception {
        Document doc = newDocument();
        Element model = doc.createElementNS(CMF_NS, "Model");
        doc.appendChild(model);

        model.setAttributeNS(XMLNS_NS, "xmlns", CMF_NS);
        model.setAttributeNS(XMLNS_NS, "xmlns:cmf", CMF_NS);
        model.setAttributeNS(XMLNS_NS, "xmlns:structures", STRUCTURES_NS);
        model.setAttributeNS(XMLNS_NS, "xmlns:xsi", XSI_NS);
        model.setAttributeNS(XMLConstants.XML_NS_URI, "xml:lang", "en-US");

        String xml = write(doc);

        assertEquals(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <Model
                  xmlns="https://docs.oasis-open.org/niemopen/ns/specification/cmf/1.0/"
                  xmlns:cmf="https://docs.oasis-open.org/niemopen/ns/specification/cmf/1.0/"
                  xmlns:structures="https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xml:lang="en-US"/>
                """,
                xml);
    }

    @Test
    void standaloneElementCopiesInScopeDefaultNamespaceDeclaration() throws Exception {
        Document doc = newDocument();

        Element wrapper = doc.createElement("wrapper");
        doc.appendChild(wrapper);
        wrapper.setAttributeNS(XMLNS_NS, "xmlns", CMF_NS);
        wrapper.setAttributeNS(XMLNS_NS, "xmlns:cmf", CMF_NS);
        wrapper.setAttributeNS(XMLNS_NS, "xmlns:structures", STRUCTURES_NS);
        wrapper.setAttributeNS(XMLNS_NS, "xmlns:xsi", XSI_NS);

        Element model = doc.createElementNS(CMF_NS, "Model");
        model.setAttributeNS(XMLConstants.XML_NS_URI, "xml:lang", "en-US");
        wrapper.appendChild(model);

        String xml = write(model);

        assertEquals(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <Model
                  xmlns="https://docs.oasis-open.org/niemopen/ns/specification/cmf/1.0/"
                  xmlns:cmf="https://docs.oasis-open.org/niemopen/ns/specification/cmf/1.0/"
                  xmlns:structures="https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xml:lang="en-US"/>
                """,
                xml);
    }

    @Test
    void doesNotSynthesizeDefaultNamespaceDeclarationWhenMissing() throws Exception {
        Document doc = newDocument();
        Element model = doc.createElementNS(CMF_NS, "Model");
        doc.appendChild(model);

        model.setAttributeNS(XMLNS_NS, "xmlns:xsi", XSI_NS);

        String xml = write(doc);

        assertEquals(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <Model
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                """,
                xml);
    }

    private static Document newDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().newDocument();
    }

    private static String write(Document doc) throws Exception {
        StringWriter sw = new StringWriter();
        new XMLWriter().writeXML(doc, sw);
        return sw.toString();
    }

    private static String write(Element elem) throws Exception {
        StringWriter sw = new StringWriter();
        new XMLWriter().writeXML(elem, sw);
        return sw.toString();
    }
}
