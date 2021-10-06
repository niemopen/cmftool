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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.mitre.niem.nmf.ClassType;
import org.mitre.niem.nmf.Component;
import static org.mitre.niem.nmf.Component.C_CLASSTYPE;
import static org.mitre.niem.nmf.Component.C_DATATYPE;
import org.mitre.niem.nmf.Datatype;
import org.mitre.niem.nmf.Facet;
import org.mitre.niem.nmf.HasProperty;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.nmf.Namespace;
import org.mitre.niem.nmf.Property;
import org.mitre.niem.nmf.RestrictionOf;
import org.mitre.niem.nmf.UnionOf;
import static org.mitre.niem.xsd.NamespaceDecls.SK_EXTERNAL;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import static org.mitre.niem.NIEMConstants.PROXY_NS_URI_PREFIX;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSD {
    static final Logger LOG = LogManager.getLogger(ModelFromXSD.class);
    
    private List<String> omitSimpleTypes = null;        // list of FooSimpleType to omit from model
    private NamespaceDecls nsDecls = null;          // namespace declaration manager
    private Model m = null;
    private XSModel xs = null;
    
    public Model createModel (Schema s) throws ParserConfigurationException, SAXException, IOException {
        omitSimpleTypes = new ArrayList<>();
        nsDecls = new NamespaceDecls();        
        m = new Model();
        xs = s.xsmodel();
        generateNamespaces();
        generateClassesAndDatatypes();
        generateProperties();
        omitSimpleTypes();
        return m;
    }
       
    private void generateNamespaces () {
        Set<String> nsList      = new HashSet<>();
        XSNamespaceItemList nsl = xs.getNamespaceItems();
        for (int i = 0; i < nsl.getLength(); i++) {
            XSNamespaceItem nsi = nsl.item(i);
            String nsuri = nsi.getSchemaNamespace();
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
            if (nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) continue;   // skip structures namespace
            if (nsuri.startsWith(PROXY_NS_URI_PREFIX)) continue;        // skip the proxy namespace
            if (SK_EXTERNAL == nsDecls.getNSType(nsuri)) continue;      // skip external namespaces
            Namespace nsobj = new Namespace(m);
            nsobj.setNamespacePrefix(nsDecls.getPrefix(nsuri));
            nsobj.setNamespaceURI(nsuri);
            nsobj.setDefinition(nsDecls.getDocumentation(nsuri));
            m.addNamespace(nsobj);
        }
    }   
    
    private void generateClassesAndDatatypes () {
        XSNamedMap xmap = xs.getComponents(XSTypeDefinition.COMPLEX_TYPE);
        for (int i = 0; i < xmap.getLength(); i++) {
            XSComplexTypeDefinition ct = (XSComplexTypeDefinition)xmap.item(i); 
            if (ct.getNamespace().startsWith(PROXY_NS_URI_PREFIX)) continue;    // don't generate unused built-in datatypes         
            if (ct.getNamespace().equals(XSD_NS_URI)) continue;                 // don't generate unused built-in datatypes
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
        if (SK_EXTERNAL == nsDecls.getNSType(nsuri)) return null;               // skip types in external namespaces
        
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
            ClassType clobj = new ClassType(m);
            initComponent(clobj, ct);
            if (ct.getAbstract()) clobj.setAbstractIndicator("true");
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
                if (null != op) {
                    HasProperty hop = new HasProperty(m);
                    hop.setProperty(op);
                    hop.setSequenceID(String.format("%d", seq++));
                    hop.setMinOccursQuantity(String.format("%d", p.getMinOccurs()));
                    hop.setMaxOccursQuantity(p.getMaxOccursUnbounded() ? "unbounded" : String.format("%d", p.getMaxOccurs()));
                    clobj.addHasProperty(hop);
                }                               
            }
            for (XSAttributeUse au : aset) {
                XSAttributeDeclaration ad = au.getAttrDeclaration();
                XSTypeDefinition adt = ad.getTypeDefinition();
                Property dp = createProperty(ad, adt);
                if (null != dp) {
                    HasProperty hdp = new HasProperty(m);
                    hdp.setProperty(dp);
                    hdp.setMaxOccursQuantity("1");
                    if (au.getRequired()) hdp.setMinOccursQuantity("1");
                    else hdp.setMinOccursQuantity("0");
                    clobj.addHasProperty(hdp);
                }              
            }
            m.addClassType(clobj);
            return clobj;
        }  
        // For a proxy niem-xs:foo type, just return the base xs:foo type
        if (ct.getNamespace().startsWith(PROXY_NS_URI_PREFIX)) return base;
        
        // When FooType extends FooSimpleType, rename the FooSimpleType object 
        // as FooType and return that.
        if (null != base && bt.getName().endsWith("SimpleType")) {
            Datatype bdt = (Datatype) base;
            omitSimpleTypes.add(bdt.getURI());      // remember we already handled this simple type
            m.removeDatatype(bdt);
            initComponent(bdt, ct);
            m.addDatatype(bdt);
            return base;
        }
        // For an empty extension of a simple type, create a Datatype with an
        // empty restriction of the base
        if (null != base) {
            Datatype bdt = (Datatype)base;
            Datatype dt = new Datatype(m);
            RestrictionOf r = new RestrictionOf(m);
            initComponent(dt, ct);
            r.setDatatype(bdt);
            dt.setRestrictionOf(r);
            m.addDatatype(dt);
            return dt;
        }
        // At this point we have a complex type with no child elements 
        // and no semantic attributes.  There is either no base type, or the
        // base type is in an external namespace or the structures namespace.
        // That doesn't go into the model.
        return null;
    }
    
    private List<XSParticle> collectClassElements (XSComplexTypeDefinition ct) {
        List<XSParticle> el = new ArrayList<>();        // element particles in this type
        List<XSParticle> bl = new ArrayList<>();        // element particles from base types
        XSTypeDefinition base = ct.getBaseType();
        XSParticle par = ct.getParticle();
        collectElements(par, el);                       // all elements in this type & base types 
        while (null != base) {                          // collect elements from base types
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
            createProperty(e, t);
        }        
    }
    
    private Property createProperty (XSObject o, XSTypeDefinition t) {
        String nsuri = o.getNamespace();
        String cname = o.getName();        
        Property op = m.getProperty(nsuri, cname);
        if (null != op) return op;
        if (nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) return null;    // skip properties in structures namespace
        if (SK_EXTERNAL == nsDecls.getNSType(nsuri)) return null;       // skip properties in external namespace        
        op = new Property(m);
        initComponent(op, o);
        m.addProperty(op);
        if (ELEMENT_DECLARATION == o.getType()) {
            XSElementDeclaration ed = (XSElementDeclaration)o;
            XSElementDeclaration sub = ed.getSubstitutionGroupAffiliation();
            if (null != sub) {
                XSTypeDefinition st = sub.getTypeDefinition();      // substitution group == subproperty
                Property sp = createProperty(sub, st);
                op.setSubPropertyOf(sp);
            }
            if (ed.getAbstract()) op.setAbstractIndicator("true");
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
        d = new Datatype(m);
        initComponent(d, st);
        m.addDatatype(d);
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
    
    // Remove all the FooSimpleType datatype objects that were renamed to
    // FooType.
    private void omitSimpleTypes () {
        for (String curi : omitSimpleTypes) {
            Component c = m.getComponent(curi);
            if (null != c && C_DATATYPE == c.getType()) {
                Datatype d = (Datatype)c;
                m.removeDatatype(d);
            }
        }
    }
    
    private UnionOf createUnionOf (XSSimpleTypeDefinition st) {
        UnionOf u = new UnionOf(m);
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
        RestrictionOf r = new RestrictionOf(m);
        r.setDatatype(bt);
        XSObjectList flist = st.getFacets();
        int facetCt = 0;
        for (int i = 0; i < flist.getLength(); i++) {
            XSFacet f = (XSFacet)flist.item(i);
            if (XSD_NS_URI.equals(st.getNamespace()) && FACET_WHITESPACE == f.getFacetKind()) continue; // FIXME
            if (XSD_NS_URI.equals(base.getNamespace()) && "token".equals(base.getName()) && FACET_WHITESPACE == f.getFacetKind()) continue;
            Facet fo  = new Facet(m);
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
            for (int j = 0; j < annl.getLength() && j < vals.getLength(); j++) {
                XSAnnotation an = (XSAnnotation)annl.item(j);
                String val = vals.item(j);
                String def = null;
                if (null != an) def = parseDefinition((XSAnnotation)annl.item(j));
                Facet fo   = new Facet(m);
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
        c.setNamespace(m.getNamespace(o.getNamespace()));
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
            case ELEMENT_DECLARATION: alist = ((XSElementDeclaration)o).getAnnotations(); break;
            case FACET:               alist = ((XSFacet)o).getAnnotations(); break;
            case MULTIVALUE_FACET:    alist = ((XSMultiValueFacet)o).getAnnotations(); break;
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
