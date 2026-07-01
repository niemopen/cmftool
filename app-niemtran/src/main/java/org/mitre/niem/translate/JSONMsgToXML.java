/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import static org.apache.commons.lang3.StringUtils.capitalize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.niem.cmf.AugmentRecord;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.DataProperty;
import org.mitre.niem.cmf.ListType;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.NamespaceMap;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.json.Context;
import org.mitre.niem.utility.MapToList;
import org.mitre.niem.utility.MapToSet;
import static org.mitre.niem.utility.StringUtils.replaceSuffix;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.XMLDocument.makeQN;
import org.mitre.niem.xsd.NamespaceKind;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class JSONMsgToXML {
    static final Logger LOG = LogManager.getLogger(JSONMsgToXML.class);
    
    private final Model m;
    private final Context cxt;
    private final MapToList<String,AugmentRecord> ctU2augL = new MapToList<>();
    private final MapToSet<ClassType,Property> ct2apropS = new MapToSet<>();
    private final MapToList<ClassType,PropRec> ct2opropRL = new MapToList<>();
    
    private record PropRec (boolean isRepeatable, List<Property> propL) { }
    
    public JSONMsgToXML (Model m) throws CMFException {
        this(m, new Context(m));
    }
    
    public JSONMsgToXML (Model m, Context cxt) { 
        this.m = m;
        this.cxt = cxt;
        for (var ct : m.classTypeL()) createClassProps(ct);
        indexAugmentations();
        duplicatePropertyWarnings();
    }    
    
    private void indexAugmentations () {
        for (var ns : m.namespaceList()) {
            for (var arec : ns.augL()) {
                var ctU = "";
                var codeS = new HashSet<>(arec.codeS());
                codeS.add("CLASS");
                for (var code : codeS) {
                    switch (code) {
                    case "CLASS": 
                        ctU = arec.classType().uri();
                        break;
                    case "ASSOCIATION":
                    case "LITERAL":
                    case "OBJECT":
                        ctU = capitalize(code.toLowerCase());
                        break;
                    }
                    var augL = ctU2augL.get(ctU);
                    augL.add(arec);
                }               
            }
        }
    }

    // For each clsss, built a list of its non-attribute properties.  Each entry in
    // the list is a set of alternatives; that is, the property listed in 
    // the class object, plus all of its subproperties.  Gather up all of 
    // the inherited properties while we're at it.  Augmentatations handled
    // elsewhere.
    private void createClassProps (ClassType ct) {
        
        // Make sure all of the inherited classes are done first
        // Then gather them up.
        if (ct2opropRL.containsKey(ct)) return;
        if (null != ct.subClassOf()) createClassProps(ct.subClassOf());
        var ctQ = ct.qname(); //DEBUG
        var parents = new ArrayDeque<ClassType>();
        for (var pct = ct.subClassOf(); pct != null; pct = pct.subClassOf()) {
            var ptcQ = pct.qname();//debug
            parents.push(pct);
        }
        // Copy the property set lists from the parent classes, in reverse order
        // of inheritance; that is, starting with the class that has no subclass.        
        var propLL = ct2opropRL.get(ct);    // empty list of property lists
        for (var pct : parents) {
            ct2apropS.get(ct).addAll(ct2apropS.get(pct));
            propLL.addAll(ct2opropRL.get(pct));
        }
        // Now work through the property associations for this class.
        // Attribute properties just go into a set for the class.
        // Turn each object property into a set that includes its subproperties.
        for (var pa : ct.propAssocL()) {
            var precL = ct2opropRL.get(ct);
            var p  = pa.property();
            var pQ = p.qname(); //DEBUG
            if (p.isAttribute()) ct2apropS.add(ct, p);
            else {
                var propL = new ArrayList<Property>();
                var propS = new HashSet<>(p.allSubProps());
                propS.add(p);
                for (var sp : propS) {
                    if (!sp.isAbstract()) propL.add(sp);
                }
                if (!propL.isEmpty()) {
                    Collections.sort(propL);
                    precL.add(new PropRec(pa.isRepeatable(), propL));
                }
            }
        }
        // DEBUG
//        System.err.println(ct.qname()+":");
//        for (var prec : ct2opropRL.get(ct)) {
//            var sep = "";
//            var pL = prec.propL;
//            if (pL.size() > 1) System.err.print("  { ");
//            else System.err.print("  ");
//            for (var p : pL) {
//                System.err.print(sep + p.qname());
//                sep = ", ";
//            }
//            if (pL.size() > 1) System.err.print(" }");
//            if (pL.isEmpty()) System.err.print("EMPTY");
//            if (prec.isRepeatable()) System.err.print(" REPEAT");
//            System.err.println("");
//        }
//        System.err.println("");
    }
    
    private void  duplicatePropertyWarnings () {
        for (var ct : m.classTypeL()) {
            var ctQ  = ct.qname();
            var seen = new HashMap<Property,Integer>();
            var prL  = ct2opropRL.get(ct);
            for (var prec : prL) {
                for (var p : prec.propL()) {
                    var x = seen.getOrDefault(p, 0);
                    seen.put(p, x+1);
                }
            }
            var augL = ctU2augL.get(ct.uri());
            for (var arec : augL) {
                var p = arec.property();
                if (p.isAttribute()) continue;
                var x = seen.getOrDefault(p, 0);
                seen.put(p, x+1);
            }
            seen.forEach((p, x) -> {
                if (x > 1) LOG.warn("Property {} appears more than once in {}", p.qname(), ctQ);
            });
        }
    }
    
    
    /**
     * Creates an XML document equivalent to the input NIEM JSON message.
     * Uses the context provided in constructor; ignores any @context in the JSON object.
     * Throws an exception unless the JSON object has exactly one property key,
     * or if the value of the property key is not an object,
     * or if the property key does not expand to a model object property URI.
     * @param jmsg NIEM JSON message object
     * @return NIEM XML message
     * @throws NIEMTranException
     * @throws ParserConfigurationException 
     */
    public Document convert (JsonObject jmsg) throws NIEMTranException, ParserConfigurationException {
        // Find message property key, extract message value object
        String mkey = null;
        for (var key : jmsg.keySet()) {
            if (key.equals("@context")) continue;
            if (null != mkey) 
                throw new NIEMTranException(String.format(
                "Too many property keys (found %s and %s)", mkey, key));
            mkey = key;
        }
        if (null == mkey) throw new NIEMTranException("No message property key");
        var mval = jmsg.get(mkey);
        if (!mval.isJsonObject()) 
            throw new NIEMTranException(String.format("Value of %s is not an object", mkey));
        
        // Find the message property in the model
        var pU = cxt.expand(mkey);
        var op  = m.uriToObjectProperty(pU);
        if (null == op) {
            String msg;
            if (pU.equals(mkey)) msg = String.format("Message property %s is not in model", pU);
            else msg = String.format("Message key %s expands to %s, which is not in model", mkey, pU);
            throw new NIEMTranException(msg);            
        }
        
        // Create root element
        var pre  = op.namespace().prefix();
        var pnsU = op.namespaceURI();
        var pQ   = op.qname();
        var db   = ParserBootstrap.docBuilder();
        var doc  = db.newDocument();
        var root = doc.createElementNS(pnsU, pQ);
        doc.appendChild(root);
        
        // Convert message object and its children; remember namespaces
        var nsmap  = new NamespaceMap();
        var needNS = new HashSet<String>();
        nsmap.assignPrefix(pre, pnsU);
        needNS.add(pnsU);
        convertObjectProp(op, mval.getAsJsonObject(), nsmap, needNS, root);
        
        // Create namespace declarations in the root element
        for (var uri : needNS) {
            var prefix = nsmap.getPrefix(uri);
            root.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, "xmlns:" + prefix, uri);
        }
        return doc;
    }
    
    private static final Set<String> uriAllowedCodes  = Set.of("ANY", "ANYURI", "INTERNAL", "RELURI");
    
    // Takes an model object property and a JSON object with values for an instance
    // of that property, and fills out the provided, already-created XML element.
    private void convertObjectProp(
        ObjectProperty op, 
        JsonObject obj, 
        NamespaceMap nsmap, 
        Set<String> needNS,
        Element propE) 
        throws NIEMTranException {
        
        // Create map of child property to JsonElement values, using context
        // to expand compact IRIs and terms to a model property URI
        var doc = propE.getOwnerDocument();
        var ct  = op.classType();
        var ctU = ct.uri(); //DEBUG
        var child = new HashMap<String,JsonElement>();
        var unk   = new HashMap<String,JsonElement>();
        for (Map.Entry<String,JsonElement> e : obj.entrySet()) {
            var key = e.getKey();
            var val = e.getValue();
            var pU  = cxt.expand(key);
            var p   = m.uriToProperty(pU);
            if (null == p) unk.put(key, val);
            else {
                var pQ  = p.qname();
                child.put(pQ, val);
            }
        }
        // Add property attributes to parent element
        for (var p : ct2apropS.get(ct)) {
            var pre = p.namespace().prefix();
            var nsU = p.namespaceURI();
            var pQ  = p.qname();
            var val = child.remove(pQ);
            if (null == val) continue;
            var vstr = val.getAsString();
            propE.setAttributeNS(nsU, pQ, vstr);
            nsmap.assignPrefix(pre, nsU);
            needNS.add(nsU);
        }
        // Handle @id keys
        var id = unk.remove("@id");
        if (null != id) {
            var attLN   = "";
            var idval   = id.getAsString();
            var refCode = ct.effectiveReferenceCode();
            if (uriAllowedCodes.contains(refCode)) attLN = "uri";
            else {
                if (idval.startsWith("#")) idval = idval.substring(1);
                if (child.isEmpty()) attLN = "ref";
                else attLN = "id";
            }
            
            var pns = op.namespace();
            var vers = pns.archVersion();
            var structU = NamespaceKind.builtinNSU(vers, "STRUCTURES");
            var structP = nsmap.assignPrefix("structures", structU);
            var attQ    = makeQN(structP, attLN);
            needNS.add(structU);
            propE.setAttributeNS(structU, attQ, idval);
            if ("ref".equals(attLN)) {
                var xsiPre = nsmap.assignPrefix("xsi", W3C_XML_SCHEMA_INSTANCE_NS_URI);
                needNS.add(W3C_XML_SCHEMA_INSTANCE_NS_URI);
                propE.setAttributeNS(W3C_XML_SCHEMA_INSTANCE_NS_URI, makeQN(xsiPre, "nil"), "true");
            }
        }
        
        // Add value of a literal property to the parent element
        if (ct.isLiteralClass()) {
            var lp  = ct.literalDataProperty();
            var lpQ = lp.qname();
            var lval = child.remove(lpQ);
            if (null == lval)
                throw new NIEMTranException(String.format(
                "Literal property %s not found in JSON object %s", lp.qname(), obj.toString()));
            if (lval instanceof JsonArray vA) {
                var sb = new StringBuilder();
                for (var aval : vA) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(aval.getAsString());
                }
                propE.setTextContent(sb.toString());
            }
            else propE.setTextContent(lval.getAsString());
        }
        // Otherwise create child elements for object properties
        else {
            var precL = ct2opropRL.get(ct);
            for (var prec : precL) {
                var repF = prec.isRepeatable();
                for (var p : prec.propL()) {
                    var pQ  = p.qname();
                    var val = child.remove(pQ);
                    if (null == val) continue;
                    convertProperty(p, repF, val, nsmap, needNS, propE);
                }
            }
        }
        // Handle augmentation properties.
        // Start with attribute augs and augs without an augmentation type.
        var augL = new ArrayDeque<>(ctU2augL.get(ctU));
        for (var arec : augL) {
            var ap   = arec.property();
            var apQ  = ap.qname();
            var val  = child.get(apQ);
            if (null == val) continue;
            if (ap.isAttribute() || arec.index() < 0) {
                if (ap.isAttribute()) {
                    var vstr = val.getAsString();
                    propE.setAttributeNS(ap.namespaceURI(), apQ, vstr);                    
                }
                else {
                    convertProperty(ap, false, val, nsmap, needNS, propE);
                }
                addNamespace(nsmap, needNS, ap.namespace());
                child.remove(apQ);                
            }
        }
        // Now create augmentation elements that have augmentation types
        var apln = replaceSuffix(ct.name(), "Type", "Augmentation");    // augmentation local name
        while (!augL.isEmpty()) {
            var arec = augL.remove();
            var p  = arec.property();
            var pQ = p.qname();
            var val = child.get(pQ);
            
            // If this augmentation property has a value in the JSON object,
            // then create an augmentation element from its namespace.
            if (null != val) {
                var naugL = new ArrayDeque<AugmentRecord>();    // augmentations from a different namespace
                var augRL = new ArrayList<AugmentRecord>();     // augmentations from this namespace
                 augRL.add(arec);
                
                // Seperate all augmentations from current namespace
                for (var ar2 : augL) {
                    if (ar2.namespace() == arec.namespace()) augRL.add(ar2);
                    else naugL.add(ar2);
                }
                augL = naugL;   // augmentations from other namespaces
                
                // Create and populate an augmentation element
                var augNSuri = arec.namespace().uri();
                var augNSpre = arec.namespace().prefix();
                var aE = doc.createElementNS(augNSuri, makeQN(augNSpre, apln));
                propE.appendChild(aE);
                nsmap.assignPrefix(augNSpre, augNSuri);
                needNS.add(augNSuri);
                for (var ar : augRL) {
                    var ap  = ar.property();
                    var apQ = ap.qname();
                    val = child.remove(apQ);
                    if (null != val) {
                        convertProperty(ap, ar.isRepeatable(), val, nsmap, needNS, aE);
                    }
                }
            }
        }  
        // Handle unknown keys
        for (var key : child.keySet()) LOG.warn("Unknown key " + key);
        for (var key : unk.keySet())   LOG.warn("Unknown key " + key);
    }
    
    // Creates an XML element for the model property using values in the JSON element.
    // The new element becomes a child of the parent element.    
    private void convertProperty (
        Property p,
        boolean repF,                   // repeatable property
        JsonElement jval,               // array, object, or primitive property value
        NamespaceMap nsmap,             // namespace declarations needed in root
        Set<String> needNS,
        Element parent)                 // add children to this element
        throws NIEMTranException {
        
        var doc = parent.getOwnerDocument();
        
        // An array value is either a repeatable element or a list
        if (jval instanceof JsonArray vA) {
            var dt = p.datatype();
            if (repF) {
                for (var aval : vA) {
                    convertProperty(p, false, aval, nsmap, needNS, parent);
                }
            }                
            else if (dt instanceof ListType lt) {
                var sb = new StringBuilder();
                for (var aval : vA) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(aval.getAsString());
                }
                var pre = p.namespace().prefix();
                var nsU = p.namespaceURI();
                var e = doc.createElementNS(nsU, p.qname());
                e.setTextContent(sb.toString());
                parent.appendChild(e);  
                nsmap.assignPrefix(pre, nsU);
                needNS.add(nsU);
            }
            else throw new NIEMTranException(String.format(
                "Data property %s is not repeatable, not a list, can't have array value %s", p.qname(), vA.toString()));
        }
        // A primitive value is an element with simple content
        else if (jval instanceof JsonPrimitive vprim) {
            if (p instanceof DataProperty dp) {
                var pre = p.namespace().prefix();
                var nsU = p.namespaceURI();
                var e = doc.createElementNS(nsU, p.qname());
                e.setTextContent(vprim.getAsString());
                parent.appendChild(e);
                nsmap.assignPrefix(pre, nsU);
                needNS.add(nsU);
            }
            else throw new NIEMTranException(String.format(
                "Object property %s can't have primitive value %s", p.qname(), jval.toString()));
        }
        // An object value is either an element with complex content
        // or an element with simple content and attributes
        else if (jval instanceof JsonObject vobj) {
            if (p instanceof ObjectProperty op) {
                var pre = p.namespace().prefix();
                var nsU = p.namespaceURI();
                var e   = doc.createElementNS(nsU, p.qname());
                parent.appendChild(e);
                convertObjectProp(op, vobj, nsmap, needNS, e);
                nsmap.assignPrefix(pre, nsU);
                needNS.add(nsU);
            }
            else throw new NIEMTranException(String.format(
                "Data property %s can't have an object value %s", p.qname(), vobj.toString()));
        }
        // Null value is an error
        else throw new NIEMTranException("Nulls not allowed in JSON data");
    }
    
    // Add namespace prefix and uri to the namespace binding map.
    private void addNamespace (NamespaceMap nsmap, Set<String> needNS, Namespace ns) {
        var pre = ns.prefix();
        var uri = ns.uri();
        nsmap.assignPrefix(pre, uri);
        needNS.add(uri);
    }
    
}
