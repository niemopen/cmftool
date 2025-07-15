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

import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An abstract class for a Property object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Property extends Component {    
    
    public Property () { super(); }
    public Property (String outsideURI) { super(outsideURI); }
    public Property (Namespace ns, String name) { super(ns, name); }
    
    @Override
    public boolean isProperty ()                { return true; }
    public boolean isDataProperty ()            { return false; }
    public boolean isObjectProperty ()          { return false; }
   
    private boolean isAbstract = false;         // cmf:AbstractIndicator
    private boolean isOrdered = false;          // cmf:OrderedPropertyIndicator
    private boolean isRelationship = false;     // cmf:RelationshipIndicator
    private Property subprop = null;            // cmf:SubPropertyOf
    
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
    public Property subPropertyOf ()            { return subprop; }
    
    public void setIsAbstract (boolean f)       { isAbstract = f; }
    public void setIsOrdered (boolean f)        { isOrdered = f; }
    public void setIsRelationship (boolean f)   { isRelationship = f; }
    public void setSubproperty (Property p)     { subprop = p; }
    
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
    public boolean addToProperty (String eln, String loc, Property p) {
        p.setSubproperty(this);
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
