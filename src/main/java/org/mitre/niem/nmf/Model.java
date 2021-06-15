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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mitre.niem.xsd.XMLDataRecord;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Model extends ObjectType {
    
    protected List<ClassType> classList               = new ArrayList<>();
    protected List<DataProperty> dataPropertyList     = new ArrayList<>();
    protected List<Datatype> datatypeList             = new ArrayList<>();
    protected List<Namespace> namespaceList           = new ArrayList<>();
    protected List<ObjectProperty> objectPropertyList = new ArrayList<>();
    
    public List<ClassType> classList()               { return classList; }
    public List<DataProperty> dataPropertyList()     { return dataPropertyList; }
    public List<Datatype> datatypeList()             { return datatypeList; }
    public List<Namespace> namespaceList()           { return namespaceList; }
    public List<ObjectProperty> objectPropertyList() { return objectPropertyList; }   
    
    public Model (Model m, String ens, String eln, Attributes a) {
        super(null, ens, eln, a);
    }
    
    @Override
    public int addChild (ObjectType child, int index) {
        return child.addToModel(this, index);
    }
    
    public void testTraverse() {
        Set<ObjectType>seen = new HashSet<>();
        Map<ObjectType,Integer> refct = new HashMap<>();
        TraverseFunc func = (ObjectType o) -> {
            int ct = 1;
            if (refct.containsKey(o)) {
                ct = refct.get(o);
                refct.put(o, ct+1);
            }
            else {
                refct.put(o, 1);
            }
        };
        this.traverse(seen, func);
    }

    @Override
    void traverse (Set<ObjectType> seen, TraverseFunc f) {
        f.func(this);
        if (seen.contains(this)) return;
        seen.add(this);
        Collections.sort(classList);          for (var x : classList)          x.traverse(seen, f);
        Collections.sort(dataPropertyList);   for (var x : dataPropertyList)   x.traverse(seen, f);
        Collections.sort(datatypeList);       for (var x : datatypeList)       x.traverse(seen, f);
        Collections.sort(namespaceList);      for (var x : namespaceList)      x.traverse(seen, f);
        Collections.sort(objectPropertyList); for (var x : objectPropertyList) x.traverse(seen, f);
    }    
}
 