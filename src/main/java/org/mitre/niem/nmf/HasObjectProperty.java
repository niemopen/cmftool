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
import java.util.Set;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class HasObjectProperty extends ObjectType {
    private final List<ObjectProperty> objectPropertyList = new ArrayList<>();
    private String minOccursQuantity = null;
    private String maxOccursQuantity = null;
    
    public void setMinOccursQuantity (String s) { minOccursQuantity = s; }
    public void setMaxOccursQuantity (String s) { maxOccursQuantity = s; }
    
    public String minOccursQuantity() { return minOccursQuantity; }
    public String maxOccursQuantity() { return maxOccursQuantity; } 
    
    public List<ObjectProperty> getObjectPropertyList() { return objectPropertyList; }
    
    public HasObjectProperty (Model m) {
        super(m);
    }
     
    public void addObjectProperty (ObjectProperty c) {
        this.objectPropertyList.add(c);
    }
    
    public void removeObjectProperty (ObjectProperty c) throws NMFException {
        int index = this.objectPropertyList.indexOf(c);
        if (index < 0) throw(new NMFException(String.format("Can't remove ObjectProperty object; not in HasObjectProperty object")));
        this.objectPropertyList.remove(index);
    }
    
    public void replaceObjectProperty (ObjectProperty oc, ObjectProperty nc) throws NMFException {
        int index = this.objectPropertyList.indexOf(oc);
        if (index < 0) throw(new NMFException(String.format("Can't replace ObjectProperty object; not in HasObjectProperty object")));
        this.objectPropertyList.set(index, nc);        
    }    
           
    @Override
    void traverse (Set<ObjectType> seen, TraverseFunc f) {
        f.func(this);
        if (seen.contains(this)) return;
        seen.add(this);
        if (null != objectPropertyList)   for (var x: objectPropertyList) x.traverse(seen, f);
    }    
}
