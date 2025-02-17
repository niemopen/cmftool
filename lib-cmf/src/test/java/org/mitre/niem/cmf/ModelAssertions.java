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
import static org.junit.jupiter.api.Assertions.fail;
import org.mitre.niem.xml.LanguageString;

/**
 * Assertions against a Model object created by a derived test class.
 * Don't want to write the assertions more than once.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelAssertions {
    
    public static void checkAugmentRecord (Model m) {
        var jns = m.prefix2namespace("j");
        var ncns = m.prefix2namespace("nc");
        var testns = m.prefix2namespace("test");

        assertThat(jns.augL())
            .extracting(AugmentRecord::classType)
            .containsExactly(
                m.qn2classType("nc:EducationType"),
                m.qn2classType("test:T2Type"));
        assertThat(ncns.augL())
            .extracting(AugmentRecord::classType)
            .containsExactly(
                m.qn2classType("test:T2Type"),
                m.qn2classType("test:T2Type"));
        assertNull(testns.augL().get(0).classType());
        
        assertThat(jns.augL())
            .extracting(AugmentRecord::property)
            .containsExactly(
                m.qn2property("j:EducationTotalYearsText"),
                m.qn2property("test:aProp4"));
        assertThat(ncns.augL())
            .extracting(AugmentRecord::property)
            .containsExactly(
                m.qn2property("test:aProp3"),
                m.qn2property("test:aProp4"));
        assertThat(testns.augL())
            .extracting(AugmentRecord::property)
            .containsExactly(m.qn2property("test:privAtt"));
        
        assertThat(jns.augL())
            .extracting(AugmentRecord::minOccurs, AugmentRecord::maxOccurs, AugmentRecord::index)
            .containsExactly(
                Assertions.tuple("0","unbounded","0"),
                Assertions.tuple("0","1",""));
        assertThat(ncns.augL())
            .extracting(AugmentRecord::minOccurs, AugmentRecord::maxOccurs, AugmentRecord::index)
            .containsExactly(
                Assertions.tuple("0","1",""),
                Assertions.tuple("0","1",""));
        assertThat(testns.augL())
            .extracting(AugmentRecord::minOccurs, AugmentRecord::maxOccurs, AugmentRecord::index)
            .containsExactly(
                Assertions.tuple("0","1",""));
        
        assertTrue(jns.augL().get(0).codeS().isEmpty());
        assertTrue(jns.augL().get(1).codeS().isEmpty());
        assertTrue(ncns.augL().get(0).codeS().isEmpty());
        assertTrue(ncns.augL().get(1).codeS().isEmpty());    
        assertThat(testns.augL().get(0).codeS()).contains("ASSOCIATION", "OBJECT", "LITERAL") ;
    }

    public static void checkChildPropertyAssociation (Model m) {
        var propList = m.qn2classType("test:T2Type").propL();
        for (var pa: propList) {
            switch (pa.property().name()) {
            case "OProp2": 
                assertEquals("1", pa.minOccurs());
                assertEquals("1", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                assertFalse(pa.isOrdered());
                break;
            case "OProp3": 
                assertEquals("1", pa.minOccurs());
                assertEquals("unbounded", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                assertTrue(pa.isOrdered());
                break;
            case "OProp4": 
                assertEquals(0, pa.minOccurs());
                assertEquals("1", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                assertFalse(pa.isOrdered());
                break;
            case "OProp5": 
                assertEquals(0, pa.minOccurs());
                assertEquals(20, pa.maxOccurs());
                assertEquals("reason for OProp5", pa.docL().get(0).text());
                assertFalse(pa.isOrdered());
                break;
            case "AProp2": 
                assertEquals("1", pa.minOccurs());
                assertEquals("1", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                assertFalse(pa.isOrdered());
                break;
            case "AProp3": 
                assertEquals(0, pa.minOccurs());
                assertEquals("1", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                assertFalse(pa.isOrdered());
                break;
            case "AProp4": 
                assertEquals(0, pa.minOccurs());
                assertEquals("1", pa.maxOccurs());
                assertTrue(pa.docL().isEmpty());
                assertFalse(pa.isOrdered());
                break;
            }                
        }
    }
        
    // Test abstract, reference code, subclass
    public static void checkClass (Model m) {
        assertNotNull(m.namespace("http://example.com/test/"));
        assertNotNull(m.namespace("http://www.w3.org/2001/XMLSchema"));
        assertNotNull(m.namespace("test"));
        assertNotNull(m.namespace("xs"));
        assertNull(m.namespace("http://boogala"));
        assertNull(m.namespace("boog"));
            
        var ct = m.qn2classType("test:Test1Type");
        var ct2 = m.qn2classType("test:Test2Type");
        var ct3 = m.qn2classType("test:Test3Type");
        var ct4 = m.qn2classType("test:Test4Type");
        var ct5 = m.qn2classType("test:Test5Type");
        assertTrue(ct.isAbstract());
        assertFalse(ct2.isAbstract());
        assertFalse(ct3.isAbstract());
        assertFalse(ct4.isAbstract());
        assertFalse(ct5.isAbstract());
        assertEquals("ANY", ct.referenceCode());
        assertEquals("ANY", ct2.referenceCode());
        assertEquals("URI", ct3.referenceCode());
        assertEquals("REF", ct4.referenceCode());
        assertEquals("NONE", ct5.referenceCode());
        assertNull(ct.subClass());
        assertNull(ct2.subClass());
        assertEquals(ct3.subClass(), ct);
        assertEquals(ct4.subClass(), ct);
        assertEquals(ct5.subClass(), ct);
    }
    
    // Test the model built from xsd?/codeListBinding.xsd or read from cmf/codeListBinding.cmf
    // Test codeListURI, column name, isConstraining
    public static void checkCodeListBinding (Model m) {
        var clb = m.qn2datatype("test:TCodeList2Type").codeListBinding();
        assertEquals("http://code.list.uri", clb.codeListURI());
        assertEquals("#code", clb.column());
        assertTrue(clb.isConstraining());
        
        clb = m.qn2datatype("test:TCodeList3Type").codeListBinding();
        assertEquals("http://list.uri", clb.codeListURI());
        assertEquals("key", clb.column());
        assertFalse(clb.isConstraining());        
    }
        
    // Test the model built from xsd?/component.xsd or read from cmf/component.cmf
    // Test name, namespace, documentation, and deprecated properties of each component
    // Test class, property, datatype components
    public static void checkComponent (Model m) {
        var ct = m.qn2classType("test:Test1Type");
        var dt = m.qn2datatype("test:Test2DataType");
        var p1 = m.qn2property("test:AnElement");
        var p2 = m.qn2property("test:AnotherElement");
        var p3 = m.qn2property("test:anAttribute");
        
        assertEquals("Test1Type", ct.name());
        assertEquals("http://example.com/components/", ct.namespaceURI());
        assertTrue(ct.isDeprecated());
        assertThat(ct.docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(
                Assertions.tuple("Test1Type doc string #1", "en-US"),
                Assertions.tuple("Voulez -vous coucher avec moi?", "fr")
            );
        
        assertEquals("Test2DataType", dt.name());
        assertEquals("http://example.com/components/", dt.namespaceURI());
        assertFalse(dt.isDeprecated());
        assertThat(dt.docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(Assertions.tuple("Test2DataType doc string", "en-US"));
        
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
    
    // Test the model build from xsd?/datatypes.xsd or read from cmf/datatypes.cmf
    // Test abstract, relationship, attribute, refAttribute.    
    public static void checkDataProperty (Model m) {
        assertTrue(m.qn2property("test:DataProperty").isAbstract());
        assertFalse(m.qn2property("test:DProp2").isAbstract());
        
        assertFalse(m.qn2property("test:DataProperty").isRelationship());
        assertFalse(m.qn2property("test:DProp2").isRelationship());
        assertTrue (m.qn2property("test:DProp3").isRelationship());
        assertFalse(m.qn2property("test:DProp4").isRelationship());
        assertTrue (m.qn2property("test:DProp5").isRelationship());
        assertFalse(m.qn2property("test:AProp2").isRelationship());
        assertTrue (m.qn2property("test:AProp3").isRelationship());
        assertFalse(m.qn2property("test:AProp4").isRelationship());
        assertTrue (m.qn2property("test:AProp5").isRelationship());

        assertFalse(m.qn2dataProperty("test:DataProperty").isAttribute());
        assertFalse(m.qn2dataProperty("test:DProp2").isAttribute());
        assertFalse(m.qn2dataProperty("test:DProp3").isAttribute());
        assertFalse(m.qn2dataProperty("test:DProp4").isAttribute());
        assertFalse(m.qn2dataProperty("test:DProp5").isAttribute());
        assertTrue (m.qn2dataProperty("test:AProp2").isAttribute());
        assertTrue (m.qn2dataProperty("test:AProp3").isAttribute());
        assertTrue (m.qn2dataProperty("test:AProp4").isAttribute());
        assertTrue (m.qn2dataProperty("test:AProp5").isAttribute());        
        
        assertFalse(m.qn2dataProperty("test:DataProperty").isRefAttribute());
        assertFalse(m.qn2dataProperty("test:DProp2").isRefAttribute());
        assertFalse(m.qn2dataProperty("test:DProp3").isRefAttribute());
        assertFalse(m.qn2dataProperty("test:DProp4").isRefAttribute());
        assertFalse(m.qn2dataProperty("test:DProp5").isRefAttribute());
        assertFalse(m.qn2dataProperty("test:AProp2").isRefAttribute());
        assertFalse(m.qn2dataProperty("test:AProp3").isRefAttribute());
        assertTrue (m.qn2dataProperty("test:AProp4").isRefAttribute());
        assertTrue (m.qn2dataProperty("test:AProp5").isRefAttribute());           
    }

    // Test the model build from xsd?/datatypes.xsd or read from cmf/datatypes.cmf
    //      Test list of named simple, union, restriction types
    //      Test union of named simple types and restriction type
    //      Test restriction type and all facets
    //      Test empty restriction type
    public static void checkDatatype (Model m) {
       var dtFlt = m.qn2datatype("xs:float");
       var dtInt = m.qn2datatype("xs:integer");
       var dtLs1 = m.qn2datatype("test:List1Type"); 
       var dtLs2 = m.qn2datatype("test:List2Type");
       var dtUn  = m.qn2datatype("test:UnionType");
       
       assertEquals(dtInt, dtLs1.itemType()); 
       assertTrue(dtLs1.isOrdered());
       assertEquals(dtUn, dtLs2.itemType());
       assertFalse(dtLs2.isOrdered());
       
       assertThat(dtUn.memberL())
               .extracting(Datatype::qname)
               .containsExactlyInAnyOrder("xs:integer",
                       "xs:float",
                       "test:Restrict8Type");
       
       var rdt = m.qn2datatype("test:Restrict1Type");
       assertEquals("xs:token", rdt.base().qname());
       assertEquals(5, rdt.facetL().size());
       for (var f : rdt.facetL()) {
           switch (f.category()) {
               case "minLength" -> assertEquals("3", f.value());
               case "maxLength" -> assertEquals("4", f.value());
               case "pattern"   -> assertEquals("[A-Z]{3}", f.value());
               case "enumeration" -> {
                   switch (f.value()) {
                       case "FOO" -> assertEquals("The FOO token", f.docL().get(0).text());
                       case "BAR" -> assertTrue(f.docL().isEmpty());
                       default -> fail("bogus enumeration");
                   }
               }
               default -> fail("bogus facet");
           }
       }
       rdt = m.qn2datatype("test:Restrict2Type");
       assertEquals("xs:integer", rdt.base().qname());
       for (var f : rdt.facetL()) {
           switch (f.category()) {
               case "minInclusive" -> assertEquals("10", f.value());
               case "maxInclusive" -> assertEquals("20", f.value());
               default -> fail("bogus facet");
           }
       }
       rdt = m.qn2datatype("test:Restrict3Type");
       assertEquals("xs:integer", rdt.base().qname());
       for (var f : rdt.facetL()) {
           switch (f.category()) {
               case "minExclusive" -> assertEquals("10", f.value());
               case "maxExclusive" -> assertEquals("20", f.value());
               default -> fail("bogus facet");
           }
       }
       rdt = m.qn2datatype("test:Restrict4Type");
       assertEquals("xs:string", rdt.base().qname());
       for (var f : rdt.facetL()) {
           switch (f.category()) {
               case "minLength" -> assertEquals("3", f.value());
               case "maxLength" -> assertEquals("4", f.value());
               default -> fail("bogus facet");
           }
       }       
       rdt = m.qn2datatype("test:Restrict5Type");
       assertEquals("xs:string", rdt.base().qname());
       for (var f : rdt.facetL()) {
           switch (f.category()) {
               case "length"     -> assertEquals("3", f.value());
               case "whiteSpace" -> assertEquals("collapse", f.value());
               default -> fail("bogus facet");
           }
       }       
       rdt = m.qn2datatype("test:Restrict6Type");
       assertEquals("xs:decimal", rdt.base().qname());
       for (var f : rdt.facetL()) {
           switch (f.category()) {
               case "fractionDigits" -> assertEquals("2", f.value());
               case "totalDigits"    -> assertEquals("5", f.value());
               default -> fail("bogus facet");
           }
       }  
       rdt = m.qn2datatype("test:Restrict7Type");
       assertEquals("xs:token", rdt.base().qname());
       for (var f : rdt.facetL()) {
           switch (f.category()) {
               case "enumeration" -> {
                   switch (f.value()) {
                       case "GB" -> assertEquals("UNITED KINGDOM", f.docL().get(0).text());
                       case "US" -> assertEquals("UNITED STATES", f.docL().get(0).text());
                       default -> fail("bogus enumeration");
                   }
               }
               default -> fail("bogus facet");
           }
       }  
       rdt = m.qn2datatype("test:Restrict8Type");
       assertEquals("xs:token", rdt.base().qname());
       for (var f : rdt.facetL()) {
           switch (f.category()) {
               case "enumeration" -> {
                   switch (f.value()) {
                       case "GB" -> assertTrue(f.docL().isEmpty());
                       case "US" -> assertTrue(f.docL().isEmpty());
                       default -> fail("bogus enumeration");
                   }
               }
               default -> fail("bogus facet");
           }
       }  
       rdt = m.qn2datatype("test:Restrict9Type");
       assertEquals("xs:integer", rdt.base().qname());
       assertEquals(0, rdt.facetL().size());
    }
    
    // Test the model built from xsd?/localTerm.xsd or read from cmf/localTerm.cmf
    // Test term, literal, sourceURI, citation, documentation
    public static void checkLocalTerm (Model m) {
        Namespace ns = m.prefix2namespace("test");
        List<LocalTerm> lts = ns.locTermL();
        assertEquals(3, lts.size());
        assertThat(lts).extracting(LocalTerm::term)
                .containsOnly("2D", "3D", "Test");
        
        for (var lt: lts) {
            switch(lt.term()) {
            case "2D": 
                assertEquals("Two-dimensional", lt.literal());
                assertTrue(lt.docL().isEmpty());
                assertEquals(0, lt.sourceL().size());
                assertEquals(0, lt.citationL().size());
                break;
            case "3D":
                assertTrue(lt.literal().isEmpty());
                assertEquals("Three-dimensional", lt.docL().get(0).text());
                assertEquals(0, lt.sourceL().size());
                assertEquals(0, lt.citationL().size());
                break;
             case "Test":
                assertTrue(lt.literal().isEmpty());
                assertEquals("only for test purposes", lt.docL().get(0).text());
                assertThat(lt.sourceL()).containsOnly("http://example.com/1","http://example.com/2");
                assertThat(lt.citationL())
                    .extracting(LanguageString::text, LanguageString::lang)
                    .containsExactly(Assertions.tuple("citation #1", "en-US"),
                        Assertions.tuple("citation #2", "en-US"));
                break;
            }
        }     
    }    

    // Test the model built from xsd?/namespace.xsd or read from cmf/namespace.cmf 
    // Test CTAs, filepath, version, language
    public static void checkNamespace (Model m) {
        Namespace ns = m.prefix2namespace("test");
        assertEquals("test", ns.prefix());
        assertEquals("http://example.com/namespace/", ns.uri());
        assertEquals("namespace.xsd", ns.documentFilePath());
        assertEquals("1-alpha.1", ns.version());
        assertEquals("en-US", ns.language());
        assertThat(ns.ctargL())
            .containsExactly("https://docs.oasis-open.org/niemopen/ns/specification/XNDR/6.0/#SubsetSchemaDocument",
                "http://example.com/Whatever");
        assertThat(ns.docL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(
                Assertions.tuple("Namespace test schema", "en-US"),
                Assertions.tuple("Test CTAs, filepath, version, language, import doc", "en-US"));     
        assertThat(ns.impDocL())
            .extracting(LanguageString::text, LanguageString::lang)
            .containsExactly(
                Assertions.tuple("imported blah blah", "en-US"));
            }
    
    // Test the model built from xsd?/objectProperty.xsd or read from cmf/objectProperty.cmf 
    // Test abstract, relationship, reference code.
    public static void checkObjectProperty (Model m) {
        assertTrue (m.qn2objectProperty("test:OProp1").isAbstract());
        assertFalse(m.qn2objectProperty("test:OProp2").isAbstract());
        assertFalse(m.qn2objectProperty("test:OProp3").isAbstract());
        assertFalse(m.qn2objectProperty("test:OProp4").isAbstract());
        
        assertFalse(m.qn2objectProperty("test:OProp1").isRelationship());
        assertTrue (m.qn2objectProperty("test:OProp2").isRelationship());
        assertFalse(m.qn2objectProperty("test:OProp3").isRelationship());
        assertFalse(m.qn2objectProperty("test:OProp4").isRelationship());

//        assertEquals("ALL", m.qn2objectProperty("test:OProp1").getReferenceCode());
//        assertEquals("REF", m.qn2objectProperty("test:OProp2").getReferenceCode());
//        assertEquals("URI", m.qn2objectProperty("test:OProp3").getReferenceCode());
//        assertEquals("NONE", m.qn2objectProperty("test:OProp4").getReferenceCode());
    }

}

