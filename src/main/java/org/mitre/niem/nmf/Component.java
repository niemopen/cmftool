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
 * An abstract class for NIEM model components that have a name and namespace.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public abstract class Component extends ObjectType implements Comparable<Component> {
    public static final short C_CLASSTYPE = 0;
    public static final short C_DATAPROPERTY = 1;
    public static final short C_DATATYPE = 2;
    public static final short C_OBJECTPROPERTY = 3;
    
    protected short type;
    private String name = null;
    private Namespace namespace = null;
    private String definition = null;
    
    public void setName (String s)          { name = s; }
    public void setNamespace (Namespace ns) { namespace = ns; }
    public void setDefinition (String s)    { definition = s; }
    
    public short getType()           { return type; }
    public String getName ()         { return name; }
    public Namespace getNamespace () { return namespace; }
    public String getDefinition ()   { return definition; }
    public String getURI () {
        if (null == name || null == namespace) return null;
        if (namespace.getNamespaceURI().endsWith("#")) return namespace.getNamespaceURI()+name;
        return namespace.getNamespaceURI()+"#"+name;
    }
    
    Component () {}
    
    Component (Model m) {
        super(m);
    }      
       
    @Override
    public int compareTo (Component o) {
        int v = this.namespace.getNamespaceURI().compareTo(o.namespace.getNamespaceURI());
        if (0 == v) {
            v = this.name.compareTo(o.name);
        }
        return v;
    }    
}
