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
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;

public class ModelToJSONSchemaTest {
    
    private static final String RDIR = "src/test/resources";
    
    private static final Configuration JSG = Configuration.builder()
        .jsonProvider(new GsonJsonProvider())
        .mappingProvider(new GsonMappingProvider())
        .build();
    
    private static final SchemaRegistry SREG_D7 = SchemaRegistry.withDialect(Dialects.getDraft7());
    private static final SchemaRegistry SREG_D201909 = SchemaRegistry.withDialect(Dialects.getDraft201909());
    private static final SchemaRegistry SREG_D202012 = SchemaRegistry.withDialect(Dialects.getDraft202012());
    
    private static final Schema METASCH_D7 =
        SREG_D7.getSchema(SchemaLocation.of(Dialects.getDraft7().getId()));
    private static final Schema METASCH_D201909 =
        SREG_D201909.getSchema(SchemaLocation.of(Dialects.getDraft201909().getId()));
    private static final Schema METASCH_D202012 =
        SREG_D202012.getSchema(SchemaLocation.of(Dialects.getDraft202012().getId()));
    
    @Test
    public void testCodeType () throws Exception {
        var sch  = makeSchema("json/codeType.cmf");
        var defs = getDefs(sch);
        var refP = refPrefix(sch);
        var ctx  = JsonPath.using(JSG).parse(defs);
        
        assertEquals(refP + "xs:string",
            ctx.read("$['t:IntegerCodeType']['allOf'][0]['$ref']", String.class));
        assertEquals(refP + "xs:token",
            ctx.read("$['t:TokenCodeType']['allOf'][0]['$ref']", String.class));
    }
    
    @Test
    public void testTwoChoice () throws Exception {
        var sch  = makeSchema("json/twoChoice.cmf");
        var defs = getDefs(sch);
        var refP = refPrefix(sch);
        var msgT = defs.getAsJsonObject("t:MessageType");
        var prop = msgT.getAsJsonObject("properties");
        var ctx  = JsonPath.using(JSG).parse(prop);
        
        assertEquals(
            Set.of("t:FooString", "t:FooToken", "t:BarString", "t:BarToken", "t:BugToken", "t:OptToken"),
            prop.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toSet())
        );
        
        assertEquals("array", ctx.read("$['t:FooString']['type']", String.class));
        assertEquals("array", ctx.read("$['t:FooToken']['type']", String.class));
        assertEquals(refP + "xs:string", ctx.read("$['t:FooString']['items']['$ref']", String.class));
        assertEquals(refP + "xs:token", ctx.read("$['t:FooToken']['items']['$ref']", String.class));
        assertEquals(10, ctx.read("$['t:FooString']['maxItems']", Integer.class).intValue());
        assertEquals(10, ctx.read("$['t:FooToken']['maxItems']", Integer.class).intValue());
        
        assertEquals(refP + "xs:string", ctx.read("$['t:BarString']['$ref']", String.class));
        assertEquals(refP + "xs:token", ctx.read("$['t:BarToken']['$ref']", String.class));
        assertEquals(refP + "xs:token", ctx.read("$['t:BugToken']['$ref']", String.class));
        assertEquals(refP + "xs:token", ctx.read("$['t:OptToken']['$ref']", String.class));
        
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
    
