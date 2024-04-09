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
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import static java.lang.Character.isUpperCase;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import static org.apache.commons.io.FileUtils.createParentDirectories;
import org.apache.commons.io.FilenameUtils;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import static org.apache.commons.io.IOUtils.copy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.NIEMConstants.CONFORMANCE_ATTRIBUTE_NAME;
import org.mitre.niem.cmf.AugmentRecord;
import static org.mitre.niem.cmf.AugmentRecord.AUG_ASSOC;
import static org.mitre.niem.cmf.AugmentRecord.AUG_NONE;
import static org.mitre.niem.cmf.AugmentRecord.AUG_OBJECT;
import static org.mitre.niem.cmf.AugmentRecord.AUG_SIMPLE;
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
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.RestrictionOf;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.mitre.niem.cmf.NamespaceKind;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_APPINFO;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CLI;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CLSA;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CTAS;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_PROXY;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_STRUCTURES;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_NOTBUILTIN;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_BUILTIN_COUNT;
import static org.mitre.niem.cmf.NamespaceKind.NSK_BUILTIN;
import org.mitre.niem.cmf.ReferenceGraph;
import static org.mitre.utility.IndefiniteArticle.articalize;
import org.xml.sax.SAXException;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UNKNOWN;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XML;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XSD;
import static org.mitre.niem.cmf.NamespaceKind.getBuiltinNS;
import static org.w3c.dom.Node.ELEMENT_NODE;


