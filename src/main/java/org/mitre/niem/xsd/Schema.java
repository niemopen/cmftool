/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2021 The MITRE Corporation.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.xerces.impl.xs.util.StringListImpl;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import static org.mitre.niem.NIEMConstants.XML_CATALOG_NS_URI;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMLocator;

/**
 * A class to construct a Xerces XSModel object for an XML schema specified 
 * by a list of initial XML Schema documents (or namespace URIs), plus an
 * optional XML Catalog document.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Schema {
    private final List<String> catalogs    = new ArrayList<>(); 
    private final List<String> schemaDocs  = new ArrayList<>();
    private final List<String> initialNS   = new ArrayList<>();
    private final List<String> schemaURIs  = new ArrayList<>();
    private XMLCatalogResolver resolver       = null;
    private XSModel xsmodel                = null;
    
    // Use the static genSchema method to create a Schema object
    private Schema () { }
    
    // Creates a Schema object from a list of XML Catalog files, XML Schema documents,
    // and namespace URIs.  Figures out which is which.  Order is preserved.
    public static Schema newInstance (String ... args) throws IOException, FileNotFoundException, ParserConfigurationException, SchemaException {
        Schema s = new Schema();
        for (String arg : args) {
            // Argument in URI syntax (but not a file://) is a namespace URI
            // Remember those for later.
            URI u = null;
            try { u = new URI(arg); } 
            catch (URISyntaxException ex) { } // IGNORE
            if (null != u && null != u.getScheme() && !u.getScheme().startsWith("file:")) {
                s.initialNS.add(arg);
            }
            // Other arguments are either catalog or schema document files
            // Parse the first element to see which.
            else {
                String ns = getXMLDocumentNamespace(arg);
                if (XML_CATALOG_NS_URI.equals(ns)) { 
                    File f  = new File(arg);
                    File cf = f.getCanonicalFile();
                    String furi = cf.toURI().toString();
                    s.catalogs.add(furi);
                }
                else s.schemaDocs.add(arg);
            }
        }
        // Generate catalog resolver object from list of catalog files
        if (!s.catalogs.isEmpty()) {
//            StringBuilder catlist = new StringBuilder(s.catalogs.get(0));
//            for (int i = 1; i < s.catalogs.size(); i++) {
//                catlist.append(";"+s.catalogs.get(i));
//            }
//            s.resolver = CatalogManager.catalogResolver(
//                    CatalogFeatures.builder()
//                            .with(Feature.FILES, catlist.toString())
//                            .with(Feature.RESOLVE, "continue")
//                            .build()
//            );
            String[] cats = s.catalogs.toArray(new String[0]);
            s.resolver = new XMLCatalogResolver(cats);
        }
        // Go through the arguments again (in original order), skipping catalog files
        for (String arg : args) {
            // Resolve initial namespace argument into file URI string
            if (s.initialNS.contains(arg)) {
                if (null == s.resolver) 
                    throw new SchemaException(
                        String.format("Can't resolve initial namespace %s: No catalog files provided", arg));
                String furi = s.resolveURI(arg);
                if (null == furi)
                    throw new SchemaException(
                        String.format("Can't resolve initial namespace %s: No matching catalog entry", arg));
                s.schemaURIs.add(furi);
            }
            // Turn schema document path into file URI string
            else if (s.schemaDocs.contains(arg)) {
                File f  = new File(arg);
                File cf = f.getCanonicalFile();
                if (!cf.canRead()) 
                    throw new IOException(
                        String.format("Can't read schema document %s", cf.toString()));
                String furi = cf.toURI().toString();
                s.schemaURIs.add(furi);
            }
        }
        // All done.  Resolver and list of initial schema file URIs established.
        return s;
    }
    
    private static String getXMLDocumentNamespace (String fn) throws FileNotFoundException, ParserConfigurationException {
        String ns = null;
        try {
            FileInputStream is = new FileInputStream(fn);
            XMLEventReader er = ParserBootstrap.staxReader(is);
            while (null == ns && er.hasNext()) {
                XMLEvent e = er.nextEvent();
                if (e.isStartElement()) {
                    StartElement se = e.asStartElement();
                    QName qn = se.getName();
                    ns = qn.getNamespaceURI();
                }
            }
        } catch (XMLStreamException ex) {
            ns = "";
        }
        return ns;
    }

    public String resolveURI (String u) {
        String res = "";
        try {
            if (null == resolver) return null;
            res = resolver.resolveURI(u);
        } catch (IOException ex) {
            Logger.getLogger(Schema.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }
    
    public XSModel xsmodel () {
        if (null != xsmodel) return xsmodel;    // cached result
        XSLoader loader;
        try {
            loader = ParserBootstrap.xsLoader(); // don't reuse these, they keep state
        } catch (ParserConfigurationException ex) {
            return null;    // can't happen
        }
        Handler handler = new Handler();
        DOMConfiguration config = loader.getConfig();
        config.setParameter("validate", true);
        config.setParameter("error-handler",handler);
        if (null != resolver) config.setParameter("resource-resolver", resolver);
        StringList slist = new StringListImpl(
                schemaURIs.toArray(new String[0]),
                schemaURIs.size());
        xsmodel = loader.loadURIList(slist);
        
//        XSNamespaceItemList nsl = xsmodel.getNamespaceItems();
//        for (int i = 0; i < nsl.getLength(); i++) {
//            XSNamespaceItem nsi = nsl.item(i);
//            String ns = nsi.getSchemaNamespace();
//            System.out.println(String.format("NSI #%d: %s", i, ns));
//            
//            XSObjectList ans = nsi.getAnnotations();
//            System.out.println(String.format("%d annotations:", ans.getLength()));
//            for (int j = 0; j < ans.getLength(); j++) {
//                XSAnnotation a = (XSAnnotation)ans.item(j);
//                String as = a.getAnnotationString();
//                System.out.println(String.format(" #%d: '%s'", j, as));
//            }
//            
//            StringList docl = nsi.getDocumentLocations();
//            for (int j = 0; j < docl.getLength(); j++) {
//                String duri = docl.item(j);
//                System.out.println(String.format(" doc#%d: %s", j, duri));
//            }            
//         }
        return xsmodel;
    }
    
    private class Handler implements DOMErrorHandler {
        Handler () { super(); }
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
            System.out.println(String.format("%s %s %d:%d %s",
                    sevstr,
                    fn,
                    loc.getLineNumber(),
                    loc.getColumnNumber(),
                    e.getMessage()));
            return true;
        }
    }
    
}
