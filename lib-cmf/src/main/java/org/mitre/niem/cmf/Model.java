/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2025 The MITRE Corporation.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mitre.niem.xsd.NamespaceMap;

/**
 * A class for a Model object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Model extends CMFObject {    
    
    public Model () { super(); nsmap = new NamespaceMap(); }
//    public Model (NamespaceMap nsmap) { super(); this.nsmap = nsmap; }
    
    @Override
    public int getType () { return CMF_MODEL; }
    
    private final NamespaceMap nsmap;
    private final Map<String,Namespace> uri2ns          = new HashMap<>();  // uri -> Namespace
    private final Map<String,Component> compMap         = new HashMap<>();  // uri -> Component
    private final Map<String,ClassType> classMap        = new HashMap<>();  // uri -> ClassType
    private final Map<String,DataProperty> dpropMap     = new HashMap<>();  // uri -> DataProperty
    private final Map<String,Datatype> dtypeMap         = new HashMap<>();  // uri -> Datatype
    private final Map<String,ObjectProperty>opropMap    = new HashMap<>();  // uri -> ObjectProperty
    private final Map<String,Property> propMap          = new HashMap<>();  // uri -> Property
    private List<Component> ordComp      = null;
    private List<Namespace> ordNS        = null;
    
    public Component qn2component (String qn)           { return compMap.get(qn2uri(qn)); }
    public ClassType qn2classType (String qn)           { return classMap.get(qn2uri(qn)); }
    public DataProperty qn2dataProperty (String qn)     { return dpropMap.get(qn2uri(qn)); }
    public Datatype qn2datatype (String qn)             { return dtypeMap.get(qn2uri(qn)); }
    public ObjectProperty qn2objectProperty (String qn) { return opropMap.get(qn2uri(qn)); }
    public Property qn2property (String qn)             { return propMap.get(qn2uri(qn)); } 
    
    public Component uri2component (String uri)         { return compMap.get(uri); }
    public ClassType uri2classType (String uri)         { return classMap.get(uri); }
    public DataProperty uri2dataProperty (String uri)   { return dpropMap.get(uri); }
    public Datatype uri2datatype (String uri)           { return dtypeMap.get(uri); }
    public ObjectProperty uri2objectProperty (String uri){ return opropMap.get(uri); }
    public Property uri2property (String uri)           { return propMap.get(uri); }
    
    public String uri2prefix (String p)                 { return nsmap.getURI(p); }
    public String prefix2uri (String u)                 { return nsmap.getPrefix(u); }
    public Namespace uri2namespace (String u)           { return uri2ns.get(u); }
    public Namespace prefix2namespace (String p) {
        var uri = nsmap.getURI(p);
        if (null == uri) return null;
        return uri2ns.get(uri);
    }
    public Namespace namespace (String preOrURI) {
        if (preOrURI.contains(":")) return uri2namespace(preOrURI);
        return prefix2namespace(preOrURI);
    }
    
    public String qn2uri (String qname) {
        if (null == qname || qname.isEmpty()) return "";
        var ci = qname.indexOf(':');
        if (ci < 1) return "";
        var p = qname.substring(0, ci);
        var ln = qname.substring(ci+1);
        var ns = prefix2namespace(p);
        if (null == ns || ln.isBlank()) return "";
        var uri = ns.uri();
        if (uri.endsWith("/")) return uri + ln;
        return uri + "/" + ln;       
    }
    
    public String uri2qn (String uri) {
        if (null == uri || uri.isEmpty()) return "";
        int si = uri.lastIndexOf('/');
        if (si < 2) return "";
        var base = uri.substring(0, si);
        var ln   = uri.substring(si+1);
        var pre  = nsmap.getPrefix(base);
        if (pre.isEmpty()) return "";
        return pre + ":" + ln;
    }

    public void addNamespace (Namespace n) throws CMFException {
        if (uri2ns.containsKey(n.uri())) return;
        var cnsuri = nsmap.getPrefix(n.prefix());
        if (null != cnsuri && !n.uri().equals(cnsuri)) {
            throw new CMFException(String.format(
                "Can't add namespace %s=%s (prefix already assigned to %s)",
                n.prefix(), n.uri(), cnsuri));
        }
        nsmap.assignPrefix(n.prefix(), n.uri());
        uri2ns.put(n.uri(), n);
        ordNS = null;
    }
    
    
    public void addClassType (ClassType c) {
        compMap.put(c.uri(), c); 
        classMap.put(c.uri(), c);
        ordComp = null;
    }
    
    public void addDataProperty (DataProperty c) {
        compMap.put(c.uri(), c); 
        propMap.put(c.uri(), c);
        dpropMap.put(c.uri(), c);
        ordComp = null;        
    }
    
    public void addDatatype (Datatype c) {
        compMap.put(c.uri(), c); 
        dtypeMap.put(c.uri(), c);
        ordComp = null;
    }
    
    public void addObjectProperty (ObjectProperty c) {
        compMap.put(c.uri(), c); 
        propMap.put(c.uri(), c);
        opropMap.put(c.uri(), c);
        ordComp = null;        
    }
  
    public void componentUpdate () {
        ordComp = null;
    }


    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        return child.addToModel(eln, loc, this);
    }    
}
