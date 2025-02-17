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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.dom.DOMInputImpl;
import org.mitre.niem.utility.Resource;
import static org.mitre.niem.utility.URIfuncs.FileToCanonicalURI;
import static org.mitre.niem.utility.URIfuncs.URIStringToFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

/**
 * A class for an XML Catalog resolver for XML Schema assembly.
 * Useful when you want to ensure that only local resources are used.
 * Also provides some diagnostics about catalogs and resolutions.
 * 
 * Doesn't do anything with public or system IDs.  Those resolve to null.
 * 
 * The only thing it will resolve is a namespace URI, and it only resolves
 * those to a local resource (file:/path/). If the catalogs specify anything
 * else, it returns null.
 * 
 * You can ask for a list of all catalog files, including those added by
 * nextCatalog elements.  You can ask for a list of validation errors for 
 * each of those files.  This doesn't use a lazy evaluation, it follows all 
 * the nextCatalog elements, needed or not.
 * 
 * You can also ask for a map of all namespace URI resolutions performed so far.
 * 
 * Delegates to org.apache.xerces.util.XMLCatalogResolver.  (Doesn't extend,
 * because some methods there are final.)
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLCatalogResolver implements LSResourceResolver {
    
    static final Logger LOG = LogManager.getLogger(XMLCatalogResolver.class);    
    
    public static final String NO_MAP = "NO MAP";                   // object for URI with no resolution
    public static final String REMOTE_MAP = "REMOTE MAP";           // object for URI with nonlocal resolution
    private final HashMap<String,String> resmap = new HashMap<>();  // cached namespace URI resolutions
    private final org.apache.xerces.util.XMLCatalogResolver del;    // delegate resolver
    private final List<String> initCatalogs = new ArrayList<>();    // initial catalog files, as file URI strings
    private final Set<String> allCatalogs = new HashSet<>();        // all catalog files encountered
    private List<String> msgs = null;                               // all catalog parse errors            
    
    // No public default constructor
    protected XMLCatalogResolver () { del = null;}
    
    /**
     * Constructs a catalog resolver with the given list of entry files.
     * @param catalogs - list of file URI strings or file path strings
     */
    public XMLCatalogResolver (String[] catalogs) {  
        for (int i = 0; i < catalogs.length; i++) {
            URI u   = null;
            var arg = catalogs[i];
            if (arg.startsWith("file:")) {
                try { u = new URI(arg); } catch (URISyntaxException ex) {} // IGNORE
            }
            else {
                u = FileToCanonicalURI(new File(arg));
            }
            if (null == u) {
                File argF = null;
                try {
                    argF = new File(arg).getCanonicalFile();
                } catch (IOException ex) {} // IGNORE
            }
            if (null != u) initCatalogs.add(u.toString());
        }
        del = new org.apache.xerces.util.XMLCatalogResolver();        
        del.setCatalogList(initCatalogs.toArray(new String[0]));
    }
        
    /**
     * Returns the URI mapping in the catalog for the given URI reference.
     * Returns "NO MAP" if the reference can't be resolved.
     * Returns "REMOTE MAP" if the resolution isn't a local file.
     * @param uri - URI string of resource to resolve
     * @return resolution result URI string
     * @throws IOException - if catalogs can't be read
     */
    public final synchronized String resolveURI (String uri) throws IOException {
        if (null == uri) return null;
        var res = resmap.get(uri);        
        if (null != res) return res;
        res = del.resolveURI(uri);
        if (null == res) res = NO_MAP;
        else {
            URI u = null;
            try { u = new URI(res); } catch (URISyntaxException ex) { }
            if (null == u) res = NO_MAP;
            else if (null == u.getScheme() || !"file".equals(u.getScheme())) res = REMOTE_MAP;
            else if (null != u.getHost()) res = REMOTE_MAP;
        }
        resmap.put(uri, res);
        return res;
    }

    /**
     * Resolves a resource using only the catalog and the given namespace reference.
     * The other parameters are ignored.
     * @return LSInput object describing the new input local file.
     */
    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        String res = null;
        try { res = resolveURI(namespaceURI); } catch (IOException ex) {} // IGNORE
        if (null == res) return null;
        if (NO_MAP.equals(res)) return null;
        if (REMOTE_MAP.equals(res)) return null;
        return new DOMInputImpl(publicId, res, baseURI);       
    }
    
    /**
     * Forces the cache of catalog mappings to be cleared.
     */
    public void clear () {
        resmap.clear();
        del.clear();
    }
    
    /**
     * Returns a mapping of all catalog resolutions performed (URI to file://)
     * @return resolution map
     */
    public Map<String,String> allResolutions () {
        return resmap;
    }
    
    /**
     * Returns a set of all catalog files, including those added by nextCatalog
     * elements.
     * @return set of catalog file URI strings
     */
    public Set<String> allCatalogs () {
        validateCatalogs();
        return allCatalogs;
    }
    
    /**
     * Validates all catalog files, including those added by nextCatalog elements,
     * and returns a list of i/o and parsing errors encountered.
     * @return list of error message strings.
     */
    public List<String> allMessages () {
        validateCatalogs();
        return msgs;
    }

    private void validateCatalogs () {
        if (null != msgs) return;
        msgs = new ArrayList<>();
            
        DocumentBuilder db = null;
        Schema sch = null;
        try {
            var xpfact = XPathFactory.newInstance();
            var dbf    = DocumentBuilderFactory.newInstance();
            var schURI = Resource.getResourceURI("xsd/XMLCatalogSchema.xsd");
            var schF   = new File(schURI);
            var schFac = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sch = schFac.newSchema(schF);
            dbf.setSchema(sch);
            dbf.setNamespaceAware(true);
            // dbf.setValidating(true);  DON'T DO THIS, it then expects a DTD
            db  = dbf.newDocumentBuilder();
        } catch (SAXException ex) {            
            LOG.error("can't load schema for XML Catalog files: {}" + ex.getMessage());
            return;
        } catch (ParserConfigurationException ex) {
            LOG.error(ex.getMessage());
            return;
        }
        var cats = new Stack<String>();
        cats.addAll(initCatalogs);
        while (!cats.isEmpty()) {
            var catUstr = cats.pop();                   // file:/some/dir/foo.xml
            var catF    = URIStringToFile(catUstr);     // File object for /some/dir/foo.xml
            var catD    = catF.getParentFile();         // File object for /some/dir
            var catFN   = catF.getName();               // foo.xml
            if (allCatalogs.contains(catUstr)) continue;
  
            Document doc = null;
            NodeList nodes = null;       
            var s   = db.getSchema();
            var eh  = new SAXErrorHandler();
            db.reset();
            db.setErrorHandler(eh);
            try {
                doc = db.parse(catUstr);
            } catch (SAXException  | IOException ex) {
                msgs.add(String.format("[error] %s: %s", catFN, ex.getMessage()));
            }
            for (var m : eh.messages()) msgs.add(m);
            if (null == doc) continue;                  // i/o error
            if (!eh.messages().isEmpty()) continue;     // XSD validation failed
            allCatalogs.add(catUstr);
            
            nodes = doc.getElementsByTagNameNS("*", "nextCatalog");
            if (null == nodes || 0 == nodes.getLength()) continue;
            for (int i = 0; i < nodes.getLength(); i++) {
                File ncF = null;
                var el = (Element)nodes.item(i);
                var av = el.getAttribute("catalog");
                if (av.startsWith("file:/")) ncF = URIStringToFile(av);
                else ncF = new File(catD, av);
                try { ncF = ncF.getCanonicalFile(); } catch (Exception ex) {} // IGNORE
                var ncUstr = ncF.toURI().toString();
                cats.add(ncUstr);
//                if (ncF.canRead()) cats.add(ncURIs);
//                else addMessage(catURIs, String.format("<nextCatalog catalog=\"%s\">: can't read file", av));
            }
        }   
    }

}
