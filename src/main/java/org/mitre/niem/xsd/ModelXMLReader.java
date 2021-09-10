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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.NIEMConstants.NMF_NS_URI_PREFIX;
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
    static final Logger LOG = LogManager.getLogger(ModelXMLReader.class);
    
    public Model readXML(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory sf = SAXParserFactory.newInstance();
        sf.setNamespaceAware(true);
        sf.setValidating(false);
        SAXParser saxp = sf.newSAXParser();
        Handler h = new Handler();
        saxp.parse(is, h);
        return h.model;
    }
    
    class Handler extends DefaultHandler {      
        private final Set<ObjectType> realObjs      = new HashSet<>();     // set of non-placeholder model objects
        private final Map<String,XObjectType> idMap = new HashMap<>();     // map @id value -> object
        private final List<XObjectType> refPHolders = new ArrayList<>();   // list of IDREF/URI placeholder objects to be replaced
        private final Stack<XObjectType> objStack   = new Stack<>();       // elements under construction
        private StringBuilder chars                 = new StringBuilder(); // character content of current element
        private Locator loc                         = new LocatorImpl();   // for error msg line numbers
        private XModel xmodel = null;                                      // the model document element
        private Model model = null;                                        // the model object
        
        @Override
        public void setDocumentLocator (Locator l) {
            this.loc = l;
        }
        
        // Put a dummy element on the stack instead of coding for the empty
        // stack condition.  When we reach the end of the document element, 
        // we'll try to add it to this dummy element... and nothing will happen.
        @Override
        public void startDocument () {
            XObjectType dummyRoot = new XObjectType();
            objStack.add(dummyRoot);
        }
        
        @Override
        public void startElement(String eNamespace, String eLocalName, String eQName, Attributes atts) {
            // Create appropriate object and initialize from XML attributes
            XObjectType parent = objStack.peek();
            XObjectType child = newObject(model, parent, eNamespace, eLocalName, atts, loc.getLineNumber());
            // First object created is the Model object
            if (objStack.size() == 1) {             // element #0 is the dummy
                xmodel = (XModel)child;
                model = (Model)child.getObject();
            }
            objStack.add(child);
            chars = new StringBuilder();
        }
        
        @Override
        public void endElement(String eNamespace, String eLocalName, String eQName) {
            XObjectType child    = objStack.pop();
            XObjectType parent   = objStack.peek();          
            if (chars.length() > 0) {
                child.setStringVal(chars.toString().trim());
                chars = new StringBuilder();
            }            
            // Remember objects with @id, or non-empty objects with @uri
            // Use these to replace reference placeholder objects upon endDocument
            if (null != child.getIDKey()) {
                idMap.put(child.getIDKey(), child);
            }
            // Remember reference placeholder objects to be replaced upon endDocument
            if (null != child.getRefKey()) {
                refPHolders.add(child);             // remember IDREF/URI placeholder for later replacement
            }
            // Not a placeholder? Remember object to be added to model at endDocument.
            else {
                ObjectType obj = child.getObject();
                if (null != obj && obj.isModelChild()) realObjs.add(obj);
            }
            // Add child to parent even if child is a placeholder
            parent.addAsChild(child);      
        }
        
        @Override
        public void endDocument () {
            for (var ro : refPHolders) {
                String ref = ro.getRefKey();        // IDREF or URI attribute string value
                XObjectType idObj = idMap.get(ref); // object with ID = ref string
                if (null == idObj) {
                    LOG.error("no matching ID for IDREF/URI {} at line {}", ref, ro.getLineNumber());
                }
                else {
                    ro.setIDRepl(idObj);            // set so that addChild executes a replacement
                    ro.getParent().addAsChild(ro);  // replace the placeholder with the referenced object
                }
            }
            // Placeholders replaced, now collect real component objects for Model
            for (ObjectType obj : realObjs) {
                if (null == obj) continue;
                obj.addToModelSet(model);
            }
        }
        
        @Override
        public void characters (char[] ch, int start, int length) {
            chars.append(ch, start, length);
        }
        
        private XObjectType newObject (Model m, XObjectType p, String ens, String eln, Attributes atts, int lineNum) {
            XObjectType o = null;
            if (ens.startsWith(NMF_NS_URI_PREFIX)) {
                switch (eln) {
                case "AbstractIndicator":   o = new XStringObject(m, p, ens, eln, atts, lineNum); break;
                case "Class":               o = new XClassType(m, p, ens, eln, atts, lineNum); break;
                case "Datatype":            o = new XDatatype(m, p, ens, eln, atts, lineNum); break;
                case "DefinitionText":      o = new XStringObject(m, p, ens, eln, atts, lineNum); break;
                case "Enumeration":         o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "ExtensionOfClass":    o = new XClassType(m, p, ens, eln, atts, lineNum); break;
                case "FractionDigits":      o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "HasProperty":         o = new XHasProperty(m, p, ens, eln, atts, lineNum); break;
                case "HasValue":            o = new XDatatype(m, p, ens, eln, atts, lineNum); break;
                case "Length":              o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "ListOf":              o = new XDatatype(m, p, ens, eln, atts, lineNum); break;
                case "MaxExclusive":        o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "MaxInclusive":        o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "MaxLength":           o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "MinExclusive":        o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "MinInclusive":        o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "MinLength":           o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "Model":               o = new XModel(m, p, ens, eln, atts, lineNum); break;
                case "Name":                o = new XStringObject(m, p, ens, eln, atts, lineNum); break;
                case "Namespace":           o = new XNamespace(m, p, ens, eln, atts, lineNum); break;
                case "NamespacePrefixName": o = new XStringObject(m, p, ens, eln, atts, lineNum); break;
                case "NamespaceURI":        o = new XStringObject(m, p, ens, eln, atts, lineNum); break;
                case "NonNegativeValue":    o = new XStringObject(m, p, ens, eln, atts, lineNum); break;
                case "Property":            o = new XProperty(m, p, ens, eln, atts, lineNum); break;
                case "Pattern":             o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "PositiveValue":       o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "RestrictionOf":       o = new XRestrictionOf(m, p, ens, eln, atts, lineNum); break;
                case "StringValue":         o = new XStringObject(m, p, ens, eln, atts, lineNum); break;
                case "SubPropertyOf":       o = new XProperty(m, p, ens, eln, atts, lineNum); break;
                case "TotalDigits":         o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "UnionOf":             o = new XUnionOf(m, p, ens, eln, atts, lineNum); break;
                case "WhiteSpace":          o = new XFacet(m, p, ens, eln, atts, lineNum); break;
                case "WhiteSpaceValueCode": o = new XStringObject(m, p, ens, eln, atts, lineNum); break;
                default:
                    LOG.error("unknown element '{'{}'}'{} at line {}", ens, eln, lineNum);
                    o = new XUnknownObject(m, p, ens, eln, atts, lineNum);
                    break;
                }
            }
            else {
                LOG.error("unknown element '{'{}'}'{} at line {}", ens, eln, lineNum);
                o = new XUnknownObject(m, p, ens, eln, atts, lineNum);
            }
            return o;
        }
    }
    
    public class XUnknownObject extends XObjectType {
        XUnknownObject (Model m, XObjectType p, String ens, String eln, Attributes atts, int lineNum) {
        }
    }

}
