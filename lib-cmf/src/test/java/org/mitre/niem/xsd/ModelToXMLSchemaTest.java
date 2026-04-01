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
import java.io.OutputStreamWriter;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import static javax.xml.xpath.XPathConstants.BOOLEAN;
import static javax.xml.xpath.XPathConstants.NODESET;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.cmf.ModelXMLWriter;
import org.mitre.niem.xml.XMLDocument;
import org.w3c.dom.Element;
import static org.w3c.dom.Node.ELEMENT_NODE;
import org.w3c.dom.NodeList;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXMLSchemaTest {
    private final static String resCDN  = "src/test/resources/cmf";
    private final static String resXDN = "src/test/resources/xsd6/";
    private static XPath xp;
    
    @TempDir
    File tmpD;
        
    public ModelToXMLSchemaTest() {
        xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new NamespaceContext() {
        @Override 
        public String getNamespaceURI(String prefix) {
          return switch (prefix) {
            case "appinfo" -> "https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/";
            case "j"    -> "https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/";
            case "nc"   -> "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/";
            case "xs"   -> XMLConstants.W3C_XML_SCHEMA_NS_URI;
            case "t"    -> "http://example.com/test/";
            case "test" -> "http://example.com/test/";
            case "structures" -> "https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/";
            case "structures5" -> "http://release.niem.gov/niem/structures/5.0/";
            default     -> XMLConstants.NULL_NS_URI;
        };
      }
      @Override public String getPrefix(String namespaceURI) { return null; }
      @Override public Iterator<String> getPrefixes(String namespaceURI) { return null; }
      });
    }
    
