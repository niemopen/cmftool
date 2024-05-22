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

import java.io.IOException;
import java.io.OutputStream;
import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.UnionOf;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.Model;
import java.util.Collections;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.mitre.niem.NIEMConstants.CMF_NS_URI;
import static org.mitre.niem.NIEMConstants.CMF_STRUCTURES_NS_URI;
import org.mitre.niem.cmf.AugmentRecord;
import org.mitre.niem.cmf.CodeListBinding;
import org.mitre.niem.cmf.LocalTerm;
import static org.mitre.niem.cmf.NamespaceKind.namespaceKind2Code;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLWriter {
    
    static final Logger LOG = LogManager.getLogger(ModelXMLWriter.class);    
    
    public void writeXML (Model m, OutputStream os) throws TransformerConfigurationException, TransformerException, ParserConfigurationException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document dom = db.newDocument();
        Element root = genModel(dom, m);
        dom.appendChild(root);
        XMLWriter xw = new XMLWriter(dom, os);
        xw.writeXML();
    }
    
    private Element genModel (Document dom, Model m) {
        Element e = dom.createElementNS(CMF_NS_URI, "Model");
        e.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:cmf", CMF_NS_URI);
        e.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi", W3C_XML_SCHEMA_INSTANCE_NS_URI);
        e.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:structures", CMF_STRUCTURES_NS_URI); 
        for (Namespace z : m.getNamespaceList()) { addNamespace(dom, e, z); }
        for (Component c : m.getComponentList()) { addProperty(dom, e, c.asProperty()); }
        for (Component c : m.getComponentList()) { addClassType(dom, e, c.asClassType()); }
        for (Component c : m.getComponentList()) { addDatatype(dom, e, c.asDatatype()); }

        return e;
    }
 
    private void addClassType (Document dom, Element p, ClassType x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "Class");
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", componentIDString(x));
        addComponentChildren(dom, e, x);
        addOptionalIndicator(dom, e, "AbstractIndicator", x.isAbstract());
        addComponentRef(dom, e, "ExtensionOfClass", x.getExtensionOfClass());
        addOptionalIndicator(dom, e, "AugmentableIndicator", x.isAugmentable());
        addSimpleChild(dom, e, "ReferenceCode", x.getReferenceCode());
        if (null != x.hasPropertyList()) 
            for (HasProperty z : x.hasPropertyList()) { addHasProperty(dom, e, z); }
        p.appendChild(e);
    }
    
    private void addDatatype (Document dom, Element p, Datatype x) {
        if (null == x) return;
        LOG.debug("addDatatype {}", x.getQName());
        if (null != x.getListOf()) addListDatatype(dom, p, x);
        else if (!x.unionOf().isEmpty()) addUnionDatatype(dom, p, x);
        else if (null != x.getRestrictionBase()) addRestrictionDatatype(dom, p, x);
        else {
            Element e = dom.createElementNS(CMF_NS_URI, "Datatype");
            e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", componentIDString(x));
            addComponentChildren(dom, e, x);  
            p.appendChild(e);
        }
    }
    
    private void addListDatatype (Document dom, Element p, Datatype x) {
        Element e = dom.createElementNS(CMF_NS_URI, "ListDatatype");
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", componentIDString(x));
        addComponentChildren(dom, e, x);  
        addComponentRef(dom, e, "ListOf", x.getListOf());
        addOptionalIndicator(dom, e, "OrderedPropertyIndicator", x.getOrderedItems());
        p.appendChild(e);
    }
    
    private void addUnionDatatype (Document dom, Element p, Datatype x) {
        Element e = dom.createElementNS(CMF_NS_URI, "UnionDatatype");
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", componentIDString(x));
        addComponentChildren(dom, e, x);  
        for (var udt : x.unionOf()) addComponentRef(dom, e, "UnionOf", udt);
        p.appendChild(e);        
    }
    
    private void addRestrictionDatatype (Document dom, Element p, Datatype x) {
        Element e = dom.createElementNS(CMF_NS_URI, "RestrictionDatatype");
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", componentIDString(x));
        addComponentChildren(dom, e, x);  
        addComponentRef(dom, e, "RestrictionBase", x.getRestrictionBase());
        for (var f : x.facetList()) addFacet(dom, e, f);
        addCodeListBinding(dom, e, x.getCodeListBinding());
        p.appendChild(e);        
    }

    
    private void addCodeListBinding (Document dom, Element p, CodeListBinding x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "CodeListBinding");
        addSimpleChild(dom, e, "CodeListURI", x.getURI());
        addSimpleChild(dom, e, "CodeListColumnName", x.getColumn());
        addOptionalIndicator(dom, e, "CodeListConstrainingIndicator", x.isConstraining());
        p.appendChild(e);
    }
        
    private void addFacet (Document dom, Element p, Facet x) {
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
        addSimpleChild(dom, e, "DocumentationText", x.getDefinition());
        p.appendChild(e);
    }
        
    private void addHasProperty (Document dom, Element p, HasProperty x) {
        if (null == x) return;
        var e  = dom.createElementNS(CMF_NS_URI, "HasProperty");  
        var pr = x.getProperty();
        
        if (null == pr.getClassType()) addComponentRef(dom, e, "DataProperty", pr);
        else addComponentRef(dom, e, "ObjectProperty", pr);
        
        addSimpleChild(dom, e, "MinOccursQuantity", ""+x.minOccurs());            
        addSimpleChild(dom, e, "MaxOccursQuantity", x.maxUnbounded() ? "unbounded" : ""+x.maxOccurs());
        addSimpleChild(dom, e, "DocumentationText", x.getDefinition());
        addOptionalIndicator(dom, e, "OrderedPropertyIndicator", x.orderedProperties());
        x.augmentingNS().stream().sorted().forEach((ns) -> {
            addNamespaceRef(dom, e, "AugmentingNamespace", ns);
        });        
        p.appendChild(e);
    }
            
    private void addNamespace (Document dom, Element p, Namespace x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "Namespace");
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", x.getNamespacePrefix());
        addSimpleChild(dom, e, "NamespaceURI", x.getNamespaceURI());
        addSimpleChild(dom, e, "NamespacePrefixText", x.getNamespacePrefix());
        addSimpleChild(dom, e, "DocumentationText", x.getDocumentation());
        int nsk = x.getKind();
        addSimpleChild(dom, e, "NamespaceKindCode", namespaceKind2Code(nsk));
        for (var cta : x.confTargList()) addSimpleChild(dom, e, "ConformanceTargetURI", cta);
        addSimpleChild(dom, e, "DocumentFilePathText", x.getFilePath());
        addSimpleChild(dom, e, "NIEMVersionText", x.getNIEMVersion());
        addSimpleChild(dom, e, "NamespaceVersionText", x.getSchemaVersion());
        addSimpleChild(dom, e, "NamespaceLanguageName", x.getLanguage());
        Collections.sort(x.augmentList());
        Collections.sort(x.localTermList());
        for (AugmentRecord z : x.augmentList()) addAugmentRec(dom, e, z);
        for (LocalTerm z : x.localTermList()) addLocalTerm(dom, e, z);
        p.appendChild(e);
    }  
    
    private void addAugmentRec (Document dom, Element p, AugmentRecord x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "AugmentationRecord");
        addComponentRef(dom, e, "Class", x.getClassType());
        
        var pr = x.getProperty();
        if (null == pr.getClassType()) addComponentRef(dom, e, "DataProperty", pr);
        else addComponentRef(dom, e, "ObjectProperty", pr);
        
        addSimpleChild(dom, e, "AugmentationIndex", ""+x.indexInType());
        addSimpleChild(dom, e, "MinOccursQuantity", ""+x.minOccurs());  
        addSimpleChild(dom, e, "MaxOccursQuantity", x.maxUnbounded() ? "unbounded" : ""+x.maxOccurs());
        addSimpleChild(dom, e, "GlobalAugmented", x.getGlobalAugmented());
        p.appendChild(e);        
    }
    
    private void addLocalTerm (Document dom, Element p, LocalTerm x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "LocalTerm");
        addSimpleChild(dom, e, "TermName", x.getTerm());
        addSimpleChild(dom, e, "DocumentationText", x.getDefinition());
        addSimpleChild(dom, e, "TermLiteralText", x.getLiteral());
        addSimpleChild(dom, e, "SourceURIList", x.getSourceURIs());
        for (var z : x.citationList()) addSimpleChild(dom, e, "SourceCitationText", z);
        p.appendChild(e);
    }
    
    private void addNamespaceRef (Document dom, Element p, String lname, Namespace x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, lname);
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:ref", x.getNamespacePrefix());
        e.setAttributeNS(W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
        p.appendChild(e);
    }
    
    private void addProperty (Document dom, Element p, Property x) {
        Element e = null;
        if (null == x) return;
        if (null == x.getClassType()) e = dom.createElementNS(CMF_NS_URI, "DataProperty");
        else e = dom.createElementNS(CMF_NS_URI, "ObjectProperty");
        
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", componentIDString(x));
        addComponentChildren(dom, e, x);
        addOptionalIndicator(dom, e, "AbstractIndicator", x.isAbstract());
        addComponentRef(dom, e, "SubPropertyOf", x.getSubPropertyOf());
        addOptionalIndicator(dom, e, "RelationshipPropertyIndicator", x.isRelationship()); 
        
        if (null == x.getClassType()) {
            addComponentRef(dom, e, "Datatype", x.getDatatype());            
            addOptionalIndicator(dom, e, "AttributeIndicator", x.isAttribute());
            addOptionalIndicator(dom, e, "RefAttributeIndicator", x.isRefAttribute());
        }
        else {
            addComponentRef(dom, e, "Class", x.getClassType());
            addSimpleChild(dom, e, "ReferenceCode", x.getReferenceCode());
        }
        p.appendChild(e);
    }
      
    private void addUnionOf (Document dom, Element p, UnionOf x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "UnionOf");        
        if (null != x.getDatatypeList())
            for (Datatype z : x.getDatatypeList()) { addComponentRef(dom, e, "Datatype", z); }
        p.appendChild(e);
    }

    private void addComponentRef (Document dom, Element p, String lname, Component x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, lname);
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:ref", componentIDString(x));
        e.setAttributeNS(W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
        p.appendChild(e);
    }
    
    private void addComponentChildren (Document dom, Element p, Component x) {
        if (null == x) return;
        addSimpleChild(dom, p, "Name", x.getName());
        addNamespaceRef(dom, p, "Namespace", x.getNamespace());
        addSimpleChild(dom, p, "DocumentationText", x.getDocumentation());
        addOptionalIndicator(dom, p, "DeprecatedIndicator", x.isDeprecated());
    }
    
    private void addSimpleChild (Document dom, Element p, String eln, String value) {
        if (null == value) return;
        Element c = dom.createElementNS(CMF_NS_URI, eln);
        c.setTextContent(value);
        p.appendChild(c);
    }
    
    private void addSimpleChild (Document dom, Element p, String eln, boolean value) {
        addSimpleChild(dom, p, eln, value ? "true" : "false");
    }
    
    private void addOptionalIndicator (Document dom, Element p, String eln, boolean value) {
        if (value) addSimpleChild(dom, p, eln, "true");
    }
    
    private void addAttribute (Document dom, Element p, String an, String value) {
        if (null == value) return;
        p.setAttributeNS(CMF_NS_URI, an, value);
    }
    
    private static String componentIDString (Component x) {
        Namespace ns  = x.getNamespace();
        String prefix = ns.getNamespacePrefix();
        String lname  = x.getName();
        String id = prefix + "." + lname;
        return id;
    }
}
