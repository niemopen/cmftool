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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import static org.apache.xerces.xs.XSComplexTypeDefinition.CONTENTTYPE_ELEMENT;
import static org.apache.xerces.xs.XSConstants.DERIVATION_EXTENSION;

import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.MODEL_GROUP;
import static org.apache.xerces.xs.XSConstants.TYPE_DEFINITION;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSNamespaceItem;
import org.apache.xerces.xs.XSNamespaceItemList;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import static org.apache.xerces.xs.XSTypeDefinition.COMPLEX_TYPE;
import org.apache.xerces.xs.XSWildcard;
import static org.mitre.niem.NIEMConstants.XSD_NS_URI;
import org.mitre.niem.nmf.ClassType;
import org.mitre.niem.nmf.Component;
import org.mitre.niem.nmf.ExtensionOf;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.nmf.NMFException;
import org.mitre.niem.nmf.Namespace;
import static org.mitre.niem.xsd.NamespaceDecls.SK_EXTERNAL;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSD {
    static final Logger LOG = LogManager.getLogger(ModelFromXSD.class);
    
    private final NamespaceDecls nsDecls = new NamespaceDecls();
    
    public Model createModel (Schema s) throws ParserConfigurationException, SAXException, IOException, NMFException {
        Model m = new Model();
        XSModel xs = s.xsmodel();
        generateNamespaces(m, xs);
        generateClasses(m, xs);
        return m;
    }
    
    private void generateNamespaces (Model m, XSModel xs) throws ParserConfigurationException, SAXException, IOException, NMFException {
        List<Namespace> nsObjs  = new ArrayList<>();
        Set<String> nsList      = new HashSet<>();
        XSNamespaceItemList nsl = xs.getNamespaceItems();
        for (int i = 0; i < nsl.getLength(); i++) {
            XSNamespaceItem nsi = nsl.item(i);
            String nsuri = nsi.getSchemaNamespace();
            if (!XSD_NS_URI.equals(nsuri)) {            // skip the xs: namespace
                String schemaDoc = "";
                nsList.add(nsuri);
                StringList docl = nsi.getDocumentLocations();
                if (docl.size() < 1) {
                    LOG.error("No document listed for namespace {}", nsuri);
                } else {
                    if (docl.size() > 1) {
                        LOG.warn("Multiple documents listed for namespace {}?", nsuri);
                    }
                    StringBuilder docStr = new StringBuilder();
                    String furi = docl.item(0);
                    nsDecls.processNamespace(nsuri, furi);
                }
            }
        }
        for (String nsuri : nsList) {
//            if (!nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) {      // skip structures NS
                Namespace nsobj = new Namespace(m);
                nsobj.setNamespacePrefix(nsDecls.getPrefix(nsuri));
                nsobj.setNamespaceURI(nsuri);
                nsobj.setDefinition(nsDecls.getDocumentation(nsuri));
                m.addNamespace(nsobj);
//            }
        }
    }    
    
    private void generateClasses (Model m, XSModel xs) throws NMFException {
        XSNamedMap xmap = xs.getComponents(TYPE_DEFINITION);
        for (int i = 0; i < xmap.getLength(); i++) {
            XSTypeDefinition t = (XSTypeDefinition)xmap.item(i);
            genClassType(m, t);
        }
    }
    
    private ClassType genClassType (Model m, XSTypeDefinition t) throws NMFException {
        String nsuri = t.getNamespace();
        String name = t.getName();
        ClassType c = m.getClassType(nsuri, name);
        if (null != c) return c;                                        // already defined
        if (XSD_NS_URI.equals(nsuri)) return null ;                     // skip types in xs: namespace
        if (SK_EXTERNAL == nsDecls.getNSType(nsuri)) return null ;      // skip types in external namespace
        if (COMPLEX_TYPE != t.getTypeCategory()) return null ;          // only complex types
        XSComplexTypeDefinition ct = (XSComplexTypeDefinition) t;  
        
        c = new ClassType(m);
        c.setAbstractIndicator(ct.getAbstract() ? "true" : null);
        c.setName(name);
        c.setNamespace(m.getNamespace(nsuri));
        c.setDefinition(nsDecls.getDocumentation(nsuri));
        System.out.println(String.format("%-20s %-60s", name, nsuri));
        m.addClassType(c);
        
        // Generate ExtensionOf object for type defs with xs:extension
        if (CONTENTTYPE_ELEMENT != ct.getContentType()) return c ;   // only types with children    
        if (DERIVATION_EXTENSION == ct.getDerivationMethod()) {
            XSTypeDefinition base = ct.getBaseType();
            ClassType baseClassType = genClassType(m, base);
            if (null != baseClassType) {
                ExtensionOf ext = genExtensionOf(m, c, baseClassType, ct);
                c.setExtensionOf(ext);
            }
        }
        return c;
    }
    
    public ExtensionOf genExtensionOf (Model m, ClassType derived, ClassType base, XSComplexTypeDefinition ct) {   
        ExtensionOf ext = new ExtensionOf(m);
        ext.setClassType(base);
        
        // Particle of the derived complex type def should be a model group
        XSTerm pt = ct.getParticle().getTerm();
        if (MODEL_GROUP != pt.getType()) {
            LOG.warn("genExtensionOf thought complex type def particle would be a model group!");
            return ext;
        }        
        // If derived type defines elements, model group should have two entries; otherwise one
        XSModelGroup g   = (XSModelGroup)pt;
        XSObjectList mglist = g.getParticles();
        if (2 != mglist.getLength()) {
            if (1 != mglist.getLength()) LOG.warn("genExtensionOf expected model group with 1 or 2 entries");
            return ext;
        }
        // Second entry in model group should be the extension sequence (model group)        
        XSParticle p = (XSParticle)mglist.item(mglist.getLength()-1);
        pt = p.getTerm();
        if (MODEL_GROUP != pt.getType()) {
            LOG.warn("genExtensionOf thought last item in model group would be a sequence");
            return ext;
        }
        // Sequence should be all element declarations (the elements defined in this class)
        g = (XSModelGroup)pt;
        mglist = g.getParticles();
        for (int i = 0; i < mglist.getLength(); i++) {
            p = (XSParticle)mglist.get(i);
            pt = p.getTerm();
            if (ELEMENT_DECLARATION != pt.getType()) {
                LOG.warn("genExtensionOf thought sequence item #{} should be element decls", i);
                continue;
            }
            XSElementDeclaration ed = (XSElementDeclaration)pt;
            System.out.println(String.format("  element %s", ed.getName()));
        }
        return ext;
    }
    


}
