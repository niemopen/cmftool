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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import nl.altindag.log.LogCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.mitre.niem.cmf.AugmentRecord;
import static org.mitre.niem.cmf.AugmentRecord.AUG_OBJECT;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.CodeListBinding;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.LocalTerm;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import static org.mitre.niem.cmf.NamespaceKind.NSK_OTHERNIEM;
import org.mitre.niem.cmf.Property;
import static org.mitre.niem.xsd.CheckModel.checkAugCCwA;
import static org.mitre.niem.xsd.CheckModel.checkAugCCwE;
import static org.mitre.niem.xsd.CheckModel.checkAugGEAonly;
import static org.mitre.niem.xsd.CheckModel.checkAugGEBoth;
import static org.mitre.niem.xsd.CheckModel.checkAugGEOonly;
import static org.mitre.niem.xsd.CheckModel.checkAugGESonly;
import static org.mitre.niem.xsd.CheckModel.checkAugSCwA;
import static org.mitre.niem.xsd.CheckModel.checkAugSCwE;
import static org.mitre.niem.xsd.CheckModel.checkComponents;
import static org.mitre.niem.xsd.CheckModel.checkDatatypes;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSDTest {

    private static List<LogCaptor> logs;
    public String[] testDirs = {"src/test/resources/xsd6"};
//    public String[] testDirs = {"src/test/resources/xsd5", "src/test/resources/xsd6"};

    public ModelFromXSDTest() {
    }

    @BeforeAll
    public static void setupLogCaptor() {
        logs = new ArrayList<>();
        logs.add(LogCaptor.forClass(ModelFromXSD.class));
        logs.add(LogCaptor.forClass(XMLSchema.class));
        logs.add(LogCaptor.forClass(XMLSchemaDocument.class));
        logs.add(LogCaptor.forClass(XStringObject.class));
        logs.add(LogCaptor.forClass(XMLCatalogResolver.class));
    }

    @AfterEach
    public void clearLogs() {
        for (var log : logs) {
            log.clearLogs();
        }
    }

    @AfterAll
    public static void tearDown() {
        for (var log : logs) {
            log.close();
        }
    }

    public void assertEmptyLogs() {
        for (var log : logs) {
            var errors = log.getErrorLogs();
            var warns = log.getWarnLogs();
            assertTrue(errors.isEmpty());
            assertTrue(warns.isEmpty());
        }
    }
    
    public List<String> getInfoLogs() {
        var allInfos = new ArrayList<String>();
        for (var log : logs) {
            var warns = log.getInfoLogs();
            allInfos.addAll(warns);
        }
        return allInfos;
    }

     public List<String> getWarnLogs() {
        var allWarns = new ArrayList<String>();
        for (var log : logs) {
            var warns = log.getWarnLogs();
            allWarns.addAll(warns);
        }
        return allWarns;
    }
     
    public List<String> getErrorLogs() {
        var allErrors = new ArrayList<String>();
        for (var log : logs) {
            var errors = log.getErrorLogs();
            allErrors.addAll(errors);
        }
        return allErrors;
    }
     
    public Model createModel (File f) throws Exception {
        var mfact = new ModelFromXSD();
        var model = mfact.createModel(f.toString());
        return model;
    }
        
    @Test
    public void testAugCCwA () throws Exception {
        for (var tdir : testDirs) {
            var f = new File(tdir, "augCCwA.xsd");
            if (!f.canRead()) continue;
            checkAugCCwA(createModel(f));
            assertEmptyLogs();
        }
    }
    
    @Test
    public void testAugCCwE () throws Exception {
        for (var tdir : testDirs) {
            var f = new File(tdir, "augCCwE.xsd");
            if (!f.canRead()) continue;
            checkAugCCwE(createModel(f));
            assertEmptyLogs();
        }
    }
    
    @Test
    public void testAugSCwA () throws Exception {
        for (var tdir : testDirs) {
            var f = new File(tdir, "augSCwA.xsd");
            if (!f.canRead()) continue;
            checkAugSCwA(createModel(f));
            assertEmptyLogs();
        }
    }
    
    @Test
    public void testAugSCwE () throws Exception {
        for (var tdir : testDirs) {
            var f = new File(tdir, "augSCwE.xsd");
            if (!f.canRead()) continue;
            checkAugSCwE(createModel(f));
            assertTrue(getErrorLogs().isEmpty());
            assertThat(getWarnLogs()).anyMatch(s -> s.matches("augmentingNamespace .* does not exist .*"));            
        }
    }
        
    @Test
    public void testAugGEBoth () throws Exception {
        for (var tdir : testDirs) {
            var f = new File(tdir, "augGEBoth.xsd");
            if (!f.canRead()) continue;
            checkAugGEBoth(createModel(f));
            assertEmptyLogs();          
        }
    }    
        
    @Test
    public void testAugGEAonly () throws Exception {
        for (var tdir : testDirs) {
            var f = new File(tdir, "augGEAonly.xsd");
            if (!f.canRead()) continue;
            checkAugGEAonly(createModel(f));
            assertEmptyLogs();          
        }
    }

    @Test
    public void testAugGEOonly () throws Exception {
        for (var tdir : testDirs) {
            var f = new File(tdir, "augGEOonly.xsd");
            if (!f.canRead()) continue;
            checkAugGEOonly(createModel(f));
            assertEmptyLogs();          
        }
    }

    @Test
    public void testAugGESonly () throws Exception {
        for (var tdir : testDirs) {
            var f = new File(tdir, "augGESonly.xsd");
            if (!f.canRead()) continue;
            checkAugGESonly(createModel(f));
            assertEmptyLogs();          
        }
    }

    @Test
    public void testComponents () throws Exception {
        for (var tdir : testDirs) {
            var f = new File(tdir, "components.xsd");
            if (!f.canRead()) continue;
            checkComponents(createModel(f));
            assertEmptyLogs();
        }
    }
    
    @Test
    public void testDatatypes () throws Exception {
        for (var tdir : testDirs) {
            var f = new File(tdir, "datatypes.xsd");
            if (!f.canRead()) continue;
            checkDatatypes(createModel(f));
            assertEmptyLogs();
        }        
    }
     
    /////////////////////////////

    @Test
    public void testCreateModel() throws Exception {
        for (var tdir : testDirs) {
            String sch = tdir + "/externals.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            List<Namespace> nslist = m.getNamespaceList();
            assertThat(nslist)
                    .hasSize(8)
                    .extracting(Namespace::getNamespacePrefix)
                    .contains("geo", "gml", "nc", "niem-xs", 
                              "ns", "structures", "xlink", "xs");
            assertEmptyLogs();
        }
    }

    @Test
    public void testAttAugmentation() throws Exception {
        for (var tdir : testDirs) {
            String sch = tdir + "/attAug.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);
            var myNS = m.getNamespaceByPrefix("my");
            var ncNS = m.getNamespaceByPrefix("nc");
            var edCT = m.getClassType("nc:EducationType");
            var hp = edCT.getHasProperty("my:privacyText");
            assertNotNull(hp);
            assertThat(hp.augmentingNS())
                    .containsExactly(myNS);
        }
    }

    @Test
    @DisplayName("Augmentation")
    public void testAugmentation() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/augment.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertThat(m.getNamespaceList())
                    .hasSize(6)
                    .extracting(Namespace::getNamespacePrefix)
                    .contains("j", "nc", "niem-xs", "structures", "test", "xs");
            assertEquals("Augmentation test schema", m.getNamespaceByPrefix("test").getDocumentation());
            assertThat(m.getNamespaceByPrefix("nc").augmentList()).hasSize(0);
            assertThat(m.getNamespaceByPrefix("xs").augmentList()).hasSize(0);
            assertThat(m.getNamespaceByPrefix("j").augmentList())
                    .hasSize(3)
                    .extracting(AugmentRecord::getClassType)
                    .containsOnly(m.getClassType("nc:AddressType"));
            assertThat(m.getNamespaceByPrefix("j").augmentList())
                    .hasSize(3)
                    .extracting(AugmentRecord::getProperty)
                    .containsExactly(m.getProperty("j:AddressCommentText"),
                            m.getProperty("j:AddressVerifiedDate"),
                            m.getProperty("j:AnotherAddress"));
            assertNotNull(m.getProperty("test:BoogalaText"));
            assertThat(m.getNamespaceByPrefix("test").augmentList())
                    .extracting(AugmentRecord::getProperty)
                    .containsOnly(m.getProperty("j:AddressCommentText"),
                            m.getProperty("test:BoogalaText"),
                            m.getProperty("test:boogalaProp"));

            ClassType ct = m.getClassType("nc:AddressType");
            assertNotNull(ct);
            assertTrue(ct.isAugmentable());
            List<HasProperty> hpl = ct.hasPropertyList();
            assertNotNull(hpl);
            assertEquals(6, hpl.size());

            HasProperty hp = hpl.get(0);
            assertEquals(hp.getProperty(), m.getProperty("nc:AddressFullText"));
            assertEquals(1, hp.minOccurs());
            assertEquals(1, hp.maxOccurs());
            assertFalse(hp.maxUnbounded());
            assertEquals(0, hp.augmentingNS().size());

            hp = hpl.get(1);
            assertEquals(hp.getProperty(), m.getProperty("j:AddressCommentText"));
            assertEquals(0, hp.minOccurs());
            assertEquals(1, hp.maxOccurs());
            assertFalse(hp.maxUnbounded());
            assertThat(hp.augmentingNS())
                    .hasSize(2)
                    .extracting(Namespace::getNamespacePrefix)
                    .contains("test", "j");

            hp = hpl.get(2);
            assertEquals(hp.getProperty(), m.getProperty("j:AddressVerifiedDate"));
            assertEquals(0, hp.minOccurs());
            assertTrue(hp.maxUnbounded());
            assertEquals(1, hp.augmentingNS().size());
            assertThat(hp.augmentingNS())
                    .hasSize(1)
                    .extracting(Namespace::getNamespacePrefix)
                    .contains("j");

            hp = hpl.get(3);
            assertEquals(hp.getProperty(), m.getProperty("j:AnotherAddress"));
            assertEquals(0, hp.minOccurs());
            assertTrue(hp.maxUnbounded());
            assertThat(hp.augmentingNS())
                    .hasSize(1)
                    .extracting(Namespace::getNamespacePrefix)
                    .contains("j");

            hp = hpl.get(4);
            assertEquals(hp.getProperty(), m.getProperty("test:BoogalaText"));
            assertEquals(0, hp.minOccurs());
            assertEquals(1, hp.maxOccurs());
            assertFalse(hp.maxUnbounded());
            assertThat(hp.augmentingNS())
                    .hasSize(1)
                    .extracting(Namespace::getNamespacePrefix)
                    .contains("test");
            assertEmptyLogs();
        }
    }

    @Test
    public void testAugmentProp() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/augmentProp.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertTrue(m.getClassType("nc:AmountType").isAugmentable());
            assertFalse(m.getClassType("nc:CaseDispositionDecisionType").isAugmentable());
            assertNull(m.getProperty("nc:AmountAugmentationPoint"));
            assertNull(m.getProperty("nc:CaseDispositionDecisionAugmentationPoint"));
        }
    }

    @Test
    @DisplayName("CLI")
    public void testCodeListInstance() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/cli.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            var ct = m.getClassType("nc:CodeType");
            var p1 = m.getProperty("nc:CodeLiteral");
            var c1 = m.getProperty("cli:codeListColumnName");
            var c2 = m.getProperty("cli:codeListConstrainingIndicator");
            var c3 = m.getProperty("cli:codeListURI");
            assertThat(ct.hasPropertyList())
                    .hasSize(4)
                    .extracting(HasProperty::getProperty)
                    .contains(p1, c1, c2, c3);
            assertEmptyLogs();
        }
    }

    @Test
    @DisplayName("CLSA")
    public void testCodeListSchemaAppinfo() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/clsa.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            Datatype dt = m.getDatatype("genc:CountryAlpha2CodeType");
            CodeListBinding clb = dt.getCodeListBinding();
            assertNotNull(dt);
            assertNotNull(clb);
            assertEquals("http://api.nsgreg.nga.mil/geo-political/GENC/2/3-11", clb.getURI());
            assertEquals("foo", clb.getColumn());
            assertTrue(clb.isConstraining());

            dt = m.getDatatype("genc:CountryAlpha3CodeType");
            clb = dt.getCodeListBinding();
            assertNotNull(dt);
            assertNotNull(clb);
            assertEquals("http://api.nsgreg.nga.mil/geo-political/GENC/3/3-11", clb.getURI());
            assertEquals("#code", clb.getColumn());
            assertFalse(clb.isConstraining());
            assertEmptyLogs();
        }
    }

