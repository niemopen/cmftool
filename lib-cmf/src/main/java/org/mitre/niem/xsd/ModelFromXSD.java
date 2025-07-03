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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_SIMPLE;
import static org.apache.xerces.xs.XSConstants.ATTRIBUTE_DECLARATION;
import static org.apache.xerces.xs.XSConstants.TYPE_DEFINITION;
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
import static org.apache.xerces.xs.XSTypeDefinition.SIMPLE_TYPE;
import org.mitre.niem.cmf.AnyProperty;
import org.mitre.niem.cmf.AugmentRecord;
import org.mitre.niem.cmf.CMFException;
import static org.mitre.niem.cmf.CMFObject.CMF_LIST;
import static org.mitre.niem.cmf.CMFObject.CMF_RESTRICTION;
import static org.mitre.niem.cmf.CMFObject.CMF_UNION;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.CodeListBinding;
import org.mitre.niem.cmf.Component;
import static org.mitre.niem.cmf.Component.makeURI;
import static org.mitre.niem.cmf.Component.qnToName;
import static org.mitre.niem.cmf.Component.qnToPrefix;
import static org.mitre.niem.cmf.Component.uriToNamespace;
import org.mitre.niem.cmf.DataProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.ListType;
import org.mitre.niem.cmf.LocalTerm;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.PropertyAssociation;
import org.mitre.niem.cmf.Restriction;
import org.mitre.niem.cmf.Union;
import org.mitre.niem.utility.MapToList;
import static org.mitre.niem.xml.XMLSchemaDocument.evalForNodes;
import static org.mitre.niem.xml.XMLSchemaDocument.evalForString;
import static org.mitre.niem.xml.XMLSchemaDocument.getDocumentation;
import static org.mitre.niem.xml.XMLSchemaDocument.getLanguageString;
import org.mitre.niem.xml.XMLSchemaException;
import org.mitre.niem.xml.Xerces;
import static org.mitre.niem.xsd.NIEMSchemaDocument.qnToURI;
//import static org.mitre.niem.xsd.NIEMSchemaDocument.qnToName;
//import static org.mitre.niem.xsd.NIEMSchemaDocument.qnToPrefix;
//import static org.mitre.niem.xsd.NIEMSchemaDocument.qnToURI;
import static org.mitre.niem.xsd.NamespaceKind.*;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import static org.w3c.dom.Node.ELEMENT_NODE;

