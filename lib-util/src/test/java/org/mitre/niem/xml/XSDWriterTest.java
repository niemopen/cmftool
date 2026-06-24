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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class XSDWriterTest {

    private static final String XS_NS = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    private static final String XSI_NS = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
    private static final String XMLNS_NS = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

    @Test
    void writesSchemaRootAttributesInRequestedOrderAndAddsXmlnsXs() throws Exception {
        Document doc = newDocument();
        Element schema = doc.createElementNS(XS_NS, "xs:schema");
        doc.appendChild(schema);

        schema.setAttribute("elementFormDefault", "qualified");
        schema.setAttributeNS(XMLNS_NS, "xmlns:xsi", XSI_NS);
        schema.setAttribute("targetNamespace", "urn:test");
        schema.setAttributeNS(XMLNS_NS, "xmlns:b", "urn:b");
        schema.setAttribute("attributeFormDefault", "unqualified");
        schema.setAttributeNS(XMLNS_NS, "xmlns:a", "urn:a");

        String xml = write(doc);

        assertEquals(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema
                  targetNamespace="urn:test"
                  xmlns:a="urn:a"
                  xmlns:b="urn:b"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  attributeFormDefault="unqualified"
                  elementFormDefault="qualified"/>
                """,
                xml);
    }

    @Test
    void ordersXsElementAttributes() throws Exception {
        Document doc = schemaDocument();
        Element schema = doc.getDocumentElement();

        Element e = doc.createElementNS(XS_NS, "xs:element");
        e.setAttribute("maxOccurs", "1");
        e.setAttribute("type", "xs:string");
        e.setAttribute("substitutionGroup", "tns:Base");
        e.setAttribute("name", "Person");
        e.setAttribute("nillable", "true");
        e.setAttribute("minOccurs", "0");
        e.setAttribute("ref", "tns:PersonRef");
        schema.appendChild(e);

        String xml = write(doc);

        assertTrue(xml.contains(
                "  <xs:element name=\"Person\" ref=\"tns:PersonRef\" type=\"xs:string\" minOccurs=\"0\" maxOccurs=\"1\" substitutionGroup=\"tns:Base\" nillable=\"true\"/>"));
    }

    @Test
    void ordersXsImportAttributes() throws Exception {
        Document doc = schemaDocument();
        Element schema = doc.getDocumentElement();

        Element e = doc.createElementNS(XS_NS, "xs:import");
        e.setAttribute("id", "imp1");
        e.setAttribute("schemaLocation", "external.xsd");
        e.setAttribute("namespace", "urn:external");
        schema.appendChild(e);

        String xml = write(doc);

        assertTrue(xml.contains(
                "  <xs:import namespace=\"urn:external\" schemaLocation=\"external.xsd\" id=\"imp1\"/>"));
    }

    @Test
    void ordersXsComplexTypeAttributes() throws Exception {
        Document doc = schemaDocument();
        Element schema = doc.getDocumentElement();

        Element e = doc.createElementNS(XS_NS, "xs:complexType");
        e.setAttribute("abstract", "true");
        e.setAttribute("type", "tns:BaseType");
        e.setAttribute("name", "PersonType");
        schema.appendChild(e);

        String xml = write(doc);

        assertTrue(xml.contains(
                "  <xs:complexType name=\"PersonType\" type=\"tns:BaseType\" abstract=\"true\"/>"));
    }

    @Test
    void ordersXsAttributeAttributes() throws Exception {
        Document doc = schemaDocument();
        Element schema = doc.getDocumentElement();

        Element e = doc.createElementNS(XS_NS, "xs:attribute");
        e.setAttribute("type", "xs:token");
        e.setAttribute("use", "required");
        e.setAttribute("name", "id");
        e.setAttribute("ref", "tns:IdAttribute");
        e.setAttribute("fixed", "ABC");
        schema.appendChild(e);

        String xml = write(doc);

        assertTrue(xml.contains(
                "  <xs:attribute name=\"id\" ref=\"tns:IdAttribute\" type=\"xs:token\" use=\"required\" fixed=\"ABC\"/>"));
    }

    @Test
    void ordersXsChoiceAttributes() throws Exception {
        Document doc = schemaDocument();
        Element schema = doc.getDocumentElement();

        Element e = doc.createElementNS(XS_NS, "xs:choice");
        e.setAttribute("id", "choice1");
        e.setAttribute("maxOccurs", "unbounded");
        e.setAttribute("minOccurs", "0");
        schema.appendChild(e);

        String xml = write(doc);

        assertTrue(xml.contains(
                "  <xs:choice minOccurs=\"0\" maxOccurs=\"unbounded\" id=\"choice1\"/>"));
    }

    @Test
    void standaloneSchemaElementCopiesInScopeNamespacesKeepsOrderingAndAddsXmlnsXs() throws Exception {
        Document doc = newDocument();

        Element wrapper = doc.createElement("wrapper");
        doc.appendChild(wrapper);
        wrapper.setAttributeNS(XMLNS_NS, "xmlns:xsi", XSI_NS);
        wrapper.setAttributeNS(XMLNS_NS, "xmlns:a", "urn:a");

        Element schema = doc.createElementNS(XS_NS, "xs:schema");
        schema.setAttribute("elementFormDefault", "qualified");
        schema.setAttribute("targetNamespace", "urn:test");
        wrapper.appendChild(schema);

        String xml = write(schema);

        assertEquals(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema
                  targetNamespace="urn:test"
                  xmlns:a="urn:a"
                  xmlns:xs="http://www.w3.org/2001/XMLSchema"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  elementFormDefault="qualified"/>
                """,
                xml);
    }

    private static Document schemaDocument() throws Exception {
        Document doc = newDocument();
        Element schema = doc.createElementNS(XS_NS, "xs:schema");
        doc.appendChild(schema);
        schema.setAttributeNS(XMLNS_NS, "xmlns:tns", "urn:test:tns");
        return doc;
    }

    private static Document newDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().newDocument();
    }

    private static String write(Document doc) throws Exception {
        StringWriter sw = new StringWriter();
        new XSDWriter().writeXML(doc, sw);
        return sw.toString();
    }

    private static String write(Element elem) throws Exception {
        StringWriter sw = new StringWriter();
        new XSDWriter().writeXML(elem, sw);
        return sw.toString();
    }
}