//    @Test
//    @DisplayName("Code list")
//    public void testCodelist() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
//        for (var tdir : testDirs) {
//            String sch = tdir + "/codelist.xsd";
//            File f = new File(sch);
//            if (!f.canRead()) {
//                continue;
//            }
//            ModelFromXSD mfact = new ModelFromXSD();
//            Model m = mfact.createModel(sch);
//
//            assertEquals(3, m.getNamespaceList().size());
//            assertEquals(4, m.getComponentList().size());
//            Datatype dt = m.getDatatype("nc:EmploymentPositionBasisCodeType");
//            assertNotNull(dt);
//            assertNull(dt.getCodeListBinding());
//            RestrictionOf r = dt.getRestrictionOf();
//            assertNotNull(r);
//            assertEquals(r.getDatatype(), m.getDatatype("xs:token"));
//            List<Facet> fl = r.getFacetList();
//            assertNotNull(fl);
//            assertEquals(3, fl.size());
//            assertEquals(fl.get(0).getFacetKind(), "Enumeration");
//            assertEquals(fl.get(0).getStringVal(), "contractor");
//            assertEquals(fl.get(1).getFacetKind(), "Enumeration");
//            assertEquals(fl.get(1).getStringVal(), "non-permanent");
//            assertEquals(fl.get(2).getFacetKind(), "Enumeration");
//            assertEquals(fl.get(2).getStringVal(), "permanent");
//            Property p1 = m.getProperty("nc:EmploymentPositionBasisAbstract");
//            assertNotNull(p1);
//            assertNull(p1.getClassType());
//            assertNull(p1.getDatatype());
//            assertTrue(p1.isAbstract());
//            Property p2 = m.getProperty("nc:EmploymentPositionBasisCode");
//            assertNotNull(p2);
//            assertEquals(p2.getSubPropertyOf(), p1);
//            assertEquals(p2.getDatatype(), dt);
//            assertNull(p2.getClassType());
//            assertNotNull(m.getDatatype("xs:token"));
//            assertNull(m.getDatatype("nc:EmploymentPositionBasisCodeSimpleType"));
//            assertEmptyLogs();
//        }
//    }

