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
package org.mitre.niem.cmf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.mitre.niem.xml.XMLDocument.makeQN;
import static org.mitre.niem.xml.XMLDocument.makeURI;
import static org.mitre.niem.xml.XMLDocument.qnToName;
import static org.mitre.niem.xml.XMLDocument.qnToPrefix;
import org.mitre.niem.xsd.NamespaceKind;

/**
 * A class for representing same-as mappings from a data model component QName
 * to a simple synonym QName. Used to specify property names for simple 
 * (vs. canonical) message formats; for example, a simple format might have 
 * "msg:lname" or just "lname" instead of "nc:PersonSurName".
 * 
 * Mappings whose source QName is in the XSD namespace are ignored.
 * 
 * You can create a template object from a model, write to a file and edit later.
 * The template will have a dummy mapping for each property.
 * 
 * You can create a single-namespace mapping from a model.  Every component is
 * mapped to a QName in a single namespace.  Target local names are munged
 * in case of collision; if the model has foo:Name and bar:Name, then the
 * targets will be t:foo_Name and t:bar_Name.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Mapping {

    // Namespace prefix assignments to support source and target QNames 
    // in the mappings.  These assignments might not be the same
    // as the namespace prefixes in the source model.
    private final Map<String,String> pre2uri  = new HashMap<>();    // namespace prefix -> URI
    private final Map<String,String> uri2pre  = new HashMap<>();    // namespace URI -> prefix
    
    // Keep track of mappings in several ways.
    // * Source QN or URI to target QN, URI, namespace URI, or local name
    // * Target URI to source URI
    private final Map<String,String> qn2mapQ  = new HashMap<>();    // source QName -> target QName
    private final Map<String,String> uri2mapQ = new HashMap<>();    // souorce URI  -> target QName
    private final Map<String,String> mapU2uri = new HashMap<>();    // target URI   -> source URI
    private final Map<String,String> mapQ2qn  = new HashMap<>();    // target QName -> source QName
//    private final Map<String,String> uri2mapN = new HashMap<>();    // component URI -> mapped name
//    private final Map<String,String> mapN2uri = new HashMap<>();    // mapped name -> component URI

    public Mapping () { }

    /**
     * Returns true if the argument source QName is mapped.
     * @param fromQ source component QName
     * @return true if mapped
     */
    public boolean isMappedQ (String fromQ) {
        return qn2mapQ.containsKey(fromQ);
    }

    /**
     * Returns true if the argument source URI is mapped.
     * @param fromU source component URI
     * @return true if mapped
     */
    public boolean isMappedU (String fromU) {
        return uri2mapQ.containsKey(fromU);
    }
    
    /**
     * Returns the target QName mapped to the source QName.
     * Returns null if the source is not mapped.
     * @param fromQ source component QName
     * @return 
     */
    public String qnToTargetQN (String fromQ) {
        return qn2mapQ.get(fromQ);
    }
    
    /**
     * Returns the target QName mapped to the source URI.
     * Returns null if the source is not mapped.
     * @param fromU source component URI
     * @return 
     */
    public String uriToTargetQN (String fromU) {
        return uri2mapQ.get(fromU);
    }
    
    /**
     * Returns the namespace URI from the mapping of the source URI.
     * For example, if source:foo is mapped to target:bar, then the URI
     * mapped to the prefix "target" is returned.
     * Returns null if the source is not mapped.
     * @param fromU
     * @return 
     */
    public String uriToTargetNSU (String fromU) {
        var tQ = uri2mapQ.get(fromU);
        if (null == tQ) return null;
        var pre = qnToPrefix(tQ);
        var nsU = pre2uri.get(pre);
        return nsU;
    }
    
    /**
     * Returns the target URI mapped to the source URI.
     * Returns null if the source is not mapped
     * @param fromU source component URI
     * @return 
     */
    public String uriToTargetURI (String fromU) {
        var tQ = uri2mapQ.get(fromU);
        if (null == tQ) return null;
        var ln  = qnToName(tQ);
        var pre = qnToPrefix(tQ);
        var nsU = pre2uri.get(pre);
        var tU  = makeURI(nsU, ln);
        return tU;
    }
    
    /**
     * Returns the source QName that is mapped to the target QName.
     * Returns null if nothing mapped to the target QName
     * @param targetQ target QName
     * @return 
     */
    public String targetQtoSourceQN (String targetQ) {
        return mapQ2qn.get(targetQ);
    }
    
    /**
     * Returns the source component URI that is mapped to the target URI.
     * Returns null if no such component.
     * @param targetU
     * @return 
     */
    public String targetUToSourceURI (String targetU) {
        return mapU2uri.get(targetU);
    }

    /**
     * Returns the namespace URI assigned to the namespace prefix.
     * Returns null if prefix not assigned.
     * @param prefix namespace prefix
     * @return namespace URI
     */
    public String prefixToURI (String prefix) {
        return pre2uri.get(prefix);
    }

    /**
     * Adds a prefix/URI pair to the Mapping object.
     * Throws an exception if the prefix or URI are already assigned inconsistently.
     * @param prefix desired prefix
     * @param uri URI for prefix
     * @throws CMFException if the assignment is inconsistent with an existing one
     */
    public void assignPrefix (String prefix, String uri) throws CMFException {
        var mpre = uri2pre.get(uri);
        if (null != mpre && !mpre.equals(prefix)) {
            throw new CMFException(String.format(
                "Can't assign prefix %s to %s (uri already mapped to %s)", prefix, uri, mpre));
        }
        var muri = pre2uri.get(prefix);
        if (null != muri && !muri.equals(uri)) {
            throw new CMFException(String.format(
                "Can't assign prefix %s to %s (prefix already assigned to %s)", prefix, uri, muri));
        }
        pre2uri.put(prefix, uri);
        uri2pre.put(uri, prefix);
    }

    /**
     * Adds a mapping from the model component QName to a target name.
     * The source must be a QName whose prefix has already been assigned via
     * {@link #assignPrefix}. The target must be a QName, and its prefix must 
     * also already be assigned.  Throws an exception if the source is already
     * mapped to a different target.
     * 
     * If the source QName is in the XSD namespace, the mapping is ignored.
     *
     * @param fromQ model component QName
     * @param toQ target QName
     * @throws CMFException if prefixes are undeclared or the mapping is inconsistent
     */
    public void addMapping (String fromQ, String toQ) throws CMFException {
        if (!QNAME_PAT.matcher(fromQ).matches()) {
            throw new CMFException("Invalid source QName: " + fromQ);
        }
        if (!QNAME_PAT.matcher(toQ).matches()) {
            throw new CMFException("Invalid source QName: " + toQ);
        }
        var fromPre = qnToPrefix(fromQ);
        var fromLN  = qnToName(fromQ);
        var fromNS  = pre2uri.get(fromPre);
        if (null == fromNS) {
            throw new CMFException("Undeclared source prefix: " + fromPre);
        }
        if (W3C_XML_SCHEMA_NS_URI.equals(fromNS)) return;

        var fromU = makeURI(fromNS, fromLN);
        var toPre = qnToPrefix(toQ);
        var toLN  = qnToName(toQ);
        var toNS  = pre2uri.get(toPre);
        if (null == toNS) {
            throw new CMFException("Undeclared source prefix: " + toNS);
        }
        var toU = makeURI(toNS, toLN);

        var s2t = qn2mapQ.get(fromQ);
        var t2s = mapQ2qn.get(toQ);
        if (null != s2t && !s2t.equals(toQ)) {
            throw new CMFException(String.format(
                "Can't map %s to %s (%s already mapped to %s)", fromQ, toQ, fromQ, s2t));
        }
        if (null != t2s && !t2s.equals(fromQ)) {
            throw new CMFException(String.format(
                "Can't map %s to %s (%s already mapped to %s)", fromQ, toQ, toQ, t2s));
        }
        qn2mapQ.put(fromQ, toQ);
        uri2mapQ.put(fromU, toQ);
        mapQ2qn.put(toQ, fromQ);
        mapU2uri.put(toU, fromU);
    }

    /**
     * Creates a mapping object with a dummy target QName for each mappable
     * component in a model. Components in the XSD namespace are skipped.
     * The result is suitable for editing once written to a file.
     *
     * @param m model object
     * @param defPrefix prefix for each mapping target in template
     * @param defURI URI for each mapping target namespace
     * @param includeTypes map classes and datatypes?
     * @return new Mapping object
     * @throws CMFException if the mapping cannot be constructed
     */
    public static Mapping createTemplate (
        Model m, String defPrefix, String defURI, boolean includeTypes) throws CMFException {
        return createTemplate(m, new HashSet<ObjectProperty>(), defPrefix, defURI, includeTypes);
    }

    /**
     * Creates a mapping object with a dummy target QName for the model
     * components required to create a message schema for the specified
     * message property. Components in the XSD namespace are skipped.
     * Abstract components are skipped. The result is suitable for editing
     * once written to a file.
     *
     * @param m model object
     * @param msgPropS only map components needed for these message properties
     * @param defPrefix prefix for each mapping target in template
     * @param defURI URI for each mapping target namespace
     * @param includeTypes map classes and datatypes?
     * @return new Mapping object
     * @throws CMFException if defPrefix is a prefix of a model namespace
     */
    public static Mapping createTemplate (
        Model m, Set<ObjectProperty> msgPropS, String defPrefix, String defURI, 
        boolean includeTypes) throws CMFException {

        var map = new Mapping();
        for (var ns : m.namespaceSet()) {
            map.assignPrefix(ns.prefix(), ns.uri());
        }
        map.assignPrefix(defPrefix, defURI);

        List<Component> compL;
        if (msgPropS.isEmpty()) compL = new ArrayList<>(m.componentList());
        else compL = new ArrayList<>(m.messageComponents(msgPropS));

        int nmap = 0;
        for (var c : compL) {
            if (isXSDComponent(c)) continue;
            if (c.isAbstract()) continue;
            if (!c.isProperty() && !includeTypes) continue;
            nmap++;
        }

        var digits = Long.toString(Math.max(nmap, 1)).length();
        var tfmt   = "TEMP%0" + digits + "d";
        var cnum = 0;
        for (var c : compL) {
            if (isXSDComponent(c)) continue;
            if (c.isAbstract()) continue;
            if (!c.isProperty() && !includeTypes) continue;
            var tname = String.format(tfmt, cnum++);
            var tQ = makeQN(defPrefix, tname);
            map.addMapping(c.qname(), tQ);
        }
        return map;
    }

    /**
     * Creates a mapping object that maps every mappable component to a QName
     * in a single target namespace. Components in the XSD namespace are skipped.
     * <p>
     * Uses the source namespace prefix to mung property names in case of collision.
     * For example, if your model has nc:PersonName and foo:PersonName, then
     * the mapping will have my:nc_PersonName and my:foo_PersonName.
     * </p>
     * <p>
     * If your model includes object references, then the result will include
     * mappings for structures:id, ref, and uri.
     * </p>
     *
     * @param m model object
     * @param msgPropS only map components required for these message properties
     * @param defPrefix target namespace prefix
     * @param defURI target namespace URI
     * @param includeTypes also map classes and datatypes?
     * @return new Mapping object
     * @throws CMFException if defPrefix is the prefix of a model namespace
     */
    public static Mapping createOneNamespaceMapping (
        Model m, Set<ObjectProperty> msgPropS, String defPrefix, String defURI,
        boolean includeTypes) throws CMFException {

        var map = new Mapping();
        var nsm = new NamespaceMap();
        for (var ns : m.namespaceSet()) {
            map.assignPrefix(ns.prefix(), ns.uri());
            nsm.assignPrefix(ns.prefix(), ns.uri());
        }
        map.assignPrefix(defPrefix, defURI);
        nsm.assignPrefix(defPrefix, defURI);

        List<Component> compL;
        if (msgPropS.isEmpty()) compL = new ArrayList<>(m.componentList());
        else compL = new ArrayList<>(m.messageComponents(msgPropS));

        // Count number of times each local name appears in component list.
        // Ignore XSD components because they are not mapped.
        var lnct = new HashMap<String,Integer>();
        for (var c : compL) {
            if (isXSDComponent(c)) continue;
            if (c.isAbstract()) continue;
            if (!c.isProperty() && !includeTypes) continue;
            if (!c.namespace().isModelNS()) continue;
            var lct = lnct.getOrDefault(c.name(), 0);
            lnct.put(c.name(), lct + 1);
        }
        // Do we need mappings for XML reference attributes?
        var structUs = new HashSet<String>();
        for (var c : compL) {
            if (isXSDComponent(c)) continue;
            if (c.isAbstract()) continue;
            if (!c.isProperty() && !includeTypes) continue;
            if (!c.namespace().isModelNS()) continue;
            if (c instanceof ClassType ct && !"NONE".equals(ct.effectiveReferenceCode())) {
                var vers = ct.namespace().version();
                var structU = NamespaceKind.builtinNSU(vers, "STRUCTURES");
                if (!structUs.contains(structU)) {
                    var structP = nsm.assignPrefix("structures", structU);
                    map.assignPrefix(structP, structU);
                    structUs.add(structU);
                    lnct.put("id",  lnct.getOrDefault("id", 0) + 1);
                    lnct.put("ref", lnct.getOrDefault("ref", 0) + 1);
                    lnct.put("uri", lnct.getOrDefault("uri", 0) + 1);
                }
            }
        }
        // Add component mappings.
        for (var c : compL) {
            if (isXSDComponent(c)) continue;
            if (c.isAbstract()) continue;
            if (!c.isProperty() && !includeTypes) continue;
            var cns  = c.namespace();
            var cln  = c.name();
            var cpre = cns.prefix();
            int num  = lnct.getOrDefault(cln, 1);
            if (num > 1) {
                map.addMapping(c.qname(), makeQN(defPrefix, cpre + "_" + cln));
            }
            else {
                map.addMapping(c.qname(), makeQN(defPrefix, cln));
            }
        }
        // Add reference attribute mappings.
        for (var structU : structUs) {
            var structP = nsm.getPrefix(structU);
            for (var cln : Set.of("id", "ref", "uri")) {
                var fromQ = makeQN(structP, cln);
                var toQ = "";
                int num = lnct.getOrDefault(cln, 1);
                if (num > 1) toQ = makeQN(defPrefix, structP + "_" + cln);
                else toQ = makeQN(defPrefix, cln);
                map.addMapping(fromQ, toQ);
            }
        }
        return map;
    }
    
    
    /**
     * Writes the mapping object to a file.
     * @param w output writer
     * @throws IOException if output fails
     */
    public void write (Writer w) throws IOException {
        var prefixL = new ArrayList<>(pre2uri.keySet());
        var maxLen  = 0;
        Collections.sort(prefixL);
        for (var pre : prefixL) {
            maxLen = Math.max(maxLen, pre.length());
        }
        maxLen += 1;
        var fmt = "PREFIX %-" + maxLen + "s  %s\n";
        for (var pre : prefixL) {
            var uri = pre2uri.get(pre);
            if (!W3C_XML_SCHEMA_NS_URI.equals(uri)) {
                w.write(String.format(fmt, pre, uri));
            }
        }
        var order = new HashMap<String,String>();
        var fromL = new ArrayList<>(qn2mapQ.keySet());
        Collections.sort(fromL);
        maxLen = 1;
        for (var fromQ : fromL) {
            maxLen = Math.max(maxLen, fromQ.length());
            var fpre = qnToPrefix(fromQ);
            var fnsU = pre2uri.get(fpre);
            var kind = NamespaceKind.namespaceToKindCode(fnsU);
            if ("STRUCTURES".equals(kind)) order.put(fromQ, "structures");
            else if (fromQ.endsWith("Type")) order.put(fromQ, "type");
            else order.put(fromQ, "prop");
        }
        fmt = "%-" + maxLen + "s    %s\n";
        w.write(String.format(fmt, "# FromQName", "ToQName"));
        for (var fromQ : fromL) {
            if ("prop".equals(order.get(fromQ))) {
                w.write(String.format(fmt, fromQ, qn2mapQ.get(fromQ)));
            }
        }
        for (var fromQ : fromL) {
            if ("type".equals(order.get(fromQ))) {
                w.write(String.format(fmt, fromQ, qn2mapQ.get(fromQ)));
            }
        }
        for (var fromQ : fromL) {
            if ("structures".equals(order.get(fromQ))) {
                w.write(String.format(fmt, fromQ, qn2mapQ.get(fromQ)));
            }
        }
    }

    /**
     * Reads a mapping object from a File.
     * @param f mapping file
     * @return mapping object
     * @throws IOException if input fails
     * @throws CMFException if the mapping file is invalid
     */
    public static Mapping readFile(File f) throws IOException, CMFException {
        try (var rdr = java.nio.file.Files.newBufferedReader(
            f.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
            return read(rdr);
        }
    }

    private static final String PREFIX = "[A-Za-z][A-Za-z0-9_-]*";
    private static final String LOCAL = "[A-Za-z0-9_.-]+";
    private static final String PREFIXED_NAME = PREFIX + ":" + LOCAL;

    private static final Pattern QNAME_PAT = Pattern.compile(
        "^" + PREFIXED_NAME + "$",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern LOCAL_PAT = Pattern.compile(
        "^" + LOCAL + "$",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern WRITE_PREFIX_PAT = Pattern.compile(
        "^PREFIX\\s+(" + PREFIX + ")\\s+(\\S+)(?:\\s+#.*)?\\s*$",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern WRITE_MAPPING_PAT = Pattern.compile(
        "^(" + PREFIXED_NAME + ")\\s+(" + PREFIXED_NAME + "|" + LOCAL + ")(?:\\s+#.*)?\\s*$",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    /**
     * Reads a mapping object from a reader in the text format produced by
     * {@link #write(Writer)}.
     *
     * @param r source reader
     * @return mapping object
     * @throws IOException if input fails
     * @throws CMFException if the mapping syntax or semantics are invalid
     */
    public static Mapping read(Reader r) throws IOException, CMFException {
        var map = new Mapping();
        var br = (r instanceof BufferedReader) ? (BufferedReader) r : new BufferedReader(r);
        String line;
        int lnum = 0;

        while ((line = br.readLine()) != null) {
            lnum++;
            if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                line = line.substring(1);
            }
            line = line.strip();
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) continue;

            var pm = WRITE_PREFIX_PAT.matcher(line);
            if (pm.matches()) {
                var prefix = pm.group(1);
                var uri = pm.group(2);
                try {
                    map.assignPrefix(prefix, uri);
                } catch (CMFException ex) {
                    throw new CMFException("Line " + lnum + ": " + ex.getMessage());
                }
                continue;
            }
            var mm = WRITE_MAPPING_PAT.matcher(line);
            if (mm.matches()) {
                var fromQ = mm.group(1);
                var toN = mm.group(2);
                try {
                    map.addMapping(fromQ, toN);
                } catch (CMFException ex) {
                    throw new CMFException("Line " + lnum + ": " + ex.getMessage());
                }
                continue;
            }
            throw new CMFException("Line " + lnum + ": invalid mapping syntax: " + line);
        }
        return map;
    }

    /**
     * Validates that this mapping is consistent with the supplied model.
     * <p>
     * Checks that:
     * </p>
     * <ul>
     *   <li>every mapped source QName uses a prefix declared in the model</li>
     *   <li>every mapped source prefix is bound to the same URI in the mapping
     *       and the model</li>
     *   <li>every mapped source QName identifies a component in the model</li>
     *   <li>every mapped target prefix, if present, is declared in the mapping</li>
     *   <li>if a mapped target prefix is also used by the model, it is bound
     *       to the same URI in both places</li>
     * </ul>
     *
     * @param m model object
     * @throws NullPointerException if {@code m} is {@code null}
     * @throws CMFException if the mapping is inconsistent with the model
     */
    public void validateAgainstModel(Model m) throws CMFException {
        if (null == m) {
            throw new NullPointerException("m must not be null");
        }

        var errs = new ArrayList<String>();

        var fromL = new ArrayList<>(qn2mapQ.keySet());
        Collections.sort(fromL);

        for (var fromQ : fromL) {
            var fromPre = qnToPrefix(fromQ);
            var mapNSU = pre2uri.get(fromPre);
            var modNSU = m.prefixToNSU(fromPre);

            if (null == modNSU) {
                errs.add(String.format(
                    "%s: source prefix %s is not declared in model",
                    fromQ, fromPre));
                continue;
            }

            if (null != mapNSU && !modNSU.equals(mapNSU)) {
                errs.add(String.format(
                    "%s: source prefix %s maps to %s in mapping but %s in model",
                    fromQ, fromPre, mapNSU, modNSU));
                continue;
            }

            if (null == m.qnToComponent(fromQ)) {
                errs.add(String.format(
                    "%s: no such component in model",
                    fromQ));
            }
        }

        var seenTargetN = new HashSet<String>();
        for (var toN : qn2mapQ.values()) {
            if (!seenTargetN.add(toN)) continue;
            if (!QNAME_PAT.matcher(toN).matches()) continue;

            var toPre = qnToPrefix(toN);
            var mapNSU = pre2uri.get(toPre);
            if (null == mapNSU) {
                errs.add(String.format(
                    "%s: target prefix %s is not declared in mapping",
                    toN, toPre));
                continue;
            }

            var modNSU = m.prefixToNSU(toPre);
            if (null != modNSU && !modNSU.equals(mapNSU)) {
                errs.add(String.format(
                    "%s: target prefix %s maps to %s in mapping but %s in model",
                    toN, toPre, mapNSU, modNSU));
            }
        }

        if (!errs.isEmpty()) {
            throw new CMFException(String.join("\n", errs));
        }
    }

    private static boolean isXSDComponent(Component c) {
        return W3C_XML_SCHEMA_NS_URI.equals(c.namespace().uri());
    }
}