/**
 * An abstract class for writing a Model as a NIEM XML schema pile.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public abstract class ModelToXSD {
    static final Logger LOG = LogManager.getLogger(ModelToXSD.class);
    
    // These are the same for the whole schema
    protected Model m = null;
    protected String catPath = null;                          // path to created XML catalog file, if one is wanted
    protected String messageNSuri = null;                     // URI of message schema "root namespace"
    protected ReferenceGraph refGraph = null;                 // what namespaces XYZ will be transitively imported by FOO?
    protected final Map<String, String> ns2file;              // map nsURI -> absolute schema document file path
    protected final Set<String> nsfiles;                      // set of schema document file paths
    protected final Set<String> utilityNSuri;                 // set of utility namespaces needed in schema document pile
    protected final Map<Property,Set<Property>> substituteMap;// map Property -> all subprops in a message schema
    protected final Map<String, List<String>> subpropDeps;    // map nsURI -> list of subproperty Namespace dependency URIs
    protected final Set<ClassType> litTypes;                  // set of ClassType objects that will become complex type, simple content
    protected final Set<Property> litProps;                   // set of literal properties not needed in XSD
    protected final Set<Datatype> needSimpleType;             // Union, list, or non-empty restriction datatypes
    protected final List<AugmentRecord> gAttAugs;             // global attribute augmentation records
    protected boolean hasGEAug = false;                       // global augmentation for associations or objects?
    
    // These change as each namespace is processed
    protected Map<String,Element> nsAttributeDecls = null;    // map name -> schema declaration of attribute in a namespace
    protected Map<String,Element> nsElementDecls = null;      // map name -> schema declaration of element in a namespace
    protected Map<String,Element> nsTypedefs = null;          // map name -> schema definition of type in a namespace
    protected Set<String> nsNSdeps = null;                    // Namespace of namespaces referenced in current namespace
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

    // Private so you can't create an object without a Model.
    private ModelToXSD () { 
        ns2file        = new HashMap<>();
        nsfiles        = new HashSet<>();
        utilityNSuri   = new HashSet<>();
        substituteMap  = new HashMap<>();
        subpropDeps    = new HashMap<>();
        litTypes       = new HashSet<>();
        litProps       = new HashSet<>(); 
        needSimpleType = new HashSet<>();
        gAttAugs       = new ArrayList<>();
    }   
    
    public ModelToXSD (Model m) {
        this();
        this.m = m;
    }
    
    /**
     * Causes an XML Catalog file for the schema to be generated in the
     * specified file.  A null parameter results in no catalog file.
     * @param cp -- relative path from schema pile root to catalog file
     */
    public void setCatalog (String cp) { catPath = cp; }
    
    /**
     * Designates a "root" schema document for additional import elements so that
     * a schema constructed from the root document will have all required components.
     * If namespace FOO has an augmentation, or a substitution for another namespace,
     * and FOO is not imported by any other schema document in the pile, then the
     * root schema document imports FOO.
     * @param s -- namespace prefix or URI of root schema document
     * @throws org.mitre.niem.cmf.CMFException
     */
    public void setMessageNamespace (String s) throws CMFException {
        Namespace msgNS;
        if (null == s) return;
        boolean isURI    = s.contains(":");
        if (isURI) msgNS = m.getNamespaceByURI(s);
        else msgNS = m.getNamespaceByPrefix(s);
        if (null == msgNS) {
            throw(new CMFException(String.format(
                    "namespace %s '%s' is not in the model",
                    (isURI ? "URI" : "prefix"), s)));
        }
        messageNSuri = msgNS.getNamespaceURI();
    }
    
    // Write the Model to an XML Schema pile in directory "od".
    // Target directory should be empty.  If it isn't, you'll get munged
    // file names whenever the preferred file name already exists.
    public void writeXSD (File od) throws FileNotFoundException, ParserConfigurationException, TransformerException, IOException {
        
        // Add all the augmentation points and components to the Model object
        generateAugmentationComponents();

        // Build a map of substitutions from subproperties.
        // Remember cross-namespace subproperties; need these for import dependencies
        for (var ns : m.getNamespaceList()) { subpropDeps.put(ns.getNamespaceURI(), new ArrayList<>()); }
        for (var c : m.getComponentList()) {
            var subp = c.asProperty();
            if (null == subp) continue;            
            var p = subp.getSubPropertyOf();
            var sqn = subp.getQName();
            if (null == p) continue;
            var sns = subp.getNamespace();          // sns is the namespace; sns:subp is the subproperty
            var pns =  p.getNamespace();            // pns is the namespace; pns:p is the property
            addToPropMap(substituteMap, p, subp);   // sns:subp has @substitutionGroup pns:p
            if (sns != pns)
                subpropDeps.get(sns.getNamespaceURI()).add(pns.getNamespaceURI());
        }
        // Find the ClassType objects that will become CSCs with a FooLiteral property
        // First property in the type is a simple element named FooLiteral
        // Either type is referenceable, or has >0 other properties that are all attributes
        for (Component c : m.getComponentList()) {
            var ct = c.asClassType();
            if (null == ct) continue;
            if (ct.hasPropertyList().size() < 1) continue;
            var plist = ct.hasPropertyList();
            var prop  = plist.get(0).getProperty();
            var cscflag = (null != prop.getDatatype() && prop.getName().endsWith("Literal"));
            for (int i = 1; i < plist.size(); i++) {
                if (!plist.get(i).getProperty().isAttribute()) {
                    cscflag = false;
                    break;
                }
            }
            if (cscflag) {
                litTypes.add(ct);
                LOG.debug("litType " + ct.getQName());
            }
        }
        // Build a graph of namespaces referenced by components
        if (null != messageNSuri) refGraph = new ReferenceGraph(m.getComponentList());
        
        // Collect all Datatype objects for which we must create a simpleType declaration
        // Those are unions, lists,types of attributes, and FooDatatypes for FooLiteral properties
        var nstList = new HashSet<Datatype>();
        for (Component c : m.getComponentList()) {
            var p  = c.asProperty();
            var dt = c.asDatatype();
            if (null != dt && null != dt.getUnionOf()) {
                nstList.add(dt);
                for (var udt : dt.getUnionOf().getDatatypeList()) 
                    nstList.add(udt);
            }
            else if (null != dt && null != dt.getListOf()) {
                nstList.add(dt);
                nstList.add(dt.getListOf());
            }
            else if (null != p && p.isAttribute()) {
                nstList.add(p.getDatatype());
            }
//            else if (null != p && p.getDatatype().getName().endsWith("Datatype")) {
            else if (null != p && p.getName().endsWith("Literal")) {
                var pdt = p.getDatatype();
                if (pdt.getName().endsWith("Datatype")) nstList.add(p.getDatatype());
            }
        }
        // Don't need simple type for XSD primitives
        nstList.forEach((dt) -> {
            if (!W3C_XML_SCHEMA_NS_URI.equals(dt.getNamespaceURI()))
                needSimpleType.add(dt);
        });
        
        // Collect all the NIEM versions, establish builtin namespace prefixes
        ArrayList<String> allVers = new ArrayList<>();
        m.getNamespaceList().forEach((ns) -> {
            String nvers = ns.getNIEMVersion();
            if (null != nvers && !nvers.isBlank() && !allVers.contains(nvers)) allVers.add(nvers);
        });
        if (allVers.isEmpty()) allVers.add(getDefaultNIEMVersion());
        Collections.sort(allVers);
        Collections.reverse(allVers);   // highest versions first
        var arch = getArchitecture();
        for (String nv : allVers) {
            for (int uk = 0; uk < NIEM_BUILTIN_COUNT; uk++) {   // iterate kinds of namespace
                if (NIEM_NOTBUILTIN == uk) continue;
                String nsuri   = NamespaceKind.getBuiltinNS(uk, arch, nv);
                String uprefix;
                if (null == nsuri) continue;
                var ns = m.getNamespaceByURI(nsuri);
                if (null != ns) uprefix = ns.getNamespaceURI();         // try prefix from schema document
                else uprefix = NamespaceKind.namespaceUtil2Builtin(uk); // use default prefix for this utility         
                m.namespaceMap().assignPrefix(uprefix, nsuri);            
            }
        }
        
        // Make sure we have a new file with a unique name for all the schema documents.
        // Schema document file paths can be suggested in the Namespace object, otherwise default
        // paths will be created.  All this hinting and defaulting is not guaranteed 
        // to produce a unique file path for each namespace, so we ensure that here,
        // beginning with the model namespaces.
        for (Namespace ns : m.getNamespaceList()) {
            if (W3C_XML_SCHEMA_NS_URI.equals(ns.getNamespaceURI())) continue;   // no document for xs: namespace
            var nsuri = ns.getNamespaceURI();
            String fp = ns.getFilePath();
            var absfp = genSchemaFilePath(nsuri, fp, od);
        }
        // Now generate new unique file names for all possible utility schema documents.
        if (null != m.getNamespaceByURI(XML_NS_URI)) utilityNSuri.add(XML_NS_URI);
        for (var vers : allVers) {
            for (int util = 0; util < NIEM_NOTBUILTIN; util++) {
                if (NIEM_CLI == util) continue;
                var nsuri = NamespaceKind.getBuiltinNS(util, arch, vers);
                if (null == nsuri) continue;
                var ns = m.getNamespaceByURI(nsuri);
                if (null != ns) genSchemaFilePath(nsuri, ns.getFilePath(), od);
                else genSchemaFilePath(nsuri, null, od);
            }
        }
        // Now write the schema document for each namespace
        for (Namespace ns : m.getNamespaceList()) {
            if (ns.getKind() == NSK_BUILTIN) continue;
            if (ns.getKind() == NSK_XSD) continue;
            if (ns.getKind() == NSK_XML) continue;
            if (ns.getKind() == NSK_UNKNOWN) continue;
            var nsuri = ns.getNamespaceURI();
            var ofp    = ns2file.get(nsuri);
            var of     = new File(ofp);
            createParentDirectories(of);
            var os = new FileOutputStream(of);
            if (ns.isExternal()) writeExternalDocument(nsuri, os);
            else writeModelDocument(nsuri, os);
            os.close();
        }
        // Next, write the needed builtin schema documents to the destination
        for (String uri : utilityNSuri) {
            var ofp = ns2file.get(uri);     // file path we created earlier for this namespace
            writeBuiltinDocument(uri, ofp);
        }
        // Write the XML Catalog file if one is desired
        if (null != catPath) {
            var cf  = new File(od, catPath);
            var xcc = new XMLCatalogCreator(ns2file);
            xcc.writeCatalog(cf);
        }
    }
    
    // Writes the builtin schema document with the given namespace URI 
    // to the specified file path. Sometimes we can just copy a file from
    // our resources, sometimes we have to modify that file a bit.
    protected void writeBuiltinDocument (String nsuri, String ofp)  {
        var is = getBuiltinSchemaStream(nsuri);
        if (null == is) {
            LOG.error("can't find builtin schema document for {} in resources", nsuri);
            return;
        }
        FileOutputStream os = null;
        try {
            var of = new File(ofp);         // destination File
            createParentDirectories(of);    // starting at the schema pile root
            os = new FileOutputStream(of);
            var kind = NamespaceKind.uri2Builtin(nsuri);
            switch (kind) {
            case NIEM_STRUCTURES: writeStructuresDocument(nsuri, is, os);  break;
            case NIEM_PROXY:      writeProxyDocument(nsuri, is, os); break;
            default:              copy(is, os); break;
            }
            os.close();
        } catch (IOException | ParserConfigurationException | SAXException  ex) {
            LOG.error("can't write document {} for builtin {}: {}", ofp, nsuri, ex.getMessage());
            return;
        }
    }

    // If there are global attribute augmentations, then we need to write them
    // into the structures namespace. Also add namespace declarations and
    // imports as needed.
    protected void writeStructuresDocument (String nsuri, InputStream is, OutputStream os) throws IOException, ParserConfigurationException, SAXException {
        var db   = ParserBootstrap.docBuilder();
        var doc  = db.parse(is);
        editStructuresDocument(doc, nsuri);
        var mw = new XSDWriter(doc, os);
        mw.writeXML();
    }

    // Does the work.  Extra work for message schema documents (see ModelToMsgXSD)
    protected void editStructuresDocument (Document doc, String nsuri) {
        var appn = getBuiltinNS(NIEM_APPINFO, nsuri);                   // uri of appinfo for this structures NS
        var apre = m.getPrefix(appn);                                   // prefix of appinfo for this structures NS
        var root = doc.getDocumentElement();
        var nset = new HashSet<Namespace>();                            // namespaces augmenting this ns                                             
        for (var ar : gAttAugs) {
            if (null == ar.getGlobalAugmented()) continue;
            var part = ar.getGlobalAugmented().split(":");              // eg. structures:ObjectType
            var pre  = part[0];                                         // structures
            var name = part[1];                                         // ObjectType
            var uri  = m.getNamespaceByPrefix(pre).getNamespaceURI();   // namespace URI for structures
            if (!nsuri.equals(uri)) continue;                           // not augmenting this structures namespace
            nset.add(ar.getProperty().getNamespace());
            
            // Find the augmented component in the current namespace
            Element parent = null;
            var cl = root.getChildNodes();
            for (int i = 0; i < cl.getLength() && null == parent; i++) {
                var node = cl.item(i);
                if (ELEMENT_NODE != node.getNodeType()) continue;
                var ce = (Element)node;
                var cen = ce.getAttribute("name");
                if (name.equals(cen)) parent = ce;  // the augmented type or attributeGroup 
            }
            if (null == parent) continue;
            
            // Find the last anyAttribute child of the augmented component
            Element wild = null;
            cl = parent.getChildNodes();
            for (int i = cl.getLength()-1; i >= 0 && null == wild; i--) {
                var node = cl.item(i);
                if (ELEMENT_NODE != node.getNodeType()) continue;
                if ("anyAttribute".equals(node.getLocalName())) wild = (Element)node;
            }    
            // Build the xs:attribute element for the augmentation
            var ap   = ar.getProperty();
            var apns = ap.getNamespaceURI();
            var xa   = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            xa.setAttribute("ref", ap.getQName());
            xa.setAttributeNS(appn, apre + ":" + "augmentingNamespace", apns);
            if (ar.minOccurs() > 0) xa.setAttribute("use", "required");   
            if (null == wild) parent.appendChild(xa);   // no wildcard, append
            else parent.insertBefore(xa, wild);         // insert before wildcard
        }
        // Where to add imports? After first annotation, or as first element
        Element first = null;
        Element annot = null;
        Element after = null;
        var cl = root.getChildNodes();
        for (var i = 0; i < cl.getLength() && null == after; i++) {
            var node = cl.item(i);
            if (ELEMENT_NODE != node.getNodeType()) continue;
            if (null == first) first = (Element)node;
            if ("annotation".equals(node.getLocalName())) annot =(Element)node;
            else if (null != annot) after = (Element)node;
        }
        // Add namespace declarations and imports for all augmenting namespaces.
        // Then add a namespace declaration for appinfo.
        for (var ns : nset) {
            var pre = ns.getNamespacePrefix();
            var uri = ns.getNamespaceURI();
            var relp = makeRelativePath(nsuri, ns.getNamespaceURI());
            var ximp = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:import");
            ximp.setAttribute("namespace", ns.getNamespaceURI());
            ximp.setAttribute("schemaLocation", relp);
            if (null != after) root.insertBefore(ximp, annot);
            else root.insertBefore(ximp, first);
            root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + pre, uri); 
        }
        root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + apre, appn); 
    }
    
    // The proxy namespace includes structures, and we don't know where that
    // schema document is going to be in advance.  Fix the schemaLocation
    // as needed.
    protected void writeProxyDocument (String nsuri, InputStream is, OutputStream os) throws ParserConfigurationException, SAXException, IOException {
        var db   = ParserBootstrap.docBuilder();
        var doc  = db.parse(is);
        var root = doc.getDocumentElement();
        var cl   = root.getChildNodes();
        for (int i = 0; i < cl.getLength(); i++) {
            var node = cl.item(i);
            if (ELEMENT_NODE != node.getNodeType()) continue;
            if (!"import".equals(node.getLocalName())) continue;
            var el   = (Element)node;
            var imps = el.getAttribute("namespace");
            var relp = makeRelativePath(nsuri, imps);
            el.setAttribute("schemaLocation", relp);
        }
        var mw = new XSDWriter(doc, os);
        mw.writeXML();
    } 
    
    // The actual external schema document cannot be created from a Model.
    // So we create a placeholder document instead.
    protected void writeExternalDocument (String nsuri, OutputStream os) throws ParserConfigurationException, TransformerException, TransformerConfigurationException, IOException {
        var ns   = m.getNamespaceByURI(nsuri);
        var pre  = ns.getNamespacePrefix();
        var lang = ns.getLanguage();
        var vers = ns.getSchemaVersion();
        var db   = ParserBootstrap.docBuilder();
        var dom  = db.newDocument();
        var rel  = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:schema");
        dom.appendChild(rel); 
        rel.setAttribute("targetNamespace", nsuri);
        rel.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + pre, nsuri);
        if (null != lang && !lang.isBlank()) rel.setAttributeNS(XML_NS_URI, "lang", lang);
        if (null != vers && !vers.isBlank()) rel.setAttribute("version", vers);        

        rel.appendChild(dom.createComment(" * "));        
        rel.appendChild(dom.createComment(" * This placeholder must be replaced with the actual external schema document"));
        rel.appendChild(dom.createComment(" * "));   
        
        addDocumentation(dom, rel, null, ns.getDocumentation());
        for (var c : m.getComponentList()) {
            var p = c.asProperty();
            if (null == p) continue;
            if (ns != p.getNamespace()) continue;
            var pe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");            
            pe.setAttribute("name", p.getName());
            addDocumentation(dom, pe, null, p.getDocumentation());           
            rel.appendChild(pe);
        }
        var xsdw = new XSDWriter(dom, os);
        xsdw.writeXML();
        os.close();
    }
    
    // Write the schema document for the specified namespace
    protected void writeModelDocument (String nsuri, OutputStream os) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        root = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:schema");
        root.setAttribute("targetNamespace", nsuri);
        dom.appendChild(root);
        
        var  ns       = m.getNamespaceByURI(nsuri);   // current namespace object
        var arch      = getArchitecture();
        nsNIEMVersion = ns.getNIEMVersion();
        if (null == nsNIEMVersion) nsNIEMVersion = getDefaultNIEMVersion();

        appinfoURI    = NamespaceKind.getBuiltinNS(NIEM_APPINFO, arch, nsNIEMVersion);
        clsaURI       = NamespaceKind.getBuiltinNS(NIEM_CLSA, arch, nsNIEMVersion);
        proxyURI      = NamespaceKind.getBuiltinNS(NIEM_PROXY, arch, nsNIEMVersion);
        structURI     = NamespaceKind.getBuiltinNS(NIEM_STRUCTURES, arch, nsNIEMVersion);
        appinfoPrefix = m.getPrefix(appinfoURI);
        clsaPrefix    = m.getPrefix(clsaURI);
        proxyPrefix   = m.getPrefix(proxyURI);
        structPrefix  = m.getPrefix(structURI);
        nsNSdeps    = new HashSet<>();
        nsAttributeDecls = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        nsElementDecls   = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);        
        nsTypedefs  = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        nsNSdeps.addAll(subpropDeps.get(nsuri));
        nsNSdeps.add(structURI);
        utilityNSuri.add(structURI);
        LOG.debug("Writing schema document for {}", nsuri);
        
        // Add namespace version number and language attribute, if specified
        String lang = ns.getLanguage();
        String nsv  = ns.getSchemaVersion();
        if (null != lang && !lang.isBlank()) root.setAttributeNS(XML_NS_URI, "lang", lang);
        if (null != nsv && !nsv.isBlank())   root.setAttribute("version", nsv);
        
        // Add conformance target assertions, if any
        String cta = fixConformanceTargets(m.getNamespaceByURI(nsuri).getConfTargets());
        if (null != cta && !cta.isBlank()) {
            String ctns = NamespaceKind.getBuiltinNS(NIEM_CTAS, arch, nsNIEMVersion);
            String ctprefix;
            ctprefix = m.getPrefix(ctns);
            root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:"+ctprefix, ctns);
            root.setAttributeNS(ctns, ctprefix + ":" + CONFORMANCE_ATTRIBUTE_NAME, cta);
        }
        nsNSdeps.add(nsuri);                    // always define prefix for target namespace 
        
        // Create annotation element for documentation and local term appinfo
        Element ae = null;
        ae = addDocumentation(dom, root, ae, ns.getDocumentation());
        if (!ns.localTermList().isEmpty()) {
            ae = addAnnotation(dom, root, ae);
            var ai = addAppinfo(dom, ae, null);
            for (var lt : ns.localTermList()) {
                addLocalTerm(dom, ai, lt);
            }
        }
        // Create type definitions for ClassType objects
        // Then create typedefs for Datatype objects (when not already created)
        // Remember external namespaces when encountered
        for (Component c : m.getComponentList()) 
            createComplexTypeFromClass(dom, nsuri, c.asClassType());
        for (Component c : m.getComponentList()) 
            createComplexTypeFromDatatype(dom, nsuri, c.asDatatype());
        for (Datatype dt : needSimpleType)
            createSimpleTypeFromDatatype(dom, nsuri, dt);
        
        // Create elements and attributes for Property objects
        for (Component c : m.getComponentList()) createDeclaration(dom, nsuri, c.asProperty());
        
        // Add a namespace declaration for each namespace dependency
        for (var dnsuri : nsNSdeps) {
            String dnspre = m.getPrefix(dnsuri);
            root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + dnspre, dnsuri);                
        }
        // Construct a list of Namespace objects for the namespaces to be imported.
        // Soon we will sort those namespaces into a pleasing order.
        // Possibly create a temporary Namespace for builtins.
        List<Namespace> orderedDeps = new ArrayList<>();
        for (var uri : nsNSdeps) {
            var dns = m.getNamespaceByURI(uri);
            if (null == dns) {
                var kind = NamespaceKind.uri2Kind(uri);
                if (NSK_BUILTIN == kind) {
                    var pre = m.getPrefix(uri);
                    dns = new Namespace(pre, uri);
                    dns.setKind(kind);
                }
            }
            if (null != dns) orderedDeps.add(dns);
        }
        // If this is the message root namespaces, add imports for everything in the schema
        // that is not already transitively imported.
        if (null != messageNSuri && nsuri.equals(messageNSuri)) {
            var rset = refGraph.reachableFrom(ns);
            for (var ons : m.getNamespaceList()) {
                if (NSK_XML != ons.getKind() && !orderedDeps.contains(ons))
                    orderedDeps.add(ons);
            }
        }
        // Import order: extension, domain, core, otherniem, builtin, external.
        // Sort by prefix within each class.
        Collections.sort(orderedDeps, new NamespaceImportCmp());

        // Add an import element for all of the namespace dependencies
        for (var dns : orderedDeps) {
            var dnsuri = dns.getNamespaceURI();
            if (W3C_XML_SCHEMA_NS_URI.equals(dnsuri)) continue;     // don't import XSD
            if (appinfoURI.equals(dnsuri)) continue;                // don't import appinfo
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
        nsAttributeDecls.forEach((name,element) -> {
            root.appendChild(element);
        });
        nsElementDecls.forEach((name,element) -> {
            root.appendChild(element);
        });
        var xsdw = new XSDWriter(dom, os);
        xsdw.writeXML();;
    }
    
    protected String makeRelativePath (String from, String to) {
        var fromP = Paths.get(ns2file.get(from));
        var toP   = Paths.get(ns2file.get(to));
        var fpar  = fromP.getParent();
        if (null != fpar) {
            var sloc = fpar.relativize(toP).toString();
            sloc = separatorsToUnix(sloc);
            return sloc;
        }
        var sloc = toP.toString();
        sloc = separatorsToUnix(sloc);
        return sloc;
    }
    
    // Iterate through all the ClassType objects in the model; create an
    // augmentation point property for each augmentable ClassType.  Need to 
    // do this for the whole model before trying to create augmentation elements.
