/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2028 The MITRE Corporation.
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
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.xml.XMLConstants;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.w3c.dom.Node.ELEMENT_NODE;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utility methods for working with XML documents.
 *
 * <p>This class wraps a parsed DOM document and provides convenience methods for:
 * <ul>
 * <li>obtaining the document and document element</li>
 * <li>enumerating namespace declarations</li>
 * <li>evaluating XPath expressions</li>
 * <li>working with QName strings and component URIs</li>
 * <li>reading selected metadata from XML and XSD documents using StAX</li>
 * </ul>
 *
 * <p>Instances are parsed eagerly in the constructor. This makes it possible to
 * support one-shot inputs such as {@link InputStream}.
 *
 * <p>When constructed from an {@link InputStream} or {@link InputSource}, the
 * document URI may be unknown unless supplied explicitly. Providing a system ID
 * is recommended when relative URI resolution matters.
 */
public class XMLDocument {

    static final Logger LOG = LogManager.getLogger(XMLDocument.class);

    private static final NamespaceContext NS_CONTEXT = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix == null) return XMLConstants.NULL_NS_URI;
            return switch (prefix) {
                case XML_NS_PREFIX -> XML_NS_URI;
                case XMLNS_ATTRIBUTE -> XMLNS_ATTRIBUTE_NS_URI;
                case "xs" -> W3C_XML_SCHEMA_NS_URI;
                case "xsi" -> "http://www.w3.org/2001/XMLSchema-instance";
                default -> XMLConstants.NULL_NS_URI;
            };
        }

        @Override
        public String getPrefix(String namespaceURI) {
            if (namespaceURI == null) return null;
            return switch (namespaceURI) {
                case XML_NS_URI -> XML_NS_PREFIX;
                case XMLNS_ATTRIBUTE_NS_URI -> XMLNS_ATTRIBUTE;
                case W3C_XML_SCHEMA_NS_URI -> "xs";
                case "http://www.w3.org/2001/XMLSchema-instance" -> "xsi";
                default -> null;
            };
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            var p = getPrefix(namespaceURI);
            return (p == null)
                ? Collections.<String>emptyIterator()
                : Collections.singleton(p).iterator();
        }
    };

    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

    private final URI docURI;
    private final File docF;
    private final Document doc;
    private List<XMLNamespaceDeclaration> nsdecls = null;

    private record Frame(Element el, int depth) {}

    /**
     * Constructs an XMLDocument by parsing the supplied file.
     *
     * @param f XML document file
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the file is not well-formed XML
     * @throws IOException if the file cannot be read
     */
    public XMLDocument(File f) throws ParserConfigurationException, SAXException, IOException {
        Objects.requireNonNull(f, "file must not be null");
        this.docF = f.getCanonicalFile();
        this.docURI = this.docF.toURI();
        this.doc = parse(this.docF);
    }

    /**
     * Constructs an XMLDocument by parsing the supplied input stream.
     *
     * <p>The resulting document has no associated file or URI.
     *
     * @param is XML input stream
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the stream is not well-formed XML
     * @throws IOException if the stream cannot be read
     */
    public XMLDocument(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        this(is, (URI) null);
    }

    /**
     * Constructs an XMLDocument by parsing the supplied input stream and
     * associating it with the supplied document URI.
     *
     * <p>If the URI is non-null, it is used as the {@code systemId} of the
     * underlying {@link InputSource}.
     *
     * @param is XML input stream
     * @param uri document URI or null
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the stream is not well-formed XML
     * @throws IOException if the stream cannot be read
     */
    public XMLDocument(InputStream is, URI uri) throws ParserConfigurationException, SAXException, IOException {
        Objects.requireNonNull(is, "input stream must not be null");
        this.docF = null;
        this.docURI = uri;
        var src = new InputSource(is);
        if (uri != null) src.setSystemId(uri.toString());
        this.doc = parse(src);
    }

    /**
     * Constructs an XMLDocument by parsing the supplied input source.
     *
     * <p>The resulting document URI is taken from the input source system ID if
     * present; otherwise it is null.
     *
     * @param src XML input source
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the source is not well-formed XML
     * @throws IOException if the source cannot be read
     */
    public XMLDocument(InputSource src) throws ParserConfigurationException, SAXException, IOException {
        Objects.requireNonNull(src, "input source must not be null");
        this.docF = null;
        this.docURI = parseURI(src.getSystemId());
        this.doc = parse(src);
    }

    /**
     * Constructs an XMLDocument by parsing the supplied input source and
     * associating it with the supplied document URI.
     *
     * <p>If the URI is non-null and the input source has no system ID, the URI is
     * assigned as the source system ID before parsing.
     *
     * @param src XML input source
     * @param uri document URI or null
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the source is not well-formed XML
     * @throws IOException if the source cannot be read
     */
    public XMLDocument(InputSource src, URI uri) throws ParserConfigurationException, SAXException, IOException {
        Objects.requireNonNull(src, "input source must not be null");
        this.docF = null;
        this.docURI = uri;
        if (uri != null && (src.getSystemId() == null || src.getSystemId().isBlank())) {
            src.setSystemId(uri.toString());
        }
        this.doc = parse(src);
    }

    /**
     * Returns the document URI, or null if none is known.
     *
     * @return document URI or null
     */
    public URI docURI() {
        return docURI;
    }

    /**
     * Returns the source file, or null if this document was not constructed from a file.
     *
     * @return source file or null
     */
    public File docFile() {
        return docF;
    }

    /**
     * Returns the parsed DOM document.
     *
     * @return DOM document
     */
    public Document dom() {
        return doc;
    }

    /**
     * Returns the document element.
     *
     * @return document element, or null if the DOM has no document element
     */
    public Element documentElement() {
        return doc == null ? null : doc.getDocumentElement();
    }

    /**
     * Returns all namespace declarations found in the document, in traversal order.
     *
     * <p>The result is computed lazily and cached. The returned list is unmodifiable.
     *
     * @return list of namespace declarations
     */
    public List<XMLNamespaceDeclaration> namespaceDeclarations() {
        if (nsdecls != null) return nsdecls;

        var root = documentElement();
        if (root == null) {
            nsdecls = List.of();
            return nsdecls;
        }

        var todo = new ArrayDeque<Frame>();
        var found = new ArrayList<XMLNamespaceDeclaration>();
        todo.push(new Frame(root, 0));

        while (!todo.isEmpty()) {
            var fr = todo.pop();
            var el = fr.el();
            var dep = fr.depth();
            var ats = el.getAttributes();

            for (int i = 0; i < ats.getLength(); i++) {
                var n = ats.item(i);
                if (XMLNS_ATTRIBUTE_NS_URI.equals(n.getNamespaceURI())) {
                    var pre = XMLNS_ATTRIBUTE.equals(n.getNodeName()) ? "" : n.getLocalName();
                    var uri = n.getNodeValue();
                    found.add(new XMLNamespaceDeclaration(pre, uri, 0, dep));
                }
            }

            var chs = el.getChildNodes();
            for (int i = chs.getLength() - 1; i >= 0; i--) {
                var n = chs.item(i);
                if (ELEMENT_NODE == n.getNodeType()) {
                    todo.push(new Frame((Element) n, dep + 1));
                }
            }
        }

        nsdecls = Collections.unmodifiableList(found);
        return nsdecls;
    }

    /**
     * Evaluates an XPath expression as a boolean against the supplied element.
     *
     * @param e context element
     * @param expr XPath expression
     * @return boolean result, or false if the expression is invalid
     */
    public static boolean evalForBoolean(Element e, String expr) {
        try {
            var xpr = compileXPath(expr);
            return (Boolean) xpr.evaluate(e, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException ex) {
            LOG.error("Invalid XPath expression {}: {}", expr, ex.getMessage());
            return false;
        }
    }

    /**
     * Evaluates an XPath expression as a string against the supplied element.
     *
     * @param e context element
     * @param expr XPath expression
     * @return string result, or the empty string if the expression is invalid
     */
    public static String evalForString(Element e, String expr) {
        try {
            var xpr = compileXPath(expr);
            return evalForString(e, xpr);
        } catch (XPathExpressionException ex) {
            LOG.error("Invalid XPath expression {}: {}", expr, ex.getMessage());
            return "";
        }
    }

    /**
     * Evaluates a compiled XPath expression as a string against the supplied element.
     *
     * @param e context element
     * @param xpr compiled XPath expression
     * @return string result, or the empty string if evaluation fails
     */
    public static String evalForString(Element e, XPathExpression xpr) {
        try {
            return (String) xpr.evaluate(e, XPathConstants.STRING);
        } catch (XPathExpressionException ex) {
            LOG.error("Invalid XPath expression {}: {}", xpr, ex.getMessage());
            return "";
        }
    }

    /**
     * Evaluates an XPath expression as a node set against the supplied element.
     *
     * @param e context element
     * @param expr XPath expression
     * @return node list result, or null if the expression is invalid
     */
    public static NodeList evalForNodes(Element e, String expr) {
        try {
            var xpr = compileXPath(expr);
            return evalForNodes(e, xpr);
        } catch (XPathExpressionException ex) {
            LOG.error("Invalid XPath expression {}: {}", expr, ex.getMessage());
            return null;
        }
    }

    /**
     * Evaluates a compiled XPath expression as a node set against the supplied element.
     *
     * @param e context element
     * @param xpr compiled XPath expression
     * @return node list result, or null if evaluation fails
     */
    public static NodeList evalForNodes(Element e, XPathExpression xpr) {
        try {
            return (NodeList) xpr.evaluate(e, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            LOG.error("Invalid XPath expression {}: {}", xpr, ex.getMessage());
            return null;
        }
    }

    /**
     * Returns the local-name portion of a QName string.
     *
     * <p>If the input contains no usable prefix separator, the input is returned unchanged.
     *
     * @param qn QName string
     * @return local name or original string
     */
    public static String qnToName(String qn) {
        var indx = qn.indexOf(':');
        if (indx < 1 || indx >= qn.length() - 1) return qn;
        return qn.substring(indx + 1);
    }

    /**
     * Returns the prefix portion of a QName string.
     *
     * <p>If the input contains no usable prefix separator, the empty string is returned.
     *
     * @param qn QName string
     * @return prefix or the empty string
     */
    public static String qnToPrefix(String qn) {
        var indx = qn.indexOf(':');
        if (indx < 1 || indx >= qn.length() - 1) return "";
        return qn.substring(0, indx);
    }

    /**
     * Creates a QName string from a prefix and local name.
     *
     * <p>If the prefix is null or blank, only the local name is returned.
     *
     * @param prefix namespace prefix
     * @param name local name
     * @return QName string or local name
     */
    public static String makeQN(String prefix, String name) {
        if (prefix == null || prefix.isBlank()) return name;
        return prefix + ":" + name;
    }

    /**
     * Constructs a component URI from a namespace URI and local name.
     *
     * <p>This method prefers slash URIs, respects hash URIs, and appends the
     * local name with a colon for URNs.
     *
     * @param nsU namespace URI
     * @param lname local name
     * @return component URI
     */
    public static String makeURI(String nsU, String lname) {
        if (nsU.startsWith("urn:")) return nsU + ":" + lname;
        if (nsU.endsWith("/")) return nsU + lname;
        if (nsU.endsWith("#")) return nsU + lname;
        return nsU + "/" + lname;
    }

    /**
     * Reads just enough of an XML document to obtain the namespace URI of its
     * document element.
     *
     * <p>Returns the empty string if the document element has no namespace or if
     * the file is not well-formed XML.
     *
     * @param path file path to an XML document
     * @return document element namespace URI, or the empty string
     * @throws IOException if the file cannot be read
     */
    public static String getXMLDocumentElementNamespace(String path) throws IOException {
        var xif = newSafeXmlInputFactory();

        try (var is = Files.newInputStream(new File(path).toPath())) {
            XMLEventReader er = xif.createXMLEventReader(is);
            while (er.hasNext()) {
                XMLEvent e = er.nextEvent();
                if (e.isStartElement()) {
                    StartElement se = e.asStartElement();
                    QName qn = se.getName();
                    var ns = qn.getNamespaceURI();
                    return ns == null ? "" : ns;
                }
            }
        } catch (XMLStreamException ex) {
            var loc = ex.getLocation();
            var line = loc == null ? -1 : loc.getLineNumber();
            LOG.warn("parse error at {} line {}: {}", path, line, ex.getMessage());
        }
        return "";
    }

    /**
     * Reads just enough of an XML document to obtain the namespace URI of its
     * document element.
     *
     * @param xmlF XML document file
     * @return document element namespace URI, or the empty string
     * @throws IOException if the file cannot be read
     */
    public static String getXMLDocumentElementNamespace(File xmlF) throws IOException {
        return getXMLDocumentElementNamespace(xmlF.getPath());
    }

    /**
     * Reads just enough of an XML document to obtain the {@code targetNamespace}
     * attribute from an XSD document element.
     *
     * <p>Returns the empty string if the document is not an XML Schema document,
     * if the document element has no {@code targetNamespace}, or if the file is
     * not well-formed XML.
     *
     * @param path file path to an XML Schema document
     * @return target namespace URI, or the empty string
     * @throws IOException if the file cannot be read
     */
    public static String getXSDTargetNamespace(String path) throws IOException {
        var xif = newSafeXmlInputFactory();

        try (var is = Files.newInputStream(new File(path).toPath())) {
            XMLEventReader er = xif.createXMLEventReader(is);
            while (er.hasNext()) {
                XMLEvent e = er.nextEvent();
                if (e.isStartElement()) {
                    var se = e.asStartElement();
                    var seqn = se.getName();
                    if (!W3C_XML_SCHEMA_NS_URI.equals(seqn.getNamespaceURI())
                        || !"schema".equals(seqn.getLocalPart())) {
                        return "";
                    }
                    var a = se.getAttributeByName(new QName("targetNamespace"));
                    return a == null ? "" : a.getValue();
                }
            }
        } catch (XMLStreamException ex) {
            var loc = ex.getLocation();
            var line = loc == null ? -1 : loc.getLineNumber();
            LOG.warn("parse error at {} line {}: {}", path, line, ex.getMessage());
        }
        return "";
    }

    /**
     * Reads just enough of an XML document to obtain the {@code targetNamespace}
     * attribute from an XSD document element.
     *
     * @param xsdF XML Schema document file
     * @return target namespace URI, or the empty string
     * @throws IOException if the file cannot be read
     */
    public static String getXSDTargetNamespace(File xsdF) throws IOException {
        return getXSDTargetNamespace(xsdF.getPath());
    }

    private static Document parse(File f) throws ParserConfigurationException, SAXException, IOException {
        var db = ParserBootstrap.docBuilder();
        return db.parse(f);
    }

    private static Document parse(InputSource src) throws ParserConfigurationException, SAXException, IOException {
        var db = ParserBootstrap.docBuilder();
        return db.parse(src);
    }

    private static XPathExpression compileXPath(String expr) throws XPathExpressionException {
        XPath xp = XPATH_FACTORY.newXPath();
        xp.setNamespaceContext(NS_CONTEXT);
        return xp.compile(expr);
    }

    private static URI parseURI(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return URI.create(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static XMLInputFactory newSafeXmlInputFactory() {
        var xif = XMLInputFactory.newFactory();
        try {
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        } catch (IllegalArgumentException ex) {
            LOG.debug("Could not set StAX property SUPPORT_DTD: {}", ex.getMessage());
        }
        try {
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        } catch (IllegalArgumentException ex) {
            LOG.debug("Could not set StAX property IS_SUPPORTING_EXTERNAL_ENTITIES: {}", ex.getMessage());
        }
        return xif;
    }
}
