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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import static org.mitre.niem.NIEMConstants.NMF_NS_URI_PREFIX;
import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI_PREFIX;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.mitre.niem.nmf.*;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;

/**
 * A class for constructing a NIEM Model object from an XML model file
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLReader {
    
    protected static final Logger LOG = LogManager.getLogger(ModelXMLReader.class);    
    
    public Model readXML(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory sf = SAXParserFactory.newInstance();
        sf.setNamespaceAware(true);
        sf.setValidating(false);
        SAXParser saxp = ParserBootstrap.sax2Parser();
        Handler h = new Handler();
        saxp.parse(is, h);
        return h.model;
    }
    
    class Handler extends DefaultHandler {      
        protected Map<String,ObjectType> idMap     = new HashMap<>();     // map @id value -> object
        protected Map<ObjectType,RefRecord> refMap = new HashMap<>();     // map placeholder object - > RefRecord
        protected Stack<ObjectType> objStack       = new Stack<>();       // elements under construction
        protected Stack<Integer> lineNumStack      = new Stack<>();       // line number of start of element
        protected StringBuilder chars              = new StringBuilder();
        protected Locator loc                      = new LocatorImpl();
        protected Model model = null;
        protected int depth = 1;
        
        @Override
        public void setDocumentLocator (Locator l) {
            this.loc = l;
        }
        
        @Override
        public void startDocument () {
            ObjectType dummyRoot = new ObjectType();
            objStack.add(dummyRoot);
            lineNumStack.add(0);
        }
        
        @Override
        public void startElement(String eNamespace, String eLocalName, String eQName, Attributes atts) {
            System.out.print(String.format("%"+depth+"s+ %s\n", "", eQName));
            depth++;
           
            // Get element IDREF from XML attributes
            String ref = null;
            for (int i = 0; i < atts.getLength(); i++) {
                if ("ref".equals(atts.getLocalName(i)) && atts.getURI(i).startsWith(STRUCTURES_NS_URI_PREFIX)) {
                    ref = atts.getValue(i).trim();
                    break;
                }
            } 
            // Create appropriate object and initialize from XML attributes
            ObjectType child = newObject(model, eNamespace, eLocalName, atts, (ref != null), loc.getLineNumber());
            ObjectType parent = objStack.peek();
            
            // Element ID is part of child object; remember for ref placeholder replacement
            String id = child.getID();
            if (null != id) {
                idMap.put(id, child);
            }
            // Remember reference placeholders so we can replace them later
            else if (null != ref) {
                RefRecord rr = new RefRecord(parent, ref);
                refMap.put(child, rr);
            }   
            // First object created is the Model object
            if (objStack.size() == 1) {
                model = (Model)child;
            }
            objStack.add(child);
            lineNumStack.add(loc.getLineNumber());
        }
        
        @Override
        public void endElement(String eNamespace, String eLocalName, String eQName) {
            ObjectType child    = objStack.pop();
            ObjectType parent   = objStack.peek();
            int lineNum         = lineNumStack.pop();
            if (chars.length() > 0) {
                child.setStringVal(chars.toString().trim());
                chars = new StringBuilder();
            }
            // If this child is a @ref placeholder, remember index if child is added to a list
            int index = parent.addChild(child, -1);
            RefRecord rr = refMap.get(child);
            if (null != rr) {
                rr.index = index;
            }
            // If not a placeholder, add Model children to model object
            else {
                child.addToModel(model, -1);
            }
            depth--;
            System.out.print(String.format("%"+depth+"s- %s\n", "", eQName));            
        }
        
        @Override
        public void endDocument () {
            for (var rr : refMap.values()) {
                ObjectType parent = rr.parent;          // object with the @ref placeholder child
                ObjectType idObj  = idMap.get(rr.ref);  // object with ID matching the REF
                int index         = rr.index;
                if (null != idObj) {
                    parent.addChild(idObj, index);
                }
            }    
            model.testTraverse();
        }
        
        @Override
        public void characters (char[] ch, int start, int length) {
            chars.append(ch, start, length);
        }
        
        private ObjectType newObject (Model m, String ens, String eln, Attributes atts, boolean isRef, int lineNum) {
            ObjectType o = null;
            if (ens.startsWith(NMF_NS_URI_PREFIX)) {
                switch (eln) {
                case "AbstractIndicator":   o = new StringObject(m, ens, eln, atts); break;
                case "Class":               o = new ClassType(m, ens, eln, atts); break;
                case "ContentStyleCode":    o = new StringObject(m, ens, eln, atts); break;
                case "DataProperty":        o = new DataProperty(m, ens, eln, atts); break;
                case "Datatype":            o = new Datatype(m, ens, eln, atts); break;
                case "DefinitionText":      o = new StringObject(m, ens, eln, atts); break;
                case "Enumeration":         o = new Facet(m, ens, eln, atts); break;
                case "ExtensionOf":         o = new ExtensionOf(m, ens, eln, atts); break;
                case "FractionDigits":      o = new Facet(m, ens, eln, atts); break;
                case "HasDataProperty":     o = new HasDataProperty(m, ens, eln, atts); break;
                case "HasObjectProperty":   o = new HasObjectProperty(m, ens, eln, atts); break;
                case "HasValue":            o = new HasValue(m, ens, eln, atts); break;
                case "Length":              o = new Facet(m, ens, eln, atts); break;
                case "MaxExclusive":        o = new Facet(m, ens, eln, atts); break;
                case "MaxInclusive":        o = new Facet(m, ens, eln, atts); break;
                case "MaxLength":           o = new Facet(m, ens, eln, atts); break;
                case "MinExclusive":        o = new Facet(m, ens, eln, atts); break;
                case "MinInclusive":        o = new Facet(m, ens, eln, atts); break;
                case "MinLength":           o = new Facet(m, ens, eln, atts); break;
                case "Model":               o = new Model(m, ens, eln, atts); break;
                case "Name":                o = new StringObject(m, ens, eln, atts); break;
                case "Namespace":           o = new Namespace(m, ens, eln, atts); break;
                case "NamespacePrefixName": o = new StringObject(m, ens, eln, atts); break;
                case "NamespaceURI":        o = new StringObject(m, ens, eln, atts); break;
                case "NonNegativeValue":    o = new Facet(m, ens, eln, atts); break;
                case "ObjectProperty":      o = new ObjectProperty(m, ens, eln, atts); break;
                case "Pattern":             o = new Facet(m, ens, eln, atts); break;
                case "PositiveValue":       o = new Facet(m, ens, eln, atts); break;
                case "RestrictionOf":       o = new RestrictionOf(m, ens, eln, atts); break;
                case "StringValue":         o = new StringObject(m, ens, eln, atts); break;
                case "SubPropertyOf":       o = new SubPropertyOf(m, ens, eln, atts); break;
                case "TotalDigits":         o = new Facet(m, ens, eln, atts); break;
                case "UnionOf":             o = new UnionOf(m, ens, eln, atts); break;
                case "WhiteSpace":          o = new Facet(m, ens, eln, atts); break;
                default:
                    LOG.error("unknown element '{'{}'}'{} at line {}", ens, eln, lineNum);
                    o = new UnknownObject(m, ens, eln, atts);
                    break;
                }
            }
            else {
                LOG.error("unknown element '{'{}'}'{} at line {}", ens, eln, lineNum);
                o = new UnknownObject(m, ens, eln, atts);
            }
            return o;
        }
    }
    
    
    public class UnknownObject extends ObjectType {
        UnknownObject (Model m, String ens, String eln, Attributes atts) {
        }
    }
    
    
    /**
     * A record for a @ref element that needs to be filled in by the matching
     * @id element object.  We create one of these for each reference 
     * placeholder.  When the model document is completely processed, we
     * replace each placeholder with the matching @id object.
     */
    class RefRecord {
        ObjectType parent =  null;      // the model object with the ref placeholder child
        int index = -1;                 // index of the placeholder if in a list, otherwise -1
        String ref = null;              // the @ref (or @uri) value
        
        RefRecord (ObjectType o, String r) {
            parent = o;
            ref = r;                    
        }
    }

}