    @Test
    public void testOneChoice () throws Exception {
        var sch   = makeSchema("json/oneChoice.cmf");
        var defs  = getDefs(sch);
        var refP  = refPrefix(sch);
        var msgT  = defs.getAsJsonObject("t:MessageType");
        var props = msgT.getAsJsonObject("properties");
        var ctx   = JsonPath.using(JSG).parse(props);
        
        assertEquals("array", ctx.read("$['t:FooString']['type']", String.class));
        assertEquals(refP + "xs:string",
                     ctx.read("$['t:FooString']['items']['$ref']", String.class));
        assertEquals(10, ctx.read("$['t:FooString']['maxItems']", Integer.class).intValue());
        
        assertEquals("array", ctx.read("$['t:FooToken']['type']", String.class));
        assertEquals(refP + "xs:token",
                     ctx.read("$['t:FooToken']['items']['$ref']", String.class));
        assertEquals(10, ctx.read("$['t:FooToken']['maxItems']", Integer.class).intValue());
        
        assertEquals(refP + "xs:token",
                     ctx.read("$['t:BarToken']['$ref']", String.class));
        
        ctx = JsonPath.using(JSG).parse(msgT);
        JsonArray anyOf = msgT.getAsJsonArray("anyOf");
        assertEquals(2, anyOf.size(),
                "The anyOf array should contain exactly two alternatives");
        
        var requiredSet = new HashSet<String>();
        for (var elem : anyOf) {
            var altObj = elem.getAsJsonObject();
            var reqArr = altObj.getAsJsonArray("required");
            assertEquals(1, reqArr.size(),
                    "Each anyOf alternative must have a single required property");
            requiredSet.add(reqArr.get(0).getAsString());
        }
        
        assertEquals(Set.of("t:FooString", "t:FooToken"), requiredSet,
                "anyOf alternatives should require FooString and FooToken");
        
        JsonArray required = msgT.getAsJsonArray("required");
        assertEquals(1, required.size());
        assertEquals("t:BarToken", required.get(0).getAsString());
        
        assertFalse(msgT.get("additionalProperties").getAsBoolean());
    }
    
    @Test
    public void testNoChoice () throws Exception {
        var sch   = makeSchema("json/noChoice.cmf");
        var defs  = getDefs(sch);
        var refP  = refPrefix(sch);
        var msgT  = defs.getAsJsonObject("t:MessageType");
        var props = msgT.getAsJsonObject("properties");
        var ctx   = JsonPath.using(JSG).parse(props);
        
        assertEquals("array", ctx.read("$['t:FooString']['type']", String.class));
        assertEquals(refP + "xs:string",
                     ctx.read("$['t:FooString']['items']['$ref']", String.class));
        assertEquals(10, ctx.read("$['t:FooString']['maxItems']", Integer.class).intValue());
        assertEquals(2, ctx.read("$['t:FooString']['minItems']", Integer.class).intValue());
        
        assertEquals(refP + "xs:string",
                     ctx.read("$['t:BarString']['$ref']", String.class));
        
        JsonArray required = msgT.getAsJsonArray("required");
        assertEquals(2, required.size());
        assertTrue(required.contains(new JsonPrimitive("t:FooString")));
        assertTrue(required.contains(new JsonPrimitive("t:BarString")));
        
        assertFalse(msgT.get("additionalProperties").getAsBoolean());
        assertTrue(defs.has("xs:string"));
        assertTrue(defs.has("xs:token"));
    }
    
    @Test
    public void testRefCode () throws Exception {
        var sch   = makeSchema("cmf/refCode.cmf");
        var defs  = getDefs(sch);
        var refP  = refPrefix(sch);

        var withId = Set.of(
                "t:AnyRefType",
                "t:FourType",
                "t:URIRefType"
        );

        assertTrue(defs.has("xs:anyURI"), "Referenceable classes require xs:anyURI definition");
        assertTrue(defs.has("xs:string"), "Referenceable classes should also retain xs:string definition");

        for (var entry : defs.entrySet()) {
            String defName = entry.getKey();
            JsonObject defObj = entry.getValue().getAsJsonObject();

            JsonObject props = defObj.getAsJsonObject("properties");
            boolean hasId = props != null && props.has("@id");

            if (withId.contains(defName)) {
                assertTrue(hasId,
                        "Definition \"" + defName + "\" should contain an \"@id\" property");

                JsonObject idObj = props.getAsJsonObject("@id");
                assertNotNull(idObj, "\"@id\" value for " + defName + " should be a JSON object");
                assertTrue(idObj.has("$ref"),
                        "\"@id\" object for " + defName + " must contain a \"$ref\" member");
                assertEquals(refP + "xs:anyURI",
                        idObj.get("$ref").getAsString(),
                        "\"@id\" $ref for " + defName + " is incorrect");
            } else {
                assertFalse(hasId,
                        "Definition \"" + defName + "\" should NOT contain an \"@id\" property");
            }
        }
    }
    
