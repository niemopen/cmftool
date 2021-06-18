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
public class UnionOf extends ObjectType {
    private final List<Datatype> datatypeList = new ArrayList<>();
    
    public List<Datatype> getDatatypeList()        { return datatypeList; } 
    
    public UnionOf (Model m) {
        super(m);
    }    
    
    public void addDatatype (Datatype c) throws NMFException {
        this.datatypeList.add(c);
    }
    
    public void removeDatatype (Datatype c) throws NMFException {
        int index = this.datatypeList.indexOf(c);
        if (index < 0) throw(new NMFException(String.format("Can't remove Datatype object; not in UnionOf object")));
        this.datatypeList.remove(index);
    }
    
    public void replaceDatatype (Datatype oc, Datatype nc) throws NMFException {
        int index = this.datatypeList.indexOf(oc);
        if (index < 0) throw(new NMFException(String.format("Can't replace Datatype object; not in UnionOf object")));
        this.datatypeList.set(index, nc);        
    } 
        
    @Override
    void traverse (Set<ObjectType> seen, TraverseFunc f) {
        f.func(this);
        if (seen.contains(this)) return;
        seen.add(this);
        if (null != datatypeList) for (var x : datatypeList) x.traverse(seen, f);
    }
}
