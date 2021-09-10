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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ClassType extends Component {
    private String abstractIndicator = null;
    private ClassType extensionOfClass  = null;
    private Datatype hasValue = null;
    private final List<HasProperty> hasPropertyList = new ArrayList<>();
    
    public void setAbstractIndicator (String s)   { abstractIndicator = s; }
    public void setExtensionOfClass (ClassType e) { extensionOfClass = e; }
    public void setHasValue (Datatype v)          { hasValue = v; }
    
    public String getAbstractIndicator ()   { return abstractIndicator; }
    public ClassType getExtensionOfClass () { return extensionOfClass; }
    public Datatype getHasValue()           { return hasValue; }
            
    public List<HasProperty> hasPropertyList ()       { return hasPropertyList; }

    public void addHasProperty (HasProperty c) {
        this.hasPropertyList.add(c);
    }
    
    public void removeHasProperty (HasProperty c) {
        this.hasPropertyList.remove(c);
    }
    
    public void replaceHasProperty (HasProperty op, HasProperty np) {
        int index = this.hasPropertyList.indexOf(op);
        if (index < 0) return;
        this.hasPropertyList.set(index, np);
        
    }
      
    public ClassType () { type = C_CLASSTYPE; }
       
    public ClassType (Model m) {
        super(m);
        type = C_CLASSTYPE;
    }
    
    @Override
    public void addToModelSet (Model m) {
        m.addClassType(this);
    }
 
}
