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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.RestrictionOf;
import org.mitre.niem.cmf.SchemaDocument;
import org.mitre.niem.cmf.SchemaPile;
import org.mitre.niem.cmf.UnionOf;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLReaderIT {
    
    private static final String testDirPath = "src/test/resources";
    
    public ModelXMLReaderIT () { 
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
    public void testAttributeIndicator () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/extension.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        
        Property p = m.getProperty("nc:partialIndicator");
        assertNotNull(p);
        assertTrue(p.isAttribute());
        
        p = m.getProperty("nc:PersonGivenName");
        assertNotNull(p);
        assertFalse(p.isAttribute());
    }
    
    @Test
    public void testSchemaDocument () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/extension.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        
        SchemaPile sp = m.getSchemaPile();
        assertNotNull(sp);
        Collection<SchemaDocument> sdc = sp.getAllSchemaDocuments();
        assertNotNull(sdc);
        assertEquals(5, sdc.size());
        
        SchemaDocument sd = sp.getSchemaDocument("http://release.niem.gov/niem/appinfo/5.0/");
        assertNotNull(sd);
        assertEquals("appinfo", sd.prefix());
        assertEquals("5.0", sd.niemVersion());
        assertNull(sd.confTargets());
        assertNull(sd.filePath());
        assertNull(sd.schemaVersion());
        
        sd = sp.getSchemaDocument("http://release.niem.gov/niem/conformanceTargets/3.0/");
        assertNotNull(sd);
        assertEquals("ct", sd.prefix());
        assertEquals("3.0", sd.niemVersion());
        assertNull(sd.confTargets());
        assertNull(sd.filePath());
        assertNull(sd.schemaVersion());    
    
        sd = sp.getSchemaDocument("http://release.niem.gov/niem/niem-core/5.0/");
        assertNotNull(sd);
        assertEquals("nc", sd.prefix());
        assertEquals("5.0", sd.niemVersion());
        assertEquals("http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument", sd.confTargets());
        assertEquals("niem/niem-core.xsd", sd.filePath());
        assertEquals("1", sd.schemaVersion());  
    }
    
    @Test
    public void testExtensionOf () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/extension.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        
        assertEquals(2, m.getNamespaceList().size());
        assertNotNull(m.getNamespaceByPrefix("nc"));
        assertNotNull(m.getNamespaceByPrefix("xs"));
        
        assertEquals(8, m.getComponentList().size());
        ClassType c1 = m.getClassType("nc:PersonNameTextType");
        ClassType c2 = m.getClassType("nc:ProperNameTextType");
        ClassType c3 = m.getClassType("nc:TextType");
        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(c3);
        
        assertEquals(1, c1.hasPropertyList().size());
        assertEquals(0, c2.hasPropertyList().size());
        assertEquals(1, c3.hasPropertyList().size());
        
        assertEquals(c1.getExtensionOfClass(), c2);
        assertEquals(c2.getExtensionOfClass(), c3);
        assertNull(c3.getExtensionOfClass());
    }

    @Test
    public void testCodetype () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/codeType.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        
        assertEquals(2, m.getNamespaceList().size());
        assertNotNull(m.getNamespaceByPrefix("nc"));
        assertNotNull(m.getNamespaceByPrefix("xs"));
        
        Datatype d1 = m.getDatatype("nc:EmploymentPositionBasisCodeType");
        assertNotNull(d1);
        assertFalse(d1.isAbstract());
        assertFalse(d1.isDeprecated());
        RestrictionOf r = d1.getRestrictionOf();
        assertNotNull(r);
        assertEquals(r.getDatatype(), m.getDatatype("xs:token"));
        assertNotNull(r.getFacetList());
        assertEquals(3, r.getFacetList().size());
        assertEquals("Enumeration", r.getFacetList().get(0).getFacetKind());
        assertEquals("contractor",  r.getFacetList().get(0).getStringVal());
        assertNull(r.getFacetList().get(0).getDefinition());
        assertNull(d1.getListOf());
        assertNull(d1.getUnionOf());
        assertEquals(d1.getModel(), m);
        
        Datatype d2 = m.getDatatype("xs:token");
        assertNotNull(d2);
        assertFalse(d2.isAbstract());
        assertFalse(d2.isDeprecated());
        assertNull(d2.getDefinition());
        assertNull(d2.getListOf());
        assertNull(d2.getRestrictionOf());
        assertNull(d2.getUnionOf());
        assertEquals(d2.getModel(), m);
        
        Property p1 = m.getProperty("nc:EmploymentPositionBasisAbstract");
        assertNotNull(p1);
        assertTrue(p1.isAbstract());
        assertFalse(p1.isDeprecated());
        assertNull(p1.getClassType());
        assertNull(p1.getDatatype());
        assertNull(p1.getSubPropertyOf());
        
        Property p2 = m.getProperty("nc:EmploymentPositionBasisCode");
        assertNotNull(p2);
        assertFalse(p2.isAbstract());
        assertFalse(p2.isDeprecated());
        assertNull(p2.getClassType());
        assertEquals(p2.getDatatype(), d1);
        assertEquals(p2.getSubPropertyOf(), p1);
    }

   @Test
    public void testDeprecated () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/deprecated.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertTrue(m.getDatatype("nc:NumericType").isDeprecated());
        assertFalse(m.getDatatype("xs:decimal").isDeprecated());
    }
    
    @Test
    public void testHasvalue () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/hasValue.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        
        assertEquals(2, m.getNamespaceList().size());
        assertNotNull(m.getNamespaceByPrefix("nc"));
        assertNotNull(m.getNamespaceByPrefix("xs"));
        
        assertEquals(4, m.getComponentList().size());
        
        Datatype dt = m.getDatatype("nc:Degree90SimpleType");
        assertNotNull(dt);
        
        Property p = m.getProperty("nc:errorValue");
        assertNotNull(p);
        
        ClassType c = m.getClassType("nc:Degree90Type");
        assertNotNull(c);
        assertFalse(c.isAbstract());
        assertFalse(c.isAugmentable());
        assertFalse(c.isDeprecated());
        assertFalse(c.isExternal());
        assertNotNull(c.getDefinition());
        assertEquals(c.getModel(), m);
        assertEquals(c.getHasValue(), dt);
        assertEquals(1, c.hasPropertyList().size());
        HasProperty hp = c.hasPropertyList().get(0);
        assertNotNull(hp);
        assertEquals(hp.getProperty(), p);
        assertEquals(0, hp.minOccurs());
        assertEquals(1, hp.maxOccurs());
    }
    
    @Test
    public void testFacets () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/facets.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        
        assertEquals(2, m.getNamespaceList().size());
        assertNotNull(m.getNamespaceByPrefix("nc"));
        assertNotNull(m.getNamespaceByPrefix("xs"));
        
        assertEquals(2, m.getComponentList().size());
        
        Datatype dt = m.getDatatype("nc:FacetsType");
        assertNotNull(dt);
        assertNull(dt.getListOf());
        assertNull(dt.getUnionOf());
        RestrictionOf r = dt.getRestrictionOf();
        assertNotNull(r);
        assertEquals(r.getDatatype(), m.getDatatype("xs:decimal"));
        // Eleven facets tested (Enumeration not included here)
        List<Facet> fl = r.getFacetList();
        assertNotNull(fl);
        assertEquals(11, fl.size());
        assertEquals("WhiteSpace",     fl.get(0).getFacetKind());
        assertEquals("FractionDigits", fl.get(1).getFacetKind());
        assertEquals("Length",         fl.get(2).getFacetKind());
        assertEquals("MaxExclusive",   fl.get(3).getFacetKind());
        assertEquals("MaxInclusive",   fl.get(4).getFacetKind());
        assertEquals("MaxLength",      fl.get(5).getFacetKind());
        assertEquals("MinExclusive",   fl.get(6).getFacetKind());
        assertEquals("MinInclusive",   fl.get(7).getFacetKind());
        assertEquals("MinLength",      fl.get(8).getFacetKind());
        assertEquals("Pattern",        fl.get(9).getFacetKind());
        assertEquals("TotalDigits",    fl.get(10).getFacetKind());
        
        assertEquals("2",    fl.get(1).getStringVal());
        assertEquals("8",    fl.get(2).getStringVal());
        assertEquals("99.2", fl.get(3).getStringVal());
        assertEquals("27.5", fl.get(4).getStringVal());
        assertEquals("45",   fl.get(5).getStringVal());
        assertEquals("10.1", fl.get(6).getStringVal());
        assertEquals("34.5", fl.get(7).getStringVal());
        assertEquals("99",   fl.get(8).getStringVal());    
        assertEquals("\\d{4}\\.\\d{2}",fl.get(9).getStringVal());    
        assertEquals("5",    fl.get(10).getStringVal());
    }
    
    @Test
    public void testListOf () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/listOf.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        
        assertEquals(2, m.getNamespaceList().size());
        assertNotNull(m.getNamespaceByPrefix("nc"));
        assertNotNull(m.getNamespaceByPrefix("xs"));
        
        assertEquals(4, m.getComponentList().size());
 
        Datatype dt = m.getDatatype("nc:TokenListType");
        assertNotNull(dt);
        assertNull(dt.getUnionOf());
        assertNull(dt.getRestrictionOf());
        assertEquals(dt.getListOf(), m.getDatatype("xs:token"));       
    }

    @Test
    public void testProxy () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/proxy.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        assertEquals(2, m.getNamespaceList().size());  
        assertNull(m.getNamespaceByURI("http://release.niem.gov/niem/proxy/niem-xs/5.0/"));
        assertEquals(3, m.getComponentList().size());
        assertNotNull(m.getDatatype("xs:decimal"));
     }
    
    @Test
    public void testUnionOf () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/unionOf.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        
        assertEquals(2, m.getNamespaceList().size());
        assertNotNull(m.getNamespaceByPrefix("ns"));
        assertNotNull(m.getNamespaceByPrefix("xs"));
        
        assertEquals(4, m.getComponentList().size());
 
        Datatype dt = m.getDatatype("ns:UnionType");
        assertNotNull(dt);
        assertNull(dt.getListOf());
        assertNull(dt.getRestrictionOf());
        UnionOf u = dt.getUnionOf();
        assertNotNull(u);
        assertEquals(2, u.getDatatypeList().size());
        assertEquals("xs:decimal", u.getDatatypeList().get(0).getQName());
        assertEquals("xs:float", u.getDatatypeList().get(1).getQName());        
    }

    @Test
    public void testExternals () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/externals.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        
        assertEquals(5, m.getNamespaceList().size());
        assertNotNull(m.getNamespaceByPrefix("geo"));
        assertNotNull(m.getNamespaceByPrefix("gml"));
        assertNotNull(m.getNamespaceByPrefix("nc"));
        assertNotNull(m.getNamespaceByPrefix("xs"));
        
        assertEquals("OTHERNIEM", m.getNamespaceByPrefix("geo").getKindCode());
        assertEquals("EXTERNAL", m.getNamespaceByPrefix("gml").getKindCode());
        assertEquals("CORE", m.getNamespaceByPrefix("nc").getKindCode());
        assertEquals("EXTENSION", m.getNamespaceByPrefix("ns").getKindCode());
        assertEquals("XSD", m.getNamespaceByPrefix("xs").getKindCode());
        
        Namespace gml = m.getNamespaceByURI("http://www.opengis.net/gml/3.2");
        assertNotNull(gml);
        assertTrue(gml.isExternal());
        
        ClassType geoP = m.getClassType("geo:PointType");
        assertNotNull(geoP);
        assertTrue(geoP.isExternal());
        
        ClassType tpt = m.getClassType("ns:TrackPointType");
        assertNotNull(tpt);
        assertEquals(1, tpt.hasPropertyList().size());
        HasProperty h1 = tpt.hasPropertyList().get(0);
        assertEquals("geo:LocationGeospatialPoint", h1.getProperty().getQName());
        assertEquals(1, h1.minOccurs());
        assertEquals(1, h1.maxOccurs());
        assertFalse(h1.maxUnbounded());
    }

    @Test
    public void testAugmentations () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/augment-0.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNotNull(m);
        assertEquals(0, mr.getMessages().size());
        
        assertEquals(4, m.getNamespaceList().size());
        assertEquals(11, m.getComponentList().size());
        
        ClassType ct = m.getClassType("nc:AddressType");
        assertNotNull(ct);
        assertTrue(ct.isAugmentable());
        assertEquals(5, ct.hasPropertyList().size());
        
        HasProperty hp = ct.hasPropertyList().get(1);
        assertEquals("j:AddressCommentText", hp.getProperty().getQName());
        assertNull(hp.augmentElementNS());
        assertEquals(2, hp.augmentTypeNS().size());
        assertTrue(hp.augmentTypeNS().contains(m.getNamespaceByPrefix("test")));
        assertTrue(hp.augmentTypeNS().contains(m.getNamespaceByPrefix("j")));        
        
        hp = ct.hasPropertyList().get(3);
        assertEquals("j:AnotherAddress", hp.getProperty().getQName());
        assertEquals(hp.augmentElementNS(), m.getNamespaceByPrefix("j"));
        assertEquals(0, hp.augmentTypeNS().size());
    }
    
    @Test
    public void testMissingIDREF () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/missingIDRef.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNull(m);
        assertEquals(1, mr.getMessages().size());
        assertTrue(mr.getMessages().get(0).contains("no matching ID"));
    }
    
    @Test
    public void testMismatchIDRef () {
        FileInputStream cmfIS = null;
        File cmfFile = new File(testDirPath, "cmf/mismatchIDRef.cmf");
        try {
            cmfIS = new FileInputStream(cmfFile);
        } catch (FileNotFoundException ex) {
            fail("Where is my input file?");
        }
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(cmfIS);
        assertNull(m);
        assertEquals(1, mr.getMessages().size());
        assertTrue(mr.getMessages().get(0).contains("IDREF/URI type mismatch"));
    }

}
