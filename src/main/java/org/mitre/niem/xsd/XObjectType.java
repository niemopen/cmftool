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
package org.mitre.niem.xsd;

import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI_PREFIX;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ObjectType;
import org.xml.sax.Attributes;

/**
 * A class for an element in a CMF XML document.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XObjectType {
 
    private String id             = null;
    private String ref            = null;
    private String uri            = null;
    private String seq            = null;
    private String stringVal      = null;
    private boolean isEmpty       = true;    // false if this XML element has child or string content
    XObjectType parent            = null;    // remember parent of IDREF/URI placeholder
    private int lineNumber        = 0;       // XML element starting line number
    private String componentLname = null;    // NIEM model component local name
    
    XObjectType () {}
    
    XObjectType (Model m, XObjectType pobj, String ens, String eln, Attributes a, int line) {
        parent = pobj;
        componentLname = eln;
        lineNumber = line;
        for (int i = 0; i < a.getLength(); i++) {
            if (a.getURI(i).startsWith(STRUCTURES_NS_URI_PREFIX)) {
                if (null != a.getLocalName(i)) {
                    switch (a.getLocalName(i)) {
                        case "id":         id = a.getValue(i); break;
                        case "ref":        ref = a.getValue(i); break;
                        case "uri":        uri = a.getValue(i); break;
                        case "sequenceID": seq = a.getValue(i); break;
                        default: break;
                    }
                }
            }
        }
    }

    public void setStringVal (String s) { 
        stringVal = s;
        isEmpty = false;
    }   
    public void setObject (ObjectType o)    { }                     // override in subclass
    public ObjectType getObject ()          { return null; }        // override in subclass
    public String getStringVal ()           { return stringVal; }
    public boolean getIsEmpty ()            { return isEmpty; }
    public XObjectType getParent ()         { return parent; }
    public String getSequenceID ()          { return seq; }
    public int getLineNumber ()             { return lineNumber; }
    public String getComponentLname ()      { return componentLname; }
       
    // Returns the reference key in a placeholder object, or null if not a
    // placeholder.  (Reference placeholders have @ref, or @uri and no content)
    public String getRefKey () {
        if (null != ref) return ref;
        else if (null == uri) return null;
        else if (isEmpty) return uri;
        else return null;
    }
    
    // Returnss the reference identifier for an object that can be the target
    // of a reference placeholder; ie. objects with @id, or @uri with content
    public String getIDKey () {
        if (null != id) return id;
        else if (null == uri) return null;
        else if (isEmpty) return null;
        else return uri;
    }
    
    public void addAsChild  (XObjectType child) { 
        this.isEmpty = false; 
    }
  
    public void addToClassType (XClassType c) { }
    public void addToDatatype (XDatatype dt) { }
    public void addToFacet (XFacet f) { }
    public void addToHasProperty (XHasProperty h) { }
    public void addToNamespace (XNamespace ns) { }
    public void addToProperty (XProperty op) { }
    public void addToRestrictionOf (XRestrictionOf r) { }
    public void addToUnionOf (XUnionOf u) { }    
}
