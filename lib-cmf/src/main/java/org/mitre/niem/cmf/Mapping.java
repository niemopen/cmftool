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
import static javax.xml.XMLConstants.NULL_NS_URI;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.mitre.niem.utility.StringUtils.replaceSuffix;
import static org.mitre.niem.xml.XMLDocument.makeQN;
import static org.mitre.niem.xml.XMLDocument.makeURI;
import static org.mitre.niem.xml.XMLDocument.qnToName;
import static org.mitre.niem.xml.XMLDocument.qnToPrefix;
import org.mitre.niem.xsd.NamespaceKind;

/**
 * A class for representing same-as mappings from model data components
 * to their synonym names and URIs.
 *
 * Used to specify property names for simple (vs.canonical) message formats;
 * for example, a simple format might have "msg:lname" or just "lname" instead
 * of "nc:PersonSurName".
 *
 * Also used to specify mappings for class names, when creating a
 * single-namespace simple XML message format.
 *
 * Mappings exist between URIs. Mappings must be one-to-one. Trying to map one
 * source to more than one target, or more than one source to the same target,
 * will throw an exception.
 *
 * For convenience, a mapping file records mappings in terms of QNames.
 * The mapping file must specify the namespace binding of every prefix used.
 * For example:
 *
 * PREFIX nc https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/
 * PREFIX sx http://example.com/Refs/1.0/
 * # FROM TO
 * nc:PersonSurName sx:lname
 * nc:personNameInitialIndicator isInitial
 *
 * A target without a prefix is assumed to be in the null namespace. This can
 * be useful for single-namespace simple XML.
 *
 * Mapping lookup takes a source URI and returns:<ul>
 * <li> the target URI</li>
 * <li> a record containing target URI, namespace URI, local name, and prefix
 * bound to namespace URI in the mapping file</li></ul>
 *
 * The class has a method to create a template mapping file from a model.
 * Editing this template is easier than creating a mapping file from scratch.
 *
 * The class has a method to create a single-namespace mapping from a model.
 * Every component is mapped to a URI in a single namespace. Target local names
 * are munged in case of collision; if the model has foo:Name and bar:Name, then
 * the targets will be t:foo_Name and t:bar_Name.
 */
public class Mapping {

    // Namespace prefix assignments to support source and target QNames
    // in the mappings. These assignments might not be the same
    // as the namespace prefixes in the source model.
    private final Map<String, String> prefix2uri = new HashMap<>();
    private final Map<String, String> uri2prefix = new HashMap<>();

    private final Map<String, String> targetU2sourceU = new HashMap<>();  // target URI   -> source URI

    private final Map<String, MapRec> sourceU2mapRec = new HashMap<>();

    // Everything known about the mapping for a source component.
    public record MapRec(
        String sourceQN,            // given by addMapping()
        String prefix,              // target prefix, or "" if none
        String localName,           // target local name; eg. "bar" for target "foo:bar"
        String qname,               // target QName, with or without prefix
        String targetArgument,      // target argument passed to addMappping()
        String uri,                 // target component URI; local name for target w/o prefix
        String namespace)           // target namespace URI
        implements Comparable<MapRec> {

        @Override
        public int compareTo(MapRec other) {
            boolean thisBlankPrefix = prefix() == null || prefix().isBlank();
            boolean otherBlankPrefix = other.prefix() == null || other.prefix().isBlank();
            if (thisBlankPrefix != otherBlankPrefix) return thisBlankPrefix ? 1 : -1;

            boolean thisEndsWithType = this.localName() != null && this.localName().endsWith("Type");
            boolean otherEndsWithType = other.localName() != null && other.localName().endsWith("Type");
            if (thisEndsWithType != otherEndsWithType)  return thisEndsWithType ? 1 : -1;
        
            int cmp = String.CASE_INSENSITIVE_ORDER.compare(this.sourceQN(), other.sourceQN());
            if (cmp != 0) return cmp;
            return(this.sourceQN().compareTo(other.sourceQN()));
        }
    }

//    private static final MapRec NULL_REC = new MapRec("", "", "", "", "", "");

    public Mapping() { }

    /**
     * Returns true if the argument source URI is mapped.
     *
     * @param fromU source component URI
     * @return true if mapped
     */
    public boolean isMappedU(String fromU) {
        return sourceU2mapRec.containsKey(fromU);
    }

