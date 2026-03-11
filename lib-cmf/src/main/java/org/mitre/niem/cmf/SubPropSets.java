/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
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

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import org.mitre.niem.utility.MapToSet;

/**
 * A class for computing the set of direct and indirect subproperties for
 * the properties in a model.  CMF records subproperties in the other direction;
 * that is, subPropertyOf.  Lazy evaluation, results not computed until
 * needed, then cached.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class SubPropSets {
    
    private final Model m;
    private MapToSet<Property,Property> direct = null;
    private MapToSet<Property,Property> all    = null;
    
    private SubPropSets ()       { m = null; }
    
    public SubPropSets (Model m) { this.m = m; }
    
    public Set<Property> direct (Property p) {
        if (null == direct) {
            direct = new MapToSet<>();
            for (var pp : m.propertyL()) {
                for (var spof : pp.subPropertyOfS()) {
                    direct.add(spof, pp);
                }
            }
        }
        return direct.get(p);
    }
    
    public Set<Property> all (Property p) {
        if (null == all) all = new MapToSet<>();
        if (all.containsKey(p)) return all.get(p);
        var res  = all.get(p);
        var seen = new HashSet<Property>();
        var todo = new Stack<Property>();
        todo.add(p);
        while (!todo.empty()) {
            var np = todo.removeFirst();
            if (seen.contains(np)) continue;
            if (!np.isChoice()) res.add(np);
            seen.add(np);
            todo.addAll(direct(np));
        }
        return res;
    }
  
}
