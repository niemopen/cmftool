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
import static org.mitre.niem.xml.XMLSchemaDocument.makeQN;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToName;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToPrefix;
import org.mitre.niem.xsd.NamespaceKind;
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
    
    public String nsUToPrefix (String p)                { return nsmap.getURI(p); }
    public String prefixToNSU (String u)                { return nsmap.getPrefix(u); }
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
    
    
    /**
     * Constructs a component URI from a namespace URI and local name.
     * @param nsU
     * @param lname
     * @return 
     */
    public static String makeURI (String nsU, String lname) {
        if (nsU.startsWith("urn:")) return nsU + ":" + lname;   // urn:some:NS:lname
        if (nsU.endsWith("/"))      return nsU + lname;         // http://someNS/lname
        return nsU + "/" + lname;
    }
    
    /**
     * Returns the component URI corresponding to a QName.
     * @param qname
     * @return 
     */
    public String qnToURI (String qname) {
        var pre = qnToPrefix(qname);
        var ln  = qnToName(qname);
        var nsU = nsmap.getURI(pre);
        if (null == nsU || nsU.isEmpty()) return "";
        return makeURI(nsU, ln);      
    }
    
    /**
     * Returns the QName corresponding to a component URI.
     * Returns the empty string if the component's namespace is not in the model.
     * @param uri
     * @return 
     */
    public String uriToQN (String uri) {
        if (null == uri || uri.isEmpty()) return "";
        var nsU = uriToNSU(uri);
        var ln  = uriToName(uri);
        var pre = nsmap.getPrefix(nsU);
        if (null == pre || pre.isEmpty()) return "";
        return makeQN(pre, ln);
    }
    
    /**
     * Returns the namespace object given a component URI in the model.
     * @param uri - component URI string
     * @return - namespace object
     */    
    public Namespace uriToNSObj (String uri) {
        var nsU = uriToNSU(uri);
        var ns  = namespaceObj(nsU);
        return ns;
    }
     
    /**
     * Returns the URI of a namespace in the model, given a component URI.
     * Returns "http://someNS/" for http://someNS/FooType.
     * Returns "urn:some:NS" for "urn:some:NS:LocalName".
     * Returns builtin namespace URI for builtin namespace components.
     * Handles namespace URIs that do not end in a slash.
     * 
     * The URI for a NIEM namespace SHOULD end in a "/" character.  But it might
     * not.  The Model object can handle this... unless you have two namespaces
     * with URIs that vary only in the final "/" character... in which case, you
     * are beyond my help. :-)
     * @param uri - component URI
     * @return - component name
     */    
    public String uriToNSU (String uri) {
        int indx = 0;
        if (uri.startsWith("urn:")) indx = uri.lastIndexOf(":");
        else indx = uri.lastIndexOf("/");
        if (indx < 0 || indx >= uri.length()) {
            LOG.error("bad component URI {}", uri);
            return "";
        }
        var nsU  = uri.substring(0, indx+1);
        var nsU2 = nsU.substring(0, nsU.length()-1);
        if (nsmap.hasURI(nsU)) return nsU;              // http://someNS/
        if (nsmap.hasURI(nsU2)) return nsU2;            // http://someNS
        if (NamespaceKind.isBuiltin(nsU)) return nsU;   // https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/
        LOG.error("no namespace in model for component URI {}", uri);
        return "";
    }
    
    /**
     * Returns the name portion of a component URI.
     * Returns "FooType" for http://someNS/FooType.
     * Returns "LocalName" for "urn:some:NS:LocalName".
     * @param uri - component URI
     * @return - component name
     */
    public static String uriToName (String uri) {
        int indx = 0;
        if (uri.startsWith("urn:")) indx = uri.lastIndexOf(":");
        else indx = uri.lastIndexOf("/");
        if (indx < 0 || indx >= uri.length()) return "";
        return uri.substring(indx+1);
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
