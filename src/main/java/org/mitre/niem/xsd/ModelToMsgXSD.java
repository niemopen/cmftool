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
import static org.mitre.niem.NIEMConstants.DEFAULT_NIEM_VERSION;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.NamespaceKind;
import static org.mitre.niem.cmf.NamespaceKind.NSK_CORE;
import org.mitre.niem.cmf.Property;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class to generate a NIEM 6 message schema from a Model
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToMsgXSD extends ModelToXSD {
    
    public ModelToMsgXSD (Model m) { super(m); }
   
    // Create xs:complexContent and xs:extension elements for this xs:complexType
    // in a message schema.
    @Override
    protected Element handleComplexContentReferenceCode (Document dom, Element cte, Element sqe, ClassType ct) {

        // Create xs:extension and xs:complexContent if this class has a model parent.
        var attParent = cte;
        var basect  = ct.getExtensionOfClass();
        if (null != basect) {
            var cce = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexContent");
            var exe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
            exe.setAttribute("base", basect.getQName());
            attParent = exe;
            nsNSdeps.add(basect.getNamespace().getNamespaceURI());
            exe.appendChild(sqe);
            cce.appendChild(exe);
            cte.appendChild(cce);
        }
        // Otherwise there's no xs:extension; xs:complexType has xs:sequence, put attributes there
        else cte.appendChild(sqe);
        
        var rcode = ct.getReferenceCode();
        switch (rcode) {
        case "NONE":
            addAppinfoAttribute(dom, cte, "referenceCode", rcode);
            break;
        case "REF":
            addAttribute(dom, attParent, structPrefix + ":appliesToParent");
            addAttribute(dom, attParent, structPrefix + ":id");
            addAttribute(dom, attParent, structPrefix + ":ref");
            addAppinfoAttribute(dom, cte, "referenceCode", rcode);
            break;
        case "URI":
            addAttribute(dom, attParent, structPrefix + ":appliesToParent");
            addAttribute(dom, attParent, structPrefix + ":uri");
            addAppinfoAttribute(dom, cte, "referenceCode", rcode);
            break;
        case "ANY":
        default:
            addAttribute(dom, attParent, structPrefix + ":appliesToParent");
            addAttribute(dom, attParent, structPrefix + ":id");
            addAttribute(dom, attParent, structPrefix + ":ref");
            addAttribute(dom, attParent, structPrefix + ":uri");
            // refCode ANY is the default; don't add
            break;
        }
        return attParent;
    }
    
    // Add attribute group and reference code appinfo for a message schema.
    @Override
    protected void handleSimpleContentReferenceCode (Document dom, Element cte, Element exe, ClassType ct) { 
        var rcode = ct.getReferenceCode();
        switch (rcode) {
        case "ANY":
            addAttribute(dom, exe, structPrefix + ":id");
            addAttribute(dom, exe, structPrefix + ":ref");
            addAttribute(dom, exe, structPrefix + ":uri");
            addAppinfoAttribute(dom, cte, "referenceCode", rcode);
            break;
        case "REF":
            addAttribute(dom, exe, structPrefix + ":id");
            addAttribute(dom, exe, structPrefix + ":ref");
            addAppinfoAttribute(dom, cte, "referenceCode", rcode);
            break;
        case "URI":
            addAttribute(dom, exe, structPrefix + ":uri");
            addAppinfoAttribute(dom, cte, "referenceCode", rcode);
            break;
        case "NONE":
            break;
        // Default is ANY if there are model attributes; otherwise NONE
        default:
            if (ct.hasPropertyList().size() < 2) break;
            addAttribute(dom, exe, structPrefix + ":id");
            addAttribute(dom, exe, structPrefix + ":ref");
            addAttribute(dom, exe, structPrefix + ":uri");
            addAppinfoAttribute(dom, cte, "referenceCode", rcode);
            break;
        }       
    }
    
    protected void addAttribute (Document dom, Element attParent, String atqn) {
        var ate = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        ate.setAttribute("ref", atqn);
        attParent.appendChild(ate);
    }
    
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
            var rse = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:restriction");
            rse.setAttribute("base", stqn);
            cte.appendChild(rse);
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
    
    // Convert NIEM v3-5 ctargs to NIEM 6.
    // Convert NIEM 6 message schema to subset schema ctarg.
    @Override
    protected String fixConformanceTargets (String ctaStr) {
        if (null == ctaStr) return null;
        if (ctaStr.isBlank()) return "";
        var ctab = new StringBuilder();
        var ctas = ctaStr.split("\\s+");
        var n5pf = NamespaceKind.getCTPrefix("NIEM5");
        var n6pf = NamespaceKind.getCTPrefix("NIEM6");
        var sep  = "";
        for (String cta : ctas) {
            if (cta.startsWith(n5pf)) {
                var targ = NamespaceKind.cta2Target(cta);
                cta = n6pf + DEFAULT_NIEM_VERSION + "/" + targ;
            }
            cta = cta.replace("SubsetSchemaDocument", "MessageSchemaDocument");
            ctab.append(sep).append(cta);
            sep = " ";
        }
        ctaStr = ctab.toString();               
        return ctaStr;    
    }
    
    @Override
    protected String fixSchemaVersion (String nsuri) {
        var ns = m.getNamespaceByURI(nsuri);
        var kind = ns.getKind();
        var rv   = m.getNamespaceByURI(nsuri).getSchemaVersion();
        if (kind > NSK_CORE) return rv;
        if (null == rv) return "message";
        if (rv.startsWith("source")) rv = rv.substring(6);
        else if (rv.startsWith("subset"))rv = rv.substring(6);
        else if (rv.startsWith("message")) rv = rv.substring(7);
        return "message"+rv;
    }    
    
    @Override
    protected String getShareSuffix () { return "-msg"; }
    
    // In a message schema, object properties are nillable unless otherwise
    // specified. Data properties are never nillable -- if you make a property
    // referencable with appinfo:referenceCode, it becomes an object property.
    @Override
    protected boolean isPropertyNillable (Property p) {
        if (p.isAttribute()) return false;
        if (p.isAbstract())  return false;
        if (null == p.getClassType()) return false; // a data property
        return !"NONE".equals(p.getReferenceCode());
    }    

    // Don't convert "xs:foo" to "xs-proxy:foo" in message schema documents
    @Override
    protected String proxifiedDatatypeQName (Datatype dt) {
        return dt.getQName();
    }    
}
