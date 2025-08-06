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
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mitre.niem.cmf.ModelXMLWriter;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXMLSchemaTest {
    private final static String resDN = "src/test/resources/xsd6/";
    
    @TempDir
    File tmpD;
        
    public ModelToXMLSchemaTest() {
    }
    
    @Test
    public void testWriteModelXSD () throws Exception {
        var resF  = new File(resDN);
        var files = FileUtils.listFiles(resF, new String[]{"xsd"}, false);

        for (var xsdF : files) {
            if ("externals.xsd".equals(xsdF.getName())) continue;
            if ("imports.xsd".equals(xsdF.getName())) continue;
            testFile(xsdF);
        }
    }

    public void testFile (File xsdF) throws Exception {
        var xsdName = xsdF.getName();
        var xsdBase = FilenameUtils.getBaseName(xsdF.toString());
        var cmfOneF = new File(tmpD, "one.cmf");
        var xsdD    = new File(tmpD, "xsd");
//        System.err.println(xsdName);

        // Create first CMF file from XSD source
        var sch   = new NIEMSchema(xsdF);
        var mfxsd = new ModelFromXSD();
        var mw    = new ModelXMLWriter();
        var model = mfxsd.createModel(sch);
        var os    = new FileOutputStream(cmfOneF);
        var ow    = new OutputStreamWriter(os, "UTF-8");
        mw.writeXML(model, ow);
        ow.close();
        
        // Create a new XSD pile from CMF
        var mtxsd = new ModelToXMLSchema(model);
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
//    
//    @Test
//    public void testOneFile () throws Exception {
//        var resF  = new File(resDN, "datatypes.xsd");
//        testFile(resF);
//    }
}
