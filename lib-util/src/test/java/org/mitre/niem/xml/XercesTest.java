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

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_MAXINCLUSIVE;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_WHITESPACE;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSFacet;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.junit.jupiter.api.Test;

class XercesTest {

    private static final String TEST_NS = "urn:test";

    private static final String TEST_SCHEMA = """
        <?xml version="1.0" encoding="UTF-8"?>
        <xs:schema
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:t="urn:test"
            targetNamespace="urn:test"
            elementFormDefault="qualified"
            xml:lang="en">

          <xs:element name="Root" type="xs:string">
            <xs:annotation>
              <xs:documentation>Element doc 1</xs:documentation>
              <xs:documentation xml:lang="fr">Element doc 2</xs:documentation>
            </xs:annotation>
          </xs:element>

          <xs:attribute name="Code" type="xs:string">
            <xs:annotation>
              <xs:documentation>Attribute doc</xs:documentation>
            </xs:annotation>
          </xs:attribute>

          <xs:simpleType name="CustomByte">
            <xs:annotation>
              <xs:documentation>Type doc</xs:documentation>
            </xs:annotation>
            <xs:restriction base="xs:byte">
              <xs:maxInclusive value="127"/>
            </xs:restriction>
          </xs:simpleType>

        </xs:schema>
        """;

    @Test
    void getDocumentationReturnsEmptyForNullInputs() {
        assertTrue(Xerces.getDocumentation((org.apache.xerces.xs.XSObject) null).isEmpty());
        assertTrue(Xerces.getDocumentation((org.apache.xerces.xs.XSAnnotation) null).isEmpty());
    }

    @Test
    void getDocumentationExtractsDocsFromAnnotatedObjects() throws Exception {
        XSModel model = loadModel(TEST_SCHEMA);

        XSElementDeclaration el = model.getElementDeclaration("Root", TEST_NS);
        XSAttributeDeclaration at = model.getAttributeDeclaration("Code", TEST_NS);
        XSTypeDefinition td = model.getTypeDefinition("CustomByte", TEST_NS);

        assertNotNull(el);
        assertNotNull(at);
        assertNotNull(td);

        assertEquals(2, Xerces.getDocumentation(el).size());
        assertEquals(1, Xerces.getDocumentation(at).size());
        assertEquals(1, Xerces.getDocumentation(td).size());
    }

    @Test
    void getDocumentationExtractsDocsFromAnnotation() throws Exception {
        XSModel model = loadModel(TEST_SCHEMA);
        XSElementDeclaration el = model.getElementDeclaration("Root", TEST_NS);

        XSObjectList anns = Xerces.getAnnotations(el);
        assertNotNull(anns);
        assertEquals(1, anns.getLength());

        var ann = (org.apache.xerces.xs.XSAnnotation) anns.item(0);
        var docs = Xerces.getDocumentation(ann);

        assertEquals(2, docs.size());
    }

    @Test
    void getAnnotationsReturnsExpectedLists() throws Exception {
        XSModel model = loadModel(TEST_SCHEMA);

        XSElementDeclaration el = model.getElementDeclaration("Root", TEST_NS);
        XSAttributeDeclaration at = model.getAttributeDeclaration("Code", TEST_NS);
        XSSimpleTypeDefinition td = (XSSimpleTypeDefinition) model.getTypeDefinition("CustomByte", TEST_NS);

        assertEquals(1, Xerces.getAnnotations(el).getLength());
        assertEquals(1, Xerces.getAnnotations(at).getLength());
        assertEquals(1, Xerces.getAnnotations(td).getLength());

        XSFacet facet = (XSFacet) td.getFacets().item(0);
        assertEquals(0, Xerces.getAnnotations(facet).getLength());
    }

    @Test
    void facetKindToElementNameMapsKnownValues() {
        assertEquals("maxExclusive", Xerces.facetKindToElementName(FACET_MAXEXCLUSIVE));
        assertEquals("maxInclusive", Xerces.facetKindToElementName(FACET_MAXINCLUSIVE));
        assertEquals("whiteSpace", Xerces.facetKindToElementName(FACET_WHITESPACE));
        assertEquals("", Xerces.facetKindToElementName((short) -123));
    }

    @Test
    void facetElementNameToKindMapsKnownValues() {
        assertEquals(FACET_MAXEXCLUSIVE, Xerces.facetElementNameToKind("maxExclusive"));
        assertEquals(FACET_MAXINCLUSIVE, Xerces.facetElementNameToKind("maxInclusive"));
        assertEquals(FACET_WHITESPACE, Xerces.facetElementNameToKind("whiteSpace"));
        assertEquals(-1, Xerces.facetElementNameToKind("notAFacet"));
    }

    @Test
    void isDefaultFacetDetectsBuiltinFacets() throws Exception {
        XSModel model = loadModel(TEST_SCHEMA);
        XSTypeDefinition byteType = model.getTypeDefinition("byte", W3C_XML_SCHEMA_NS_URI);

        assertNotNull(byteType);
        assertTrue(Xerces.isDefaultFacet(byteType, FACET_MAXINCLUSIVE, "127"));
        assertTrue(Xerces.isDefaultFacet(byteType, FACET_WHITESPACE, "collapse"));
        assertFalse(Xerces.isDefaultFacet(byteType, FACET_MAXINCLUSIVE, "126"));
    }

    @Test
    void isDefaultFacetHandlesNulls() {
        assertFalse(Xerces.isDefaultFacet((XSTypeDefinition) null, FACET_MAXINCLUSIVE, "127"));
        assertFalse(Xerces.isDefaultFacet((XSTypeDefinition) null, (XSFacet) null));
    }

    @Test
    void isDefaultFacetXsFacetOverloadWorks() throws Exception {
        XSModel model = loadModel(TEST_SCHEMA);
        XSSimpleTypeDefinition byteType = (XSSimpleTypeDefinition) model.getTypeDefinition("byte", W3C_XML_SCHEMA_NS_URI);

        assertNotNull(byteType);

        XSFacet maxInclusiveFacet = null;
        XSObjectList facets = byteType.getFacets();
        for (int i = 0; i < facets.getLength(); i++) {
            XSFacet f = (XSFacet) facets.item(i);
            if (f.getFacetKind() == FACET_MAXINCLUSIVE && "127".equals(f.getLexicalFacetValue())) {
                maxInclusiveFacet = f;
                break;
            }
        }

        assertNotNull(maxInclusiveFacet);
        assertTrue(Xerces.isDefaultFacet(byteType, maxInclusiveFacet));
    }

    private static XSModel loadModel(String schemaText) throws Exception {
        var msgs = new ArrayList<String>();
        try (var in = new ByteArrayInputStream(schemaText.getBytes(StandardCharsets.UTF_8))) {
            var model = XMLSchema.xsmodelFromStream(in, msgs);
            assertNotNull(model, () -> "XSModel was null. Messages: " + msgs);
            return model;
        }
    }
}
