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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.Set;
import java.util.TreeMap;
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
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.createParentDirectories;
import org.apache.commons.io.FilenameUtils;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.NIEMConstants.CONFORMANCE_ATTRIBUTE_NAME;
import org.mitre.niem.cmf.AugmentRecord;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.CodeListBinding;
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
import org.mitre.niem.cmf.NamespaceKind;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_APPINFO;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CLI;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CLSA;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CTAS;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_NOTUTILITY;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_PROXY;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_STRUCTURES;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_UTILITY_COUNT;


/**
 * A class for writing a Model as a NIEM XML schema pile.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public abstract class ModelToXSD {
    static final Logger LOG = LogManager.getLogger(ModelToXSD.class);
        
    protected Model m = null;
    protected final Map<String, String> ns2file;              // map nsURI -> absolute schema document file path
    protected final Set<String> nsfiles;                      // set of schema document file paths
    protected final Set<String> utilityNSuri;                 // set of utility namespaces needed in schema document pile
    protected final Map<String, List<String>> subpropDeps;    // map nsURI -> list of subproperty Namespace dependency URIs
    protected final Set<ClassType> litTypes;                  // set of ClassType objects that will become complex type, simple content
    protected final Set<Property> litProps;                   // set of literal properties not needed in XSD
    protected final Set<Datatype> needSimpleType;             // Union, list, or non-empty restriction datatypes
    
    // These change as each namespace is processed
    protected Map<String,Element> nsPropdecls = null;         // map name -> schema declaration of attribute/element in a namespace
    protected Map<String,Element> nsTypedefs = null;          // map name -> schema definition of type in a namespace
    protected Set<String> nsNSdeps = null;                    // Namespace URIs of namespaces referenced in current namespace
    protected String nsNIEMVersion = null;                    // NIEM version of current document (eg. "5.0")
    protected String appinfoPrefix = null;                    // appinfo namespace prefix for NIEM versin of current document
    protected String clsaPrefix = null;                       // clsa namespace prefix
    protected String proxyPrefix = null;                      // proxy namespace prefix 
    protected String structPrefix = null;                     // structures namespace prefix
    protected String appinfoURI = null;                       // appinfo namespace URI for NIEM versin of current document
    protected String clsaURI = null;                          // clsa namespace
    protected String proxyURI = null;                         // proxy namespace URI
    protected String structURI = null;                        // structures namespace URI


    protected Element root = null;                            // current document element

    
    public ModelToXSD () {
        ns2file        = new HashMap<>();
        nsfiles        = new HashSet<>();
        utilityNSuri   = new HashSet<>();
        subpropDeps    = new HashMap<>();
        litTypes       = new HashSet<>();
        litProps       = new HashSet<>(); 
        needSimpleType = new HashSet<>();
    }
    
    public ModelToXSD (Model m) {
        this();
        this.m = m;
    }
    
    public void setModel (Model m) { this.m = m; }

    // Write the Model to an XML Schema pile in directory "od".
    // Target directory should be empty.  If it isn't, you'll get munged
    // file names whenever the preferred file name already exists.
    public void writeXSD (File od) throws FileNotFoundException, ParserConfigurationException, TransformerException, IOException {
         // Remember cross-namespace subproperties; need these for import dependencies
        for (Namespace ns : m.getNamespaceList()) { subpropDeps.put(ns.getNamespaceURI(), new ArrayList<>()); }
        for (Component c : m.getComponentList()) {
            Property subp = c.asProperty();
            if (null == subp) continue;
            if (null == subp.getSubPropertyOf()) continue;
            Namespace subpns = subp.getNamespace();                     // namespace of subp (which is a subproperty)
            Namespace parpns =  subp.getSubPropertyOf().getNamespace(); // subp is subproperty of property in this namespace
            if (subpns != parpns)
                subpropDeps.get(parpns.getNamespaceURI()).add(subpns.getNamespaceURI());
        }
        // Find the ClassType objects that will become CSCs with a FooLiteral property
        // First property in the type is a simple element named FooLiteral
        // Either type has metadata, or >0 other properties that are all attributes
        for (Component c : m.getComponentList()) {
            ClassType ct = c.asClassType();
            if (null == ct) continue;
            if (ct.hasPropertyList().size() < 2) continue;
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
            if (null != nvers && !nvers.isBlank() && !allVers.contains(nvers)) allVers.add(nvers);
        });
        if (allVers.isEmpty()) allVers.add("5");
        Collections.sort(allVers);
        Collections.reverse(allVers);   // highest versions first
        var arch = getArchitecture();
        for (String nv : allVers) {
            for (int uk = 0; uk < NIEM_UTILITY_COUNT; uk++) {
                if (NIEM_NOTUTILITY == uk) continue;
                String nsuri   = NamespaceKind.getBuiltinNS(uk, arch, nv);
                String uprefix;
                if (null == nsuri) continue;
                var sdoc = m.schemadoc().get(nsuri);
                if (null != sdoc) uprefix = sdoc.targetPrefix();        // try prefix from schema document
                else uprefix = NamespaceKind.namespaceUtil2Builtin(uk);    // use default prefix for this utility         
                m.namespaceMap().assignPrefix(uprefix, nsuri);            
            }
        }
        // Generate augmentation points for all augmentable ClassTypes in the model.
        generateAugmentationPoints();
        
        // Make sure we have a new file with a unique name for all the schema documents
        // Schema document file paths can be suggested in the Model, otherwise default
        // paths will be created.  All this hinting and defaulting is not guaranteed 
        // to produce a unique file path for each namespace, so we ensure that here,
        // beginning with the model namespaces.
        for (Namespace ns : m.getNamespaceList()) {
            if (W3C_XML_SCHEMA_NS_URI.equals(ns.getNamespaceURI())) continue;   // no document for xs: namespace
            if (ns.isExternal()) continue;                              // someone else's job
            var nsuri = ns.getNamespaceURI();
            var sdoc  = m.schemadoc().get(nsuri);
            String fp = null;
            if (null != sdoc) fp = sdoc.filePath();                     // use hint from Model
            var absfp = genSchemaFile(nsuri, od, fp);
        }
        // Now generate new unique file names for the utility schema documents.
        for (var vers : allVers) {
            for (int util = 0; util < NIEM_NOTUTILITY; util++) {
                if (NIEM_CLI == util) continue;
                var nsuri = NamespaceKind.getBuiltinNS(util, arch, vers);
                if (null == nsuri) continue;
                var sdoc = m.schemadoc().get(nsuri);
                String fp = null;
                if (null != sdoc) fp = sdoc.filePath();
                var absfp = genSchemaFile(nsuri, od, fp);
            }
        }
        // Now write the schema document for each namespace
        for (Namespace ns : m.getNamespaceList()) {
            if (W3C_XML_SCHEMA_NS_URI.equals(ns.getNamespaceURI())) continue;
            if (XML_NS_URI.equals(ns.getNamespaceURI())) continue;  // copy it later
            if (ns.isExternal()) continue; // FIXME
            var nsuri = ns.getNamespaceURI();
            var ofp    = ns2file.get(nsuri);
            var of     = new File(ofp);
            createParentDirectories(of);
            FileWriter ofw = new FileWriter(of);
            writeDocument(ns.getNamespaceURI(), ofw);
            ofw.close();
        }
        // Next, copy the needed builtin schema documents to the destination
        for (String uri : utilityNSuri) {
            var ofp = ns2file.get(uri);
            var df  = new File(ofp);
            var sf = getShareSchemaFile(uri);
            createParentDirectories(df);
            if (NIEM_PROXY == NamespaceKind.builtin(uri)) writeProxyDocument(uri, sf, df);
            else copyFile(sf, df);
        }
        // Finally, copy the XML namespace schema if needed
        if (null != m.getNamespaceByURI(XML_NS_URI)) {
            var ofp = ns2file.get(XML_NS_URI);
            var df  = new File(ofp);
            var sf = getShareSchemaFile(XML_NS_URI);
            createParentDirectories(df);
            copyFile(sf, df);            
        }
    }
    
    // The proxy schema imports structures, and we don't know where the structures
    // schema document will be before runtime, so we can't just copy a file like
    // we do for the other builtins.  There's only one import element, and we know
    // what it looks like, so we just hack it.
    protected static final Pattern importPat = Pattern.compile("<xs:import\\s");
    protected void writeProxyDocument (String proxyURI, File sf, File df) throws IOException {
        var arch      = getArchitecture();
        var nver      = NamespaceKind.version(proxyURI);
        var structURI = NamespaceKind.getBuiltinNS(NIEM_STRUCTURES, arch, nver);
        var structFN  = ns2file.get(structURI);
        var structP   = Paths.get(structFN);       
        var dp        = df.toPath();
        String structSL;
        Path relP;
        if (null == dp.getParent()) relP = structP;
        else relP = dp.getParent().relativize(structP);
        structSL = separatorsToUnix(relP.toString());
        
        var dfr       = new FileWriter(df);
        var sfr       = new FileReader(sf);
        var sfbr      = new BufferedReader(sfr);
        String line;
        while (null != (line = sfbr.readLine())) {
            var match = importPat.matcher(line);
            if (match.find()) {
                dfr.write(String.format("  <xs:import namespace=\"%s\" schemaLocation=\"%s\"/>\n",
                        structURI, structSL));
            }
            else dfr.write(line+"\n");
        }
        dfr.close();
        sfr.close();
    }
    
    
    // Write the schema document for the specified namespace
    protected void writeDocument (String nsuri, Writer ofw) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        root = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:schema");
        root.setAttribute("targetNamespace", nsuri);
        dom.appendChild(root);

        var arch      = getArchitecture();
        nsNIEMVersion = m.niemVersion(nsuri);        
        appinfoURI    = NamespaceKind.getBuiltinNS(NIEM_APPINFO, arch, nsNIEMVersion);
        clsaURI       = NamespaceKind.getBuiltinNS(NIEM_CLSA, arch, nsNIEMVersion);
        proxyURI      = NamespaceKind.getBuiltinNS(NIEM_PROXY, arch, nsNIEMVersion);
        structURI     = NamespaceKind.getBuiltinNS(NIEM_STRUCTURES, arch, nsNIEMVersion);
        appinfoPrefix = m.namespaceMap().getPrefix(appinfoURI);
        clsaPrefix    = m.namespaceMap().getPrefix(clsaURI);
        proxyPrefix   = m.namespaceMap().getPrefix(proxyURI);
        structPrefix  = m.namespaceMap().getPrefix(structURI);
        nsNSdeps    = new HashSet<>();
        nsPropdecls = new TreeMap<>();
        nsTypedefs  = new TreeMap<>();
        var deps    = subpropDeps.get(nsuri);
        nsNSdeps.addAll(subpropDeps.get(nsuri));
        nsNSdeps.add(structURI);
        utilityNSuri.add(structURI);
        
        // Create augmentation points, types, elements
        LOG.debug("Writing schema document for {}", nsuri);
        generateAugmentationComponents(nsuri);
        
        // Add namespace version number and language attribute, if specified
        String lang = m.schemaLanguage(nsuri);
        String nsv  = m.schemaVersion(nsuri);
        if (null != lang && !lang.isBlank()) root.setAttributeNS(XML_NS_URI, "lang", lang);
        if (null != nsv && !nsv.isBlank())   root.setAttribute("version", nsv);
        
        // Add conformance target assertions, if any
        String cta = m.conformanceTargets(nsuri);
        if (null != cta) {
            String ctns = NamespaceKind.getBuiltinNS(NIEM_CTAS, arch, nsNIEMVersion);
            String ctprefix;
            ctprefix = m.namespaceMap().getPrefix(ctns);
            root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:"+ctprefix, ctns);
            root.setAttributeNS(ctns, ctprefix + ":" + CONFORMANCE_ATTRIBUTE_NAME, cta);
        }
        // Add the <xs:annotation> element with namespace definition
        Namespace ns = m.getNamespaceByURI(nsuri);
        var ae = addDocAnnotation(dom, root, ns.getDefinition());
        nsNSdeps.add(nsuri);       // always define prefix for target namespace
        
        // Local term appinfo handled here, someday
        
        // Create type definitions for ClassType objects
        // Then create typedefs for Datatype objects (when not already created)
        // Remember external namespaces when encountered
        for (Component c : m.getComponentList()) createComplexTypeFromClass(dom, nsuri, c.asClassType());
        for (Component c : m.getComponentList()) createComplexTypeFromDatatype(dom, nsuri, c.asDatatype());
        for (Datatype dt : needSimpleType)       createSimpleTypeFromDatatype(dom, nsuri, dt);
        
        // Create elements and attributes for Property objects
        for (Component c : m.getComponentList()) createDeclaration(dom, nsuri, c.asProperty());

        // Add a namespace declaration and import element for each namespace dependency
        List<String> orderedDeps = new ArrayList<>();
        orderedDeps.addAll(nsNSdeps);
        Collections.sort(orderedDeps);
        for (String dnsuri : orderedDeps) {
            Namespace dns = m.getNamespaceByURI(dnsuri);
            String dnspre = m.namespaceMap().getPrefix(dnsuri);
            root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + dnspre, dnsuri);
            if (W3C_XML_SCHEMA_NS_URI.equals(dnsuri)) continue;     // don't import XSD
            if (nsuri.equals(dnsuri)) continue;                     // don't import current namespace
            Path thisDoc   = Paths.get(ns2file.get(nsuri));
            Path importDoc = Paths.get(ns2file.get(dnsuri));
            Path toImportDoc;
            if (null != thisDoc.getParent()) toImportDoc = thisDoc.getParent().relativize(importDoc);
            else toImportDoc = importDoc;
            String sloc = separatorsToUnix(toImportDoc.toString());
            Element ie = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:import");
            ie.setAttribute("namespace", dnsuri);
            ie.setAttribute("schemaLocation", sloc);
            if (null != dns && dns.isExternal()) addAppinfoAttribute(dom, ie, "externalImportIndicator", "true");
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
    
    // Iterate through all the ClassType objects in the model; create an
    // augmentation point property for each augmentable ClassType.  Need to 
    // do this for the whole model before trying to create augmentation elements.
    protected void generateAugmentationPoints () {
        for (var c : m.getComponentList()) {
            var targCT = c.asClassType();
            if (null == targCT) continue;               // not a ClassType
            if (!targCT.isAugmentable()) continue;      // not augmentable
            var targCTns   = targCT.getNamespace();
            var targCTName = targCT.getName();                                      // FooType
            var targName   = targCTName.substring(0, targCTName.length()-4);        // FooType -> Foo
            var augPtName  = targName + "AugmentationPoint";                        // FooType -> FooAugmentationPoint
            var augPoint = new Property(targCTns, augPtName);
            augPoint.setDefinition("An augmentation point for " + targCTName + ".");
            augPoint.setIsAbstract(true);
            m.addComponent(augPoint);
            var hp = new HasProperty();
            hp.setProperty(augPoint);
            hp.setMinOccurs(0);
            hp.setMaxUnbounded(true);
            targCT.addHasProperty(hp);        
        }        
    }
    
    // Use the augmentation info in each Namespace to generate augmentation 
    // types, and elements required for this namespace.
    protected void generateAugmentationComponents (String nsuri) {
        var class2Aug = new HashMap<ClassType,List<AugmentRecord>>();
        var ns = m.getNamespaceByURI(nsuri);

        // Create map from Class to list of its augmentations in this namespace
        for (AugmentRecord ar : ns.augmentList()) {
            var targCT = ar.getClassType();
            var arlist = class2Aug.get(targCT);
            if (null == arlist) {
                arlist = new ArrayList<>();
                class2Aug.put(targCT, arlist);
            }
            arlist.add(ar);
        }
        // Handle augmentations for each class augmented in this namespace
        for (var targCT : class2Aug.keySet()) {
            var targNS     = targCT.getNamespace();
            var targNSuri  = targNS.getNamespaceURI();
            var targCTName = targCT.getName();                                      // FooType
            var targName   = targCTName.substring(0, targCTName.length()-4);        // FooType -> Foo
            var augElName  = targName + "Augmentation";                             // fooType -> FooAugmentation
            var augTpName  = augElName + "Type";                                    // FooType => FooAugmentationType
            var augPtName  = targName + "AugmentationPoint";                        // FooType -> FooAugmentationPoint
            var augPoint   = m.getProperty(targNSuri, augPtName);                       // augmentation point Property
          
            // Create ordered list of augmenting properties
            var arlist = class2Aug.get(targCT);
            Collections.sort(arlist, Comparator.comparing(AugmentRecord::indexInType));
            
            // Handle augmentation elements (not part of an augmentation type
            int indx;
            for (indx = 0; indx < arlist.size(); indx++) {
                var ar = arlist.get(indx);
                if (ar.indexInType() >= 0) break;
                var augp = ar.getProperty();
                augp.setSubPropertyOf(augPoint);
            }
            // If no properties left, we're done; don't need an augmentation type or element
            if (indx >= arlist.size()) continue;
            
            // Otherwise create augmentation type for remaining properties
            ClassType augCT = new ClassType(ns, augTpName);
            augCT.setDefinition("A data type for additional information about a " + targName + ".");
            while (indx < arlist.size()) {
                var ar = arlist.get(indx++);
                var hp = new HasProperty();
                hp.setProperty(ar.getProperty());
                hp.setMinOccurs(ar.minOccurs());
                hp.setMaxOccurs(ar.maxOccurs());
                hp.setMaxUnbounded(ar.maxUnbounded());
                hp.setOrderedProperties(ar.orderedProperties());
                augCT.addHasProperty(hp);
            }
            m.addComponent(augCT);
          
            // Create an augmentation element
            var augP = new Property(ns, augElName);
            augP.setDefinition("Additional information about a " + targName + ".");
            augP.setClassType(augCT);
            augP.setSubPropertyOf(augPoint);
            m.addComponent(augP);
        }        

    }
    
    // Add @appinfo:appatt="value" to an element.  Get the right namespace prefix and URI
    // for the current document.  Add the namespace declaration for appinfo, but
    // don't import it.
    protected void addAppinfoAttribute (Document dom, Element e, String appatt, String value) {
        var appinfoQName = appinfoPrefix + ":" + appatt;
        root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:"+appinfoPrefix, appinfoURI);
        e.setAttributeNS(appinfoURI, appinfoQName, value);
        utilityNSuri.add(appinfoURI);
    }
    
    // Create a complex type declaration from a ClassType object
    // This can have simpleContent if the ClassType has a literal property
    // Otherwise it has complexContent
    protected void createComplexTypeFromClass (Document dom, String nsuri, ClassType ct) { 
        if (null == ct) return;
        String cname = ct.getName();
        if (nsTypedefs.containsKey(cname)) return;              // already created
        if (!nsuri.equals(ct.getNamespaceURI())) return;        // different namespace
        Element cte = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        Element exe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
        cte.setAttribute("name", cname);
        var ae = addDocAnnotation(dom, cte, ct.getDefinition());
        if (ct.isAbstract())   cte.setAttribute("abstract", "true");
        if (ct.isDeprecated()) addAppinfoAttribute(dom, cte, "deprecated", "true");
        if (ct.isExternal())   addAppinfoAttribute(dom, cte, "externalAdapterTypeIndicator", "true");
        nsTypedefs.put(cname, cte);
        
        // Create simple content for class with FooLiteral property
        if (litTypes.contains(ct)) {
            var sce   = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
            var plist = ct.hasPropertyList();
            var lp    = plist.get(0).getProperty();     // FooLiteral property
            var lpt   = lp.getDatatype();               // datatype for FooLiteral
            var lptqn = proxifiedDatatypeQName(lpt);
            if (needSimpleType.contains(lpt)) {
                lptqn = lptqn.replaceFirst("Type$", "SimpleType");
                lptqn = lptqn.replaceFirst("Datatype$", "SimpleType");
            }
            exe.setAttribute("base", lptqn);
            if (lptqn.endsWith("SimpleType")) addSimpleTypeExtension(dom, exe);

            litProps.add(lp);                   // remember we don't need an element for this
            plist.remove(0);                    // leaving the attribute properties for later
            cte.appendChild(sce);               // xs:complexType has xs:simpleContent
            sce.appendChild(exe);               // xs:simpleContent has xs:extension
        }
        // Otherwise create complex content with sequence of element refs
        else {
            Element sqe = null;
            var cce     = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexContent");
            var basect  = ct.getExtensionOfClass();
            if (null == basect) {
                if (cname.endsWith("AssociationType"))        exe.setAttribute("base", structPrefix + ":AssociationType");
                else if (cname.endsWith("MetadataType"))      exe.setAttribute("base", structPrefix + ":MetadataType");
                else if (!cname.endsWith("AugmentationType")) exe.setAttribute("base", structPrefix + ":ObjectType");
                else exe.setAttribute("base", structPrefix + ":AugmentationType");
            } 
            else {
                exe.setAttribute("base", basect.getQName());
                nsNSdeps.add(basect.getNamespace().getNamespaceURI());
            }
            // Add element refs for element properties
            for (HasProperty hp : ct.hasPropertyList()) {
                if (!hp.augmentingNS().isEmpty()) continue;
                if (null == sqe) sqe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:sequence");
                addElementRef(dom, sqe, hp);
                nsNSdeps.add(hp.getProperty().getNamespace().getNamespaceURI());
            }
            cte.appendChild(cce);                   // xs:complexType has xs:complexContent
            cce.appendChild(exe);                   // xs:complexContent has xs:extension
            if (null != sqe) exe.appendChild(sqe);  // xs:extension has xs:sequence
        }
        // Now add attribute properties in hasProperty list to the xs:extension element
        // Do this for simple content or complex content
        for (HasProperty hp : ct.hasPropertyList()) {
            Property p = hp.getProperty();
            if (!p.isAttribute()) continue;
            var ate = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            ate.setAttribute("ref", p.getQName());
            if (0 != hp.minOccurs()) ate.setAttribute("use", "required");
            exe.appendChild(ate);
            nsNSdeps.add(p.getNamespace().getNamespaceURI());
        }
    }
    
    protected void addSimpleTypeExtension (Document dom, Element exe) { }
    
    // Create <xs:element ref="foo">, append it to some <xs:sequence>
    protected void addElementRef (Document dom, Element sqe, HasProperty hp) {
        if (hp.getProperty().isAttribute()) return;
        var hpe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
        if (1 != hp.minOccurs()) hpe.setAttribute("minOccurs", "" + hp.minOccurs());
        if (hp.maxUnbounded())   hpe.setAttribute("maxOccurs", "unbounded");
        else if (1 != hp.maxOccurs()) hpe.setAttribute("maxOccurs", "" + hp.maxOccurs());
        hpe.setAttribute("ref", hp.getProperty().getQName());
        if (hp.orderedProperties())
            addAppinfoAttribute(dom, hpe, "orderedPropertyIndicator", "true");
        sqe.appendChild(hpe);
    }
    
    // Create a complex type declaration from a Datatype object (FooType)
    protected void createComplexTypeFromDatatype (Document dom, String nsuri, Datatype dt) {
        if (null == dt) return;
//        if (needSimpleType.contains(dt)) return;                    // create xs:simpleType instead
        var cname = dt.getName().replaceFirst("Datatype$", "SimpleType");
        var bname = cname.replaceFirst("SimpleType$", "Type");
        if (nsTypedefs.containsKey(cname)) return;                  // already created
        if (nsTypedefs.containsKey(bname)) return;                  // already have FooType complex type
        if (!nsuri.equals(dt.getNamespaceURI())) return;            // not in this namespace
        if (W3C_XML_SCHEMA_NS_URI.equals(dt.getNamespaceURI())) return;        // don't create XSD builtins
        
        var cte = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        var sce = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
        var exe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
        cte.setAttribute("name", cname);        
        var ae = addDocAnnotation(dom, cte, dt.getDefinition());
        if (dt.isDeprecated()) addAppinfoAttribute(dom, cte, "deprecated", "true");

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
        if (btqn.endsWith("SimpleType")) addSimpleTypeExtension(dom, exe);

        exe.setAttribute("base", btqn);
        sce.appendChild(exe);   // xs:simpleContent has xs:extension
        cte.appendChild(sce);   // xs:complexType has xs:simpleContent
        nsTypedefs.put(cname, cte);
    } 
    
    // Create a simple type declaration from a Datatype object
    protected void createSimpleTypeFromDatatype (Document dom, String nsuri, Datatype dt) {
        if (null == dt) return;
        var cname = dt.getName();
        if (cname.endsWith("Datatype")) 
            cname = cname.substring(0,cname.length()-8) + "SimpleType";
        else cname = cname.replaceFirst("Type$", "SimpleType");
        if (!nsuri.equals(dt.getNamespaceURI())) return;    // not in this namespace
        if (nsTypedefs.containsKey(cname)) return;   // already created

        var ste   = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleType");
        var ae = addDocAnnotation(dom, ste, dt.getDefinition());
        if (null != dt.getCodeListBinding()) {
            if (null == ae) ae = addAnnotation(dom,ste);
            var ap = addAppinfo(dom, ae);
            var cb = addCodeListBinding(dom, ap, nsuri, dt.getCodeListBinding());
        }
        if (null != dt.getUnionOf()) addUnionElement(dom, ste, dt);
        else if (null != dt.getListOf()) addListElement(dom, ste, dt);
        else if (null != dt.getRestrictionOf()) addRestrictionElement(dom, ste, dt);
        ste.setAttribute("name", cname);
        nsTypedefs.put(cname, ste);   
        nsNSdeps.add(dt.getNamespace().getNamespaceURI());        
    }
    
    protected void addUnionElement (Document dom, Element ste, Datatype bdt) {
        Element une = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:union");
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
    
    protected void addListElement (Document dom, Element ste, Datatype bdt) {
        Element lse = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:list");
        lse.setAttribute("itemType", maybeSimpleTypeQName(bdt.getListOf()));
        ste.appendChild(lse);
    }
    
    protected void addRestrictionElement (Document dom, Element ste, Datatype bdt) {
        RestrictionOf r = bdt.getRestrictionOf();
        Element rse = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:restriction");
        rse.setAttribute("base", maybeSimpleTypeQName(r.getDatatype()));
        for (Facet f : r.getFacetList()) {
            String fk = f.getFacetKind();
            String ename = "xs:" + toLowerCase(fk.charAt(0)) + fk.substring(1);
            Element fce = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, ename);
            fce.setAttribute("value", f.getStringVal());
            addDocAnnotation(dom, fce, f.getDefinition());
            rse.appendChild(fce);
        }
        ste.appendChild(rse);
    }
    
    // Returns QName for FooSimpleType if that type exists, otherwise QName for FooType
    protected String maybeSimpleTypeQName (Datatype dt) {
        String dtqn   = dt.getQName();
        String dtbase = dtqn.replaceFirst("Type$", "");
        String dtsqn  = dtbase + "SimpleType";
        if (needSimpleType.contains(dt)) return dtsqn;
        return dtqn;
    }
    
    // Convert "xs:foo" to "xs-proxy:foo"
    protected String proxifiedDatatypeQName (Datatype dt) {
        String dtqn = dt.getQName();
        if (W3C_XML_SCHEMA_NS_URI.equals(dt.getNamespaceURI())) {
            dtqn = proxyPrefix + ":" + dt.getName();
            nsNSdeps.add(proxyURI);
            utilityNSuri.add(proxyURI);
        }
        return dtqn;
    }
    
    // Create an element or attribute declaration from a Property object
    protected void createDeclaration(Document dom, String nsuri, Property p) {
        if (null == p) return;
        if (litProps.contains(p)) return;
        boolean isAttribute = p.isAttribute();
        ClassType pct = p.getClassType();
        Datatype pdt  = p.getDatatype();
        Element pe;
        if (!nsuri.equals(p.getNamespaceURI())) return;   
        LOG.debug("Creating {} for {}", (p.isAttribute() ? "attribute" : "element"), p.getQName());
        if (isAttribute) pe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        else pe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
        pe.setAttribute("name", p.getName());
        if (p.isReferenceable()) pe.setAttribute("nillable", "true");
        if (p.isAbstract())      pe.setAttribute("abstract", "true");
        if (p.isDeprecated())    addAppinfoAttribute(dom, pe, "deprecated", "true");
        if (p.isRefAttribute())  addAppinfoAttribute(dom, pe, "referenceAttributeIndicator", "true");
        if (p.isRelationship())  addAppinfoAttribute(dom, pe, "relationshipPropertyIndicator", "true");
        var ae = addDocAnnotation(dom, pe, p.getDefinition());
        if (null != pct) {
            pe.setAttribute("type", pct.getQName());
            nsNSdeps.add(pct.getNamespace().getNamespaceURI());
        }
        // Attribute declarations can use XSD types
        // Replace x:FooDatatype and x:FooType with x:FooSimpleType
        if (isAttribute && null != pdt) {
            var pdtQN = pdt.getQName();
            pdtQN = pdtQN.replaceFirst("Type$", "SimpleType");
            pdtQN = pdtQN.replaceFirst("Datatype$", "SimpleType");            
            pe.setAttribute("type", pdtQN);
            nsNSdeps.add(pdt.getNamespace().getNamespaceURI());
        }
        // Element declarations use proxy types instead of XSD types
        else if (!isAttribute && null != pdt) {
            var pdtQN = proxifiedDatatypeQName(pdt);
            var pdtNS = pdt.getNamespace();
            pe.setAttribute("type", pdtQN);
            nsNSdeps.add(pdtNS.getNamespaceURI());
        }
        if (null != p.getSubPropertyOf()) {
            var subp   = p.getSubPropertyOf();
            var subpQN = subp.getQName();
            pe.setAttribute("substitutionGroup", subpQN);
            nsNSdeps.add(subp.getNamespace().getNamespaceURI());
        }
        nsPropdecls.put(p.getName(), pe);
    }
    
    // Use this to add annotation and documentation elements to a schema component.
    // Returns the annotation element (to add appinfo, later).
    // Returns null (and does nothing) if the documentation string is null or blank.
    protected Element addDocAnnotation (Document dom, Element e, String s) {
        if (null == s || s.isBlank()) return null;
        var ae = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        var de = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:documentation");
        de.setTextContent(s);
        ae.appendChild(de);
        e.appendChild(ae);
        return ae;
    }
    
    protected Element addAnnotation (Document dom, Element e) {
        var ae = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        e.appendChild(ae);
        return ae;
    }
    
    protected Element addDocumentation (Document dom, Element e) {
        var de = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:documentation");
        e.appendChild(de);
        return de;
    }
    
    protected Element addAppinfo (Document dom, Element e) {
        var ai = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:appinfo");
        e.appendChild(ai);
        return ai;
    }
    
    protected Element addCodeListBinding (Document dom, Element e, String nsuri, CodeListBinding clb) {
        Element cbe = dom.createElementNS(clsaURI, clsaPrefix + ":" + "SimpleCodeListBinding");
        cbe.setAttribute("columnName", clb.getColumn());
        cbe.setAttribute("codeListURI", clb.getURI());
        if (clb.getIsConstraining()) cbe.setAttribute("constrainingIndicator", "true");
        e.appendChild(cbe);
        return cbe;
    }

//    // debug tool
//    private void domToString (Document dom) {
//        nsTypedefs.forEach((name,element) -> {
//            root.appendChild(element);
//        });
//        nsPropdecls.forEach((name,element) -> {
//            root.appendChild(element);
//        });        
//        try {
//            Transformer tr = TransformerFactory.newInstance().newTransformer();
//            tr.setOutputProperty(OutputKeys.INDENT, "yes");
//            tr.setOutputProperty(OutputKeys.METHOD, "xml");
//            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//            StringWriter ostr = new StringWriter();
//            tr.transform(new DOMSource(dom), new StreamResult(ostr));
//            String buf = ostr.toString();
//            int i = 0;
//        } catch (TransformerException ex) {
//            java.util.logging.Logger.getLogger(ModelToXSD.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
    // Writes the XSD document model.  Post-processing of XSLT output to do
    // what we wish XSLT would do, but doesn't.  You can't process arbitrary XML in
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
    
    // Returns a File object for the proper schema document in the "share" directory
    // for utility and XML namespaces.
    protected File getShareSchemaFile (String nsuri) {
        // Path to share directory is different when running from IDE
        var sdirfn = ModelToXSD.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (sdirfn.endsWith(".jar")) sdirfn = FilenameUtils.concat(sdirfn, "../../share/xsd");
        else sdirfn = FilenameUtils.concat(sdirfn, "../../../../src/main/dist/share/xsd");
        var sdir = new File(sdirfn);
        
        if (XML_NS_URI.equals(nsuri)) return new File(sdir, "xml.xsd");
        
        var vsuf = getShareVersionSuffix();
        var vers = NamespaceKind.version(nsuri) + vsuf;
        int util = NamespaceKind.builtin(nsuri);
        var bfn  = NamespaceKind.defaultBuiltinFN(util);
        bfn = FilenameUtils.concat(vers, bfn);
        return new File(sdir, bfn);
    }
    
    protected String getArchitecture ()       { return "NIEM5"; }
    protected String getShareVersionSuffix () { return ".0"; }
    
    // Generate a file path for thie namespace schema document
    // Use a default relative path if no hint provided.
    // Mung the file path as needed to make path unique and not existing
    // Return the path to the created file (including outputDir)
    private static Pattern nsnamePat = Pattern.compile("/(\\w+)/\\d(\\.\\d+)*(/#)?$");
    private String genSchemaFile (String nsuri, File outputDir, String hint) throws IOException {
        if (null == hint) {
            if (XML_NS_URI.equals(nsuri)) hint = "xml.xsd";
            else {
                int util = NamespaceKind.builtin(nsuri);
                if (util < NIEM_NOTUTILITY) hint = NamespaceKind.defaultBuiltinFN(util);
                else {
                    var m = nsnamePat.matcher(nsuri);
                    if (m.find()) hint = m.group(1);
                    else hint = "schema";                    
                }
            }
        }
        if (hint.endsWith(".xsd")) hint = hint.substring(0, hint.length()-4);
        var munged = hint;
        int mungct = 0;
        var fp = new File(outputDir, munged + ".xsd");
        while (nsfiles.contains(munged) || fp.exists()) {
            munged = String.format("%s_%d.xsd", hint, mungct++);
            fp = new File(outputDir, munged);
        }
        var fpath = fp.getPath();
        nsfiles.add(munged);
        ns2file.put(nsuri, fpath);
        return fpath;
    }
    
}
