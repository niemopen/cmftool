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
import org.mitre.niem.nmf.ClassType;
import org.mitre.niem.nmf.HasObjectProperty;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.nmf.NMFException;
import static org.mitre.niem.xsd.ModelXMLReader.LOG;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XHasObjectProperty extends XObjectType {
    private HasObjectProperty obj = null;
    
    @Override
    public HasObjectProperty getObject () { return obj; }
    
    XHasObjectProperty (Model m, String ens, String eln, Attributes a, int line) {
        super(m, ens, eln, a, line);
        obj = new HasObjectProperty(m);
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
        child.addToHasObjectProperty(this);
    }
   
    @Override
    public void addToClassType (XClassType x) { 
        XObjectType r        = x.getIDRepl();      // null unless replacing IDREF/URI placeholder
        ClassType po         = x.getObject();      // parent object
        HasObjectProperty co = this.getObject();   // child object, perhaps an IDREF/URI placeholder
        try {
            if (null != r) {
                HasObjectProperty ro = (HasObjectProperty)r.getObject();    // object with desired ID
                po.replaceHasObjectProperty(co, ro);                        // replace the placeholder
            } else {
                po.addHasObjectProperty(co);
            }
        } catch (NMFException e) {
            LOG.error("line {}: {}", (null == r ? "??" : r.getLineNumber()), e.getMessage());
        } catch (ClassCastException e) {
            LOG.error("line {}: ID/REF type mismatch", r.getLineNumber());
        }
    }    
}
