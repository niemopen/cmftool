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

import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.UnionOf;
import static org.mitre.niem.xsd.ModelXMLReader.LOG;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XDatatype extends XObjectType {  
    private Datatype obj = null;
    
    @Override
    public Datatype getObject () { return obj; }
    
    XDatatype (Model m, XObjectType p, String ens, String eln, Attributes a, int line) {
        super(m, p, ens, eln, a, line);
        obj = new Datatype(m);
    }
    
    // Replacing a placeholder? Use idRepl to replace obj
    // Otherwise obj is the object to use
    @Override
    public Datatype getObjectToAdd () {
        Datatype r = this.obj;
        if (null != this.idRepl) {
            try {
                r = (Datatype)this.idRepl.getObject();
            }
            catch (ClassCastException e) {
                LOG.error("line {}: ID/REF type mismatch", this.getLineNumber());
            }
        }
        return r;
    }
        
    @Override
    public void addAsChild (XObjectType child) {
        child.addToDatatype(this);
        super.addAsChild(child);  
    }   
    
    // A datatype object added to a parent classtype object must be the result
    // of a HasValue element
    @Override
    public void addToClassType (XClassType x) {
        x.getObject().setHasValue(this.getObjectToAdd());        
    }
    
    // A datatype object added to a parent datatype object must be the result
    // of a ListOf element
    @Override
    public void addToDatatype (XDatatype x) {
        x.getObject().setListOf(this.getObjectToAdd());        
    }
        
    @Override
    public void addToProperty (XProperty x) {
        x.getObject().setDatatype(this.getObjectToAdd());
    }
    
    @Override
    public void addToRestrictionOf (XRestrictionOf x) {
        x.getObject().setDatatype(this.getObjectToAdd());
    }     
    
    @Override
    public void addToUnionOf (XUnionOf x) { 
        XObjectType r = this.getIDRepl();       // null unless replacing IDREF/URI placeholder
        UnionOf po    = x.getObject();          // parent object
        Datatype co   = this.getObject();       // child object, null or an IDREF/URI placeholder
        try {
            if (null != r) {
                Datatype ro = (Datatype)r.getObject();    // object with desired ID
                po.replaceDatatype(co, ro);               // replace the placeholder
            } else {
                po.addDatatype(co);
            }
        } catch (ClassCastException e) {
            LOG.error("line {}: ID/REF type mismatch", r.getLineNumber());
        }
    }     
}
