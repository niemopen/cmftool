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
package org.mitre.niem.cmf;

import java.util.List;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mitre.niem.xml.LanguageString;

/**
 * Assertions against a Model object created by a test in ModelXMLReaderTest
 * or ModelFromXSDTest.  Don't want to write the assertions more than once.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelAssertions {
    
    public static void checkAny (Model model) {
        var ct1 = model.qnToClassType("test:Test1Type");
        var ct2 = model.qnToClassType("test:Test2Type");
        var ct3 = model.qnToClassType("test:Test3Type");
        var ct4 = model.qnToClassType("test:Test4Type");
        var ct5 = model.qnToClassType("test:Test5Type");
        
        var ap = ct1.anyL().get(0);
        assertThat(ct1.anyL()).hasSize(1);
        assertEquals("lax", ap.processCode());
        assertEquals("http://someNS/ http://otherNS/", ap.nsConstraint());
        assertFalse(ap.isAttribute());

        ap = ct2.anyL().get(0);
        assertThat(ct2.anyL()).hasSize(1);
        assertEquals("strict", ap.processCode());
        assertEquals("", ap.nsConstraint());
        assertTrue(ap.isAttribute());

        assertThat(ct3.anyL()).hasSize(0);   
        assertThat(ct4.anyL()).hasSize(0);   
        assertThat(ct5.anyL()).hasSize(0);
    }
    
    public static void checkArchVersions (Model m) {
        var nc6 = m.namespaceObj("nc");
        var nc5 = m.namespaceObj("nc5");
        assertEquals("NIEM6.0", nc6.archVersion());
        assertEquals("NIEM5.0", nc5.archVersion());
    }
        
    public static void checkAttAugment (Model m) {
        var tns = m.prefixToNamespaceObj("test");
        var op = m.qnToProperty("test:ObjProp");
        assertThat(tns.augL())
            .extracting(AugmentRecord::classType, AugmentRecord::property, AugmentRecord::minOccurs, 
                AugmentRecord::maxOccurs, AugmentRecord::index)
            .containsExactlyInAnyOrder(
                Assertions.tuple(
                    m.qnToClassType("test:CCOneType"),
                    m.qnToProperty("test:attProp"),
                    "0", "1", ""),
                Assertions.tuple(
                    m.qnToClassType("test:CCTwoType"),
                    m.qnToProperty("test:attProp"),
                    "1", "1", ""),
                Assertions.tuple(
                    m.qnToClassType("test:SCOneType"),
                    m.qnToProperty("test:attProp"),
                    "0", "1", ""),
                 Assertions.tuple(
                    m.qnToClassType("test:SCTwoType"),
                    m.qnToProperty("test:ObjProp"),
                    "0", "1", "")
                 );  
    }
    
    public static void checkAugment (Model m) {
        var jns = m.prefixToNamespaceObj("j");
        var ncns = m.prefixToNamespaceObj("nc");
        var tns = m.prefixToNamespaceObj("test");
        
        assertThat(m.namespaceList())
            .extracting(Namespace::prefix)
            .containsExactly("j", "nc", "test", "xml", "xs");

        assertThat(jns.augL())
            .extracting(AugmentRecord::classType, AugmentRecord::property, AugmentRecord::minOccurs, 
                AugmentRecord::maxOccurs, AugmentRecord::index, AugmentRecord::codeString)
            .containsExactly(
                Assertions.tuple(
                    m.qnToClassType("nc:EducationType"),
                    m.qnToProperty("j:EducationTotalYearsText"),
                    "0", "unbounded", "0", "")
            );
        assertThat(tns.augL())
            .extracting(AugmentRecord::classType, AugmentRecord::property, AugmentRecord::minOccurs, 
                AugmentRecord::maxOccurs, AugmentRecord::index, AugmentRecord::codeString)
            .containsExactlyInAnyOrder(
                Assertions.tuple(
                    m.qnToClassType("nc:EducationType"),
                    m.qnToProperty("nc:personNameCommentText"),
                    "0", "1", "-1", ""),
                Assertions.tuple(
                    m.qnToClassType("nc:EducationType"),
                    m.qnToProperty("test:CommentDestinationText"),
                    "1", "1", "0", ""),
                Assertions.tuple(
                    m.qnToClassType("nc:EducationType"),
                    m.qnToProperty("nc:CommentText"),
                    "0", "1", "1", ""),
                Assertions.tuple(
                    m.qnToClassType("nc:EducationType"),
                    m.qnToProperty("j:EducationTotalYearsText"),
                    "1", "1", "2", ""),
                Assertions.tuple(
                    m.qnToClassType("nc:EducationType"),
                    m.qnToProperty("test:TestAugElement"),
                    "0", "unbounded", "", ""),
                Assertions.tuple(
                    m.qnToClassType("nc:CommentType"),
                    m.qnToProperty("test:CommentDestinationText"),
                    "0", "unbounded", "", "")           
            );            
        assertTrue(jns.augL().get(0).codeS().isEmpty());
        assertThat(tns.augL().get(0).codeS().isEmpty());
    }
    
    public static void checkComponent (Model m) {
        var ct = m.qnToClassType("t:OneClassType");
        var dt = m.qnToDatatype("t:TwoDataType");
        var p1 = m.qnToProperty("t:AnElement");
        var p2 = m.qnToProperty("t:AnotherElement");
        var p3 = m.qnToProperty("t:anAttribute");

        assertTrue(ct.isDeprecated());
        assertThat(ct.docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactlyInAnyOrder(
                Assertions.tuple("OneClassType doc string #1", "en-US"),
                Assertions.tuple("chaîne de documentation", "fr")
            );
            
        assertEquals("TwoDataType", dt.name());
        assertEquals("http://example.com/components/", dt.namespaceURI());
        assertFalse(dt.isDeprecated());
        assertThat(dt.docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(Assertions.tuple("Exercise code list", "en-US"));
        
        assertEquals("AnElement", p1.name());
        assertEquals("http://example.com/components/", p1.namespaceURI());
        assertTrue(p1.isDeprecated());
        assertThat(p1.docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(Assertions.tuple("AnElement doc string #1", "en-US"));
        
        assertEquals("AnotherElement", p2.name());
        assertEquals("http://example.com/components/", p2.namespaceURI());
        assertFalse(p2.isDeprecated());        
        assertThat(p2.docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(Assertions.tuple("AnotherElement doc string #1", "en-US"));
        
        assertEquals("anAttribute", p3.name());
        assertEquals("http://example.com/components/", p3.namespaceURI());
        assertFalse(p3.isDeprecated());   
        assertThat(p3.docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(Assertions.tuple("Attribute doc string", "en-US"));        
    }
        
    public static void checkClass (Model m) {
        var ct1 = m.qnToClassType("test:Test1Type");
        var ct2 = m.qnToClassType("test:Test2Type");
        var ct3 = m.qnToClassType("test:Test3Type");
        var ct4 = m.qnToClassType("test:Test4Type");
        var ct5 = m.qnToClassType("test:Test5Type");
        
        var cp = ct1.propL().get(0);
        assertTrue(ct1.isAbstract());
        assertNull(ct1.subClassOf());
        assertThat(ct1.propL()).hasSize(1);
        assertEquals("test:AnElement", cp.property().qname());
        assertEquals("0", cp.minOccurs());
        assertEquals("1", cp.maxOccurs());
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
        assertNull(ct2.subClassOf());
        assertThat(ct2.propL()).hasSize(1);
        assertEquals("test:AnElement", cp.property().qname());
        assertEquals("1", cp.minOccurs());
        assertEquals("1", cp.maxOccurs());
        assertThat(cp.docL()).hasSize(0);
        ap = ct2.anyL().get(0);
        assertThat(ct2.anyL()).hasSize(1);
        assertEquals("strict", ap.processCode());
        assertEquals("", ap.nsConstraint());
        assertTrue(ap.isAttribute());
        
        cp = ct3.propL().get(0);
        assertFalse(ct3.isAbstract());
        assertEquals("test:Test1Type", ct3.subClassOf().qname());
        assertThat(ct3.propL()).hasSize(1);
        assertEquals("test:AnotherElement", cp.property().qname());
        assertEquals("1", cp.minOccurs());
        assertEquals("1", cp.maxOccurs());
        assertThat(cp.docL()).hasSize(0);
        assertThat(ct3.anyL()).hasSize(0);   
            }
    
    // Test the model built from xsd?/codeListBinding.xsd or read from cmf/codeListBinding.cmf
    // Test codeListURI, column name, isConstraining
    public static void checkCodeListBinding (Model m) {
        var dt = m.qnToDatatype("test:TCodeList2Type");
        var clb = dt.codeListBinding();
        assertEquals("http://code.list.uri", clb.codeListURI());
        assertEquals("", clb.column());
        assertTrue(clb.isConstraining());

        dt = m.qnToDatatype("test:TCodeList3Type");
        clb = dt.codeListBinding();
        assertEquals("http://list.uri", clb.codeListURI());
        assertEquals("key", clb.column());
        assertFalse(clb.isConstraining());    
    }
//        
//    // Test the model built from xsd?/component.xsd or read from cmf/component.cmf
//    // Test name, namespace, documentation, and deprecated properties of each component
//    // Test class, property, datatype components
//    public static void checkComponent (Model m) {
//        var ct = m.qnToClassType("test:Test1Type");
//        var dt = m.qnToDatatype("test:Test2DataType");
//        var p1 = m.qnToProperty("test:AnElement");
//        var p2 = m.qnToProperty("test:AnotherElement");
//        var p3 = m.qnToProperty("test:anAttribute");
//        
//        assertEquals("Test1Type", ct.name());
//        assertEquals("http://example.com/components/", ct.namespaceURI());
//        assertTrue(ct.isDeprecated());
//        assertThat(ct.docL())
//            .extracting(LanguageString::text, LanguageString::lang)
//            .containsExactly(
//                Assertions.tuple("Test1Type doc string #1", "en-US"),
//                Assertions.tuple("Voulez -vous coucher avec moi?", "fr")
//            );
//        
//        assertEquals("Test2DataType", dt.name());
//        assertEquals("http://example.com/components/", dt.namespaceURI());
//        assertFalse(dt.isDeprecated());
//        assertThat(dt.docL())
//            .extracting(LanguageString::text, LanguageString::lang)
//            .containsExactly(Assertions.tuple("Test2DataType doc string", "en-US"));
//        
//        assertEquals("AnElement", p1.name());
//        assertEquals("http://example.com/components/", p1.namespaceURI());
//        assertTrue(p1.isDeprecated());
//        assertThat(p1.docL())
//            .extracting(LanguageString::text, LanguageString::lang)
//            .containsExactly(Assertions.tuple("AnElement doc string #1", "en-US"));
//        
//        assertEquals("AnotherElement", p2.name());
//        assertEquals("http://example.com/components/", p2.namespaceURI());
//        assertFalse(p2.isDeprecated());        
//        assertThat(p2.docL())
//            .extracting(LanguageString::text, LanguageString::lang)
//            .containsExactly(Assertions.tuple("AnotherElement doc string #1", "en-US"));
//        
//        assertEquals("anAttribute", p3.name());
//        assertEquals("http://example.com/components/", p3.namespaceURI());
//        assertFalse(p3.isDeprecated());   
//        assertThat(p3.docL())
//            .extracting(LanguageString::text, LanguageString::lang)
//            .containsExactly(Assertions.tuple("Attribute doc string", "en-US"));
//    }
    
    // Test the model build from xsd?/datatypes.xsd or read from cmf/datatypes.cmf
    // Test abstract, relationship, attribute, refAttribute.    
    public static void checkDataProperty (Model m) {
        assertTrue(m.qnToProperty("test:DataProperty").isAbstract());
        assertFalse(m.qnToProperty("test:DProp2").isAbstract());
        
        assertFalse(m.qnToProperty("test:DataProperty").isRelationship());
        assertFalse(m.qnToProperty("test:DProp2").isRelationship());
        assertTrue (m.qnToProperty("test:DProp3").isRelationship());
        assertFalse(m.qnToProperty("test:DProp4").isRelationship());
        assertTrue (m.qnToProperty("test:DProp5").isRelationship());
        assertFalse(m.qnToProperty("test:aProp2").isRelationship());
        assertTrue (m.qnToProperty("test:aProp3").isRelationship());
        assertFalse(m.qnToProperty("test:aProp4").isRelationship());
        assertTrue (m.qnToProperty("test:aProp5").isRelationship());

        assertFalse(m.qnToDataProperty("test:DataProperty").isAttribute());
        assertFalse(m.qnToDataProperty("test:DProp2").isAttribute());
        assertFalse(m.qnToDataProperty("test:DProp3").isAttribute());
        assertFalse(m.qnToDataProperty("test:DProp4").isAttribute());
        assertFalse(m.qnToDataProperty("test:DProp5").isAttribute());
        assertTrue (m.qnToDataProperty("test:aProp2").isAttribute());
        assertTrue (m.qnToDataProperty("test:aProp3").isAttribute());
        assertTrue (m.qnToDataProperty("test:aProp4").isAttribute());
        assertTrue (m.qnToDataProperty("test:aProp5").isAttribute());        
        
        assertFalse(m.qnToDataProperty("test:DataProperty").isRefAttribute());
        assertFalse(m.qnToDataProperty("test:DProp2").isRefAttribute());
        assertFalse(m.qnToDataProperty("test:DProp3").isRefAttribute());
        assertFalse(m.qnToDataProperty("test:DProp4").isRefAttribute());
        assertFalse(m.qnToDataProperty("test:DProp5").isRefAttribute());
        assertFalse(m.qnToDataProperty("test:aProp2").isRefAttribute());
        assertFalse(m.qnToDataProperty("test:aProp3").isRefAttribute());
        assertTrue (m.qnToDataProperty("test:aProp4").isRefAttribute());
        assertTrue (m.qnToDataProperty("test:aProp5").isRefAttribute());           
    
        assertFalse(m.qnToDataProperty("test:DataProperty").isOrdered());
        assertFalse(m.qnToDataProperty("test:DProp2").isOrdered());
        assertFalse(m.qnToDataProperty("test:DProp3").isOrdered());
        assertFalse(m.qnToDataProperty("test:DProp4").isOrdered());
        assertTrue(m.qnToDataProperty("test:DProp5").isOrdered());
        assertFalse(m.qnToDataProperty("test:aProp2").isOrdered());
        assertFalse(m.qnToDataProperty("test:aProp3").isOrdered());
        assertFalse (m.qnToDataProperty("test:aProp4").isOrdered());
        assertFalse(m.qnToDataProperty("test:aProp5").isOrdered()); 
    }

    // Test the model build from xsd?/datatypes.xsd or read from cmf/datatypes.cmf
    //      Test list of named simple, union, restriction types
    //      Test union of named simple types and restriction type
    //      Test restriction type and all facets
    //      Test empty restriction type
    public static void checkDatatypes (Model m) {
        var dt = m.qnToDatatype("test:List1Type");
        var docL = dt.docL();
        var item = dt.itemType();
        assertThat(docL).extracting(LanguageString::text).containsExactly("List of a named simple type");
        assertEquals("xs:integer", item.qname());
        
        dt = m.qnToDatatype("test:List2Type");
        item = dt.itemType();
        assertEquals("test:UnionType", item.qname());
        
        dt = m.qnToDatatype("test:List3Type");
        item = dt.itemType();
        assertEquals("test:Restrict8Type", item.qname());
        
        assertNotNull(m.qnToClassType("test:Literal1Type"));
        var dp = m.qnToDataProperty("test:Literal1Literal");
        assertEquals("test:Restrict8Type", dp.datatype().qname());
        
        assertNotNull(m.qnToClassType("test:Literal2Type"));        
        dp = m.qnToDataProperty("test:Literal2Literal");
        assertEquals("test:Literal2SimpleType", dp.datatype().qname());

        assertNotNull(m.qnToClassType("test:Literal3Type"));
        dp = m.qnToDataProperty("test:Literal3Literal");
        assertEquals("xs:token", dp.datatype().qname());

        assertNotNull(m.qnToClassType("test:Literal4Type"));
        dp = m.qnToDataProperty("test:Literal4Literal");
        assertEquals("xs:integer", dp.datatype().qname());
        
        assertNull(m.qnToDatatype("test:Literal1Type"));
        assertNull(m.qnToDatatype("test:Literal2Type"));        
        assertNull(m.qnToDatatype("test:Literal3Type"));
        assertNull(m.qnToDatatype("test:Literal4Type"));
        assertNull(m.qnToDatatype("test:Restrict8SimpleType"));
        
        dt = m.qnToDatatype("test:Restrict1Type");
        var base = dt.base();
        var fL = dt.facetL();
        assertEquals("xs:token", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactly(
                Assertions.tuple("enumeration", "BAR"),
                Assertions.tuple("enumeration", "FOO"),
                Assertions.tuple("maxLength", "4"),
                Assertions.tuple("minLength", "3"),
                Assertions.tuple("pattern", "[A-Z]{3}")
            );
        assertThat(fL.get(0).docL()).isEmpty();
        assertThat(fL.get(4).docL()).isEmpty();
        assertThat(fL.get(2).docL()).isEmpty();         
        assertThat(fL.get(1).docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(Assertions.tuple("The FOO token", "en-US"));
        assertThat(fL.get(3).docL()).isEmpty();      
        
        dt = m.qnToDatatype("test:Restrict2Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("minInclusive", "10"),
                Assertions.tuple("maxInclusive", "20"));

        dt = m.qnToDatatype("test:Restrict3Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("minExclusive", "30"),
                Assertions.tuple("maxExclusive", "40"));

        dt = m.qnToDatatype("test:Restrict4Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:string", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("minLength", "3"),
                Assertions.tuple("maxLength", "4"));

        dt = m.qnToDatatype("test:Restrict5Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:string", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("length", "3"),
                Assertions.tuple("whiteSpace", "collapse"));
        

        dt = m.qnToDatatype("test:Restrict6Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:decimal", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("fractionDigits", "2"),
                Assertions.tuple("totalDigits", "5"));
        

        dt = m.qnToDatatype("test:Restrict7Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:token", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("enumeration", "GB"),
                Assertions.tuple("enumeration", "US"));  
        assertThat(fL.get(0).docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(
                Assertions.tuple("UNITED KINGDOM", "en-US"));
        assertThat(fL.get(1).docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(
                Assertions.tuple("UNITED STATES", "en-US"),
                Assertions.tuple("LES ETAS-UNIS", "fr"));
    
        dt = m.qnToDatatype("test:Restrict8Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:token", base.qname());
        assertThat(fL)
            .extracting(Facet::category, Facet::value)
            .containsExactlyInAnyOrder(
                Assertions.tuple("enumeration", "GB"),
                Assertions.tuple("enumeration", "US"));      
    
        dt = m.qnToDatatype("test:Restrict9Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL).isEmpty();
      
        dt = m.qnToDatatype("test:Restrict10Type");
        base = dt.base();
        fL = dt.facetL();
        assertEquals("xs:integer", base.qname());
        assertThat(fL).isEmpty();
      
        dt = m.qnToDatatype("test:UnionType");
        assertThat(dt.memberL())
            .extracting(Datatype::qname)
            .containsExactlyInAnyOrder("xs:integer", "xs:float", "test:Restrict8Type");
    }
    
    public static void checkExternals (Model m) throws Exception {
        assertNotNull(m.prefixToNamespaceObj("gml"));
        assertNotNull(m.prefixToNamespaceObj("niem-gml"));
        
        var pL = m.qnToClassType("niem-gml:PointAdapterType").propL();
        var p  = pL.get(0).property();
        var op = (ObjectProperty)p;
        assertEquals("gml:Point", p.qname());
        assertNull(op.classType());    

        pL = m.qnToClassType("niem-gml:PolygonAdapterType").propL();
        p  = pL.get(0).property();
        op = (ObjectProperty)p;
        assertEquals("gml:Polygon", p.qname());
        assertNull(op.classType());        
    }

    public static void checkGaLitAtt (Model m) throws Exception {
        var tns = m.prefixToNamespaceObj("test");
        var op = m.qnToProperty("test:ObjProp");
        assertNull(m.qnToDatatype("test:SCOneType"));
        assertNull(m.qnToDatatype("test:SCTwoType"));
        assertThat(tns.augL())
            .extracting(AugmentRecord::classType, AugmentRecord::property, AugmentRecord::minOccurs, 
                AugmentRecord::maxOccurs, AugmentRecord::index, AugmentRecord::codeString)
            .containsExactly(
                Assertions.tuple(
                    null,
                    m.qnToDataProperty("test:attProp"),
                    "0", "1", "", "LITERAL")
                 );         
    }
    
    public static void checkGaLitObj (Model m) throws Exception {
        var tns = m.prefixToNamespaceObj("test");
        var op = m.qnToProperty("test:ObjProp");
        assertNull(m.qnToDatatype("test:SCOneType"));
        assertNull(m.qnToDatatype("test:SCTwoType"));
        assertThat(tns.augL())
            .extracting(AugmentRecord::classType, AugmentRecord::property, AugmentRecord::minOccurs, 
                AugmentRecord::maxOccurs, AugmentRecord::index, AugmentRecord::codeString)
            .containsExactly(
                Assertions.tuple(
                    null,
                    m.qnToObjectProperty("test:ObjProp"),
                    "0", "1", "", "LITERAL")
                 );          
    }
    
    public static void checkGaObjAtt (Model m) throws Exception {
        var tns = m.prefixToNamespaceObj("test");
        var op = m.qnToProperty("test:ObjProp");
        assertNotNull(m.qnToDatatype("test:SCOneType"));
        assertNotNull(m.qnToDatatype("test:SCTwoType"));
        assertThat(tns.augL())
            .extracting(AugmentRecord::classType, AugmentRecord::property, AugmentRecord::minOccurs, 
                AugmentRecord::maxOccurs, AugmentRecord::index, AugmentRecord::codeString)
            .containsExactly(
                Assertions.tuple(
                    null,
                    m.qnToDataProperty("test:attProp"),
                    "0", "1", "", "OBJECT")
                 );         
    }
    
    public static void checkGaObjObj (Model m) throws Exception {
        var tns = m.prefixToNamespaceObj("test");
        var op = m.qnToProperty("test:ObjProp");
        assertNotNull(m.qnToDatatype("test:SCOneType"));
        assertNotNull(m.qnToDatatype("test:SCTwoType"));
        assertThat(tns.augL())
            .extracting(AugmentRecord::classType, AugmentRecord::property, AugmentRecord::minOccurs, 
                AugmentRecord::maxOccurs, AugmentRecord::index, AugmentRecord::codeString)
            .containsExactlyInAnyOrder(
                Assertions.tuple(
                    null,
                    m.qnToDataProperty("test:DataProp"),
                    "1", "1", "1", "OBJECT"),
                 Assertions.tuple(
                    null,
                    m.qnToObjectProperty("test:ObjProp"),
                    "1", "1", "0", "OBJECT")
                 );         
    }
    
    public static void checkImports (Model m) {
        var test = m.prefixToNamespaceObj("test");
        var nc   = m.prefixToNamespaceObj("nc");
        var j    = m.prefixToNamespaceObj("j");
        var xml  = m.prefixToNamespaceObj("xml");
        var gml  = m.prefixToNamespaceObj("gml");
        
        assertEquals("NIEM6.0", test.archVersion());
        assertEquals("NIEM6.0", nc.archVersion());
        assertEquals("NIEM6.0", j.archVersion());
        assertEquals("", xml.archVersion());
        assertEquals("", gml.archVersion());
        
        assertEquals("http://example.com/test/", test.uri());
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/", nc.uri());
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/", j.uri());
        assertEquals("http://www.w3.org/XML/1998/namespace", xml.uri());
        assertEquals("http://www.opengis.net/gml/3.2", gml.uri());
        
        assertThat(test.idocL("http://www.opengis.net/gml/3.2"))
            .extracting(LanguageString::text)
            .containsExactly("Geography Markup Language.");
    }
        
    public static void checkList (Model m) throws Exception {
        var oneDt = m.qnToDatatype("t:NonNegativeDoubleListType");
        var twoDt = oneDt.itemType();
        var threeDt = twoDt.base();
        assertEquals("t:NonNegativeDoubleType", twoDt.qname());
        assertEquals("xs:double", threeDt.qname());   
    }
        
    public static void checkLiteralClass (Model m) throws Exception {
        assertNotNull(m.prefixToNamespaceObj("xml"));
        assertNotNull(m.qnToClassType("test:PersonNameTextType"));
        assertNotNull(m.qnToClassType("test:ProperNameTextType"));
        assertNotNull(m.qnToClassType("test:TextType"));   
        assertNotNull(m.qnToDataProperty("test:TextLiteral"));
        assertNotNull(m.qnToClassType("test:FooTopType"));  
        assertNull(m.qnToClassType("test:FooMiddleType"));
        assertNull(m.qnToClassType("test:FooBottomType"));
        assertNotNull(m.qnToDataProperty("test:FooTopLiteral"));   
    }
        
    public static void checkLiteralProps (Model m) throws Exception {
        var dp = m.qnToDataProperty("t:OneLiteral");
        assertEquals("xs:token", dp.datatype().qname());
        dp = m.qnToDataProperty("t:TwoLiteral");
        assertEquals("xs:token", dp.datatype().qname());
        dp = m.qnToDataProperty("t:ThreeLiteral");
        assertEquals("t:SomeType", dp.datatype().qname());
        dp = m.qnToDataProperty("t:FourLiteral");
        assertEquals("t:FourSimpleType", dp.datatype().qname());
        dp = m.qnToDataProperty("t:FiveLiteral");
        assertEquals("t:AnotherType", dp.datatype().qname());
        dp = m.qnToDataProperty("t:SixLiteral");
        assertEquals("t:AnotherType", dp.datatype().qname());
        dp = m.qnToDataProperty("t:SevenLiteral");
        assertEquals("t:ListType", dp.datatype().qname());
        dp = m.qnToDataProperty("t:EightLiteral");
        assertNull(dp);   
    }
    
    // Test the model built from xsd?/localTerm.xsd or read from cmf/localTerm.cmf
    // Test term, literal, sourceURI, citation, documentation
    public static void checkLocalTerm (Model m) {
        Namespace ns = m.prefixToNamespaceObj("test");
        List<LocalTerm> lts = ns.locTermL();
        assertEquals(3, lts.size());
        assertThat(lts).extracting(LocalTerm::term)
                .containsOnly("2D", "3D", "Test");
        
        for (var lt: lts) {
            switch(lt.term()) {
            case "2D": 
                assertEquals("Two-dimensional", lt.literal());
                assertEquals("", lt.documentation());
                assertEquals(0, lt.sourceL().size());
                assertEquals(0, lt.citationL().size());
                break;
            case "3D":
                assertTrue(lt.literal().isEmpty());
                assertEquals("Three-dimensional", lt.documentation());
                assertEquals(0, lt.sourceL().size());
                assertEquals(0, lt.citationL().size());
                break;
             case "Test":
                assertTrue(lt.literal().isEmpty());
                assertEquals("only for test purposes", lt.documentation());
                assertThat(lt.sourceL()).containsOnly("http://example.com/1","http://example.com/2");
                assertThat(lt.citationL())
                    .extracting(LanguageString::text, LanguageString::lang)
                    .containsExactly(
                        Assertions.tuple("citation #1", "en-US"),
                        Assertions.tuple("citation numéro deux", "fr")
                    );
                break;
            }
        }     
    }    

    // Test the model built from xsd?/namespace.xsd or read from cmf/namespace.cmf 
    // Test CTAs, filepath, version, language
    public static void checkNamespace (Model m) {
        assertThat(m.namespaceSet())
            .extracting(Namespace::prefix)
            .containsExactlyInAnyOrder("t", "nc", "xml", "xs");
        
        Namespace ns = m.prefixToNamespaceObj("t");
        assertEquals("t", ns.prefix());
        assertEquals("http://example.com/namespace/", ns.uri());
        assertEquals("namespace.xsd", ns.documentFilePath());
        assertEquals("EXTENSION", ns.kindCode());
        assertEquals("1-alpha.1", ns.version());
        assertEquals("NIEM6.0", ns.archVersion());
        assertEquals("en-US", ns.language());
        assertThat(ns.ctargL())
            .containsExactly("https://docs.oasis-open.org/niemopen/ns/specification/NDR/6.0/#SubsetSchemaDocument",
                "http://example.com/Whatever");
        assertThat(ns.docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(
                Assertions.tuple("Namespace test schema.", "en-US"),
                Assertions.tuple("Test CTAs, documentation, language, filepath, kind, version, NIEMVersion, importDocumentation.", "en-US"));
        
        ns = m.prefixToNamespaceObj("nc");       
        assertEquals("niem/niem-core-skel.xsd", ns.documentFilePath());
        assertEquals("CORE", ns.kindCode());
        assertEquals("ps02", ns.version());
        assertEquals("NIEM6.0", ns.archVersion());
        assertEquals("en-US", ns.language());

        ns = m.prefixToNamespaceObj("xml");       
        assertEquals("niem/external/xml.xsd", ns.documentFilePath());
        assertEquals("XML", ns.kindCode());

        ns = m.prefixToNamespaceObj("xs");       
        assertEquals("", ns.documentFilePath());
        assertEquals("XSD", ns.kindCode());
    }
    
    // Test the model built from xsd?/objectProperty.xsd or read from cmf/objectProperty.cmf 
    // Test abstract, relationship.
    public static void checkObjectProperty (Model m) {
        var op1 = m.qnToObjectProperty("test:OProp1");
        var op2 = m.qnToObjectProperty("test:OProp2");
        var op3 = m.qnToObjectProperty("test:OProp3");
        var op4 = m.qnToObjectProperty("test:OProp4");
        var op5 = m.qnToObjectProperty("test:OProp5");
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
        
        assertFalse(op1.isRelationship());
        assertTrue(op2.isRelationship());
        assertFalse(op3.isRelationship());
        assertFalse(op4.isRelationship());
        assertFalse(op5.isRelationship());
        
        assertFalse(op1.isOrdered());
        assertFalse(op2.isOrdered());
        assertFalse(op3.isOrdered());
        assertTrue(op4.isOrdered());
        assertFalse(op5.isOrdered());        
        
        assertNull(op1.subPropertyOf());
        assertNull(op2.subPropertyOf());
        assertNull(op3.subPropertyOf());
        assertNull(op4.subPropertyOf());
        assertEquals("test:OProp1", op5.subPropertyOf().qname());
    }  

    public static void checkPropAssoc (Model m) {
        var propList = m.qnToClassType("test:T2Type").propL();
        for (var pa: propList) {
            switch (pa.property().name()) {
            case "OProp2": 
                assertEquals("1", pa.minOccurs());
                assertEquals("1", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                break;
            case "OProp3": 
                assertEquals("1", pa.minOccurs());
                assertEquals("unbounded", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                break;
            case "OProp4": 
                assertEquals("0", pa.minOccurs());
                assertEquals("1", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                break;
            case "OProp5": 
                assertEquals("0", pa.minOccurs());
                assertEquals("20", pa.maxOccurs());
                assertEquals("reason for OProp5", pa.docL().get(0).text());
                break;
            case "AProp2": 
                assertEquals("1", pa.minOccurs());
                assertEquals("1", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                break;
            case "AProp3": 
                assertEquals("0", pa.minOccurs());
                assertEquals("1", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                break;
            case "AProp4": 
                assertEquals("0", pa.minOccurs());
                assertEquals("1", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                break;
            }                
        }
    }
    
    public static void checkRefCode (Model m) {
        var t1 = m.qnToClassType("t:OneType");
        var t2 = m.qnToClassType("t:TwoType");
        var t3 = m.qnToClassType("t:ThreeType");
        var t4 = m.qnToClassType("t:FourType");
        var t5 = m.qnToClassType("t:FiveType");    
        assertEquals("", t1.referenceCode());
        assertEquals("NONE", t1.effectiveReferenceCode());
        
        assertEquals("NONE", t2.referenceCode());
        assertEquals("NONE", t2.effectiveReferenceCode());
        
        assertEquals("", t3.referenceCode());
        assertEquals("NONE", t3.effectiveReferenceCode());
        
        assertEquals("INTERNAL", t4.referenceCode());
        assertEquals("INTERNAL", t4.effectiveReferenceCode());
        
        assertEquals("", t5.referenceCode());
        assertEquals("INTERNAL", t5.effectiveReferenceCode());
        
        var p3 = m.qnToObjectProperty("t:ThreeProp");
        var p4 = m.qnToObjectProperty("t:FourProp");
        var p5 = m.qnToObjectProperty("t:FiveProp");
        
        assertEquals("", p3.referenceCode());
        assertEquals("NONE", p3.effectiveReferenceCode());
        
        assertEquals("INTERNAL", p4.referenceCode());
        assertEquals("INTERNAL", p4.effectiveReferenceCode());

        assertEquals("ANY", p5.referenceCode());
        assertEquals("ANY", p5.effectiveReferenceCode());
    }
    
    public static void checkSimpleTypes (Model m) {
        var oneDt = m.qnToDatatype("t:OneType");
        var twoDt = m.qnToDatatype("t:TwoType");
        var fooDt = m.qnToDatatype("t:FooType");
        assertEquals("xs:string", oneDt.base().qname());
        assertEquals("t:OneType", twoDt.base().qname());
        assertEquals("xs:integer", fooDt.base().qname());       
    }
    
    public static void checkUnion (Model m) {
        var dt = m.qnToDatatype("t:TelephoneNumberCategoryCodeType");
        var u  = (Union)dt;
        assertThat(u.memberL())
            .extracting(Datatype::qname)
            .containsExactlyInAnyOrder(
                "t:CategoryCodeType", 
                "t:TelephoneNumberCategoryAdditionalCodeType");
    }
}

