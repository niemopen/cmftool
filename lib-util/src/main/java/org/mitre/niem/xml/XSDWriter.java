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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Writes readable XML Schema documents directly from a DOM.
 *
 * <p>This writer preserves the pretty-printing behavior of {@link XMLWriter},
 * but applies XSD-specific attribute ordering rules:
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
 */
public class XSDWriter extends XMLWriter {

    private static final String XSD_NS = XMLConstants.W3C_XML_SCHEMA_NS_URI;

    private static final Comparator<NameValue> DEFAULT_NS_DECL_ORDER = (a, b) -> {
        int ra = namespaceDeclarationRank(a.name);
        int rb = namespaceDeclarationRank(b.name);
        if (ra != rb) return Integer.compare(ra, rb);

        String pa = namespaceSortKey(a.name);
        String pb = namespaceSortKey(b.name);

        int c = compareNaturalIgnoreCase(pa, pb);
        if (c != 0) return c;

        return a.name.compareTo(b.name);
    };

    private static final Comparator<NameValue> SCHEMA_NS_DECL_ORDER = (a, b) -> {
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

    public XSDWriter() {
        super();
    }

    public static String nodeToText(Node n) {
        if (n == null) return "";

        try {
            StringWriter sw = new StringWriter();
            XSDWriter xw = new XSDWriter();

            if (n.getNodeType() == Node.DOCUMENT_NODE) {
                xw.writeXML((Document) n, sw);
            } else if (n.getNodeType() == Node.ELEMENT_NODE) {
                xw.writeXML((Element) n, sw);
            } else {
                xw.writeNode(n, sw, 0);
            }
            return sw.toString();
        } catch (IOException ex) {
            return "";
        }
    }

    @Override
    protected void writeElement(Element elem, Writer w, int level, boolean isRoot) throws IOException {
        writeElementInternal(elem, w, level, isRoot, false);
    }

    @Override
    protected void writeStandaloneElement(Element elem, Writer w, int level) throws IOException {
        writeElementInternal(elem, w, level, true, true);
    }

    private void writeElementInternal(
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
            w.write("\n");
            return;
        }

        if (isTextOnly(children)) {
            w.write(">");
            writeInlineChildren(children, w);
            w.write("</");
            w.write(elem.getTagName());
            w.write(">");
            w.write("\n");
            return;
        }

        w.write(">");
        w.write("\n");

        for (Node child : children) {
            writeNode(child, w, level + 1);
        }

        indent(w, level);
        w.write("</");
        w.write(elem.getTagName());
        w.write(">");
        w.write("\n");
    }

    protected void writeRootAttributes(
            List<NameValue> nsDecls,
            List<NameValue> attrs,
            Writer w,
            int level) throws IOException {

        for (NameValue nv : nsDecls) {
            w.write("\n");
            indent(w, level + 1);
            writeAttribute(nv.name, nv.value, w);
        }

        for (NameValue nv : attrs) {
            w.write("\n");
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

    private void writeSchemaRootAttributes(
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
            w.write("\n");
            indent(w, level + 1);
            writeAttribute(targetNamespace.name, targetNamespace.value, w);
        }

        for (NameValue nv : nsDecls) {
            w.write("\n");
            indent(w, level + 1);
            writeAttribute(nv.name, nv.value, w);
        }

        for (NameValue nv : otherAttrs) {
            w.write("\n");
            indent(w, level + 1);
            writeAttribute(nv.name, nv.value, w);
        }
    }

    private void writeSchemaInlineAttributes(
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

    private List<NameValue> ownNamespaceDeclarations(Element elem) {
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

    private List<NameValue> nonNamespaceAttributesList(Element elem) {
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

    private List<NameValue> inScopeNamespaceDeclarationsList(Element elem) {
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

    protected int attributeRank(Element elem, String attrName) {
        if (!isSchemaElement(elem)) return Integer.MAX_VALUE;

        String local = schemaLocalName(elem);

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

    private Comparator<NameValue> attributeOrderFor(Element elem) {
        return (a, b) -> {
            int ra = attributeRank(elem, a.name);
            int rb = attributeRank(elem, b.name);

            if (ra != rb) return Integer.compare(ra, rb);

            int c = compareNaturalIgnoreCase(a.name, b.name);
            if (c != 0) return c;

            return a.name.compareTo(b.name);
        };
    }

    protected boolean isNamespaceDeclaration(Attr attr) {
        if (attr == null) return false;
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) return true;

        String name = attr.getName();
        return "xmlns".equals(name) || (name != null && name.startsWith("xmlns:"));
    }

    private void ensureXSNamespaceDeclaration(List<NameValue> nsDecls) {
        if (!containsName(nsDecls, "xmlns:xs")) {
            nsDecls.add(new NameValue("xmlns:xs", XSD_NS));
        }
    }

    private boolean containsName(List<NameValue> values, String name) {
        for (NameValue nv : values) {
            if (nv.name.equals(name)) return true;
        }
        return false;
    }

    protected boolean isSchemaElement(Element elem) {
        return XSD_NS.equals(elem.getNamespaceURI());
    }

    protected String schemaLocalName(Element elem) {
        String ln = elem.getLocalName();
        if (ln != null) return ln;

        String tn = elem.getTagName();
        int c = tn.indexOf(':');
        return c >= 0 ? tn.substring(c + 1) : tn;
    }

    protected static int namespaceDeclarationRank(String name) {
        if ("xmlns".equals(name)) return 0;
        if (name != null && name.startsWith("xmlns:")) return 1;
        return 2;
    }

    protected static int schemaNamespaceRank(String name) {
        if ("xmlns:xs".equals(name)) return 1;
        if ("xmlns:xsi".equals(name)) return 2;
        return 0;
    }

    protected static final class NameValue {
        protected final String name;
        protected final String value;

        protected NameValue(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }
}
