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
package org.mitre.niem.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.ModelXMLReader;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ContextTest {

    private final static String resDN = "src/test/resources/map/"; 
    
    public ContextTest() {
    }

    @Test
    public void testCreate () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "reqres.cmf"));
        var res   = new StringWriter();
        var cxt   = Context.create(model);
        var x = cxt.getAsJsonPrimitive("msg");
        assertTrue("http://example.com/ReqRes/1.0/".equals(cxt.getAsJsonPrimitive("msg").getAsString()));
        Context.write(cxt, res);
        assertTrue(res.toString().startsWith("{\n  \"@context\":"));
    }    

    @Test
    public void testCreateTo () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "reqres.cmf"));
        var mr    = new BufferedReader(new FileReader(new File(resDN, "rr.map")));
        var map   = Mapping.read(mr);
        var res   = new StringWriter();
        Context.createTo(res, model, map);
        assertFalse(res.toString().isBlank());
    }
}
