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

import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Model;
import static org.mitre.niem.xsd.ModelXMLReader.LOG;
import org.xml.sax.Attributes;
import static org.mitre.niem.NIEMConstants.CMF_NS_URI_PREFIX;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XHasProperty extends XObjectType {
    private HasProperty obj = null;
    
    @Override
    public HasProperty getObject () { return obj; }
    
    XHasProperty (Model m, XObjectType p, String ens, String eln, Attributes a, int line) {
        super(m, p, ens, eln, a, line);
        obj = new HasProperty(m);
        obj.setSequenceID(this.getSequenceID());       
    }      
    
    @Override
    public void addAsChild (XObjectType child) {
        child.addToHasProperty(this);
        super.addAsChild(child);
    }
   
    @Override
    public void addToClassType (XClassType x) { 
        XObjectType r  = this.getIDRepl();      // null unless replacing IDREF/URI placeholder
        ClassType po   = x.getObject();         // parent object
        HasProperty co = this.getObject();      // child object, perhaps an IDREF/URI placeholder
        try {
            if (null != r) {
                HasProperty ro = (HasProperty)r.getObject();    // object with desired ID
                po.replaceHasProperty(co, ro);                  // replace the placeholder
            } else {
                po.addHasProperty(co);
            }
        } catch (ClassCastException e) {
            LOG.error("line {}: ID/REF type mismatch", r.getLineNumber());
        }
    }    
}
