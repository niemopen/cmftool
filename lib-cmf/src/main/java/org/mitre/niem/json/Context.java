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
package org.mitre.niem.json;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToName;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToPrefix;

/**
 * Utility methods for creating and writing a JSON-LD {@code @context}
 * derived from a NIEM model.
 * <p>
 * The generated context includes namespace prefix declarations for all
 * namespaces referenced by the selected model components and term
 * definitions for mapped and ordered properties.
 * </p>
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public final class Context {

    private Context() { }

    /**
     * Creates a JSON-LD context for all components in the supplied model,
     * using no QName mappings.
     *
     * @param m the source model
     * @return the generated context object
     * @throws NullPointerException if {@code m} is {@code null}
     * @throws CMFException if the context cannot be constructed
     */
    public static JsonObject create(Model m) throws CMFException {
        return create(m, null, null, false);
    }

    /**
     * Creates a JSON-LD context for all components in the supplied model,
     * using the provided QName mapping.
     *
     * @param m the source model
     * @param map QName mapping to apply, or {@code null} for no mappings
     * @return the generated context object
     * @throws NullPointerException if {@code m} is {@code null}
     * @throws CMFException if the context cannot be constructed
     */
    public static JsonObject create(Model m, Mapping map) throws CMFException {
        return create(m, map, null, false);
    }

    /**
     * Creates a JSON-LD context for the components required by the supplied
     * message root property, using the provided QName mapping.
     *
     * @param m the source model
     * @param map QName mapping to apply, or {@code null} for no mappings
     * @param msg the message root property
     * @return the generated context object
     * @throws NullPointerException if {@code m} or {@code msg} is {@code null}
     * @throws CMFException if the context cannot be constructed
     */
    public static JsonObject create(
        Model m,
        Mapping map,
        ObjectProperty msg) throws CMFException {

        Objects.requireNonNull(msg, "msg must not be null");
        return create(m, map, Set.of(msg), false);
    }

    /**
     * Creates a JSON-LD context for the components required by the supplied
     * message root properties, using the provided QName mapping.
     *
     * @param m the source model
     * @param map QName mapping to apply, or {@code null} for no mappings
     * @param msgS message root properties, or {@code null} to include all
     *        model components
     * @return the generated context object
     * @throws NullPointerException if {@code m} is {@code null}, or if
     *         {@code msgS} contains {@code null}
     * @throws CMFException if the context cannot be constructed
     */
    public static JsonObject create(
        Model m,
        Mapping map,
        Set<ObjectProperty> msgS) throws CMFException {

        return create(m, map, msgS, false);
    }

    /**
     * Creates a JSON-LD context for the supplied model and selected message
     * roots.
     * <p>
     * The generated context includes:
     * </p>
     * <ul>
     *   <li>namespace prefix declarations for all referenced namespaces</li>
     *   <li>term definitions for mapped properties</li>
     *   <li>{@code @container: @list} definitions for ordered properties</li>
     * </ul>
     * <p>
     * If {@code msgS} is {@code null}, all model components are included.
     * Otherwise, only components required for the supplied message root
     * properties are included.
     * </p>
     * <p>
     * If {@code noPrefix} is {@code true}, mapped property local names are
     * used as terms instead of mapped QNames. In that mode, context creation
     * fails if two properties map to the same local name or if a local name
     * conflicts with a namespace prefix.
     * </p>
     *
     * @param m the source model
     * @param map QName mapping to apply, or {@code null} for no mappings
     * @param msgS message root properties, or {@code null} to include all
     *        model components
     * @param noPrefix {@code true} to use mapped local names as property
     *        terms; {@code false} to use mapped QNames
     * @return the generated context object
     * @throws NullPointerException if {@code m} is {@code null}, or if
     *         {@code msgS} contains {@code null}
     * @throws CMFException if the context cannot be constructed, including
     *         ambiguous term definitions, namespace prefix conflicts, or
     *         missing namespace URIs for mapped prefixes
     */
    public static JsonObject create(
        Model m,
        Mapping map,
        Set<ObjectProperty> msgS,
        boolean noPrefix) throws CMFException {

        Objects.requireNonNull(m, "m must not be null");
        if (msgS != null) {
            for (var msg : msgS)
                if (null == msg)
                    throw new NullPointerException("msgS must not contain null");
        }

        var cxt = new JsonObject();
        if (map == null) {
            map = new Mapping();
        }

        Set<Component> compS = (msgS == null) ? m.componentSet() : m.messageComponents(msgS);
        var propL = sortedProperties(compS);

        // Collect namespace declarations from component namespaces and mapped property targets.
        var nsmap = new HashMap<String, String>();
        for (var c : compS) {
            addNamespace(nsmap, c.namespace().prefix(), c.namespace().uri());

            if (c instanceof Property p) {
                var cQ = p.qname();
                var mQ = map.qnToMappedQ(cQ);
                if (!cQ.equals(mQ)) {
                    var mpre = qnToPrefix(mQ);
                    var mnsU = map.prefixToURI(mpre);
                    if (mnsU == null) {
                        mnsU = nsmap.get(mpre);
                    }
                    if (mnsU == null) {
                        throw new CMFException(String.format(
                            "Can't construct context (no namespace URI for mapped prefix %s in %s)",
                            mpre, mQ));
                    }
                    addNamespace(nsmap, mpre, mnsU);
                }
            }
        }

        // Validate no-prefix local names.
        if (noPrefix) {
            var lnmap = new HashMap<String, String>();
            for (var p : propL) {
                var pQ = p.qname();
                var mQ = map.qnToMappedQ(pQ);
                var ln = qnToName(mQ);

                if (nsmap.containsKey(ln)) {
                    throw new CMFException(String.format(
                        "Can't construct no-prefix context (local name %s is also a namespace prefix)",
                        ln));
                }

                var oQ = lnmap.putIfAbsent(ln, mQ);
                if (oQ != null && !oQ.equals(mQ)) {
                    throw new CMFException(String.format(
                        "Can't construct no-prefix context (%s and %s have same local name)",
                        oQ, mQ));
                }
            }
        }

        // Validate that generated term keys do not collide.
        var termSrc = new HashMap<String, String>();
        for (var p : propL) {
            var cQ = p.qname();
            var mQ = map.qnToMappedQ(cQ);
            var term = noPrefix ? qnToName(mQ) : mQ;

            if (p.isOrdered()) {
                reserveTerm(termSrc, term, cQ);
                if (!term.equals(cQ)) {
                    reserveTerm(termSrc, cQ, cQ);
                }
            } else if (noPrefix || !cQ.equals(mQ)) {
                reserveTerm(termSrc, term, cQ);
            }
        }

        // Write namespace mappings in sorted order.
        var nspreL = new ArrayList<>(nsmap.keySet());
        nspreL.sort((a, b) -> {
            int cmp = String.CASE_INSENSITIVE_ORDER.compare(a, b);
            return (cmp != 0) ? cmp : a.compareTo(b);
        });
        for (var nspre : nspreL) {
            cxt.addProperty(nspre, nsmap.get(nspre));
        }

        // Write property term definitions in sorted order.
        for (var p : propL) {
            var cQ = p.qname();
            var mQ = map.qnToMappedQ(cQ);
            var term = noPrefix ? qnToName(mQ) : mQ;

            if (p.isOrdered()) {
                var obj = new JsonObject();
                if (!term.equals(cQ)) {
                    obj.addProperty("@id", cQ);
                }
                obj.addProperty("@container", "@list");
                cxt.add(term, obj);

                if (!term.equals(cQ)) {
                    var orig = new JsonObject();
                    orig.addProperty("@container", "@list");
                    cxt.add(cQ, orig);
                }
            } else if (noPrefix) {
                cxt.addProperty(term, cQ);
            } else if (!cQ.equals(mQ)) {
                cxt.addProperty(mQ, cQ);
            }
        }

        return cxt;
    }

    /**
     * Writes a JSON document containing the supplied context object as the
     * value of {@code @context}.
     *
     * @param res the context object to write
     * @param w the destination writer
     * @throws NullPointerException if {@code res} or {@code w} is {@code null}
     */
    public static void write(JsonObject res, Writer w) {
        Objects.requireNonNull(res, "res must not be null");
        Objects.requireNonNull(w, "w must not be null");

        var gson = new GsonBuilder().setPrettyPrinting().create();
        var cxt = new JsonObject();
        cxt.add("@context", res);
        gson.toJson(cxt, w);
    }

    private static ArrayList<Property> sortedProperties(Set<Component> compS) {
        var propL = new ArrayList<Property>();
        for (var c : compS) {
            if (c instanceof Property p) {
                propL.add(p);
            }
        }
        propL.sort((a, b) -> {
            int cmp = String.CASE_INSENSITIVE_ORDER.compare(a.qname(), b.qname());
            return (cmp != 0) ? cmp : a.qname().compareTo(b.qname());
        });
        return propL;
    }

    private static void addNamespace(
        Map<String, String> nsmap,
        String prefix,
        String uri) throws CMFException {

        Objects.requireNonNull(prefix, "namespace prefix must not be null");
        Objects.requireNonNull(uri, "namespace URI must not be null");

        var prior = nsmap.putIfAbsent(prefix, uri);
        if (prior != null && !prior.equals(uri)) {
            throw new CMFException(String.format(
                "Can't construct context (namespace prefix %s is bound to both %s and %s)",
                prefix, prior, uri));
        }
    }

    private static void reserveTerm(
        Map<String, String> termSrc,
        String term,
        String sourceQName) throws CMFException {

        var prior = termSrc.putIfAbsent(term, sourceQName);
        if (prior != null && !prior.equals(sourceQName)) {
            throw new CMFException(String.format(
                "Can't construct context (%s and %s both define term %s)",
                prior, sourceQName, term));
        }
    }
}
