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

import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import static org.mitre.niem.NIEMConstants.NMF_NS_URI;
import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI;
import static org.mitre.niem.NIEMConstants.XML_NS_URI;
import static org.mitre.niem.NIEMConstants.XSI_NS_URI;
import org.mitre.niem.nmf.*;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLWriter {
    
    public void writeXML (Model m, OutputStream os) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom = db.newDocument();           
            Element root = genModel(dom, m);
            dom.appendChild(root);
            

            
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            // send DOM to file
            tr.transform(new DOMSource(dom), new StreamResult(os));         
            
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(ModelXMLWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(ModelXMLWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(ModelXMLWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Element genModel (Document dom, Model m) {
        Element e = dom.createElementNS(NMF_NS_URI, "Model");
        e.setAttributeNS(XML_NS_URI, "xmlns:mm", NMF_NS_URI);
        e.setAttributeNS(XML_NS_URI, "xmlns:xsi", XSI_NS_URI);
        e.setAttributeNS(XML_NS_URI, "xmlns:structures", STRUCTURES_NS_URI); 
        for (Namespace z : m.namespaceSet())           { addNamespace(dom, e, z); }
        for (ObjectProperty z : m.objectPropertySet()) { addObjectProperty(dom, e, z); }
        for (ClassType z : m.classTypeSet())           { addClassType(dom, e, z); }
        for (DataProperty z : m.dataPropertySet())     { addDataProperty(dom, e, z); }
        for (Datatype z : m.datatypeSet())             { addDatatype(dom, e, z); }
        return e;
    }
 
    public void addClassType (Document dom, Element p, ClassType x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "Class");
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", componentIDString(x));
        addComponentChildren(dom, e, x);
        addSimpleChild(dom, e, "AbstractIndicator", x.getAbstractIndicator());
        addComponentRef(dom, e, "ExtensionOfClass", x.getExtensionOfClass());
        addSimpleChild(dom, e, "ContentStyleCode", x.getContentStyleCode());
        if (null != x.getHasValueList()) 
            for (HasValue z : x.getHasValueList()) { addHasValue(dom, e, z); }
        if (null != x.getHasDataPropertyList()) 
            for (HasDataProperty z : x.getHasDataPropertyList()) { addHasDataProperty(dom, e, z); }
        if (null != x.getHasObjectPropertyList()) 
            for (HasObjectProperty z : x.getHasObjectPropertyList()) { addHasObjectProperty(dom, e, z); }        
        p.appendChild(e);
    }
    
    public void addDataProperty (Document dom, Element p, DataProperty x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "DataProperty");
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", componentIDString(x));
        addComponentChildren(dom, e, x);        
        addComponentRef(dom, e, "Datatype", x.getDatatype());
        p.appendChild(e);
    }
    
    public void addDatatype (Document dom, Element p, Datatype x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "Datatype");
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", componentIDString(x));
        addComponentChildren(dom, e, x);        
        addRestrictionOf(dom, e, x.getRestrictionOf());
        addUnionOf(dom, e, x.getUnionOf());
        p.appendChild(e);
    }
        
    public void addFacet (Document dom, Element p, Facet x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, x.getFacetKind());
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
        }
        addSimpleChild(dom, e, "DefinitionText", x.getDefinition());
        p.appendChild(e);
    }
        
    public void addHasDataProperty (Document dom, Element p, HasDataProperty x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "HasDataProperty");
        addAttribute(dom, e, "mm:minOccursQuantity", x.minOccursQuantity());
        addAttribute(dom, e, "mm:maxOccursQuantity", x.maxOccursQuantity());
        if (null != x.getDataPropertyList())
            for (DataProperty z : x.getDataPropertyList()) { addComponentRef(dom, e, "DataProperty", z); }
        p.appendChild(e);
    }

    public void addHasObjectProperty (Document dom, Element p, HasObjectProperty x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "HasObjectProperty");
        addAttribute(dom, e, "mm:minOccursQuantity", x.minOccursQuantity());
        addAttribute(dom, e, "mm:maxOccursQuantity", x.maxOccursQuantity());        
        if (null != x.getSequenceID()) e.setAttributeNS(STRUCTURES_NS_URI, "structures:sequenceID", x.getSequenceID());
        if (null != x.getObjectPropertyList())
            for (ObjectProperty z : x.getObjectPropertyList()) { addComponentRef(dom, e, "ObjectProperty", z); }
        p.appendChild(e);
    }
            
    public void addHasValue (Document dom, Element p, HasValue x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "HasValue");
        addComponentRef(dom, e, "Datatype", x.getDatatype());
        p.appendChild(e);
    }

    public void addNamespace (Document dom, Element p, Namespace x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "Namespace");
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", x.getNamespacePrefix());
        addSimpleChild(dom, e, "NamespaceURI", x.getNamespaceURI());
        addSimpleChild(dom, e, "NamespacePrefixName", x.getNamespacePrefix());
        addSimpleChild(dom, e, "DefinitionText", x.getDefinition());
        p.appendChild(e);
    }  
    
    public void addNamespaceRef (Document dom, Element p, Namespace x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "Namespace");
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", x.getNamespacePrefix());
        e.setAttributeNS(XSI_NS_URI, "xsi:nil", "true");
        p.appendChild(e);
    }
    
    public void addObjectProperty (Document dom, Element p, ObjectProperty x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "ObjectProperty");
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", componentIDString(x));
        addComponentChildren(dom, e, x);
        addSubPropertyOf(dom, e, x.getSubPropertyOf());
        addComponentRef(dom, e, "Class", x.getClassType());
        addSimpleChild(dom, e, "AbstractIndicator", x.getAbstractIndicator());
        p.appendChild(e);
    }
       
    public void addRestrictionOf (Document dom, Element p, RestrictionOf x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "Restrictionof");        
        addComponentRef(dom, e, "Datatype", x.getDatatype());
        if (null != x.getFacetList())
            for (Facet z : x.getFacetList()) { addFacet(dom, e, z); }
        p.appendChild(e);
    }
    
    public void addSubPropertyOf (Document dom, Element p, SubPropertyOf x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "SubPropertyOf");        
        addComponentRef(dom, e, "ObjectProperty", x.getObjectProperty());
        p.appendChild(e);
    }
         
    public void addUnionOf (Document dom, Element p, UnionOf x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, "UnionOf");        
        if (null != x.getDatatypeList())
            for (Datatype z : x.getDatatypeList()) { addComponentRef(dom, e, "Datatype", z); }
        p.appendChild(e);
    }

    public void addComponentRef (Document dom, Element p, String lname, Component x) {
        if (null == x) return;
        Element e = dom.createElementNS(NMF_NS_URI, lname);
        e.setAttributeNS(STRUCTURES_NS_URI, "structures:uri", componentIDString(x));
        e.setAttributeNS(XSI_NS_URI, "xsi:nil", "true");
        p.appendChild(e);
    }
    
    public void addComponentChildren (Document dom, Element p, Component x) {
        if (null == x) return;
        addSimpleChild(dom, p, "Name", x.getName());
        addNamespaceRef(dom, p, x.getNamespace());
        addSimpleChild(dom, p, "DefinitionText", x.getDefinition());
    }
    
    public void addSimpleChild (Document dom, Element p, String eln, String value) {
        if (null == value) return;
        Element c = dom.createElementNS(NMF_NS_URI, eln);
        c.setTextContent(value);
        p.appendChild(c);
    }
    
    public void addAttribute (Document dom, Element p, String an, String value) {
        if (null == value) return;
        p.setAttributeNS(NMF_NS_URI, an, value);
    }
    
    private static String componentIDString (Component x) {
        Namespace ns  = x.getNamespace();
        String prefix = ns.getNamespacePrefix();
        String lname  = x.getName();
        String id = prefix + "." + lname;
        return id;
    }
}
