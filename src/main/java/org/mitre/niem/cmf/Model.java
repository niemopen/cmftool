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
   
    // Map of namespace prefix and URI.  Includes mappings for built-in namespaces 
    // that are not part of the model (but still need to be unique).  Code for
    // processing XML schema piles is allowed to use this.
    private final NamespaceMap nsmap = new NamespaceMap();
    
    // Index of model children (components and namespaces)
    // Built-in namespaces do not appear in these data structures.
    // Must update all three indices when child property changes (namespace, local name, NSprefix)
    private Map<String,Component> components        = new HashMap<>();      // QName -> Component
    private final Map<String,Namespace> prefix2NS   = new HashMap<>();      // prefix -> Namespace
    private final Map<String,Namespace> uri2NS      = new HashMap<>();      // nsURI -> Namespace
    private List<Component> orderedComponents       = null;                 // all Components ordered by QName
    private List<ClassType> orderedClasses          = null;                 // ClassType objects ordered by QName
    private List<Namespace> orderedNamespaces       = null;                 // ordered by namespace prefix

    public Model () { super(); }
    
    public NamespaceMap namespaceMap () { return nsmap; }
    
    // Convenience routine to get URI for this prefix from the namespace map.
    public String getPrefix (String uri) {
        return nsmap.getPrefix(uri);
    }
    
    // Convenience routine to get prefix for this URI from the namespace map.
    public String getURI (String prefix) {
        return nsmap.getURI(prefix);
    }

    // Returns a list of Component objects in the model, ordered by QName.
    // Generates sorted list when necessary, caches for later.
    public List<Component> getComponentList () {
        if (null != orderedComponents) return orderedComponents;
        orderedComponents = new ArrayList<>();
        orderedComponents.addAll(components.values());
        Collections.sort(orderedComponents);
        return orderedComponents;
    }
    
    // Returns a list of ClassType objects in the model, ordered by QName.
    // Generates sorted list when necessary, caches for later.
    public List<ClassType> getClassTypeList () {
        if (null == orderedComponents || null == orderedClasses) {
            orderedClasses = new ArrayList<>();
            for (var c : getComponentList()) {
                var ct = c.asClassType();
                if (null != ct) orderedClasses.add(ct);
            }
        }
        return orderedClasses;
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
        var qn = getQN(nsuri, lname);
        if (null == qn) return null;
        else return components.get(qn);
    }
    
    // Convenience function to generate QName from namespace and name
    public String getQN (String nsuri, String lname) {
        var ns = uri2NS.get(nsuri);
        if (null == ns) {
            LOG.error("no namespace object for Model.getQN({},{})", nsuri, lname);
            return null;
        }
        else return ns.getNamespacePrefix() + ":" + lname;
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
    public Namespace getNamespaceByPrefixOrURI (String s) {
        if (s.contains(":")) return getNamespaceByURI(s);
        else return getNamespaceByPrefix(s);
    }
           
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
    public void componentChange (Component c) {
        components.values().removeIf(v -> c == v);  // remove old mapping QName->Component
        components.put(c.getQName(), c);            // add new mapping
        orderedComponents = null;                   // now we need to regenerate sorted component list
    }
    
    // Must be called by a namespace object when changing its prefix
    // Enforces unique prefix <-> URI mapping, maintains QName->component map
    public void namespacePrefixChange (String nsuri, String opre, String npre) throws CMFException {
        
        // Nothing to do if old and new prefix are the same
        if (opre.equals(npre)) return;
        
        // Fail if new prefix currently bound to a different URI
        var npreURI = nsmap.getURI(npre);
        if (null != npreURI && !npreURI.equals(nsuri)) {
             throw new CMFException(
                String.format("Can't assign prefix %s to %s (already assigned to URI %s)",
                        npre, nsuri, npreURI));        
        }     
        // Update the prefix map and the namespace map object
        var ns = getNamespaceByURI(nsuri);
        prefix2NS.remove(opre);
        prefix2NS.put(npre, ns);
        nsmap.changePrefix(npre, nsuri);    // and update the namespace map object
        
        // When the prefix changes, a lot of QNames may also change
        // Rebuild the QName->component map from scratch and regenerate 
        // the sorted namespace list
        Map<String,Component> nc = new HashMap<>();
        components.values().forEach((Component c) -> {
            var nqn = npre + ":" + c.getName();
            nc.put(nqn, c);
        });
        components = nc;
        orderedNamespaces = null;
    }
  
    // Must be called by a namespace object when changing its URI
    // Enforces unique prefix <-> URI mapping
    public void namespaceURIChange (String prefix, String ouri, String nuri) throws CMFException {
        
        // Nothing to do if old and new URI are the same
        if (ouri.equals(nuri)) return;
        
        // Fail if new URI is currently bound to a different prefix
        var nuriPre = nsmap.getPrefix(nuri);
        if (null != nuriPre && !nuriPre.equals(prefix)) {
             throw new CMFException(
                String.format("Can't change URI for prefix %s to %s (already has prefix %s)",
                        prefix, nuri, nuriPre));                
        }
        // Update the URI map and the namespace map object
        var ns = getNamespaceByPrefix(prefix);
        uri2NS.remove(ouri);
        uri2NS.put(nuri, ns);
        nsmap.removePrefix(prefix);
        nsmap.assignPrefix(prefix, nuri);
    }

    
}
 