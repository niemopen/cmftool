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
 * namespace prefix.  This, plus the NIEM naming rules, ensures that each 
 * QName identifies at most one Component.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Model extends ObjectType {
    static final Logger LOG = LogManager.getLogger(Model.class);
    
    // Info about schema documents needed when writing the model as XSD
    static private final SchemaDocument none            = new SchemaDocument(null,null,null,null,null);
    private final Map<String,SchemaDocument> schemadoc  = new HashMap<>();           // nsURI -> schema info
    
    public Map<String,SchemaDocument> schemadoc () { return schemadoc; }
    public void addSchemaDoc(String nsuri, SchemaDocument doc) {
        schemadoc.put(nsuri, doc);
    }
    public String conformanceTargets (String ns)    { return schemadoc.getOrDefault(ns,none).confTargets(); }
    public String filePath (String ns)              { return schemadoc.getOrDefault(ns,none).filePath(); }
    public String niemVersion (String ns)           { return schemadoc.getOrDefault(ns,none).niemVersion(); }
    public String schemaVersion (String ns)         { return schemadoc.getOrDefault(ns,none).schemaVersion(); }

    
    // Map of namespace prefix and URI.  Includes mappings for built-in namespaces 
    // that are not part of the model (but still need to be unique).  Code for
    // processing XML schema piles is allowed to use this.
    private final NamespaceMap nsmap = new NamespaceMap();
    
    // Index of model children (components and namespaces)
    // Built-in namespaces do not appear in these data structures.
    // Must update all three indices when child property changes (namespace, local name, NSprefix)
    private Map<String,Component> components                = new HashMap<>();      // QName -> Component
    private final Map<String,Namespace> prefix2NS           = new HashMap<>();      // prefix -> Namespace
    private final Map<String,Namespace> uri2NS              = new HashMap<>();      // nsURI -> Namespace
    private List<Component> orderedComponents               = null;                 // ordered by QName
    private List<Namespace> orderedNamespaces               = null;                 // ordered by namespace prefix

    public Model () { super(); }
    
    public NamespaceMap namespaceMap () { return nsmap; }

    // Returns a list of Component objects in the model, ordered by QName.
    // Generates sorted list when necessary, caches for later.
    public List<Component> getComponentList () {
        if (null != orderedComponents) return orderedComponents;
        orderedComponents = new ArrayList<>();
        orderedComponents.addAll(components.values());
        Collections.sort(orderedComponents);
        return orderedComponents;
    }
    
    // Returns a list of Namespace objects in the Model, ordered by prefix.
    // Generates sorted list when necessary, caches for later.
    public List<Namespace> getNamespaceList () {
        if (null != orderedNamespaces) return orderedNamespaces;
        orderedNamespaces = new ArrayList<>();
        orderedNamespaces.addAll(uri2NS.values());                
        Collections.sort(orderedNamespaces);
        return orderedNamespaces;
    }
   
    public Component getComponent (String qn) {
        return components.get(qn);
    }
 
    public Component getComponent (String nsuri, String lname) {
        Namespace n = uri2NS.get(nsuri);
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

    public Namespace getNamespaceByPrefix (String prefix) { return prefix2NS.get(prefix); }
    public Namespace getNamespaceByURI (String nsuri)     { return uri2NS.get(nsuri); }
           
    public void addComponent (Component c) {
        String qn = c.getQName();
        components.put(qn, c);
        orderedComponents = null;
        c.setModel(this);
    }
    
    // There are a lot of referential integrity problems lurkning here.
    // When used to remove augmentation components, those problems don't occur.
    // Beware if you use it for anything else.
    public void removeComponent (Component c) {
        String qn = c.getQName();
        components.remove(qn);
        orderedComponents = null;
        c.setModel(null);
    }

    // Enforces guarantee that each namespace has a unique prefix.
    public void addNamespace (Namespace n) throws CMFException {
        String prefix     = n.getNamespacePrefix(); // desired namespace prefix
        String nsuri      = n.getNamespaceURI();    // namespace URI
        String currentURI = nsmap.getURI(prefix);   // current URI assigned to prefix (if anything)
        
        // Throw exception if the desired prefix is already assigned to something else
        if (null != currentURI && !currentURI.equals(nsuri)) {
             throw new CMFException(
                String.format("Can't add namespace %s=%s to model (prefix already assigned to %s)",
                        prefix, nsuri, currentURI));
        }
        nsmap.assignPrefix(prefix, nsuri);  // we just checked, no munging required
        uri2NS.put(nsuri, n);               // also update model map (uri->model NS object)
        prefix2NS.put(prefix, n);           // also update model map (prefix->model NS object)
        orderedNamespaces = null;           // now we need to regenerate sorted namespace list
        n.setModel(this);                   // tell the NS object it now belongs to this model
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
        components.values().removeIf(v -> c == v);  // remove old mapping QName->Component
        components.put(c.getQName(), c);            // add new mapping
        orderedComponents = null;                   // now we need to regenerate sorted component list
    }
    
    // Must be called by a namespace object when its prefix or URI is changed.
    // Enforces unique prefix mapping.
    public void childChanged (Namespace n, String oldPrefix) throws CMFException {
        String newPrefix  = n.getNamespacePrefix();
        String nsuri      = n.getNamespaceURI();
        String currentURI = nsmap.getURI(newPrefix);
        if (null != currentURI && !currentURI.equals(nsuri)) {
             throw new CMFException(
                String.format("Can't add namespace %s=%s to model (prefix already assigned to %s)",
                        newPrefix, nsuri, currentURI));            
        }
        uri2NS.values().removeIf(v -> n == v);      // remove old mapping prefix->Namespace
        uri2NS.put(n.getNamespaceURI(), n);         // add new mapping prefix->Namespace
        nsmap.changePrefix(newPrefix, nsuri);
        
        // If the prefix changes, a lot of QNames may also change
        // We rebuild the QName->component map from scratch
        if (null != oldPrefix && !oldPrefix.equals(n.getNamespacePrefix())) {
            prefix2NS.remove(oldPrefix);
            prefix2NS.put(n.getNamespacePrefix(), n);
            Map<String,Component> nc = new HashMap<>();
            components.values().forEach((Component c) -> {
                nc.put(c.getQName(), c);
            });
            components = nc;
        }
        orderedNamespaces = null;   // now we need to regenerate sorted namespace list
    }
    
}
 