    /**
     * Returns the map record for the argument URI.
     * Returns null if argument is not mapped.
     *
     * @param fromU
     * @return map record or null
     */
    public MapRec uriToMapRec(String fromU) {
        return sourceU2mapRec.get(fromU);
    }

    /**
     * Adds a prefix/URI pair to the Mapping object.
     * Throws an exception if the prefix or URI are already assigned
     * inconsistently.
     *
     * @param prefix desired prefix
     * @param uri URI for prefix
     * @throws CMFException if the assignment is inconsistent with an existing
     * one
     */
    public void assignPrefix(String prefix, String uri) throws CMFException {
        if (null == prefix || prefix.isBlank())
            throw new CMFException("assignPrefix: blank or null prefix");
        if (null == uri || uri.isBlank())
            throw new CMFException("assignPrefix: blank or null URI");
        
        var mpre = uri2prefix.get(uri);
        if (null != mpre && !mpre.equals(prefix)) {
            throw new CMFException(String.format(
                "Can't assign prefix %s to %s (uri already mapped to %s)", prefix, uri, mpre));
        }
        var muri = prefix2uri.get(prefix);
        if (null != muri && !muri.equals(uri)) {
            throw new CMFException(String.format(
                "Can't assign prefix %s to %s (prefix already assigned to %s)", prefix, uri, muri));
        }
        prefix2uri.put(prefix, uri);
        uri2prefix.put(uri, prefix);
    }