//    @Test
//    public void testOne () throws Exception {
//        var fnam = "augment";
//        var cmF  = new File(resCDN, fnam + ".cmf");
//        var rdr  = new ModelXMLReader();
//        var m    = rdr.readFiles(cmF);
//        var mtx  = new NewModelToXMLSchema(m);
//        var outD = new File(tmpD, "msg.xsd");
//        mtx.writeModelXSD(outD);
//    }
    
    @Test
    public void testAny () throws Exception {
        var fnam = "any";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        
        isFalse("//xs:element[@substitutionGroup]", doc);
        isFalse("//xs:element[@abstract='true']", doc);
        
        isTrue("(//xs:complexType[@name='Test1Type']//xs:any)[1]/@namespace = 'http://someNS/ http://otherNS/'", doc);
        isTrue("(//xs:complexType[@name='Test1Type']//xs:any)[1]/@processContents = 'lax'", doc);
        isTrue("(//xs:complexType[@name='Test2Type']//xs:anyAttribute)[1]/@processContents = 'strict'", doc);
        isTrue("not(//xs:complexType[@name='Test3Type']//xs:any)", doc);
    }
    
    @Test
    public void testArchVersions () throws Exception {
        var fnam = "archVersions";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
         
        isFalse("//xs:element[@substitutionGroup]", doc);
        isFalse("//xs:element[@abstract='true']", doc);
         
        isTrue("boolean(namespace::*[name()='nc' and string(.)='https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/'])", doc);
        isTrue("boolean(namespace::*[name()='nc5' and string(.)='http://release.niem.gov/niem/niem-core/5.0/'])", doc);
        isTrue("count(//xs:complexType[@name='OneTestType']//xs:element[@ref='nc:PersonName']) = 1", doc);
        isTrue("count(//xs:complexType[@name='OneTestType']//xs:element[@ref='nc5:CommentText']) = 1", doc);
        
        doc = makeDoc(pile, "niem5/niem-core5-skel");        
        isTrue("//xs:complexType[@name='PersonNameType']/xs:attribute[@ref='structures:metadata']", doc);
    }
    
    @Test
    public void testAttAugment () throws Exception {
        var fnam = "attAugment";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;        

        isFalse("//xs:element[@substitutionGroup]", doc);
        isFalse("//xs:element[@abstract='true']", doc);

        isTrue("//xs:complexType[@name='CCOneType']/xs:attribute[@ref='t:attProp']", doc);
        isTrue("//xs:complexType[@name='CCTwoType']/xs:attribute[@ref='t:attProp' and @use='required']", doc);
        isTrue("//xs:complexType[@name='SCOneType']/xs:simpleContent//xs:attribute[@ref='t:attProp']", doc);
        isTrue("//xs:complexType[@name='SCTwoType']/xs:simpleContent//xs:attribute[@ref='t:objPropRef']", doc);
    }
    
    @Test
    public void testAugment () throws Exception {
        var fnam = "augment";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        isFalse("//xs:element[@substitutionGroup]", doc);
        isFalse("//xs:element[@abstract='true']", doc);
         
        e = evalE("//xs:complexType[@name='EducationAugmentationType']/xs:sequence", doc);
        isTrue("count(./xs:element)=3", e);
        isTrue("./xs:element[1][@ref='t:CommentDestinationText']", e);
        isTrue("./xs:element[2][@ref='nc:CommentText']", e);
        isTrue("./xs:element[3][@ref='j:EducationTotalYearsText']", e);
        
        isTrue("//xs:element[@name='CommentDestinationText'][@type='xs:string']", doc);
        isTrue("//xs:element[@name='EducationAugmentation'][@type='t:EducationAugmentationType']", doc);
        isTrue("//xs:element[@name='TestAugElement'][@type='xs:token']", doc);
        isFalse("//xs:element[@substitutionGroup]", doc);
        
        doc = makeDoc(pile, "niem/niem-core-skel");
        e   = evalE("//xs:complexType[@name='CommentType']", doc);
        isTrue("count(./xs:sequence/xs:element)=2", e);
        isTrue("./xs:sequence/xs:element[1][@ref='nc:CommentText'][@minOccurs='0'][@maxOccurs='unbounded']", e);
        isTrue("./xs:sequence/xs:element[2][@ref='t:CommentDestinationText'][@minOccurs='0'][@maxOccurs='unbounded']", e);

        e   = evalE("//xs:complexType[@name='EducationType']", doc);
        isTrue("count(./xs:sequence/xs:element)=1", e);
        isTrue("./xs:sequence/xs:element[1][@ref='nc:EducationDescriptionText'][@minOccurs='0'][@maxOccurs='unbounded']", e);
        isTrue("count(./xs:sequence/xs:choice[@minOccurs='0'][@maxOccurs='unbounded'])=1", e);
        e = evalE("./xs:sequence/xs:choice", e);
        isTrue("count(./xs:element)=3", e);
        isTrue("./xs:element[1][@ref='t:EducationAugmentation']", e);
        isTrue("./xs:element[2][@ref='t:TestAugElement']", e);        
        isTrue("./xs:element[3][@ref='j:EducationAugmentation']", e);
        isFalse("./xs:element[@minOccurs]", e);
        isFalse("./xs:element[@maxOccurs]", e);
    }    
    
    @Test
    public void testChoice () throws Exception {
        var fnam = "choice";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        isFalse("//xs:element[@substitutionGroup]", doc);
        isFalse("//xs:element[@abstract='true']", doc);
        
        isTrue("count(//xs:complexType[@name='T1Type'])=1", doc);
        isTrue("count(//xs:complexType[@name='T1Type']//xs:choice)=0", doc);
        e = evalE("//xs:complexType[@name='T1Type']//xs:sequence", doc);
        isTrue("count(./xs:element)=1", e);
        isTrue("./xs:element[1][@ref='t:Prop1']", e);
        
        isTrue("count(//xs:complexType[@name='T2Type'])=1", doc);
        isTrue("count(//xs:complexType[@name='T2Type']//xs:choice)=0", doc);
        e = evalE("//xs:complexType[@name='T2Type']//xs:sequence", doc);
        isTrue("count(./xs:element)=1", e);
        isTrue("./xs:element[1][@ref='t:Prop1'][@minOccurs='0']", e);
        
        isTrue("count(//xs:complexType[@name='T3Type'])=1", doc);
        isTrue("count(//xs:complexType[@name='T3Type']//xs:choice[@minOccurs='0'])=1", doc);
        e = evalE("//xs:complexType[@name='T3Type']//xs:choice", doc);
        isTrue("count(./xs:element)=2", e);
        isTrue("./xs:element[1][@ref='t:Prop1']", e);
        isTrue("./xs:element[2][@ref='t:Prop2']", e);

        isTrue("count(//xs:complexType[@name='T4Type'])=1", doc);
        isTrue("count(//xs:complexType[@name='T4Type']//xs:choice[@minOccurs='0'])=0", doc);
        e = evalE("//xs:complexType[@name='T4Type']//xs:choice", doc);
        isTrue("count(./xs:element)=2", e);
        isTrue("./xs:element[1][@ref='t:Prop1']", e);
        isTrue("./xs:element[2][@ref='t:Prop2']", e);

        isTrue("count(//xs:complexType[@name='T5Type'])=1", doc);
        isTrue("count(//xs:complexType[@name='T5Type']//xs:choice[@minOccurs='0'])=0", doc);
        e = evalE("//xs:complexType[@name='T5Type']//xs:choice", doc);
        isTrue("count(./xs:element)=3", e);
        isTrue("./xs:element[1][@ref='t:Prop1']", e);
        isTrue("./xs:element[2][@ref='t:Prop2']", e);
        isTrue("./xs:element[3][@ref='t:Prop3']", e);
    }
    
    @Test
    public void testCodeListBinding () throws Exception {
        var fnam = "codeListBinding";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        isFalse("//xs:element[@substitutionGroup]", doc);
        isFalse("//xs:element[@abstract='true']", doc);

        e = evalE("//xs:simpleType[@name='TCodeList2Type']", doc);
        isTrue("./xs:restriction[@base='xs:token']", e);
        isTrue("count(./xs:restriction/xs:enumeration)=2", e);
        isTrue(".//xs:enumeration[@value='GB']", e);
        isTrue(".//xs:enumeration[@value='US']", e);

        e = evalE("//xs:simpleType[@name='TCodeList3Type']", doc);
        isTrue("./xs:restriction[@base='xs:token']", e);
        isTrue("count(./xs:restriction/xs:enumeration)=2", e);
        isTrue(".//xs:enumeration[@value='GB']", e);
        isTrue(".//xs:enumeration[@value='US']", e);
    }
    
    @Test
    public void testDataProperty () throws Exception {
        var fnam = "dataProperty";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        isFalse("//xs:element[@substitutionGroup]", doc);
        isFalse("//xs:element[@abstract='true']", doc);
        isFalse("//*[@appinfo:referenceAttributeIndicator]", doc);
        isFalse("//*[@appinfo:relationshipPropertyIndicator]", doc);          
        isFalse("//*[@appinfo:orderedPropertyIndicator]", doc); 
        isFalse("//*[@nillable]", doc);
    }
    
    @Test
    public void testDatatypes () throws Exception {
        var fnam = "datatypes";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        isFalse("//*[@appinfo:referenceAttributeIndicator]", doc);
        isFalse("//*[@appinfo:relationshipPropertyIndicator]", doc);          
        isFalse("//*[@appinfo:orderedPropertyIndicator]", doc); 
        isFalse("//*[@nillable]", doc);

        isTrue("//xs:complexType[@name='Literal1Type']", doc);
        isTrue("//xs:complexType[@name='Literal2Type']", doc); 
        isTrue("//xs:complexType[@name='Literal3Type']", doc);
        isTrue("//xs:complexType[@name='Literal4Type']", doc);
        
        isTrue("//xs:simpleType[@name='List1Type']", doc);
        isTrue("//xs:simpleType[@name='List2Type']", doc);
        isTrue("//xs:simpleType[@name='List3Type']", doc);
        isTrue("//xs:simpleType[@name='Literal2SimpleType']", doc);
        isTrue("//xs:simpleType[@name='Restrict10Type']", doc);
        isTrue("//xs:simpleType[@name='Restrict1Type']", doc);
        isTrue("//xs:simpleType[@name='Restrict2Type']", doc);
        isTrue("//xs:simpleType[@name='Restrict3Type']", doc);
        isTrue("//xs:simpleType[@name='Restrict4Type']", doc);
        isTrue("//xs:simpleType[@name='Restrict5Type']", doc);
        isTrue("//xs:simpleType[@name='Restrict6Type']", doc);
        isTrue("//xs:simpleType[@name='Restrict7Type']", doc);
        isTrue("//xs:simpleType[@name='Restrict8Type']", doc);
        isTrue("//xs:simpleType[@name='Restrict9Type']", doc);
        isTrue("//xs:simpleType[@name='UnionType']", doc);
        isTrue("//xs:simpleType[@name='Union2Type']", doc);
        
        isTrue("//xs:attribute[@name='AttProperty']", doc);
    }
    
    @Test
    public void testGaLitAtt () throws Exception {
        var fnam = "gaLitAtt";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;    
        
        isFalse("//xs:complexType[@name='CCOneType']//xs:attribute", doc);
        isFalse("//xs:complexType[@name='CCTwoType']//xs:attribute", doc);
        isFalse("//xs:complexType[@name='ObjType']//xs:attribute", doc);
        isFalse("//xs:complexType[@name='TestAssociationType']//xs:attribute", doc);
        isTrue("//xs:complexType[@name='SCOneType']//xs:attribute[@ref='test:attProp']", doc);
        isTrue("//xs:complexType[@name='SCTwoType']//xs:attribute[@ref='test:attProp']", doc);
    }
    
    @Test
    public void testGaLitObj () throws Exception {
        var fnam = "gaLitObj";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;    
        
        isFalse("//xs:complexType[@name='CCOneType']//xs:attribute", doc);
        isFalse("//xs:complexType[@name='CCTwoType']//xs:attribute", doc);
        isFalse("//xs:complexType[@name='ObjType']//xs:attribute", doc);
        isFalse("//xs:complexType[@name='TestAssociationType']//xs:attribute", doc);
        isTrue("//xs:complexType[@name='SCOneType']//xs:attribute[@ref='test:objPropRef']", doc);
        isTrue("//xs:complexType[@name='SCTwoType']//xs:attribute[@ref='test:objPropRef']", doc);
    }
    
    @Test
    public void testGaObjAtt () throws Exception {
        var fnam = "gaObjAtt";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        isTrue("//xs:complexType[@name='CCOneType']//xs:attribute[@ref='test:attProp']", doc);
        isTrue("//xs:complexType[@name='CCTwoType']//xs:attribute[@ref='test:attProp']", doc);
        isTrue("//xs:complexType[@name='ObjType']//xs:attribute[@ref='test:attProp']", doc);
        isFalse("//xs:simpleType[@name='TestAssociationType']//xs:attribute", doc);
        isFalse("//xs:simpleType[@name='SCOneType']//xs:attribute", doc);
        isFalse("//xs:simpleType[@name='SCTwoType']//xs:attribute", doc);
    }
    
    @Test
    public void testGaObjObj () throws Exception {
        var fnam = "gaObjObj";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        e = evalE("//xs:complexType[@name='CCOneType']/xs:sequence", doc);
        isTrue("count(./xs:element)=2", e);
        isTrue("./xs:element[@ref='test:ObjectAugmentation'][@minOccurs='0'][@maxOccurs='unbounded']", e);
        isTrue("./xs:element[@ref='test:DataProp']", e);
        isFalse("./xs:element[@ref='test:DataProp'][@minOccurs]", e);
        isFalse("./xs:element[@ref='test:DataProp'][@maxOccurs]", e);
        
        e = evalE("//xs:complexType[@name='CCTwoType']/xs:sequence", doc);
        isTrue("count(./xs:element)=2", e);
        isTrue("./xs:element[@ref='test:ObjectAugmentation'][@minOccurs='0'][@maxOccurs='unbounded']", e);
        isTrue("./xs:element[@ref='test:DataProp']", e);
        isFalse("./xs:element[@ref='test:DataProp'][@minOccurs]", e);
        isFalse("./xs:element[@ref='test:DataProp'][@maxOccurs]", e);
        
        e = evalE("//xs:complexType[@name='ObjType']/xs:sequence", doc);
        isTrue("count(./xs:element)=2", e);
        isTrue("./xs:element[@ref='test:ObjectAugmentation'][@minOccurs='0'][@maxOccurs='unbounded']", e);
        isTrue("./xs:element[@ref='test:DataProp']", e);
        isFalse("./xs:element[@ref='test:DataProp'][@minOccurs]", e);
        isFalse("./xs:element[@ref='test:DataProp'][@maxOccurs]", e);

        e = evalE("//xs:complexType[@name='TestAssociationType']/xs:sequence", doc);
        isTrue("count(./xs:element)=1", e);
    }
    
    @Test
    public void testList () throws Exception {
        var fnam = "list";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        isTrue("//xs:simpleType[@name='NonNegativeDoubleListType']/xs:list[@itemType='t:NonNegativeDoubleType']", doc);
    }
    
    @Test
    public void testLiteralClass () throws Exception {
        var fnam = "literalClass";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        isTrue("//xs:simpleType[@name='FooBottomType']/xs:restriction[@base='xs:string']", doc);

        e = evalE("//xs:simpleType[@name='FooCodeSimpleType']", doc);
        isTrue("./xs:restriction[@base='xs:token']", e);
        isTrue("count(./xs:restriction/xs:enumeration)=2", e);
        isTrue(".//xs:enumeration[@value='GB']", e);
        isTrue(".//xs:enumeration[@value='US']", e);
        
        e = evalE("//xs:complexType[@name='FooCodeType']", doc);
        isTrue("./xs:simpleContent/xs:extension[@base='test:FooCodeSimpleType']", e);
        isTrue(".//xs:extension/xs:attribute[@ref='test:personNameInitialIndicator']", e);
        
        isTrue("//xs:simpleType[@name='FooMiddleType']/xs:restriction[@base='test:FooBottomType']", doc);
        
        e = evalE("//xs:complexType[@name='FooTopType']", doc);
        isTrue("./xs:simpleContent/xs:extension[@base='test:FooMiddleType']", e);
        isTrue(".//xs:extension/xs:attribute[@ref='test:personNameInitialIndicator']", e);        
    }
    
    @Test
    public void testMessage () throws Exception {
        var fnam = "message";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        e = evalE("//xs:complexType[@name='T1Type']", doc);
        isTrue("count(./xs:sequence/xs:element)=2", e);
        isTrue("./xs:sequence/xs:element[@ref='t:Prop1']", e);
        isTrue("./xs:sequence/xs:element[@ref='t:AugProp1'][@minOccurs='0'][@maxOccurs='unbounded']", e);
        isFalse("./xs:sequence/xs:element[@ref='t:Prop1'][@minOccurs]", e);
        isFalse("./xs:sequence/xs:element[@ref='t:Prop1'][@maxOccurs]", e);
        isTrue("./xs:attribute[@ref='t:aprop1']", e);

        e = evalE("//xs:complexType[@name='T2Type']", doc);
        isTrue("./xs:complexContent/xs:extension[@base='t:T1Type']", e);
        e = evalE("./xs:complexContent/xs:extension", e);
        isTrue("count(./xs:attribute)=4", e);
        isTrue("./xs:attribute[@ref='t:aprop2']", e);
        isTrue("./xs:attribute[@ref='structures:id']", e);
        isTrue("./xs:attribute[@ref='structures:ref']", e);
        isTrue("./xs:attribute[@ref='structures:uri']", e);
        e = evalE("./xs:sequence", e);
        isTrue("count(./xs:element)=1", e);
        isTrue("count(./xs:choice)=1", e);
        isTrue("./xs:element[@ref='t:Prop2']", e);
        isFalse("./xs:element[@ref='t:Prop2'][@minOccurs]", e);
        isFalse("./xs:element[@ref='t:Prop2'][@maxOccurs]", e);
        isTrue("./xs:choice[@minOccurs='0'][@maxOccurs='unbounded']", e);
        isTrue("./xs:choice/xs:element[@ref='t:OtherAugProp2']", e);
        isTrue("./xs:choice/xs:element[@ref='t:T2Augmentation']", e);
        isFalse("./xs:choice/xs:element[@minOccurs]", e);
        isFalse("./xs:choice/xs:element[@maxOccurs]", e);
        
        e = evalE("//xs:complexType[@name='T3Type']", doc);    
        isTrue("./xs:sequence/xs:element[1][@ref='t:Prop1']", e);
        isTrue("./xs:sequence/xs:element[2][@ref='t:Prop2']", e);         
        isTrue("./xs:sequence/xs:element[3][@ref='t:Prop3']", e);
        isTrue("./xs:attribute[@ref='structures:id']", e);
        isTrue("./xs:attribute[@ref='structures:ref']", e);
        isFalse("./xs:attribute[@ref='structures:uri']", e);   
        
        isFalse("//xs:element[@name='T1Prop'][@nillable='true']", doc);
        isTrue("//xs:element[@name='T2Prop'][@nillable='true']", doc);
        isTrue("//xs:element[@name='T3Prop'][@nillable='true']", doc);        
    }
    
    @Test
    public void testRefCode () throws Exception {
        var fnam = "refCode";
        var pile = makePile(fnam);
        var doc  = makeDoc(pile, fnam);
        Element e;
        
        e = evalE("//xs:complexType[@name='AnyRefType']", doc);
        isTrue("./xs:attribute[@ref='structures:id']", e);
        isTrue("./xs:attribute[@ref='structures:ref']", e);
        isTrue("./xs:attribute[@ref='structures:uri']", e);

        e = evalE("//xs:complexType[@name='OneType']", doc);
        isFalse("//xs:attribute[@ref='structures:*']", e);

        e = evalE("//xs:complexType[@name='TwoType']", doc);
        isTrue(".//xs:extension[@base='t:OneType']", e);
        isFalse("//xs:attribute[@ref='structures:*']", e);

        e = evalE("//xs:complexType[@name='ThreeType']", doc);
        isTrue(".//xs:extension[@base='t:TwoType']", e);
        isFalse("//xs:attribute[@ref='structures:*']", e);

        e = evalE("//xs:complexType[@name='FourType']", doc);
        isTrue(".//xs:extension[@base='t:ThreeType']", e);
        isTrue(".//xs:extension/xs:attribute[@ref='structures:id']", e);
        isTrue(".//xs:extension/xs:attribute[@ref='structures:ref']", e);
        isTrue(".//xs:extension/xs:attribute[@ref='structures:uri']", e);

        e = evalE("//xs:complexType[@name='FiveType']", doc);
        isFalse(".//xs:extension", e);
        isFalse("//xs:attribute[@ref='structures:*']", e);
        isTrue("count(./xs:sequence/xs:element[@ref='t:DataProperty'])=5", e);
        
        e = evalE("//xs:complexType[@name='URIRefType']", doc);
        isFalse(".//xs:extension", e);        
        isTrue("./xs:attribute[@ref='structures:id']", e);
        isFalse("./xs:attribute[@ref='structures:ref']", e);
        isTrue("./xs:attribute[@ref='structures:uri']", e);
        
        isFalse("//xs:element[@name='FiveProp'][@nillable='true']", doc);
        isTrue("//xs:element[@name='FourProp'][@nillable='true']", doc);
        isFalse("//xs:element[@name='ThreeProp'][@nillable='true']", doc);  
    }
    
    File makePile (String fname) throws Exception {
        var cmF  = new File(resCDN, fname + ".cmf");
        var rdr  = new ModelXMLReader();
        var m    = rdr.readFiles(cmF);
        var mtx  = new ModelToXMLSchema(m);
        var outD = new File(tmpD, "msg.xsd");
        mtx.writeModelXSD(outD);
        var doc  = new NIEMSchema(new File(outD, fname + ".xsd"));
        var msgs = doc.javaXMsgs();
        assertThat(msgs.isEmpty());
        return(outD);
    }
    
    Element makeDoc (File pile, String fname) throws Exception { 
        var xsdF = new File(pile, fname + ".xsd");
        var xsd  = new XMLDocument(xsdF);
        var doc  = xsd.documentElement();
        return doc;
    }
    
    void isTrue (String exp, Element e) throws XPathExpressionException {
        assertTrue((boolean)xp.evaluate(exp, e, BOOLEAN));
    }
    
    void isFalse (String exp, Element e) throws XPathExpressionException {
        assertFalse((boolean)xp.evaluate(exp, e, BOOLEAN));
    }

    boolean evalB (String exp, Element e) throws XPathExpressionException {
        return (boolean) xp.evaluate(exp, e, BOOLEAN);
    }
    
    Element evalE (String exp, Element e) throws XPathExpressionException {
        var res = (NodeList) xp.evaluate(exp, e, NODESET);
        return (Element) res.item(0);
    }
    
    NodeList evalN (String exp, Element e) throws XPathExpressionException {
        return (NodeList) xp.evaluate(exp, e, NODESET);
    }
    
    @Test
    public void testWriteModelXSD () throws Exception {
        var resF  = new File(resXDN);
        var files = FileUtils.listFiles(resF, new String[]{"xsd"}, false);

        for (var xsdF : files) {
            if ("externals.xsd".equals(xsdF.getName())) continue;
            if ("imports.xsd".equals(xsdF.getName())) continue;
            if ("objectProperty.xsd".equals(xsdF.getName())) continue;
            testFile(xsdF);
        }
    }

    public void testFile (File xsdF) throws Exception {
        var xsdName = xsdF.getName();
        var xsdBase = FilenameUtils.getBaseName(xsdF.toString());
        var cmfOneF = new File(tmpD, "one.cmf");
        var xsdD    = new File(tmpD, "xsd");
//        System.err.println(xsdName);

        // Create first CMF file from XSD source
        var sch   = new NIEMSchema(xsdF);
        var mfxsd = new ModelFromXSD();
        var mw    = new ModelXMLWriter();
        var model = mfxsd.createModel(sch);
        var os    = new FileOutputStream(cmfOneF);
        var ow    = new OutputStreamWriter(os, "UTF-8");
        mw.writeXML(model, ow);
        ow.close();
        
        // Create a new XSD pile from CMF
        var mtxsd = new ModelToXMLSchema(model);
        if (null != model.namespaceObj("test")) mtxsd.setRootNamespace("test");
        else if (null != model.namespaceObj("t")) mtxsd.setRootNamespace("t");
        mtxsd.setCatalogPath("xml-catalog.xml");
        mtxsd.writeModelXSD(xsdD);
        var xsdOneF = new File(xsdD, xsdName);
        var schOne  = new NIEMSchema(xsdOneF);
        var msgs    = schOne.javaXMsgs();
        var goodXSD = msgs.isEmpty();
        assertTrue(goodXSD);        
    }
//    
//    @Test
//    public void testOneFile () throws Exception {
//        var resF  = new File(resDN, "choice.xsd");
//        testFile(resF);
//    }
}
