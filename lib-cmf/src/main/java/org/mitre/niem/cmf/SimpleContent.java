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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class for a CMF element with a string value and a language code.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class SimpleContent extends CMFObject {
    static final Logger LOG = LogManager.getLogger(SimpleContent.class);    
    
    public SimpleContent (String name)              { super(); this.name  = name; }
    public SimpleContent (String name, String lang) { super(); this.name = name; this.lang = lang; }
    
    private final String name;
    private String content = "";
    private String lang = "";

    public String name ()       { return name.trim(); }
    public String content ()    { return content.trim(); }
    public String raw ()        { return content; }
    public String lang ()       { return lang.trim(); }
    
    @Override
    public void setContent (String v) { content = v; }
    
    @Override
    public boolean addToAnyProperty (String eln, String loc, AnyProperty ap) {
        switch (eln) {
        case "AttributeIndicator":      ap.setIsAttribute("true".equals(this.content())); break;
        case "MaxOccursQuantity":       ap.setMaxOccurs(this.content()); break; 
        case "MinOccursQuantity":       ap.setMinOccurs(this.content()); break;
        case "NamespaceConstraintText": ap.setNsConstraint(this.content()); break;
        case "ProcessingCode":          ap.setProcessCode(this.content()); break;
        default: error("AnyProperty", loc, eln);
        }
        return true;
    }

    @Override
    public boolean addToAugmentRecord (String eln, String loc, AugmentRecord ar) {
        switch (eln) {
        case "AugmentationIndex":       ar.setIndex(this.content()); break;
        case "GlobalClassCode":         ar.addCode(this.content()); break;
        case "MaxOccursQuantity":       ar.setMaxOccurs(this.content()); break;
        case "MinOccursQuantity":       ar.setMinOccurs(this.content()); break;
        default: error("AugmentRecord", loc, eln);
        }
        return true;
    }
    
    @Override
    public boolean addToClassType (String eln, String loc, ClassType c) {
        switch (eln) {
        case "AbstractIndicator":     c.setIsAbstract("true".equals(this.content())); break;
        case "ReferenceCode":         c.setReferenceCode(this.content()); break; 
        default: error("ClassType", loc, eln);
        }
        return true;
    }
    
    @Override
    public boolean addToCodeListBinding (String eln, String loc, CodeListBinding b) {
        switch (eln) {
        case "CodeListURI":                     b.setCodeListURI(this.content()); break;
        case "CodeListColumnName":              b.setColumn(this.content()); break;
        case "CodeListConstrainingIndicator":   b.setIsConstraining("true".equals(this.content())); break;
        default: error("CodeListBinding", loc, eln);
        }
        return true;
    }
    
    @Override
    public boolean addToComponent (String eln, String loc, Component c) {
        switch (eln) {
        case "DeprecatedIndicator": c.setIsDeprecated("true".equals(this.content())); break;
        case "DocumentationText":   c.addDocumentation(this.raw(), this.lang()); break;
        case "Name":                c.setName(this.content()); break;
        default: return false;
        }
        return true;
    }
    
    @Override
    public boolean addToDataProperty (String eln, String loc, DataProperty d) {
        switch (eln) {
        case "AttributeIndicator":      d.setIsAttribute("true".equals(this.content())); break;
        case "RefAttributeIndicator":   d.setIsRefAttribute("true".equals(this.content())); break;
        default: error("DataProperty", loc, eln);
        }
        return true;
    }
    
    @Override
    public boolean addToDatatype (String eln, String loc, Datatype d) {
        return false;
    }
    
    @Override
    public boolean addToFacet (String eln, String loc, Facet f) {
        switch (eln) {
        case "DocumentationText":   f.addDocumentation(this.raw(), this.lang()); break;
        case "FacetCategoryCode":   f.setCategory(this.content()); break; 
        case "FacetValue":          f.setValue(this.content()); break;
        default: error("Facet", loc, eln);
        }
        return true;
    }
    
    @Override
    public boolean addToImportDoc (String eln, String loc, ImportDoc d) {
        switch (eln) {
        case "DocumentationText":   d.addDocumentation(this.raw(), this.lang()); break;
        case "NamespaceURI":        d.setURI(this.content()); break;
        default: error("ImportDoc", loc, eln);
        }
        return true;
    }
        
    @Override
    public boolean addToListType (String eln, String loc, ListType d) {
        switch (eln) { 
        case "OrderedPropertyIndicator": d.setIsOrdered("true".equals(this.content())); break;
        default: error("ListType", loc, eln);
        }
        return true;
    }
    
    @Override
    public boolean addToLocalTerm (String eln, String loc, LocalTerm lt) {
        switch (eln) {
        case "DocumentationText":   lt.setDocumentation(this.content()); break;
        case "SourceCitationText":  lt.addCitation(this.raw(), this.lang()); break; 
        case "SourceURI":           lt.addSource(this.content()); break;
        case "TermLiteralText":     lt.setLiteral(this.content()); break;
        case "TermName":            lt.setTerm(this.content()); break;
        default: error("LocalTerm", loc, eln);
        }
        return true;
    }
    
    @Override
    public boolean addToNamespace (String eln, String loc, Namespace n) throws CMFException {
        switch (eln) {
        case "ConformanceTargetURI":    n.addConformanceTarget(this.content()); break;
        case "DocumentationText":       n.addDocumentation(this.raw(), this.lang()); break;
        case "DocumentFilePathText":    n.setDocumentFilePath(this.content()); break;
        case "NamespaceCategoryCode":   n.setKindCode(this.content()); break;
        case "NamespaceLanguageName":   n.setLanguage(this.content()); break;
        case "NamespacePrefixText":     n.setPrefix(this.content()); break;
        case "NamespaceVersionText":    n.setVersion(this.content()); break;
        case "NamespaceURI":            n.setURI(this.content()); break;
        case "NIEMVersionName":         n.setNIEMVersion(this.content()); break;
        default: error("Namespace", loc, eln);
        }
        return true;
    }
    
    @Override
    public boolean addToObjectProperty (String eln, String loc, ObjectProperty p) {
        switch (eln)  {
        case "ReferenceCode":           p.setReferenceCode(this.content()); break;
        default: error("ObjectProperty", loc, eln);
        }
        return true;
    }
    
    @Override
    public boolean addToProperty (String eln, String loc, Property p) {
        switch (eln) {
        case "AbstractIndicator":       p.setIsAbstract("true".equals(this.content())); break;
        case "RelationshipIndicator":   p.setIsRelationship("true".equals(this.content())); break;
        default: return false;
        }
        return true;
    }
    
    @Override
    public boolean addToPropertyAssociation (String eln, String loc, PropertyAssociation pa) {
        switch (eln) {
        case "DocumentationText":           pa.addDocumentation(this.raw(), this.lang()); break;
        case "MaxOccursQuantity":           pa.setMaxOccurs(this.content()); break;
        case "MinOccursQuantity":           pa.setMinOccurs(this.content()); break;
        case "OrderedPropertyIndicator":    pa.setIsOrdered("true".equals(this.content())); break;
        default: return false;
        }
        return true;
    }
        
    @Override
    public boolean addToRestriction (String eln, String loc, Restriction r) {
        switch (eln) {            
        default: error("Restriction", loc, eln);
        }
        return true;
    }
           
    @Override
    public boolean addToUnion (String eln, String loc, Union u) {
        switch (eln) {            
        default: error("Union", loc, eln);
        }
        return true;
    }
       
    private void error (String className, String loc,  String eln) {
        LOG.error("{}: can't add {} element to {}", loc, eln, className);
    }
}
