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

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Namespace extends ObjectType implements Comparable<Namespace> {
    private String namespaceURI = null;
    private String namespacePrefix = null;
    private String definition = null;
    
    @Override
    public boolean isModelChild ()            { return true; }      // Namespace objects are model children
    
    public void setNamespaceURI (String s)    { namespaceURI = s; }
    public void setNamespacePrefix (String s) { namespacePrefix = s; }
    public void setDefinition (String s)      { definition = s; }
    
    public String getNamespaceURI ()          { return namespaceURI; }
    public String getNamespacePrefix ()       { return namespacePrefix; }
    public String getDefinition ()            { return definition; }
    
    public Namespace () { }
    
    // A constructor for Namespace objects not part of a model (structures, etc.)
    public Namespace (String p, String nsuri) {
        namespacePrefix = p ;
        namespaceURI = nsuri;
    }
    
    public Namespace (Model m) {
        super(m);
    }
    
    @Override
    public void addToModelSet (Model m) {
        m.addNamespace(this);
    }   
        
    /**
     * Returns a munged namespace prefix that does not exist in the supplied prefix map
     * For example, "nc" might turn into "nc_1" 
     * @param pm map claimed prefix -> ??
     * @param op desired prefix
     * @return munged prefix not in prefix map
     */
    public static String mungedPrefix (Map<String,String>pm, String op) {
        if (!pm.containsKey(op)) return op;
        int count = 0;
        String pat = op.replaceFirst("_\\d+$", "");    // remove existing suffix if any
        pat = pat + "_%d";
        while (pm.containsKey(op)) {
            op = String.format(pat, ++count);
        }
        return op;
    }
    
    @Override
    public int compareTo (Namespace o) {
        return this.namespacePrefix.compareTo(o.namespacePrefix);
    }    
}
