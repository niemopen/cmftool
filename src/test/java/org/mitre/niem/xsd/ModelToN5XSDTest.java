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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mitre.niem.cmf.Model;


/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToN5XSDTest extends ModelToXSDTest {
    public static String testDir = "src/test/resources/xsd5/";

    
    public ModelToN5XSDTest() {
    }
    
    @ParameterizedTest
    @ValueSource(strings = { 
        "augment.xsd",
        "augmentProp.xsd",
        "cli.xsd",
        "clsa.xsd",
        "codelist.xsd",
        "codelistClassType.xsd",
        "codelistNoSType.xsd",
        "codelistUnion.xsd",
        "complexContent.xsd",
        //          "defaultFacets.xsd"         // won't round trip, not niem conforming
        "deprecated.xsd",
        "doubleType.xsd",
//        "externals.xsd",                      // won't round trip, external docs not generated
        "isRefAtt.xsd",
        "list.xsd",
        "listSimpleType.xsd",
        "literal-0.xsd",
        "literal-1.xsd",
        "literal-2.xsd",
        "literal-3.xsd",
        "literal-4.xsd",
        "literal-5.xsd",
        "literal-6.xsd",
        "literal-7.xsd",
        "localTerm.xsd",
        "metadata.xsd",
        //            "nameinfo.xsd",
        //            "namespace-1.xsd",        // can't automate this one; prefixes are changed in created XSD
        //            "noprefix.xsd",
        "proxy.xsd",
        "refDocumentation.xsd",
        "relProp.xsd",
        "restriction.xsd",
        //"twoversions-0.xsd",      // can't automate this one; prefixes are changed in created XSD
        "schemadoc.xsd",
        "structuresType.xsd",
        "union.xsd",
        "whitespace.xsd",
        "xml-lang.xsd"
        })
    public void testRoundTrip(String sourceXSD) throws Exception {
        testRT(testDir, sourceXSD);
    }
        
    @Override
    public void createXSD (File modelFP, File outDir) throws Exception {
        FileInputStream mis = new FileInputStream(modelFP);
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(mis);
        ModelToXSD mw  = new ModelToN5XSD(m);
        mw.writeXSD(outDir);
    }    
}
