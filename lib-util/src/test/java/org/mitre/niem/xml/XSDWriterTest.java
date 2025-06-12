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
package org.mitre.niem.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XSDWriterTest {
    private static final String resDN = "src/test/resources";
    
    @TempDir
    File tempDF;
        
    public XSDWriterTest() { }

    @Test
    public void testWriteXML () throws Exception {
        doTest(new File(resDN, "xsd/goodXsTest.xsd")); 
        doTest(new File(resDN, "xsd/niem/niem-core-skel.xsd"));
    }
    
    public void doTest (File xsdF) throws Exception {
        var dbf  = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        var db   = dbf.newDocumentBuilder();
        var doc  = db.parse(xsdF);
        var outF = new File(tempDF, "output.xml");
        var os   = new FileOutputStream(outF);
        var ow   = new OutputStreamWriter(os, "UTF-8");
        var xw   = new XSDWriter();
        xw.writeXML(doc, ow);
        ow.close();
        //var same = FileUtils.contentEqualsIgnoreEOL(xsdF, outF, "UTF-8");
        //assertTrue(same);         
    }
    
}
