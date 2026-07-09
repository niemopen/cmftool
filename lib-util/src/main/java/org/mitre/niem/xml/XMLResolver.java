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
 * Doesn't do anything with public or system IDs. Those resolve to null.
 *
 * The only thing it will resolve is a namespace URI, and it only resolves
 * those to a local resource (file:/path/). If the catalogs specify anything
 * else, it returns null.
 *
 * You can ask for a list of all catalog files, including those added by
 * nextCatalog elements. You can ask for a list of validation errors for
 * each of those files. This doesn't use a lazy evaluation, it follows all
 * the nextCatalog elements, needed or not.
 *
 * You can also ask for a map of all namespace URI resolutions performed so far.
 *
 * <p>At present, catalog diagnostics are limited to resolver errors and the
 * initial catalog list supplied to the constructor.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLResolver implements LSResourceResolver {

    static final Logger LOG = LogManager.getLogger(XMLResolver.class);

    public static final String NO_MAP = "NO MAP";           // object for URI with no resolution
    public static final String REMOTE_MAP = "REMOTE MAP";   // object for URI with nonlocal resolution

    private final HashMap<String, String> resmap = new HashMap<>();   // cached namespace URI resolutions
    private final List<String> initCatalogs;                          // initial catalog files, as file URI strings
    private final Set<String> allCatalogs;                            // all catalog files encountered
    private final List<String> msgs = new ArrayList<>();              // resolver/catalog messages

    private final XMLResolverConfiguration config;
    private final org.xmlresolver.XMLResolver del;

    protected XMLResolver() {                      // no public default constructor
        initCatalogs = List.of();
        allCatalogs = new HashSet<>();
        config = new XMLResolverConfiguration();
        del = new org.xmlresolver.XMLResolver(config);
    }

    /**
     * Constructs a resolver from a list of catalog file URI strings.
     *
     * <p>The supplied list is copied. Catalogs are used to resolve namespace URIs
     * to local file URIs only.
     *
     * @param catalogs list of catalog file URI strings
     */
    public XMLResolver(List<String> catalogs) {
        initCatalogs = (catalogs == null) ? List.of() : List.copyOf(catalogs);
        allCatalogs = new HashSet<>(initCatalogs);
        config = new XMLResolverConfiguration(initCatalogs);
        del = new org.xmlresolver.XMLResolver(config);
    }

    /**
     * Resolves a schema resource request.
     *
     * <p>This resolver ignores public IDs and system IDs. Only the namespace URI
     * is considered, and only resolutions to local file URIs are accepted.
     *
     * <p>If the namespace URI cannot be resolved, or resolves to a non-local
     * resource, this method returns {@code null}.
     *
     * @param type the resource type
     * @param namespaceURI the namespace URI to resolve
     * @param publicId the public identifier, ignored
     * @param systemId the system identifier, ignored
     * @param baseURI the base URI for the request
     * @return an LSInput for the resolved local resource, or {@code null}
     */
    @Override
    public LSInput resolveResource(
        String type,
        String namespaceURI,
        String publicId,
        String systemId,
        String baseURI) {

        if (namespaceURI == null || namespaceURI.isBlank()) return null;

        var resU = resolveURI(namespaceURI);
        if (NO_MAP.equals(resU)) return null;
        if (REMOTE_MAP.equals(resU)) return null;

        var input = new DOMInputImpl();
        input.setPublicId(publicId);
        input.setSystemId(resU);
        input.setBaseURI(baseURI);
        return input;
    }

    /**
     * Resolves a namespace URI using the configured XML catalogs.
     *
     * <p>If the URI cannot be resolved, this returns {@link #NO_MAP}. If the URI
     * resolves to a non-local resource, this returns {@link #REMOTE_MAP}. Only
     * local {@code file:} URIs are accepted as successful resolutions.
     *
     * <p>Resolution results are cached and can later be retrieved with
     * {@link #allResolutions()}.
     *
     * @param u namespace URI string
     * @return resolved local file URI string, {@link #NO_MAP}, or {@link #REMOTE_MAP}
     */
    public synchronized String resolveURI(String u) {
        if (u == null || u.isBlank()) return NO_MAP;

        var cached = resmap.get(u);
        if (cached != null) return cached;

        String result = NO_MAP;
        try {
            var res = del.lookupUri(u);
            if (res != null && res.isResolved()) {
                var resURI = res.getURI();
                if (resURI == null) {
                    result = NO_MAP;
                } else if (!"file".equalsIgnoreCase(resURI.getScheme())) {
                    result = REMOTE_MAP;
                } else {
                    var host = resURI.getHost();
                    if (host != null && !host.isBlank() && !"localhost".equalsIgnoreCase(host)) {
                        result = REMOTE_MAP;
                    } else {
                        result = resURI.toString();
                    }
                }
            }
        } catch (RuntimeException ex) {
            var msg = String.format("Catalog resolution error for %s: %s", u, ex.getMessage());
            LOG.warn(msg);
            msgs.add(msg);
            result = NO_MAP;
        }

        resmap.put(u, result);
        return result;
    }

    /**
     * Returns a mapping of all catalog resolutions performed so far.
     *
     * <p>The returned map associates each namespace URI that has been resolved
     * with either a local file URI, {@link #NO_MAP}, or {@link #REMOTE_MAP}.
     *
     * @return immutable map of cached resolution results
     */
    public synchronized Map<String, String> allResolutions() {
        return Map.copyOf(resmap);
    }

    /**
     * Returns the set of all catalog files known to this resolver.
     *
     * <p>This currently includes the initial catalog files supplied to the
     * constructor.
     *
     * @return immutable set of catalog file URI strings
     */
    public synchronized Set<String> allCatalogs() {
        return Set.copyOf(allCatalogs);
    }

    /**
     * Returns a list of resolver and catalog-related messages collected so far.
     *
     * <p>At present this list contains resolver errors encountered during
     * resolution attempts.
     *
     * @return immutable list of message strings
     */
    public synchronized List<String> allMessages() {
        return List.copyOf(msgs);
    }
}
