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
package org.mitre.niem.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static java.util.Map.entry;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.impl.xs.util.XSObjectListImpl;
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
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_ENUMERATION;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_FRACTIONDIGITS;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_LENGTH;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_MAXINCLUSIVE;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_MAXLENGTH;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_MINEXCLUSIVE;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_MININCLUSIVE;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_MINLENGTH;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_PATTERN;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_TOTALDIGITS;
import static org.apache.xerces.xs.XSSimpleTypeDefinition.FACET_WHITESPACE;
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
public final class Xerces {

    static final Logger LOG = LogManager.getLogger(Xerces.class);

    private Xerces() {
        throw new AssertionError("No instances");
    }

    /**
     * Returns a list of language strings from the documentation elements within
     * the annotation elements of the specified object.
     *
     * <p>Returns an empty list if the specified object is null, unsupported, or
     * has no annotations.
     *
     * @param xobj XSObject
     * @return immutable list of documentation language strings
     */
    public static List<LanguageString> getDocumentation(XSObject xobj) {
        if (xobj == null) return List.of();

        var res = new ArrayList<LanguageString>();
        var xannL = getAnnotations(xobj);
        if (xannL.getLength() < 1) return List.of();

        for (int i = 0; i < xannL.getLength(); i++) {
            var xann = (XSAnnotation) xannL.item(i);
            res.addAll(getDocumentation(xann));
        }
        return List.copyOf(res);
    }

