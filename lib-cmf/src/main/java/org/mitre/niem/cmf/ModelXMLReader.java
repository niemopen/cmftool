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
package org.mitre.niem.cmf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.cmf.CMFObject.CMF_CLASS;
import static org.mitre.niem.cmf.CMFObject.CMF_DATAPROP;
import static org.mitre.niem.cmf.CMFObject.CMF_LIST;
import static org.mitre.niem.cmf.CMFObject.CMF_NAMESPACE;
import static org.mitre.niem.cmf.CMFObject.CMF_OBJECTPROP;
import static org.mitre.niem.cmf.CMFObject.CMF_RESTRICTION;
import static org.mitre.niem.cmf.CMFObject.CMF_UNION;
import static org.mitre.niem.utility.URIfuncs.URIStringToFile;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xsd.NIEMConstants.CMF_NS_URI;
import static org.mitre.niem.xsd.NIEMConstants.CMF_STRUCTURES_NS_URI;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A class to read a Model object from one or more CMF files.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLReader {
    static final Logger LOG = LogManager.getLogger(ModelXMLReader.class);   

    public ModelXMLReader () { }
    
    private Model model;                            // object created from all the CMF input files
    private Map<File,Map<String,CMFObject>> idmaps; // each file has its own @id -> Object map
    private boolean ok = true;                      // flag to abort readFiles() upon fatal error  
   
    /**
     * Creates a Model object from one or more CMF files in XML format.
     * @param cmfFL - list of File objects
     * @return Model object, or null upon fatal error.
     */
    public Model readFiles(List<File> cmfFL) {
        model    = new Model();
        execute(cmfFL);
        if (ok) return model;
        return null;        
    }
    
    public Model readFiles (File cmfF) {
        return readFiles(List.of(cmfF));
    }
    
    public Model addFile (Model m, File cmfF) {
        model = m;
        execute(List.of(cmfF));
        if (ok) return model;
        return null;
    }

    private void execute (List<File> cmfFL) {
        // Three phases to creating a Model object from one or more CMF files.
        //   1. Create all the Namespace objects
        //   2. Create all the Component objects
        //   3. Fully populate all objects
        ok = true;
        idmaps   = new HashMap<>();
        for (var f : cmfFL) idmaps.put(f, new HashMap<>());
        for (var f : cmfFL) createNamespaces(f);
        for (var f : cmfFL) createComponents(f);
        for (var f : cmfFL) instantiateObjects(f);
    }

    // First phase: parse all CMF files to create Namespace objects from elements.
    // They will be completely populated in phase 3.
    // Namespace elements are top-level and have @id, relative @uri, or absolute @uri:
    //   <Namespace structures:id="nc"> ...
    //   <Namespace structures:uri="#nc"> ...  
    //   <Namespace structures:uri="https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"> ...
    // Namespace ref elements are not top-level, have @ref or relative @uri.
    private void createNamespaces (File cmfF) {
        var hndlr = new NamespaceHandler(idmaps.get(cmfF));
        saxParse(cmfF, hndlr);
    }

    private static class ImportDocumentation extends CMFObject {

        public ImportDocumentation() {
        }
    }
    private class NamespaceHandler extends CMFHandler {

        private NamespaceHandler (Map<String,CMFObject> id2obj) { super(id2obj); }

        @Override
        public void startElement(String ens, String eln, String eQN, Attributes atts) throws SAXException {
            if (!CMF_NS_URI.equals(ens)) fail("%s is not a CMF element", eQN);
            super.startElement(ens, eln, eQN, atts);
            CMFObject obj = null;
            switch (eln) {
            case "Model":               obj = model; break;
            case "Namespace":           obj = new Namespace(); break;
            case "NamespacePrefixText": obj = new SimpleContent(eln); break;
            case "NamespaceURI":        obj = new SimpleContent(eln); break;
            }
            objStack.push(obj);
        }      
        @Override
        public void endElement (String ens, String eln, String eQN) throws SAXException {
            var child  = objStack.pop();
            var parent = objStack.peek();
            var locstr = locSS.peek();
            var sval   = charS.peek().toString();
            var id     = strAS.peek().id();
            var uri    = strAS.peek().uri();
            if (null == id && null != uri && uri.startsWith("#")) id = uri.substring(1); 
            if ("Namespace".equals(eln)) {
                var ns = (Namespace)child;
                if (ns.uri().isBlank()) child = null;       // it's a namespace ref element
                else if (null != id) id2obj.put(id, child);
                else if (null != uri && !uri.startsWith("#") && !uri.equals(child.uri())) 
                    fail("wrong @uri for Namespace object");
            }
            if (null != child) {
                child.setContent(sval);
                child.addToModel(eln, uri, model);
                if (null != parent) parent.addChild(eln, locstr, child);
            }
            super.endElement(ens, eln, eQN);
        }     
    }
    
    // Second phase: parse all CMF files to create Component objects from elements.
    // They will be completely populated in phase 3.
    // Outside component elements (not defined in this CMF) have absolute @uri and no name.
    // Component ref elements are not top-level and have @ref or @uri.
    // Component objects created in this phase are top-level elements with a Name,
    // Namespace, and an ID, relative URI, or absolute URI; eg:
    //   <Class structures:id="nc.TextType"> ...
    //   <DataProperty structures:uri="#nc.TextType"> ...
    //   <Datatype structures:uri="https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/TextType"> ...
    private void createComponents (File cmfF) {
        var hdnlr = new ComponentHandler(idmaps.get(cmfF));
        saxParse(cmfF, hdnlr);
    }    
    private class ComponentHandler extends CMFHandler {
           
        private ComponentHandler (Map<String,CMFObject> id2obj) { super(id2obj); }
        
        @Override
        public void startElement(String ens, String eln, String eQN, Attributes atts) throws SAXException {
            super.startElement(ens, eln, eQN, atts);
            CMFObject obj = null;
            switch (eln) {
            case "Model":           obj = model; break;                
            case "Class":           obj = new ClassType(); break;
            case "DataProperty":    obj = new DataProperty(); break;
            case "Datatype":        obj = new Datatype(); break;
            case "List":            obj = new ListType(); break;
            case "ObjectProperty":  obj = new ObjectProperty(); break;
            case "Restriction":     obj = new Restriction(); break;
            case "Union":           obj = new Union(); break;
            
            case "Name":            obj = new SimpleContent(eln); break;
            case "Namespace":       // Namespace objects were created in previous phase; look it up or fail.
                var id  = strAS.peek().id();
                var uri = strAS.peek().uri();
                var ref = strAS.peek().ref();
                if (null == id && null != ref) id = ref;
                if (null == id && null != uri && uri.startsWith("#")) id = uri.substring(1);
                if (null != id) obj = id2obj.get(id);
                if (null == obj && null != uri && !uri.startsWith("#")) obj = model.nsUToNamespaceObj(uri);
                if (null == obj) fail("No object for this Namespace reference");
                if (CMF_NAMESPACE != obj.getType()) fail ("reference %s is not a Namespace object", id);
                break;
            }
            objStack.push(obj);
        }        
        @Override
        public void endElement (String ens, String eln, String eQN) throws SAXException {
            var child  = objStack.pop();
            var parent = objStack.peek();
            var locstr = locSS.peek();
            var sval   = charS.peek().toString();
            var id  = strAS.peek().id();
            var uri = strAS.peek().uri();
            if (null == id && null != uri && uri.startsWith("#")) id = uri.substring(1);
            switch (eln) {
            case "Class":
            case "DataProperty":
            case "Datatype":
            case "List":
            case "ObjectProperty":
            case "Restriction":
            case "Union":
                // Skip ref and outside components now; handle in phase 3
                var comp = (Component)child;
                if (comp.name().isEmpty()) { child = null; break; }
                if (null != id) id2obj.put(id, child);
                if (null != uri && !uri.startsWith("#") && !uri.equals(child.uri()))
                        fail("wrong @uri for this Component (should be %s)", child.uri());                
            }
            if (null != child) {
                child.setContent(sval);
                child.addToModel(eln, uri, model);
                if (null !=  parent) parent.addChild(eln, locstr, child);
            }
            super.endElement(ens, eln, eQN);
        }   
    }

    // Third phase: Parse all CMF files to instantiate all properties of the
    // Namespace and Component objects. Some properties come from elements with
    // simple content.  Some properties come from reference elements; these have
    // no content, and look like:
    //   <Class structures:ref="nc.TextType" xsi:nil="true"/>
    //   <Class structures:uri="#nc.TextType" xsi:nil="true"/>
    //   <Class structures:uri="https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/TextType" xsi:nil="true"/> 
    private void instantiateObjects (File cmfF) {
        var hdnlr = new InstantiateHandler(idmaps.get(cmfF));
        saxParse(cmfF, hdnlr);        
    }
  
    // The work of SAX parsing a CMF file (the second pass) happens in this handler class.
    private class InstantiateHandler extends CMFHandler {

        private InstantiateHandler(Map<String,CMFObject> id2obj) { super(id2obj); }

        @Override
        public void startElement(String ens, String eln, String eQN, Attributes atts) throws SAXException {
            super.startElement(ens, eln, eQN, atts);
            CMFObject obj = null;
            var id   = strAS.peek().id();
            var ref  = strAS.peek().ref();
            var uri  = strAS.peek().uri();
            var lang = langS.peek();
            switch (eln) {
            case "Model":
                obj = model;
                break;
            case "Namespace":
                if (null == id && null != uri && uri.startsWith("#")) id = uri.substring(1);
                if (null != id) obj = id2obj.get(id);
                if (null == obj && null != ref) obj = model.prefixToNamespaceObj(ref);
                if (null == obj && null != uri && !uri.startsWith("#")) obj = model.prefixToNamespaceObj(uri);
                if (null == obj) fail("No namespace object for this reference");
                if (CMF_NAMESPACE != obj.getType()) fail("reference %s is not a Namespace", id);
                break;
            case "Class":
            case "DataProperty":
            case "Datatype":
            case "List":
            case "ListItemDatatype":
            case "ObjectProperty":
            case "Restriction":
            case "RestrictionBase":
            case "SubClassOf":
            case "SubPropertyOf":
            case "Union":
            case "UnionMemberDatatype":
                if (null == id && null != uri && uri.startsWith("#")) id = uri.substring(1);
                if (null != id) obj = id2obj.get(id);
                if (null == obj && null != ref) obj = id2obj.get(ref);
                if (null == obj && (null == uri || uri.startsWith("#"))) fail ("No component object for this reference");
                // Create placeholder for outside component
                if (null == obj) {
                    switch (eln) {
                    case "Class":               obj = new ClassType(uri); break;
                    case "DataProperty":        obj = new DataProperty(uri); break;
                    case "Datatype":            obj = new Datatype(uri); break;
                    case "List":                obj = new ListType(uri); break;
                    case "ListItemDatatype":    obj = new Datatype(uri); break;
                    case "ObjectProperty":      obj = new ObjectProperty(uri); break;
                    case "Restriction":         obj = new Restriction(uri); break;
                    case "RestrictionBase":     obj = new Datatype(uri); break;
                    case "SubClassOf":          obj = new ClassType(uri); break;
                    case "SubPropertyOf":       obj = new Property(uri); break;
                    case "Union":               obj = new Union(uri); break;
                    case "UnionMemberDatatype": obj = new Datatype(uri); break;
                    }
                    model.addChild(eln, lang, obj);
                }
                // Check whether object reference is the right kind of component
                else switch (eln) {
                    case "Class":               if (CMF_CLASS != obj.getType()) fail("reference %s is not a Class", id); break;
                    case "DataProperty":        if (CMF_DATAPROP != obj.getType()) fail("reference %s is not a DataProperty", id); break;
                    case "Datatype":            if (!obj.isDatatype()) fail("reference %s is not a Datatype", id); break;
                    case "List":                if (CMF_LIST != obj.getType()) fail("reference %s is not a List", id); break;
                    case "ListItemDatatype":    if (!obj.isDatatype()) fail("reference %s is not a Datatype", id); break;
                    case "ObjectProperty":      if (CMF_OBJECTPROP != obj.getType()) fail("reference %s is not an ObjectProperty", id); break;
                    case "Restriction":         if (CMF_RESTRICTION != obj.getType()) fail("reference %s is not a Restriction", id); break;
                    case "RestrictionBase":     if (!obj.isDatatype()) fail("reference %s is not a Datatype", id); break;
                    case "SubClassOf":          if (CMF_CLASS != obj.getType()) fail("reference %s is not a Class", id); break;
                    case "SubPropertyOf":       if (!obj.isProperty()) fail("reference %s is not a Property object", id); break;
                    case "Union":               if (CMF_UNION != obj.getType()) fail("reference %s is not a Union", id); break;
                    case "UnionMemberDatatype": if (!obj.isDatatype()) fail("reference %s is not a Datatype", id); break;
                }
                break;  // Done with Component elements
            }
            // Not a namespace or component; create right kind of object for element
            if (null == obj) switch (eln) {
                case "AnyProperty":              obj = new AnyProperty(); break;
                case "AugmentationRecord":       obj = new AugmentRecord(); break;
                case "ChildPropertyAssociation": obj = new PropertyAssociation(); break; 
                case "CodeListBinding":          obj = new CodeListBinding(); break;
                case "Facet":                    obj = new Facet(); break;
                case "ImportDocumentation":      obj = new ImportDoc(); break;
                case "List":                     obj = new Datatype(); break;
                case "LocalTerm":                obj = new LocalTerm(); break;
                case "Model":                    obj = new Model(); break;
                case "Restriction":              obj = new Restriction(); break;
                case "Union":                    obj = new Union(); break;
                
                case "ListItemDatatype":
                case "RestrictionBase":
                case "SubClassOf":
                case "SubPropertyOf":
                case "UnionMemberDatatype":
                    fail("element %s must be a reference, can't have content", eln);
                    break;

                case "AbstractIndicator":
                case "AttributeIndicator":
                case "AugmentableIndicator":
                case "AugmentationIndex":
                case "AugmentedGlobalComponentID":
                case "CodeListColumnName":
                case "CodeListConstrainingIndicator":
                case "CodeListURI":
                case "ConformanceTargetURI":
                case "DocumentationText":   
                case "DeprecatedIndicator":
                case "DocumentFilePathText":
                case "ExternalAdapterTypeIndicator":
                case "FacetCategoryCode":
                case "FacetValue":
                case "GlobalClassCode":
                case "MaxOccursQuantity": 
                case "MetadataIndicator":
                case "MinOccursQuantity": 
                case "Name":        
                case "NamespaceConstraintText":
                case "NamespaceCategoryCode":
                case "NamespaceLanguageName":
                case "NamespacePrefixText":                    
                case "NamespaceURI": 
                case "NamespaceVersionText":   
                case "NIEMVersionName":
                case "OrderedPropertyIndicator":
                case "ProcessingCode":
                case "RefAttributeIndicator":
                case "ReferenceCode":
                case "RelationshipIndicator":
                case "SourceCitationText":
                case "SourceURI":   
                case "TermLiteralText":
                case "TermName":
                case "WhiteSpaceValueCode":
                    obj = new SimpleContent(eln, lang);
                    break;
                default:
                    fail("%s is not a CMF element", eln);
            }
            objStack.push(obj);
        }
        @Override
        public void endElement (String ens, String eln, String eQN) throws SAXException {
            var lang   = langS.peek();
            var child  = objStack.pop();
            var parent = objStack.peek();
            var locstr = locSS.peek();
            var sval   = charS.peek().toString();
            if (null != child) {
                child.setContent(sval);
                if (null != parent) parent.addChild(eln, locstr, child);
            }
            super.endElement(ens, eln, eQN);
        }
    }

    
    private void saxParse (File cmfF, CMFHandler h) {
        try {
            SAXParser saxp = ParserBootstrap.sax2Parser();
            saxp.parse(cmfF, h);
        } catch (ParserConfigurationException ex) {
            LOG.error("Internal parser error: {}", ex.getMessage()); ok = false;
        } catch (IOException ex) {
            LOG.error("{}: i/o error: {}", cmfF.getName(), ex.getMessage()); ok = false;
        } catch (SAXException ex) {
            LOG.error(ex.getMessage()); ok = false; // already formatted by handler
        }
    }    
    
    // Base class of the three SAX handlers.  For each element, it keeps track of
    // the locator string (file:line#), its structures attributes, and its text
    // content, making all these available in derived startElement() endElement() calls.
    private class CMFHandler extends DefaultHandler {
        protected Map<String,CMFObject> id2obj;
        protected Locator loc = null;
        protected Stack<String> locSS        = new Stack<>();
        protected Stack<StructuresAtt> strAS = new Stack<>();
        protected Stack<StringBuilder> charS = new Stack<>();
        protected Stack<String> langS        = new Stack<>();
        protected Stack<CMFObject> objStack  = new Stack<>();
        
        private CMFHandler (Map<String,CMFObject> id2obj) { this.id2obj = id2obj; }
        
        @Override
        public void startDocument () {
            langS.push("");
            objStack.push(new CMFObject());
        }
        
        @Override
        public void startElement (String ens, String eln, String eQN, Attributes atts) throws SAXException {
            var id   = atts.getValue(CMF_STRUCTURES_NS_URI, "id");
            var ref  = atts.getValue(CMF_STRUCTURES_NS_URI, "ref");
            var uri  = atts.getValue(CMF_STRUCTURES_NS_URI, "uri");
            var lang = atts.getValue(XML_NS_URI, "lang");
            var locStr = URIStringToFile(loc.getSystemId()).getName() + ":" + loc.getLineNumber();
            if (null == lang) lang = langS.peek();
            locSS.push(locStr);
            strAS.push(new StructuresAtt(id, ref, uri));
            charS.push(new StringBuilder());
            langS.push(lang);
        }
        @Override
        public void endElement (String ens, String eln, String eQN) throws SAXException {
            locSS.pop();
            strAS.pop();
            charS.pop();
            langS.pop();
        }
        @Override
        public void characters (char[] ch, int start, int length) {
            charS.peek().append(ch, start,  length);
        }
        @Override
        public void setDocumentLocator (Locator l) {
            this.loc = l;
        } 
        @Override
        public void error (SAXParseException ex) {
            LOG.error(saxMsg("SAX error", ex));
            ok = false;
        }
        @Override
        public void fatalError (SAXParseException ex) {
            LOG.error(saxMsg("SAX fatal", ex));
            ok = false;
        }
        @Override
        public void warning (SAXParseException ex) {
            LOG.warn(saxMsg("SAX warn", ex));
        }
        protected String saxMsg (String kind, Exception ex) {
            var src = loc.getSystemId();
            var fn  = URIStringToFile(src).getName();
            var msg = String.format("%s:%s: %s: %s", fn, loc.getLineNumber(), kind, ex.getLocalizedMessage());
            return msg;
        }
        protected void fail (String fmt, Object... args) throws SAXException {
            var sysid = loc.getSystemId();
            var line  = loc.getLineNumber();
            var fname = URIStringToFile(sysid).getName();
            var msg = String.format(fmt, args);
            msg = String.format("%s:%d: %s", fname, line, msg);
            throw new SAXException(msg);
        }    
    }
    private record StructuresAtt (String id, String ref, String uri) { }
        
}
