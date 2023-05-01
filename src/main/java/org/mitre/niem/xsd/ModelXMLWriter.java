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
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
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
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.mitre.niem.NIEMConstants.CMF_NS_URI;
import static org.mitre.niem.NIEMConstants.CMF_STRUCTURES_NS_URI;
import org.mitre.niem.cmf.AugmentRecord;
import org.mitre.niem.cmf.CodeListBinding;
import static org.mitre.niem.cmf.NamespaceKind.namespaceKind2Code;
import org.mitre.niem.cmf.SchemaDocument;

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
                ow.print("<Model\n");
                for (int i = 1; i < tok.length; i++) {
                    ow.print("  " + tok[i] + "\n");
                }
            }
            else ow.print(line + "\n");
        }
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
        m.schemadoc().forEach((nsuri, sd) ->     { addSchemaDocument(dom, e, nsuri, sd); });

        return e;
    }
    
    public void addSchemaDocument (Document dom, Element p, String nsuri, SchemaDocument x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "SchemaDocument");
        addSimpleChild(dom, e, "NamespacePrefixText", x.targetPrefix());
        addSimpleChild(dom, e, "NamespaceURI", nsuri);
        addSimpleChild(dom, e, "ConformanceTargetURIList", x.confTargets());
        addSimpleChild(dom, e, "DocumentFilePathText", x.filePath());
        addSimpleChild(dom, e, "NIEMVersionText", x.niemVersion());
        addSimpleChild(dom, e, "SchemaVersionText", x.schemaVersion());
        addSimpleChild(dom, e, "SchemaLanguageName", x.language());
        p.appendChild(e);
    }
 
    private void addClassType (Document dom, Element p, ClassType x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "Class");
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", componentIDString(x));
        addComponentChildren(dom, e, x);
        if (x.isAugmentable()) addSimpleChild(dom, e, "AugmentableIndicator", "true");
        if (x.isExternal())    addSimpleChild(dom, e, "ExternalAdapterTypeIndicator", "true");
        addComponentRef(dom, e, "ExtensionOfClass", x.getExtensionOfClass());
        if (null != x.hasPropertyList()) 
            for (HasProperty z : x.hasPropertyList()) { addHasProperty(dom, e, z); }    
        if (x.canHaveMD()) addSimpleChild(dom, e, "MetadataIndicator", "true");
        p.appendChild(e);
    }
    
    private void addDatatype (Document dom, Element p, Datatype x) {
        if (null == x) return;
        LOG.debug("addDatatype {}", x.getQName());
        Element e = dom.createElementNS(CMF_NS_URI, "Datatype");
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", componentIDString(x));
        addComponentChildren(dom, e, x);  
        addRestrictionOf(dom, e, x.getRestrictionOf());
        addUnionOf(dom, e, x.getUnionOf());
        addComponentRef(dom, e, "ListOf", x.getListOf());
        addCodeListBinding(dom, e, x.getCodeListBinding());
        p.appendChild(e);
    }
    
    private void addCodeListBinding (Document dom, Element p, CodeListBinding x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "CodeListBinding");
        addSimpleChild(dom, e, "CodeListURI", x.getURI());
        addSimpleChild(dom, e, "CodeListColumnName", x.getColumn());
        if (x.getIsConstraining()) addSimpleChild(dom, e, "CodeListConstrainingIndicator", "true");
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
        addSimpleChild(dom, e, "DefinitionText", x.getDefinition());
        p.appendChild(e);
    }
        
    private void addHasProperty (Document dom, Element p, HasProperty x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "HasProperty");    
        if (null != x.getSequenceID()) 
            e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:sequenceID", x.getSequenceID());
        addComponentRef(dom, e, "Property", x.getProperty());
        addSimpleChild(dom, e, "MinOccursQuantity", ""+x.minOccurs());            
        addSimpleChild(dom, e, "MaxOccursQuantity", x.maxUnbounded() ? "unbounded" : ""+x.maxOccurs()); 
        x.augmentingNS().stream().sorted().forEach((ns) -> {
            addNamespaceRef(dom, e, "AugmentationNamespace", ns);
        });        
        p.appendChild(e);
    }
            
    private void addNamespace (Document dom, Element p, Namespace x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "Namespace");
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", x.getNamespacePrefix());
        addSimpleChild(dom, e, "NamespaceURI", x.getNamespaceURI());
        addSimpleChild(dom, e, "NamespacePrefixText", x.getNamespacePrefix());
        addSimpleChild(dom, e, "DefinitionText", x.getDefinition());
        int nsk = x.getKind();
        addSimpleChild(dom, e, "NamespaceKindCode", namespaceKind2Code(nsk));
        for (AugmentRecord z : x.augmentList()) addAugmentRec(dom, e, z);
        p.appendChild(e);
    }  
    
    private void addAugmentRec (Document dom, Element p, AugmentRecord x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "AugmentRecord");
        addComponentRef(dom, e, "Class", x.getClassType());
        addComponentRef(dom, e, "Property", x.getProperty());
        addSimpleChild(dom, e, "AugmentationIndex", ""+x.indexInType());
        addSimpleChild(dom, e, "MinOccursQuantity", ""+x.minOccurs());  
        addSimpleChild(dom, e, "MaxOccursQuantity", x.maxUnbounded() ? "unbounded" : ""+x.maxOccurs());     
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
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "Property");
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", componentIDString(x));
        addComponentChildren(dom, e, x);
        addComponentRef(dom, e, "SubPropertyOf", x.getSubPropertyOf());
        addComponentRef(dom, e, "Class", x.getClassType());
        addComponentRef(dom, e, "Datatype", x.getDatatype());
        if (x.isAttribute())     addSimpleChild(dom, e, "AttributeIndicator", "true");
        if (x.canHaveMD())       addSimpleChild(dom, e, "MetadataIndicator", "true");
        if (x.isReferenceable()) addSimpleChild(dom, e, "ReferenceableIndicator", "true");
        p.appendChild(e);
    }
          
    private void addRestrictionOf (Document dom, Element p, RestrictionOf x) {
        if (null == x) return;
        Element e = dom.createElementNS(CMF_NS_URI, "RestrictionOf");        
        addComponentRef(dom, e, "Datatype", x.getDatatype());
        if (null != x.getFacetList())
            for (Facet z : x.getFacetList()) { addFacet(dom, e, z); }
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
        addSimpleChild(dom, p, "DefinitionText", x.getDefinition());
        if (x.isAbstract())   addSimpleChild(dom, p, "AbstractIndicator", "true");
        if (x.isDeprecated()) addSimpleChild(dom, p, "DeprecatedIndicator", "true");
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
