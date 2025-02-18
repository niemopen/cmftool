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
package org.mitre.niem.cmf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.mitre.niem.xml.LanguageString;
import org.mitre.niem.xml.ParserBootstrap;
import org.mitre.niem.xml.XMLWriter;
import static org.mitre.niem.xsd.NIEMConstants.CMF_NS_URI;
import static org.mitre.niem.xsd.NIEMConstants.CMF_STRUCTURES_NS_URI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class for writing a Model object to a CMF file in XML format.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLWriter {
    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(ModelXMLReader.class);  
    
    /**
     * Write a Model object as CMF-XML to a stream.  Returns true on success.  
     * Returns false on failure, with diagnostic messages written to Log4J2.
     * @param m - Model object
     * @param os - OutputStream
     */
    public boolean writeXML (Model m, OutputStream os) {
        return writeXML(m, m.nsSet(), os);
    }
    
    /**
     * Writes components from a specified set of namespaces as CMF-XML to a stream.
     * Returns false on failure, with diagnostic messages written to Log4J2.
     * @param m - Model object
     * @param nsS - set of namespace URIs or prefix strings
     * @param os  - output stream
     */
    public boolean writeXML (Model m, List<String> nsparam, OutputStream os) {
        var nsS = new HashSet<Namespace>();
        for (var s : nsparam) {
            Namespace ns = null;
            if (s.contains(":")) ns = m.uri2namespace(s);
            else ns = m.prefix2namespace(s);
            if (null ==  ns) {
                LOG.error("{}: no such namespace in model", s);
                return false;
            }
        }
        return writeXML(m, nsS, os);
    }
    
    private boolean writeXML (Model m, Set<Namespace>nsS, OutputStream os) {
        try {
            var db   = ParserBootstrap.docBuilder();
            var doc  = db.newDocument();
            var xw   = new XMLWriter(doc, os);
            var root = genModel(doc, m, nsS);
            doc.appendChild(root);
            xw.writeXML();
        } catch (ParserConfigurationException ex) {
            LOG.error("internal parser error: {}", ex.getMessage());
            return false;
        } catch (IOException ex) {
            LOG.error("i/o error: {}", ex.getMessage());
            return false;
        }
        return true;
    }
    
    private Element genModel (Document doc, Model m, Set<Namespace>nsS) {
        var e = doc.createElementNS(CMF_NS_URI, "Model");
        e.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:cmf", CMF_NS_URI);
        e.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi", W3C_XML_SCHEMA_INSTANCE_NS_URI);
        e.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:structures", CMF_STRUCTURES_NS_URI);
        e.setAttributeNS(XML_NS_URI, "xml:lang", "en-US");
        for (var n : m.namespaceList()) appendNamespace(doc, e, n, nsS);
        for (var c : m.componentList()) if (c.isProperty())     appendComponent(doc, e, c, nsS);
        for (var c : m.componentList()) if (c.isClassType())    appendComponent(doc, e, c, nsS);
        for (var c : m.componentList()) if (c.isDatatype())     appendComponent(doc, e, c, nsS);
        return e;
    }
    
    private void appendNamespace (Document doc, Element p, Namespace x, Set<Namespace>nsS) {
        if (null == x) return;
        if (!nsS.contains(x)) return;
        var e = doc.createElementNS(CMF_NS_URI, "Namespace");
        e.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", x.prefix());
        appendSimpleChild(doc, e, "NamespaceURI", x.uri());
        appendSimpleChild(doc, e, "NamespacePrefixText", x.prefix());
        for (var dls : x.docL()) appendDocumentation(doc, e, dls);
        for (var cta : x.ctargL()) appendSimpleChild(doc, e, "ConformanceTargetURI", cta);
        appendSimpleChild(doc, e, "DocumentFilePathText", x.documentFilePath());
        appendSimpleChild(doc, e, "NamespaceVersionText", x.version());
        appendSimpleChild(doc, e, "NamespaceLanguageName", x.language());
        for (var dls : x.impDocL()) appendLanguageString(doc, e, "ImportDocumentationText", dls);
        Collections.sort(x.augL());
        Collections.sort(x.locTermL());
        for (AugmentRecord z : x.augL()) appendAugmentRecord(doc, e, z, nsS);
        for (LocalTerm z : x.locTermL()) appendLocalTerm(doc, e, z);
        p.appendChild(e);    
    }
    
    private void appendComponent (Document doc, Element p, Component x, Set<Namespace>nsS) {
        if (null == x) return;
        if (x.isOutsideRef()) return;
        var c = doc.createElementNS(CMF_NS_URI, x.cmfElement());
        c.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:id", x.idRef());
        appendSimpleChild(doc, c, "Name", x.name());
        appendNamespaceReference(doc, c, x.namespace());
        for (var dls : x.docL()) appendDocumentation(doc, c, dls);
        appendOptionalIndicator(doc, c, "DeprecatedIndicator", x.isDeprecated());
        x.addComponentCMFChildren(this, doc, c, nsS);
        p.appendChild(c);
    }
    
    void addClassTypeChildren (Document doc, Element c, ClassType x, Set<Namespace>nsS) {
        if (null == x) return;
        appendOptionalIndicator(doc, c, "AbstractIndicator", x.isAbstract());
        appendOptionalIndicator(doc, c, "AnyAttributeIndicator", x.hasAnyAttribute());
        appendOptionalIndicator(doc, c, "AnyElementIndicator", x.hasAnyElement());
        appendComponentReference(doc, c, "SubClassOf", x.subClass(), nsS);
        appendSimpleChild(doc, c, "ReferenceCode", x.referenceCode());
        for (var cpa : x.propL()) appendPropertyAssociation(doc, c, cpa, nsS);    
    }
    
    void addDataPropertyChildren (Document doc, Element c, DataProperty x, Set<Namespace>nsS) {
        appendComponentReference(doc, c, "Datatype", x.datatype(), nsS);
        appendOptionalIndicator(doc, c, "AttributeIndicator", x.isAttribute());
        appendOptionalIndicator(doc, c, "RefAttributeIndicator", x.isRefAttribute());
    }
    
    void addListTypeChildren (Document doc, Element c, ListType x, Set<Namespace>nsS) {
        if (null == x) return;
        appendComponentReference(doc, c, "ListItemDatatype", x.itemType(), nsS);
        appendOptionalIndicator(doc, c, "OrderedPropertyIndicator", x.isOrdered());
    }
    
    void addObjectPropertyChildren (Document doc, Element c, ObjectProperty x, Set<Namespace>nsS) {
        appendSimpleChild(doc, c, "ReferenceCode", x.referenceCode());
        appendComponentReference(doc, c, "Class", x.classType(), nsS);
    }
    
    void addPropertyChildren (Document doc, Element c, Property x, Set<Namespace>nsS) {
        if (null == x) return;
        appendOptionalIndicator(doc, c, "AbstractIndicator", x.isAbstract());
        appendComponentReference(doc, c, "SubPropertyOf", x.subProperty(), nsS);
        appendOptionalIndicator(doc, c, "RelationshipIndicator", x.isRelationship());
    }
    
    void addRestrictionChildren (Document doc, Element c, Restriction x, Set<Namespace>nsS) {
        if (null == x) return;
        appendComponentReference(doc, c, "RestrictionBase", x.base(), nsS);
        for (var f : x.facetL()) appendFacet(doc, c, f);
        appendCodeListBinding(doc, c, x.codeListBinding());
    }
    
    void addUnionChildren (Document doc, Element c, Union x, Set<Namespace>nsS) {
        if (null == x) return;
        for (var mt : x.memberL()) appendComponentReference(doc, c, "UnionMemberDatatype", mt, nsS);
    }
    
    private void appendAugmentRecord (Document doc, Element p, AugmentRecord x, Set<Namespace>nsS) {
        if (null == x) return;
        var c = doc.createElementNS(CMF_NS_URI, "AugmentationRecord");
        appendComponentReference(doc, c, x.classType(), nsS);
        appendComponentReference(doc, c, x.property(), nsS);
        appendSimpleChild(doc, c, "MinOccursQuantity", x.minOccurs());
        appendSimpleChild(doc, c, "MaxOccursQuantity", x.maxOccurs());
        appendSimpleChild(doc, c, "AugmentationIndex", x.index());
        var gccL = new ArrayList<>(x.codeS());
        Collections.sort(gccL);
        for (var code : gccL) appendSimpleChild(doc, c, "GlobalClassCode", code);
        p.appendChild(c);
    }
    
    private void appendCodeListBinding (Document doc, Element p, CodeListBinding x) {
        if (null == x) return;
        var c = doc.createElementNS(CMF_NS_URI, "CodeListBinding");
        appendSimpleChild(doc, c, "CodeListURI", x.codeListURI());
        appendSimpleChild(doc, c, "CodeListColumnName", x.column());
        appendOptionalIndicator(doc, c, "CodeListConstrainingIndicator", x.isConstraining());
        p.appendChild(c);
    }
    
    private void appendComponentReference (Document doc, Element p, Component x, Set<Namespace>nsS) {
        if (null == x) return;
        appendComponentReference(doc, p, x.cmfElement(), x, nsS);
    }

    private void appendComponentReference (Document doc, Element p, String eln, Component x, Set<Namespace>nsS) {
        if (null == x) return;
        var c = doc.createElementNS(CMF_NS_URI, eln);
        c.setAttributeNS(W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
        if (!x.outsideURI().isEmpty() || !nsS.contains(x.namespace())) 
            c.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:uri", x.uri());
        else
            c.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:ref", x.idRef());
        p.appendChild(c);        
    }
    
    private void appendDocumentation (Document doc, Element p, LanguageString x) {
        if (null == x) return;
        appendLanguageString(doc, p, "DocumentationText", x);
    }
    
    private void appendFacet (Document doc, Element p, Facet x) {
        if (null == x) return;
        var c = doc.createElementNS(CMF_NS_URI, "Facet");
        appendSimpleChild(doc, c, "FacetCategoryCode", x.category());
        appendSimpleChild(doc, c, "FacetValue", x.value());
        for (var d : x.docL()) appendDocumentation(doc, c, d);
        p.appendChild(c);
    }
    
    private void appendLanguageString (Document doc, Element p, String eln, LanguageString x) {
        if (null == x) return;
        var c = doc.createElementNS(CMF_NS_URI, eln);
        if (!"en-US".equals(x.lang())) c.setAttributeNS(XML_NS_URI, "xml:lang", x.lang());
        c.setTextContent(x.text());
        p.appendChild(c);
    }
    
    private void appendLocalTerm (Document doc, Element p, LocalTerm x) {
        if (null == x) return;
        var c = doc.createElementNS(CMF_NS_URI, "LocalTerm");
        appendSimpleChild(doc, c, "TermName", x.term());
        for (var dls : x.docL()) appendDocumentation(doc, c, dls);
        appendSimpleChild(doc, c, "TermLiteralText", x.literal());
        for (var suri : x.sourceL())  appendSimpleChild(doc, c, "SourceURI", suri);
        for (var cit : x.citationL()) appendLanguageString(doc, c, "SourceCitationText", cit);
        p.appendChild(c);
    }

    private void appendNamespaceReference (Document doc, Element p, Namespace x) {
        if (null == x) return;
        if (x.prefix().isEmpty()) return;
        var c = doc.createElementNS(CMF_NS_URI, "Namespace");
        c.setAttributeNS(W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
        c.setAttributeNS(CMF_STRUCTURES_NS_URI, "structures:ref", x.prefix());
        p.appendChild(c);
    }
    
    private void appendPropertyAssociation (Document doc, Element p, PropertyAssociation x, Set<Namespace>nsS) {
        if (null == x) return;
        var c = doc.createElementNS(CMF_NS_URI, "ChildPropertyAssociation"); 
        appendComponentReference(doc, c, x.property(), nsS);
        appendSimpleChild(doc, c, "MinOccursQuantity", x.minOccurs());
        appendSimpleChild(doc, c, "MaxOccursQuantity", x.maxOccurs());
        for (var dls : x.docL()) appendDocumentation(doc, c, dls);
        appendOptionalIndicator(doc, c, "OrderedPropertyIndicator", x.isOrdered());
        p.appendChild(c);       
    }
    
    private void appendSimpleChild (Document doc, Element p, String eln, String value) {
        if (null == value) return;
        if (value.isBlank()) return;
        Element c = doc.createElementNS(CMF_NS_URI, eln);
        c.setTextContent(value);
        p.appendChild(c);
    }
    
    private void appendSimpleChild (Document doc, Element p, String eln, boolean value) {
        appendSimpleChild(doc, p, eln, value ? "true" : "false");
    }
    
    private void appendOptionalIndicator (Document doc, Element p, String eln, boolean value) {
        if (value) appendSimpleChild(doc, p, eln, "true");
    }
    
    private void addAttribute (Document doc, Element p, String an, String value) {
        if (null == value) return;
        p.setAttributeNS(CMF_NS_URI, an, value);
    }

}
