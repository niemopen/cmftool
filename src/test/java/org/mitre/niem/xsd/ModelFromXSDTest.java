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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.mitre.niem.cmf.ClassType;
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
    public void testAugmentation () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
        ModelFromXSD mfact = new ModelFromXSD();        
        Model m = mfact.createModel("src/test/resources/xsd/augment-0.xsd");  
        
        assertEquals(4, m.getNamespaceList().size());
        assertEquals(10, m.getComponentList().size());
        assertNotNull(m.getNamespaceByPrefix("j"));
        assertNotNull(m.getNamespaceByPrefix("nc"));
        assertNotNull(m.getNamespaceByPrefix("test"));
        assertNotNull(m.getNamespaceByPrefix("xs"));             
        ClassType ct = m.getClassType("nc:AddressType");
        assertNotNull(ct);
        assertNull(ct.getHasValue());
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
    public void testCodelist () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
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
    public void testCodelistClasstype () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
        String[] args = { "src/test/resources/xsd/codelistClassType.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args); 
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(6, m.getComponentList().size());
        Datatype dt = m.getDatatype("nc:EmploymentPositionBasisCodeSimpleType");
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
        assertEquals(ct.getHasValue(), dt);
        List<HasProperty> hpl = ct.hasPropertyList();
        assertNotNull(hpl);
        assertEquals(1, hpl.size());
        HasProperty hp = hpl.get(0);
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
        assertNotNull(m.getDatatype("nc:EmploymentPositionBasisCodeSimpleType"));
    }
    
    @Test
    @DisplayName("Code list Union")
    public void testCodelistUnion () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
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
    @DisplayName("External namespace")
    public void testComplexContent () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
        String[] args = { "src/test/resources/xsd/complexContent.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(12, m.getComponentList().size());
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
    @DisplayName("appinfo:deprecated")
    public void testDeprecated () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
        String[] args = { "src/test/resources/xsd/deprecated.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(9, m.getComponentList().size());
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
    @DisplayName("External namespace")
    public void testExternals () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
        String[] args = { "src/test/resources/xsd/externals.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(5, m.getNamespaceList().size());
        assertEquals(6, m.getComponentList().size());
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
    @DisplayName("HasValue")
    public void testHasValue () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
        String[] args = { "src/test/resources/xsd/hasValue.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(2, m.getNamespaceList().size());
        assertEquals(10, m.getComponentList().size());
        ClassType ct = m.getClassType("nc:Degree90Type");
        assertEquals(ct.getHasValue(), m.getDatatype("nc:Degree90SimpleType"));
        assertEquals(1, ct.hasPropertyList().size());
        HasProperty hp = ct.hasPropertyList().get(0);
        assertNotNull(hp);
        assertEquals(hp.getProperty(), m.getProperty("nc:errorValue"));
        assertEquals(0, hp.minOccurs());
        assertEquals(1, hp.maxOccurs());
        assertFalse(hp.maxUnbounded());
        
        Datatype dt = m.getDatatype("nc:Degree90SimpleType");
        assertNull(dt.getListOf());
        assertNull(dt.getUnionOf());
        RestrictionOf r = dt.getRestrictionOf();
        assertEquals(r.getDatatype(), m.getDatatype("xs:decimal"));
        
        ct = m.getClassType("nc:UnionTestType");
        assertEquals(ct.getHasValue(), m.getDatatype("nc:UnionSimpleType"));
        
        ct = m.getClassType("nc:ListTestType");
        assertEquals(ct.getHasValue(), m.getDatatype("nc:ListSimpleType"));
        
        dt = m.getDatatype("nc:NoAttributeTestType");
        r = dt.getRestrictionOf();
        assertEquals(r.getDatatype(), m.getDatatype("nc:Degree90SimpleType"));
    }

    @Test
    @DisplayName("List type")
    public void testListType () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
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
    @DisplayName("Namespace prefix prioritization")
    public void testNamespace_1 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
        String[] args = { "src/test/resources/xsd/namespace-1.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals("nc", m.getNamespaceByURI("http://example.com/Foo/1.0/").getNamespacePrefix());
        assertEquals("bar", m.getNamespaceByURI("http://release.niem.gov/niem/niem-core/5.0/").getNamespacePrefix());
    }

    @Test
    @DisplayName("Namespace prefix munging")
    public void testNamespace_2 () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
        String[] args = { "src/test/resources/xsd/namespace-2.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals("nc", m.getNamespaceByURI("http://example.com/Foo/1.0/").getNamespacePrefix());
        assertEquals("nc_5", m.getNamespaceByURI("http://release.niem.gov/niem/niem-core/5.0/").getNamespacePrefix());
    }

    @Test
    @DisplayName("Nillable")
    public void testNillable () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
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
    public void testProxy () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
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
    public void testRestriction () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
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
        assertEquals("WhiteSpace", f.getFacetKind());
        f = fl.get(1);
        assertEquals("MaxExclusive", f.getFacetKind());
        assertEquals("60.0", f.getStringVal());
        f = fl.get(2);
        assertEquals("MinInclusive", f.getFacetKind());
        assertEquals("0.0", f.getStringVal());        
    }

    @Test
    @DisplayName("TwoVersions")
    public void testTwoVersions () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
        String[] args = { "src/test/resources/xsd/twoversions-0.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals(16, m.getComponentList().size());
        assertEquals("http://release.niem.gov/niem/niem-core/4.0/", m.getNamespaceByPrefix("nc_4").getNamespaceURI());
        assertEquals("http://release.niem.gov/niem/niem-core/5.0/", m.getNamespaceByPrefix("nc").getNamespaceURI());
        assertEquals("http://www.w3.org/2001/XMLSchema", m.getNamespaceByPrefix("xs").getNamespaceURI());
        assertNotNull(m.getProperty("nc_4:ConfidencePercent"));
        assertNotNull(m.getProperty("nc:PersonGivenName"));
    }
  
    @Test
    @DisplayName("Union type")
    public void testUnionType () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
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
    @DisplayName("xml:lang")
    public void testXMLlang () throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException {
        String[] args = { "src/test/resources/xsd/xml-lang.xsd" };
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(args);
       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals(6, m.getComponentList().size());
        Namespace xml = m.getNamespaceByPrefix("xml");
        assertNotNull(xml);
        Property p = m.getProperty("xml:lang");
        assertNotNull(p);
        ClassType ct = m.getClassType("nc:TextType");
        assertEquals(ct.hasPropertyList().get(2).getProperty(), p);
    }
}