    /**
     * Returns a list of language strings from the documentation elements within
     * an annotation element.
     *
     * <p>Returns an empty list if the annotation is null or cannot be converted
     * to DOM.
     *
     * @param xann XSAnnotation object
     * @return immutable list of documentation language strings
     */
    public static List<LanguageString> getDocumentation(XSAnnotation xann) {
        if (xann == null) return List.of();

        var res = new ArrayList<LanguageString>();
        try {
            var db = ParserBootstrap.docBuilder();
            var doc = db.newDocument();
            xann.writeAnnotation(doc, W3C_DOM_DOCUMENT);
            var ae = doc.getDocumentElement();
            if (ae == null) return List.of();

            var docNL = ae.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "documentation");
            for (int i = 0; i < docNL.getLength(); i++) {
                var de = (Element) docNL.item(i);
                var text = de.getTextContent();
                var lang = getXMLLang(de);
                res.add(new LanguageString(text, lang));
            }
        } catch (ParserConfigurationException ex) {
            LOG.error("Internal parser error: {}", ex.getMessage());
        }
        return List.copyOf(res);
    }

    /**
     * Returns a list of annotation elements within the specified object.
     *
     * <p>You might expect that the XSObject class would have a getAnnotations
     * method, but you would be wrong.
     *
     * <p>Returns an empty list if the object is null or does not support annotations.
     *
     * @param xobj XSObject
     * @return XSObjectList of annotations, possibly empty
     */
    public static XSObjectList getAnnotations(XSObject xobj) {
        if (xobj == null) return XSObjectListImpl.EMPTY_LIST;

        switch (xobj.getType()) {
            case ATTRIBUTE_DECLARATION:
                return ((XSAttributeDeclaration) xobj).getAnnotations();

            case ELEMENT_DECLARATION:
                return ((XSElementDeclaration) xobj).getAnnotations();

            case TYPE_DEFINITION:
                var xtype = (XSTypeDefinition) xobj;
                if (SIMPLE_TYPE == xtype.getTypeCategory()) {
                    return ((XSSimpleTypeDefinition) xobj).getAnnotations();
                }
                return ((XSComplexTypeDefinition) xobj).getAnnotations();

            default:
                return XSObjectListImpl.EMPTY_LIST;
        }
    }

    /**
     * Returns the local name of the XSD element corresponding to the Xerces
     * facet kind value.
     *
     * @param kind Xerces facet kind value
     * @return XSD element local name, or the empty string if unknown
     */
    public static String facetKindToElementName(short kind) {
        switch (kind) {
            case FACET_ENUMERATION:    return "enumeration";
            case FACET_FRACTIONDIGITS: return "fractionDigits";
            case FACET_LENGTH:         return "length";
            case FACET_MAXEXCLUSIVE:   return "maxExclusive";
            case FACET_MAXINCLUSIVE:   return "maxInclusive";
            case FACET_MAXLENGTH:      return "maxLength";
            case FACET_MINEXCLUSIVE:   return "minExclusive";
            case FACET_MININCLUSIVE:   return "minInclusive";
            case FACET_MINLENGTH:      return "minLength";
            case FACET_PATTERN:        return "pattern";
            case FACET_TOTALDIGITS:    return "totalDigits";
            case FACET_WHITESPACE:     return "whiteSpace";
            default:                   return "";
        }
    }

    private static final Map<String, Short> F_CODE_TO_KIND = Map.ofEntries(
        entry("enumeration", FACET_ENUMERATION),
        entry("fractionDigits", FACET_FRACTIONDIGITS),
        entry("length", FACET_LENGTH),
        entry("maxExclusive", FACET_MAXEXCLUSIVE),
        entry("maxInclusive", FACET_MAXINCLUSIVE),
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
     * an XSD element.
     *
     * @param xsdLocalName XSD element local name
     * @return Xerces facet code value, or -1 if the name is not an XSD facet name
     */
    public static short facetElementNameToKind(String xsdLocalName) {
        Short rv = F_CODE_TO_KIND.get(xsdLocalName);
        return (rv != null) ? rv : -1;
    }

    private static final String[] XERCES_FACET_DATA = {
        "ENTITIES",           "minLength",      "1",
        "ENTITIES",           "whiteSpace",     "collapse",
        "ENTITY",             "whiteSpace",     "collapse",
        "ID",                 "whiteSpace",     "collapse",
        "IDREF",              "whiteSpace",     "collapse",
        "IDREFS",             "minLength",      "1",
        "IDREFS",             "whiteSpace",     "collapse",
        "NCName",             "pattern",        "\\i\\c*\"\"[\\i-[:]][\\c-[:]]*",
        "NCName",             "whiteSpace",     "collapse",
        "NMTOKEN",            "pattern",        "\\c+",
        "NMTOKEN",            "whiteSpace",     "collapse",
        "NMTOKENS",           "minLength",      "1",
        "NMTOKENS",           "whiteSpace",     "collapse",
        "NOTATION",           "whiteSpace",     "collapse",
        "Name",               "pattern",        "\\i\\c*",
        "Name",               "whiteSpace",     "collapse",
        "QName",              "whiteSpace",     "collapse",
        "anyURI",             "whiteSpace",     "collapse",
        "base64Binary",       "whiteSpace",     "collapse",
        "boolean",            "whiteSpace",     "collapse",
        "byte",               "fractionDigits", "0",
        "byte",               "maxInclusive",   "127",
        "byte",               "minInclusive",   "-128",
        "byte",               "pattern",        "[\\-+]?[0-9]+",
        "byte",               "whiteSpace",     "collapse",
        "date",               "whiteSpace",     "collapse",
        "dateTime",           "whiteSpace",     "collapse",
        "decimal",            "whiteSpace",     "collapse",
        "double",             "whiteSpace",     "collapse",
        "duration",           "whiteSpace",     "collapse",
        "float",              "whiteSpace",     "collapse",
        "gDay",               "whiteSpace",     "collapse",
        "gMonth",             "whiteSpace",     "collapse",
        "gMonthDay",          "whiteSpace",     "collapse",
        "gYear",              "whiteSpace",     "collapse",
        "gYearMonth",         "whiteSpace",     "collapse",
        "hexBinary",          "whiteSpace",     "collapse",
        "int",                "fractionDigits", "0",
        "int",                "maxInclusive",   "2147483647",
        "int",                "minInclusive",   "-2147483648",
        "int",                "pattern",        "[\\-+]?[0-9]+",
        "int",                "whiteSpace",     "collapse",
        "integer",            "fractionDigits", "0",
        "integer",            "pattern",        "[\\-+]?[0-9]+",
        "integer",            "whiteSpace",     "collapse",
        "language",           "pattern",        "([a-zA-Z]{1,8})(-[a-zA-Z0-9]{1,8})*",
        "language",           "whiteSpace",     "collapse",
        "long",               "fractionDigits", "0",
        "long",               "maxInclusive",   "9223372036854775807",
        "long",               "minInclusive",   "-9223372036854775808",
        "long",               "pattern",        "[\\-+]?[0-9]+",
        "long",               "whiteSpace",     "collapse",
        "negativeInteger",    "fractionDigits", "0",
        "negativeInteger",    "maxInclusive",   "-1",
        "negativeInteger",    "pattern",        "[\\-+]?[0-9]+",
        "negativeInteger",    "whiteSpace",     "collapse",
        "nonNegativeInteger", "fractionDigits", "0",
        "nonNegativeInteger", "minInclusive",   "0",
        "nonNegativeInteger", "pattern",        "[\\-+]?[0-9]+",
        "nonNegativeInteger", "whiteSpace",     "collapse",
        "nonPositiveInteger", "fractionDigits", "0",
        "nonPositiveInteger", "maxInclusive",   "0",
        "nonPositiveInteger", "pattern",        "[\\-+]?[0-9]+",
        "nonPositiveInteger", "whiteSpace",     "collapse",
        "normalizedString",   "whiteSpace",     "replace",
        "positiveInteger",    "fractionDigits", "0",
        "positiveInteger",    "minInclusive",   "1",
        "positiveInteger",    "pattern",        "[\\-+]?[0-9]+",
        "positiveInteger",    "whiteSpace",     "collapse",
        "short",              "fractionDigits", "0",
        "short",              "maxInclusive",   "32767",
        "short",              "minInclusive",   "-32768",
        "short",              "pattern",        "[\\-+]?[0-9]+",
        "short",              "whiteSpace",     "collapse",
        "string",             "whiteSpace",     "preserve",
        "time",               "whiteSpace",     "collapse",
        "token",              "whiteSpace",     "collapse",
        "unsignedByte",       "fractionDigits", "0",
        "unsignedByte",       "maxInclusive",   "255",
        "unsignedByte",       "minInclusive",   "0",
        "unsignedByte",       "pattern",        "[\\-+]?[0-9]+",
        "unsignedByte",       "whiteSpace",     "collapse",
        "unsignedInt",        "fractionDigits", "0",
        "unsignedInt",        "maxInclusive",   "4294967295",
        "unsignedInt",        "minInclusive",   "0",
        "unsignedInt",        "pattern",        "[\\-+]?[0-9]+",
        "unsignedInt",        "whiteSpace",     "collapse",
        "unsignedLong",       "fractionDigits", "0",
        "unsignedLong",       "maxInclusive",   "18446744073709551615",
        "unsignedLong",       "minInclusive",   "0",
        "unsignedLong",       "pattern",        "[\\-+]?[0-9]+",
        "unsignedLong",       "whiteSpace",     "collapse",
        "unsignedShort",      "fractionDigits", "0",
        "unsignedShort",      "maxInclusive",   "65535",
        "unsignedShort",      "minInclusive",   "0",
        "unsignedShort",      "pattern",        "[\\-+]?[0-9]+",
        "unsignedShort",      "whiteSpace",     "collapse",
    };

    private record DefaultFacet(short kind, String value) { }

    private static final Map<String, List<DefaultFacet>> DEF_FACET;

    static {
        DEF_FACET = new HashMap<>();
        for (int i = 0; i < XERCES_FACET_DATA.length; i += 3) {
            var xsdtype = XERCES_FACET_DATA[i];
            var element = XERCES_FACET_DATA[i + 1];
            var value = XERCES_FACET_DATA[i + 2];
            var fkind = facetElementNameToKind(element);

            var dfL = DEF_FACET.get(xsdtype);
            if (dfL == null) {
                dfL = new ArrayList<>();
                DEF_FACET.put(xsdtype, dfL);
            }
            dfL.add(new DefaultFacet(fkind, value));
        }
        DEF_FACET.put("anyType", List.of());
        DEF_FACET.put("anySimpleType", List.of());
    }

    /**
     * The Xerces schema model object includes facets that do not appear in
     * the schema document. These default facets are presumably used to enforce
     * builtin datatype constraints in a validating parser. For example
     * {@code <xs:restriction base="xs:byte">}
     * will create four default facets in the schema type definition.
     *
     * @param xtype type definition against which the facet is applied
     * @param facetKind facet kind code; for example FACET_LENGTH for
     * {@code <xs:length value="2"/>}
     * @param value facet value; for example "2" for {@code <xs:length value="2"/>}
     * @return true for a default facet
     */
    public static boolean isDefaultFacet(XSTypeDefinition xtype, short facetKind, String value) {
        if (xtype == null) return false;

        while (xtype != null) {
            var typeName = xtype.getName();
            if (typeName != null && DEF_FACET.containsKey(typeName)) {
                for (var dfr : DEF_FACET.get(typeName)) {
                    if (facetKind == dfr.kind && Objects.equals(value, dfr.value)) {
                        return true;
                    }
                }
                return false;
            }

            var base = xtype.getBaseType();
            if (base == xtype) break;
            xtype = base;
        }
        return false;
    }

    /**
     * Returns true if the specified facet is one of the implicit builtin facets
     * added by Xerces for the supplied type.
     *
     * @param xtype type definition against which the facet is applied
     * @param f facet
     * @return true for a default facet
     */
    public static boolean isDefaultFacet(XSTypeDefinition xtype, XSFacet f) {
        if (f == null) return false;
        return isDefaultFacet(xtype, f.getFacetKind(), f.getLexicalFacetValue());
    }
}