//    @Test
//    @DisplayName("Code list")
//    public void testCodelistNoSimpleType() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
//        for (var tdir : testDirs) {
//            String sch = tdir + "/codelistNoSType.xsd";
//            File f = new File(sch);
//            if (!f.canRead()) {
//                continue;
//            }
//            ModelFromXSD mfact = new ModelFromXSD();
//            Model m = mfact.createModel(sch);
//            var dt = m.getDatatype("nc:NamespaceKindCodeType");
//            var xstoken = m.getDatatype("xs:token");
//            var restrict = dt.getRestrictionOf();
//            assertThat(restrict.getDatatype()).isEqualTo(xstoken);
//            assertThat(restrict.getFacetList())
//                    .extracting(Facet::getStringVal)
//                    .contains("EXTENSION", "DOMAIN");
//        }
//    }
//
//    @Test
//    @DisplayName("Code list ClassType")
//    public void testCodelistClasstype() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
//        for (var tdir : testDirs) {
//            String sch = tdir + "/codelistClassType.xsd";
//            File f = new File(sch);
//            if (!f.canRead()) {
//                continue;
//            }
//            ModelFromXSD mfact = new ModelFromXSD();
//            Model m = mfact.createModel(sch);
//
//            assertEquals(3, m.getNamespaceList().size());
//            assertEquals(7, m.getComponentList().size());
//            Datatype dt = m.getDatatype("nc:EmploymentPositionBasisCodeDatatype");
//            assertNotNull(dt);
//            RestrictionOf r = dt.getRestrictionOf();
//            assertNotNull(r);
//            assertEquals(r.getDatatype(), m.getDatatype("xs:token"));
//            List<Facet> fl = r.getFacetList();
//            assertNotNull(fl);
//            assertEquals(3, fl.size());
//            assertEquals(fl.get(0).getFacetKind(), "Enumeration");
//            assertEquals(fl.get(0).getStringVal(), "contractor");
//            assertEquals(fl.get(1).getFacetKind(), "Enumeration");
//            assertEquals(fl.get(1).getStringVal(), "non-permanent");
//            assertEquals(fl.get(2).getFacetKind(), "Enumeration");
//            assertEquals(fl.get(2).getStringVal(), "permanent");
//
//            ClassType ct = m.getClassType("nc:EmploymentPositionBasisCodeType");
//            assertNotNull(ct);
//            List<HasProperty> hpl = ct.hasPropertyList();
//            assertNotNull(hpl);
//            assertEquals(2, hpl.size());
//            HasProperty hp = hpl.get(0);
//            assertNotNull(hp);
//            assertEquals(hp.getProperty(), m.getProperty("nc:EmploymentPositionBasisCodeLiteral"));
//            assertEquals(1, hp.minOccurs());
//            assertEquals(1, hp.maxOccurs());
//            assertFalse(hp.maxUnbounded());
//
//            hp = hpl.get(1);
//            assertNotNull(hp);
//            assertEquals(hp.getProperty(), m.getProperty("nc:foo"));
//            assertEquals(1, hp.minOccurs());
//            assertEquals(1, hp.maxOccurs());
//            assertFalse(hp.maxUnbounded());
//
//            Property p1 = m.getProperty("nc:EmploymentPositionBasisAbstract");
//            assertNotNull(p1);
//            assertNull(p1.getClassType());
//            assertNull(p1.getDatatype());
//            assertTrue(p1.isAbstract());
//            Property p2 = m.getProperty("nc:EmploymentPositionBasisCode");
//            assertNotNull(p2);
//            assertEquals(p2.getSubPropertyOf(), p1);
//            assertEquals(p2.getClassType(), ct);
//            assertNull(p2.getDatatype());
//            assertNotNull(m.getDatatype("xs:token"));
//            assertNotNull(m.getDatatype("nc:EmploymentPositionBasisCodeDatatype"));
//            assertEmptyLogs();
//        }
//    }

