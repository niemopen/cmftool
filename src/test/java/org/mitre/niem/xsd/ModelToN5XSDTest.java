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
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mitre.niem.cmf.Model;


/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToN5XSDTest extends ModelToXSDTest {
   static String testDir = "src/test/resources/xsd5/";

    
    public ModelToN5XSDTest() {
    }
    
    @ParameterizedTest
    @ValueSource(strings = { 
        "augment-0.xsd",
        "augmentProp.xsd",
        "cli.xsd",
        "clsa.xsd",
        "codelist.xsd",
        "codelistClassType.xsd",
        "codelistUnion.xsd",
        "complexContent.xsd",
        "deprecated.xsd",
        //            "externals.xsd",          // can't automate this one; m2x doesn't generate external documents (gml.xsd)
        "list.xsd",
        "literal-0.xsd",
        "literal-1.xsd",
        "literal-2.xsd",
        "literal-3.xsd",
        "literal-4.xsd",
        "literal-5.xsd",
        "literal-6.xsd",
        "literal-7.xsd",
        //            "nameinfo.xsd",
        //            "namespace-1.xsd",        // can't automate this one; prefixes are changed in created XSD
        //            "noprefix.xsd",
        "proxy.xsd",
        "restriction.xsd",
        //"twoversions-0.xsd",      // can't automate this one; prefixes are changed in created XSD
        "schemadoc.xsd",
        "union.xsd",
        "whitespace.xsd",
        "xml-lang.xsd"
        })
    public void testRoundTrip(String sourceXSD) throws Exception {
    
        // Create CMF from input schema, weite to temp directory #1
        String[] schemaArgs = { testDir + sourceXSD };
        File modelFP = new File(tempD1, "model.cmf");
        createCMF(schemaArgs, modelFP);
        
        // Create schema from that CMF, write to temp directory #2
        createXSD(modelFP, tempD2);

        // Valid XSD?
        File newSchema = new File(tempD2, sourceXSD);
        assertTrue(isValidXSD(newSchema));
        
        // Now create CMF from the schema in temp directory #2        
        File newModelFP = new File(tempD1, "newModel.cmf");
        createCMF(schemaArgs, newModelFP);
        
        // First and second model files should be the same
        assertTrue(FileUtils.contentEquals(modelFP, newModelFP));

        // Now create second schema from new CMF & CMF in temp directory #3
        createXSD(newModelFP, tempD3);
        
        // Second and third temp directory should have same content
        List<File> sfl2 = FileUtils.listFiles(tempD2, null, true).stream().sorted().collect(Collectors.toList());
        List<File> sfl3 = FileUtils.listFiles(tempD3, null, true).stream().sorted().collect(Collectors.toList());
        int i2 = 0;
        int i3 = 0;
        assertEquals(sfl2.size(), sfl3.size());
        while (i2 < sfl2.size() && i3 < sfl3.size()) {
            File f2 = sfl2.get(i2++);
            File f3 = sfl3.get(i3++);
            String n2 = f2.getName();
            String n3 = f3.getName();
            assertEquals(n2,n3);
            assertTrue(FileUtils.contentEquals(f2, f3));
        }
        assertEmptyLogs();
    }
    
}
