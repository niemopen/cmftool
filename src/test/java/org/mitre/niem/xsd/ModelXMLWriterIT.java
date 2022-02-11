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
import java.io.PrintWriter;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mitre.niem.cmf.Model;
import static org.mitre.niem.xsd.FileCompare.compareIgnoringTrailingWhitespace;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLWriterIT {
    
    private static final String testDirPath = "src/test/resources";
    
    public ModelXMLWriterIT () { 
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }      
    
    @Test
    public void testWriteXML () throws TransformerException {
        FileInputStream cmfIS = null;
        File cmfDir = new File(testDirPath, "cmf");
        String[] testFiles = cmfDir.list(new SuffixFileFilter(".cmf"));
//        String[] testFiles = { "mismatchIDRef.cmf" };
        for (String tfn : testFiles) {
            File inF = new File(cmfDir, tfn);
            try {
                cmfIS = new FileInputStream(inF);
            } catch (FileNotFoundException ex) {
                fail("Where is my input file?");
            }
            ModelXMLReader mr = new ModelXMLReader();
            Model m = mr.readXML(cmfIS);  
            if (!mr.getMessages().isEmpty()) continue;
            
            File outF = null;
            PrintWriter outPW = null;
            ModelXMLWriter mw = new ModelXMLWriter();
            try {
                outF = File.createTempFile("testWriteXML", ".cmf");
                outPW = new PrintWriter(outF);
                mw.writeXML(m, outPW);    
                outPW.close();
                String result = compareIgnoringTrailingWhitespace(inF, outF);
                outF.delete();
                assertNull(result);
            } catch (Exception ex) {
                fail("Can't create output model file");
            }
        }
    }
}
