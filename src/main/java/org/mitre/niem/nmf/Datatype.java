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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Datatype extends Component {   
    private RestrictionOf restrictionOf = null;
    private UnionOf unionOf = null;
    
    public void setRestrictionOf (RestrictionOf r) { restrictionOf = r; }
    public void setUnionOf (UnionOf u)             { unionOf = u; }
    
    public RestrictionOf getRestrictionOf() { return restrictionOf; }
    public UnionOf getUnionOf()             { return unionOf; }
    
    public Datatype () { type = C_DATATYPE; }
    
    public Datatype (Model m) {
        super(m);
    }
    
    @Override
    public void addToModelSet () {
        try {
            this.getModel().addDatatype(this);
        } catch (NMFException ex) {
            Logger.getLogger(Namespace.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    void traverse (Set<ObjectType> seen, TraverseFunc f) {
        f.func(this);
        if (seen.contains(this)) return;
        seen.add(this);
        if (null != this.getNamespace())   this.getNamespace().traverse(seen, f);        
        if (null != restrictionOf) restrictionOf.traverse(seen, f);
        if (null != unionOf) unionOf.traverse(seen, f);
    } 
}
