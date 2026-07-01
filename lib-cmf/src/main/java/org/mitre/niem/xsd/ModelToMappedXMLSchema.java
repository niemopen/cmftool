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
package org.mitre.niem.xsd;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ObjectProperty;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToMappedXMLSchema {
    
    private final Model m;
    private Mapping map = new Mapping();                            // canonical to simple name map, if provided
    private Set<ObjectProperty> msgPropS = null;                    // message property objects, if provided
    
    public ModelToMappedXMLSchema (Model m) {
        this.m = m;
    }
      
    /**
     * Provides a Mapping object which will be used to replace model property
     * QNames in the schema; for example, msg:lname instead of
     * nc:PersonSurName.  A null parameter clears any existing map.
     * @param map Mapping object
     */
    public void setMapping (Mapping map) {
        if (null == map) this.map = new Mapping();
        else this.map = map;
    }

    // The URI of a model component ia either a URN or contains "://".
    public void setMessageProperty (ObjectProperty mprop) {
        msgPropS = (mprop == null) ? null : Set.of(mprop);        
    }
    
    public void setMessageProperties (Set<ObjectProperty> mpropS) {
        if (null == mpropS) msgPropS = null;
        else msgPropS = mpropS;
    }

    public void writeModelXSD (File outD) {
        
        // Establish needed components for the specified message properties
        Set<Component> compS;
        if (msgPropS.isEmpty()) compS = m.componentSet();
        else compS = m.messageComponents(msgPropS);
        
        // Construct set of required namespaces (after mapping)
        var nsuS = new HashSet<String>();
        for (var c : compS) {
            var nsU = map.uriToTargetNSU(c.uri());
            nsuS.add(nsU);
        }
        
        
    }
    
    
}
