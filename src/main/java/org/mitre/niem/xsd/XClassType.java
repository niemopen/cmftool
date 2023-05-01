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
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ObjectType;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XClassType extends XObjectType {
    private ClassType obj = null;
    
    @Override
    public void setObject (ObjectType o) { 
        obj = ClassType.class == o.getClass() ? (ClassType)o : null;
    }
    
    @Override
    public ClassType getObject() { return obj; }
    
    XClassType (Model m, XObjectType p, String ens, String eln, Attributes a, int line) {
        super(m, p, ens, eln, a, line);
        obj = new ClassType();
    } 
    
    @Override
    public void addAsChild (XObjectType child) {
        child.addToClassType(this);
        super.addAsChild(child);        
    }
    
    @Override
    public void addToAugmentRecord (XAugmentRecord xar) {
        xar.getObject().setClassType(this.getObject());
    }
    
    // A classtype object added to a parent classtype object must be an extension
    // of the parent classtype.
    @Override
    public void addToClassType (XClassType c) {
        c.getObject().setExtensionOfClass(this.getObject());
    } 
    
    @Override
    public void addToProperty (XProperty op) {
        op.getObject().setClassType(this.getObject());
    }    
}
