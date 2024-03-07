/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2024 The MITRE Corporation.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.NamespaceKind;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToSrcXSDTest extends ModelToXSDTest {
    
    public static String testDir = "src/test/resources/xsd6/";

    
    public ModelToSrcXSDTest() {
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "attAug.xsd",
        "augment.xsd",
        "augmentProp.xsd",
        "cli.xsd",
        "clsa.xsd",
        "codelist.xsd",
        "codelistClassType.xsd",
        "codelistNoSType.xsd",
        "codelistUnion.xsd",
        "complexContent.xsd",
        "crossNSstype.xsd",
//        "defaultFacets.xsd",
        "deprecated.xsd",
        "doubleType.xsd",
        "externals.xsd",
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
        "namespace-1.xsd",
        "namespace-2.xsd",
        "noprefix.xsd",
        "proxy.xsd",
        "refCode-2.xsd",
        "relProp.xsd",
        "restriction.xsd",
        "schemadoc.xsd",
        "structuresType.xsd",
        "union.xsd",
        "whitespace.xsd",
        "xml-lang.xsd"
    })
    public void testRoundTrip(String sourceXSD) throws Exception {
        testRT(testDir, sourceXSD);
    }
    
    @Test
    public void testFixConformanceTargets () {
        NamespaceKind.reset();
        var mw   = new ModelToSrcXSD(null);
        var ctas = "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument";
        var rv   = mw.fixConformanceTargets(ctas);
        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument");
        
        ctas = "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument";
        rv   = mw.fixConformanceTargets(ctas);
        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument");
        
        ctas = "http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument";
        rv   = mw.fixConformanceTargets(ctas);
        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#ReferenceSchemaDocument");
        
        ctas = "http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument " +
               "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument " +
               "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument";
        rv   = mw.fixConformanceTargets(ctas);
        var rvs = rv.split("\\s+");
        assertEquals(rvs[0], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#ReferenceSchemaDocument");
        assertEquals(rvs[1], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument");
        assertEquals(rvs[2], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument");

        int i = 0;
    }
        
    @Override
    public void createXSD (File modelFP, File outDir) throws Exception {
        FileInputStream mis = new FileInputStream(modelFP);
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(mis);
        ModelToXSD mw  = new ModelToSrcXSD(m);
        mw.writeXSD(outDir);
    }    
    
}