//    @Test
//    @DisplayName("Code list Union")
//    public void testCodelistUnion() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
//        for (var tdir : testDirs) {
//            String sch = tdir + "/codelistUnion.xsd";
//            File f = new File(sch);
//            if (!f.canRead()) {
//                continue;
//            }
//            ModelFromXSD mfact = new ModelFromXSD();
//            Model m = mfact.createModel(sch);
//
//            assertEquals(3, m.getNamespaceList().size());
//            assertEquals(4, m.getComponentList().size());
//            Datatype u = m.getDatatype("test:ColorCodeType");
//            Datatype c = m.getDatatype("test:CoolColorCodeType");
//            Datatype w = m.getDatatype("test:WarmColorCodeType");
//            UnionOf ur = u.getUnionOf();
//            List<Datatype> dtl = ur.getDatatypeList();
//            assertEquals(2, dtl.size());
//            assertTrue(dtl.contains(c));
//            assertTrue(dtl.contains(w));
//            assertNotNull(m.getDatatype("xs:token"));
//            assertEmptyLogs();
//        }
//    }

    @Test
    @DisplayName("Complex content")
    public void testComplexContent() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/complexContent.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(4, m.getNamespaceList().size());
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
            assertEmptyLogs();
        }
    }

    @Test
    public void testCrossNSstype() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/crossNSstype.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(4, m.getNamespaceList().size());
            assertThat(m.getComponentList())
                    .hasSize(9)
                    .extracting(Component::getQName)
                    .contains(
                            "nc:AngMin", "test:AngMinute", "test:AttributedAngularMinuteLiteral",
                            "test:SomeAtt", "test:AttributedAngularMinuteType", "nc:AngularMinuteType",
                            "test:AngularMinuteType", "xs:decimal", "xs:token");
            assertEquals("nc:AngularMinuteType", m.getProperty("nc:AngMin").getDatatype().getQName());
            assertEquals("test:AngularMinuteType", m.getProperty("test:AngMinute").getDatatype().getQName());
            assertEquals("nc:AngularMinuteType", m.getProperty("test:AttributedAngularMinuteLiteral").getDatatype().getQName());
            assertEquals("xs:token", m.getProperty("test:SomeAtt").getDatatype().getQName());
            assertEmptyLogs();
        }
    }
