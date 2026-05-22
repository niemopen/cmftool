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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.cmf.ModelXMLReader;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mitre.niem.xml.XMLSchemaDocument.makeURI;

@Execution(ExecutionMode.SAME_THREAD)
class XMLMsgToJSONTest {
    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final static String resDN = "src/test/resources/";

    private static final String NS = "urn:test:model";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    private Model model;
    private XMLMsgToJSON converter;
    private LogCaptor logCaptor;

    private final Map<String, Property> propertiesByUri = new HashMap<>();
    private final Map<String, Property> propertiesByQname = new HashMap<>();
    private final Map<String, Namespace> namespacesByUri = new HashMap<>();

    private ClassType rootClass;

    @BeforeEach
    void setUp() {
        propertiesByUri.clear();
        propertiesByQname.clear();
        namespacesByUri.clear();

        model = mock(Model.class);

        when(model.uriToProperty(anyString()))
            .thenAnswer(inv -> propertiesByUri.get(inv.getArgument(0, String.class)));

        when(model.qnToProperty(anyString()))
            .thenAnswer(inv -> propertiesByQname.get(inv.getArgument(0, String.class)));

        when(model.namespaceObj(anyString()))
            .thenAnswer(inv -> namespacesByUri.get(inv.getArgument(0, String.class)));

        Namespace ns = mock(Namespace.class);
        when(ns.isAugmentation(anyString())).thenReturn(false);
        namespacesByUri.put(NS, ns);

        rootClass = newClassType();
        registerRootMessage("Message", "m:Message", rootClass);

        converter = new XMLMsgToJSON(model);

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
    public void testLiteral() throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "literal.cmf"));        
        var xmlF   = new File(resDN, "literal.xml");
        var xmlIS  = new InputSource(new FileInputStream(xmlF));
        var jsonW  = new StringWriter();
        var tran   = new XMLMsgToJSON(model);
        var jobj   = new JsonObject();
        var status = tran.convert(xmlIS, jobj);
        var jmsg   = gson.toJson(jobj);
        
        var m = jobj.getAsJsonObject("t:Message");
        var na = m.getAsJsonArray("nc:PersonName");
        var n1 = na.get(0).getAsJsonObject();
        var gn = n1.getAsJsonObject("nc:PersonGivenName");
        assertEquals("Peter", gn.getAsJsonPrimitive("nc:TextLiteral").getAsString());
        assertEquals("foo", gn.getAsJsonPrimitive("nc:personNameCommentText").getAsString());
    }
    
    @Test
    void convert_validSimpleDataProperty_returnsOkAndWritesPrimitive()
        throws ParserConfigurationException, SAXException, IOException {

        Datatype stringType = newDatatype("TextType", "string");
        registerDataProperty(NS, "Text", "m:Text", rootClass, stringType, false);

        JsonObject json = new JsonObject();
        int status = converter.convert(input(
            "<m:Message xmlns:m=\"" + NS + "\">" +
                "<m:Text>Hello</m:Text>" +
            "</m:Message>"
        ), json);

        assertEquals(XMLMsgToJSON.CONVERT_OK, status);
        assertTrue(json.has("m:Message"));

        JsonObject root = json.get("m:Message").getAsJsonObject();
        assertEquals("Hello", root.get("m:Text").getAsString());

        assertNoWarningsOrErrors();
    }

    @Test
    void convert_nestedObjectProperty_buildsNestedJsonObject()
        throws ParserConfigurationException, SAXException, IOException {

        Datatype stringType = newDatatype("TextType", "string");
        ClassType addressClass = newClassType();

        registerObjectProperty(NS, "Address", "m:Address", rootClass, addressClass, false);
        registerDataProperty(NS, "Street", "m:Street", addressClass, stringType, false);

        JsonObject json = new JsonObject();
        int status = converter.convert(input(
            "<m:Message xmlns:m=\"" + NS + "\">" +
                "<m:Address>" +
                    "<m:Street>Main St</m:Street>" +
                "</m:Address>" +
            "</m:Message>"
        ), json);

        assertEquals(XMLMsgToJSON.CONVERT_OK, status);

        JsonObject root = json.get("m:Message").getAsJsonObject();
        JsonObject address = root.get("m:Address").getAsJsonObject();
        assertEquals("Main St", address.get("m:Street").getAsString());

        assertNoWarningsOrErrors();
    }

    @Test
    void convert_repeatableProperty_writesJsonArray()
        throws ParserConfigurationException, SAXException, IOException {

        Datatype stringType = newDatatype("TextType", "string");
        registerDataProperty(NS, "Text", "m:Text", rootClass, stringType, true);

        JsonObject json = new JsonObject();
        int status = converter.convert(input(
            "<m:Message xmlns:m=\"" + NS + "\">" +
                "<m:Text>A</m:Text>" +
                "<m:Text>B</m:Text>" +
            "</m:Message>"
        ), json);

        assertEquals(XMLMsgToJSON.CONVERT_OK, status);

        JsonObject root = json.get("m:Message").getAsJsonObject();
        JsonArray textArray = root.get("m:Text").getAsJsonArray();

        assertEquals(2, textArray.size());
        assertEquals("A", textArray.get(0).getAsString());
        assertEquals("B", textArray.get(1).getAsString());

        assertNoWarningsOrErrors();
    }

    @Test
    void convert_numericDataProperty_writesJsonNumber()
        throws ParserConfigurationException, SAXException, IOException {

        Datatype intType = newDatatype("IntType", "int");
        registerDataProperty(NS, "Count", "m:Count", rootClass, intType, false);

        JsonObject json = new JsonObject();
        int status = converter.convert(input(
            "<m:Message xmlns:m=\"" + NS + "\">" +
                "<m:Count>42</m:Count>" +
            "</m:Message>"
        ), json);

        assertEquals(XMLMsgToJSON.CONVERT_OK, status);

        JsonPrimitive value = json.get("m:Message").getAsJsonObject()
            .get("m:Count").getAsJsonPrimitive();

        assertTrue(value.isNumber());
        assertEquals(42, value.getAsInt());

        assertNoWarningsOrErrors();
    }

    @Test
    void convert_booleanDataProperty_writesJsonBoolean()
        throws ParserConfigurationException, SAXException, IOException {

        Datatype booleanType = newDatatype("BooleanType", "boolean");
        registerDataProperty(NS, "Flag", "m:Flag", rootClass, booleanType, false);

        JsonObject json = new JsonObject();
        int status = converter.convert(input(
            "<m:Message xmlns:m=\"" + NS + "\">" +
                "<m:Flag>true</m:Flag>" +
            "</m:Message>"
        ), json);

        assertEquals(XMLMsgToJSON.CONVERT_OK, status);

        JsonPrimitive value = json.get("m:Message").getAsJsonObject()
            .get("m:Flag").getAsJsonPrimitive();

        assertTrue(value.isBoolean());
        assertTrue(value.getAsBoolean());

        assertNoWarningsOrErrors();
    }

    @Test
    void convert_invalidBoolean_returnsWarnAndWritesFalse()
        throws ParserConfigurationException, SAXException, IOException {

        Datatype booleanType = newDatatype("BooleanType", "boolean");
        registerDataProperty(NS, "Flag", "m:Flag", rootClass, booleanType, false);

        JsonObject json = new JsonObject();
        int status = converter.convert(input(
            "<m:Message xmlns:m=\"" + NS + "\">" +
                "<m:Flag>maybe</m:Flag>" +
            "</m:Message>"
        ), json);

        assertEquals(XMLMsgToJSON.CONVERT_WARN, status);

        JsonPrimitive value = json.get("m:Message").getAsJsonObject()
            .get("m:Flag").getAsJsonPrimitive();

        assertTrue(value.isBoolean());
        assertFalse(value.getAsBoolean());

        assertWarnLogged("is not a valid xs:boolean");
        assertNoErrors();
    }

    @Test
    void convert_usesModelQNamesNotInputPrefixes()
        throws ParserConfigurationException, SAXException, IOException {

        Datatype stringType = newDatatype("TextType", "string");
        registerDataProperty(NS, "Text", "m:Text", rootClass, stringType, false);

        JsonObject json = new JsonObject();
        int status = converter.convert(input(
            "<x:Message xmlns:x=\"" + NS + "\" xmlns:y=\"" + NS + "\">" +
                "<y:Text>Hello</y:Text>" +
            "</x:Message>"
        ), json);

        assertEquals(XMLMsgToJSON.CONVERT_OK, status);
        assertTrue(json.has("m:Message"));
        assertFalse(json.has("x:Message"));

        JsonObject root = json.get("m:Message").getAsJsonObject();
        assertTrue(root.has("m:Text"));
        assertFalse(root.has("y:Text"));
        assertEquals("Hello", root.get("m:Text").getAsString());

        assertNoWarningsOrErrors();
    }

    @Test
    void convert_unknownElementOutsideAdapter_isIgnoredAndReturnsWarn()
        throws ParserConfigurationException, SAXException, IOException {

        Datatype stringType = newDatatype("TextType", "string");
        registerDataProperty(NS, "Text", "m:Text", rootClass, stringType, false);

        JsonObject json = new JsonObject();
        int status = converter.convert(input(
            "<m:Message xmlns:m=\"" + NS + "\">" +
                "<m:Text>ok</m:Text>" +
                "<m:Unknown>ignore me</m:Unknown>" +
            "</m:Message>"
        ), json);

        assertEquals(XMLMsgToJSON.CONVERT_WARN, status);

        JsonObject root = json.get("m:Message").getAsJsonObject();
        assertEquals("ok", root.get("m:Text").getAsString());
        assertFalse(root.has("m:Unknown"));

        assertWarnLogged("unknown element");
        assertNoErrors();
    }

    @Test
    void convert_xsiNil_isIgnoredWithoutWarning()
        throws ParserConfigurationException, SAXException, IOException {

        Datatype stringType = newDatatype("TextType", "string");
        registerDataProperty(NS, "Text", "m:Text", rootClass, stringType, false);

        JsonObject json = new JsonObject();
        int status = converter.convert(input(
            "<m:Message xmlns:m=\"" + NS + "\" xmlns:xsi=\"" + XSI_NS + "\">" +
                "<m:Text xsi:nil=\"true\"/>" +
            "</m:Message>"
        ), json);

        assertEquals(XMLMsgToJSON.CONVERT_OK, status);

        JsonObject root = json.get("m:Message").getAsJsonObject();
        assertTrue(root.has("m:Text"));
        assertEquals("", root.get("m:Text").getAsString());

        assertNoWarningsOrErrors();
    }

    @Test
    void convert_reusedInstanceAfterWarning_shouldResetStatusPerConversion()
        throws ParserConfigurationException, SAXException, IOException {

        Datatype stringType = newDatatype("TextType", "string");
        registerDataProperty(NS, "Text", "m:Text", rootClass, stringType, false);

        JsonObject first = new JsonObject();
        int firstStatus = converter.convert(input(
            "<m:Message xmlns:m=\"" + NS + "\">" +
                "<m:Unknown>bad</m:Unknown>" +
            "</m:Message>"
        ), first);

        JsonObject second = new JsonObject();
        int secondStatus = converter.convert(input(
            "<m:Message xmlns:m=\"" + NS + "\">" +
                "<m:Text>good</m:Text>" +
            "</m:Message>"
        ), second);

        assertEquals(XMLMsgToJSON.CONVERT_WARN, firstStatus);
        assertEquals(XMLMsgToJSON.CONVERT_OK, secondStatus);
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

    private void registerRootMessage(String localName, String qname, ClassType classType) {
        Property p = newProperty(qname);
        when(p.isObjectProperty()).thenReturn(true);
        when(p.isDataProperty()).thenReturn(false);
        when(p.classType()).thenReturn(classType);

        propertiesByUri.put(makeURI(NS, localName), p);
        propertiesByQname.put(qname, p);
    }

    private Property registerDataProperty(
        String ns,
        String localName,
        String qname,
        ClassType parentType,
        Datatype datatype,
        boolean repeatable
    ) {
        Property p = newProperty(qname);
        when(p.isDataProperty()).thenReturn(true);
        when(p.isObjectProperty()).thenReturn(false);
        when(p.datatype()).thenReturn(datatype);

        propertiesByUri.put(makeURI(ns, localName), p);
        propertiesByQname.put(qname, p);

        when(parentType.isRepeatableProperty(p)).thenReturn(repeatable);
        return p;
    }

    private Property registerObjectProperty(
        String ns,
        String localName,
        String qname,
        ClassType parentType,
        ClassType childType,
        boolean repeatable
    ) {
        Property p = newProperty(qname);
        when(p.isObjectProperty()).thenReturn(true);
        when(p.isDataProperty()).thenReturn(false);
        when(p.classType()).thenReturn(childType);

        propertiesByUri.put(makeURI(ns, localName), p);
        propertiesByQname.put(qname, p);

        when(parentType.isRepeatableProperty(p)).thenReturn(repeatable);
        return p;
    }

    private Property newProperty(String qname) {
        Property p = mock(Property.class);
        when(p.qname()).thenReturn(qname);
        when(p.isAttribute()).thenReturn(false);
        when(p.isRefAttribute()).thenReturn(false);
        when(p.isRelationship()).thenReturn(false);
        when(p.isObjectProperty()).thenReturn(false);
        when(p.isDataProperty()).thenReturn(false);
        when(p.classType()).thenReturn(null);
        when(p.datatype()).thenReturn(null);
        return p;
    }

    private ClassType newClassType() {
        ClassType ct = mock(ClassType.class);
        when(ct.subClassOf()).thenReturn(null);
        when(ct.literalDataProperty()).thenReturn(null);
        when(ct.hasXmlLang()).thenReturn(false);
        when(ct.isAdapterClass()).thenReturn(false);
        return ct;
    }

    private Datatype newDatatype(String typeName, String baseName) {
        Datatype dt = mock(Datatype.class);
        when(dt.name()).thenReturn(typeName);
        when(dt.facetL()).thenReturn(null);
        when(dt.getType()).thenReturn(-1);

        Datatype base = mock(Datatype.class);
        when(base.name()).thenReturn(baseName);
        when(dt.baseXS()).thenReturn(base);

        return dt;
    }

    private InputSource input(String xml) {
        InputSource src = new InputSource(new StringReader(xml));
        src.setSystemId("memory:test.xml");
        return src;
    }
}
