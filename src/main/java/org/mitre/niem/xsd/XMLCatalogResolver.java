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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.mitre.utility.Resource;
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
 * The only thing it will resolve is a namespace URI, and it only resolves
 * those to a local resource (file:/path/).
 * Delegates to org.apache.xerces.util.XMLCatalogResolver.
 * Doesn't extend because some methods there are final.
 * Refuses to resolve anything to a non-local resource.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLCatalogResolver implements LSResourceResolver {
    
    static final Logger LOG = LogManager.getLogger(XMLCatalogResolver.class);    
    
    private static final String noMap = "NO MAP";                       // object for URI with no resolution
    private static final String remoteMap = "REMOTE MAP";               // object for URI with nonlocal resolution
    private final HashMap<String,String> resmap = new HashMap<>();      // cached namespace URI resolutions
    private final org.apache.xerces.util.XMLCatalogResolver del;        // delegate resolver
    private final List<String> initCatalogs = new ArrayList<>();        // initial catalog files provided
    private final Set<String> allCatalogs = new HashSet<>();            // all catalog files encountered
    private Map<String, List<String>> msgs = null;                                   
    
    XMLCatalogResolver (String[] catalogs) {
        del = new org.apache.xerces.util.XMLCatalogResolver();
        del.setCatalogList(catalogs);
        initCatalogs.addAll(Arrays.asList(catalogs));
    }
    
    public final synchronized String tryResolveURI (String uri) throws IOException {
        return del.resolveURI(uri);
    }
        
    public final synchronized String resolveURI (String uri) {
        String res;
        if (null == uri) return null;
        res = resmap.get(uri);
        
        if (noMap == res || remoteMap == res) return null;
        if (null != res) return res;
        try { res = del.resolveURI(uri); } catch (IOException ex) { }
        if (null == res) res = noMap;
        else {
            URI u = null;
            try { u = new URI(res); } catch (URISyntaxException ex) { }
            if (null == u) res = noMap;
            else if (null == u.getScheme() || !"file".equals(u.getScheme())) res = remoteMap;
            else if (null != u.getHost()) res = remoteMap;
        }
        resmap.put(uri, res);
        if (noMap == res || remoteMap == res) return null;
        return res;
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        String resolvedId = resolveURI(namespaceURI);
        if (null == resolvedId) return null;
        return new DOMInputImpl(publicId, resolvedId, baseURI);       
    }
    
    // Validate the initial catalog files. Follow the nextCatalog elements and
    // validate them as well.
    public Map<String,List<String>> catalogMessages () {
        if (null != msgs) return msgs;
        msgs = new HashMap<>();
            
        DocumentBuilder db = null;
        Schema sch = null;
        try {
            var xpfact = XPathFactory.newInstance();
            var dbf    = DocumentBuilderFactory.newInstance();
            var schURI = Resource.getURI("xsd/XMLCatalogSchema.xsd");
            var schF   = new File(schURI);
            var schFac = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sch = schFac.newSchema(schF);
            dbf.setSchema(sch);
            dbf.setNamespaceAware(true);
            // dbf.setValidating(true);  DON'T DO THIS, it then expects a DTD (FG$@Wxxx)
            db  = dbf.newDocumentBuilder();

        } catch (SAXException ex) {            
            LOG.error("can't load schema for XML Catalog files: {}", ex.getMessage());
            return msgs;
        } catch (ParserConfigurationException ex) {
            LOG.error(ex.getMessage());
            return msgs;
        }
        var cats = new Stack<File>();
        for (var cat : initCatalogs) {
            try { 
                var catCF = new File(cat).getCanonicalFile();
                cats.add(catCF);
            }
            catch (IOException ex) { } // IGNORE
        }
        while (!cats.isEmpty()) {
            var catCF  = cats.pop();
            var catURI = catCF.toURI().toString();
            if (allCatalogs.contains(catURI)) continue;
            allCatalogs.add(catURI);
            
            Document doc = null;
            NodeList nodes = null;       
            var s   = db.getSchema();
            var eh    = new SAXErrorHandler();
            db.reset();
            db.setErrorHandler(eh);
            try {
                doc = db.parse(catCF);
            } catch (SAXException  | IOException ex) {
                addToMessageMap(msgs, catCF.toURI().toString(), "can't parse: " + ex.getMessage());
            }
            for (var m : eh.messages()) addToMessageMap(msgs, catCF.toURI().toString(), m);
            if (!eh.messages().isEmpty()) continue;
            
            nodes = doc.getElementsByTagNameNS("*", "nextCatalog");
            if (null == nodes || 0 == nodes.getLength()) continue;
            
            var catDir = catCF.getParentFile();
            for (int i = 0; i < nodes.getLength(); i++) {
                var el = (Element)nodes.item(i);
                var av = el.getAttribute("catalog");
                var ncat = new File(catDir, av);
                try { ncat = ncat.getCanonicalFile(); } catch (Exception ex) {}
                var fp = ncat.toString();
                var fu = ncat.toURI().toString();
                var rd = ncat.canRead();
                if (ncat.canRead()) cats.add(ncat);
                else addToMessageMap(msgs, catCF.toURI().toString(), "can't read nextCatalog " + av);
            }
        }   
        return msgs;
    }
    
    private void addToMessageMap (Map<String,List<String>>map, String key, String msg) {
        var mlist = map.get(key);
        if (null == mlist) {
            mlist = new ArrayList<>();
            map.put(key, mlist);
        }
        mlist.add(msg);
    }
    
    /**
     * Returns a mapping of all catalog resolutions performed (URI to file://)
     * @return resolution map
     */
    public Map<String,String> allResolutions () {
        return resmap;
    }
    
    /**
     * Returns a list of all catalog files
     * @return 
     */
    public Set<String> allCatalogs () {
        return allCatalogs;
    }
    
    public List<String> catalogMsgs () { 
        return null;
    }
}
