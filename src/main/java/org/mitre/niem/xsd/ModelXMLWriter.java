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

import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.RestrictionOf;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.UnionOf;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.Model;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI;
import static org.mitre.niem.NIEMConstants.XML_NS_URI;
import static org.mitre.niem.NIEMConstants.XSI_NS_URI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.mitre.niem.NIEMConstants.CMF_NS_URI;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLWriter {
    
    static final Logger LOG = LogManager.getLogger(ModelXMLWriter.class);    
    
    public void writeXML (Model m, PrintWriter ow) throws TransformerConfigurationException, TransformerException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        Element root = genModel(dom, m);
        dom.appendChild(root);

        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        // send DOM to string, then format the namespace decls
        StringWriter ostr = new StringWriter();
        tr.transform(new DOMSource(dom), new StreamResult(ostr));
        Scanner scn = new Scanner(ostr.toString());
        while (scn.hasNextLine()) {
            String line = scn.nextLine();
            if (line.startsWith("<Model ")) {
                String[] tok = line.split("\\s+");
                ow.println("<Model");
                for (int i = 1; i < tok.length; i++) {
                    ow.println("  " + tok[i]);
                }
            }
            else ow.println(line);
        }
    }
    
    public Element genModel (Document dom, Model m) {
        Element e = dom.createElementNS(CMF_NS_URI, "Model");
        e.setAttributeNS(XML_NS_URI, "xmlns:cmf", CMF_NS_URI);
        e.setAttributeNS(XML_NS_URI, "xmlns:xsi", XSI_NS_URI);
        e.setAttributeNS(XML_NS_URI, "xmlns:structures", STRUCTURES_NS_URI); 
        for (Namespace z : m.getNamespaceList()) { addNamespace(dom, e, z); }
        for (Component c : m.getComponentList()) { addProperty(dom, e, c.asProperty()); }
        for (Component c : m.getComponentList()) { addClassType(dom, e, c.asClassType()); }
        for (Component c : m.getComponentList()) { addDatatype(dom, e, c.asDatatype()); }
        return e;
    }
 
    public void addClassType (Document dom, Element p, ClassType x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "Class");
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", componentIDString(x));
        addComponentChildren(dom, e, x);
        if (x.isAbstract())    addSimpleChild(dom, e, "AbstractIndicator", "true");
        if (x.isDeprecated())  addSimpleChild(dom, e, "DeprecatedIndicator", "true");
        if (x.isAugmentable()) addSimpleChild(dom, e, "AugmentableIndicator", "true");
        if (x.isExternal())    addSimpleChild(dom, e, "ExternalAdapterTypeIndicator", "true");
        addComponentRef(dom, e, "ExtensionOfClass", x.getExtensionOfClass());
        addComponentRef(dom, e, "HasValue", x.getHasValue());
        if (null != x.hasPropertyList()) 
            for (HasProperty z : x.hasPropertyList()) { addHasProperty(dom, e, z); }     
        p.appendChild(e);
    }
    
    public void addDatatype (Document dom, Element p, Datatype x) {
        if (null == x) return;
        LOG.debug("addDatatype {}", x.getQName());
        Element e = dom.createElementNS(CMF_NS_URI, "Datatype");
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", componentIDString(x));
        addComponentChildren(dom, e, x);  
        if (x.isDeprecated()) addSimpleChild(dom, e, "DeprecatedIndicator", "true");
        addRestrictionOf(dom, e, x.getRestrictionOf());
        addUnionOf(dom, e, x.getUnionOf());
        addComponentRef(dom, e, "ListOf", x.getListOf());
        p.appendChild(e);
    }
        
    public void addFacet (Document dom, Element p, Facet x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, x.getFacetKind());
        switch (x.getFacetKind()) {
            case "Enumeration":
            case "MaxExclusive":
            case "MaxInclusive":
            case "MinExclusive":
            case "MinInclusive":
            case "Pattern":
                addSimpleChild(dom, e, "StringValue", x.getStringVal());
                break;
            case "FractionDigits":
            case "Length":
            case "MinLength":
            case "MaxLength":
                addSimpleChild(dom, e, "NonNegativeValue", x.getStringVal());
                break;
            case "TotalDigits":
                addSimpleChild(dom, e, "PositiveValue", x.getStringVal());
                break;
            case "WhiteSpace":
                addSimpleChild(dom, e, "WhiteSpaceValueCode", x.getStringVal()); 
                break;
        }
        addSimpleChild(dom, e, "DefinitionText", x.getDefinition());
        p.appendChild(e);
    }
        
    public void addHasProperty (Document dom, Element p, HasProperty x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "HasProperty");    
        if (null != x.getSequenceID()) 
            e.setAttributeNS(STRUCTURES_NS_URI, "structures:sequenceID", x.getSequenceID());
        addComponentRef(dom, e, "Property", x.getProperty());
        addSimpleChild(dom, e, "MinOccursQuantity", ""+x.minOccurs());            
        addSimpleChild(dom, e, "MaxOccursQuantity", x.maxUnbounded() ? "unbounded" : ""+x.maxOccurs()); 
        addNamespaceRef(dom, e, "AugmentationElementNamespace", x.augmentElementNS()); 
        for (Namespace ns : x.augmentTypeNS()) {
            addNamespaceRef(dom, e, "AugmentationTypeNamespace", ns);
        }        
        p.appendChild(e);
    }
            
    public void addNamespace (Document dom, Element p, Namespace x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "Namespace");
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", x.getNamespacePrefix());
        addSimpleChild(dom, e, "NamespaceURI", x.getNamespaceURI());
        addSimpleChild(dom, e, "NamespacePrefixName", x.getNamespacePrefix());
        addSimpleChild(dom, e, "DefinitionText", x.getDefinition());
        if (x.isExternal()) addSimpleChild(dom, e, "ExternalNamespaceIndicator", "true");
        p.appendChild(e);
    }  
    
    public void addNamespaceRef (Document dom, Element p, String lname, Namespace x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, lname);
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", x.getNamespacePrefix());
        e.setAttributeNS(XSI_NS_URI, "xsi:nil", "true");
        p.appendChild(e);
    }
    
    public void addProperty (Document dom, Element p, Property x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "Property");
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", componentIDString(x));
        addComponentChildren(dom, e, x);
        addComponentRef(dom, e, "SubPropertyOf", x.getSubPropertyOf());
        addComponentRef(dom, e, "Class", x.getClassType());
        addComponentRef(dom, e, "Datatype", x.getDatatype());
        if (x.isAbstract())   addSimpleChild(dom, e, "AbstractIndicator", "true");
        if (x.isDeprecated()) addSimpleChild(dom, e, "DeprecatedIndicator", "true");
        p.appendChild(e);
    }
       
    public void addRestrictionOf (Document dom, Element p, RestrictionOf x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "RestrictionOf");        
        addComponentRef(dom, e, "Datatype", x.getDatatype());
        if (null != x.getFacetList())
            for (Facet z : x.getFacetList()) { addFacet(dom, e, z); }
        p.appendChild(e);
    }
      
    public void addUnionOf (Document dom, Element p, UnionOf x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "UnionOf");        
        if (null != x.getDatatypeList())
            for (Datatype z : x.getDatatypeList()) { addComponentRef(dom, e, "Datatype", z); }
        p.appendChild(e);
    }

    public void addComponentRef (Document dom, Element p, String lname, Component x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, lname);
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", componentIDString(x));
        e.setAttributeNS(XSI_NS_URI, "xsi:nil", "true");
        p.appendChild(e);
    }
    
    public void addComponentChildren (Document dom, Element p, Component x) {
        if (null == x) return;
        addSimpleChild(dom, p, "Name", x.getName());
        addNamespaceRef(dom, p, "Namespace", x.getNamespace());
        addSimpleChild(dom, p, "DefinitionText", x.getDefinition());
    }
    
    public void addSimpleChild (Document dom, Element p, String eln, String value) {
        if (null == value) return;
        Element c = dom.createElementNS(CMF_NS_URI, eln);
        c.setTextContent(value);
        p.appendChild(c);
    }
    
    public void addAttribute (Document dom, Element p, String an, String value) {
        if (null == value) return;
        p.setAttributeNS(CMF_NS_URI, an, value);
    }
    
    private static String componentIDString (Component x) {
        Namespace ns  = x.getNamespace();
        String prefix = ns.getNamespacePrefix();
        String lname  = x.getName();
        String id = prefix + ":" + lname;
        return id;
    }
}
