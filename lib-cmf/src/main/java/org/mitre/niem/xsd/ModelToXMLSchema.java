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
import static org.apache.commons.lang3.StringUtils.capitalize;
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
import static org.mitre.niem.cmf.Component.makeQN;
import static org.mitre.niem.cmf.Component.makeURI;
import static org.mitre.niem.cmf.Component.qnToName;
import static org.mitre.niem.cmf.Component.qnToPrefix;
import static org.mitre.niem.cmf.Component.uriToName;
import static org.mitre.niem.cmf.Component.uriToNamespace;
import org.mitre.niem.cmf.DataProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.ListType;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
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
        processAugmentations();
        
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
            if (null != p) subGroupL.add(p.uri(), subp.uri());
        }
    }

    // Augmentation records, indexed by augmenting namespace URI, then class URI.
    // Global augmentations have a fake class URI:  "Association", "Literal", or "Object".
    protected Map<String,MapToList<String,AugmentRecord>> nsAugs = new HashMap<>();
    
    // Augmentation records from every namespace, indexed by class URI.
    // Same fake URIs for globals.
    protected MapToList<String,PropertyAssociation> ctU2augL = new MapToList<>();
    
    // Map of namespace URI to set of reference attribute names for that NS
    protected MapToSet<String,String> nsU2refAttNS = new MapToSet<>();
    
    // Dummy property associations for global augmentation points
    protected PropertyAssociation assAugPA = new PropertyAssociation();
    protected PropertyAssociation objAugPA = new PropertyAssociation();
    
    // Process every augmentation record in every namespace to create
    // the data structures above.  Need them for writeModelDocument.
    protected void processAugmentations () {
        for (var ns : m.namespaceSet()) {     
            var nsU = ns.uri();                                 // http://AugmentingNS/
            var nsctU2augL = new MapToList<String,AugmentRecord>();
            nsAugs.put(nsU, nsctU2augL);
            
            // Iterate through all augmentations in this namespace
            for (var arec : ns.augL()) {
                var actU = "";
                var ct   = arec.classType();                    // augmented BarType or null
                var p    = arec.property();                     // http://FooNS/Property
                var pnsU = p.namespaceURI();                    // http://FooNS/
                var raN  = uncapitalize(p.name()) + "Ref";      // propertyRef
                var raU  = makeURI(pnsU, raN);                  // http://FooNS/propertyRef
                var gcs  = new HashSet<>(arec.codeS());
                if (null != ct) gcs.add("CLASS");
                for (var gc : gcs) {
                    switch (gc) {
                    case "CLASS":
                        actU = ct.uri();                                // http://BarNS/BarType (can't be null)
                        if (ct.isLiteralClass() && !p.isAttribute()) {  // this is simple content & object augmentation
                            nsU2refAttNS.add(pnsU, raN);                // so FooNS needs a ref attribute for p
                        }
                        break;
                    case "LITERAL":     actU = "Literal"; nsU2refAttNS.add(pnsU, raN); break;
                    case "ASSOCIATION": actU = "Association"; break;
                    case "OBJECT":      actU = "Object";  break;
                    }
                    // Establish substitution for augmentation not part of augmentation type
                    if (!"Literal".equals(actU) && !p.isAttribute() && arec.index().isEmpty()) {
                        var apU = replaceSuffix(actU, "Type", "");      // http://BarNS/Bar or Object
                        apU = apU + "AugmentationPoint";                // http://BarNS/BarAugmentationPoint
                        subGroupL.add(apU, p.uri());                    // or ObjectAugmentationPoint
                    }
                    nsctU2augL.add(actU, arec);     // add aug rec to class augs from this NS
                    ctU2augL.add(actU, arec);       // add aug rec to class augs from all NSs
                }
            }
        }
        // Establlish substitutions for augmentation elements
        for (var nsU : nsAugs.keySet()) {                   // http://AugmentingNS/
            var nsctU2augL = nsAugs.get(nsU);
            for (var actU : nsctU2augL.keySet()) {          // http://BarNS/BarType or Object
                var actnsU = uriToNamespace(actU);          // http://BarNS/ or ""
                var actN = uriToName(actU);                 // BarType or ""
                actN = replaceSuffix(actN, "Type", "");     // Bar or ""
                var aptU = "";
                if (actN.isEmpty()) {
                    actN = actU;                            // Object
                    aptU = actN + "AugmentationPoint";      // ObjectAugmentationPoint
                }
                else {
                    aptU = makeURI(actnsU, actN);           // http://BarNS/Bar or Object
                    aptU = aptU + "AugmentationPoint";      // http://BarNS/BarAugmentationPoint or ObjectAugmentationPoint
                }
                var aeU = makeURI(nsU, actN);               // http://SomeNS/Bar or http://SomeNS/Object
                aeU = aeU + "Augmentation";                 // http://SomeNS/BarAugmentation or http://SomeNS/ObjectAugmentation
                subGroupL.add(aptU, aeU);                   // augmentation element substitutes for augmentation point
            }
        }
        // Create global augmentation points, but don't add to model.
        var assAugP = new Property(null, "AssociationAugmentationPoint");
        var objAugP = new Property(null, "ObjectAugmentationPoint");
        assAugP.setIsAbstract(true);
        objAugP.setIsAbstract(true);
        assAugPA.setProperty(assAugP);
        assAugPA.setMinOccurs("0");
        assAugPA.setMaxOccurs("unbounded");
        objAugPA.setProperty(objAugP);
        objAugPA.setMinOccurs("0");
        objAugPA.setMaxOccurs("unbounded");        
    }
    
    // Adds a PropertyAssociation to a property list, but only if it 
    // isn't already there.  Also replaces an optional property with a required.
    protected void addToPropList (List<PropertyAssociation> lst, PropertyAssociation pa) {
        PropertyAssociation inset = null;
        var dpU = pa.property().uri();
        for (var spa : lst) {
            var spU = spa.property().uri();
            if (dpU.equals(spU)) inset = spa;
        }
        if (null == inset) lst.add(pa);
        else if (inset.minOccursVal() == 0 && pa.minOccursVal() > 0) {
            lst.remove(inset);
            lst.add(pa);
        }
    }
   
    protected void addToPropList (List<PropertyAssociation> lst, List<PropertyAssociation> adds) {
       for (var pa : adds) addToPropList(lst, pa);
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

        // Create augmentation components for this namespace
        var pU2subQ = new HashMap<String,String>();
        createAugmentationComponents(doc, defEL, decEL, refnsUs, ns, bc2pre, bc2U);

        // Create complex types for literal classes and ordinary classes.
        var xctUs = new HashSet<String>();
        for (var ct : m.classTypeL()) {
            if (ct.hasSimpleContent()) createCSCType(doc, defEL, refnsUs, nsU, ct, bc2pre, bc2U);
            else createCCCType(doc, defEL, decEL, refnsUs, nsU, ct, bc2pre, bc2U);
            xctUs.add(ct.uri());
        }
        // Create simple types and attribute/element declarations.
        for (var dt : m.datatypeL()) createSimpleType(doc, defEL, refnsUs, nsU, dt, bc2pre, bc2U);
        for (var p : m.propertyL())  createDeclaration(doc, decEL, refnsUs, nsU, p, bc2pre, bc2U, pU2subQ);         
        
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
    // this namespace.
    protected void createAugmentationComponents (Document doc, 
        List<Element> defEL,                // add typedef elements to this list
        List<Element> decEL,                // add typedef elements to this list
        Set<String> refnsUs,                // URIs of referenced namespaces
        Namespace ns,                       // URI of current namespace document
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces    
        
        // Create augmentation type and augmentation point for each class 
        // augmented by this namespace
        var nsU = ns.uri();                                     // http://AugmentingNS/
        var nsctU2augL = nsAugs.get(nsU);
        for (var actU : nsctU2augL.keySet()) {                  // http://BarNS/BarType or Object
            var actnsU = "";
            var baseN = "";                                     // Bar or Object
            switch (actU) {
                case "Association": baseN = "Association"; break;
                case "Object":      baseN = "Object"; break;
                case "LITERAL":     continue;
                default:                                        // http://BarNS/BarType
                    actnsU = uriToNamespace(actU);              // http://BarNS/
                    baseN   = uriToName(actU);                  // BarType
                    baseN   = replaceSuffix(baseN, "Type", ""); // Bar
            }
            var aeN = baseN + "Augmentation";                   // BarAugmentation or ObjectAugmentation
            var atN = aeN + "Type";                             // BarAugmentationType or ObjectAugmentationType
            var apN = aeN + "Point";                            // BarAugmentationPoint or ObjectAugmentationPoint
            
            var aeDoc = "Additional information about " + articalize(baseN).toLowerCase() + ".";
            var atDoc = "A data type for additional information about " + articalize(baseN).toLowerCase() + ".";
            
            // Create dummy ClassType for the augmentation type; use it to
            // create the CCC type definition.
            var augct = new ClassType(ns, atN);                 // http://AugmentingNS/BarAugmentationType
            var propL = new ArrayList<PropertyAssociation>();
            for (var prop : ctU2augL.get(actU)) {
                if (!prop.index().isEmpty()) propL.add(prop);
            }
            Collections.sort(propL);
            augct.addDocumentation(atDoc, "en-US");
            augct.propL().addAll(propL);
            createCCCType(doc, defEL, decEL, refnsUs, nsU, augct, bc2pre, bc2U);
            
            // Create the augmentation element to go with the augmentation type.
            var augE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
             augE.setAttribute("name", aeN);                     // BarAugmentation or ObjectAugmentation
            augE.setAttribute("type", augct.qname());           // http://AugmentingNS/BarAugmentationType
            addAnnotationDoc(doc, augE, aeDoc);
            if (!actnsU.isEmpty()) refnsUs.add(actnsU);
            decEL.add(augE);
            
//            // Augmentation element substitutes for augmentation point
//            var aeU = makeURI(nsU, aeN);                        // http://AugmentingNS/BarAugmentation
//            var apU = makeURI(actnsU, apN);                     // http://BarNS/BarAugmentationPoint
//            if (actnsU.isEmpty()) apU = apN;                    // or ObjectAugmentationPoint
//            subGroupL.add(apU, aeU);
        }
        // Create reference attributes needed in this namespace
        for (var raN : nsU2refAttNS.get(nsU)) {                 // propertyRef
            var raE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            raE.setAttribute("name", raN);
            raE.setAttribute("type", "xs:IDREFS");
            decEL.add(raE);
        }
    }
    
    // Create a complex type with complex content from a non-literal class object
    private static Set<String> needURIcodes = Set.of("ANY", "ANYURI", "INTERNAL", "RELURI");
    private static Set<String> needRefcodes = Set.of("ANY", "INTERNAL", "IDREF");
    private static Set<String> needMetadata = Set.of("NIEM2.0", "NIEM3.0", "NIEM4.0", "NIEM5.0");
    //private static Set<String> needMetadata = Set.of("NIEM2.0", "NIEM3.0", "NIEM3.1", "NIEM3.2", "NIEM4.0", "NIEM4.1", "NIEM4.2", "NIEM5.0", "NIEM5.1", "NIEM5.2");
    protected void createCCCType (Document doc, 
        List<Element> defEL,                // add typedef elements to this list
        List<Element> decEL,                // add augmentation point elements to this list
        Set<String> refnsUs,                // URIs of referenced namespaces
        String nsU,                         // URI of current namespace document
        ClassType ct,                       // create typedefs from this class
        Map<String,String> bc2pre,          // prefixes for builtin namespaces
        Map<String,String> bc2U) {          // URIs for builtin namespaces
        
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
        
        // Not inheriting?  Then add dummy association for global augmentation point,
        // followed by parent properties.
        var ctU = ct.uri();
        var propL = new ArrayList<PropertyAssociation>();
        if (!extendF) {
            if (ct.isAssociationClass()) propL.add(assAugPA);
            if (ct.isObjectClass()) propL.add(objAugPA);
            if (null != pct) getParentProperties(pct, propL);
        }
        propL.addAll(ct.propL());
        
        if (ct.isAssociationClass() || ct.isObjectClass()) {
            var ctN = replaceSuffix(ct.name(), "Type", "");
            var augPA = new PropertyAssociation();
            var augp = new Property(ct.namespace(), ctN + "AugmentationPoint");
            augp.setIsAbstract(true);
            augPA.setProperty(augp);
            augPA.setMinOccurs("0");
            augPA.setMaxOccurs("unbounded");
            propL.add(augPA);
        }
              
        // Add xs:element refs for all object property children.
        // Omit abstract elements with no substitutions.
        // Insert xs:choice if more than one substitution.
        for (var pa : propL) {
            Set<String> choiceUs = null;
            var p    = pa.property();
            var pQ   = p.qname();
            if (p.isAttribute()) continue;
            if (null == p.namespace()) {
                choiceUs = subGroupL.get(p.name());                 // ObjectAugmentationPoint
            }
            else {                                              
                choiceUs = subGroupL.get(p.uri());
                if (!p.isAbstract()) choiceUs.add(p.uri());
            }
            // Append element refs to xs:choice if more than one choice
            var parE = sqE;
            if (choiceUs.size() > 1) {
                parE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:choice");
                if (!"1".equals(pa.minOccurs())) parE.setAttribute("minOccurs", pa.minOccurs());
                if (!"1".equals(pa.maxOccurs())) parE.setAttribute("maxOccurs", pa.maxOccurs());
                addAnnotationDoc(doc, parE, pa.docL());
                sqE.appendChild(parE);
            }
              for (var spU : choiceUs) {                          // 
                var spnsU = uriToNamespace(spU);
                var spQ   = m.uriToQN(spU);
                var elE  = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
                elE.setAttribute("ref", spQ);
                if (choiceUs.size() == 1) {
                    if (!"1".equals(pa.minOccurs())) elE.setAttribute("minOccurs", pa.minOccurs());
                    if (!"1".equals(pa.maxOccurs())) elE.setAttribute("maxOccurs", pa.maxOccurs());
                    addAnnotationDoc(doc, elE, pa.docL());
                }
                parE.appendChild(elE);
                refnsUs.add(spnsU);
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
        var apropL = new ArrayList<PropertyAssociation>();
        for (var pa : ct.propL())
            if (pa.property().isAttribute()) apropL.add(pa);
        
        addToPropList(apropL, ctU2augL.get(ct.uri()));
        if (ct.isAssociationClass()) addToPropList(apropL, ctU2augL.get("Association"));
        if (ct.isObjectClass())      addToPropList(apropL, ctU2augL.get("Object"));
        for (var pa : apropL) {
            var p  = pa.property();
            var pQ = p.qname();                                     // pre:SomeProperty
            if (!p.isAttribute()) {
                if (ct.isLiteralClass() || pa.codeS().contains("LITERAL")) {
                    var pre = qnToPrefix(pQ);                       // pre
                    var pN  = qnToName(pQ);                         // SomeProperty
                    pQ = makeQN(pre, uncapitalize(pN) + "Ref");     // pre:somePropertyRef
                }
                else continue;
            }
            else if (!pa.index().isEmpty()) continue;   // aug attribute in an aug type
            
            var atE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            atE.setAttribute("ref", pQ);
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
        if (!extendF && needMetadata.contains(ver))
            addStructuresAttribute(doc, attParentE, "metadata", refnsUs, structuresPre, structuresU);
        if (!extendF)
            addStructuresAttribute(doc, attParentE, "appliesToParent", refnsUs, structuresPre, structuresU);
        defEL.add(ctE);
    }
    
    protected void addStructuresAttribute (Document doc, 
        Element parE, String name, 
        Set<String> refnsUs,
        String spre, String sU) {
        
        var refE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        refE.setAttribute("ref", makeQN(spre, name));
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
        var ctU = ct.uri();
        
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
        var classAugL = ctU2augL.get(ctU);
        var glitAugL  = ctU2augL.get("Literal");
        var apropL = new ArrayList<>(ct.propL());
        addToPropList(apropL, classAugL);
        addToPropList(apropL, glitAugL); 
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
        if (p.isAbstract()) return;

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
        if (!p.isAttribute() && !p.name().endsWith("Augmentation")) 
            decE.setAttribute("nillable", "true");
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
