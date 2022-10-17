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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.dom.DOMInputImpl;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

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
    
    XMLCatalogResolver (String[] catalogs) {
        del = new org.apache.xerces.util.XMLCatalogResolver();
        del.setCatalogList(catalogs);
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
    
    /**
     * Returns a mapping of all catalog resolutions performed
     * @return resolution map
     */
    public Map<String,String> mappings () {
        return resmap;
    }
    
    /**
     * Returns a list of all catalog files
     * @return 
     */
    public List<String> allCatalogs () {
        return null;
    }
    
    public List<String> catalogMsgs () { 
        return null;
    }
}
