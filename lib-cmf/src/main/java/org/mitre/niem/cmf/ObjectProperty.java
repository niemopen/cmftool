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
 * A class for an ObjectProperty object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ObjectProperty extends Property {
        
    public ObjectProperty () { super(); }
    public ObjectProperty (String outsideURI) { super(outsideURI); }
    public ObjectProperty (Namespace ns, String name) { super(ns,name); }

    @Override
    public int getType ()                       { return CMF_OBJECTPROP; }
    @Override
    public String cmfElement ()                 { return "ObjectProperty"; }
    @Override
    public boolean isObjectProperty ()          { return true; }
    
    private ClassType classType = null;         // cmf:Class
    private String refCode = "";                // cmf:ReferenceCode
    
    @Override
    public Component type()                     { return classType; }
    @Override
    public ClassType classType ()               { return classType; }
    @Override
    public String referenceCode ()              { return refCode; }
    
    public void setClassType (ClassType c)      { classType = c; }
    public void setReferenceCode (String s)     { refCode = s; }
    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToObjectProperty(eln, loc, this);
    }
    
    @Override
    public boolean addToObjectProperty (String eln, String loc, ObjectProperty p) {
        p.addToProperty(eln, loc, this);
        return true;
    }
    
    @Override
    public boolean addToModel (String eln, String loc, Model m) {
        m.addObjectProperty(this);
        return true;
    }
    
    @Override
    public void addComponentCMFChildren (ModelXMLWriter w, Document doc, Element c, Set<Namespace>nsS)  { 
        super.addComponentCMFChildren(w, doc, c, nsS);
        w.addObjectPropertyChildren(doc, c, this, nsS);
    }
               
}
