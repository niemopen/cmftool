/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2024 The MITRE Corporation.
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

import java.io.IOException;
import java.io.StringReader;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_SIMPLE;
import static org.apache.xerces.xs.XSConstants.ATTRIBUTE_DECLARATION;
import static org.apache.xerces.xs.XSConstants.DERIVATION_RESTRICTION;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.FACET;
import static org.apache.xerces.xs.XSConstants.MODEL_GROUP;
import static org.apache.xerces.xs.XSConstants.MULTIVALUE_FACET;
import static org.apache.xerces.xs.XSConstants.PARTICLE;
import static org.apache.xerces.xs.XSConstants.TYPE_DEFINITION;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSFacet;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSMultiValueFacet;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.*;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import static org.apache.xerces.xs.XSTypeDefinition.COMPLEX_TYPE;
import static org.apache.xerces.xs.XSTypeDefinition.SIMPLE_TYPE;
import org.mitre.niem.cmf.AugmentRecord;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.RestrictionOf;
import org.mitre.niem.cmf.UnionOf;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.CodeListBinding;
import org.mitre.niem.cmf.NamespaceKind;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CLSA;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_PROXY;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_STRUCTURES;
import static org.mitre.niem.cmf.NamespaceKind.NSK_BUILTIN;
import static org.mitre.niem.cmf.NamespaceKind.NSK_OTHERNIEM;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UNKNOWN;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XML;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XSD;
import static org.mitre.niem.cmf.NamespaceKind.NSK_EXTERNAL;
import static org.mitre.niem.cmf.NamespaceKind.uriBuiltinNum;
import org.mitre.niem.cmf.NamespaceMap;
import org.w3c.dom.Element;