//
//    @Test
//    public void testDefaultFacets() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
//        for (var tdir : testDirs) {
//            String sch = tdir + "/defaultFacets.xsd";
//            File f = new File(sch);
//            if (!f.canRead()) {
//                continue;
//            }
//            ModelFromXSD mfact = new ModelFromXSD();
//            Model m = mfact.createModel(sch);
//            for (var c : m.getComponentList()) {
//                var dt = c.asDatatype();
//                if (null == dt) {
//                    continue;
//                }
//                if (null == dt.getRestrictionOf()) {
//                    continue;
//                }
//                assertThat(dt.getRestrictionOf().getFacetList())
//                        .hasSizeBetween(0, 1);
//            }
//        }
//    }

    @Test
    @DisplayName("appinfo:deprecated")
    public void testDeprecated() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/deprecated.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(4, m.getNamespaceList().size());
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
            assertEmptyLogs();
        }
    }

    @Test
    @DisplayName("documenation")
    public void testDocumentation() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String base = tdir + "/";
            String[] testSchemas = {"complexContent", "literal-2", "literal-3", "literal-4", "literal-5", "literal-6"};
            String[] args = new String[1];

            //  Test whether all components in all schema documents have definitions
            for (var ts : testSchemas) {
                String tsf = base + ts + ".xsd";
                File f = new File(tsf);
                if (!f.canRead()) continue;
                args[0] = tsf;
                ModelFromXSD mfact = new ModelFromXSD();
                Model m = mfact.createModel(args);
                for (var c : m.getComponentList()) {
                    if ("http://release.niem.gov/niem/niem-core/5.0/".equals(c.getNamespaceURI())) {
                        assertNotNull(c.getDocumentation());
                    }
                }
            }
        }
    }
//
//    @Test
//    @DisplayName("Complex content")
//    public void testDoubleType() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
//        for (var tdir : testDirs) {
//            String sch = tdir + "/doubleType.xsd";
//            File f = new File(sch);
//            if (!f.canRead()) {
//                continue;
//            }
//            ModelFromXSD mfact = new ModelFromXSD();
//            Model m = mfact.createModel(sch);
//            var dt = m.getDatatype("cbrn:DoubleType");
//            var r = dt.getRestrictionOf();
//            var rb = r.getDatatype();
//            assertEquals("xs:double", rb.getQName());
//        }
//    }

    @Test
    @DisplayName("Complex content")
    public void testExtensionOf() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/complexContent.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(4, m.getNamespaceList().size());
            assertEquals(13, m.getComponentList().size());
            ClassType persNT = m.getClassType("nc:PersonNameTextType");
            ClassType propNT = m.getClassType("nc:ProperNameTextType");
            ClassType textT = m.getClassType("nc:TextType");
            assertEquals(textT, propNT.getExtensionOfClass());
            assertEquals(propNT, persNT.getExtensionOfClass());
            assertEmptyLogs();
        }
    }
    
    @Test
    public void testGlobalElementAug() throws Exception {
        for (var tdir : testDirs) {
            String sch = tdir + "/globalAug-2.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);
            
            var ns  = m.getNamespaceByPrefix("nc");
            var arl = ns.augmentList();
            var ar  = arl.get(0);
            assertEquals(1, arl.size());
            assertEquals(AUG_OBJECT, ar.getGlobalAugKind());
        }
    }

    @Test
    @DisplayName("xml:lang")
    public void testLanguage() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/augment.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);
            for (var ns : m.getNamespaceList()) {
                if (ns.getKind() < NSK_OTHERNIEM)
                    assertEquals("en-US", ns.getLanguage());
            }
            assertEmptyLogs();
        }
    }
