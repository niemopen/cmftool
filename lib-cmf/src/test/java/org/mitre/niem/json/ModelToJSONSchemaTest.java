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
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.cmf.Property;

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
 
    @Test
    public void test () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File("src/test/resources/json/oneChoice.cmf"));
        var js    = new ModelToJSONSchema(model);
        var w     = new StringWriter();
        List<Property> msgPL = Arrays.asList(model.qnToProperty("ms"));
//        js.setMessageProperties(msgPL);
        js.setContextURI("http://example.com/Request/JSON");
        js.writeSchema(w);
        var s     = w.toString();
        int x = 0;
    }
    
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
    
    
    public JsonObject makeSchema (String fname) {
        var cmfF  = new File(RDIR, fname);
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(cmfF);
        var js    = new ModelToJSONSchema(model);
        var sch   = new JsonObject();
        js.createSchema(sch);
        return sch;        
    }
    
}
