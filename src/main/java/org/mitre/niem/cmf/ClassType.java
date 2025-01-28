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
    private boolean isAbstract = false;
    private boolean isAugmentable = false;
    private String refCode = null;
    private ClassType subClassOf  = null;
    private final List<PropertyAssociation> propertyList = new ArrayList<>();
    
    public ClassType () { super(); type = C_CLASSTYPE; }
    
    public ClassType (Namespace ns, String lname) {
        super(ns, lname);
        type = C_CLASSTYPE;
    }
    
    public void setSubClassOf (ClassType e)       { subClassOf = e; }
    public void setIsAbstract(boolean f)          { isAbstract = f; }
    public void setIsAugmentable (boolean f)      { isAugmentable = f; }
    public void setIsAugmentable (String s)       { isAugmentable = "true".equals(s); }
    public void setReferenceCode (String s)       { refCode = s; }
    
    public ClassType subClassOf ()                { return subClassOf; }
    public boolean isAbstract ()                  { return isAbstract; }
    public boolean isAugmentable ()               { return isAugmentable; }
    public boolean isAdapter ()                   { return getName().endsWith("AdapterType"); } 
    public boolean isReferenceable ()             { return !"NONE".equals(getReferenceCode()); }
    public boolean isReferenceCodeSet ()          { return null != refCode; }

    // If the reference code is not explicitly set, it is inherited from 
    // the superclass.  If no superclass, the default code is ANY.
    public String getReferenceCode () { 
        if (null != refCode) return refCode;
        else if (null != subClassOf) return subClassOf.getReferenceCode();
        else return "ANY";
    }
            
    public List<PropertyAssociation> propertyList ()   { return propertyList; }
    
    public PropertyAssociation getProperty (String qname) {
        for (var hp : propertyList) {
            if (qname.equals(hp.getProperty().getQName())) return hp;
        }
        return null;
    }

    public void addProperty (PropertyAssociation c) {
        this.propertyList.add(c);
    }
    
    public void removeProperty (PropertyAssociation c) {
        this.propertyList.remove(c);
    }
    
    public void replaceHasProperty (PropertyAssociation op, PropertyAssociation np) {
        int index = this.propertyList.indexOf(op);
        if (index < 0) return;
        this.propertyList.set(index, np);
    }
           
    @Override
    public void addToModel (Model m) {

        m.addComponent(this);
    }
 
}