//
//    @Test
//    @DisplayName("List type")
//    public void testListType() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
//        for (var tdir : testDirs) {
//            String sch = tdir + "/list.xsd";
//            File f = new File(sch);
//            if (!f.canRead()) {
//                continue;
//            }
//            ModelFromXSD mfact = new ModelFromXSD();
//            Model m = mfact.createModel(sch);
//
//            assertEquals(3, m.getNamespaceList().size());
//            assertEquals(2, m.getComponentList().size());
//            Datatype dt = m.getDatatype("nc:TokenListType");
//            assertNotNull(dt);
//            assertNull(dt.getRestrictionOf());
//            assertNull(dt.unionOf());
//            Datatype d2 = dt.getListOf();
//            assertNotNull(d2);
//            assertEquals(d2, m.getDatatype("xs:token"));
//            assertEmptyLogs();
//        }
//    }

    @Test
    public void testLocalTerms() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/localTerm.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);
            Namespace ns = m.getNamespaceByPrefix("nc");
            List<LocalTerm> lsl = ns.localTermList();
            assertEquals(3, lsl.size());
            assertThat(lsl).extracting(LocalTerm::getTerm)
                    .containsOnly("2D", "3D", "Test");
            assertNotNull(lsl.get(0).getLiteral());
            assertNull(lsl.get(0).getDefinition());
            assertNull(lsl.get(0).getSourceURIs());
            assertEquals(0, lsl.get(0).citationList().size());
            assertNull(lsl.get(1).getLiteral());
            assertNotNull(lsl.get(1).getDefinition());
            assertNull(lsl.get(1).getSourceURIs());
            assertEquals(0, lsl.get(1).citationList().size());
            assertNull(lsl.get(2).getLiteral());
            assertNotNull(lsl.get(2).getDefinition());
            assertNotNull(lsl.get(2).getSourceURIs());
            assertEquals(2, lsl.get(2).citationList().size());
        }
    }

    @Test
    public void testLiteral_0() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/literal-0.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(4, m.getNamespaceList().size());
            assertEquals(2, m.getComponentList().size());
            Datatype dt = m.getDatatype("nc:TextType");
            assertNotNull(dt);
            assertNotNull(dt.getRestrictionBase());
            assertEmptyLogs();
        }
    }

    @Test
    public void testLiteral_1() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/literal-1.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(4, m.getNamespaceList().size());
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
            assertEmptyLogs();
        }
    }

    @Test
    public void testLiteral_2() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/literal-2.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(4, m.getNamespaceList().size());
            assertEquals(4, m.getComponentList().size());
            Datatype dt = m.getDatatype("nc:TextType");
            assertNotNull(dt);

            ClassType ct = m.getClassType("nc:TextType");
            assertNull(ct);
            assertEmptyLogs();
        }
    }

    @Test
    public void testLiteral_3() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/literal-3.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(4, m.getNamespaceList().size());
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
            assertEmptyLogs();
        }
    }

    @Test
    public void testLiteral_4() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/literal-4.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(4, m.getNamespaceList().size());
            assertEquals(7, m.getComponentList().size());
            Datatype dt1 = m.getDatatype("nc:TextType");
            assertNotNull(dt1);

            Datatype dt2 = m.getDatatype("nc:ProperNameTextType");
            assertNotNull(dt2);
            assertEquals(dt2.getRestrictionBase(), dt1);

            ClassType ct = m.getClassType("nc:PersonNameTextType");
            assertNotNull(ct);
            assertThat(ct.hasPropertyList())
                    .hasSize(2)
                    .extracting(HasProperty::getProperty)
                    .extracting(Property::getName)
                    .contains("PersonNameTextLiteral", "personNameInitialIndicator");
            assertEmptyLogs();
        }
    }

    @Test
    public void testLiteral_5() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/literal-5.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(3, m.getNamespaceList().size());
            assertEquals(2, m.getComponentList().size());
            Datatype dt1 = m.getDatatype("nc:AngularMinuteType");
            assertNotNull(dt1);
            Datatype dt2 = m.getDatatype("xs:decimal");
            assertNotNull(dt2);
            assertEquals(dt2.getRestrictionBase(), dt2);
            assertThat(dt2.facetList()).hasSize(2);
            assertEmptyLogs();
        }
    }

    @Test
    public void testLiteral_6() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/literal-6.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(3, m.getNamespaceList().size());
            assertEquals(8, m.getComponentList().size());
            ClassType ct = m.getClassType("nc:AngularMinuteType");
            Datatype dt = m.getDatatype("nc:AngularMinuteDatatype");
            Property p = m.getProperty("nc:AngularMinuteLiteral");
            assertThat(ct.hasPropertyList())
                    .hasSize(2)
                    .extracting(HasProperty::getProperty)
                    .extracting(Property::getName)
                    .contains("AngularMinuteLiteral", "SomeAtt");
            assertNotNull(dt);
            assertNotNull(p);
            assertEquals(p.getDatatype(), dt);
            assertEmptyLogs();
        }
    }

    @Test
    public void testLiteral_7() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/literal-7.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(3, m.getNamespaceList().size());
            assertEquals(8, m.getComponentList().size());
            ClassType ct = m.getClassType("nc:AttributedAngularMinuteType");
            Datatype dt = m.getDatatype("nc:AngularMinuteType");
            Property p = m.getProperty("nc:AttributedAngularMinuteLiteral");
            assertThat(ct.hasPropertyList())
                    .hasSize(2)
                    .extracting(HasProperty::getProperty)
                    .extracting(Property::getName)
                    .contains("AttributedAngularMinuteLiteral", "SomeAtt");
            assertNotNull(dt);
            assertNotNull(p);
            assertEquals(p.getDatatype(), dt);
            assertEmptyLogs();
        }
    }

    @Test
    @DisplayName("Namespace prefix prioritization")
    public void testNamespace_1() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/namespace-1.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);
            if (tdir.endsWith("6")) {
                Namespace ns = m.getNamespaceByURI("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/");
                assertEquals(5, m.getNamespaceList().size());
                assertEquals("nc", m.getNamespaceByURI("http://example.com/Foo/1.0/").getNamespacePrefix());
                assertEquals("bar", m.getNamespaceByURI("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/").getNamespacePrefix());
            }
            else {
                Namespace ns = m.getNamespaceByURI("http://release.niem.gov/niem/niem-core/5.0/");
                assertEquals(5, m.getNamespaceList().size());
                assertEquals("nc", m.getNamespaceByURI("http://example.com/Foo/1.0/").getNamespacePrefix());
                assertEquals("bar", m.getNamespaceByURI("http://release.niem.gov/niem/niem-core/5.0/").getNamespacePrefix());                
            }
            assertEmptyLogs();
        }
    }

    @Test
    @DisplayName("Namespace prefix munging")
    public void testNamespace_2() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/namespace-2.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(5, m.getNamespaceList().size());
            assertEquals("nc", m.getNamespaceByURI("http://example.com/Foo/1.0/").getNamespacePrefix());
            if (tdir.endsWith("6")) {
                assertEquals("nc_6", m.getNamespaceByURI("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/").getNamespacePrefix());
            } else {
                assertEquals("nc_5", m.getNamespaceByURI("http://release.niem.gov/niem/niem-core/5.0/").getNamespacePrefix());
            }
            assertEmptyLogs();
        }
    }

    @Test
    @DisplayName("Proxy")
    public void testProxy() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/proxy.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(4, m.getNamespaceList().size());
            assertEquals(5, m.getComponentList().size());
            Datatype dt = m.getDatatype("nc:PercentType");
            assertNotNull(dt);
            assertEquals(dt.getRestrictionBase(), m.getDatatype("xs:decimal"));
            Property p = m.getProperty("nc:ConfidencePercent");
            assertNotNull(p);
            assertEquals(p.getDatatype(), dt);
            assertNull(p.getClassType());
            p = m.getProperty("nc:SomeText");
            assertNotNull(p);
            assertEquals(p.getDatatype(), m.getDatatype("xs:string"));
            assertNull(p.getClassType());
            assertEmptyLogs();
        }
    }
    
    @Test
    public void testReferenceCode() throws Exception {
        for (var tdir : testDirs) {
            String sch = tdir + "/nillable.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);
            
            var warns = getWarnLogs();
            var infos = getInfoLogs();
            Collections.sort(infos);
            Collections.sort(warns);
            assertThat(infos.get(0)).containsPattern("nc:ObjPropAnyRef has appinfo:referenceCode");
            
            assertEquals("ANY", m.getClassType("nc:Class-1Type").getReferenceCode());
            assertEquals("REF", m.getClassType("nc:Class-2Type").getReferenceCode());
            var p = m.getProperty("nc:DataProp");
            var rc = p.getReferenceCode();
            assertEquals(null, m.getProperty("nc:DataProp").getReferenceCode());
            assertEquals("ANY", m.getProperty("nc:ObjPropAnyRef-1").getReferenceCode());
            assertEquals("ANY", m.getProperty("nc:ObjPropAnyRef-2").getReferenceCode());
            assertEquals("NONE", m.getProperty("nc:ObjPropNoRef-1").getReferenceCode());
            assertEquals("NONE", m.getProperty("nc:ObjPropNoRef-2").getReferenceCode());
            assertEquals("REF", m.getProperty("nc:ObjPropOnlyRef").getReferenceCode());
            assertEquals("URI", m.getProperty("nc:ObjPropOnlyURI").getReferenceCode());
        }
    }

    @Test
    public void testRefDocumentation() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/refDocumentation.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);
            var ct = m.getClassType("nc:PersonNameType");
            assertNotNull(ct.hasPropertyList().get(0).getDefinition());
            assertNull(ct.hasPropertyList().get(1).getDefinition());
        }
    }

    @Test
    @DisplayName("Restriction")
    public void testRestriction() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/restriction.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(3, m.getNamespaceList().size());
            assertEquals(2, m.getComponentList().size());
            assertNull(m.getDatatype("nc:AngularMinuteSimpleType"));
            Datatype dt = m.getDatatype("nc:AngularMinuteType");
            assertNotNull(dt);
            assertEquals(dt.getRestrictionBase(), m.getDatatype("xs:decimal"));
            List<Facet> fl = dt.facetList();
            Facet fa = fl.get(0);
            assertEquals("MaxExclusive", fa.getFacetKind());
            assertEquals("60.0", fa.getStringVal());
            fa = fl.get(1);
            assertEquals("MinInclusive", fa.getFacetKind());
            assertEquals("0.0", fa.getStringVal());
            assertEmptyLogs();
        }
    }

    @Test
    public void testStructuresType() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/structuresType.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            var p = m.getProperty("cbrn:CaseMetadata");
            assertNull(p.getClassType());
            assertNull(p.getDatatype());
        }
    }

    @Test
    @DisplayName("TwoVersions")
    public void testTwoVersions() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/twoversions.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals("http://release.niem.gov/niem/niem-core/4.0/", m.getURI("nc_4"));
            assertEquals("http://release.niem.gov/niem/niem-core/5.0/", m.getURI("nc"));
            assertEquals("http://www.w3.org/2001/XMLSchema", m.getURI("xs"));
            assertNotNull(m.getProperty("nc_4:ConfidencePercent"));
            assertNotNull(m.getProperty("nc:PersonGivenName"));
            assertEmptyLogs();
        }
    }

