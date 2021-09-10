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

/**
 * A class to represent a NIEM model component.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ObjectType {
    private Model model = null;             // every object knows the model it is part of
    private String sequenceID = null;       // any model component can have a sequence number

    public void setSequenceID (String s) { sequenceID = s; }

    public Model getModel()            { return model; }
    public String getSequenceID ()     { return sequenceID; }
    public boolean isModelChild ()     { return false; }        // override in Component and Namespace class
    
    public ObjectType () {
    }
   
    public ObjectType (Model m) {
        model = m;
    }
   
    // Override for components and namespaces
    public void addToModelSet (Model m) { }

}
