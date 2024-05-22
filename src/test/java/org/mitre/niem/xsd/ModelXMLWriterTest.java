/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2022 The MITRE Corporation.
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
import java.io.FileOutputStream;
import javax.xml.transform.TransformerException;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mitre.niem.cmf.Model;
import static org.mitre.niem.xsd.FileCompare.compareIgnoringTrailingWhitespace;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLWriterTest {
    
    public ModelXMLWriterTest() {
    }

    private static final String testDirPath = "src/test/resources";
    private File outF;
    
    @BeforeEach
    public void setup () throws Exception {
        outF = File.createTempFile("testWriteXML", ".cmf");
    }
    
    @AfterEach
    public void teardown () throws Exception {
        outF.delete();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "augment.cmf",
        "cli.cmf",
        "clsa.cmf",
        "complexContent.cmf",
        "components.cmf",
        "datatypes.cmf",
        "externals.cmf",
        "isRefAtt.cmf",
        "localTerm.cmf",
//        "mismatchIDRef.cmf",
//        "missingIDRef.cmf",
        "proxy.cmf",
        "relProp.cmf",
        "union.cmf"
    })
    public void testWriteXML (String cmfFN) throws TransformerException {
        FileInputStream cmfIS = null;
        File cmfDir = new File(testDirPath, "cmf");
        File inF = new File(cmfDir, cmfFN);
        try {
            cmfIS = new FileInputStream(inF);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        FileOutputStream os = null;
        ModelXMLWriter mw = new ModelXMLWriter();
        try {
            os = new FileOutputStream(outF);
            mw.writeXML(m, os);
            os.close();
            String result = compareIgnoringTrailingWhitespace(inF, outF);
            assertNull(result);
        } catch (Exception ex) {
            fail("Can't create output model file");
        }
    }
}
