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

import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ObjectType;
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
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;
import static org.mitre.niem.NIEMConstants.CMF_NS_URI_PREFIX;
import org.mitre.niem.cmf.CMFException;
import org.xml.sax.SAXParseException;

/**
 * A class for constructing a NIEM Model object from an XML model file
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLReader { 
   
    List<String> msgList = new ArrayList<>();       // messages from last CMF parsing
    
    public ModelXMLReader () { }
    
    /**
     * Create a Model object from a CMF model input stream, or null if a valid
     * Model cannot be constructed.
     * @param is model input stream
     * @return Model
     */
    public Model readXML(InputStream is) {
        msgList = new ArrayList<>();
        try {
            SAXParserFactory sf = SAXParserFactory.newInstance();
            sf.setNamespaceAware(true);
            sf.setValidating(false);
            SAXParser saxp = sf.newSAXParser();
            Handler h = new Handler(msgList);
            saxp.parse(is, h);
            if (!h.fatalErr) return h.model;
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            msgList.add(ex.toString());
        }
        return null;    // caught an exception
    }
    
    /**
     * Returns the list of messages produced by the last readXML operation.
     * @return message list
     */
    public List<String> getMessages ()  { return msgList; }
    
    
    // A single parsing pass through the CMF input stream means we must remember
    // all the element references, and replace them with the target element at
    // the end of the document.  All of this reference resolution happens before
    // any objects are added to the Model object.
    //
    // The only references allowed in a CMF document are for ClassType, 
    // Datatype, Namespace, and Property elements.  Nothing else is nillble.
    // So we don't have to worry about replacing a reference at the correct
    // index in a list.
    
    class Handler extends DefaultHandler {      
        private final Set<ObjectType> mObjs         = new HashSet<>();     // model children to be added at endDocument
        private final Map<String,XObjectType> idMap = new HashMap<>();     // map @id value -> target element
        private final List<XObjectType> refPHolders = new ArrayList<>();   // list of reference placeholder elements to be replaced
        private final Stack<XObjectType> objStack   = new Stack<>();       // XML elements under construction
        private StringBuilder chars                 = new StringBuilder(); // character content of current element
        private Locator loc                         = new LocatorImpl();   // for error msg line numbers
        private List<String> msgList                = new ArrayList<>();   // list of error messages
        private boolean fatalErr                    = false;               // true if we can't complete a valid Model
        private Model model                         = null;                // the model object
        
        Handler (List<String> ml) { msgList = ml; }
        
        @Override
        public void setDocumentLocator (Locator l) {
            this.loc = l;
        }
        
        // Put a dummy element on the stack instead of coding for the empty
        // stack condition.  When we reach the end of the document element, 
        // we'll add it to this dummy element... which does nothing.
        @Override
        public void startDocument () {
            XObjectType dummyRoot = new XObjectType();
            objStack.add(dummyRoot);
        }
        
        @Override
        public void startElement(String eNamespace, String eLocalName, String eQName, Attributes atts) {
            // Create appropriate element and initialize from XML attributes
            XObjectType parent = objStack.peek();
            XObjectType child = newObject(model, parent, eNamespace, eLocalName, atts, loc.getLineNumber());
            // First object created is the Model object
            if (objStack.size() == 1) {             // stack contains only dummy element #0
                model = (Model)child.getObject();   // in which case we just created the Model object
            }
            objStack.add(child);
            chars = new StringBuilder();
        }
        
        @Override
        public void endElement(String eNamespace, String eLocalName, String eQName) {
            XObjectType child    = objStack.pop();
            XObjectType parent   = objStack.peek();          
            child.setStringVal(chars.toString().trim());
            chars = new StringBuilder();
                        
            // Remember elements with @id, or non-empty objects with @uri
            // Use these to handle reference placeholder elements upon endDocument
            if (null != child.getIDKey()) {
                idMap.put(child.getIDKey(), child);
            }
            // Remember reference placeholder objects to be replaced upon endDocument
            if (null != child.getRefKey()) {
                refPHolders.add(child);             // remember IDREF/URI placeholder element for later replacement
            }
            // Not a placeholder? Remember object to be added to model at endDocument.
            else {
                ObjectType obj = child.getObject();
                if (null != obj && obj.isModelChild()) 
                    mObjs.add(obj);
                parent.addAsChild(child);
            }     
        }
        
        @Override
        public void endDocument () {
            // Handle all placeholder elements; add real object to parent object
            for (var ro : refPHolders) {
                String ref         = ro.getRefKey();        // IDREF or URI attribute string value
                XObjectType parent = ro.getParent();        // parent element with the reference child element
                XObjectType idObj  = idMap.get(ref);        // element with ID = ref string
                if (null == idObj) {
                    msgList.add(String.format("no matching ID for IDREF/URI %s at line %s", ref, ro.getLineNumber()));
                    fatalErr = true;
                }
                else if (ro.getObject().getClass() != idObj.getObject().getClass()) {
                    msgList.add(String.format("IDREF/URI type mismatch at line %s", ro.getLineNumber()));
                    fatalErr = true;
                }
                else {
                    ro.setObject(idObj.getObject());
                    parent.addAsChild(ro);
                }
            }
            if (fatalErr) return;
            
            // Placeholders replaced, now add the model children to the Model
            for (ObjectType obj : mObjs   ) {
                try {
                    obj.addToModel(model);
                } catch (CMFException ex) {     // duplicate namespace prefix!
                    msgList.add(ex.getLocalizedMessage());
                    fatalErr = true;
                }
            }
        }
        
        @Override
        public void characters (char[] ch, int start, int length) {
            chars.append(ch, start, length);
        }
        
        @Override
        public void error (SAXParseException ex) {
            msgList.add(String.format("SAX[error] %s", ex.getLocalizedMessage()));
        }
        @Override
        public void fatalError (SAXParseException ex) {
            msgList.add(String.format("SAX[fatal] %s", ex.getLocalizedMessage()));
            fatalErr = true;
        }
        @Override
        public void warning (SAXParseException ex) {
            msgList.add(String.format("SAX[warn]  %s", ex.getLocalizedMessage()));
        }
        
        private XObjectType newObject (Model m, XObjectType p, String ens, String eln, Attributes atts, int lineNum) {
            XObjectType o = null;
            if (ens.startsWith(CMF_NS_URI_PREFIX)) {    // try to read any version of CMF
                switch (eln) {
                case "AugmentationNamespace": o = new XNamespace(m, p, ens, eln, atts, lineNum); break;                
                case "AugmentRecord":         o = new XAugmentRecord(m, p, ens, eln, atts, lineNum); break;
                case "Class":                 o = new XClassType(m, p, ens, eln, atts, lineNum); break;
                case "CodeListBinding":       o = new XCodeListBinding(m, p, ens, eln, atts, lineNum); break;
                case "Datatype":              o = new XDatatype(m, p, ens, eln, atts, lineNum); break;
                case "ExtensionOfClass":      o = new XClassType(m, p, ens, eln, atts, lineNum); break;
                case "HasProperty":           o = new XHasProperty(m, p, ens, eln, atts, lineNum); break;
                case "LocalTerm":             o = new XLocalTerm(m, p, ens, eln, atts, lineNum); break;
                case "ListOf":                o = new XDatatype(m, p, ens, eln, atts, lineNum); break;
                case "Model":                 o = new XModel(m, p, ens, eln, atts, lineNum); break;
                case "Namespace":             o = new XNamespace(m, p, ens, eln, atts, lineNum); break;
                case "Property":              o = new XProperty(m, p, ens, eln, atts, lineNum); break;
                case "RestrictionOf":         o = new XRestrictionOf(m, p, ens, eln, atts, lineNum); break;
                case "SchemaDocument":        o = new XSchemaDocument(m, p, ens, eln, atts, lineNum); break;
                case "SubPropertyOf":         o = new XProperty(m, p, ens, eln, atts, lineNum); break;
                case "UnionOf":               o = new XUnionOf(m, p, ens, eln, atts, lineNum); break;

                case "Enumeration":
                case "FractionDigits":
                case "Length":       
                case "MaxExclusive": 
                case "MaxInclusive": 
                case "MaxLength":    
                case "MinExclusive": 
                case "MinInclusive": 
                case "MinLength":    
                case "Pattern":      
                case "TotalDigits":  
                case "WhiteSpace":
                    o = new XFacet(m, p, ens, eln, atts, lineNum);
                    break;

                case "AbstractIndicator":
                case "AttributeIndicator":
                case "AugmentableIndicator":
                case "AugmentationIndex":
                case "CodeListColumnName":
                case "CodeListConstrainingIndicator":
                case "CodeListURI":
                case "ConformanceTargetURIList":
                case "DefinitionText":   
                case "DeprecatedIndicator":
                case "DocumentFilePathText":
                case "ExternalAdapterTypeIndicator":
                case "MaxOccursQuantity": 
                case "MetadataIndicator":
                case "MinOccursQuantity": 
                case "Name":          
                case "NamespaceKindCode":
                case "NamespacePrefixText":
                case "NamespaceURI":       
                case "NIEMVersionText":
                case "NonNegativeValue": 
                case "OrderedPropertyIndicator":
                case "PositiveValue":
                case "RefAttributeIndicator":
                case "ReferenceableIndicator":
                case "RelationshipPropertyIndicator":
                case "SchemaLanguageName":
                case "SchemaVersionText":
                case "SourceCitationText":
                case "SourceURIList":
                case "StringValue":    
                case "TermLiteralText":
                case "TermName":
                case "WhiteSpaceValueCode":
                    o = new XStringObject(m, p, ens, eln, atts, lineNum);
                    break;                
                
                default:
                    msgList.add(String.format("Unknown element {%s}%s at line %d in CMF input", ens, eln, lineNum));
                    o = new XUnknownObject(m, p, ens, eln, atts, lineNum);
                    break;
                }
            }
            else {
                msgList.add(String.format("Unknown element {%s}%s at line %d in CMF input", ens, eln, lineNum));
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
