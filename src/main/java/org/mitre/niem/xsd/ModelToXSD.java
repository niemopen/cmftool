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
import static java.lang.Character.isLowerCase;
import static java.lang.Character.toLowerCase;
import java.util.ArrayList;
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
import static org.mitre.niem.NIEMConstants.PROXY_NS_URI;
import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI;
import static org.mitre.niem.NIEMConstants.XML_NS_URI;
import static org.mitre.niem.NIEMConstants.XSD_NS_URI;
import org.mitre.niem.nmf.ClassType;
import org.mitre.niem.nmf.Component;
import static org.mitre.niem.nmf.Component.C_CLASSTYPE;
import org.mitre.niem.nmf.Datatype;
import org.mitre.niem.nmf.Facet;
import org.mitre.niem.nmf.HasProperty;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.nmf.Namespace;
import org.mitre.niem.nmf.Property;
import org.mitre.niem.nmf.RestrictionOf;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXSD {
    private static final Namespace PROXY_NS = new Namespace("niem-xs", PROXY_NS_URI);
    private static final Namespace STRUCTURES_NS = new Namespace("structures", STRUCTURES_NS_URI);
    private final Model m;
    private final ModelExtension me;
    
    public ModelToXSD (Model m, ModelExtension me) {
        this.m = m;
        this.me = me;
    }
    
    public void writeXSD (File od) throws FileNotFoundException, ParserConfigurationException, TransformerException, IOException {
        for (Namespace ns : m.namespaceSet()) {
            if (XSD_NS_URI.equals(ns.getNamespaceURI())) continue;
            String fn = ns.getNamespacePrefix() + ".xsd";
            File of = new File(od, fn);
            FileWriter ofw = new FileWriter(of);
            writeDocument(ns.getNamespaceURI(), ofw);
            ofw.close();
        }
    }
    
    // Write the schema document for the specified namespace
    private void writeDocument (String nsURI, Writer ofw) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        Element root = dom.createElementNS(XSD_NS_URI, "xs:schema");
        root.setAttribute("targetNamespace", nsURI);
        dom.appendChild(root);
        
        // Add the <xs:annotation> element with namespace definition
        Namespace ns = m.getNamespace(nsURI);
        addDefinition(dom, root, ns.getDefinition());
        Element annot = (Element)root.getFirstChild();
        
        // Create type definitions for ClassType objects
        // Then create typedefs for Datatype objects (when not already created)
        // Remember external namespaces when encountered
        TreeMap<String,Element> typedefs = new TreeMap<>();
        Set<Namespace> nsdep = new HashSet<>();
        for (ClassType cl : m.classTypeSet()) createTypeFromClass(dom, typedefs, nsdep, ns, cl);
        for (Datatype dt : m.datatypeSet())   createTypeFromDatatype(dom, typedefs, nsdep, ns, dt);  

        // Add a namespace declaration and import element for each namespace dependency
        for (Namespace dns : nsdep) {
            String dnspre = dns.getNamespacePrefix();
            String dnsuri = dns.getNamespaceURI();
            root.setAttributeNS(XML_NS_URI, "xmlns:"+dns.getNamespacePrefix(), dns.getNamespaceURI());
            Element ie = dom.createElementNS(XSD_NS_URI, "xs:import");
            ie.setAttribute("namespace", dnsuri);
            root.appendChild(ie);
        }
        // Now add the type definitions to the document
        typedefs.forEach((name,element) -> {
            root.appendChild(element);
        });
        writeToXSD(dom, ofw);
    }

    
    // Create types from ClassType objects first, so that attributes are handled.
    // Some Datatype objects will be handled along the way and skipped later.
    private void createTypeFromClass (Document dom, Map<String,Element> typedefs, Set<Namespace>nsdep, Namespace ns, ClassType ct) { 
        String cname = ct.getName();
        if (typedefs.containsKey(cname)) return;
        Element cte = dom.createElementNS(XSD_NS_URI, "xs:complexType");
        cte.setAttribute("name", cname);
        addDefinition(dom, cte, ct.getDefinition());
        typedefs.put(cname, cte);
        
        // ClassType with no HasValue has complex content
        // ClassType with a HasValue has simple content
        if (null == ct.getHasValue()) createComplexContent(dom, cte, nsdep, ct);
        else createSimpleContent(dom, cte, typedefs, nsdep, ct);
    }
    
    // Create <xs:complexContent> for Class with HasProperty (and no HasValue)
    private void createComplexContent (Document dom, Element cte, Set<Namespace> nsdep, ClassType ct) {
            Element cce = dom.createElementNS(XSD_NS_URI, "xs:complexContent");
            Element exe = dom.createElementNS(XSD_NS_URI, "xs:extension");
            Element sqe = dom.createElementNS(XSD_NS_URI, "xs:sequence");  
            String cname = ct.getName();
            String spr = me.getStructuresPrefix();
            if (null == ct.getExtensionOfClass()) {
                if (cname.endsWith("AssociationType"))       exe.setAttribute("base", spr+":AssociationType");
                else if (cname.endsWith("MetadataType"))     exe.setAttribute("base", spr+":MetadataType");
                else if (cname.endsWith("AugmentationType")) exe.setAttribute("base", spr+":AugmentationType");
                else exe.setAttribute("base", spr+":ObjectType");
                nsdep.add(STRUCTURES_NS);   // static namespace object, not the URI
            } 
            else {
                exe.setAttribute("base", ct.getExtensionOfClass().getQName());
                nsdep.add(ct.getExtensionOfClass().getNamespace());
            }
            for (HasProperty hp : ct.hasPropertyList()) {
                Property p = hp.getProperty();
                if (isLowerCase(p.getName().charAt(0))) {   // FIXME
                    Element hpe = dom.createElementNS(XSD_NS_URI, "xs:attribute");
                    hpe.setAttribute("ref", p.getQName());
                    if ("1".equals(hp.minOccursQuantity())) hpe.setAttribute("use", "required");
                    else hpe.setAttribute("use", "optional");
                    exe.appendChild(hpe);
                }
                else {
                    Element hpe = dom.createElementNS(XSD_NS_URI, "xs:element");
                    if (!"1".equals(hp.maxOccursQuantity())) hpe.setAttribute("maxOccurs", hp.maxOccursQuantity());
                    if (!"1".equals(hp.minOccursQuantity())) hpe.setAttribute("minOccurs", hp.minOccursQuantity());
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
            baseQN = me.getProxyPrefix() + ":" + bdt.getName();
            nsdep.add(PROXY_NS);
        }
        // Base type has empty Restriction -> xs:extension base="FooType"
        else if (null != r && r.getFacetList().isEmpty()) {
            Datatype rb = r.getDatatype();
            String rbn = rb.getName();
            String rns = rb.getNamespace().getNamespaceURI();
            // Replace xs:foo with proxy niem-xs:foo
            if (XSD_NS_URI.equals(rns)) {
                baseQN = me.getProxyPrefix() + ":" + rbn;
                nsdep.add(PROXY_NS);
            } else {
                baseQN = rb.getQName();
                nsdep.add(rb.getNamespace());
            }
        } 
        // Base type has UnionOf, ListOf, Restriction with Facets
        // Create a simpleType, use that for xs:extension base
        else {
            Element age = dom.createElementNS(XSD_NS_URI, "xs:attributeGroup");
            age.setAttribute("ref", me.getStructuresPrefix() + ":" + "SimpleObjectAttributeGroup");
            exe.appendChild(age);
            baseQN = createSimpleType(dom, typedefs, nsdep, bdt);
        }
        // Add attribute properties, if any
        for (HasProperty hp : hplist) {
            Element hpe = dom.createElementNS(XSD_NS_URI, "xs:attribute");
            hpe.setAttribute("ref", hp.getProperty().getQName());
            if ("1".equals(hp.minOccursQuantity())) hpe.setAttribute("use", "required");
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
        
    private void createTypeFromDatatype (Document dom, Map<String,Element> typedefs, Set<Namespace>nsdep, Namespace ns, Datatype dt) {
        String cname = dt.getName();
        if (typedefs.containsKey(cname)) return;
        if (XSD_NS_URI.equals(dt.getNamespace().getNamespaceURI())) return;
        Element cte = dom.createElementNS(XSD_NS_URI, "xs:complexType");
        cte.setAttribute("name", cname);
        addDefinition(dom, cte, dt.getDefinition());
        typedefs.put(cname, cte);
        createSimpleContent(dom, cte, typedefs, nsdep, dt);
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
    private void writeToXSD (Document dom, Writer w) throws TransformerConfigurationException, TransformerException, IOException {
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter ostr = new StringWriter();
        tr.transform(new DOMSource(dom), new StreamResult(ostr));        
        
        // process string by lines to do what XSLT won't do :-(
        // For <xs:schema>: namespace decls and attributes on separate indented lines.
        // For element references: order as @ref, @minOccurs, @maxOccurs.
        Pattern elm = Pattern.compile("^(\\s+<xs:element)\\s+([^/]*)(/?>)");    // match <xs:element ...>
        Scanner scn = new Scanner(ostr.toString());
        while (scn.hasNextLine()) {
            String line = scn.nextLine();
            Matcher em = elm.matcher(line);
            // For <xs:schema>: write targetNamespace first, then namespace declarations, then attributes, on separate lines
            if (line.startsWith("<xs:schema ")) {
                line = line.substring(0, line.length()-1);
                Map<String,String> smap = keyValMap(line);
                w.write("<xs:schema");
                if (null!=smap.get("targetNamespace")) { w.write("\n  "+smap.get("targetNamespace")); smap.remove("targetNamespace"); }
                for (Map.Entry<String,String> me : smap.entrySet()) {
                    if (me.getKey().startsWith("xmlns:")) { w.write("\n  "+me.getValue()); }
                }       
                for (Map.Entry<String,String> me : smap.entrySet()) {
                    if (!me.getKey().startsWith("xmlns:")) { w.write("\n  "+me.getValue()); }
                }                 
                w.write(">");
            }
            // For element refs: write @ref, @minOccurs, @maxOccurs, then other attributes
            else if (em.matches()) {
                Map<String,String> tmap = keyValMap(em.group(2));
                w.write(em.group(1));
                if (null!=tmap.get("ref")) { w.write(" "+tmap.get("ref")); tmap.remove("ref"); }
                if (null!=tmap.get("minOccurs")) { w.write(" "+tmap.get("minOccurs")); tmap.remove("minOccurs"); }
                if (null!=tmap.get("maxOccurs")) { w.write(" "+tmap.get("maxOccurs")); tmap.remove("maxOccurs"); }                
                for (String str : tmap.values()) { w.write(" "+str); }
                w.write(em.group(3));
            }
            else w.write(line);
            w.write("\n");
        }        
    }
    
    // 
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
}

