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
package org.mitre.niem.translate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import javax.xml.parsers.ParserConfigurationException;
import static org.apache.commons.lang3.StringUtils.capitalize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Model;
import static org.mitre.niem.utility.URIfuncs.URIStringToFile;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xsd.NamespaceKind.NSK_STRUCTURES;
import static org.mitre.niem.xsd.NamespaceKind.namespaceToKind;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLMsgToJSON {
    static final Logger LOG = LogManager.getLogger(XMLMsgToJSON.class);
    
    private Model model;
    private int status = CONVERT_OK;
 
    /**
     * Constructs a new NIEM XML to NIEM JSON message transformer.
     * Messages to be transformed must be instances of the message type described
     * by the Model object.  You need a separate transformer object for each 
     * NIEM message type.  You can reuse a transformer object on any number of
     * messages of that type.
     * @param m - NIEM message type model
     */
    public XMLMsgToJSON (Model m) {
        model = m;
    }
    
    public static int CONVERT_OK = 0;
    public static int CONVERT_WARN = 1;
    
    /**
     * Creates a NIEM JSON message from a NIEM XML message, provided as an InputStream.
     * The JSON message is written into the provided (usually empty) JsonObject.
     * The XML message must conform to the NIEM model in this converter object.
     * This converter object may be reused to transform any number of XML messages
     * of the specified message format.
     * 
     * @param xmlIS - InputStream with the XML message
     * @param json - JsonObject to receive the NIEM JSON message data
     * @return - conversion status code (0 = OK, 1 = warnings encountered)
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException 
     */
    public int convert (InputSource xmlIS, JsonObject json) throws ParserConfigurationException, SAXException, IOException {
        var h   = new SAXHandler(json);
        var p   = ParserBootstrap.sax2Parser();
        p.parse(xmlIS, h);
        return(status);
    }
    
    private class SAXHandler extends DefaultHandler {
        
        private Locator loc;
        private String base = "";
        private StringBuilder chars = new StringBuilder();
        private final Stack<String> langS = new Stack<>();                // current in-scope value of xml:lang
        private final Stack<ClassType> ctypeS = new Stack<>();            // class type of CCC element in model
        private final Stack<Boolean> adapterS = new Stack<>();            // are we within an adapter property?
        private final Stack<JsonObject> objS = new Stack<>();             // json object for XML element
        
        SAXHandler(JsonObject m) {   
            objS.push(m);
            adapterS.push(false);
            ctypeS.push(null);
            langS.push("en-US");
        }
        
        @Override
        public void startElement(String nsuri, String lname, String qName, Attributes atts) {
            
            // Handle xml:base in the message element; reject it elsewhere
            var baseAtt = atts.getValue("xml:base");
            if (null != baseAtt) {
                if (1 == objS.size()) base = baseAtt;
                else { 
                    LOG.warn("xml:base on interior element at {} (ignored)", locstr());
                    status = CONVERT_WARN;
                }
            }
            // Handle xml:lang if present; preserve in-scope value otherwise
            var langAtt = atts.getValue("xml:lang");
            if (null == langAtt) langS.push(langS.peek());
            else langS.push(langAtt);
            
            var pU = model.makeURI(nsuri, lname);
            var p  = model.uriToProperty(pU);
            var ns = model.namespaceObj(nsuri);
            
            var adaptF = adapterS.peek();
            if (null != p && null != p.classType() && p.classType().isAdapterClass()) adaptF = true;
            adapterS.push(adaptF);
            
            if (null != ns && ns.isAugmentation(lname)) {
                ctypeS.push(null);
            }
            else if (!adaptF && (null == p || null == ns)) {
                LOG.warn("unknown element {} at {} (ignored)", qName, locstr());
                ctypeS.push(null);
                status = CONVERT_WARN;
            }
            else if (null == p) ctypeS.push(null);  // unknown property inside adapter element
            else ctypeS.push(p.classType());        // will be null if p is a data property
            
            var obj = new JsonObject();            
            objS.push(obj);
            
            for (int i = 0; i < atts.getLength(); i++) {
                var ansU = atts.getURI(i);              // namespace URI for this attribute
                var aQ   = atts.getQName(i);            // QName of this attribute in message
                var anam = atts.getLocalName(i);
                var aval = atts.getValue(i);
                var aU   = model.makeURI(ansU, anam);   // model URI for this component
                var aP   = model.uriToProperty(aU);
                if (NSK_STRUCTURES == namespaceToKind(ansU)) {
                    switch (anam) {
                        case "id":
                        case "ref":
                            obj.addProperty("@id", base + "#" + aval);
                            break;
                        case "uri":
                            if (aval.contains(":")) obj.addProperty("@id", aval);
                            else obj.addProperty("@id", base + aval);
                            break;
                        default:
                            LOG.warn("unknown attribute {} at {} (ignored)", aQ, locstr());        
                            status = CONVERT_WARN;
                    }
                }
                else if (anam.endsWith("Ref")) {
                    var rpnam = capitalize(anam.substring(0, anam.length()-3));
                    var rpQ   = model.makeURI(ansU, rpnam);
                    var rP    = model.uriToProperty(rpQ);
                    if (null == rP) {
                        LOG.warn("unknown reference attribute {} at {} (ignored)", aQ, locstr());
                        status = CONVERT_WARN;
                        continue;
                    }
                    if (aval.isBlank()) {
                        LOG.warn("empty reference attribute {} at {}", aQ, locstr());
                        status = CONVERT_WARN;
                        continue;
                    }
                    var refA = new JsonArray();
                    var refs = aval.split("\\s+");
                    for (int ri = 0; ri < refs.length; ri++) {
                        var ref = refs[ri];
                        var rO  = new JsonObject();
                        rO.addProperty("@id", base + "#" + ref);
                        refA.add(rO);
                    }
                    obj.add(rpQ, refA);                    
                }
                else if (null == aP || !aP.isAttribute()) {
                    if (adaptF)
                        obj.addProperty(aQ, aval);
                    else if (!"xsi:nil".equals(aQ)) {
                        LOG.warn("unknown attribute {} at {} (ignored)", aQ, locstr());                    
                        status = CONVERT_WARN;
                    }
                }
                else {
                    obj.addProperty(aQ, aval);
                }
            }
            
            chars = new StringBuilder();
            
        }

        @Override
        public void endElement(String nsuri, String lname, String qName) throws SAXException {
            var obj    = objS.pop();
            var parent = objS.peek();
            var otype  = ctypeS.pop();
            var ptype  = ctypeS.peek();
            var adaptF = adapterS.pop();
            var lang   = langS.pop();
            var pU     = model.makeURI(nsuri, lname);
            var p      = model.uriToProperty(pU);
            var ns     = model.namespaceObj(nsuri);
            var key    = qName;
            var cval   = chars.toString().trim();
            
            // For an augmentation element, just copy all the pairs into the parent.
            // But ignore any pairs from reference attributes.
            if (null != ns && ns.isAugmentation(lname)) {
                for (var ks : obj.keySet()) {
                    if (!"@id".equals(ks))
                        parent.add(ks, obj.get(ks));
                }
                return;
            }
            
            // Use model prefix for key when element is defined in model
            if (null != p) key = p.qname();
            
            // Object of literal class always has a literal property, possibly
            // inherited.
            var isPrim = false;
            if (null != p && p.isObjectProperty() && null != p.classType()) {
                var ct = p.classType();
                while (null != ct.subClassOf()) ct = ct.subClassOf();
                var lp = ct.literalDataProperty();
                if (null != lp) {
                    var valE = valuePrimitive(lp.datatype(), cval);
                    obj.add(lp.qname(), valE);
                    isPrim = true;
                }
            }
            // Simple content of unknown element with attributes is represented as @value
            else if (null == p && !obj.entrySet().isEmpty()) {
                obj.addProperty("@value", cval);
                isPrim = true;
            }
            // Add @language pair if needed in this object property
            if (isPrim && null != otype && otype.hasXmlLang() && !"en-US".equals(lang)) {
                obj.addProperty("@language", lang);
            }
            // If obj is empty at this point, then this element is a number, string, or boolean.
            // Create a primitive if this is a data property (or an unknown element)
            // Create an object with a FooLiteral pair if this is an object property.
            JsonElement value = obj;
            if (obj.entrySet().isEmpty()) {
                if (null == p) value = valuePrimitive(null, cval);
                else if (p.isDataProperty()) value = valuePrimitive(p.datatype(), cval);
                else {
                    var ct = p.classType();
                    while (null != ct.subClassOf()) ct = ct.subClassOf();
                    var lp = ct.literalDataProperty();
                    var lv = valuePrimitive(lp.datatype(), cval);
                    obj.add(lp.qname(), lv);
                }            
            }
            // Add value to parent object, key is this element QName.
            // If parent object has an array for that key, add value to that array.
            // If parent object has another element for that key, convert to an array.
            // If this property is repeatable in parent type, add new array with value.
            // Otherwise add key, value pair to parent object.
            var pel = parent.get(key);
            if (null != pel) {
                if (pel.isJsonArray()) pel.getAsJsonArray().add(value);
                else {
                    var ary = new JsonArray();
                    ary.add(pel);
                    ary.add(value);
                    parent.remove(key);
                    parent.add(key, ary);                    
                }
            }
            else if (null != ptype && ptype.isRepeatableProperty(p)) {
                var ary =  new JsonArray();
                ary.add(value);
                parent.add(key, ary);
            }
            else parent.add(key, value);
            
            // Now find any relationship properties in this object.
            // Gather them up into a single @annotation pair.
            var annP = new JsonObject();
            for (var pkey : obj.keySet()) {
                var prop = model.qnToProperty(pkey);
                if (null == prop) continue;
                if (!prop.isRelationship()) continue;
                var pval = obj.get(pkey);
                annP.add(pkey, pval);
            }
            if (!annP.isEmpty()) { 
                obj.add("@annotation", annP);
                for (var pkey : annP.keySet())
                    obj.remove(pkey);
            }
        }

        @Override
        public void characters (char[] ch, int start, int length) throws SAXException {
            chars.append(ch, start, length);
        }
        
        @Override
        public void setDocumentLocator (Locator l) {loc = l; }
        
        @Override
        public void error (SAXParseException ex) {
            LOG.error("SAX: {}", ex.getMessage());
            status = CONVERT_WARN;
        }
         
        @Override
        public void fatalError (SAXParseException ex) {
            LOG.error("SAX fatal: {}", ex.getMessage());
            status = CONVERT_WARN;
        }
        
        @Override
        public void warning (SAXParseException ex) {
            LOG.error("SAX: {}", ex.getMessage());
            status = CONVERT_WARN;
        }
        
        private String locstr () {
            var res = "";
            var sid = loc.getSystemId();
            if (null != sid) res = URIStringToFile(sid).getName() + ", ";
            res = res + "line " + loc.getLineNumber();
            return res;
        }
        
        public static final Set<String> numbers = new HashSet<>(
            Set.of("float", "double", "decimal", "integer", "nonPositiveInteger",
                    "negativeInteger", "long", "int", "short", "byte", "nonNegativeInteger",
                    "unsignedLong", "unsignedInt", "unsignedShort", "unsignedByte", "positiveInteger"));

        // Returns the appropriate primitive for the datatype XS base type.
        // * boolean for xs:boolean
        // * number for xs:double, xs:float, xs:decimal and derived types
        // * string for everything else
        public JsonPrimitive valuePrimitive (Datatype dt, String val) {
            var bname = "string";
            if (null != dt) { 
                var xsbase = dt.baseXS();
                bname = xsbase.name();
            }
            if (numbers.contains(bname)) {
                var number = new BigDecimal(val);
                return new JsonPrimitive(number);
            }
            else if ("boolean".equals(bname))
                return new JsonPrimitive("true".equals(val));
            else 
                return new JsonPrimitive(val);
        }        
    } 
}
