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

import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.RestrictionOf;
import org.mitre.niem.cmf.UnionOf;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSDTest {
    
    public ModelFromXSDTest() {
    }

    @Test
    public void testCreateModel () throws Exception {
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel("src/test/resources/xsd/externals.xsd");
        
        List<Namespace> nslist = m.getNamespaceList();
        assertEquals(5, nslist.size());
        assertEquals("geo", nslist.get(0).getNamespacePrefix());
        assertEquals("gml", nslist.get(1).getNamespacePrefix());
        assertEquals("nc", nslist.get(2).getNamespacePrefix());
        assertEquals("ns", nslist.get(3).getNamespacePrefix());
        assertEquals("xs", nslist.get(4).getNamespacePrefix());
    }
    
    @Test
    @DisplayName("Augmentation")
    public void testAugmentation () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel("src/test/resources/xsd/augment-0.xsd");  
        
        assertEquals(4, m.getNamespaceList().size());
        assertEquals(11, m.getComponentList().size());
        assertNotNull(m.getNamespaceByPrefix("j"));
        assertNotNull(m.getNamespaceByPrefix("nc"));
        assertNotNull(m.getNamespaceByPrefix("test"));
        assertNotNull(m.getNamespaceByPrefix("xs"));             
        ClassType ct = m.getClassType("nc:AddressType");
        assertNotNull(ct);
        assertTrue(ct.isAugmentable());
        List<HasProperty> hpl = ct.hasPropertyList();
        assertNotNull(hpl);
        assertEquals(5, hpl.size());
        
        HasProperty hp = hpl.get(0);
        assertEquals(hp.getProperty(), m.getProperty("nc:AddressFullText"));
        assertEquals(1, hp.minOccurs());
        assertEquals(1, hp.maxOccurs());
        assertFalse(hp.maxUnbounded());
        assertNull(hp.augmentElementNS());
        assertEquals(0, hp.augmentTypeNS().size());
        
        hp = hpl.get(1);
        assertEquals(hp.getProperty(), m.getProperty("j:AddressCommentText"));
        assertEquals(0, hp.minOccurs());
        assertEquals(1, hp.maxOccurs());
        assertFalse(hp.maxUnbounded());
        assertNull(hp.augmentElementNS());
        assertEquals(2, hp.augmentTypeNS().size());
        assertTrue(hp.augmentTypeNS().contains(m.getNamespaceByPrefix("test")));
        assertTrue(hp.augmentTypeNS().contains(m.getNamespaceByPrefix("j")));
        
        hp = hpl.get(2);
        assertEquals(hp.getProperty(), m.getProperty("j:AddressVerifiedDate"));
        assertEquals(0, hp.minOccurs());
        assertTrue(hp.maxUnbounded());
        assertNull(hp.augmentElementNS());
        assertEquals(1, hp.augmentTypeNS().size());
        assertTrue(hp.augmentTypeNS().contains(m.getNamespaceByPrefix("j")));
        
        hp = hpl.get(3);
        assertEquals(hp.getProperty(), m.getProperty("j:AnotherAddress"));
        assertEquals(0, hp.minOccurs());
        assertTrue(hp.maxUnbounded());
        assertEquals(hp.augmentElementNS(), m.getNamespaceByPrefix("j"));
        assertEquals(0, hp.augmentTypeNS().size());
        
        hp = hpl.get(4);
        assertEquals(hp.getProperty(), m.getProperty("test:BoogalaText"));
        assertEquals(0, hp.minOccurs());
        assertEquals(1, hp.maxOccurs());
        assertFalse(hp.maxUnbounded());
        assertNull(hp.augmentElementNS());
        assertEquals(1, hp.augmentTypeNS().size());        
        assertTrue(hp.augmentTypeNS().contains(m.getNamespaceByPrefix("test")));
    }

    @Test
    @DisplayName("Code list")
    public void testCodelist () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/codelist.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(4, m.getComponentList().size());
        Datatype dt = m.getDatatype("nc:EmploymentPositionBasisCodeType");
        assertNotNull(dt);
        RestrictionOf r = dt.getRestrictionOf();
        assertNotNull(r);
        assertEquals(r.getDatatype(), m.getDatatype("xs:token"));
        List<Facet> fl = r.getFacetList();
        assertNotNull(fl);
        assertEquals(3, fl.size());
        assertEquals(fl.get(0).getFacetKind(), "Enumeration");
        assertEquals(fl.get(0).getStringVal(), "contractor");
        assertEquals(fl.get(1).getFacetKind(), "Enumeration");
        assertEquals(fl.get(1).getStringVal(), "non-permanent");
        assertEquals(fl.get(2).getFacetKind(), "Enumeration");
        assertEquals(fl.get(2).getStringVal(), "permanent");
        Property p1 = m.getProperty("nc:EmploymentPositionBasisAbstract");
        assertNotNull(p1);
        assertNull(p1.getClassType());
        assertNull(p1.getDatatype());
        assertTrue(p1.isAbstract());
        Property p2 = m.getProperty("nc:EmploymentPositionBasisCode");
        assertNotNull(p2);
        assertEquals(p2.getSubPropertyOf(), p1);
        assertEquals(p2.getDatatype(), dt);
        assertNull(p2.getClassType());
        assertNotNull(m.getDatatype("xs:token"));
        assertNull(m.getDatatype("nc:EmploymentPositionBasisCodeSimpleType"));
    }
    
    @Test
    @DisplayName("Code list ClassType")
    public void testCodelistClasstype () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/codelistClassType.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args); 
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(7, m.getComponentList().size());
        Datatype dt = m.getDatatype("nc:EmploymentPositionBasisCodeDatatype");
        assertNotNull(dt);
        RestrictionOf r = dt.getRestrictionOf();
        assertNotNull(r);
        assertEquals(r.getDatatype(), m.getDatatype("xs:token"));
        List<Facet> fl = r.getFacetList();
        assertNotNull(fl);
        assertEquals(3, fl.size());
        assertEquals(fl.get(0).getFacetKind(), "Enumeration");
        assertEquals(fl.get(0).getStringVal(), "contractor");
        assertEquals(fl.get(1).getFacetKind(), "Enumeration");
        assertEquals(fl.get(1).getStringVal(), "non-permanent");
        assertEquals(fl.get(2).getFacetKind(), "Enumeration");
        assertEquals(fl.get(2).getStringVal(), "permanent");
        
        ClassType ct = m.getClassType("nc:EmploymentPositionBasisCodeType");
        assertNotNull(ct);
        List<HasProperty> hpl = ct.hasPropertyList();
        assertNotNull(hpl);
        assertEquals(2, hpl.size());
        HasProperty hp = hpl.get(0);
        assertNotNull(hp);
        assertEquals(hp.getProperty(), m.getProperty("nc:EmploymentPositionBasisCodeLiteral"));
        assertEquals(1, hp.minOccurs());
        assertEquals(1, hp.maxOccurs());
        assertFalse(hp.maxUnbounded());        

        hp = hpl.get(1);
        assertNotNull(hp);
        assertEquals(hp.getProperty(), m.getProperty("nc:foo"));
        assertEquals(1, hp.minOccurs());
        assertEquals(1, hp.maxOccurs());
        assertFalse(hp.maxUnbounded());
        
        Property p1 = m.getProperty("nc:EmploymentPositionBasisAbstract");
        assertNotNull(p1);
        assertNull(p1.getClassType());
        assertNull(p1.getDatatype());
        assertTrue(p1.isAbstract());
        Property p2 = m.getProperty("nc:EmploymentPositionBasisCode");
        assertNotNull(p2);
        assertEquals(p2.getSubPropertyOf(), p1);
        assertEquals(p2.getClassType(), ct);
        assertNull(p2.getDatatype());
        assertNotNull(m.getDatatype("xs:token"));
        assertNotNull(m.getDatatype("nc:EmploymentPositionBasisCodeDatatype"));
    }
    
    @Test
    @DisplayName("Code list Union")
    public void testCodelistUnion () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/codelistUnion.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(4, m.getComponentList().size());
        Datatype u = m.getDatatype("test:ColorCodeType");
        Datatype c = m.getDatatype("test:CoolColorCodeType");
        Datatype w = m.getDatatype("test:WarmColorCodeType");
        UnionOf ur = u.getUnionOf();
        List<Datatype> dtl = ur.getDatatypeList();
        assertEquals(2, dtl.size());
        assertTrue(dtl.contains(c));
        assertTrue(dtl.contains(w));
        assertNotNull(m.getDatatype("xs:token"));
    }
    
    @Test
    @DisplayName("Complex content")
    public void testComplexContent () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/complexContent.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(13, m.getComponentList().size());
        ClassType ct = m.getClassType("nc:PersonNameType");
        assertNotNull(ct);
        List<HasProperty> hpl = ct.hasPropertyList();
        assertNotNull(hpl);
        assertEquals(4, hpl.size());
        HasProperty hp = hpl.get(0);
        assertEquals(hp.getProperty(), m.getProperty("nc:PersonGivenName"));
        assertEquals(0, hp.minOccurs());
        assertEquals(1, hp.maxOccurs());
        assertFalse(hp.maxUnbounded());
        hp = hpl.get(1);
        assertEquals(hp.getProperty(), m.getProperty("nc:PersonMiddleName"));
        assertEquals(0, hp.minOccurs());
        assertTrue(hp.maxUnbounded());                
        hp = hpl.get(2);
        assertEquals(hp.getProperty(), m.getProperty("nc:PersonSurName"));
        assertEquals(1, hp.minOccurs());
        assertEquals(1, hp.maxOccurs());
        assertFalse(hp.maxUnbounded()); 
        hp = hpl.get(3);
        assertEquals(hp.getProperty(), m.getProperty("nc:personNameCommentText"));
        assertEquals(0, hp.minOccurs());
        assertEquals(1, hp.maxOccurs());
        assertFalse(hp.maxUnbounded()); 
        assertTrue(m.getProperty("nc:personNameCommentText").isAttribute());
        assertNotNull(m.getClassType("nc:PersonNameTextType"));
        assertNotNull(m.getClassType("nc:ProperNameTextType"));
        assertNotNull(m.getClassType("nc:TextType"));        
    }

    @Test
    public void testCrossNSstype () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/crossNSstype.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        
        assertEquals(3, m.getNamespaceList().size());
        assertThat(m.getComponentList())
                .hasSize(9)
                .extracting(Component::getQName)
                .contains(
                        "nc:AngMin", "test:AngMinute", "test:AttributedAngularMinuteLiteral",
                        "test:SomeAtt", "test:AttributedAngularMinuteType", "nc:AngularMinuteType", 
                        "test:AngularMinuteType", "xs:decimal", "xs:token");
        assertEquals("nc:AngularMinuteType",   m.getProperty("nc:AngMin").getDatatype().getQName());
        assertEquals("test:AngularMinuteType", m.getProperty("test:AngMinute").getDatatype().getQName());
        assertEquals("nc:AngularMinuteType",   m.getProperty("test:AttributedAngularMinuteLiteral").getDatatype().getQName());        
        assertEquals("xs:token",               m.getProperty("test:SomeAtt").getDatatype().getQName());
    }
    
    @Test
    @DisplayName("appinfo:deprecated")
    public void testDeprecated () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/deprecated.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(10, m.getComponentList().size());
        Property p = m.getProperty("nc:ConfidencePercent");
        assertNotNull(p);
        assertFalse(p.isDeprecated()); 
        p = m.getProperty("nc:DepAtt");
        assertNotNull(p);
        assertTrue(p.isDeprecated());        
        p = m.getProperty("nc:SecretPercent");
        assertNotNull(p);
        assertTrue(p.isDeprecated()); 
        Datatype dt = m.getDatatype("nc:PercentType");
        assertNotNull(dt);
        assertFalse(dt.isDeprecated());
        dt = m.getDatatype("nc:SecretPercentType");
        assertNotNull(dt);
        assertTrue(dt.isDeprecated());
        ClassType ct = m.getClassType("nc:TextType");
        assertNotNull(ct);
        assertTrue(ct.isDeprecated());
        
        p = m.getProperty("nc:DepAtt");
        assertNotNull(p);
        assertTrue(p.isAttribute());
        
        p = m.getProperty("nc:SecretPercent");
        assertNotNull(p);
        assertFalse(p.isAttribute());
    }
    
    @Test
    @DisplayName("Complex content")
    public void testExtensionOf () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/complexContent.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(13, m.getComponentList().size());
        ClassType persNT = m.getClassType("nc:PersonNameTextType");
        ClassType propNT = m.getClassType("nc:ProperNameTextType");
        ClassType textT  = m.getClassType("nc:TextType");
        assertEquals(textT, propNT.getExtensionOfClass());
        assertEquals(propNT, persNT.getExtensionOfClass());
    }
    
    @Test
    @DisplayName("External namespace")
    public void testExternals () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/externals.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(5, m.getNamespaceList().size());
        assertEquals(5, m.getComponentList().size());
        Namespace n = m.getNamespaceByPrefix("ns");
        assertNotNull(n);
        assertFalse(n.isExternal());
        n = m.getNamespaceByPrefix("gml");
        assertNotNull(n);
        assertTrue(n.isExternal());
        ClassType ct = m.getClassType("geo:PointType");
        assertNotNull(ct);
        assertTrue(ct.isExternal());
        ct = m.getClassType("ns:TrackPointType");
        assertNotNull(ct);
        assertFalse(ct.isExternal());
    }

    @Test
    @DisplayName("xml:lang")
    public void testLanguage () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/augment-0.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        for (var sd : m.schemadoc().values()) {
            assertEquals("en-US", sd.language());
        }
    }
    
    @Test
    @DisplayName("List type")
    public void testListType () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/list.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(2, m.getComponentList().size());
        Datatype dt = m.getDatatype("nc:TokenListType");
        assertNotNull(dt);
        assertNull(dt.getRestrictionOf());
        assertNull(dt.getUnionOf());
        Datatype d2 = dt.getListOf();
        assertNotNull(d2);
        assertEquals(d2, m.getDatatype("xs:token"));
    }

    @Test
    public void testLiteral_0 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/literal-0.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(2, m.getComponentList().size());
        Datatype dt = m.getDatatype("nc:TextType");
        assertNotNull(dt);  
        assertNotNull(dt.getRestrictionOf());
    }
 
    @Test
    public void testLiteral_1 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/literal-1.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(5, m.getComponentList().size());
        ClassType ct = m.getClassType("nc:TextType");
        assertNotNull(ct);  
        assertThat(ct.hasPropertyList())
                .hasSize(2)
                .extracting(HasProperty::getProperty)
                .extracting(Property::getName)
                .contains("TextLiteral", "partialIndicator");
        Property p = m.getProperty("nc:TextLiteral");
        assertNotNull(p);
        assertEquals(p.getDatatype().getName(), "string");
        p = m.getProperty("nc:partialIndicator");
        assertNotNull(p);
        assertTrue(p.isAttribute());
        assertEquals(p.getDatatype().getName(), "boolean");
    }
 
    @Test
    public void testLiteral_2 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/literal-2.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(5, m.getComponentList().size());
        ClassType ct = m.getClassType("nc:TextType");
        assertNotNull(ct);  
        assertThat(ct.hasPropertyList())
                .hasSize(1)
                .extracting(HasProperty::getProperty)
                .extracting(Property::getName)
                .contains("TextLiteral");
        assertTrue(ct.canHaveMD());
        Property p = m.getProperty("nc:TextLiteral");
        assertNotNull(p);
    }
     
    @Test
    public void testLiteral_3 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/literal-3.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(8, m.getComponentList().size());
        ClassType ct1 = m.getClassType("nc:TextType");
        assertNotNull(ct1);  
        assertThat(ct1.hasPropertyList())
                .hasSize(2)
                .extracting(HasProperty::getProperty)
                .extracting(Property::getName)
                .contains("TextLiteral", "partialIndicator");
        
        ClassType ct2 = m.getClassType("nc:ProperNameTextType");
        assertNotNull(ct2);
        assertEquals(ct2.getExtensionOfClass(), ct1);
        assertThat(ct2.hasPropertyList())
                .hasSize(0);
        
        ClassType ct3 = m.getClassType("nc:PersonNameTextType");
        assertNotNull(ct3);
        assertEquals(ct3.getExtensionOfClass(), ct2);
        assertThat(ct3.hasPropertyList())
                .hasSize(1)
                .extracting(HasProperty::getProperty)
                .extracting(Property::getName)
                .contains("personNameInitialIndicator");
        
        Property p = m.getProperty("nc:TextLiteral");
        assertNotNull(p);
        assertEquals(p.getDatatype().getName(), "string");
        p = m.getProperty("nc:partialIndicator");
        assertNotNull(p);
        assertTrue(p.isAttribute());
        assertEquals(p.getDatatype().getName(), "boolean");
    }
 
    @Test
    public void testLiteral_4 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/literal-4.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(7, m.getComponentList().size());
        Datatype dt1 = m.getDatatype("nc:TextType");
        assertNotNull(dt1);
        
        Datatype dt2 = m.getDatatype("nc:ProperNameTextType");
        assertNotNull(dt2);
        assertEquals(dt2.getRestrictionOf().getDatatype(), dt1);
        
        ClassType ct = m.getClassType("nc:PersonNameTextType");
        assertNotNull(ct);
        assertThat(ct.hasPropertyList())
                .hasSize(2)
                .extracting(HasProperty::getProperty)
                .extracting(Property::getName)
                .contains("PersonNameTextLiteral", "personNameInitialIndicator");                
    }
    
    @Test
    public void testLiteral_5 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/literal-5.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(2, m.getComponentList().size());
        Datatype dt1 = m.getDatatype("nc:AngularMinuteType");
        assertNotNull(dt1);
        Datatype dt2 = m.getDatatype("xs:decimal");
        assertNotNull(dt2);
        RestrictionOf r = dt1.getRestrictionOf();
        assertEquals(r.getDatatype(), dt2);
        assertThat(r.getFacetList())
                .hasSize(2);
    }
    
    @Test
    public void testLiteral_6 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/literal-6.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(8, m.getComponentList().size());
        ClassType ct = m.getClassType("nc:AngularMinuteType");
        Datatype dt  = m.getDatatype("nc:AngularMinuteDatatype");
        Property p   = m.getProperty("nc:AngularMinuteLiteral");
        assertThat(ct.hasPropertyList())
                .hasSize(2)
                .extracting(HasProperty::getProperty)
                .extracting(Property::getName)
                .contains("AngularMinuteLiteral", "SomeAtt");
        assertNotNull(dt);
        assertNotNull(p);
        assertEquals(p.getDatatype(), dt);
    }
    
    @Test
    public void testLiteral_7 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/literal-7.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(8, m.getComponentList().size());
        ClassType ct = m.getClassType("nc:AttributedAngularMinuteType");
        Datatype dt  = m.getDatatype("nc:AngularMinuteType");
        Property p   = m.getProperty("nc:AttributedAngularMinuteLiteral");
        assertThat(ct.hasPropertyList())
                .hasSize(2)
                .extracting(HasProperty::getProperty)
                .extracting(Property::getName)
                .contains("AttributedAngularMinuteLiteral", "SomeAtt");
        assertNotNull(dt);
        assertNotNull(p);
        assertEquals(p.getDatatype(), dt);
    }

    @Test
    @DisplayName("Namespace prefix prioritization")
    public void testNamespace_1 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/namespace-1.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals("nc", m.getNamespaceByURI("http://example.com/Foo/1.0/").getNamespacePrefix());
        assertEquals("bar", m.getNamespaceByURI("http://release.niem.gov/niem/niem-core/5.0/").getNamespacePrefix());
    }

    @Test
    @DisplayName("Namespace prefix munging")
    public void testNamespace_2 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/namespace-2.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals("nc", m.getNamespaceByURI("http://example.com/Foo/1.0/").getNamespacePrefix());
        assertEquals("nc_5", m.getNamespaceByURI("http://release.niem.gov/niem/niem-core/5.0/").getNamespacePrefix());
    }

    @Test
    @DisplayName("Nillable")
    public void testNillable () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/proxy.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
        
        Property p = m.getProperty("nc:ConfidencePercent");
        assertNotNull(p);
        assertTrue(p.isReferenceable());
        
        p = m.getProperty("nc:SomeText");
        assertNotNull(p);
        assertFalse(p.isReferenceable());     
    }
    
    @Test
    @DisplayName("Proxy")
    public void testProxy () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/proxy.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(5, m.getComponentList().size());
        Datatype dt = m.getDatatype("nc:PercentType");
        assertNotNull(dt);
        RestrictionOf r = dt.getRestrictionOf();
        assertNotNull(r);
        assertEquals(r.getDatatype(), m.getDatatype("xs:decimal"));
        Property p = m.getProperty("nc:ConfidencePercent");
        assertNotNull(p);
        assertEquals(p.getDatatype(), dt);
        assertNull(p.getClassType());
        p = m.getProperty("nc:SomeText");
        assertNotNull(p);
        assertEquals(p.getDatatype(), m.getDatatype("xs:string"));
        assertNull(p.getClassType());
    }

    @Test
    @DisplayName("Restriction")
    public void testRestriction () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/restriction.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(2, m.getComponentList().size());
        assertNull(m.getDatatype("nc:AngularMinuteSimpleType"));
        Datatype dt = m.getDatatype("nc:AngularMinuteType");
        assertNotNull(dt);
        RestrictionOf r = dt.getRestrictionOf();
        assertNotNull(r);
        assertEquals(r.getDatatype(), m.getDatatype("xs:decimal"));
        List<Facet> fl = r.getFacetList();
        Facet f = fl.get(0);
        assertEquals("MaxExclusive", f.getFacetKind());
        assertEquals("60.0", f.getStringVal());
        f = fl.get(1);
        assertEquals("MinInclusive", f.getFacetKind());
        assertEquals("0.0", f.getStringVal());        
    }

    @Test
    @DisplayName("TwoVersions")
    public void testTwoVersions () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/twoversions-0.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals(17, m.getComponentList().size());
        assertEquals("http://release.niem.gov/niem/niem-core/4.0/", m.getNamespaceByPrefix("nc_4").getNamespaceURI());
        assertEquals("http://release.niem.gov/niem/niem-core/5.0/", m.getNamespaceByPrefix("nc").getNamespaceURI());
        assertEquals("http://www.w3.org/2001/XMLSchema", m.getNamespaceByPrefix("xs").getNamespaceURI());
        assertNotNull(m.getProperty("nc_4:ConfidencePercent"));
        assertNotNull(m.getProperty("nc:PersonGivenName"));
    }
  
    @Test
    @DisplayName("Union type")
    public void testUnionType () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/union.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(4, m.getComponentList().size());
        Datatype dt = m.getDatatype("ns:UnionType");
        assertNotNull(dt);
        assertNull(dt.getRestrictionOf());
        assertNull(dt.getListOf());
        UnionOf u = dt.getUnionOf();
        assertNotNull(u);
        List<Datatype> ul = u.getDatatypeList();
        assertNotNull(ul);
        assertEquals(2, ul.size());
        assertEquals(ul.get(0), m.getDatatype("xs:decimal"));
        assertEquals(ul.get(1), m.getDatatype("xs:float"));        
    }
    
    @Test
    public void testWhitespace () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/whitespace.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);   
        
        Datatype dt = m.getDatatype("nc:CStringType");
        RestrictionOf r = dt.getRestrictionOf();
        Facet f = r.getFacetList().get(0);
        assertEquals(2, r.getFacetList().size());
        assertEquals("WhiteSpace", f.getFacetKind());
        assertEquals("collapse", f.getStringVal());
        f = r.getFacetList().get(1);
        assertEquals("MaxLength", f.getFacetKind());
        assertEquals("20", f.getStringVal());

        dt = m.getDatatype("nc:LStringType");
        r = dt.getRestrictionOf();
        f = r.getFacetList().get(0);
        assertEquals(1, r.getFacetList().size());
        assertEquals("MaxLength", f.getFacetKind());
        assertEquals("20", f.getStringVal());        
    
        dt = m.getDatatype("nc:AngularMinuteType");
        r = dt.getRestrictionOf();
        f = r.getFacetList().get(0);
        assertEquals(2, r.getFacetList().size());
        assertEquals("MaxExclusive", f.getFacetKind());
        assertEquals("60.0", f.getStringVal());   
        f = r.getFacetList().get(1);
        assertEquals("MinInclusive", f.getFacetKind());
        assertEquals("0.0", f.getStringVal()); 
    }

    @Test
    @DisplayName("xml:lang")
    public void testXMLlang () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        String[] args = { "src/test/resources/xsd/xml-lang.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals(7, m.getComponentList().size());
        Namespace xml = m.getNamespaceByPrefix("xml");
        assertNotNull(xml);
        Property p = m.getProperty("xml:lang");
        assertNotNull(p);
        ClassType ct = m.getClassType("nc:TextType");
        assertEquals(ct.hasPropertyList().get(3).getProperty(), p);
    }
}
