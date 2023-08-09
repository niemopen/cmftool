/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2023 The MITRE Corporation.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.cmf.Model;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToMsgXSDTest extends ModelToXSDTest {
    
    static String testDir = "src/test/resources/";    
    
    public ModelToMsgXSDTest() {
    }

    @Test
    public void testSetMessageNamespace() throws Exception {

        // Read the CMF file
        var cmfIS = new FileInputStream(testDir + "cmf6/augCCwE.cmf");
        var mr    = new ModelXMLReader();
        var m     = mr.readXML(cmfIS);   
        
        // Write XSD to tempD1 without setMsgNS.  Shouldn't import jxdm
        var mw = new ModelToMsgXSD(m);
        mw.writeXSD(tempD1);        
        var sf = new File(tempD1, "messageModel.xsd");
        var mv = fileContains(sf, "<xs:import namespace=.*jxdm");
        assertFalse(mv);

        // Write XSD to tempD2 with setMsgNS.  Should import jxdm
        mw = new ModelToMsgXSD(m);
        mw.setMessageNamespace("http://example.com/N6AugEx/1.0/");
        mw.writeXSD(tempD2);        
        sf = new File(tempD2, "messageModel.xsd");
        mv = fileContains(sf, "<xs:import namespace=.*jxdm");
        assertTrue(mv);
    }
    
}
