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

import java.util.List;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class for a Datatype object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Datatype extends Component {
    static final Logger LOG = LogManager.getLogger(Datatype.class);   
    
    public Datatype () { super(); }
    public Datatype (String outsideURI) { super(outsideURI); }
    public Datatype (Namespace ns, String name) { super(ns,name); }    

    @Override
    public int getType ()               { return CMF_DATATYPE; }
    @Override
    public String cmfElement ()         { return "Datatype"; }
    @Override
    public boolean isDatatype ()        { return true; }
    
    // Override these in derived class (ListType, Restriction, Union)
    public Restriction asRestriction ()         { return null; }
    public Datatype itemType ()                 { return null; }
    @Override
    public boolean isOrdered ()                 { return false; }
    public List<Datatype> memberL ()            { return null; }
    public Datatype base ()                     { return null; }
    public List<Facet> facetL()                 { return null; }
    @Override
    public CodeListBinding codeListBinding ()   { return null; }
    
    public Datatype baseXS () {
        if (W3C_XML_SCHEMA_NS_URI.equals(namespaceURI())) return this;
        else return base().baseXS();
    }
    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToDatatype(eln, loc, this);

    }
    
    @Override
    public boolean addToDataProperty (String eln, String loc, DataProperty d) {
        d.setDatatype(this);
        return true;
    }
    
    @Override
    public boolean addToListType (String eln, String loc, ListType lt) {
        lt.setItemType(this);
        return true;
    }
    
    @Override
    public boolean addToModel (String eln, String loc, Model m) {
        m.addDatatype(this);
        return true;
    }
    
    @Override
    public boolean addToRestriction (String eln, String loc, Restriction r) {
        r.setBase(this);
        return true;
    }
    
    @Override
    public boolean addToUnion (String eln, String loc, Union u) {
        u.addMember(this);
        return true;
    }
    
    @Override
    public void addComponentCMFChildren (ModelXMLWriter w, Document doc, Element c, Set<Namespace>nsS)  { 
        super.addComponentCMFChildren(w, doc, c, nsS);
        // Nothing to add for Datatype
    }

}