    @Test
    public void testArchVersions () throws Exception {
        var sch   = makeSchema("cmf/archVersions.cmf");
        var defs  = getDefs(sch);
        var refP  = refPrefix(sch);
        var ctx   = JsonPath.using(JSG).parse(defs);
        
        assertEquals(refP + "xs:string",
            ctx.read("$['nc:TextType']['properties']['nc:TextLiteral']['$ref']", String.class));
        assertEquals(refP + "xs:string",
            ctx.read("$['nc5:TextType']['properties']['nc5:TextLiteral']['$ref']", String.class));
    }
    
    @Test
    public void testAttAugment () throws Exception {
        var sch   = makeSchema("cmf/attAugment.cmf");
        var defs  = getDefs(sch);
        
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
        var defs  = getDefs(sch);
        var ctx   = JsonPath.using(JSG).parse(defs);

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
        var defs = getDefs(sch);
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
        var defs = getDefs(sch);
        var hasAttProp = new HashSet<String>();
        
        for (var def : defs.entrySet()) {
            var key   = def.getKey();
            var defO  = def.getValue().getAsJsonObject();
            if (!defO.has("properties")) continue;
            var propO = defO.get("properties").getAsJsonObject();
            if (propO.has("test:attProp")) hasAttProp.add(key);
        }
        assertEquals(Set.of("test:SCOneType", "test:SCTwoType"), hasAttProp);
    }
    
    @Test
    public void testGaLitObj () throws Exception {
        var sch  = makeSchema("cmf/gaLitObj.cmf");
        var defs = getDefs(sch);
        var hasObjProp = new HashSet<String>();
        
        for (var def : defs.entrySet()) {
            var key   = def.getKey();
            var defO  = def.getValue().getAsJsonObject();
            if (!defO.has("properties")) continue;
            var propO = defO.get("properties").getAsJsonObject();
            if (propO.has("test:ObjProp")) hasObjProp.add(key);
        }
        assertEquals(Set.of("test:SCOneType", "test:SCTwoType"), hasObjProp);
    }

    @Test
    public void testGaObjAtt () throws Exception {
        var sch  = makeSchema("cmf/gaObjAtt.cmf");
        var defs = getDefs(sch);
        var hasAttProp = new HashSet<String>();
        
        for (var def : defs.entrySet()) {
            var key   = def.getKey();
            var defO  = def.getValue().getAsJsonObject();
            if (!defO.has("properties")) continue;
            var propO = defO.get("properties").getAsJsonObject();
            if (propO.has("test:attProp")) hasAttProp.add(key);
        }
        assertEquals(Set.of("test:CCOneType", "test:CCTwoType", "test:ObjType"), hasAttProp);
    }
    
    @Test
    public void testGaObjObj () throws Exception {
        var sch  = makeSchema("cmf/gaObjObj.cmf");
        var defs = getDefs(sch);
        var hasObjProp = new HashSet<String>();
        
        for (var def : defs.entrySet()) {
            var key   = def.getKey();
            var defO  = def.getValue().getAsJsonObject();
            if (!defO.has("properties")) continue;
            var propO = defO.get("properties").getAsJsonObject();
            if (propO.has("test:ObjProp")) hasObjProp.add(key);
        }
        assertEquals(Set.of("test:CCOneType", "test:CCTwoType", "test:ObjType"), hasObjProp);
    }
    
