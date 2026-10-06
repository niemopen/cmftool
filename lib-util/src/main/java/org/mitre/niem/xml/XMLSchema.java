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
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import static org.apache.commons.lang3.StringUtils.getCommonPrefix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.dom.DOMInputImpl;
import org.apache.xerces.impl.xs.util.StringListImpl;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamespaceItem;
import org.apache.xerces.xs.XSNamespaceItemList;
import static org.mitre.niem.utility.URIfuncs.URIStringToFile;
import static org.mitre.niem.xml.XMLResolver.NO_MAP;
import static org.mitre.niem.xml.XMLResolver.REMOTE_MAP;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMLocator;
import org.xml.sax.SAXException;

/**
 * Represents an assembled XML Schema "pile" consisting of XML Schema documents,
 * XML Catalog documents, and namespace URIs supplied to the constructor.
 *
 * <p>Creating an XMLSchema object establishes the list of initial catalog files,
 * initial schema documents, and initial namespace URIs. You supply an argument
 * list of file paths and URIs; the constructor determines which are catalog
 * documents, schema documents, and namespace URIs to be resolved through the
 * catalogs.
 *
 * <p>A Xerces {@link XSModel}, a JAXP {@link Schema}, and a list of
 * {@link XMLSchemaDocument} objects can then be created on demand.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLSchema {

    static final String XML_CATALOG_NS_URI = "urn:oasis:names:tc:entity:xmlns:xml:catalog";
    static final Logger LOG = LogManager.getLogger(XMLSchema.class);

    // Creating an XMLSchema object establishes the list of initial catalog files,
    // initial schema documents, and initial namespace URIs. You supplied an
    // argument list of filepaths and URIs. Now you can see how those were divided
    // into catalog and schema documents.

    private final ArrayList<String> catalogs = new ArrayList<>();     // canonical file URIs for XML catalogs
    private final ArrayList<String> schemaDocs = new ArrayList<>();   // canonical file URIs for schema documents
    private final ArrayList<String> initialNS = new ArrayList<>();    // list of initial namespace URIs

    private List<String> resmsgL = List.of();
    private XMLResolver resolver = null;

    // XSModel and its assembly messages are created on demand and cached.
    private XSModel xs = null;
    private List<String> xsmsgs = null;

    // javax Schema and its messages are created on demand and cached.
    private Schema javaxSchema = null;
    private List<String> javaxMsgs = null;

    // Schema pile parsing info is created by the object constructor.
    private final List<XMLSchemaDocument> sdocL = new ArrayList<>();
    private final Map<String, XMLSchemaDocument> sdoc = new HashMap<>();              // namespace URI -> representative sdoc
    private final Map<String, List<XMLSchemaDocument>> ns2sdoc = new HashMap<>();     // namespace URI -> all sdocs
    private String pileRoot = "";

    protected XMLSchema() { }  // no public default constructor

    /**
     * Creates an XMLSchema object from an argument list of file paths and URIs,
     * in any order.
     *
     * <p>File paths and local {@code file:} URIs in the argument list are opened
     * and inspected to see if they are XML Schema documents or XML Catalog
     * documents. A file that cannot be opened, is not a local resource, or is
     * neither kind of document will raise an exception.
     *
     * <p>A catalog resolver is created from the catalog files, if any. Non-file
     * URIs in the argument list are treated as namespace URIs and resolved into
     * local schema documents. URIs which cannot be resolved, resolve to a local
     * resource which is not a schema document, or resolve to a remote resource
     * will raise an exception.
     *
     * <p>A Xerces XSModel object, a JAXP validation Schema object, and a list of
     * XMLSchemaDocument objects can then be created on demand, using the resolver
     * and the schema documents provided as arguments or created by resolving URI
     * arguments.
     *
     * @param args list of schema documents, catalogs, and namespace URIs
     * @throws XMLSchemaException if any argument cannot be classified, resolved,
     * or parsed into the schema pile
     */
    public XMLSchema(String... args) throws XMLSchemaException {
        List<Integer> nsIndexes = new ArrayList<>();

        for (var arg : args) {
            classifyArgument(arg, nsIndexes);
        }

        // Create the resolver object. OK if there are no catalog files.
        resolver = new XMLResolver(catalogs);
        resmsgL = resolver.allMessages();

        // Convert each initial namespace URI to a schema document file URI.
        resolveInitialNamespaces(nsIndexes);

        // We have the catalog resolver and all of the initial schema documents.
        // Now parse all documents in the pile and create XMLSchemaDocument objects.
        try {
            parseSchemaPile();
        } catch (SAXException ex) {
            throw new XMLSchemaException("Parsing error: " + ex.getMessage(), ex);
        } catch (ParserConfigurationException ex) {
            throw new XMLSchemaException("Internal parser error: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new XMLSchemaException(ex.getMessage(), ex);
        }
    }

    /**
     * Returns the list of catalog file paths found in the constructor's arguments
     * and used to initialize the resolver.
     *
     * @return immutable list of catalog file URI strings
     */
    public List<String> initialCatalogs() {
        return List.copyOf(catalogs);
    }

    /**
     * Returns the list of file URIs for the initial schema documents assembled
     * into the XML schema.
     *
     * <p>The list includes schema document file paths, schema document file URIs,
     * and resolved namespace URIs found in the constructor's arguments.
     *
     * @return immutable list of file URI strings
     */
    public List<String> initialSchemaDocs() {
        return List.copyOf(schemaDocs);
    }

    /**
     * Returns the list of namespace URI strings found in the constructor's arguments.
     *
     * @return immutable list of initial namespace URI strings
     */
    public List<String> initialNS() {
        return List.copyOf(initialNS);
    }

    /**
     * Returns the catalog resolver constructed from the XML catalog files.
     *
     * @return resolver
     */
    public XMLResolver resolver() {
        return resolver;
    }

    /**
     * Returns a list of messages generated while initializing and using the XML
     * catalog resolver.
     *
     * @return immutable list of resolver messages; empty if none
     */
    public List<String> resolverMessages() {
        return resmsgL == null ? List.of() : List.copyOf(resmsgL);
    }

    /**
     * Creates an XSModel object by assembling the schema documents in the pile,
     * using the schema documents and catalog documents provided as arguments to
     * the constructor.
     *
     * <p>Messages from creating the object are retained and available through
     * {@link #xsModelMsgs()}.
     *
     * @return XSModel object, or null on error
     */
    public synchronized XSModel xsmodel() {
        if (xs != null) return xs;    // cached result

        XSLoader loader;
        try {
            loader = ParserBootstrap.xsLoader(); // don't reuse these, they keep state
        } catch (ParserConfigurationException ex) {
            LOG.error("Can't create Xerces XSLoader: {}", ex.getMessage());
            xsmsgs = List.of("Can't create Xerces XSLoader: " + ex.getMessage());
            return null;
        }

        var msgs = new ArrayList<String>();
        var handler = new XSModelHandler(msgs);
        DOMConfiguration config = loader.getConfig();
        config.setParameter("validate", true);
        config.setParameter("error-handler", handler);
        if (resolver != null) config.setParameter("resource-resolver", resolver);

        StringList slist = new StringListImpl(
            schemaDocs.toArray(new String[0]),
            schemaDocs.size());

        xs = loader.loadURIList(slist);
        xsmsgs = List.copyOf(msgs);
        return xs;
    }

    /**
     * Returns a list of messages generated while creating the XSModel object
     * from the schema document pile.
     *
     * @return immutable list of messages; empty list if none
     */
    public List<String> xsModelMsgs() {
        if (xs == null) xsmodel();
        return xsmsgs == null ? List.of() : List.copyOf(xsmsgs);
    }

    /**
     * Use the schema's catalog files to resolve a resource URI string.
     *
     * <p>Returns null if the resource can't be resolved, or resolves to a
     * remote resource.
     *
     * @param uri resource URI string
     * @return resolved local resource URI string, or null
     */
    public String resolveURI(String uri) {
        if (resolver == null) return null;
        String res = resolver.resolveURI(uri);
        if (NO_MAP.equals(res) || REMOTE_MAP.equals(res)) return null;
        return res;
    }

    /**
     * Returns an XSModel from an XSD input stream.
     *
     * <p>XML schema validation messages are returned in the supplied list.
     * Relative imports and includes may not resolve correctly unless the source
     * has a meaningful system ID and resolver context.
     *
     * @param is XSD input stream
     * @param msgs list to receive validation and assembly messages
     * @return XSModel object, or null on error
     */
    public static XSModel xsmodelFromStream(InputStream is, List<String> msgs) {
        return xsmodelFromStream(is, null, null, msgs);
    }

    /**
     * Returns an XSModel from an XSD input stream, with optional system ID and resolver.
     *
     * <p>XML schema validation messages are returned in the supplied list.
     *
     * @param is XSD input stream
     * @param systemId system ID for the stream, or null
     * @param resolver resource resolver to use for imports/includes, or null
     * @param msgs list to receive validation and assembly messages
     * @return XSModel object, or null on error
     */
    public static XSModel xsmodelFromStream(
        InputStream is,
        String systemId,
        XMLResolver resolver,
        List<String> msgs) {

        XSLoader loader;
        try {
            loader = ParserBootstrap.xsLoader(); // don't reuse these, they keep state
        } catch (ParserConfigurationException ex) {
            LOG.error("Can't create Xerces XSLoader: {}", ex.getMessage());
            if (msgs != null) msgs.add("Can't create Xerces XSLoader: " + ex.getMessage());
            return null;
        }

        var outMsgs = (msgs == null) ? new ArrayList<String>() : msgs;
        var handler = new XSModelHandler(outMsgs);
        DOMConfiguration config = loader.getConfig();
        config.setParameter("validate", true);
        config.setParameter("error-handler", handler);
        if (resolver != null) config.setParameter("resource-resolver", resolver);

        DOMInputImpl d = new DOMInputImpl();
        d.setByteStream(is);
        d.setSystemId(systemId);
        d.setBaseURI(systemId);
        return loader.load(d);
    }

    /**
     * Creates a javax Schema object by assembling the schema documents in the pile,
     * using the schema documents and catalog documents provided as arguments to
     * the constructor.
     *
     * <p>Messages from creating the object are retained and available through
     * {@link #javaXMsgs()}.
     *
     * @return Schema object, or null on error
     * @throws SAXException if schema assembly fails
     */
    public synchronized Schema javaxSchema() throws SAXException {
        if (javaxSchema != null) return javaxSchema;

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        SAXErrorHandler h = new SAXErrorHandler();
        factory.setErrorHandler(h);
        if (resolver != null) factory.setResourceResolver(resolver);

        List<Source> slist = new ArrayList<>();
        for (String s : schemaDocs) {
            slist.add(new StreamSource(s));
        }

        javaxSchema = factory.newSchema(slist.toArray(new Source[0]));
        javaxMsgs = h.messages();
        return javaxSchema;
    }

    /**
     * Returns a list of messages generated while creating the javax Schema object
     * from the schema document pile.
     *
     * @return immutable list of messages; empty list if none
     * @throws SAXException if schema assembly fails
     */
    public List<String> javaXMsgs() throws SAXException {
        if (javaxSchema == null) javaxSchema();
        return javaxMsgs == null ? List.of() : List.copyOf(javaxMsgs);
    }

    /**
     * Validates the XML document in the input string against the XML schema
     * represented by this object.
     *
     * <p>Returns a list of validation messages. Returns an empty list if
     * validation is completely successful.
     *
     * @param doc a string containing an XML document
     * @return list of validation messages
     * @throws SAXException if schema setup fails
     */
    public List<String> validate(String doc) throws SAXException {
        var rdr = new StringReader(doc);
        var src = new StreamSource(rdr);
        return validate(src);
    }

    /**
     * Validates the XML document in the specified file against the XML schema
     * represented by this object.
     *
     * <p>Returns a list of validation messages. Returns an empty list if
     * validation is completely successful.
     *
     * @param f file containing an XML document
     * @return list of validation messages
     * @throws SAXException if schema setup fails
     */
    public List<String> validate(File f) throws SAXException {
        var src = new StreamSource(f);
        return validate(src);
    }

    /**
     * Validates the XML document in the specified StreamSource against the XML schema
     * represented by this object.
     *
     * <p>Returns a list of validation messages. Returns an empty list if
     * validation is completely successful.
     *
     * @param src StreamSource containing an XML document
     * @return list of validation messages
     * @throws SAXException if schema setup fails
     */
    public List<String> validate(StreamSource src) throws SAXException {
        var res = new ArrayList<String>();
        var sch = javaxSchema();
        var vldr = sch.newValidator();
        var hndlr = new SAXErrorHandler();
        vldr.setErrorHandler(hndlr);

        try {
            vldr.validate(src);
        } catch (SAXException ex) {
            if (hndlr.messages().isEmpty() && ex.getMessage() != null) {
                res.add(ex.getMessage());
            }
        } catch (IOException ex) {
            if (ex.getMessage() != null) res.add(ex.getMessage());
        }

        res.addAll(hndlr.messages());
        return res;
    }

    /**
     * Returns the list of schema document objects assembled into the pile.
     *
     * @return immutable list of schema document objects
     */
    public List<XMLSchemaDocument> schemaDocumentL() {
        return List.copyOf(sdocL);
    }

    /**
     * Returns the set of target namespace URIs from all documents in the pile.
     *
     * @return immutable set of namespace URI strings
     */
    public Set<String> schemaNamespaceUs() {
        return Set.copyOf(ns2sdoc.keySet());
    }

    /**
     * Returns a representative XMLSchemaDocument object for the schema document
     * with the specified target namespace.
     *
     * <p>If more than one schema document in the pile has the same target
     * namespace, this returns one representative document. Use
     * {@link #schemaDocuments(String)} to obtain all of them.
     *
     * @param nsuri target namespace URI
     * @return XMLSchemaDocument object, or null if no such document exists
     */
    public XMLSchemaDocument schemaDocument(String nsuri) {
        return sdoc.get(nsuri);
    }

    /**
     * Returns all XMLSchemaDocument objects in the pile having the specified
     * target namespace.
     *
     * @param nsuri target namespace URI
     * @return immutable list of matching schema documents; empty if none
     */
    public List<XMLSchemaDocument> schemaDocuments(String nsuri) {
        var docs = ns2sdoc.get(nsuri);
        return docs == null ? List.of() : List.copyOf(docs);
    }

    /**
     * Returns the file: URI of the root directory of the schema document pile.
     *
     * @return root directory URI string, or the empty string if none
     */
    public String pileRoot() {
        return pileRoot == null ? "" : pileRoot;
    }

    /**
     * Turns a file URI string into a file path relative to the pile root directory.
     *
     * <p>Returns an empty string if the URI is not in the pile root directory.
     *
     * @param docFileU file URI string
     * @return relative file path, or empty string
     */
    public String fileUtoPath(String docFileU) {
        if (docFileU == null || pileRoot == null || pileRoot.isBlank()) {
            return "";
        }
        try {
            var rootPath = new File(URI.create(pileRoot)).toPath().toAbsolutePath().normalize();
            var docPath = new File(URI.create(docFileU)).toPath().toAbsolutePath().normalize();
            if (!docPath.startsWith(rootPath)) {
                return "";
            }
            return rootPath.relativize(docPath).toString().replace('\\', '/');
        } catch (Exception ex) {
            return "";
        }
    }


    /**
     * Returns the file path for a schema document, relative to the schema document
     * pile root directory.
     *
     * @param sd XMLSchemaDocument object
     * @return relative file path, or empty string
     */
    public String docFilePath(XMLSchemaDocument sd) {
        if (sd == null || sd.docURI() == null) return "";
        return fileUtoPath(sd.docURI().toString());
    }

    private void classifyArgument(String arg, List<Integer> nsIndexes) throws XMLSchemaException {
        if (arg == null || arg.isBlank()) {
            throw new XMLSchemaException("Blank schema/catalog/namespace argument");
        }

        if (looksLikeLocalPath(arg)) {
            classifyLocalFile(new File(arg));
            return;
        }

        URI uri = tryParseUri(arg);
        if (uri == null || uri.getScheme() == null) {
            classifyLocalFile(new File(arg));
            return;
        }

        if ("file".equalsIgnoreCase(uri.getScheme())) {
            classifyLocalFile(fileFromFileUri(uri));
            return;
        }

        initialNS.add(arg);                   // resolve this when we have the catalog
        schemaDocs.add(null);                 // leave a space in the list of schema documents
        nsIndexes.add(schemaDocs.size() - 1); // remember the index of the space
    }

    private void classifyLocalFile(File f) throws XMLSchemaException {
        final File cf;
        try {
            cf = f.getCanonicalFile();
        } catch (IOException ex) {
            throw new XMLSchemaException(
                String.format("Can't canonicalize path %s: %s", f.getPath(), ex.getMessage()),
                ex);
        }

        final String furi = cf.toURI().toString();
        final String docnsU;
        try {
            docnsU = XMLDocument.getXMLDocumentElementNamespace(cf);
        } catch (IOException ex) {
            throw new XMLSchemaException(
                String.format("I/O error with %s: %s", cf.getPath(), ex.getMessage()),
                ex);
        }

        if (XML_CATALOG_NS_URI.equals(docnsU)) catalogs.add(furi);
        else if (W3C_XML_SCHEMA_NS_URI.equals(docnsU)) schemaDocs.add(furi);
        else throw new XMLSchemaException(
            String.format("%s is not a schema document or XML catalog", cf.getPath()));
    }

    private void resolveInitialNamespaces(List<Integer> nsIndexes) throws XMLSchemaException {
        for (int i = 0; i < initialNS.size(); i++) {
            String ns = initialNS.get(i);
            String sf = resolver.resolveURI(ns);

            if (REMOTE_MAP.equals(sf)) {
                throw new XMLSchemaException(
                    String.format("%s resolves to %s, which is not a local URI", ns, sf));
            }
            if (NO_MAP.equals(sf)) {
                throw new XMLSchemaException(String.format("Can't resolve %s", ns));
            }

            URI sfu;
            try {
                sfu = new URI(sf);
            } catch (URISyntaxException ex) {
                throw new XMLSchemaException(
                    String.format("%s resolves to %s, which is not valid URI syntax", ns, sf),
                    ex);
            }

            File sfFile = URIStringToFile(sfu.toString());
            if (sfFile == null) {
                throw new XMLSchemaException(
                    String.format("%s resolves to %s, which is not a local file URI", ns, sf));
            }

            try {
                var dkind = XMLDocument.getXMLDocumentElementNamespace(sfFile);
                var sftns = XMLDocument.getXSDTargetNamespace(sfFile);
                if (!W3C_XML_SCHEMA_NS_URI.equals(dkind)) {
                    throw new XMLSchemaException(
                        String.format("%s resolves to %s -- not a schema document", ns, sf));
                }
                if (!ns.equals(sftns)) {
                    throw new XMLSchemaException(
                        String.format("%s resolves to %s -- wrong target namespace %s", ns, sf, sftns));
                }
            } catch (IOException ex) {
                throw new XMLSchemaException(
                    String.format("I/O error on %s: %s", sfFile.getPath(), ex.getMessage()),
                    ex);
            }

            int index = nsIndexes.get(i);
            schemaDocs.set(index, sfu.toString());
        }
    }

    private void parseSchemaPile()
        throws SAXException, ParserConfigurationException, IOException, XMLSchemaException {

        if (xs == null) xsmodel();      // generate the XSModel object if necessary
        if (xs == null) {               // can't create XSModel object!
            throw new XMLSchemaException("unable to create XSModel object");
        }

        // Iterate over XSNamespaceItems to process schema documents.
        // One entry for each namespace URI that was a @targetNamespace in any document.
        // One entry if there is a no-namespace document.
        XSNamespaceItemList nslist = xs.getNamespaceItems();
        for (int i = 0; i < nslist.getLength(); i++) {
            XSNamespaceItem xnsi = nslist.item(i);
            String nsuri = xnsi.getSchemaNamespace();
            StringList docl = xnsi.getDocumentLocations();

            if (nsuri == null || nsuri.isEmpty()) {
                for (int j = 0; j < docl.getLength(); j++) {
                    LOG.warn("Schema document {} does not have a target namespace", docl.item(j));
                }
                continue;
            }

            if (docl.size() < 1 && !W3C_XML_SCHEMA_NS_URI.equals(nsuri)) {
                throw new XMLSchemaException(
                    String.format("Xerces weirdness: no schema document for namespace %s", nsuri));
            }

            for (int j = 0; j < docl.getLength(); j++) {
                var sdUstr = xercesLocationURI(docl.item(j));
                var sdF = URIStringToFile(sdUstr);
                if (sdF == null) {
                    throw new XMLSchemaException("Schema location is not a local file URI: " + sdUstr);
                }

                var sd = newSchemaDocument(sdF);    // NOT new XMLSchemaDocument; see below
                var targNS = sd.targetNamespace();
                if (targNS != null && !nsuri.equals(targNS)) {
                    throw new XMLSchemaException(
                        String.format(
                            "Xerces weirdness: schema document for namespace %s has targetNamespace %s",
                            nsuri, targNS));
                }

                sdocL.add(sd);
                sdoc.putIfAbsent(nsuri, sd);
                ns2sdoc.computeIfAbsent(nsuri, k -> new ArrayList<>()).add(sd);
            }

            if (docl.size() > 1) {
                LOG.warn("Multiple documents listed for namespace {} in XSModel", nsuri);
            }
        }

        // Each schema document object has a list of its imports, so we know all
        // the schema documents in the pile. Add all the catalog documents and
        // find the greatest common prefix.
        HashSet<String> alldocs = new HashSet<>();
        alldocs.addAll(catalogs);
        for (var sd : sdocL) {
            if (sd.docURI() != null) {
                alldocs.add(sd.docURI().toString());
            }
        }

        if (alldocs.isEmpty()) {
            pileRoot = "";
            return;
        }

        pileRoot = getCommonPrefix(alldocs.toArray(new String[0]));
        int prlen = pileRoot.lastIndexOf("/");
        if (prlen >= 0) pileRoot = pileRoot.substring(0, prlen + 1);
    }

    // Xerces reports schema document location as file URIs that sometimes
    // include percent-encoded backslash characters as separators.
    private String xercesLocationURI(String locuri) {
        if (locuri == null || locuri.isBlank()) return locuri;
        return locuri
            .replace("%5C", "/")
            .replace("%5c", "/")
            .replace("\\", "/");
    }

    // Override this in a derived XMLSchema class to create a derived
    // XMLSchemaDocument object.
    protected XMLSchemaDocument newSchemaDocument(File sdF)
        throws SAXException, IOException, ParserConfigurationException {
        return new XMLSchemaDocument(sdF);
    }

    private static boolean looksLikeLocalPath(String arg) {
        if (arg == null || arg.isBlank()) return false;
        if (arg.startsWith("/") || arg.startsWith("./") || arg.startsWith("../")) return true;
        return arg.matches("^[A-Za-z]:[\\\\/].*");
    }

    private static URI tryParseUri(String arg) {
        try {
            return new URI(arg);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private static File fileFromFileUri(URI uri) throws XMLSchemaException {
        String host = uri.getHost();
        if (host != null && !host.isBlank() && !"localhost".equalsIgnoreCase(host)) {
            throw new XMLSchemaException(
                String.format("A non-local hostname is not allowed in file URI %s", uri));
        }
        try {
            if ("localhost".equalsIgnoreCase(host)) {
                return new File(uri.getPath());
            }
            return new File(uri);
        } catch (IllegalArgumentException ex) {
            throw new XMLSchemaException(
                String.format("Invalid local file URI %s: %s", uri, ex.getMessage()),
                ex);
        }
    }

    private static class XSModelHandler implements DOMErrorHandler {
        private final List<String> msgs;

        XSModelHandler(List<String> m) {
            msgs = m;
        }

        @Override
        public boolean handleError(DOMError e) {
            short sevCode = e.getSeverity();
            String sevstr;
            switch (sevCode) {
                case DOMError.SEVERITY_FATAL_ERROR: sevstr = "[fatal]"; break;
                case DOMError.SEVERITY_ERROR:       sevstr = "[error]"; break;
                default:                            sevstr = "[warn] "; break;
            }

            DOMLocator loc = e.getLocation();
            String uri = (loc == null) ? null : loc.getUri();
            int line = (loc == null) ? -1 : loc.getLineNumber();
            int col = (loc == null) ? -1 : loc.getColumnNumber();

            String fn = "";
            if (uri != null) {
                int index = uri.lastIndexOf('/');
                fn = (index != -1) ? uri.substring(index + 1) + ":" : uri + ":";
            }

            msgs.add(String.format("%s %s %d:%d %s",
                sevstr,
                fn,
                line,
                col,
                e.getMessage()));
            return true;
        }
    }
}
