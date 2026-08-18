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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;
import org.mitre.niem.cmf.AugmentRecord;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.DataProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.ListType;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import static org.mitre.niem.cmf.Model.uriToName;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.NamespaceMap;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.PropertyAssociation;
import org.mitre.niem.cmf.Restriction;
import org.mitre.niem.cmf.Union;
import static org.mitre.niem.utility.IndefiniteArticle.articalize;
import org.mitre.niem.utility.MapToList;
import org.mitre.niem.utility.MapToSet;
import org.mitre.niem.utility.NaturalOrderIgnoreCaseComparator;
import static org.mitre.niem.utility.StringUtils.replaceSuffix;
import org.mitre.niem.utility.UniquePathSet;
import org.mitre.niem.xml.LanguageString;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.XMLDocument.makeQN;
import static org.mitre.niem.xml.XMLDocument.makeURI;
import static org.mitre.niem.xml.XMLDocument.qnToName;
import static org.mitre.niem.xml.XMLDocument.qnToPrefix;
import static org.mitre.niem.xsd.NIEMConstants.hasMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXMLSchema {

    private final Model model;                              // actual model object; don't change
    private final Mapping map;                              // canonical to simple name map, if provided
    private final Set<ObjectProperty> propS;                // message properties, or null for whole model
    
    public ModelToXMLSchema (Model m) {
        this.model = m;
        this.map = new Mapping();
        this.propS = null;
    }
    
    public ModelToXMLSchema (Model m, Mapping map) {
        this.model = m;
        this.map = map;
        this.propS = null;
    }
    
    public ModelToXMLSchema (Model m, Mapping map, ObjectProperty prop) {
        this.model = m;
        this.map = map;
        this.propS = Set.of(prop);
    }

    public ModelToXMLSchema (Model m, Mapping map, Set<ObjectProperty> propS) {
        this.model = m;
        this.map = map;
        this.propS = propS;
    }
    
    private Map<String,String> pathSpec = new HashMap<>();  // user specified namespace URI -> relative schema doc path
    private String useArchVersion = null;                   // override arch version in namespace objects
    private String rootNS = null;                           // root namespace prefix or URI, possibly mapped
    
    /**
     * Call this to provide a relative path for schema documents in the pile,
     * in case you don't like the paths recorded in the model namespace objects
     * (or the default paths, if the model has none). The map is from namespace
     * URI to relative file path in the pile.
     * 
     * @param paths 
     */
    public void setNamespacePaths (Map<String,String> paths) {
        pathSpec = new HashMap<>(paths);
    }

    // Call this to use a single architecture version in the generated schema
    // documents instead of the version in the namespace objects.  This controls
    // the utility schema documents (eg. structures.xsd) in the pile.  It doesn't
    // change the namespace URI of any model component.  Takes values like "NIEM5.0".
    public void setArchVersion (String vers) throws CMFException {
        if (!NamespaceKind.knownVersions().contains(vers))
            throw new CMFException("Unknown NIEM architecture: " + vers);
        useArchVersion = vers;
    }

    // Call this to specify the "root namespace".  The schema document for that
    // namespace will include extra xs:import elements as needed to ensure that
    // the entire schema can be assembled from this document alone.
    public void setRootNamespace (String nsPrefixOrURI) {
        if (null == nsPrefixOrURI) return;
        rootNS = nsPrefixOrURI;
    }

    
    private NamespaceMap nsmap = null;                          // prefix to URI for namespaces in generated pile
    private Set<Component> compS = null;                        // set of components to create
    
    /**
     * Writes the model to an XSD pile at the specified location.  If every schema
     * component is mapped to a single namespace, this may create a single schema 
     * document, instead of a document pile in the specified directory.
     * 
     * @param outLoc
     */
    public void writeModelXSD (File outLoc) throws ParserConfigurationException, IOException {  
        nsmap = new NamespaceMap(model.nsmap());
        if (null == propS) compS = new HashSet<>(model.componentSet());
        else compS = new HashSet<>(model.messageComponents(propS));
        
        createAugmentationComponents();
        collectClassAttributes();
        createStructuresComponents();
        assignComponentsToNamespaces();
        establishFilePaths();
        
//        var arecs = new StringBuilder();
//        for (var cu : ctU2augL.keySet()) {
//            arecs.append(cu + ":\n");
//            for (var arec : ctU2augL.get(cu)) {
//                arecs.append("  " + arec.toString() + "\n");
//            }
//        }
//        
//        var augs = new StringBuilder();
//        for (var cu : ctU2augChoiceS.keySet()) {
//            augs.append(cu + ":\n");
//            for (var p : ctU2augChoiceS.get(cu)) {
//                augs.append("  " + p.qname() + "\n");
//            }
//        }
//        var atypes = new StringBuilder();
//        for (var c : compS) {
//            if (!c.name().endsWith("AugmentationType")) continue;
//            atypes.append(c.uri() + ":\n");
//            var ct = (ClassType)c;
//            for (var pa : ct.propAssocL()) {
//                atypes.append("  " + pa.property().qname() + "\n");
//            }
//        }
        var comps = new StringBuilder();
        for (var c : compS) comps.append(c.uri() + "\n");
//        
//        var maps = new StringBuilder();
//        for (var nsu : nsU2compS.keySet()) {
//            maps.append(nsu + ":\n");
//            for (var c : nsU2compS.get(nsu)) {
//                var curi = c.uri();
//                var mqn  = compU2qn.get(curi);
//                maps.append("  " + curi + " --> " + mqn + "\n");
//            }
//        }
//        var paths = new StringBuilder();
//        nsU2path.forEach((nsU,path) -> {
//            paths.append(nsU + " --> " + path + "\n");
//        });
        
        nsU2compS.removeKey(W3C_XML_SCHEMA_NS_URI);
        for (var nsU : nsU2compS.keySet()) 
            writeSchemaDocument(nsU, outLoc);
    }
    
    // Create property and class objects for augmentation properties that are
    // global, or that apply to a class in compS.  Don't add them to the model
    // object (because we never change that) but do add them to this.compS.  We
    // will check applicable mappings and namespace assignments later on. 
    
    private MapToSet<String,Property> ctU2augChoiceS;       // class URI -> property choices for class's aug point    
    private MapToList<String,AugmentRecord> ctU2augL;       // class URI -> list of all its augment records
    
    private void createAugmentationComponents () {
        
        ctU2augChoiceS = new MapToSet<>();
        ctU2augL       = new MapToList<>();
        
        // For each NS, first collect augment records by augmented class or global code
        for (var ns : model.namespaceSet()) {
            var nsU = ns.uri();
            var arecL = new MapToList<String,AugmentRecord>();
            for (var arec : ns.augL()) {
                var gcodeS = new HashSet<>(arec.codeS());
                gcodeS.add("CLASS");
                for (var gc : gcodeS) {
                    switch (gc) {
                    case "CLASS":
                        var ct = arec.classType();
                        if (null == ct || !compS.contains(ct)) break;
                        arecL.add(ct.uri(), arec);
                        break;
                    case "LITERAL":
                    case "ASSOCIATION":
                    case "OBJECT":
                        var cu = capitalize(gc.toLowerCase());
                        arecL.add(cu, arec);
                        break;
                    }
                }
            }
            // Augmentation records are now indexed by class URI or global code
            // First, handle global literal augmentations
            if (arecL.containsKey("Literal")) {
                var arlist = arecL.removeKey("Literal");
                for (var arec : arlist) {
                    var p = arec.property();
                    if (!p.isAttribute()) p = createRefAtt(p);
                    compS.add(p);
                    ctU2augL.add("Literal", arec);
                }
            }
            // Create augmentation properties and classes for this namespace
            for (var ctU:  arecL.keySet()) {
                var arlist = arecL.get(ctU);                     // foo:BarType, or Object              
                var cname  = uriToName(ctU);                     // BarType, or ""
                if (cname.isEmpty()) cname = ctU;                // BarType, or Object
                else cname = replaceSuffix(cname, "Type", "");  // Bar, or Object
                var apname = cname + "Augmentation";
                var aptnm  = cname + "AugmentationType";
                var noun   = articalize(cname);
                var apdoc  = "Additional information about " + noun;
                var aptdoc = "A data type for additional information about " + noun;
                
                // Construct a dummy class object for the augmentation type.
                // We will ignore it later if the type turns out to be empty.
                var ct = model.uriToClassType(ctU);          // class being augmented (null for global)
                var aptype = new ClassType(ns, aptnm);      // dummy for augmentation type
                aptype.addDocumentation(aptdoc, "en-US");
                Collections.sort(arlist);                   // by order within aug type
                for (var arec : arlist) {
                    var p  = arec.property();
                    var pn = p.qname();
                    ctU2augL.add(ctU, arec);
                    
                    // A literal class doesn't have an augmentation point, so we
                    // must make a reference attribute for an object property augmentation.
                    if (null != ct && ct.isLiteralClass() && !p.isAttribute()) {
                        var rp = createRefAtt(p);
                        compS.add(rp);
                    }
                    // Non-negative index means this property goes into the augmentation type.
                    // Both object properties and attribute properties handled here.
                    else if (arec.index() >= 0) {
                        aptype.addPropertyAssociation(arec);
                    }
                    // Negative index means an object property directly substituted 
                    // for an augmentation point.
                    else if (!p.isAttribute()) ctU2augChoiceS.add(ctU, p);
                }
                // Create augmentation property unless the aug type is empty
                if (!aptype.propAssocL().isEmpty()) {                
                    var ap = new ObjectProperty(ns, apname);
                    ap.setClassType(aptype);
                    ap.addDocumentation(apdoc, "en-US");
                    ctU2augChoiceS.add(ctU, ap);
                    compS.add(aptype);
                    compS.add(ap);
                }
            }
        }
    }
    
    // Creates a reference attribute DataProperty for the object property, or
    // return the reference attribute if already created.
    private Property createRefAtt (Property p) {
        var nsU = p.namespaceURI();
        var rn  = uncapitalize(p.name()) + "Ref";
        for (var c : compS) {
            if (c instanceof Property dp) {
                if (dp.namespaceURI().equals(nsU) && dp.name().equals(rn)) return dp;
            }
        }
        var u  = makeURI(p.namespaceURI(), rn);
        var dp = new DataProperty(p.namespace(), rn);
        dp.setIsAttribute(true);
        dp.setIsRefAttribute(true);
        return dp;
    }

    // Collect the attribute property associations and attribute augmentations
    // for each class.  It's OK if the resulting list has duplicate attributes;
    // we will sort that out later.
    private MapToList<String,PropertyAssociation> ctU2attL;
    private void collectClassAttributes () {
        ctU2attL = new MapToList<>();
        var newAttS = new HashSet<Property>();
        for (var c : compS) {
            if (c instanceof ClassType ct) {
                var ctU = ct.uri();
                var apL = ctU2attL.get(ctU);                    // empty list of attribute properties
                var pL  = new ArrayList<>(ct.propAssocL());     // copy of properties of this class
                pL.addAll(ctU2augL.get(ctU));                   // add augmentation properties for this class
                
                if (ct.isAssociationClass()) pL.addAll(ctU2augL.get("Association"));
                if (ct.isObjectClass())      pL.addAll(ctU2augL.get("Object"));
                if (ct.isLiteralClass())     pL.addAll(ctU2augL.get("Literal"));
                
                var rct = ct;                                   // class inheritance root
                while (null != rct.subClassOf()) 
                    rct = rct.subClassOf();
                
                // Make list of all attribute properties of this class.
                // An object property for a literal class must be an augmentation,
                // and here turns into a reference attribute.
                for (var pa : pL) {
                    var p = pa.property();
                    if (rct.isLiteralClass() && !p.isAttribute() && !p.name().endsWith("Literal")) {
                        p = createRefAtt(p);
                        pa = new PropertyAssociation(pa);
                        pa.setProperty(p);
                        newAttS.add(p);
                    }
                    if (p.isAttribute()) apL.add(pa);
                }
            }
        }
        for (var p : newAttS) compS.add(p);     // reference attributes created above
    }

    // Create components for structures:id, ref, uri, metadata attributes as needed,
    // by walking through the class objects and looking at their reference codes.
    // Handle a multi-architecture model.
    
    private static final Set<String> needURIcodes  = Set.of("ANY", "ANYURI", "INTERNAL", "RELURI");
    private static final Set<String> needRefcodes  = Set.of("ANY", "INTERNAL", "IDREF");

    private void createStructuresComponents () {
        var snss = new HashMap<String,Namespace>();     // structures URI -> namespace object
        var done = new HashMap<String,DataProperty>();  // structures property uri -> property object
        for (var c : compS) {
            if (c instanceof ClassType ct) {
                var rcode = ct.referenceCode();
                var avers = ct.namespace().archVersion();
                if (null != useArchVersion) avers = useArchVersion;
                if (hasMetadata.contains(avers))  createStructAtt(snss, done, avers, "metadata");
                if (needURIcodes.contains(rcode)) createStructAtt(snss, done, avers, "uri");
                if (needRefcodes.contains(rcode)) createStructAtt(snss, done, avers, "ref");
                if (!rcode.isEmpty() && !"NONE".equals(rcode)) {
                    createStructAtt(snss, done, avers, "appliesToParent");
                    createStructAtt(snss, done, avers, "id");
                }
            }
        }
        compS.addAll(done.values());
    }
    
    // Create a single structures attribute in the namespace for the specified
    // architecture.
    private void createStructAtt (
        Map<String,Namespace> snss,         // structures namespace created so far
        Map<String,DataProperty> done,      // structures attributes created so far
        String avers,                       // create attributes for this architecture version
        String aname) {                     // name of structures attribute to create
        
        // Get namespace object for this structures namespace, or create it
        var structU = NamespaceKind.builtinNSU(avers, "STRUCTURES");
        var sns = snss.get(structU);
        if (null == sns) {
            var spre = nsmap.assignPrefix("structures", structU);
            sns = new Namespace(spre, structU);
            snss.put(structU, sns);
        }
        // Have we already created this attribute in this namespace?
        var apU = makeURI(structU, aname);
        if (!done.containsKey(apU)) {
            var sap = new DataProperty(sns, aname);
            sap.setIsAttribute(true);
            done.put(apU, sap);
        }
    }
    
    // Assign each model component required for this message schema to its 
    // namespace, AFTER applying the Mapping object.
    
    private final MapToSet<String,Component> nsU2compS = new MapToSet<>();  // namespace URI -> set of components
    private final Map<String,String> compU2qn = new HashMap<>();            // model component URI -> schema QName
    private void assignComponentsToNamespaces () {
        for (var c : compS) {
            var mU   = c.uri();
            var mln  = c.name();
            var mnsU = c.namespaceURI();
            var mrec = map.uriToMapRec(mU);
            if (null != mrec) {
                mU   = mrec.uri();
                mln  = mrec.localName();
                mnsU = mrec.namespace();
                nsmap.assignPrefix(mrec.prefix(), mrec.namespace());
            }
            var mpre = nsmap.getPrefix(mnsU);
            var mQ   = makeQN(mpre, mln);
            nsU2compS.add(mnsU, c);         // component belongs to its mapped namespace
            compU2qn.put(c.uri(), mQ);      // component URI -> mapped QName
        }
    }

    // Establish the relative path for each schema document, taking into account
    // the document file path specified in each namespace object and the namespace
    // to path mapping given in the writeModelXSD call (if any).  File names are
    // munged as needed to make each path unique.
    protected Map<String,String> nsU2path;          // namespace URI -> relative path in document pile   
    protected void establishFilePaths () {
        // Begin with the supplied map of namespace URI to file path.
        // Did you put duplicates into that map? Nice try, you horrible thing.
        var uset = new UniquePathSet();
        nsU2path = new HashMap<>();
        pathSpec.forEach((ns,path) -> {
            var upath = uset.add(path);
            nsU2path.put(ns, "./" + upath);
        });
        for (var nsU : nsU2compS.keySet()) {
            if (pathSpec.containsKey(nsU)) continue;
            var path = "";
            var ns = model.namespaceObj(nsU);
            if (null != ns) path = ns.documentFilePath();
            if (path.isEmpty()) {
                var kind = NamespaceKind.namespaceToKindCode(nsU);
                if (!kind.isBlank()) path = NamespaceKind.builtinPath().getOrDefault(kind, "");
            }                
            if (path.isEmpty()) {
                var prefix = nsmap.getPrefix(nsU);
                path = prefix + ".xsd";
            }
//            path = "./" + path;
            var upath = uset.add(path);
            nsU2path.put(nsU, upath);      
        }
    }
    
    private void writeSchemaDocument (String nsU, File outLoc) throws ParserConfigurationException, IOException {
        
        // Initialize the document and xs:schema root element
        var db   = ParserBootstrap.docBuilder();
        var doc  = db.newDocument();
        var root = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:schema");
        setAttribute(root, "targetNamespace", nsU); 
        doc.appendChild(root);
        
        // Create lists of definition and declaration elements
        var qrefs = new HashSet<String>();      // QNames referenced in namespace components
        var defns = new ArrayList<Element>();
        var decls = new ArrayList<Element>();
        for (var c : nsU2compS.get(nsU)) {
            if (c instanceof ClassType ct)     defns.add(createComplexType(doc, ct, qrefs));    // can't be null
            else if (c instanceof Datatype dt) defns.add(createSimpleType(doc, dt, qrefs));     // may be null
            else if (c instanceof Property p)  decls.add(createDeclaration(doc, p, qrefs));     // may be null
        }
        
        // Add namespace declarations for all referenced components
        var nsdecls = new HashMap<String,String>();
        for (var qn : qrefs) {
            var prefix = qnToPrefix(qn);
            var uri = nsmap.getURI(prefix);
            nsdecls.put(prefix, uri);
        }
        nsdecls.forEach((prefix,uri) -> {
            root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:"+prefix, uri);
        });
        
        // Construct list of namespaces to be imported; add import elements
        var nsloc = nsU2path.get(nsU);
        var impnsUL = new ArrayList<>(nsdecls.values());
        impnsUL.remove(nsU);
        Collections.sort(impnsUL);
        var dir = outLoc.toPath();
        var nsp = dir.resolve(nsloc).normalize();
        for (var impU : impnsUL) {
            if (W3C_XML_SCHEMA_NS_URI.equals(impU)) continue;
            var isp  = nsU2path.get(impU);
            var iloc = dir.resolve(isp).normalize();
            var rel  = nsp.getParent().relativize(iloc);
            var sloc = separatorsToUnix(rel.toString());
            var impE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:import");
            impE.setAttribute("namespace", impU);
            impE.setAttribute("schemaLocation", sloc);
            root.appendChild(impE);
        }
        
        // Add element/attribute declarations
        defns.removeIf(Objects::isNull);
        decls.removeIf(Objects::isNull);
        Collections.sort(defns, definitionComparator);
        Collections.sort(decls, declarationComparator);
        for (var cE : defns) root.appendChild(cE);
        for (var cE : decls) root.appendChild(cE);
        
        var outF = new File(outLoc, nsloc);
        outF.getParentFile().mkdirs();
        var os = new FileOutputStream(outF);
        var ow = new OutputStreamWriter(os, "UTF-8");
        var xsdW = new NIEMXSDWriter("appinfo");
        xsdW.writeXML(doc, ow);
        ow.close();        
        
//        var sw = new StringWriter();
//        var xw = new NIEMXSDWriter("appinfo");
//        xw.writeXML(doc, sw);
//        var debug = sw.toString();
    }
    
    // Construct an xs:complexType element for the given class object.
    // Add the QName of each referenced component to the set.
    private Element createComplexType (Document doc, ClassType ct, Set<String> qrefs) {
        var ctU  = ct.uri();
        var name = ct.name();
        var mrec = map.uriToMapRec(ctU);                // if this class URI is mapped
        if (null != mrec) name = mrec.localName();      // then use the mapping local name
        
        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        ctE.setAttribute("name", name);
        addDocumentation(ctE, ct.docL());

        // Make note of structures attributes to include in this complex type.
        // Don't need to include any inherited structures attributes.
        // But we can't extend from parent class if reference codes are not compatible.
        var pct     = ct.subClassOf();
        var refCode = ct.effectiveReferenceCode();
        var needURI = needURIcodes.contains(refCode);
        var needRef = needRefcodes.contains(refCode);
        var extendF = false;
        if (null != pct) {
            var prefCode = pct.effectiveReferenceCode();
            var pNeedURI = needURIcodes.contains(prefCode);
            var pNeedRef = needRefcodes.contains(prefCode);
            extendF = true;
            if (pNeedURI && !needURI) extendF = false;  // parent has @uri, this class doesn't; can't extend
            if (pNeedRef && !needRef) extendF = false;  // parent has @ref, this class doesn't; can't extend
            if (extendF) {
                needURI = needURI && !pNeedURI;         // parent doesn't have @uri; this class must add it
                needRef = needRef && !pNeedRef;         // parent doesn't have @ref; this class must add it
            }
        }
        // Construct a list of model attribute properties for this type.
        // Also construct list of inherited model attribute properties.
        var attL  = new ArrayList<>(ctU2attL.get(ctU));     // atts for this class
        var pattL = new ArrayList<PropertyAssociation>();   // inherited atts
        while (null != pct) {
            pattL.addAll(ctU2attL.get(pct.uri()));
            pct = pct.subClassOf();
        }
        // If we are extending a base class, remove inherited attributes from attL.
        // Ideally there shouldn't be any duplicates.
        if (extendF) {
            var paS = pattL.stream()
                .map(PropertyAssociation::property)
                .collect(Collectors.toSet());
            attL.removeIf(pa -> paS.contains(pa.property()));
        }
        // If we can't extend, merge inherited attributes into attL
        else {
            var mergeM = new HashMap<Property,PropertyAssociation>();
            for (var pa : attL)  mergeM.put(pa.property(), pa);
            for (var pa : pattL) mergeM.putIfAbsent(pa.property(), pa);
            attL = new ArrayList<>(mergeM.values());
        }
        // Create child elements for the xs:complexType.
        // Returns the parent element for all xs:attribute refs
        Element attParentE;
        if (ct.hasSimpleContent()) attParentE = createCSCType(ct, ctE, extendF, qrefs);
        else attParentE = createCCCType(ct, ctE, extendF, qrefs);
        
        // Now add model attribute references to their parent element
        var attS = new HashSet<>(attL);
        for (var apa : attS) {
            if (apa.property().isAttribute())
                addPropertyRef(apa, attParentE, qrefs);
        }
        // Add xs:anyAttribute wildcards as needed
        for (var ap : ct.anyL()) {
            if (!ap.isAttribute()) continue;
            var anyE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:anyAttribute");            
            setAttribute(anyE, "processContents", ap.processCode());
            setAttribute(anyE, "namespace", ap.nsConstraint());
            attParentE.appendChild(anyE);
        }  
        // Add structures attributes as needed, respecting the architecture
        // version for this namespace.
        var arch = ct.namespace().archVersion();
        if (null != useArchVersion) arch = useArchVersion;
        var structU   = NamespaceKind.builtinNSU(arch, "STRUCTURES");
        if (needURI || needRef) {
            addStructuresAttribute(structU, "appliesToParent", attParentE, qrefs);
            addStructuresAttribute(structU, "id", attParentE, qrefs);
        }
        if (needRef) addStructuresAttribute(structU, "ref", attParentE, qrefs);
        if (needURI) addStructuresAttribute(structU, "uri", attParentE, qrefs);
        if (!extendF && hasMetadata.contains(arch))
            addStructuresAttribute(structU, "metadata", attParentE, qrefs);
        
        return ctE;
    }

    
    // Populate the xs:complexType element with child elements needed for a CSC type.
    // Return the parent element for any xs:attribute refs.
    private Element createCSCType (ClassType ct,   // building CSC for this literal class
        Element ctE,                            // populating this xs:complexType element
        boolean extendF,                        // are we extending a base type?
        Set<String> qrefs) {                    // add QName refs to this set
     
        var doc = ctE.getOwnerDocument();
        var scE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
        var exE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
        scE.appendChild(exE);
        ctE.appendChild(scE);

        String baseU;
        var dt = ct.literalDatatype();
        if (null != dt) baseU = dt.uri();
        else baseU = ct.subClassOf().uri();
        
        String baseQ;
        var bnsU  = model.uriToNSU(baseU);
        var bname = uriToName(baseU);
        if (W3C_XML_SCHEMA_NS_URI.equals(bnsU)) baseQ = makeQN("xs", bname);
        else baseQ = compU2qn.get(baseU);
        exE.setAttribute("base", baseQ);
        qrefs.add(baseQ);
        return exE;
    }
    
    
    // Populate the xs:complexType element with child elements needed for the 
    // element properties associated with a CCC type.  Returns the parent element 
    // for any xs:attribute refs.
    private Element createCCCType (ClassType ct,    // building CSC for this literal class
        Element ctE,                                // populating this xs:complexType element
        boolean extendF,                            // are we extending a base type?
        Set<String> qrefs) {                        // add QName refs to this set
       
        // Need xs:complexContent and xs:extension elements if we are using xs:extension
        // with a base type.  Otherwise we only need the xs:sequence element.
        Element attParentE;
        var ctU = ct.uri();
        var doc = ctE.getOwnerDocument();
        var sqE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:sequence");
        if (extendF) {
            var ccE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexContent");
            var exE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
            var pct = ct.subClassOf();          // parent class
            var pQ  = compU2qn.get(pct.uri());  // possibly-mapped parent class QName
            qrefs.add(pQ);
            exE.setAttribute("base", pQ);
            exE.appendChild(sqE);
            ccE.appendChild(exE);
            ctE.appendChild(ccE);
            attParentE = exE;           // add attributes to the xs:extension
        }
        else { 
            ctE.appendChild(sqE);
            attParentE = ctE;           // add attributes to the xs:complexType
        }
        // If we can't use xs:extension, then we must first add any global element
        // augmentations, followed by any inherited properties.
        if (!extendF) {
            if (ct.isAssociationClass()) addAugChoices("Association", sqE, qrefs);
            if (ct.isObjectClass())      addAugChoices("Object", sqE, qrefs);
            addParentProperties(ct.subClassOf(), sqE, qrefs);
        } 
        // Now add xs:element refs for the property associations in this class
        for (var pa : ct.propAssocL()) {
            var p = pa.property();
            if (p.isAttribute()) continue;
            addPropertyChoices(pa, sqE, qrefs);
        }
        // Add augmentation choices for this class
        addAugChoices(ctU, sqE, qrefs);
        
        // Add xs:any elements to the xs:sequence
        for (var ap : ct.anyL()) {
            if (ap.isAttribute()) continue;
            var anyE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:any");            
            if (!"1".equals(ap.minOccurs())) anyE.setAttribute("minOccurs", ap.minOccurs());
            if (!"1".equals(ap.maxOccurs())) anyE.setAttribute("maxOccurs", ap.maxOccurs());
            setAttribute(anyE, "processContents", ap.processCode());
            setAttribute(anyE, "namespace", ap.nsConstraint());
            sqE.appendChild(anyE);
        }
        return attParentE;
    }    
       
    // Called when a subclass can't use xs:extension (because ref codes are
    // incompatible).  Adds all the element properties of all the inherited classes,
    // deepest first.
    private void addParentProperties (ClassType pct, Element sqE, Set<String> qrefs) {
        if (null == pct) return;
        addParentProperties(pct.subClassOf(), sqE, qrefs);  // adding depth-first
        for (var pa : pct.propAssocL()) 
            if (!pa.property().isAttribute())               // attributes handled elsewhere
                addPropertyChoices(pa, sqE, qrefs);         // could have subproperties
        addAugChoices(pct.uri(), sqE, qrefs);               // add parent class augmentations
    }
    
    // Adds all of the augmentation choices for a class to its xs:sequence element.
    // Adds them to an xs:choice with cardinality 0:* if more than one choice.
    // Just adds a single element ref (with 0:*) if only one choice.
    // Does nothing if no augmentation choices for this class.
    private void addAugChoices (String ctU, Element sqE, Set<String> qrefs) {
        var pS = ctU2augChoiceS.get(ctU);
        var pa = new PropertyAssociation();     // dummy object to hold 0:* cardinality
        pa.setMinOccurs("0");
        pa.setMaxOccurs("unbounded");
        addChoiceSet(pa, pS, sqE, qrefs);
    }

    // Create a set of the specified properties plus all of its subproperties
    // and pass it to addChoiceSet.
    private void addPropertyChoices (PropertyAssociation pa, Element parent, Set<String> qrefs) {
        var p    = pa.property();
        var subS = new HashSet<Property>();
        for (var sp : model.allSubProps(p)) {
            if (!sp.isAbstract()) subS.add(sp);
        }
        addChoiceSet(pa, subS, parent, qrefs);
    }
    
    // Adds elements for a set of properties to the parent element.
    // 
    private void addChoiceSet (PropertyAssociation pa, Set<Property>pS, Element parent, Set<String> qrefs) {
        if (pS.isEmpty()) return;
        var pE  = parent;
        var tpa = new PropertyAssociation();
        tpa.setMaxOccurs("1");
        tpa.setMinOccurs("1");
        if (pS.size() > 1) {
            pE = parent.getOwnerDocument().createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:choice");
            pE.setAttribute("minOccurs", pa.minOccurs());
            pE.setAttribute("maxOccurs", pa.maxOccurs());
            parent.appendChild(pE);
        }
        var pL = new ArrayList<>(pS);
        Collections.sort(pL);
        for (var p : pL) {
            tpa.setProperty(p);
            if (pS.size() ==1) {
                tpa.setMinOccurs(pa.minOccurs());
                tpa.setMaxOccurs(pa.maxOccurs());
            }
            addPropertyRef(tpa, pE, qrefs);
        }
    }

    // Adds a single property element to its parent.
    private void addPropertyRef (PropertyAssociation pa, Element parent, Set<String> qrefs) {
        Element pE;
        var p  = pa.property();
        var pU = p.uri();
        var pQ = compU2qn.get(pU);
        var doc = parent.getOwnerDocument();
        if (p.isAttribute()) {
            pE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            if ("1".equals(pa.minOccurs())) pE.setAttribute("use", "required");
        }
        else {
            pE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");            
            if (!"1".equals(pa.minOccurs())) pE.setAttribute("minOccurs", pa.minOccurs());
            if (!"1".equals(pa.maxOccurs())) pE.setAttribute("maxOccurs", pa.maxOccurs());            
        }
        pE.setAttribute("ref", pQ);
        addDocumentation(pE, pa.docL());
        qrefs.add(pQ);
        parent.appendChild(pE);
    }
    
    // Add a structures attribute to the specified XSD element. Apply the mapping
    // for the attribute's URI, if any.
    private void addStructuresAttribute (String structU, String lname, Element pE, Set<String> qrefs) {
        var sU = makeURI(structU, lname);
        var sQ = compU2qn.get(sU);
        var sE = pE.getOwnerDocument().createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        sE.setAttribute("ref", sQ);
        pE.appendChild(sE);
        qrefs.add(sQ);
    }

    private Element createSimpleType (Document doc, Datatype dt, Set<String> qrefs) {
        var nsU = dt.namespaceURI();
        if (W3C_XML_SCHEMA_NS_URI.equals(nsU)) return null;  // don't create XSD types
        if (XML_NS_URI.equals(nsU)) return null;             // don't create XML types 
        
        var dtU = dt.uri();
        var dtQ = compU2qn.get(dtU);
        var dn  = qnToName(dtQ);
        var stE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleType");
        stE.setAttribute("name", dn);
        addDocumentation(stE, dt.docL());
        
        if (dt instanceof ListType lt) populateList(stE, lt, qrefs);
        else if (dt instanceof Restriction rt) populateRestriction(stE, rt, qrefs);
        else if (dt instanceof Union ut)       populateUnion(stE, ut, qrefs);
        
        return stE;
    }
    
    private void populateList (Element stE, ListType lt, Set<String> qrefs) {
        var idt = lt.itemType();
        var iU  = idt.uri();
        var iQ  = compU2qn.get(iU);
        var lE  = stE.getOwnerDocument().createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:list");
        lE.setAttribute("itemType", iQ);
        stE.appendChild(lE);
        qrefs.add(iQ);
    }
    
    private void populateRestriction (Element stE, Restriction r, Set<String> qrefs) {
        var doc = stE.getOwnerDocument();
        var bdt = r.base();
        var bU  = bdt.uri();
        var bQ  = compU2qn.get(bU);
        var rE  = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:restriction");
        rE.setAttribute("base", bQ);
        stE.appendChild(rE);
        qrefs.add(bQ);
        
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
    }
    
    private void populateUnion (Element stE, Union u, Set<String> qrefs) {
        var uE   = stE.getOwnerDocument().createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:union");
        var mbrs = "";
        var sep  = "";
        for (var mdt : u.memberL()) {
            var mU = mdt.uri();
            var mQ = compU2qn.get(mU);
            mbrs = mbrs + sep + mQ;
            sep = " ";
            qrefs.add(mQ);
        }
        setAttribute(uE, "memberTypes", mbrs);
        stE.appendChild(uE);        
    }
    
    private Element createDeclaration (Document doc, Property p, Set<String> qrefs) {
        var pnsU = p.namespaceURI();
        if (W3C_XML_SCHEMA_NS_URI.equals(pnsU)) return null;
        if (p.name().endsWith("Literal")) return null;
        if (p.isAbstract()) return null;
        
        Element decE;
        if (p.isAttribute()) decE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        else decE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
        addDocumentation(decE, p.docL());

        var pU = p.uri();
        var pQ = compU2qn.get(pU);
        var pN = qnToName(pQ);
        decE.setAttribute("name", pN);
        qrefs.add(pQ);
        
        var pt = p.type();
        if (p.isRefAttribute()) {
            decE.setAttribute("type", "xs:IDREFS");
        }
        else if (null != pt) {
            var ptU = pt.uri();
            var ptQ = compU2qn.get(ptU);
            decE.setAttribute("type", ptQ);
            qrefs.add(ptQ);
        }
        else if (XML_NS_URI.equals(pnsU)) {
            switch (pN) {
            case "lang":  decE.setAttribute("type", "xs:language"); break;
            case "space": decE.setAttribute("type", "xs:NCName"); break;
            case "base":  decE.setAttribute("type", "xs:anyURI"); break;
            }
        }
        if (p.isReferenceable()) decE.setAttribute("nillable", "true");
        return decE;
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

    // Returns the first xs:annotation child, creating that element if necessary.
    protected Element getAnnotationElement (Element e) {
        return getFirstSchemaChild(e, "annotation");
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
}
