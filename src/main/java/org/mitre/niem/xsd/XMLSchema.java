/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2022 The MITRE Corporation.
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
package org.mitre.niem.xsd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
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
import static org.mitre.niem.NIEMConstants.XML_CATALOG_NS_URI;
import org.mitre.niem.cmf.NamespaceKind;
import static org.mitre.niem.cmf.NamespaceKind.NSK_EXTERNAL;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UNKNOWN;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMLocator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A class for assembling an XML schema from a schema document pile.
 * The schema pile is specified by a list of schema document pathnames,
 * XML catalog pathnames, and namespace URIs, in any order.
 * Schema assembly is guaranteed to use only local resources.
 * <p>
 * The class can provide the schema in the form of a Xerces XSModel object,
 * or a javax.xml.validation.Schema object.
 *<br>
 * The class can also provide information from the schema documents that is not
 * available through the XSModel API
 *<p>
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLSchema {
    static final Logger LOG = LogManager.getLogger(XMLSchema.class);
    private Object URIDecoder;
    
    /**
     * Returns the list of catalog file paths found in the constructor's arguments
     * and used to initialize the resolver.
     * @return list of catalogs
     */
    public List<String> catalogs ()          { return catalogs; }
    /**
     * Returns the list of file URIs for the initial schema documents assembled
     * into the XML schema.  The list includes the schema document file paths, 
     * schema document file URIs, and resolved namespaced URIs found in the
     * constructor's arguments.
     * @return the list of file URIs
     */
    public List<String> initialSchemaDocs () { return schemaDocs; }
    /**
     * Returns the list of namespace URIs found in the constructor's arguments.
     * @return list of namespace URIs
     */
    public List<String> initialNS ()         { return initialNS; }
    /**
     * Returns the catalog resolver constructed from the XML catalog files
     * @return resolver
     */
    public XMLCatalogResolver resolver ()    { return resolver; }
     
    // Creating an XMLSchema object (via genSchema) establishes the list of
    // initial catalog files, initial schema documents, and initial namespace URIs.
    // You supplied an argument list of filepaths and URIs.  Now you can see how 
    // those were divided into catalog and schema documents.

    private final ArrayList<String> catalogs   = new ArrayList<>();     // canonical file URIs for XML catalogs
    private final ArrayList<String> schemaDocs = new ArrayList<>();     // canonical file URIs for schema documents
    private final ArrayList<String> initialNS  = new ArrayList<>();     // list of initial namespace URIs
    private XMLCatalogResolver resolver = null;
    
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
     */
    public XMLSchema (String... args) throws XMLSchemaException, IOException {
        List<Integer> iNSindex = new ArrayList<>();
        
        // Go through the arguments, figure out what they are, validate them
        for (int i = 0; i < args.length; i++) {
            // Handle an argument in URI syntax.  
            // Anything other than a file: URI is a namespace URI.
            // A file: URI must not have a hostname component (local files only)
            String arg = args[i];
            String path = null;
            URI u = null;
            try { u = new URI(arg); } catch (URISyntaxException ex) { } // IGNORE
            if (null != u && null != u.getScheme()) {
                // File URIs must not have a hostname
                if ("file".equals(u.getScheme())) {
                    if (null == u.getHost()) path = u.getPath();
                    else throw new XMLSchemaException(String.format("hostname not allowed in file URI %s", u.toString()));
                } 
                // Any other URI is an initial namespace URI
                else {
                    initialNS.add(arg);                   // resolve this when we have the catalog
                    schemaDocs.add(null);                 // leave a space in the list of schema documents
                    iNSindex.add(schemaDocs.size() - 1);  // remember the index of the space
                }
            } else path = arg;        // not a URI
            
            // If we have a file: URI or a pathname, it's a catalog or schema document
            // Parse the first element to see which.
            if (null != path) {
                File f = new File(path);
                File cf = f.getCanonicalFile();
                String furi = cf.toURI().toString();
                String dkind = getXMLDocumentNamespace(path);
                if (XML_CATALOG_NS_URI.equals(dkind)) catalogs.add(furi);
                else if (W3C_XML_SCHEMA_NS_URI.equals(dkind))    schemaDocs.add(furi);
                else throw new XMLSchemaException(String.format("%s is not a schema document or XML catalog", path));
            }
        }
        // Create the resolver object.  OK if there are no catalog files
        String[] cats = catalogs.toArray(new String[0]);
        resolver = new XMLCatalogResolver(cats);

        // Convert each initial namespace URI to a schema document file URI
        // Check for a vast number of possible errors...
        for (int i = 0; i < initialNS.size(); i++) {
            String ns = initialNS.get(i);
            String sf = resolver.tryResolveURI(ns);
            if (null == sf) throw new XMLSchemaException("can't resolve " + ns);
            URI sfu = null;
            try { sfu = new URI(sf); } 
            catch (URISyntaxException ex) { 
                throw new XMLSchemaException(String.format("%s resolves to %s -- not a URI", ns, sf));
            }
            if (!"file".equals(sfu.getScheme()) || null != sfu.getHost()) {
                throw new XMLSchemaException(String.format("%s resolves to %s -- not a local URI", ns, sf));                
            }
            String dkind = getXMLDocumentNamespace(sfu.getPath());
            if (!W3C_XML_SCHEMA_NS_URI.equals(dkind)) {
                throw new XMLSchemaException(String.format("%s resolves to %s -- not a schema document", ns, sf));                    
            }
            String sftns = getXSDTargetNamespace(sfu.getPath());
            if (!sftns.equals(ns)) {
                throw new XMLSchemaException(String.format("%s resolves to %s -- wrong target namespace %s", ns, sf, sftns));                    
            }
            int index = iNSindex.get(i);
            schemaDocs.set(index, sfu.toString());
        }
    }

    // Reads an XML file to obtain the namespace URI of the XML document element.
    // Returns an empty string if the file isn't XML.
    private static String getXMLDocumentNamespace (String path) throws IOException {
        String ns = null;
        try {
            FileInputStream is = new FileInputStream(path);
            XMLEventReader er = ParserBootstrap.staxReader(is);
            while (null == ns && er.hasNext()) {
                XMLEvent e = er.nextEvent();
                if (e.isStartElement()) {
                    StartElement se = e.asStartElement();
                    QName qn = se.getName();
                    ns = qn.getNamespaceURI();
                }
            }
            is.close();
        } catch (XMLStreamException | ParserConfigurationException ex) {
            ns = "";
        }
        return ns;
    }
    
    // Reads an XML Schema file to obtain the target namespace URI.
    // Returns an empty string if the file isn't XSD or doesn't declare a
    // target namespace.
    private static String getXSDTargetNamespace (String path) throws IOException {
        String tns = null;
        try {
            FileInputStream is = new FileInputStream(path);
            XMLEventReader er = ParserBootstrap.staxReader(is);
            while (null == tns && er.hasNext()) {
                XMLEvent e = er.nextEvent();
                if (e.isStartElement()) {
                    StartElement se = e.asStartElement();
                    QName tnsa = new QName("targetNamespace");
                    Attribute a = se.getAttributeByName(tnsa);
                    tns = a.getValue();
                }
            }
            is.close();
        } catch (XMLStreamException | ParserConfigurationException ex) {
            tns = "";
        }
        return tns;
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
            LOG.error("can't create XSLoader: " + ex.getMessage());
            return null;
        }
        xsmsgs = new ArrayList<>();
        var handler = new XSModelHandler(xsmsgs);
//        XMLSchema.XSModelHandler handler = new XMLSchema.XSModelHandler();
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
        private List<String> msgs;
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
    
    // Returns an XSModel from an XSD input stream.  XML schema validation
    // messages are returned in the list.  Best of luck with your imports!
    public static XSModel xsmodelFromStream (InputStream is, List<String> msgs) {
        XSLoader loader;
        try {
            loader = ParserBootstrap.xsLoader(); // don't reuse these, they keep state
        } catch (ParserConfigurationException ex) {
            LOG.error("can't create XSLoader: " + ex.getMessage());
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
     */    
    public Schema javaxSchema () throws SAXException { 
        if (null != javaxSchema) return javaxSchema;
        
        LOG.debug("javaxSchema");
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
     */
    public List<String> javaXMsgs () throws SAXException { 
        if (null == javaxSchema) javaxSchema(); 
        return javaxMsgs; 
    }     
    
    ///// SchemaDocument stuff //////////////////////////////////////
    
    // Schema pile parsing info is created on demand and cached
    private Map<String,XMLSchemaDocument> sdocs = null;
    private String pileRoot;
       
    /**
     * Returns a map of namespace URIs to the SchemaDocument object having that
     * target namespace URI.  
     * @return map(nsuri) -> SchemaDocument
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws org.mitre.niem.xsd.XMLSchema.XMLSchemaException 
     */
    public Map<String,XMLSchemaDocument> schemaDocuments () throws SAXException, ParserConfigurationException, IOException, XMLSchemaException { 
        parseSchemaPile(); 
        return sdocs; 
    }
    
    /**
     * Returns the file: URI of the root directory of the schema document pile.
     * @return root directory URI
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws org.mitre.niem.xsd.XMLSchema.XMLSchemaException 
     */
    public String pileRoot () throws SAXException, ParserConfigurationException, IOException, XMLSchemaException {
        parseSchemaPile();
        return pileRoot;
    }
    
    private void parseSchemaPile () throws SAXException, ParserConfigurationException, IOException, XMLSchemaException {
        if (null != sdocs ) return;     // already done
        if (null == xs) xsmodel();      // generate the XSModel object if necessary
        if (null == xs) {               // can't create XSModel object!
            throw new XMLSchemaException("unable to create XSModel object");
        }    
        sdocs = new HashMap<>();
        
        // Iterate over XSNamespaceItems to collect all the schema document URIs
        HashSet<String> alldocs = new HashSet<>();
        XSNamespaceItemList nslist = xs.getNamespaceItems();   
        for (int i = 0; i < nslist.getLength(); i++) {
            XSNamespaceItem xnsi = nslist.item(i);
            StringList docl = xnsi.getDocumentLocations();
            for (int j = 0; j < docl.size(); j++) {
                String furi = xercesLocationURI(docl.item(j));
                alldocs.add(furi);
            }
        }
        // Now we know all the schema documents in the pile.  Add all the catalog
        // documents and find the greatest common prefix
        alldocs.addAll(catalogs);
        pileRoot = getCommonPrefix(alldocs.toArray(new String[0]));
        int prlen = pileRoot.lastIndexOf("/");
        if (prlen >= 0) pileRoot = pileRoot.substring(0, prlen + 1);
        LOG.debug("pileRoot: " + pileRoot);
        
        // Iterate over XSNamespaceItems to process schema documents 
        // One entry for each namespace URI that was a @targetNamespace in any document
        // One entry if there is a no-namespace document (which is not NIEM conforming)    
        for (int i = 0; i < nslist.getLength(); i++) {
            XSNamespaceItem xnsi = nslist.item(i);
            String nsuri = xnsi.getSchemaNamespace();
            StringList docl = xnsi.getDocumentLocations();
            if (null == nsuri || nsuri.isEmpty()) {
                for (int j = 0; j < docl.getLength(); j++) {
                    LOG.warn("schema document {} does not have a target namespace", docl.item(j));
                }
                continue;
            }
            if (sdocs.containsKey(nsuri)) continue;
            if (docl.size() < 1 && !W3C_XML_SCHEMA_NS_URI.equals(nsuri))
                throw new XMLSchemaException(String.format("Xerces weirdness: no schema document for namespace %s", nsuri)); 
            else {
                if (docl.size() > 1) LOG.warn("Multiple documents listed for namespace {} in XSModel?", nsuri);
                for (int j = 0; j < docl.getLength(); j++) {
                    String sdfuri = xercesLocationURI(docl.item(j));
                    String sdpath = sdfuri.substring(prlen+1);
                    LOG.debug("Processing {} for namespace {}", sdfuri, nsuri);
                    var sd = new XMLSchemaDocument(sdfuri, sdpath);
                    var targetNS = sd.targetNamespace();
                    if (null != targetNS && !nsuri.equals(targetNS))
                        throw new XMLSchemaException(
                                String.format("Xerces weirdness: schema document for namespace %s has targetNamespace %s", nsuri, targetNS));
                    sdocs.put(nsuri, sd);
                }
            }
        }
        // Now we can mark the external namespaces
        LOG.debug("parseSchemaPile: marking externals");
        LOG.debug(String.format("sdocs.size() = %d", sdocs.size()));
        for (var sd : sdocs.values()) {
            for (var extns : sd.externalImports()) {
                LOG.debug(String.format("%s imported external by %s", extns, sd.targetNamespace()));
                var esd  = sdocs.get(extns);
                int kind = NamespaceKind.uri2Kind(extns);
                if (NSK_UNKNOWN == kind) {
                    esd.setSchemaKind(NSK_EXTERNAL);
                    NamespaceKind.setKind(extns, kind);
                }
            }
        }
    }
    
    // Xerces reports schema document location as file URIs that sometimes
    // include percent-encoded backslash characters as separators.  Who knew?
    private String xercesLocationURI (String locuri) {
        var furi = locuri;
        try {
            furi = URLDecoder.decode(locuri, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOG.error("Can't decode UTF-8: " + ex.getMessage());
        }
        furi = furi.replace('\\', '/');
        return furi;
    }
    
    public class XMLSchemaException extends Exception {
        public XMLSchemaException (String msg) { super(msg); }
    }
    
}
