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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.niem.xsd.NamespaceMap;

/**
 * A class for a Model object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Model extends CMFObject {    
    static final Logger LOG = LogManager.getLogger(Model.class);    
    
    public Model () { 
        super(); 
        nsmap = new NamespaceMap();
        var xsNS = new Namespace("xs", W3C_XML_SCHEMA_NS_URI);
        xsNS.setKindCode("XSD");
        try { addNamespace(xsNS); } catch (CMFException ex) { } // CAN'T HAPPEN
    }

    
    @Override
    public int getType () { return CMF_MODEL; }
    
    private final NamespaceMap nsmap;
    private final Set<Namespace> nsS                    = new HashSet<>();  // set of Namespace objects
    private final Map<String,Namespace> uri2ns          = new HashMap<>();  // uri -> Namespace
    private final Map<String,Component> compMap         = new HashMap<>();  // uri -> Component
    private final Map<String,ClassType> classMap        = new HashMap<>();  // uri -> ClassType
    private final Map<String,DataProperty> dpropMap     = new HashMap<>();  // uri -> DataProperty
    private final Map<String,Datatype> dtypeMap         = new HashMap<>();  // uri -> Datatype
    private final Map<String,ObjectProperty>opropMap    = new HashMap<>();  // uri -> ObjectProperty
    private final Map<String,Property> propMap          = new HashMap<>();  // uri -> Property
    private List<Component> ordComp      = null;
    private List<Namespace> ordNS        = null;
    
    public List<ClassType> classTypeL ()                { return new ArrayList<>(classMap.values()); }
    public List<Datatype> datatypeL ()                  { return new ArrayList<>(dtypeMap.values()); }
    public List<Property> propertyL ()                  { return new ArrayList<>(propMap.values()); }
    public List<DataProperty> dataPropertyL ()          { return new ArrayList<>(dpropMap.values()); }
    
    public Component qnToComponent (String qn)          { return compMap.get(qnToURI(qn)); }
    public ClassType qnToClassType (String qn)          { return classMap.get(qnToURI(qn)); }
    public DataProperty qnToDataProperty (String qn)    { return dpropMap.get(qnToURI(qn)); }
    public Datatype qnToDatatype (String qn)            { return dtypeMap.get(qnToURI(qn)); }
    public ObjectProperty qnToObjectProperty (String qn){ return opropMap.get(qnToURI(qn)); }
    public Property qnToProperty (String qn)            { return propMap.get(qnToURI(qn)); } 
    
    public Component uriToComponent (String uri)        { return compMap.get(uri); }
    public ClassType uriToClassType (String uri)        { return classMap.get(uri); }
    public DataProperty uriToDataProperty (String uri)  { return dpropMap.get(uri); }
    public Datatype uriToDatatype (String uri)          { return dtypeMap.get(uri); }
    public ObjectProperty uriToObjectProperty (String uri){ return opropMap.get(uri); }
    public Property uriToProperty (String uri)          { return propMap.get(uri); }
    
    public String nsUToNSprefix (String p)              { return nsmap.getURI(p); }
    public String prefixToNSuri (String u)              { return nsmap.getPrefix(u); }
    public Namespace nsUToNamespaceObj (String u)       { return uri2ns.get(u); }
    public Namespace prefixToNamespaceObj (String p) {
        var uri = nsmap.getURI(p);
        if (null == uri) return null;
        return uri2ns.get(uri);
    }
    public Namespace namespaceObj (String preOrURI) {
        if (preOrURI.contains(":")) return nsUToNamespaceObj(preOrURI);
        return prefixToNamespaceObj(preOrURI);
    }
    public Set<Namespace> namespaceSet ()               { return nsS; }
    
    public String qnToURI (String qname) {
        if (null == qname || qname.isEmpty()) return "";
        var ci = qname.indexOf(':');
        if (ci < 1) return "";
        var p = qname.substring(0, ci);
        var ln = qname.substring(ci+1);
        var ns = prefixToNamespaceObj(p);
        if (null == ns || ln.isBlank()) return "";
        var uri = ns.uri();
        if (uri.endsWith("/")) return uri + ln;
        return uri + "/" + ln;       
    }
    
    public String uriToQN (String uri) {
        if (null == uri || uri.isEmpty()) return "";
        int si = uri.lastIndexOf('/');
        if (si < 2) return "";
        var base = uri.substring(0, si+1);
        var ln   = uri.substring(si+1);
        var pre  = nsmap.getPrefix(base);
        if (pre.isEmpty()) return "";
        return pre + ":" + ln;
    }
   
    /**
     * Returns the name portion of a component URI; e.g. "TextType" for
     * https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/TextType.
     * @param componentURI - NIEM component URI
     * @return component name
     */
    public String compUToName (String componentURI) {
        int indx = componentURI.lastIndexOf("/");
        if (indx < 0 || indx + 1 >= componentURI.length()) return "";
        return componentURI.substring(indx+1);
    }
    
    /**
     * Returns the namespace URI portion of a component URI; e.g. 
     * https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/ for
     * https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/TextType.
     * 
     * The URI for a NIEM namespace SHOULD end in a "/" character.  But it might
     * not.  The Model object can handle this... unless you have two namespaces
     * with URIs that vary only in the final "/" character... in which case, you
     * are beyond my help. :-)
     * @param componentURI - NIEM component URI
     * @return component namespace URI
     */
    public String compUToNamespaceU (String componentURI) {
        int indx = componentURI.lastIndexOf("/");
        if (indx < 0 || indx + 1 >= componentURI.length()) return "";
        var compNSuri = componentURI.substring(0, indx+1);
        if (uri2ns.containsKey(compNSuri)) return compNSuri;
        compNSuri = compNSuri.substring(0, compNSuri.length()-1);
        if (uri2ns.containsKey(compNSuri)) return compNSuri;
        LOG.error("no namespace object matching component URI {}", componentURI);
        return "";
    }
    
    /**
     * Returns the namespace object for a component URI.
     * @param componentURI - NIEM component URI
     * @return namespace object
     */
    public Namespace compUToNamespaceObj (String componentURI) {
        var nsuri = compUToNamespaceU(componentURI);
        return namespaceObj(nsuri);
    }
   
    public void addNamespace (Namespace n) throws CMFException {
        if (null == n) return;
        if (uri2ns.containsKey(n.uri())) return;
        var cnsuri = nsmap.getPrefix(n.prefix());
        if (null != cnsuri && !n.uri().equals(cnsuri)) {
            throw new CMFException(String.format(
                "Can't add namespace %s=%s (prefix already assigned to %s)",
                n.prefix(), n.uri(), cnsuri));
        }
        nsmap.assignPrefix(n.prefix(), n.uri());
        nsS.add(n);
        uri2ns.put(n.uri(), n);
        ordNS = null;
        n.setModel(this);
    }
    
    
    public void addClassType (ClassType c) {
        if (null == c) return;
        compMap.put(c.uri(), c); 
        classMap.put(c.uri(), c);
        ordComp = null;
        c.setModel(this);
    }
    
    public void addDataProperty (DataProperty c) {
        if (null == c) return;
        compMap.put(c.uri(), c); 
        propMap.put(c.uri(), c);
        dpropMap.put(c.uri(), c);
        ordComp = null;       
        c.setModel(this);
    }
    
    public void addDatatype (Datatype c) {
        if (null == c) return;
        compMap.put(c.uri(), c); 
        dtypeMap.put(c.uri(), c);
        ordComp = null;
        c.setModel(this);
    }
    
    public void addObjectProperty (ObjectProperty c) {
        if (null == c) return;
        compMap.put(c.uri(), c); 
        propMap.put(c.uri(), c);
        opropMap.put(c.uri(), c);
        ordComp = null;        
        c.setModel(this);
    }
    
    public void addProperty (Property p) {
        if (p.isDataProperty()) addDataProperty((DataProperty) p);
        else addObjectProperty((ObjectProperty)p);
    }
    
    public void removeClassType (ClassType ct) {
        if (null == ct) return;
        compMap.remove(ct.uri());
        classMap.remove(ct.uri());
        ordComp = null;
    }
    
    public void removeDatatype (String dtU) {
        compMap.remove(dtU);
        dtypeMap.remove(dtU);
        ordComp = null;
    }
    
    public void removeObjectProperty (ObjectProperty c) {
        if (null == c) return;
        compMap.remove(c.uri());
        propMap.remove(c.uri());
        opropMap.remove(c.uri());
        ordComp = null;
    }
  
    public void componentUpdate () {
        ordComp = null;
    }
    
    public List<Namespace> namespaceList () {
        if (null != ordNS) return ordNS;
        ordNS = new ArrayList<>(nsS);
        Collections.sort(ordNS);
        return ordNS;
    }
    
    public List<Component> componentList () {
        if (null != ordComp) return ordComp;
        ordComp = new ArrayList<>();
        for (var c : compMap.values()) {
           var cname = c.name();
           if (!c.isOutsideRef()) ordComp.add(c);
        }
        Collections.sort(ordComp);
        return ordComp;
    }


    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        return child.addToModel(eln, loc, this);
    }    
}
