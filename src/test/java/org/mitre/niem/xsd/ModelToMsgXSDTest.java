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
import java.util.ArrayList;
import java.util.List;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.MODEL_GROUP;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import static org.apache.xerces.xs.XSTypeDefinition.COMPLEX_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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
    public void testInherit () throws Exception {
        var sf = new File(testDir, "inherit.xsd");
        var mf = createMessageXSD(sf);
        var m  = createModel(mf);
        var xs = createXSmodel(mf);     
        var ns = m.getNamespaceByPrefix("nc");
        var nc = ns.getNamespaceURI();
               
        assertThat(getElementNames(xs, "ConveyanceType", nc)).containsOnly(
                "ItemModelName",
                "ItemName",
                "ItemAugmentation", // my:ItemAugmentation
                "ConveyanceCargoText",
                "ConveyanceEngineQuantity");
        assertThat(getElementNames(xs, "IdentificationType", nc)).containsOnly(
                "IdentificationID",
                "IdentificationJurisdiction");
        assertThat(getElementNames(xs, "ItemType", nc)).containsOnly(
                "ItemModelName",
                "ItemName",
                "ItemAugmentation");
        assertThat(getElementNames(xs, "JurisdictionType", nc)).containsOnly(
                "JurisdictionDescriptionText");
        var foo = getElementNames(xs, "VesselType", nc);
        assertThat(getElementNames(xs, "VesselType", nc)).containsOnly(
                "ItemModelName",
                "ItemName",
                "ItemAugmentation",
                "ConveyanceCargoText",
                "ConveyanceEngineQuantity",
                "VesselHullIdentification",
                "VesselAugmentation");  // my:VesselAugmentation
        assertThat(getElementNames(xs, "TextType", nc)).isEmpty();
    }
    
    @Test
    public void testRefCodeComplexContent () throws Exception {
        var sf = new File(testDir, "refCodeCC.xsd");
        var mf = createMessageXSD(sf);
        var m  = createModel(mf);
        var xs = createXSmodel(mf);     
        var ns = m.getNamespaceByPrefix("nc");
        var nc = ns.getNamespaceURI();        
        
        assertThat(getAttributeNames(xs, "AnyRefType", nc)).containsOnly("appliesToParent", "id", "ref", "uri");        
        assertThat(getAttributeNames(xs, "RefRefType", nc)).containsOnly("appliesToParent", "id", "ref");        
        assertThat(getAttributeNames(xs, "URIRefType", nc)).containsOnly("appliesToParent", "uri");        
        assertThat(getAttributeNames(xs, "NoRefType", nc)).isEmpty();
    }

    @Test
    public void testRefCodeSimpleContent () throws Exception {
        var sf = new File(testDir, "refCodeSC.xsd");
        var mf = createMessageXSD(sf);
        var m  = createModel(mf);
        var xs = createXSmodel(mf);     
        var ns = m.getNamespaceByPrefix("nc");
        var nc = ns.getNamespaceURI(); 
    
        assertThat(getAttributeNames(xs, "SimpleContentAnyType", nc)).containsOnly("appliesToParent", "id", "ref", "uri");        
        assertThat(getAttributeNames(xs, "SimpleContentRefType", nc)).containsOnly("appliesToParent", "id", "ref");        
        assertThat(getAttributeNames(xs, "SimpleContentURIType", nc)).containsOnly("appliesToParent", "uri");        
        assertThat(getAttributeNames(xs, "SimpleContentNoneType", nc)).isEmpty();
    }
    
//    @Test
//    public void testFixConformanceTargets () {
//        NamespaceKind.reset();
//        var mw   = new ModelToMsgXSD(null);
//        var ctas = "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument";
//        var rv   = mw.fixConformanceTargets(ctas);
//        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
//        
//        ctas = "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument";
//        rv   = mw.fixConformanceTargets(ctas);
//        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
//        
//        ctas = "http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument";
//        rv   = mw.fixConformanceTargets(ctas);
//        assertEquals(rv, "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#ReferenceSchemaDocument");
//        
//        ctas = "http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument " +
//               "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument " +
//               "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument";
//        rv   = mw.fixConformanceTargets(ctas);
//        var rvs = rv.split("\\s+");
//        assertEquals(rvs[0], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#ReferenceSchemaDocument");
//        assertEquals(rvs[1], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
//        assertEquals(rvs[2], "https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#MessageSchemaDocument");
//    }
    
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
    
    public List<String> getElementNames (XSModel xs, String name, String nsuri) {
        var res = new ArrayList<String>();
        var xt  = xs.getTypeDefinition(name, nsuri);     
        if (COMPLEX_TYPE != xt.getTypeCategory()) return res;
        var xct    = (XSComplexTypeDefinition)xt;
        var par    = xct.getParticle();
        var xplist = new ArrayList<XSParticle>();
        collectElements(par, xplist);
        for (var xp : xplist) {
            var xterm  = xp.getTerm();
            var xed    = (XSElementDeclaration)xterm;
            res.add(xed.getName());
        }
        return res;        
    }

    // Recursively descend through model groups, collecting element declarations
    public void collectElements (XSParticle par, List<XSParticle> epars) {
        if (null == par) return;
        XSTerm pt = par.getTerm();
        if (null == pt) return;
        switch (pt.getType()) {
            case ELEMENT_DECLARATION:
                epars.add(par);
                break;
            case MODEL_GROUP:
                XSModelGroup mg = (XSModelGroup)pt;
                XSObjectList objs = mg.getParticles();
                for (int i = 0; i < objs.getLength(); i++) {
                    XSParticle pp = (XSParticle)objs.item(i);
                    collectElements(pp, epars);
                }    
                break;
        }
    }    
    
    public File createMessageXSD (File f) throws Exception {
        var dir = f.getParent();
        var fn  = f.getName();
        var m   = createModel(f);
        var mw  = new ModelToMsgXSD(m);
        mw.writeXSD(tempD1);
        var rf  = new File(tempD1, fn);
        return rf;
    } 

    public XSModel createXSmodel (File f) throws Exception {
        // Create CMF from input schema, write to temp directory #1
        String[] schemaArgs = { f.toString() };
        var xs = new XMLSchema(schemaArgs);
        return xs.xsmodel();
    }
}
