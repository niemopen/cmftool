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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FilenameUtils;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.mitre.niem.cmf.AugmentRecord;
import static org.mitre.niem.cmf.CMFObject.CMF_LIST;
import static org.mitre.niem.cmf.CMFObject.CMF_RESTRICTION;
import static org.mitre.niem.cmf.CMFObject.CMF_UNION;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import static org.mitre.niem.cmf.Component.qnToName;
import static org.mitre.niem.cmf.Component.qnToPrefix;
import org.mitre.niem.cmf.DataProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.ListType;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.ReferenceGraph;
import org.mitre.niem.cmf.Restriction;
import org.mitre.niem.cmf.Union;
import static org.mitre.niem.utility.IndefiniteArticle.articalize;
import org.mitre.niem.utility.NaturalOrderComparator;
import org.mitre.niem.utility.ResourceManager;
import org.mitre.niem.xml.LanguageString;
import org.mitre.niem.xml.ParserBootstrap;
import org.mitre.niem.xml.XMLCatalogCreator;
import org.mitre.niem.xml.XSDWriter;
import static org.mitre.niem.xsd.ModelFromXSD.replaceSuffix;
//import static org.mitre.niem.xsd.NIEMSchemaDocument.qnToName;
//import static org.mitre.niem.xsd.NIEMSchemaDocument.qnToPrefix;
import static org.mitre.niem.xsd.NamespaceKind.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class for writing a NIEM model as an XML Schema document pile.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXSD {
    static final Logger LOG = LogManager.getLogger(ModelToXSD.class);

    protected Model m;
    protected String useNiemVersion = null;
    protected String catalogPath = null;
    protected Namespace rootNS = null;
    protected final NamespaceMap prefixMap              = new NamespaceMap();   // all prefixes for namespaces in the model
    protected final Set<String> niemVersions            = new HashSet<>();      // all the NIEM version names in the model
    protected final Map<String,String> namespaceU2Path  = new HashMap<>();      // nsU -> file path in outD
    protected final Map<String,Integer> namespaceU2Kind = new HashMap<>();      // nsU -> namespace kind
    protected final Set<String> extNSs                  = new HashSet<>();      // URIs of external namespaces
    
    public ModelToXSD (Model m) {
        this.m = m;
    }
    
    public void setNIEMVersion (String vers) {
        
    }
    
    public void setCatalogPath (String path) {
        catalogPath = path;
        LOG.debug("catalog path = {}", catalogPath);
    }
    
    public void setRootNamespace (String nsPrefixOrURI) {
        if (null == nsPrefixOrURI) return;
        rootNS = m.namespaceObj(nsPrefixOrURI);
        if (null == rootNS)
            LOG.error("Model does not contain namespace '{}'", nsPrefixOrURI);
    }
    
    /**
     * Writes the model to an XSD pile in the specified directory.  The schema
     * document for each model namespace gets the NIEM version specified in the
     * arguments.
     * @param outD
     * @param niemVersion 
     */
    public void writeModelXSD (File outD, String niemVersion) throws ParserConfigurationException, IOException {
        useNiemVersion = niemVersion;
        writeModelXSD(outD);
    }
    
    /**
     * Writes the model to an XSD pile in the specified directory.  The schema 
     * document for each model namespace gets the NIEM version specified in the model
     * namespace object.
     * @param outD 
     */
    public void writeModelXSD (File outD) throws ParserConfigurationException, IOException {
        collectNIEMVersions();
        collectNamespacePrefixes();
        collectNamespaceKinds();
        establishFilePaths();
        identifySimpleTypes();
        for (var ns : m.namespaceSet())
            if (ns.isExternal()) extNSs.add(ns.uri());
        for (var ns : m.namespaceSet()) 
            if (ns.isModelNS()) writeModelDocument(ns, outD);
        for (var vers : niemVersions)
            writeVersionBuiltins(vers, outD);
        if (null != catalogPath) {
            var catF = new File(outD, catalogPath);
            var outS = new FileOutputStream(catF);
            var outW = new OutputStreamWriter(outS, "UTF-8");
            var catW = new XMLCatalogCreator();
            var catP = new File(catalogPath).toPath();
            if (null == catP.getParent()) catP = new File(".").toPath();
            else catP = catP.getParent();
            catW.writeCatalog(namespaceU2Path, catP, outW);
            outW.close();
        }
            
    }

    // Examine all the namespaces to collect all the NIEM versions.  If the version
    // was specified in the call to writeModelXSD, then there will only be one.
    protected void collectNIEMVersions () {
        if (null != useNiemVersion) niemVersions.add(useNiemVersion);
        else 
            for (var ns : m.namespaceSet()) {
                var nver = ns.niemVersion();
                if (!nver.isEmpty()) niemVersions.add(ns.niemVersion());
        }
    }
    
    // Reserve the namespace prefix for every namespace in the model.  That will
    // include any external namespace, plus the XSD and XML namespaces.  We need
    // these to establish a prefix for each builtin namespace... because, you know,
    // there's no rule against using "appinfo" for a model namespace.  Grr. 
    protected void collectNamespacePrefixes () {
        for (var ns: m.namespaceSet()) {
            prefixMap.assignPrefix(ns.prefix(), ns.uri());
        }
    }
    
    protected void collectNamespaceKinds () {
        for (var ns : m.namespaceSet()) {
            var nsU = ns.uri();
            var kcode = ns.kindCode();
            var kind  = NamespaceKind.codeToKind(kcode);
            namespaceU2Kind.put(nsU, kind);
        }
        for (var nver : niemVersions) {
            for (var kcode : builtins()) {
                var kind = codeToKind(kcode);
                var bnsU = builtinNSU(nver, kcode);
                if (bnsU.isEmpty()) continue;
                namespaceU2Kind.put(bnsU, kind);
            }
        }
        namespaceU2Kind.put(W3C_XML_SCHEMA_NS_URI, NSK_XSD);
        namespaceU2Kind.put(XML_NS_URI, NSK_XML);
    }
    
    // Establish the relative path for each schema document, taking into account
    // the document file path specified in each namespace object and the namespace
    // to path mapping given in the writeModelXSD call (if any).  File names are
    // munged as needed to make each path unique.
    protected void establishFilePaths () {
        // Did you put duplicates into your path map, you horrible creature? Nice try.
        var ns2p  = new HashMap<String,String>(namespaceU2Path);
        var paths = new HashSet<String>();
        namespaceU2Path.clear();
        ns2p.forEach((ns,path) -> {
            path = mungPath(paths, path);
            paths.add(path);
            namespaceU2Path.put(ns, "./" + path);
        });
        // Now do the namespaces
        for (var ns : m.namespaceSet()) {
            if (namespaceU2Path.containsKey(ns.uri())) continue;
            var path = ns.documentFilePath();
            if (path.isEmpty()) continue;
            path = mungPath(paths, path);
            paths.add(path);
            namespaceU2Path.put(ns.uri(), "./" + path);
        }
        // Now do the builtins for each NIEM version
        for (var vers : niemVersions) {
            var vdir = "";
            if (null != useNiemVersion || 1 == niemVersions.size()) vdir = "niem/";
            else vdir = NamespaceKind.versionDirName().get(vers);
            if (null == vdir) continue;
            for (var kcode : NamespaceKind.builtins()) {
                var nsU   = NamespaceKind.builtinNSU(vers, kcode);
                var rpath = NamespaceKind.builtinPath().get(kcode);
                var path  = vdir + rpath;
                if (namespaceU2Path.containsKey(nsU)) continue;
                path = mungPath(paths, path);
                paths.add(path);
                namespaceU2Path.put(nsU, "./" + path);
            }
        }
    }
    
    // We must create a simple type definition for each Datatype object that is
    // a list, a list item, a union, a union member, or an attribute property type.
    // Also sometimes for the datatype of a literal property.
    // But not for datatypes in the XML or XML Schema namespaces.
    protected final Set<Datatype> simpleTypes =  new HashSet<>();
    protected void identifySimpleTypes () {
        var stUs = new HashSet<Datatype>();
        for (var dt : m.datatypeL()) {
            switch (dt.getType()) {
            case CMF_LIST:
                stUs.add(dt);
                stUs.add(dt.itemType());
                break;
            case CMF_UNION:
                stUs.add(dt);
                for (var mdt : dt.memberL()) stUs.add(mdt);
                break;
            }
        }
        for (var dp : m.dataPropertyL()) {
            var dpQ = dp.qname();
            var dt  = dp.datatype();
            if (null == dt) continue;
            var dtQ = dt.qname();
            if (dp.isAttribute()) 
                stUs.add(dt);
            if (dpQ.endsWith("Literal") && dtQ.endsWith("SimpleType")) 
                stUs.add(dt);
        }
        // Only need xs:simpleType elements for Datatype objects with a model namespace
        for (var dt : stUs) {
            if (null == dt) continue;           // eg. datatype of xml:lang
            var dtnsU = dt.namespaceURI();
            var dtq = dt.qname();
            if (W3C_XML_SCHEMA_NS_URI.equals(dtnsU)) continue;
            if (!dt.isModelComponent()) continue;
            simpleTypes.add(dt);        
            LOG.debug("need simple type: {}", dt.qname());
        }
    }

    
    protected void writeModelDocument (Namespace ns, File outD) throws ParserConfigurationException, IOException {       
        // Initialize the document and xs:schema root element
        var db   = ParserBootstrap.docBuilder();
        var doc  = db.newDocument();
        var root = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:schema");
        doc.appendChild(root);
        
        // Get namespace URI and NIEM version; set the xs:schema attributes that
        // don't need a namespace prefix. (We don't know what the prefixes are yet.)
        var nsU  = ns.uri();
        var nver = ns.niemVersion();
        if (null != useNiemVersion) nver = useNiemVersion;
        setAttribute(root, "targetNamespace", nsU);
        setAttribute(root, "version", ns.version());
        setAttribute(root, "xml:lang", ns.language());
        
        // Given the NIEM version, we can get builtin namespace URIs and assign prefixes
        var bc2pre = new HashMap<String,String>();
        var bc2U   = new HashMap<String,String>();
        var nsmap  = new NamespaceMap(prefixMap);
        for (var bcode : NamespaceKind.builtins()) {
            var bnsU = NamespaceKind.builtinNSU(nver, bcode);
            var bpre = bcode.toLowerCase();
            if (null == nsmap.getURI(bnsU)) {
                bpre = nsmap.assignPrefix(bpre, bnsU);
                bc2pre.put(bcode, bpre);
                bc2U.put(bcode, bnsU);
            }
        }
        var appinfoPre = bc2pre.get("APPINFO");
        var appinfoU   = bc2U.get("APPINFO");        
        var refnsUs    = new HashSet<String>();         // need prefixes and imports for these namespaces
        var defEL      = new ArrayList<Element>();      // list of type definition elements
        var decEL      = new ArrayList<Element>();      // list of attribute/element declaration elements
        
        // Create xs:annotation element; add namespace documentation
        var annE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        var appE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:appinfo");
        addDocumentation(doc, annE, ns.docL());
        
        // Add conformance target assertions
        var ctNSU = versionToCtNsURI(nver);
        var ctPre = nsmap.assignPrefix("ct", ctNSU);
        var ctQ   = ctPre + ":" + "conformanceTargets";
        root.setAttributeNS(ctNSU, ctQ, listToString(ns.ctargL()));
        root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:"+ctPre, ctNSU); 
            
        // Add appinfo:Augmentation elements and appinfo:LocalTerm elements.
        // If xs:appinfo has children, append it to xs:annotation.
        // If xs:annotation has children, append it to xs:schema.
        for (var arec : ns.augL()) {
            if (!arec.property().isAttribute()) continue;
            if ("-1".equals(arec.index())) continue;
            var act  = arec.classType();
            var augE = doc.createElementNS(appinfoU, appinfoPre + ":" + "Augmentation");
            setAttribute(augE, "property", arec.property().qname());
            setAttribute(augE, "globalClassCode", setToString(arec.codeS()));
            if (null != act) setAttribute(augE, "class", act.qname());            
            if ("1".equals(arec.minOccurs())) setAttribute(augE, "use", "required");
            appE.appendChild(augE);
        }
        addLocalTerms(doc, appE, ns, appinfoPre, appinfoU);
        if (appE.getChildNodes().getLength() > 0) {
            annE.appendChild(appE);
            refnsUs.add(appinfoU);            
        }
        if (annE.getChildNodes().getLength() > 0) root.appendChild(annE);

        var pU2subQ = new HashMap<String,String>();
        createAugmentationComponents(doc, defEL, decEL, refnsUs, ns, bc2pre, bc2U, pU2subQ);       

        // Create complex types for literal classes and ordinary classes.
        var xctUs = new HashSet<String>();
        for (var ct : m.classTypeL()) {
            var ctname = ct.qname();
            if (hasSimpleContent(ct)) createCSCType(doc, defEL, refnsUs, nsU, ct, bc2pre, bc2U);
            else createCCCType(doc, defEL, decEL, refnsUs, nsU, ct, bc2pre, bc2U, !ct.name().endsWith("AdapterType"));
            xctUs.add(ct.uri());
        }
        // Create CSC types for datatypes.  But don't create a CSC wrapper around
        // a simple type if we already created a type with the same name.
        for (var dt : m.datatypeL()) {
            var wrapU = replaceSuffix(dt.uri(), "SimpleType", "Type");
            if (!xctUs.contains(wrapU))
                createCSCType(doc, defEL, refnsUs, nsU, dt, bc2pre, bc2U);
        }
        // Create simple types and attribute/element declarations.
        for (var dt : simpleTypes)   createSimpleType(doc, defEL, refnsUs, nsU, dt, bc2pre, bc2U);
        for (var p : m.propertyL())  createDeclaration(doc, decEL, refnsUs, nsU, p, bc2pre, bc2U, pU2subQ);
        
        // Need appinfo if an external namespace is referenced
        var extF = false;
        for (var refnsU : refnsUs) 
            if (extNSs.contains(refnsU)) extF = true;
        if (extF) refnsUs.add(appinfoU);            
        
        // At this point we know all of the referenced namespaces.
        // Create namespace declarations; add import elements in a pleasing order.
        refnsUs.add(nsU);
        refnsUs.add(bc2U.get("STRUCTURES"));
        var op = namespaceU2Path.get(nsU);
        var outF = new File(outD, namespaceU2Path.get(nsU));
        var outP = new File(namespaceU2Path.get(nsU)).getParentFile().toPath();
        var impL = new ArrayList<Pair<String,String>>();
        if (ns == rootNS) {
            var refGraph = new ReferenceGraph(m);
            var reachS   = refGraph.reachableFrom(ns);
            for (var ons : m.namespaceSet()) {
                if (!reachS.contains(ons))
                    refnsUs.add(ons.uri());
            }
        }
        // Prepare list of imports for sorting
        for (var refnsU : refnsUs) {
            var pre  = nsmap.getPrefix(refnsU);
            var kind = namespaceU2Kind.getOrDefault(refnsU, NSK_UNKNOWN);
            var key  = String.format("%02d%s", kind, pre);          
            if (NSK_APPINFO != kind && NSK_CLSA != kind) 
                impL.add(new Pair<>(key, refnsU));
            if (NSK_XML != kind) 
                root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:"+pre, refnsU); 
        }
        // Sort imports into pleasing order and generate xs:import elements
        Collections.sort(impL, importPairComparator);
        for (var p : impL) {
            var refnsU = p.getValue1();
            if (W3C_XML_SCHEMA_NS_URI.equals(refnsU)) continue;
            if (nsU.equals(refnsU)) continue;
            var rns  = m.namespaceObj(refnsU);
            var snF  = new File(namespaceU2Path.get(refnsU));
            var snP  = snF.toPath();
            var relP = outP.relativize(snP);
            var sloc = separatorsToUnix(relP.toString());
            var impE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:import");
            impE.setAttribute("namespace", refnsU);
            impE.setAttribute("schemaLocation", sloc);
            if (extNSs.contains(refnsU))
                impE.setAttributeNS(appinfoU, appinfoPre + ":" + "externalImportIndicator", "true");
            addAnnotationDoc(doc, impE, ns.idocL(refnsU));
            root.appendChild(impE);
        }
        
        Collections.sort(defEL, definitionComparator);
        Collections.sort(decEL, declarationComparator);
        for (var e : defEL) root.appendChild(e);
        for (var e : decEL) root.appendChild(e);        

        writeXSD(doc, outF);
    }
    
    protected void createAugmentationComponents (Document doc, 
        List<Element> defEL,                // add typedef elements to this list
        List<Element> decEL,                // add typedef elements to this list
        Set<String> refnsUs,                // URIs of referenced namespaces
        Namespace ns,                       // URI of current namespace document
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U,            // URIs for builtin namespaces    
        Map<String,String> pU2subQ) {       // ordinary property aug subsitutionGroup QN
        
        var classQ2ArecL = new HashMap<String,List<AugmentRecord>>();
        for (var arec : ns.augL()) {
            if (null == arec.classType()) continue;         // do nothing for global augmentation
            var propU  = arec.property().uri();
            var classQ = arec.classType().qname();
            var augPQ  = replaceSuffix(classQ, "Type", "AugmentationPoint");
            if (arec.index().isEmpty()) {
                pU2subQ.put(propU, augPQ);
            }
            else {
                var arecL = classQ2ArecL.get(classQ);
                if (null == arecL) {
                    arecL = new ArrayList<>();
                    classQ2ArecL.put(classQ, arecL);
                }
                arecL.add(arec);
            }
        }
        for (var classQ : classQ2ArecL.keySet()) {
            var augmct = m.qnToClassType(classQ);
            var propL  = classQ2ArecL.get(classQ);
            var augptQ = replaceSuffix(classQ, "Type", "AugmentationPoint");
            var cname  = qnToName(classQ);
            var cnoun  = articalize(replaceSuffix(cname, "Type", "").toLowerCase());
            var aname  = replaceSuffix(cname, "Type", "Augmentation");
            var atname = aname + "Type";
            var docstr = "A data type for additional information about " + cnoun + ".";
            var augct  = new ClassType(ns, atname);
            Collections.sort(propL);
            augct.addDocumentation(docstr, "en-US");
            augct.propL().addAll(propL);
            createCCCType(doc, defEL, decEL, refnsUs, ns.uri(), augct, bc2pre, bc2U, false);
            
            docstr   = "Additional information about " + cnoun + ".";
            var augE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
            var docL = List.of(new LanguageString(docstr, "en-US"));
            augE.setAttribute("name", aname);
            augE.setAttribute("type", augct.qname());
            augE.setAttribute("substitutionGroup", augptQ);
            addAnnotationDoc(doc, augE, docL);
            refnsUs.add(augmct.namespaceURI());
            decEL.add(augE);
        }
    }
    
    // Create a complex type with complex content from a non-literal class object
    protected void createCCCType (Document doc, 
        List<Element> defEL,                // add typedef elements to this list
        List<Element> decEL,                // add augmentation point elements to this list
        Set<String> refnsUs,                // URIs of referenced namespaces
        String nsU,                         // URI of current namespace document
        ClassType ct,                       // create typedefs from this class
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U,            // URIs for builtin namespaces
        boolean augPointF) {                // include an augmentation point?
        
        if (!nsU.equals(ct.namespaceURI())) return;
        var appinfoPre    = bc2pre.get("APPINFO");
        var appinfoU      = bc2U.get("APPINFO");
        var proxyPre      = bc2pre.get("NIEM-XS");
        var proxyU        = bc2U.get("NIEM-XS");
        var structuresPre = bc2pre.get("STRUCTURES");

        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        var anE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        var ccE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexContent");
        var exE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
        var sqE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:sequence");

        populateTypeElement(doc, ctE, ct, refnsUs, bc2pre, bc2U);
        exE.appendChild(sqE);
        ccE.appendChild(exE);
        ctE.appendChild(ccE);

        var baseQ = structuresPre + ":";
        if (null != ct.subClass()) { 
            baseQ = ct.subClass().qname();
            refnsUs.add(ct.subClass().namespaceURI());
        }
        else if (ct.name().endsWith("AdapterType"))      baseQ = baseQ + "AdapterType";
        else if (ct.name().endsWith("AssociationType"))  baseQ = baseQ + "AssociationType";
        else if (ct.name().endsWith("AugmentationType")) baseQ = baseQ + "AugmentationType"; 
        else baseQ = baseQ + "ObjectType";            
        exE.setAttribute("base", baseQ);
        
        for (var pa : ct.propL()) {
            var p = pa.property();
            if (p.isAttribute()) continue;
            var elE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
            elE.setAttribute("ref", p.qname());
            if (!"1".equals(pa.minOccurs())) elE.setAttribute("minOccurs", pa.minOccurs());
            if (!"1".equals(pa.maxOccurs())) elE.setAttribute("maxOccurs", pa.maxOccurs());
            if (pa.isOrdered()) {
                elE.setAttributeNS(appinfoU, appinfoPre + ":" + "orderedPropertyIndicator", "true");
                refnsUs.add(appinfoU);
            }
            refnsUs.add(p.namespaceURI());
            addAnnotationDoc(doc, elE, pa.docL());
            sqE.appendChild(elE);
        }
        for (var ap : ct.anyL()) {
            if (ap.isAttribute()) continue;
            var anyE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:any");            
            if (!"1".equals(ap.minOccurs())) anyE.setAttribute("minOccurs", ap.minOccurs());
            if (!"1".equals(ap.maxOccurs())) anyE.setAttribute("maxOccurs", ap.maxOccurs());
            setAttribute(anyE, "processContents", ap.processCode());
            setAttribute(anyE, "namespace", ap.nsConstraint());
            sqE.appendChild(anyE);
        }
        if (augPointF) {
            var elE    = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
            var apname = replaceSuffix(ct.name(), "Type", "AugmentationPoint");
            elE.setAttribute("ref", ct.namespace().prefix() + ":" + apname);
            elE.setAttribute("minOccurs", "0");
            elE.setAttribute("maxOccurs", "unbounded");
            sqE.appendChild(elE);
            var apE    = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
            apE.setAttribute("name", apname);
            apE.setAttribute("abstract", "true");
            addAnnotationDoc(doc, apE, "An augmentation point for " + ct.name() + ".");
            decEL.add(apE);
        }
        for (var pa : ct.propL()) {
            var p = pa.property();
            if (!p.isAttribute()) continue;
            var atE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            var dp  = (DataProperty)pa.property();
            atE.setAttribute("ref", dp.qname());
            if ("1".equals(pa.minOccurs())) atE.setAttribute("use", "required");
            refnsUs.add(p.namespaceURI());
            addAnnotationDoc(doc, atE, pa.docL());
            exE.appendChild(atE);
        }
        for (var ap : ct.anyL()) {
            if (!ap.isAttribute()) continue;
            var anyE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:anyAttribute");            
            setAttribute(anyE, "processContents", ap.processCode());
            setAttribute(anyE, "namespace", ap.nsConstraint());
            exE.appendChild(anyE);
        }
        defEL.add(ctE);
    }

    // Create a complex type with simple content from a literal class object,
    // or a class derived from a literal class.
    protected void createCSCType (Document doc, 
        List<Element> eL,                   // add typedef elements to this list
        Set<String> refnsUs,                // URIs of referenced namespaces
        String nsU,                         // URI of current namespace document
        ClassType ct,                       // create typedefs from this class
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces

        if (!nsU.equals(ct.namespaceURI())) return;
        var appinfoPre    = bc2pre.get("APPINFO");
        var appinfoU      = bc2U.get("APPINFO");
        var proxyPre      = bc2pre.get("NIEM-XS");
        var proxyU        = bc2U.get("NIEM-XS");
        var structuresPre = bc2pre.get("STRUCTURES");
        
        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        var anE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        var scE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
        var exE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");

        populateTypeElement(doc, ctE, ct, refnsUs, bc2pre, bc2U);
        scE.appendChild(exE);
        ctE.appendChild(scE);

        // Add all the attribute references to xs:extension
        for (int i = 0; i < ct.propL().size(); i++) {
            var pa = ct.propL().get(i);
            var atE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            var dp  = (DataProperty)pa.property();
            if (!dp.isAttribute()) continue;
            atE.setAttribute("ref", dp.qname());
            if ("1".equals(pa.minOccurs())) atE.setAttribute("use", "required");
            refnsUs.add(dp.namespaceURI());
            addDocumentation(doc, atE, pa.docL());
            exE.appendChild(atE);
        }
        // Extension base may be a simple type
        var ctname = ct.qname();
        var dt   = ct.literalDatatype();
        if (simpleTypes.contains(dt)) {
            var agE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attributeGroup");
            var dtnsU = dt.namespaceURI();
            var dtQ   = dt.qname();
            if (!dtQ.endsWith("SimpleType"))
                dtQ = replaceSuffix(dtQ, "Type", "SimpleType");
            exE.setAttribute("base", dtQ);
            agE.setAttribute("ref", structuresPre + ":" + "SimpleObjectAttributeGroup");
            exE.appendChild(agE);
            refnsUs.add(dtnsU);
        }
        // Or the extension base may be another model class
        else if (null != dt && dt.namespace().isModelNS()) {
            var baseQ = dt.qname();
            exE.setAttribute("base", baseQ);
            refnsUs.add(dt.namespaceURI());
        }
        else if (null != ct.subClass()) {
            var baseQ = ct.subClass().qname();
            exE.setAttribute("base", baseQ);
            refnsUs.add(ct.subClass().namespaceURI());
        }
        // Or the extension base may be a XSD primitive
        else if (null != dt) {
            var baseQ = proxyPre + ":" + dt.name();
            exE.setAttribute("base", baseQ);
            refnsUs.add(proxyU);
        }   
        else LOG.error("Can't determine extension base for {}", ct.qname());
        eL.add(ctE);
    }
    
    // Create an xs:complexType element from a Datatype object.
    protected void createCSCType (Document doc, 
        List<Element> eL,                   // add typedef elements to this list
        Set<String> refnsUs,                // URIs of referenced namespaces
        String nsU,                         // URI of current namespace document
        Datatype dt,                        // create typedefs from this class
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces
        
        if (!nsU.equals(dt.namespaceURI())) return;
        if (W3C_XML_SCHEMA_NS_URI.equals(dt.namespaceURI())) return;
        if (XML_NS_URI.equals(dt.namespaceURI())) return;
        
        var appinfoPre    = bc2pre.get("APPINFO");
        var appinfoU      = bc2U.get("APPINFO");
        var proxyPre      = bc2pre.get("NIEM-XS");
        var proxyU        = bc2U.get("NIEM-XS");
        var structuresPre = bc2pre.get("STRUCTURES");
        
        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");       
        var scE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
        
        populateTypeElement(doc, ctE, dt, refnsUs, bc2pre, bc2U);
        ctE.appendChild(scE);
        if (dt.name().endsWith("SimpleType")) 
            setAttribute(ctE, "name", replaceSuffix(dt.name(), "SimpleType", "Type"));

        var atgE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attributeGroup");
        setAttribute(atgE, "ref", structuresPre + ":" + "SimpleObjectAttributeGroup");
        
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
            var bdtQ = proxifyQName(bdt, refnsUs, proxyPre, proxyU);
            addRestriction(doc, scE, refnsUs, r, bdtQ, bc2pre, bc2U);
        }
        eL.add(ctE);
    }
    
    // Create an xs:simpleType element from a Datatype object.
    protected void createSimpleType (Document doc, 
        List<Element> eL,                   // add typedef elements to this list
        Set<String> refnsUs,                // URIs of referenced namespaces
        String nsU,                         // URI of current namespace document
        Datatype dt,                        // create typedefs from this class
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces
        
        if (!nsU.equals(dt.namespaceURI())) return;
        if (W3C_XML_SCHEMA_NS_URI.equals(dt.namespaceURI())) return;
        if (XML_NS_URI.equals(dt.namespaceURI())) return;
        
        var stE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleType");
        populateTypeElement(doc, stE, dt, refnsUs, bc2pre, bc2U);
        if (!dt.name().endsWith("SimpleType")) 
            setAttribute(stE, "name", replaceSuffix(dt.name(), "Type", "SimpleType"));
        
        switch (dt.getType()) {
        case CMF_LIST:          addList(doc, stE, refnsUs, dt, bc2pre, bc2U); break;
        case CMF_RESTRICTION:   addRestriction(doc, stE, refnsUs, dt, bc2pre, bc2U); break;
        case CMF_UNION:         addUnion(doc, stE, refnsUs, dt, bc2pre, bc2U); break;
        }
        eL.add(stE);
    }
    
    protected void createDeclaration (Document doc, 
        List<Element> eL,                   // add declaration elements to this list
        Set<String> refnsUs,                // URIs of referenced namespaces
        String nsU,                         // URI of current namespace document
        Property p,                         // create declaration from this property
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U,            // URIs for builtin namespaces
        Map<String,String> pU2subQ) {       // ordinary property aug substitutionGroup QN
        
        if (!nsU.equals(p.namespaceURI())) return;
        if (W3C_XML_SCHEMA_NS_URI.equals(p.namespaceURI())) return;
        if (XML_NS_URI.equals(p.namespaceURI())) return;
        if (p.name().endsWith("Literal")) return;

        var appinfoPre = bc2pre.get("APPINFO");
        var appinfoU   = bc2U.get("APPINFO");      
        var proxyPre   = bc2pre.get("NIEM-XS");
        var proxyU     = bc2U.get("NIEM-XS");

        Element decE;
        if (p.isAttribute()) decE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        else decE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
        
        var ptQ = "";
        var pt  = p.type();
        if (null != pt) { 
            refnsUs.add(pt.namespaceURI());
            ptQ = pt.qname();
            if (!p.isAttribute() && W3C_XML_SCHEMA_NS_URI.equals(pt.namespaceURI())) {
                ptQ = proxyPre + ":" + pt.name();
                refnsUs.add(proxyU);
            }
            else if (p.isAttribute() && !ptQ.endsWith("SimpleType"))
                ptQ = replaceSuffix(ptQ, "Type", "SimpleType");
        }
        decE.setAttribute("name", p.name());
        setAttribute(decE, "type", ptQ);
        if (p.isAbstract()) decE.setAttribute("abstract", "true");
        if (!p.isAttribute()) decE.setAttribute("nillable", "true");
        if (p.isDeprecated()) {
            setAttribute(decE, appinfoU, appinfoPre + ":" + "deprecated", "true");
            refnsUs.add(appinfoU);
        }
        if (p.isRefAttribute()) {
            setAttribute(decE, appinfoU, appinfoPre + ":" + "referenceAttributeIndicator", "true");
            refnsUs.add(appinfoU);
        }
        if (!p.referenceCode().isEmpty()) {
            setAttribute(decE, appinfoU, appinfoPre + ":" + "referenceCode", p.referenceCode());
            refnsUs.add(appinfoU);
        }
        if (p.isRelationship()) {
            setAttribute(decE, appinfoU, appinfoPre + ":" + "relationshipPropertyIndicator", "true");
            refnsUs.add(appinfoU);
        }
        addAnnotationDoc(doc, decE, p.docL());
        
        var subQ = "";
        if (null != p.subProperty()) subQ = p.subProperty().qname();
        else subQ = pU2subQ.getOrDefault(p.uri(), "");
        if (!subQ.isEmpty() && !p.isAttribute()) {
            setAttribute(decE, "substitutionGroup", subQ);
            var subpre = qnToPrefix(subQ);
            var subnsU = prefixMap.getURI(subpre);
            refnsUs.add(subnsU);
        }
        eL.add(decE);        
    }
    
    protected void populateTypeElement (Document doc, 
        Element e,                          // xs:complexType or xs:simpleType
        Component c,                        // create typedefs from this component
        Set<String> refnsUs,                // URIs of referenced namespaces
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces
        
        var appinfoPre = bc2pre.get("APPINFO");
        var appinfoU   = bc2U.get("APPINFO"); 
        var anE        = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        
        if (null != c.codeListBinding()) {
            var clsaPre = bc2pre.get("CLSA");
            var clsaU   = bc2U.get("CLSA");
            var clb     = c.codeListBinding();
            var apE     = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:appinfo");
            var clE     = doc.createElementNS(clsaU, clsaPre + ":" + "SimpleCodeListBinding");
            setAttribute(clE, "codeListURI", clb.codeListURI());
            setAttribute(clE, "columnName", clb.column());
            if (clb.isConstraining()) setAttribute(clE, "constrainingIndicator", "true");
            refnsUs.add(clsaU);
            apE.appendChild(clE);
            anE.appendChild(apE);
        }
        addDocumentation(doc, anE, c.docL());
        if (anE.getChildNodes().getLength() > 0) e.appendChild(anE);
        
        setAttribute(e, "name", c.name());
        if (c.isAbstract())   e.setAttribute("abstract", "true");
        if (c.isDeprecated()) {
            setAttribute(e, appinfoU, appinfoPre + ":" + "deprecated", "true");
            refnsUs.add(appinfoU);
        }
        if (c.isOrdered()) {
            setAttribute(e, appinfoU, appinfoPre + ":" + "orderedPropertyIndicator", "true");        
            refnsUs.add(appinfoU);
        }
        if (!c.referenceCode().isEmpty()) {
            setAttribute(e, appinfoU, appinfoPre + ":" + "referenceCode", c.referenceCode());        
            refnsUs.add(appinfoU);
        }
    }
  
    protected void addList (Document doc, 
        Element e,                          // append xs:list to this element
        Set<String> refnsUs,                // URIs of referenced namespaces
        Datatype dt,                        // create typedefs from this class
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces

        var ldt = (ListType)dt;
        var idt = ldt.itemType();
        var iE  = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:list");
        setAttribute(iE, "itemType", datatypeQName(idt));
        e.appendChild(iE);
        refnsUs.add(idt.namespaceURI());
    }
    
    protected void addRestriction (Document doc, 
        Element e,                          // append xs:list to this element
        Set<String> refnsUs,                // URIs of referenced namespaces
        Datatype dt,                        // create typedefs from this class
        String baseQ,                       // QName of restriction base
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces
        
        var r   = (Restriction)dt;
        var rE  = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:restriction");
        var bdt = r.base();
        setAttribute(rE, "base", baseQ);
        refnsUs.add(bdt.namespaceURI());
        
        var fL = new ArrayList<>(r.facetL());
        Collections.sort(fL);
        for (var f : fL) {
            var fname = f.xsdFacetName();
            var fval  = f.value();
            var fE    = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:" + fname);
            fE.setAttribute("value", fval);
            if (!f.docL().isEmpty()) {
                var aE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
                addDocumentation(doc, aE, f.docL());
                fE.appendChild(aE);
            }
            rE.appendChild(fE);        
        }
        e.appendChild(rE);
    }
    
    protected void addRestriction (Document doc, 
        Element e,                          // append xs:list to this element
        Set<String> refnsUs,                // URIs of referenced namespaces
        Datatype dt,                        // create typedefs from this class
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces
        
        var r = (Restriction)dt;
        var bdt = r.base();
        var bdtQ = datatypeQName(bdt);
        addRestriction(doc, e, refnsUs, dt, bdtQ, bc2pre, bc2U);
    }
    
    protected void addUnion (Document doc, 
        Element e,                          // append xs:list to this element
        Set<String> refnsUs,                // URIs of referenced namespaces
        Datatype dt,                        // create typedefs from this class
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces

        var udt  = (Union)dt;
        var uE   = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:union");
        var mbrs = "";
        var sep  = "";
        for (var mdt : udt.memberL()) {
            mbrs = mbrs + sep + datatypeQName(mdt);
            sep = " ";
            refnsUs.add(mdt.namespaceURI());
        }
        setAttribute(uE, "memberTypes", mbrs);
        e.appendChild(uE);
    }
    
    protected void addLocalTerms (Document doc, Element appE, Namespace ns, String appPre, String appU) {
        for (var lt : ns.locTermL()) {
            var ltE = doc.createElementNS(appU, "appinfo:LocalTerm");
            ltE.setAttribute("term", lt.term());
            ltE.setAttribute("literal", lt.literal());
            setAttribute(ltE, "definition", lt.documentation());
            setAttribute(ltE, "sourceURIs", listToString(lt.sourceL()));
            for (var cit : lt.citationL()) {
                var citE = doc.createElementNS(appU, appPre + ":" + "SourceText");
                citE.setTextContent(cit.text());
                if (!"en-US".equals(cit.lang())) citE.setAttribute("xml:lang", cit.lang());
                ltE.appendChild(citE);
            }
            appE.appendChild(ltE);
        }
    }
    
    protected void addAnnotationDoc (Document doc, Element e, String s) {
        var ls = new LanguageString(s, "en-US");
        addAnnotationDoc(doc, e, List.of(ls));
    }
    
    protected void addAnnotationDoc (Document doc, Element e, List<LanguageString>docL) {
        if (docL.isEmpty()) return;
        var aE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        addDocumentation(doc, aE, docL);
        e.appendChild(aE);
    }
    
    protected void addDocumentation (Document doc, Element e, List<LanguageString>docL) {
        for (var ls : docL) {
            var dE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:documentation");
            dE.setTextContent(ls.text());
            if (!"en-US".equals(ls.lang())) dE.setAttribute("xml:lang", ls.lang());
            e.appendChild(dE);
        }
    }
    
    protected boolean hasSimpleContent (ClassType ct) {
        if (null != ct.literalDatatype()) return true;
        else if (null == ct.subClass()) return false;
        else return hasSimpleContent(ct.subClass());
    }
    
    protected String datatypeQName (Datatype dt) {
        if (!simpleTypes.contains(dt)) return dt.qname();
        else return replaceSuffix(dt.qname(), "Type", "SimpleType");
    }
    
    protected String proxifyQName (Datatype dt, Set<String> refnsUs, String proxyPre, String proxyU) {
        if (simpleTypes.contains(dt)) return replaceSuffix(dt.qname(), "Type", "SimpleType");
        else if (W3C_XML_SCHEMA_NS_URI.equals(dt.namespaceURI())) {
            refnsUs.add(proxyU);
            return proxyPre + ":" + dt.name();
        }
        else return dt.qname();
    }
    
    protected void setAttribute (Element e, String nsU, String qname, String value) {
        if (null != value && !value.isEmpty())
            e.setAttributeNS(nsU, qname, value);
    }
    
    protected void setAttribute (Element e, String name, String value) {
        if (null != value && !value.isEmpty())
            e.setAttribute(name, value);
    }

    protected ResourceManager rmgr = new ResourceManager(ModelToXSD.class);
    
    protected void writeVersionBuiltins (String vers, File outD) {
        for (var kcode : NamespaceKind.builtins()) {
            var nsU  = NamespaceKind.builtinNSU(vers, kcode);
            var vdir = NamespaceKind.versionDirName().get(vers);
            var rn   = NamespaceKind.builtinPath().get(kcode);
            var res  = "xsd/" + vdir + rn;
            var path = namespaceU2Path.get(nsU);
            var outF = new File(outD, path);
            var outP = outF.toPath().getParent();
            try {
                Files.createDirectories(outP);
                rmgr.copyResourceToFile(res, outF);
            } catch (IOException ex) {
                LOG.error("Can't create builtin schema documents for {}: {}", vers, ex.getMessage());
            }
        }
    }
    
    protected void writeXSD (Document doc, File outF) throws IOException {
        var pF   = outF.getParentFile();
        pF.mkdirs();
        var os = new FileOutputStream(outF);
        var ow = new OutputStreamWriter(os, "UTF-8");
        var xsdW = new XSDWriter();
        xsdW.writeXML(doc, ow);
        ow.close();
    }
    
    // Returns a file path string that is not in the set, munging the file
    // name as needed.
    public static String mungPath (Set<String> paths, String rpath) {
        if (!paths.contains(rpath)) return rpath;
        var dir  = FilenameUtils.getPath(rpath);
        var ext  = FilenameUtils.getExtension(rpath);
        var name = FilenameUtils.getBaseName(rpath);
        name = name.replaceAll("-\\d+$", "");       // assume -# suffix is a munging
        int mungCt = 0;
        if (!ext.isEmpty()) ext = "." + ext;
        while (true) {
            var mungBase = String.format("%s-%d", name, mungCt++);
            var mungName = mungBase + ext;
            rpath = dir + mungName;
            if (!paths.contains(rpath)) return rpath;
        }
    }
    
    public static String listToString (List<String> sL) {
        var res = "";
        var sep = "";
        for (var s : sL) {
            res = res + sep + s;
            sep = " ";
        }
        return res;
    }
    
    public static String setToString (Set<String> sL) {
        var res = "";
        var sep = "";
        for (var s : sL) {
            res = res + sep + s;
            sep = " ";
        }
        return res;
    }

    protected final DeclarationComparator declarationComparator = new DeclarationComparator();
    protected class DeclarationComparator implements Comparator<Element> {
        @Override
        public int compare(Element o1, Element o2) {
            int i = o1.getLocalName().compareTo(o2.getLocalName());
            if (i != 0) return i;
            var n1 = o1.getAttribute("name");
            var n2 = o2.getAttribute("name");
            return NaturalOrderComparator.comp(n1, n2);
        }
    }
    
    protected final DefinitionComparator definitionComparator = new DefinitionComparator();
    protected class DefinitionComparator implements Comparator<Element> {
        @Override
        public int compare(Element o1, Element o2) {
            var n1 = o1.getAttribute("name");
            var n2 = o2.getAttribute("name");
            return NaturalOrderComparator.comp(n1, n2);
        }
    }

    protected final ImportPairComparator importPairComparator = new ImportPairComparator();
    protected class ImportPairComparator implements Comparator<Pair<String,String>> {
        @Override
        public int compare (Pair<String,String> one, Pair<String,String> two) {
            var oneK = one.getValue0();
            var twoK = two.getValue0();
            return oneK.compareToIgnoreCase(twoK);
        }
    }


}
