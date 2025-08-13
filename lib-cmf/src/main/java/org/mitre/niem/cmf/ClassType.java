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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class for a Class object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ClassType extends Component {
    
    public ClassType () { super(); }
    public ClassType (String outsideURI) { super(outsideURI); }
    public ClassType (Namespace ns, String name) { super(ns, name); }
    
    @Override
    public int getType ()           { return CMF_CLASS; }
    @Override
    public boolean isClassType ()   { return true; }
    @Override
    public String cmfElement ()     { return "Class"; }
    
    private boolean isAbstract = false;         // cmf:AbstractIndicator
    private String refCode = "";                // cmf:ReferenceCode
    private ClassType subclass = null;          // cmf:SubClassOf
    private final List<PropertyAssociation> propL = new ArrayList<>();  // cmf:ChildPropertyAssociation
    private final List<AnyProperty> anyL = new ArrayList<>();           // cmf:AnyProperty
    
    @Override
    public boolean isAbstract ()                { return isAbstract; }
    @Override
    public String referenceCode ()              { return refCode; }
    public ClassType subClassOf ()              { return subclass; }
    public List<PropertyAssociation> propL ()   { return propL; }
    public List<AnyProperty> anyL ()            { return anyL; }
    
    public boolean isAssociationClass ()        { return name().endsWith("AssociationType"); }
    public boolean isAdapterClass ()            { return name().endsWith("AdapterType"); }
    public boolean isAugmentationClass ()       { return name().endsWith("AugmentationType"); }    
    public boolean isLiteralClass ()            { return null != literalDatatype(); }
    public boolean isObjectClass () { 
        return !isAssociationClass() && !isAdapterClass() && !isAugmentationClass() && !isLiteralClass();
    }

    
    public void setIsAbstract (boolean f)       { isAbstract = f; }
    @Override
    public void setReferenceCode (String s)     { super.setReferenceCode(s); refCode = s; }
    public void setSubclass (ClassType c)       { subclass = c; }
    
    public void addPropertyAssociation (PropertyAssociation pa) {
        propL.add(pa);
    }
    public void addAnyProperty (AnyProperty ap) {
        anyL.add(ap);
    }
    public String effectiveReferenceCode () {
        if (!refCode.isEmpty()) return refCode;
        else if (null != subclass) return subclass.effectiveReferenceCode();
        else return "NONE";
    }
    
    public boolean isReferenceable () {
        return (!"NONE".equals(effectiveReferenceCode()));
    }
    
    public DataProperty literalDataProperty () {
        if (propL.isEmpty()) return null;
        var pa = propL().get(0);
        var p  = pa.property();
        if (!p.name().endsWith("Literal")) return null;
        if (!p.isDataProperty()) return null;
        return ((DataProperty)p);
    }
    
    public Datatype literalDatatype () {
        var ldp = this.literalDataProperty();
        if (null == ldp) return null;
        return ldp.datatype();
    }
    
    public boolean hasSimpleContent () {
        if (null != literalDatatype()) return true;
        else if (null == subClassOf()) return false;
        else return subClassOf().hasSimpleContent();
    }    
    
    public boolean hasXmlLang () {
        for (var pa : propL()) {
            var p = pa.property();
            if (XML_NS_URI.equals(p.namespaceURI()) && "lang".equals(p.name()))
                return true;
        }
        return false;
    }
    
    public boolean isRepeatableProperty (Property p) {
        for (var pa : propL()) {
            if (p == pa.property()) {
                if (pa.maxOccursVal() > 1 || pa.isMaxUnbounded())
                    return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToClassType(eln, loc, this);
    }
    
    @Override
    public boolean addToAugmentRecord (String eln, String loc, AugmentRecord ar) {
        ar.setClassType(this);
        return true;
    }
        
    @Override
    public boolean addToClassType (String eln, String loc, ClassType c) {
        c.setSubclass(this);
        return true;
    }
    
    @Override
    public boolean addToModel (String eln, String loc, Model m) {
        m.addClassType(this);
        return true;
    }
    
    @Override
    public boolean addToObjectProperty (String eln, String loc, ObjectProperty op) {
        op.setClassType(this);
        return true;        
    }
    
    @Override
    public void addComponentCMFChildren (ModelXMLWriter w, Document doc, Element c, Set<Namespace>nsS)  { 
        super.addComponentCMFChildren(w, doc, c, nsS);
        w.addClassTypeChildren(doc, c, this, nsS);
    }
}
