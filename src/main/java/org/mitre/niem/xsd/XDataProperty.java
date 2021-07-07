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

import org.mitre.niem.nmf.DataProperty;
import org.mitre.niem.nmf.HasDataProperty;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.nmf.NMFException;
import static org.mitre.niem.xsd.ModelXMLReader.LOG;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XDataProperty extends XObjectType {
    private DataProperty obj = null;
    
    @Override
    public DataProperty getObject () { return obj; }
    
    XDataProperty (Model m, String ens, String eln, Attributes a, int line) {
        super(m, ens, eln, a, line);
        obj = new DataProperty(m);
    }    
    
    // Replacing a placeholder? Use idRepl to replace obj
    // Otherwise obj is the object to use
    @Override
    public DataProperty getObjectToSet () {
        DataProperty r = this.obj;
        if (null != this.idRepl) {
            try {
                r = (DataProperty)this.idRepl.getObject();
            }
            catch (ClassCastException e) {
                LOG.error("line {}: ID/REF type mismatch", this.getLineNumber());
            }
        }
        return r;
    }
    
    @Override
    public void addChild (XObjectType child) {
        child.addToDataProperty(this);
    } 
    
    @Override
    public void addToHasDataProperty(XHasDataProperty x) {
        XObjectType r        = x.getIDRepl();      // null unless replacing IDREF/URI placeholder
        HasDataProperty po   = x.getObject();      // parent object
        DataProperty co = this.getObject();        // child object, perhaps an IDREF/URI placeholder
        try {
            if (null != r) {
                DataProperty ro = (DataProperty)r.getObject();    // object with desired ID
                po.replaceDataProperty(co, ro);                   // replace the placeholder
            } else {
                po.addDataProperty(co);
            }
        } catch (NMFException e) {
            LOG.error("line {}: {}", r.getLineNumber(), e.getMessage());
        } catch (ClassCastException e) {
            LOG.error("line {}: ID/REF type mismatch", r.getLineNumber());
        }
    }  

    // Ignore reference placeholders. All namespace objects will be collected after parsing.    
    @Override
    public void addToModel(XModel x) {
        if (null != x.getRefKey()) return;  // ignore placeholder child of Model
        if (null != x.getIDRepl()) return;  // no placeholder children to replace
        try {
            x.getObject().addDataProperty(this.getObject());
        } catch (NMFException e) {
            LOG.error("line {}: {}", x.getLineNumber(), e.getMessage());
        }
    }    
}
