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

import java.util.ArrayList;
import java.util.List;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.mitre.niem.NIEMConstants.DEFAULT_NIEM_VERSION;
import static org.mitre.niem.cmf.AugmentRecord.AUG_ASSOC;
import static org.mitre.niem.cmf.AugmentRecord.AUG_OBJECT;
import static org.mitre.niem.cmf.AugmentRecord.AUG_SIMPLE;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.PropertyAssociation;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
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
    
    // Don't create declaration of abstract elements; not needed in message schema.
    @Override
    protected void createDeclaration(Document dom, String nsuri, Property p) {
        if (null == p) return;
        if (p.isAbstract()) return;
        super.createDeclaration(dom, nsuri, p);
    }    

    // Don't write @substitutionGroup in a message schema.
    @Override
    protected void handleSubproperty (Property p, Element pe) { }

    // Create xs:complexContent and xs:extension elements for this xs:complexType
    // in a message schema.
    @Override
    protected void createComplexTypeFromClass (Document dom, String nsuri, ClassType ct) { 
        if (null == ct) return;
        var cname = ct.getName();
        if (nsTypedefs.containsKey(cname)) return;              // already created
        if (!nsuri.equals(ct.getNamespaceURI())) return;        // different namespace
        var cte = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");

        cte.setAttribute("name", cname);
        var ae = addDocumentation(dom, cte, null, ct.getDocumentation());
        if (ct.isAbstract())   cte.setAttribute("abstract", "true");
        if (ct.isDeprecated()) addAppinfoAttribute(dom, cte, "deprecated", "true");
        nsTypedefs.put(cname, cte);

        // Create list of class inheritances, deepest class first
        var ctList = new ArrayList<ClassType>();
        var ctNext = ct;
        var ctname = ct.getQName();
        do {
            ctList.add(0, ctNext);
            ctNext = ctNext.subClassOf();
        } while (null != ctNext);
        
        // Look at the deepest type; does it have a FooLiteral property?
        // If it does, then we have complexType -> simpleContent -> extension
        // All other properties are attributes and belong to xs:extension.
        Element aParent = null;
        ClassType ct0   = ctList.get(0);
        var ct0name = ct0.getQName();
        if (litTypes.contains(ct0)) {
            var hp = ct0.propertyList().get(0);
            var p  = hp.getProperty();                  // FooLiteral property
            var lpt   = p.getDatatype();                // datatype for FooLiteral
            var lptqn = lpt.getQName();                 // datatype QName (not proxified)
            if (needSimpleType.contains(lpt)) {
                lptqn = lptqn.replaceFirst("Type$", "SimpleType");
                lptqn = lptqn.replaceFirst("Datatype$", "SimpleType");
            }
            litProps.add(p);                    // don't need FooLiteral property
            var sce = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
            aParent  = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
            aParent.setAttribute("base", lptqn);
            sce.appendChild(aParent);           // xs:simpleContent has xs:extension
            cte.appendChild(sce);               // xs:complexType has xs:simpleContent            
        }
        // Otherwise we have complexType -> sequence.
        // Add element references, depth first; add attribute refs later
        else {
            var sqe = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:sequence");
            cte.appendChild(sqe);               // xs:complexType has xs:sequence
            aParent = cte;                      // attribute refs belong to xs:complexType
            if (hasGEAug && !cname.endsWith("AugmentationType")) {
                var which = cname.endsWith("AssociationType")  ? "Association" : "Object";                
                var sApQN = structPrefix + ":" + which + "AugmentationPoint";
                var sAp   = m.getProperty(sApQN);
                if (null != sAp) addOptionalSubstitutingElements(dom, sqe, sAp);
            }
            for (var nextCT : ctList) {
                for (var hp : nextCT.propertyList()) {
                    if (!hp.augmentingNS().isEmpty()) continue; // will be child of FooAugmentation
                    var p    = hp.getProperty();
                    var pqn  = p.getQName();
                    var subs = substituteMap.get(p);
                    if (null != subs) addSubstitutingElements(dom, sqe, hp);
                    else if (!p.isAbstract()) addElementRef(dom, sqe, hp);
                }
            }
        }
        // Now add model attribute references to the appropriate parent element.
        // Works the same for simple and complex content (because of aParent).
        var attList = new ArrayList<AttProp>();
        handleAttributeProperties(attList, ct);
        addAttributeElements(dom, aParent, attList);   
        
        // Augmentation types don't have refs or referenceCode.
        // Add structures @id, @ref, @uri based on reference code to other types.
        if (!ct.getName().endsWith("AugmentationType")) {
            var rc = ct.getReferenceCode();
            if (null == rc) rc = "";            // not specified in CMF, use default
            switch (rc) {
            case "NONE":
                break;
            case "REF":
                addAttribute(dom, aParent, structPrefix + ":appliesToParent");
                addAttribute(dom, aParent, structPrefix + ":id");
                addAttribute(dom, aParent, structPrefix + ":ref");
                break;
            case "URI":
                addAttribute(dom, aParent, structPrefix + ":appliesToParent");
                addAttribute(dom, aParent, structPrefix + ":uri");
                break;
            case "ANY":
            default:
                addAttribute(dom, aParent, structPrefix + ":appliesToParent");
                addAttribute(dom, aParent, structPrefix + ":id");
                addAttribute(dom, aParent, structPrefix + ":ref");
                addAttribute(dom, aParent, structPrefix + ":uri");
                break;
            }        
            // Set appinfo:referenceCode if needed
            switch (rc) {
            case "NONE": addAppinfoAttribute(dom, cte, "referenceCode", "NONE"); break;
            case "REF":  addAppinfoAttribute(dom, cte, "referenceCode", "REF"); break;
            case "URI":  addAppinfoAttribute(dom, cte, "referenceCode", "URI"); break;
            case "ANY": 
            default:     // ANY is the default for complex content in a message schema
                break;
            }
        }       
    }
   
    // A depth-first recursion through class inheritance to add all the
    // attribute properties to the complex type.
    protected void handleAttributeProperties (List<AttProp> alist, ClassType ct) {
        var parent = ct.subClassOf();
        if (null != parent) handleAttributeProperties(alist, parent);
        buildAttributeList(alist, ct);
        
        // Now do the global attribute augmentations
        var ctn = ct.getName();                     // FooAssociationType or FooType
        for (var ar : gAttAugs) {
            var ga = ar.getGlobalAugKind();
            if (AUG_ASSOC == ga  && !ctn.endsWith("AssociationType")) continue;
            if (AUG_OBJECT == ga && ctn.endsWith("AssociatinType")) continue;
            if (AUG_SIMPLE == ga) continue;
            var ap    = ar.getProperty();
            var isreq = ar.minOccurs() > 0;
            var arec = new AttProp(ap, isreq);
            alist.add(arec);            
        }
    }
    
    protected void addOptionalSubstitutingElements (Document dom, Element sqe, Property p) {
        var hp = new PropertyAssociation();
        hp.setProperty(p);
        hp.setMaxUnbounded(true);
        hp.setMinOccurs(0);
        addSubstitutingElements(dom, sqe, hp);
    }
    
    protected void addSubstitutingElements (Document dom, Element sqe, PropertyAssociation hp) {
        var prop = hp.getProperty();
        var subs = substituteMap.get(prop);
        if (prop.isAbstract() && null == subs) return;      // omit abstract element with no subs
        if (prop.isAbstract() && 1 == subs.size()) {        // replace abstract element with 1 sub
            for (var sp : subs) addElementRef(dom, sqe, hp, sp);
        }
        // Not abstract, or more than 1 sub? Create a choice
        else {
            var che = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:choice");
            if (1 != hp.minOccurs()) che.setAttribute("minOccurs", "" + hp.minOccurs());
            if (hp.maxUnbounded())   che.setAttribute("maxOccurs", "unbounded");
            else if (1 != hp.maxOccurs()) che.setAttribute("maxOccurs", "" + hp.maxOccurs());
            sqe.appendChild(che);
            addToChoice(dom, che, prop);
        }
    }
    
    protected void addToChoice (Document dom, Element par, Property p) {
        var subs = substituteMap.get(p);
        if (!p.isAbstract()) addElementOnceRef(dom, par, p);
        if (null != subs)
            for (var sp : subs) addToChoice(dom, par, sp);
    }
    
    protected void addAttribute (Document dom, Element attParent, String atqn) {
        var ate = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        ate.setAttribute("ref", atqn);
        attParent.appendChild(ate);
    }
    
    // For a message schema, we create a simple type declaration from a Datatype object (FooType)
    @Override
    protected void createComplexTypeFromDatatype (Document dom, String nsuri, Datatype dt) {
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
            var rbdt  = dt.getRestrictionBase();
            var rbdqn = proxifiedDatatypeQName(rbdt);
            addRestrictionElement(dom, cte, dt, rbdt, rbdqn);
        }
        nsTypedefs.put(cname, cte);
    }   
    
   @Override
   protected void editStructuresDocument (Document doc, String nsuri) {
        super.editStructuresDocument(doc, nsuri);
        var root = doc.getDocumentElement();       
        var nl = root.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "anyAttribute");
        for (int i = nl.getLength() -1 ; i >= 0; i-- ) {
            var e = nl.item(i);
            var p = e.getParentNode();
            var en = e.getNodeName();
            var pn = p.getNodeName();
            var r = p.removeChild(e);
            var lc = p.getLastChild();
            var lcn = lc.getNodeName();
            int j = 0;
        }
   }    

    @Override
    protected String getArchitecture ()       { return "NIEM6"; }   
       
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
    
    // Convert NIEM 6 subset schema ctarg to message schema ctarg.
    @Override
    protected String genConformanceTargets (Namespace ns) {
        var ctas = super.genConformanceTargets(ns);
        ctas.replaceAll("#SubsetSchemaDocument", "#MessageSchemaDocument");
        return ctas;
    }     
    
    // In a message schema, object properties are nillable unless otherwise
    // specified. Data properties are never nillable -- if you make a property
    // referencable with appinfo:referenceCode, it becomes an object property.
    @Override
    protected boolean isPropertyNillable (Property p) {
        if (p.isAttribute()) return false;
        if (p.isAbstract())  return false;
        if (null == p.getClassType()) return false; // a data property
        var crc = p.getReferenceCode();             // reference code for a class
        if (null == crc) return true;               // nillable by default
        return !"NONE".equals(p.getReferenceCode());// nillable unless refcode is NONE
    }    

    // Don't convert "xs:foo" to "xs-proxy:foo" in message schema documents
    @Override
    protected String proxifiedDatatypeQName (Datatype dt) {
        if (gAttAugs.isEmpty()) return dt.getQName();
        else return super.proxifiedDatatypeQName(dt);
    }    
}
