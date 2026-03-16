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

import java.util.HashSet;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class for a Property object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Property extends Component {    
    
    public Property () { super(); }
    public Property (String outsideURI) { super(outsideURI); }
    public Property (Namespace ns, String name) { super(ns, name); }

    @Override
    public int getType ()               { return CMF_PROPERTY; }
    @Override
    public String cmfElement ()         { return "Property"; }
    
    @Override
    public boolean isProperty ()                { return true; }
    public boolean isDataProperty ()            { return false; }
    public boolean isObjectProperty ()          { return false; }
   
    private boolean isAbstract = false;                         // cmf:AbstractIndicator
    private boolean isOrdered = false;                          // cmf:OrderedPropertyIndicator
    private boolean isRelationship = false;                     // cmf:RelationshipIndicator
    private boolean isChoice = false;                           // cmf:XSDChoiceIndicator   
    private final Set<Property> subpropOfS = new HashSet<>();   // cmf:SubPropertyOf
    
    public ClassType classType ()               { return null; }
    public Datatype datatype ()                 { return null; }
    public Component type ()                    { return null; }
 
    @Override
    public boolean isAbstract ()                { return isAbstract; }
    public boolean isAttribute ()               { return false; }
    public boolean isRefAttribute()             { return false; }
    public boolean isReferenceable ()           { return null == classType() ? false : classType().isReferenceable(); }
    @Override
    public boolean isOrdered ()                 { return isOrdered; }
    public boolean isRelationship ()            { return isRelationship; }
    public boolean isChoice ()                  { return isChoice; }
    
    public void setIsAbstract (boolean f)       { isAbstract = f; }
    public void setIsOrdered (boolean f)        { isOrdered = f; }
    public void setIsRelationship (boolean f)   { isRelationship = f; }
    public void setIsChoice (boolean f)         { isChoice = f; }
    
    
    // Subproperties are complicated, because CMF only records SubPropertyOf
    // (which we get from @substitutionGroup in XSD).  So a Property object
    // knows that Y is subproperty of X.  But often we instead want to know 
    // all of the subproperties of X.  We can only get that from the complete model.

    // This property is a subProperty of zero or more other properties.
    // Returns that set.  Doesn't include augmentation points (because those
    // aren't model objects).  Substitution for other components is possible 
    // but unusual in NIEM XSD.  So these are usually the result of xs:choice
    // elements in an extension schema document.
    public Set<Property> subPropertyOfS ()     { return subpropOfS; }
    
    public void addSubPropertyOf (Property p) {
        if (subpropOfS.contains(p)) return;
        subpropOfS.add(p);
        model().changeSubProps();
    }
    
    public void removeSubPropertyOf (Property p) {
        subpropOfS.remove(p);
        model().changeSubProps();
    }
    
    // Returns a set of all properties that have this property in their
    // subPropertyOf set.
    public Set<Property> directSubProps () {
        return model().directSubProps(this);
    }
    
    // Returns a set of all direct and indirect subproperties of this object.
    public Set<Property> allSubProps () {
        return model().allSubProps(this);
    }
    
    
    // Routines for creating model objects from CMF-XML
    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToProperty(eln, loc, this);

    }
    
    @Override
    public boolean addToAugmentRecord (String eln, String loc, AugmentRecord ar) {
        ar.setProperty(this);
        return true;
    }
    
    @Override
    public boolean addToModel (String eln, String loc, Model m) {
        m.addProperty(this);
        return true;
    }
    
    @Override
    public boolean addToProperty (String eln, String loc, Property p) {
        p.addSubPropertyOf(this);
        return true;           
    }
    
    @Override
    public boolean addToPropertyAssociation (String eln, String loc, PropertyAssociation pa) {
        pa.setProperty(this);
        return true;
    }
    
    @Override
    public void addComponentCMFChildren (ModelXMLWriter w, Document doc, Element c, Set<Namespace>nsS)  { 
        super.addComponentCMFChildren(w, doc, c, nsS);
        w.addPropertyChildren(doc, c, this, nsS);
    }
           
}
