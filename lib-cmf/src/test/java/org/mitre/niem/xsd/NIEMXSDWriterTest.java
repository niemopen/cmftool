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
package org.mitre.niem.xsd;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class NIEMXSDWriterTest {

    private static final String XS_NS = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    private static final String XMLNS_NS = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
    private static final String APPINFO_NS = "https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/";
    private static final String OTHER_NS = "urn:test:other";

    @Test
    void ordersAppinfoLocalTermAttributes() throws Exception {
        Document doc = schemaDocument();
        Element schema = doc.getDocumentElement();

        Element e = doc.createElementNS(APPINFO_NS, "appinfo:LocalTerm");
        e.setAttribute("literal", "Some literal");
        e.setAttribute("term", "SomeTerm");
        e.setAttribute("definition", "Some definition");
        schema.appendChild(e);

        String xml = write(doc);

        assertTrue(xml.contains(
                "  <appinfo:LocalTerm term=\"SomeTerm\" definition=\"Some definition\" literal=\"Some literal\"/>"));
    }

    @Test
    void ordersAppinfoAugmentationAttributes() throws Exception {
        Document doc = schemaDocument();
        Element schema = doc.getDocumentElement();

        Element e = doc.createElementNS(APPINFO_NS, "appinfo:Augmentation");
        e.setAttribute("id", "aug-1");
        e.setAttribute("globalClassCode", "GC");
        e.setAttribute("use", "required");
        e.setAttribute("property", "nc:Activity");
        e.setAttribute("class", "nc:PersonType");
        schema.appendChild(e);

        String xml = write(doc);

        assertTrue(xml.contains(
                "  <appinfo:Augmentation class=\"nc:PersonType\" property=\"nc:Activity\" use=\"required\" globalClassCode=\"GC\" id=\"aug-1\"/>"));
    }

    @Test
    void doesNotApplyAppinfoOrderingToDifferentPrefix() throws Exception {
        Document doc = schemaDocument();
        Element schema = doc.getDocumentElement();

        Element e = doc.createElementNS(OTHER_NS, "other:LocalTerm");
        e.setAttribute("term", "SomeTerm");
        e.setAttribute("alpha", "A");
        schema.appendChild(e);

        String xml = write(doc);

        assertTrue(xml.contains(
                "  <other:LocalTerm alpha=\"A\" term=\"SomeTerm\"/>"));
    }

    @Test
    void preservesBaseXsdWriterOrderingForXsElement() throws Exception {
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

    private static Document schemaDocument() throws Exception {
        Document doc = newDocument();
        Element schema = doc.createElementNS(XS_NS, "xs:schema");
        doc.appendChild(schema);

        schema.setAttributeNS(XMLNS_NS, "xmlns:xs", XS_NS);
        schema.setAttributeNS(XMLNS_NS, "xmlns:appinfo", APPINFO_NS);
        schema.setAttributeNS(XMLNS_NS, "xmlns:other", OTHER_NS);
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
        new NIEMXSDWriter("appinfo").writeXML(doc, sw);
        return sw.toString();
    }
}

