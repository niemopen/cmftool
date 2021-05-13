/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2021 The MITRE Corporation.
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

package org.mitre.niem.nmf.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.mitre.niem.nmf.ModelType;

/**
 * A class for a NIEM model object.
 * 
 * @author Scott Renner
 *  <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelTypeEx extends ModelType {

    protected Map<String,ClassTypeEx> clas                    = new HashMap<>();     // uri->class
    protected Map<String,DataPropertyTypeEx> dataProp         = new HashMap<>();
    protected Map<String,DatatypeTypeEx> datatype             = new HashMap<>();
    protected Map<String,NamespaceTypeEx> namespace           = new HashMap<>();
    protected Map<String,ObjectPropertyTypeEx> objectProperty = new HashMap<>();
    
    public void addClass (String uri, ClassTypeEx c)                    { clas.put(uri, c); }
    public void addDataProperty (String uri, DataPropertyTypeEx dp)     { dataProp.put(uri, dp); }
    public void addDatatype (String uri, DatatypeTypeEx dt)            { datatype.put(uri, dt); }
    public void addNamespace (String uri, NamespaceTypeEx ns)           { namespace.put(uri, ns); }
    public void addObjectProperty (String uri, ObjectPropertyTypeEx op) { objectProperty.put(uri, op); }
    
    public void addComponents () {
        Set<ObjectTypeEx> haveSeen = new HashSet<>();
        for (ObjectTypeEx o : this.clazzOrDataPropertyOrDatatype) {
            o.addToModel(this, haveSeen);
        }
    }
}
