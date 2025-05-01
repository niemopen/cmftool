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
package org.mitre.niem.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xs.XSAnnotation;
import static org.apache.xerces.xs.XSAnnotation.W3C_DOM_DOCUMENT;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import static org.apache.xerces.xs.XSConstants.ATTRIBUTE_DECLARATION;
import static org.apache.xerces.xs.XSConstants.ELEMENT_DECLARATION;
import static org.apache.xerces.xs.XSConstants.TYPE_DEFINITION;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSFacet;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.*;
import org.apache.xerces.xs.XSTypeDefinition;
import static org.apache.xerces.xs.XSTypeDefinition.SIMPLE_TYPE;
import static org.mitre.niem.xml.XMLSchemaDocument.getXMLLang;
import org.w3c.dom.Element;

/**
 * A class with several static methods useful in conjunction with the Xerces 
 * XML Schema API.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Xerces {
    static final Logger LOG = LogManager.getLogger(Xerces.class);      

    /**
     * Returns a list of language strings from the document elements within the
     * annotation elements within the specified object.  Returns an empty list if
     * the specified object doesn't have annotations.     * 
     * @param xobj - XSObject
     * @return list of documentation language strings
     */
    public static List<LanguageString> getDocumentation (XSObject xobj) {
        var res   = new ArrayList<LanguageString>();
        var xannL = getAnnotations(xobj);
        if (null == xannL || xannL.getLength() < 1) return res;
        for (int i = 0; i < xannL.getLength(); i++) {
            var xann = (XSAnnotation)xannL.item(i);
            res.addAll(getDocumentation(xann));
        }
        return res;
    }
    
    /**
     * Returns a list of language strings from the document elements within an
     * annotation element.
     * @param xann - XSAnnotation object
     * @return list of documentation language strings
     */
    public static List<LanguageString> getDocumentation (XSAnnotation xann) {
        var res = new ArrayList<LanguageString>();
        try {
            var db  = ParserBootstrap.docBuilder();
            var doc = db.newDocument();
            xann.writeAnnotation(doc, W3C_DOM_DOCUMENT);
            var ae  = doc.getDocumentElement();
            var docNL = ae.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "documentation");
            for (int i = 0; i < docNL.getLength(); i++) {
                var de = (Element)docNL.item(i);
                var text = de.getTextContent();
                var lang = getXMLLang(de);
                res.add(new LanguageString(text, lang));                    
            }
        } catch (ParserConfigurationException ex) {
            LOG.error("Internal parser error: {}", ex.getMessage());
        }
        return res;
    }
    
    // Returns a list of annotation elements within the specified object.  You
    // might expect that the XSObject class would have a getAnnotations method,
    // but you would be wrong.
    public static XSObjectList getAnnotations (XSObject xobj)  {
        switch (xobj.getType()) {
        case ATTRIBUTE_DECLARATION: return ((XSAttributeDeclaration)xobj).getAnnotations();
        case ELEMENT_DECLARATION:   return ((XSElementDeclaration)xobj).getAnnotations();
        case TYPE_DEFINITION:
            var xtype = (XSTypeDefinition)xobj;
            if (SIMPLE_TYPE == xtype.getTypeCategory()) return ((XSSimpleTypeDefinition)xobj).getAnnotations();
            else return ((XSComplexTypeDefinition)xobj).getAnnotations();
        }
        return null;
    }
    
    public static String facetKind2Code (String s) { return null; }
        
    
    /**
     * Returns the local name of the XSD element corresponding to the Xerces
     * facet kind value.
     * @param kind - Xerces facet kind value
     * @return XSD element local name
     */
    public static String facetKindToElementName (short kind) {
        switch (kind) {
            case FACET_ENUMERATION:     return "enumeration";
            case FACET_FRACTIONDIGITS:  return "fractionDigits";
            case FACET_LENGTH:          return "length";
            case FACET_MAXEXCLUSIVE:    return "maxExclusive";
            case FACET_MAXINCLUSIVE:    return "maxInclusive";
            case FACET_MAXLENGTH:       return "maxLength";
            case FACET_MINEXCLUSIVE:    return "minExclusive";
            case FACET_MININCLUSIVE:    return "minInclusive";
            case FACET_MINLENGTH:       return "minLength";
            case FACET_PATTERN:         return "pattern";
            case FACET_TOTALDIGITS:     return "totalDigits";
            case FACET_WHITESPACE:      return "whiteSpace";
            default: return "";
        }
    }
    
    private final static Map<String,Short> fCode2Kind = Map.ofEntries(
        entry("enumeration", FACET_ENUMERATION),
        entry("fractionDigits", FACET_FRACTIONDIGITS),
        entry("length", FACET_LENGTH),
        entry("mxExclusive", FACET_MAXEXCLUSIVE),
        entry("mxInclusive", FACET_MAXINCLUSIVE),
        entry("maxLength", FACET_MAXLENGTH),
        entry("minExclusive", FACET_MINEXCLUSIVE),
        entry("minInclusive", FACET_MININCLUSIVE),
        entry("minLength", FACET_MINLENGTH),
        entry("pattern", FACET_PATTERN),
        entry("totalDigits", FACET_TOTALDIGITS),
        entry("whiteSpace", FACET_WHITESPACE)
    );
    /**
     * Returns the Xerces facet kind value corresponding to a local name of
     * an XSD element. Returns -1 if the name is not an XSD facet name.
     * @param xsdLocalName - XSD element local name
     * @return Xerces facet code value
     */
    public static short facetElementNameToKind (String xsdLocalName) {
        Short rv = fCode2Kind.get(xsdLocalName);
        if (null != rv) return rv;
        return -1;
    }

    private static final String[] xercesFacetData =  {
        "ENTITIES",           "minLength",             "1",
        "ENTITIES",           "whiteSpace",            "collapse",
        "ENTITY",             "whiteSpace",            "collapse",
        "ID",                 "whiteSpace",            "collapse",
        "IDREF",              "whiteSpace",            "collapse",
        "IDREFS",             "minLength",             "1",
        "IDREFS",             "whiteSpace",            "collapse",
        "NCName",             "pattern",               "\\i\\c*\"\"[\\i-[:]][\\c-[:]]*",
        "NCName",             "whiteSpace",            "collapse",
        "NMTOKEN",            "pattern",               "\\c+",
        "NMTOKEN",            "whiteSpace",            "collapse",
        "NMTOKENS",           "minLength",             "1",
        "NMTOKENS",           "whiteSpace",            "collapse",
        "NOTATION",           "whiteSpace",            "collapse",
        "Name",               "pattern",               "\\i\\c*",
        "Name",               "whiteSpace",            "collapse",
        "QName",              "whiteSpace",            "collapse",
        "anyURI",             "whiteSpace",            "collapse",
        "base64Binary",       "whiteSpace",            "collapse",
        "boolean",            "whiteSpace",            "collapse",
        "byte",               "fractionDigits",        "0",
        "byte",               "maxInclusive",          "127",
        "byte",               "minInclusive",          "-128",
        "byte",               "pattern",               "[\\-+]?[0-9]+",
        "byte",               "whiteSpace",            "collapse",
        "date",               "whiteSpace",            "collapse",
        "dateTime",           "whiteSpace",            "collapse",
        "decimal",            "whiteSpace",            "collapse",
        "double",             "whiteSpace",            "collapse",
        "duration",           "whiteSpace",            "collapse",
        "float",              "whiteSpace",            "collapse",
        "gDay",               "whiteSpace",            "collapse",
        "gMonth",             "whiteSpace",            "collapse",
        "gMonthDay",          "whiteSpace",            "collapse",
        "gYear",              "whiteSpace",            "collapse",
        "gYearMonth",         "whiteSpace",            "collapse",
        "hexBinary",          "whiteSpace",            "collapse",
        "int",                "fractionDigits",        "0",
        "int",                "maxInclusive",          "2147483647",
        "int",                "minInclusive",          "-2147483648",
        "int",                "pattern",               "[\\-+]?[0-9]+",
        "int",                "whiteSpace",            "collapse",
        "integer",            "fractionDigits",        "0",
        "integer",            "pattern",               "[\\-+]?[0-9]+",
        "integer",            "whiteSpace",            "collapse",
        "language",           "pattern",               "([a-zA-Z]{1,8})(-[a-zA-Z0-9]{1,8})*",
        "language",           "whiteSpace",            "collapse",
        "long",               "fractionDigits",        "0",
        "long",               "maxInclusive",          "9223372036854775807",
        "long",               "minInclusive",          "-9223372036854775808",
        "long",               "pattern",               "[\\-+]?[0-9]+",
        "long",               "whiteSpace",            "collapse",
        "negativeInteger",    "fractionDigits",        "0",
        "negativeInteger",    "maxInclusive",          "-1",
        "negativeInteger",    "pattern",               "[\\-+]?[0-9]+",
        "negativeInteger",    "whiteSpace",            "collapse",
        "nonNegativeInteger", "fractionDigits",        "0",
        "nonNegativeInteger", "minInclusive",          "0",
        "nonNegativeInteger", "pattern",               "[\\-+]?[0-9]+",
        "nonNegativeInteger", "whiteSpace",            "collapse",
        "nonPositiveInteger", "fractionDigits",        "0",
        "nonPositiveInteger", "maxInclusive",          "0",
        "nonPositiveInteger", "pattern",               "[\\-+]?[0-9]+",
        "nonPositiveInteger", "whiteSpace",            "collapse",
        "normalizedString",   "whiteSpace",            "replace",
        "positiveInteger",    "fractionDigits",        "0",
        "positiveInteger",    "minInclusive",          "1",
        "positiveInteger",    "pattern",               "[\\-+]?[0-9]+",
        "positiveInteger",    "whiteSpace",            "collapse",
        "short",              "fractionDigits",        "0",
        "short",              "maxInclusive",          "32767",
        "short",              "minInclusive",          "-32768",
        "short",              "pattern",               "[\\-+]?[0-9]+",
        "short",              "whiteSpace",            "collapse",
        "string",             "whiteSpace",            "preserve",
        "time",               "whiteSpace",            "collapse",
        "token",              "whiteSpace",            "collapse",
        "unsignedByte",       "fractionDigits",        "0",
        "unsignedByte",       "maxInclusive",          "255",
        "unsignedByte",       "minInclusive",          "0",
        "unsignedByte",       "pattern",               "[\\-+]?[0-9]+",
        "unsignedByte",       "whiteSpace",            "collapse",
        "unsignedInt",        "fractionDigits",        "0",
        "unsignedInt",        "maxInclusive",          "4294967295",
        "unsignedInt",        "minInclusive",          "0",
        "unsignedInt",        "pattern",               "[\\-+]?[0-9]+",
        "unsignedInt",        "whiteSpace",            "collapse",
        "unsignedLong",       "fractionDigits",        "0",
        "unsignedLong",       "maxInclusive",          "18446744073709551615",
        "unsignedLong",       "minInclusive",          "0",
        "unsignedLong",       "pattern",               "[\\-+]?[0-9]+",
        "unsignedLong",       "whiteSpace",            "collapse",
        "unsignedShort",      "fractionDigits",        "0",
        "unsignedShort",      "maxInclusive",          "65535",
        "unsignedShort",      "minInclusive",          "0",
        "unsignedShort",      "pattern",               "[\\-+]?[0-9]+",
        "unsignedShort",      "whiteSpace",            "collapse",        
    };
    
    private record DefaultFacet (short kind, String value) { }
    private static final Map<String,List<DefaultFacet>> defFacet;
    static {
        defFacet = new HashMap<>();
        for (int i = 0; i < xercesFacetData.length; i += 3) {
            var xsdtype = xercesFacetData[i];
            var element = xercesFacetData[i+1];
            var value   = xercesFacetData[i+2];
            var fkind   = facetElementNameToKind(element);
            var dfL     = defFacet.get(xsdtype);
            if (null == dfL) {
                dfL = new ArrayList<>();
                defFacet.put(xsdtype, dfL);
            }
            dfL.add(new DefaultFacet(fkind, value));
        }
        defFacet.put("anyType", new ArrayList<>());
        defFacet.put("anySimpleType", new ArrayList<>());
    }

    /**
    * The Xerces schema model object includes facets that do not appear in 
    * the schema document.  These default facets are presumably used to enforce 
    * bultin datatype constraints in a validating parser.  For example
    *    &lt;xs:restriction base="xs:byte"&gt;
    * will create four default facets in the schema type definition.
     * @param xtype - type definition against which the Facet is applied
     * @param facetKind - facet kind code; e.g. FACET_LENGTH for &lt;xs:length value="2"/&gt; 
     * @param value - facet value; e.g "2" for &lt;xs:length value="2"/&gt;
     * @return true for a default facet
     */
    public static boolean isDefaultFacet (XSTypeDefinition xtype, short facetKind, String value) {
        var typeName = xtype.getName();
        while (!defFacet.containsKey(typeName)) {
            xtype = xtype.getBaseType();
            typeName = xtype.getName();
        }
        for (var dfr : defFacet.get(typeName)) {
            if (facetKind == dfr.kind && value.equals(dfr.value)) return true;
        }
        return false;
    }
    
    public static boolean isDefaultFacet (XSTypeDefinition xtype, XSFacet f) {
        return isDefaultFacet(xtype, f.getFacetKind(), f.getLexicalFacetValue());
    }
    
}
