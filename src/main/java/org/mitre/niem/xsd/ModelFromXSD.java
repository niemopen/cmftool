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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import static org.apache.commons.lang3.StringUtils.getCommonPrefix;
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
import org.apache.xerces.xs.XSNamespaceItem;
import org.apache.xerces.xs.XSNamespaceItemList;
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
import static org.mitre.niem.cmf.Component.C_CLASSTYPE;
import static org.mitre.niem.cmf.Component.C_DATATYPE;
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
import static org.mitre.niem.NIEMConstants.PROXY_NS_URI_PREFIX;
import org.mitre.niem.cmf.CMFException;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_CODE_LISTS_INSTANCE;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_PROXY;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinNamespaceVersion;
import static org.mitre.niem.xsd.NIEMBuiltins.isBuiltinNamespace;
import static org.mitre.niem.xsd.NamespaceInfo.NSK_BUILTIN;
import static org.mitre.niem.xsd.NamespaceInfo.NSK_EXTERNAL;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinNamespaceKind;
import org.mitre.niem.xsd.NamespaceInfo.AppinfoRec;
import static org.mitre.niem.xsd.NamespaceInfo.NSK_UNKNOWN;

/**
 * An object for constructing a Model from a Schema.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSD {
    static final Logger LOG = LogManager.getLogger(ModelFromXSD.class);
    
    private NamespaceInfo nsInfo = null;               // info from schema parsing (and not in Xerces API)
    private Model m = null;                             // model under construction
    private ModelExtension me = null;                   // model extension for XSD
    private XSModel xs = null;                          // from the Schema object
    
    /**
     * Constructs an object that can generate a Model from the Schema.
     * @param s XML Schema object
     */
    public ModelFromXSD (Schema s) {
        xs = s.xsmodel();
    }
    
    /**
     * Constructs the Model, ModelExtension, and NamespaceInfo objects from the
     * XML schema.
     * @param m Model object to be constructed
     * @param me ModelExtension object to be constructed
     * @param nsi NamespaceInfo object to be constructed
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public void createModel (Model m, ModelExtension me, NamespaceInfo nsi) throws ParserConfigurationException, SAXException, IOException {
        this.nsInfo = nsi; 
        this.m = m;
        this.me = me;
        generateNamespaces();
        generateClassesAndDatatypes();
        generateProperties();
        processAppinfo();
        processAugmentations();
    }
       
    private void generateNamespaces () {
        Map<String,String> nsFile = new HashMap<>();          // nsURI -> schema document file URI
        XSNamespaceItemList nsl = xs.getNamespaceItems();
        // One entry for each namespace URI that was a @targetNamespace in any document
        // One entry if there is a no-namespace document
        for (int i = 0; i < nsl.getLength(); i++) {
            XSNamespaceItem nsi = nsl.item(i);
            String nsuri = nsi.getSchemaNamespace();
            StringList docl = nsi.getDocumentLocations();
            if (docl.size() < 1) {
                if (!XSD_NS_URI.equals(nsuri)) LOG.error("No document listed for namespace {}", nsuri);                
            } 
            else {
              LOG.debug("Processing namespace {}", nsuri);
              if (docl.size() > 1) LOG.warn("Multiple documents listed for namespace {}?", nsuri);
              nsFile.put(nsuri,docl.item(0));
              for (int j = 0; j < docl.getLength(); j++) nsInfo.processSchemaDocument(docl.item(j));
            }
        }
        // Find the root directory of the schema document pile
        // Make each document path relative to that directory
        String rootDirURI = getCommonPrefix(nsFile.values().toArray(new String[0]));
        int rlen = rootDirURI.length();
        
        // All namespaces now processed, with preferred prefix assigned
        // Remember relative path to schema documents
        // Create namespace objects and add to model
        nsFile.forEach((String nsuri, String furi) -> {
            me.setDocumentFilepath(nsuri, furi.substring(rlen));
            me.setPrefix(nsuri, nsInfo.getPrefix(nsuri));
            me.setNIEMVersion(nsuri, nsInfo.getNIEMVersion(nsuri));
            int nsk = nsInfo.getNSType(nsuri);
            int bik = getBuiltinNamespaceKind(nsuri);
            // Don't create Namespace for builtins (except code-lists-instance) or unknowns
            if ((NIEM_CODE_LISTS_INSTANCE == bik || 0 > bik) && NSK_UNKNOWN != nsk) {
                Namespace nsobj = new Namespace(nsInfo.getPrefix(nsuri), nsuri);
                nsobj.setNamespaceURI(nsuri);
                nsobj.setDefinition(nsInfo.getDocumentation(nsuri));
                if (NSK_EXTERNAL == nsInfo.getNSType(nsuri)) nsobj.setIsExternal(true);
                try {
                    m.addNamespace(nsobj);
                } catch (CMFException ex) {
                    // CAN'T HAPPEN
                }
                me.setConformanceTargets(nsuri, nsInfo.getConformanceTargets(nsuri));
                me.setNIEMVersion(nsuri, nsInfo.getNIEMVersion(nsuri));
                me.setNSVersion(nsuri, nsInfo.getNSVersion(nsuri));
            }
        });
        // ModelExtension values set for structures, proxy, code list instance namespaces
        // Appinfo, code list schema, and conformanceTarget built-ins don't have namespaceItems
        // Make ModelExtension entries for them anyway
        nsFile.forEach((nsuri, furi) -> {
            if (NSK_BUILTIN != nsInfo.getNSType(nsuri)) {
                for (String declaredNS : nsInfo.getAllNamespacesDeclared(nsuri)) {
                    if (isBuiltinNamespace(declaredNS)) {
                        String prefix = nsInfo.getPrefix(declaredNS);
                        String version = getBuiltinNamespaceVersion(declaredNS);
                        if (null != prefix) me.setPrefix(declaredNS, prefix);
                        if (null != version) me.setNIEMVersion(declaredNS, version);
                    }
                }
            }
        });
        // Create namespace for XSD if it doesn't already exist
        // (Sometimes people import it, sometimes they don't)
        if (null == m.getNamespaceByURI(XSD_NS_URI)) {
            Namespace xsd = new Namespace(nsInfo.getPrefix(XSD_NS_URI), XSD_NS_URI);
            try {
                m.addNamespace(xsd);
            } catch (CMFException ex) {
                // CAN'T HAPPEN
            }
        }
    }   
    
    private void generateClassesAndDatatypes () {
        XSNamedMap xmap = xs.getComponents(XSTypeDefinition.COMPLEX_TYPE);
        for (int i = 0; i < xmap.getLength(); i++) {
            XSComplexTypeDefinition ct = (XSComplexTypeDefinition)xmap.item(i); 
            if (ct.getNamespace().startsWith(PROXY_NS_URI_PREFIX)) continue;    // don't generate unused built-in datatypes         
            if (ct.getNamespace().equals(XSD_NS_URI)) continue;                 // don't generate unused built-in datatypes
            if (ct.getName().endsWith("SimpleType")) continue;                  // ignore here, create if needed elsewhere
            createClassOrDatatype(ct);
        }
    }
    
    // Recursively process base types depth-first.
    // 1. Complex content becomes a ClassType object
    // 2. Simple content with a ClassType base becomes a ClassType
    // 2. Simple content with attributes becomes a ClassType object with HasValue
    // 3. Simple content w/o attributes
    //    A. Base is a ClassType object:  becomes a ClassType object
    //    B. Base is FooSimpleType: create FooType Datatype from FooSimpleType
    private Component createClassOrDatatype (XSTypeDefinition t) {
        String nsuri = t.getNamespace();
        String cname = t.getName();        
        Component c  = m.getComponent(nsuri, cname);
        if (null != c) return c;                                                // already processed this complex type def
        if (XSD_NS_URI.equals(nsuri) && "anyType".equals(cname)) return null;   // recursion ends here
        if (nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) return null;            // skip types in structures namespace
        if (NSK_EXTERNAL == nsInfo.getNSType(nsuri)) return null;               // skip types in external namespaces
        if (NSK_UNKNOWN == nsInfo.getNSType(nsuri)) return null;                // skip types in unknown namespaces
        
        // Simple type is always a Datatype
        if (SIMPLE_TYPE == t.getTypeCategory()) {
            return createDatatype((XSSimpleTypeDefinition)t);
        }
        // Recursive call: complete base type before finishing with this type
        XSComplexTypeDefinition ct = (XSComplexTypeDefinition)t;
        XSTypeDefinition bt = ct.getBaseType();
        Component base = null;
        if (null != bt) base = createClassOrDatatype(bt);
        // Create a ClassType object if there are element or attribute properties,
        // or if the base is a ClassType
        List<XSParticle> elist   = collectClassElements(ct);
        Set<XSAttributeUse> aset = collectClassAttributes(ct);
        if (!elist.isEmpty() 
                || !aset.isEmpty() 
                || (null != base && C_CLASSTYPE == base.getType())) {
            ClassType clobj = new ClassType(m.getNamespaceByURI(ct.getNamespace()), ct.getName());
            clobj.setDefinition(getDefinition(ct));
            LOG.debug("Creating ClassType {}", clobj.getQName());
            if (ct.getAbstract()) clobj.setIsAbstract("true");
            if (null != base) {
                if (C_CLASSTYPE == base.getType()) clobj.setExtensionOfClass((ClassType)base);
                else clobj.setHasValue((Datatype)base);
            }
            int seq = 1;
            for (XSParticle p : elist) {
                XSTerm pt = p.getTerm();
                XSElementDeclaration e = (XSElementDeclaration) pt;
                XSTypeDefinition edt = e.getTypeDefinition();
                Property op = createProperty(pt, edt);
                
                // Augmentation point elements will be removed from the model.
                // Don't add the augmentation point as a property of this ClassPoint.
                // Do remember that this complex type has one in the CMF-XSD extension.
                if (e.getName().endsWith("AugmentationPoint")) clobj.setIsAugmentable(true);
                else if (null != op) {
                    HasProperty hop = new HasProperty();
                    hop.setProperty(op);
                    hop.setSequenceID(String.format("%d", seq++));
                    hop.setMinOccurs(p.getMinOccurs());
                    hop.setMaxOccurs(p.getMaxOccurs());
                    hop.setMaxUnbounded(p.getMaxOccursUnbounded());
                    clobj.addHasProperty(hop);
                }                               
            }
            for (XSAttributeUse au : aset) {
                XSAttributeDeclaration ad = au.getAttrDeclaration();
                XSTypeDefinition adt = ad.getTypeDefinition();
                Property dp = createProperty(ad, adt);
                if (null != dp) {
                    me.setIsAttribute(dp.getQName());
                    HasProperty hdp = new HasProperty();
                    hdp.setProperty(dp);
                    hdp.setMaxOccurs(1);
                    if (au.getRequired()) hdp.setMinOccurs(1);
                    else hdp.setMinOccurs(0);
                    clobj.addHasProperty(hdp);
                }              
            }
            m.addComponent(clobj);
            return clobj;
        }  
        // For a proxy niem-xs:foo type, just return the base xs:foo type
        if (NIEM_PROXY == getBuiltinNamespaceKind(ct.getNamespace())) return base;

        // When FooType extends FooSimpleType, rename the FooSimpleType object 
        // as FooType and return that.
        if (null != base && bt.getName().endsWith("SimpleType")) {
            Datatype bdt = (Datatype) base;
            bdt.setName(ct.getName());
            bdt.setNamespace(m.getNamespaceByURI(ct.getNamespace()));
            bdt.setDefinition(getDefinition(ct));
//            m.addComponent(bdt);
            LOG.debug("Creating Datatype {} from {}SimpleType", bdt.getQName(), ct.getName());
            return base;
        }
        // For an empty extension of a simple type, create a Datatype with an
        // empty restriction of the base
        if (null != base) {
            Datatype bdt = (Datatype)base;
            RestrictionOf r = new RestrictionOf();
            Datatype dt = new Datatype(m.getNamespaceByURI(ct.getNamespace()), ct.getName());
            dt.setDefinition(getDefinition(ct));
            r.setDatatype(bdt);
            dt.setRestrictionOf(r);
            m.addComponent(dt);
            LOG.debug("Creating Datatype {} from {}", dt.getQName(), bdt.getQName());            
            return dt;
        }
        // At this point we have a complex type with no child elements 
        // and no semantic attributes.  There is either no base type, or the
        // base type is in an external namespace or the structures namespace.
        // For that, we have a ClassType with no properties
        ClassType clobj = new ClassType(m.getNamespaceByURI(ct.getNamespace()), ct.getName());
        clobj.setDefinition(getDefinition(ct));
        m.addComponent(clobj);
        LOG.debug("Creating empty ClassType {}", clobj.getQName());
        return clobj;
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
    
    // Returns a list of attributes declared by this complex type.  The Xerces API 
    // gives us those attributes plus all attributes inherited from the base types.
    // We have to remove the inherited attributes.     
    private Set<XSAttributeUse> collectClassAttributes (XSComplexTypeDefinition ct) {
        // First build a set of all attribute uses (in this type and in its base types)
        Set<XSAttributeUse> aset = new HashSet<>();
        XSObjectList atl = ct.getAttributeUses();
        for (int i = 0; i < atl.getLength(); i++) {
            XSAttributeUse au = (XSAttributeUse)atl.item(i);
            XSAttributeDeclaration a = au.getAttrDeclaration();
            // Don't add attributes from the structures namespace
            if (!a.getNamespace().startsWith(STRUCTURES_NS_URI_PREFIX)) aset.add(au);
        }
        // Now remove attribute uses in the base types
        XSTypeDefinition base = ct.getBaseType();
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
        
    private void generateProperties () {
        XSNamedMap xmap = xs.getComponents(ELEMENT_DECLARATION);
        for (int i = 0; i < xmap.getLength(); i++) {
            XSElementDeclaration e = (XSElementDeclaration)xmap.item(i); 
            XSTypeDefinition t = e.getTypeDefinition();
            String nsuri = e.getNamespace();
            if (NSK_EXTERNAL == nsInfo.getNSType(nsuri)) continue;     // only generate external properties if referenced
            createProperty(e, t);
        }        
        xmap = xs.getComponents(ATTRIBUTE_DECLARATION);
        for (int i = 0; i < xmap.getLength(); i++) {
            XSAttributeDeclaration a = (XSAttributeDeclaration)xmap.item(i);
            XSTypeDefinition t = a.getTypeDefinition();
            String nsuri = a.getNamespace();
            if (NSK_EXTERNAL == nsInfo.getNSType(nsuri)) continue;     // only generate external attributes if referenced
            Property p = createProperty(a, t);
            if (null != p) me.setIsAttribute(p.getQName());
        }
    }
    
    private Property createProperty (XSObject o, XSTypeDefinition t) {
        String nsuri = o.getNamespace();
        String cname = o.getName();       
        Property op = m.getProperty(nsuri, cname);
        if (null != op) return op;
        if (nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) return null;    // skip properties in structures namespace
        if (NSK_UNKNOWN == nsInfo.getNSType(nsuri)) return null;        // skip properties in unknown namespaces    
        op = new Property(m.getNamespaceByURI(o.getNamespace()), o.getName());
        op.setDefinition(getDefinition(o));
        m.addComponent(op);
        LOG.debug("Creating Property {}", op.getQName());
        
        // If an external property, we're done; no substitution or types for those
        if (NSK_EXTERNAL == nsInfo.getNSType(nsuri)) return op;
        
        if (ELEMENT_DECLARATION == o.getType()) {
            XSElementDeclaration ed = (XSElementDeclaration)o;
            XSElementDeclaration sub = ed.getSubstitutionGroupAffiliation();
            // Handle substitution, but not for external components
            if (null != sub) {
                XSTypeDefinition st = sub.getTypeDefinition();      // substitution group == subproperty
                Property sp = createProperty(sub, st);
                op.setSubPropertyOf(sp);
            }
            if (ed.getAbstract()) op.setIsAbstract("true");
            if (ed.getNillable()) me.setIsNillable(op.getQName());
            
        }
        else if (ATTRIBUTE_DECLARATION == o.getType()) {
            
        }
        Component tc = createClassOrDatatype(t);
        if (null != tc) {
            if (C_DATATYPE == tc.getType()) op.setDatatype((Datatype)tc);
            else op.setClassType((ClassType)tc);
        }
        return op;
    }
       
    private Datatype createDatatype (XSSimpleTypeDefinition st) {
        String cname = st.getName();
        String nsuri = st.getNamespace();
        Datatype d  = m.getDatatype(nsuri, cname);
        if (null != d) return d;                    // already created
        d = new Datatype(m.getNamespaceByURI(st.getNamespace()), st.getName());
        d.setDefinition(getDefinition(st));
        m.addComponent(d);
        LOG.debug("Creating Datatype {}", d.getQName());
        if (XSD_NS_URI.equals(nsuri)) return d;     // xs: types are primatives
        switch(st.getVariety()) {
            case VARIETY_UNION:
                d.setUnionOf(createUnionOf(st));
                break;
            case VARIETY_LIST:
                XSSimpleTypeDefinition it = st.getItemType();
                Datatype idt = createDatatype(it);
                d.setListOf(idt);
                break;
            default:
                d.setRestrictionOf(createRestrictionOf(st));
        }
        return d;
    }
    
    // Appinfo attributes are not available through the Xerces XSD API, so we
    // collect them while we are parsing the schema documents for namespace 
    // declarations and import elements.  Now we iterate through the collected
    // attributes and adjust model elements accordingly.
    private void processAppinfo () {
        for (AppinfoRec ar : nsInfo.getAppinfo()) {
            switch (ar.attribute) {
                case "deprecated":
                    Component c = m.getComponent(ar.nsuri, ar.lname);
                    if (null != c) c.setIsDeprecated(ar.value);
                    break;
                case "externalAdapterTypeIndicator":
                    ClassType cl = m.getClassType(ar.nsuri, ar.lname);
                    if (null != cl) cl.setIsExternal(ar.value);
                    break;
            }
        }
    }
    
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
            // If Property p has an augmentation type, add type children to the augmented type
            ClassType ptype = p.getClassType();
            if (null != ptype && ptype.getName().endsWith("AugmentationType")) {
                for (HasProperty hp : ptype.hasPropertyList()) {
                    addAugmentPropertyToClass(augmented, p.getNamespace(), hp, true);
                }
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
            if (ahp.maxUnbounded()) augHP.setMaxUnbounded(true);
            else if (!augHP.maxUnbounded()) augHP.setMaxOccurs(max(augHP.maxOccurs(), ahp.maxOccurs()));
        }
        if (fromAugType) augHP.augmentTypeNS().add(ans);
        else augHP.setAugmentElementNS(ans);
    }
    
    private UnionOf createUnionOf (XSSimpleTypeDefinition st) {
        UnionOf u = new UnionOf();
        XSObjectList members = st.getMemberTypes();
        if (null == members || members.getLength() < 1) return null;
        for (int i = 0; i < members.getLength(); i++) {
            XSSimpleTypeDefinition mt = (XSSimpleTypeDefinition)members.item(i);
            Datatype mdt = createDatatype(mt);
            if (null != mdt) u.addDatatype(mdt);
        }
        return u;
    }
    
    private RestrictionOf createRestrictionOf (XSSimpleTypeDefinition st) {
        XSTypeDefinition base = st.getBaseType();
        if (null == base) return null;
        if (SIMPLE_TYPE != base.getTypeCategory()) return null;
        if (XSD_NS_URI.equals(base.getNamespace()) && "anySimpleType".equals(base.getName())) return null;
        Datatype bt = createDatatype((XSSimpleTypeDefinition)base);
        if (null == bt) return null;
        RestrictionOf r = new RestrictionOf();
        r.setDatatype(bt);
        XSObjectList flist = st.getFacets();
        int facetCt = 0;
        for (int i = 0; i < flist.getLength(); i++) {
            XSFacet f = (XSFacet)flist.item(i);
            if (XSD_NS_URI.equals(st.getNamespace()) && FACET_WHITESPACE == f.getFacetKind()) continue; // FIXME
            if (XSD_NS_URI.equals(base.getNamespace()) && "token".equals(base.getName()) && FACET_WHITESPACE == f.getFacetKind()) continue;
            Facet fo  = new Facet();
            fo.setFacetKind(facetKind(f));
            fo.setStringVal(f.getLexicalFacetValue());
            fo.setDefinition(getDefinition(f));
            r.addFacet(fo);
            facetCt++;
        }
        flist = st.getMultiValueFacets();
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
                facetCt++;
            }
        }
        return 0 == facetCt ? null : r;     // no RestrictionOf without Facet
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
    
    private void initComponent (Component c, XSObject o) {
        c.setName(o.getName());
        c.setNamespace(m.getNamespaceByURI(o.getNamespace()));
        c.setDefinition(getDefinition(o));
    }

    
    // Component definitions are provided in the annotations.  Annotations are 
    // provided as XML text.  Parse these text strings and return the content of
    // the first xs:documentation element encountered.
    private String getDefinition (XSObject o) {
        XSObjectList alist = getAnnotations(o);
        if (null == alist) return null;
        for (int i = 0; i < alist.getLength(); i++) {           
            XSAnnotation a = (XSAnnotation)alist.item(i);
            String ds = parseDefinition(a);
            if (null != ds) return ds.trim();
        }
        return null;
    }
    
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
