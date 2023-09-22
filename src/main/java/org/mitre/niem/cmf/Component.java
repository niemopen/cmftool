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
 * An abstract class for NIEM model components that have a name and namespace.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public abstract class Component extends ObjectType implements Comparable<Component> {
    public static final short C_CLASSTYPE = 0;
    public static final short C_DATAPROPERTY = 1;
    public static final short C_DATATYPE = 2;
    public static final short C_OBJECTPROPERTY = 3;
    
    private Model model = null;             // Component objects know the Model they are part of 
    protected short type;                   // component type (ClassType, Property, etc.)
    private String name = null;             // local name
    private Namespace namespace = null;     // namespace object
    private String definition = null;       // xs:documentation string
    private boolean isAbstract = false;
    private boolean isDeprecated = false;
    
    public Component () { super(); }

    public Component (Namespace ns, String lname) {
        super();
        namespace = ns;
        name = lname;
    }
    
    @Override
    public boolean isModelChild ()          { return true; }    // Components are model children

    public void setName (String s) { 
        name = s;
        if (null != getModel()) getModel().componentChange(this);
    }
    
    public void setNamespace (Namespace ns) { 
        namespace = ns; 
        if (null != getModel()) getModel().componentChange(this);
    }
    
    void setModel (Model m)                 { model = m; }    
    public void setDefinition (String s)    { definition = (null == s ? null : s.strip().replaceAll("\\s+", " ")); }
    public void setIsAbstract(boolean f)    { isAbstract = f; }
    public void setIsDeprecated(boolean f)  { isDeprecated = f; }
    public void setIsAbstract (String s)    { isAbstract = "true".equals(s); }
    public void setIsDeprecated (String s)  { isDeprecated = "true".equals(s); }    
    
    public Model getModel ()                { return model; }
    public short getType()                  { return type; }
    public String getName ()                { return name; }
    public Namespace getNamespace ()        { return namespace; }
    public String getNamespaceURI ()        { return namespace.getNamespaceURI(); }
    public String getDefinition ()          { return definition; }
    public boolean isAbstract ()            { return isAbstract; }
    public boolean isDeprecated ()          { return isDeprecated; }    
    
    public String getQName () {
        return namespace.getNamespacePrefix()+":"+name;
    }
    
    public String getURI () {  
        return genURI(namespace.getNamespaceURI(), this.getName());
    }
    
    public ClassType asClassType () {
        return C_CLASSTYPE == type ? (ClassType)this : null; 
    }
    public Datatype asDatatype () {
        return C_DATATYPE == type ? (Datatype)this : null; 
    }
    public Property asProperty () {
        return C_OBJECTPROPERTY == type || C_DATAPROPERTY == type ? (Property)this : null; 
    }
    
    @Override
    public void addToModel (Model m) {
        m.addComponent(this);
    }
    
    // Model components are ordered by their QNames
    @Override
    public int compareTo (Component o) {
        int rv = this.namespace.getNamespacePrefix().compareToIgnoreCase(o.namespace.getNamespacePrefix());
        if (rv != 0) return rv;
        return this.name.compareTo(o.name);
    }    
    
    public static String genURI (String nsuri, String lname) {
        if (nsuri.endsWith("#")) return nsuri.concat(lname);
        return nsuri.concat("#").concat(lname);
    }
}
