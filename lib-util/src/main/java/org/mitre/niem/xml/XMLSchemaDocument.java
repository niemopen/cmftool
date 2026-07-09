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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import static org.w3c.dom.Node.ELEMENT_NODE;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Provides information from an XML Schema document that is not directly
 * available through the Xerces XSModel API.
 *
 * <p>This class adds convenience accessors for:
 * <ul>
 * <li>schema document {@code targetNamespace}</li>
 * <li>schema document {@code xml:lang}</li>
 * <li>schema document {@code version}</li>
 * <li>schema-level documentation</li>
 * <li>top-level {@code xs:import} elements</li>
 * </ul>
 */
public class XMLSchemaDocument extends XMLDocument {

    static final Logger LOG = LogManager.getLogger(XMLSchemaDocument.class);

    private String targetNS = null;
    private String lang = null;
    private String version = null;
    private List<LanguageString> docL = null;
    private List<XMLSchemaImport> importL = null;

    /**
     * Constructs an XMLSchemaDocument from an XSD file.
     *
     * @param sdF XSD file
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the file is not well-formed XML
     * @throws IOException if the file cannot be read
     */
    public XMLSchemaDocument(File sdF) throws ParserConfigurationException, SAXException, IOException {
        super(sdF);
    }

    /**
     * Constructs an XMLSchemaDocument from an input stream.
     *
     * @param is XSD input stream
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the stream is not well-formed XML
     * @throws IOException if the stream cannot be read
     */
    public XMLSchemaDocument(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        super(is);
    }

    /**
     * Constructs an XMLSchemaDocument from an input stream and document URI.
     *
     * @param is XSD input stream
     * @param uri document URI or null
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the stream is not well-formed XML
     * @throws IOException if the stream cannot be read
     */
    public XMLSchemaDocument(InputStream is, URI uri)
        throws ParserConfigurationException, SAXException, IOException {
        super(is, uri);
    }

    /**
     * Constructs an XMLSchemaDocument from an input source.
     *
     * @param src XSD input source
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the source is not well-formed XML
     * @throws IOException if the source cannot be read
     */
    public XMLSchemaDocument(InputSource src)
        throws ParserConfigurationException, SAXException, IOException {
        super(src);
    }

    /**
     * Constructs an XMLSchemaDocument from an input source and document URI.
     *
     * @param src XSD input source
     * @param uri document URI or null
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the source is not well-formed XML
     * @throws IOException if the source cannot be read
     */
    public XMLSchemaDocument(InputSource src, URI uri)
        throws ParserConfigurationException, SAXException, IOException {
        super(src, uri);
    }

    /**
     * Returns the {@code xml:lang} attribute of the schema document element.
     * Returns the empty string if absent.
     *
     * @return schema document language or the empty string
     */
    public String language() {
        if (lang != null) return lang;
        var docE = documentElement();
        lang = (docE == null) ? "" : docE.getAttributeNS(XML_NS_URI, "lang");
        return lang;
    }

    /**
     * Returns the {@code targetNamespace} attribute of the schema document element.
     * Returns the empty string if absent.
     *
     * @return target namespace or the empty string
     */
    public String targetNamespace() {
        if (targetNS != null) return targetNS;
        var docE = documentElement();
        targetNS = (docE == null) ? "" : docE.getAttribute("targetNamespace");
        return targetNS;
    }

    /**
     * Returns the {@code version} attribute of the schema document element.
     * Returns the empty string if absent.
     *
     * @return schema version or the empty string
     */
    public String version() {
        if (version != null) return version;
        var docE = documentElement();
        version = (docE == null) ? "" : docE.getAttribute("version");
        return version;
    }

    /**
     * Returns schema-level documentation from {@code xs:annotation/xs:documentation}
     * children of the schema document element.
     *
     * @return unmodifiable list of documentation strings
     */
    public List<LanguageString> documentation() {
        if (docL == null) {
            docL = Collections.unmodifiableList(getDocumentation(documentElement()));
        }
        return docL;
    }

    /**
     * Returns all top-level {@code xs:import} elements from the schema document.
     *
     * @return unmodifiable list of import records
     */
    public List<XMLSchemaImport> importElements() {
        if (importL != null) return importL;

        var found = new ArrayList<XMLSchemaImport>();
        var docE = documentElement();
        if (docE == null) {
            importL = Collections.emptyList();
            return importL;
        }

        var nls = docE.getChildNodes();
        for (int i = 0; i < nls.getLength(); i++) {
            var node = nls.item(i);
            if (ELEMENT_NODE != node.getNodeType()) continue;
            if (!W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())) continue;
            if (!"import".equals(node.getLocalName())) continue;

            var e = (Element) node;
            var nsU = e.getAttribute("namespace");
            var sloc = e.getAttribute("schemaLocation");
            var docStrings = getDocumentation(e);

            var atts = e.getAttributes();
            var attL = new ArrayList<XMLAttribute>();
            for (int j = 0; j < atts.getLength(); j++) {
                var att = (Attr) atts.item(j);
                var ansU = att.getNamespaceURI();
                if (ansU == null) continue;

                var attQ = att.getName();
                var aname = qnToName(attQ);
                var aval = att.getValue();
                attL.add(new XMLAttribute(ansU, aname, aval));
            }

            found.add(new XMLSchemaImport(nsU, sloc, attL, docStrings));
        }

        importL = Collections.unmodifiableList(found);
        return importL;
    }

    /**
     * Returns documentation strings from the immediate
     * {@code xs:annotation/xs:documentation} content of the supplied schema element.
     *
     * @param e schema element
     * @return list of documentation strings; empty if none
     */
    public static List<LanguageString> getDocumentation(Element e) {
        var res = new ArrayList<LanguageString>();
        if (e == null) return res;

        var nodeL = e.getChildNodes();
        for (int i = 0; i < nodeL.getLength(); i++) {
            var node = nodeL.item(i);
            if (ELEMENT_NODE != node.getNodeType()) continue;

            var ce = (Element) node;
            if (!W3C_XML_SCHEMA_NS_URI.equals(ce.getNamespaceURI())) continue;
            if (!"annotation".equals(ce.getLocalName())) continue;

            var dlist = ce.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "documentation");
            for (int j = 0; j < dlist.getLength(); j++) {
                var de = (Element) dlist.item(j);
                var text = de.getTextContent();
                var lang = getXMLLang(de);
                res.add(new LanguageString(text, lang));
            }
        }
        return res;
    }

    /**
     * Returns a LanguageString containing the element text content and the
     * in-scope {@code xml:lang}.
     *
     * @param e element
     * @return language string
     */
    public static LanguageString getLanguageString(Element e) {
        if (e == null) return new LanguageString("", "en-US");
        return new LanguageString(e.getTextContent(), getXMLLang(e));
    }

    /**
     * Returns the in-scope value of {@code xml:lang} for an element.
     * Returns {@code en-US} if no {@code xml:lang} is in scope.
     *
     * @param e element
     * @return in-scope language
     */
    public static String getXMLLang(Element e) {
        var cur = e;
        while (cur != null) {
            var lang = cur.getAttributeNS(XML_NS_URI, "lang");
            if (!lang.isEmpty()) return lang;

            var parent = cur.getParentNode();
            if (parent == null || parent.getNodeType() != ELEMENT_NODE) break;
            cur = (Element) parent;
        }
        return "en-US";
    }
}
