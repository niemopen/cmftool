/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.niem.cmf.AugmentRecord;
import static org.mitre.niem.cmf.CMFObject.CMF_LIST;
import static org.mitre.niem.cmf.CMFObject.CMF_RESTRICTION;
import static org.mitre.niem.cmf.CMFObject.CMF_UNION;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.PropertyAssociation;
import org.mitre.niem.utility.MapToList;
import org.mitre.niem.utility.MapToSet;
import static org.mitre.niem.utility.StringUtils.replaceSuffix;
import static org.mitre.niem.xml.XMLDocument.makeQN;
import static org.mitre.niem.xml.XMLDocument.makeURI;
import static org.mitre.niem.xml.XMLDocument.qnToName;
import static org.mitre.niem.xml.XMLDocument.qnToPrefix;
import static org.mitre.niem.xsd.NIEMConstants.hasMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class for writing a non-conforming schema document pile for validating
 * an instance of an XML message format.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXMLSchema extends ModelToXSD {
    private static final Logger LOG = LogManager.getLogger(ModelToXMLSchema.class);  
    
    protected Set<Property> refAttPropS = new HashSet<>();      // need ref att for this object property
    
    // Set of augmentation property URIs for each augmentation point URI
    protected MapToSet<String,String> augPtU2augUs = new MapToSet<>();
    
    // List of appinfo:Augmentation records for each class URI
    protected MapToList<String,AugmentRecord> cU2appAugL = new MapToList<>();
    
    // Dummy property associations for global augmentation points
    protected PropertyAssociation assAugPA = new PropertyAssociation();
    protected PropertyAssociation objAugPA = new PropertyAssociation();    
    
    public ModelToXMLSchema (Model m)        { super(m); }
    
    // Also make simple type definitions for Restriction objects
    @Override
    protected void identifySimpleTypes () {
        super.identifySimpleTypes();
        for (var dt : m.datatypeL()) {
            if (CMF_RESTRICTION == dt.getType()) 
                simpleTypes.add(dt);
        }
    }
    
    // Also make a set of all required reference attributes.
    // Also create set of augmentation elements for each augmentation point
    // Also create dummy global augmentation points, and property associations for them.
    @Override
    protected void indexAugmentations () {
        super.indexAugmentations();
        
        // Identify the required reference attributes.
        // Create set of augmentation property URIs for each augmentation point URI.
        for (var ns: m.namespaceSet()) {
            for (var arec : ns.augL()) {                
                var p    = arec.property();             // augmentation property
                var ct   = arec.classType();            // augmented class, or null
                var gcs  = new HashSet<>(arec.codeS()); // global aug codes
                if (null != ct) gcs.add("CLASS");
                for (var gc : gcs) {
                    var aeU  = "";                          // augmentation property URI
                    var aptU = "";                          // augmentation point URI
                    var ctU  = "";                          // augmented class, or Object / Augmentation
                    switch (gc) {
                    case "CLASS": 
                        if (ct.isLiteralClass() && !p.isAttribute()) refAttPropS.add(p); 
                        var aptN = replaceSuffix(ct.name(), "Type", "");
                        aptU = makeURI(ct.namespaceURI(), aptN);    // http://AugmentedNS/ClassName
                        aeU  = makeURI(ns.uri(), aptN);             // http://AugmentingNS/ClassName
                        ctU  = ct.uri();
                        break;
                    case "LITERAL": 
                        if (!p.isAttribute()) refAttPropS.add(p);
                        ctU = "Literal";
                        break;
                    case "ASSOCIATION":
                    case "OBJECT":
                        aptU = capitalize(gc.toLowerCase());    // Object or Association
                        aeU  = makeURI(ns.uri(), aptU);
                        ctU  = aptU;
                        break;
                    }
                    // Keep list of appinfo:Augmentation records by augmented class
                    if (-2 == arec.index()) {
                        cU2appAugL.add(ctU, arec);  
                    }
                    else if (-1 == arec.index()) {
                        aptU = aptU + "AugmentationPoint";
                        augPtU2augUs.add(aptU, p.uri());
                    }
                    else if (!aptU.isBlank()) {
                        aeU  = aeU + "Augmentation";
                        aptU = aptU + "AugmentationPoint";
                        augPtU2augUs.add(aptU, aeU);                        
                    }
                }
            }
        }     
        // Construct global augmentation point property associations
        var assAugP = new Property(null, "AssociationAugmentationPoint");
        var objAugP = new Property(null, "ObjectAugmentationPoint");
        assAugP.setIsAbstract(true);
        objAugP.setIsAbstract(true);
        assAugPA.setProperty(assAugP);
        assAugPA.setMinOccurs("0");
        assAugPA.setMaxOccurs("unbounded");
        objAugPA.setProperty(objAugP);
        objAugPA.setMinOccurs("0");
        objAugPA.setMaxOccurs("unbounded");         
    }

    // Also create reference attributes needed in this namespace.    
    @Override
    protected void createAugmentationComponents(Document doc, Namespace ns) {
        super.createAugmentationComponents(doc, ns);
        for (var p : refAttPropS) {
            if (ns != p.namespace()) continue;
            var raN = uncapitalize(p.name()) + "Ref";
            var raE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            raE.setAttribute("name", raN);
            raE.setAttribute("type", "xs:IDREFS");
            decEL.add(raE);
        }
    }
    
    // A class with these reference codes turns into a CCC type that must have
    // structures:uri or structures:ref
    private static final Set<String> needURIcodes  = Set.of("ANY", "ANYURI", "INTERNAL", "RELURI");
    private static final Set<String> needRefcodes  = Set.of("ANY", "INTERNAL", "IDREF");    

    @Override
    protected void createCCCType(Document doc, ClassType ct) {
        var appinfoPre = bc2pre.get("APPINFO");
        var appinfoU   = bc2U.get("APPINFO");
        var structPre  = bc2pre.get("STRUCTURES");
        var structU    = bc2U.get("STRUCTURES");
        
        var ver = ct.namespace().archVersion();
        if (null != useArchVersion) ver = useArchVersion;

        var ctN = ct.name();
        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        var anE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:annotation");
        var ccE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexContent");
        var exE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
        var sqE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:sequence");

        ctE.setAttribute("name", ct.name());
        addDocumentation(ctE, ct.docL());
        defEL.add(ctE);        

        // Make note of structures attributes needed in this complex type.
        // Don't need any inherited structures attributes.
        // Can't extend parent class if reference codes are not compatible.
        var pct      = ct.subClassOf();
        var refCode  = ct.effectiveReferenceCode();
        var needURI  = needURIcodes.contains(refCode);      // must this type have s:uri?
        var needRef  = needRefcodes.contains(refCode);      // must this type have s:ref?
        var extendF  = false;                               // does this type extend parent type?
        if (null != pct) {                                  // check compatibility with parent class
            var prefCode = pct.effectiveReferenceCode();
            var pNeedURI = needURIcodes.contains(prefCode); // parent class needs s:uri
            var pNeedRef = needRefcodes.contains(prefCode); // parent class needs s:ref
            extendF = true;                                 // can we extend from parent class?
            if (pNeedURI && !needURI) extendF = false;      // not if parent needs s:uri but child doesn't
            if (pNeedRef && !needRef) extendF = false;      // not if parent needs s:ref but child doesn't
            if (extendF) {
                needURI = needURI && !pNeedURI;             // must have s:uri if parent doesn't
                needRef = needRef && !pNeedRef;             // must have s:ref if parent doesn't
            }
        } 
        // Need xs:complexContent and xs:extension elements if we are extending a parent class.
        // Otherwise we only need the xs:sequence element.
        var attParentE = ctE;           // add attributes to the xs:complexType
        if (extendF) {
            refnsUS.add(pct.namespaceURI());
            exE.setAttribute("base", pct.qname());
            exE.appendChild(sqE);
            ccE.appendChild(exE);
            ctE.appendChild(ccE);
            attParentE = exE;           // add attributes to the xs:extension
        }
        else ctE.appendChild(sqE);
                
        // Construct property association list for children of xs:sequence
        // If we are not extending a parent class, we must add dummy prop association
        // for the global augmentation point, followed by the inherited properties.
        // Then append the properties defined in this class.
        var propL = new ArrayList<PropertyAssociation>();
        if (!extendF) {
            if (ct.isAssociationClass()) propL.add(assAugPA);
            if (ct.isObjectClass())      propL.add(objAugPA);
            if (null != pct) addParentProperties(pct, propL);
        }
        propL.addAll(ct.propAssocL());
            
        // Finally, append a dummy augmentation point property association
        if (ct.isAssociationClass() || ct.isObjectClass()) {
            var apN   = replaceSuffix(ct.name(), "Type", "AugmentationPoint");
            var augp  = new Property(ct.namespace(), apN);
            var augPA = new PropertyAssociation();
            augp.setIsAbstract(true);
            augPA.setProperty(augp);
            augPA.setMinOccurs("0");
            augPA.setMaxOccurs("unbounded");
            propL.add(augPA);            
        }
       
        // Add xs:element refs or xs:choice to the xs:sequence element
        // for all object properties.
        for (var pa : propL) {
            var p  = pa.property();          
            var pU = p.uri();
            if (p.isAttribute()) continue;      // attributes done later
            
            // Construct a set of possible choices for this property association
            var choiceUS = new HashSet<String>();
            
            // If this property is an augmentation point, add all the 
            // corresponding augmentation properties to the choice set
            if (pU.endsWith("AugmentationPoint")) {
                choiceUS.addAll(augPtU2augUs.get(pU));
            }
            // If it's not an augmentation point, then add this property, plus
            // all of its subproperties (from substitutionGroup or xs:choice in model XSD)
            else {
                if (!p.isAbstract()) choiceUS.add(p.uri());
                for (var subp : p.allSubProps()) 
                    if (!subp.isAbstract()) choiceUS.add(subp.uri());
            }
            // Append element refs to an xs:choice element if more than one choice.
            // Otherwise add the element ref to the xs:sequence element.
            var parE = sqE;
            if (choiceUS.size() > 1) {
                parE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:choice");
                if (!"1".equals(pa.minOccurs())) parE.setAttribute("minOccurs", pa.minOccurs());
                if (!"1".equals(pa.maxOccurs())) parE.setAttribute("maxOccurs", pa.maxOccurs());
                addDocumentation(parE, pa.docL());
                sqE.appendChild(parE);                
            }
            var choiceUL = new ArrayList<>(choiceUS);
            Collections.sort(choiceUL);
            for (var spU : choiceUL) {                          // 
              var spnsU = m.uriToNSU(spU);
              var spQ   = m.uriToQN(spU);
              var elE  = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
              elE.setAttribute("ref", spQ);
              if (choiceUS.size() == 1) {
                  if (!"1".equals(pa.minOccurs())) elE.setAttribute("minOccurs", pa.minOccurs());
                  if (!"1".equals(pa.maxOccurs())) elE.setAttribute("maxOccurs", pa.maxOccurs());
                  addDocumentation(elE, pa.docL());
              }
              parE.appendChild(elE);
              if (!spnsU.isBlank()) refnsUS.add(spnsU);     // could be ObjectAugmentation
            }
        }
        // Add xs:any elements to the xs:sequence
        for (var ap : ct.anyL()) {
            if (ap.isAttribute()) continue;
            var anyE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:any");            
            if (!"1".equals(ap.minOccurs())) anyE.setAttribute("minOccurs", ap.minOccurs());
            if (!"1".equals(ap.maxOccurs())) anyE.setAttribute("maxOccurs", ap.maxOccurs());
            setAttribute(anyE, "processContents", ap.processCode());
            setAttribute(anyE, "namespace", ap.nsConstraint());
            sqE.appendChild(anyE);
        }
        // Construct a list of attributes for this type.  Start with the 
        // attributes in the class.  
        var apropL = new ArrayList<PropertyAssociation>();
        for (var pa : ct.propAssocL()) {
            if (pa.property().isAttribute()) apropL.add(pa);
        }
        // Next, add augmentation attributes not already present.
        // These have to come from appinfo:Augmentation records.
        // Some are augmentations for this class, others are global augmentations.
        addToAttPAList(apropL, ct, cU2appAugL.get(ct.uri()));
        if (ct.isAssociationClass()) addToAttPAList(apropL, ct, cU2appAugL.get("Association"));
        if (ct.isObjectClass())      addToAttPAList(apropL, ct, cU2appAugL.get("Object"));
        if (ct.isLiteralClass())     addToAttPAList(apropL, ct, cU2appAugL.get("Literal"));
        for (var pa : apropL) {
            var p  = pa.property();
            var pQ = p.qname();
            if (!p.isAttribute()) {
                if (ct.isLiteralClass() || pa.codeS().contains("LITERAL")) {
                    var pre = qnToPrefix(pQ);                       // pre
                    var pN  = qnToName(pQ);                         // SomeProperty
                    pQ = makeQN(pre, uncapitalize(pN) + "Ref");     // pre:somePropertyRef
                }
                else continue;  // not an attribute; can this happen?                
            }
            else if (pa.index() >= 0) continue;     // aug attribute in an aug type; can this happen?
        
            var atE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            atE.setAttribute("ref", pQ);
            if ("1".equals(pa.minOccurs())) atE.setAttribute("use", "required");
            refnsUS.add(p.namespaceURI());
            addDocumentation(atE, pa.docL());
            attParentE.appendChild(atE);
        }
        // Add xs:anyAttribute wildcards as needed
        for (var ap : ct.anyL()) {
            if (!ap.isAttribute()) continue;
            var anyE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:anyAttribute");            
            setAttribute(anyE, "processContents", ap.processCode());
            setAttribute(anyE, "namespace", ap.nsConstraint());
            attParentE.appendChild(anyE);
        }   
        // Add structures attributes as needed
        if (needURI || needRef) addStructuresAttribute(attParentE, "id", structPre, structU);
        if (needRef)            addStructuresAttribute(attParentE, "ref", structPre, structU);
        if (needURI)            addStructuresAttribute(attParentE, "uri", structPre, structU);
        if (!extendF && hasMetadata.contains(ver))
            addStructuresAttribute(attParentE, "metadata", structPre, structU);    
    }
    
    protected void addStructuresAttribute (Element parent, String name, String sPre, String sU) {
        var sE = parent.getOwnerDocument().createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        sE.setAttribute("ref", makeQN(sPre, name));
        parent.appendChild(sE);
        refnsUS.add(sU);
    }
    
    // Create a list of property associations for a class hierarchy, beginning
    // with the top of the inheritance chain.
    protected void addParentProperties (ClassType pct, List<PropertyAssociation> propL) {
        if (null != pct.subClassOf()) addParentProperties(pct.subClassOf(), propL);
        propL.addAll(pct.propAssocL());
    }    
    
    // Add each property association to the list, if it's an attribute property, and
    // if it isn't already present.  But replace an optional property with required.
    protected void addToAttPAList (List<PropertyAssociation> lst, ClassType ct, List<AugmentRecord> addL) {
        for (var add : addL) {
            var addp = add.property();
            
            // An object property added to a literal class becomes a reference attribute
            if (!addp.isAttribute()) {
                if (!ct.isLiteralClass()) continue;     // should be a choice for the aug point
                var pre  = addp.namespace().prefix();
                var name = addp.name();
                var refQ = makeQN(pre, uncapitalize(name) + "Ref");
            }
            PropertyAssociation inlist = null;
            for (var lrec : lst) {
                if (lrec.property() == add.property()) inlist = lrec;
            }
            if (null == inlist) lst.add(add);
            else if (inlist.minOccursVal() ==0 && add.minOccursVal() > 0) {
                lst.remove(inlist);
                lst.add(add);
            }
        }
    }

    // Create a complex type with simple content from a literal class object,
    // or a class derived from a literal class.
    @Override
    protected void createCSCType(Document doc, ClassType ct) {
        var ctU = ct.uri();
        var ns  = ct.namespace();
        var ver = ns.archVersion();
        
        var ctE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:complexType");
        var scE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleContent");
        var exE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:extension");
        
        ctE.setAttribute("name", ct.name());
        addDocumentation(ctE, ct.docL());
        scE.appendChild(exE);
        ctE.appendChild(scE);
        defEL.add(ctE);

        // Make note of structures attributes needed in this complex type.
        // Don't need any inherited structures attributes.
        // Can't extend parent class if reference codes are not compatible.
        var pct      = ct.subClassOf();
        var refCode  = ct.effectiveReferenceCode();
        var needURI  = needURIcodes.contains(refCode);
        var needRef  = needRefcodes.contains(refCode);
        var extendF  = false;
        if (null != pct) {
            var prefCode = pct.effectiveReferenceCode();
            var pNeedURI = needURIcodes.contains(prefCode);
            var pNeedRef = needRefcodes.contains(prefCode);
            extendF = true;
            if (pNeedURI && !needURI) extendF = false;
            if (pNeedRef && !needRef) extendF = false;
            if (extendF) {
                needURI = needURI && !pNeedURI;
                needRef = needRef && !pNeedRef;
            }
        }
        // Construct a list of attributes for this type.  Start with the 
        // attributes in the class.  
        var apropL = new ArrayList<PropertyAssociation>();
        for (var pa : ct.propAssocL()) {
            if (pa.property().isAttribute()) apropL.add(pa);
        }
        // Next, add augmentation attributes not already present.
        // These have to come from appinfo:Augmentation records.
        // Some are augmentations for this class, others are global augmentations
        // for literal classes.  If the augmentation is an object property,
        // add a reference attribute instead.
        addToAttPAList(apropL, ct, cU2appAugL.get(ct.uri()));
        addToAttPAList(apropL, ct, cU2appAugL.get("Literal"));
        for (var pa : apropL) {
            var p    = pa.property();
            var refQ = p.qname();
            if (refQ.endsWith("Literal")) continue;
            if (!p.isAttribute()) {
                var refp = qnToPrefix(refQ);
                var refn = qnToName(refQ);
                refQ = refp + ":" + uncapitalize(refn) + "Ref";
            }
            var atE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
            atE.setAttribute("ref", refQ);
            if ("1".equals(pa.minOccurs())) atE.setAttribute("use", "required");
            refnsUS.add(p.namespaceURI());
            addDocumentation(atE, pa.docL());
            exE.appendChild(atE);
        }
            // Add structures attributes as needed
        var structPre  = bc2pre.get("STRUCTURES");
        var structU    = bc2U.get("STRUCTURES");
        if (needURI || needRef) addStructuresAttribute(exE, "id", structPre, structU);
        if (needURI)            addStructuresAttribute(exE, "uri", structPre, structU);
        if (needRef)            addStructuresAttribute(exE, "ref", structPre, structU);
        if (!extendF && hasMetadata.contains(ver))
            addStructuresAttribute(exE, "metadata", structPre, structU);  
    
        // Extension base may be a simple type
        var ctname = ct.qname();
        var dt   = ct.literalDatatype();
        if (dt != null && simpleTypes.contains(dt)) {
            var dtnsU = dt.namespaceURI();
            var dtQ   = dt.qname();
            exE.setAttribute("base", dtQ);
            refnsUS.add(dtnsU);
        }
        // Or the extension base may be another model class
        else if (null != dt && dt.namespace().isModelNS()) {
            var baseQ = dt.qname();
            exE.setAttribute("base", baseQ);
            refnsUS.add(dt.namespaceURI());
        }
        // Or the extension base may be inherited?
        else if (null != ct.subClassOf()) {
            var baseQ = ct.subClassOf().qname();
            exE.setAttribute("base", baseQ);
            refnsUS.add(ct.subClassOf().namespaceURI());
        }
        // Or the extension base may be a XSD primitive
        else if (null != dt) {
            exE.setAttribute("base", dt.qname());
        }   
        else LOG.error("Can't determine extension base for {}", ct.qname());    
    }

    // Not needed when creating a message schema; datatype objects always
    // become a simple type declaration.
    @Override
    protected void createCSCType(Document doc, Datatype dt) {
        
    }

    // Create an xs:simpleType element from a Datatype object.
    @Override
    protected void createSimpleType(Document doc, Datatype dt) {
        var nsU = dt.namespaceURI();
        var dtQ = dt.qname();
        if (W3C_XML_SCHEMA_NS_URI.equals(nsU)) return;  // don't create XSD types
        if (XML_NS_URI.equals(nsU)) return;             // don't create XML types
        
        var stE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:simpleType");
        stE.setAttribute("name", dt.name());
        addDocumentation(stE, dt.docL());
        defEL.add(stE);
        
        switch (dt.getType()) {
        case CMF_LIST:          populateList(doc, stE, dt); break;
        case CMF_RESTRICTION:   populateRestriction(doc, stE, dt); break;
        case CMF_UNION:         populateUnion(doc, stE, dt); break;            
        }
    }

    // Create an attribute or element declaration
    @Override
    protected void createDeclaration(Document doc, Property p) {
        if (W3C_XML_SCHEMA_NS_URI.equals(p.namespaceURI())) return;
        if (XML_NS_URI.equals(p.namespaceURI())) return;
        if (p.name().endsWith("Literal")) return;
        if (p.isAbstract()) return;

        Element decE;
        if (p.isAttribute()) decE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attribute");
        else decE = doc.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:element");
        decEL.add(decE);
        
        var ptQ = "";
        var pt  = p.type();
        if (null != pt) { 
            refnsUS.add(pt.namespaceURI());
            ptQ = pt.qname();
        }
        // No abstract, appinfo, or substitionGroup in a message schema
        decE.setAttribute("name", p.name());
        setAttribute(decE, "type", ptQ);
        if (p.isReferenceable())
            decE.setAttribute("nillable", "true");
        addDocumentation(decE, p.docL());
    }

    @Override
    protected void createComplexTypes(Document doc, Namespace ns) {
        for (var ct : m.classTypeL()) {
            if (ct.namespace() != ns) continue;
            if (ct.hasSimpleContent()) createCSCType(doc, ct);
            else createCCCType(doc, ct);
        }
    }
    
    // For a message schema, we have to go through all the element declarations
    // and remove @substitutionGroup to augmentation points, because there aren't any
    // augmentation points in a message schema.
    @Override
    protected void processDeclarations () {
        for (var e : decEL) {
            var sub = e.getAttribute("substitutionGroup");
            if (null == sub || !sub.endsWith("AugmentationPoint")) continue;
            e.removeAttribute("substitutionGroup");
        }
    }
    
    @Override
    protected String datatypeQName (Datatype dt) {
        return dt.qname();
    }
    
}
