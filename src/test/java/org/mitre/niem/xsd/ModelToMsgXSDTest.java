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
import java.util.ArrayList;
import java.util.List;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSModel;
import static org.apache.xerces.xs.XSTypeDefinition.COMPLEX_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.NamespaceKind;

/**
 *
 * @author sar
 */
public class ModelToMsgXSDTest extends ModelToXSDTest {
    
    public static String testDir = "src/test/resources/xsd6/";    

    public ModelToMsgXSDTest() {
    }
    
    @Test
    public void testRefCodeComplexContent () throws Exception {
        // Create CMF from input schema, write to temp directory #1
        var sdoc = "refCodeCC.xsd";
        String[] schemaArgs = { testDir + sdoc };
        File modelFP = new File(tempD1, "model.cmf");
        createCMF(schemaArgs, modelFP);
        
        // Create schema from that CMF, write to temp directory #2
        createXSD(modelFP, tempD2);
        
        // Get XSModel for schema
        File newSchema = new File(tempD2, sdoc);
        schemaArgs[0] = newSchema.toString();
        var schema = new XMLSchema(schemaArgs);
        var xs = schema.xsmodel();
        
        assertThat(getAttributeNames(xs, "AnyRefType", "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"))
            .containsOnly("appliesToParent", "id", "ref", "uri");
        
        assertThat(getAttributeNames(xs, "RefRefType", "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"))
            .containsOnly("appliesToParent", "id", "ref");
        
        assertThat(getAttributeNames(xs, "URIRefType", "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"))
            .containsOnly("appliesToParent", "uri");
        
        assertThat(getAttributeNames(xs, "NoRefType", "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"))
            .isEmpty();
    }

    @Test
    public void testRefCodeSimpleContent () throws Exception {
        // Create CMF from input schema, write to temp directory #1
        var sdoc = "refCodeSC.xsd";
        String[] schemaArgs = { testDir + sdoc };
        File modelFP = new File(tempD1, "model.cmf");
        createCMF(schemaArgs, modelFP);
        
        // Create schema from that CMF, write to temp directory #2
        createXSD(modelFP, tempD2);
        
        // Get XSModel for schema
        File newSchema = new File(tempD2, sdoc);
        schemaArgs[0] = newSchema.toString();
        var schema = new XMLSchema(schemaArgs);
        var xs = schema.xsmodel();
        
        assertThat(getAttributeNames(xs, "SimpleContentAnyType", "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"))
            .containsOnly("id", "ref", "uri");
        
        assertThat(getAttributeNames(xs, "SimpleContentRefType", "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"))
            .containsOnly("id", "ref");
        
        assertThat(getAttributeNames(xs, "SimpleContentURIType", "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"))
            .containsOnly("uri");
        
        assertThat(getAttributeNames(xs, "SimpleContentNoneType", "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"))
            .isEmpty();
    }
    
    public List<String> getAttributeNames (XSModel xs, String name, String nsuri) {
        var res = new ArrayList<String>();
        var xt  = xs.getTypeDefinition(name, nsuri);     
        if (COMPLEX_TYPE != xt.getTypeCategory()) return res;
        
        var xct   = (XSComplexTypeDefinition)xt;
        var xatts = xct.getAttributeUses();
        for (int i = 0; i < xatts.getLength(); i++) {
            var xause = (XSAttributeUse)xatts.get(i);
            var xadec = xause.getAttrDeclaration();
            var aname = xadec.getName();
            res.add(aname);
        }
        return res;
    }
    
    @Override
    public void createXSD (File modelFP, File outDir) throws Exception {
        FileInputStream mis = new FileInputStream(modelFP);
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(mis);
        ModelToXSD mw  = new ModelToMsgXSD(m);
        mw.writeXSD(outDir);
    }        

    @Test
    public void testFixConformanceTargets () {
        NamespaceKind.reset();
        var mw   = new ModelToMsgXSD(null);
        var ctas = "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument";
        var rv   = mw.fixConformanceTargets(ctas);
        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
        
        ctas = "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument";
        rv   = mw.fixConformanceTargets(ctas);
        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
        
        ctas = "http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument";
        rv   = mw.fixConformanceTargets(ctas);
        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#ReferenceSchemaDocument");
        
        ctas = "http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument " +
               "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument " +
               "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument";
        rv   = mw.fixConformanceTargets(ctas);
        var rvs = rv.split("\\s+");
        assertEquals(rvs[0], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#ReferenceSchemaDocument");
        assertEquals(rvs[1], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
        assertEquals(rvs[2], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
    }
    
}