//    @Test
//    @DisplayName("Union type")
//    public void testUnionType() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
//        for (var tdir : testDirs) {
//            String sch = tdir + "/union.xsd";
//            File f = new File(sch);
//            if (!f.canRead()) {
//                continue;
//            }
//            ModelFromXSD mfact = new ModelFromXSD();
//            Model m = mfact.createModel(sch);
//
//            assertEquals(3, m.getNamespaceList().size());
//            assertEquals(4, m.getComponentList().size());
//            Datatype dt = m.getDatatype("ns:UnionType");
//            assertNotNull(dt);
//            assertNull(dt.getRestrictionOf());
//            assertNull(dt.getListOf());
//            UnionOf u = dt.getUnionOf();
//            assertNotNull(u);
//            List<Datatype> ul = u.getDatatypeList();
//            assertNotNull(ul);
//            assertEquals(2, ul.size());
//            assertEquals(ul.get(0), m.getDatatype("xs:decimal"));
//            assertEquals(ul.get(1), m.getDatatype("xs:float"));
//            assertEmptyLogs();
//        }
//    }

    @Test
    public void testWhitespace() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/whitespace.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            Datatype dt = m.getDatatype("nc:CStringType");
            Facet fa = dt.facetList().get(0);
            assertEquals("MaxLength", fa.getFacetKind());
            assertEquals("20", fa.getStringVal());
            fa = dt.facetList().get(1);
            assertEquals(2, dt.facetList().size());
            assertEquals("WhiteSpace", fa.getFacetKind());
            assertEquals("collapse", fa.getStringVal());

            dt = m.getDatatype("nc:LStringType");
            fa = dt.facetList().get(0);
            assertEquals(1, dt.facetList().size());
            assertEquals("MaxLength", fa.getFacetKind());
            assertEquals("20", fa.getStringVal());

            dt = m.getDatatype("nc:AngularMinuteType");
            fa = dt.facetList().get(0);
            assertEquals(2, dt.facetList().size());
            assertEquals("MaxExclusive", fa.getFacetKind());
            assertEquals("60.0", fa.getStringVal());
            fa = dt.facetList().get(1);
            assertEquals("MinInclusive", fa.getFacetKind());
            assertEquals("0.0", fa.getStringVal());
            assertEmptyLogs();
        }
    }

    @Test
    @DisplayName("xml:lang")
    public void testXMLlang() throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        for (var tdir : testDirs) {
            String sch = tdir + "/xml-lang.xsd";
            File f = new File(sch);
            if (!f.canRead()) {
                continue;
            }
            ModelFromXSD mfact = new ModelFromXSD();
            Model m = mfact.createModel(sch);

            assertEquals(5, m.getNamespaceList().size());
            assertEquals(7, m.getComponentList().size());
            Namespace xml = m.getNamespaceByPrefix("xml");
            assertNotNull(xml);
            Property p = m.getProperty("xml:lang");
            assertNotNull(p);
            ClassType ct = m.getClassType("nc:TextType");
            assertEquals(ct.hasPropertyList().get(3).getProperty(), p);
            assertEmptyLogs();
        }
    }

//    @Test
//    public void debugTest () throws Exception {         
//        var def = Charset.defaultCharset().displayName();
//        String[] args = { "tmp/debug.xsd" };
//        ModelFromXSD mfact = new ModelFromXSD();
//        Model m = mfact.createModel(args);
//        
//        var sw = new StringWriter();
//        var pw = new PrintWriter(sw);
//        var mw = new ModelXMLWriter();
//        mw.writeXML(m, pw);         
//        int i = 0;
//    }
}
