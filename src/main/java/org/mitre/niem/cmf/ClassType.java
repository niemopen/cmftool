/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2024 The MITRE Corporation.
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

import java.util.ArrayList;
import java.util.List;

/**
 * A class for a CMF Class object
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ClassType extends Component {
    private String refCode = "ANY";
    private boolean isAugmentable = false;
    private ClassType extensionOfClass  = null;
    private final List<HasProperty> hasPropertyList = new ArrayList<>();
    
    public ClassType () { super(); type = C_CLASSTYPE; }
    
    public ClassType (Namespace ns, String lname) {
        super(ns, lname);
        type = C_CLASSTYPE;
    }

    public void setReferenceCode (String s)       { refCode = s; }
    public void setIsAugmentable (boolean f)      { isAugmentable = f; }
    public void setIsAugmentable (String s)       { isAugmentable = "true".equals(s); }
    public void setExtensionOfClass (ClassType e) { extensionOfClass = e; }
    
    public String getReferenceCode()              { return refCode; }
    public boolean isAugmentable ()               { return isAugmentable; }
    public boolean isAdapter ()                   { return getName().endsWith("AdapterType"); }
    public boolean isReferenceable ()             { return !refCode.isBlank() && !"NONE".equals(refCode); }
    public ClassType getExtensionOfClass ()       { return extensionOfClass; }
            
    public List<HasProperty> hasPropertyList ()   { return hasPropertyList; }
    
    public HasProperty getHasProperty (String qname) {
        for (var hp : hasPropertyList) {
            if (qname.equals(hp.getProperty().getQName())) return hp;
        }
        return null;
    }

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
           
    @Override
    public void addToModel (Model m) {

        m.addComponent(this);
    }
 
}
