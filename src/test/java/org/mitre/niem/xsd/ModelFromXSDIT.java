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

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
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

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSDIT {
    
    private static final String testDirPath = "src/test/resources";
    
    public ModelFromXSDIT() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    @Test
    @DisplayName("Augmentation")
    public void testAugmentation () {
        String[] args = { "src/test/resources/xsd/augment-0.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
    public void testCodelist () {
        String[] args = { "src/test/resources/xsd/codelist.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
    public void testCodelistClasstype () {
        String[] args = { "src/test/resources/xsd/codelistClassType.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
    public void testCodelistUnion () {
        String[] args = { "src/test/resources/xsd/codelistUnion.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
    public void testComplexContent () {
        String[] args = { "src/test/resources/xsd/complexContent.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
        assertTrue(me.isAttribute("nc:personNameCommentText"));
        assertNotNull(m.getClassType("nc:PersonNameTextType"));
        assertNotNull(m.getClassType("nc:ProperNameTextType"));
        assertNotNull(m.getClassType("nc:TextType"));        
    }
        
    @Test
    @DisplayName("appinfo:deprecated")
    public void testDeprecated () {
        String[] args = { "src/test/resources/xsd/deprecated.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
        assertTrue(me.isAttribute("nc:DepAtt"));
        assertFalse(me.isAttribute("nc:SecretPercent"));
    }

    @Test
    @DisplayName("External namespace")
    public void testExternals () {
        String[] args = { "src/test/resources/xsd/externals.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
    public void testHasValue () {
        String[] args = { "src/test/resources/xsd/hasValue.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
    public void testListType () {
        String[] args = { "src/test/resources/xsd/list.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
    public void testNamespace_1 () {
        String[] args = { "src/test/resources/xsd/namespace-1.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals("nc", m.getNamespaceByURI("http://example.com/Foo/1.0/").getNamespacePrefix());
        assertEquals("bar", m.getNamespaceByURI("http://release.niem.gov/niem/niem-core/5.0/").getNamespacePrefix());
    }

    @Test
    @DisplayName("Namespace prefix munging")
    public void testNamespace_2 () {
        String[] args = { "src/test/resources/xsd/namespace-2.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals("nc", m.getNamespaceByURI("http://example.com/Foo/1.0/").getNamespacePrefix());
        assertEquals("nc_5", m.getNamespaceByURI("http://release.niem.gov/niem/niem-core/5.0/").getNamespacePrefix());
    }

    @Test
    @DisplayName("Nillable")
    public void testNillable () {
        String[] args = { "src/test/resources/xsd/proxy.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);     
        assertTrue(me.isNillable("nc:ConfidencePercent"));
        assertFalse(me.isNillable("nc:SomeText"));
    }
    
    @Test
    @DisplayName("Proxy")
    public void testProxy () {
        String[] args = { "src/test/resources/xsd/proxy.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
    public void testRestriction () {
        String[] args = { "src/test/resources/xsd/restriction.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
    public void testTwoVersions () {
        String[] args = { "src/test/resources/xsd/twoversions-0.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals(16, m.getComponentList().size());
        assertEquals("http://release.niem.gov/niem/niem-core/4.0/", m.getNamespaceByPrefix("nc").getNamespaceURI());
        assertEquals("http://release.niem.gov/niem/niem-core/5.0/", m.getNamespaceByPrefix("nc_5").getNamespaceURI());
        assertEquals("http://www.w3.org/2001/XMLSchema", m.getNamespaceByPrefix("xs").getNamespaceURI());
        assertNotNull(m.getProperty("nc:ConfidencePercent"));
        assertNotNull(m.getProperty("nc_5:PersonGivenName"));
    }
  
    @Test
    @DisplayName("Union type")
    public void testUnionType () {
        String[] args = { "src/test/resources/xsd/union.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
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
    public void testXMLlang () {
        String[] args = { "src/test/resources/xsd/xml-lang.xsd" };
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsd = new NamespaceInfo(); 
        readModel(m, me, nsd, args);       
        assertEquals(3, m.getNamespaceList().size());
        assertEquals(6, m.getComponentList().size());
        Namespace xml = m.getNamespaceByPrefix("xml");
        assertNotNull(xml);
        Property p = m.getProperty("xml:lang");
        assertNotNull(p);
        ClassType ct = m.getClassType("nc:TextType");
        assertEquals(ct.hasPropertyList().get(2).getProperty(), p);
    }
  
    private void readModel(Model m, ModelExtension me, NamespaceInfo nsd, String[] args) {
        try {
            Schema s = Schema.genSchema(args);
            ModelFromXSD mfact = new ModelFromXSD(s);
            mfact.createModel(m, me, nsd);
        } catch (Exception ex) {
            fail("Can't create model");
        } 
    }
}
