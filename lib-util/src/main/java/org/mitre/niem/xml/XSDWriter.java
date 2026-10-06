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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * Writes readable XML Schema documents directly from a DOM.
 *
 * <p>This writer applies XSD-specific attribute ordering rules:
 *
 * <ul>
 *   <li>xs:element: name, ref, type, minOccurs, maxOccurs, substitutionGroup, then others</li>
 *   <li>xs:import: namespace, schemaLocation, then others</li>
 *   <li>xs:complexType: name, type, then others</li>
 *   <li>xs:attribute: name, ref, type, use, then others</li>
 *   <li>xs:choice: minOccurs, maxOccurs, then others</li>
 *   <li>xs:schema: targetNamespace, then namespace declarations, with xmlns:xs and xmlns:xsi last</li>
 * </ul>
 *
 * <p>The writer also ensures the serialized root includes
 * {@code xmlns:xs="http://www.w3.org/2001/XMLSchema"}.
 *
 * <p>This class is designed for subclassing. Override {@link #attributeRank(Element, String)}
 * to customize attribute ordering for specific element types.
 */
public class XSDWriter {

    protected static final Logger LOG = LogManager.getLogger(XSDWriter.class);
    protected static final String XSD_NS = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    protected static final String NL = "\n";
    protected static final String INDENT = "  ";

    protected static final Comparator<NameValue> DEFAULT_NS_DECL_ORDER = (a, b) -> {
        int ra = namespaceDeclarationRank(a.name);
        int rb = namespaceDeclarationRank(b.name);
        if (ra != rb) return Integer.compare(ra, rb);

        String pa = namespaceSortKey(a.name);
        String pb = namespaceSortKey(b.name);

        int c = compareNaturalIgnoreCase(pa, pb);
        if (c != 0) return c;

        return a.name.compareTo(b.name);
    };

    protected static final Comparator<NameValue> SCHEMA_NS_DECL_ORDER = (a, b) -> {
        int dra = namespaceDeclarationRank(a.name);
        int drb = namespaceDeclarationRank(b.name);
        if (dra != drb) return Integer.compare(dra, drb);

        int ra = schemaNamespaceRank(a.name);
        int rb = schemaNamespaceRank(b.name);
        if (ra != rb) return Integer.compare(ra, rb);

        String pa = namespaceSortKey(a.name);
        String pb = namespaceSortKey(b.name);

        int c = compareNaturalIgnoreCase(pa, pb);
        if (c != 0) return c;

        return a.name.compareTo(b.name);
    };

    public XSDWriter() { }

    public void writeXML(Document dom, File outF) throws IOException {
        Objects.requireNonNull(outF, "outF must not be null");
        try (Writer w = Files.newBufferedWriter(outF.toPath(), StandardCharsets.UTF_8)) {
            writeXML(dom, w);
        }
    }

    public void writeXML(Document dom, Writer w) throws IOException {
        Objects.requireNonNull(dom, "dom must not be null");
        Objects.requireNonNull(w, "writer must not be null");

        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        w.write(NL);

        NodeList kids = dom.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            writeDocumentChild(kids.item(i), w);
        }
    }

    public void writeXML(Element elem, File outF) throws IOException {
        Objects.requireNonNull(outF, "outF must not be null");
        try (Writer w = Files.newBufferedWriter(outF.toPath(), StandardCharsets.UTF_8)) {
            writeXML(elem, w);
        }
    }

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
    public String nodeToText(Node n) {
        if (n == null) return "";

        try {
            StringWriter sw = new StringWriter();

            if (n.getNodeType() == Node.DOCUMENT_NODE) {
                writeXML((Document) n, sw);
            } else if (n.getNodeType() == Node.ELEMENT_NODE) {
                writeXML((Element) n, sw);
            } else {
                writeNode(n, sw, 0);
            }
            return sw.toString();
        } catch (IOException ex) {
            LOG.error("Error serializing DOM node", ex);
            return "";
        }
    }

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

    protected void writeElement(Element elem, Writer w, int level, boolean isRoot) throws IOException {
        writeElementInternal(elem, w, level, isRoot, false);
    }

    protected void writeStandaloneElement(Element elem, Writer w, int level) throws IOException {
        writeElementInternal(elem, w, level, true, true);
    }

    protected void writeElementInternal(
            Element elem,
            Writer w,
            int level,
            boolean isRoot,
            boolean copyInScopeNamespaces) throws IOException {

        indent(w, level);
        w.write("<");
        w.write(elem.getTagName());

        List<NameValue> nsDecls = copyInScopeNamespaces
                ? inScopeNamespaceDeclarationsList(elem)
                : ownNamespaceDeclarations(elem);

        List<NameValue> attrs = nonNamespaceAttributesList(elem);

        if (isRoot) {
            ensureXSNamespaceDeclaration(nsDecls);
        }

        nsDecls.sort(isSchemaElement(elem) ? SCHEMA_NS_DECL_ORDER : DEFAULT_NS_DECL_ORDER);
        attrs.sort(attributeOrderFor(elem));

        if (isSchemaElement(elem)) {
            if (isRoot) {
                writeSchemaRootAttributes(nsDecls, attrs, w, level);
            } else {
                writeSchemaInlineAttributes(nsDecls, attrs, w);
            }
        } else {
            if (isRoot) {
                writeRootAttributes(nsDecls, attrs, w, level);
            } else {
                writeInlineAttributes(nsDecls, attrs, w);
            }
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

    protected void writeRootAttributes(
            List<NameValue> nsDecls,
            List<NameValue> attrs,
            Writer w,
            int level) throws IOException {

        for (NameValue nv : nsDecls) {
            w.write(NL);
            indent(w, level + 1);
            writeAttribute(nv.name, nv.value, w);
        }

        for (NameValue nv : attrs) {
            w.write(NL);
            indent(w, level + 1);
            writeAttribute(nv.name, nv.value, w);
        }
    }

    protected void writeInlineAttributes(
            List<NameValue> nsDecls,
            List<NameValue> attrs,
            Writer w) throws IOException {

        for (NameValue nv : nsDecls) {
            w.write(" ");
            writeAttribute(nv.name, nv.value, w);
        }

        for (NameValue nv : attrs) {
            w.write(" ");
            writeAttribute(nv.name, nv.value, w);
        }
    }

    protected void writeSchemaRootAttributes(
            List<NameValue> nsDecls,
            List<NameValue> attrs,
            Writer w,
            int level) throws IOException {

        NameValue targetNamespace = null;
        List<NameValue> otherAttrs = new ArrayList<>();

        for (NameValue nv : attrs) {
            if (targetNamespace == null && "targetNamespace".equals(nv.name)) {
                targetNamespace = nv;
            } else {
                otherAttrs.add(nv);
            }
        }

        if (targetNamespace != null) {
            w.write(NL);
            indent(w, level + 1);
            writeAttribute(targetNamespace.name, targetNamespace.value, w);
        }

        for (NameValue nv : nsDecls) {
            w.write(NL);
            indent(w, level + 1);
            writeAttribute(nv.name, nv.value, w);
        }

        for (NameValue nv : otherAttrs) {
            w.write(NL);
            indent(w, level + 1);
            writeAttribute(nv.name, nv.value, w);
        }
    }

    protected void writeSchemaInlineAttributes(
            List<NameValue> nsDecls,
            List<NameValue> attrs,
            Writer w) throws IOException {

        NameValue targetNamespace = null;
        List<NameValue> otherAttrs = new ArrayList<>();

        for (NameValue nv : attrs) {
            if (targetNamespace == null && "targetNamespace".equals(nv.name)) {
                targetNamespace = nv;
            } else {
                otherAttrs.add(nv);
            }
        }

        if (targetNamespace != null) {
            w.write(" ");
            writeAttribute(targetNamespace.name, targetNamespace.value, w);
        }

        for (NameValue nv : nsDecls) {
            w.write(" ");
            writeAttribute(nv.name, nv.value, w);
        }

        for (NameValue nv : otherAttrs) {
            w.write(" ");
            writeAttribute(nv.name, nv.value, w);
        }
    }

    protected void writeAttribute(String name, String value, Writer w) throws IOException {
        w.write(name);
        w.write("=\"");
        w.write(escapeAttribute(value));
        w.write("\"");
    }

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

    protected void writeComment(Comment comment, Writer w, int level) throws IOException {
        indent(w, level);
        w.write("<!--");
        w.write(comment.getData());
        w.write("-->");
        w.write(NL);
    }

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

    protected void writeCDATA(CDATASection cdata, Writer w, int level) throws IOException {
        indent(w, level);
        w.write("<![CDATA[");
        w.write(cdata.getData());
        w.write("]]>");
        w.write(NL);
    }

    protected void writeIndentedText(String text, Writer w, int level) throws IOException {
        if (text == null || text.isBlank()) return;
        indent(w, level);
        w.write(escapeText(text));
        w.write(NL);
    }

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

    protected List<NameValue> ownNamespaceDeclarations(Element elem) {
        List<NameValue> out = new ArrayList<>();
        NamedNodeMap nnm = elem.getAttributes();

        for (int i = 0; i < nnm.getLength(); i++) {
            Attr attr = (Attr) nnm.item(i);
            if (isNamespaceDeclaration(attr)) {
                out.add(new NameValue(attr.getName(), attr.getValue()));
            }
        }
        return out;
    }

    protected List<NameValue> nonNamespaceAttributesList(Element elem) {
        List<NameValue> out = new ArrayList<>();
        NamedNodeMap nnm = elem.getAttributes();

        for (int i = 0; i < nnm.getLength(); i++) {
            Attr attr = (Attr) nnm.item(i);
            if (!isNamespaceDeclaration(attr)) {
                out.add(new NameValue(attr.getName(), attr.getValue()));
            }
        }
        return out;
    }

    protected List<NameValue> inScopeNamespaceDeclarationsList(Element elem) {
        Map<String, String> seen = new HashMap<>();

        for (Node cur = elem; cur != null && cur.getNodeType() == Node.ELEMENT_NODE; cur = cur.getParentNode()) {
            NamedNodeMap nnm = cur.getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
                Attr attr = (Attr) nnm.item(i);
                if (isNamespaceDeclaration(attr)) {
                    seen.putIfAbsent(attr.getName(), attr.getValue());
                }
            }
        }

        List<NameValue> out = new ArrayList<>();
        for (Map.Entry<String, String> me : seen.entrySet()) {
            out.add(new NameValue(me.getKey(), me.getValue()));
        }
        return out;
    }

    protected Comparator<NameValue> attributeOrderFor(Element elem) {
        return (a, b) -> {
            int ra = attributeRank(elem, a.name);
            int rb = attributeRank(elem, b.name);

            if (ra != rb) return Integer.compare(ra, rb);

            int c = compareNaturalIgnoreCase(a.name, b.name);
            if (c != 0) return c;

            return a.name.compareTo(b.name);
        };
    }

    /**
     * Returns the sort rank for an attribute on the specified element.
     *
     * <p>Subclasses can override this method to add element-specific ordering
     * rules while still delegating to {@code super.attributeRank(...)} for the
     * default XSD ordering.
     *
     * @param elem owning element
     * @param attrName attribute local or qualified name
     * @return sort rank; lower values sort first
     */
    protected int attributeRank(Element elem, String attrName) {
        if (elem == null || attrName == null) return Integer.MAX_VALUE;
        if (!isSchemaElement(elem)) return Integer.MAX_VALUE;

        String local = elementLocalName(elem);

        switch (local) {
            case "element":
                return rank(attrName,
                        "name",
                        "ref",
                        "type",
                        "minOccurs",
                        "maxOccurs",
                        "substitutionGroup");

            case "import":
                return rank(attrName,
                        "namespace",
                        "schemaLocation");

            case "complexType":
                return rank(attrName,
                        "name",
                        "type");

            case "attribute":
                return rank(attrName,
                        "name",
                        "ref",
                        "type",
                        "use");

            case "choice":
                return rank(attrName,
                        "minOccurs",
                        "maxOccurs");

            case "schema":
                return rank(attrName,
                        "targetNamespace");

            default:
                return Integer.MAX_VALUE;
        }
    }

    protected static int rank(String attrName, String... orderedNames) {
        for (int i = 0; i < orderedNames.length; i++) {
            if (orderedNames[i].equals(attrName)) return i;
        }
        return Integer.MAX_VALUE;
    }

    protected boolean isNamespaceDeclaration(Attr attr) {
        if (attr == null) return false;
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) return true;

        String name = attr.getName();
        return "xmlns".equals(name) || (name != null && name.startsWith("xmlns:"));
    }

    protected void ensureXSNamespaceDeclaration(List<NameValue> nsDecls) {
        if (!containsName(nsDecls, "xmlns:xs")) {
            nsDecls.add(new NameValue("xmlns:xs", XSD_NS));
        }
    }

    protected boolean containsName(List<NameValue> values, String name) {
        for (NameValue nv : values) {
            if (nv.name.equals(name)) return true;
        }
        return false;
    }

    protected boolean isSchemaElement(Element elem) {
        return elem != null && XSD_NS.equals(elem.getNamespaceURI());
    }

    protected String elementPrefix(Element elem) {
        String pfx = elem.getPrefix();
        if (pfx != null) return pfx;

        String tn = elem.getTagName();
        int c = tn.indexOf(':');
        return c >= 0 ? tn.substring(0, c) : "";
    }

    protected String elementLocalName(Element elem) {
        String ln = elem.getLocalName();
        if (ln != null) return ln;

        String tn = elem.getTagName();
        int c = tn.indexOf(':');
        return c >= 0 ? tn.substring(c + 1) : tn;
    }

    protected void indent(Writer w, int level) throws IOException {
        for (int i = 0; i < level; i++) {
            w.write(INDENT);
        }
    }

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

    protected static int namespaceDeclarationRank(String name) {
        if ("xmlns".equals(name)) return 0;
        if (name != null && name.startsWith("xmlns:")) return 1;
        return 2;
    }

    protected static String namespaceSortKey(String name) {
        if ("xmlns".equals(name)) return "";
        if (name != null && name.startsWith("xmlns:")) return name.substring(6);
        return name;
    }

    protected static int schemaNamespaceRank(String name) {
        if ("xmlns:xs".equals(name)) return 1;
        if ("xmlns:xsi".equals(name)) return 2;
        return 0;
    }

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

                if (lena != lenb) return (lena < lenb) ? -1 : 1;

                for (int i = 0; i < lena; i++) {
                    char da = a.charAt(ia + i);
                    char db = b.charAt(ib + i);
                    if (da != db) return (da < db) ? -1 : 1;
                }

                if (lena == 0 && lenb == 0) {
                    if (za != zb) return (za < zb) ? -1 : 1;
                } else if (za != zb) {
                    return (za < zb) ? -1 : 1;
                }

                ia = ea;
                ib = eb;
                continue;
            }

            int fa = Character.toLowerCase(ca);
            int fb = Character.toLowerCase(cb);
            if (fa != fb) return (fa < fb) ? -1 : 1;

            ia++;
            ib++;
        }

        if (ia < na) return 1;
        if (ib < nb) return -1;

        return a.compareTo(b);
    }

    protected static class NameValue {
        protected final String name;
        protected final String value;

        protected NameValue(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
