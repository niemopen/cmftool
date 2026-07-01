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
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import static org.apache.commons.lang3.StringUtils.capitalize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.mitre.niem.cmf.AugmentRecord;
import static org.mitre.niem.cmf.CMFObject.CMF_LIST;
import static org.mitre.niem.cmf.CMFObject.CMF_UNION;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.ListType;
import org.mitre.niem.cmf.Model;
import static org.mitre.niem.cmf.Model.uriToName;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.NamespaceMap;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.ReferenceGraph;
import org.mitre.niem.cmf.Restriction;
import org.mitre.niem.cmf.Union;
import static org.mitre.niem.utility.IndefiniteArticle.articalize;
import org.mitre.niem.utility.MapToList;
import org.mitre.niem.utility.NaturalOrderIgnoreCaseComparator;
import org.mitre.niem.utility.ResourceManager;
import static org.mitre.niem.utility.StringUtils.replaceSuffix;
import org.mitre.niem.utility.UniquePathSet;
import org.mitre.niem.xml.LanguageString;
import org.mitre.niem.xml.ParserBootstrap;
import org.mitre.niem.xml.XMLCatalogCreator;
import static org.mitre.niem.xsd.NamespaceKind.NSK_UNKNOWN;
import static org.mitre.niem.xsd.NamespaceKind.NSK_XSD;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class for code common to writing the XSD representation of a model
 * and writing an XSD message schema.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public abstract class ModelToXSD {
    private static final Logger LOG = LogManager.getLogger(ModelToXSD.class);
    
    // These object globals are computed once and used for every 
    // schema document in the pile.
    protected Model m;
    protected String catalogPath = null;        // write XML catalog here
    protected Namespace rootNS = null;          // root namespace gets extra imports, maybe
    protected Map<String,String> pathSpec;      // user specified nsU -> file path in outD
    protected String useArchVersion = null;     // ignore versions in model, use this one
    protected Set<String> archVersions;         // all versions found in model
    protected Map<String,Integer> nsU2KindVal;  // namespace URI -> namespace kind value      
    protected Map<String,String> nsU2Path;      // namespace URI -> relative path in document pile
    protected Set<Datatype> simpleTypes;        // need xs:simpleType for these datatypes
    protected Set<String> externalNSUs;         // URIs of all external namespaces
    protected Set<String> allRefNSUs;           // URIs of namespaces referenced in the model
        
    // All augmentation records, indexed by augmenting namespace URI, then class URI.
    // Global augmentations have a fake class URI:  "Association", "Literal", or "Object".
    protected Map<String,MapToList<String,AugmentRecord>> nsU2classU2augL;   

    // Augmentations that are not part of an augmentation type are tracked here.
    // They are turned into augmentation point QName values in xs:element @substitutionGroup 
    protected HashMap<String,String> pU2augPU = new HashMap<>();    
    
    
    // The following object globals are initialized anew for each schema document
    // generated for a model namespace.
    protected List<Element> defEL;              // type definition elements in the document
    protected List<Element> decEL;              // attribute and element declarations
    protected Set<String> refnsUS;              // URIs of referenced namespaces
    protected Map<String,String> bc2pre;        // prefixes for builtin namespaces
    protected Map<String,String> bc2U;          // URIs for builtin namespaces    
    
    
    protected ModelToXSD () { }
    
    public ModelToXSD (Model m)     { 
        this.m = m; 
        pathSpec = new HashMap<>();
    }
    
    // Call this to use a single architecture version in the generated schema
    // documents instead of the version in the namespace objects.  This controls
    // the utility schema documents (eg. structures.xsd) in the pile.  It doesn't
    // change the namespace URI of any model component.  Takes valueslike "NIEM5.0".
    public void setArchVersion (String vers) {
        useArchVersion = vers;
    }
    
    // Call this to generate an XML Catalog file at this relative path from
    // the schema document pile root.
    public void setCatalogPath (String path) {
        catalogPath = path;
    }   
    
    // Call this to specify the "root namespace".  The schema document for that
    // namespace will include extra xs:import elements as needed to ensure that
    // the entire schema can be assembled from this document alone.
    public void setRootNamespace (String nsPrefixOrURI) {
        if (null == nsPrefixOrURI) return;
        rootNS = m.namespaceObj(nsPrefixOrURI);
        if (null == rootNS)
            LOG.error("Can't make '{}' the root namespace; not in model", nsPrefixOrURI);
    }
    
    // Call this to provide a relative path for schema documents in the pile,
    // in case you don't like the paths recorded in the model namespace objects
    // (or the default paths, if the model has none).  The map is from namespace URI 
    // to relative file path in the pile.
    public void setNamespacePaths (Map<String,String> paths) {
        pathSpec = new HashMap<>(paths);
    }
      
    
    /**
     * Writes the model to an XSD pile in the specified directory.  The schema 
     * document for each model namespace gets the NIEM version specified in each
     * model namespace object.
     * @param outD 
     */
    public void writeModelXSD (File outD) throws ParserConfigurationException, IOException {        
        collectArchVersions();
        collectNamespaceKinds();
        establishFilePaths();
        identifySimpleTypes();
        collectExternalNamespaces();
        indexAugmentations();
        
        allRefNSUs = new HashSet<>();
        for (var ns : m.namespaceSet()) {
            if (ns.isModelNS()) writeModelDocument(ns, outD);
        }
        writeBuiltinDocuments(outD);
        writeCatalog(outD);
    }
    
    // Internal methods below this point
    
    // Examine all the namespaces to collect all the NIEM versions.  If the version
    // was specified in the call to writeModelXSD, then there will only be one.
    protected void collectArchVersions () {
        archVersions = new HashSet<>();
        if (null != useArchVersion) archVersions.add(useArchVersion);
        else 
            for (var ns : m.namespaceSet()) {
                var nver = ns.archVersion();
                if (!nver.isEmpty()) archVersions.add(ns.archVersion());
        }
    }
    
    // Collect the namespace kind value for each namespace, including the utilities.
    // Only looking at namespace URI, so extension and external will show as unknown.
    // Use these when creating file paths and import elements.
    protected void collectNamespaceKinds () {
        nsU2KindVal = new HashMap<>();
        for (var ns : m.namespaceSet()) {
            var kval = NamespaceKind.namespaceToKindValue(ns.uri());
            nsU2KindVal.put(ns.uri(), kval);
        }
    }

    // Establish the relative path for each schema document, taking into account
    // the document file path specified in each namespace object and the namespace
    // to path mapping given in the writeModelXSD call (if any).  File names are
    // munged as needed to make each path unique.
    protected void establishFilePaths () {
        // Begin with the supplied map of namespace URI to file path.
        // Did you put duplicates into that map? Nice try, you horrible thing.
        var uset = new UniquePathSet();
        nsU2Path = new HashMap<>();
        pathSpec.forEach((ns,path) -> {
            var upath = uset.add(path);
            nsU2Path.put(ns, "./" + upath);
        });
        // Namespaces with no specified path may have a path in the model object.
        // If not, try the namespace prefix with ".xsd" suffix.
        for (var ns : m.namespaceSet()) {
            if (pathSpec.containsKey(ns.uri())) continue;
            if (NSK_XSD == nsU2KindVal.getOrDefault(ns.uri(), NSK_UNKNOWN)) continue;
            var path = ns.documentFilePath();
            if (path.isEmpty()) path = ns.prefix() + ".xsd";
            var upath = uset.add(path);
            pathSpec.put(ns.uri(), "./" + upath);
            nsU2Path.put(ns.uri(), "./" + upath);
        }
        // Now do the builtins for each NIEM version
        for (var vers : archVersions) {
            var vdir = "";
            if (1 == archVersions.size()) vdir = "niem/";
            else vdir = NamespaceKind.versionDirName().get(vers);
            if (null == vdir) continue;
            for (var kcode : NamespaceKind.builtins()) {
                var nsU   = NamespaceKind.builtinNSU(vers, kcode);
                var rpath = NamespaceKind.builtinPath().get(kcode);
                var path  = vdir + rpath;
                if (pathSpec.containsKey(nsU)) continue;
                var upath = uset.add(path);
                pathSpec.put(nsU, "./" + path);
                nsU2Path.put(nsU, "./" + path);
            }
        }
    }    
    
    // We must create a simple type definition for each Datatype object that is
    // a list, a list item, a union, a union member, or an attribute property type.
    // Also sometimes for the datatype of a literal property.
    // But not for datatypes in the XML or XML Schema namespaces.
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
            if (dp.isAttribute()) stUs.add(dt);
            if (dpQ.endsWith("Literal") && dtQ.endsWith("SimpleType")) stUs.add(dt);
        }
        // Only need xs:simpleType elements for Datatype objects with a model namespace
        simpleTypes =  new HashSet<>();
        for (var dt : stUs) {
            if (null == dt) continue;           // eg. datatype of xml:lang
            var dtnsU = dt.namespaceURI();
            if (W3C_XML_SCHEMA_NS_URI.equals(dtnsU)) continue;
            if (!dt.isModelComponent()) continue;
            simpleTypes.add(dt);  
        }
    }

    // Create a set of all external namespace URIs.  Need this to create
    // xs:import elements and to decide if appinfo namespace is needed.
    protected void collectExternalNamespaces () {
        externalNSUs = new HashSet<>();
        for (var ns : m.namespaceSet())
            if (ns.isExternal()) externalNSUs.add(ns.uri());        
    }
    
    // Create an index into all the augmentation records in the model.
    //
    // Augmentations that are recorded in XSD as part of an augmentation
    // type (including attributes) will have a non-negative index value.
    // 
    // Augmentations that are recorded in XSD as an element substitutable
    // for an augmentation point will have an index value of -1.  The URI
    // of these properties are mapped to the augmentation point URI in
    // the pU2augPU map.
    //
    // Augmentations recorded as appinfo:Augmentation are attribute augmentations;
    // if the property is an object property and the class has simple content,
    // then the augmentation is a reference attribute.  These augmentations have
    // an index of -2.  These are indexed in classU2augL/
    //
    // All of these augmentations are indexed in nsU2classU2augL:
    //   * first by the augmenting namespace
    //   * then by the augmented class
    //   * giving a list of AugmentRecord objects

    protected void indexAugmentations () {
        nsU2classU2augL = new HashMap<>();
        for (var ns: m.namespaceSet()) {
            var nsU   = ns.uri();                               // http://AugmentingNS/
            if (W3C_XML_SCHEMA_NS_URI.equals(nsU)) continue;    // skip XSD namespace
            var cU2aL = new MapToList<String,AugmentRecord>();
            nsU2classU2augL.put(nsU, cU2aL);
            for (var arec : ns.augL()) {
                var p    = arec.property();
                var pU   = p.uri();
                var ct   = arec.classType();        // augmented class, or null
                
                // Decide if aug belongs to augmentation type, or directly
                // substitutes augmentation point, or is appinfo:Augmentation
                if (arec.index() < 0) {                                 // >=0 means augmentation type
                    if (p.isAttribute()) arec.setIndex(-2);             // attribute not in aug type
                    else if (null == ct) arec.setIndex(-2);             // global object property aug
                    else if (ct.isLiteralClass()) arec.setIndex(-2);    // element aug for simple content
                    else arec.setIndex(-1);                             // element sub for aug point
                }
                // Augs substituting for aug point added to pU2augPU
                // All augmentations indexed in nsU2classU2augL
                if (null == ct) {
                    for (var gc : arec.codeS()) {
                        gc = capitalize(gc.toLowerCase());
                        cU2aL.add(gc, arec);
                        if (-1 == arec.index()) pU2augPU.put(pU, gc + "AugmentationPoint");
                    }
                } else {
                    cU2aL.add(ct.uri(), arec);
                    if (-1 == arec.index())
                        pU2augPU.put(pU, replaceSuffix(ct.uri(), "Type", "") + "AugmentationPoint");
                }
            }
        }
    }

    // Write the schema document for a model namespace.  There are abstract
    // functions for things that are different in a model XSD versus a message
    // XML schema.
    protected void writeModelDocument (Namespace ns, File outD) 
        throws ParserConfigurationException, IOException {       
        
        // Initialize the document and xs:schema root element
        var db   = ParserBootstrap.docBuilder();
        var doc  = db.newDocument();
        var root = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:schema");
        doc.appendChild(root);
        
        // Get namespace URI and NIEM version; set the xs:schema attributes that
        // don't need a namespace prefix. (We don't know what the prefixes are yet.)
        var nsU  = ns.uri();
        var nver = ns.archVersion();
        if (null != useArchVersion) nver = useArchVersion;
        setAttribute(root, "targetNamespace", nsU);
        setAttribute(root, "version", ns.version());
        setAttribute(root, "xml:lang", ns.language());

        // Initialize global variables for creating this schema document
        defEL   = new ArrayList<>(); 
        decEL   = new ArrayList<>();
        refnsUS = new HashSet<>(); 
        bc2pre  = new HashMap<>();
        bc2U    = new HashMap<>();
        
        // Given the NIEM version, we can get builtin namespace URIs and 
        // assign builtin prefixes for this schema document.  Why all this bother?
        // Well, if the model has more than one NIEM version, then only one version
        // gets the "appinfo" prefix.  Also, nothing stops a model designer from 
        // using "appinfo" as a model namespace prefix.  Grrr.
        var nsmap  = new NamespaceMap(m.nsmap());
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
        var clsaU      = bc2U.get("CLSA");
     
        // Create xs:annotation element; add namespace documentation
        var appE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:appinfo");
        addDocumentation(root, ns.docL());
        
        // Add conformance target assertions, augment records, local terms if needed
        addConformanceAssertions(root, ns, nsmap);
        addAugmentAppinfo(root, ns);
        addLocalTerms(root, ns);
        
        // Create augmentation components for this namespace
        createAugmentationComponents(doc, ns);
        
        // Create complex types.  For an XSD model, make some CSC typedefs for
        // datatype objects.  For an XML message schema, every datatype is a simple typedef
        createComplexTypes(doc, ns);
        
        // Create simple types and attribute/element declarations.
        for (var dt : simpleTypes) if (dt.namespace() == ns) createSimpleType(doc, dt);
        for (var p : m.propertyL())  if (p.namespace() == ns)  createDeclaration(doc, p); 
        
        // Construct list of namespaces to be imported.
        // If this is the root namespace, ensure that schema constructed from 
        // this schema document will include every namespace in the model.
        var impnsUs = new HashSet<>(refnsUS);
        if (ns == rootNS) {
            var refGraph = new ReferenceGraph(m);
            var reachS   = refGraph.reachableFrom(ns);
            for (var ons : m.namespaceSet()) {
                if (!reachS.contains(ons))
                    impnsUs.add(ons.uri());
            }
        }
        // Sort the namespace imports into a pleasing order, using namespace
        // kind values (extensions, domains, core, ...)
        var impL = new ArrayList<Pair<String,String>>();
        for (var rnsU : impnsUs) {
            var pre  = nsmap.getPrefix(rnsU);
            var kind = nsU2KindVal.getOrDefault(rnsU, NSK_UNKNOWN);
            var key  = String.format("%02d%s", kind, pre);
            impL.add(new Pair<>(key, rnsU));
        }
        Collections.sort(impL, importPairComparator);
        var outF = new File(outD, nsU2Path.get(nsU));
        var outP = new File(nsU2Path.get(nsU)).getParentFile().toPath();
        for (var pv : impL) {
            var insU = pv.getValue1();
            if (nsU.equals(insU)) continue;                     // don't import yourself
            if (appinfoU.equals(insU)) continue;                // don't import appinfo
            if (clsaU.equals(insU)) continue;                   // don't import code list schema appinfo
            if (W3C_XML_SCHEMA_NS_URI.equals(insU)) continue;   // don't import XSD
            var snP  = new File(nsU2Path.get(insU)).toPath();
            var relP = outP.relativize(snP);
            var sloc = separatorsToUnix(relP.toString());
            var impE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:import");
            impE.setAttribute("namespace", insU);
            impE.setAttribute("schemaLocation", sloc);
            if (externalNSUs.contains(insU))
                impE.setAttributeNS(appinfoU, appinfoPre + ":" + "externalImportIndicator", "true");
            addDocumentation(impE, ns.idocL(insU));
            root.appendChild(impE);
        }
        // Create namespace declarations for every referenced namespace.
        // Also create declaration for the current namespace.
        refnsUS.add(nsU);
        for (var rnsU : refnsUS) {
            if (XML_NS_URI.equals(rnsU)) continue;
            var pre  = nsmap.getPrefix(rnsU);
            root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:"+pre, rnsU);
        }
        // Update the set of all namespaces referenced in the model
        allRefNSUs.addAll(refnsUS);
        
        // Sort declarations and definitions, append to document, write XSD
        processDeclarations();
        Collections.sort(defEL, definitionComparator);
        Collections.sort(decEL, declarationComparator);
        for (var e : defEL) root.appendChild(e);
        for (var e : decEL) root.appendChild(e);        
        writeXSD(doc, outF, appinfoPre);        
    }
    
    // Write schema documents for builtin namespaces as needed.
    // Model representations will reference some or all of the builtins.
    // Message schemas won't reference the builtins, so this will do nothing there.
    protected void writeBuiltinDocuments (File outD) {
        for (var vers : archVersions) {
            for (var kcode : NamespaceKind.builtins()) {
                var bnsU = NamespaceKind.builtinNSU(vers, kcode);
                if (!allRefNSUs.contains(bnsU)) continue;
                writeBuiltinDocument(outD, bnsU, vers, kcode);
            }
        }
    }
    
    protected static final ResourceManager rmgr = new ResourceManager(ModelToXSD.class);    
    
    // Write a builtin schema document
    protected void writeBuiltinDocument (File outD, String bnsU, String vers, String kcode) {
        var bpath = NamespaceKind.builtinPath().get(kcode);     // eg. utility/appinfo.xsd
        var vdir  = NamespaceKind.versionDirName().get(vers);   // eg. "niem6"
        var rname = "/xsd/" + vdir + bpath;                     // resource name; eg. "/xsd/niem6/utility/appinfo.xsd"
        var fpath = nsU2Path.get(bnsU);                         // relative location in pile
        var outF  = new File(outD, fpath);                      // absolute file location
        var outP  = outF.toPath().getParent();                  // parent directory of file location
            try {
                Files.createDirectories(outP);
                if ("NIEM-XS".equals(kcode)) {
                    writeProxyDocument(vers, bnsU, rname, outF);
                }                    
                else rmgr.copyResourceToFile(rname, outF);
            } catch (IOException ex) {
                LOG.error("Can't create builtin schema documents for {}: {}", vers, ex.getMessage());
            }        
    }
       
    // Write an XML Catalog file for schema documents in the pile
    protected void writeCatalog (File outD) throws IOException, ParserConfigurationException {
        if (null == catalogPath) return;
        var catF = new File(outD, catalogPath);
        var outS = new FileOutputStream(catF);
        var outW = new OutputStreamWriter(outS, "UTF-8");
        var catW = new XMLCatalogCreator();
        var catP = new File(catalogPath).toPath();
        if (null == catP.getParent()) catP = new File(".").toPath();
        else catP = catP.getParent();
        catW.writeCatalog(nsU2Path, catP, outW);
        outW.close();        
    }
    
    // Creates schema components for augmentation types and augmentation elements
    // in this namespace.
    protected void createAugmentationComponents (Document doc, Namespace ns) {
        var nsU   = ns.uri();
        var cU2aL = nsU2classU2augL.get(nsU);       // classU in this namespace -> list of aug recs
        var augCL = new ArrayList<>(cU2aL.keySet());// list of augmented classes in this namespace
        
        // Iterate over all the globals and class URIs that are augmented by this namespace.
        // Skip augmentation records for object properties substituting for an augmentation point.
        // Skip augmentation records for appinfo:Augmentation elements.
        for (var cU : augCL) {                      // http://AugmentedNS/Class or global code
            var ctnsU = m.uriToNSU(cU);             // http://AugmentedNS/ or ""
            var ctns  = m.namespaceObj(ctnsU);      // AugmentedNS namespace object or null
            var ctN  = "";                          // augmented class name (eg. BarType) or global
            switch (cU) {
            case "Association": ctN = "AssociationType"; break;
            case "Literal":     continue;
            case "Object":      ctN = "ObjectType"; break;
            default:            ctN = uriToName(cU); break; // BarType
            }
            var baseN = replaceSuffix(ctN, "Type", "");     // Bar or Object 
            var aeN   = baseN + "Augmentation";             // BarAugmentation or ObjectAugmentation
            var atN   = aeN + "Type";                       // BarAugmentationType ...
            var bphrs = "";                                 // base type documentation phrase
            switch (cU) {
            case "Association": bphrs = "all association objects"; break;
            case "Object":      bphrs = "all ordinary objects"; break;
            default:            bphrs = articalize(baseN).toLowerCase(); break;
            }
            var aeDoc = "Additional information about " + bphrs + ".";
            var atDoc = "A data type for additional information about " + bphrs + ".";
            
            // Determine augmentation point QName
            var apQ = "";
            if (null == ctns) apQ = bc2pre.get("STRUCTURES") + ":" + baseN;
            else apQ = ctns.prefix() + ":" + baseN;
            apQ = apQ + "AugmentationPoint";         
            
            // Create a class object for the augmentation type, then create
            // a CCC type definition in the current schema document.
            // Don't add object properties substituting for an augmentation point,
            // or attribute augmentations from appinfo:Augmentation elements.
            var act   = new ClassType(ns, atN);
            var propL = new ArrayList<AugmentRecord>();
            for (var arec : cU2aL.get(cU)) {
                if (arec.index() >= 0) propL.add(arec); // don't add appinfo:Augmentation augs
            }
            // Don't create empty augmentation type or useless augmentation element
            if (propL.isEmpty()) continue;
            
            Collections.sort(propL);                // OK to sort this list by index
            act.propAssocL().addAll(propL);
            act.addDocumentation(atDoc, "en-US");
            createCCCType(doc, act);
            
            // Create an augmentation property with the augmentation type
            var apE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
            apE.setAttribute("name", aeN);
            apE.setAttribute("type", act.qname());
            apE.setAttribute("substitutionGroup", apQ);
            addDocumentation(apE, aeDoc);
            if (!ctnsU.isEmpty()) refnsUS.add(ctnsU);
            decEL.add(apE);         
        }
    }  
    
    // Populates an xs:simpleType element with schema children for a list datatype.
    protected void populateList (Document doc, 
        Element e,                              // append xs:list to this element
        Datatype dt) {                          // create typedefs from this class

        var ldt = (ListType)dt;
        var idt = ldt.itemType();
        var iE  = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:list");
        var idQ = datatypeQName(idt);
        setAttribute(iE, "itemType", datatypeQName(idt));
        e.appendChild(iE);
        refnsUS.add(idt.namespaceURI());
    }
    
    // Populates an xs:simpleType element with schema children for a restriction datatype.
    // Restriction base type is taken from the datatype object.
    protected void populateRestriction (Document doc, 
        Element e,                          // append xs:list to this element
        Datatype dt) {                      // create typedefs from this class
        
        var r = (Restriction)dt;
        var bdt = r.base();
        var bdtQ = datatypeQName(bdt);
        populateRestriction(doc, e, dt, bdtQ);
    }
    
    // Populates an xs:simpleType element with schema children for a restriction datatype.
    // Restriction base type is provided.
    protected void populateRestriction (Document doc, 
        Element e,                          // append xs:list to this element
        Datatype dt,                        // create typedefs from this class
        String baseQ) {                     // QName of restriction base
        
        var r   = (Restriction)dt;
        var rE  = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:restriction");
        var bdt = r.base();
        setAttribute(rE, "base", baseQ);
        refnsUS.add(bdt.namespaceURI());
        
        var fL = new ArrayList<>(r.facetL());
        Collections.sort(fL);
        for (var f : fL) {
            var fname = f.xsdFacetName();
            var fval  = f.value();
            var fE    = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:" + fname);
            fE.setAttribute("value", fval);
            addDocumentation(fE, f.docL());
            rE.appendChild(fE);        
        }
        e.appendChild(rE);
    }
    
    // Populates an xs:simpleType element with schema children for a union datatype.
    protected void populateUnion (Document doc, 
        Element e,                          // append xs:list to this element
        Datatype dt) {                      // create typedefs from this class

        var udt  = (Union)dt;
        var uE   = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:union");
        var mbrs = "";
        var sep  = "";
        for (var mdt : udt.memberL()) {
            mbrs = mbrs + sep + datatypeQName(mdt);
            sep = " ";
            refnsUS.add(mdt.namespaceURI());
        }
        setAttribute(uE, "memberTypes", mbrs);
        e.appendChild(uE);
    }


    // Empty functions, possibly overriden by derived class to write model XSD 
    // and class to write message schemas
    
    protected void addConformanceAssertions (Element root, Namespace ns, NamespaceMap nsmap) {}
    
    protected void addAugmentAppinfo (Element root, Namespace ns) {}
    
    protected void addLocalTerms (Element root, Namespace ns) {}
    
    protected abstract void createComplexTypes (Document doc, Namespace ns) ;
    
    protected abstract void createCCCType (Document doc, ClassType ct) ;
    
    protected abstract void createCSCType (Document doc, ClassType ct) ;
    
    protected abstract void createCSCType (Document doc, Datatype dt) ;
    
    protected abstract void createSimpleType (Document doc, Datatype dt) ;
    
    protected abstract void createDeclaration (Document doc, Property p) ;
  
    protected void processDeclarations() {}
        
    protected void writeProxyDocument (String vers, String pnsU, String rname, File outF) {};

    
    // Various utility functions below
    
    // The schema QName corresponding to a datatype object depends on whether
    // we have created an xs:simpleType or xs:complexType element for it.
    protected String datatypeQName (Datatype dt) {
        if (!simpleTypes.contains(dt)) return dt.qname();
        else return replaceSuffix(dt.qname(), "Type", "SimpleType");
    }
    
    // Returns the first xs:annotation child, creating that element if necessary.
    protected Element getAnnotationElement (Element e) {
        return getFirstSchemaChild(e, "annotation");
    }
    
    // Returns the first xs:appinfo child of the first xs:annotation child of
    // the specified element, creating elements as necessary.
    protected Element getAppinfoElement (Element e) {
        var annE = getAnnotationElement(e);
        var appE = getFirstSchemaChild(annE, "appinfo");
        return appE;        
    }
    
    // Returns the first child element with the given local name.
    // Creates and appends that child if necessary.
    protected Element getFirstSchemaChild (Element e, String lname) {
        var nL = e.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, lname);
        if (nL.getLength() > 0) return (Element)nL.item(0);
        var aE = e.getOwnerDocument().createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:" + lname);
        e.appendChild(aE);
        return aE;        
    }
    
    // Appends a single English-language string as an xs:documentation child
    // to the first xs:annotation child of the specified element.
    protected void addDocumentation (Element e, String s) {
        var ls = new LanguageString(s, "en-US");
        addDocumentation(e, List.of(ls));
    }    

    // Appends each language-annotated string as an xs:documentation child to the
    // first xs:annotation child of the specified element.
    protected void addDocumentation (Element e, List<LanguageString>docL) {
        if (docL.isEmpty()) return;
        var aE = getAnnotationElement(e);
        for (var ls : docL) {
            var dE = e.getOwnerDocument().createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:documentation");
            dE.setTextContent(ls.text());
            if (!"en-US".equals(ls.lang())) dE.setAttribute("xml:lang", ls.lang());
            aE.appendChild(dE);
        }
    }    
    
    // Sets an attribute with a namespace in an element.
    // Does nothing if the value is missing or empty.
    protected void setAttribute (Element e, String nsU, String qname, String value) {
        if (null != value && !value.isEmpty())
            e.setAttributeNS(nsU, qname, value);
    }

    // Sets an attribute with no namespace in an element.
    // Does nothing if the value is missing or empty.    
    protected void setAttribute (Element e, String name, String value) {
        if (null != value && !value.isEmpty())
            e.setAttribute(name, value);
    }    
    
    // Write a NIEM schema document to a file, with attributes reordered  
    // in a pleasing way.
    protected void writeXSD (Document doc, File outF, String appinfoPre) throws IOException {
        var pF   = outF.getParentFile();
        pF.mkdirs();
        var os = new FileOutputStream(outF);
        var ow = new OutputStreamWriter(os, "UTF-8");
        var xsdW = new NIEMXSDWriter(appinfoPre);
        xsdW.writeXML(doc, ow);
        ow.close();
    }

    // Comparison classes for declarations, definitions, import statements
    
    protected final DeclarationComparator declarationComparator = new DeclarationComparator();
    protected class DeclarationComparator implements Comparator<Element> {
        @Override
        public int compare(Element o1, Element o2) {
            int i = o1.getLocalName().compareTo(o2.getLocalName());
            if (i != 0) return i;
            var n1 = o1.getAttribute("name");
            var n2 = o2.getAttribute("name");
            return NaturalOrderIgnoreCaseComparator.comp(n1, n2);
        }
    }
    
    protected final DefinitionComparator definitionComparator = new DefinitionComparator();
    protected class DefinitionComparator implements Comparator<Element> {
        @Override
        public int compare(Element o1, Element o2) {
            var n1 = o1.getAttribute("name");
            var n2 = o2.getAttribute("name");
            return NaturalOrderIgnoreCaseComparator.comp(n1, n2);
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
