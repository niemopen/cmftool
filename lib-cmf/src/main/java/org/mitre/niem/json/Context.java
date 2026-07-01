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
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.xml.XMLDocument;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToName;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToPrefix;

/**
 * A class for a JSON-LD context object that is capable of term
 * and compact IRI expansion.
 * 
 * There are constructors for creating a context based on a Model, with
 * or without a Mapping.  Constructors can also create a context that 
 * includes only the components needed for a set of message properties.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Context {
    
    private JsonObject cxt = null;

    private Context() { }
    
    public Context (Reader r) throws CMFException {
        try {
            var obj = JsonParser.parseReader(r).getAsJsonObject();
            if (obj.has("@context")) cxt = obj.getAsJsonObject("@context");
            else cxt = obj;
        }
        catch (Exception ex) {
            throw new CMFException("can't read context: " + ex.getMessage());}        
    }
    
    public JsonObject jsonObject () { return cxt; }

    /**
     * Creates a JSON-LD context for all components in the supplied model,
     * using no QName mappings.
     *
     * @param m the source model
     * @return the generated context object
     * @throws NullPointerException if {@code m} is {@code null}
     * @throws CMFException if the context cannot be constructed
     */
    public Context (Model m) throws CMFException {
        this(m, null, null, false);
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
    public Context (Model m, Mapping map) throws CMFException {
        this(m, map, null, false);
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
    public Context (Model m, Mapping map, ObjectProperty msg) throws CMFException {
        Objects.requireNonNull(msg, "msg must not be null");
        this(m, map, Set.of(msg), false);
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
    public Context (Model m, Mapping map, Set<ObjectProperty> msgS) throws CMFException {
        this(m, map, msgS, false);
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
    public Context (Model m, Mapping map, Set<ObjectProperty> msgS, boolean noPrefix) throws CMFException {
        Objects.requireNonNull(m, "Model m must not be null");
        if (msgS != null) {
            for (var msg : msgS)
                if (null == msg)
                    throw new NullPointerException("ObjectProperty msgS must not contain null");
        }
        cxt = new JsonObject();
        if (map == null) {
            map = new Mapping();
        }
        // Create a list of properties in the model, or a list of those required
        // for the specified message properties.
        Set<Component> compS = (msgS == null || msgS.isEmpty()) 
            ? m.componentSet() 
            : m.messageComponents(msgS);
        var propL = sortedProperties(compS);

        // Collect namespace declarations from component namespaces and mapped property targets.
        // Don't add a declaration for the XSD namespace.
        var nsmap = new HashMap<String, String>();
        for (var p : propL) {
            // Assign prefix and namespace from model properties
            var pns = p.namespace();
            if (!W3C_XML_SCHEMA_NS_URI.equals(pns.uri())) 
                addNamespace(nsmap, pns.prefix(), pns.uri());
            
            // Assign prefix and namespace from target mapping, if any.
            // Throws an exception if target's mapping conflicts with model's mapping.
            if (!noPrefix) {
                var pU = p.uri();
                var mQ = map.uriToTargetQN(pU);
                if (null != mQ) {
                    var mpre = qnToPrefix(mQ);          // target prefix
                    var mnsU = map.prefixToURI(mpre);   // uri of prefix from mapping
                    addNamespace(nsmap, mpre, mnsU);    // assign target prefix,uri
                }
            }
        }
        // If we are constructing a no-prefix context, then we must make
        // sure there is no term that is also a namespace prefix, and that 
        // no two properties map to the same local name.
        if (noPrefix) {
            var lnmap = new HashMap<String, String>();
            for (var p : propL) {
                var pU = p.uri();
                var mQ = map.uriToTargetQN(pU);
                if (null == mQ) mQ = p.qname();     // not mapped, use model qname
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
            var pQ = p.qname();
            var pU = p.uri();
            var mQ = map.uriToTargetQN(pU);
            if (null == mQ) mQ = pQ;
            var term = noPrefix ? qnToName(mQ) : mQ;
            if (p.isOrdered()) {
                var obj = new JsonObject();
                if (!term.equals(pU)) {
                    obj.addProperty("@id", pU);
                }
                obj.addProperty("@container", "@list");
                cxt.add(term, obj);

                if (!term.equals(pU)) {
                    var orig = new JsonObject();
                    orig.addProperty("@container", "@list");
                    cxt.add(pU, orig);
                }
            } 
            else if (noPrefix) {
                cxt.addProperty(term, pQ);
            } 
            else if (!pQ.equals(mQ)) {
                cxt.addProperty(mQ, pQ);
            }
        }
    }

    /**
     * Writes a JSON document representing the context object.
     *
     * @param w the destination writer
     * @throws NullPointerException if {@code w} is {@code null}
     */
    public void write(Writer w) {
        Objects.requireNonNull(w, "w must not be null");

        var gson = new GsonBuilder().setPrettyPrinting().create();
        var res = new JsonObject();
        res.add("@context", cxt);
        gson.toJson(res, w);
    }

    // Returns an ordered list of properties in a set of components.
    private static ArrayList<Property> sortedProperties(Set<Component> compS) {
        var propL = new ArrayList<Property>();
        for (var c : compS) {
            if (c instanceof Property p) {
                propL.add(p);
            }
        }
        Collections.sort(propL);
        return propL;
    }

    // Adds a namespace binding to a namespace map.  Throws an exception if
    // the prefix or uri is null, or if the prefix has already been differently
    // assigned.
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
    
    /**
     * Expands a JSON-LD term or compact IRI using this context object.
     * <p>
     * Expansion is performed in the following order:
     * </p>
     * <ol>
     * <li>If {@code termOrCompactIRI} is defined directly in the context as a
     * string value, that value is used and recursively expanded.</li>
     * <li>If {@code termOrCompactIRI} is defined directly in the context as an
     * object containing an {@code @id} entry, the {@code @id} value is used
     * and recursively expanded.</li>
     * <li>If the value is a compact IRI of the form {@code prefix:name}, and
     * {@code prefix} is defined in the context as a namespace IRI, the
     * result is the namespace IRI concatenated with {@code name}.</li>
     * <li>If the value is an unprefixed term and the context defines
     * {@code @vocab}, the result is the vocabulary IRI concatenated with
     * the term.</li>
     * <li>If no expansion rule applies, the original input value is returned
     * unchanged.</li>
     * </ol>
     * <p>
     * This method is intended to support expansion of context entries generated by
     * this class, including simple term definitions and object definitions such as
     * ordered properties represented with an {@code @id} and {@code @container:@list}.
     * </p>
     * <p>
     * If recursive term definitions form a cycle, expansion stops and the
     * current
     * value is returned unchanged.
     * </p>
     *
     * @param termOrCompactIRI a JSON-LD term, compact IRI, or absolute IRI
     * @return the expanded full IRI if the input can be expanded; otherwise the
     * original input value
     * @throws NullPointerException if {@code termOrCompactIRI} is {@code null}
     */
    public String expand(String termOrCompactIRI) {
        Objects.requireNonNull(termOrCompactIRI, "termOrCompactIRI must not be null");
        return expand(termOrCompactIRI, new java.util.HashSet<>());
    }

    private String expand(String value, Set<String> seen) {
        if (!seen.add(value)) {
            return value;
        }

        // If this exact term is defined in the context, expand from that definition.
        var def = cxt.get(value);
        if (def != null) {
            if (def.isJsonPrimitive() && def.getAsJsonPrimitive().isString()) {
                var mapped = def.getAsString();
                return mapped.equals(value) ? mapped : expand(mapped, seen);
            }
            if (def.isJsonObject()) {
                var obj = def.getAsJsonObject();
                if (obj.has("@id")) {
                    var id = obj.get("@id").getAsString();
                    return id.equals(value) ? id : expand(id, seen);
                }
            }
        }

        // Expand compact IRI: prefix:name -> namespaceURI + name
        int colon = value.indexOf(':');
        if (colon > 0) {
            var prefix = value.substring(0, colon);
            var suffix = value.substring(colon + 1);

            var nsDef = cxt.get(prefix);
            if (nsDef != null && nsDef.isJsonPrimitive() && nsDef.getAsJsonPrimitive().isString()) {
                return XMLDocument.makeURI(nsDef.getAsString(), suffix);
            }

            // No matching prefix in the context; assume this is already an absolute IRI
            // or otherwise not expandable.
            return value;
        }

        // Optional JSON-LD default vocabulary support.
        var vocabDef = cxt.get("@vocab");
        if (vocabDef != null && vocabDef.isJsonPrimitive() && vocabDef.getAsJsonPrimitive().isString()) {
            return vocabDef.getAsString() + value;
        }

        // Not expandable.
        return value;
    }
    
}