/**
 * A class to create a Model object from a NIEM XSD schema. Not reusable.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSD {
    static final Logger LOG = LogManager.getLogger(ModelFromXSD.class);
    
    private Model m = null;
    private NIEMSchema sch = null;
    private XSModel xs = null;

    private XPathExpression XPR_ANYATT = null;
    private XPathExpression XPR_BASE = null;
    private XPathExpression XPR_CHILDREN = null;
    
    public ModelFromXSD () throws XMLSchemaException {
        var XPF = XPathFactory.newInstance();
        try {
            XPR_ANYATT   = XPF.newXPath().compile(".//*[local-name()='anyAttribute']");
            XPR_BASE     = XPF.newXPath().compile("(.//*[@base != ''])[1]/@base");
            XPR_CHILDREN = XPF.newXPath().compile(
                ".//*[(local-name()='attribute' or local-name()='element' or local-name()='any' or local-name() = 'anyAttribute')]");
        } catch (XPathExpressionException ex) {
            throw new XMLSchemaException(ex.getMessage());
        }
     }
    
    public Model createModel (String... args) throws XMLSchemaException, CMFException {
        sch = new NIEMSchema(args);
        return createModel(sch);
    }
    
    public Model createModel (NIEMSchema s) throws CMFException {
        m = new Model();
        sch = s;
        xs = sch.xsmodel();
        buildElementLists();                // the component elements in model schema documents
        createNamespaces();                 // an object for each model namespace
        processAugmentRecords();            // note each augmented class and any global augs
        processTypeDefinitions();           // now we can distinguish datatypes from literal classes
        initializeDatatypes();              // now we can create Datatype objects
        populateDatatypes();                // and with all Datatypes in hand, we can fill them out
        initializeClassTypes();             // create ClassType objects for CCC types and literalClass types
        createPropertiesFromAttributes();   // attribute properties can be created in one pass
        initializePropertiesFromElements(); // element properties require the init/populate 2-step
        populatePropertiesFromElements();   // now all subproperty objects are initalized
        createLiteralProperties();
        populateClassTypes();
        createAugmentRecords();
        return m;
    }
    
    // Construct a list of top-level type definition and component declaration
    // elements for each schema document.  Also collect the appinfo attributes for
    // all defs and decls in the schema.
//    private Map<String,List<Element>> types         = new HashMap<>();  // list of type defns in namespace
    private MapToList<String,Element> types         = new MapToList<>();
    private Map<String,List<Element>> elements      = new HashMap<>();  // list of element decls in namespace
    private Map<String,List<Element>> attributes    = new HashMap<>();  // list of attribute decls in namespace
    private Map<String,Element> comp2Element        = new HashMap<>();  // component URI -> sdoc Element
    private Map<String,Map<String,String>> appinfo  = new HashMap<>();  // component URI -> appinfo map
    private void buildElementLists () {
        for (var sd : sch.schemaDocL()) {
            var vers  = sd.niemVersion();
            var nsU   = sd.targetNamespace();
//            var tL    = new ArrayList<Element>();   // type definitions
            var eL    = new ArrayList<Element>();   // element declarations
            var aL    = new ArrayList<Element>();   // attribute declarations
            var appU = builtinNSU(vers, "APPINFO"); // appinfo namespace URI in this document
//            types.put(nsU, tL);
            elements.put(nsU, eL);
            attributes.put(nsU, aL);
            var root  = sd.dom().getDocumentElement();
            var ndsl = root.getChildNodes();
            for (int i = 0; i < ndsl.getLength(); i++) {
                var node = ndsl.item(i);
                if (ELEMENT_NODE != node.getNodeType()) continue;
                var schE  = (Element)node;
                var name  = schE.getAttribute("name");
                if (name.isEmpty()) continue;
                var compU = makeURI(nsU, name);
                comp2Element.put(compU, schE);
                var ln = node.getLocalName();
                switch(node.getLocalName()) {
                    case "complexType":
//                    case "simpleType": tL.add(schE); break;
                    case "simpleType": types.add(nsU, schE); break;
                    case "element":    eL.add(schE); break;
                    case "attribute":  aL.add(schE); break;
                }
                var appi = getAppinfoAttributes(schE, appU);
                appinfo.put(compU, appi);
            }
        }
    }
    
    // Create and populate Namespace objects for all the model namespaces.
    // The XSD namespace is automatically part of every model.  The XML namespace
    // is added later on demand.
    private void createNamespaces () throws CMFException {
        for (var sd : sch.schemaDocL()) {
            var kind = sch.namespaceKind(sd);
            var kcode = kindToCode(kind);
            if (!isModelKind(kind) && NSK_EXTERNAL != kind) continue;
            var nsU = sd.targetNamespace();
            var pre = sch.namespaceMap().getPrefix(nsU);
            var ns  = new Namespace(pre, nsU);
            ns.setDocumentFilePath(sch.docFilePath(sd));
            ns.setKindCode(kcode);
            ns.setVersion(sd.version());
            ns.setNIEMVersion(sd.niemVersion());
            ns.setLanguage(sd.language());      
            ns.setConformanceTargets(sd.ctAssertions());
            ns.docL().addAll(sd.documentation());
            for (var imp : sd.importElements()) {
                for (var ls : imp.docL())
                    ns.addImportDocumentation(imp.nsU(), ls);
            }
            createLocalTerms(ns, sd);
            m.addNamespace(ns);
        }
    }
    
    private void createLocalTerms (Namespace ns, NIEMSchemaDocument sd) {
        var vers = sd.niemVersion();
        var appU = builtinNSU(vers, "APPINFO");
        var root = sd.documentElement();
        var sns  = root.getChildNodes();
        for (int i = 0; i < sns.getLength(); i++) {
            if (ELEMENT_NODE != sns.item(i).getNodeType()) continue;
            if (!"annotation".equals(sns.item(i).getLocalName())) continue;
            var annE = (Element)sns.item(i);           
            var anns = annE.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "appinfo");
            for (int j = 0; j < anns.getLength(); j++) {
                var appE = (Element)anns.item(j);
                var apns = appE.getElementsByTagNameNS(appU, "LocalTerm");
                for (int k = 0; k < apns.getLength(); k++) {
                    var ltE  = (Element)apns.item(k);
                    var srcs = ltE.getAttribute("sourceURIs");
                    var lt   = new LocalTerm();
                    lt.setTerm(ltE.getAttribute("term"));
                    lt.setLiteral((ltE.getAttribute("literal")));
                    lt.setDocumentation(ltE.getAttribute("definition"));
                    if (!srcs.isEmpty()) {
                        for (var s : srcs.split("\\s+"))
                            lt.addSource(s);
                    }
                    var stxs = ltE.getElementsByTagNameNS(appU, "SourceText");
                    for (int l = 0; l < stxs.getLength(); l++) {
                        var stxE = (Element)stxs.item(l);
                        var stx  = getLanguageString(stxE);
                        lt.addCitation(stx);
                    }
                    ns.addLocalTerm(lt);
                }  
            }
        }
    }
    
    // Process the appinfo:Augmentation elements in all the namespaces.  
    // In most, the augmenting property will be an attribute.  
    // If the augmented type has simple content, the augmenting property can 
    // also be an ObjectProperty -- and then the message schema will contain a
    // reference attribute.
    private final Map<String,List<Element>> augments = new HashMap<>(); // nsU -> appinfo:Augmentation elements
    private final Set<String> attAugTypeUS           = new HashSet<>(); // set of types with att augmentations
    private boolean anyGlobalLitAugF = false;
    
    private void processAugmentRecords () {
        for (var ns : m.namespaceSet()) {
            var nsU   = ns.uri();
            var sd    = sch.schemaDocument(nsU);
            if (null == sd) continue;
            var vers  = sd.niemVersion();
            var appU  = builtinNSU(vers, "APPINFO");
            var root  = sd.documentElement();
            var augNL = root.getElementsByTagNameNS(appU, "Augmentation");
            var aeL   = new ArrayList<Element>();
            augments.put(nsU, aeL);
            for (int i = 0; i < augNL.getLength(); i++)  {
                var augE  = (Element)augNL.item(i);                
                var typeQ = augE.getAttribute("class");
                var codes = augE.getAttribute("globalClassCode");
                aeL.add(augE);
                if (codes.isBlank()) {
                    var typeU = m.qnToURI(typeQ);
                    var tnsU  = uriToNamespace(typeU);
                    var tname  = qnToName(typeQ);
                    var xctype = xs.getTypeDefinition(tname, tnsU);
                    attAugTypeUS.add(typeU);
                }
                else {
                    var codeL = codes.trim().split("\\s+");
                    for (var code : codeL) {
                        if ("LITERAL".equals(code)) anyGlobalLitAugF = true;
                    }
                }
            }
        }
    }
    
    // Process all of the type definitions.  Some will turn into datatype objects,
    // some into literal class objects, some into ordinary class objects.
    
    private static final Set<String> EMPTY_STRING_SET = new HashSet<>();
    private final Map<String,XSSimpleTypeDefinition> dtU2xstype = new HashMap<>();  // datatype uri -> XSSimpleType object
    private final Map<String,String> stU2dtU                    = new HashMap<>();  // FooSimpleType renamed FooType
    private final Set<String> datatypeUs                        = new HashSet<>();  // datatype URIs
    private final Set<String> cscClassUs                        = new HashSet<>();  // class URIs from CSC types
    private final Set<String> litClassUs                        = new HashSet<>();  // literal class URIs
    private final Set<String> simpleDtUs                        = new HashSet<>();  // FooSimpleType datatypes
    private final Map<String,String> baseTypeU                  = new HashMap<>();  // base of this type
    private final Map<String,Set<String>> hasBaseUs             = new HashMap<>();  // types with this base
    private final Map<String,String> litPropTypeU               = new HashMap<>();  // literal prop uri -> datatype uri

    private void processTypeDefinitions () {
        var sTUs   = new HashSet<String>();                     // simple type definition URIs
        var cscTUs = new HashSet<String>();                     // complex type with simple content URIs
        var tU2XS  = new HashMap<String,XSTypeDefinition>();    // type URI -> XSmodel object
        for (var sd : sch.schemaDocL()) {
            if (!sch.isModelNamespace(sd)) continue;
            var nsU  = sd.targetNamespace();            // namespace uri
            var tL   = types.get(nsU);                  // type definition elements in this document
            for (var schE : tL) {
                var name  = schE.getAttribute("name");
                var tU    = makeURI(nsU, name);
                var xtype = xs.getTypeDefinition(name, nsU);
                var baseQ = evalForString(schE, XPR_BASE);
                var baseU = qnToURI(schE, baseQ);
                tU2XS.put(tU, xtype);
                baseTypeU.put(tU, baseU);
                if (sch.isModelComponentU(baseU)) addToStringSetMap(hasBaseUs, baseU, tU);
                if ("simpleType".equals(schE.getLocalName())) {
                    sTUs.add(tU);
                    dtU2xstype.put(tU, (XSSimpleTypeDefinition)xtype);
                }
                else {
                    var xctype = (XSComplexTypeDefinition)xtype;
                    var contp  = xctype.getContentType();
                    if (CONTENTTYPE_SIMPLE == contp) cscTUs.add(tU);
                }
            }
        }
        // Divide CSCs into datatypes and possible literal classes
        var maybeLitTUs = new HashSet<String>();        // possible literal class uris
        for (var cscU : cscTUs) {
            var schE   = comp2Element.get(cscU);
            var xtype  = tU2XS.get(cscU);
            var xctype = (XSComplexTypeDefinition)xtype;
            var appi   = appinfo.get(cscU);
            var rc     = appi.getOrDefault("referenceCode", "");
            var litF   = "ANY".equals(rc) || "REF".equals(rc) || "URI".equals(rc);
            if (!litF) litF = hasAttributes(xctype);
            if (!litF) litF = attAugTypeUS.contains(cscU);
            if (!litF) litF = anyGlobalLitAugF;
            if (!litF) {
                var anyAtt = evalForNodes(schE, XPR_ANYATT);
                litF = anyAtt.getLength() > 0;
            }
            if (litF) {
                cscClassUs.add(cscU);
                maybeLitTUs.add(cscU);
            }
            else {
                datatypeUs.add(cscU);
                dtU2xstype.put(cscU, xctype.getSimpleType());
            }
        }
        // Three cases for each FooSimpleType:
        // #1: The schema has a CSC datatype wrapping the simple type.  Use that instead of FooSimpleType.
        // #2: The schema doesn't have a class named FooType.  Create a FooType datatype and use it instead.
        // #3: The model must have a FooSimpleType datatype.
        for (var stU : sTUs) {
            var xstype = dtU2xstype.get(stU);
            var cscU  = replaceSuffix(stU, "SimpleType", "Type");
            var wrapU = "";
            var stnsU = sch.uriToNamespaceU(stU);
            var hbUs  = hasBaseUs.getOrDefault(stU, EMPTY_STRING_SET);
            for (var hbU : hbUs) {
                if (!datatypeUs.contains(hbU)) continue;
                var hbnsU = sch.uriToNamespaceU(hbU);
                if (wrapU.isEmpty() || hbnsU.equals(stnsU)) wrapU = hbU;
                if (hbU.equals(cscU)) wrapU = hbU;
            }
            // Case #1: Use the best existing WrapType that wraps FooSimpleType
            if (!wrapU.isEmpty()) {
                stU2dtU.put(stU, wrapU);        // use WrapType instead of FooSimpleType
                dtU2xstype.put(wrapU, xstype);  // build WrapType from FooSimpleType definition
            } 
            // Case #2: Schema doesn't have a FooType, create one for this simple type definition
            else if (!comp2Element.containsKey(cscU)) {
                stU2dtU.put(stU, cscU);                         // use FooType instead of FooSimpleType
                dtU2xstype.put(cscU, xstype);                   // build FooType from FooSimpleType defn
                comp2Element.put(cscU, comp2Element.get(stU));  // remember xs:simpleType element
                baseTypeU.put(cscU, baseTypeU.get(stU));        // get @base from xs:simpleType
                appinfo.put(cscU, appinfo.get(stU));            // get appinfo from xs:simpleType
                datatypeUs.add(cscU);
            }
            // Case #3: Model has a FooType class, must also have a FooSimpleType datatype
            else {
                datatypeUs.add(stU);
                simpleDtUs.add(stU);
            }
        }
        // Handle literal class and all classes derived from them.
        // A class derived from a literal class is not a literal class.
        for (var cscU : maybeLitTUs) {
            var baseU = baseTypeU.get(cscU);
            if (maybeLitTUs.contains(baseU)) {  // is this CSC derived from a literal class?
                cscClassUs.add(cscU);           // if so, create an ordinary class for this CSC
            }
            // Figure out the datatype for the literal property
            else {
                var xctype = (XSComplexTypeDefinition)tU2XS.get(cscU);
                var xstype = xctype.getSimpleType();
                var wrapU  = stU2dtU.get(baseU);
                litClassUs.add(cscU);
                if (simpleDtUs.contains(baseU))      litPropTypeU.put(cscU, baseU);
                else if (null != wrapU)              litPropTypeU.put(cscU, wrapU);
                else if (datatypeUs.contains(baseU)) litPropTypeU.put(cscU, baseU);
                else if (isEmptyXStype(xstype))      litPropTypeU.put(cscU, baseU);
                else {
                    // CSC FooType is a restriction with facets of a proxy type.
                    // Create FooSimpleType for that restriction.
                    var stU = replaceSuffix(cscU, "Type", "SimpleType");
                    comp2Element.put(stU, comp2Element.get(cscU));
                    appinfo.put(stU, appinfo.get(cscU));
                    baseTypeU.put(stU, baseU);
                    dtU2xstype.put(stU, xstype);
                    datatypeUs.add(stU);
                    simpleDtUs.add(stU);
                    litPropTypeU.put(cscU, stU);                    
                }
            }
        }
    }
 
    // Create the right kind of datatype object (Restriction, List, Union)
    // and add to the model.  Objects are empty now, populated later.
    private void initializeDatatypes () {
        for (var dtU : datatypeUs) {
            Datatype dt = null;
            var dtns  = m.compUToNamespaceObj(dtU);    // datatype namespace object
            var name  = sch.uriToLocalName(dtU);
            var xstype = dtU2xstype.get(dtU);
            switch(xstype.getVariety()) {
            case VARIETY_ATOMIC: dt = new Restriction(dtns, name); break;
            case VARIETY_LIST:   dt = new ListType(dtns, name); break;
            case VARIETY_UNION:  dt = new Union(dtns, name); break;
            }
            m.addDatatype(dt);            
        }   
    }
    
    // If we tried to do this in one pass, we would wind up trying to populate
    // <xs:list itemType="foo:BarType"> before we had the object for foo:BarType.
    // Two passes is so much easier than some complicated recursive approach!
    private void populateDatatypes () throws CMFException {    
        for (var dtU : datatypeUs) {
            if (!sch.isModelComponentU(dtU)) continue;  // init but don't populate external datatypes
            var dt     = m.uriToDatatype(dtU);
            var nsU    = dt.namespaceURI();
            var sd     = sch.schemaDocument(nsU);
            var vers   = sd.niemVersion();
            var clsaU  = builtinNSU(vers, "CLSA");           // code list appinfo URI
            var schE   = comp2Element.get(dtU);
            var xstype = dtU2xstype.get(dtU);
            var appi   = appinfo.get(dtU);
            populateComponent(dt, schE, appi);
            switch(dt.getType()) {
            case CMF_LIST:        populateListType((ListType)dt, xstype, appi); break;
            case CMF_RESTRICTION: populateRestriction((Restriction)dt, xstype, sd, schE, clsaU); break;
            case CMF_UNION:       populateUnion((Union)dt, xstype); break;
            }            
        }
    }

    // Populates documentation and isDeprecated fields for all Component objects.
    // Look for xs:documentation children of xs:annotation children.
    private void populateComponent (Component c, Element schE, Map<String,String>appi) {
        var docL = getDocumentation(schE);
        var dep  = appi.getOrDefault("deprecated", "");
        c.setDocumentation(docL);
        c.setIsDeprecated("true".equals(dep));
    }

    // Populate a ListType object with appinfo from its CSC type definition and
    // the list item type from its simple type definition.
    private void populateListType (ListType lt, XSSimpleTypeDefinition xstype, Map<String,String>appi) throws CMFException {
        var ltname = lt.name();
        var oFlag = appi.getOrDefault("orderedPropertyIndicator", "");
        var xitem = xstype.getItemType();       // list item simple type def from XS ST def
        var itemU = xObjToURI(xitem);           // URI for list item 
        var idt   = getDatatype(itemU);
        if (null == idt)
            throw new CMFException(
                String.format("can't find item datatype %s for list datatype %s", itemU, lt.uri()));
        lt.setItemType(idt);
        if (null != oFlag) lt.setIsOrdered("true".equals(oFlag));
    }
    
    // Populate a Restriction object with appinfo from its CSC type definition,
    // including CLSA appinfo. Populate the facet list from its XS simple type 
    // definition. Get the base type from the schema document element.
    private void populateRestriction (
            Restriction r,                          // populate this object
            XSSimpleTypeDefinition xstype,          // XS typedef object for datatype
            NIEMSchemaDocument sd,                  // schema document with type definition
            Element schE,                           // complexType element in schema doc
            String clsaU)                           // CLSA namespace URI in this schema doc
        throws CMFException {
        // Handle code list schema appinfo
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
            fobj.setCategory(fcode);
            fobj.setValue(fval);
            fobj.setDocumentation(docL);
            r.addFacet(fobj);
            hasF = true;
        }
        // Pattern and enumeration facets (multi-value facets) are different.
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
                fobj.setCategory(fcode);
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
        Collections.sort(r.facetL());
        
        // Determine the base datatype of this restriction.
        // Case #1: @base is FooSimpleType without a wrapper
        // Case #2: @base is FooType or wrapped FooSimpleType
        // Case #3: @base is proxy or primitive
        Datatype dt;
        var rU    = r.uri();
        var baseU = baseTypeU.get(rU);
        var wrapU = stU2dtU.getOrDefault(baseU, "");
        var xbase = xstype.getBaseType();

        // Case #1: @base is a FooSimpleType that is a datatype
        if (simpleDtUs.contains(baseU)) {
            dt = m.uriToDatatype(baseU);
        }
        else if (wrapU.equals(rU)) {
            baseU = xObjToURI(xbase);
            dt = getDatatype(baseU);
        }
        // Case #2: @base is a FooSimpleType that has a wrapper
        else if (!wrapU.isEmpty()) {
            dt = m.uriToDatatype(wrapU);
        }
        // Case #3: @base is a FooType
        else if (sch.isModelComponentU(baseU)) {
            dt = m.uriToDatatype(baseU);
        }
        // Case #4: @base is a proxy or primitive
        else dt = getDatatype(baseU);


        var dtname = dt.qname();
        r.setBase(dt);
        // Get restriction base QN from schema document.  
        // If it's a SimpleType QN and not in the model, then get the base from xstype.
//        var rU = r.uri();
//        var baseU = baseTypeU.get(rU);
//        var renU  = stU2dtU.getOrDefault(baseU, baseU);
//        var bdt   = getDatatype(renU);
//        if (null != renU && renU.equals(rU)) {
//            var xbtype = (XSSimpleTypeDefinition)xstype.getBaseType();
//            baseU = xObjToURI(xbtype);
//        }
//        else if (null != renU && renU.endsWith("SimpleType")) {
//            var sE = tU2element.get(renU);
//            var bQ = sd.evalForString(sE, XPR_BASE);
//            baseU = qnToURI(sE, bQ);
//            int x = 0;
//        }
//        var bdt = getDatatype(baseU);
//        if (null == bdt && baseQ.endsWith("SimpleType")) {
//            var xbtype = xstype.getBaseType();
//            if (SIMPLE_TYPE == xbtype.getTypeCategory()) {
//                var xbstype = (XSSimpleTypeDefinition)xbtype;
//                bdt = getDatatypeFromXStype(xbstype);
//            }
//        }
    }
    
    // Populates a Union object with appinfo from its CSC type definition plus
    // the member types from its simple type definition.
    private void populateUnion (Union u, XSSimpleTypeDefinition xstype) throws CMFException {
        var xmbrL = xstype.getMemberTypes();
        for (var i = 0; i < xmbrL.getLength(); i++) {
            var xmbr = (XSSimpleTypeDefinition)xmbrL.item(i);
            var mbrU = xObjToURI(xmbr);
            var mdt  = getDatatype(mbrU);
            if (null == mdt)
                throw new CMFException(
                    String.format("can't find union datatype %s for union datatype %s", mbrU, u.uri()));
            u.addMember(mdt);
        }
    }
    
    // Create a ClassType object for each model CCC type definition, and also for
    // the literal classes identified earlier.  Augmentation types aren't going to
    // be part of the model, but we create and add them here, use and remove them later.
    private final Set<String> allClassUs = new HashSet<>();
    private void initializeClassTypes () {
        for (var ns : m.namespaceSet()) {
            if (!isModelKind(codeToKind(ns.kindCode()))) continue;
            var nsU = ns.uri();
            var tL  = types.get(nsU);
            if (null == tL) continue;
            for (var e : tL) {
                if (!"complexType".equals(e.getLocalName())) continue;
                var name   = e.getAttribute("name");
                var xtype  = xs.getTypeDefinition(name, nsU);
                var xctype = (XSComplexTypeDefinition)xtype;
                if (CONTENTTYPE_SIMPLE == xctype.getContentType()) continue;
                var ct   = new ClassType(ns, name);
                m.addClassType(ct);
                allClassUs.add(ct.uri());
            }
        }
        // CCC types handled, now do the CSC types becoming ClassType objects
        for (var ctypeU : cscClassUs) {
            var ctns   = m.compUToNamespaceObj(ctypeU);
            var ctname = m.compUToName(ctypeU);
            var ct     = new ClassType(ctns, ctname);
            m.addClassType(ct);
            allClassUs.add(ct.uri());
        }        
    }
    
    // We have all the Datatype objects and can create DataProperty objects
    // from attribute declarations in model namespaces.  We also create objects
    // for attributes declared in extension namespaces, but will add them to the
    // model later, if and only if they are referenced in a model class.
    // Iterate over attribute declaration elements in the schema documents to 
    // efficiently get the appinfo attributes.
    private final Map<String,Property> uri2externalProp = new HashMap<>();
    private void createPropertiesFromAttributes () {
        for (var sd : sch.schemaDocL()) { 
            if (!sch.isModelNamespace(sd) && !sch.isExternal(sd)) continue;
            var nsU  = sd.targetNamespace();               // namespace URI
            var ns   = m.namespaceObj(nsU);                // namespace object
            var vers = sd.niemVersion();
            var appU = builtinNSU(vers, "APPINFO");        // appinfo ns URI in this document
            var aL   = attributes.get(nsU);
            for (var schE : aL) {
                var name = schE.getAttribute("name");
                var dp   = new DataProperty(ns, name);
                var xadec = uriToXSAttribute(dp.uri());
                var xstype = xadec.getTypeDefinition();
                var appi  = getAppinfoAttributes(schE, appU);
                populateComponent(dp, schE, appi);
                dp.setIsAttribute(true);
                dp.setIsRefAttribute("true".equals(appi.getOrDefault("referenceAttributeIndicator", "")));
                dp.setIsRelationship("true".equals(appi.getOrDefault("relationshipPropertyIndicator", "")));
                var typeU = xObjToURI(xstype);
                var dt    = getDatatype(typeU);
                dp.setDatatype(dt);
                if (sch.isExternal(nsU)) uri2externalProp.put(dp.uri(), dp);
                else m.addDataProperty(dp);
            }
        }
    }
    
    // You might think we're now ready to populate all the Property objects.
    // But we have to ensure we have the Property object when we process
    // the substitutionGroup.  So it's the two-phase thing again.
    // We create property objects for element declarations in external namespaces
    // but don't add them to the model yet.  Also keep track of augmentations 
    // and augmentation points, we use and remove them from the model later.
    private final Set<ObjectProperty> globalAugS = new HashSet<>();
    private final List<ObjectProperty> augPropL = new ArrayList<>();
    private final List<ObjectProperty> augPointL = new ArrayList<>();
    private void initializePropertiesFromElements () {
        for (var sd : sch.schemaDocL()) { 
            if (sch.isModelNamespace(sd)) {
                var nsU  = sd.targetNamespace();               // namespace URI
                var ns   = m.namespaceObj(nsU);                // namespace object
                var eL   = elements.get(nsU);
                for (var schE : eL) {
                    var name  = schE.getAttribute("name");
                    var typeQ = schE.getAttribute("type");
                    var typeU = schemaQNToURI(schE, typeQ);
                    if (typeU.isEmpty() || allClassUs.contains(typeU)) {
                        var op = new ObjectProperty(ns, name);
                        if (name.endsWith("AugmentationPoint")) augPointL.add(op);
                        else if (name.endsWith("Augmentation")) augPropL.add(op);
                        else if (sch.isExternal(nsU)) uri2externalProp.put(op.uri(), op);
                        m.addObjectProperty(op);    // augmentation components are removed from model later                    
                    }
                    else {
                        var dp = new DataProperty(ns, name);
                        if (sch.isExternal(nsU)) uri2externalProp.put(dp.uri(), dp);
                        else m.addDataProperty(dp);                    
                    }           
                }
            }
            else if (sch.isExternal(sd)) {
                var nsU  = sd.targetNamespace();               // namespace URI
                var ns   = m.namespaceObj(nsU);                // namespace object
                var eL   = elements.get(nsU);
                for (var schE : eL) {
                    var name  = schE.getAttribute("name");
                    var xel   = xs.getElementDeclaration(name, nsU);
                    var xtype = xel.getTypeDefinition();
                    Property p;
                    if (SIMPLE_TYPE == xtype.getTypeCategory()) p = new DataProperty(ns, name);
                    else {
                        var xctype = (XSComplexTypeDefinition)xtype;
                        var xct    = xctype.getContentType();
                        if (CONTENTTYPE_SIMPLE == xct) p = new DataProperty(ns, name);
                        else p = new ObjectProperty(ns, name);
                    }
                    uri2externalProp.put(p.uri(), p);
                }                
            }
        }
    }
 
    
    // The model has (unpopulated) ClassType objects and (complete) Datatype objects,
    // so now we can create ObjectProperty and DataProperty objects from element
    // declarations.  Properties from external namespaces don't get populated because
    // they are just placeholders in CMF.
    private void populatePropertiesFromElements () {
         for (var sd : sch.schemaDocL()) {
            if (!sch.isModelNamespace(sd)) continue;
            var nsU  = sd.targetNamespace();
            var ns   = m.namespaceObj(nsU);
            var vers = sd.niemVersion();
            var appU = builtinNSU(vers, "APPINFO");
            var eL   = elements.get(nsU);
              for (var schE : eL) {
                var appi  = getAppinfoAttributes(schE, appU);
                var name  = schE.getAttribute("name");
                var typeQ = schE.getAttribute("type");
                var typeU = schemaQNToURI(schE, typeQ);
                var subQ  = schE.getAttribute("substitutionGroup");
                var propU = makeURI(nsU, name);

                Property p;
                if (typeU.isEmpty() || allClassUs.contains(typeU)) {
                    var op = m.uriToObjectProperty(propU);
                    var ct = m.uriToClassType(typeU);
                    op.setClassType(ct);
                    op.setReferenceCode(appi.getOrDefault("referenceCode", ""));
                    p = op;                    
                }
                else {
                    var dp = m.uriToDataProperty(propU);
                    var dt = getDatatype(typeU);
                    dp.setDatatype(dt);
                    p = dp;                      
                }
                if (!subQ.isEmpty()) {
                    var subU   = schemaQNToURI(schE, subQ);
                    var subnsU = uriToNamespace(subU);
                    if (NSK_STRUCTURES == NamespaceKind.namespaceToKind(subnsU)) {
                        globalAugS.add((ObjectProperty)p);
                    }
                    else {
                        var subp = m.uriToProperty(subU);
                        p.setSubproperty(subp);
                    }
                }
                var xedec = xs.getElementDeclaration(name, nsU);
                populateComponent(p, schE, appi);
                p.setIsAbstract(xedec.getAbstract());
                p.setIsRelationship("true".equals(appi.getOrDefault("relationshipPropertyIndicator", "")));
            }
        }
    }
    
    // Create the literal property for each literal class.  
    // Once created, add to the literal class object.
    private void createLiteralProperties () {
        for (var ltU : litClassUs) {
            var ct     = m.uriToClassType(ltU);
            var schE   = comp2Element.get(ltU);
            var dtU    = litPropTypeU.get(ltU);
            var dt     = getDatatype(dtU);
            var lpname = replaceSuffix(ct.name(), "Type", "Literal");
            var lp     = new DataProperty(ct.namespace(), lpname);
            var appi   = new HashMap<String,String>();
            populateComponent(lp, schE, appi);
            lp.setDatatype(dt);
            m.addDataProperty(lp);
            var pa = new PropertyAssociation();
            pa.setProperty(lp);
            pa.setMaxOccurs("1");
            pa.setMinOccurs("1");
            ct.addPropertyAssociation(pa);
        }
    }
    
    // Populate the ClassType objects from the xs:complexType element in the
    // schema document.  Can't get everything needed from the XSModel objects,
    // or correlate them with with the schema DOM.  Does make you wonder if using
    // the Xerces XML Schema API was a mistake.  Not going to rewrite all that now!
    private void populateClassTypes () {
        for (var sd : sch.schemaDocL()) {
            if (!sch.isModelNamespace(sd)) continue;
            var nsU  = sd.targetNamespace();               // namespace URI
            var vers = sd.niemVersion();
            var appU = builtinNSU(vers, "APPINFO");        // appinfo ns URI in this document
            var tL   = types.get(nsU);
            for (var schE : tL) {
                var name = schE.getAttribute("name");
                var ctU  = makeURI(nsU, name);
                if (!allClassUs.contains(ctU)) continue;
                var ct   = m.uriToClassType(ctU);
                if (null == ct) {
                    LOG.error("Model is missing ClassType {}", ctU);
                    continue;
                }
                var appi   = getAppinfoAttributes(schE, appU);
                var xctype = uriToXSCType(ctU);
                var xbase  = xctype.getBaseType();
                var baseU  = xObjToURI(xbase);
                var basect = m.uriToClassType(baseU);
                populateComponent(ct, schE, appi);
                if (null != basect) ct.setSubclass(basect);
                ct.setIsAbstract(xctype.getAbstract());
                ct.setReferenceCode(appi.getOrDefault("referenceCode", ""));
            
                var nodeL  = sd.evalForNodes(schE, XPR_CHILDREN);
                for (int j = 0; j < nodeL.getLength(); j++) {
                    var e = (Element)nodeL.item(j);
                    var eref = e.getAttribute("ref");
                    var docL = getDocumentation(e);
                    var min  = e.getAttribute("minOccurs");
                    var max  = e.getAttribute("maxOccurs");
                    var use  = e.getAttribute("use");

                    switch (e.getLocalName()) {
                    case "attribute":
                    case "element":
                        Property prop = null;
                        var ref  = e.getAttribute("ref");
                        var refU = sd.qnToURI(e, ref);
                        if (ref.startsWith("xml:")) prop = getXMLproperty(ref);
                        else if (uri2externalProp.containsKey(refU)) {
                            prop = uri2externalProp.get(refU);
                            m.addProperty(prop);
                        }
                        else {
                            if (refU.isEmpty()) { 
                                LOG.error("can't find QName {} in complex type {}", ref, ctU);
                                break;
                            }
                            var rname  = qnToName(ref);
                            var refnsU = m.compUToNamespaceU(refU);
                            if (refU.endsWith("AugmentationPoint")) break;
                            if (!sch.isModelNamespace(refnsU)) break;
                            prop = m.uriToProperty(refU);
                        }
                        var cpa  = new PropertyAssociation();
                        cpa.setProperty(prop);
                        cpa.setDocumentation(docL);
                        if ("attribute".equals(e.getLocalName())) {
                            cpa.setMaxOccurs("1");
                            cpa.setMinOccurs("required".equals(use) ? "1" : "0");
                        }
                        else {
                            var eapi = getAppinfoAttributes(e, appU);
                            var ord  = eapi.getOrDefault("orderedPropertyIndicator", "");
                            if (!max.isBlank()) cpa.setMaxOccurs(max);
                            if (!min.isBlank()) cpa.setMinOccurs(min);
                            cpa.setIsOrdered("true".equals(eapi.getOrDefault("orderedPropertyIndicator", "")));
                        }
                        ct.propL().add(cpa);
                        break;
                    case "any":                 
                    case "anyAttribute":
                        var proc = e.getAttribute("processContents");
                        var ncon = e.getAttribute("namespace");
                        var ap   = new AnyProperty();
                        if (!max.isBlank()) ap.setMaxOccurs(max);
                        if (!min.isBlank()) ap.setMinOccurs(min);
                        ap.setProcessCode(proc);
                        ap.setNsConstraint(ncon);
                        ap.setIsAttribute("anyAttribute".equals(e.getLocalName()));
                        ct.addAnyProperty(ap);
                        break;
                    }
                }
            }
        }
    }
    
    // Turn augmentation elements and augmentation types into AugmentRecord objects
    // belonging to the appropriate Namespace.  Remove all augmentation components
    // (FooAugmentation, FooAugmentationPoint, FooAugmentationType) from the model.
    private static Set VALID_CODES = Set.of("ASSOCIATION", "OBJECT", "LITERAL");
    private void createAugmentRecords () {
        for (var aprop : augPropL) {
            ClassType atype = null;
            String gcode = null;          
            var augt   = aprop.classType();             // augmentation type; eg. j:EducationAugmentationType
            var augns  = augt.namespace();              // augmenting namespace object      
            if (globalAugS.contains(aprop)) {
                var name = aprop.name();
                if (name.startsWith("Object")) gcode = "OBJECT";
                if (name.startsWith("Association")) gcode = "ASSOCIATION";
            }
            else {
                var augp = aprop.subPropertyOf();       // augmentation point property
                var augU = augp.uri();                  // augmentation point URI; eg. nc:EducationAugmentationPoint
                var augmtU = replaceSuffix(augU, "AugmentationPoint", "Type"); // augmented class URI
                atype  = m.uriToClassType(augmtU);      // augmented ClassType object                
            }
            var index  = 0;
            for (var cpa : augt.propL()) {
                var pname = cpa.property().qname();
                var arec = new AugmentRecord(cpa);
                arec.setClassType(atype);
                arec.addCode(gcode);
                if (cpa.property().isAttribute()) arec.setIndex("-1");
                else arec.setIndex(Integer.toString(index++));
                augns.addAugmentRecord(arec);
            }
            m.removeObjectProperty(aprop);
        }
        // Handle elements with augmentation point substitutionGroup
        for (var p : m.propertyL()) {
            var apoint = p.subPropertyOf();
            if (null == apoint) continue;
            if (!apoint.name().endsWith("AugmentationPoint")) continue;
            var augmtU = replaceSuffix(apoint.uri(), "AugmentationPoint", "Type");
            var atype  = m.uriToClassType(augmtU);
            var augns = p.namespace();
            var arec = new AugmentRecord();
            arec.setClassType(atype);
            arec.setProperty(p);
            arec.setMinOccurs("0");
            arec.setMaxOccurs("unbounded");
            augns.addAugmentRecord(arec);
            p.setSubproperty(null);
        }
        for (var op: augPointL) m.removeObjectProperty(op);
        for (var ct: m.classTypeL())
            if (ct.name().endsWith("AugmentationType"))
                m.removeClassType(ct);
        
        // Now create augmentation records from appinfo:Augmentation elements
        for (var ns : m.namespaceSet()) {
            var nsU = ns.uri();
            var sd  = sch.schemaDocument(nsU);
            var aeL = augments.get(nsU);
            if (null == aeL) continue;
            for (var ae : aeL) {
                var classQ = ae.getAttribute("class");
                var propQ  = ae.getAttribute("property");
                var use    = ae.getAttribute("use");
                var codes  = ae.getAttribute("globalClassCode");
                var classU = sd.qnToURI(ae, classQ);
                var propU  = sd.qnToURI(ae, propQ);
                var ct     = m.uriToClassType(classU);
                var p      = m.uriToProperty(propU);
                var codeL  = new String[0];
                if (!codes.isEmpty()) codeL  = codes.split("\\s+");
                
                // Let's do a little error checking!
                var badProp  = (null == p);
                var badClass = (!classQ.isEmpty() && null == ct);
                var badCodes = false;
                for (var code : codeL) if (!VALID_CODES.contains(code)) badCodes = true;
                if (badProp || badClass || badCodes) {
                    var aeStr = "<Augmentation property=\"" + propQ + "\"";
                    if (!classQ.isEmpty()) aeStr = aeStr + " class=\"" + classQ + "\"";
                    if (!codes.isEmpty())  aeStr = aeStr + " globalClassCode=\"" + codes + "\"";      
                    aeStr = aeStr + ">";
                    if (badProp)  LOG.warn("{}: unknown property in {}", nsU, aeStr);
                    if (badClass) LOG.warn("{}: unknown class in {}", nsU, aeStr);
                    if (badCodes) LOG.warn("{}: unknown globalClassCode in {}", nsU, aeStr);
                }
                var arec   = new AugmentRecord();
                arec.setClassType(ct);
                arec.setProperty(p);
                arec.setMaxOccurs("1");
                arec.setMinOccurs("required".equals(use) ? "1" : "0");
                if (!codes.isEmpty())
                    for (int i = 0; i < codeL.length; i++) arec.addCode(codeL[i]);
                ns.addAugmentRecord(arec);
            }
        }
    }

    // Turns a datatype URI into a Datatype object.  Proxy URIs are turned into
    // the XSD equivalent.  XSD and XML datatype objects aren't created in the 
    // model until they are referenced. The XML namespace isn't added to the model
    // until an XML component is referenced, and that happens here.
    private Datatype getDatatype (String dtU) {
        var dt   = m.uriToDatatype(dtU);
        var renU = stU2dtU.get(dtU);
        if (null != dt) return dt;
        if (null != renU) {
            dt = m.uriToDatatype(renU);
            return dt;
        }
        // Turn proxy to primitive; create primitive if necessary
        var name = sch.uriToLocalName(dtU);
        var nsU  = sch.uriToNamespaceU(dtU);
        var ns   = m.namespaceObj(nsU);
        if (NSK_NIEM_XS == sch.namespaceKind(nsU)) {
            nsU = W3C_XML_SCHEMA_NS_URI;
            ns  = m.namespaceObj(nsU);
        }
        if (W3C_XML_SCHEMA_NS_URI.equals(nsU)) {
            dt = new Datatype(ns, name);
        }
        m.addDatatype(dt);
        return dt;        
    }
    
    // We only create DataProperty objects for XML attributes when they are used.
    // We also create the XML namespace here if needed.
    private DataProperty getXMLproperty (String qname) {
        var xmlns = m.namespaceObj(XML_NS_URI);
        if (null == xmlns) {
            var sd = sch.schemaDocument(XML_NS_URI);
            xmlns = new Namespace("xml", XML_NS_URI);
            xmlns.setKindCode("XML");
            if (null != sd) xmlns.setDocumentFilePath(sch.docFilePath(sd));
            try { m.addNamespace(xmlns); } catch (CMFException ex) { } // CAN'T HAPPEN
        }
        var dp = m.qnToDataProperty(qname);
        if (null == dp) {
            var name = qnToName(qname);
            dp = new DataProperty(xmlns, name);
            dp.setIsAttribute(true);
            m.addDataProperty(dp);
        }
        return dp;
    }
    
    // Returns true if the XSSimpleType object contains a model facet.
    // Default Xerces facets don't count.
    private boolean hasModelFacet (XSSimpleTypeDefinition xstype) {
        var flst = xstype.getFacets();
        for (int i = 0; i < flst.getLength(); i++) {
            var f = (XSFacet)flst.item(i);
            if (!Xerces.isDefaultFacet(xstype, f)) return true;
        }
        var mvflst = xstype.getMultiValueFacets();
        for (int i = 0; i < mvflst.getLength(); i++) {
            var mvf  = (XSMultiValueFacet)mvflst.item(i);
            var fvls = mvf.getLexicalFacetValues();
            for (int j = 0; j < fvls.getLength(); j++) {
                var fval = fvls.item(j);
                if (!Xerces.isDefaultFacet(xstype, mvf.getFacetKind(), fval)) return true;
            }
        }
        return false;
    }

    // Turns a QName into a component URI, using the namespace bindings in the
    // scope of the eleemnt.
    private String schemaQNToURI (Element e, String qn) {
        
        if (qn.isEmpty()) return "";
        var en   = e.getAttribute("name");
        var pre  = qnToPrefix(qn);
        var name = qnToName(qn);
        var nsU  = e.lookupNamespaceURI(pre);
        if (nsU.endsWith("/")) return nsU + name;
        return nsU + "/" + name;
    }
    
    // Returns true if this complex type has or inherits attributes
    // that are not in a structures namespace.
    private boolean hasAttributes (XSComplexTypeDefinition xctype) {
        var xobjL = xctype.getAttributeUses();
        for (int i = 0; i < xobjL.size(); i++) {
            var xattu = (XSAttributeUse)xobjL.item(i);
            var xatt  = xattu.getAttrDeclaration();
            var nsuri = xatt.getNamespace();
            if (NSK_STRUCTURES != NamespaceKind.namespaceToKind(nsuri)) return true;        
        }
        return false;
    }

    // Returns false if this XSTypeDefinition is a list, or a union, or has
    // model facets.
    private boolean isEmptyXStype (XSSimpleTypeDefinition xst) {
        var tvar = xst.getVariety();
        if (VARIETY_LIST == tvar) return false;
        if (VARIETY_UNION == tvar) return false;
        if (hasModelFacet(xst)) return false;
        return true;
    }

    // Returns the component URI corresponding to an XSObject.  
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
    
    // Returns all the appinfo attributes for a schema document element;
    // used eg. if (map.getOrDefault("orderedPropertyIndicator", "") == "true") ...
    
    private final Set<String> knownAppinfoS = Set.of(
        "augmentingNamespace",
        "deprecated",
        "externalImportIndicator",
        "orderedPropertyIndicator",
        "referenceCode",
        "referenceAttributeIndicator",
        "relationshipPropertyIndicator"
    );
    private Map<String,String> getAppinfoAttributes (Element e, String appU) {
        var res  = new HashMap<String,String>();
        var attL = e.getAttributes();
        for (int i = 0; i < attL.getLength(); i++) {
            var att    = (Attr)attL.item(i);
            var attnsU = att.getNamespaceURI();
            if (null != attnsU && attnsU.equals(appU)) {
                var aval  = att.getValue();
                var aname = att.getName();
                var indx  = aname.indexOf(":");
                if (indx > 0 && indx < aname.length()-1) aname = aname.substring(indx+1);
                if (!knownAppinfoS.contains(aname))
                    LOG.warn("ignored unknown appinfo:{} attribute", aname);
                res.put(aname, aval);
            }
        }
        return res;
    }

    // These three methods return the XSObject corresponding to a component URI.
    // Accounts for namespace URIs that don't end in "/" (grrr.)
    private XSAttributeDeclaration uriToXSAttribute (String uri) {
        int indx = uri.lastIndexOf("/");
        if (indx < 0 || indx >= uri.length()) return null;
        var nsuri = uri.substring(0, indx+1);
        var name  = uri.substring(indx+1);
        var xobj  = xs.getAttributeDeclaration(name, nsuri);
        if (null == xobj) xobj = xs.getAttributeDeclaration(name, nsuri.substring(0, nsuri.length()-1));
        return xobj;
    }    
    private XSTypeDefinition uriToXSType (String uri) {
        int indx = uri.lastIndexOf("/");
        if (indx < 0 || indx >= uri.length()) return null;
        var nsuri = uri.substring(0, indx+1);
        var name  = uri.substring(indx+1);
        var xobj  = xs.getTypeDefinition(name, nsuri);
        if (null == xobj) xobj = xs.getTypeDefinition(name, nsuri.substring(0, nsuri.length()-1));
        return xobj;
    }
    private XSComplexTypeDefinition uriToXSCType (String uri) {
        return (XSComplexTypeDefinition)uriToXSType(uri);
    }
    
    public static void addToStringSetMap (Map<String,Set<String>> map, String key, String val) {
        var set = map.get(key);
        if (null == set) {
            set = new HashSet<>();
            map.put(key, set);
        }
        set.add(val);        
    }
    
    public static String replaceSuffix (String s, String oSuf, String nSuf) {
        if (s.endsWith(oSuf))
            return s.substring(0, s.length() - oSuf.length()) + nSuf;
        return s;
    }
    
    private void dumpXSD () {
        var nsmap = sch.namespaceMap();
        var tmap  = xs.getComponents(TYPE_DEFINITION);
        for (int i = 0; i < tmap.getLength(); i++) {
            var xtype = (XSTypeDefinition)tmap.item(i);
            var xbase = xtype.getBaseType();
            if (null == xbase) continue;
            var name  = xtype.getName();
            var nsU   = xtype.getNamespace();
            var nspre = nsmap.getPrefix(nsU);
            var qn    = nspre + ":" + name;
            var bname = xbase.getName();
            if (null == bname) continue;
            var bU    = xbase.getNamespace();
            var bpre  = nsmap.getPrefix(bU);
            var bqn   = bpre + ":" + bname;
            if (bname.endsWith("SimpleType")) {
                System.out.println(String.format("%-40s %s", bqn, qn));
            }
        }
        tmap = xs.getComponents(ATTRIBUTE_DECLARATION);
        for (int i = 0; i < tmap.getLength(); i++) {
            var xatt = (XSAttributeDeclaration)tmap.item(i);
            var xstype = xatt.getTypeDefinition();
            var name = xatt.getName();
            var nsU  = xatt.getNamespace();
            var nspre = nsmap.getPrefix(nsU);
            var qn    = nspre + ":" + name;
            var bname = xstype.getName();
            if (null == bname) continue;
            var bU    = xstype.getNamespace();
            var bpre  = nsmap.getPrefix(bU);
            var bqn   = bpre + ":" + bname;
            if (bname.endsWith("SimpleType")) {
                System.out.println(String.format("%-40s %s", bqn, qn));
            }        }
        System.exit(0);
    }
}