    @Test
    public void testUnion () throws Exception {
        var sch  = makeSchema("cmf/union.cmf");
        var defs = getDefs(sch);
        var refP = refPrefix(sch);
        var ctx  = JsonPath.using(JSG).parse(defs);
        var tref = new TypeRef<List<Map<String,Object>>>(){};
        
        var r1 = ctx.read("$['t:TelephoneNumberCategoryCodeType']['anyOf']", tref);
        assertEquals(2, r1.size());

        Set<String> refs = r1.stream().map(m -> (String) m.get("$ref")).collect(Collectors.toSet());
        assertEquals(Set.of(
            refP + "t:TelephoneNumberCategoryAdditionalCodeType",
            refP + "t:CategoryCodeType"
        ), refs);
    }
    
    @Test
    public void testContextAllowsOnlyObjectWhenNoContextURI() throws Exception {
        var sch = makeSchema("cmf/choice.cmf");
        var props = sch.getAsJsonObject("properties");
        var cxtO = props.getAsJsonObject("@context");
        var anyOf = cxtO.getAsJsonArray("anyOf");

        assertEquals(1, anyOf.size());

        var onlyAlt = anyOf.get(0).getAsJsonObject();
        assertEquals("object", onlyAlt.get("type").getAsString());
        assertFalse(onlyAlt.has("const"));
    }

    
    @Test
    public void testContextURIAllowsObjectOrConstString () throws Exception {
        var model = loadModel("cmf/choice.cmf");
        var js    = new ModelToJSONSchema(model);
        js.setAllDefinitions(true);
        js.setContextURI("http://example.org/context");
        
        var sch = new JsonObject();
        js.createSchema(sch);
        assertTrue(schemaValid(sch));
        
        var props = sch.getAsJsonObject("properties");
        var cxtO  = props.getAsJsonObject("@context");
        var anyOf = cxtO.getAsJsonArray("anyOf");
        
        assertEquals(2, anyOf.size());
        
        JsonObject objAlt = null;
        JsonObject strAlt = null;
        for (var alt : anyOf) {
            var altO = alt.getAsJsonObject();
            if ("object".equals(altO.get("type").getAsString())) objAlt = altO;
            if ("string".equals(altO.get("type").getAsString())) strAlt = altO;
        }
        
        assertNotNull(objAlt);
        assertNotNull(strAlt);
        assertEquals("http://example.org/context", strAlt.get("const").getAsString());
    }
    
    @Test
    public void testSetMessagePropertyNullClearsMessageProperty () throws Exception {
        var model = loadModel("json/request.cmf");
        var js    = new ModelToJSONSchema(model);
        js.setAllDefinitions(true);
        
        js.setMessageProperty(model.qnToProperty("msg:Request"));
        js.setMessageProperty(null);
        
        var sch = new JsonObject();
        js.createSchema(sch);
        assertTrue(schemaValid(sch));
        
        var required = sch.getAsJsonArray("required");
        assertEquals(1, required.size());
        assertEquals("@context", required.get(0).getAsString());
        assertFalse(sch.has("additionalProperties"));
    }
    
    @Test
    public void testGeneratorReusableProducesSameSchema () throws Exception {
        var model = loadModel("cmf/choice.cmf");
        var js    = new ModelToJSONSchema(model);
        js.setAllDefinitions(true);
        
        var sch1 = new JsonObject();
        js.createSchema(sch1);
        
        var sch2 = new JsonObject();
        js.createSchema(sch2);
        
        assertTrue(schemaValid(sch1));
        assertTrue(schemaValid(sch2));
        assertEquals(sch1, sch2, "Reusing the same generator should not accumulate state");
    }
    
    @Test
    public void testDraft07UsesDefinitions () throws Exception {
        var sch = makeSchema("json/noChoice.cmf", "draft-07");
        assertEquals("http://json-schema.org/draft-07/schema#", sch.get("$schema").getAsString());
        assertTrue(sch.has("definitions"));
        assertFalse(sch.has("$defs"));
        
        var defs = getDefs(sch);
        var msgT = defs.getAsJsonObject("t:MessageType");
        var props = msgT.getAsJsonObject("properties");
        var ctx = JsonPath.using(JSG).parse(props);
        assertEquals("#/definitions/xs:string",
            ctx.read("$['t:FooString']['items']['$ref']", String.class));
        assertEquals("#/definitions/xs:string",
            ctx.read("$['t:BarString']['$ref']", String.class));
    }
    
