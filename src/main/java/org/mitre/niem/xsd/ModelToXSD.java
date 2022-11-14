/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2021 The MITRE Corporation.
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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import static java.lang.Character.toLowerCase;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.createParentDirectories;
import org.apache.commons.io.FilenameUtils;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.NIEMConstants.CONFORMANCE_ATTRIBUTE_NAME;
import static org.mitre.niem.NIEMConstants.XSD_NS_URI;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.RestrictionOf;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.mitre.niem.NIEMConstants.XMLNS_URI;
import static org.mitre.niem.NIEMConstants.XML_NS_URI;
import org.mitre.niem.cmf.SchemaDocument;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_APPINFO;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_BUILTINS_COUNT;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_CONFORMANCE_TARGETS;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_PROXY;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_STRUCTURES;
import static org.mitre.niem.xsd.NIEMBuiltins.defaultBuiltinFilename;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinDefaultPrefix;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinDocumentFile;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinKind;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinURI;

/**
 * A class for writing a Model as a NIEM XML schema pile.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXSD {
    static final Logger LOG = LogManager.getLogger(ModelToXSD.class);
        
    private final Model m;
    private final Map<String,Namespace> builtinNSmap;           // map nsURI of a correct version built-in -> its Namespace object
    private final HashMap<ClassType,Property> augPoint;         // map of ClassType -> augmentation point Property
    private final HashMap<Property,ClassType> augElement;       // map of property -> ClassType with augmentation point it substitutes for
    private final ArrayList<AugmentRec> hasAugType;             // list of (Namespace, ClassType for which this namespace has an augmentation) pairs
    private final HashMap<String, List<Namespace>> subpropDeps; // map nsURI -> list of subproperty Namespace dependencies
    private final HashSet<ClassType> litTypes;                  // set of ClassType objects that will become complex type, simple content
    private final HashSet<Property> litProps;                   // set of literal properties not needed in XSD
    private final HashSet<Datatype> needSimpleType;             // Union, list, or non-empty restriction datatypes
    
    // These change as each namespace is processed
    private TreeMap<String,Element> nsPropdecls = null;   // map name -> schema declaration of attribute/element in a namespace
    private TreeMap<String,Element> nsTypedefs = null;    // map name -> schema definition of type in a namespace
    private HashSet<Namespace> nsNSdeps = null;           // Namespace objects of namespaces referenced in current namespace
    private String nsNIEMVersion = null;                  // NIEM version of current document (eg. "5.0")
    private Namespace nsProxyNS = null;                   // proxy namespace for NIEM version of current document
    private Namespace nsStructNS = null;                  // structures namespace for NIEM version of current document
    private Element root = null;                          // current document element

    
    public ModelToXSD (Model m) {
        this.m = m;
        builtinNSmap = new HashMap<>();
        augPoint     = new HashMap<>();
        augElement   = new HashMap<>();
        hasAugType   = new ArrayList<>();
        subpropDeps  = new HashMap<>();
        litTypes     = new HashSet<>();
        litProps     = new HashSet<>(); 
        needSimpleType = new HashSet<>();
    }

    public void writeXSD (File od) throws FileNotFoundException, ParserConfigurationException, TransformerException, IOException {
        // Remember augmentations in all namespaces
        // Create augmentation points for augmentable types, remember for later
        for (Component c : m.getComponentList()) {
            ClassType ct = c.asClassType();
            if (null == ct) continue;
            HashSet<Namespace> augNS = new HashSet<>();
            for (HasProperty hp : ct.hasPropertyList()) {
                if (null != hp.augmentElementNS()) augElement.put(hp.getProperty(), ct);
                for (Namespace ns : hp.augmentTypeNS()) {
                    if (augNS.contains(ns)) continue; 
                    AugmentRec augRec = new AugmentRec(ns, ct);
                    hasAugType.add(augRec);
                    augNS.add(ns);
                }
            }
            if (ct.isAugmentable()) {
                String apn = augmentationPointName(ct);
                Property augP = new Property(ct.getNamespace(), apn);
                augP.setIsAbstract(true);
                augP.setDefinition("An augmentation point for " + ct.getName() + ".");
                augPoint.put(ct, augP);            
            }
        }
        // Remember cross-namespace subproperties; need these for import dependencies
        for (Namespace ns : m.getNamespaceList()) { subpropDeps.put(ns.getNamespaceURI(), new ArrayList<>()); }
        for (Component c : m.getComponentList()) {
            Property subp = c.asProperty();
            if (null == subp) continue;
            if (null == subp.getSubPropertyOf()) continue;
            Namespace subpns = subp.getNamespace();                     // namespace of subp (which is a subproperty)
            Namespace parpns =  subp.getSubPropertyOf().getNamespace(); // subp is subproperty of property in this namespace
            if (subpns != parpns)
                subpropDeps.get(parpns.getNamespaceURI()).add(subpns);
        }
        // Find the ClassType objects that will become CSCs with a FooLiteral property
        // First property in the type is a simple element named FooLiteral
        // Either type has metadata, or >0 other properties that are all attributes
        for (Component c : m.getComponentList()) {
            ClassType ct = c.asClassType();
            if (null == ct) continue;
            if (!ct.canHaveMD() && ct.hasPropertyList().size() < 2) continue;
            var plist = ct.hasPropertyList();
            var prop  = plist.get(0).getProperty();
            boolean cscflag = (null != prop.getDatatype() && prop.getName().endsWith("Literal"));
            for (int i = 1; i < plist.size(); i++) {
                if (!plist.get(i).getProperty().isAttribute()) {
                    cscflag = false;
                    break;
                }
            }
            if (cscflag) litTypes.add(ct);
        }
        // Collect all Datatype objects for which we must create a simpleType declaration
        // Those are unions, or lists, or restrictions with facets
        for (Component c : m.getComponentList()) {
            Datatype dt = c.asDatatype();
            if (null == dt) continue;
            if (dt.getName().endsWith("SimpleType")) needSimpleType.add(dt);
            else if (null != dt.getUnionOf() || null != dt.getListOf()
                    || (null != dt.getRestrictionOf() && !dt.getRestrictionOf().getFacetList().isEmpty())) {
                needSimpleType.add(dt);
            }
        }
        // Collect all the NIEM versions, establish builtin namespace prefixes
        ArrayList<String> allVers = new ArrayList<>();
        m.schemadoc().forEach((ns,sd) -> {
            String nvers = sd.niemVersion();
            if (null != nvers && !allVers.contains(nvers)) allVers.add(nvers);
        });
        Collections.sort(allVers);
        Collections.reverse(allVers);   // highest versions first
        for (String nv : allVers) {
            for (int bkind = 0; bkind < NIEM_BUILTINS_COUNT; bkind++) {
                String bprefix = getBuiltinDefaultPrefix(bkind);
                String bnsuri  = getBuiltinURI(bkind, nv);
                if (null != m.namespaceMap().getURI(bnsuri)) continue;      // already has a prefix
                if (null == m.namespaceMap().getPrefix(bprefix)) {
                    m.namespaceMap().assignPrefix(bprefix, bnsuri);         // preferred prefix is available
                }
                else {
                    String vsuf = nv.replaceAll(":","");
                    m.namespaceMap().assignPrefix(bprefix+vsuf, bnsuri);
                }
            }
        }
        // Now write a schema document for each namespace
        for (Namespace ns : m.getNamespaceList()) {
            if (XSD_NS_URI.equals(ns.getNamespaceURI())) continue;
            if (XML_NS_URI.equals(ns.getNamespaceURI())) continue;
            if (ns.isExternal()) continue;
            String dfp = m.filePath(ns.getNamespaceURI());
            File of = new File(od, dfp);
            createParentDirectories(of);
            FileWriter ofw = new FileWriter(of);
            writeDocument(ns.getNamespaceURI(), ofw);
            ofw.close();
        }
        // Next, copy the needed builtin schema documents to the destination
        for (String uri : builtinNSmap.keySet()) {
            String dfp = m.filePath(uri);
            if (null == dfp) {
                int which = getBuiltinKind(uri);
                dfp = defaultBuiltinFilename(which);
            }
            File df = new File(od, dfp);
            File sf = getBuiltinDocumentFile(uri);
            createParentDirectories(df);
            copyFile(sf, df);
        }
        // Finally, copy the XML namespace schema if needed
        SchemaDocument xmlsd = m.schemadoc().get(XML_NS_URI);
        String fp = m.filePath(XML_NS_URI);
        if (null != xmlsd) {
            File sf;
            String sfp;
            String appdir = NIEMBuiltins.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            if (appdir.endsWith(".jar")) sfp = FilenameUtils.concat(appdir, "../../share/xsd");
            else sfp = FilenameUtils.concat(appdir, "../../../../src/main/dist/share/xsd"); 
            sfp = FilenameUtils.concat(sfp, "xml.xsd");
            sf = new File(sfp);
            if (null == fp) fp = "xml.xsd";
            File df = new File(od, fp);   
            createParentDirectories(df);
            copyFile(sf, df);
        }
    }
    
    // Write the schema document for the specified namespace
    private void writeDocument (String nsuri, Writer ofw) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        root = dom.createElementNS(XSD_NS_URI, "xs:schema");
        root.setAttribute("targetNamespace", nsuri);
        dom.appendChild(root);
        
        nsNIEMVersion = m.niemVersion(nsuri);
        nsNSdeps    = new HashSet<>();
        nsPropdecls = new TreeMap<>();
        nsTypedefs  = new TreeMap<>();
        nsProxyNS   = getBuiltinNamespace(NIEM_PROXY, nsNIEMVersion);
        nsStructNS  = getBuiltinNamespace(NIEM_STRUCTURES, nsNIEMVersion);      
        nsNSdeps.addAll(subpropDeps.get(nsuri));
        nsNSdeps.add(nsStructNS);
        
        LOG.debug("Writing schema document for {}", nsuri);
        
        // Add namespace version number and language attribute, if specified
        String lang = m.schemaLanguage(nsuri);
        String nsv  = m.schemaVersion(nsuri);
        if (null != lang && !lang.isBlank()) root.setAttributeNS(XML_NS_URI, "lang", lang);
        if (null != nsv && !nsv.isBlank())   root.setAttribute("version", nsv);
        
        // Add conformance target assertions, if any
        String cta = m.conformanceTargets(nsuri);
        if (null != cta) {
            String ctns = getBuiltinURI(NIEM_CONFORMANCE_TARGETS, nsNIEMVersion);
            String ctprefix;
            ctprefix = m.namespaceMap().getPrefix(ctns);
            root.setAttributeNS(XMLNS_URI, "xmlns:"+ctprefix, ctns);
            root.setAttributeNS(ctns, ctprefix + ":" + CONFORMANCE_ATTRIBUTE_NAME, cta);
        }
        // Add the <xs:annotation> element with namespace definition
        Namespace ns = m.getNamespaceByURI(nsuri);
        addDefinition(dom, root, ns.getDefinition());
        nsNSdeps.add(ns);       // always define prefix for target namespace
        
        // Create type definitions for ClassType objects
        // Then create typedefs for Datatype objects (when not already created)
        // Remember external namespaces when encountered
        for (Component c : m.getComponentList()) createComplexTypeFromClass(dom, nsuri, c.asClassType());
        for (Component c : m.getComponentList()) createComplexTypeFromDatatype(dom, nsuri, c.asDatatype());
        for (Datatype dt : needSimpleType)       createSimpleTypeFromDatatype(dom, nsuri, dt);
        
        // Create elements and attributes for Property objects
        for (Component c : m.getComponentList()) createDeclaration(dom, nsuri, c.asProperty());
        
        // Generate augmentation types and elements for this namespace
        for (AugmentRec ar : hasAugType) {
            if (ar.nsWithAug != ns) continue;
            ClassType targCT  = ar.augmentedType();
            String targCTName = targCT.getName();
            String targName   = targCTName.substring(0, targCTName.length()-4);
            String augElName  = targName + "Augmentation";
            String augTpName  = augElName + "Type";
            
            // Create the augmentation type
            ClassType augCT = new ClassType(ns, augTpName);
            for (HasProperty hp : targCT.hasPropertyList()) augCT.addHasProperty(hp);
            augCT.setDefinition("A data type for additional information about a " + targName);
            createComplexTypeFromClass(dom, nsuri, augCT);
            
            // Create the augmentation element
            Property augP = new Property(ns, augElName);
            augP.setDefinition("Additional information about a " + targName);
            augP.setClassType(augCT);
            augP.setSubPropertyOf(augPoint.get(targCT));
            createDeclaration(dom, nsuri, augP);
        }
        // Add a namespace declaration and import element for each namespace dependency
        List<Namespace> orderedDeps = new ArrayList<>();
        orderedDeps.addAll(nsNSdeps);
        Collections.sort(orderedDeps, 
                Comparator.comparing(Namespace::getKind)
                .thenComparing(Namespace::getNamespaceURI));
        for (Namespace dns : orderedDeps) {
            String dnspre = dns.getNamespacePrefix();
            String dnsuri = dns.getNamespaceURI();
            root.setAttributeNS(XMLNS_URI, "xmlns:" + dnspre, dnsuri);
            if (XSD_NS_URI.equals(dnsuri)) continue;   // don't import XSD
            if (nsuri.equals(dnsuri)) continue;        // don't import current namespace
            Path thisDoc   = Paths.get(m.filePath(nsuri));
            Path importDoc = Paths.get(m.filePath(dnsuri));
            Path toImportDoc;
            if (null != thisDoc.getParent()) toImportDoc = thisDoc.getParent().relativize(importDoc);
            else toImportDoc = importDoc;
            String sloc = separatorsToUnix(toImportDoc.toString());
            Element ie = dom.createElementNS(XSD_NS_URI, "xs:import");
            ie.setAttribute("namespace", dnsuri);
            ie.setAttribute("schemaLocation", sloc);
            if (dns.isExternal()) addAppinfo(dom, ie, nsuri, "externalImportIndicator", "true");
            root.appendChild(ie);
        }
        // Now add the type definitions and element/attribute declarations to the document
        nsTypedefs.forEach((name,element) -> {
            root.appendChild(element);
        });
        nsPropdecls.forEach((name,element) -> {
            root.appendChild(element);
        });
        writeDom(dom, ofw);
    }
    
    // Add @appinfo:appatt="value" to an element.  Get the right namespace prefix and URI
    // for the current document.  Add the namespace declaration for appinfo, but
    // don't import it.
    private void addAppinfo (Document dom, Element e, String nsuri, String appatt, String value) {
        String niemVersion = m.niemVersion(nsuri);
        String appinfoNS = getBuiltinURI(NIEM_APPINFO, niemVersion);
        String appinfoPR = m.namespaceMap().getPrefix(appinfoNS);
        String appinfoQName = appinfoPR + ":" + appatt;
        root.setAttributeNS(XMLNS_URI, "xmlns:"+appinfoPR, appinfoNS);
        e.setAttributeNS(appinfoNS, appinfoQName, value);
    }
    
    // Create a complex type declaration from a ClassType object
    // This can have simpleContent if the ClassType has a literal property
    // Otherwise it has complexContent
    private void createComplexTypeFromClass (Document dom, String nsuri, ClassType ct) { 
        if (null == ct) return;
        String cname = ct.getName();
        if (nsTypedefs.containsKey(cname)) return;              // already created
        if (!nsuri.equals(ct.getNamespaceURI())) return;        // different namespace
        Element cte = dom.createElementNS(XSD_NS_URI, "xs:complexType");
        Element exe = dom.createElementNS(XSD_NS_URI, "xs:extension");
        cte.setAttribute("name", cname);
        addDefinition(dom, cte, ct.getDefinition());
        if (ct.canHaveMD())    addAppinfo(dom, cte, nsuri, "metadataIndicator", "true");
        if (ct.isDeprecated()) addAppinfo(dom, cte, nsuri, "deprecated", "true");
        if (ct.isExternal())   addAppinfo(dom, cte, nsuri, "externalAdapterTypeIndicator", "true");
        nsTypedefs.put(cname, cte);
        
        // Create simple content for class with FooLiteral property
        if (litTypes.contains(ct)) {
            var sce   = dom.createElementNS(XSD_NS_URI, "xs:simpleContent");
            var plist = ct.hasPropertyList();
            var lp    = plist.get(0).getProperty();     // FooLiteral property
            var lpt   = lp.getDatatype();               // datatype for FooLiteral
            var lptqn = proxifiedDatatypeQName(lpt);
            if (needSimpleType.contains(lpt)) {
                lptqn = lptqn.replaceFirst("Type$", "SimpleType");
                lptqn = lptqn.replaceFirst("Datatype$", "SimpleType");
            }
            exe.setAttribute("base", lptqn);
            if (lptqn.endsWith("SimpleType")) {         // need simpleObjectAttributeGroup
                var spr  = nsStructNS.getNamespacePrefix();
                var agqn = spr + ":SimpleObjectAttributeGroup";
                var age = dom.createElementNS(XSD_NS_URI, "xs:attributeGroup");
                age.setAttribute("ref", agqn);
                exe.appendChild(age);                
            }
            litProps.add(lp);                   // remember we don't need an element for this
            plist.remove(0);                    // leaving the attribute properties for later
            cte.appendChild(sce);               // xs:complexType has xs:simpleContent
            sce.appendChild(exe);               // xs:simpleContent has xs:extension
        }
        // Otherwise create complex content with sequence of element refs
        else {
            boolean isAugType = false;
            var cce    = dom.createElementNS(XSD_NS_URI, "xs:complexContent");
            var sqe    = dom.createElementNS(XSD_NS_URI, "xs:sequence");
            var basect = ct.getExtensionOfClass();
            if (null == basect) {
                String spr = nsStructNS.getNamespacePrefix();
                if (cname.endsWith("AssociationType"))        exe.setAttribute("base", spr + ":AssociationType");
                else if (cname.endsWith("MetadataType"))      exe.setAttribute("base", spr + ":MetadataType");
                else if (!cname.endsWith("AugmentationType")) exe.setAttribute("base", spr + ":ObjectType");
                else {
                    exe.setAttribute("base", spr + ":AugmentationType");
                    isAugType = true;
                }
            } 
            else {
                exe.setAttribute("base", basect.getQName());
                nsNSdeps.add(basect.getNamespace());
            }
            // Add element refs for element properties
            for (HasProperty hp : ct.hasPropertyList()) {
                // always skip augmentation elements (directly substituted for augmentation point
                // skip augmentation properties when building the augmented type
                // skip properties not added by this namespace when building an augmentation type
                // add other non-attribute properties as element reference
                if (null != hp.augmentElementNS()) continue; 
                if (!isAugType && !hp.augmentTypeNS().isEmpty()) continue;
                if (isAugType && !hp.augmentTypeNS().contains(ct.getNamespace())) continue;
                addElementRef(dom, sqe, hp);
                nsNSdeps.add(hp.getProperty().getNamespace());
            }
            // Add an augmentation point?
            if (ct.isAugmentable()) {
                var ap  = augPoint.get(ct);     // we created the augmentation Property in advance
                var ahp = new HasProperty();    // now build a HasProperty object for addElementRef
                ahp.setProperty(ap);
                ahp.setMinOccurs(0);
                ahp.setMaxUnbounded(true);
                addElementRef(dom, sqe, ahp);
                createDeclaration(dom, ct.getNamespaceURI(), ap);
            }
            cte.appendChild(cce);   // xs:complexType has xs:complexContent
            cce.appendChild(exe);   // xs:complexContent has xs:extension
            exe.appendChild(sqe);   // xs:extension has xs:sequence
        }
        // Now add attribute properties in hasProperty list to the xs:extension element
        // Do this for simple content or complex content
        for (HasProperty hp : ct.hasPropertyList()) {
            Property p = hp.getProperty();
            if (!p.isAttribute()) continue;
            var ate = dom.createElementNS(XSD_NS_URI, "xs:attribute");
            ate.setAttribute("ref", p.getQName());
            if (0 != hp.minOccurs()) ate.setAttribute("use", "required");
            exe.appendChild(ate);
            nsNSdeps.add(p.getNamespace());
        }
    }
    
    // Create <xs:element ref="foo">, append it to some <xs:sequence>
    private void addElementRef (Document dom, Element sqe, HasProperty hp) {
        if (hp.getProperty().isAttribute()) return;
        var hpe = dom.createElementNS(XSD_NS_URI, "xs:element");
        if (1 != hp.minOccurs()) hpe.setAttribute("minOccurs", "" + hp.minOccurs());
        if (hp.maxUnbounded())   hpe.setAttribute("maxOccurs", "unbounded");
        else if (1 != hp.maxOccurs()) hpe.setAttribute("maxOccurs", "" + hp.maxOccurs());
        hpe.setAttribute("ref", hp.getProperty().getQName());
        sqe.appendChild(hpe);
    }
    
    // Create a complex type declaration from a Datatype object (FooType)
    private void createComplexTypeFromDatatype (Document dom, String nsuri, Datatype dt) {
        if (null == dt) return;
//        if (needSimpleType.contains(dt)) return;                    // create xs:simpleType instead
        var cname = dt.getName().replaceFirst("Datatype$", "SimpleType");
        var bname = cname.replaceFirst("SimpleType$", "Type");
        if (nsTypedefs.containsKey(cname)) return;                  // already created
        if (nsTypedefs.containsKey(bname)) return;                  // already have FooType complex type
        if (!nsuri.equals(dt.getNamespaceURI())) return;            // not in this namespace
        if (XSD_NS_URI.equals(dt.getNamespaceURI())) return;        // don't create XSD builtins
        
        var cte = dom.createElementNS(XSD_NS_URI, "xs:complexType");
        var sce = dom.createElementNS(XSD_NS_URI, "xs:simpleContent");
        var exe = dom.createElementNS(XSD_NS_URI, "xs:extension");
        cte.setAttribute("name", cname);        
        addDefinition(dom, cte, dt.getDefinition());
        if (dt.isDeprecated()) addAppinfo(dom, cte, nsuri, "deprecated", "true");

        // Restriction without facets becomes an empty xs:extension
        // Replace XSD primitive with proxy type
        String  btqn = null;
        RestrictionOf r = dt.getRestrictionOf();
        if (null != r && r.getFacetList().isEmpty()) {
            var bdt  = r.getDatatype();
            btqn = proxifiedDatatypeQName(bdt);
            if (needSimpleType.contains(bdt)) {
                btqn = btqn.replaceFirst("Type$", "SimpleType");
                btqn = btqn.replaceFirst("Datatype$", "SimpleType");  
            }
        }
        // For union, list, restriction with facets, extension base is FooSimpleType
        else {
            btqn = dt.getQName().replaceFirst("Type$", "SimpleType");
            btqn = btqn.replaceFirst("Datatype$", "SimpleType");
        }
        
        // If extension base is a FooSimpleType, we must add SimpleObjectAttributeGroup
        if (btqn.endsWith("SimpleType")) {
            var spr  = nsStructNS.getNamespacePrefix();
            var agqn = spr + ":SimpleObjectAttributeGroup";
            var age = dom.createElementNS(XSD_NS_URI, "xs:attributeGroup");
            age.setAttribute("ref", agqn);
            exe.appendChild(age);
        }
        exe.setAttribute("base", btqn);
        sce.appendChild(exe);   // xs:simpleContent has xs:extension
        cte.appendChild(sce);   // xs:complexType has xs:simpleContent
        nsTypedefs.put(cname, cte);
    } 
    
    // Create a simple type declaration from a Datatype object
    private void createSimpleTypeFromDatatype (Document dom, String nsuri, Datatype dt) {
        if (null == dt) return;
        var cname = dt.getName();
        if (cname.endsWith("Datatype")) 
            cname = cname.substring(0,cname.length()-8) + "SimpleType";
        else cname = cname.replaceFirst("Type$", "SimpleType");
        if (!nsuri.equals(dt.getNamespaceURI())) return;    // not in this namespace
        if (nsTypedefs.containsKey(cname)) return;   // already created

        var ste   = dom.createElementNS(XSD_NS_URI, "xs:simpleType");
        ste.setAttribute("name", cname);
        addDefinition(dom, ste, dt.getDefinition());
        if (null != dt.getUnionOf()) addUnionElement(dom, ste, dt);
        else if (null != dt.getListOf()) addListElement(dom, ste, dt);
        else if (null != dt.getRestrictionOf()) addRestrictionElement(dom, ste, dt);
        nsTypedefs.put(cname, ste);   
        nsNSdeps.add(dt.getNamespace());        
    }
    
    private void addUnionElement (Document dom, Element ste, Datatype bdt) {
        Element une = dom.createElementNS(XSD_NS_URI, "xs:union");
        StringBuilder members = new StringBuilder();
        String sep = "";
        for (Datatype udt : bdt.getUnionOf().getDatatypeList()) {
            String memberQN = maybeSimpleTypeQName(udt);
            members.append(sep).append(memberQN);
            sep = " ";
        }
        une.setAttribute("memberTypes", members.toString());
        ste.appendChild(une);        
    }
    
    private void addListElement (Document dom, Element ste, Datatype bdt) {
        Element lse = dom.createElementNS(XSD_NS_URI, "xs:list");
        lse.setAttribute("itemType", maybeSimpleTypeQName(bdt.getListOf()));
        ste.appendChild(lse);
    }
    
    private void addRestrictionElement (Document dom, Element ste, Datatype bdt) {
        RestrictionOf r = bdt.getRestrictionOf();
        Element rse = dom.createElementNS(XSD_NS_URI, "xs:restriction");
        rse.setAttribute("base", maybeSimpleTypeQName(r.getDatatype()));
        for (Facet f : r.getFacetList()) {
            String fk = f.getFacetKind();
            String ename = "xs:" + toLowerCase(fk.charAt(0)) + fk.substring(1);
            Element fce = dom.createElementNS(XSD_NS_URI, ename);
            fce.setAttribute("value", f.getStringVal());
            addDefinition(dom, fce, f.getDefinition());
            rse.appendChild(fce);
        }
        ste.appendChild(rse);
    }
    
    // Returns QName for FooSimpleType if that type exists, otherwise QName for FooType
    private String maybeSimpleTypeQName (Datatype dt) {
        String dtqn   = dt.getQName();
        String dtbase = dtqn.replaceFirst("Type$", "");
        String dtsqn  = dtbase + "SimpleType";
        if (needSimpleType.contains(dt)) return dtsqn;
        return dtqn;
    }
    
    // Convert "xs:foo" to "xs-proxy:foo"
    private String proxifiedDatatypeQName (Datatype dt) {
        String dtqn = dt.getQName();
        if (XSD_NS_URI.equals(dt.getNamespaceURI())) {
            dtqn = nsProxyNS.getNamespacePrefix() + ":" + dt.getName();
            nsNSdeps.add(nsProxyNS);
        }
        return dtqn;
    }
    
    // Create an element or attribute declaration from a Property object
    private void createDeclaration(Document dom, String nsuri, Property p) {
        if (null == p) return;
        if (litProps.contains(p)) return;
        boolean isAttribute = p.isAttribute();
        ClassType pct = p.getClassType();
        Datatype pdt  = p.getDatatype();
        Element pe;
        if (!nsuri.equals(p.getNamespaceURI())) return;   
        LOG.debug("Creating {} for {}", (p.isAttribute() ? "attribute" : "element"), p.getQName());
        if (isAttribute) pe = dom.createElementNS(XSD_NS_URI, "xs:attribute");
        else pe = dom.createElementNS(XSD_NS_URI, "xs:element");
        addDefinition(dom, pe, p.getDefinition());
        pe.setAttribute("name", p.getName());
        if (p.isReferenceable()) pe.setAttribute("nillable", "true");
        if (p.isAbstract())      pe.setAttribute("abstract", "true");
        if (p.isDeprecated())    addAppinfo(dom, pe, nsuri, "deprecated", "true");
        if (null != pct) {
            pe.setAttribute("type", pct.getQName());
            nsNSdeps.add(pct.getNamespace());
        }
        // Attribute declarations can use XSD types
        // Replace x:FooDatatype and x:FooType with x:FooSimpleType
        if (isAttribute && null != pdt) {
            var pdtQN = pdt.getQName();
            pdtQN = pdtQN.replaceFirst("Type$", "SimpleType");
            pdtQN = pdtQN.replaceFirst("Datatype$", "SimpleType");            
            pe.setAttribute("type", pdtQN);
            nsNSdeps.add(pdt.getNamespace());
        }
        // Element declarations use proxy types instead of XSD types
        else if (!isAttribute && null != pdt) {
            var pdtQN = proxifiedDatatypeQName(pdt);
            var pdtNS = pdt.getNamespace();
            pe.setAttribute("type", pdtQN);
            nsNSdeps.add(pdtNS);
        }
        if (null != p.getSubPropertyOf()) {
            pe.setAttribute("substitutionGroup", p.getSubPropertyOf().getQName());
            nsNSdeps.add(p.getSubPropertyOf().getNamespace());
        }
        // Handle ordinary element substituting for augmentation point
        else if (augElement.containsKey(p)) {
            ClassType augType   = augElement.get(p);
            Namespace augTypeNS = augType.getNamespace();
            String augPQN = augmentationPointQName(augType);
            pe.setAttribute("substitutionGroup", augPQN);
            nsNSdeps.add(augTypeNS);
        }
        nsPropdecls.put(p.getName(), pe);
    }
    
    private void addDefinition (Document dom, Element e, String def) {
        if (null == def || def.isBlank()) return;
        Element ae = dom.createElementNS(XSD_NS_URI, "xs:annotation");
        Element de = dom.createElementNS(XSD_NS_URI, "xs:documentation");
        de.setTextContent(def);
        ae.appendChild(de);
        e.appendChild(ae);
    }

    // debug tool
    private void domToString (Document dom) {
        nsTypedefs.forEach((name,element) -> {
            root.appendChild(element);
        });
        nsPropdecls.forEach((name,element) -> {
            root.appendChild(element);
        });        
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter ostr = new StringWriter();
            tr.transform(new DOMSource(dom), new StreamResult(ostr));
            String buf = ostr.toString();
            int i = 0;
        } catch (TransformerException ex) {
            java.util.logging.Logger.getLogger(ModelToXSD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // Writes the XSD document model.  Post-processing of XSLT output to do
    // what XSLT should do, but doesn't.  You can't process arbitrary XML in
    // this way, but we know what the XSLT output is going to be, so it works.
    private void writeDom (Document dom, Writer w) throws TransformerConfigurationException, TransformerException, IOException {
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter ostr = new StringWriter();
        tr.transform(new DOMSource(dom), new StreamResult(ostr));
        
        // process string by lines to do what XSLT won't do :-(
        // For <xs:schema>, namespace decls and attributes on separate indented lines.
        // For <xs:element>, order as @ref, @minOccurs, @maxOccurs, @name, @type, @substitutionGroup, then others
        // For <xs:import>, order as @namespace, @schemaLocation, then others
        Pattern linePat = Pattern.compile("^(\\s*)<([^\\s>]+)(.*)");
        String[][]reorder = {
                { "xs:element", "name", "ref", "type", "minOccurs", "maxOccurs", "substitutionGroup" },
                { "xs:import", "namespace", "schemaLocation" },
                { "xs:complexType", "name", "type" },
                { "xs:attribute", "name", "type" }
        };
        Scanner scn = new Scanner(ostr.toString());
        String line = scn.nextLine();
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");    // don't want/need standalone="no"
        while (scn.hasNextLine()) {           
            line = scn.nextLine();
            Matcher lineM = linePat.matcher(line);
//            System.out.print(String.format("line:   '%s'\n", line));
            if (!lineM.matches()) w.write(line);
            else {
                String indent = lineM.group(1);
                String tag = lineM.group(2);
                String res = lineM.group(3);
                String end;
                res = res.stripTrailing();
                if (res.endsWith("/>")) end = "/>";
                else end = ">";
                res = res.substring(0, res.length() - end.length());
//                System.out.print(String.format("indent: '%s'\n", indent));
//                System.out.print(String.format("tag:    '%s'\n", tag));
//                System.out.print(String.format("rest:   '%s'\n", res));
//                System.out.print(String.format("end:    '%s'\n", end));
                for (int i = 0; i < reorder.length; i++) {
                    if (tag.equals(reorder[i][0])) {
                        w.write(indent);
                        w.write("<" + tag);
                        Map<String,String>tmap = keyValMap(res);
                        for (int j = 1; j < reorder[i].length; j++) {
                            String key = reorder[i][j];
                            if (null != tmap.get(key)) {
                                w.write(" " + tmap.get(key));
                                tmap.remove(key);
                            }
                        }
                        for (String str : tmap.values()) w.write(" " + str);
                        w.write(end);
                        line = null;
                        i = reorder.length;
                    }
                }
                if (null != line && "xs:schema".equals(tag)) {
                    w.write("<xs:schema");
                    Map<String,String>tmap = keyValMap(res);
                    if (null != tmap.get("targetNamespace")) {
                        w.write("\n  " + tmap.get("targetNamespace"));
                        tmap.remove("targetNamespace");
                    }
                    for (Map.Entry<String,String>me : tmap.entrySet()) {
                        if (me.getKey().startsWith("xmlns:")) w.write("\n  " + me.getValue());
                    }
                    for (Map.Entry<String,String>me : tmap.entrySet()) {
                        if (!me.getKey().startsWith("xmlns:")) w.write("\n  " + me.getValue());
                    }     
                    w.write(end);
                    line = null;
                }
                if (null != line) w.write(line);
            }
            w.write("\n");
        }        
    }
    
    // Breaks a string of key="value" pairs into a sorted map
    private TreeMap<String,String> keyValMap (String s) {
        TreeMap<String,String> kvm = new TreeMap<>();
        String[] tok = s.split("\\s+");
        for (String tokl : tok) {
            int ei = tokl.indexOf("=");
            if (ei >= 0) {
                String key = tokl.substring(0, ei);
                kvm.put(key, tokl);
            }
        }
        return kvm;
    }
    
    // Returns a Namespace object with the right prefix and URI for the
    // specified builtin and NIEM version.  Mungs the prefix if necessary.
    // Creates a Namespace object if necessary -- but doesn't add it to the
    // model.
    private Namespace getBuiltinNamespace (int kind, String niemVersion) {
        String bnsuri = getBuiltinURI(kind, niemVersion);  // eg. gimme the URI for structures in NIEM 4.0
        Namespace bns = builtinNSmap.get(bnsuri);                   // do we already have a Namespace for that
        if (null != bns) return bns;                                // if so, return it
        String bprefix = m.namespaceMap().getPrefix(bnsuri);        // get prefix; munged if necessary to make unique
        bns = new Namespace(bprefix, bnsuri);                       // create Namespace object for this built-in
        builtinNSmap.put(bnsuri, bns);                              // remember it if we need it again
        return bns;                                                 // and return the Namespace; ta da!
    }
    
    // Returns the name of the augmentation point element for a ClassType
    private static String augmentationPointName (ClassType ct) {
        String qn = ct.getName();
        String rv = qn.substring(0, qn.length()-4) + "AugmentationPoint";
        return rv;        
    }
    
    // Returns the name of the augmentation point element for a ClassType
    private static String augmentationPointQName (ClassType ct) {
        return ct.getNamespace().getNamespacePrefix() + ":" + augmentationPointName(ct);
    }
    
    private record AugmentRec (
        Namespace nsWithAug,            // Namespace with augmentation element
        ClassType augmentedType) {};    // augmented ClassType
}

