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
    
    // The nine kinds of namespace.  Order is signficant, because it controls
    // the order of namespace prefix assignment: extension, niem-model, builtin,
    // XSD, XML, external, unknown.  
    public final static int NSK_EXTENSION  = 0;     // has conformance assertion, not in NIEM model
    public final static int NSK_DOMAIN     = 1;     // domain schema
    public final static int NSK_CORE       = 2;     // niem core schema
    public final static int NSK_OTHERNIEM  = 3;     // other niem; starts with release or publication prefix
    public final static int NSK_BUILTIN    = 4;     // appinfo, code-lists, conformance, proxy, structures
    public final static int NSK_XSD        = 5;     // namespace for XSD datatypes
    public final static int NSK_XML        = 6;     // namespace for xml: attributes
    public final static int NSK_EXTERNAL   = 7;     // imported with appinfo:externalImportIndicator
    public final static int NSK_UNKNOWN    = 8;     // no conformance assertion
    public final static int NSK_NUMKINDS   = 9;     // this many kinds of schemas  
    
    private final static String[] namespaceKind = { "EXTENSION", "DOMAIN", "CORE", "OTHERNIEM", "BUILTIN", "XSD", "XML", "EXTERNAL", "UNKNOWN" };
    private final static boolean[] nskInModel   = { true,        true,     true,   true,        false,     true,  true,  true,       false};
    
    public static String kindCode (int kind)      { return kind < 0 || kind > NSK_NUMKINDS ? "UNKNOWN" : namespaceKind[kind]; }
    public static boolean kindInModel (int kind)  { return kind < 0 || kind > NSK_NUMKINDS ? false : nskInModel[kind]; }
    
    
    private Model model = null;             // Namespace objects know the Model they are part of     
    private String namespaceURI = null;
    private String namespacePrefix = null;
    private String definition = null;
    private int nsKind = NSK_UNKNOWN;
    
    public Namespace () { super(); }
     
    public Namespace (String p, String nsuri) {
        super();
        namespacePrefix = p ;
        namespaceURI = nsuri;
    }
    
    @Override
    public boolean isModelChild ()            { return true; }      // Namespace objects are model children
    
    public void setNamespaceURI (String s) throws CMFException { 
        namespaceURI = s;
        if (null != getModel()) getModel().childChanged(this, namespacePrefix);
    }
    
    public void setNamespacePrefix (String s) throws CMFException { 
        String oldPrefix = namespacePrefix;
        namespacePrefix = s;
        if (null != getModel()) {
            getModel().childChanged(this, oldPrefix);
        }
    }
    
    void setModel (Model m)                   { model = m; }
    public void setDefinition (String s)      { definition = s; }
    public void setKind (int k)               { nsKind = k; }
    public void setKind (String c) {
        nsKind = 0;
        while (nsKind < NSK_NUMKINDS && !c.equals(namespaceKind[nsKind])) nsKind++;
    }
    
    public Model getModel ()                  { return model; }
    public String getNamespaceURI ()          { return namespaceURI; }
    public String getNamespacePrefix ()       { return namespacePrefix; }
    public String getDefinition ()            { return definition; }
    public int getKind ()                     { return nsKind; }
    public String getKindCode ()              { return kindCode(nsKind); }
    public boolean isExternal ()              { return nsKind == NSK_EXTERNAL; }

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