    @Test
    public void testDraft201909UsesDefs () throws Exception {
        var sch = makeSchema("json/noChoice.cmf", "2019-09");
        assertEquals("https://json-schema.org/draft/2019-09/schema", sch.get("$schema").getAsString());
        assertTrue(sch.has("$defs"));
        assertFalse(sch.has("definitions"));
        
        var defs = getDefs(sch);
        var msgT = defs.getAsJsonObject("t:MessageType");
        var props = msgT.getAsJsonObject("properties");
        var ctx = JsonPath.using(JSG).parse(props);
        assertEquals("#/$defs/xs:string",
            ctx.read("$['t:FooString']['items']['$ref']", String.class));
        assertEquals("#/$defs/xs:string",
            ctx.read("$['t:BarString']['$ref']", String.class));
    }
    
    @Test
    public void testDraft202012UsesDefs () throws Exception {
        var sch = makeSchema("json/oneChoice.cmf", "2020-12");
        assertEquals("https://json-schema.org/draft/2020-12/schema", sch.get("$schema").getAsString());
        assertTrue(sch.has("$defs"));
        assertFalse(sch.has("definitions"));
        
        var defs = getDefs(sch);
        var msgT = defs.getAsJsonObject("t:MessageType");
        var props = msgT.getAsJsonObject("properties");
        var ctx = JsonPath.using(JSG).parse(props);
        assertEquals("#/$defs/xs:string",
            ctx.read("$['t:FooString']['items']['$ref']", String.class));
        assertEquals("#/$defs/xs:token",
            ctx.read("$['t:FooToken']['items']['$ref']", String.class));
        assertEquals("#/$defs/xs:token",
            ctx.read("$['t:BarToken']['$ref']", String.class));
    }
    
    private Model loadModel (String fname) {
        var cmfF = new File(RDIR, fname);
        var rdr  = new ModelXMLReader();
        return rdr.readFiles(cmfF);
    }
    
    private JsonObject makeSchema (String fname) throws Exception {
        var model = loadModel(fname);
        var js    = new ModelToJSONSchema(model);
        var sch   = new JsonObject();
        js.setAllDefinitions(true);
        js.createSchema(sch);
        assertTrue(schemaValid(sch));
        return sch;
    }
    
    private JsonObject makeSchema (String fname, String version) throws Exception {
        var model = loadModel(fname);
        var js    = new ModelToJSONSchema(model);
        var sch   = new JsonObject();
        js.setAllDefinitions(true);
        js.setSchemaVersion(version);
        js.createSchema(sch);
        assertTrue(schemaValid(sch));
        return sch;
    }
    
    private static JsonObject getDefs (JsonObject sch) {
        if (sch.has("$defs")) return sch.getAsJsonObject("$defs");
        return sch.getAsJsonObject("definitions");
    }
    
    private static String refPrefix (JsonObject sch) {
        return sch.has("$defs") ? "#/$defs/" : "#/definitions/";
    }
    
    private boolean schemaValid (JsonObject sch) {
        var metasch = metaSchemaFor(sch);
        List<com.networknt.schema.Error> errors = metasch.validate(
            sch.toString(),
            InputFormat.JSON,
            executionContext -> executionContext.executionConfig(
                executionConfig -> executionConfig.formatAssertionsEnabled(true)
            )
        );
        return errors.isEmpty();
    }
    
    private Schema metaSchemaFor (JsonObject sch) {
        var schemaUri = sch.get("$schema").getAsString();
        return switch (schemaUri) {
            case "http://json-schema.org/draft-07/schema#" -> METASCH_D7;
            case "https://json-schema.org/draft/2019-09/schema" -> METASCH_D201909;
            case "https://json-schema.org/draft/2020-12/schema" -> METASCH_D202012;
            default -> throw new IllegalArgumentException("Unsupported JSON Schema dialect: " + schemaUri);
        };
    }
}