//    protected void generateAugmentationPoints () {
//        for (var c : m.getComponentList()) {
//            var targCT = c.asClassType();
//            if (null == targCT) continue;               // not a ClassType
//            if (!targCT.isAugmentable()) continue;      // not augmentable
//            var targCTns   = targCT.getNamespace();
//            var targCTName = targCT.getName();                                      // FooType
//            var targName   = targCTName.substring(0, targCTName.length()-4);        // FooType -> Foo
//            var augPtName  = targName + "AugmentationPoint";                        // FooType -> FooAugmentationPoint
//            var augPoint = new Property(targCTns, augPtName);
//            augPoint.setDocumentation("An augmentation point for " + targCTName + ".");
//            augPoint.setIsAbstract(true);
//            m.addComponent(augPoint);
//            var hp = new HasProperty();
//            hp.setProperty(augPoint);
//            hp.setMinOccurs(0);
//            hp.setMaxUnbounded(true);
//            targCT.addHasProperty(hp);        
//        }        
//    }

    // Use the augmentation info in each Namespace to generate augmentation 
    // types, augmentation elements, and augmentation points.
    protected void generateAugmentationComponents () {
        
        // Iterate through all the ClassType objects in the model; create an
        // augmentation point property for each augmentable ClassType.  Need to 
        // do this for the whole model before trying to create augmentation elements.
        for (var c : m.getComponentList()) {
            var targCT = c.asClassType();
            if (null == targCT) continue;               // not a ClassType
            if (!targCT.isAugmentable()) continue;      // not augmentable
            var targCTns   = targCT.getNamespace();
            var targCTName = targCT.getName();                                      // FooType
            var targName   = targCTName.substring(0, targCTName.length()-4);        // FooType -> Foo
            var augPtName  = targName + "AugmentationPoint";                        // FooType -> FooAugmentationPoint
            var augPoint = new Property(targCTns, augPtName);
            augPoint.setDocumentation("An augmentation point for " + targCTName + ".");
            augPoint.setIsAbstract(true);
            m.addComponent(augPoint);
            var hp = new HasProperty();
            hp.setProperty(augPoint);
            hp.setMinOccurs(0);
            hp.setMaxUnbounded(true);
            targCT.addHasProperty(hp);        
        } 
        // Now use the augmentation info in each Namespace to generate augmentation 
        // types, augmentation elements, and augmentation points.
        for (var ns : m.getNamespaceList()) {
            var arch = getArchitecture();
            var vers = ns.getNIEMVersion();
            var strURI = NamespaceKind.getBuiltinNS(NIEM_STRUCTURES, arch, vers);
            var strNS  = m.getNamespaceByURI(strURI);
            var strPre = m.getPrefix(strURI);

            // Create map from augmented class QName to list of its augmentations in this namespace
            var class2Aug = new HashMap<String, List<AugmentRecord>>();
            for (var ar : ns.augmentList()) {
                    String ctqn = null;
                    switch (ar.getGlobalAug()) {
                    case AUG_NONE:   
                        ctqn = ar.getClassType().getQName(); 
                        break;
                    case AUG_SIMPLE: 
                        gAttAugs.add(ar); 
                        break;
                    case AUG_OBJECT:
                    case AUG_ASSOC:
                        if (ar.getProperty().isAttribute()) {
                            gAttAugs.add(ar);
                            break;
                        }
                        hasGEAug = true;
                        var which = AUG_OBJECT == ar.getGlobalAug() ? "Object" : "Association";
                        var apt = new Property(strNS, which + "AugmentationPoint");
                        apt.setIsAbstract(true);
                        ctqn = strPre + ":" + which + "Type";
                        m.addComponent(apt);
                        break;
                    default:
                    }
                    if (null == ctqn) continue;
                    var arlist = class2Aug.get(ctqn);
                    if (null == arlist) arlist = new ArrayList<>();
                    class2Aug.put(ctqn, arlist);
                    arlist.add(ar);
            }
            // Iterate over the augmented classes to create augmentation components
            for (var targQN : class2Aug.keySet()) {                                 // ns:FooType
                var part = targQN.split(":");
                var targPre = part[0];                                              // ns
                var targCTName = part[1];                                           // FooType
                var augElName = targCTName.replaceFirst("Type$", "Augmentation");   // FooAugmentation
                var augTpName = augElName + "Type";                                 // FooAugmentationType
                var augPtName = augElName + "Point";                                // FooAugmentationPoint
                var augPoint = m.getProperty(targPre + ":" + augPtName);            // augmentation point Property

                // Create ordered list of augmentation records for this class
                var arlist = class2Aug.get(targQN);
                Collections.sort(arlist, Comparator.comparing(AugmentRecord::indexInType));

                // All of the augmentation elements have index -1, so they will be
                // first in the ordered list.  Handle those now.
                int arIndex;
                for (arIndex = 0; arIndex < arlist.size(); arIndex++) {
                    var ar = arlist.get(arIndex);
                    if (ar.indexInType() >= 0) break;       // all augmentation elements processed
                    else {
                        var augp = ar.getProperty();        // augmentation element?
                        if (augp.isAttribute()) continue;   // no, it's an attribute
                        if (null != augp.getSubPropertyOf()) {
                            var opqn = augp.getSubPropertyOf().getQName();
                            LOG.error("{} augments {} but is already subPropertyOf {}", augp.getQName(), targQN, opqn);
                        } 
                        else augp.setSubPropertyOf(augPoint);
                    }
                }
                // If no properties left, we're done; don't need an augmentation type or element
                if (arIndex >= arlist.size()) continue;

                // Otherwise create an augmentation type for remaining augmentation records
                String typePhrase = typeNameToPhrase(targCTName);
                ClassType augCT = new ClassType(ns, augTpName);
                augCT.setDocumentation("A data type for additional information about " + typePhrase + ".");
                while (arIndex < arlist.size()) {
                    var ar = arlist.get(arIndex++);
                    var hp = new HasProperty();
                    var p  = ar.getProperty();
//                    addToPropMap(substituteMap, augPoint, p);
                    hp.setProperty(p);
                    hp.setMinOccurs(ar.minOccurs());
                    hp.setMaxOccurs(ar.maxOccurs());
                    hp.setMaxUnbounded(ar.maxUnbounded());
                    hp.setOrderedProperties(ar.orderedProperties());
                    augCT.addHasProperty(hp);
                }
                m.addComponent(augCT);

                // Create an augmentation element
                var augEP = new Property(ns, augElName);
                augEP.setDocumentation("Additional information about " + typePhrase + ".");
                augEP.setClassType(augCT);
                augEP.setSubPropertyOf(augPoint);
                addToPropMap(substituteMap, augPoint, augEP);
                m.addComponent(augEP);
            }
        }
    }    

    // Add @appinfo:appatt="value" to an element.  Get the right namespace prefix and URI
    // for the current document.  Add the namespace declaration for appinfo, but
    // don't import it. Only works while executing writeModelDocument.
    protected void addAppinfoAttribute (Document dom, Element e, String appatt, String value) {
        var appinfoQName = appinfoPrefix + ":" + appatt;
        root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:"+appinfoPrefix, appinfoURI);
        e.setAttributeNS(appinfoURI, appinfoQName, value);
        nsNSdeps.add(appinfoURI);
        utilityNSuri.add(appinfoURI);
    }
    
    // Create a complex type declaration from a ClassType object
    // This can have simpleContent if the ClassType has a literal property
    // Otherwise it has complexContent
    protected void createComplexTypeFromClass (Document dom, String nsuri, ClassType ct) { 
        if (null == ct) return;
        var cname = ct.getName();
        if (nsTypedefs.containsKey(cname)) return;              // already created
        if (!nsuri.equals(ct.getNamespaceURI())) return;        // different namespace
        var cte = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        var exe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
        cte.setAttribute("name", cname);
        var ae = addDocumentation(dom, cte, null, ct.getDocumentation());
        if (ct.isAbstract())   cte.setAttribute("abstract", "true");
        if (ct.isDeprecated()) addAppinfoAttribute(dom, cte, "deprecated", "true");
        nsTypedefs.put(cname, cte);
        
        // Create simple content for a class with a FooLiteral property
        if (litTypes.contains(ct)) {
            var plist = ct.hasPropertyList();
            var lp    = plist.get(0).getProperty();     // FooLiteral property
            var lpt   = lp.getDatatype();               // datatype for FooLiteral
            var lptqn = proxifiedDatatypeQName(lpt);
            if (needSimpleType.contains(lpt)) {
                lptqn = lptqn.replaceFirst("Type$", "SimpleType");
                lptqn = lptqn.replaceFirst("Datatype$", "SimpleType");
            }
            // Handle FooLiteral datatype that is empty restriction of XSD type
            else if (null == lpt.getListOf() && null == lpt.getUnionOf()) {
                var r = lpt.getRestrictionOf();
                if (null != r && r.getFacetList().isEmpty()) {
                    var rbt = r.getDatatype();
                    if (W3C_XML_SCHEMA_NS_URI.equals(rbt.getNamespaceURI()))
                        lptqn = proxifiedDatatypeQName(r.getDatatype());
                }
            }
            litProps.add(lp);       // don't need FooLiteral, don't create it later
            var sce = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
            exe.setAttribute("base", lptqn);
            cte.appendChild(sce);   // xs:complexType has xs:simpleContent
            sce.appendChild(exe);   // xs:simpleContent has xs:extension
            
            // Add SimpleObjectAttributeGroup to xs:extension
            var agqn = structPrefix + ":SimpleObjectAttributeGroup";
            var age = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attributeGroup");
            age.setAttribute("ref", agqn);
            exe.appendChild(age); 

            // Set appinfo:referenceCode if needed
            if (1 == ct.hasPropertyList().size()) {
                var rcode = ct.getReferenceCode();
                if (null != rcode && !"NONE".equals(rcode))
                    addAppinfoAttribute(dom, cte, "referenceCode", rcode);
            }
        }
        // Otherwise create complex content with xs:sequence of element refs.
        else {
            var cce = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexContent");     
            var sqe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:sequence");
            exe.appendChild(sqe);   // xs:extension has xs:sequence
            cce.appendChild(exe);   // xs:complexContent has xs:extension
            cte.appendChild(cce);   // xs:complexType has xs:complexContent
            
            // Set xs:extension base from the class parent or a structures type
            var basect  = ct.getExtensionOfClass();
            if (null == basect) exe.setAttribute("base", structuresBaseType(ct.getName()));
            else {
                exe.setAttribute("base", basect.getQName());
                nsNSdeps.add(basect.getNamespace().getNamespaceURI());
            }       
            // Add element refs for element properties (but not element augmentations)
            for (HasProperty hp : ct.hasPropertyList()) {
                if (!hp.augmentingNS().isEmpty()) continue;
                if (hp.getProperty().isAttribute()) continue;
                if (null == sqe) sqe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:sequence");
                addElementRef(dom, sqe, hp);
            }
            // Set appinfo:referenceCode if needed (never for augmentation types)
            if (!ct.getName().endsWith("AugmentationType")) {
                var rc = ct.getReferenceCode();
                if (null == rc) rc = "";
                switch (rc) {
                case "NONE": addAppinfoAttribute(dom, cte, "referenceCode", "NONE"); break;
                case "REF":  addAppinfoAttribute(dom, cte, "referenceCode", "REF"); break;
                case "URI":  addAppinfoAttribute(dom, cte, "referenceCode", "URI"); break;
                case "ANY": 
                default:     // ANY is the default for complex content in source or NIEM5 schema
                    break;
                }
            }
        }
        // For simple or complex content, build list of attribute properties, 
        // then add them to xs:extension.
        var attList = new ArrayList<AttProp>();
        buildAttributeList(attList, ct);
        addAttributeElements(dom, exe, attList);
    }

    // Create <xs:element ref="foo"> from a HasProperty object, append it to a sequence or choice
    protected void addElementRef (Document dom, Element parent, HasProperty hp) {
        if (hp.getProperty().isAttribute()) return;
        addElementRef(dom, parent, hp, hp.getProperty());
    }

    // Create element ref for a QName, with minOccurs = maxOccurs = 1.
    protected void addElementOnceRef (Document dom, Element parent, Property p) {
        var hp = new HasProperty();
        hp.setMaxOccurs(1);
        hp.setMinOccurs(1);
        addElementRef(dom, parent, hp, p);
    } 
    
    // Create element ref for a QName, with minOccurs = maxOccurs = 1.
    protected void addElementAnyRef (Document dom, Element parent, Property p) {
        var hp = new HasProperty();
        hp.setMinOccurs(0);
        hp.setMaxUnbounded(true);
        addElementRef(dom, parent, hp, p);
    } 
    
    // Create an element ref for Property p, using min/maxoccurs from hp
    protected void addElementRef (Document dom, Element parent, HasProperty hp, Property p) {
        if (p.isAttribute()) return;
        var hpe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
        hpe.setAttribute("ref", p.getQName());
        if (1 != hp.minOccurs()) hpe.setAttribute("minOccurs", "" + hp.minOccurs());
        if (hp.maxUnbounded())   hpe.setAttribute("maxOccurs", "unbounded");
        else if (1 != hp.maxOccurs()) hpe.setAttribute("maxOccurs", "" + hp.maxOccurs());
        if (hp.orderedProperties())
            addAppinfoAttribute(dom, hpe, "orderedPropertyIndicator", "true");
        addDocumentation(dom, hpe, null, hp.getDefinition());
        parent.appendChild(hpe);        
        nsNSdeps.add(p.getNamespaceURI());
    }
    
    // Build a list of attribute properties to be added to a complex type.
    // Why this complexity? In a message schema a complex type has the attribute properties
    // from all the inherited classes. Need to collect them all (and handle
    // duplicates) before appending xs:attribute elements.
    protected class AttProp implements Comparable<AttProp> {
        protected Property ap             = null;
        protected boolean required        = false;
        protected Set<Namespace> augNSset = new HashSet<>();
        protected AttProp () { };
        protected AttProp (Property p, boolean req) { ap = p; required = req; }
        protected String getAugNSlist () { 
            var sep = "";
            var rv  = "";
            for (var ns : augNSset) {
                rv  = rv + sep + ns.getNamespaceURI();
                sep = " ";
            }
            return rv;
        }
        @Override
        public int compareTo(AttProp o) { return o.ap.getQName().compareTo(this.ap.getQName()); }
    }
    protected void buildAttributeList (List<AttProp> alist, ClassType ct) {
        for (HasProperty hp : ct.hasPropertyList()) {
            var p = hp.getProperty();
            if (!p.isAttribute()) continue;
            AttProp arec = null;
            for (var a : alist) if (a.ap == p) arec = a;
            if (null == arec) arec = new AttProp(p, hp.minOccurs() > 0);
            
            // Build list of augmenting namespaces if this property is an augmentation.
            // Ignore properties from an augmentation type (those have index >= 0).
            for (var ns : hp.augmentingNS()) {
                var ar = ns.findAugmentRecord(ct, p);
                if (null != ar && ar.indexInType() < 0) arec.augNSset.add(ns);
            }
            if (!hp.augmentingNS().isEmpty() && arec.augNSset.isEmpty()) continue;
            nsNSdeps.add(p.getNamespace().getNamespaceURI()); 
            alist.add(arec);
        }        
    }
    // Add the xs:attribute elements specified in the AttProp list.
    protected void addAttributeElements (Document dom, Element attParent, List<AttProp> alist) {
        Collections.sort(alist);
        for (var arec : alist) {
            var ae = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            ae.setAttribute("ref", arec.ap.getQName());
            if (arec.required) ae.setAttribute("use", "required");
            if (!arec.augNSset.isEmpty())
                addAppinfoAttribute(dom, ae, "augmentingNamespace", arec.getAugNSlist());
            attParent.appendChild(ae);
            nsNSdeps.add(arec.ap.getNamespaceURI());
        }
    }
    
    // Create a complex type declaration from a Datatype object (FooType)
    protected void createComplexTypeFromDatatype (Document dom, String nsuri, Datatype dt) {
        if (null == dt) return;
        var cname = dt.getName().replaceFirst("Datatype$", "Type");     // FooDatatype -> FooType
        if (nsTypedefs.containsKey(cname)) return;                      // already created xs:ComplexType for this
        if (!nsuri.equals(dt.getNamespaceURI())) return;                // datatype is not in this namespace
        if (W3C_XML_SCHEMA_NS_URI.equals(dt.getNamespaceURI())) return; // don't create XSD builtins
        
        var cte = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        cte.setAttribute("name", cname);        
        var ae = addDocumentation(dom, cte, null, dt.getDocumentation());
        if (dt.isDeprecated()) addAppinfoAttribute(dom, cte, "deprecated", "true");
        
        var sce = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
        cte.appendChild(sce);   // xs:complexType has xs:simpleContent

        if (null != dt.getCodeListBinding()) {
            ae = addAnnotation(dom, cte, ae);
            var ap = addAppinfo(dom, ae, null);
            var cb = addCodeListBinding(dom, ap, nsuri, dt.getCodeListBinding());
        }        
        if (needSimpleType.contains(dt)) {
            var stqn = dt.getQName().replaceFirst("Type$", "SimpleType");
            addEmptyExtensionElement(dom, sce, dt, stqn);
        }
        else {
            var r     = dt.getRestrictionOf();
            var rbdt  = r.getDatatype();
            var rbdqn = proxifiedDatatypeQName(rbdt);
            var rfl   = r.getFacetList();
            if (W3C_XML_SCHEMA_NS_URI.equals(rbdt.getNamespaceURI()) && rfl.isEmpty())
                addEmptyExtensionElement(dom, sce, rbdt, rbdt.getQName());
            else
                addRestrictionElement(dom, sce, dt, r.getDatatype(), rbdqn);
        }
        nsTypedefs.put(cname, cte);
    }
    
    // Adds <xs:extension base="baseQN"> with SimpleObjectAttributeGroup
    // Or adds <xs:extension base="niem-xs:name"/>
    protected void addEmptyExtensionElement (Document dom, Element sce, Datatype dt, String baseQN) {
        var exe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
        if (W3C_XML_SCHEMA_NS_URI.equals(dt.getNamespaceURI())) {
            baseQN = proxifiedDatatypeQName(dt);
        }
        else {
            var atg = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attributeGroup");
            atg.setAttribute("ref", structPrefix + ":SimpleObjectAttributeGroup");
            exe.appendChild(atg);        
        }
        exe.setAttribute("base", baseQN);
        sce.appendChild(exe);   
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

        Element ae = null;
        var ste = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleType");       
        ae = addDocumentation(dom, ste, ae, dt.getDocumentation());
        if (null != dt.getCodeListBinding()) {
            ae = addAnnotation(dom, ste, ae);
            var ap = addAppinfo(dom, ae, null);
            var cb = addCodeListBinding(dom, ap, nsuri, dt.getCodeListBinding());
        }
        if (null != dt.getUnionOf()) addUnionElement(dom, ste, dt);
        else if (null != dt.getListOf()) addListElement(dom, ste, dt);
        else if (null != dt.getRestrictionOf()) {
            var r = dt.getRestrictionOf();
            addRestrictionElement(dom, ste, dt, r.getDatatype(), r.getDatatype().getQName());
        }
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
            String udtns    = udt.getNamespaceURI();
            members.append(sep).append(memberQN);
            nsNSdeps.add(udtns);
            sep = " ";
        }
        une.setAttribute("memberTypes", members.toString());
        ste.appendChild(une);        
    }
    
    protected void addListElement (Document dom, Element ste, Datatype bdt) {
        var lse  = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:list");
        var idt  = bdt.getListOf();
        var idtns = idt.getNamespaceURI();
        nsNSdeps.add(idtns);
        lse.setAttribute("itemType", maybeSimpleTypeQName(idt));
        ste.appendChild(lse);
    }
    
    protected void addRestrictionElement (Document dom, Element ste, Datatype bdt, Datatype rbdt, String rbqn) {
        RestrictionOf r = bdt.getRestrictionOf();
        Element rse = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:restriction");
        rse.setAttribute("base", rbqn);
        Collections.sort(r.getFacetList());
        for (Facet f : r.getFacetList()) {
            String fk = f.getFacetKind();
            String ename = "xs:" + toLowerCase(fk.charAt(0)) + fk.substring(1);
            Element fce = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, ename);
            fce.setAttribute("value", f.getStringVal());
            addDocumentation(dom, fce, null, f.getDefinition());
            rse.appendChild(fce);
        }
        nsNSdeps.add(rbdt.getNamespaceURI());
        ste.appendChild(rse);
    }
    
    // Returns QName for FooSimpleType if that type exists, otherwise QName for FooType
    protected String maybeSimpleTypeQName (Datatype dt) {
        if (W3C_XML_SCHEMA_NS_URI.equals(dt.getNamespaceURI())) return dt.getQName();
        String dtqn   = dt.getQName();
        String dtbase = dtqn.replaceFirst("Type$", "");
        String dtsqn  = dtbase + "SimpleType";
        if (needSimpleType.contains(dt)) return dtsqn;
        return dtqn;
    }
    
    // Convert "xs:foo" to "niem-proxy:foo"
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
        var pqn   = p.getQName();
        var isnil = isPropertyNillable(p);
        var rcode = p.getReferenceCode();
        if (null == rcode)       rcode = "";
        if (isnil)               pe.setAttribute("nillable", "true");
        if (p.isAbstract())      pe.setAttribute("abstract", "true");
        if (p.isDeprecated())    addAppinfoAttribute(dom, pe, "deprecated", "true");
        if (p.isRefAttribute())  addAppinfoAttribute(dom, pe, "referenceAttributeIndicator", "true");
        if (p.isRelationship())  addAppinfoAttribute(dom, pe, "relationshipPropertyIndicator", "true");
        if (!rcode.isBlank()) {
            if (isnil && !"ANY".equals(rcode))   addAppinfoAttribute(dom, pe, "referenceCode", rcode); // ANY is default for nillable
            if (!isnil && !"NONE".equals(rcode)) addAppinfoAttribute(dom, pe, "referenceCode", rcode); // NONE is default for non-nillable
        }
        var ae = addDocumentation(dom, pe, null, p.getDocumentation());
        // Handle property from element with type from structures; blech
        if (null == pct && null == pdt && !p.isAbstract() && !m.getNamespaceByURI(nsuri).isExternal()) {
            String pdtQN = structuresBaseType(p.getName());
            if (null != pdtQN) pe.setAttribute("type", pdtQN);
        }
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
        handleSubproperty(p, pe);
        if (isAttribute) nsAttributeDecls.put(p.getName(), pe);
        else nsElementDecls.put(p.getName(), pe);
    }
    
    protected void handleSubproperty (Property p, Element pe) {
        var baseP = p.getSubPropertyOf();
        if (null == baseP) return;
        pe.setAttribute("substitutionGroup", baseP.getQName());
        nsNSdeps.add(baseP.getNamespaceURI());
    }
    
    protected Element addCodeListBinding (Document dom, Element e, String nsuri, CodeListBinding clb) {
        Element cbe = dom.createElementNS(clsaURI, clsaPrefix + ":" + "SimpleCodeListBinding");
        cbe.setAttribute("columnName", clb.getColumn());
        cbe.setAttribute("codeListURI", clb.getURI());
        if (clb.getIsConstraining()) cbe.setAttribute("constrainingIndicator", "true");
        e.appendChild(cbe);
        return cbe;
    }
    
    // Returns a InputStream object for the proper schema document for builtin and XML namespaces.
    // Look in the JAR first, then in the "share" directory (FIXME)
    protected InputStream getBuiltinSchemaStream (String nsuri) {
        String bfn;
        if (XML_NS_URI.equals(nsuri)) bfn = new String("xml.xsd");      
        else {
            var vers = NamespaceKind.uri2Version(nsuri) + ".0";     // eg. "6.0"
            int util = NamespaceKind.uri2Builtin(nsuri);            // eg. NIEM_PROXY
            var bfp  = NamespaceKind.defaultBuiltinPath(util);      // eg. "utility/structures.xsd"
            bfn = FilenameUtils.getName(bfp);                       // eg. "structures.xsd"
            bfn = vers + "/" + bfn;                                 // eg. "6.0/structures.xsd"     
        }
        // Running from IDE?  Open stream 
        InputStream is = null;
        var sdirfn = ModelToXSD.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (!sdirfn.endsWith(".jar")) {
            sdirfn = FilenameUtils.concat(sdirfn, "../../../../src/main/resources/xsd");
            var sf = new File(sdirfn, bfn);
            try {
                is = new FileInputStream(sf);
            } catch (FileNotFoundException ex) {
                LOG.error("writing document for {}, could not open {}: {}", nsuri, sf.toString(), ex.getMessage());
            }
        }
        // Running from JAR? Get resource
        else {
            is = ModelToXSD.class.getResourceAsStream("/xsd/" + bfn);
            if (null == is) LOG.error("writing document for {}: can't open resource /xsd/{}", nsuri, bfn);
        }
        if (null == is) return InputStream.nullInputStream();
        return is;
    }
    
    // Generate a file path for thie namespace schema document
    // Use a default relative path if no hint provided.
    // Mung the file path as needed to make path unique and not existing
    // Return the path to the created file (including outputDir)
    private static Pattern nsnamePat = Pattern.compile("/(\\w+)/\\d(\\.\\d+)*(/#)?$");
    private String genSchemaFilePath (String nsuri, String hint, File outputDir) throws IOException {
        if (ns2file.containsKey(nsuri)) return ns2file.get(nsuri);
        if (null == hint) {
            int util = NamespaceKind.uri2Builtin(nsuri);
            if (util < NIEM_NOTBUILTIN) hint = NamespaceKind.defaultBuiltinPath(util);
            else {
                var m = nsnamePat.matcher(nsuri);
                if (m.find()) hint = m.group(1);
                else hint = "schema";                    
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
        LOG.debug(String.format("genSchemaFile %s -> %s", nsuri, fpath));
        return fpath;
    }
    
    // Adds annotation element to component if needed.
    protected Element addAnnotation (Document dom, Element component, Element ae) {
        if (null != ae) return ae;
        ae = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        component.appendChild(ae);
        return ae;
    }
    
    // Adds appinfo element to annotation if needed.
    // Returns the appinfo element.
    protected Element addAppinfo (Document dom, Element ae, Element ap) {
        if (null != ap) return ap;
        ap = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:appinfo");
        ae.appendChild(ap);
        return ap;
    }
    
    // Adds documentation element and maybe annotation element to component if needed.
    // Returns the annotation element.
    // Does nothing if doc is null or blank.
    protected Element addDocumentation (Document dom, Element component, Element ae, String doc) {
        if (null == doc || doc.isBlank()) return ae;
        ae = addAnnotation(dom, component, ae);
        var de = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:documentation");
        de.setTextContent(doc);
        ae.appendChild(de);
        return ae;
    }
    
    // Adds an appinfo:LocalTerm element to the xs:appinfo element
    protected void addLocalTerm (Document dom, Element ap, LocalTerm lt) {
        if (null == lt) return;
        var lte = dom.createElementNS(appinfoURI, appinfoPrefix + ":LocalTerm");
        setAttribute(lte, "definition", lt.getDefinition());
        setAttribute(lte, "literal", lt.getLiteral());
        setAttribute(lte, "sourceURIs", lt.getSourceURIs());
        setAttribute(lte, "term", lt.getTerm());
        for (var cite : lt.citationList()) {
            var ce = dom.createElementNS(appinfoURI, appinfoPrefix + ":SourceText");
            ce.setTextContent(cite);
            lte.appendChild(ce);
        }
        nsNSdeps.add(appinfoURI);
        utilityNSuri.add(appinfoURI);        
        ap.appendChild(lte);       
    }
    
    // Convience routine for possibly null attribute values
    protected void setAttribute(Element e, String n, String x) {
        if (null == x) return;
        e.setAttribute(n, x);
    }
    
    protected class NamespaceImportCmp implements Comparator<Namespace> {
        @Override
        public int compare(Namespace a, Namespace b) {
            int ak = a.getKind();
            int bk = b.getKind();
            if (ak != bk) return Integer.compare(ak, bk);
            return a.getNamespacePrefix().compareTo(b.getNamespacePrefix());
        }
    }
    
    // Convert a camel-case type name to a noun phrase
    // "TelephoneNumberType" -> "a telephone number"
    protected String typeNameToPhrase (String typeName) {
        var sep  = "";
        var buf  = new StringBuilder();
        var name = typeName.replaceFirst("Type$", "");          // FooType -> Foo
        for (int i = 0; i < name.length(); i++) {
            var ch = name.charAt(i);
            if (isUpperCase(ch)) {
                buf.append(sep);
                sep = " ";
            }
            buf.append(toLowerCase(ch));            
        }
        var res = buf.toString();
        res = articalize(res);
        return res;
    }
    
    protected void addToPropMap (Map<Property,Set<Property>>map, Property key, Property val) {
        var set = map.get(key);
        if (null == set) {
            set = new HashSet<Property>();
            map.put(key, set);
        }
        set.add(val);
    }
    
    // Difference between subset schemas and message schemas is implemented by 
    // overriding these subroutines
    
    protected String getArchitecture ()       { return "NIEM6"; }
    protected String getDefaultNIEMVersion () { return "6"; }
    
    protected String fixConformanceTargets (String nsuri) {
        return m.getNamespaceByURI(nsuri).getConfTargets();
    }
    
    protected String fixSchemaVersion (String nsuri) {
        return m.getNamespaceByURI(nsuri).getSchemaVersion();
    }
    
    protected abstract boolean isPropertyNillable (Property p);
    
    protected String structuresBaseType (String compName) {
        var bt = structPrefix + ":";
        if (compName.endsWith("Association") || compName.endsWith("AssociationType")) bt = bt + "AssociationType";
        else if (compName.endsWith("Augmentation") || compName.endsWith("AugmentationType")) bt = bt + "AugmentationType";        
        else if (compName.endsWith("Adapter") || compName.endsWith("AdapterType")) bt = bt + "AdapterType";         
        else bt = bt + "ObjectType";
        return bt;
    }
}
