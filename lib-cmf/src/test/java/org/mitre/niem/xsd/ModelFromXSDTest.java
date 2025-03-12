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
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLWriter;
import org.mitre.niem.xml.LanguageString;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSDTest {
    private final static String resDN = "src/test/resources/";
    
    public ModelFromXSDTest() {
    }
    
    @Test
    public void testAttributeAugmentation () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/attAugment.xsd");
        var model = mb.createModel(sch);

        assertNull(model.qnToDatatype("test:TCodeList2Type"));
        assertNotNull(model.qnToDatatype("test:TCodeList3Type"));
        assertNotNull(model.qnToDataProperty("test:TCodeList2Literal"));
    }
    
    @Test
    public void testGlobalAttributeAugmentation () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/globalAttAugment.xsd");
        var model = mb.createModel(sch);

        assertNull(model.qnToDatatype("test:TCodeList2Type"));
        assertNull(model.qnToDatatype("test:TCodeList3Type"));
        assertNotNull(model.qnToClassType("test:TCodeList2Type"));
        assertNotNull(model.qnToClassType("test:TCodeList2Type"));
        assertNotNull(model.qnToDataProperty("test:TCodeList2Literal"));
        assertNotNull(model.qnToDataProperty("test:TCodeList2Literal"));
    }
    
    @Test
    public void testElementAugmentation () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/augment.xsd");
        var model = mb.createModel(sch);   
        debugModelWriter(model);
    }
    
    @Test
    public void testClassType () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/class.xsd");
        var model = mb.createModel(sch);   
        
        var ct1 = model.qnToClassType("test:Test1Type");
        var ct2 = model.qnToClassType("test:Test2Type");
        var ct3 = model.qnToClassType("test:Test3Type");
        var ct4 = model.qnToClassType("test:Test4Type");
        var ct5 = model.qnToClassType("test:Test5Type");
        
        var cp = ct1.propL().get(0);
        assertTrue(ct1.isAbstract());
        assertNull(ct1.subClass());
        assertEquals("", ct1.referenceCode());
        assertThat(ct1.propL()).hasSize(1);
        assertEquals("test:AnElement", cp.property().qname());
        assertEquals("0", cp.minOccurs());
        assertEquals("1", cp.maxOccurs());
        assertTrue(cp.isOrdered());
        assertThat(cp.docL())
            .extracting(LanguageString::text)
            .containsExactly("AnElement property of Test1Type");
        var ap = ct1.anyL().get(0);
        assertThat(ct1.anyL()).hasSize(1);
        assertEquals("lax", ap.processCode());
        assertEquals("http://someNS/ http://otherNS/", ap.nsConstraint());
        assertFalse(ap.isAttribute());
        
        cp = ct2.propL().get(0);
        assertFalse(ct2.isAbstract());
        assertNull(ct2.subClass());
        assertEquals("ANY", ct2.referenceCode());
        assertThat(ct2.propL()).hasSize(1);
        assertEquals("test:AnElement", cp.property().qname());
        assertEquals("1", cp.minOccurs());
        assertEquals("1", cp.maxOccurs());
        assertFalse(cp.isOrdered());
        assertThat(cp.docL()).hasSize(0);
        ap = ct2.anyL().get(0);
        assertThat(ct2.anyL()).hasSize(1);
        assertEquals("strict", ap.processCode());
        assertEquals("", ap.nsConstraint());
        assertTrue(ap.isAttribute());
        
        cp = ct3.propL().get(0);
        assertFalse(ct3.isAbstract());
        assertEquals("test:Test1Type", ct3.subClass().qname());
        assertEquals("URI", ct3.referenceCode());
        assertThat(ct3.propL()).hasSize(1);
        assertEquals("test:AnotherElement", cp.property().qname());
        assertEquals("1", cp.minOccurs());
        assertEquals("1", cp.maxOccurs());
        assertFalse(cp.isOrdered());
        assertThat(cp.docL()).hasSize(0);
        assertThat(ct3.anyL()).hasSize(0);   
        
        assertEquals("REF", ct4.referenceCode());
        assertEquals("NONE", ct5.referenceCode());
        
        debugModelWriter(model);
    }
        
    @Test
    public void testCodeListBindings () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/codeListBinding.xsd");
        var model = mb.createModel(sch);
        
        var dt = model.qnToDatatype("test:TCodeList2Type");
        var clb = dt.codeListBinding();
        assertEquals("http://code.list.uri", clb.codeListURI());
        assertEquals("", clb.column());
        assertTrue(clb.isConstraining());

        dt = model.qnToDatatype("test:TCodeList3Type");
        clb = dt.codeListBinding();
        assertEquals("http://list.uri", clb.codeListURI());
        assertEquals("key", clb.column());
        assertFalse(clb.isConstraining());
    }
    
    @Test
    public void testDataProperties () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/dataProperty.xsd");
        var model = mb.createModel(sch);

        var ap2 = model.qnToDataProperty("test:aProp2");
        var ap3 = model.qnToDataProperty("test:aProp3");
        var ap4 = model.qnToDataProperty("test:aProp4");
        var ap5 = model.qnToDataProperty("test:aProp5");    
        assertTrue(ap2.isAttribute());
        assertTrue(ap3.isAttribute());
        assertTrue(ap4.isAttribute());
        assertTrue(ap5.isAttribute());
        assertFalse(ap2.isRefAttribute());
        assertFalse(ap3.isRefAttribute());   
        assertTrue(ap4.isRefAttribute());
        assertTrue(ap5.isRefAttribute());
        assertFalse(ap2.isRelationship());
        assertTrue(ap3.isRelationship());
        assertFalse(ap4.isRelationship());
        assertTrue(ap5.isRelationship());
    }
    
    @Test
    public void testDatatypes () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/datatypes.xsd");
        var model = mb.createModel(sch);
        
        var dt = model.qnToDatatype("test:List1Type");
        var docL = dt.docL();
        var item = dt.itemType();
        assertThat(docL).extracting(LanguageString::text).containsExactly("List of a named simple type");
        assertEquals("xs:integer", item.qname());
        
        dt = model.qnToDatatype("test:List2Type");
        item = dt.itemType();
        assertEquals("test:UnionType", item.qname());
        
        dt = model.qnToDatatype("test:List3Type");
        item = dt.itemType();
        assertEquals("test:Restrict8Type", item.qname());
        
        assertNull(model.qnToDatatype("test:Literal1Type"));
        
        assertNotNull(model.qnToClassType("test:Literal2Type"));        
        assertNull(model.qnToDatatype("test:Literal2Type"));
        dt = model.qnToDatatype("test:Literal2SimpleType");
        var base = dt.base();
        assertEquals("xs:float", base.qname());

        assertNotNull(model.qnToClassType("test:Literal3Type"));
        assertNotNull(model.qnToClassType("test:Literal4Type"));
        assertNull(model.qnToDatatype("test:Literal3Type"));
        assertNull(model.qnToDatatype("test:Literal4Type"));
        
        dt = model.qnToDatatype("test:Restrict1Type");
        base = dt.base();
        var fL = dt.facetL();
        assertEquals("xs:token", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactly(
                Assertions.tuple("MinLength", "3"),
                Assertions.tuple("MaxLength", "4"),
                Assertions.tuple("Pattern", "[A-Z]{3}"),
                Assertions.tuple("Enumeration", "FOO"),
                Assertions.tuple("Enumeration", "BAR"));
        assertThat(fL.get(0).docL()).isEmpty();
        assertThat(fL.get(1).docL()).isEmpty();
        assertThat(fL.get(2).docL()).isEmpty();         
        assertThat(fL.get(3).docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(Assertions.tuple("The FOO token", "en-US"));
        assertThat(fL.get(4).docL()).isEmpty();      
        
        dt = model.qnToDatatype("test:Restrict2Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("MinInclusive", "10"),
                Assertions.tuple("MaxInclusive", "20"));

        dt = model.qnToDatatype("test:Restrict3Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("MinExclusive", "30"),
                Assertions.tuple("MaxExclusive", "40"));

        dt = model.qnToDatatype("test:Restrict4Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:string", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("MinLength", "3"),
                Assertions.tuple("MaxLength", "4"));

        dt = model.qnToDatatype("test:Restrict5Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:string", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("Length", "3"),
                Assertions.tuple("WhiteSpace", "collapse"));
        

        dt = model.qnToDatatype("test:Restrict6Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:decimal", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("FractionDigits", "2"),
                Assertions.tuple("TotalDigits", "5"));
        

        dt = model.qnToDatatype("test:Restrict7Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:token", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("Enumeration", "GB"),
                Assertions.tuple("Enumeration", "US"));  
        assertThat(fL.get(0).docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(
                Assertions.tuple("UNITED KINGDOM", "en-US"));
        assertThat(fL.get(1).docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(
                Assertions.tuple("UNITED STATES", "en-US"),
                Assertions.tuple("LES ETAS-UNIS", "fr"));
    
        dt = model.qnToDatatype("test:Restrict8Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:token", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("Enumeration", "GB"),
                Assertions.tuple("Enumeration", "US"));      
    
        dt = model.qnToDatatype("test:Restrict9Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL).isEmpty();
      
        dt = model.qnToDatatype("test:Restrict10Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL).isEmpty();
      
        dt = model.qnToDatatype("test:UnionType");
        assertThat(dt.memberL())
            .extracting(Datatype::qname)
            .containsExactlyInAnyOrder("xs:integer", "xs:float", "test:Restrict8Type");
    }
    
    @Test
    public void testLiteralClass () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/literalClass.xsd");
        var model = mb.createModel(sch);
        var lp = model.qnToDataProperty("test:TestCodeLiteral");

        assertNotNull(model.prefixToNamespaceObj("xml"));
        assertNotNull(model.qnToClassType("test:PersonNameTextType"));
        assertNotNull(model.qnToClassType("test:ProperNameTextType"));
        assertNotNull(model.qnToClassType("test:TextType"));   
        assertNotNull(model.qnToDataProperty("test:TextLiteral"));
        assertNotNull(model.qnToClassType("test:FooTopType"));  
        assertNull(model.qnToClassType("test:FooMiddleType"));  
        assertNull(model.qnToClassType("test:FooBottomType"));
        assertNotNull(model.qnToDataProperty("test:FooTopLiteral"));
    
    }

    @Test
    public void testNamespaces () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd/imports.xsd");
        var model = mb.createModel(sch);
        
        var test = model.prefixToNamespaceObj("test");
        var nc   = model.prefixToNamespaceObj("nc");
        var j    = model.prefixToNamespaceObj("j");
        var xml  = model.prefixToNamespaceObj("xml");
        var gml  = model.prefixToNamespaceObj("gml");
        
        assertEquals("http://example.com/test/", test.uri());
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/", nc.uri());
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/", j.uri());
        assertEquals("http://www.w3.org/XML/1998/namespace", xml.uri());
        assertEquals("http://www.opengis.net/gml/3.2", gml.uri());
        assertThat(gml.impDocL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(Assertions.tuple("Geography Markup Language.", "en-US"));        
        int x = 0;
    }

    @Test
    public void testObjectProperties () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/objectProperty.xsd");
        var model = mb.createModel(sch);     
        var op1 = model.qnToObjectProperty("test:OProp1");
        var op2 = model.qnToObjectProperty("test:OProp2");
        var op3 = model.qnToObjectProperty("test:OProp3");
        var op4 = model.qnToObjectProperty("test:OProp4");
        var op5 = model.qnToObjectProperty("test:OProp5");
        assertEquals("test:TestType", op1.classType().qname());
        assertEquals("test:TestType", op2.classType().qname());
        assertEquals("test:TestType", op3.classType().qname());
        assertEquals("test:TestType", op4.classType().qname());
        assertEquals("test:TestType", op5.classType().qname());
        assertTrue(op1.isAbstract());
        assertFalse(op2.isAbstract());
        assertFalse(op3.isAbstract());
        assertFalse(op4.isAbstract());
        assertFalse(op5.isAbstract());    
        assertEquals("ANY", op1.referenceCode());
        assertEquals("REF", op2.referenceCode());
        assertEquals("URI", op3.referenceCode());
        assertEquals("NONE", op4.referenceCode());
        assertEquals("", op5.referenceCode());
        assertFalse(op1.isRelationship());
        assertTrue(op2.isRelationship());
        assertFalse(op3.isRelationship());
        assertFalse(op4.isRelationship());
        assertFalse(op5.isRelationship());
        assertNull(op1.subProperty());
        assertNull(op2.subProperty());
        assertNull(op3.subProperty());
        assertNull(op4.subProperty());
        assertEquals("test:OProp1", op5.subProperty().qname());
    }
    
    private void debugModelWriter (Model m) throws Exception {
        var outF = new File(resDN + "out.cmf");
        var outS = new FileOutputStream(outF);
        var mw   = new ModelXMLWriter();
        mw.writeXML(m, outS);
        outS.close();      
    }
    
}