    /**
     * Adds a mapping from the model component QName to a target XML name.
     * The source must be a QName whose prefix has already been assigned via
     * {@link #assignPrefix}. The target may be either:
     *
     * <ul>
     * <li>a QName whose prefix has already been assigned, or</li>
     * <li>an unprefixed local name, which is interpreted as being in the
     * null namespace</li>
     * </ul>
     *
     * Throws an exception if the source is already mapped to a different
     * target.
     *
     * @param fromQ model component QName
     * @param toN target QName or local name
     * @throws CMFException if prefixes are undeclared or the mapping is
     * inconsistent
     */
    public void addMapping(String fromQ, String toN) throws CMFException {
        if (!isQName(fromQ)) {
            throw new CMFException("Invalid source QName: " + fromQ);
        }
        if (!isQName(toN) && !isLocalName(toN)) {
            throw new CMFException("Invalid target name: " + toN);
        }

        var fromPre = qnToPrefix(fromQ);
        var fromLN = qnToName(fromQ);
        var fromNS = prefix2uri.get(fromPre);
        if (null == fromNS) {
            throw new CMFException("Undeclared source prefix: " + fromPre);
        }
        var fromU = makeURI(fromNS, fromLN);

        final String toLN;
        final String toNS;
        String toPre = "";
        String toQN = "";
        String toString = toN;
        if (isQName(toN)) {
            toQN = toN;
            toPre = qnToPrefix(toN);
            toLN = qnToName(toN);
            toNS = prefix2uri.get(toPre);
            if (null == toNS) {
                throw new CMFException("Undeclared target prefix: " + toPre);
            }
        } else {
            toLN = toN;
            toQN = toLN;
            toNS = NULL_NS_URI;
        }
        var toU = makeTargetURI(toNS, toLN);
        
        var s2t = sourceU2mapRec.get(fromU);
        var t2s = targetU2sourceU.get(toU);
        if (null != s2t && !s2t.uri().equals(toU)) {
            throw new CMFException(String.format(
                "Can't map %s to %s (%s already mapped to %s)", fromQ, toN, fromQ, s2t.uri()));
        }
        if (null != t2s && !t2s.equals(fromU)) {
            throw new CMFException(String.format(
                "Can't map %s to %s (%s already mapped to %s)", fromQ, toN, t2s, toN));
        }
        var mrec = new MapRec(fromQ, toPre, toLN, toQN, toString, toU, toNS);
        sourceU2mapRec.put(fromU, mrec);
        targetU2sourceU.put(toU, fromU);
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
    public static Mapping createTemplate(
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
    public static Mapping createTemplate(
        Model m, Set<ObjectProperty> msgPropS, String defPrefix, String defURI,
        boolean includeTypes) throws CMFException {

        // Keep all namespace prefix assignments from the model
        // Throw an exception if the defPrefix -> defURI assignment conflicts.
        var map = new Mapping();
        for (var ns : m.namespaceSet()) {
            map.assignPrefix(ns.prefix(), ns.uri());
        }
        map.assignPrefix(defPrefix, defURI);

        // Make a list of properties and classes needed for specified message types
        List<Component> compL;
        if (null == msgPropS || msgPropS.isEmpty()) compL = new ArrayList<>(m.componentList());
        else compL = new ArrayList<>(m.messageComponents(msgPropS));

        // Count number of mappings required to create pleasing target local names
        int nmap = 0;
        for (var c : compL) {
            if (needsMapping(c, includeTypes)) nmap++;
        }
        var digits = Long.toString(Math.max(nmap, 1)).length();
        var tfmt = "TEMP%0" + digits + "d";
        
        var cnum = 0;
        for (var c : compL) {
            if (needsMapping(c, includeTypes)) {
                var tname = String.format(tfmt, cnum++);
                var tQ = makeQN(defPrefix, tname);
                map.addMapping(c.qname(), tQ);
            }
        }
        return map;
    }

    /**
     * Creates a mapping object that maps every mappable component to a QName
     * in a single target namespace. Components in the XSD namespace are
     * skipped.
     *
     * Uses the source namespace prefix to mung property names in case of
     * collision.  For example, if your model has nc:PersonName and foo:PersonName, 
     * then the mapping will have my:nc_PersonName and my:foo_PersonName.
     *
     * If your model includes object references, then the result will include
     * mappings for structures:id, ref, and uri.
     *
     * @param m model object
     * @param msgPropS only map components required for these message properties
     * @param defPrefix target namespace prefix
     * @param defURI target namespace URI
     * @param includeTypes also map classes and datatypes?
     * @return new Mapping object
     * @throws CMFException if defPrefix is the prefix of a model namespace
     */
    public static Mapping createOneNamespaceMapping(
        Model m, Set<ObjectProperty> msgPropS, String defPrefix, String defURI,
        boolean includeTypes) throws CMFException {

        // Keep all namespace prefix assignments from the model
        // Throw an exception if the defPrefix -> defURI assignment conflicts.
        // Also create a NamespaceMap; we might mung structures prefix later.
        var map = new Mapping();
        var nsm = new NamespaceMap();
        for (var ns : m.namespaceSet()) {
            map.assignPrefix(ns.prefix(), ns.uri());
            nsm.assignPrefix(ns.prefix(), ns.uri());
        }
        map.assignPrefix(defPrefix, defURI);
        nsm.assignPrefix(defPrefix, defURI);

        // Make a list of properties and classes needed for specified message types
        Set<Component> compS;
        if (null == msgPropS || msgPropS.isEmpty()) compS = m.componentSet();
        else compS = m.messageComponents(msgPropS);

        // Augmentation properties and elements must be mapped
        for (var ns : m.namespaceSet()) {
            for (var arec : ns.augL()) {
                if (!needsMapping(arec.property(), includeTypes)) continue;
                var base = "";
                var ct = arec.classType();
                var gcS  = new HashSet<>(arec.codeS());
                gcS.add("CLASS");
                for (var gc : gcS) {
                    switch (gc) {
                    case "CLASS":
                        if (compS.contains(ct) && !arec.property().isAttribute()) 
                            base = replaceSuffix(ct.name(), "Type", "");
                        break;
                    case "ASSOCIATION":
                    case "OBJECT":
                        base = capitalize(gc.toLowerCase());
                        break;
                    }
                    if (!base.isEmpty()) {
                        var p = new Property(arec.namespace(), base + "Augmentation");
                        compS.add(p);
                    }  
                }
                if (null == ct || compS.contains(ct)) {
                    compS.add(arec.property());
                }
            }
        }
        // Count number of times each local name appears among mapped components.
        // Also account for structures attributes if there are referencable classes.
        var lnct = new HashMap<String, Integer>();
        var structUs = new HashSet<String>();           // all structures namespace URIs
        for (var c : compS) {
            if (needsMapping(c, includeTypes)) {
                var lct = lnct.getOrDefault(c.name(), 0);
                lnct.put(c.name(), lct + 1);
              
                // If this is a referencable class, we will want to map its
                // the structures:id, ref, and uri attributes, so add them
                // to the lname counts.
                if (c instanceof ClassType ct) {
                    if (!"NONE".equals(ct.effectiveReferenceCode())) {
                        var vers = ct.namespace().archVersion();
                        var structU = NamespaceKind.builtinNSU(vers, "STRUCTURES");
                        if (!structUs.contains(structU)) {
                            var structP = nsm.assignPrefix("structures", structU);
                            map.assignPrefix(structP, structU);
                            structUs.add(structU);
                            lnct.put("id", lnct.getOrDefault("id", 0) + 1);
                            lnct.put("ref", lnct.getOrDefault("ref", 0) + 1);
                            lnct.put("uri", lnct.getOrDefault("uri", 0) + 1);
                        }                      
                    }
                }
            }
        }
        // Create mappings for model components.
        for (var c : compS) {
            if (needsMapping(c, includeTypes)) {
                var cns  = c.namespace();
                var cln  = c.name();
                var cpre = cns.prefix();
                int num  = lnct.getOrDefault(cln, 1);
                var tpre = defPrefix;
                if (c instanceof Property p)
                    if (p.isAttribute()) tpre = "";
                if (num > 1) map.addMapping(c.qname(), makeQN(tpre, cpre + "_" + cln));
                else map.addMapping(c.qname(), makeQN(tpre, cln));
            }
        }
        // Create mappings for reference attributes in structures namespace if needed
        for (var structU : structUs) {
            var structP = nsm.getPrefix(structU);
            for (var cln : Set.of("id", "ref", "uri")) {
                var fromQ = makeQN(structP, cln);
                var toQ = "";
                int num = lnct.getOrDefault(cln, 1);
                if (num > 1) toQ = makeQN("", structP + "_" + cln);
                else         toQ = makeQN("", cln);
                map.addMapping(fromQ, toQ);
            }
        }
        return map;
    }

    /**
     * Writes the mapping object to a file.
     * If a target is in the null namespace, it is written as an unprefixed
     * local name.
     *
     * @param w output writer
     * @throws IOException if output fails
     */
    public void write(Writer w) throws IOException {
        
        // Write prefix lines for namespace assignments
        var prefixL = new ArrayList<>(prefix2uri.keySet());
        var maxLen = 0;
        Collections.sort(prefixL);
        for (var pre : prefixL) {
            maxLen = Math.max(maxLen, pre.length());
        }
        maxLen += 1;
        var fmt = "PREFIX %-" + maxLen + "s  %s\n";
        for (var pre : prefixL) {
            var uri = prefix2uri.get(pre);
            if (!W3C_XML_SCHEMA_NS_URI.equals(uri)) {
                w.write(String.format(fmt, pre, uri));
            }
        }
        // Create ordered list of mappings
        var mapL = new ArrayList<>(sourceU2mapRec.values());
        Collections.sort(mapL);
        
        // Compute length of longest source QName
        maxLen = 1;
        for (var mrec : mapL) {
            maxLen = Math.max(maxLen, mrec.sourceQN().length());
        }
        fmt = "%-" + maxLen + "s    %s\n";
        w.write(String.format(fmt, "# FromQName", "ToName"));
        
        for (var mrec : mapL) {
            w.write(String.format(fmt, mrec.sourceQN(), mrec.targetArgument()));
        }
    }

    /**
     * Reads a mapping object from a File.
     *
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

    // NIEM allows only a subset of XML QNames
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
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }

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

    private static boolean isXSDComponent(Component c) {
        return W3C_XML_SCHEMA_NS_URI.equals(c.namespace().uri());
    }

    private static boolean isQName(String s) {
        return QNAME_PAT.matcher(s).matches();
    }

    private static boolean isLocalName(String s) {
        return LOCAL_PAT.matcher(s).matches();
    }
    
    private static boolean needsMapping (Component c, boolean includeTypes) {
        if (isXSDComponent(c)) return false;
        if (c.isAbstract()) return false;
        if (!c.isProperty() && !includeTypes) return false;
        return true;
    }

    private static String makeTargetURI(String nsU, String localName) {
        if (null == nsU || NULL_NS_URI.equals(nsU)) {
            return localName;
        }
        return makeURI(nsU, localName);
    }

}
