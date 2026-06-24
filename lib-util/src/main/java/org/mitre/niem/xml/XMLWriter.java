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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/**
 * Writes readable XML directly from a DOM.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Writes its own XML declaration using UTF-8</li>
 *   <li>Writes the root element with namespace declarations first, then
 *       other attributes, each on a separate indented line</li>
 *   <li>Uses deterministic natural sorted ordering for namespace declarations
 *       and attributes, ignoring case</li>
 *   <li>Puts the default namespace declaration {@code xmlns} before prefixed
 *       namespace declarations when present</li>
 *   <li>Pretty-prints element content with 2-space indentation</li>
 * </ul>
 *
 * <p>This writer is intended for data-centric XML. If a DOM contains
 * significant mixed content, formatting may not match the original lexical form.
 */
public class XMLWriter {

    private static final Logger LOG = LogManager.getLogger(XMLWriter.class);
    private static final String NL = "\n";
    private static final String INDENT = "  ";

    private static final Comparator<String> XML_NAME_ORDER = (a, b) -> compareNaturalIgnoreCase(a, b);

    private static final Comparator<String> NS_DECL_ORDER = (a, b) -> {
        int ra = namespaceDeclarationRank(a);
        int rb = namespaceDeclarationRank(b);
        if (ra != rb) return Integer.compare(ra, rb);

        String pa = namespaceSortKey(a);
        String pb = namespaceSortKey(b);

        int c = compareNaturalIgnoreCase(pa, pb);
        if (c != 0) return c;

        return a.compareTo(b);
    };

    public XMLWriter() { }

    public void writeXML(Document dom, File outF) throws IOException {
        Objects.requireNonNull(outF, "outF must not be null");
        try (Writer w = Files.newBufferedWriter(outF.toPath(), StandardCharsets.UTF_8)) {
            writeXML(dom, w);
        }
    }

