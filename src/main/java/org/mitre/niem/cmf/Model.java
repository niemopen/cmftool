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
package org.mitre.niem.cmf;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.cmf.Component.C_CLASSTYPE;
import static org.mitre.niem.cmf.Component.C_DATATYPE;
import static org.mitre.niem.cmf.Component.C_OBJECTPROPERTY;
import static org.mitre.niem.cmf.Namespace.mungedPrefix;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Model extends ObjectType {
    static final Logger LOG = LogManager.getLogger(Model.class);   
    
    private final Map<String,Component> modelComponents = new HashMap<>();      // map component URI to object; for duplicate checking
    private final SortedSet<ClassType> classTypeSet     = new TreeSet<>();
    private final SortedSet<Datatype> datatypeSet       = new TreeSet<>();
    private final SortedSet<Namespace> namespaceSet     = new TreeSet<>();
    private final SortedSet<Property> propertySet       = new TreeSet<>();
    private final Map<String,String> prefixMap          = new HashMap<>();      // prefix -> nsURI
    private final Map<String,Namespace> namespaceMap    = new HashMap<>();      // nsURI -> Namespace

    public SortedSet<ClassType> classTypeSet()           { return classTypeSet; }
    public SortedSet<Datatype> datatypeSet()             { return datatypeSet; }
    public SortedSet<Namespace> namespaceSet()           { return namespaceSet; }
    public SortedSet<Property> propertySet()             { return propertySet; }   
       
    public Model () { }
    public Model (Model m) { }  // FIXME -- useless? expect copy, but doesn't
    
    /**
     * Returns a copy of the map from namespace prefix to namespace URI.
     * @return namespace prefix map
     */
    public Map<String,String> getPrefixMap () { 
        Map<String,String> rv = new HashMap<>();
        prefixMap.forEach((p,n) -> { rv.put(p, n); });
        return rv; 
    }
    
    public Component getComponent (String curi) {
        return modelComponents.get(curi);
    }
 
    public Component getComponent (String nsuri, String lname) {
        String curi = Component.genURI(nsuri, lname);
        return modelComponents.get(curi);
    }
    
    public ClassType getClassType (String nsuri, String lname) {
        Component com = getComponent(nsuri, lname);
        if (null == com) return null;
        return (C_CLASSTYPE == com.getType() ? (ClassType)com : null);
    }
    
    public Datatype getDatatype (String nsuri, String lname) {
        Component com = getComponent(nsuri, lname);
        if (null == com) return null;
        return (C_DATATYPE == com.getType() ? (Datatype)com : null);
    }

    public Property getProperty (String nsuri, String lname) {
        Component com = getComponent(nsuri, lname);
        if (null == com) return null;
        return (C_OBJECTPROPERTY == com.getType() ? (Property)com : null);
    }

    public Namespace getNamespace (String nsuri) { return namespaceMap.get(nsuri); }
           
    public void addComponent (Component c) {
        String curi = c.getURI();
        if (null == curi) return;
        if (modelComponents.containsValue(c)) return;
        if (modelComponents.containsKey(curi)) return;            
        else modelComponents.put(curi, c);
    }
    
    public void removeComponent (Component c) {
        String curi = c.getURI();
        if (null == curi) return;
        if (!modelComponents.containsKey(curi)) return;
        else modelComponents.remove(curi); 
    }
    
    public void addClassType (ClassType x) {
        addComponent(x);
        classTypeSet.add(x);
    }
    
    public void removeClassType (ClassType x) {
        if (!classTypeSet.contains(x)) return;
        removeComponent(x);
        classTypeSet.remove(x);
    }
    
    public void addDatatype (Datatype x) {
        addComponent(x);
        datatypeSet.add(x);
    }
    
    public void removeDatatype (Datatype x) {
        if (!datatypeSet.contains(x)) return;
        removeComponent(x);
        datatypeSet.remove(x);
    }

    public void addProperty (Property x) {
        addComponent(x);
        propertySet.add(x);
    }
    
    public void removeProperty (Property x) {
        if (!propertySet.contains(x)) return;
        removeComponent(x);
        propertySet.remove(x);
    }
    
    // Enforces guarantee that each namespace prefix is mapped to exactly
    // one namespace URI.
    public void addNamespace (Namespace x) {
        // See if Namespace object is already in the model
         for (var ns : namespaceSet) { 
             if (x != ns && x.getNamespaceURI().equals(ns.getNamespaceURI())) {
                 LOG.warn("Namespace {} already in model", x.getNamespaceURI());
                 return;
             }         
         }
         String prefix = x.getNamespacePrefix();
         String nsuri  = x.getNamespaceURI();
         if (prefixMap.containsKey(prefix)) {
             String nprefix = mungedPrefix(prefixMap, prefix);
             LOG.warn("Namespace prefix {} already assigned to {}", prefix, prefixMap.get(prefix));
             LOG.warn("Mapped prefix {} to {} instead", nprefix, nsuri);
             x.setNamespacePrefix(nprefix);
         }
         namespaceSet.add(x); 
         namespaceMap.put(x.getNamespaceURI(), x);
         prefixMap.put(x.getNamespacePrefix(), x.getNamespaceURI());
    }
    
    public void removeNamespace (Namespace x) {
        if (!this.namespaceSet.contains(x))
        this.namespaceSet.remove(x);
        this.namespaceMap.remove(x.getNamespaceURI());
    }
   
}
 