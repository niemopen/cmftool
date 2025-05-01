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
public class ModelToXSDTest {
    private final static String resDN = "src/test/resources/";

    @TempDir
    File tmpD;
    
    public ModelToXSDTest() {
    }

    @Test
    public void testWriteModelXSD_File_String() {
    }

    @Test
    public void testWriteModelXSD () throws Exception {
        testRoundTrip(new File(resDN, "xsd6/attAugment.xsd"));
        testRoundTrip(new File(resDN, "xsd6/augment.xsd"));
        testRoundTrip(new File(resDN, "xsd6/class.xsd"));
        testRoundTrip(new File(resDN, "xsd6/codeListBinding.xsd"));
        testRoundTrip(new File(resDN, "xsd6/component.xsd"));
        testRoundTrip(new File(resDN, "xsd6/dataProperty.xsd"));
        testRoundTrip(new File(resDN, "xsd6/datatypes.xsd"));
//        testRoundTrip(new File(resDN, "xsd6/externals.xsd"));             // CAN'T ROUNDTRIP
        testRoundTrip(new File(resDN, "xsd6/globalAttAugment.xsd"));
//        testRoundTrip(new File(resDN, "xsd6/imports.xsd"));               // CAN'T ROUNDTRIP
        testRoundTrip(new File(resDN, "xsd6/list.xsd"));
        testRoundTrip(new File(resDN, "xsd6/literalClass.xsd"));
        testRoundTrip(new File(resDN, "xsd6/literalProps.xsd"));
        testRoundTrip(new File(resDN, "xsd6/localTerm.xsd"));
        testRoundTrip(new File(resDN, "xsd6/namespace.xsd"));
        testRoundTrip(new File(resDN, "xsd6/niemVersions.xsd"));
        testRoundTrip(new File(resDN, "xsd6/objectProperty.xsd"));
        testRoundTrip(new File(resDN, "xsd6/propAssoc.xsd"));
        testRoundTrip(new File(resDN, "xsd6/simpleTypes.xsd"));
        testRoundTrip(new File(resDN, "xsd6/union.xsd"));
    }
    
    public void testRoundTrip (File xsdF) throws Exception {

        var xsdName = xsdF.getName();
        var xsdBase = FilenameUtils.getBaseName(xsdF.toString());
        var cmfOneF = new File(tmpD, "one.cmf");
        var cmfTwoF = new File(tmpD, "two.cmf");
        var xsdOneD = new File(tmpD, "xsd.one");
        var xsdTwoD = new File(tmpD, "xsd.two");

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
        var mtxsd = new ModelToXSD(model);
        mtxsd.setRootNamespace("test");
        mtxsd.setCatalogPath("xml-catalog.xml");
        mtxsd.writeModelXSD(xsdOneD);
        var xsdOneF = new File(xsdOneD, xsdName);
        var schOne  = new NIEMSchema(xsdOneF);
        var msgs    = schOne.javaXMsgs();
        var goodXSD = msgs.isEmpty();
        assertTrue(goodXSD);
        
        // Create second CMF file from the new XSD pile
        model = mfxsd.createModel(schOne);
        os    = new FileOutputStream(cmfTwoF);
        ow    = new OutputStreamWriter(os, "UTF-8");
        mw.writeXML(model, ow);
        ow.close();
        
        var same = FileUtils.contentEqualsIgnoreEOL(cmfOneF, cmfTwoF, "UTF-8");
        assertTrue(same);
    }
}
