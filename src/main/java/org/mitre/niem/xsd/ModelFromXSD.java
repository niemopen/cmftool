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
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_ELEMENT;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_EMPTY;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_MIXED;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_SIMPLE;
import static org.apache.xerces.xs.XSConstants.DERIVATION_EXTENSION;
import static org.apache.xerces.xs.XSConstants.DERIVATION_NONE;
import static org.apache.xerces.xs.XSConstants.DERIVATION_RESTRICTION;
import static org.apache.xerces.xs.XSConstants.DERIVATION_SUBSTITUTION;
import static org.apache.xerces.xs.XSConstants.DERIVATION_UNION;

import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.MODEL_GROUP;
import static org.apache.xerces.xs.XSConstants.TYPE_DEFINITION;
import static org.apache.xerces.xs.XSConstants.WILDCARD;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import static org.apache.xerces.xs.XSModelGroup.COMPOSITOR_ALL;
import static org.apache.xerces.xs.XSModelGroup.COMPOSITOR_CHOICE;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSNamespaceItem;
import org.apache.xerces.xs.XSNamespaceItemList;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import static org.apache.xerces.xs.XSTypeDefinition.COMPLEX_TYPE;
import static org.apache.xerces.xs.XSTypeDefinition.SIMPLE_TYPE;
import org.apache.xerces.xs.XSWildcard;
import static org.mitre.niem.NIEMConstants.NIEM_XS_PREFIX;
import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI_PREFIX;
import static org.mitre.niem.NIEMConstants.XSD_NS_URI;
import org.mitre.niem.nmf.ClassType;
import org.mitre.niem.nmf.Component;
import static org.mitre.niem.nmf.Component.C_OBJECTPROPERTY;
import org.mitre.niem.nmf.DataProperty;
import org.mitre.niem.nmf.Datatype;
import org.mitre.niem.nmf.HasDataProperty;
import org.mitre.niem.nmf.HasObjectProperty;
import org.mitre.niem.nmf.HasValue;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.nmf.NMFException;
import org.mitre.niem.nmf.Namespace;
import org.mitre.niem.nmf.ObjectProperty;
import org.mitre.niem.nmf.RestrictionOf;
import static org.mitre.niem.xsd.NamespaceDecls.SK_EXTERNAL;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSD {
    static final Logger LOG = LogManager.getLogger(ModelFromXSD.class);
    
    private NamespaceDecls nsDecls = null;
    private Model m = null;
    private XSModel xs = null;
    
    public Model createModel (Schema s) throws ParserConfigurationException, SAXException, IOException, NMFException {
        nsDecls = new NamespaceDecls();
        m = new Model();
        xs = s.xsmodel();
        generateNamespaces();
        generateClasses();
//        generateProperties();
//        generateTypes();
        return m;
    }
    
    private void generateNamespaces () {
        Set<String> nsList      = new HashSet<>();
        XSNamespaceItemList nsl = xs.getNamespaceItems();
        for (int i = 0; i < nsl.getLength(); i++) {
            XSNamespaceItem nsi = nsl.item(i);
            String nsuri = nsi.getSchemaNamespace();
            if (nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) continue;   // skip structures namespace
            if (nsuri.startsWith(NIEM_XS_PREFIX)) continue;             // skip the proxy namespace
            nsList.add(nsuri);
            StringList docl = nsi.getDocumentLocations();
            if (docl.size() < 1) {
                if (!XSD_NS_URI.equals(nsuri)) LOG.error("No document listed for namespace {}", nsuri);                
            } 
            else {
                if (docl.size() > 1) LOG.warn("Multiple documents listed for namespace {}?", nsuri);
                String furi = docl.item(0);
                nsDecls.processNamespace(nsuri, furi);
            }
        }
        // All namespaces now processed, with preferred prefix assigned
        // Create namespace objects and add to model
        for (String nsuri : nsList) {
            Namespace nsobj = new Namespace(m);
            nsobj.setNamespacePrefix(nsDecls.getPrefix(nsuri));
            nsobj.setNamespaceURI(nsuri);
            nsobj.setDefinition(nsDecls.getDocumentation(nsuri));
            try {
                m.addNamespace(nsobj);
            } catch (NMFException ex) {
                java.util.logging.Logger.getLogger(ModelFromXSD.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }   
    
    private void generateClasses () {
        XSNamedMap xmap = xs.getComponents(TYPE_DEFINITION);
        for (int i = 0; i < xmap.getLength(); i++) {
            XSTypeDefinition t = (XSTypeDefinition)xmap.item(i); 
            createClass(t);
        }
    }
    
    private ClassType createClass(XSTypeDefinition t) {     
        String nsuri = t.getNamespace();
        String cname = t.getName();
        ClassType clobj = m.getClassType(nsuri, cname);
        if (null != clobj) return clobj;       
        if (XSD_NS_URI.equals(nsuri)) return null ;                    // skip types in xs: namespace
        if (nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) return null;   // skip types in structures namespace
        if (nsuri.startsWith(NIEM_XS_PREFIX)) return null;             // skip types in the proxy namespace        
        if (SK_EXTERNAL == nsDecls.getNSType(nsuri)) return null;      // skip types in external namespace
        if (SIMPLE_TYPE == t.getTypeCategory()) return null;           // skip simple types (handled elsewhere)     
        
        XSComplexTypeDefinition ct = (XSComplexTypeDefinition)t;
        clobj = new ClassType(m);
        initComponent(clobj, ct);
        addElementProperties(clobj, ct);
        addAttributeProperties(clobj, ct);
        addBaseType(clobj, ct);
        
        try {
            m.addClassType(clobj);
        } catch (NMFException ex) {
            java.util.logging.Logger.getLogger(ModelFromXSD.class.getName()).log(Level.SEVERE, null, ex);
        }
        return clobj;
    }
    
    private void addElementProperties (ClassType clobj, XSComplexTypeDefinition ct) {
        // Particle of the complex typedef should have a model group term
        XSParticle par = ct.getParticle();  if (null == par) return;
        XSTerm pt      = par.getTerm();     if (null == pt) return;
        if (MODEL_GROUP != pt.getType()) {
            LOG.warn("addElementProperties expected model group as complex typedef's particle, not type #{}", pt.getType());
            return;
        }
        // The last term in the model group is a particle with element declarations for this type
        XSModelGroup g = (XSModelGroup)pt;
        XSObjectList list = g.getParticles();
        par = (XSParticle)list.item(list.getLength()-1);
        pt  = par.getTerm();
        if (MODEL_GROUP != pt.getType() ) {
            LOG.warn("addElementProperties expected model group as last term, not type #{}", pt.getType());
            return;
        }
        // Iterate through terms in this model group to handle element declarations
        g = (XSModelGroup)pt;
        list = g.getParticles();
        for (int i = 0; i < list.getLength(); i++) {
            par = (XSParticle)list.item(i);
            pt  = par.getTerm();
            if (pt.getName().endsWith("AugmentationPoint")) continue;   // skip augmentation point elements
            if (ELEMENT_DECLARATION != pt.getType()) continue;
            clobj.setContentStyleCode("HasObjectProperty");
            XSElementDeclaration ed = (XSElementDeclaration)pt;
            XSTypeDefinition edt = ed.getTypeDefinition();
            if (COMPLEX_TYPE == edt.getTypeCategory()) {
                ObjectProperty op = createObjectProperty(ed); 
                if (null == op) continue;
                HasObjectProperty hop = new HasObjectProperty(m);
                hop.addObjectProperty(op);
                hop.setSequenceID(String.format("%d", i+1));
                hop.setMinOccursQuantity(String.format("%d", par.getMinOccurs()));
                hop.setMaxOccursQuantity(par.getMaxOccursUnbounded() ? "unbounded" : String.format("%d", par.getMaxOccurs()));
                clobj.addHasObjectProperty(hop);
            }
            else {
                DataProperty dp = createDataProperty(ed);
                if (null == dp) continue;
                HasDataProperty hdp = new HasDataProperty(m);
                hdp.addDataProperty(dp);
                hdp.setSequenceID(String.format("%d", i+1));
                hdp.setMinOccursQuantity(String.format("%d", par.getMinOccurs()));
                hdp.setMaxOccursQuantity(par.getMaxOccursUnbounded() ? "unbounded" : String.format("%d", par.getMaxOccurs()));                    
                clobj.addHasDataProperty(hdp);
             }
        }
    }
    
    private void addAttributeProperties (ClassType clobj, XSComplexTypeDefinition ct) {
        // First build a set of all attribute uses (in this type and in its base types)
        XSObjectList atl = ct.getAttributeUses();
        Set<XSAttributeUse> auses = new HashSet<>();
        for (int i = 0; i < atl.getLength(); i++) {
            XSAttributeUse au = (XSAttributeUse)atl.item(i);
            auses.add(au);
        }
        // Now remove attribute uses in the base types
        System.out.println(String.format("Adding attributes to %s", ct.getName()));
        XSTypeDefinition base = ct.getBaseType();
        while (null != base) {
            System.out.println(String.format("base=%s %s", base.getName(), base.getNamespace()));
            if (COMPLEX_TYPE != base.getTypeCategory()) base = null;
            else {
                atl = ((XSComplexTypeDefinition) base).getAttributeUses();
                for (int i = 0; i < atl.getLength(); i++) {
                    XSAttributeUse au = (XSAttributeUse) atl.item(i);
                    auses.remove(au);
                }
                base = base.getBaseType();
                if (XSD_NS_URI.equals(base.getNamespace()) && "anyType".equals(base.getName())) break;
            }
        }
        // Now create data properties for the remaining attribute uses
        for (XSAttributeUse au : auses) {
            XSAttributeDeclaration ad = au.getAttrDeclaration();
            DataProperty dp = createDataProperty(ad);
            if (null != dp) {
                HasDataProperty hdp = new HasDataProperty(m);
                hdp.addDataProperty(dp);
                if (au.getRequired()) hdp.setMinOccursQuantity("1");
                else hdp.setMinOccursQuantity("0");
                clobj.addHasDataProperty(hdp);
            }
        }
    }
    
    private void addBaseType (ClassType clobj, XSComplexTypeDefinition ct) {
        XSTypeDefinition base = ct.getBaseType();         
        while (null != base && base.getNamespace().startsWith(NIEM_XS_PREFIX)) {
            base = base.getBaseType();
        }
        // Extending a simple type?  ClassType with HasValue; base becomes a Datatype
        if (null != base && SIMPLE_TYPE == base.getTypeCategory()) {
            Datatype dtype = createDatatype((XSSimpleTypeDefinition) base);
            HasValue hv = new HasValue(m);
            hv.setDatatype(dtype);
            clobj.addHasValue(hv);
            clobj.setContentStyleCode("HasValue");
        } // Complex type becomes extensionOfClass property. But not the types from structures
        else if (null != base && !base.getNamespace().startsWith(STRUCTURES_NS_URI_PREFIX)) {
            ClassType baseObj = createClass((XSComplexTypeDefinition) base);
            if (null != baseObj) clobj.setExtensionOfClass(baseObj);
        }        
    }
    
    private ObjectProperty createObjectProperty (XSElementDeclaration ed) {
        String nsuri = ed.getNamespace();
        String cname = ed.getName();        
        ObjectProperty op = m.getObjectProperty(nsuri, cname);
        if (null != op) return op;
        if (cname.endsWith("AugmentatioPoint")) return null;        // skip augmentatations
        op = new ObjectProperty(m);
        initComponent(op, ed);
        
        XSTypeDefinition t = ed.getTypeDefinition();
        ClassType ct = createClass(t);
        op.setClassType(ct);
        
        try {
            m.addObjectProperty(op);
        } catch (NMFException ex) {
            java.util.logging.Logger.getLogger(ModelFromXSD.class.getName()).log(Level.SEVERE, null, ex);
        }
        return op;
    }
    
    private DataProperty createDataProperty (XSObject t) {
        String nsuri = t.getNamespace();
        String cname = t.getName(); 
        DataProperty dp = m.getDataProperty(nsuri, cname);
        if (null != dp) return dp;
        if (nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) return null;    // skip elements and attributes in structures
        dp = new DataProperty(m);
        initComponent(dp, t);
        
        if (ELEMENT_DECLARATION == t.getType()) {
            XSTypeDefinition base = ((XSElementDeclaration)t).getTypeDefinition();
        }

        
        try {
            m.addDataProperty(dp);
        } catch (NMFException ex) {
            java.util.logging.Logger.getLogger(ModelFromXSD.class.getName()).log(Level.SEVERE, null, ex);
        }
        return dp;
    }
    

    private void dumpParticle (XSParticle p, int depth) {
        XSTerm t = p.getTerm();
        System.out.print(String.format("%"+depth+"s", ""));
        if (null == t) System.out.print("null");
        else switch (t.getType()) {
            case MODEL_GROUP: 
                XSModelGroup mg = (XSModelGroup)t;
                System.out.print("ModelGroup "); 
                if (COMPOSITOR_ALL == mg.getCompositor()) System.out.print("All");
                else if (COMPOSITOR_CHOICE == mg.getCompositor()) System.out.print("Choice");
                else System.out.print("Sequence");
                break;
            case ELEMENT_DECLARATION: System.out.print(String.format("Element %s", t.getName())); break;
            case WILDCARD: System.out.print("Wildcard"); break;
            default: System.out.print(t.getType());
        }
        System.out.print(String.format(" [%d %s]\n",
                p.getMinOccurs(),
                p.getMaxOccursUnbounded() ? "unbounded" : p.getMaxOccurs()));
        if (MODEL_GROUP == t.getType()) {
            XSModelGroup mg = (XSModelGroup)t;
            XSObjectList objs = mg.getParticles();
            for (int i = 0; i < objs.getLength(); i++) {
                XSParticle pp = (XSParticle)objs.item(i);
                dumpParticle(pp, depth+2);
            }
        }
        
    }
    
    private Datatype createDatatype (XSSimpleTypeDefinition st) {
        System.out.println(String.format("DT %-40.40s %-40.40s", st.getName(), st.getNamespace())); 
        String cname = st.getName();
        String nsuri = st.getNamespace();
        Datatype d  = m.getDatatype(nsuri, cname);
        if (null != d) return d;                    // already created
        
        d = new Datatype(m);
        initComponent(d, st);
        try {
            m.addDatatype(d);
        } catch (NMFException ex) {
            java.util.logging.Logger.getLogger(ModelFromXSD.class.getName()).log(Level.SEVERE, null, ex);
        }
        return d;
    }
    
    private void initComponent (Component c, XSObject o) {
        c.setName(o.getName());
        c.setNamespace(m.getNamespace(o.getNamespace()));
        c.setDefinition(getDefinition(o));
    }
    
    private void generateProperties () {
        XSNamedMap xmap = xs.getComponents(ELEMENT_DECLARATION);
        for (int i = 0; i < xmap.getLength(); i++) {
            XSElementDeclaration e = (XSElementDeclaration)xmap.item(i);
            XSTypeDefinition t = e.getTypeDefinition();
            if (e.getNamespace().startsWith(STRUCTURES_NS_URI_PREFIX)) continue;    // skip structures
            if (e.getName().endsWith("AugmentationPoint")) continue;                // skip augmentation points
            if (SK_EXTERNAL == nsDecls.getNSType(e.getNamespace())) continue;       // skip external namespaces
            if (null == t) continue;                                                // skip untyped properties FIXME?
            if (COMPLEX_TYPE == t.getTypeCategory()) {
                XSComplexTypeDefinition ct = (XSComplexTypeDefinition)t;
                if (CONTENTTYPE_ELEMENT == ct.getContentType() || hasSemanticAttribute(ct)) 
                    generateObjectProperty(e, ct);
                else
                    generateDataProperty(e, ct);
            }
            // FIXME -- simple types?
        }
    }
    
    private void generateObjectProperty (XSElementDeclaration e, XSComplexTypeDefinition ct) {
        System.out.println(String.format("OP %-40.40s: type=%-20.20s %s", 
                e.getName(),
                ct.getName(),
                ct.getNamespace()));
        XSObjectList anl  = e.getAnnotations();
        String def = getDefinition(e);
        ObjectProperty op = new ObjectProperty(m);
        op.setName(e.getName());
        op.setNamespace(m.getNamespace(e.getNamespace()));
        op.setDefinition(def);
        if (e.getAbstract()) op.setAbstractIndicator("true");
        XSElementDeclaration sub = e.getSubstitutionGroupAffiliation();
        if (null != sub) {
            System.out.println(String.format("  subpropOf %s", sub.getName()));
        }
        try {
            m.addObjectProperty(op);
        } catch (NMFException ex) {
            LOG.warn("Adding property {}: {}", e.getName(), ex.getMessage());
        }
     }
   
    private void generateDataProperty (XSElementDeclaration e, XSComplexTypeDefinition ct) {
        System.out.println(String.format("DP %-40.40s: type=%-20.20s %s", 
                e.getName(),
                ct.getName(),
                ct.getNamespace()));

    }
    
    private void generateTypes () {
        XSNamedMap xmap = xs.getComponents(TYPE_DEFINITION);
        for (int i = 0; i < xmap.getLength(); i++) {
            XSTypeDefinition t = (XSTypeDefinition)xmap.item(i);
            String nsuri = t.getNamespace();
            if (XSD_NS_URI.equals(nsuri)) continue ;                    // skip types in xs: namespace
            if (nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) continue;   // skip types in structures namespace
            if (nsuri.startsWith(NIEM_XS_PREFIX)) continue;             // skip types in the proxy namespace        
            if (SK_EXTERNAL == nsDecls.getNSType(nsuri)) continue;      // skip types in external namespace
            if (SIMPLE_TYPE == t.getTypeCategory()) continue;          // skip simple types (handled elsewhere)
            XSComplexTypeDefinition ct = (XSComplexTypeDefinition) t;
            if (CONTENTTYPE_SIMPLE != ct.getContentType() || hasSemanticAttribute(ct)) 
                generateClass(ct);
            else
                generateDatatype(ct);
        }
    }
    
    private void generateClass (XSComplexTypeDefinition ct) {
//        System.out.println(String.format("CL %-40.40s %-40.40s", ct.getName(), ct.getNamespace()));
    }
    
    // Complex type with simple content and no semantic attributes.
    // Generate datatype for the base type (FooSimpleType) but give it the name
    // of the complex type (FooType).
    private void generateDatatype (XSComplexTypeDefinition ct) {
//        System.out.println(String.format("DT %d %-40.40s %-40.40s", ct.getDerivationMethod(), ct.getName(), ct.getNamespace()));     
        String cname = ct.getName();
        String nsuri = ct.getNamespace();
        XSTypeDefinition base = ct.getBaseType();
        if (SIMPLE_TYPE != base.getTypeCategory()) {
            LOG.error("Base of {}#{} can't be a complex type", nsuri, cname);
            return;
        }
        XSSimpleTypeDefinition st = (XSSimpleTypeDefinition)base;
        Datatype d = createDatatype(st);
        d.setName(cname);
        d.setNamespace(m.getNamespace(nsuri));
        d.setDefinition(getDefinition(ct));
        try {
            m.addDatatype(d);
        } catch (NMFException ex) {
            java.util.logging.Logger.getLogger(ModelFromXSD.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    


//    private void generateClasses (Model m, XSModel xs) throws NMFException {
//        XSNamedMap xmap = xs.getComponents(TYPE_DEFINITION);
//        for (int i = 0; i < xmap.getLength(); i++) {
//            XSTypeDefinition t = (XSTypeDefinition)xmap.item(i);
//            genClassType(m, t);
//        }
//    }
//    
//    private ClassType genClassType (Model m, XSTypeDefinition t) throws NMFException {
//        String nsuri = t.getNamespace();
//        String name = t.getName();
//        ClassType c = m.getClassType(nsuri, name);
//        if (null != c) return c;                                        // already defined
//        if (XSD_NS_URI.equals(nsuri)) return null ;                     // skip types in xs: namespace
//        if (nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) return null;    // skip types in structures namespace
//        if (nsuri.startsWith(NIEM_XS_PREFIX)) return null;              // skip types in the proxy namespace        
//        if (SK_EXTERNAL == nsDecls.getNSType(nsuri)) return null ;      // skip types in external namespace
//        if (COMPLEX_TYPE != t.getTypeCategory()) return null ;          // only complex types
//        XSComplexTypeDefinition ct = (XSComplexTypeDefinition) t;  
//        
//        c = new ClassType(m);
//        c.setAbstractIndicator(ct.getAbstract() ? "true" : null);
//        c.setName(name);
//        c.setNamespace(m.getNamespace(nsuri));
//        c.setDefinition(nsDecls.getDocumentation(nsuri));
//        System.out.println(String.format("%-20s %-60s", name, nsuri));
//        m.addClassType(c);
//        
//        // Generate ExtensionOf object for type defs with xs:extension
//        if (CONTENTTYPE_ELEMENT != ct.getContentType()) return c ;   // only types with children    
//        if (DERIVATION_EXTENSION == ct.getDerivationMethod()) {
//            XSTypeDefinition base = ct.getBaseType();
//            ClassType baseClassType = genClassType(m, base);
////            if (null != baseClassType) {
////                ExtensionOf ext = genExtensionOf(m, c, baseClassType, ct);
////                c.setExtensionOf(ext);
////            }
//        }
//        return c;
//    }
    
//    public ExtensionOf genExtensionOf (Model m, ClassType derived, ClassType base, XSComplexTypeDefinition ct) {   
//        ExtensionOf ext = new ExtensionOf(m);
//        ext.setClassType(base);
//        
//        // Particle of the derived complex type def should be a model group
//        XSTerm pt = ct.getParticle().getTerm();
//        if (MODEL_GROUP != pt.getType()) {
//            LOG.warn("genExtensionOf thought complex type def particle would be a model group!");
//            return ext;
//        }        
//        // If derived type defines elements, model group should have two entries; otherwise one
//        XSModelGroup g   = (XSModelGroup)pt;
//        XSObjectList mglist = g.getParticles();
//        if (2 != mglist.getLength()) {
//            if (1 != mglist.getLength()) LOG.warn("genExtensionOf expected model group with 1 or 2 entries");
//            return ext;
//        }
//        // Second entry in model group should be the extension sequence (model group)        
//        XSParticle p = (XSParticle)mglist.item(mglist.getLength()-1);
//        pt = p.getTerm();
//        if (MODEL_GROUP != pt.getType()) {
//            LOG.warn("genExtensionOf thought last item in model group would be a sequence");
//            return ext;
//        }
//        // Sequence should be all element declarations (the elements defined in this class)
//        g = (XSModelGroup)pt;
//        mglist = g.getParticles();
//        for (int i = 0; i < mglist.getLength(); i++) {
//            p = (XSParticle)mglist.get(i);
//            pt = p.getTerm();
//            if (ELEMENT_DECLARATION != pt.getType()) {
//                LOG.warn("genExtensionOf thought sequence item #{} should be element decls", i);
//                continue;
//            }
//            XSElementDeclaration ed = (XSElementDeclaration)pt;
//            System.out.println(String.format("  element %s", ed.getName()));
//        }
//        return ext;
//    }
    
    
    // Component definitions are provided in the annotations.  Annotations are 
    // provided as XML text.  Parse these text strings and return the content of
    // the first xs:documentation element encountered.
    private String getDefinition (XSObject o) {
        XSObjectList anl = null;
        switch (o.getType()) {
            case TYPE_DEFINITION:
                if (SIMPLE_TYPE == ((XSTypeDefinition)o).getTypeCategory())
                    anl = ((XSSimpleTypeDefinition)o).getAnnotations();
                else anl = ((XSComplexTypeDefinition)o).getAnnotations();                
                break;
            case ELEMENT_DECLARATION:
                anl = ((XSElementDeclaration)o).getAnnotations();
                break;
            default:
                return null;
        }
        for (int i = 0; i < anl.getLength(); i++) {
            try {
                SAXParser saxp = ParserBootstrap.sax2Parser();
                AnnotationHandler h = new AnnotationHandler();                
                XSAnnotation a = (XSAnnotation)anl.item(i);
                String as = a.getAnnotationString();
                StringReader sr = new StringReader(as);
                InputSource is = new InputSource(sr);
                saxp.parse(is, h);
                String ds = h.getDocumentation();
                if (null != ds) return ds;
            } catch (SAXException | IOException | ParserConfigurationException ex) {
                LOG.error("Parsing annotation string: {}", ex.getMessage());
            }
        }
        return null;
    }
    
    private class AnnotationHandler extends DefaultHandler {
        private StringBuilder dsb = null;
        private String ds = null;
        public String getDocumentation () {
            return null == ds ? null : ds.toString();
        }       
        @Override
        public void startElement(String ns, String ln, String qn, Attributes atts) {  
            if (null != ds) return;                     // already processed a documentation element
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

    
    // Returns true if the complex type has an attribute that is not in the
    // structures namespace. 
    private static boolean hasSemanticAttribute (XSComplexTypeDefinition ct) {
        String ln = ct.getName();
        XSObjectList atl = ct.getAttributeUses();
        for (int i = 0; i < atl.getLength(); i++) {
            XSAttributeUse au = (XSAttributeUse)atl.item(i);
            XSAttributeDeclaration ad = au.getAttrDeclaration();
            if (!ad.getNamespace().startsWith(STRUCTURES_NS_URI_PREFIX)) return true;
        }
        return false;
    }


}
