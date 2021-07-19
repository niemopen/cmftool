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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import static org.mitre.niem.nmf.Component.C_CLASSTYPE;
import static org.mitre.niem.nmf.Component.C_DATAPROPERTY;
import static org.mitre.niem.nmf.Component.C_DATATYPE;
import static org.mitre.niem.nmf.Component.C_OBJECTPROPERTY;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Model extends ObjectType {
    private final Map<String,Component> modelComponents       = new HashMap<>();        // map component URI to object; for duplicate checking
    private final SortedSet<ClassType> classTypeSet           = new TreeSet<>();
    private final SortedSet<DataProperty> dataPropertySet     = new TreeSet<>();
    private final SortedSet<Datatype> datatypeSet             = new TreeSet<>();
    private final SortedSet<Namespace> namespaceSet           = new TreeSet<>();
    private final SortedSet<ObjectProperty> objectPropertySet = new TreeSet<>();
    
    private final Map<String,Namespace> namespaceMap          = new HashMap<>();

    public SortedSet<ClassType> classTypeSet()           { return classTypeSet; }
    public SortedSet<DataProperty> dataPropertySet()     { return dataPropertySet; }
    public SortedSet<Datatype> datatypeSet()             { return datatypeSet; }
    public SortedSet<Namespace> namespaceSet()           { return namespaceSet; }
    public SortedSet<ObjectProperty> objectPropertySet() { return objectPropertySet; }   
    
    public Model () { }
    public Model (Model m) { }
 
    public Component getComponent (String nsuri, String lname) {
        String curi = nsuri.endsWith("#") ? nsuri + lname : nsuri + "#" + lname;
        return modelComponents.get(curi);
    }
    
    public ClassType getClassType (String nsuri, String lname) {
        Component com = getComponent(nsuri, lname);
        if (null == com) return null;
        return (C_CLASSTYPE == com.getType() ? (ClassType)com : null);
    }
    public DataProperty getDataProperty (String nsuri, String lname) {
        Component com = getComponent(nsuri, lname);
        if (null == com) return null;
        return (C_DATAPROPERTY == com.getType() ? (DataProperty)com : null);
    }
    
    public Datatype getDatatype (String nsuri, String lname) {
        Component com = getComponent(nsuri, lname);
        if (null == com) return null;
        return (C_DATATYPE == com.getType() ? (Datatype)com : null);
    }

    public ObjectProperty getObjectProperty (String nsuri, String lname) {
        Component com = getComponent(nsuri, lname);
        if (null == com) return null;
        return (C_OBJECTPROPERTY == com.getType() ? (ObjectProperty)com : null);
    }

    public Namespace getNamespace (String nsuri) { return namespaceMap.get(nsuri); }
           
    // These methods test arguments for duplicate components and such.
    // Suitable for a user interface or when processing user-supplied data.
    public void addComponent (Component c) throws NMFException {
        String curi = c.getURI();
        if (null == curi) return;
        if (modelComponents.containsValue(c)) return;
        if (modelComponents.containsKey(curi)) throw(new NMFException(String.format("Can't add component %s to model; already present", curi)));            
        else modelComponents.put(curi, c);
        
    }
    
    public void removeComponent (Component c) throws NMFException {
        String curi = c.getURI();
        if (null == curi) return;
        if (!modelComponents.containsKey(curi)) throw(new NMFException(String.format("Can't remove component %s from model; not present", curi)));
        else modelComponents.remove(curi);
    }
    
    public void addClassType (ClassType x) throws NMFException {
        this.addComponent(x);
        this.classTypeSet.add(x);
    }
    
    public void removeClassType (ClassType x) throws NMFException {
        if (!this.classTypeSet.contains(x)) 
            throw(new NMFException(String.format("Can't remove class %s; not in model", x.getURI())));
        removeComponent(x);
        this.classTypeSet.remove(x);
    }
    
    public void addDataProperty (DataProperty x) throws NMFException {
        this.addComponent(x);
        this.dataPropertySet.add(x);
    }
    
    public void removeDataProperty (DataProperty x) throws NMFException {
        if (!this.dataPropertySet.contains(x))
            throw(new NMFException(String.format("Can't remove data property %s; not in model", x.getURI())));
        removeComponent(x);
        this.dataPropertySet.remove(x);
    }
    
    public void addDatatype (Datatype x) throws NMFException {
        this.addComponent(x);
        this.datatypeSet.add(x);
    }
    
    public void removeDatatype (Datatype x) throws NMFException {
        if (!this.datatypeSet.contains(x))
            throw(new NMFException(String.format("Can't remove data property %s; not in model", x.getURI())));
        removeComponent(x);
        this.datatypeSet.remove(x);
    }

    public void addObjectProperty (ObjectProperty x) throws NMFException {
        this.addComponent(x);
        this.objectPropertySet.add(x);
    }
    
    public void removeObjectProperty (ObjectProperty x) throws NMFException {
        if (!this.objectPropertySet.contains(x))
            throw(new NMFException(String.format("Can't remove object property %s; not in model", x.getURI())));
        removeComponent(x);
        this.objectPropertySet.remove(x);
    }
    
    public void addNamespace (Namespace x) throws NMFException {
         for (var ns : namespaceSet) { 
             if (x != ns && x.getNamespaceURI().equals(ns.getNamespaceURI()))
                 throw(new NMFException(String.format("Can't add namespace %s; already in model", x.getNamespaceURI())));
         }
         this.namespaceSet.add(x); 
         this.namespaceMap.put(x.getNamespaceURI(), x);
    }
    
    public void removeNamespace (Namespace x) throws NMFException {
        if (!this.namespaceSet.contains(x))
            throw(new NMFException(String.format("Can't remove namespace %s; not in model", x.getNamespaceURI())));
        this.namespaceSet.remove(x);
        this.namespaceMap.remove(x.getNamespaceURI());
    }
    
    
    public void collectModelObjects() {
        Set<ObjectType>seen = new HashSet<>();
        TraverseFunc func = (ObjectType o) -> {
            o.addToModelSet();
        };
        this.traverse(seen, func);
    }

    @Override
    void traverse (Set<ObjectType> seen, TraverseFunc f) {
        for (var x : classTypeSet)      { x.traverse(seen, f); }
        for (var x : dataPropertySet)   { x.traverse(seen, f); }
        for (var x : datatypeSet)       { x.traverse(seen, f); }
        for (var x : objectPropertySet) { x.traverse(seen, f); }
        for (var x : namespaceSet)      { x.traverse(seen, f); }        
    }    
}
 