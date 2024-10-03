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

import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.mitre.niem.cmf.AugmentRecord;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Property;

/**
 * Check assertions against a Model object created by ModelXMLReaderTest
 * or ModelFromXSDTest.  The test assertions are the same and we don't want
 * to write them twice.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class CheckModel {

    // Test the model built from xsd?/augCCwA.xsd or read from cmf/augCCwA.cmf
    // Augmenting complex content with attributes
    public static void checkAugCCwA (Model m) {
        var tns  = m.getNamespaceByPrefix("test");
        var adrt = m.getClassType("nc:AddressType");
        for (var hp : adrt.hasPropertyList()) {
            switch(hp.getProperty().getQName()) {
                case "nc:AddressFullText" -> {
                    assertEquals(1, hp.minOccurs());
                    assertEquals(1, hp.maxOccurs());
                    assertNull(hp.getDefinition());
                    assertTrue(hp.augmentingNS().isEmpty());
                }
                case "test:privacyText" -> {
                    assertEquals(0, hp.minOccurs());
                    assertEquals(1, hp.maxOccurs());
                    assertEquals("A privacy restriction.", hp.getDefinition());
                    assertThat(hp.augmentingNS()).containsExactly(tns);                            
                }
                default -> fail("bogus hasProperty");
            }
        }
        for (var ns : m.getNamespaceList()) assertThat(ns == tns || ns.augmentList().isEmpty());
        assertThat(tns.augmentList()).hasSize(1);
        var ar = tns.augmentList().get(0);
        assertEquals("nc:AddressType", ar.getClassType().getQName());
        assertEquals("test:privacyText", ar.getProperty().getQName());
        assertEquals(0, ar.minOccurs());
        assertEquals(1, ar.maxOccurs()); 
        assertNull(ar.getAugmentedGlobal());
    }
    
    // Test the model built from xsd?/augCCwE.xsd or read from cmf/augCCwE.cmf
    // Augmenting complex content with elements. Includes an attribute in 
    // an augmentation type (blech).
    public static void checkAugCCwE (Model m) {
        var tns  = m.getNamespaceByPrefix("test");
        var jns  = m.getNamespaceByPrefix("j");
        var adrt = m.getClassType("nc:AddressType");
        assertThat(adrt.hasPropertyList())
                .extracting(HasProperty::getProperty)
                .extracting(Property::getQName)
                .containsExactly("nc:AddressFullText",
                        "j:AddressCommentText",
                        "j:AddressVerifiedDate",
                        "test:BoogalaText",
                        "test:boogalaProp",
                        "test:SecretAddress");
        assertThat(adrt.getHasProperty("j:AddressCommentText").augmentingNS())
                .containsExactlyInAnyOrder(tns, jns);
        assertThat(adrt.getHasProperty("j:AddressVerifiedDate").augmentingNS())
                .containsExactly(jns);
        assertThat(adrt.getHasProperty("test:BoogalaText").augmentingNS())
                .containsExactly(tns);
        
        assertThat(tns.augmentList())
                .hasSize(4)
                .extracting(AugmentRecord::getClassType)
                .extracting(ClassType::getQName)
                .containsOnly("nc:AddressType");
        assertThat(tns.augmentList())
                .extracting(AugmentRecord::getProperty)
                .extracting(Property::getQName)
                .containsExactlyInAnyOrder("j:AddressCommentText",
                        "test:BoogalaText",
                        "test:SecretAddress",
                        "test:boogalaProp");
        for (var ar : tns.augmentList()) {
            var pqn = ar.getProperty().getQName();
            switch (pqn) {
                case "j:AddressCommentText" -> {
                    assertEquals(1, ar.minOccurs());
                    assertEquals(1, ar.maxOccurs());
                    assertEquals(1, ar.indexInType());
                    assertNull(ar.getAugmentedGlobal());
                }
                case "test:BoogalaText" -> {
                    assertEquals(1, ar.minOccurs());
                    assertEquals(1, ar.maxOccurs());
                    assertEquals(0, ar.indexInType());
                    assertNull(ar.getAugmentedGlobal());
                }
                case "test:SecretAddress" -> {
                    assertEquals(0, ar.minOccurs());
                    assertEquals(true, ar.maxUnbounded());
                    assertEquals(-1, ar.indexInType());
                    assertNull(ar.getAugmentedGlobal());
                }
                case "test:boogalaProp" -> {
                    assertEquals(0, ar.minOccurs());
                    assertEquals(1, ar.maxOccurs());
                    assertEquals(-1, ar.indexInType());
                    assertNull(ar.getAugmentedGlobal());
                }  
            }
        }
        assertThat(jns.augmentList())
                .hasSize(2)
                .extracting(AugmentRecord::getClassType)
                .extracting(ClassType::getQName)
                .containsOnly("nc:AddressType");
        assertThat(jns.augmentList())
                .extracting(AugmentRecord::getProperty)
                .extracting(Property::getQName)
                .containsExactlyInAnyOrder("j:AddressCommentText",
                        "j:AddressVerifiedDate");  
        for (var ar : jns.augmentList()) {
            var pqn = ar.getProperty().getQName();
            switch (pqn) {
                case "j:AddressCommentText" -> {
                    assertEquals(1, ar.minOccurs());
                    assertEquals(1, ar.maxOccurs());
                    assertEquals(0, ar.indexInType());
                    assertNull(ar.getAugmentedGlobal());
                }
                case "j:AddressVerifiedDate" -> {
                    assertEquals(0, ar.minOccurs());
                    assertEquals(true, ar.maxUnbounded());
                    assertEquals(1, ar.indexInType());
                    assertNull(ar.getAugmentedGlobal());
                }             
            }
        }                
        for (var ns : m.getNamespaceList()) assertThat(ns == tns || ns == jns || ns.augmentList().isEmpty());
    }
    
    // Test the model built from xsd?/augSCwA.xsd or read from cmf/augSCwA.cmf
    // Augmenting simple content with attributes
    public static void checkAugSCwA (Model m) {
        var tns  = m.getNamespaceByPrefix("test");
        var txc = m.getClassType("nc:TextType");
        for (var hp : txc.hasPropertyList()) {
            switch(hp.getProperty().getQName()) {
                case "nc:TextLiteral" -> {
                    assertEquals(1, hp.minOccurs());
                    assertEquals(1, hp.maxOccurs());
                    assertNull(hp.getDefinition());
                    assertTrue(hp.augmentingNS().isEmpty());
                }
                case "test:privacyText" -> {
                    assertEquals(0, hp.minOccurs());
                    assertEquals(1, hp.maxOccurs());
                    assertEquals("A privacy restriction.", hp.getDefinition());
                    assertThat(hp.augmentingNS()).containsExactly(tns);                            
                }
                default -> fail("bogus hasProperty");
            }
        }
        for (var ns : m.getNamespaceList()) assertThat(ns == tns || ns.augmentList().isEmpty());
        assertThat(tns.augmentList()).hasSize(1);
        var ar = tns.augmentList().get(0);
        assertEquals("nc:TextType", ar.getClassType().getQName());
        assertEquals("test:privacyText", ar.getProperty().getQName());
        assertEquals(0, ar.minOccurs());
        assertEquals(1, ar.maxOccurs()); 
        assertNull(ar.getAugmentedGlobal());
    }
    
    // Test the model built from xsd?/augSCwA.xsd or read from cmf/augSCwA.cmf
    // Augmenting simple content with elements
    // Should warn for non-existing augmenting namespace
    public static void checkAugSCwE (Model m) {
        var tns  = m.getNamespaceByPrefix("test");
        var txc = m.getClassType("nc:TextType");
        for (var hp : txc.hasPropertyList()) {
            switch(hp.getProperty().getQName()) {
                case "nc:TextLiteral" -> {
                    assertEquals(1, hp.minOccurs());
                    assertEquals(1, hp.maxOccurs());
                    assertNull(hp.getDefinition());
                    assertTrue(hp.augmentingNS().isEmpty());
                }
                case "test:privacyAssertionRef" -> {
                    assertEquals(0, hp.minOccurs());
                    assertEquals(1, hp.maxOccurs());
                    assertNull(hp.getDefinition());
                    assertThat(hp.augmentingNS()).containsExactly(tns);                            
                }
                default -> fail("bogus hasProperty");
            }
        }
        for (var ns : m.getNamespaceList()) assertThat(ns == tns || ns.augmentList().isEmpty());
        assertThat(tns.augmentList()).hasSize(1);
        var ar = tns.augmentList().get(0);
        assertEquals("nc:TextType", ar.getClassType().getQName());
        assertEquals("test:privacyAssertionRef", ar.getProperty().getQName());
        assertEquals(0, ar.minOccurs());
        assertEquals(1, ar.maxOccurs()); 
        assertNull(ar.getAugmentedGlobal());
    }
    
    // Test the model built from xsd?/augGEOonly.xsd or read from cmf/augGEOonly.cmf
    // Global augmenting all associations (but not objects) with elements
    public static void checkAugGEAonly (Model m) {
        var tns   = m.getNamespaceByPrefix("test");
        for (var ct : m.getClassTypeList()) {
            var hp = ct.getHasProperty("test:PrivacyAssertion");
            var ctn = ct.getQName();
            switch (ct.getQName()) {
                case "nc:AssociationType":
                    assertThat(hp.augmentingNS()).containsExactly(tns);
                    break;
                case "nc:AddressType":
                case "test:PrivacyAssertionType":
                    assertNull(hp);
                    break;
                default: fail("unexpected class "+ct.getQName());
            }
        }
        for (var ns : m.getNamespaceList()) assertThat(ns == tns || ns.augmentList().isEmpty());
        assertThat(tns.augmentList()).hasSize(1);
        var ar = tns.augmentList().get(0);
        assertNull(ar.getClassType());
        assertEquals("test:PrivacyAssertion", ar.getProperty().getQName());
        assertEquals(1, ar.minOccurs());
        assertEquals(1, ar.maxOccurs()); 
        assertEquals("structures:AssociationAugmentationPoint", ar.getAugmentedGlobal());      
    }
    
    // Test the model built from xsd?/augGEOonly.xsd or read from cmf/augGEOonly.cmf
    // Global augmenting all objects and associations with elements
    public static void checkAugGEBoth (Model m) {
        var tns   = m.getNamespaceByPrefix("test");
        for (var ct : m.getClassTypeList()) {
            var hp = ct.getHasProperty("test:PrivacyAssertion");
            var ctn = ct.getQName();
            switch (ct.getQName()) {
                case "nc:AddressType":
                case "nc:AssociationType":
                    assertThat(hp.augmentingNS()).containsExactly(tns);
                    break;
                case "nc:TextType":
                case "test:PrivacyAssertionType":
                    assertNull(hp);
            }
        }
        for (var ns : m.getNamespaceList()) assertThat(ns == tns || ns.augmentList().isEmpty());
        assertThat(tns.augmentList()).hasSize(2);
        assertThat(tns.augmentList())
                .hasSize(2)
                .extracting(AugmentRecord::getAugmentedGlobal)
                .containsExactlyInAnyOrder("structures:AssociationAugmentationPoint",
                        "structures:ObjectAugmentationPoint");
        for (var ar : tns.augmentList()) {
            assertNull(ar.getClassType());
            assertEquals("test:PrivacyAssertion", ar.getProperty().getQName());
            assertEquals(1, ar.minOccurs());
            assertEquals(1, ar.maxOccurs()); 
        }
    }
    
    // Test the model built from xsd?/augGEOonly.xsd or read from cmf/augGEOonly.cmf
    // Global augmenting all objects (but not associations) with elements
    public static void checkAugGEOonly (Model m) {
        var tns   = m.getNamespaceByPrefix("test");
        for (var ct : m.getClassTypeList()) {
            var hp = ct.getHasProperty("test:PrivacyAssertion");
            switch (ct.getQName()) {
                case "nc:AddressType":
                    assertThat(hp.augmentingNS()).containsExactly(tns);
                    break;
                case "nc:AssociationType":
                case "test:PrivacyAssertionType":
                    assertNull(hp);
                    break;
                default: fail("unexpected class "+ct.getQName());
            }
        }
        for (var ns : m.getNamespaceList()) assertThat(ns == tns || ns.augmentList().isEmpty());
        assertThat(tns.augmentList()).hasSize(1);
        var ar = tns.augmentList().get(0);
        assertNull(ar.getClassType());
        assertEquals("test:PrivacyAssertion", ar.getProperty().getQName());
        assertEquals(1, ar.minOccurs());
        assertEquals(1, ar.maxOccurs()); 
        assertEquals("structures:ObjectAugmentationPoint", ar.getAugmentedGlobal());      
    }
    
    // Test the model built from xsd?/augGEOonly.xsd or read from cmf/augGEOonly.cmf
    // Global augmenting all simple content (but not objects or associations)
    public static void checkAugGESonly (Model m) {
        var tns   = m.getNamespaceByPrefix("test");
        for (var ct : m.getClassTypeList()) {
            var hp = ct.getHasProperty("test:PrivacyAssertion");
            switch (ct.getQName()) {
                case "nc:TextType":
                    assertThat(hp.augmentingNS()).containsExactly(tns);
                    break;
                case "nc:AddressType":
                case "nc:AssociationType":
                case "test:PrivacyAssertionType":
                    assertNull(hp);
                    break;
                default: fail("unexpected class "+ct.getQName());
            }
        }        
    }   
    
    // Test the model built from xsd?/components.xsd or read from cmf/compnents.cmf
    // Test name, namespace, documentation, and deprecated properties of each component
    // Test class, property, datatype components
    public static void checkComponents (Model m) {
        var ct = m.getClassType("test:Test1Type");
        var dt = m.getDatatype("test:Test2DataType");
        var p1 = m.getProperty("test:AnElement");
        var p2 = m.getProperty("test:AnotherElement");
        
        assertEquals("Test1Type", ct.getName());
        assertEquals("http://example.com/components/", ct.getNamespaceURI());
        assertEquals("Test1Type doc string #1", ct.getDocumentation());
        assertTrue(ct.isDeprecated());
        
        assertEquals("Test2DataType", dt.getName());
        assertEquals("http://example.com/components/", dt.getNamespaceURI());
        assertEquals("Test2DataType doc string", dt.getDocumentation());
        assertTrue(dt.isDeprecated());

        assertEquals("AnElement", p1.getName());
        assertEquals("http://example.com/components/", p1.getNamespaceURI());
        assertEquals("AnElement doc string #1", p1.getDocumentation());
        assertTrue(p1.isDeprecated());
        
        assertEquals("AnotherElement", p2.getName());
        assertEquals("http://example.com/components/", p2.getNamespaceURI());
        assertEquals("AnotherElement doc string #1", p2.getDocumentation());
        assertFalse(p2.isDeprecated());        
    }
    
    // Test the model build from xsd?/datatypes.xsd or read from cmf/datatypes.cmf
    // Test ListDatatype and orderedProperty
    // Test UnionDatatype
    public static void checkDatatypes (Model m) {
       var dtFlt = m.getDatatype("xs:float");
       var dtInt = m.getDatatype("xs:integer");
       var dtLs1 = m.getDatatype("test:List1Type"); 
       var dtLs2 = m.getDatatype("test:List2Type");
       var dtUn  = m.getDatatype("test:UnionType");
       
       assertEquals(dtInt, dtLs1.getListOf()); 
       assertTrue(dtLs1.getOrderedItems());
       assertEquals(dtUn, dtLs2.getListOf());
       assertFalse(dtLs2.getOrderedItems());
       
       assertThat(dtUn.unionOf())
               .extracting(Datatype::getQName)
               .containsExactlyInAnyOrder("xs:integer",
                       "xs:float",
                       "test:Restrict8Type");
       
       var rdt = m.getDatatype("test:Restrict1Type");
       assertEquals("xs:token", rdt.getRestrictionBase().getQName());
       for (var f : rdt.facetList()) {
           switch (f.getFacetKind()) {
               case "MinLength" -> assertEquals("3", f.getStringVal());
               case "MaxLength" -> assertEquals("4", f.getStringVal());
               case "Pattern"   -> assertEquals("[A-Z]{3}", f.getStringVal());
               case "Enumeration" -> {
                   switch (f.getStringVal()) {
                       case "FOO" -> assertEquals("The FOO token", f.getDefinition());
                       case "BAR" -> assertNull(f.getDefinition());
                       default -> fail("bogus enumeration");
                   }
               }
               default -> fail("bogus facet");
           }
       }
       rdt = m.getDatatype("test:Restrict2Type");
       assertEquals("xs:integer", rdt.getRestrictionBase().getQName());
       for (var f : rdt.facetList()) {
           switch (f.getFacetKind()) {
               case "MinInclusive" -> assertEquals("10", f.getStringVal());
               case "MaxInclusive" -> assertEquals("20", f.getStringVal());
               default -> fail("bogus facet");
           }
       }
       rdt = m.getDatatype("test:Restrict3Type");
       assertEquals("xs:integer", rdt.getRestrictionBase().getQName());
       for (var f : rdt.facetList()) {
           switch (f.getFacetKind()) {
               case "MinExclusive" -> assertEquals("10", f.getStringVal());
               case "MaxExclusive" -> assertEquals("20", f.getStringVal());
               default -> fail("bogus facet");
           }
       }
       rdt = m.getDatatype("test:Restrict4Type");
       assertEquals("xs:string", rdt.getRestrictionBase().getQName());
       for (var f : rdt.facetList()) {
           switch (f.getFacetKind()) {
               case "MinLength" -> assertEquals("3", f.getStringVal());
               case "MaxLength" -> assertEquals("4", f.getStringVal());
               default -> fail("bogus facet");
           }
       }       
       rdt = m.getDatatype("test:Restrict5Type");
       assertEquals("xs:string", rdt.getRestrictionBase().getQName());
       for (var f : rdt.facetList()) {
           switch (f.getFacetKind()) {
               case "Length"     -> assertEquals("3", f.getStringVal());
               case "WhiteSpace" -> assertEquals("collapse", f.getStringVal());
               default -> fail("bogus facet");
           }
       }       
       rdt = m.getDatatype("test:Restrict6Type");
       assertEquals("xs:decimal", rdt.getRestrictionBase().getQName());
       for (var f : rdt.facetList()) {
           switch (f.getFacetKind()) {
               case "FractionDigits" -> assertEquals("2", f.getStringVal());
               case "TotalDigits"    -> assertEquals("5", f.getStringVal());
               default -> fail("bogus facet");
           }
       }  
       rdt = m.getDatatype("test:Restrict7Type");
       assertEquals("xs:token", rdt.getRestrictionBase().getQName());
       for (var f : rdt.facetList()) {
           switch (f.getFacetKind()) {
               case "Enumeration" -> {
                   switch (f.getStringVal()) {
                       case "GB" -> assertEquals("UNITED KINGDOM", f.getDefinition());
                       case "US" -> assertEquals("UNITED STATES", f.getDefinition());
                       default -> fail("bogus enumeration");
                   }
               }
               default -> fail("bogus facet");
           }
       }  
       rdt = m.getDatatype("test:Restrict8Type");
       assertEquals("xs:token", rdt.getRestrictionBase().getQName());
       for (var f : rdt.facetList()) {
           switch (f.getFacetKind()) {
               case "Enumeration" -> {
                   switch (f.getStringVal()) {
                       case "GB" -> assertNull(f.getDefinition());
                       case "US" -> assertNull(f.getDefinition());
                       default -> fail("bogus enumeration");
                   }
               }
               default -> fail("bogus facet");
           }
       }  
    }
    
    
}