    /**
     * Write the Document to the output writer.
     *
     * @param dom DOM document
     * @param w destination writer
     * @throws IOException if writing fails
     */
    public void writeXML(Document dom, Writer w) throws IOException {
        Objects.requireNonNull(dom, "dom must not be null");
        Objects.requireNonNull(w, "writer must not be null");

        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        w.write(NL);

        NodeList kids = dom.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node child = kids.item(i);
            writeDocumentChild(child, w);
        }
    }

    public void writeXML(Element elem, File outF) throws IOException {
        Objects.requireNonNull(outF, "outF must not be null");
        try (Writer w = Files.newBufferedWriter(outF.toPath(), StandardCharsets.UTF_8)) {
            writeXML(elem, w);
        }
    }

    /**
     * Write an Element as a standalone XML document.
     *
     * <p>This method copies any in-scope namespace declarations from ancestor
     * elements onto the serialized root element so the output is self-contained.
     *
     * @param elem element to serialize
     * @param w destination writer
     * @throws IOException if writing fails
     */
    public void writeXML(Element elem, Writer w) throws IOException {
        Objects.requireNonNull(elem, "elem must not be null");
        Objects.requireNonNull(w, "writer must not be null");

        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        w.write(NL);

        writeStandaloneElement(elem, w, 0);
    }

    /**
     * Serializes a DOM node using this writer's formatting rules.
     * For a Document or Element, includes the XML declaration.
     * For other node types, returns a fragment.
     *
     * @param n DOM node
     * @return text representation
     */
    public static String nodeToText(Node n) {
        if (n == null) return "";

        try {
            StringWriter sw = new StringWriter();
            XMLWriter xw = new XMLWriter();

            if (n.getNodeType() == Node.DOCUMENT_NODE) {
                xw.writeXML((Document) n, sw);
            } else if (n.getNodeType() == Node.ELEMENT_NODE) {
                xw.writeXML((Element) n, sw);
            } else {
                xw.writeNode(n, sw, 0);
            }
            return sw.toString();
        } catch (IOException ex) {
            LOG.error("Error serializing DOM node", ex);
            return "";
        }
    }

    // Writes top-level children of a Document, preserving comments, PIs, doctype,
    // and the document element.
    protected void writeDocumentChild(Node n, Writer w) throws IOException {
        if (n == null) return;

        switch (n.getNodeType()) {
            case Node.DOCUMENT_TYPE_NODE:
                writeDocType((DocumentType) n, w);
                break;

            case Node.ELEMENT_NODE:
                writeElement((Element) n, w, 0, true);
                break;

            case Node.COMMENT_NODE:
                writeComment((Comment) n, w, 0);
                break;

            case Node.PROCESSING_INSTRUCTION_NODE:
                writeProcessingInstruction((ProcessingInstruction) n, w, 0);
                break;

            case Node.TEXT_NODE:
                if (!n.getNodeValue().isBlank()) {
                    writeIndentedText(n.getNodeValue(), w, 0);
                }
                break;

            default:
                LOG.debug("Ignoring unsupported document child node type {}", n.getNodeType());
                break;
        }
    }

    // Writes a non-document node with indentation appropriate to its nesting level.
    protected void writeNode(Node n, Writer w, int level) throws IOException {
        if (n == null) return;

        switch (n.getNodeType()) {
            case Node.ELEMENT_NODE:
                writeElement((Element) n, w, level, false);
                break;

            case Node.TEXT_NODE:
                writeIndentedText(n.getNodeValue(), w, level);
                break;

            case Node.CDATA_SECTION_NODE:
                writeCDATA((CDATASection) n, w, level);
                break;

            case Node.COMMENT_NODE:
                writeComment((Comment) n, w, level);
                break;

            case Node.PROCESSING_INSTRUCTION_NODE:
                writeProcessingInstruction((ProcessingInstruction) n, w, level);
                break;

            case Node.ENTITY_REFERENCE_NODE:
                indent(w, level);
                w.write("&");
                w.write(n.getNodeName());
                w.write(";");
                w.write(NL);
                break;

            default:
                LOG.debug("Ignoring unsupported node type {}", n.getNodeType());
                break;
        }
    }

    // Writes an element from a Document. Only the document root gets the special
    // multi-line namespace-first attribute formatting.
    protected void writeElement(Element elem, Writer w, int level, boolean isRoot) throws IOException {
        indent(w, level);
        w.write("<");
        w.write(elem.getTagName());

        TreeMap<String, String> nsDecls = new TreeMap<>(NS_DECL_ORDER);
        TreeMap<String, String> attrs = new TreeMap<>(XML_NAME_ORDER);
        splitAttributes(elem, nsDecls, attrs);

        if (isRoot) {
            writeRootAttributes(nsDecls, attrs, w, level);
        } else {
            writeInlineAttributes(nsDecls, attrs, w);
        }

        List<Node> children = significantChildren(elem);

        if (children.isEmpty()) {
            w.write("/>");
            w.write(NL);
            return;
        }

        if (isTextOnly(children)) {
            w.write(">");
            writeInlineChildren(children, w);
            w.write("</");
            w.write(elem.getTagName());
            w.write(">");
            w.write(NL);
            return;
        }

        w.write(">");
        w.write(NL);

        for (Node child : children) {
            writeNode(child, w, level + 1);
        }

        indent(w, level);
        w.write("</");
        w.write(elem.getTagName());
        w.write(">");
        w.write(NL);
    }

    // Writes an arbitrary Element as the root of a standalone XML document.
    // Namespace declarations inherited from ancestors are copied onto this root.
    protected void writeStandaloneElement(Element elem, Writer w, int level) throws IOException {
        indent(w, level);
        w.write("<");
        w.write(elem.getTagName());

        TreeMap<String, String> nsDecls = inScopeNamespaceDeclarations(elem);
        TreeMap<String, String> attrs = nonNamespaceAttributes(elem);

        writeRootAttributes(nsDecls, attrs, w, level);

        List<Node> children = significantChildren(elem);

        if (children.isEmpty()) {
            w.write("/>");
            w.write(NL);
            return;
        }

        if (isTextOnly(children)) {
            w.write(">");
            writeInlineChildren(children, w);
            w.write("</");
            w.write(elem.getTagName());
            w.write(">");
            w.write(NL);
            return;
        }

        w.write(">");
        w.write(NL);

        for (Node child : children) {
            writeNode(child, w, level + 1);
        }

        indent(w, level);
        w.write("</");
        w.write(elem.getTagName());
        w.write(">");
        w.write(NL);
    }

    // Writes root-element namespace declarations first, then ordinary attributes,
    // each on its own indented line.
    protected void writeRootAttributes(
            TreeMap<String, String> nsDecls,
            TreeMap<String, String> attrs,
            Writer w,
            int level) throws IOException {

        for (var me : nsDecls.entrySet()) {
            w.write(NL);
            indent(w, level + 1);
            writeAttribute(me.getKey(), me.getValue(), w);
        }

        for (var me : attrs.entrySet()) {
            w.write(NL);
            indent(w, level + 1);
            writeAttribute(me.getKey(), me.getValue(), w);
        }
    }

    // Writes namespace declarations and attributes inline on a single start tag.
    protected void writeInlineAttributes(
            TreeMap<String, String> nsDecls,
            TreeMap<String, String> attrs,
            Writer w) throws IOException {

        for (var me : nsDecls.entrySet()) {
            w.write(" ");
            writeAttribute(me.getKey(), me.getValue(), w);
        }

        for (var me : attrs.entrySet()) {
            w.write(" ");
            writeAttribute(me.getKey(), me.getValue(), w);
        }
    }

    // Writes a single attribute with XML-escaped value text.
    protected void writeAttribute(String name, String value, Writer w) throws IOException {
        w.write(name);
        w.write("=\"");
        w.write(escapeAttribute(value));
        w.write("\"");
    }

    // Writes a document type declaration if one is present in the DOM.
    protected void writeDocType(DocumentType dt, Writer w) throws IOException {
        w.write("<!DOCTYPE ");
        w.write(dt.getName());

        String publicId = dt.getPublicId();
        String systemId = dt.getSystemId();
        String subset = dt.getInternalSubset();

        if (publicId != null) {
            w.write(" PUBLIC \"");
            w.write(publicId);
            w.write("\"");
            if (systemId != null) {
                w.write(" \"");
                w.write(systemId);
                w.write("\"");
            }
        } else if (systemId != null) {
            w.write(" SYSTEM \"");
            w.write(systemId);
            w.write("\"");
        }

        if (subset != null && !subset.isBlank()) {
            w.write(" [");
            w.write(subset);
            w.write("]");
        }

        w.write(">");
        w.write(NL);
    }

    // Writes an XML comment on its own line.
    protected void writeComment(Comment comment, Writer w, int level) throws IOException {
        indent(w, level);
        w.write("<!--");
        w.write(comment.getData());
        w.write("-->");
        w.write(NL);
    }

    // Writes a processing instruction on its own line.
    protected void writeProcessingInstruction(ProcessingInstruction pi, Writer w, int level) throws IOException {
        indent(w, level);
        w.write("<?");
        w.write(pi.getTarget());
        if (pi.getData() != null && !pi.getData().isEmpty()) {
            w.write(" ");
            w.write(pi.getData());
        }
        w.write("?>");
        w.write(NL);
    }

    // Writes a CDATA section on its own line.
    protected void writeCDATA(CDATASection cdata, Writer w, int level) throws IOException {
        indent(w, level);
        w.write("<![CDATA[");
        w.write(cdata.getData());
        w.write("]]>");
        w.write(NL);
    }

    // Writes text content with indentation, skipping blank text nodes.
    protected void writeIndentedText(String text, Writer w, int level) throws IOException {
        if (text == null || text.isBlank()) return;
        indent(w, level);
        w.write(escapeText(text));
        w.write(NL);
    }

    // Returns child nodes that should be written. If the element has structured
    // children, blank formatting-only text nodes are removed.
    protected List<Node> significantChildren(Element elem) {
        List<Node> raw = new ArrayList<>();
        boolean hasStructuredChildren = false;

        NodeList kids = elem.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node child = kids.item(i);
            raw.add(child);

            short t = child.getNodeType();
            if (t == Node.ELEMENT_NODE
                    || t == Node.COMMENT_NODE
                    || t == Node.PROCESSING_INSTRUCTION_NODE
                    || t == Node.ENTITY_REFERENCE_NODE) {
                hasStructuredChildren = true;
            }
        }

        if (!hasStructuredChildren) {
            return raw;
        }

        List<Node> filtered = new ArrayList<>();
        for (Node child : raw) {
            if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().isBlank()) {
                continue;
            }
            filtered.add(child);
        }
        return filtered;
    }

    // Returns true if all children are text or CDATA so the element can be
    // written on a single logical line.
    protected boolean isTextOnly(List<Node> children) {
        if (children.isEmpty()) return false;
        for (Node child : children) {
            short t = child.getNodeType();
            if (t != Node.TEXT_NODE && t != Node.CDATA_SECTION_NODE) {
                return false;
            }
        }
        return true;
    }

    // Writes text-only child content inline between start and end tags.
    protected void writeInlineChildren(List<Node> children, Writer w) throws IOException {
        for (Node child : children) {
            switch (child.getNodeType()) {
                case Node.TEXT_NODE:
                    w.write(escapeText(child.getNodeValue()));
                    break;

                case Node.CDATA_SECTION_NODE:
                    w.write("<![CDATA[");
                    w.write(((CDATASection) child).getData());
                    w.write("]]>");
                    break;

                default:
                    break;
            }
        }
    }

    // Splits an element's own attributes into namespace declarations and
    // ordinary attributes.
    protected void splitAttributes(
            Element elem,
            TreeMap<String, String> nsDecls,
            TreeMap<String, String> attrs) {

        NamedNodeMap nnm = elem.getAttributes();
        for (int i = 0; i < nnm.getLength(); i++) {
            Attr attr = (Attr) nnm.item(i);
            String name = attr.getName();
            String value = attr.getValue();

            if (isNamespaceDeclaration(attr)) {
                nsDecls.put(name, value);
            } else {
                attrs.put(name, value);
            }
        }
    }

    // Collects all namespace declarations in scope for an element by walking
    // up the ancestor chain. The nearest declaration for a prefix wins.
    protected TreeMap<String, String> inScopeNamespaceDeclarations(Element elem) {
        TreeMap<String, String> nsDecls = new TreeMap<>(NS_DECL_ORDER);

        for (Node cur = elem; cur != null && cur.getNodeType() == Node.ELEMENT_NODE; cur = cur.getParentNode()) {
            NamedNodeMap nnm = cur.getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
                Attr attr = (Attr) nnm.item(i);
                if (isNamespaceDeclaration(attr)) {
                    nsDecls.putIfAbsent(attr.getName(), attr.getValue());
                }
            }
        }

        return nsDecls;
    }

    // Collects only non-namespace attributes declared directly on the element.
    protected TreeMap<String, String> nonNamespaceAttributes(Element elem) {
        TreeMap<String, String> attrs = new TreeMap<>(XML_NAME_ORDER);

        NamedNodeMap nnm = elem.getAttributes();
        for (int i = 0; i < nnm.getLength(); i++) {
            Attr attr = (Attr) nnm.item(i);
            if (!isNamespaceDeclaration(attr)) {
                attrs.put(attr.getName(), attr.getValue());
            }
        }

        return attrs;
    }

    // Returns true if an attribute is a namespace declaration. This handles
    // both parsed DOMs and programmatically constructed DOMs where "xmlns"
    // may have been created without a namespace URI.
    protected boolean isNamespaceDeclaration(Attr attr) {
        if (attr == null) return false;
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) return true;

        String name = attr.getName();
        return "xmlns".equals(name) || (name != null && name.startsWith("xmlns:"));
    }

    // Writes indentation using two spaces per nesting level.
    protected void indent(Writer w, int level) throws IOException {
        for (int i = 0; i < level; i++) {
            w.write(INDENT);
        }
    }

    // Escapes character data for element text content.
    protected String escapeText(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '\r':
                    sb.append("&#xD;");
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }

    // Escapes character data for attribute values.
    protected String escapeAttribute(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\n':
                    sb.append("&#xA;");
                    break;
                case '\r':
                    sb.append("&#xD;");
                    break;
                case '\t':
                    sb.append("&#x9;");
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }

    // Default namespace declaration sorts before all prefixed namespace declarations.
    protected static int namespaceDeclarationRank(String name) {
        if ("xmlns".equals(name)) return 0;
        if (name != null && name.startsWith("xmlns:")) return 1;
        return 2;
    }

    // Returns the sortable prefix part of a namespace declaration name.
    // "xmlns" sorts as the empty prefix; "xmlns:xs" sorts as "xs".
    protected static String namespaceSortKey(String name) {
        if ("xmlns".equals(name)) return "";
        if (name != null && name.startsWith("xmlns:")) return name.substring(6);
        return name;
    }

    // Compares strings using natural ordering, ignoring case. Numeric runs are
    // compared by numeric value, so "foo2" sorts before "foo12". Ties are broken
    // using the original string's case-sensitive order so distinct XML names do
    // not collide in a TreeMap.
    protected static int compareNaturalIgnoreCase(String a, String b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        int ia = 0;
        int ib = 0;
        int na = a.length();
        int nb = b.length();

        while (ia < na && ib < nb) {
            char ca = a.charAt(ia);
            char cb = b.charAt(ib);

            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int sa = ia;
                int sb = ib;

                while (ia < na && a.charAt(ia) == '0') ia++;
                while (ib < nb && b.charAt(ib) == '0') ib++;

                int za = ia - sa;
                int zb = ib - sb;

                int ea = ia;
                int eb = ib;

                while (ea < na && Character.isDigit(a.charAt(ea))) ea++;
                while (eb < nb && Character.isDigit(b.charAt(eb))) eb++;

                int lena = ea - ia;
                int lenb = eb - ib;

                if (lena != lenb) {
                    return (lena < lenb) ? -1 : 1;
                }

                for (int i = 0; i < lena; i++) {
                    char da = a.charAt(ia + i);
                    char db = b.charAt(ib + i);
                    if (da != db) {
                        return (da < db) ? -1 : 1;
                    }
                }

                if (lena == 0 && lenb == 0) {
                    if (za != zb) {
                        return (za < zb) ? -1 : 1;
                    }
                } else if (za != zb) {
                    return (za < zb) ? -1 : 1;
                }

                ia = ea;
                ib = eb;
                continue;
            }

            int fa = Character.toLowerCase(ca);
            int fb = Character.toLowerCase(cb);
            if (fa != fb) {
                return (fa < fb) ? -1 : 1;
            }

            ia++;
            ib++;
        }

        if (ia < na) return 1;
        if (ib < nb) return -1;

        return a.compareTo(b);
    }
}
