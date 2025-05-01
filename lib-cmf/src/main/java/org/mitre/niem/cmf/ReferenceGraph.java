/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2025 The MITRE Corporation.
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
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class for a graph of references between Namespace objects.
 * Namespace FOO references namespace BAR when FOO has a component
 * (Class, Datatype, Property) with a reference to a component in BAR.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ReferenceGraph {
    static final Logger LOG = LogManager.getLogger(ReferenceGraph.class);  
    
    private final Map<Namespace, Set<Namespace>> graph = new HashMap<>();
    
    public ReferenceGraph (Model m) {
        for (var ct : m.classTypeL()) addClassRefs(ct);
        for (var dt : m.datatypeL())  addDatatypeRefs(dt);
        for (var p  : m.propertyL())  addPropertyRefs(p);
        for (var ns : m.namespaceSet()) addAugmentRefs(ns);
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
    
    private void addClassRefs (ClassType ct) {
        var ns  = ct.namespace();
        var sub = ct.subClass();
        if (null != sub) addRef(ns, sub.namespace()); 
        for (var pa : ct.propL()) {
            addRef(ns, pa.property().namespace());
        }
    }
    
    private void addDatatypeRefs (Datatype d) {
        var ns  = d.namespace();
        if (null != d.base()) addRef(ns, d.base().namespace());
        if (null != d.itemType()) addRef(ns, d.itemType().namespace());
        if (null != d.memberL()) {
            for (var m : d.memberL())
                addRef(ns, m.namespace());
        }
    }
    
    private void addPropertyRefs (Property p) {
        if (null == p.type()) return;
        var ns = p.namespace();
        addRef(ns, p.type().namespace());
        if (null != p.subProperty()) addRef(ns, p.subProperty().namespace());
    }
    
    private void addAugmentRefs (Namespace ns) {
        for (var ar : ns.augL()) {
            var ct = ar.classType();
            if (null != ct) addRef(ns, ct.namespace());
        }
    }
    
    private void addRef (Namespace from, Namespace to) {
        if (null == from) return;
        if (null == to) return;
        if (from == to) return;
        var rset = graph.get(from);
        if (null == rset) {
            rset = new HashSet<>();
            graph.put(from, rset);
        }
        rset.add(to);
    }
         
}
