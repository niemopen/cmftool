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
package org.mitre.niem.translate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.StringReader;
import javax.xml.XMLConstants;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mitre.niem.cmf.AugmentRecord;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.DataProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.PropertyAssociation;
import org.mitre.niem.cmf.Union;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.SAME_THREAD)
class XMLMsgToJSONTest {

    private static final String RES_DN = "src/test/resources/";
    private static final String TEST_NS = "http://example.com/test/";
    private static final String AUG_NS = "http://example.com/aug/";

    private LogCaptor logCaptor;

    @BeforeEach
    void setUp() {
        logCaptor = LogCaptor.forClass(XMLMsgToJSON.class);
        logCaptor.clearLogs();
    }

    @AfterEach
    void tearDown() {
        if (logCaptor != null) {
            logCaptor.close();
        }
    }

    @Test
    void testList() throws Exception {
        var model = readModel("list.cmf");
        var xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <t:ObjectElement
              xmlns:t="http://example.com/Test/">
              <t:SimpleElement>1 2 3</t:SimpleElement>
              <t:SimpleElement>4 5</t:SimpleElement>
              <t:ListDataElement>6 7 8</t:ListDataElement>
              <t:ListDataElement>6 7 88</t:ListDataElement>
              <t:ListObjectElement t:myAtt="foo">9 10 11</t:ListObjectElement>
            </t:ObjectElement>
            """;
        var expected = """
            {
              "t:ObjectElement": {
                "t:SimpleElement": [ "1 2 3", "4 5" ],
                "t:ListDataElement": [
                  [ 6, 7, 8 ],
                  [ 6, 7, 88 ]
                ],
                "t:ListObjectElement": {
                  "t:myAtt": "foo",
                  "t:ListObjectLiteral": [ 9, 10, 11 ]
                }
              }
            }
            """;

        var result = convert(model, xml);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals(expected, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testLiteral() throws Exception {
        var model = readModel("literal.cmf");
        var xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <t:Message
             xmlns:nc="https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"
             xmlns:t="http://example.com/test/">
              <nc:PersonName>
                <nc:PersonGivenName nc:personNameCommentText="foo">Peter</nc:PersonGivenName>
                <nc:PersonMiddleName>Death</nc:PersonMiddleName>
                <nc:PersonMiddleName>Bredon</nc:PersonMiddleName>
                <nc:PersonSurName>Wimsey</nc:PersonSurName>
              </nc:PersonName>
            </t:Message>
            """;
        var expected = """
            {
              "t:Message": {
                "nc:PersonName": [
                  {
                    "nc:PersonGivenName": {
                      "nc:personNameCommentText": "foo",
                      "nc:TextLiteral": "Peter"
                    },
                    "nc:PersonMiddleName": [
                      { "nc:TextLiteral": "Death" },
                      { "nc:TextLiteral": "Bredon" }
                    ],
                    "nc:PersonSurName": { "nc:TextLiteral": "Wimsey" }
                  }
                ]
              }
            }
            """;

        var result = convert(model, xml);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals(expected, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testUnknownElementIsIgnoredAndSetsWarnStatus() throws Exception {
        var model = readModel("list.cmf");
        var xml = """
            <t:ObjectElement xmlns:t="http://example.com/Test/">
              <t:UnknownElement>
                <t:ListObjectElement t:myAtt="nope">1 2</t:ListObjectElement>
              </t:UnknownElement>
              <t:ListObjectElement t:myAtt="foo">9 10 11</t:ListObjectElement>
            </t:ObjectElement>
            """;
        var expected = """
            {
              "t:ObjectElement": {
                "t:ListObjectElement": {
                  "t:myAtt": "foo",
                  "t:ListObjectLiteral": [ 9, 10, 11 ]
                }
              }
            }
            """;

        var result = convert(model, xml);

        assertEquals(XMLMsgToJSON.CONVERT_WARN, result.status);
        assertJsonEquals(expected, result.json);
        assertWarnLogged("unknown element t:UnknownElement");
        assertNoErrors();
    }

    @Test
    void testUnknownAttributeIsIgnoredAndSetsWarnStatus() throws Exception {
        var model = readModel("list.cmf");
        var xml = """
            <t:ObjectElement xmlns:t="http://example.com/Test/">
              <t:ListObjectElement t:bogus="x">9 10 11</t:ListObjectElement>
            </t:ObjectElement>
            """;
        var expected = """
            {
              "t:ObjectElement": {
                "t:ListObjectElement": {
                  "t:ListObjectLiteral": [ 9, 10, 11 ]
                }
              }
            }
            """;

        var result = convert(model, xml);

        assertEquals(XMLMsgToJSON.CONVERT_WARN, result.status);
        assertJsonEquals(expected, result.json);
        assertWarnLogged("unknown attribute t:bogus");
        assertNoErrors();
    }

    @Test
    void testXsiNilAttributeIsIgnoredWithoutWarning() throws Exception {
        var model = readModel("list.cmf");
        var xml = """
            <t:ObjectElement
              xmlns:t="http://example.com/Test/"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <t:ListObjectElement xsi:nil="true">9 10 11</t:ListObjectElement>
            </t:ObjectElement>
            """;
        var expected = """
            {
              "t:ObjectElement": {
                "t:ListObjectElement": {
                  "t:ListObjectLiteral": [ 9, 10, 11 ]
                }
              }
            }
            """;

        var result = convert(model, xml);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals(expected, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testInteriorXmlBaseLogsWarningAndIsIgnored() throws Exception {
        var model = readModel("list.cmf");
        var xml = """
            <t:ObjectElement xmlns:t="http://example.com/Test/">
              <t:ListObjectElement xml:base="http://example.org/base/" t:myAtt="foo">9 10 11</t:ListObjectElement>
            </t:ObjectElement>
            """;
        var expected = """
            {
              "t:ObjectElement": {
                "t:ListObjectElement": {
                  "t:myAtt": "foo",
                  "t:ListObjectLiteral": [ 9, 10, 11 ]
                }
              }
            }
            """;

        var result = convert(model, xml);

        assertEquals(XMLMsgToJSON.CONVERT_WARN, result.status);
        assertJsonEquals(expected, result.json);
        assertWarnLogged("xml:base on interior element");
        assertNoErrors();
    }

    @Test
    void testOutputUsesModelQNameNotInputPrefix() throws Exception {
        var model = readModel("list.cmf");
        var xml = """
            <x:ObjectElement xmlns:x="http://example.com/Test/">
              <x:ListObjectElement>9 10</x:ListObjectElement>
            </x:ObjectElement>
            """;
        var expected = """
            {
              "t:ObjectElement": {
                "t:ListObjectElement": {
                  "t:ListObjectLiteral": [ 9, 10 ]
                }
              }
            }
            """;

        var result = convert(model, xml);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals(expected, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testConverterCanBeReusedForMultipleMessages() throws Exception {
        var model = readModel("list.cmf");
        var tran = new XMLMsgToJSON(model);

        var result1 = convert(tran, """
            <t:ObjectElement xmlns:t="http://example.com/Test/">
              <t:ListObjectElement>1 2</t:ListObjectElement>
            </t:ObjectElement>
            """);

        var result2 = convert(tran, """
            <t:ObjectElement xmlns:t="http://example.com/Test/">
              <t:ListObjectElement>3 4 5</t:ListObjectElement>
            </t:ObjectElement>
            """);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result1.status);
        assertEquals(XMLMsgToJSON.CONVERT_OK, result2.status);

        assertJsonEquals("""
            {
              "t:ObjectElement": {
                "t:ListObjectElement": {
                  "t:ListObjectLiteral": [ 1, 2 ]
                }
              }
            }
            """, result1.json);

        assertJsonEquals("""
            {
              "t:ObjectElement": {
                "t:ListObjectElement": {
                  "t:ListObjectLiteral": [ 3, 4, 5 ]
                }
              }
            }
            """, result2.json);

        assertNoWarningsOrErrors();
    }

    @Test
    void testMalformedXmlThrowsSAXException() throws Exception {
        var model = readModel("list.cmf");
        var tran = new XMLMsgToJSON(model);
        var out = new JsonObject();
        var xml = new InputSource(new StringReader("""
            <t:ObjectElement xmlns:t="http://example.com/Test/">
              <t:ListObjectElement>1 2</t:ListObjectElement>
            """));

        assertThrows(SAXException.class, () -> tran.convert(xml, out));
    }

    @Test
    void testBooleanTrueFalseOneZeroConvertCorrectly() throws Exception {
        var parts = createBaseModel();
        var xsBoolean = addXsdDatatype(parts, "boolean");
        addDataProperty(parts.messageType, parts.tNs, "Flag", xsBoolean, "1");

        logCaptor.clearLogs();
        var resultTrue = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Flag>true</t:Flag>
            </t:Message>
            """);
        assertEquals(XMLMsgToJSON.CONVERT_OK, resultTrue.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Flag": true
              }
            }
            """, resultTrue.json);
        assertNoWarningsOrErrors();

        logCaptor.clearLogs();
        var resultFalse = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Flag>false</t:Flag>
            </t:Message>
            """);
        assertEquals(XMLMsgToJSON.CONVERT_OK, resultFalse.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Flag": false
              }
            }
            """, resultFalse.json);
        assertNoWarningsOrErrors();

        logCaptor.clearLogs();
        var resultOne = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Flag>1</t:Flag>
            </t:Message>
            """);
        assertEquals(XMLMsgToJSON.CONVERT_OK, resultOne.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Flag": true
              }
            }
            """, resultOne.json);
        assertNoWarningsOrErrors();

        logCaptor.clearLogs();
        var resultZero = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Flag>0</t:Flag>
            </t:Message>
            """);
        assertEquals(XMLMsgToJSON.CONVERT_OK, resultZero.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Flag": false
              }
            }
            """, resultZero.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testInvalidBooleanWarnsAndDefaultsFalse() throws Exception {
        var parts = createBaseModel();
        var xsBoolean = addXsdDatatype(parts, "boolean");
        addDataProperty(parts.messageType, parts.tNs, "Flag", xsBoolean, "1");

        var result = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Flag>maybe</t:Flag>
            </t:Message>
            """);

        assertEquals(XMLMsgToJSON.CONVERT_WARN, result.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Flag": false
              }
            }
            """, result.json);
        assertWarnLogged("is not a valid xs:boolean");
        assertNoErrors();
    }

    @Test
    void testNumericDatatypeProducesJsonNumber() throws Exception {
        var parts = createBaseModel();
        var xsInt = addXsdDatatype(parts, "int");
        addDataProperty(parts.messageType, parts.tNs, "Count", xsInt, "1");

        var result = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Count>42</t:Count>
            </t:Message>
            """);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Count": 42
              }
            }
            """, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testUnionWithNumericMemberAndNumericLexicalValueProducesNumber() throws Exception {
        var parts = createBaseModel();
        var xsString = addXsdDatatype(parts, "string");
        var xsDecimal = addXsdDatatype(parts, "decimal");

        var union = new Union(parts.tNs, "NumberishUnionType");
        union.addMember(xsString);
        union.addMember(xsDecimal);
        parts.model.addDatatype(union);

        addDataProperty(parts.messageType, parts.tNs, "Value", union, "1");

        var result = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Value>12.34</t:Value>
            </t:Message>
            """);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Value": 12.34
              }
            }
            """, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testUnionWithNumericMemberAndNonNumericLexicalValueProducesString() throws Exception {
        var parts = createBaseModel();
        var xsString = addXsdDatatype(parts, "string");
        var xsDecimal = addXsdDatatype(parts, "decimal");

        var union = new Union(parts.tNs, "NumberishUnionType");
        union.addMember(xsString);
        union.addMember(xsDecimal);
        parts.model.addDatatype(union);

        addDataProperty(parts.messageType, parts.tNs, "Value", union, "1");

        var result = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Value>ABC</t:Value>
            </t:Message>
            """);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Value": "ABC"
              }
            }
            """, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testCodeTypeRemainsStringEvenIfNumericLooking() throws Exception {
        var parts = createBaseModel();
        var codeType = new Datatype(parts.tNs, "CountryCodeType");
        parts.model.addDatatype(codeType);
        addDataProperty(parts.messageType, parts.tNs, "CountryCode", codeType, "1");

        var result = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:CountryCode>123</t:CountryCode>
            </t:Message>
            """);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:CountryCode": "123"
              }
            }
            """, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testSingleOccurrenceOfRepeatablePropertyStillCreatesArray() throws Exception {
        var parts = createBaseModel();
        var xsString = addXsdDatatype(parts, "string");
        addDataProperty(parts.messageType, parts.tNs, "Item", xsString, "2");

        var result = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Item>only</t:Item>
            </t:Message>
            """);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Item": [ "only" ]
              }
            }
            """, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testReferenceAttributeCreatesArrayOfIdObjects() throws Exception {
        var parts = createBaseModel();

        var holderType = new ClassType(parts.tNs, "HolderType");
        parts.model.addClassType(holderType);
        addObjectProperty(parts.messageType, parts.tNs, "Holder", holderType, "1");

        var xsString = addXsdDatatype(parts, "string");

        var fooProp = new DataProperty(parts.tNs, "Foo");
        fooProp.setDatatype(xsString);
        parts.model.addProperty(fooProp);

        var fooRefAttr = new DataProperty(parts.tNs, "fooRef");
        fooRefAttr.setIsAttribute(true);
        fooRefAttr.setIsRefAttribute(true);
        fooRefAttr.setDatatype(xsString);
        parts.model.addProperty(fooRefAttr);

        var result = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Holder t:fooRef="id1 id2"/>
            </t:Message>
            """);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Holder": {
                  "t:Foo": [
                    { "@id": "#id1" },
                    { "@id": "#id2" }
                  ]
                }
              }
            }
            """, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testRelationshipPropertiesMovedUnderAnnotation() throws Exception {
        var parts = createBaseModel();
        var xsString = addXsdDatatype(parts, "string");

        var holderType = new ClassType(parts.tNs, "HolderType");
        parts.model.addClassType(holderType);
        addObjectProperty(parts.messageType, parts.tNs, "Holder", holderType, "1");

        var relatedType = addLiteralClass(parts.tNs, "RelatedType", "RelatedLiteral", xsString);
        var relatedProp = addObjectProperty(holderType, parts.tNs, "RelatedItem", relatedType, "1");
        relatedProp.setIsRelationship(true);

        var result = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/">
              <t:Holder>
                <t:RelatedItem>abc</t:RelatedItem>
              </t:Holder>
            </t:Message>
            """);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "t:Holder": {
                  "@annotation": {
                    "t:RelatedItem": {
                      "t:RelatedLiteral": "abc"
                    }
                  }
                }
              }
            }
            """, result.json);
        assertNoWarningsOrErrors();
    }

    @Test
    void testAugmentationElementMergesChildrenIntoParent() throws Exception {
        var parts = createBaseModel();
        var xsString = addXsdDatatype(parts, "string");

        var augNs = new Namespace("a", AUG_NS);
        parts.model.addNamespace(augNs);

        var extra = new DataProperty(augNs, "Extra");
        extra.setDatatype(xsString);
        parts.model.addProperty(extra);

        var ar = new AugmentRecord();
        ar.setClassType(parts.messageType);
        augNs.addAugmentRecord(ar);

        var result = convert(parts.model, """
            <t:Message xmlns:t="http://example.com/test/" xmlns:a="http://example.com/aug/">
              <a:MessageAugmentation>
                <a:Extra>foo</a:Extra>
              </a:MessageAugmentation>
            </t:Message>
            """);

        assertEquals(XMLMsgToJSON.CONVERT_OK, result.status);
        assertJsonEquals("""
            {
              "t:Message": {
                "a:Extra": "foo"
              }
            }
            """, result.json);
        assertNoWarningsOrErrors();
    }

    private Model readModel(String cmfName) throws Exception {
        return new ModelXMLReader().readFiles(new File(RES_DN, cmfName));
    }

    private ConversionResult convert(Model model, String xml) throws Exception {
        return convert(new XMLMsgToJSON(model), xml);
    }

    private ConversionResult convert(XMLMsgToJSON tran, String xml) throws Exception {
        var xmlIS = new InputSource(new StringReader(xml));
        var jobj = new JsonObject();
        var status = tran.convert(xmlIS, jobj);
        return new ConversionResult(status, jobj);
    }

    private void assertJsonEquals(String expected, JsonObject actual) {
        assertEquals(JsonParser.parseString(expected).getAsJsonObject(), actual);
    }

    private void assertWarnLogged(String expectedFragment) {
        assertTrue(
            logCaptor.getWarnLogs().stream().anyMatch(msg -> msg.contains(expectedFragment)),
            () -> "Expected WARN containing [" + expectedFragment + "], but got: " + logCaptor.getWarnLogs()
        );
    }

    private void assertNoWarningsOrErrors() {
        assertTrue(
            logCaptor.getWarnLogs().isEmpty(),
            () -> "Expected no WARN logs, but got: " + logCaptor.getWarnLogs()
        );
        assertTrue(
            logCaptor.getErrorLogs().isEmpty(),
            () -> "Expected no ERROR logs, but got: " + logCaptor.getErrorLogs()
        );
    }

    private void assertNoErrors() {
        assertTrue(
            logCaptor.getErrorLogs().isEmpty(),
            () -> "Expected no ERROR logs, but got: " + logCaptor.getErrorLogs()
        );
    }

    private TestModelParts createBaseModel() throws Exception {
        var parts = new TestModelParts();
        parts.model = new Model();
        parts.tNs = new Namespace("t", TEST_NS);
        parts.model.addNamespace(parts.tNs);

        parts.xsNs = parts.model.namespaceObj(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        assertNotNull(parts.xsNs);

        parts.messageType = new ClassType(parts.tNs, "MessageType");
        parts.model.addClassType(parts.messageType);

        parts.messageProp = new ObjectProperty(parts.tNs, "Message");
        parts.messageProp.setClassType(parts.messageType);
        parts.model.addProperty(parts.messageProp);

        return parts;
    }

    private Datatype addXsdDatatype(TestModelParts parts, String localName) {
        var dt = new Datatype(parts.xsNs, localName);
        parts.model.addDatatype(dt);
        return dt;
    }

    private DataProperty addDataProperty(ClassType owner, Namespace ns, String localName, Datatype datatype, String maxOccurs) {
        var prop = new DataProperty(ns, localName);
        prop.setDatatype(datatype);
        owner.model().addProperty(prop);
        associate(owner, prop, maxOccurs);
        return prop;
    }

    private ObjectProperty addObjectProperty(ClassType owner, Namespace ns, String localName, ClassType type, String maxOccurs) {
        var prop = new ObjectProperty(ns, localName);
        prop.setClassType(type);
        owner.model().addProperty(prop);
        associate(owner, prop, maxOccurs);
        return prop;
    }

    private ClassType addLiteralClass(Namespace ns, String className, String literalPropName, Datatype literalDatatype) {
        var model = literalDatatype.model();
        var ct = new ClassType(ns, className);
        model.addClassType(ct);

        var literal = new DataProperty(ns, literalPropName);
        literal.setDatatype(literalDatatype);
        model.addProperty(literal);

        associate(ct, literal, "1");
        return ct;
    }

    private void associate(ClassType owner, Property prop, String maxOccurs) {
        var pa = new PropertyAssociation();
        pa.setProperty(prop);
        pa.setMaxOccurs(maxOccurs);
        owner.addPropertyAssociation(pa);
    }

    private static final class ConversionResult {
        final int status;
        final JsonObject json;

        ConversionResult(int status, JsonObject json) {
            this.status = status;
            this.json = json;
        }
    }

    private static final class TestModelParts {
        Model model;
        Namespace tNs;
        Namespace xsNs;
        ClassType messageType;
        ObjectProperty messageProp;
    }
}

