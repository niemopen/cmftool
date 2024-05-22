/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2023 The MITRE Corporation.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import static org.mitre.niem.cmf.NamespaceKind.NSK_BUILTIN;

/**
 * A class for a graph of references between Namespace objects.
 * Namespace FOO references namespace BAR when FOO has a component
 * (Class, Datatype, Property) with a reference to a component in BAR.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ReferenceGraph {
    
    private final Map<Namespace, Set<Namespace>> graph = new HashMap<>();
    
    public ReferenceGraph (List<Component> comps) {
        create(comps);
    }
    
    public Set<Namespace> reachableFrom (Namespace ns) {
        var result = new HashSet<Namespace>();
        var done   = new HashSet<Namespace>();
        var left   = new Stack<Namespace>();
        left.add(ns);
        while (!left.isEmpty()) {
            ns = left.pop();
            result.add(ns);
            done.add(ns);
            var rset = graph.get(ns);
            if (null == rset) continue;
            for (var rns : rset) 
                if (!done.contains(rns)) left.push(rns);
        }
        return result;
    }
    
    public Set<Namespace> referencedBy (Namespace ns) {
        return null;
    }
    
    private void create (List<Component> comps) {
        for (var c : comps) {
            addClassRefs(c.asClassType());
            addDatatypeRefs(c.asDatatype());
            addPropertyRefs(c.asProperty());
        }            
    }
    
    private void addClassRefs (ClassType ct) {
        if (null == ct) return;
        var ctns = ct.getNamespace();
        var ext  = ct.getExtensionOfClass();
        if (null != ext) addRef(ctns, ext.getNamespace());        
        for (var hp : ct.hasPropertyList()) {
            if (hp.augmentingNS().isEmpty())
                addRef(ctns, hp.getProperty().getNamespace());
        }
    }
    
    private void addDatatypeRefs (Datatype d) {
        if (null == d) return;
        var dns  = d.getNamespace();
        var drb  = d.getRestrictionBase();
        var duni = d.unionOf();
        var dlst = d.getListOf();
        if (null != drb) addRef(dns, drb.getNamespace());
        if (null != dlst) addRef(dns, d.getListOf().getNamespace());
        for (var udt : duni) addRef(dns, udt.getNamespace());
    }
    
    private void addPropertyRefs (Property p) {
        if (null == p) return;
        var pns  = p.getNamespace();
        var ct   = p.getClassType();
        var dt   = p.getDatatype();
        if (null != ct) addRef(pns, ct.getNamespace());
        if (null != dt) addRef(pns, dt.getNamespace());
    }
    
    private void addRef (Namespace sns, Namespace tns) {
        if (sns == tns) return;
        if (NSK_BUILTIN == tns.getKind()) return;
        var rset = graph.get(sns);
        if (null == rset) {
            rset = new HashSet<>();
            graph.put(sns, rset);
        }
        rset.add(tns);
    }
     
}
