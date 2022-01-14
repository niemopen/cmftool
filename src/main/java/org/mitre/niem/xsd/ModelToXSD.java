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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.NIEMConstants.CONFORMANCE_ATTRIBUTE_NAME;
import static org.mitre.niem.NIEMConstants.XML_NS_URI;
import static org.mitre.niem.NIEMConstants.XSD_NS_URI;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import static org.mitre.niem.cmf.Component.C_CLASSTYPE;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.RestrictionOf;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_APPINFO;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_CONFORMANCE_TARGETS;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_PROXY;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_STRUCTURES;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinDocumentFile;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinNamespaceURI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class for writing a Model as a NIEM XML schema pile.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXSD {
    static final Logger LOG = LogManager.getLogger(ModelToXSD.class);
        
    private final Model m;
    private final ModelExtension me;
    private final Map<String,Namespace> builtinNSmap = new HashMap<>();
    
    public ModelToXSD (Model m, ModelExtension me) {
        this.m = m;
        this.me = me;
    }
    
    public void writeXSD (File od) throws FileNotFoundException, ParserConfigurationException, TransformerException, IOException {
        // Write a schema document for each namespace
        for (Namespace ns : m.getNamespaceList()) {
            if (XSD_NS_URI.equals(ns.getNamespaceURI())) continue;
            if (ns.isExternal()) continue;
            String fn = me.getDocumentFilepath(ns.getNamespaceURI());
            File of = new File(od, fn);
            createParentDirectories(of);
            FileWriter ofw = new FileWriter(of);
            writeDocument(ns.getNamespaceURI(), ofw);
            ofw.close();
        }
        // Copy builtin schema documents to the destination
        for (String uri : builtinNSmap.keySet()) {
            String dfp = me.getDocumentFilepath(uri);
            File df = new File(od, dfp);
            File sf = getBuiltinDocumentFile(uri);
            createParentDirectories(df);
            copyFile(sf, df);
        }
    }
    
    // Write the schema document for the specified namespace
    private void writeDocument (String nsuri, Writer ofw) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        Element root = dom.createElementNS(XSD_NS_URI, "xs:schema");
        root.setAttribute("targetNamespace", nsuri);
        dom.appendChild(root);
        
        LOG.debug("Writing schema document for {}", nsuri);
        
        // Add namespace version number, if specified
        String nsv = me.getNamespaceVersion(nsuri);
        if (null != nsv && !nsv.isBlank()) root.setAttribute("version", nsv);
        
        // Add conformance target assertions, if any
        String niemVersion = me.getNIEMVersion(nsuri);
        String cta = me.getConformanceTargets(nsuri);
        if (null != cta) {
            String ctns = getBuiltinNamespaceURI(NIEM_CONFORMANCE_TARGETS, niemVersion);
            String ctprefix = me.getBuiltinPrefix(ctns);
            root.setAttributeNS(XML_NS_URI, "xmlns:"+ctprefix, ctns);
            root.setAttributeNS(ctns, ctprefix+":"+CONFORMANCE_ATTRIBUTE_NAME, cta);
        }
        // Add the <xs:annotation> element with namespace definition
        Namespace ns = m.getNamespaceByURI(nsuri);
        addDefinition(dom, root, ns.getDefinition());
        Element annot = (Element)root.getFirstChild();
        
        // Create type definitions for ClassType objects
        // Then create typedefs for Datatype objects (when not already created)
        // Remember external namespaces when encountered
        TreeMap<String,Element> typedefs = new TreeMap<>();
        TreeSet<Namespace> nsdep = new TreeSet<>();
        for (Component c : m.getComponentList()) createTypeFromClass(dom, typedefs, nsdep, nsuri, c.asClassType());
        for (Component c : m.getComponentList()) createTypeFromDatatype(dom, typedefs, nsdep, nsuri, c.asDatatype());
        
        // Create elements and attributes for Property objects
        TreeMap<String,Element> propdecls = new TreeMap<>();
        for (Component c : m.getComponentList()) createElementOrAttribute(dom, propdecls, nsdep, nsuri, c.asProperty());

        // Add a namespace declaration and import element for each namespace dependency
        for (Namespace dns : nsdep) {
            String dnspre = dns.getNamespacePrefix();
            String dnsuri = dns.getNamespaceURI();
            root.setAttributeNS(XML_NS_URI, "xmlns:"+dns.getNamespacePrefix(), dns.getNamespaceURI());
            if (XSD_NS_URI.equals(dns.getNamespaceURI())) continue;   // don't import XSD
            if (nsuri.equals(dns.getNamespaceURI())) continue;        // don't import current namespace
            Path thisDoc = Paths.get(me.getDocumentFilepath(nsuri));
            Path importDoc = Paths.get(me.getDocumentFilepath(dns.getNamespaceURI()));
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
        typedefs.forEach((name,element) -> {
            root.appendChild(element);
        });
        propdecls.forEach((name,element) -> {
            root.appendChild(element);
        });
        writeDom(dom, ofw);
    }
    
    private void addAppinfo (Document dom, Element e, String nsuri, String appatt, String value) {
        String niemVersion = me.getNIEMVersion(nsuri);
        String appinfoNS = getBuiltinNamespaceURI(NIEM_APPINFO, niemVersion);
        String appinfoPR = me.getBuiltinPrefix(appinfoNS);
        String appinfoQName = appinfoPR + ":" + appatt;
        Element root = dom.getDocumentElement();
        root.setAttributeNS(XML_NS_URI, "xmlns:"+appinfoPR, appinfoNS);
        e.setAttributeNS(appinfoNS, appinfoQName, value);
    }
    
    // Create types from ClassType objects first, so that attributes are handled.
    // Some Datatype objects will be handled along the way and skipped later.
    private void createTypeFromClass (Document dom, Map<String,Element> typedefs, Set<Namespace>nsdep, String nsuri, ClassType ct) { 
        LOG.debug("Creating type for {}", ct.getQName());
        String cname = ct.getName();
        if (typedefs.containsKey(cname)) return;
        if (!nsuri.equals(ct.getNamespace().getNamespaceURI())) return;
        Element cte = dom.createElementNS(XSD_NS_URI, "xs:complexType");
        cte.setAttribute("name", cname);
        addDefinition(dom, cte, ct.getDefinition());
        typedefs.put(cname, cte);
        if (ct.isDeprecated()) addAppinfo(dom, cte, nsuri, "deprecatedIndicator", "true");
        if (ct.isExternal())   addAppinfo(dom, cte, nsuri, "externalAdapterTypeIndicator", "true");
        
        // ClassType with properties has complex content
        // ClassType without properties and no HasValue has complex content
        // ClassType without properties and with a HasValue has simple content
        if (ct.hasPropertyList().isEmpty() && null != ct.getHasValue())
            createSimpleContent(dom, cte, typedefs, nsdep, ct);
        else
            createComplexContent(dom, cte, nsdep, ct);
    }
    
    // Create <xs:complexContent> for Class with HasProperty (and no HasValue)
    private void createComplexContent (Document dom, Element cte, Set<Namespace> nsdep, ClassType ct) {
            Element cce = dom.createElementNS(XSD_NS_URI, "xs:complexContent");
            Element exe = dom.createElementNS(XSD_NS_URI, "xs:extension");
            Element sqe = dom.createElementNS(XSD_NS_URI, "xs:sequence");  
            String cname = ct.getName();
 
            if (null == ct.getExtensionOfClass()) {
                String nver = me.getNIEMVersion(ct.getNamespace().getNamespaceURI());
                Namespace structuresNS = getBuiltinNamespace(NIEM_STRUCTURES, nver);
                String spr = structuresNS.getNamespacePrefix();
                if (cname.endsWith("AssociationType"))       exe.setAttribute("base", spr+":AssociationType");
                else if (cname.endsWith("MetadataType"))     exe.setAttribute("base", spr+":MetadataType");
                else if (cname.endsWith("AugmentationType")) exe.setAttribute("base", spr+":AugmentationType");
                else exe.setAttribute("base", spr+":ObjectType");
                nsdep.add(structuresNS);   // static namespace object, not the URI
            } 
            else {
                exe.setAttribute("base", ct.getExtensionOfClass().getQName());
                nsdep.add(ct.getExtensionOfClass().getNamespace());
            }
            for (HasProperty hp : ct.hasPropertyList()) {
                Property p = hp.getProperty();
                if (me.isAttribute(p.getQName())) { 
                    Element hpe = dom.createElementNS(XSD_NS_URI, "xs:attribute");
                    hpe.setAttribute("ref", p.getQName());
                    if ("1".equals(hp.minOccurs())) hpe.setAttribute("use", "required");
                    else hpe.setAttribute("use", "optional");
                    exe.appendChild(hpe);
                }
                else {
                    Element hpe = dom.createElementNS(XSD_NS_URI, "xs:element");
                    if (1 != hp.minOccurs()) hpe.setAttribute("minOccurs", ""+hp.minOccurs());
                    if (hp.maxUnbounded()) hpe.setAttribute("maxOccurs", "unbounded");
                    else if (1 != hp.maxOccurs()) hpe.setAttribute("maxOccurs", ""+hp.maxOccurs());
                    hpe.setAttribute("ref", hp.getProperty().getQName());
                    sqe.appendChild(hpe);
                }
                nsdep.add(hp.getProperty().getNamespace());
            }
            exe.appendChild(sqe);
            cce.appendChild(exe);
            cte.appendChild(cce);      
    }
    
    // Called by createTypeFromClass:     base type for xs:extension is the HasValue 
    // Called by createTypeFromDatatype:  base type for xs:extension is the Datatype
    private void createSimpleContent(Document dom, Element cte, Map<String, Element> typedefs, Set<Namespace> nsdep, Component c) {
        Element sce = dom.createElementNS(XSD_NS_URI, "xs:simpleContent");
        Element exe = dom.createElementNS(XSD_NS_URI, "xs:extension");
        String niemVer = me.getNIEMVersion(c.getNamespace().getNamespaceURI());
        Namespace proxyNS = getBuiltinNamespace(NIEM_PROXY, niemVer);
        List<HasProperty> hplist = new ArrayList<>();
        Datatype bdt = null;                            // base type for xs:extension element

        if (C_CLASSTYPE == c.getType()) {
            ClassType ct = (ClassType) c;
            bdt = ct.getHasValue();
            hplist = ct.hasPropertyList();
        } else {
            bdt = (Datatype) c;
        }
        RestrictionOf r = bdt.getRestrictionOf();
        String baseQN = "";
        
        // Base type is xs:foo w/o restriction -> xs:extension base="niem-xs:foo"
        if (null == r && XSD_NS_URI.equals(bdt.getNamespace().getNamespaceURI())) {
            baseQN = proxyNS.getNamespacePrefix() + ":" + bdt.getName();
            nsdep.add(proxyNS);
        }
        // Base type has empty Restriction -> xs:extension base="FooType"
        else if (null != r && r.getFacetList().isEmpty()) {
            Datatype rb = r.getDatatype();
            String rbn = rb.getName();
            String rns = rb.getNamespace().getNamespaceURI();
            // Replace xs:foo with proxy niem-xs:foo
            if (XSD_NS_URI.equals(rns)) {
                baseQN = proxyNS.getNamespacePrefix() + ":" + rbn;
                nsdep.add(proxyNS);
            } else {
                baseQN = rb.getQName();
                nsdep.add(rb.getNamespace());
            }
        } 
        // Base type has UnionOf, ListOf, Restriction with Facets
        // Create a simpleType, use that for xs:extension base
        else {
            Element age = dom.createElementNS(XSD_NS_URI, "xs:attributeGroup");
//            age.setAttribute("ref", me.getStructuresPrefix() + ":" + "SimpleObjectAttributeGroup");
            age.setAttribute("ref", "structures" + ":" + "SimpleObjectAttributeGroup");
            exe.appendChild(age);
            baseQN = createSimpleType(dom, typedefs, nsdep, bdt);
        }
        // Add attribute properties, if any
        for (HasProperty hp : hplist) {
            Element hpe = dom.createElementNS(XSD_NS_URI, "xs:attribute");
            hpe.setAttribute("ref", hp.getProperty().getQName());
            if ("1".equals(hp.minOccurs())) hpe.setAttribute("use", "required");
            exe.appendChild(hpe);
            nsdep.add(hp.getProperty().getNamespace());
        }
        exe.setAttribute("base", baseQN);
        sce.appendChild(exe);
        cte.appendChild(sce);
    }
    
    // Create a simpleType element and return its QName
    private String createSimpleType (Document dom, Map<String,Element>typedefs, Set<Namespace>nsdep, Datatype bdt) {
        String cname = bdt.getName();
        if (!cname.endsWith("SimpleType")) cname = cname.replaceFirst("Type$", "SimpleType");
        String cqn = bdt.getNamespace().getNamespacePrefix() + ":" + cname;
        if (typedefs.containsKey(cname)) return cqn;
        Element ste = dom.createElementNS(XSD_NS_URI, "xs:simpleType");
        ste.setAttribute("name", cname);
        addDefinition(dom, ste, bdt.getDefinition());
        typedefs.put(cname, ste);        
        if (null != bdt.getUnionOf()) createUnionType(dom, ste, bdt);
        else if (null != bdt.getListOf()) createListType(dom, ste, bdt);
        else if (null != bdt.getRestrictionOf()) createRestrictionType(dom, ste, bdt);
        nsdep.add(bdt.getNamespace());
        return cqn;
    }
    
    private void createUnionType (Document dom, Element ste, Datatype bdt) {
        Element une = dom.createElementNS(XSD_NS_URI, "xs:union");
        StringBuilder members = new StringBuilder();
        String sep = "";
        for (Datatype udt : bdt.getUnionOf().getDatatypeList()) {
            members.append(sep).append(udt.getQName());
            sep = " ";
        }
        une.setAttribute("memberTypes", members.toString());
        ste.appendChild(une);        
    }
    
    private void createListType (Document dom, Element ste, Datatype bdt) {
        Element lse = dom.createElementNS(XSD_NS_URI, "xs:list");
        lse.setAttribute("itemType", bdt.getListOf().getQName());
        ste.appendChild(lse);
    }
    
    private void createRestrictionType (Document dom, Element ste, Datatype bdt) {
        RestrictionOf r = bdt.getRestrictionOf();
        Element rse = dom.createElementNS(XSD_NS_URI, "xs:restriction");
        rse.setAttribute("base", r.getDatatype().getQName());
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
        
    private void createTypeFromDatatype (Document dom, Map<String,Element> typedefs, Set<Namespace>nsdep, String nsuri, Datatype dt) {
        String cname = dt.getName();
        if (typedefs.containsKey(cname)) return;
        if (!nsuri.equals(dt.getNamespace().getNamespaceURI())) return;        
        if (XSD_NS_URI.equals(dt.getNamespace().getNamespaceURI())) return;
        Element cte = dom.createElementNS(XSD_NS_URI, "xs:complexType");
        addDefinition(dom, cte, dt.getDefinition());
        cte.setAttribute("name", cname);
        if (dt.isDeprecated()) addAppinfo(dom, cte, nsuri, "deprecatedIndicator", "true");
        typedefs.put(cname, cte);
        createSimpleContent(dom, cte, typedefs, nsdep, dt);
    }   
    
    private void createElementOrAttribute(Document dom, Map<String,Element>propdecls, Set<Namespace>nsdep, String nsuri, Property p) {
        ClassType pt = p.getClassType();
        Datatype dt  = p.getDatatype();
        Element pe;
        if (!nsuri.equals(p.getNamespace().getNamespaceURI())) return;   
        LOG.debug("Creating {} for {}", (me.isAttribute(p.getQName()) ? "attribute" : "element"), p.getQName());
        if (me.isAttribute(p.getQName())) pe = dom.createElementNS(XSD_NS_URI, "xs:attribute");
        else pe = dom.createElementNS(XSD_NS_URI, "xs:element");
        addDefinition(dom, pe, p.getDefinition());
        pe.setAttribute("name", p.getName());
        if (me.isNillable(p.getQName())) pe.setAttribute("nillable", "true");
        if (p.isAbstract())   pe.setAttribute("abstract", "true");
        if (p.isDeprecated()) addAppinfo(dom, pe, nsuri, "deprecatedIndicator", "true");
        if (null != pt) {
            pe.setAttribute("type", pt.getQName());
            nsdep.add(pt.getNamespace());
        }
        else if (null != dt) {
            pe.setAttribute("type", dt.getQName());
            nsdep.add(dt.getNamespace());
        }
        if (null != p.getSubPropertyOf()) {
            pe.setAttribute("substitutionGroup", p.getSubPropertyOf().getQName());
            nsdep.add(p.getSubPropertyOf().getNamespace());
        }
        propdecls.put(p.getName(), pe);
    }
    
    private void addDefinition (Document dom, Element e, String def) {
        if (null == def || def.isBlank()) return;
        Element ae = dom.createElementNS(XSD_NS_URI, "xs:annotation");
        Element de = dom.createElementNS(XSD_NS_URI, "xs:documentation");
        de.setTextContent(def);
        ae.appendChild(de);
        e.appendChild(ae);
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
        while (scn.hasNextLine()) {           
            String line = scn.nextLine();
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
    // specified builtin and NIEM version.
    private Namespace getBuiltinNamespace (int kind, String niemVersion) {
        String bnsuri = getBuiltinNamespaceURI(kind, niemVersion);
        Namespace bns = builtinNSmap.get(bnsuri);
        if (null != bns) return bns;
        String bpr = me.getBuiltinPrefix(bnsuri);
        bns = new Namespace(bpr, bnsuri);
        builtinNSmap.put(bnsuri, bns);
        return bns;       
    }

}

