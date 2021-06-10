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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.mitre.niem.xsd.XMLDataRecord;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Model extends ObjectType {
    
    protected int nextID                              = 0;
    protected List<ObjectType> allModelObjects        = new ArrayList<>();
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
    
    public void addToObjectList (ObjectType o) { 
        allModelObjects.add(o);
        addObjectID(o.getID());
    }
    
    
    public Model (Model m, String ens, String eln, Attributes a) {
        super(null, ens, eln, a);
    }
    
    @Override
    public int addChild (XMLDataRecord cdat) {
        return cdat.obj.addToModel(this, cdat);
    }
    
    public String generateID () {
        return String.format("i%02d", nextID++);
    }
    
    private void addObjectID (String id) {
        if (null == id) return;
        if (id.matches("i\\d+")) {
            int inum = Integer.parseInt(id.substring(1));
            if (inum >= nextID) {
                nextID = inum + 1;
            }
        }
    }
   
    public void dump () {}
}
