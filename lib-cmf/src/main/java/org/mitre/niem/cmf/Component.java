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
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.niem.utility.NaturalOrderComparator;
import org.mitre.niem.xml.LanguageString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An abstract class for a Component object in a CMF model.  
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public abstract class Component extends CMFObject implements Comparable<Component> {
    static final Logger LOG = LogManager.getLogger(Component.class);
    
    public Component () { super(); } 
    public Component (String outsideURI) { super(); this.outsideURI = outsideURI; }
    public Component (Namespace ns, String name) { super(); this.namespace = ns; this.name = name; }
    
    private Model model = null;                 // component belongs to this model
    private String outsideURI = "";             // absolute URI of component in another CMF model
    private Namespace namespace = null;         // cmf:Namespace
    private String name = "";                   // cmf:Name
    private boolean isDeprecated = false;       // cmf:DeprecatedIndicator
    private final List<LanguageString> docL = new ArrayList<>();  // cmf:DocumentationText
    
    public Model model ()                       { return model; }
    public Namespace namespace ()               { return namespace; }
    public String namespaceURI ()               { return namespace.uri(); }
    public String name ()                       { return name; }
    public String outsideURI ()                 { return outsideURI; }
    public CodeListBinding codeListBinding ()   { return null; }
    public String referenceCode ()              { return ""; }
    public boolean isAbstract ()                { return false; }
    public boolean isDeprecated ()              { return isDeprecated; }
    public boolean isOrdered ()                 { return false; }
    public boolean isOutsideRef ()              { return !outsideURI.isEmpty() && null == namespace; }
    public boolean isModelComponent ()          { return namespace().isModelNS(); }
    
    public List<LanguageString> docL ()         { return docL; }
    public String definition ()                 { return docL.isEmpty() ? null : docL.get(0).text(); }
    
    public void setModel (Model m)              { model = m; }
    public void setNamespace (Namespace ns)     { namespace = ns; change(); }
    public void setName (String n)              { name = n; change(); }
    public void setOutsideURI (String u)        { outsideURI = u; }
    public void setIsDeprecated (boolean f)     { isDeprecated = f; }
    
    public void addDocumentation (String doc, String lang) {
        docL.add(new LanguageString(doc, lang));
    }
    public void setDocumentation (List<LanguageString> dL) {
        docL.clear();
        docL.addAll(dL);
    }
    
    private static Set<String> refCodes = Set.of("", "ANY", "ANYURI", "INTERNAL", "RELURI", "IDREF", "NONE");
    public void setReferenceCode (String code) {
        if (!refCodes.contains(code))
            LOG.warn("Invalid reference code {} for component {}", code, name);
    }
    
    /**
     * Returns the qualified name of this component; e.g. "nc:TextType".
     */
    public String qname () {
        if (null == namespace)
            if (null == name) return "";
            else return name;
        return namespace.prefix() + ":" + name;
    }
    
    /**
     * Returns the absolute URI of this component; e.g.
     * https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/TextType.
     * For components defined in this model, the URI is composed of namespace URI
     * plus a slash if needed, plus local name.  For outside components defined 
     * elsewhere, the URI was provided when this placeholder object was created.
     * @return component URI
     */
    @Override
    public String uri () {
        if (null == namespace && !outsideURI.isEmpty()) return outsideURI;
        if (null == namespace || name.isEmpty()) return "";
        var nsuri = namespace.uri();
        if (nsuri.endsWith("/")) return nsuri + name;
        return nsuri + "/" + name;
    }    
    
    /**
     * Returns the @structures:id value for this compnent; eg. "nc.TextType"
     */
    public String idRef () {
        if (null ==  namespace ||  null == name) return "";
        return namespace.prefix() + "." + name;
    }

    /**
     * Returns the prefix portion of a QName
     * @param qn
     * @return 
     */
    public static String qnToPrefix (String qn) {
        var indx = qn.indexOf(":");
        if (indx < 1 || indx >= qn.length()-1) return "";
        return qn.substring(0, indx);        
    }
    
    /**
     * Returns the local name portion of a QName
     * @param qn
     * @return 
     */
    public static String qnToName (String qn) {
        var indx = qn.indexOf(":");
        if (indx < 1 || indx >= qn.length()-1) return "";        
        return qn.substring(indx+1);
    }    
    
    /**
     * Returns the namespace portion of a component URI; for example, 
     * returns "http://someNS/" for http://someNS/FooType.
     * @param uri - component URI
     * @return - component name
     */    
    public static String uriToNamespace (String uri) {
        int indx = uri.lastIndexOf("/");
        if (indx < 0 || indx >= uri.length()) return "";
        return uri.substring(0, indx+1);        
    }
    
    /**
     * Returns the name portion of a component URI; for example, returns "FooType"
     * for http://someNS/FooType.
     * @param uri - component URI
     * @return - component name
     */
    public static String uriToName (String uri) {
        int indx = uri.lastIndexOf("/");
        if (indx < 0 || indx >= uri.length()) return "";
        return uri.substring(indx+1);
    }
    
    public static String  makeQN (String prefix, String name) {
        return prefix + ":" + name;
    }
    
    public static String makeURI (String nsuri, String name) {
        if (nsuri.endsWith("/")) return nsuri + name;
        else return nsuri + "/" + name;
    }
  
    // Notify the model object that the name or namespace of one of 
    // its components has changed.
    private void change () {
        if (null != model) model.componentUpdate();
    }
    
    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        return child.addToComponent(eln, loc, this);
    }
    
    // Dispatch to Component's proper ModelXMLWriter method
    public void addComponentCMFChildren (ModelXMLWriter w, Document doc, Element c, Set<Namespace>nsS)  { }

    @Override
    public int compareTo(Component o) {
        int rv = this.namespace().compareTo(o.namespace());
        if (rv != 0) return rv;
        var thisName = StringUtils.lowerCase(name);
        var oName    = StringUtils.lowerCase(o.name());
        return NaturalOrderComparator.comp(thisName, oName);
    }
        
}
