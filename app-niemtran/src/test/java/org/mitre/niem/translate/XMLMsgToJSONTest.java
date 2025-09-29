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
package org.mitre.niem.translate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mitre.niem.cmf.ModelXMLReader;
import org.xml.sax.InputSource;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLMsgToJSONTest {
    
    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final static String resDN = "src/test/resources/";
    
    public XMLMsgToJSONTest() {
    }
    
    @Test
    public void testAugCCwA () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "augCCwA.cmf"));        
        var xmlF   = new File(resDN, "augCCwA.xml");
        var xmlIS  = new InputSource(new FileInputStream(xmlF));
        var jsonW  = new StringWriter();
        var tran   = new XMLMsgToJSON(model);
        var jobj   = new JsonObject();
        var status = tran.convert(xmlIS, jobj);
        var jmsg   = gson.toJson(jobj);
       
        var m = jobj.getAsJsonObject("t:Message");
        var pe = m.getAsJsonObject("nc:PersonEducation");
        var ed = pe.getAsJsonArray("nc:EducationDescriptionText");
        var sp = pe.getAsJsonPrimitive("t:aProp");
        
        assertEquals(1, ed.size());
        assertEquals("FOO", sp.getAsString());
    }
    
    @Test
    public void testAugCCwE () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "augCCwE.cmf"));        
        var xmlF   = new File(resDN, "augCCwE.xml");
        var xmlIS  = new InputSource(new FileInputStream(xmlF));
        var jsonW  = new StringWriter();
        var tran   = new XMLMsgToJSON(model);
        var jobj   = new JsonObject();
        var status = tran.convert(xmlIS, jobj);
        var jmsg   = gson.toJson(jobj);
       
        var m = jobj.getAsJsonObject("t:Message");
        var pe = m.getAsJsonObject("nc:PersonEducation");
        var ed = pe.getAsJsonArray("nc:EducationDescriptionText");
        var sp = pe.getAsJsonPrimitive("t:StringProp");
        
        assertEquals(1, ed.size());
        assertEquals("An augmentation", sp.getAsString());
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
    
}
