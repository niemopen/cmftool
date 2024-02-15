/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2023 The MITRE Corporation.
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

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Model;
import static org.mitre.niem.cmf.NamespaceKind.NSK_CORE;
import org.w3c.dom.Document;

/**
 * A class to generate a NIEM 6 message schema from a Model
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToMsgXSD extends ModelToXSD {
    
    public ModelToMsgXSD () { super(); }
    public ModelToMsgXSD (Model m) { super(m); }
    
    // For a message schema, we create a simple type declaration from a Datatype object (FooType)
    @Override
    protected void createTypeFromDatatype (Document dom, String nsuri, Datatype dt) {
        if (null == dt) return;
        var cname = dt.getName().replaceFirst("Datatype$", "Type");     // FooDatatype -> FooType
        if (nsTypedefs.containsKey(cname)) return;                      // already created xs:ComplexType for this
        if (!nsuri.equals(dt.getNamespaceURI())) return;                // datatype is not in this namespace
        if (W3C_XML_SCHEMA_NS_URI.equals(dt.getNamespaceURI())) return; // don't create XSD builtins        
        
        var cte = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleType");
        cte.setAttribute("name", cname);        
        var ae = addDocumentation(dom, cte, null, dt.getDocumentation());
        if (dt.isDeprecated()) addAppinfoAttribute(dom, cte, "deprecated", "true");
        
        if (null != dt.getCodeListBinding()) {
            ae = addAnnotation(dom, cte, ae);
            var ap = addAppinfo(dom, ae, null);
            var cb = addCodeListBinding(dom, ap, nsuri, dt.getCodeListBinding());
        }        
        if (needSimpleType.contains(dt)) {
            var stqn = dt.getQName().replaceFirst("Type$", "SimpleType");
            addEmptyExtensionElement(dom, cte, stqn);
        }
        else {
            var r     = dt.getRestrictionOf();
            var rbdt  = r.getDatatype();
            var rbdqn = proxifiedDatatypeQName(rbdt);
            var rfl   = r.getFacetList();
            addRestrictionElement(dom, cte, dt, r.getDatatype(), rbdqn);
        }
        nsTypedefs.put(cname, cte);
    }    

    @Override
    protected String getArchitecture ()       { return "NIEM6"; }   
    
    @Override
    protected String getConformanceTargets (String nsuri) {
        var rv = m.conformanceTargets(nsuri);
        rv = rv.replaceAll("ReferenceSchemaDocument", "MessageSchemaDocument");
        rv = rv.replaceAll("ExtensionSchemaDocument", "MessageSchemaDocument");
        rv = rv.replaceAll("SubsetSchemaDocument", "MessageSchemaDocument");               
        return rv;
    }    
    
    @Override
    protected String getSchemaVersion (String nsuri) {
        var ns = m.getNamespaceByURI(nsuri);
        var kind = ns.getKind();
        var rv   = m.schemaVersion(nsuri);
        if (kind > NSK_CORE) return rv;
        if (null == rv) return "message";
        if (rv.startsWith("source")) rv = rv.substring(6);
        else if (rv.startsWith("subset"))rv = rv.substring(6);
        else if (rv.startsWith("message")) rv = rv.substring(7);
        return "message"+rv;
    }    
    
    @Override
    protected String getShareSuffix () { return "-msg"; }
    
    // Don't convert "xs:foo" to "xs-proxy:foo" in message schema documents
    @Override
    protected String proxifiedDatatypeQName (Datatype dt) {
        return dt.getQName();
    }    
}
