/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.StringJoiner;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.cmf.CMFObject.CMF_LIST;
import static org.mitre.niem.cmf.CMFObject.CMF_RESTRICTION;
import static org.mitre.niem.cmf.CMFObject.CMF_UNION;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.DataProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.NamespaceMap;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.Restriction;
import org.mitre.niem.utility.MapToSet;
import static org.mitre.niem.utility.StringUtils.listToString;
import static org.mitre.niem.utility.StringUtils.replaceSuffix;
import static org.mitre.niem.utility.StringUtils.setToString;
import org.mitre.niem.xml.XMLSchemaDocument;
import org.mitre.niem.xml.XSDWriter;
import static org.mitre.niem.xsd.NamespaceKind.versionToCtNsURI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import static org.w3c.dom.Node.ELEMENT_NODE;
import org.xml.sax.SAXException;

/**
 * A class for writing the XSD representation of a particular model.  In theory,
 * you can use one instance of this class to write the same model to many
 * schema document piles, perhaps with a differnt architecture version, catalog
 * path, and/or root namespace.  
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXSDModel extends ModelToXSD {
    private static final Logger LOG = LogManager.getLogger(ModelToXSDModel.class);
    
    protected final MapToSet<String,String> proxyUs     = new MapToSet<>();     // proxy nsU -> set of proxy type QNs
    protected final HashSet<String> complexTypeUS       = new HashSet<>();      // type def URIs
   
    public ModelToXSDModel (Model m) { super(m); }

    // Creates a prefix for the conformance target assertions namespace (preferred = ct)
    // Adds a namespace declaration and a CTA attribute to the root schema element.
    @Override
    protected void addConformanceAssertions(Element root, Namespace ns, NamespaceMap nsmap) {
        var vers = ns.archVersion();
        if (null != useArchVersion) vers = useArchVersion;
        var ctNSU = versionToCtNsURI(vers);
        var ctPre = nsmap.assignPrefix("ct", ctNSU);
        var ctQ   = ctPre + ":" + "conformanceTargets";
        root.setAttributeNS(ctNSU, ctQ, listToString(ns.ctargL()));
        root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:"+ctPre, ctNSU);
        
        var structU = bc2U.get("STRUCTURES");
        refnsUS.add(structU);
    }

    // Create appinfo:Augmentation elements as needed within the first
    // xs:annotation/xs:appinfo element in the root schema element,
    // creating those as needed.
    @Override
    protected void addAugmentAppinfo (Element root, Namespace ns) {
        Element appE = null;                    // xs:appinfo element, if we need it
        for (var arec : ns.augL()) {
            var ap  = arec.property();
            var act = arec.classType();
            if (arec.index() != -2) continue;   // this aug doesn't need an Augmentation element
                
            if (null == appE) appE = getAppinfoElement(root);
            var appinfoPre = bc2pre.get("APPINFO");
            var appinfoU   = bc2U.get("APPINFO");
            var doc  = root.getOwnerDocument();
            var augE = doc.createElementNS(appinfoU, appinfoPre + ":" + "Augmentation");
            setAttribute(augE, "property", arec.property().qname());
            setAttribute(augE, "globalClassCode", setToString(arec.codeS()));
            if (null != act) setAttribute(augE, "class", act.qname());            
            if ("1".equals(arec.minOccurs())) setAttribute(augE, "use", "required");
            appE.appendChild(augE);
            refnsUS.add(appinfoU);
        }        
    }

    // Create appinfo:LocalTerm elements as needed within the first
    // xs:annotation/xs:appinfo element in the root schema element,
    // creating those as needed.
    @Override
    protected void addLocalTerms(Element root, Namespace ns) {
        if (ns.locTermL().isEmpty()) return;
        var appinfoPre = bc2pre.get("APPINFO");
        var appinfoU   = bc2U.get("APPINFO");
        var appE = getAppinfoElement(root);
        for (var lt : ns.locTermL()) {
            var doc = root.getOwnerDocument();
            var ltE = doc.createElementNS(appinfoU, appinfoPre + ":" + "LocalTerm");
            setAttribute(ltE, "term", lt.term());
            setAttribute(ltE, "literal", lt.literal());
            setAttribute(ltE, "definition", lt.documentation());
            setAttribute(ltE, "sourceURIs", listToString(lt.sourceL()));
            for (var cit : lt.citationL()) {
                var citE = doc.createElementNS(appinfoU, appinfoPre + ":" + "SourceText");
                citE.setTextContent(cit.text());
                if (!"en-US".equals(cit.lang())) citE.setAttribute("xml:lang", cit.lang());
                ltE.appendChild(citE);
            }
            if (null == appE) appE = getAppinfoElement(root);
            appE.appendChild(ltE);
            refnsUS.add(appinfoU);
        }
    }  
        
    @Override
    protected void createComplexTypes (Document doc, Namespace ns) {
        // Create complex types for literal classes and ordinary classes.
        var xctUs = new HashSet<String>();
        for (var ct : m.classTypeL()) {
            if (ct.namespace() != ns) continue;
            if (ct.hasSimpleContent()) createCSCType(doc, ct);
            else createCCCType(doc, ct);
            xctUs.add(ct.uri());
        }
        // Create CSC types for datatypes.  But don't create a CSC wrapper around
        // a simple type if we already created a type with the same name.
        for (var dt : m.datatypeL()) {
            var wrapU = replaceSuffix(dt.uri(), "SimpleType", "Type");
            if (dt.namespace() == ns && !xctUs.contains(wrapU))
                createCSCType(doc, dt);
        }        
    }
    
    // Create a complex type with complex content from a non-literal class object
    @Override
    protected void createCCCType (Document doc, ClassType ct) {
        var appinfoPre    = bc2pre.get("APPINFO");
        var appinfoU      = bc2U.get("APPINFO");
        var structuresPre = bc2pre.get("STRUCTURES");
        var structU   = bc2U.get("STRUCTURES");
        
        var ctN = ct.name();
        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        var anE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        var ccE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexContent");
        var exE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
        var sqE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:sequence");

        populateTypeElement(doc, ctE, ct);
        exE.appendChild(sqE);
        ccE.appendChild(exE);
        ctE.appendChild(ccE);
        defEL.add(ctE);

        var baseQ = structuresPre + ":";
        if (null != ct.subClassOf()) { 
            baseQ = ct.subClassOf().qname();
            refnsUS.add(ct.subClassOf().namespaceURI());
        }
        else if (ct.name().endsWith("AdapterType"))      baseQ = baseQ + "AdapterType";
        else if (ct.name().endsWith("AssociationType"))  baseQ = baseQ + "AssociationType";
        else if (ct.name().endsWith("AugmentationType")) baseQ = baseQ + "AugmentationType"; 
        else baseQ = baseQ + "ObjectType";            
        exE.setAttribute("base", baseQ);
        
        // Process object properties
        for (var pa : ct.propAssocL()) {
            Element pE;
            var p = pa.property();
            if (p.isAttribute()) {
                pE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
                var dp  = (DataProperty)pa.property();
                pE.setAttribute("ref", dp.qname());
                if ("1".equals(pa.minOccurs())) pE.setAttribute("use", "required");
                exE.appendChild(pE);
            }
            else if (p.isChoice()) {
                pE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:choice");
                var choiceL = new ArrayList<>(p.allSubProps());
                Collections.sort(choiceL);
                for (var chp : choiceL) {
                    var chE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
                    chE.setAttribute("ref", chp.qname());
                    refnsUS.add(chp.namespaceURI());  
                    pE.appendChild(chE);
                }
                if (!"1".equals(pa.minOccurs())) pE.setAttribute("minOccurs", pa.minOccurs());
                if (!"1".equals(pa.maxOccurs())) pE.setAttribute("maxOccurs", pa.maxOccurs()); 
                if (pE.hasChildNodes()) sqE.appendChild(pE);
            }
            else {
                pE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
                pE.setAttribute("ref", p.qname());    
                if (!"1".equals(pa.minOccurs())) pE.setAttribute("minOccurs", pa.minOccurs());
                if (!"1".equals(pa.maxOccurs())) pE.setAttribute("maxOccurs", pa.maxOccurs());                
                sqE.appendChild(pE);
            }
            refnsUS.add(p.namespaceURI());
            addDocumentation(pE, pa.docL());     
        }
        // Process object wildcards    
        for (var ap : ct.anyL()) {
            Element aE;
            if (ap.isAttribute()) {
                aE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:anyAttribute");
                exE.appendChild(aE);
            }
            else {
                aE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:any");            
                if (!"1".equals(ap.minOccurs())) aE.setAttribute("minOccurs", ap.minOccurs());
                if (!"1".equals(ap.maxOccurs())) aE.setAttribute("maxOccurs", ap.maxOccurs());                
                sqE.appendChild(aE);
            }
            setAttribute(aE, "processContents", ap.processCode());
            setAttribute(aE, "namespace", ap.nsConstraint());   
        }
        // Possibly create and add an augmentation point element
        if (!ctN.endsWith("AdapterType") && !ctN.endsWith("AugmentationType")) {
            var elE    = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
            var apname = replaceSuffix(ctN, "Type", "AugmentationPoint");
            elE.setAttribute("ref", ct.namespace().prefix() + ":" + apname);
            elE.setAttribute("minOccurs", "0");
            elE.setAttribute("maxOccurs", "unbounded");
            sqE.appendChild(elE);
            var apE    = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
            apE.setAttribute("name", apname);
            apE.setAttribute("abstract", "true");
            addDocumentation(apE, "An augmentation point for " + ctN + ".");
            decEL.add(apE);
        }
    }

    // Create a complex type with simple content from a literal class object,
    // or a class derived from a literal class.
    @Override
    protected void createCSCType (Document doc, ClassType ct) {
        var proxyPre  = bc2pre.get("NIEM-XS");
        var proxyU    = bc2U.get("NIEM-XS");
        var structPre = bc2pre.get("STRUCTURES");
        var structU   = bc2U.get("STRUCTURES");
        
        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        var anE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        var scE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
        var exE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");

        populateTypeElement(doc, ctE, ct);
        scE.appendChild(exE);
        ctE.appendChild(scE);

        // Add all the attribute references to xs:extension
        for (int i = 0; i < ct.propAssocL().size(); i++) {
            var pa = ct.propAssocL().get(i);
            var atE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            var dp  = (DataProperty)pa.property();
            if (!dp.isAttribute()) continue;
            atE.setAttribute("ref", dp.qname());
            if ("1".equals(pa.minOccurs())) atE.setAttribute("use", "required");
            refnsUS.add(dp.namespaceURI());
            addDocumentation(atE, pa.docL());
            exE.appendChild(atE);
        }
        // Extension base may be a simple type
        var ctname = ct.qname();
        var dt   = ct.literalDatatype();
        if (null != dt && simpleTypes.contains(dt)) {
            var agE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attributeGroup");
            var dtnsU = dt.namespaceURI();
            var dtQ   = dt.qname();
            if (!dtQ.endsWith("SimpleType"))
                dtQ = replaceSuffix(dtQ, "Type", "SimpleType");
            exE.setAttribute("base", dtQ);
            agE.setAttribute("ref", structPre + ":" + "SimpleObjectAttributeGroup");
            exE.appendChild(agE);
            refnsUS.add(dtnsU);
        }
        // Or the extension base may be another model class
        else if (null != dt && dt.namespace().isModelNS()) {
            var baseQ = dt.qname();
            exE.setAttribute("base", baseQ);
            refnsUS.add(dt.namespaceURI());
        }
        else if (null != ct.subClassOf()) {
            var baseQ = ct.subClassOf().qname();
            exE.setAttribute("base", baseQ);
            refnsUS.add(ct.subClassOf().namespaceURI());
        }
        // Or the extension base may be a XSD primitive
        else if (null != dt) {
            var baseQ = proxyPre + ":" + dt.name();
            exE.setAttribute("base", baseQ);
            refnsUS.add(proxyU);
            proxyUs.add(proxyU, dt.name());
        }   
        else LOG.error("Can't determine extension base for {}", ct.qname());
        defEL.add(ctE);
    }    
    
    // Create an xs:complexType element from a Datatype object.
    @Override
    protected void createCSCType (Document doc, Datatype dt) {
        if (W3C_XML_SCHEMA_NS_URI.equals(dt.namespaceURI())) return;
        if (XML_NS_URI.equals(dt.namespaceURI())) return;
        
        var structPre = bc2pre.get("STRUCTURES");
        var structU   = bc2U.get("STRUCTURES");
        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");       
        var scE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
        
        populateTypeElement(doc, ctE, dt);
        ctE.appendChild(scE);
        if (dt.name().endsWith("SimpleType")) 
            setAttribute(ctE, "name", replaceSuffix(dt.name(), "SimpleType", "Type"));

        var atgE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attributeGroup");
        setAttribute(atgE, "ref", structPre + ":" + "SimpleObjectAttributeGroup");
        
        if (simpleTypes.contains(dt)) {
            var extE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
            setAttribute(extE, "base", datatypeQName(dt));
            extE.appendChild(atgE);
            scE.appendChild(extE);
        }
        else {
            if (CMF_RESTRICTION != dt.getType()) {
                LOG.error("{} is not a simple type or a restriction", dt.qname());
                return;
            }
            var r    = (Restriction)dt;
            var bdt  = dt.base();
            var bdtQ = proxifyQName(bdt);
            populateRestriction(doc, scE, r, bdtQ);
        }
        decEL.add(ctE);
    }

    // Create an xs:simpleType element from a Datatype object.    
    @Override
    protected void createSimpleType (Document doc, Datatype dt) {
        if (W3C_XML_SCHEMA_NS_URI.equals(dt.namespaceURI())) return;
        if (XML_NS_URI.equals(dt.namespaceURI())) return;
        
        var stE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleType");
        populateTypeElement(doc, stE, dt);
        if (!dt.name().endsWith("SimpleType")) 
            setAttribute(stE, "name", replaceSuffix(dt.name(), "Type", "SimpleType"));
        
        switch (dt.getType()) {
        case CMF_LIST:          populateList(doc, stE, dt); break;
        case CMF_RESTRICTION:   populateRestriction(doc, stE, dt); break;
        case CMF_UNION:         populateUnion(doc, stE, dt); break;
        }
        defEL.add(stE);
    }


    // Populate the common attributes of xs:simpleType and xs:complexType elements
    // from class or datatype objects.
    protected void populateTypeElement (Document doc, 
        Element e,                          // xs:complexType or xs:simpleType
        Component c) {                      // create typedefs from this component

        e.setAttribute("name", c.name());
        addDocumentation(e, c.docL());
        
        Element appE    = null;
        var appinfoPre = bc2pre.get("APPINFO");
        var appinfoU   = bc2U.get("APPINFO"); 
        
        if (null != c.codeListBinding()) {
            var clsaPre = bc2pre.get("CLSA");
            var clsaU   = bc2U.get("CLSA");
            var clb     = c.codeListBinding();
            var apE     = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:appinfo");
            var clE     = doc.createElementNS(clsaU, clsaPre + ":" + "SimpleCodeListBinding");
            setAttribute(clE, "codeListURI", clb.codeListURI());
            setAttribute(clE, "columnName", clb.column());
            if (clb.isConstraining()) setAttribute(clE, "constrainingIndicator", "true");
            refnsUS.add(clsaU);
            if (null == appE) appE = getAppinfoElement(e);
            appE.appendChild(clE);
        }
        if (c.isAbstract())   e.setAttribute("abstract", "true");
        if (c.isDeprecated()) {
            setAttribute(e, appinfoU, appinfoPre + ":" + "deprecated", "true");
            refnsUS.add(appinfoU);
        }
        if (!c.referenceCode().isEmpty()) {
            setAttribute(e, appinfoU, appinfoPre + ":" + "referenceCode", c.referenceCode());        
            refnsUS.add(appinfoU);
        }
    }
    
    // Creates an xs:attribute or xs:element schema element from a property object.
    @Override
    protected void createDeclaration (Document doc, Property p) {
        if (W3C_XML_SCHEMA_NS_URI.equals(p.namespaceURI())) return;
        if (XML_NS_URI.equals(p.namespaceURI())) return;
        if (p.name().endsWith("Literal")) return;
        if (p.isChoice()) return;

        var appinfoPre = bc2pre.get("APPINFO");
        var appinfoU   = bc2U.get("APPINFO");      
        var proxyPre   = bc2pre.get("NIEM-XS");
        var proxyU     = bc2U.get("NIEM-XS");
        var structPre  = bc2pre.get("STRUCTURES");

        Element decE;
        if (p.isAttribute()) decE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        else decE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
        
        var pU  = p.uri();
        var ptQ = "";
        var pt  = p.type();
        if (null != pt) { 
            refnsUS.add(pt.namespaceURI());
            ptQ = pt.qname();
            if (!p.isAttribute() && W3C_XML_SCHEMA_NS_URI.equals(pt.namespaceURI())) {
                ptQ = proxyPre + ":" + pt.name();
                refnsUS.add(proxyU);
                proxyUs.add(proxyU, pt.name());
            }
            else if (p.isAttribute() && !ptQ.endsWith("SimpleType"))
                ptQ = replaceSuffix(ptQ, "Type", "SimpleType");
        }
        decE.setAttribute("name", p.name());
        setAttribute(decE, "type", ptQ);
        if (p.isAbstract()) {
            decE.setAttribute("abstract", "true");
        }
        if (!p.isAttribute() && !p.isAbstract()) {
            decE.setAttribute("nillable", "true");
        }
        if (p.isDeprecated()) {
            setAttribute(decE, appinfoU, appinfoPre + ":" + "deprecated", "true");
            refnsUS.add(appinfoU);
        }
        if (p.isOrdered()) {
            setAttribute(decE, appinfoU, appinfoPre + ":" + "orderedPropertyIndicator", "true");
            refnsUS.add(appinfoU);
        }
        if (p.isRefAttribute()) {
            setAttribute(decE, appinfoU, appinfoPre + ":" + "referenceAttributeIndicator", "true");
            refnsUS.add(appinfoU);
        }
        if (!p.referenceCode().isEmpty()) {
            setAttribute(decE, appinfoU, appinfoPre + ":" + "referenceCode", p.referenceCode());
            refnsUS.add(appinfoU);
        }
        if (p.isRelationship()) {
            setAttribute(decE, appinfoU, appinfoPre + ":" + "relationshipPropertyIndicator", "true");
            refnsUS.add(appinfoU);
        }
        addDocumentation(decE, p.docL());

        // Gather all the subproperties created by cmf:SubPropertyOf elements.
        // These typically come from an xs:choice element in a model XSD document.
        // These will be @substitutionGroup values
        var subQL = new StringJoiner(" ");
        var subL  = new ArrayList<>(p.subPropertyOfS());
        Collections.sort(subL);
        for (var subp : subL) {
            if (!subp.isChoice()) {
                subQL.add(subp.qname());
                refnsUS.add(subp.namespaceURI());
            }
        }
        // Now consider augmentations not part of an augmentation type
        // These are created by element decs substituable for an augmentation point.
        var apQ = "";
        var apU = pU2augPU.get(pU);
        if (null != apU) subQL.add(m.uriToQN(apU));
        var subQ = subQL.toString();
        if (!subQ.isBlank()) setAttribute(decE, "substitutionGroup", subQ);
        decEL.add(decE);        
    }
    

    
    // A datatype object in the XSD namespace turns into a proxy QName.
    protected String proxifyQName (Datatype dt) {
        if (simpleTypes.contains(dt)) return replaceSuffix(dt.qname(), "Type", "SimpleType");
        else if (W3C_XML_SCHEMA_NS_URI.equals(dt.namespaceURI())) {
            var proxyPre = bc2pre.get("NIEM-XS");
            var proxyU = bc2U.get("NIEM-XS");
            refnsUS.add(proxyU);
            proxyUs.add(proxyU, dt.name());
            return proxyPre + ":" + dt.name();
        }
        else return dt.qname();
    }    
    
    // Write the proxy schema document for the specified proxy namespace.
    // Only include proxy types that are used in the model.
    // Ensure import of structures namespace has correct schemaLocation.
    @Override
    protected void writeProxyDocument(String vers, String pnsU, String rname, File outF) {
        XMLSchemaDocument sch;
        File resF;
        var xw   = new XSDWriter();
        try {
            resF = rmgr.getResourceFile(rname);
            sch = new XMLSchemaDocument(resF);
        } catch (ParserConfigurationException ex) {
            LOG.error("Parser configuration error: {}", ex.getMessage());
            return;
        } catch (SAXException ex) {
            LOG.error("Can't parse proxy file {}: {}", outF.toString(), ex.getMessage());
            return;
        } catch (IOException ex) {
            LOG.error("Can't read proxy file from resource {} file: {}", rname, ex.getMessage());
            return;
        }          
        // Compute relative path from niem-xs.xsd to structures.xsd
        var structU  = NamespaceKind.builtinNSU(vers, "STRUCTURES");
        var proxyU   = NamespaceKind.builtinNSU(vers, "NIEM-XS");
        var structFN = nsU2Path.get(structU);
        var proxyFN  = nsU2Path.get(proxyU);
        var structP  = Paths.get(structFN).normalize().toAbsolutePath();
        var proxyP   = Paths.get(proxyFN).normalize().toAbsolutePath();
        var proxyD   = proxyP.getParent();
        var relP     = proxyD.relativize(structP);
        var relS     = relP.toString().replace("\\", "/"); 
                    
        // Parse and modify the niem-xs.xsd resource to correct the import element
        // for structures namespace, and to remove unused proxy types.
        var dom  = sch.dom();
        var root = dom.getDocumentElement();
        var delS = new HashSet<Node>();
        var nS   = proxyUs.get(proxyU);
        var cnds = root.getChildNodes();
        for (int i = 0; i < cnds.getLength(); i++) {
            var node  = cnds.item(i);
            if (ELEMENT_NODE != node.getNodeType()) continue;
            var nodeN = node.getLocalName();
            var nodeE = (Element)node;
            
            switch (nodeN) {
            case "import":
                var insU = nodeE.getAttribute("namespace");
                if (insU.equals(structU)) {
                    nodeE.setAttribute("schemaLocation", relS);
                }
                break;
            case "complexType":
                var tname = nodeE.getAttribute("name");
                if (!nS.contains(tname)) delS.add(node);
                break;
            }
        }
        if (!delS.isEmpty()) {
            var ctaNS = versionToCtNsURI(vers);
            var cta   = root.getAttributeNS(ctaNS, "conformanceTargets");
            cta = cta.replace("#ReferenceSchemaDocument", "#SubsetSchemaDocument");
            root.setAttributeNS(ctaNS, "ct:conformanceTargets", cta);
        }
        for (var cn : delS) root.removeChild(cn);
        try {         
            xw.writeXML(dom, outF);
        } catch (IOException ex) {
            LOG.error("Can't write proxy document {}: {}", outF.toString(), ex.getMessage());
        }
    }
    
    
}
