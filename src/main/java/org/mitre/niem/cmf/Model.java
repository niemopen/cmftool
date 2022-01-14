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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.cmf.Component.C_CLASSTYPE;
import static org.mitre.niem.cmf.Component.C_DATATYPE;
import static org.mitre.niem.cmf.Component.C_OBJECTPROPERTY;

/**
 * A class for a CMF model.
 * 
 * The class guarantees that every Namespace in the Model will have a unique
 * namespace prefix, which means a QName is a unique identifier for a 
 * Component.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Model extends ObjectType {
    static final Logger LOG = LogManager.getLogger(Model.class);   
    
    // Index of model children (components and namespaces)
    // Must update all three indices when child property changes (namespace, local name, NSprefix)
    private Map<String,Component> components                = new HashMap<>();      // QName -> Component
    private final Map<String,Namespace> nsPrefix            = new HashMap<>();      // prefix -> Namespace
    private final Map<String,Namespace> namespaces          = new HashMap<>();      // nsURI -> Namespace
    private List<Component> orderedComponents               = null;
    private List<Namespace> orderedNamespaces               = null;

    public Model () { super(); }
    
    public Map<String,Namespace> getPrefixMap () { return nsPrefix; }

    public List<Component> getComponentList () {
        if (null != orderedComponents) return orderedComponents;
        orderedComponents = new ArrayList<>();
        orderedComponents.addAll(components.values());
        Collections.sort(orderedComponents);
        return orderedComponents;
    }
    
    public List<Namespace> getNamespaceList () {
        if (null != orderedNamespaces) return orderedNamespaces;
        orderedNamespaces = new ArrayList<>();
        orderedNamespaces.addAll(namespaces.values());                
        Collections.sort(orderedNamespaces);
        return orderedNamespaces;
    }
   
    public Component getComponent (String qn) {
        return components.get(qn);
    }
 
    public Component getComponent (String nsuri, String lname) {
        Namespace n = namespaces.get(nsuri);
        if (null == n) return null;
        String qn = n.getNamespacePrefix() + ":" + lname;
        return components.get(qn);
    }
       
    public ClassType getClassType (String qname) {
        Component com = getComponent(qname);
        return (null != com && C_CLASSTYPE == com.getType()) ? (ClassType)com : null;
    }
    
    public Datatype getDatatype (String qname) {
        Component com = getComponent(qname);
        return (null != com && C_DATATYPE == com.getType()) ? (Datatype)com : null;
    }

    public Property getProperty (String qname) {
        Component com = getComponent(qname);
        return (null != com && C_OBJECTPROPERTY == com.getType()) ? (Property)com : null;
    }       
    
    public ClassType getClassType (String nsuri, String lname) {
        Component com = getComponent(nsuri, lname);
        return (null != com && C_CLASSTYPE == com.getType()) ? (ClassType)com : null;
    }
    
    public Datatype getDatatype (String nsuri, String lname) {
        Component com = getComponent(nsuri, lname);
        return (null != com && C_DATATYPE == com.getType()) ? (Datatype)com : null;
    }

    public Property getProperty (String nsuri, String lname) {
        Component com = getComponent(nsuri, lname);
        return (null != com && C_OBJECTPROPERTY == com.getType()) ? (Property)com : null;
    }

    public Namespace getNamespaceByPrefix (String prefix) { return nsPrefix.get(prefix); }
    public Namespace getNamespaceByURI (String nsuri)     { return namespaces.get(nsuri); }
           
    public void addComponent (Component c) {
        String qn = c.getQName();
        components.put(qn, c);
        orderedComponents = null;
        c.setModel(this);
    }
    
// There are a lot of referential integrity problems here...
    public void removeComponent (Component c) {
        String qn = c.getQName();
        components.remove(qn);
        orderedComponents = null;
        c.setModel(null);
    }

    // Enforces guarantee that each namespace has a unique prefix.
    public void addNamespace (Namespace n) throws CMFException {
        String prefix = n.getNamespacePrefix();
        String nsuri  = n.getNamespaceURI();
        Namespace en  = nsPrefix.get(prefix);
        if (null != en && !en.getNamespaceURI().equals(nsuri)) {
            throw new CMFException(
                String.format("Duplicate namespace prefix \"%s\" (bound to %s and %s)",
                        prefix, nsuri, en.getNamespaceURI()));
        }
        namespaces.put(nsuri, n);
        nsPrefix.put(prefix, n);
        orderedNamespaces = null;
        n.setModel(this);
    }
    
// There are even more referential integrity problems here...
//    public void removeNamespace (Namespace n) {
//        String prefix = n.getNamespacePrefix();
//        String nsuri  = n.getNamespaceURI();
//        nsPrefix.remove(prefix);
//        namespaces.remove(nsuri);
//        orderedNamespaces = null;
//        n.setModel(null);
//    }
    
    // Must be called by a component when namespace or local name is changed
    public void childChanged (Component c) {
        components.values().removeIf(v -> c == v);
        components.put(c.getQName(), c);
        orderedComponents = null;
    }
    
    // Must be called by a namespace when the prefix or URI is changed
    public void childChanged (Namespace n, String oldPrefix) {
        namespaces.values().removeIf(v -> n == v);
        namespaces.put(n.getNamespaceURI(), n);
        if (null != oldPrefix && !oldPrefix.equals(n.getNamespacePrefix())) {
            nsPrefix.remove(oldPrefix);
            nsPrefix.put(n.getNamespacePrefix(), n);
            Map<String,Component> nc = new HashMap<>();
            components.values().forEach((Component c) -> {
                nc.put(c.getQName(), c);
            });
            components = nc;
        }
        orderedNamespaces = null;
    }
    
}
 