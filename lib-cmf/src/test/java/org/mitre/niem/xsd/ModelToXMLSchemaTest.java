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
package org.mitre.niem.xsd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.TempDir;
import org.mitre.niem.cmf.ModelXMLWriter;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXMLSchemaTest {
    private final static String resDN = "src/test/resources/";
    
    @TempDir
    File tmpD;
        
    public ModelToXMLSchemaTest() {
    }

    @Test
    public void testRoundTrip () throws Exception {

        var xsdF   = new File(resDN, "xsd6/message.xsd");
        var xsdName = xsdF.getName();
        var xsdBase = FilenameUtils.getBaseName(xsdF.toString());
        var cmfOneF = new File(tmpD, "one.cmf");
        var cmfTwoF = new File(tmpD, "two.cmf");
        var xsdD    = new File(tmpD, "xsd");

        // Create first CMF file from XSD source
        var sch   = new NIEMSchema(xsdF);
        var mfxsd = new ModelFromXSD();
        var mw    = new ModelXMLWriter();
        var model = mfxsd.createModel(sch);
        var os    = new FileOutputStream(cmfOneF);
        var ow    = new OutputStreamWriter(os, "UTF-8");
        mw.writeXML(model, ow);
        ow.close();
        
        // Create a XSD message schema from CMF
//        var mtxsd = new ModelToXMLSchema(model);
        var mtxsd = new ModelToXSDModel(model);
        if (null != model.namespaceObj("test")) mtxsd.setRootNamespace("test");
        else if (null != model.namespaceObj("t")) mtxsd.setRootNamespace("t");
        mtxsd.setCatalogPath("xml-catalog.xml");
        mtxsd.writeModelXSD(xsdD);
        var xsdOneF = new File(xsdD, xsdName);
        var schOne  = new NIEMSchema(xsdOneF);
        var msgs    = schOne.javaXMsgs();

        var goodXSD = msgs.isEmpty();
        assertTrue(goodXSD);

    }
}
