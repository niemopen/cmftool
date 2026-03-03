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
    
    public ModelToJSONSchemaTest() {
    }

    @Test
    public void test () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File("src/test/resources/json/itl.cmf"));
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
    public void testSetMapping() {
    }

    @Test
    public void testSetMessageProperty() {
    }

    @Test
    public void testSetMessageProperties() {
    }

    @Test
    public void testSetContextURI() {
    }

    @Test
    public void testSetFullContext() {
    }

    @Test
    public void testSetIncludeDescription() {
    }

    @Test
    public void testWriteSchema() throws Exception {
    }

    @Test
    public void testCreateSchema() {
    }

    @Test
    public void testAddPair() {
    }

    @Test
    public void testMakeObject_String_String() {
    }

    @Test
    public void testMakeObject_String_JsonElement() {
    }

    @Test
    public void testMakeStringArray() {
    }

    @Test
    public void testMakeElement() {
    }

    @Test
    public void testSetNoPrefix() {
    }
    
}
