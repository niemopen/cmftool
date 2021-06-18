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

import static org.mitre.niem.NIEMConstants.NMF_NS_URI_PREFIX;
import org.mitre.niem.nmf.HasDataProperty;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.nmf.NMFException;
import org.mitre.niem.nmf.ExtensionOf;
import static org.mitre.niem.xsd.XObjectType.LOG;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XHasDataProperty extends XObjectType {
    private HasDataProperty obj = null;
    
    @Override
    public HasDataProperty getObject () { return obj; }
    
    XHasDataProperty (Model m, String ens, String eln, Attributes a, int line) {
        super(m, ens, eln, a, line);
        obj = new HasDataProperty(m);
        for (int i = 0; i < a.getLength(); i++) {
            if (a.getURI(i).startsWith(NMF_NS_URI_PREFIX)) {
                if ("minOccursQuantity".equals(a.getLocalName(i))) {
                    obj.setMinOccursQuantity(a.getValue(i));
                }
                else if ("maxOccursQuantity".equals(a.getLocalName(i))) {
                    obj.setMaxOccursQuantity(a.getValue(i));
                }
            }
        }        
    }

    @Override
    public void addChild (XObjectType child) {
        child.addToHasDataProperty(this);
    }

    @Override
    public void addToExtensionOf (XExtensionOf x) { 
        XObjectType r      = x.getIDRepl();      // null unless replacing IDREF/URI placeholder
        ExtensionOf po     = x.getObject();      // parent object
        HasDataProperty co = this.getObject();   // child object, perhaps an IDREF/URI placeholder
        try {
            if (null != r) {
                HasDataProperty ro = (HasDataProperty)r.getObject();    // object with desired ID
                po.replaceHasDataProperty(co, ro);                // replace the placeholder
            } else {
                po.addHasDataProperty(co);
            }
        } catch (NMFException e) {
            LOG.error("line {}: {}", r.getLineNumber(), e.getMessage());
        } catch (ClassCastException e) {
            LOG.error("line {}: ID/REF type mismatch", r.getLineNumber());
        }
    }    
    
}
