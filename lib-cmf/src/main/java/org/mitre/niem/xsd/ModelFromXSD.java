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
package org.mitre.niem.xsd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xs.XSAnnotation;
import static org.apache.xerces.xs.XSAnnotation.W3C_DOM_DOCUMENT;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_SIMPLE;
import static org.apache.xerces.xs.XSConstants.ATTRIBUTE_DECLARATION;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSFacet;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSMultiValueFacet;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_PATTERN;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.VARIETY_ATOMIC;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.VARIETY_LIST;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.VARIETY_UNION;
import org.apache.xerces.xs.XSTypeDefinition;
import static org.apache.xerces.xs.XSTypeDefinition.COMPLEX_TYPE;
import static org.apache.xerces.xs.XSTypeDefinition.SIMPLE_TYPE;
import org.mitre.niem.cmf.AugmentRecord;
import org.mitre.niem.cmf.CMFException;
import static org.mitre.niem.cmf.CMFObject.CMF_LIST;
import static org.mitre.niem.cmf.CMFObject.CMF_RESTRICTION;
import static org.mitre.niem.cmf.CMFObject.CMF_UNION;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.CodeListBinding;
import org.mitre.niem.cmf.Component;
import static org.mitre.niem.cmf.Component.uriNamePart;
import org.mitre.niem.cmf.DataProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.ListType;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.Restriction;
import org.mitre.niem.cmf.Union;
import org.mitre.niem.xml.ParserBootstrap;
import org.mitre.niem.xml.XMLSchema;
import org.mitre.niem.xml.Xerces;
import static org.mitre.niem.xsd.NamespaceKind.*;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class to create a Model object from a NIEM XSD schema
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSD {
    
    private Model m = null;
    private NIEMSchema sch = null;
    private XSModel xs = null;

    
    /**
     * create Namespace objects
     * collect all the attribute augmentations
     * go through CSC types to find all datatypes and literal classes
     * go through CCC types to find ordinary classes
     *   identify needed SimpleTypes (for list, union, literal properties)
     * go through declarations to find properties
     * populate class objects with properties
     * profit!
     * 
     */
    
    ModelFromXSD () { }
    
    public Model createModel (String... args) throws XMLSchema.XMLSchemaException, CMFException {
        sch = new NIEMSchema(args);
        return createModel(sch);
    }
    
    public Model createModel (NIEMSchema s) throws CMFException {
        m = new Model();
        sch = s;
        xs = sch.xsmodel();
        createNamespaces();                 // an object for each model namespace
        indexAttributeAugmentations();      // make map classURI -> list of its attribute augmentations
        identifyCSCtypes();                 // now we can tell datatypes from literal classes
        identifySimpleTypes();              // now we can tell which SimpleType names we need
        initializeDatatypes();              // now we can create Datatype objects
        populateDatatypes();                // and with all Datatypes in hand, we can fill them out
        initializeClassTypes();             // create ClassType objects for CCC types and literalClass types
        createPropertiesFromAttributes();
        createPropertiesFromElements();
        createLiteralProperties();
        // createDataProperties();            // we have the Datatypes, can create the DataProperties
        // createClassTypes();                // create ClassType objects, but leave property associations for later
        // createObjectProperties();          // we have the ClassTypes, can create the ObjectProperties
        // createPropertyAssociations();      // now we have all the Property objects 
        return m;
    }
    
    // Create and populate Namespace objects for all the model namespaces
    // (including XML and XS namespaces, but not the builtins).
    private void createNamespaces () throws CMFException {
        for (var nsuri : sch.schemaNamespaceURIs()) {
            var kind = sch.namespaceKind(nsuri);
            switch (kind) {
            case NSK_APPINFO:
            case NSK_CLSA:
            case NSK_PROXY:
            case NSK_STRUCTURES:
            case NSK_NOTNIEM:
            case NSK_UNKNOWN: continue;     // no namespace object for these namespaces
            }
            var sd = sch.schemaDocument(nsuri);
            var prefix = sch.namespaceMap().getPrefix(nsuri);
            var ns = new Namespace(prefix, nsuri);
            ns.setDocumentFilePath(sch.pilePath(sd));
            ns.setVersion(sd.version());
            ns.setLanguage(sd.language());       
            ns.setConformanceTargets(sd.ctAssertions());
            ns.docL().addAll(sd.documentation());
            ns.locTermL().addAll(sd.localTerms());
            m.addNamespace(ns);
        }
        // Go through all the imports to record import documentation
        for (var ns : m.namespaceSet()) {
            var docL = sch.importDocumentation(ns.uri());
            if (null != docL) ns.impDocL().addAll(docL);
        }
    }
    
    // Create a map from the URIs for ClassTypes that are augmented by one or more
    // attribute properties, to a list of the URIs for the augmenting attributes.
    // Also create lists of any global attribute augmentations for association, object,
    // and literal classes.  We need this to decide whether a CSC type should be
    // a literal ClassType object or a Datatype object.
    private record AugRec (String classU, String propU, String codes) { };
    private Map<String,List<AugRec>> attAugForClass = new HashMap<>();      // ClassURI -> augRecs
    private Map<String,List<AugRec>> globalAugs = new HashMap<>();          // code -> augRecs
    private boolean globalLiteralAugs = false;
    private void indexAttributeAugmentations () {
        for (var nsuri : sch.schemaNamespaceURIs()) {
            var sd    = sch.schemaDocument(nsuri);
            var augEL = sd.attributeAugmentations();
            for (var augE : augEL) {
                var classQ = augE.getAttribute("class");
                var propQ  = augE.getAttribute("property");
                var codes  = augE.getAttribute("globalClassCode");
                var propU  = m.qnToURI(propQ);
                if (codes.isBlank()) {
                    var classU = m.qnToURI(classQ);
                    var augRecL = attAugForClass.get(classU);
                    if (null == augRecL) {
                        augRecL = new ArrayList<>();
                        attAugForClass.put(classU, augRecL);
                    }
                    var arec = new AugRec(classU, propU, "");
                    augRecL.add(arec);
                }
                else {
                    var codeL = codes.trim().split("\\s+");
                    for (var code : codeL) {
                        if ("LITERAL".equals(code)) globalLiteralAugs = true;
                        var augRecL = globalAugs.get(code);
                        if (null == augRecL) {
                            augRecL = new ArrayList<>();
                            globalAugs.put(code, augRecL);
                        }
                        var arec = new AugRec("", propU, code);
                        augRecL.add(arec);
                    }
                }              
            }
        }
    }
    
    // Examine all of the CSC types to categorize them as literal ClassTypes, or
    // Datatypes, or proxy types.
    // * A literal class has at least one model attribute (including augmentations)
    // * A proxy type comes from the proxy namespace for this schema document (just ignore these)
    // * A datatype is every other CSC type
    private final Set<String> literalClassUs        = new HashSet<>();  // literal class component URIs
    private final Set<String> datatypeUs            = new HashSet<>();  // datatype component URIs
    
    // At some point we will have a model object and want the XSObject we need to
    // populate the model object fields.  This map goes from model object URI to 
    // XSObject.  (From the model object type you can know how to cast the XSObject.)
    private final Map<String,XSObject> modelURI2xobj = new HashMap<>();
    
    private void identifyCSCtypes () {
        var xMap = xs.getComponents(COMPLEX_TYPE);
        for (int i = 0; i < xMap.size(); i++) {
            var xctype = (XSComplexTypeDefinition)xMap.item(i);
            var ctypeU = xObjToURI(xctype);         // CSC type URI; eg. http://someNS/FooType
            var ctnsU  = xctype.getNamespace();     // CSC type namespace URI; eg. http://someNS/
            var ctname = xctype.getName();          // CSC type name; eg. FooType
            var nskind = sch.namespaceKind(ctnsU);
            if (CONTENTTYPE_SIMPLE != xctype.getContentType()) continue;
            if (!isModelKind(nskind)) continue;     // not doing proxy or external types
            var attUL  = collectAttributes(xctype); // list of model attribute URIs in CSC type
            
            // A CSC type (eg. FooType) with attributes is a literal ClassType object.
            if (!attUL.isEmpty() || null != attAugForClass.get(ctypeU) || globalLiteralAugs) {
                var xstype = xctype.getSimpleType();
                var stypeU = xObjToURI(xstype);
                if (ctypeU.equals(stypeU.replaceAll("SimpleType$", "Type"))) {
                    datatypeUs.add(stypeU);
                    modelURI2xobj.put(stypeU, xstype);
                }
                literalClassUs.add(ctypeU);             // http://someNS/FooType will be a literal class
                modelURI2xobj.put(ctypeU, xctype);      // remember CSC defn for this literal class
            }
            // A CSC type with no attributes is a Datatype object.
            else {
                datatypeUs.add(ctypeU);                 // http://someNS/FooType will be a datatype
                modelURI2xobj.put(ctypeU, xctype);      // populate model object from the CSC definition
            }
        }
    }

    // Usually a FooSimpleType has a corresponding CSC FooType, which we handled above,
    // and so usually nothing has to be done for FooSimpleType.  However, sometimes we
    // will find a FooSimpleType without that CSC type.  Then either we need a datatype named
    // FooSimpleType for a literal class (handled above), or we need this datatype in
    // in the model, but named FooType.
    private void identifySimpleTypes () {
        var xMap = xs.getComponents(SIMPLE_TYPE);
        for (int i = 0; i < xMap.size(); i++) {
            var xstype = (XSSimpleTypeDefinition)xMap.item(i);
            if (!sch.isModelNamespace(xstype.getNamespace())) continue;
            var stypeU = xObjToURI(xstype);                         // schema ST URI, eg. http://someNS/FooSimpleType
            var dtypeU = stypeU.replaceAll("SimpleType$", "Type");  // datatype URI; http://someNS/FooType

            if (datatypeUs.contains(stypeU)) continue;              // already a datatype (for a literal property)
            if (datatypeUs.contains(dtypeU)) continue;              // already have this FooType datatype
                
            // We need this datatype, but named FooType, not FooSimpleType.
            datatypeUs.add(dtypeU);                 // must create datatype for http://someNS/FooType
            modelURI2xobj.put(dtypeU, xstype);      // populate model object from the simple type definition
        }
    }
 
    // We have a list of all the Datatypes in the model, and we know which ones
    // need to be named FooType and which named FooSimpleType. 
    private final Map<Datatype,XSSimpleTypeDefinition> datatype2xstype = new HashMap<>();
    private void initializeDatatypes () {
        for (var dtU : datatypeUs) {
            XSSimpleTypeDefinition xstype;   
            var dtns   = m.compUToNamespaceObj(dtU);    // datatype namespace object
            var dtname = uriNamePart(dtU);              // datatype name
            var xtype  = (XSTypeDefinition)modelURI2xobj.get(dtU);
            if (COMPLEX_TYPE == xtype.getTypeCategory()) {
                var xctype = (XSComplexTypeDefinition) xtype;
                xstype = xctype.getSimpleType();
            }
            else xstype = (XSSimpleTypeDefinition)xtype;

            // Simple type definition tells us if we have a list, union, or restriction.
            Datatype dt = null;
            switch(xstype.getVariety()) {
            case VARIETY_ATOMIC: dt = new Restriction(dtns, dtname); break;
            case VARIETY_LIST:   dt = new ListType(dtns, dtname); break;
            case VARIETY_UNION:  dt = new Union(dtns, dtname); break;
            }
            datatype2xstype.put(dt, xstype);
            m.addDatatype(dt);
        }     
    }
    
    // If we tried to do this in one pass, we would wind up trying to populate
    // <xs:list itemType="foo:BarType"> before we had the object for foo:BarType.
    // Two passes is so much easier than some complicated recursive approach!
    private void populateDatatypes () throws CMFException {
        for (var dt : m.datatypeL()) {
            XSSimpleTypeDefinition xstype = null;
            XSComplexTypeDefinition xctype = null;
            Element schE = null;
            var dtU   = dt.uri();
            var xtype = (XSTypeDefinition)modelURI2xobj.get(dtU);
            if (COMPLEX_TYPE == xtype.getTypeCategory()) {
                xctype = (XSComplexTypeDefinition)xtype;
                xstype = xctype.getSimpleType();        // populate Datatype from this CSC defn
                schE   = getSchemaDocElement(xctype);   // get appinfo from this Element
                populateComponent(dt, xctype, schE);
            }
            else { 
                xstype = (XSSimpleTypeDefinition)xtype; // populate Datatype from this ST defn
                schE   = getSchemaDocElement(xstype);   // get appinfo from this Element
                populateComponent(dt, xstype, schE);
            }
            switch(dt.getType()) {
                case CMF_LIST:        populateListType((ListType)dt, xstype, schE); break;
                case CMF_RESTRICTION: populateRestriction((Restriction)dt, xstype, schE); break;
                case CMF_UNION:       populateUnion((Union)dt, xstype, schE); break;
            }
        }
    }
    
    // Populates documentation and isDeprecated fields for all Component objects.
    private void populateComponent (Component c, XSObject xobj, Element schE) {
        var docL  = Xerces.getDocumentation(xobj);  // extract documentation from the XS decl or defn
        var appi  = getAppinfoAttributes(schE);        // extract appinfo from the schema document element
        var dep   = appi.getOrDefault("deprecated", "");
        c.setDocumentation(docL);
        if (null != dep) c.setIsDeprecated("true".equals(dep));
    }

    // Populate a ListType object with appinfo from its CSC type definition and
    // the list item type from its simple type definition.
    private void populateListType (ListType lt, XSSimpleTypeDefinition xstype, Element schE) throws CMFException {
        var ltname = lt.name();
        var appi  = getAppinfoAttributes(schE);
        var oFlag = appi.getOrDefault("orderedPropertyIndicator", "");
        var xitem = xstype.getItemType();       // list item simple type def from XS ST def
        var itemU = xObjToURI(xitem);           // URI for list item 
        var idt   = getDatatypeFromXStype(xitem);
        if (null == idt)
            throw new CMFException(
                String.format("can't find item datatype %s for list datatype %s", itemU, lt.uri()));
        lt.setItemType(idt);
        if (null != oFlag) lt.setIsOrdered("true".equals(oFlag));
    }
    
    // Populate a Restriction object with appinfo from its CSC type definition,
    // including CLSA appinfo. Populate the base type and facet list from its 
    // simple type definition
    private void populateRestriction (Restriction r, XSSimpleTypeDefinition xstype, Element schE) throws CMFException {
        // Handle code list schema appinfo
        var rnsU  = r.namespaceURI();
        var sd    = sch.schemaDocument(rnsU);
        var clsaU = sd.builtinNS("CLSA");
        var appiL = getAppinfoChildren(schE);
        for (var appi : appiL) {
            var clsaNL = appi.getElementsByTagNameNS(clsaU, "SimpleCodeListBinding");
            for (int i = 0; i < clsaNL.getLength(); i++) {
                var clsaE    = (Element)clsaNL.item(i);
                var clU      = clsaE.getAttribute("codeListURI");
                var column   = clsaE.getAttribute("columnName");
                var consFlag = clsaE.getAttribute("constrainingIndicator");
                var clb      = new CodeListBinding(clU, column, "true".equals(consFlag));
                r.setCodeListBinding(clb);
            }
        }
        // Now handle the restriction facets
        var hasF  = false;                  // true if any model facets
        var facetL = xstype.getFacets();
        for (int i = 0; i < facetL.getLength(); i++) {
            var xf    = (XSFacet)facetL.item(i);
            var fkind = xf.getFacetKind();
            var fcode = Xerces.facetKindToElementName(fkind);
            var fval  = xf.getLexicalFacetValue();
            if (Xerces.isDefaultFacet(xstype, fkind, fval)) continue;
            var docL  = Xerces.getDocumentation(xf);
            var fobj  = new Facet();      
            fobj.setCategory(StringUtils.capitalize(fcode));
            fobj.setValue(fval);
            fobj.setDocumentation(docL);
            r.addFacet(fobj);
            hasF = true;
        }
        facetL = xstype.getMultiValueFacets();
        for (int i = 0; i < facetL.getLength(); i++) {
            var xf    = (XSMultiValueFacet)facetL.item(i);
            var fkind = xf.getFacetKind();
            var fcode = (FACET_PATTERN == fkind ? "pattern" : "enumeration");
            var fvalL = xf.getLexicalFacetValues();
            var xannL = xf.getAnnotations();
            for (int j = 0; j < fvalL.getLength(); j++) {
                var fval = fvalL.item(j);
                if (Xerces.isDefaultFacet(xstype, fkind, fval)) continue;
                var fobj = new Facet();
                fobj.setCategory(StringUtils.capitalize(fcode));
                fobj.setValue(fval);
                if (j < xannL.getLength() && null != xannL.item(j)) {
                    var xann = (XSAnnotation)xannL.item(j);
                    var docL = Xerces.getDocumentation(xann);
                    fobj.setDocumentation(docL);
                }
                r.addFacet(fobj);
                hasF = true;
            }
        }
        // An XSD type with no facets is a proxy equivalent; its base is funky
        XSSimpleTypeDefinition xbase;
        if (hasF) xbase = (XSSimpleTypeDefinition)xstype.getBaseType();
        else {
            var dtxtype = datatype2xstype.get(r);
            if (W3C_XML_SCHEMA_NS_URI.equals(dtxtype.getNamespace())) xbase = dtxtype;
            else xbase = (XSSimpleTypeDefinition)xstype.getBaseType();
        }
        var baseU = xObjToURI(xbase);
        var bdt   = getDatatypeFromXStype(xbase);
        if (null == bdt)
            throw new CMFException(
                String.format("can't find base datatype %s for restriction datatype %s", baseU, r.uri()));
        r.setBase(bdt);
    }
    
    // Populates a Union object with appinfo from its CSC type definition plus
    // the member types from its simple type definition.
    private void populateUnion (Union u, XSSimpleTypeDefinition xstype, Element sobjU) throws CMFException {
        var xmbrL = xstype.getMemberTypes();
        for (var i = 0; i < xmbrL.getLength(); i++) {
            var xmbr = (XSSimpleTypeDefinition)xmbrL.item(i);
            var mbrU = xObjToURI(xmbr);
            var mdt  = getDatatypeFromXStype(xmbr);
            if (null == mdt)
                throw new CMFException(
                    String.format("can't find item datatype %s for list datatype %s", mbrU, u.uri()));
            u.addMember(mdt);
        }
    }
    
    // Create a ClassType object for each model CCC type definition, and also for
    // the literal classes identified earlier.
    private void initializeClassTypes () {
        var xMap = xs.getComponents(COMPLEX_TYPE);
        for (int i = 0; i < xMap.size(); i++) {
            var xctype = (XSComplexTypeDefinition)xMap.item(i);
            var ctypeU = xObjToURI(xctype);         // CSC type URI; eg. http://someNS/FooType
            var ctnsU  = xctype.getNamespace();     // CSC type namespace URI; eg. http://someNS/
            var ctname = xctype.getName();          // CSC type name; eg. FooType
            var nskind = sch.namespaceKind(ctnsU);
            if (CONTENTTYPE_SIMPLE == xctype.getContentType()) continue;
            if (!isModelKind(nskind)) continue;     // not doing proxy or external types
            var ctns   = m.namespaceObj(ctnsU);
            var ct = new ClassType(ctns, ctname);
            m.addClassType(ct);
            modelURI2xobj.put(ctypeU, xctype);
        }
        for (var ctypeU : literalClassUs) {
            var ctns   = m.compUToNamespaceObj(ctypeU);
            var ctname = m.compUToName(ctypeU);
            var ct     = new ClassType(ctns, ctname);
            m.addClassType(ct);
            // added to modelURI2xobj in identifyCSCtypes
        }
    }
    
    // We have all the Datatype objects and can create DataProperty objects
    // from attribute declarations.
    private void createPropertiesFromAttributes () {
        var xMap = xs.getComponents(ATTRIBUTE_DECLARATION);
        for (int i = 0; i < xMap.getLength(); i++) {
            var xadec  = (XSAttributeDeclaration)xMap.item(i); 
            var xstype = xadec.getTypeDefinition();
            var schE   = getSchemaDocElement(xadec);
            var appi   = getAppinfoAttributes(schE);
            var propU  = xObjToURI(xadec);
            var pnsU   = xadec.getNamespace();
            var pns    = m.namespaceObj(pnsU);
            var pname  = xadec.getName();
            var dp     = new DataProperty(pns, pname);
            populateComponent(dp, xadec, schE);
            dp.setIsAttribute(true);
            dp.setIsRefAttribute("true".equals(appi.getOrDefault("referenceAttributeIndicator", "")));
            dp.setIsRelationship("true".equals(appi.getOrDefault("relationshipPropertyIndicator", "")));
            var typeU = xObjToURI(xstype);
            var dt    = m.uriToDatatype(typeU);
            if (null == dt) dt = createXSDPrimitive(xstype);
            dp.setDatatype(dt);
            m.addDataProperty(dp);
        }
    }
    
    // The model has (unpopulated) ClassType objects and (complete) Datatype objects,
    // so now we can create ObjectProperty and DataProperty objects from element
    // declarations
    private void createPropertiesFromElements () {
        var xMap = xs.getComponents(ELEMENT_DECLARATION);
        for (int i = 0; i < xMap.getLength(); i++) {
            var xedec  = (XSElementDeclaration)xMap.item(i);
            var xtype  = xedec.getTypeDefinition();
            var xsub   = xedec.getSubstitutionGroupAffiliation();
            var schE   = getSchemaDocElement(xedec);
            var appi   = getAppinfoAttributes(schE);
            if (COMPLEX_TYPE != xtype.getTypeCategory()) continue;  // NIEM elements must have complex type
            Property p;
            var xctype = (XSComplexTypeDefinition)xtype;
            var propU  = xObjToURI(xedec);
            var pnsU   = xedec.getNamespace();
            var pname  = xedec.getName();
            var pns    = m.namespaceObj(pnsU);
            if (CONTENTTYPE_SIMPLE == xctype.getContentType()) {
                var dp     = new DataProperty(pns, pname);
                var stypeU = xObjToURI(xctype.getSimpleType());
                var dt     = m.uriToDatatype(stypeU);
                dp.setDatatype(dt);
                m.addDataProperty(dp);
                p = dp;
            }
            else {
                var op = new ObjectProperty(pns, pname);
                var ctypeU = xObjToURI(xctype);
                var ct = m.uriToClassType(ctypeU);
                op.setClassType(ct);
                op.setReferenceCode(appi.getOrDefault("referenceCode", ""));
                m.addObjectProperty(op);
                p = op;
            }
            if (null != xsub) {
                var subU = xObjToURI(xsub);
                var subP = m.uriToProperty(subU);
                p.setSubproperty(subP);
            }
            populateComponent(p, xedec, schE);
            p.setIsAbstract(xedec.getAbstract());
            p.setIsRelationship("true".equals(appi.getOrDefault("relationshipPropertyIndicator", "")));
        }
    }
    
    private void createLiteralProperties () {
        for (var lcU : literalClassUs) {
            var xctype = (XSComplexTypeDefinition)modelURI2xobj.get(lcU); 
            var xstype = xctype.getSimpleType();
            var schE   = getSchemaDocElement(xctype);
            var lpdtU  = xObjToURI(xstype);             // literal property datatype URI
            var lpdt   = m.uriToDatatype(lpdtU);        // datatype object
            var lcname = m.compUToName(lcU);            // FooType
            var lpname = lcname.replaceAll("Type$", "Literal");     // FooLiteral
            var lpns   = m.compUToNamespaceObj(lcU);                // namespace object for http://someNS/
            var lp     = new DataProperty(lpns, lpname);
            populateComponent(lp, xctype, schE);        // use CSC doc and appinfo
            lp.setDatatype(lpdt);
            m.addDataProperty(lp);
        }
    }
    
    // Returns the Datatype object corresponding to the XSSimpleTypeDefinition from
    // a xs:list or xs:union type.  
    private Datatype getDatatypeFromXStype (XSSimpleTypeDefinition xstype) {
        var typeU = xObjToURI(xstype);          // http://someNS/FooSimpleType
        var tnsU  = xstype.getNamespace();      // http://someNS/
        var dt    = m.uriToDatatype(typeU);
        if (null == dt) {
            if (W3C_XML_SCHEMA_NS_URI.equals(tnsU)) dt = createXSDPrimitive(xstype);
            else {
                typeU = typeU.replaceAll("SimpleType$", "Type");
                dt = m.uriToDatatype(typeU);
            }
        }
        return dt;
    }
        
    // We only create Datatype objects for XSD primitives when they are used.
    private Datatype createXSDPrimitive (XSSimpleTypeDefinition xstype) {
        var compU = xObjToURI(xstype);
        var dt    = m.uriToDatatype(compU);
        if (null != dt) return dt;
        var cname = m.compUToName(compU);
        var curi  = m.compUToNamespaceU(compU);
        var cNS   = m.namespaceObj(curi);
        dt = new Datatype(cNS, cname);
        m.addDatatype(dt);
        return dt;
    }
    
    // Returns the set of URIS for the model attributes in a complex type definition.
    // Does not include structures attributes. 
    private Set<String> collectAttributes (XSComplexTypeDefinition xctype) {
        var res   = new HashSet<String>();
        var xobjL = xctype.getAttributeUses();
        for (int i = 0; i < xobjL.size(); i++) {
            var xattu = (XSAttributeUse)xobjL.item(i);
            var xatt  = xattu.getAttrDeclaration();
            var nsuri = xatt.getNamespace();
            if (!sch.isModelNamespace(nsuri)) continue;
            res.add(xObjToURI(xatt));
        }
        // Xerces gives us every attribute in the type derivation chain, not just those
        // defined in this particular complex type.  So walk the type derivation chain
        // to remove attributes defined in the base type(s).
        var xbt = xctype.getBaseType();
        while (null != xbt) {
            if (COMPLEX_TYPE != xbt.getType()) break;
            var xbct = (XSComplexTypeDefinition)xbt;
            xobjL = xbct.getAttributeUses();
            for (int i = 0; i < xobjL.size(); i++) {
                var xattu = (XSAttributeUse)xobjL.item(i);
                var xatt  = xattu.getAttrDeclaration();
                res.remove(xObjToURI(xatt));
            }            
            xbt = xbt.getBaseType();
        }
        return res;
    }

    // Returns the component URI corresponding to an XSObject
    private String xObjToURI (XSObject xo) {
        if (null == xo) return "";
        if (xo.getNamespace().endsWith("/")) return xo.getNamespace() + xo.getName();
        return xo.getNamespace() + "/" + xo.getName();
    }
    
    // Returns a list of the children of all the xs:appinfo children
    // for a schema document element
    private List<Element> getAppinfoChildren (Element e) {
        var res = new ArrayList<Element>();
        var appiNL = e.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "appinfo");
        for (int i = 0; i < appiNL.getLength(); i++) {
            var appE = (Element)appiNL.item(i);
            res.add(appE);
        }
        return res;
    }
    
    // Returns all the appinfo attributes for a schema document element.
    private Map<String,String> getAppinfoAttributes (Element e) {
        var res  = new HashMap<String,String>();
        var docE = e.getOwnerDocument();
        var root = docE.getDocumentElement();
        var targ = root.getAttribute("targetNamespace");
        var sd   = sch.schemaDocument(targ);
        var appU = sd.builtinNS("APPINFO");
        var attL = e.getAttributes();
        for (int i = 0; i < attL.getLength(); i++) {
            var att    = (Attr)attL.item(i);
            var attnsU = att.getNamespaceURI();
            if (null != attnsU && attnsU.equals(appU)) {
                var aval  = att.getValue();
                var aname = att.getName();
                var indx  = aname.indexOf(":");
                if (indx > 0 && indx < aname.length()-1) aname = aname.substring(indx+1);
                res.put(aname, aval);
            }
        }
        return res;
    }
    
    // Returns an Element object with the same name as the XSObject, from the schema 
    // document for the XSObject's namespace.
    private Element getSchemaDocElement (XSObject xobj) {
        var nsuri = xobj.getNamespace();
        var name  = xobj.getName();
        var sd    = sch.schemaDocument(nsuri);
        var xpe   = "/*/*[@name='" + name + "']";
        var e     = (Element)sd.evalForOneNode(xpe);
        return e;
    }
    
    // Returns a DOM object for an annotation retrieved from Xerces, suitable
    // for XPath evaluation.
    private Document xsAnnotationToDOM (XSAnnotation xann) {
        try {
            var db  = ParserBootstrap.docBuilder();
            var doc  = db.newDocument();
            xann.writeAnnotation(doc, W3C_DOM_DOCUMENT);
            return doc;
        } catch (ParserConfigurationException ex) {
            LOG.error("internal parser error: {}", ex.getMessage());
            return null;
        }
    }
    
    // These three methods return the XSObject corresponding to a component URI.
    // Accounts for namespace URIs that don't end in "/" (grrr.)
    private XSAttributeDeclaration uriToXSAttribute (String uri) {
        int indx = uri.lastIndexOf("/");
        if (indx < 0 || indx >= uri.length()) return null;
        var nsuri = uri.substring(0, indx);
        var name  = uri.substring(indx+1);
        var xobj  = xs.getAttributeDeclaration(name, nsuri);
        if (null == xobj) xobj = xs.getAttributeDeclaration(name, nsuri.substring(0, nsuri.length()-1));
        return xobj;
    }    
    private XSElementDeclaration uriToXSElement (String uri) {
        int indx = uri.lastIndexOf("/");
        if (indx < 0 || indx >= uri.length()) return null;
        var nsuri = uri.substring(0, indx);
        var name  = uri.substring(indx+1);
        var xobj  = xs.getElementDeclaration(name, nsuri);
        if (null == xobj) xobj = xs.getElementDeclaration(name, nsuri.substring(0, nsuri.length()-1));
        return xobj;
    }
    private XSTypeDefinition uriToXSType (String uri) {
        int indx = uri.lastIndexOf("/");
        if (indx < 0 || indx >= uri.length()) return null;
        var nsuri = uri.substring(0, indx);
        var name  = uri.substring(indx+1);
        var xobj  = xs.getTypeDefinition(name, nsuri);
        if (null == xobj) xobj = xs.getTypeDefinition(name, nsuri.substring(0, nsuri.length()-1));
        return xobj;
    }
    
}
