/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2021 The MITRE Corporation.
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
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import static org.apache.xerces.xs.XSConstants.ATTRIBUTE_DECLARATION;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.FACET;
import static org.apache.xerces.xs.XSConstants.MODEL_GROUP;
import static org.apache.xerces.xs.XSConstants.MULTIVALUE_FACET;
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
import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI_PREFIX;
import static org.mitre.niem.NIEMConstants.XSD_NS_URI;
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
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import static org.mitre.niem.NIEMConstants.XML_NS_URI;
import org.mitre.niem.cmf.CMFException;
import static org.mitre.niem.cmf.NamespaceKind.NSK_OTHERNIEM;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UNKNOWN;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XML;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XSD;
import static org.mitre.niem.cmf.NamespaceKind.isNamespaceKindInCMF;
import static org.mitre.niem.cmf.NamespaceKind.namespaceKindFromURI;
import org.mitre.niem.cmf.NamespaceMap;
import org.mitre.niem.cmf.SchemaDocument;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_PROXY;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_STRUCTURES;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinKind;

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
    private XMLSchema s = null;                             // schema from which we are creating the model
    private XSModel xs = null;                              // from the XMLSchema object    
    private Map<String,XMLSchemaDocument> sdoc = null;      // map nsURI -> XMLSchemaDocument object from XMLSchema object
    
    // Global appinfo is a map of component QName -> list of appinfo records
    // Element ref appinfo is a double map: Complex type QName,  Element ref QName -> list of appinfo records
    private Map<String,List<Appinfo>> globalAppinfo = null;
    private Map<String,Map<String,List<Appinfo>>> elementRefAppinfo = null;
    
    // Remember the XS objects from which we initialized the CMF objects
    private Map<Property,XSObject> propertyXSobj = null;               // initialized CMF property paired with schema object
    private Map<Datatype,XSTypeDefinition> datatypeXSobj = null;       // initialized CMF datatype paired with schema object
    private Map<ClassType,XSComplexTypeDefinition> classXSobj = null;  // initialized CMF classtype paired with schema object
    private Map<String,Datatype> datatypeMap = null;                   // map QName -> initialized Datatype object
    private Set<ClassType> hasLiteralProperty = null;                  // FooType class objects requiring a FooLiteral property
    
    
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
        elementRefAppinfo    = new HashMap<>();
        propertyXSobj        = new HashMap<>();
        datatypeXSobj        = new HashMap<>();
        classXSobj           = new HashMap<>();
        datatypeMap             = new HashMap<>();
        hasLiteralProperty   = new HashSet<>();
         
        generateNamespaces();           // normalize namespace prefixes so we can use QNames
        processAppinfo();               // index all the appinfo by QName for easy retrieval
        initializeDeclarations();       // create properties for element and attribute declarations
        initializeDefinitions();        // create class and datatype objects for type definitions
        setClassInheritance();          // establish ClassType inheritance
        findClassWithLiteralProperty();
        handleSimpleTypes();
        processProperties();            // populate all fields of property objects from schema
        processClassTypes();            // populate all fields of class objects
        processDatatypes();             // populate all fields of datatype objects
        processAugmentations();         // add augmentation properties to augmented class objects

        // Add to the model a schema document record for each document in the pile
        sdoc.forEach((nsuri, sd) -> {
            SchemaDocument cmfsd = new SchemaDocument();
            cmfsd.setTargetNS(nsuri);
            cmfsd.setConfTargets(sd.conformanceTargets());
            cmfsd.setFilePath(sd.filepath());
            cmfsd.setNIEMversion(sd.niemVersion());
            cmfsd.setSchemaVersion(sd.schemaVersion());
            cmfsd.setLanguage(sd.language());
            m.addSchemaDoc(nsuri, cmfsd);
        });
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
        
        // Add namespace objects for NIEM, external, and XSD namespaces found in schema pile
        // The XML namespace we initialize later, and only if someone uses it
        // NIEM XSD doesn't require a namespace prefix, but CMF does, so make one if necessary.
        sdoc.forEach((nsuri, sd) -> {
            String prefix = nsmap.getPrefix(nsuri);
            if (null == prefix) {
                prefix = bodgeUpAPrefix(nsuri);
                nsmap.assignPrefix(prefix, nsuri);
            }
            int kind = sd.schemaKind();
            if (NSK_XML != kind && isNamespaceKindInCMF(kind)) {
                Namespace n = new Namespace(prefix, nsuri);
                n.setDefinition(sd.documentation());
                n.setKind(kind);
                try { m.addNamespace(n); } catch (CMFException ex) { } // CAN'T HAPPEN
                LOG.debug("Created namespace {}", nsuri);
            }
        });
        // Add namespace for XSD to model if it isn't already there
        // (Sometimes people import it, sometimes they don't)
        xsdns = m.getNamespaceByURI(XSD_NS_URI);
        if (null == xsdns) {
            xsdns = new Namespace(nsmap.getPrefix(XSD_NS_URI), XSD_NS_URI); // reserved prefix
            xsdns.setKind(NSK_XSD);
            try { m.addNamespace(xsdns); } catch (CMFException ex) { }    // CAN'T HAPPEN
            LOG.debug("Created namespace {}", XSD_NS_URI);
        }
    }
    
    // Now we have unique namespace prefixes assigned, we go through all the 
    // appinfo records in all the schema documents and index them by global
    // compnent QName (and sometimes then element reference QName).  Sure would be
    // nice if appinfo was available through XSModel, but it isn't.
    private void processAppinfo () {
        sdoc.forEach((ns,sd) -> {
            for (var arec : sd.appinfo()) {
                String cns = arec.componentEQN().getValue0();
                String cln = arec.componentEQN().getValue1();
                String cqn = m.getNamespaceByURI(cns).getNamespacePrefix() + ":" + cln;
                
                // Component appinfo; add arec to appinfo list for this component
                if (null == arec.elementEQN()) {
                    var arlist = globalAppinfo.get(cqn);
                    if (null == arlist) {
                        arlist = new ArrayList<>();
                        globalAppinfo.put(cqn, arlist);
                    }
                    arlist.add(arec);
                }
                // Element reference appinfo; add arec to list for this component and element ref
                else {
                    String ens = arec.elementEQN().getValue0();
                    String eln = arec.elementEQN().getValue1();
                    String eqn = m.getNamespaceByURI(ens).getNamespacePrefix() + ":" + eln;
                    var eqnmap = elementRefAppinfo.get(cqn);
                    if (null == eqnmap) {
                        eqnmap = new HashMap<>();
                        elementRefAppinfo.put(cqn, eqnmap);
                    }
                    var arlist = eqnmap.get(eqn);
                    if (null == arlist) {
                        arlist = new ArrayList<>();
                        eqnmap.put(eqn, arlist);
                    }
                    arlist.add(arec);
                }
            }
        });
    }

    // Create property placeholder objects for all the element and attribute declarations 
    // in NIEM-conforming schema documents. Only namespace and name initialized, and
    // attributes distingushed from elements; the rest happens later. After this 
    // the model contains most of the property objects, but those objects are incomplete.
    private void initializeDeclarations () {
        XSNamedMap xmap = xs.getComponents(ATTRIBUTE_DECLARATION);
        for (int i = 0; i < xmap.getLength(); i++) {
            XSObject xobj = xmap.item(i);
            initializeOneDeclaration(xobj, true);
        }
        xmap = xs.getComponents(ELEMENT_DECLARATION);
        for (int i = 0; i < xmap.getLength(); i++) {
            XSObject xobj = xmap.item(i);
            initializeOneDeclaration(xobj, false);
        }        
    }
    
    private void initializeOneDeclaration (XSObject xobj, boolean isAttribute) {
        String nsuri = xobj.getNamespace();
        String lname = xobj.getName();
        int kind = namespaceKindFromSchema(nsuri);
        if (kind > NSK_OTHERNIEM) return;               // FIXME code list namespace?
        Namespace ns = m.getNamespaceByURI(nsuri);
        Property p = new Property(ns, lname);
        String doc = getDocumentation(xobj);
        p.setDefinition(doc);
        p.setIsAttribute(isAttribute);
        p.addToModel(m);
        propertyXSobj.put(p, xobj);
        LOG.debug("initialized property " + p.getQName());     
    }
    
    // Create class and datatype placeholder objects for all the type definitions in
    // NIEM-conforming schema documents. Only name and namespace initialized at
    // this point.  Adding datatype objects to the model happens later.  We build 
    // a separate map of QName to datatype object here, then possibly revise some
    // mappings when we handle FooSimpleType declarations.  We don't create datatype
    // objects for proxy types at all.  We don't create datatype objects for types
    // in the XSD namespace until we see we need them.
    private void initializeDefinitions () throws CMFException {
        XSNamedMap xmap = xs.getComponents(TYPE_DEFINITION);
        for (int i = 0; i < xmap.getLength(); i++) {
            var xtype = (XSTypeDefinition)xmap.item(i);
            var xbase = xtype.getBaseType();
            var nsuri = xtype.getNamespace();
            var lname = xtype.getName();
            var ns    = m.getNamespaceByURI(nsuri);
            int kind  = namespaceKindFromSchema(nsuri);
            if (kind > NSK_OTHERNIEM) continue;         // FIXME code list namespace?

            // Simple type is always a Datatype object
            if (SIMPLE_TYPE == xtype.getTypeCategory()) {
                Datatype dt = new Datatype(ns, lname);
                String doc  = getDocumentation(xtype);
                dt.setDefinition(doc);
                datatypeXSobj.put(dt, xtype);
                datatypeMap.put(dt.getQName(), dt);
                LOG.debug("initialized datatype " + dt.getQName());
            }
            else {
                // Complex type can be a class or a datatype object.
                // Simple content without model attributes or metadata is a Datatype object
                boolean hasAttributes = false;
                var qname  = ns.getNamespacePrefix() + ":" + lname;
                var xctype = (XSComplexTypeDefinition)xtype;
                var atts   = xctype.getAttributeUses();
                for (int j = 0; j < atts.getLength(); j++) {        // iterate over all attributes, incl. inherited
                    var au    = (XSAttributeUse)atts.item(j);
                    var adecl = au.getAttrDeclaration();
                    var ans   = adecl.getNamespace();
                    if (NIEM_STRUCTURES != getBuiltinKind(ans)) {   // attributes in structures NS don't count
                        hasAttributes = true;                       // found a semantic attribute; we're done
                        break;
                    }
                }
                // Walk the inheritance chain, looking for metadata
                boolean hasMetadata = false;
                XSTypeDefinition ictype = xctype;
                while (!hasMetadata && null != ictype) {
                    var icnsuri = ictype.getNamespace();
                    int ikind = namespaceKindFromURI(icnsuri);
                    if (ikind > NSK_OTHERNIEM) break;
                    var icQN = getQN(ictype);
                    hasMetadata = ("true".equals(getAppinfo(icQN, null, "metadataIndicator")));
                    ictype = ictype.getBaseType();
                }
                boolean isSimple = (null != xctype.getSimpleType());
                if (isSimple && !hasAttributes && !hasMetadata) {   // it's a Datatype
                    Datatype dt = new Datatype(ns, lname);
                    String doc  = getDocumentation(xctype);
                    dt.setDefinition(doc);                    
                    datatypeXSobj.put(dt, xtype);
                    datatypeMap.put(dt.getQName(), dt);
                    LOG.debug("initialized datatype " + dt.getQName()); 
                }
                // Complex content is a ClassType object
                // Simple content with model attributes or metadata anywhere in derivation is a ClassType object
                else {
                    initializeXMLattributes(xctype);
                    ClassType ct = new ClassType(ns, lname);
                    String doc  = getDocumentation(xctype);
                    ct.setDefinition(doc);                       
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
            Property p = new Property(xmlns, aln);
            p.setIsAttribute(true);
            p.addToModel(m);
            propertyXSobj.put(p, adecl);
            LOG.debug("initialized property " + p.getQName());            
        }
    }
    
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

    // There are no Datatype objects named "FooSimpleType" in CMF.
    // If there is a ClassType object named "FooType", then the Datatype is named "FooDatatype".
    // If no such ClassType object, then the Datatype is named "FooType.
    private void handleSimpleTypes () throws CMFException {
        Map<Datatype,Datatype> replacements = new HashMap<>();      
        for (var sdt : datatypeXSobj.keySet()) {
            var xtype  = datatypeXSobj.get(sdt);
            var sdtqn  = sdt.getQName();                            // "x:FooSimpleType"
            var baseqn = sdtqn.replaceFirst("SimpleType$", "Type"); // "x:FooType"
            var ddtqn  = baseqn.replaceFirst("Type$", "Datatype");  // "x:FooDatatype"
            var ct     = m.getClassType(baseqn);                    // ClassObject named x:FooType, if any
            var dt     = datatypeMap.get(baseqn);                   // Datatype object named x:FooType, if any
            if (SIMPLE_TYPE != xtype.getTypeCategory()) continue;   // dt is not from xs:simpleType declaration
            
            // We are either going to rename Datatype sdt, or replace the content
            // of Datatype dt with the content of sdt.
            boolean replace  = false;
            String rename    = null;
            
            // If there is no x:FooType object, then rename the Datatype from 
            // "x:FooSimpleType" to "x:FooType"
            if (null == ct && null == dt) {
                rename = baseqn;               
            }
            // If there is a x:FooType class object, then it has a x:FooSimpleType literal.
            // Rename the datatype object from "x:FooSimpleType" to "x:FooDatatype"
            else if (null != ct) {
                rename = ddtqn;
            }
            else {
                var dtxtype = datatypeXSobj.get(dt);        // xs:complexType for x:FooType
                var dtxbase = dtxtype.getBaseType();        // schema extension base
                var dtxbqn  = getQN(dtxbase);               // QName of extension base
                if (dtxbqn.equals(sdtqn)) {                 // if equal, dt is an empty extension of sdt
                    replace = true;                         // so replace dt with sdt
                }
                else {                                      // not equal
                    rename = ddtqn;                         // rename x:FooSimpleType to x:FooDatatype
                }
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
        // Can't modify datatyhpeXSobj while iterating over it, so do replacements now
        replacements.forEach((dt, sdt) -> {
            var xtype = datatypeXSobj.get(sdt);
            datatypeXSobj.put(dt, xtype);
            datatypeXSobj.remove(sdt);
            datatypeMap.put(sdt.getQName(), dt);
        });
    }
 
    // Now we have all of the properties from the schema, all of the classes, 
    // and all of the datatypes except XSD datatypes. Time to complete all
    // the property objects.
    private void processProperties () throws CMFException {
        for (var p : propertyXSobj.keySet()) {
            LOG.debug("processing property " + p.getQName());
            var xobj = propertyXSobj.get(p);
            p.setIsDeprecated(getAppinfo(p.getQName(), null, "deprecated"));
            if (p.isAttribute()) {
                var xadecl = (XSAttributeDeclaration)xobj;
                var xatype = xadecl.getTypeDefinition();
                var dt     = getDatatype(xatype.getNamespace(), xatype.getName());
                p.setDatatype(dt);
               }
            else {
                var xedecl = (XSElementDeclaration)xobj;
                var xetype = xedecl.getTypeDefinition();
                var pclass = m.getClassType(xetype.getNamespace(), xetype.getName());
                if (null == pclass) p.setDatatype(getDatatype(xetype.getNamespace(), xetype.getName()));
                else p.setClassType(pclass);
                var xesubg = xedecl.getSubstitutionGroupAffiliation();
                if (null != xesubg) {
                    p.setSubPropertyOf(m.getProperty(xesubg.getNamespace(), xesubg.getName()));
                }
                p.setIsAbstract(xedecl.getAbstract());
                p.setIsReferenceable(xedecl.getNillable());
                p.setCanHaveMD(getAppinfo(p.getQName(), null, "metadataIndicator"));
            }
        }
    }
       
    // Time to complete all the class objects.  Special handling for simple content
    // with attributes or metadata: we create a FooLiteral property to hold the 
    // content of an element of FooType.
    private void processClassTypes () throws CMFException {
        for (var ct : classXSobj.keySet()) {
            LOG.debug("processing class " + ct.getQName());
            var ctqn    = ct.getQName();
            var ctns    = ct.getNamespace();                // class namespace object
            var ctnsuri = ctns.getNamespaceURI();           // class namespace URI
            var xctype  = classXSobj.get(ct);               // XSComplexTypeDefinition object
            var xbase   = xctype.getBaseType();             // base type XSTypeDefinition
            var xsbase  = xctype.getSimpleType();           // base type XSSimpleTypeDefinition (possibly null)
            ct.setCanHaveMD(getAppinfo(ct.getQName(), null, "metadataIndicator"));
            ct.setIsAbstract(xctype.getAbstract());
            ct.setIsDeprecated(getAppinfo(ct.getQName(), null, "deprecated"));
            ct.setIsExternal(getAppinfo(ct.getQName(), null, "externalAdapterTypeIndicator"));

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
                Property np = new Property(ctns, npln);
                LOG.debug("created literal property " + np.getQName());
                np.setDatatype(basedt);
                np.addToModel(m);
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
                    // Element declaration in external namespace will return null
                    var p = m.getProperty(xed.getNamespace(), xed.getName());
                    if (null != p) {
                        var hp = new HasProperty();
                        hp.setProperty(p);
                        hp.setMinOccurs(xp.getMinOccurs());
                        if (xp.getMaxOccursUnbounded()) hp.setMaxUnbounded(true);
                        else hp.setMaxOccurs(xp.getMaxOccurs());
                        hp.setOrderedProperties("true".equals(getAppinfo(ct.getQName(), p.getQName(), "orderedPropertyIndicator")));
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
                ct.addHasProperty(hp);
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
            if (!a.getNamespace().startsWith(STRUCTURES_NS_URI_PREFIX)) aset.add(au);
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
                if (XSD_NS_URI.equals(base.getNamespace()) && "anyType".equals(base.getName())) break;
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
            if (XSD_NS_URI.equals(base.getNamespace()) && "anyType".equals(base.getName())) break;            
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
       
    // Time to complete all the datatype objects.  The schema object is either
    // a simple type, or a complex type with simple content and no attributes.
    private void processDatatypes () throws CMFException {
        for (var dt : datatypeXSobj.keySet()) {
            dt.setIsDeprecated(getAppinfo(dt.getQName(), null, "deprecated"));
            m.addComponent(dt);
            if (xsdns == dt.getNamespace()) continue;               // nothing more to do for XSD types
            LOG.debug("processing datatype " + dt.getQName());
            
            var xtype = datatypeXSobj.get(dt);
            XSSimpleTypeDefinition xstype = null;
            boolean isComplex = (COMPLEX_TYPE == xtype.getTypeCategory());
            if (isComplex) {
                var xctype = (XSComplexTypeDefinition)xtype;
                var xbtype = xctype.getBaseType();
                if (null != xbtype && COMPLEX_TYPE == xbtype.getTypeCategory()) {
                    var r  = new RestrictionOf();
                    var bt = getDatatype(xbtype.getNamespace(), xbtype.getName());
                    r.setDatatype(bt);
                    dt.setRestrictionOf(r);
                    continue;
                }
                else xstype = xctype.getSimpleType();
            }
            else xstype = (XSSimpleTypeDefinition)xtype;
            
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
                    var r = createRestrictionOf(xstype);
                    dt.setRestrictionOf(r);
                    break;
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
        var xbase = xstype.getBaseType();
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
            XSFacet f   = (XSFacet)flist.item(i);
            String fval = f.getLexicalFacetValue();
            Facet fo    = new Facet();
            fo.setFacetKind(facetKind(f));
            fo.setStringVal(f.getLexicalFacetValue());
            fo.setDefinition(getDocumentation(f));            
            // Include a whitespace facet only for types derived from xs:string or xs:normalizedString,
            // and only when the value is not the default
            if (FACET_WHITESPACE == f.getFacetKind()) {
                var atombase = getAtomicBaseType(xstype);
                var ablname  = atombase.getName();
                switch (ablname) {
                    case "string":           if ("preserve".equals(fval))  fo = null; break;
                    case "normalizedString": if (!"collapse".equals(fval)) fo = null; break;
                    default: fo = null; break;
                }
            }
            if (null != fo) r.addFacet(fo);
        }
        flist = xstype.getMultiValueFacets();
        for (int i = 0; i < flist.getLength(); i++) {
            XSMultiValueFacet f = (XSMultiValueFacet)flist.item(i);
            String fkind = FACET_PATTERN == f.getFacetKind() ? "Pattern" : "Enumeration";
            XSObjectList annl = f.getAnnotations();
            StringList   vals = f.getLexicalFacetValues();
            for (int j = 0; j < vals.getLength(); j++) {
                String val = vals.item(j);
                String def = null;
                if (j < annl.getLength()) {
                    XSAnnotation an = (XSAnnotation)annl.item(j);
                    if (null != an) def = parseDefinition((XSAnnotation)annl.item(j));
                }
                Facet fo = new Facet();
                fo.setFacetKind(fkind);
                fo.setStringVal(val);
                fo.setDefinition(def);
                r.addFacet(fo);
            }
        }
        return r;
    }
    
    private String facetKind (XSFacet f) {
        switch (f.getFacetKind()) {
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
                LOG.error("Unknown facet kind {}", f.getFacetKind());
                return "";
        }
    }

    // Special handling for augmentations.  Augmentation types do not appear in 
    // the model.  Instead, their properties are added to the property list of
    // the augmented type.
    private void processAugmentations () {
        // Find all elements substitutable for an augmentation point
        for (Component c : m.getComponentList()) {
            Property p = c.asProperty();
            if (null == p || null == p.getSubPropertyOf()) continue;
            if (!p.getSubPropertyOf().getName().endsWith("AugmentationPoint")) continue;
            
            // Property p is subsitutable for an augmentation point; find the augmented type
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
                for (HasProperty hp : ptype.hasPropertyList()) {
                    addAugmentPropertyToClass(augmented, p.getNamespace(), hp, true);
                }
                LOG.debug("Done with augmentation type {}", ptype.getQName());
            }
            // Otherwise add augmentation property p to the augmented type
            else {
                HasProperty ahp = new HasProperty();
                ahp.setProperty(p);
                ahp.setMinOccurs(0);
                ahp.setMaxUnbounded(true); 
                addAugmentPropertyToClass(augmented, p.getNamespace(), ahp, false);
                p.setSubPropertyOf(null);    // remove AugmentationPoint subpropertyOf
            }         
        }
        // All augmentations processed. Remove augmentation types,
        // augmentation points, and augmentation elements from model
        List<Component> delComps = new ArrayList<>();
        for (Component c : m.getComponentList()) {
            String lname = c.getName();
            if (lname.endsWith("AugmentationType")
                    || lname.endsWith("AugmentationPoint")
                    || lname.endsWith("Augmentation")) delComps.add(c);
        }
        for (Component c : delComps) { m.removeComponent(c); }
    }
    
    // Adds augmentation property to the augmented class
    // aug = augmented class
    // ans = namespace of the augmentation property
    // ahp = augmentation property, with min/max occurs
    // fromAugType = true if ahp is member of an augmentation type
    private void addAugmentPropertyToClass (ClassType aug, Namespace ans, HasProperty ahp, boolean fromAugType) {
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
            augHP.setSequenceID(""+aug.hasPropertyList().size());
        }
        // Already there?  Perhaps adjust max occurs
        else {
            LOG.debug("Augmenting {} with repeated augmentation {}", aug.getQName(), ahp.getProperty().getQName());
            if (ahp.maxUnbounded()) augHP.setMaxUnbounded(true);
            else if (!augHP.maxUnbounded()) augHP.setMaxOccurs(max(augHP.maxOccurs(), ahp.maxOccurs()));
        }
        if (fromAugType) augHP.augmentTypeNS().add(ans);
        else augHP.setAugmentElementNS(ans);
    }
 
    // Retrieve appinfo attribute value for a global component or element reference.
    // Returns an empty string for appinfo that doesn't exist; never returns null.
    // For <xs:complexType name="ThingType" appinfo:deprecated="true">, do 
    //     getAppinfo("nc:ThingType", null, "deprecated")
    // For <xs:element ref="nc:Thing" appinfo:orderedPropertyIndicator="true", do
    //     getAppinfo("nc:ThingType", "nc:Prop", "orderedPropertyIndicator")
    private String getAppinfo (String compQN, String erefQN, String alname) {
        if (null == erefQN) {
            var arlist = globalAppinfo.get(compQN);
            if (null == arlist) return "";
            for (var arec : arlist) {
                if (arec.attLname().equals(alname)) return arec.attValue();
            }
            return "";
        }
        var eqnmap = elementRefAppinfo.get(compQN);
        if (null == eqnmap) return "";
        var arlist = eqnmap.get(erefQN);
        if (null == arlist) return "";
        for (var arec : arlist) {
            if (arec.attLname().equals(alname)) return arec.attValue();   
        }
        return "";
    }
    
    // Retrieve an intialized datatype object by uri and local name.
    // If you ask for 
    // If you ask for a proxy type, you get the underlying XSD type.  
    // If you ask for an XSD type, it's added to the model and then returned to you.  
    // If you ask for any type in the XML namespace, you get xs:string.  
    // If you ask for xs:anyType or xs:anySimpleType, you get null.
    private Datatype getDatatype (String nsuri, String lname) throws CMFException {
        if (NIEM_PROXY == getBuiltinKind(nsuri)) nsuri = XSD_NS_URI;    // replace proxy types with XSD
        var dtqn = getQN(nsuri, lname);
        var res  = datatypeMap.get(dtqn);
        if (null != res)                   return res;
        if (XML_NS_URI.equals(nsuri))      return getDatatype(XSD_NS_URI, "string");
        if (!XSD_NS_URI.equals(nsuri))     throw new CMFException(String.format("no datatype for %s#%s", nsuri, lname));
        if ("anyType".equals(lname))       return null;
        if ("anySimpleType".equals(lname)) return null;
        res = new Datatype(xsdns, lname);
        res.addToModel(m);
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
        if (NIEM_PROXY == getBuiltinKind(nsuri)) nsuri = XSD_NS_URI;    // replace proxy types with XSD
        var ns = m.getNamespaceByURI(nsuri);
        if (null == ns) 
            throw new CMFException(String.format("no namespace object for %s", nsuri));
        return ns.getNamespacePrefix() + ":" + lname;        
    }
    
    // replaceEnding("FooSimpleType", "SimpleType", "Datatype") -> "FooDatatype"
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
    
    // Component definitions are provided in the annotations.  Annotations are 
    // provided as XML text.  Parse these text strings and return the content of
    // the first xs:documentation element encountered.
    private String getDocumentation (XSObject o) {
        XSObjectList alist = getAnnotations(o);
        if (null == alist) return null;
        for (int i = 0; i < alist.getLength(); i++) {           
            XSAnnotation a = (XSAnnotation)alist.item(i);
            String ds = parseDefinition(a);
            if (null != ds) return ds.trim();
        }
        return null;
    }
    
    // Sure would be nice if XSObject returned a list of annotations.
    // Instead we have to figure out what kind of schema object we have, and
    // call the appropriate function.
    private XSObjectList getAnnotations (XSObject o) {
        XSObjectList alist = null;
        short otype = o.getType();
        switch (otype) {
            case TYPE_DEFINITION:
                if (SIMPLE_TYPE == ((XSTypeDefinition)o).getTypeCategory())
                    alist = ((XSSimpleTypeDefinition)o).getAnnotations();
                else alist = ((XSComplexTypeDefinition)o).getAnnotations();                
                break;
            case ATTRIBUTE_DECLARATION: alist = ((XSAttributeDeclaration)o).getAnnotations(); break;
            case ELEMENT_DECLARATION:   alist = ((XSElementDeclaration)o).getAnnotations(); break;
            case FACET:                 alist = ((XSFacet)o).getAnnotations(); break;
            case MULTIVALUE_FACET:      alist = ((XSMultiValueFacet)o).getAnnotations(); break;
        }
        return alist;
    }    
    
    private String parseDefinition (XSAnnotation a) {
        String ds = null;
        try {
            SAXParser saxp = ParserBootstrap.sax2Parser();
            AnnotationHandler h = new AnnotationHandler();
            String as = a.getAnnotationString();
            StringReader sr = new StringReader(as);
            InputSource is = new InputSource(sr);
            saxp.parse(is, h);
            ds = h.getDocumentation();
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            LOG.error("Parsing annotation string: {}", ex.getMessage());
        }
        return ds;
    }
    
    private class AnnotationHandler extends DefaultHandler {
        private StringBuilder dsb = null;
        private String ds = null;
        public String getDocumentation () { return ds; }       
        @Override
        public void startElement(String ns, String ln, String qn, Attributes atts) {  
            if (null != ds) return;                     // already finished a documentation element
            if (!"documentation".equals(ln)) return;    // this isn't a documentation element
            if (!XSD_NS_URI.equals(ns)) return;         // this isn't one either
            dsb = new StringBuilder();                  // OK, start remembering characters
        }      
        @Override
        public void endElement(String ns, String ln, String qn) {
            if (null != dsb) {
                ds = dsb.toString();                    // documentation is the chars remembered
                dsb = null;                             // now stop remembering forever
            }
        }        
        @Override
        public void characters (char[] ch, int start, int length) {
            if (null != dsb) dsb.append(ch, start, length);
        }        
    }

}
