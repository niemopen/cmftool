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

import java.util.Map;
import java.util.Set;
import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI_PREFIX;
import org.mitre.niem.xsd.XMLDataRecord;
import org.xml.sax.Attributes;

/**
 * A class to represent a NIEM model component.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ObjectType {
    
    // These are properties of the NIEM model object
    protected Model model = null;             // every object knows the model it is part of
    protected String id = null;               // needed for any object in graph more than once
    protected String sequenceID = null;       // any model component can have a sequence number
    protected String componentNS = null;      // NIEM model component URI
    protected String componentLname = null;   // NIEM model component local name

    public void setID (String s)         { id = s; }
    public void setSequenceID (String s) { sequenceID = s; }
    public void setStringVal (String s)  { }                    // override this in objects with string value

    public Model getModel()            { return model; }
    public boolean hasID()             { return null != id; }
    public String getID()              { return id; }
    public String getSequenceID ()     { return sequenceID; }
    public String getComponentNS ()    { return componentNS; }
    public String getComponentLname () { return componentLname; }
    
    public ObjectType () {
    }
    
    /**
     * Construct a NIEM model component object from values supplied by SAX parser
     * @param ens element namespace uri
     * @param eln element local name
     * @param a list of XML attributes from SAX parser
     */
    public ObjectType (Model m, String ens, String eln, Attributes a) {
        model = m;
        componentNS = ens;
        componentLname = eln;
        for (int i = 0; i < a.getLength(); i++) {
            if (a.getURI(i).startsWith(STRUCTURES_NS_URI_PREFIX)) {
                if ("sequenceId".equals(a.getLocalName(i))) {
                    sequenceID = a.getValue(i);
                }
                // Remember @id to avoid needless diffs between model file versions
                else if ("id".equals(a.getLocalName(i))) {
                    id = a.getValue(i);
                }
            }
        }
    }
    
    public int addChild  (ObjectType child, int index) { return -1; }
    
    public int addToClass (ClassType c, int index) { return -1; }
    public int addToDataProperty (DataProperty dp, int index) { return -1; }
    public int addToDatatype (Datatype dt, int index) { return -1; }
    public int addToExtensionOf (ExtensionOf e, int index) { return -1; }
    public int addToHasDataProperty (HasDataProperty h, int index) { return -1; }
    public int addToHasObjectProperty (HasObjectProperty h, int index) { return -1; }
    public int addToHasValue (HasValue h, int index) { return -1; }
    public int addToModel (Model m, int index) { return -1; }
    public int addToNamespace (Namespace ns, int index) { return -1; }
    public int addToObjectProperty (ObjectProperty op, int index) { return -1; }
    public int addToRestrictionOf (RestrictionOf r, int index) { return -1; }
    public int addToSubPropertyOf (SubPropertyOf s, int index) { return -1; }
    public int addToUnionOf (UnionOf u, int index) { return -1; }

    interface TraverseFunc {
        void func (ObjectType o);
    }
    
    void traverse (Set<ObjectType> seen, TraverseFunc f) {
    }
    
}
