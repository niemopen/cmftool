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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
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
import javax.xml.transform.sax.SAXSource;
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
import static org.mitre.niem.xml.XMLResolver.*;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMLocator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLSchema {
    static final String XML_CATALOG_NS_URI = "urn:oasis:names:tc:entity:xmlns:xml:catalog";    
    static final Logger LOG = LogManager.getLogger(XMLSchema.class);

   /**
     * Returns the list of catalog file paths found in the constructor's arguments
     * and used to initialize the resolver.
     * @return list of catalogs
     */
    public List<String> initialCatalogs ()          { return catalogs; }
    
    /**
     * Returns the list of file URIs for the initial schema documents assembled
     * into the XML schema.  The list includes the schema document file paths, 
     * schema document file URIs, and resolved namespaced URIs found in the
     * constructor's arguments.
     * @return the list of file URIs
     */
    public List<String> initialSchemaDocs () { return schemaDocs; }
    
    /**
     * Returns the list of namespace URI strings found in the constructor's arguments.
     * @return list of namespace URI strings
     */
    public List<String> initialNS ()         { return initialNS; }
    
    /**
     * Returns the catalog resolver constructed from the XML catalog files
     * @return resolver
     */
    public XMLResolver resolver ()          { return resolver; }
     
    // Creating an XMLSchema object (via genSchema) establishes the list of
    // initial catalog files, initial schema documents, and initial namespace URIs.
    // You supplied an argument list of filepaths and URIs.  Now you can see how 
    // those were divided into catalog and schema documents.

    private final ArrayList<String> catalogs   = new ArrayList<>();     // canonical file URIs for XML catalogs
    private final ArrayList<String> schemaDocs = new ArrayList<>();     // canonical file URIs for schema documents
    private final ArrayList<String> initialNS  = new ArrayList<>();     // list of initial namespace URIs
    private List<String> resmsgL               = null;
    private XMLResolver resolver               = null;
    
    protected XMLSchema () { }  // no public default constructor
    
    /**
     * Creates an XMLSchema object from an argument list of file paths and URIs,
     * in any order. 
     * <p>
     * File paths and local file: URIs in the argument list are opened and inspected to 
     * see if they are XML Schema documents or XML Catalog documents.  A file that
     * cannot be opened, is not a local resource, or is neither kind of document
     * will raise an exception.
     * <p>
     * A catalog resolver is created from the catalog files, if any. Non-file: URIs 
     * in the argument list are resolved into a local schema document.  URIs which 
     * cannot be resolved, resolve to a local resource which is not a schema document,
     * or resolve to a remote resource will raise an exception.
     * <p>
     * A Xerces XSModel object or Javax validation schema object can be created
     * on demand, using the resolver and the schema documents provided as arguments
     * or created by resolving URI arguments.  A list of XMLSchemaDocument objects,
     * containing schema information not available through the Xerces XML Schema API,
     * can also be created on demand.
     *
     * @param args List of schema documents, catalogs, namespace URIs
     * @throws XMLSchemaException
     */
    public XMLSchema (String... args) throws XMLSchemaException {
        List<Integer> iNSindex = new ArrayList<>();
        
        // Go through the arguments, figure out what they are, validate them
        for (var arg : args) {
            // Handle an argument in URI syntax.  
            // Anything other than a file: URI is a namespace URI.
            // A file: URI must not have a hostname component (local files only)
            String path = null;
            URI u = null;
            try { u = new URI(arg); } catch (URISyntaxException ex) { } // IGNORE
            if (null != u && null != u.getScheme()) {
                // File URIs must not have a hostname. Turn it into a path string.
                if ("file".equals(u.getScheme())) {
                    if (null == u.getHost()) path = u.getPath();
                    else throw new XMLSchemaException(String.format("A hostname is not allowed in file URI %s", u.toString()));
                } 
                // Any other URI is an initial namespace URI
                else {
                    initialNS.add(arg);                   // resolve this when we have the catalog
                    schemaDocs.add(null);                 // leave a space in the list of schema documents
                    iNSindex.add(schemaDocs.size() - 1);  // remember the index of the space
                }
            } 
            else path = arg;        // not a URI
            // If we have a string with a file: URI or a pathname, it's a catalog 
            // or a schema document. Parse the first element to see which.
            if (null != path) {
                String furi   = null;        // file URI string constructed from filename path
                String docnsU = null;        // namespace of document element in file
                try {
                    var f  = new File(path);
                    var cf = f.getCanonicalFile();
                    furi = cf.toURI().toString();
                } catch (IOException ex) {
                    throw new XMLSchemaException(String.format("Can't canonicalize path %s: %s", path, ex.getMessage()));
                }
                try {
                    docnsU = XMLDocument.getXMLDocumentElementNamespace(path);
                } catch (IOException ex) {
                    throw new XMLSchemaException(String.format("I/O error with %s: %s", path, ex.getMessage()));
                }
                if (XML_CATALOG_NS_URI.equals(docnsU)) catalogs.add(furi);
                else if (W3C_XML_SCHEMA_NS_URI.equals(docnsU)) schemaDocs.add(furi);
                else throw new XMLSchemaException(String.format("%s is not a schema document or XML catalog", path));
            }
        }
        // Create the resolver object.  OK if there are no catalog files
        resolver = new XMLResolver(catalogs);
        resmsgL  = resolver.allMessages();

        // Convert each initial namespace URI to a schema document file URI
        // Check for a vast number of possible errors...
        for (int i = 0; i < initialNS.size(); i++) {
            String ns = initialNS.get(i);
            String sf = resolver.resolveURI(ns);

            if (REMOTE_MAP.equals(sf))
                throw new XMLSchemaException(String.format("%s resolves to %s, which is not a local URI", ns, sf)); 
            if (NO_MAP.equals(sf)) 
                throw new XMLSchemaException(String.format("Can't resolve %s", ns));
            
            // Catalog resolution worked, now check syntax of the result
            URI sfu = null;
            try { sfu = new URI(sf); } 
            catch (URISyntaxException ex) { 
                throw new XMLSchemaException(String.format("%s resolves to %s, which is not valid URI syntax", ns, sf));
            }
            // It's a valid local file URI, now see if file is XSD
            try {
                var dkind = XMLDocument.getXMLDocumentElementNamespace(sfu.getPath());
                var sftns = XMLDocument.getXSDTargetNamespace(sfu.getPath());
                if (!W3C_XML_SCHEMA_NS_URI.equals(dkind))
                    throw new XMLSchemaException(String.format("%s resolves to %s -- not a schema document", ns, sf));                    
                if (!sftns.equals(ns)) 
                    throw new XMLSchemaException(String.format("%s resolves to %s -- wrong target namespace %s", ns, sf, sftns));                    
            } catch (IOException ex) {
                throw new XMLSchemaException(String.format("I/O error on %s: %s", sfu.getPath(), ex.getMessage()));
            }
            // It's a schema document, so fill the hole we left in the list of file URIs.
            int index = iNSindex.get(i);
            schemaDocs.set(index, sfu.toString());
        }
        // We have the catalog resolver and all of the initial schema documents.
        // Now parse all documents in the pile and create XMLSchemaDocument objects.
        try {
            parseSchemaPile();
        } catch (SAXException ex) {
            throw new XMLSchemaException("Parsing error: " + ex.getMessage());
        } catch (ParserConfigurationException ex) {
            throw new XMLSchemaException("Internal parser error: " + ex.getMessage());
        } catch (IOException ex) {
            throw new XMLSchemaException(ex.getMessage());
        }
    }  

    ///// XSModel stuff ////////////////////////////////////////////////////
     
    // XSModel and its assembly messages are created on demand and cached
    private XSModel xs = null;
    private List<String> xsmsgs = null;
    
    /**
     * Creates an XSModel object by assembling the schema documents in the pile,
     * using the schema documents and catalog documents provided as arguments to 
     * the constructor,  Messages from creating the object are retained and available.
     * @return XSModel object, or null on error.
     */
    public XSModel xsmodel () {
        if (null != xs) return xs;    // cached result
        XSLoader loader;
        try {
            loader = ParserBootstrap.xsLoader(); // don't reuse these, they keep state
        } catch (ParserConfigurationException ex) {
            LOG.error("Can't create Xerces XSLoader: {}", ex.getMessage());
            return null;
        }
        xsmsgs = new ArrayList<>();
        var handler = new XSModelHandler(xsmsgs);
        DOMConfiguration config = loader.getConfig();
        config.setParameter("validate", true);
        config.setParameter("error-handler", handler);
        if (null != resolver) config.setParameter("resource-resolver", resolver);
        StringList slist = new StringListImpl(
                schemaDocs.toArray(new String[0]),
                schemaDocs.size());

        xs = loader.loadURIList(slist);
        return xs;        
    }
    
    /**
     * Returns a list of messages generated while creating the XSModel object
     * from the schema document pile.
     * @return list of messages; empty list if none
     */
    public List<String> xsModelMsgs () { if (null == xs) xsmodel(); return xsmsgs; }      

    private static class XSModelHandler implements DOMErrorHandler {
        private final List<String> msgs;
        XSModelHandler (List<String>m) { super(); msgs = m;}
        @Override
        public boolean handleError (DOMError e) {
            short sevCode = e.getSeverity();
            String sevstr;
            switch (sevCode) {
                case DOMError.SEVERITY_FATAL_ERROR: sevstr = "[fatal]"; break;
                case DOMError.SEVERITY_ERROR:       sevstr = "[error]"; break;
                default:                            sevstr = "[warn] "; break;
            }
            DOMLocator loc = e.getLocation();
            String uri = loc.getUri();
            String fn  = "";
            if (uri != null) {
                int index = uri.lastIndexOf('/');
                if (index != -1) {
                    fn = uri.substring(index + 1)+":";
                }
            }
            msgs.add(String.format("%s %s %d:%d %s",
                    sevstr,
                    fn,
                    loc.getLineNumber(),
                    loc.getColumnNumber(),
                    e.getMessage()));
            return true;
        }
    }
    
    /**
     * Use the schema's catalog files to resolve a resource URI string.
     * Returns null if the resource can't be resolved, or resolves to a
     * remote resource.
     * @param uri resource URI string
     * @return resolved resource URI string
     */
    public String resolveURI (String uri) {
        String res = resolver.resolveURI(uri);
        if ("NO MAP".equals(res)) return null;
        if ("REMOTE RESOURCE".equals(res)) return null;
        return res;
    }
    
    // Returns an XSModel from an XSD input stream.  XML schema validation
    // messages are returned in the list.  Best of luck with your imports!
    public static XSModel xsmodelFromStream (InputStream is, List<String> msgs) {
        XSLoader loader;
        try {
            loader = ParserBootstrap.xsLoader(); // don't reuse these, they keep state
        } catch (ParserConfigurationException ex) {
            LOG.error("Can't create Xerces XSLoader: " + ex.getMessage());
            return null;
        }
        var handler = new XSModelHandler(msgs);
        DOMConfiguration config = loader.getConfig();
        config.setParameter("validate", true);
        config.setParameter("error-handler", handler);  
        DOMInputImpl d = new DOMInputImpl();
        d.setByteStream(is);
        return loader.load(d);
    }

    ///// JAVAX schema stuff ////////////////////////////////////////
    
    private Schema javaxSchema = null;
    private List<String> javaxMsgs = null;
    
    /**
     * Creates a javax Schema object by assembling the schema documents in the pile,
     * using the schema documents and catalog documents provided as arguments to 
     * the constructor,  Messages from creating the object are retained and available.
     * @return Schema object, or null on error.
     * @throws org.xml.sax.SAXException
     */    
    public Schema javaxSchema () throws SAXException { 
        if (null != javaxSchema) return javaxSchema;
        
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);             
        SAXErrorHandler  h = new SAXErrorHandler();   
        factory.setErrorHandler(h);
        factory.setResourceResolver(resolver);
        List<Source> slist = new ArrayList<>();
        for (String s : schemaDocs) {
            var is = new InputSource(s);
            slist.add(new SAXSource(is));
//            slist.add(new StreamSource(new File(s)));
        }
        javaxSchema = (Schema)factory.newSchema(slist.toArray(new Source[0]));
        javaxMsgs = h.messages();
        return javaxSchema;
    }
    
    /**
     * Returns a list of messages generated while creating the XSModel object
     * from the schema document pile.
     * @return list of messages; empty list if none
     * @throws org.xml.sax.SAXException
     */
    public List<String> javaXMsgs () throws SAXException { 
        if (null == javaxSchema) javaxSchema(); 
        return javaxMsgs; 
    }     
    
    /**
     * Validates the XML document in the input string against the XML schema
     * represented by this object.  Returns a list of validation messages.
     * Returns an empty list if validation is completely successful.
     * @param doc -- A string containing an XML document
     * @return The list of validation messages
     * @throws SAXException 
     */
    public List<String> validate (String doc) throws SAXException {
        var rdr = new StringReader(doc);
        var src = new StreamSource(rdr);
        return validate(src);
    }
    
    /**
     * Validates the XML document in the specified file against the XML schema
     * represented by this object.  Returns a list of validation messages.
     * Returns an empty list if validation is completely successful.
     * @param f -- File containing an XML document
     * @return The list of validation messages
     * @throws SAXException 
     */
    public List<String> validate (File f) throws SAXException {
        var src = new StreamSource(f);
        return validate(src);
    }
    
    /**
     * Validates the XML document in the input StreamSource against the XML schema
     * represented by this object.  Returns a list of validation messages.
     * Returns an empty list if validation is completely successful.
     * @param src -- StreamSource containing an XML document
     * @return The list of validation messages
     * @throws SAXException 
     */
    public List<String> validate (StreamSource src) throws SAXException {
        var res   = new ArrayList<String>();
        var sch   = javaxSchema();
        var vldr  = sch.newValidator();
        var hndlr = new SAXErrorHandler();
        vldr.setErrorHandler(hndlr);
        try {
            vldr.validate(src);
            res.addAll(hndlr.messages());
        } catch (IOException ex) {
            res.add(ex.getMessage());
        }
        return res;        
    }

    ///// XMLSchemaDocument stuff ///////////////////////////////////////////////
    
    // Schema pile parsing info is created by the object constructor.
    
    private final List<XMLSchemaDocument> sdocL = new ArrayList<>();    // list of schema document objects
    private final Map<String,XMLSchemaDocument> sdoc = new HashMap<>(); // namespace URI -> sdoc object
    private String pileRoot;                                            // common prefix of all document paths

    public List<XMLSchemaDocument> schemaDocumentL () {
        return sdocL;
    }
    
    /**
     * Returns the set of target namespace URIs from all documents in the pile.
     * @return set of namespace URIs
     */
    public Set<String> schemaNamespaceUs () {
        return sdoc.keySet();
    }
    
    /**
     * Returns the XMLSchemaDocument object for the schema document with the
     * specified target namespace.  Returns null if no such document in the pile.
     * @param nsuri - target namespace URI
     * @return XMLSchemaDocument object (or null)
     */
    public XMLSchemaDocument schemaDocument (String nsuri) {
        return sdoc.get(nsuri);
    }
    
  /**
     * Returns the file: URI of the root directory of the schema document pile.
     * @return root directory URI string
     */
    public String pileRoot () {
        return pileRoot;
    }
    
    /**
     * Turns a file URI string into a file path relative to the pile root directory.
     * Returns an empty string if the URI is not in the pile root directory.
     * @param docFileU
     * @return relative file path
     */
    public String fileUtoPath (String docFileU) {
        if (docFileU.startsWith(pileRoot))
            return docFileU.substring(pileRoot.length());
        return "";
    }
    
    /**
     * Returns the file path for a schema document, relative to the schema document
     * pile root directory.
     * @param sd - XMLSchemaDocument object
     * @return - relative file path
     */
    public String docFilePath (XMLSchemaDocument sd) {
        var sdUs = sd.docURI().toString();
        return fileUtoPath(sdUs);
    }

    private void parseSchemaPile () throws SAXException, ParserConfigurationException, IOException, XMLSchemaException {
        if (null == xs) xsmodel();      // generate the XSModel object if necessary
        if (null == xs) {               // can't create XSModel object!
            throw new XMLSchemaException("unable to create XSModel object");
        }    
        
        // Iterate over XSNamespaceItems to process schema documents 
        // One entry for each namespace URI that was a @targetNamespace in any document
        // One entry if there is a no-namespace document (which is not NIEM conforming)    
        XSNamespaceItemList nslist = xs.getNamespaceItems();  
        for (int i = 0; i < nslist.getLength(); i++) {
            XSNamespaceItem xnsi = nslist.item(i);
            String nsuri = xnsi.getSchemaNamespace();
            StringList docl = xnsi.getDocumentLocations();
            if (null == nsuri || nsuri.isEmpty()) {
                for (int j = 0; j < docl.getLength(); j++) {
                    LOG.warn("Schema document {} does not have a target namespace", docl.item(j));
                }
                continue;
            }
            if (sdoc.containsKey(nsuri)) continue;
            if (docl.size() < 1 && !W3C_XML_SCHEMA_NS_URI.equals(nsuri))
                throw new XMLSchemaException(String.format("Xerces weirdness: no schema document for namespace %s", nsuri)); 
            else {
                if (docl.size() > 1) LOG.warn("Multiple documents listed for namespace {} in XSModel?", nsuri);
                for (int j = 0; j < docl.getLength(); j++) {
                    var sdUstr = xercesLocationURI(docl.item(j));
                    var sdF    = URIStringToFile(sdUstr);
                    var sd     = newSchemaDocument(sdF);    // NOT new XMLSchemaDocument; see below
                    var targNS = sd.targetNamespace();
                    if (null != targNS && !nsuri.equals(targNS))
                        throw new XMLSchemaException(
                                String.format("Xerces weirdness: schema document for namespace %s has targetNamespace %s", nsuri, targNS));
                    sdoc.put(nsuri, sd);
                    sdocL.add(sd);
                }
            }
        }
        // Each schema document object has a list of all its imports, so we know 
        // all the schema documents in the pile.  Add all the catalog documents 
        // and find the greatest common prefix
        HashSet<String> alldocs = new HashSet<>();
        alldocs.addAll(catalogs);
        for (var sd : sdoc.values()) {
            alldocs.add(sd.docURI().toString());
//            for (var irec : sd.importRecs()) {
//                alldocs.add(irec.imported());
//            }
        }
        pileRoot = getCommonPrefix(alldocs.toArray(new String[0]));
        int prlen = pileRoot.lastIndexOf("/");
        if (prlen >= 0) pileRoot = pileRoot.substring(0, prlen + 1);
    }
        
    // Xerces reports schema document location as file URIs that sometimes
    // include percent-encoded backslash characters as separators.  Who knew?
    private String xercesLocationURI (String locuri) {
        var furi = locuri;
        try {
            furi = URLDecoder.decode(locuri, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOG.error("URLDecoder can't decode UTF-8 charset??: " + ex.getMessage());
        }
        furi = furi.replace('\\', '/');
        return furi;
    }
    
    // Override this in a derived XMLSchema class to create a derived XMLSchemaDocument object.
    protected XMLSchemaDocument newSchemaDocument (File sdF) throws SAXException, IOException, ParserConfigurationException {
        return new XMLSchemaDocument(sdF);
    }

}
