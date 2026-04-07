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
package org.mitre.niem.json;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.cmf.ModelXMLReader;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToJSONSchemaTest {
    
    private static final String RDIR = "src/test/resources";
    private static final Configuration JSG = Configuration.builder()
        .jsonProvider(new GsonJsonProvider())
        .mappingProvider(new GsonMappingProvider())
        .build();
    
    public ModelToJSONSchemaTest() {
    }
    
//    @Test
//    public void testITL () throws Exception {
//        var rdr   = new ModelXMLReader();
//        var model = rdr.readFiles(new File("C:\\Work\\im26\\Biometrics\\itl-2015\\itl.cmf"));
//        var js    = new ModelToJSONSchema(model);
//        js.setNoPrefix(true);
//        var w     = new StringWriter();
//        List<Property> msgPL = Arrays.asList(model.qnToProperty("itl:NISTBiometricInformationExchangePackage"));
//        js.setMessageProperties(msgPL);
//        js.setContextURI("http://example.com/Request/JSON");
//        js.writeSchema(w);
//        var s = w.toString();
//        assertTrue(schemaValid(s));
//        int x = 0;
//    }    
 
//    @Test
//    public void test () throws Exception {
//        var rdr   = new ModelXMLReader();
//        var model = rdr.readFiles(new File("src/test/resources/json/request.cmf"));
//        var map   = Mapping.readFile(new File("src/test/resources/json/map-request.ttl"));
//        var js    = new ModelToJSONSchema(model);
//        js.setMapping(map);
//        js.setNoPrefix(true);
//        var w     = new StringWriter();
//        List<Property> msgPL = Arrays.asList(model.qnToProperty("msg:Request"));
//        js.setMessageProperties(msgPL);
//        js.setContextURI("http://example.com/Request/JSON");
//        js.writeSchema(w);
//        var s     = w.toString();
//        assertTrue(schemaValid(s));
//        int x = 0;
//    }
    
    @Test
    public void testTwoChoice () {
        var sch  = makeSchema("json/twoChoice.cmf");
        var defs = sch.getAsJsonObject("definitions");
        var msgT = defs.getAsJsonObject("t:MessageType");
        var prop = msgT.getAsJsonObject("properties");
        var ctx  = JsonPath.using(JSG).parse(prop);
        
        assertEquals("array", ctx.read("$.t:FooString.type", String.class));
        assertEquals("array", ctx.read("$.t:FooToken.type", String.class));
        assertEquals("#/definitions/xs:string", ctx.read("$.t:FooString.items.$ref", String.class));
        assertEquals("#/definitions/xs:token", ctx.read("$.t:FooToken.items.$ref", String.class));    
        assertEquals(10, ctx.read("$.t:FooString.maxItems", Integer.class).intValue());
        assertEquals(10, ctx.read("$.t:FooToken.maxItems", Integer.class).intValue());
        
        assertEquals("#/definitions/xs:string", ctx.read("$.t:BarString.$ref", String.class));
        assertEquals("#/definitions/xs:token", ctx.read("$.t:BarToken.$ref", String.class));
        assertEquals("#/definitions/xs:token", ctx.read("$.t:BugToken.$ref", String.class));
        assertEquals("#/definitions/xs:token", ctx.read("$.t:OptToken.$ref", String.class));       
        
        JsonArray res;
        ctx = JsonPath.using(JSG).parse(msgT);
        assertEquals(2, msgT.getAsJsonArray("allOf").size());
        res = ctx.read("$.allOf[?('t:BarToken' in @.anyOf[*].required[0] && 't:BarString' in @.anyOf[*].required[0])]");
        assertEquals(1, res.size());
        res = ctx.read("$.allOf[?('t:FooToken' in @.anyOf[*].required[0] && 't:FooString' in @.anyOf[*].required[0])]");
        assertEquals(1, res.size());
        
        res = ctx.read("$.required");
        assertEquals(1, res.size());
        
        assertFalse(msgT.get("additionalProperties").getAsBoolean());
    }
    
    /**
     * Validates the JSON‑Schema generated from {@code json/oneChoice.cmf}.
     * The expected fragment (excerpt) is:
     *
     * <pre>
     * "definitions": {
     *   "t:MessageType": {
     *     "type": "object",
     *     "properties": {
     *       "t:FooString": { "type":"array","items":{"$ref":"#/definitions/xs:string"},"maxItems":10 },
     *       "t:FooToken":  { "type":"array","items":{"$ref":"#/definitions/xs:token"},"maxItems":10 },
     *       "t:BarToken":  { "$ref":"#/definitions/xs:token" }
     *     },
     *     "anyOf": [
     *       { "required":["t:FooString"] },
     *       { "required":["t:FooToken"] }
     *     ],
     *     "required": ["t:BarToken"],
     *     "additionalProperties": false
     *   },
     *   "xs:string": { "type":"string" },
     *   "xs:token":  { "type":"string" }
     * }
     * </pre>
     */
    @Test
    public void testOneChoice () {
        // -------------------------------------------------------------
        // 1️⃣ Build the schema from the CMF model
        // -------------------------------------------------------------
        var sch   = makeSchema("json/oneChoice.cmf");
        var defs  = sch.getAsJsonObject("definitions");
        var msgT  = defs.getAsJsonObject("t:MessageType");
        var props = msgT.getAsJsonObject("properties");
        
        // -------------------------------------------------------------
        // 2️⃣ Verify the three properties (FooString, FooToken, BarToken)
        // -------------------------------------------------------------
        var ctx = JsonPath.using(JSG).parse(props);
        
        // FooString – array of xs:string, maxItems = 10
        assertEquals("array", ctx.read("$.t:FooString.type", String.class));
        assertEquals("#/definitions/xs:string",
                     ctx.read("$.t:FooString.items.$ref", String.class));
        assertEquals(10, ctx.read("$.t:FooString.maxItems", Integer.class).intValue());
        
        // FooToken – array of xs:token, maxItems = 10
        assertEquals("array", ctx.read("$.t:FooToken.type", String.class));
        assertEquals("#/definitions/xs:token",
                     ctx.read("$.t:FooToken.items.$ref", String.class));
        assertEquals(10, ctx.read("$.t:FooToken.maxItems", Integer.class).intValue());
        
        // BarToken – single reference to xs:token
        assertEquals("#/definitions/xs:token",
                     ctx.read("$.t:BarToken.$ref", String.class));
        
        // -------------------------------------------------------------
        // 3️⃣ Verify the anyOf choice block (two alternatives, order‑independent)
        // -------------------------------------------------------------
        // now the whole MessageType object
        ctx = JsonPath.using(JSG).parse(msgT);
        JsonArray anyOf = msgT.getAsJsonArray("anyOf");
        assertEquals(2, anyOf.size(),
                "The anyOf array should contain exactly two alternatives");
        
        // Collect the required property name from each alternative
        var requiredSet = new java.util.HashSet<String>();
        for (var elem : anyOf) {
            var altObj = elem.getAsJsonObject();
            var reqArr = altObj.getAsJsonArray("required");
            // Each alternative must require exactly one property
            assertEquals(1, reqArr.size(),
                    "Each anyOf alternative must have a single required property");
            requiredSet.add(reqArr.get(0).getAsString());
        }
        
        // The two alternatives must be FooString and FooToken, order does not matter
        var expected = java.util.Set.of("t:FooString", "t:FooToken");
        assertEquals(expected, requiredSet,
                "anyOf alternatives should require FooString and FooToken (order‑independent)");
        
        // -------------------------------------------------------------
        // 4️⃣ Verify top‑level required list and additionalProperties flag
        // -------------------------------------------------------------
        JsonArray required = msgT.getAsJsonArray("required");
        assertEquals(1, required.size());
        assertEquals("t:BarToken", required.get(0).getAsString());
        
        assertFalse(msgT.get("additionalProperties").getAsBoolean());
    }
    
    /**
     * Validates the JSON‑Schema generated from {@code json/noChoice.cmf}.
     * The expected fragment (excerpt) is:
     *
     * <pre>
     * "definitions": {
     *   "t:MessageType": {
     *     "type": "object",
     *     "properties": {
     *       "t:FooString": {
     *         "type":"array",
     *         "items":{"$ref":"#/definitions/xs:string"},
     *         "maxItems":10,
     *         "minItems":2
     *       },
     *       "t:BarString": { "$ref":"#/definitions/xs:string" }
     *     },
     *     "required": ["t:FooString","t:BarString"],
     *     "additionalProperties": false
     *   },
     *   "xs:string": { "type":"string" },
     *   "xs:token":  { "type":"string" }
     * }
     * </pre>
     */
    @Test
    public void testNoChoice () {
        // -------------------------------------------------------------
        // 1️⃣ Build the schema from the CMF model (json/noChoice.cmf)
        // -------------------------------------------------------------
        var sch   = makeSchema("json/noChoice.cmf");
        var defs  = sch.getAsJsonObject("definitions");
        var msgT  = defs.getAsJsonObject("t:MessageType");
        var props = msgT.getAsJsonObject("properties");
        
        // -------------------------------------------------------------
        // 2️⃣ Verify the two properties (FooString and BarString)
        // -------------------------------------------------------------
        var ctx = JsonPath.using(JSG).parse(props);
        
        // FooString – array of xs:string, maxItems = 10, minItems = 2
        assertEquals("array", ctx.read("$.t:FooString.type", String.class));
        assertEquals("#/definitions/xs:string",
                     ctx.read("$.t:FooString.items.$ref", String.class));
        assertEquals(10, ctx.read("$.t:FooString.maxItems", Integer.class).intValue());
        assertEquals(2,  ctx.read("$.t:FooString.minItems", Integer.class).intValue());
        
        // BarString – single reference to xs:string
        assertEquals("#/definitions/xs:string",
                     ctx.read("$.t:BarString.$ref", String.class));
        
        // -------------------------------------------------------------
        // 3️⃣ Verify required list and additionalProperties flag
        // -------------------------------------------------------------
        JsonArray required = msgT.getAsJsonArray("required");
        assertEquals(2, required.size());
        // Order is not guaranteed; ensure both required entries are present
        assertTrue(required.contains(new JsonPrimitive("t:FooString")));
        assertTrue(required.contains(new JsonPrimitive("t:BarString")));
        
        assertFalse(msgT.get("additionalProperties").getAsBoolean());
        
        // -------------------------------------------------------------
        // 4️⃣ Verify primitive definitions exist (optional sanity check)
        // -------------------------------------------------------------
        assertTrue(defs.has("xs:string"));
        assertTrue(defs.has("xs:token"));
    }
    
    /**
     * Validates that the reference‑code related definitions contain an {@code @id}
     * property whose value is {@code {"$ref":"#/definitions/xs:anyURI"}} and that
     * no other definitions have an {@code @id} property.
     *
     * The model file {@code json/refCode.cmf} defines three types that are
     * referenceable:
     *   - t:AnyRefType
     *   - t:FourType
     *   - t:URIRefType
     *
     * All other definitions (including primitive XSD types) must *not* have an
     * {@code @id} property.
     */
    @Test
    public void testRefCode () {
        // -------------------------------------------------------------
        // 1️⃣ Build the schema from the CMF model that contains reference‑code types
        // -------------------------------------------------------------
        var sch   = makeSchema("cmf/refCode.cmf");
        var defs  = sch.getAsJsonObject("definitions");

        // -------------------------------------------------------------
        // 2️⃣ Set of definition names that are expected to have an "@id" property
        // -------------------------------------------------------------
        var withId = java.util.Set.of(
                "t:AnyRefType",
                "t:FourType",
                "t:URIRefType"
        );

        // -------------------------------------------------------------
        // 3️⃣ Iterate over every definition and verify the presence/absence of "@id"
        // -------------------------------------------------------------
        for (var entry : defs.entrySet()) {
            String defName = entry.getKey();
            JsonObject defObj = entry.getValue().getAsJsonObject();

            // The "@id" property is defined inside the "properties" object of the definition.
            JsonObject props = defObj.getAsJsonObject("properties");
            boolean hasId = props != null && props.has("@id");

            if (withId.contains(defName)) {
                // The definition must contain an "@id" property
                assertTrue(hasId,
                        "Definition \"" + defName + "\" should contain an \"@id\" property inside its properties object");

                // The value of "@id" must be an object with "$ref":"#/definitions/xs:anyURI"
                JsonObject idObj = props.getAsJsonObject("@id");
                assertNotNull(idObj, "\"@id\" value for " + defName + " should be a JSON object");
                assertTrue(idObj.has("$ref"),
                        "\"@id\" object for " + defName + " must contain a \"$ref\" member");
                assertEquals("#/definitions/xs:anyURI",
                        idObj.get("$ref").getAsString(),
                        "\"@id\" $ref for " + defName + " is incorrect");
            } else {
                // No other definition should have an "@id" property
                assertFalse(hasId,
                        "Definition \"" + defName + "\" should NOT contain an \"@id\" property inside its properties object");
            }
        }
    }
    
    @Test
    public void testArchVersions () throws Exception {
        var sch   = makeSchema("cmf/archVersions.cmf");
        var defs  = sch.getAsJsonObject("definitions");
        var ctx = JsonPath.using(JSG).parse(defs);
        
        assertEquals("#/definitions/xs:string",
            ctx.read("$.nc:TextType.properties.nc:TextLiteral.$ref", String.class));
        assertEquals("#/definitions/xs:string",
            ctx.read("$.nc5:TextType.properties.nc5:TextLiteral.$ref", String.class));
    }
    
    @Test
    public void testAttAugment () throws Exception {
        var sch   = makeSchema("cmf/attAugment.cmf");
        var defs  = sch.getAsJsonObject("definitions");
        
        var hasAttProp = new HashSet<String>();
        var hasObjProp = new HashSet<String>();
        
        for (var def : defs.entrySet()) {
            var key   = def.getKey();
            var defO  = def.getValue().getAsJsonObject();
            if (!defO.has("properties")) continue;
            var propO = defO.get("properties").getAsJsonObject();
            if (propO.has("t:attProp")) hasAttProp.add(key);
            if (propO.has("t:ObjProp")) hasObjProp.add(key);
        }
        assertEquals(Set.of("t:CCOneType", "t:CCTwoType", "t:SCOneType"), hasAttProp);
        assertEquals(Set.of("t:SCTwoType"), hasObjProp);
    }
    
    @Test
    public void testAugment () throws Exception {
        var sch   = makeSchema("cmf/augment.cmf");
        var defs  = sch.getAsJsonObject("definitions");
        var ctx = JsonPath.using(JSG).parse(defs);

        assertNotNull(ctx.read("$['nc:CommentType']['properties']['t:CommentDestinationText']"));
        assertNotNull(ctx.read("$['nc:EducationType']['properties']['t:CommentDestinationText']"));
        assertNotNull(ctx.read("$['nc:EducationType']['properties']['j:EducationTotalYearsText']"));
        assertNotNull(ctx.read("$['nc:EducationType']['properties']['nc:CommentText']"));
        assertNotNull(ctx.read("$['nc:EducationType']['properties']['nc:personNameCommentText']"));
        assertNotNull(ctx.read("$['nc:EducationType']['properties']['t:TestAugElement']"));
    }
    
    @Test
    public void testChoice () throws Exception {
        var sch  = makeSchema("cmf/choice.cmf");
        var defs = sch.getAsJsonObject("definitions");
        var ctx  = JsonPath.using(JSG).parse(defs);
        var tref = new TypeRef<List<String>>(){};
        
        var r1 = ctx.read("$['t:T4Type']['anyOf'][*].required[0]", tref);
        assertAll(
            () -> assertEquals(2, r1.size()),
            () -> assertEquals(Set.of("t:Prop1", "t:Prop2"), new HashSet<>(r1))
        );
        var r0 = ctx.read("$['t:T5Type']['anyOf'][*].required[0]", tref);
        assertAll(
            () -> assertEquals(3, r0.size()),
            () -> assertEquals(Set.of("t:Prop1", "t:Prop2", "t:Prop3"), new HashSet<>(r0))
        );
    }
    
    @Test
    public void testGaLitAtt () throws Exception {
        var sch  = makeSchema("cmf/gaLitAtt.cmf");
        var defs = sch.getAsJsonObject("definitions");
        var hasAttProp = new HashSet<String>();
        
        for (var def : defs.entrySet()) {
            var key   = def.getKey();
            var defO  = def.getValue().getAsJsonObject();
            if (!defO.has("properties")) continue;
            var propO = defO.get("properties").getAsJsonObject();
            if (propO.has("test:attProp")) hasAttProp.add(key);
        }
        assertEquals(hasAttProp, Set.of("test:SCOneType", "test:SCTwoType"));
    }    
    
    @Test
    public void testGaLitObj () throws Exception {
        var sch  = makeSchema("cmf/gaLitObj.cmf");
        var defs = sch.getAsJsonObject("definitions");
        var hasObjProp = new HashSet<String>();
        
        for (var def : defs.entrySet()) {
            var key   = def.getKey();
            var defO  = def.getValue().getAsJsonObject();
            if (!defO.has("properties")) continue;
            var propO = defO.get("properties").getAsJsonObject();
            if (propO.has("test:ObjProp")) hasObjProp.add(key);
        }
        assertEquals(hasObjProp, Set.of("test:SCOneType", "test:SCTwoType"));
    }

    @Test
    public void testGaObjAtt () throws Exception {
        var sch  = makeSchema("cmf/gaObjAtt.cmf");
        var defs = sch.getAsJsonObject("definitions");
        var hasAttProp = new HashSet<String>();
        
        for (var def : defs.entrySet()) {
            var key   = def.getKey();
            var defO  = def.getValue().getAsJsonObject();
            if (!defO.has("properties")) continue;
            var propO = defO.get("properties").getAsJsonObject();
            if (propO.has("test:attProp")) hasAttProp.add(key);
        }
        assertEquals(hasAttProp, Set.of("test:CCOneType", "test:CCTwoType", "test:ObjType"));
    }
    
    @Test
    public void testGaObjObj () throws Exception {
        var sch  = makeSchema("cmf/gaObjObj.cmf");
        var defs = sch.getAsJsonObject("definitions");
        var hasObjProp = new HashSet<String>();
        
        for (var def : defs.entrySet()) {
            var key   = def.getKey();
            var defO  = def.getValue().getAsJsonObject();
            if (!defO.has("properties")) continue;
            var propO = defO.get("properties").getAsJsonObject();
            if (propO.has("test:ObjProp")) hasObjProp.add(key);
        }
        assertEquals(hasObjProp, Set.of("test:CCOneType", "test:CCTwoType", "test:ObjType"));
    }
    
    @Test
    public void testUnion () throws Exception {
        var sch  = makeSchema("cmf/union.cmf");
        var defs = sch.getAsJsonObject("definitions");
        var ctx = JsonPath.using(JSG).parse(defs);
        var tref = new TypeRef<List<Map<String,Object>>>(){};
        
        var r1 = ctx.read("$['t:TelephoneNumberCategoryCodeType']['anyOf']", tref);
        assertEquals(2, r1.size());

        Set<String> refs = r1.stream().map(m -> (String) m.get("$ref")).collect(Collectors.toSet());
        assertEquals(refs, Set.of(
            "#/definitions/t:TelephoneNumberCategoryAdditionalCodeType",
            "#/definitions/t:CategoryCodeType"
        ));    
    }
    
    
    
    private static final SchemaRegistry sreg = SchemaRegistry.withDialect(Dialects.getDraft7());
    private static final Schema metasch = sreg.getSchema(SchemaLocation.of(Dialects.getDraft7().getId()));
    
    private boolean schemaValid (String s) {
        List<com.networknt.schema.Error> errors = metasch.validate(s, InputFormat.JSON, executionContext -> {
            executionContext.executionConfig(executionConfig -> executionConfig.formatAssertionsEnabled(true));
        });
        return errors.isEmpty();        
    }
    
    public JsonObject makeSchema (String fname) {
        var cmfF  = new File(RDIR, fname);
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(cmfF);
        var js    = new ModelToJSONSchema(model);
        var sch   = new JsonObject();
        js.setAllDefinitions(true);
        js.createSchema(sch);
        var s = sch.toString();
        assertTrue(schemaValid(s));
        return sch;        
    }
    
}
