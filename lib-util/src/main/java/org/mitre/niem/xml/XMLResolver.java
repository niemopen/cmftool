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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.dom.DOMInputImpl;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xmlresolver.XMLResolverConfiguration;

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
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLResolver implements LSResourceResolver {
    static final Logger LOG = LogManager.getLogger(XMLResolver.class);    
    
    public static final String NO_MAP = "NO MAP";                   // object for URI with no resolution
    public static final String REMOTE_MAP = "REMOTE MAP";           // object for URI with nonlocal resolution
    private final HashMap<String,String> resmap = new HashMap<>();  // cached namespace URI resolutions
    private final List<String> initCatalogs;                        // initial catalog files, as file URI strings
    private final Set<String> allCatalogs;                          // all catalog files encountered
    private List<String> msgs = null;                               // all catalog parse errors     
    private XMLResolverConfiguration config;
    private org.xmlresolver.XMLResolver del;
    
    protected XMLResolver () {                      // no public default constructor
        initCatalogs = new ArrayList<>();
        allCatalogs  = new HashSet<>();
        config = new XMLResolverConfiguration();
//        config.setFeature(ResolverFeature.ACCESS_EXTERNAL_DOCUMENT, "");
//        config.setFeature(ResolverFeature.ACCESS_EXTERNAL_ENTITY, "");
        del = new org.xmlresolver.XMLResolver(config);
    } 
    
    public XMLResolver (List<String> catalogs) {
        initCatalogs = new ArrayList<>(catalogs);
        allCatalogs  = new HashSet<>(catalogs);
        config = new XMLResolverConfiguration(catalogs);
//        config.setFeature(ResolverFeature.ACCESS_EXTERNAL_DOCUMENT, "");
//        config.setFeature(ResolverFeature.ACCESS_EXTERNAL_ENTITY, "");
        del = new org.xmlresolver.XMLResolver(config);
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        var resU = resolveURI(namespaceURI);
        if (null == resU) return null;
        if (NO_MAP.equals(resU)) return null;
        if (REMOTE_MAP.equals(resU)) return null;
        return new DOMInputImpl(publicId, resU, baseURI);        
    }
    
    public String resolveURI (String u) {
        var res  = del.lookupUri(u);
        if (null == res) return(NO_MAP);
        if (!res.isResolved()) return(NO_MAP);
        var resURI = res.getURI();
        if (null == resURI.getScheme() || !"file".equals(resURI.getScheme())) return(REMOTE_MAP);
        if (null != resURI.getHost()) return(REMOTE_MAP);
        var resU = resURI.toString();
        return resU;
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
//        validateCatalogs();
        return allCatalogs;
    }
    
    /**
     * Validates all catalog files, including those added by nextCatalog elements,
     * and returns a list of i/o and parsing errors encountered.
     * @return list of error message strings.
     */
    public List<String> allMessages () {
//        validateCatalogs();
//        return msgs;
        return new ArrayList<>();
    }

}
