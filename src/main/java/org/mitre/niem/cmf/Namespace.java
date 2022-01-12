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
package org.mitre.niem.cmf;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Namespace extends ObjectType implements Comparable<Namespace> {
    private Model model = null;             // Component objects know the Model they are part of     
    private String namespaceURI = null;
    private String namespacePrefix = null;
    private String definition = null;
    private boolean isExternal = false;
    
    public Namespace () { super(); }
     
    public Namespace (String p, String nsuri) {
        super();
        namespacePrefix = p ;
        namespaceURI = nsuri;
    }
    
    @Override
    public boolean isModelChild ()            { return true; }      // Namespace objects are model children
    
    public void setNamespaceURI (String s)    { 
        namespaceURI = s;
        if (null != getModel()) getModel().childChanged(this, namespacePrefix);
    }
    
    public void setNamespacePrefix (String s) throws CMFException { 
        String oldPrefix = namespacePrefix;
        namespacePrefix = s;
        if (null != getModel()) {
            Namespace en = getModel().getNamespaceByPrefix(s);
            if (null != en) {
            throw new CMFException(
                String.format("Duplicate namespace prefix \"%s\" (bound to %s and %s)",
                        s, namespaceURI, en.getNamespaceURI()));
            }
            getModel().childChanged(this, oldPrefix);
        }
    }
    
    void setModel (Model m)                   { model = m; }
    public void setDefinition (String s)      { definition = s; }
    public void setIsExternal (boolean f)     { isExternal = f; }
    public void setIsExternal (String s)      { isExternal = "true".equals(s); }
    
    public Model getModel ()                  { return model; }
    public String getNamespaceURI ()          { return namespaceURI; }
    public String getNamespacePrefix ()       { return namespacePrefix; }
    public String getDefinition ()            { return definition; }
    public boolean isExternal ()           { return isExternal; }

    // Enforces guarantee that each namespace in a model has a unique prefix
    @Override
    public void addToModel (Model m) throws CMFException {
        m.addNamespace(this);
    }   
    
    @Override
    public int compareTo (Namespace o) {
        return this.namespacePrefix.compareTo(o.namespacePrefix);
    }    
}
