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
package org.mitre.niem.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.cmf.CMFObject.CMF_DATATYPE;
import static org.mitre.niem.cmf.CMFObject.CMF_LIST;
import static org.mitre.niem.cmf.CMFObject.CMF_RESTRICTION;
import static org.mitre.niem.cmf.CMFObject.CMF_UNION;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.ListType;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.PropertyAssociation;
import org.mitre.niem.cmf.Restriction;
import org.mitre.niem.cmf.Union;
import org.mitre.niem.utility.MapToList;
import static org.mitre.niem.xsd.NIEMConstants.hasMetadata;

/**
 * A class for generating JSON Schema to validate a JSON object, known as the
 * message.
 * 
 * An instance of this class is a generator that creates JSON Schema from a
 * model.  The schema can be returned as a GSON object (createSchema) or as
 * characters written to a Writer (writeSchema).  Based on options, the 
 * generated schema may:
 * <ul>
 *   <li>Validate messages of a particular message type</li>
 *   <li>Validate messages of a set of message types</li>
 *   <li>Validate message components against the model but not against any
 *       particular message type</li>
 *   <li>Validate messages with canonical property QNames (e.g. nc:Person), or
 *       with replacement property names from a Mapping.</li>
 * </ul>
 * A single instance may be used to generate different schemas by providing
 * different options between createSchema and writeSchema calls.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToJSONSchema {
    static final Logger LOG = LogManager.getLogger(ModelToJSONSchema.class);
    
    private final Model m;
    
    private final List<Property> metadataPL = new ArrayList<>();    // list of NIEM 3,4,5 metadata properties
    private Mapping map = new Mapping();                            // canonical to simple name map, if provided
    private List<Property> msgPropL = null;                         // message property objects, if provided
    private String contextU = null;                                 // @context URI, if provided
    private boolean inclDesc = true;                                // include component definitions in schema?
    private boolean inclMinMax = false;                             // XSD atomic types test min & max values?
    private boolean inclPattern = false;                            // XSD atomic types include pattern tests?
    
    private MapToList<String,PropertyAssociation> ctU2augL = null;  // classQ -> list of augmentation propQs for class
    private Deque<Component> doTypes = null;                        // class and datatype schemas remaining
    private Map<Component,JsonObject> typeSch = null;               // type -> schema json object
    private boolean needID = false;                                 // true if any class is referenceable
    
    private Datatype xsStringDT = null;                             // xs:string Datatype object
    private static final String XS_STRING_U = W3C_XML_SCHEMA_NS_URI + "/xs:string"; // xs:string URI
    
    /**
     * Creates a new converter for generating JSON Schema based on the 
     * supplied NIEM model.
     * @param model 
     */
    public ModelToJSONSchema (Model model) {
        m = model;
    }
    
    /**
     * Provides a Mapping object which will be used to replace model property
     * QNames in the schema; for example, msg:lname or lname instead of
     * nc:PersonSurName.  A null parameter clears any existing map.
     * @param map Mapping object
     */
    public void setMapping (Mapping map) {
        if (null == map) this.map = new Mapping();
        else this.map = map;
    }
    
    /**
     * When set true, the generated schema will not include namespace prefixes
     * in property QNames.  Requires all properties in the model to have the
     * same prefix after mapping. TODO
     * @param noPrefix 
     */
    public void setNoPrefix (boolean noPrefix) {
    }
    
    /**
     * When the message property is set, the generated schema will require the
     * root object to contain two keys: this property's qname, and @context.
     * @param mprop message property's QName
     */
    public void setMessageProperty (Property mprop) {
        msgPropL = List.of(mprop);
    }
    
    /**
     * The generated schema will require the root object to contain two keys:
     * the qname of exactly one of these properties, and @context.
     * @param mpropL 
     */
    public void setMessageProperties (List<Property> mpropL) {
        if (null == mpropL) msgPropL = null;
        else msgPropL = mpropL;
    }
    
    /**
     * Documents conforming to the generated schema must have a @context key,
     * which may be an object or a string.  When this method is called, the
     * generated schema will use the supplied URI as the required value for the
     * string.
     * @param contextU
     */
    public void setContextURI (String contextU) {
        this.contextU = contextU;
    }
    
    /**
     * When set, the generated schema will include model component documentation.
     * @param inclDesc 
     */
    public void setIncludeDescription (boolean inclDesc) {
        this.inclDesc = inclDesc;
    }
    
    public void writeSchema (Writer w) throws IOException {
        var sch  = new JsonObject();
        createSchema(sch);
        JSONWriter.write(sch, w);
    }
    
    private static final String CARDINALITY_TEMPLATE = """
          [
            { "$ref": "#/definitions/%s" },
            { "type": "array", "items": { "$ref": "#/definitions/%s" } }
          ]""";

    private static final JsonObject IDREF_PATTERN_OBJ = 
        makeObject("pattern", "\"[4][-._A-Za-z0-9]*\"");
    
    private static final JsonElement ID_OBJECT_ARRAY_SCHEMA = makeElement("""
        {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "@id": {
                "type": "string",
                "format": "uri-reference"
              }
            },
            "required": ["@id"],
            "additionalProperties": false
          }
        }
        """);

    /**
     * Generates a JSON schema from the model and the specified options, writing
     * the schema pairs into the (presumably empty) JsonObject provided.
     * @param root 
     */
    public void createSchema (JsonObject root) {
        // Get xs:string datatype from model
        xsStringDT = m.uriToDatatype(XS_STRING_U);
        if (null == xsStringDT) {                       // are you kidding me?
            var xsns = m.namespaceObj(W3C_XML_SCHEMA_NS_URI);
            xsStringDT = new Datatype(xsns, "string");
        }
        // Get metadata properties from NIEM 3,4,5 namespaces
        for (var p : m.propertyL()) {
            var ns = p.namespace();
            if (hasMetadata.contains(ns.archVersion())) {
                if (p.name().endsWith("Metadata"))
                    metadataPL.add(p);
            }
        }        
        // Organize all the augmentation records by the augmented class
        // (including global augmentation codes "OBJECT", etc.)
        ctU2augL = new MapToList<>();
        for (var ns : m.namespaceSet()) {
            for (var arec : ns.augL()) {
                var ct = arec.classType();
                if (null != ct) ctU2augL.add(ct.qname(), arec);
                for (var gcode : arec.codeS()) ctU2augL.add(gcode, arec);
            }
        } 
        // Start of schema object.
        root.addProperty("$schema", "http://json-schema.org/draft-07/schema#");
        root.addProperty("type", "object");
        
        // Generate schema for required root-level keys in the message; could
        // be "required", or "oneOf"; might be "additionalProperties".
        // Start with schema for message properties, if specified.
        if (null != msgPropL && !msgPropL.isEmpty()) {
            // If several message properties are specified, then create schema like
            // "oneOf": [
            //   { "required": [ "@context", "request" ] },
            //   { "required": [ "@context", "response" ] } ]
            if (msgPropL.size() > 1) {
                var oneOfA = new JsonArray();
                for (var mprop : msgPropL) {
                    var mkey = qnToKey(mprop.qname());
                    var reqO = makeObject("required", makeStringArray("@context", mkey));
                    oneOfA.add(reqO);                    
                }
                root.add("oneOf", oneOfA);
            }
            // Otherwise, if exactly one message property specified, generate 
            // schema like "required": [ "@context", "msg:Request" ]
            else {
                var mkey = qnToKey(msgPropL.get(0).qname());
                var reqA = makeStringArray("@context", mkey);
                root.add("required", reqA);
            }
            // Validating a message? That's always a context pair plus one property:value pair
            root.addProperty("additionalProperties", false);
        }
        // Not validating messages?  Then we don't need "additionalProperties".
        // Generate schema:  "required": [ "@context" ]
        else {
            root.add("required", makeStringArray("@context"));
        }
        
        // Now generate a "properties" array for the keys that can appear
        // in the message root.  That will be the message properties, if provided,
        // plus the @context key.  When validating components against the model, 
        // that will be all the properties in the model.  Keep track of the classes
        // and datatypes seen -- need to generate schema for those, later.
        var propVal = new JsonObject();             // value of "properties" key in schema
        var typeS   = new HashSet<Component>();     // set of classes and datatypes to create
        var propL   = msgPropL;                     // list of message properties
        if (null == propL || propL.isEmpty()) {     // all properties in the model, if none
            propL = m.propertyL();
            Collections.sort(propL);
        }
        for (var p : propL) {
            var pt = p.type();
            if (p.isAbstract()) continue;
            if (null == pt) pt = xsStringDT;    // externals, xml:lang, xml:base
            var key  = qnToKey(p.qname());      // property QName, possibly mapped
            var refQ = pt.qname();              // type QNames are not mapped
            var prO  = new JsonObject();
            typeS.add(pt);
            if (inclDesc && null != p.definition()) prO.addProperty("description", p.definition());
            
            // Validating a message?  Message properties are never repeated
            if (null != msgPropL && !msgPropL.isEmpty()) {
                prO.addProperty("$ref", "#/definitions/" + refQ);
            }
            // Validating components? Those can be repeated.
            else {
                var card = String.format(CARDINALITY_TEMPLATE, refQ, refQ);
                var cardA = makeElement(card);
                prO.add("oneOf", cardA);
            }
            propVal.add(key, prO);
        } 
        // Add schema for the @context pair to finish the properties array.
        var cxtO = new JsonObject();
        var anyA = new JsonArray();
        var anyO = new JsonObject();
        anyO.addProperty("type", "object");
        anyA.add(anyO);
        if (null != contextU) {
            anyO = new JsonObject();
            anyO.addProperty("type", "string");
            anyO.addProperty("const", contextU);
            anyA.add(anyO);
        }
        cxtO.add("anyOf", anyA);
        propVal.add("@context", cxtO);
        root.add("properties", propVal);
        
        // Generate a "definitions" array.  Start with the classes and datatypes
        // encountered while generating the "properties" array.  Add the type
        // of each property from each class.
        doTypes = new ArrayDeque<>(typeS);      // class and datatypes left to process
        typeSch = new HashMap<>();              // map of type QN -> schema object
        needID  = false;                        // any referencable class in model?
        while (!doTypes.isEmpty()) {
            var type = doTypes.removeFirst();
            var tq = type.qname();
            if (type.isClassType()) createClass((ClassType)type);
            else createDatatype((Datatype)type);
        }
        // Make sure we have xs:anyURI definition if any class is referencable
        // Make sure we have an xs:string definition
        if (needID) {
            var xsns = m.namespaceObj(W3C_XML_SCHEMA_NS_URI);
            var dtU  = W3C_XML_SCHEMA_NS_URI + "/anyURI";
            var dt   = m.uriToDatatype(dtU);
            if (null == dt) {
                dt = new Datatype(xsns, "anyURI");
            }
            if (!typeSch.containsKey(dt)) createDatatype(dt);
            if (!typeSch.containsKey(xsStringDT.qname())) createDatatype(xsStringDT);
        }
        // Sort the type objects; add class, then datatypes; add "definitions" key
        var defVal = new JsonObject();      // value of "definitions" key
        var typeL  = new ArrayList<>(typeSch.keySet());
        Collections.sort(typeL);
        for (var tp : typeL) { 
            if (tp.isClassType())
                defVal.add(tp.qname(), typeSch.get(tp)); 
        }
        for (var tp : typeL) { 
            if (!tp.isClassType())
                defVal.add(tp.qname(), typeSch.get(tp)); 
        }
        // Add definition for @id object array if there are metadata properties\
        if (!metadataPL.isEmpty()) {
            defVal.add("idObjectArray", ID_OBJECT_ARRAY_SCHEMA);                
        }
        root.add("definitions", defVal);
    }
    
    // Stores a schema object for a class in the typeSch map.  Later on
    // that object will become the value of a "classQN": {...} pair in 
    // the top-level "definitions" object.
    private void createClass (ClassType ct) {
        if (typeSch.containsKey(ct)) return;
        var defO   = new JsonObject();       // will be value for ct.qname key in "definitions"
        var propO  = new JsonObject();       // value of "properties" key
        var reqA   = new JsonArray();        // value of "required" key
        var allOfL = new ArrayList<JsonObject>();    // list of "allOf" array entries
        typeSch.put(ct, defO);
        
        if (inclDesc && null != ct.definition())
            defO.addProperty("description", ct.definition());
        defO.addProperty("type", "object");
        defO.add("properties", propO);
        
        // Build the complete property association list.  Start with global augmentations.
        // Then add inherited property associations, deepest first.  Also augmentations
        // at each inheritance level.  Properties for this particular class are last.
        var paL = new ArrayList<PropertyAssociation>();
        var wildF  = false;
        var classS = new Stack<ClassType>();
        var xct    = ct;
        if (ct.name().endsWith("Association")) paL.addAll(ctU2augL.get("ASSOCIATION"));
        else paL.addAll(ctU2augL.get("OBJECT"));
        while (null != xct) {
            classS.add(xct);
            xct = xct.subClassOf();
        }
        while (!classS.isEmpty()) {
            xct = classS.pop();
            paL.addAll(xct.propL());
            paL.addAll(ctU2augL.get(xct.qname()));
            if (!xct.anyL().isEmpty()) wildF = true;    // should handle wildcards better TODO
        }
        // Adjust cardinality for properties occuring more than once TODO
        // Should include subproperties
        
        // Handle each property in the property association list.
        // Start by making a set of the non-abstract choices for a property.
        for (var pa : paL) {
            var choS = new HashSet<Property>();
            var p    = pa.property();
            for (var subp : p.allSubProps())
                if (!subp.isAbstract()) choS.add(subp);
            if (!p.isAbstract()) choS.add(p);

            // Create a "properties" entry for each choice
            if (choS.isEmpty()) continue;
            for (var chp : choS) {
                var cct  = chp.type();
                if (null == cct) cct = xsStringDT;          // externals, xml:lang, xml:base
                var rstr = "#/definitions/" + cct.qname();
                var key  = qnToKey(chp.qname());
                var schO = new JsonObject();
                propO.add(key, schO);
                if (inclDesc) {
                    if (null != chp.definition()) schO.addProperty("description", chp.definition());
                    if (null != pa.definition())  schO.addProperty("associationDescription", pa.definition());
                }
                // A repeatable property is an array in the message.
                // Can't enforce minItems if there are subproperty choices.
                if (pa.maxOccursVal() > 1 || pa.isMaxUnbounded()) {
                    var refO = new JsonObject();
                    refO.addProperty("$ref", rstr);
                    schO.addProperty("type", "array");
                    schO.add("items", refO);
                    if (pa.maxOccursVal() > 1) schO.addProperty("maxItems", pa.maxOccursVal());
                    if (1 == choS.size() && pa.minOccursVal() > 0) schO.addProperty("minItems", pa.minOccursVal());
                }
                else schO.addProperty("$ref", rstr);
                if (!typeSch.containsKey(cct)) doTypes.add(cct);
            }
            // Handle a required property.  If no choices, add key to "required" array.
            // If choices, create and remember an "anyOf":[] object for the choices.
            // That may become an entry in an "allOf" array once all properties are done.
            if (pa.minOccursVal() > 0) {
                JsonArray anyA  = null;     // array of { "required": [ "foo" ] } objects
                JsonObject anyO = null;     // the { "anyOf": array } object
                for (var chp : choS) {
                    var key = qnToKey(chp.qname());
                    if (1 == choS.size()) reqA.add(key);    // top-level "required" array
                    else {
                        var rO = makeObject("required", makeStringArray(key));
                        if (null == anyA) { 
                            anyO = new JsonObject();
                            anyA = new JsonArray();
                            anyO.add("anyOf", anyA);
                            allOfL.add(anyO);
                        }
                        anyA.add(rO);
                    }
                }
            }
            // Warn if schema doesn't check cardinality on property with choices TODO
            if (choS.size() > 1 && !pa.isMaxUnbounded()) {
                LOG.warn(String.format("Schema does not enforce maxOccurs=%d on %s in %s",
                    pa.maxOccursVal(), p.qname(), ct.qname()));
            }
        }
        // Add metadata properties (if any) for NIEM 2,3,4,5
        for (var p : metadataPL) {
            var key = qnToKey(p.qname());
            var ref = new JsonObject();
            ref.addProperty("$ref", "#/definitions/idObjectArray");
            propO.add(key, ref);
        }
        // Done with properties; add @id if this class can be referenced
        if (!"NONE".equals(ct.effectiveReferenceCode())) {
            var idrefO = new JsonObject();
            idrefO.addProperty("$ref", "#/definitions/xs:anyURI");
            propO.add("@id", idrefO);
            needID = true;
        }
        // Handle list of objects for the "allOf" key.
        // If only one object in the list, skip the allOf.
        if (1 == allOfL.size()) {
            var anyO = allOfL.get(0);
            var anyA = anyO.get("anyOf").getAsJsonArray();
            defO.add("anyOf", anyA);
        }
        else if (allOfL.size() > 1) {
            var allA = new JsonArray();
            for (var ao : allOfL) allA.add(ao);
            defO.add("allOf", allA);
        }
        if (!reqA.isEmpty()) defO.add("required", reqA);
        if (!wildF) defO.addProperty("additionalProperties", false);
    }
    
    
    // Stores a schema object for a datatype in the typeSch map.  Later on
    // that object will become the value of a "datatypeQN": {...} pair in 
    // the top-level "definitions" object.
    private void createDatatype (Datatype dt) {
        if (typeSch.containsKey(dt)) return;
        var defO  = new JsonObject();       // will be value for dt.qname key
        typeSch.put(dt, defO);
        
        if (inclDesc && null != dt.definition()) 
            defO.addProperty("description", dt.definition());
        
        var dtp = dt.getType();
        switch(dt.getType()) {
            case CMF_RESTRICTION -> createRestriction((Restriction) dt, defO);
            case CMF_LIST -> createList((ListType) dt, defO);
            case CMF_UNION -> createUnion((Union) dt, defO);
            case CMF_DATATYPE -> createXSDPrimitive(dt.name(), defO);
        }
    }
    
      
    private void createRestriction (Restriction r, JsonObject defO) {
        if (W3C_XML_SCHEMA_NS_URI.equals(r.namespaceURI())) {
            createXSDPrimitive(r.name(), defO);
            return;
        }
        // Restrictions not in XSD namespace must have a base type
        var bt   = r.base();
        if (!typeSch.containsKey(bt)) doTypes.add(bt);

        var facO  = new JsonObject();
        var enumA = new JsonArray();
        for (var f : r.facetL()) {
            JsonPrimitive fval;
            switch (f.category()) {
                case "enumeration" ->  { enumA.add(f.value()); }
                case "length" ->       { facO.addProperty("maxLength", new BigDecimal(f.value()));
                                         facO.addProperty("minLength", new BigDecimal(f.value())); }
                case "maxExclusive" -> { facO.addProperty("exclusiveMaximum", new BigDecimal(f.value())); }
                case "maxInclusive" -> { facO.addProperty("maximum", new BigDecimal(f.value())); }
                case "maxLength" ->    { facO.addProperty("maxLength", new BigDecimal(f.value())); }
                case "minExclusive" -> { facO.addProperty("exclusiveMinimum", new BigDecimal(f.value())); }
                case "minInclusive" -> { facO.addProperty("minimum", new BigDecimal(f.value())); }
                case "minLength" ->    { facO.addProperty("minLength", new BigDecimal(f.value())); }
                case "pattern" ->      { facO.addProperty("pattern", f.value()); }
                case "fractionDigits",
                     "totalDigits",
                     "whiteSpace" -> {
                    LOG.warn(String.format("schema does not enforce %s facet on %s", f.category(), r.qname()));
                }
            }
        }
        // Determine base type.  Code type primitive is always string
        var refName = bt.qname();
        if (r.name().endsWith("CodeType") && W3C_XML_SCHEMA_NS_URI.equals(bt.namespaceURI()))
            refName = "xs:string";
            
        if (!enumA.isEmpty()) facO.add("enum", enumA);
        if (!facO.isEmpty()) {
            var allA = new JsonArray();
            var refO = new JsonObject();
            refO.addProperty("$ref", "#/definitions/" + refName);
            allA.add(refO);
            allA.add(facO);
            defO.add("allOf", allA);
        }
        else defO.addProperty("$ref", "#/definitions/" + refName);
    }
    
    
    private void createList (ListType lt, JsonObject defO) {
        var itype = lt.itemType();
        var refO  = new JsonObject();
        refO.addProperty("$ref", "#/definitions/" + itype.qname());
        defO.addProperty("type", "array");
        defO.add("items", refO);
        if (!typeSch.containsKey(itype)) doTypes.add(itype);        
    }
        
    private void createUnion (Union u, JsonObject defO) {
        var anyA = new JsonArray();
        for (var mdt : u.memberL()) {
            var refO = new JsonObject();
            refO.addProperty("$ref", "#/definitions/" + mdt.qname());
            anyA.add(refO);
            if (!typeSch.containsKey(mdt)) doTypes.add(mdt);
        }
        defO.add("anyOf", anyA);
    }
    
    private void createXSDPrimitive(String name, JsonObject defO) {
        switch (name) {
            case "anyAtomicType" -> {
                defO.addProperty("type", "string");
            }
            case "anySimpleType" -> {
                defO.addProperty("type", "string");
            }
            case "anyURI" -> {
                defO.addProperty("type", "string");
                defO.addProperty("format", "uri");
                if (inclPattern) defO.addProperty("pattern", "^[^\\s]+$");
            }   
            // Based on the XSD 1.1 regexp for the lexical space; modified to
            // allow any single whitespace character where a space is allowed.
            case "base64Binary" -> {
                defO.addProperty("type", "string");
                defO.addProperty("format", "byte");
                if (inclPattern) defO.addProperty("pattern",
                    "^(?:"
                    + "(?:(?:[A-Za-z0-9+/][ \\t\\r\\n]?){4})*"
                    + "(?:"
                    + "(?:[A-Za-z0-9+/][ \\t\\r\\n]?){3}[A-Za-z0-9+/]"
                    + "|(?:[A-Za-z0-9+/][ \\t\\r\\n]?){2}[AEIMQUYcgkosw048][ \\t\\r\\n]?="
                    + "|[A-Za-z0-9+/][ \\t\\r\\n]?[AQgw][ \\t\\r\\n]?=[ \\t\\r\\n]?="
                    + ")?"
                    + ")?$");
            }
            case "boolean" -> {
                defO.addProperty("type", "boolean");
                // If you need lexical validation instead:
                // defO.addProperty("type", "string");
                // defO.addProperty("pattern", "^(?:true|false|1|0)$");
            }
            case "byte" -> {
                defO.addProperty("type", "integer");
                defO.addProperty("format", "int8");
                if (inclMinMax) defO.addProperty("minimum", -128);
                if (inclMinMax) defO.addProperty("maximum", 127);
            }
            case "date" -> {
                defO.addProperty("type", "string");
                defO.addProperty("format", "date");
                if (inclPattern) defO.addProperty("pattern", "^-?\\d{4,}-\\d{2}-\\d{2}(?:Z|[+-]\\d{2}:\\d{2})?$");
            }
            case "dateTime" -> {
                defO.addProperty("type", "string");
                defO.addProperty("format", "date-time");
                if (inclPattern) defO.addProperty("pattern", "^-?\\d{4,}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?$");
            }
            case "dateTimeStamp" -> {
                defO.addProperty("type", "string");
                defO.addProperty("format", "date-time");
                if (inclPattern) defO.addProperty("pattern", "^-?\\d{4,}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})$");
            }
            case "dayTimeDuration" -> {
                defO.addProperty("type", "string");
                defO.addProperty("format", "duration");
                if (inclPattern) defO.addProperty("pattern", "^-?P(?=.+)(?:\\d+D)?(?:T(?:\\d+H)?(?:\\d+M)?(?:\\d+(?:\\.\\d+)?S)?)?$");
            }
            case "decimal" -> {
                defO.addProperty("type", "number");
                defO.addProperty("format", "decimal");
                // If you must enforce XSD lexical form:
                // defO.addProperty("type", "string");
                // if (inclPattern) defO.addProperty("pattern", "^[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)$");
            }
            case "double" -> {
                defO.addProperty("type", "number");
                defO.addProperty("format", "double");
            }
            case "duration" -> {
                defO.addProperty("type", "string");
                defO.addProperty("format", "duration");
                if (inclPattern) defO.addProperty("pattern", "^-?P(?=.+)(?:\\d+Y)?(?:\\d+M)?(?:\\d+D)?(?:T(?:\\d+H)?(?:\\d+M)?(?:\\d+(?:\\.\\d+)?S)?)?$");
            }
            case "ENTITIES" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[A-Za-z_][A-Za-z0-9_.-]*(?:\\s+[A-Za-z_][A-Za-z0-9_.-]*)*$");
            }
            case "ENTITY" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[A-Za-z_][A-Za-z0-9_.-]*$");
            }
            case "float" -> {
                defO.addProperty("type", "number");
                defO.addProperty("format", "float");
            }
            case "gDay" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^---\\d{2}(?:Z|[+-]\\d{2}:\\d{2})?$");
            }
            case "gMonth" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^--\\d{2}(?:Z|[+-]\\d{2}:\\d{2})?$");
            }
            case "gMonthDay" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^--\\d{2}-\\d{2}(?:Z|[+-]\\d{2}:\\d{2})?$");
            }
            case "gYear" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^-?\\d{4,}(?:Z|[+-]\\d{2}:\\d{2})?$");
            }
            case "gYearMonth" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^-?\\d{4,}-\\d{2}(?:Z|[+-]\\d{2}:\\d{2})?$");
            }
            case "hexBinary" -> {
                defO.addProperty("type", "string");
                defO.addProperty("format", "hex");
                if (inclPattern) defO.addProperty("pattern", "^(?:[0-9A-Fa-f]{2})*$");
            }
            case "ID" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[A-Za-z_][A-Za-z0-9_.-]*$");
            }
            case "IDREF" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[A-Za-z_][A-Za-z0-9_.-]*$");
            }
            case "IDREFS" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[A-Za-z_][A-Za-z0-9_.-]*(?:\\s+[A-Za-z_][A-Za-z0-9_.-]*)*$");
            }
            case "int" -> {
                defO.addProperty("type", "integer");
                defO.addProperty("format", "int32");
                if (inclMinMax) if (inclMinMax) defO.addProperty("minimum", -2147483648L);
                if (inclMinMax) defO.addProperty("maximum", 2147483647L);
            }
            case "integer" -> {
                defO.addProperty("type", "integer");
            }
            case "language" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[a-zA-Z]{1,8}(?:-[a-zA-Z0-9]{1,8})*$");
            }
            case "long" -> {
                defO.addProperty("type", "integer");
                defO.addProperty("format", "int64");
                if (inclMinMax) defO.addProperty("minimum", new java.math.BigInteger("-9223372036854775808"));
                if (inclMinMax) defO.addProperty("maximum", new java.math.BigInteger("9223372036854775807"));
            }
            case "Name" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[A-Za-z_][A-Za-z0-9_.:-]*$");
            }
            case "NCName" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[A-Za-z_][A-Za-z0-9_.-]*$");
            }
            case "negativeInteger" -> {
                defO.addProperty("type", "integer");
                if (inclMinMax) defO.addProperty("maximum", -1);
            }
            case "NMTOKEN" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[A-Za-z0-9_.:-]+$");
            }
            case "NMTOKENS" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[A-Za-z0-9_.:-]+(?:\\s+[A-Za-z0-9_.:-]+)*$");
            }
            case "nonNegativeInteger" -> {
                defO.addProperty("type", "integer");
                if (inclMinMax) defO.addProperty("minimum", 0);
            }
            case "nonPositiveInteger" -> {
                defO.addProperty("type", "integer");
                if (inclMinMax) defO.addProperty("maximum", 0);
            }
            case "normalizedString" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^[^\\r\\n\\t]*$");
            }
            case "NOTATION", "notation" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^(?:[A-Za-z_][A-Za-z0-9_.-]*:)?[A-Za-z_][A-Za-z0-9_.-]*$");
            }
            case "positiveInteger" -> {
                defO.addProperty("type", "integer");
                if (inclMinMax) defO.addProperty("minimum", 1);
            }
            case "precisionDecimal" -> {
                defO.addProperty("type", "number");
                defO.addProperty("format", "decimal");
            }
            case "QName" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^(?:[A-Za-z_][A-Za-z0-9_.-]*:)?[A-Za-z_][A-Za-z0-9_.-]*$");
            }
            case "short" -> {
                defO.addProperty("type", "integer");
                defO.addProperty("format", "int16");
                if (inclMinMax) defO.addProperty("minimum", -32768);
                if (inclMinMax) defO.addProperty("maximum", 32767);
            }
            case "string" -> {
                defO.addProperty("type", "string");
            }
            case "time" -> {
                defO.addProperty("type", "string");
                defO.addProperty("format", "time");
                if (inclPattern) defO.addProperty("pattern", "^\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})?$");
            }
            case "token" -> {
                defO.addProperty("type", "string");
                if (inclPattern) defO.addProperty("pattern", "^(?:$|\\S+(?: \\S+)*)$");
            }
            case "unsignedByte" -> {
                defO.addProperty("type", "integer");
                defO.addProperty("format", "uint8");
                if (inclMinMax) defO.addProperty("minimum", 0);
                if (inclMinMax) defO.addProperty("maximum", 255);
            }
            case "unsignedInt" -> {
                defO.addProperty("type", "integer");
                defO.addProperty("format", "uint32");
                if (inclMinMax) defO.addProperty("minimum", 0);
                if (inclMinMax) defO.addProperty("maximum", 4294967295L);
            }
            case "unsignedLong" -> {
                defO.addProperty("type", "integer");
                defO.addProperty("format", "uint64");
                if (inclMinMax) defO.addProperty("minimum", java.math.BigInteger.ZERO);
                if (inclMinMax) defO.addProperty("maximum", new java.math.BigInteger("18446744073709551615"));
            }
            case "unsignedShort" -> {
                defO.addProperty("type", "integer");
                defO.addProperty("format", "uint16");
                if (inclMinMax) defO.addProperty("minimum", 0);
                if (inclMinMax) defO.addProperty("maximum", 65535);
            }
            case "untypedAtomic" -> {
                defO.addProperty("type", "string");
            }
            case "yearMonthDuration" -> {
                defO.addProperty("type", "string");
                defO.addProperty("format", "duration");
                if (inclPattern) defO.addProperty("pattern", "^-?P(?=.+)(?:\\d+Y)?(?:\\d+M)?$");
            }
            default -> {
                defO.addProperty("type", "string");
            }
        } 
    }
    
    
    // Returns 
    public String qnToKey (String qn) {
        return map.qnToN(qn);
    }
    
    // Parses JSON text to create a JsonObject containing a pair
    // "key": having the value of the parsed text.
    public static JsonObject makeObject (String key, String valueTxt) {
        var valE = makeElement(valueTxt);
        var obj  = new JsonObject();
        obj.add(key, valE);
        return obj;
    }

    // Creates a JsonObject containing one pair, "key": val
    public static JsonObject makeObject (String key, JsonElement val) {
        var obj = new JsonObject();
        obj.add(key, val);
        return obj;
    }
    
    public static JsonArray makeStringArray (String... vals) {
        var a = new JsonArray();
        for (var v : vals) a.add(v);
        return a;
    }

    // Parses JSON text to return a JsonElement
    public static JsonElement makeElement (String jsonText) {
        var je = JsonParser.parseString(jsonText);
        return je;
    }


}
