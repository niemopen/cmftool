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
public class ExtensionOf extends ObjectType {
    private ClassType classType = null;
    private final List<HasValue> hasValueList = new ArrayList<>();
    private final List<HasDataProperty> hasDataPropertyList = new ArrayList<>();
    private final List<HasObjectProperty> hasObjectPropertyList = new ArrayList<>();
    
    public void setClassType (ClassType c) { classType = c; }    
    
    public ClassType getClassType ()                          { return classType; }
    public List<HasValue> getHasValueList()                   { return hasValueList; }
    public List<HasDataProperty> getHasDataPropertyList()     { return hasDataPropertyList; }
    public List<HasObjectProperty> getHasObjectPropertyList() { return hasObjectPropertyList; }
          
    public List<HasValue> hasValueList ()                     { return hasValueList; }
    public List<HasDataProperty> hasDataPropertyList ()       { return hasDataPropertyList; }
    public List<HasObjectProperty> hasObjectPropertyList ()   { return hasObjectPropertyList; }
    
    public void addHasValue (HasValue c) throws NMFException {
        this.hasValueList.add(c);
    }
    
    public void removeHasValue (HasValue c) throws NMFException {
        int index = this.hasValueList.indexOf(c);
        if (index < 0) throw(new NMFException(String.format("Can't remove HasValue object; not in ExtensionOf object")));
        this.hasValueList.remove(index);
    }
    
    public void replaceHasValue (HasValue oc, HasValue nc) throws NMFException {
        int index = this.hasValueList.indexOf(oc);
        if (index < 0) throw(new NMFException(String.format("Can't replace HasValue object; not in ExtensionOf object")));
        this.hasValueList.set(index, nc);        
    }    

    public void addHasDataProperty (HasDataProperty c) throws NMFException {
        this.hasDataPropertyList.add(c);
    }
    
    public void removeHasDataProperty (HasDataProperty c) throws NMFException {
        int index = this.hasDataPropertyList.indexOf(c);
        if (index < 0) throw(new NMFException(String.format("Can't remove HasDataProperty object; not in ExtensionOf object")));
        this.hasDataPropertyList.remove(index);
    }
    
    public void replaceHasDataProperty (HasDataProperty oc, HasDataProperty nc) throws NMFException {
        int index = this.hasDataPropertyList.indexOf(oc);
        if (index < 0) throw(new NMFException(String.format("Can't replace HasDataProperty object; not in ExtensionOf object")));
        this.hasDataPropertyList.set(index, nc);        
    }    

    public void addHasObjectProperty (HasObjectProperty c) throws NMFException {
        this.hasObjectPropertyList.add(c);
    }
    
    public void removeHasObjectProperty (HasObjectProperty c) throws NMFException {
        int index = this.hasObjectPropertyList.indexOf(c);
        if (index < 0) throw(new NMFException(String.format("Can't remove HasObjectProperty object; not in ExtensionOf object")));
        this.hasObjectPropertyList.remove(index);
    }
    
    public void replaceHasObjectProperty (HasObjectProperty oc, HasObjectProperty nc) throws NMFException {
        int index = this.hasObjectPropertyList.indexOf(oc);
        if (index < 0) throw(new NMFException(String.format("Can't replace HasObjectProperty object; not in ExtensionOf object")));
        this.hasObjectPropertyList.set(index, nc);        
    }    

    public ExtensionOf (Model m) {
        super(m);
    }


    @Override
    void traverse (Set<ObjectType> seen, TraverseFunc f) {
        f.func(this);
        if (seen.contains(this)) return;
        seen.add(this);
        if (null != classType) classType.traverse(seen, f);
        if (null != hasDataPropertyList)   for (var x : hasValueList) x.traverse(seen, f);
        if (null != hasDataPropertyList)   for (var x: hasDataPropertyList) x.traverse(seen, f);
        if (null != hasObjectPropertyList) for (var x : hasObjectPropertyList) x.traverse(seen, f);
    }     
}