/**
 * An object for constructing a CMF Model from a XML Schema document pile.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSD {
    static final Logger LOG = LogManager.getLogger(ModelFromXSD.class);
    
    private Model m = null;                                 // model object under construction
    private NamespaceMap nsmap = null;                      // namespace prefix/URI mapping handler in model
    private Namespace xsdns = null;                         // XSD namespace object in model
    private Namespace xmlns = null;                         // XML namespace object in model (null if no xml: attributes)
    private Namespace structNS = null;                      // structures namespace object in model
    private boolean hasGElementAug = false;                 // true if model has global element augmentations
    private boolean hasGAttributeAug = false;               // true if model has global attribute augmentations
    private XMLSchema s = null;                             // schema from which we are creating the model
    private XSModel xs = null;                              // from the XMLSchema object    
    private Map<String,XMLSchemaDocument> sdoc = null;      // map nsURI -> XMLSchemaDocument object from XMLSchema object
    
    // Global appinfo is a map of component QName -> list of appinfo records
    // Reference appinfo is a double map: Complex type QName,  Element/Attribute ref QName -> list of appinfo records
    private Map<String,List<AppinfoAttribute>> globalAppinfo = null;
    private Map<String,Map<String,List<AppinfoAttribute>>> refAppinfo = null;
    
    // Remember the XS objects from which we initialized the CMF objects
    private Map<Property,XSObject> propertyXSobj = null;               // initialized CMF property paired with schema object
    private Map<Datatype,XSTypeDefinition> datatypeXSobj = null;       // initialized CMF datatype paired with schema object
    private Map<ClassType,XSComplexTypeDefinition> classXSobj = null;  // initialized CMF classtype paired with schema object
    private Map<String,Datatype> datatypeMap = null;                   // map QName -> initialized Datatype object
    private Set<ClassType> hasLiteralProperty = null;                  // FooType class objects requiring a FooLiteral property
    private Set<Property> referencedProps = null;                      // some Class has this Property
    
    
    public ModelFromXSD () { }
    
    public Model createModel (String ... args) 
            throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException  {
        XMLSchema s = new XMLSchema(args);
        return createModel(s);
    }
    
    public Model createModel (XMLSchema sch) 
            throws SAXException, ParserConfigurationException, IOException, XMLSchema.XMLSchemaException, CMFException {
        s      = sch;
        m      = new Model();
        nsmap  = m.namespaceMap();
        xs     = s.xsmodel();           // these methods can throw exceptions
        sdoc   = s.schemaDocuments();   // so let's get it over with here and now
        
        globalAppinfo        = new HashMap<>();
        refAppinfo           = new HashMap<>();
        propertyXSobj        = new HashMap<>();
        datatypeXSobj        = new HashMap<>();
        classXSobj           = new HashMap<>();
        datatypeMap          = new HashMap<>();
        hasLiteralProperty   = new HashSet<>();
        referencedProps      = new HashSet<>();
         
        generateNamespaces();           // normalize namespace prefixes so we can use QNames
        checkGlobalAugmentations();     // global augmentations? create ClassType and Property objects for structures
        processSchemaAnnotations();     // get documentation and local terms from schema annotations
        processAppinfoAttributes();     // index all the appinfo attributes by QName for easy retrieval
        initializeDeclarations();       // create properties for element and attribute declarations
        initializeDefinitions();        // create class and datatype objects for type definitions
        setClassInheritance();          // establish ClassType inheritance
        findClassWithLiteralProperty();
        handleSimpleTypes();
        processClassTypes();            // populate all fields of class objects
        processDatatypes();             // populate all fields of datatype objects
        processProperties();            // populate all fields of property objects from schema
        processElementAugmentations();  // add augmentation elements to augmented class objects
        processAttributeAugmentations();// add augmentation attributes to augmented class objects
        handleGlobalAugmentations();
        return m;
    }
    
    // Create namespace objects for all namespaces in the model.  After this, we
    // have a unique prefix for each namespace, so we can use QNames instead of uri,lname.
    private void generateNamespaces () {
        // Generate namespace prefix map from sorted list of all namespace declarations in schema
        // Prefer prefix bindings from schema documents in this order:
        // 1. extension schema documents
        // 2. NIEM model schema documents (core and domains)
        // 3. external (non-NIEM) schema documents
        // Within a schema document, outer declarations before inner, and then in document order
        // This sort order is part of the XMLNamespaceDeclaration class.
        List<XMLNamespaceDeclaration> allnsd = new ArrayList<>();
        for (var sd : sdoc.values()) {
            allnsd.addAll(sd.namespaceDecls());
        }
        Collections.sort(allnsd);
        for (var nsd : allnsd) nsmap.assignPrefix(nsd.decPrefix(), nsd.decURI());
        
        // Add namespace objects for the namespaces found in schema pile
        // The XML namespace we initialize later, and only if someone uses it
        // NIEM XSD doesn't require a namespace prefix, but CMF does, so make one if necessary.
        sdoc.forEach((nsuri, sd) -> {
            String prefix = nsmap.getPrefix(nsuri);
            if (null == prefix) {
                prefix = bodgeUpAPrefix(nsuri);
                nsmap.assignPrefix(prefix, nsuri);
            }
            int kind = sd.schemaKind();
            if (NSK_XML != kind) {
                Namespace n = new Namespace(prefix, nsuri);
                n.setKind(kind);
                try { m.addNamespace(n); } catch (CMFException ex) { } // CAN'T HAPPEN
                n.setConfTargets(sd.conformanceTargets());
                n.setFilePath(sd.filepath());
                n.setNIEMversion(sd.niemVersion());
                n.setSchemaVersion(sd.schemaVersion());
                n.setLanguage(sd.language());
                LOG.debug("Created namespace {}", nsuri);
                if (NSK_BUILTIN == kind && NIEM_STRUCTURES == uriBuiltinNum(nsuri)) structNS = n;
            }
        });        
        // Add namespace for XSD to model if it isn't already there
        // (Sometimes people import it, sometimes they don't)
        xsdns = m.getNamespaceByURI(W3C_XML_SCHEMA_NS_URI);
        if (null == xsdns) {
            xsdns = new Namespace(nsmap.getPrefix(W3C_XML_SCHEMA_NS_URI), W3C_XML_SCHEMA_NS_URI); // reserved prefix
            xsdns.setKind(NSK_XSD);
            try { m.addNamespace(xsdns); } catch (CMFException ex) { }    // CAN'T HAPPEN
            LOG.debug("Created namespace {}", W3C_XML_SCHEMA_NS_URI);
        }
    }
    
    // We need to know early whether there are any global augmentations,
    // because if so, there are no Datatypes (other than XSD builtins; 
    // everything is a Class.  Create ClassType and Property objects for 
    // structures:AssociationType and ObjectType if needed.
    private void checkGlobalAugmentations () {
        
        // Is any declaration subsitutable for augmentation point in structures?
        var snuri = structNS.getNamespaceURI();
        var xmap  = xs.getComponents(ELEMENT_DECLARATION);
        for (int i = 0; i < xmap.getLength() && !hasGElementAug; i++) {
            var xobj  = (XSElementDeclaration)xmap.item(i);
            var xsbg  = xobj.getSubstitutionGroupAffiliation();
            if (null == xsbg) continue;
            var suri  = xsbg.getNamespace();
            var sname = xsbg.getName();
            if (suri.equals(snuri)) {
                if (sname.endsWith("AugmentationPoint")) hasGElementAug = true;
            }
        }
        // Are there augmentation attributes for ObjectType or AssociationType?
        xmap = xs.getComponentsByNamespace(TYPE_DEFINITION, snuri);
        for (int i = 0; i < xmap.getLength() && !hasGAttributeAug; i++) {
            var xobj  = (XSTypeDefinition)xmap.item(i);
            if (COMPLEX_TYPE != xobj.getTypeCategory()) continue;
            var xctype = (XSComplexTypeDefinition)xobj;
            var xattrs = xctype.getAttributeUses();
            for (int j = 0; j < xattrs.getLength(); j++) {
                var xattr = (XSAttributeUse)xattrs.item(j);
                var xattd = xattr.getAttrDeclaration();
                var anuri = xattd.getNamespace();
                if (!snuri.equals(anuri)) hasGAttributeAug = true;
            }
        }
        // Create Class and Property for global element augmentations.
        // Use them to add augmentation properties to every Class.
        // We won't actually write these into the model.
        if (hasGElementAug) {
            var ct = new ClassType(structNS, "ObjectType");
            var ap = new Property(structNS, "ObjectAugmentationPoint");
            ct.setIsAugmentable(true);
            m.addComponent(ct);
            m.addComponent(ap);

            ct = new ClassType(structNS, "AssociationType");
            ap = new Property(structNS, "AssociationAugmentationPoint");
            ct.setIsAugmentable(true);
            m.addComponent(ct);
            m.addComponent(ap);
        }
    }
    
    // Schema level documentation and appinfo is parsed in the XMLSchemaDocument object
    // Copy it into the Namespace object now.
    private void processSchemaAnnotations () {
        sdoc.forEach((nsuri, sd) -> {
            var ns = m.getNamespaceByURI(nsuri);
            if (null != ns) {
                if (!sd.documentationStrings().isEmpty()) 
                    ns.setDocumentation(sd.documentationStrings().get(0));
                for (var lt : sd.localTerms()) ns.addLocalTerm(lt);
            }
        });       
    }
    
    // Now that we have unique namespace prefixes assigned, we go through all the 
    // appinfo attribute records in all the schema documents and index them by global
    // component QName (and sometimes then element reference QName).  Sure would be
    // nice if appinfo attributes were available through XSModel, but they aren't.
    private void processAppinfoAttributes () {
        sdoc.forEach((ns,sd) -> {
            for (var arec : sd.appinfoAtts()) {
                String cnsuri = arec.componentEQN().getValue0();
                String cln    = arec.componentEQN().getValue1();
                String cqn    = m.getNamespaceByURI(cnsuri).getNamespacePrefix() + ":" + cln;
                
                // Component appinfo; add arec to appinfo list for this component
                if (null == arec.elementEQN()) {
                    var arlist = globalAppinfo.get(cqn);
                    if (null == arlist) {
                        arlist = new ArrayList<>();
                        globalAppinfo.put(cqn, arlist);
                    }
                    arlist.add(arec);
                }
                // Element or attribute reference appinfo; add arec to list for this component and element ref
                else {
                    var ensuri = arec.elementEQN().getValue0();
                    var eln    = arec.elementEQN().getValue1();
                    var ens    = m.getNamespaceByURI(ensuri);
                    if (null == ens) {
                        LOG.warn(String.format("no NIEM namespace for appinfo namespace=\"%s\"", ensuri));
                    }
                    else {
                        String eqn = ens.getNamespacePrefix() + ":" + eln;
                        var eqnmap = refAppinfo.get(cqn);
                        if (null == eqnmap) {
                            eqnmap = new HashMap<>();
                            refAppinfo.put(cqn, eqnmap);
                        }
                        var arlist = eqnmap.get(eqn);
                        if (null == arlist) {
                            arlist = new ArrayList<>();
                            eqnmap.put(eqn, arlist);
                        }
                        arlist.add(arec);
                    }
                }
            }
        });
    }

    // Create property placeholder objects for all the element and attribute declarations 
    // in NIEM-conforming schema documents. Namespace, name, and documentation is 
    // initialized here, and attributes distingushed from elements; the rest happens later. 
    // After this the model contains most of the property objects, but those objects 
    // are incomplete.
    //
    // Sometimes iterating through the XSNamedMap hits the same component more
    // than once. This happens when we create the entire NIEM model by
    // supplying domains/*.xsd and codes/*.xsd to XMLSchema.initialSchemaDocs
    private void initializeDeclarations () {
        HashSet<String>seen = new HashSet<>();
        XSNamedMap xmap = xs.getComponents(ATTRIBUTE_DECLARATION);
        for (int i = 0; i < xmap.getLength(); i++) {
            var xobj  = (XSAttributeDeclaration)xmap.item(i);
            var nsuri = xobj.getNamespace();
            var lname = xobj.getName();
            var clkN  = String.format("{%s}%s", nsuri, lname);
            if (seen.contains(clkN)) continue;      // weird repeats in XSNamedMap
            seen.add(clkN);
            var p = initializeOneDeclaration(xobj, true);
            if (null == p) continue;
            var docs = getDocumentation(xobj);
            if (!docs.isEmpty()) p.setDocumentation(docs.get(0));
        }
        seen = new HashSet<>();
        xmap = xs.getComponents(ELEMENT_DECLARATION);
        for (int i = 0; i < xmap.getLength(); i++) {
            var xobj = (XSElementDeclaration)xmap.item(i);
            var nsuri = xobj.getNamespace();
            var lname = xobj.getName();
            var clkN  = String.format("{%s}%s", nsuri, lname);
            if (seen.contains(clkN)) continue;      // weird repeats in XSNamedMap
            seen.add(clkN);           
            var xet  = xobj.getTypeDefinition();
            var xetu = NamespaceKind.uriBuiltinNum(xet.getNamespace());
            if (NIEM_STRUCTURES == xetu && xet.getName().endsWith("AugmentationType")) {
                LOG.info(String.format("Discarding element %s:%s; type structures:AugmentationType not allowed",
                        nsmap.getPrefix(xobj.getNamespace()), xobj.getName()));
                continue;
            }
            var p    = initializeOneDeclaration(xobj, false);
            if (null == p) continue;
            var docs = getDocumentation(xobj);
            if (!docs.isEmpty()) p.setDocumentation(docs.get(0));
        }        
    }
    
    // Why can't we handle the declaration's documentation here?  Because the
    // XSModel API is different for attribute and element declarations.  Pfui.
    private Property initializeOneDeclaration (XSObject xobj, boolean isAttribute) {
        var nsuri = xobj.getNamespace();
        var lname = xobj.getName();
        int kind  = namespaceKindFromSchema(nsuri);
        if (kind > NSK_OTHERNIEM && kind != NSK_EXTERNAL) return null;
        var ns  = m.getNamespaceByURI(nsuri);
        var p   = new Property(ns, lname);
        p.setIsAttribute(isAttribute);
        p.addToModel(m);
        propertyXSobj.put(p, xobj);
        LOG.debug("initialized property " + p.getQName());     
        return p;
    }
    
    // Create class and datatype placeholder objects for all the type definitions in
    // NIEM-conforming schema documents. Only name and namespace initialized at
    // this point.  Handling annotations and documention happens later.  Adding 
    // datatype objects to the model happens later.  We build a separate map of 
    // QName to datatype object here, then possibly revise some mappings when we 
    // handle FooSimpleType declarations.  We don't create datatype objects for 
    // proxy types at all.  We don't create datatype objects for types in the XSD 
    // namespace until we see we need them.
    //
    // Sometimes iterating through the XSNamedMap hits the same component more
    // than once. This happens when we create the entire NIEM model by
    // supplying domains/*.xsd and codes/*.xsd to XMLSchema.initialSchemaDocs
    private void initializeDefinitions () throws CMFException {     
        HashSet<String>seen = new HashSet<>();
        XSNamedMap xmap = xs.getComponents(TYPE_DEFINITION);
        for (int i = 0; i < xmap.getLength(); i++) {
            var xtype = (XSTypeDefinition)xmap.item(i);
            var xbase = xtype.getBaseType();
            var nsuri = xtype.getNamespace();
            var lname = xtype.getName();
            var clkN  = String.format("{%s}%s", nsuri, lname);
            if (seen.contains(clkN)) continue;      // weird repeats in XSNamedMap
            seen.add(clkN);
            var ns    = m.getNamespaceByURI(nsuri);
            int kind  = namespaceKindFromSchema(nsuri);
            if (kind > NSK_OTHERNIEM) continue;

            // Simple type is always a Datatype object
            if (SIMPLE_TYPE == xtype.getTypeCategory()) {
                var dt   = new Datatype(ns, lname);
                datatypeXSobj.put(dt, xtype);
                datatypeMap.put(dt.getQName(), dt);
                LOG.debug("initialized datatype " + dt.getQName());
            }
            else {
                // Complex type can be a class or a datatype object.
                // Simple content without model attributes is a Datatype object
                // Everything is a class if there are global augmentations.
                boolean hasModelAttributes = false;
                var qname  = ns.getNamespacePrefix() + ":" + lname;
                var xctype = (XSComplexTypeDefinition)xtype;
                var atts   = xctype.getAttributeUses();
                for (int j = 0; j < atts.getLength(); j++) {        // iterate over all attributes, incl. inherited
                    var au    = (XSAttributeUse)atts.item(j);
                    var adecl = au.getAttrDeclaration();
                    var ans   = adecl.getNamespace();
                    if (NIEM_STRUCTURES != NamespaceKind.uriBuiltinNum(ans)) {    // attributes in structures NS don't count
                        hasModelAttributes = true;                          // found a model attribute; we're done
                        break;
                    }
                }
                boolean isSimple = CONTENTTYPE_SIMPLE == xctype.getContentType();
                if (isSimple && !hasModelAttributes) {   // it's a Datatype
                    var dt   = new Datatype(ns, lname);               
                    datatypeXSobj.put(dt, xtype);
                    datatypeMap.put(dt.getQName(), dt);
                    LOG.debug("initialized datatype " + dt.getQName());                  
                }
                // Complex, mixed, empty content is a ClassType object
                // Simple content with model attributes anywhere in derivation is a ClassType object
                else {
                    initializeXMLattributes(xctype);
                    var ct   = new ClassType(ns, lname);                    
                    ct.addToModel(m);
                    classXSobj.put(ct, xctype);
                    LOG.debug("initialized class " + ct.getQName());
                }
            }
        }
    }
    
    // We don't need the XML namespace or any properties for attributes in that
    // namespace unless they are referenced in a NIEM-conforming schema document.
    private void initializeXMLattributes (XSComplexTypeDefinition xctype) throws CMFException {
        XSObjectList atts = xctype.getAttributeUses();
        for (int i = 0; i < atts.getLength(); i++) {                // iterate over all attributes for this type
            var au    = (XSAttributeUse)atts.item(i);
            var adecl = au.getAttrDeclaration();
            var ans   = adecl.getNamespace();
            var aln   = adecl.getName();
            if (!XML_NS_URI.equals(ans)) continue;                  // attribute not in XML namespace; ignore
            if (null == xmlns) {                                    // create the XML namespace, we need it
                String xmlPrefix = nsmap.getPrefix(XML_NS_URI);     // prefix is predefined in the NamespaceMap
                xmlns = new Namespace(xmlPrefix, XML_NS_URI);
                m.addNamespace(xmlns);   
                LOG.debug("Created namespace {}", XML_NS_URI);                
            }
            var p = m.getProperty(ans, aln);
            if (null == p) {
                p = new Property(xmlns, aln);
                p.setIsAttribute(true);
                p.addToModel(m);
                propertyXSobj.put(p, adecl);
                LOG.debug("initialized property " + p.getQName());         
            }
        }
    }
    
    // It is much simpler to establish class inheritance after we have added
    // all the ClassType objects to the model.
    private void setClassInheritance () {
        for (var ct : classXSobj.keySet()) {
            var xctype = classXSobj.get(ct);
            var xbase  = xctype.getBaseType();
            var bct    = m.getClassType(xbase.getNamespace(), xbase.getName());
            if (null != bct) {
                ct.setExtensionOfClass(bct);
                LOG.debug("class " + ct.getQName() + " extends class " + bct.getQName());                
            }
        }
    }
    
    // Find the set of ClassType objects that require a literal property for 
    // their simple content.
    private void findClassWithLiteralProperty () throws CMFException {
        for (var ct : classXSobj.keySet()) {
            var bct    = ct.getExtensionOfClass();
            var xctype = classXSobj.get(ct);
            var xbtype = xctype.getBaseType();
            if (null == xctype.getSimpleType()) continue;   // doesn't have simple content
            if (null != bct) continue;                      // extends some other class with the literal
            hasLiteralProperty.add(ct);
            LOG.debug(ct.getQName() + " needs a literal property");
        }
    }

    // There are no Datatype objects named "FooSimpleType" in CMF.  Four cases:
    // 1. This is a simple type named "FooType" from a message schema.
    // 2. There is no FooType in the schema.
    // 3. There is a FooType with simple content and model attributes.
    // 4. Everything else (FooType CSC with no model attributes).
    // For case #1, process the simpleType object as a Datatype.
    // For case #2, rename FooSimpleType to FooType.
    // For case #3, rename FooSimpleType to FooDatatype.
    // For case #4, replace the content of FooType with content of FooSimpleType,
    // and remove FooSimpleType.
    // Simple types for lists, unions, attributes are a concern for ModelToXSD,
    // not here.
    private void handleSimpleTypes () throws CMFException {
        Map<Datatype,Datatype> replacements = new HashMap<>();      
        for (var sdt : datatypeXSobj.keySet()) {
            var xtype  = datatypeXSobj.get(sdt);
            if (SIMPLE_TYPE != xtype.getTypeCategory()) continue;   // dt is not from xs:simpleType declaration
            
            var sdtqn  = sdt.getQName();                            // "x:FooSimpleType"
            var baseqn = sdtqn.replaceFirst("SimpleType$", "Type"); // "x:FooType"
            var ddtqn  = baseqn.replaceFirst("Type$", "Datatype");  // "x:FooDatatype"
            var ct     = m.getClassType(baseqn);                    // ClassObject named x:FooType, if any
            var dt     = datatypeMap.get(baseqn);                   // Datatype object named x:FooType, if any

            // We are either going to rename Datatype sdt, or replace the content
            // of Datatype dt with the content of sdt.
            boolean replace  = false;
            String rename    = null;
            
            // Case #1: This is a simple type declaration in a message schema document
            // Don't change the type name.
            if (!sdtqn.endsWith("SimpleType")) {
                rename = sdtqn;     // keep as FooType (no change)
            }
            // Case #2: the schema defines x:FooSimpleType but not x:FooType, and there is no
            // schema for a x:FooType class or datatype object.  So we rename the Datatype from 
            // "x:FooSimpleType" to "x:FooType"
            else if (null == ct && null == dt) {
                rename = baseqn;               
            }
            // Case #3: there is a x:FooType class object, and so it has a 
            // x:FooSimpleType literal.  Rename the datatype object from 
            // x:FooSimpleType to "x:FooDatatype"; it will be the datatype of the literal property.
            else if (null != ct) {
                rename = ddtqn;
            }
            // Case #4: the schema defined both x:FooSimpleType and x:FooType, and so
            // x:FooType has simple content and no model attributes.  
            // Rename datatype for x:FooSimpleType to x:FooType.
            else {
                replace = true;
            }
            if (replace) {
                replacements.put(dt, sdt);          
            }
            else {
                var ci  = rename.indexOf(":");
                var np  = rename.substring(0, ci);       // "x"
                var nn  = rename.substring(ci+1);        // "FooDatatype"
                var nns = m.getNamespaceByPrefix(np);
                sdt.setNamespace(nns);
                sdt.setName(nn);           
            }
        }  
        // Can't modify datatypeXSobj while iterating over it, so do replacements now
        replacements.forEach((dt, sdt) -> {
            var xtype = datatypeXSobj.get(sdt);
            datatypeXSobj.put(dt, xtype);
            datatypeXSobj.remove(sdt);
            datatypeMap.put(sdt.getQName(), dt);
        });
    }
 
    // Now we have all of the properties from the schema, all of the classes, 
    // and all of the datatypes except XSD datatypes. Time to complete all
    // the property objects.  Documentation has already been handled.
    // Unreferenced external properties are removed from the model here.
    private void processProperties () throws CMFException {
        for (var p : propertyXSobj.keySet()) {
            if (NSK_EXTERNAL == p.getNamespace().getKind()) {
                if (!referencedProps.contains(p)) m.removeComponent(p);
                continue;
            }
            LOG.debug("processing property " + p.getQName());
            var xobj = propertyXSobj.get(p);
            p.setIsDeprecated(getAppinfoAttribute(p.getQName(), null, "deprecated"));
            p.setIsRefAttribute(getAppinfoAttribute(p.getQName(), null, "referenceAttributeIndicator"));
            p.setIsRelationship(getAppinfoAttribute(p.getQName(), null, "relationshipPropertyIndicator"));            
            if (p.isAttribute()) {
                var xadecl = (XSAttributeDeclaration)xobj;
                var xatype = xadecl.getTypeDefinition();
                var dt     = getDatatype(xatype.getNamespace(), xatype.getName());
                p.setDatatype(dt);
               }
            else {
                var xedecl = (XSElementDeclaration)xobj;
                var xetype = xedecl.getTypeDefinition();
                var xet    = xetype.getNamespace();
                var xetb   = NamespaceKind.uriBuiltinNum(xet);
                if (NIEM_STRUCTURES == xetb) {
                    LOG.warn(String.format("Element %s:%s has type is from structures namespace (not allowed in NIEM 6)",
                        nsmap.getPrefix(xobj.getNamespace()), xobj.getName()));
                }
                else {
                    var pclass = m.getClassType(xetype.getNamespace(), xetype.getName());
                    if (null == pclass) p.setDatatype(getDatatype(xetype.getNamespace(), xetype.getName()));
                    else p.setClassType(pclass);
                    var xesubg = xedecl.getSubstitutionGroupAffiliation();
                    if (null != xesubg) {
                        p.setSubPropertyOf(m.getProperty(xesubg.getNamespace(), xesubg.getName()));
                    }    
                    p.setIsAbstract(xedecl.getAbstract());
                    
                    var nlbf  = xedecl.getNillable();
                    var rcode = getAppinfoAttribute(p.getQName(), null, "referenceCode");
                    if (rcode.isBlank()) {
                        rcode = nlbf ? "ANY" : "NONE";
                    }
                    p.setReferenceCode(rcode);
                }
            }
        }
    }
    
    
    private void handleGlobalAugmentations () {
        
    }
       
    // Time to complete all the class objects.  Documentation is handled here.
    // Special handling for simple content with attributes or metadata: we create 
    // a FooLiteral property to hold the content of an element of FooType.
    private void processClassTypes () throws CMFException {
        for (var ct : classXSobj.keySet()) {
            LOG.debug("processing class " + ct.getQName());
            var ctqn    = ct.getQName();
            var ctns    = ct.getNamespace();                // class namespace object
            var ctnsuri = ctns.getNamespaceURI();           // class namespace URI
            var xctype  = classXSobj.get(ct);               // XSComplexTypeDefinition object
            var xbase   = xctype.getBaseType();             // base type XSTypeDefinition
            var xsbase  = xctype.getSimpleType();           // base type XSSimpleTypeDefinition (possibly null)
            ct.setIsAbstract(xctype.getAbstract());
            ct.setIsDeprecated(getAppinfoAttribute(ct.getQName(), null, "deprecated"));
            ct.setIsExternal("true".equals(getAppinfoAttribute(ct.getQName(), null, "externalAdapterTypeIndicator"))
                                || ct.getName().endsWith("AdapterType"));
            
            var rcode = getAppinfoAttribute(ct.getQName(), null, "referenceCode");
            if (!rcode.isBlank()) ct.setReferenceCode(rcode);

            var docs = getDocumentation(xctype);
            if (!docs.isEmpty()) ct.setDocumentation(docs.get(0));
            
            // Create a FooLiteral property for FooType if needed
            if (hasLiteralProperty.contains(ct)) { 
                var basedt = getDatatype(xbase.getNamespace(), xbase.getName());
                var npbase = replaceSuffix(ct.getName(), "Type", "");
                var npln   = npbase + "Literal";
                int mungct = 0;
                // Already have FooLiteralValue?  GR##!!$^*
                while (null != m.getProperty(ctnsuri, npln))  {
                    npln = npbase + "Literal" + String.format("%02d", mungct++);
                }
                var np   = new Property(ctns, npln);
                var doc  = ct.getDocumentation();
                if (null != doc) {
                    var ndoc = doc.replaceFirst("A data type", "A literal value");
                    np.setDocumentation(ndoc);
                }
                np.setDatatype(basedt);
                np.addToModel(m);
                LOG.debug("created literal property " + np.getQName());
                var hasp = new HasProperty();
                hasp.setProperty(np);
                hasp.setMinOccurs(1);
                hasp.setMaxOccurs(1);
                ct.addHasProperty(hasp);
            }
            // Complex content, set extensionOf and create element property list
            if (null != xbase) {
                var xelrefs = collectClassElements(xctype);
                for (var xp : xelrefs) {
                    XSTerm xpt = xp.getTerm();
                    XSElementDeclaration xed = (XSElementDeclaration) xpt;
                    XSTypeDefinition xetype  = xed.getTypeDefinition();
                    if (xed.getName().endsWith("AugmentationPoint")) {
                        ct.setIsAugmentable(true);
                        continue;
                    }
                    var p = m.getProperty(xed.getNamespace(), xed.getName());
                    if (null != p) {
                        referencedProps.add(p);
                        var hp = new HasProperty();
                        hp.setProperty(p);
                        hp.setMinOccurs(xp.getMinOccurs());
                        if (xp.getMaxOccursUnbounded()) hp.setMaxUnbounded(true);
                        else hp.setMaxOccurs(xp.getMaxOccurs());
                        
                        var refDocs = getDocumentation(xp);
                        if (!refDocs.isEmpty()) hp.setDefinition(refDocs.get(0));
                        
                        var pqn = p.getQName();                        
                        var opi = getAppinfoAttribute(ctqn, pqn, "orderedPropertyIndicator");
                        hp.setOrderedProperties("true".equals(opi));
                        ct.addHasProperty(hp);
                    }
                }
            }
            // Add properties for any attributes
            var xattuses = collectClassAttributes(xctype);
            for (var xatt : xattuses) {
                var xattdecl = xatt.getAttrDeclaration();
                Property p = m.getProperty(xattdecl.getNamespace(), xattdecl.getName());
                HasProperty hp = new HasProperty();
                hp.setProperty(p);
                hp.setMaxOccurs(1);
                if (xatt.getRequired()) hp.setMinOccurs(1);
                else hp.setMinOccurs(0);
                
                var attDocs = getDocumentation(xattdecl);
                if (!attDocs.isEmpty()) hp.setDefinition(attDocs.get(0));
                ct.addHasProperty(hp);
                
                // Handle appinfo:augmentingNamespace FIXME
//                var augNSuri = getAppinfoAttribute(ct.getQName(), p.getQName(), "augmentingNamespace");
//                if (!augNSuri.isEmpty()) {
//                    var augNS = m.getNamespaceByURI(augNSuri);
//                    hp.augmentingNS().add(augNS);
//                    addAugmentRecord(augNS, ct, hp, -1);                   
//                }
            }      
        }
    }    

    // Returns a list of attributes declared by this complex type.  The Xerces API 
    // gives us those attributes plus all attributes inherited from the base types.
    // We have to remove the inherited attributes.     
    private List<XSAttributeUse> collectClassAttributes (XSComplexTypeDefinition xctype) {
        // First build a set of all attribute uses (in this type and in its base types)
        List<XSAttributeUse> aset = new ArrayList<>();
        XSObjectList atl = xctype.getAttributeUses();
        for (int i = 0; i < atl.getLength(); i++) {
            XSAttributeUse au = (XSAttributeUse)atl.item(i);
            XSAttributeDeclaration a = au.getAttrDeclaration();
            // Don't add attributes from the structures namespace
            if (NIEM_STRUCTURES != NamespaceKind.uriBuiltinNum(a.getNamespace())) 
                aset.add(au);
        }
        // Now remove attribute uses in the base types
        XSTypeDefinition base = xctype.getBaseType();
        while (null != base) {
            if (COMPLEX_TYPE != base.getTypeCategory()) base = null;
            else {
                atl = ((XSComplexTypeDefinition) base).getAttributeUses();
                for (int i = 0; i < atl.getLength(); i++) {
                    XSAttributeUse au = (XSAttributeUse) atl.item(i);
                    aset.remove(au);
                }
                base = base.getBaseType();
                if (W3C_XML_SCHEMA_NS_URI.equals(base.getNamespace()) && "anyType".equals(base.getName())) break;
            }
        }        
        return aset;
    }
    
    // Returns a list of elements declared by this complex type.  The Xerces API 
    // gives us those elements plus all elements inherited from the base types.
    // We have to remove the inherited elements.
    private List<XSParticle> collectClassElements (XSComplexTypeDefinition ct) {
        List<XSParticle> el = new ArrayList<>();        // element particles in this type
        List<XSParticle> bl = new ArrayList<>();        // element particles from base types
        XSTypeDefinition base = ct.getBaseType();
        XSParticle par = ct.getParticle();
        collectElements(par, el);                       // all elements in this type & base types 
        
        // Now collect elements from just the base types
        while (null != base) {                          
            if (COMPLEX_TYPE != base.getTypeCategory()) break;
            XSComplexTypeDefinition bct = (XSComplexTypeDefinition)base;
            par = bct.getParticle();
            collectElements(par, bl); 
            base = base.getBaseType();
            if (W3C_XML_SCHEMA_NS_URI.equals(base.getNamespace()) && "anyType".equals(base.getName())) break;            
        }
        // Remove elements in base types from element list
        for (XSParticle p : bl) {
            el.remove(p);
        }
        return el;
    }

    // Recursively descend through model groups, collecting element declarations
    private void collectElements (XSParticle par, List<XSParticle> epars) {
        if (null == par) return;
        XSTerm pt = par.getTerm();
        if (null == pt) return;
        switch (pt.getType()) {
            case ELEMENT_DECLARATION:
                epars.add(par);
                break;
            case MODEL_GROUP:
                XSModelGroup mg = (XSModelGroup)pt;
                XSObjectList objs = mg.getParticles();
                for (int i = 0; i < objs.getLength(); i++) {
                    XSParticle pp = (XSParticle)objs.item(i);
                    collectElements(pp, epars);
                }    
                break;
        }
    }
       
    // Time to complete all the datatype objects.  Documentation and appinfo is
    // handled here.  The schema object is either a simple type, or a complex 
    // type with simple content and no attributes.
    private void processDatatypes () throws CMFException {
        for (var dt : datatypeXSobj.keySet()) {
            LOG.debug("processing datatype " + dt.getQName());
            dt.setIsDeprecated(getAppinfoAttribute(dt.getQName(), null, "deprecated"));
            m.addComponent(dt);
            if (xsdns == dt.getNamespace()) continue;               // nothing more to do for XSD types

            var xtype = datatypeXSobj.get(dt);                      // complex or simple type definition for this Datatype
            var docs  = getDocumentation(xtype);
            if (!docs.isEmpty()) dt.setDocumentation(docs.get(0));
            handleCLSA(dt, xtype);
            var typeNS = xtype.getNamespace();
            var typeLN = xtype.getName();
            
            // Create union, list, or restriction for a simple type
            if (SIMPLE_TYPE == xtype.getTypeCategory()) {
                var xstype = (XSSimpleTypeDefinition)xtype;
                switch (xstype.getVariety()) {
                    case VARIETY_UNION:
                        dt.setUnionOf(createUnionOf(xstype));
                        break;
                    case VARIETY_LIST:
                        var xitem = xstype.getItemType();
                        var listdt = getDatatype(xitem.getNamespace(), xitem.getName());
                        dt.setListOf(listdt);
                        break;
                    default:
                        var xbase = xstype.getBaseType();
                        var r = createRestrictionOf(xbase, xstype);
                        dt.setRestrictionOf(r);
                        break;                
                }
                continue;
            }
            // Handle a complex type -- it will have simple content and no model attributes.
            // It can't be a union or list -- that would be a simple type.
            // Usually we create a datatype that is a restriction of the base type.
            // If the base type is a proxy, create a restriction of the XSD type instead.
            var xctype  = (XSComplexTypeDefinition)xtype;
            var xbaseST = xctype.getSimpleType();
            var xbase   = xctype.getBaseType();

            var baseNSuri = xbase.getNamespace();
            var baseLN    = xbase.getName();
            var baseDT    = getDatatype(baseNSuri, baseLN);
            var isComplexBase = COMPLEX_TYPE == xbase.getTypeCategory();
            var isProxyBase   = NIEM_PROXY == NamespaceKind.uri2Kind(baseNSuri);
            var isRestriction = DERIVATION_RESTRICTION == xctype.getDerivationMethod();
            
            // Handle a restriction with facets
            if (null != xbaseST) {
                var r = createRestrictionOf(xbase, xbaseST);
                dt.setRestrictionOf(r);
            }
            else {
                var r = new RestrictionOf();
                r.setDatatype(baseDT);
                dt.setRestrictionOf(r);
                LOG.debug(String.format("%s is a restriction of %s", dt.getQName(), baseDT.getQName()));
            }
        }
    }
    
    // Look through the children of xs:appinfo for code list schema appinfo (CLSA)
    // elements, and apply them to the Datatype object.
    private void handleCLSA (Datatype dt, XSTypeDefinition xtype) {
        var alist = getAppinfoElements(xtype);
        if (null == alist) return;
        var dtns = dt.getNamespaceURI();
        var sd   = sdoc.get(dtns);
        var arch = sd.niemArch();
        var nver = sd.niemVersion();
        var clsaURI = NamespaceKind.getBuiltinNS(NIEM_CLSA, arch, nver);
        for (var ae : alist) {
            var clsaElements = ae.getElementsByTagNameNS(clsaURI, "SimpleCodeListBinding");
            for (int i = 0; i < clsaElements.getLength(); i++) {
                var clsa = (Element)clsaElements.item(i);
                var col  = clsa.getAttribute("columnName");
                var uri  = clsa.getAttribute("codeListURI");
                var cons = clsa.getAttribute("constrainingIndicator");
                if (uri.isEmpty()) continue;
                var clb = new CodeListBinding();
                if (col.isEmpty()) clb.setColumm("#code");  // see code list specification rule 4-11
                else clb.setColumm(col);
                clb.setURI(uri);
                clb.setIsConstraining("true".equals(cons));
                dt.setCodeListBinding(clb);
                return;
            }
        }
    }
           
    // Construct a UnionOf object from a schema simple type definition.  Member types
    // are already in the model, or are XSD types.
    private UnionOf createUnionOf (XSSimpleTypeDefinition st) throws CMFException {
        UnionOf u = new UnionOf();
        XSObjectList members = st.getMemberTypes();
        if (null == members || members.getLength() < 1) 
            throw new CMFException(String.format("no members in union type {%s}%s??", st.getNamespace(), st.getName()));
        for (int i = 0; i < members.getLength(); i++) {
            XSSimpleTypeDefinition mt = (XSSimpleTypeDefinition)members.item(i);
            Datatype mdt = getDatatype(mt.getNamespace(), mt.getName());
            if (null != mdt) u.addDatatype(mdt);
            LOG.debug("  union includes: {}", mdt.getQName());
        }
        return u;
    }
    
    // Construct a RestrictionOf object from a schema simple type definition.
    private RestrictionOf createRestrictionOf (XSSimpleTypeDefinition xstype) throws CMFException {
        var xstypeName = xstype.getName();
        var xbase = xstype.getBaseType();
        var xbaseName = xbase.getName();
        if (null == xbase || SIMPLE_TYPE != xbase.getTypeCategory())
            throw new CMFException(
                    String.format("schema has no simple base type for restriction of %s#%s", 
                            xstype.getNamespace(), xstype.getName()));
        // Ignore restrictions of xs:anyType
        var bt = getDatatype(xbase.getNamespace(), xbase.getName());
        if (null == bt) return null;
        
        RestrictionOf r = new RestrictionOf();
        r.setDatatype(bt);
        XSObjectList flist = xstype.getFacets();
        for (int i = 0; i < flist.getLength(); i++) {
            var f    = (XSFacet)flist.item(i);
            if (isXercesDefaultFacet(xbase, f.getFacetKind(), f.getLexicalFacetValue())) continue;
            var fval = f.getLexicalFacetValue();
            var fo   = new Facet();
            var docs = getDocumentation (f);
            if (!docs.isEmpty()) fo.setDefinition(docs.get(0));
            fo.setFacetKind(facetKind2Code(f.getFacetKind()));
            fo.setStringVal(f.getLexicalFacetValue());         
            if (null != fo) r.addFacet(fo);
        }
        // Pattern and Extension facets are different.  We get parallel lists, one of
        // facet values, the other of facet annotations.
        flist = xstype.getMultiValueFacets();
        for (int i = 0; i < flist.getLength(); i++) {
            var f     = (XSMultiValueFacet)flist.item(i);
            var fkind = FACET_PATTERN == f.getFacetKind() ? "Pattern" : "Enumeration";
            var vals  = f.getLexicalFacetValues();
            var anns  = getAnnotations(f);
            for (int j = 0; j < vals.getLength(); j++) {
                var val = vals.item(j);
                if (isXercesDefaultFacet(xbase, f.getFacetKind(), val)) continue;
                var fo = new Facet();
                if (j < anns.size() && null != anns.get(j)) {
                    var an   = (XSAnnotation)anns.get(j);
                    var docs = new ArrayList<String>();
                    getDocumentationStrings(docs, an);
                    if (!docs.isEmpty()) fo.setDefinition(docs.get(0));
                }
                fo.setFacetKind(fkind);
                fo.setStringVal(val);
                r.addFacet(fo);
            }
        }
        Collections.sort(r.getFacetList());
        return r;
    }
    
    private RestrictionOf createRestrictionOf (XSTypeDefinition xbase, XSSimpleTypeDefinition xstype) throws CMFException {
        var bt = getDatatype(xbase.getNamespace(), xbase.getName());
        if (null == bt) return null;
        
        RestrictionOf r = new RestrictionOf();
        r.setDatatype(bt);
        XSObjectList flist = xstype.getFacets();
        for (int i = 0; i < flist.getLength(); i++) {
            var f    = (XSFacet)flist.item(i);
            if (isXercesDefaultFacet(xbase, f.getFacetKind(), f.getLexicalFacetValue())) continue;
            var fval = f.getLexicalFacetValue();
            var fo   = new Facet();
            var docs = getDocumentation (f);
            if (!docs.isEmpty()) fo.setDefinition(docs.get(0));
            fo.setFacetKind(facetKind2Code(f.getFacetKind()));
            fo.setStringVal(f.getLexicalFacetValue());         
            if (null != fo) r.addFacet(fo);
        }
        // Pattern and Extension facets are different.  We get parallel lists, one of
        // facet values, the other of facet annotations.
        flist = xstype.getMultiValueFacets();
        for (int i = 0; i < flist.getLength(); i++) {
            var f     = (XSMultiValueFacet)flist.item(i);
            var fkind = FACET_PATTERN == f.getFacetKind() ? "Pattern" : "Enumeration";
            var vals  = f.getLexicalFacetValues();
            var anns  = getAnnotations(f);
            for (int j = 0; j < vals.getLength(); j++) {
                var val = vals.item(j);
                if (isXercesDefaultFacet(xbase, f.getFacetKind(), val)) continue;
                var fo = new Facet();
                if (j < anns.size() && null != anns.get(j)) {
                    var an   = (XSAnnotation)anns.get(j);
                    var docs = new ArrayList<String>();
                    getDocumentationStrings(docs, an);
                    if (!docs.isEmpty()) fo.setDefinition(docs.get(0));
                }
                fo.setFacetKind(fkind);
                fo.setStringVal(val);
                r.addFacet(fo);
            }
        }
        Collections.sort(r.getFacetList());
        return r;        
    }
    
    // Convert Xerces facet kind value to CMF facet kind code.
    private String facetKind2Code (short kind) {
        switch (kind) {
            case FACET_ENUMERATION:     return "Enumeration";
            case FACET_FRACTIONDIGITS:  return "FractionDigits";
            case FACET_LENGTH:          return "Length";
            case FACET_MAXEXCLUSIVE:    return "MaxExclusive";
            case FACET_MAXINCLUSIVE:    return "MaxInclusive";
            case FACET_MAXLENGTH:       return "MaxLength";
            case FACET_MINEXCLUSIVE:    return "MinExclusive";
            case FACET_MININCLUSIVE:    return "MinInclusive";
            case FACET_MINLENGTH:       return "MinLength";
            case FACET_PATTERN:         return "Pattern";
            case FACET_TOTALDIGITS:     return "TotalDigits";
            case FACET_WHITESPACE:      return "WhiteSpace";
            default: 
                LOG.error("Unknown facet kind {}", kind);
                return "";
        }
    }
    
    // Convert CMF facet kind code to Xerces facet kind value.

    private final static Map<String,Short> fCode2Kind = Map.ofEntries(
        entry("Enumeration", FACET_ENUMERATION),
        entry("FractionDigits", FACET_FRACTIONDIGITS),
        entry("Length", FACET_LENGTH),
        entry("MaxExclusive", FACET_MAXEXCLUSIVE),
        entry("MaxInclusive", FACET_MAXINCLUSIVE),
        entry("MaxLength", FACET_MAXLENGTH),
        entry("MinExclusive", FACET_MINEXCLUSIVE),
        entry("MinInclusive", FACET_MININCLUSIVE),
        entry("MinLength", FACET_MINLENGTH),
        entry("Pattern", FACET_PATTERN),
        entry("TotalDigits", FACET_TOTALDIGITS),
        entry("WhiteSpace", FACET_WHITESPACE)
    );
    private short facetCode2Kind (String code) throws CMFException {
        Short rv = fCode2Kind.get(code);
        if (null != rv) return rv;
        throw new CMFException(String.format("unknown facet type '%s'", code));
    }

    // The Xerces schema model object includes facets that do not appear in 
    // the schema document.  These default facets are presumably used to enforce 
    // bultin datatype constraints in a validating parser.  For example
    //    <xs:restriction base="xs:byte">
    // will create four default facets in the schema type definition. We don't
    // want those default facets in the CMF representation.  The following 
    // table and function are used to identify Xerces default facets so that 
    // they can be omitted from CMF.
    private static final String[] xercesFacetData =  {
        "ENTITIES",           "MinLength",             "1",
        "ENTITIES",           "WhiteSpace",            "collapse",
        "ENTITY",             "WhiteSpace",            "collapse",
        "ID",                 "WhiteSpace",            "collapse",
        "IDREF",              "WhiteSpace",            "collapse",
        "IDREFS",             "MinLength",             "1",
        "IDREFS",             "WhiteSpace",            "collapse",
        "NCName",             "Pattern",               "\\i\\c*\"\"[\\i-[:]][\\c-[:]]*",
        "NCName",             "WhiteSpace",            "collapse",
        "NMTOKEN",            "Pattern",               "\\c+",
        "NMTOKEN",            "WhiteSpace",            "collapse",
        "NMTOKENS",           "MinLength",             "1",
        "NMTOKENS",           "WhiteSpace",            "collapse",
        "NOTATION",           "WhiteSpace",            "collapse",
        "Name",               "Pattern",               "\\i\\c*",
        "Name",               "WhiteSpace",            "collapse",
        "QName",              "WhiteSpace",            "collapse",
        "anyURI",             "WhiteSpace",            "collapse",
        "base64Binary",       "WhiteSpace",            "collapse",
        "boolean",            "WhiteSpace",            "collapse",
        "byte",               "FractionDigits",        "0",
        "byte",               "MaxInclusive",          "127",
        "byte",               "MinInclusive",          "-128",
        "byte",               "Pattern",               "[\\-+]?[0-9]+",
        "byte",               "WhiteSpace",            "collapse",
        "date",               "WhiteSpace",            "collapse",
        "dateTime",           "WhiteSpace",            "collapse",
        "decimal",            "WhiteSpace",            "collapse",
        "double",             "WhiteSpace",            "collapse",
        "duration",           "WhiteSpace",            "collapse",
        "float",              "WhiteSpace",            "collapse",
        "gDay",               "WhiteSpace",            "collapse",
        "gMonth",             "WhiteSpace",            "collapse",
        "gMonthDay",          "WhiteSpace",            "collapse",
        "gYear",              "WhiteSpace",            "collapse",
        "gYearMonth",         "WhiteSpace",            "collapse",
        "hexBinary",          "WhiteSpace",            "collapse",
        "int",                "FractionDigits",        "0",
        "int",                "MaxInclusive",          "2147483647",
        "int",                "MinInclusive",          "-2147483648",
        "int",                "Pattern",               "[\\-+]?[0-9]+",
        "int",                "WhiteSpace",            "collapse",
        "integer",            "FractionDigits",        "0",
        "integer",            "Pattern",               "[\\-+]?[0-9]+",
        "integer",            "WhiteSpace",            "collapse",
        "language",           "Pattern",               "([a-zA-Z]{1,8})(-[a-zA-Z0-9]{1,8})*",
        "language",           "WhiteSpace",            "collapse",
        "long",               "FractionDigits",        "0",
        "long",               "MaxInclusive",          "9223372036854775807",
        "long",               "MinInclusive",          "-9223372036854775808",
        "long",               "Pattern",               "[\\-+]?[0-9]+",
        "long",               "WhiteSpace",            "collapse",
        "negativeInteger",    "FractionDigits",        "0",
        "negativeInteger",    "MaxInclusive",          "-1",
        "negativeInteger",    "Pattern",               "[\\-+]?[0-9]+",
        "negativeInteger",    "WhiteSpace",            "collapse",
        "nonNegativeInteger", "FractionDigits",        "0",
        "nonNegativeInteger", "MinInclusive",          "0",
        "nonNegativeInteger", "Pattern",               "[\\-+]?[0-9]+",
        "nonNegativeInteger", "WhiteSpace",            "collapse",
        "nonPositiveInteger", "FractionDigits",        "0",
        "nonPositiveInteger", "MaxInclusive",          "0",
        "nonPositiveInteger", "Pattern",               "[\\-+]?[0-9]+",
        "nonPositiveInteger", "WhiteSpace",            "collapse",
        "normalizedString",   "WhiteSpace",            "replace",
        "positiveInteger",    "FractionDigits",        "0",
        "positiveInteger",    "MinInclusive",          "1",
        "positiveInteger",    "Pattern",               "[\\-+]?[0-9]+",
        "positiveInteger",    "WhiteSpace",            "collapse",
        "short",              "FractionDigits",        "0",
        "short",              "MaxInclusive",          "32767",
        "short",              "MinInclusive",          "-32768",
        "short",              "Pattern",               "[\\-+]?[0-9]+",
        "short",              "WhiteSpace",            "collapse",
        "string",             "WhiteSpace",            "preserve",
        "time",               "WhiteSpace",            "collapse",
        "token",              "WhiteSpace",            "collapse",
        "unsignedByte",       "FractionDigits",        "0",
        "unsignedByte",       "MaxInclusive",          "255",
        "unsignedByte",       "MinInclusive",          "0",
        "unsignedByte",       "Pattern",               "[\\-+]?[0-9]+",
        "unsignedByte",       "WhiteSpace",            "collapse",
        "unsignedInt",        "FractionDigits",        "0",
        "unsignedInt",        "MaxInclusive",          "4294967295",
        "unsignedInt",        "MinInclusive",          "0",
        "unsignedInt",        "Pattern",               "[\\-+]?[0-9]+",
        "unsignedInt",        "WhiteSpace",            "collapse",
        "unsignedLong",       "FractionDigits",        "0",
        "unsignedLong",       "MaxInclusive",          "18446744073709551615",
        "unsignedLong",       "MinInclusive",          "0",
        "unsignedLong",       "Pattern",               "[\\-+]?[0-9]+",
        "unsignedLong",       "WhiteSpace",            "collapse",
        "unsignedShort",      "FractionDigits",        "0",
        "unsignedShort",      "MaxInclusive",          "65535",
        "unsignedShort",      "MinInclusive",          "0",
        "unsignedShort",      "Pattern",               "[\\-+]?[0-9]+",
        "unsignedShort",      "WhiteSpace",            "collapse",
};

    private record DefFacet (short kind, String val) { }
    private static Map<String,List<DefFacet>> xercesFacet = null;
    
    private boolean isXercesDefaultFacet (XSTypeDefinition bt, short fk, String fv) throws CMFException {
        if (null == xercesFacet) {
            xercesFacet = new HashMap<>();
            for (int i = 0; i < xercesFacetData.length; i += 3) {
                var btype  = xercesFacetData[i];
                var fkcode = xercesFacetData[i+1];
                var fval   = xercesFacetData[i+2];
                var flst   = xercesFacet.get(btype);
                if (null == flst) {
                    flst = new ArrayList<>();
                    xercesFacet.put(btype, flst);
                }
                var fkind = facetCode2Kind(fkcode);
                var frec = new DefFacet(fkind, fval);
                flst.add(frec);
            }
            xercesFacet.put("anyType", new ArrayList<>());
            xercesFacet.put("anySimpleType", new ArrayList<>());
        }
        var btname = bt.getName();
        while (!xercesFacet.containsKey(btname)) {
            bt = bt.getBaseType();
            btname = bt.getName();
        }
        var deflist = xercesFacet.get(btname);
        for (var dfr : deflist) {
            if (fk == dfr.kind && fv.equals(dfr.val)) return true;
        }
        return false;
    }

    // Special handling for element augmentations.  Augmentation types do not 
    // appear in the model.  Instead, their properties are added to the property 
    // list of the augmented type.
    private void processElementAugmentations () {
        // Find all elements substitutable for an augmentation point
        for (Component c : m.getComponentList()) {
            Property p = c.asProperty();
            if (null == p || null == p.getSubPropertyOf()) continue;
            if (!p.getSubPropertyOf().getName().endsWith("AugmentationPoint")) continue;
            
            // Property p is substitutable for an augmentation point; find the augmented type
            String atqn = new String(p.getSubPropertyOf().getQName()).replace("AugmentationPoint", "Type");
            ClassType augmented = m.getClassType(atqn);
            if (null == augmented) {
                LOG.warn("Augmentation {} found, but no corresponding augmentable type {}", p.getQName(), atqn);
                continue;
            }
            if (!augmented.isAugmentable()) {
                LOG.warn("Augmentation {} found, but {} is not augmentable", p.getQName(), atqn);
                continue;
            }
            LOG.debug("{} augments {}", p.getQName(), augmented.getQName());
            
            // If Property p has an augmentation type, add type children to the augmented type
            ClassType ptype = p.getClassType();
            if (null != ptype && ptype.getName().endsWith("AugmentationType")) {
                LOG.debug("Augmenting {} with augmentation type {}", augmented.getQName(), ptype.getQName());
                int index = 0;
                for (HasProperty hp : ptype.hasPropertyList()) {
                    addAugmentPropertyToClass(augmented, p.getNamespace(), hp);
                    addAugmentRecord(ptype.getNamespace(), augmented, hp, index++);
                }
                LOG.debug("Done with augmentation type {}", ptype.getQName());
            }
            // Otherwise add augmentation property p to the augmented type
            else {
                LOG.debug("Augmenting {} with augmentation property {}", augmented.getQName(), p.getQName());
                HasProperty ahp = new HasProperty();
                ahp.setProperty(p);
                ahp.setMinOccurs(0);
                ahp.setMaxUnbounded(true); 
                addAugmentPropertyToClass(augmented, p.getNamespace(), ahp);
                addAugmentRecord(p.getNamespace(), augmented, ahp, -1);
                p.setSubPropertyOf(null);    // remove AugmentationPoint subpropertyOf
            }         
        }
        // All augmentations processed. Remove augmentation types,
        // augmentation points, and augmentation elements from model.
        // Special dance to avoid changing a list while iterating over it.
        List<Component> delComps = new ArrayList<>();
        for (Component c : m.getComponentList()) {
            String lname = c.getName();
            if (lname.endsWith("AugmentationType")
                    || lname.endsWith("AugmentationPoint")
                    || lname.endsWith("Augmentation")) delComps.add(c);
        }
        for (Component c : delComps) { m.removeComponent(c); }
    }
    
    // Add augmentation property to a Class.  Handle difference between
    // properties with an augmentation type and properties without.
    private void processOneElementAugmentation (ClassType augmented, Property augp) {
          // If property has an augmentation type, add type children to the augmented type
            ClassType ptype = augp.getClassType();
            if (null != ptype && ptype.getName().endsWith("AugmentationType")) {
                LOG.debug("Augmenting {} with augmentation type {}", augmented.getQName(), ptype.getQName());
                int index = 0;
                for (HasProperty hp : ptype.hasPropertyList()) {
                    addAugmentPropertyToClass(augmented, augp.getNamespace(), hp);
                    addAugmentRecord(ptype.getNamespace(), augmented, hp, index++);
                }
                LOG.debug("Done with augmentation type {}", ptype.getQName());
            }
            // Otherwise add augmentation property p to the augmented type
            else {
                LOG.debug("Augmenting {} with augmentation property {}", augmented.getQName(), augp.getQName());
                HasProperty ahp = new HasProperty();
                ahp.setProperty(augp);
                ahp.setMinOccurs(0);
                ahp.setMaxUnbounded(true); 
                addAugmentPropertyToClass(augmented, augp.getNamespace(), ahp);
                addAugmentRecord(augp.getNamespace(), augmented, ahp, -1);
                augp.setSubPropertyOf(null);    // remove AugmentationPoint subpropertyOf
            }                 
    }
    
    // Special handing for attribute augmentations. These are marked in XSD
    // by appinfo:augmentingNamespace.
    private void processAttributeAugmentations () {
        for (var cqn : refAppinfo.keySet()) {
            var ct      = m.getClassType(cqn);
            var propMap = refAppinfo.get(cqn);
            for (var prop : propMap.keySet()) {
                var hp    = ct.getHasProperty(prop);
                var alist = propMap.get(prop);
                if (null == hp) continue;
                for (var arec : alist) {
                    if ("augmentingNamespace".equals(arec.attLname())) {
                        var ansLstr = arec.attValue().trim();
                        if (ansLstr.isBlank()) continue;
                        var ansList = ansLstr.split("\\s+");
                        for (int i = 0; i < ansList.length; i++) {
                            var ansuri = ansList[i];
                            var ans = m.getNamespaceByURI(ansuri);
                            if (null == ans) {
                                LOG.warn(String.format("augmenting namespace %s is not NIEM-conforming in schema", ansuri));
                                continue;
                            }
                            LOG.debug("{} augments {} with attribute {}", ansuri, cqn, prop);
                            hp.augmentingNS().add(ans);
                            addAugmentRecord(ans, ct, hp, -1);
                        }            
                    }
                }
            }
        }
    }
    
    // Adds augmentation property to the augmented class
    // aug = augmented class
    // ans = namespace of the augmentation property
    // ahp = augmentation property, with min/max occurs
    // fromAugType = true if ahp is member of an augmentation type
    private void addAugmentPropertyToClass (ClassType aug, Namespace ans, HasProperty ahp) {
        // See if augmentation property is already a class member
        HasProperty augHP = null;
        for (HasProperty hp : aug.hasPropertyList()) {
            if (hp.getProperty() == ahp.getProperty()) { augHP = hp; break; }
        }
        // Not there?  Create new HasProperty and add to class
        if (null == augHP) {
            LOG.debug("Augmenting {} with new augmentation {}", aug.getQName(), ahp.getProperty().getQName());
            augHP = new HasProperty();
            augHP.setProperty(ahp.getProperty());
            augHP.setMaxUnbounded(ahp.maxUnbounded());
            augHP.setMaxOccurs(ahp.maxOccurs());    // maxOccurs from aug type (assume aug element not repeated)
            augHP.setMinOccurs(0);                  // augmentation properties always optional
            aug.addHasProperty(augHP);
        }
        // Already there?  Perhaps adjust max occurs
        else {
            LOG.debug("Augmenting {} with repeated augmentation {}", aug.getQName(), ahp.getProperty().getQName());
            if (ahp.maxUnbounded()) augHP.setMaxUnbounded(true);
            else if (!augHP.maxUnbounded()) augHP.setMaxOccurs(max(augHP.maxOccurs(), ahp.maxOccurs()));
        }
        LOG.debug("Class {} hasPropertyList:", aug.getQName());
        for (var xhp : aug.hasPropertyList()) LOG.debug("  " + xhp.getProperty().getQName());
        augHP.augmentingNS().add(ans);
    }
    
    // Adds an Augmenting element to the Property object, recording that
    // Property hp.getProperty() is a useful augmentation for Class ct,
    // according to the owner of Namespace augn.
    private void addAugmentRecord (Namespace augn, ClassType ct, HasProperty hp, int index) {
        for (var ar : augn.augmentList()) {
            if (ar.getClassType() != ct) continue;
            if (ar.getProperty() == hp.getProperty()) return;
        }
        var ar = new AugmentRecord();
        ar.setClassType(ct);
        ar.setProperty(hp.getProperty());
        ar.setIndexInType(index);
        ar.setMinOccurs(hp.minOccurs());
        ar.setMaxOccurs(hp.maxOccurs());
        ar.setMaxUnbounded(hp.maxUnbounded());
        augn.addAugmentRecord(ar);
        LOG.debug(String.format("namespace %s augments %s with %s (index %d)", 
                augn.getNamespacePrefix(), ct.getQName(), hp.getProperty().getQName(), index));
    }
 
    // Retrieve appinfo attribute value for a global component or an element reference.
    // Returns an empty string for appinfo that doesn't exist; never returns null.
    // For <xs:complexType name="ThingType" appinfo:deprecated="true">, do 
    //     getAppinfoAttribute("nc:ThingType", null, "deprecated")
    // For <xs:element ref="nc:Thing" appinfo:orderedPropertyIndicator="true", do
    //     getAppinfoAttribute("nc:ThingType", "nc:Prop", "orderedPropertyIndicator")
    private String getAppinfoAttribute (String compQN, String erefQN, String alname) {
        if (null == erefQN) {
            var arlist = globalAppinfo.get(compQN);
            if (null == arlist) return "";
            for (var arec : arlist) {
                if (arec.attLname().equals(alname)) return arec.attValue();
            }
            return "";
        }
        var eqnmap = refAppinfo.get(compQN);
        if (null == eqnmap) return "";
        var arlist = eqnmap.get(erefQN);
        if (null == arlist) return "";
        for (var arec : arlist) {
            if (arec.attLname().equals(alname)) return arec.attValue();   
        }
        return "";
    }
    
    // Retrieve an intialized datatype object by uri and local name.
    // If you ask for a proxy type, you get the underlying XSD type.  
    // If you ask for an XSD type, it's added to the model and then returned to you.  
    // If you ask for any type in the XML namespace, you get xs:string.  
    // If you ask for xs:anyType or xs:anySimpleType, you get null.
    private Datatype getDatatype (String nsuri, String lname) throws CMFException {
        if (NIEM_PROXY == NamespaceKind.uriBuiltinNum(nsuri)) nsuri = W3C_XML_SCHEMA_NS_URI;     // replace proxy types with XSD
        var dtqn = getQN(nsuri, lname);
        var res  = datatypeMap.get(dtqn);
        if (null != res)                   return res;
        if (XML_NS_URI.equals(nsuri))      return getDatatype(W3C_XML_SCHEMA_NS_URI, "string");
        if (!W3C_XML_SCHEMA_NS_URI.equals(nsuri))     throw new CMFException(String.format("no datatype for %s#%s", nsuri, lname));
        if ("anyType".equals(lname))       return null;
        if ("anySimpleType".equals(lname)) return null;
        res = new Datatype(xsdns, lname);
        res.addToModel(m);
        datatypeMap.put(res.getQName(), res);
        LOG.debug("created datatype " + res.getQName());
        return res;
    }
    
    private XSSimpleTypeDefinition getAtomicBaseType (XSSimpleTypeDefinition xstype) {
        var lname  = xstype.getName();
        var xsbase = xstype.getBaseType();
        if ("token".equals(lname) || "normalizedString".equals(lname) || "string".equals(lname)) return xstype;
        if (null == xsbase || "anySimpleType".equals(xsbase.getName())) return xstype;
        return getAtomicBaseType((XSSimpleTypeDefinition)xsbase);
    }
    
    // Returns the CMF QName for a schema object
    private String getQN (XSObject xobj) throws CMFException {
        return getQN(xobj.getNamespace(), xobj.getName());
    }
    
    private String getQN (String nsuri, String lname) throws CMFException {
        if (NIEM_PROXY == NamespaceKind.uriBuiltinNum(nsuri)) nsuri = W3C_XML_SCHEMA_NS_URI;     // replace proxy types with XSD
        var ns = m.getNamespaceByURI(nsuri);
        if (null == ns) 
            throw new CMFException(String.format("no namespace object for %s", nsuri));
        return ns.getNamespacePrefix() + ":" + lname;        
    }
    
    // Returns an attribute value by name.
    // Returns null if the element does not have the attribute.
    private String getAttribute (Element e, String lname) {
        var attr = e.getAttributeNode(lname);
        if (null == attr) return null;
        return e.getAttribute(lname);
    }
    
    // replaceSuffix("FooSimpleType", "SimpleType", "Datatype") -> "FooDatatype"
    private String replaceSuffix (String s, String ending, String replacement) {
        if (!s.endsWith(ending)) return s;
        int slen = s.length();
        int elen = ending.length();
        return s.substring(0, slen-elen) + replacement;
    }
 
    // You know, a schema document doesn't have to define a prefix for the target namespace.
    // But a CMF model has to have one.  Try to create something sensible from the namespace URI.
    private String bodgeUpAPrefix (String nsuri) {
        String prefix = nsuri.replaceFirst("/?[0123456789.]*/?$", "");
        int ls = prefix.lastIndexOf("/");
        if (ls >= 0) {
            prefix = prefix.substring(ls + 1);
            prefix = prefix.toLowerCase();
            prefix = prefix.replaceAll("[^a-z-.]+", "");
            prefix = String.format("%-8.8s", prefix);
            if (!prefix.isBlank()) return prefix;
        }
        return "ext";
    }
    
    private int namespaceKindFromSchema (String nsuri) {
        var sd = sdoc.get(nsuri);
        if (null == sd) return NSK_UNKNOWN;
        return sd.schemaKind();
    }
    
    // Annotations are available from the XSModel as an XML string.
    // We parse those XML strings into DOM elements to extract documentation
    // and appinfo.  
    
    // The XSModel API is funky.  You might think that XSObject
    // would provide a list of annotations, but no.  Instead the different
    // schema object types have their own annotation API. So we roll our own
    // annotation interface for XSObject.  Returns null for an object that 
    // does not have annotations.
    private XSObjectList getAnnotations (XSObject o) {
        switch (o.getType()) {
            case ATTRIBUTE_DECLARATION: return ((XSAttributeDeclaration)o).getAnnotations();
            case ELEMENT_DECLARATION:   return ((XSElementDeclaration)o).getAnnotations();
            case FACET:                 return ((XSFacet)o).getAnnotations();
            case MULTIVALUE_FACET:      return ((XSMultiValueFacet)o).getAnnotations();
            case PARTICLE:              return ((XSParticle)o).getAnnotations();
            case TYPE_DEFINITION:
                var td = (XSTypeDefinition)o;
                if (COMPLEX_TYPE == td.getTypeCategory()) return ((XSComplexTypeDefinition)td).getAnnotations();
                else return ((XSSimpleTypeDefinition)td).getAnnotations();
        }
        return null;
    }
    
    // Populstes a list of the specified  XSD elements (documentation or appinfo).
    //   annList -- empty List of Elements to populate
    //   lname   -- local name of element to extract
    private void parseAnnotation (List<Element> annList, XSAnnotation an, String lname) {
        try {
            var as = an.getAnnotationString();
            var sr = new StringReader(as);
            var is = new InputSource(sr);
            var db  = ParserBootstrap.docBuilder();
            var d   = db.parse(is);
            var del = d.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, lname);
            for (int i = 0; i < del.getLength(); i++) {
                var de   = (Element)del.item(i);
                annList.add(de);
            }
        } catch (Exception ex) { } // IGNORE
    }
    
    // Returns a list of documentation strings for this schema object
    private List<String> getDocumentation (XSObject o) {
        List<String> docList = new ArrayList<>();   // list of documentation strings
        List<Element> aeList = new ArrayList<>();   // list of documentation elements
        XSObjectList alist = getAnnotations(o);     // annotation elements from xsmodel
        if (null == alist) return docList;
        for (int i = 0; i < alist.getLength(); i++) {
            var an = (XSAnnotation)alist.item(i);           // annotation element
            getDocumentationStrings(docList, an);
        }
        return docList;
    }
    
    // Populates a list of documentation strings from a single annotation element
    private void getDocumentationStrings (List<String> docList, XSAnnotation an) {
        List<Element> aeList = new ArrayList<>();   // list of documentation elements  
        parseAnnotation(aeList, an, "documentation");
        for (var ae : aeList) {
            var docstr = ae.getTextContent();
            docList.add(docstr);
        }
    }
    
    // Returns a list of appinfo elements for this schema object
    private List<Element> getAppinfoElements (XSObject o) {
        List<Element> apList = new ArrayList<>();
        XSObjectList alist = getAnnotations(o);
        if (null == alist) return apList;
        for (int i = 0; i < alist.getLength(); i++) {
            var an = (XSAnnotation)alist.item(i);
            parseAnnotation(apList, an, "appinfo");
        }
        return apList;
    }

//    // debug tool
//    private String elementToString (Element e) {  
//        String result = "";
//        try {
//            Transformer tr = TransformerFactory.newInstance().newTransformer();
//            tr.setOutputProperty(OutputKeys.INDENT, "yes");
//            tr.setOutputProperty(OutputKeys.METHOD, "xml");
//            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//            StringWriter ostr = new StringWriter();
//            tr.transform(new DOMSource(e), new StreamResult(ostr));
//            result = ostr.toString();
//            int i = 0;
//        } catch (TransformerException ex) {
//            java.util.logging.Logger.getLogger(ModelToXSD.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return result;
//    }    

}
