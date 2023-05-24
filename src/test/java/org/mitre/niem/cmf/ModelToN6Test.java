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
package org.mitre.niem.cmf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.xsd.ModelXMLReader;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToN6Test {
    
    private static final String testDirPath = "src/test/resources";  
    
    public ModelToN6Test() {
    }

    @Test
    public void testConvert() throws CMFException, FileNotFoundException, TransformerException, TransformerConfigurationException, ParserConfigurationException {
        FileInputStream cmfIS = null;
        var cmfFile = new File(testDirPath, "cmf5/complexContent.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        var mr = new ModelXMLReader();
        var m = mr.readXML(cmfIS);   
        var m5to6 = new ModelToN6();
        m5to6.convert(m);
        assertThat(m.getNamespaceByPrefix("nc").getNamespaceURI()).startsWith("https://docs.oasis-open.org/niemopen/");
        for (var sd : m.schemadoc().values()) {
            assertThat(sd.targetNS()).startsWith("https://docs.oasis-open.org/niemopen/");
        }               
        int i = 0;
    }
    
}
