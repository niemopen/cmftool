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
 * A class for a DataProperty object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class DataProperty extends Property {    
    
    public DataProperty () { super(); }
    public DataProperty (String outsideURI) { super(outsideURI); }
    public DataProperty (Namespace ns, String name) { super(ns,name); }

    @Override
    public int getType ()           { return CMF_DATAPROP; }
    @Override
    public boolean isProperty ()    { return true; }
    @Override
    public String cmfElement ()     { return "DataProperty"; }
    
    private boolean isAtt = false;              // cmf:AttributeIndicator
    private boolean isRefAtt = false;           // cmf:RefAttributeIndicator
    private Datatype datatype = null;           // cmf:Datatype

    public boolean isAttribute ()               { return isAtt; }
    public boolean isRefAttribute ()            { return isRefAtt; }
    public Datatype datatype ()                 { return datatype; }
    
    public void setIsAttribute (boolean f)      { isAtt = f; }
    public void setIsRefAttribute (boolean f)   { isRefAtt = f; }
    public void setDatatype (Datatype d)        { datatype = d; }
    
    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToDataProperty(eln, loc, this);

    }
    
    @Override
    public boolean addToDataProperty (String eln, String loc, DataProperty p) {
        p.addToProperty(eln, loc, this);
        return true;
    }
    
    @Override
    public boolean addToModel (String eln, String loc, Model m) {
        m.addDataProperty(this);
        return true;
    }
    
    @Override
    public void addComponentCMFChildren (ModelXMLWriter w, Document doc, Element c, Set<Namespace>nsS)  { 
        super.addComponentCMFChildren(w, doc, c, nsS);
        w.addDataPropertyChildren(doc, c, this, nsS);
    }
           
}
