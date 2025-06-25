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
import static org.apache.commons.lang3.StringUtils.uncapitalize;
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
import static org.mitre.niem.cmf.Component.uriToName;
import org.mitre.niem.cmf.DataProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.ListType;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.PropertyAssociation;
import org.mitre.niem.cmf.ReferenceGraph;
import org.mitre.niem.cmf.Restriction;
import org.mitre.niem.cmf.Union;
import static org.mitre.niem.utility.IndefiniteArticle.articalize;
import org.mitre.niem.utility.MapToList;
import org.mitre.niem.utility.MapToSet;
import org.mitre.niem.utility.NaturalOrderComparator;
import org.mitre.niem.utility.ResourceManager;
import org.mitre.niem.xml.LanguageString;
import org.mitre.niem.xml.ParserBootstrap;
import org.mitre.niem.xml.XMLCatalogCreator;
import org.mitre.niem.xml.XSDWriter;
import static org.mitre.niem.xsd.ModelFromXSD.replaceSuffix;
import static org.mitre.niem.xsd.NamespaceKind.NSK_APPINFO;
import static org.mitre.niem.xsd.NamespaceKind.NSK_CLSA;
import static org.mitre.niem.xsd.NamespaceKind.NSK_UNKNOWN;
import static org.mitre.niem.xsd.NamespaceKind.NSK_XML;
import static org.mitre.niem.xsd.NamespaceKind.NSK_XSD;
import static org.mitre.niem.xsd.NamespaceKind.builtinNSU;
import static org.mitre.niem.xsd.NamespaceKind.codeToKind;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class for writing a non-conforming schema document pile for validating
 * an instance of an XML message format.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXMLSchema {
    static final Logger LOG = LogManager.getLogger(ModelToXMLSchema.class);

    protected Model m;
    protected String useNiemVersion = null;
    protected String catalogPath = null;
    protected Namespace rootNS = null;
    protected final NamespaceMap prefixMap              = new NamespaceMap();   // all prefixes for namespaces in the model
    protected final Set<String> niemVersions            = new HashSet<>();      // all the NIEM version names in the model
    protected final Map<String,String> namespaceU2Path  = new HashMap<>();      // nsU -> file path in outD
    protected final Map<String,Integer> namespaceU2Kind = new HashMap<>();      // nsU -> namespace kind
    protected final Set<String> extNSs                  = new HashSet<>();      // URIs of external namespaces
    protected final MapToSet<String,String> subGroupL   = new MapToSet<>();     // propU -> set of substitutable propUs
    protected final Set<String> refNSs                  = new HashSet<>();      // URIs of referenced namespaces
    
    public ModelToXMLSchema (Model m) {
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
        buildSubstitutionMap();
        organizeAugmentations();
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
    
    // Construct the map of namespace URI to namespace kind code
    protected void collectNamespaceKinds () {
        for (var ns : m.namespaceSet()) {
            var nsU = ns.uri();
            var kcode = ns.kindCode();
            var kind  = NamespaceKind.codeToKind(kcode);
            namespaceU2Kind.put(nsU, kind);
        }
        for (var nver : niemVersions) {
            for (var kcode : NamespaceKind.builtins()) {
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
    
    // Update the substitution map with properties and subproperties
    protected void buildSubstitutionMap () {
        for (var subp : m.propertyL()) {
            var p = subp.subPropertyOf();
            if (null != p) subGroupL.add(p.qname(), subp.qname());
        }
    }
    
    // Create a list of augmentations for each augmented class,
    // and for each kind of global augmentation.
    protected MapToList<String,PropertyAssociation> ns2classAugs = new MapToList<>();
    protected MapToList<String,PropertyAssociation> globalCode2augs = new MapToList<>();
    protected MapToSet<String,String> ns2refAttUS = new MapToSet<>();
    protected void organizeAugmentations () {
        for (var ns : m.namespaceSet()) {
            for (var arec : ns.augL()) {
                var p   = arec.property();
                var ct  = arec.classType();
                var gcs = arec.codeS();
                if (null != ct) {
                    var attL = ns2classAugs.get(ct.uri());
                    addAugToPropList(attL, arec);
                    if (ct.isLiteralClass() && !p.isAttribute())
                        ns2refAttUS.add(ns.uri(), p.name());
                }
                for (var gc : gcs) {
                    var attL = globalCode2augs.get(gc);
                    addAugToPropList(attL, arec);
                    if ("LITERAL".equals(gc)) 
                        ns2refAttUS.add(ns.uri(), p.name());
                }
            }
        }
    }
    
    // Adds an augmentation record (which is derived from PropertyAssociation) to a
    // property list, but only if it isn't already there.  Also replaces an optional
    // augmentation with a required.
    protected void addAugToPropList (List<PropertyAssociation> lst, PropertyAssociation pa) {
        PropertyAssociation inset = null;
        var dpU = pa.property().uri();
        for (var spa : lst) {
            if (dpU.equals(spa.property().uri())) inset = spa;
        }
        if (null == inset) lst.add(pa);
        else if (inset.minOccursVal() == 0 && pa.minOccursVal() > 0) {
            lst.remove(inset);
            lst.add(pa);
        }
    }
   
   protected void addAttributeToPropList (List<PropertyAssociation> lst, List<PropertyAssociation> adds) {
       for (var pa : adds) addAugToPropList(lst, pa);
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
        var refnsUs    = new HashSet<String>();         // need prefixes and imports for these namespaces
        var defEL      = new ArrayList<Element>();      // list of type definition elements
        var decEL      = new ArrayList<Element>();      // list of attribute/element declaration elements
        
        // Create xs:annotation element; add namespace documentation
        var annE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        addDocumentation(doc, annE, ns.docL());
        if (annE.getChildNodes().getLength() > 0) root.appendChild(annE);
        
        // Message schema documents don't have conformance target assertions,
        // augmentation appinfo, or local terms.

        // Create augmentation types and elements for this namespace
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
        // Create simple types and attribute/element declarations.
        for (var dt : m.datatypeL()) createSimpleType(doc, defEL, refnsUs, nsU, dt, bc2pre, bc2U);
        for (var p : m.propertyL())  createDeclaration(doc, decEL, refnsUs, nsU, p, bc2pre, bc2U, pU2subQ);
        
        // Create reference attributes
        for (var name : ns2refAttUS.get(nsU)) {
            var rn = uncapitalize(name) + "Ref";
            var aE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            aE.setAttribute("name", rn);
            aE.setAttribute("type", "xs:IDREFS");
            addAnnotationDoc(doc, aE, "A list of references to " + name + " objects.");
            decEL.add(aE);
        }
        
        // Need appinfo if an external namespace is referenced
        var extF = false;
        for (var refnsU : refnsUs) 
            if (extNSs.contains(refnsU)) extF = true;
//        if (extF) refnsUs.add(appinfoU);            
        
        // At this point we know all of the referenced namespaces.
        // Create namespace declarations; add import elements in a pleasing order.
        refnsUs.add(nsU);
        refNSs.addAll(refnsUs);
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
//            if (extNSs.contains(refnsU))
//                impE.setAttributeNS(appinfoU, appinfoPre + ":" + "externalImportIndicator", "true");
            addAnnotationDoc(doc, impE, ns.idocL(refnsU));
            root.appendChild(impE);
        }
        
        Collections.sort(defEL, definitionComparator);
        Collections.sort(decEL, declarationComparator);
        for (var e : defEL) root.appendChild(e);
        for (var e : decEL) root.appendChild(e);        

        writeXSD(doc, outF);
    }
    
    // Create an augmentation type and element for each class augmented by
    // this namespace. Note the substitution group for each element property
    // augmentation. Update the substitution group map.
    protected void createAugmentationComponents (Document doc, 
        List<Element> defEL,                // add typedef elements to this list
        List<Element> decEL,                // add typedef elements to this list
        Set<String> refnsUs,                // URIs of referenced namespaces
        Namespace ns,                       // URI of current namespace document
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U,            // URIs for builtin namespaces    
        Map<String,String> pU2subQ) {       // ordinary property aug subsitutionGroup QN
        
        // Create a list of AugmentRecords for each Class augmented by this namespace.
        // Also establish the substitutionGroup for each element property augmentation.
        var classQ2ArecL = new MapToList<String,AugmentRecord>();
        for (var arec : ns.augL()) {
            var p = arec.property();
            if (null == arec.classType()) continue;     // do nothing here for global augmentation
            if (p.isAttribute()) continue;              // do nothing here for attribute augmentation
            var propU  = p.uri();
            var propQ  = p.qname();
            var classQ = arec.classType().qname();
            var augPQ  = replaceSuffix(classQ, "Type", "AugmentationPoint");
            if (arec.index().isEmpty()) {
                pU2subQ.put(propU, augPQ);              // element property augmentation
                subGroupL.add(augPQ, propQ);            // propQ is substitutable for augPQ
            }
            else classQ2ArecL.add(classQ, arec);        // arec is augmentation record for classQ
        }
        // Create augmentation type for each class augmented by this namespace.
        // The list of augmentation records, sorted by index number, becomes the 
        // property association list for the augmentation type.
        for (var classQ : classQ2ArecL.keySet()) {
            var augmct = m.qnToClassType(classQ);       // augmented class
            var propL  = classQ2ArecL.get(classQ);
            var augptQ = replaceSuffix(classQ, "Type", "AugmentationPoint");
            var cname  = qnToName(classQ);
            var cnoun  = articalize(replaceSuffix(cname, "Type", "").toLowerCase());
            var aename = replaceSuffix(cname, "Type", "Augmentation");
            var atname = aename + "Type";
            var docstr = "A data type for additional information about " + cnoun + ".";
            var augct  = new ClassType(ns, atname);
            Collections.sort(propL);
            augct.addDocumentation(docstr, "en-US");
            augct.propL().addAll(propL);
            createCCCType(doc, defEL, decEL, refnsUs, ns.uri(), augct, bc2pre, bc2U, false);
            
            // Create the augmentation element to go with the augmentation type.
            // No substitionGroup in a message schema
            docstr   = "Additional information about " + cnoun + ".";
            var augE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
            var docL = List.of(new LanguageString(docstr, "en-US"));
            augE.setAttribute("name", aename);
            augE.setAttribute("type", augct.qname());;
            addAnnotationDoc(doc, augE, docL);
            refnsUs.add(augmct.namespaceURI());
            decEL.add(augE);
            subGroupL.add(augptQ, ns.prefix() + ":" + aename);
        }
    }
    
    // Create a complex type with complex content from a non-literal class object
    private static Set<String> needURIcodes = Set.of("ANY", "ANYURI", "INTERNAL", "RELURI");
    private static Set<String> needRefcodes = Set.of("ANY", "INTERNAL", "IDREF");
    private static Set<String> needMetadata = Set.of("NIEM2.0", "NIEM3.0", "NIEM4.0", "NIEM5.0");
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
        var ns  = m.namespaceObj(nsU);    
        var ver = ns.niemVersion();
        if (null != useNiemVersion) ver = useNiemVersion;
        
        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        var anE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        var ccE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexContent");
        var exE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
        var sqE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:sequence");

        // Set xs:complexType name; add documentation.
        populateTypeElement(doc, ctE, ct, refnsUs, bc2pre, bc2U);
        
        // Can't extend parent class if reference codes are not compatible.
        var pct      = ct.subClassOf();
        var refCode  = ct.effectiveReferenceCode();
        var needURI  = needURIcodes.contains(refCode);
        var needRef  = needRefcodes.contains(refCode);
        var extendF  = false;
        if (null != pct) {
            var prefCode = pct.effectiveReferenceCode();
            var pNeedURI = needURIcodes.contains(prefCode);
            var pNeedRef = needRefcodes.contains(prefCode);
            extendF = true;
            if (pNeedURI && !needURI) extendF = false;
            if (pNeedRef && !needRef) extendF = false;
            if (extendF) {
                needURI = needURI && !pNeedURI;
                needRef = needRef && !pNeedRef;
            }
        }            
        // Need xs:complexContent and xs:extension elements if we are extending a parent class.
        // Otherwise just need the xs:sequence element.
        var attParentE = ctE;
        if (extendF) {
            refnsUs.add(pct.namespaceURI());
            exE.setAttribute("base", pct.qname());
            exE.appendChild(sqE);
            ccE.appendChild(exE);
            ctE.appendChild(ccE);
            attParentE = exE;
        }
        else ctE.appendChild(sqE);
        
        // Start with list of inherited properties if we can't extend from a parent class.
        // Then add properties from this class.
        var propL = new ArrayList<PropertyAssociation>();
        if (!extendF && null != pct) getParentProperties(pct, propL);
        propL.addAll(ct.propL());
        
        // Add augmentation point if needed
        if (augPointF) {
            var name = replaceSuffix(ct.name(), "Type", "AugmentationPoint");
            var pdoc = "An augmentation point for " + ct.name() + ".";
            var p    = new ObjectProperty(ns, name);
            var pa   = new PropertyAssociation();
            p.setIsAbstract(true);
            p.addDocumentation(pdoc, "en-US");
            pa.setProperty(p);
            pa.setMinOccurs("0");
            pa.setMaxOccurs("unbounded");
            propL.add(pa);
        }        
        // Add xs:element refs for all object property children.
        // Omit abstract elements with no substitutions.
        // Insert xs:choice if more than one substitution.
        for (var pa : propL) {
            var p    = pa.property();
            var pQ   = p.qname();
            var subs = subGroupL.get(pQ);
            if (p.isAttribute()) continue;
            if (!p.isAbstract()) subs.add(pQ);
            var parE = sqE;
            if (subs.size() > 1) {
                parE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:choice");
                if (!"1".equals(pa.minOccurs())) parE.setAttribute("minOccurs", pa.minOccurs());
                if (!"1".equals(pa.maxOccurs())) parE.setAttribute("maxOccurs", pa.maxOccurs());
                addAnnotationDoc(doc, parE, pa.docL());
                sqE.appendChild(parE);
            }
            for (var spQ : subs) {
                var ens = m.namespaceObj(qnToPrefix(spQ));
                var elE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
                elE.setAttribute("ref", spQ);
                if (subs.size() == 1) {
                    if (!"1".equals(pa.minOccurs())) elE.setAttribute("minOccurs", pa.minOccurs());
                    if (!"1".equals(pa.maxOccurs())) elE.setAttribute("maxOccurs", pa.maxOccurs());
                    addAnnotationDoc(doc, elE, pa.docL());
                }
                parE.appendChild(elE);
                refnsUs.add(ens.uri());
            }
        }
        // Add xs:any wildcards as needed
        for (var ap : ct.anyL()) {
            if (ap.isAttribute()) continue;
            var anyE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:any");            
            if (!"1".equals(ap.minOccurs())) anyE.setAttribute("minOccurs", ap.minOccurs());
            if (!"1".equals(ap.maxOccurs())) anyE.setAttribute("maxOccurs", ap.maxOccurs());
            setAttribute(anyE, "processContents", ap.processCode());
            setAttribute(anyE, "namespace", ap.nsConstraint());
            sqE.appendChild(anyE);
        }
        // Add xs:attribute refs.  Start with attributes in this class.  Then
        // add agumentations not already present.  Then add any global augmentations.
        var apropL = new ArrayList<>(ct.propL());
        addAttributeToPropList(apropL, ns2classAugs.get(ct.uri()));
        if (ct.isAssociationClass()) addAttributeToPropList(apropL, globalCode2augs.get("ASSOCIATION"));
        if (ct.isObjectClass())      addAttributeToPropList(apropL, globalCode2augs.get("OBJECT"));
        for (var pa : apropL) {
            var p = pa.property();
            if (!p.isAttribute()) continue;
            var atE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            var dp  = (DataProperty)pa.property();
            atE.setAttribute("ref", dp.qname());
            if ("1".equals(pa.minOccurs())) atE.setAttribute("use", "required");
            refnsUs.add(p.namespaceURI());
            addAnnotationDoc(doc, atE, pa.docL());
            attParentE.appendChild(atE);
        }
        // Add xs:anyAttribute wildcards as needed
        for (var ap : ct.anyL()) {
            if (!ap.isAttribute()) continue;
            var anyE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:anyAttribute");            
            setAttribute(anyE, "processContents", ap.processCode());
            setAttribute(anyE, "namespace", ap.nsConstraint());
            attParentE.appendChild(anyE);
        }
        // Add reference attributes as needed
        var structuresPre = bc2pre.get("STRUCTURES");
        var structuresU   = bc2U.get("STRUCTURES");
        if (needURI || needRef)
            addStructuresAttribute(doc, attParentE, "id", refnsUs, structuresPre, structuresU);
        if (needURI)
            addStructuresAttribute(doc, attParentE, "uri", refnsUs, structuresPre, structuresU);
        if (needRef)
            addStructuresAttribute(doc, attParentE, "ref", refnsUs, structuresPre, structuresU);
        if (null == pct && needMetadata.contains(ver))
            addStructuresAttribute(doc, attParentE, "metadata", refnsUs, structuresPre, structuresU);
        if (null == pct)
            addStructuresAttribute(doc, attParentE, "appliesToParent", refnsUs, structuresPre, structuresU);
        defEL.add(ctE);
    }
    
    protected void addStructuresAttribute (Document doc, 
        Element parE, String name, 
        Set<String> refnsUs,
        String spre, String sU) {
        
        var refE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        refE.setAttribute("ref", spre + ":" + name);
        parE.appendChild(refE);
        refnsUs.add(sU);   
    }
    
    // Create a list of property associations for a class hierarchy, beginning
    // with the top of the inheritance chain.
    protected void getParentProperties (ClassType pct, List<PropertyAssociation> propL) {
        if (null != pct.subClassOf()) getParentProperties(pct.subClassOf(), propL);
        propL.addAll(pct.propL());
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
        
        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        var anE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        var scE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
        var exE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");

        populateTypeElement(doc, ctE, ct, refnsUs, bc2pre, bc2U);
        scE.appendChild(exE);
        ctE.appendChild(scE);

        // Add all the attribute references to xs:extension.  Start with attributes
        // in this class.  Then add augmentations not already present.  Then add
        // any global augmentations.  If the augmentation is an object property,
        // create and add a reference attribute instead.
        var apropL = new ArrayList<>(ct.propL());
        addAttributeToPropList(apropL, ns2classAugs.get(ct.uri()));
        addAttributeToPropList(apropL, globalCode2augs.get("LITERAL"));        
        for (var pa : apropL) {
            var p    = pa.property();
            var refQ = p.qname();
            if (refQ.endsWith("Literal")) continue;
            if (!p.isAttribute()) {
                var refp = qnToPrefix(refQ);
                var refn = qnToName(refQ);
                refQ = refp + ":" + uncapitalize(refn) + "Ref";
            }
            var atE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            atE.setAttribute("ref", refQ);
            if ("1".equals(pa.minOccurs())) atE.setAttribute("use", "required");
            refnsUs.add(p.namespaceURI());
            addDocumentation(doc, atE, pa.docL());
            exE.appendChild(atE);
        }
        // Extension base may be a simple type
        var ctname = ct.qname();
        var dt   = ct.literalDatatype();
        if (simpleTypes.contains(dt)) {
//            var dtnsU = dt.namespaceURI();
//            var dtQ   = dt.qname();
//            if (!dtQ.endsWith("SimpleType"))
//                dtQ = replaceSuffix(dtQ, "Type", "SimpleType");
//            exE.setAttribute("base", dtQ);
//            refnsUs.add(dtnsU);
        }
        // Or the extension base may be another model class
        else if (null != dt && dt.namespace().isModelNS()) {
            var baseQ = dt.qname();
            exE.setAttribute("base", baseQ);
            refnsUs.add(dt.namespaceURI());
        }
        else if (null != ct.subClassOf()) {
            var baseQ = ct.subClassOf().qname();
            exE.setAttribute("base", baseQ);
            refnsUs.add(ct.subClassOf().namespaceURI());
        }
        // Or the extension base may be a XSD primitive
        else if (null != dt) {
            exE.setAttribute("base", dt.qname());
        }   
        else LOG.error("Can't determine extension base for {}", ct.qname());
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

        Element decE;
        if (p.isAttribute()) decE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        else decE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
        
        var ptQ = "";
        var pt  = p.type();
        if (null != pt) { 
            refnsUs.add(pt.namespaceURI());
            ptQ = pt.qname();
            if (p.isAttribute() && !ptQ.endsWith("SimpleType"))
                ptQ = replaceSuffix(ptQ, "Type", "SimpleType");
        }
        // No abstract, appinfo, or substitionGroup in a message schema
        decE.setAttribute("name", p.name());
        setAttribute(decE, "type", ptQ);
        if (!p.isAttribute()) decE.setAttribute("nillable", "true");
        addAnnotationDoc(doc, decE, p.docL());
        eL.add(decE);
    }
    
    protected void populateTypeElement (Document doc, 
        Element e,                          // xs:complexType or xs:simpleType
        Component c,                        // create typedefs from this component
        Set<String> refnsUs,                // URIs of referenced namespaces
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces
         
        var anE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        addDocumentation(doc, anE, c.docL());
        if (anE.getChildNodes().getLength() > 0) e.appendChild(anE);
        e.setAttribute("name", c.name());
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
        else if (null == ct.subClassOf()) return false;
        else return hasSimpleContent(ct.subClassOf());
    }
    
    protected String datatypeQName (Datatype dt) {
        if (!simpleTypes.contains(dt)) return dt.qname();
        else return replaceSuffix(dt.qname(), "Type", "SimpleType");
    }
    
    // Don't convert xs types to niem-xs types in a message schema
    protected String proxifyQName (Datatype dt, Set<String> refnsUs, String proxyPre, String proxyU) {
        if (simpleTypes.contains(dt)) return replaceSuffix(dt.qname(), "Type", "SimpleType");
        return dt.qname();
    }
    
    protected void setAttribute (Element e, String nsU, String qname, String value) {
        if (null != value && !value.isEmpty())
            e.setAttributeNS(nsU, qname, value);
    }
    
    protected void setAttribute (Element e, String name, String value) {
        if (null != value && !value.isEmpty())
            e.setAttribute(name, value);
    }

    protected ResourceManager rmgr = new ResourceManager(ModelToXSDModel.class);
    
    // Write builtin schema documents for the specified NIEM version.
    // Only write builtins and proxy types that are used in the model.
    protected void writeVersionBuiltins (String vers, File outD) {
        for (var kcode : NamespaceKind.builtins()) {
            var nsU  = NamespaceKind.builtinNSU(vers, kcode);
            if (!refNSs.contains(nsU)) continue;
            
            var vdir = NamespaceKind.versionDirName().get(vers);
            var rn   = NamespaceKind.builtinPath().get(kcode);
            var res  = "/xsd/" + vdir + rn;
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
