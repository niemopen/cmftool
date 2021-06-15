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
package org.mitre.niem.nmf;

import java.util.Set;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ClassType extends Component {
    
    protected String abstractIndicator = null;
    protected String contentStyleCode  = null;
    protected ExtensionOf extensionOf  = null;
    
    public void setAbstractIndicator (String s) { abstractIndicator = s; }
    public void setContentStyleCode  (String s) { contentStyleCode = s; }
    public void setExtensionOf (ExtensionOf e)  { extensionOf = e; }
    
    public String getAbstractIndicator () { return abstractIndicator; }
    public String getContentStyleCode ()  { return contentStyleCode; }
    public ExtensionOf getExtensionOf ()  { return extensionOf; }
    
    public ClassType () { }
       
    public ClassType (Model m, String ens, String eln, Attributes a) {
        super(m, ens, eln, a);
    }
    
    @Override
    public int addChild (ObjectType child, int index) {
        return child.addToClass(this, index);
    }
    
    @Override
    public int addToExtensionOf (ExtensionOf e, int index) {
        e.setClassType(this);
        return -1;
    }
    
    // Special handing for adding ClassType to Model; can't be a ref placeholder
    @Override
    public int addToModel(Model m, int index) { 
        m.classList().add(this);
        return -1;
    }
    
    @Override
    public int addToObjectProperty (ObjectProperty op, int index) {
        op.setClassType(this);
        return -1;
    }

    @Override
    void traverse (Set<ObjectType> seen, TraverseFunc f) {
        f.func(this);
        if (seen.contains(this)) return;
        seen.add(this);
        if (null != namespace)   namespace.traverse(seen, f);
        if (null != extensionOf) extensionOf.traverse(seen, f);
    }    
}
