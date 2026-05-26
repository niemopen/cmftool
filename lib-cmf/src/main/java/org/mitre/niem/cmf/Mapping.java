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
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import static org.mitre.niem.xml.XMLDocument.qnToPrefix;
import static org.mitre.niem.xml.XMLDocument.qnToName;
import static org.mitre.niem.xml.XMLSchemaDocument.makeQN;
import static org.mitre.niem.xsd.NIEMConstants.OWL_NS_URI;

/**
 * A class for representing same-as mappings from a data model component QName
 * to a simple synonym QName.  Used to specify property names for simple 
 * (vs. canonical) message formats; for example, a simple format
 * might have "msg:lname" or just "lname" instead of "nc:PersonSurName".
 * 
 * A mapping file is a Turtle document, with "#" comment lines.
 * Best practice puts all of the @prefix lines before all of the triples.
 * Prefixes must be declared before they are used in a triple.
 * You cannot have two prefixes for the same namespace, or 
 * two namespaces for the same prefix.
 * 
 * You can create an empty Mapping object and set all your mappings one by one.
 * QName A maps to at most one B, and no other QName maps to that B.  You get an 
 * exception if you try anything else.
 * 
 * You can create and then edit a mapping template file.  The "createTemplate"
 * method writes a mapping file with a dummy mapping for every property in
 * a model.
 * 
 * Sometimes you are content with the property local names in the model, and
 * just want to squash everything into a single namespace.  (Can be handy 
 * for a simple XML message format.)  The "createDefault" method will do that.
 * You'll get mappings like:
 *   nc:PersonName owl:equivalentProperty target:PersonName .
 * 
 * If your model has two properties with the same local name, then both of them
 * will be munged in the createDefault output:
 *   nc:PersonName  owl:equivalentProperty target:nc_PersonName .
 *   foo:PersonName owl:equivalentProperty target:foo_PersonName .
 * 
 * If your model has a class that allows references, then the createDefault
 * output will have dummy mappings for structures:{id, ref, uri}.  You have
 * to edit the output to put in the correct prefix and URI for the structures
 * namespace(s) in your model.  Or, if you don't need XML messages, you can
 * just delete these lines.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Mapping {

    final NamespaceMap nsmap                  = new NamespaceMap(); // prefix/URI pairs known in mapping
    private final Map<String,String> qn2mapQ  = new HashMap<>();    // property QN -> mapped qn
    private final Map<String,String> mapQ2qn  = new HashMap<>();    // mapped QN -> property QN
    private final Map<String,String> toU2pre  = new HashMap<>();    // mapped URI -> its prefix
    private final Set<String> toNSuriS        = new HashSet<>();    // set of all target namespace URIs
    private boolean noPrefix                  = false;              // return mapped name w/o prefix?   
    
    public Mapping () { }
    
    /**
     * Returns the QName mapping for the argument QName.  Returns the argument
     * QName if it is not mapped.
     * @param fromQ
     * @return mapped QName
     */
    public String qnToMappedQN (String fromQ) { 
        var mapQ = qn2mapQ.get(fromQ);
        if (null == mapQ) return fromQ;
        return mapQ;
    }
    
    /**
     * Returns the name mapped to the argument QName.  This could be a QName, or
     * just the local name (without prefix) if noPrefix is set.  Returns the argument 
     * QName if not mapped.
     * @param fromQ
     * @return mapped local name or QName
     */
    public String qnToMappedName (String fromQ)   { 
        var mapQ = qn2mapQ.get(fromQ);
        if (null == mapQ) return fromQ;
        if (noPrefix) return qnToName(mapQ);
        return mapQ;
    }
    
    public boolean noPrefix () { return noPrefix; }
    
    /**
     * When set true, mapping targets will return the local name without prefix.
     * Sets and returns false if the map has more than one target namespace.
     * @param val
     * @return new value of noPrefix
     */
    public boolean setNoPrefix (boolean val) {
        if (val && toU2pre.size() > 1) val = false;
        noPrefix = val;
        return val;
    }
    
    /**
     * Adds a prefix/URI pair to the Mapping object.  The prefix may be munged
     * if the desired prefix is already assigned.
     * @param prefix -- desired prefix
     * @param uri -- URI for prefix
     * @return the assigned prefix (possibly munged)
     */
    public String assignPrefix (String prefix, String uri) throws CMFException {
        var mpre = nsmap.getPrefix(uri);
        if (null != mpre && !mpre.equals(prefix)) {
            throw new CMFException(String.format(
                "Can't assign prefix %s to %s (prefix %s already assigned)", prefix, uri, mpre));
        }
        return nsmap.assignPrefix(prefix, uri);
    }    
    
    /**
     * Adds a mapping from the model property QName to a target QName.
     * The prefixes of both QNames must already be mapped via assignPrefix.
     * @param fromQ - model property QName
     * @param toQ - target QName
     * @throws CMFException 
     */
    public void addMapping (String fromQ, String toQ) throws CMFException {
        var fromPre = qnToPrefix(fromQ);
        var toPre   = qnToPrefix(toQ);
        var toU     = nsmap.getURI(toPre);
        var cToQ    = qn2mapQ.get(fromQ);
        var cFromQ  = mapQ2qn.get(toQ);
        var cU2pre  = toU2pre.get(toU);
        if (nsmap.getURI(fromPre) == null) {
            throw new CMFException("Undeclared source prefix: " + fromPre);
        }
        if (null == toU) {
            throw new CMFException("Undeclared target prefix: " + toPre);
        }
        if (null != cU2pre && !cU2pre.equals(toPre)) {
            throw new CMFException(String.format(
                "Can't add mapping %s -> %s (target namespace %s already used with prefix %s)",
                fromQ, toQ, toU, cU2pre));            
        }
        if (null != cToQ && !cToQ.equals(toQ)) {
            throw new CMFException(String.format(
                "Can't add mapping %s -> %s (%s already mapped to %s)", fromQ, toQ, fromQ, cToQ));
        }
        if (null != cFromQ && !cFromQ.equals(fromQ)) {
            throw new CMFException(String.format(
                "Can't add mapping %s -> %s (%s already mapped to %s)", fromQ, toQ, cFromQ, toQ));     
        }        
        qn2mapQ.put(fromQ, toQ);
        mapQ2qn.put(toQ, fromQ);
        toU2pre.put(toU, toPre);
        toNSuriS.add(toU);
        if (toU2pre.size() > 1) noPrefix = false;
    }
    
    /**
     * Suppose you want a simple message format with a single namespace.  This method
     * accepts a default namespace prefix and URI, and creates a mapping from 
     * every property QName in the model to a QName with that prefix plus 
     * the property name.
     * 
     * For example, given defPrefix "my" and defURI "http://my.default/",
     * then nc:PersonName would be mapped to my:PersonName,
     * which would expand to http://my.default/PersonName.
     *  
     * Uses the namespace prefix to mung property names in case of collision; for example,
     * if your model has nc:PersonSurName and foo:PersonSurName, then the mapping file
     * will have my:nc_PersonSurName and my:foo_PersonSurName.
     * 
     * If your model includes object references, then the output will include
     * dummy mappings for structures:id, ref, and uri.  You will have to edit
     * the output to provide the correct structures prefix and URI.
     *
     * @param m Model object
     * @param defPrefix 
     * @param defURI 
     * @return new Mapping object
     */
    public static Mapping createDefault (Model m, String defPrefix, String defURI) {
        var map = new Mapping();
        
        // Add mapping for each model namespace
        try {
            for (var ns : m.namespaceList()) {
                if (ns.isModelNS()) {
                    map.assignPrefix(ns.prefix(), ns.uri());
                }
            }
            // Add mapping for default prefix/URI.  Prefix might be munged.
            defPrefix = map.assignPrefix(defPrefix, defURI);
        } catch (CMFException ex) { // CAN'T HAPPEN
            throw new IllegalStateException("Unexpected namespace mapping collision");
        }
        
        // Add mappings for structures namespace if needed
        var needStructures = false;
        for (var ct : m.classTypeL()) {
            if (!"NONE".equals(ct.effectiveReferenceCode())) {
                needStructures = true;
                break;
            }
        }
        if (needStructures) {
            try {
                var sPre = map.assignPrefix("YOUR_STRUCTURES_PREFIX", "http://YOUR_STRUCTURES_NS_URI_HERE");
                map.addMapping(makeQN(sPre, "id"),  makeQN(defPrefix, "__STRUCTURES_ID__"));
                map.addMapping(makeQN(sPre, "ref"), makeQN(defPrefix, "__STRUCTURES_REF__"));
                map.addMapping(makeQN(sPre, "uri"), makeQN(defPrefix, "__STRUCTURES_URI__"));
            } catch (CMFException ex) { // EXTREMELY WEIRD MODEL
                throw new IllegalStateException("Unexpected structures mapping collision");
            }
        }        
        // How many times does a local name appear in the model?
        var lnct = new HashMap<String,Integer>();
        for (var p : m.propertyL()) {
            if (!p.namespace().isModelNS()) continue;
            if (p.isAbstract()) continue;
            var lct = lnct.getOrDefault(p.name(), 0);
            lnct.put(p.name(), lct + 1);
        }
        // Add mappings; munged mapping when a local name appears more than once.
        for (var p : m.propertyL()) {
            if (!p.namespace().isModelNS()) continue;
            if (p.isAbstract()) continue;
            var pQ  = p.qname();
            var lct = lnct.get(p.name());
            try {
                if (lct > 1) {
                    var toQ = makeQN(defPrefix, qnToPrefix(pQ) + "_" + p.name());
                    map.addMapping(p.qname(), toQ);
                } else {
                    var toQ = makeQN(defPrefix, p.name());
                    map.addMapping(p.qname(), toQ);
                }
            } catch (CMFException ex) {  // CAN'T HAPPEN
                throw new IllegalStateException("Impossible addMapping exception happened");
            }
        }
        return map;
    }
    
    public static Mapping createTemplate (Model m) {
        return createTemplate(m, "T", "http://example.com/YourNamespaceURIGoesHere/");
    }

    /**
     * Creates a mapping object with a dummy "to" QName for each property in a model.
     * @param m Model object
     * @param defPrefix - prefix for each mapping target in template
     * @param defURI - URI for each mapping target
     * @return new Mapping object
     */
    public static Mapping createTemplate (Model m, String defPrefix, String defURI) {
        var map   = new Mapping();
        try {
            for (var ns : m.namespaceList()) {
                if (ns.isModelNS()) {
                    map.assignPrefix(ns.prefix(), ns.uri());
                }
            }
            defPrefix = map.assignPrefix(defPrefix, defURI);
        } catch (CMFException ex) { } // IGNORE
        
        var num = 0;
        var spL = new ArrayList<>(m.propertyL());
        Collections.sort(spL);
        for (var p : spL) {
            if (p.namespace().isModelNS() && !p.isAbstract()) {
                try {
                    map.addMapping(p.qname(), String.format("%s:TEMP%04d", defPrefix, num++));
                } catch (CMFException ex) {  // CAN'T HAPPEN
                    throw new IllegalStateException("Impossible addMapping exception happened");
                }
            }
        }
        return map;
    }
    
    /**
     * Writes the mapping object in RDF/Turtle format.
     * @param w - output writer
     * @throws IOException 
     */
    public void write (Writer w) throws IOException {
        var owlPre  = nsmap.assignPrefix("owl", OWL_NS_URI);
        var prefixL = new ArrayList<>(nsmap.prefixList());
        var maxLen  = 0;
        Collections.sort(prefixL);                     // alphabetical
        for (var pre : prefixL) {
            if (nsmap.isReserved(pre)) continue;         // skip reserved prefixes
            maxLen = Math.max(maxLen, pre.length());
        }
        maxLen += 1;
        var fmt = "@prefix %-" + maxLen + "s <%s> .\n";
        for (var pre : prefixL) {
            if (nsmap.isReserved(pre)) continue;     // do not emit reserved prefixes
            w.write(String.format(fmt, pre + ":", nsmap.getURI(pre)));
        }        
        var srcL = new ArrayList<>(qn2mapQ.keySet());
        maxLen   = 0;
        Collections.sort(srcL);
        for (var src : srcL) {
            maxLen = Math.max(maxLen, src.length());
        }
        fmt = "%-" + maxLen + "s " + owlPre + ":equivalentProperty %s .\n";
        for (var srcQ : srcL) {
            var tQ = qn2mapQ.get(srcQ);
            w.write(String.format(fmt, srcQ, tQ));
        }        
    }
    
    /**
     * Reads a mapping object from a File in RDF/Turtle format
     * @param f - mapping file
     * @return mapping object
     * @throws IOException
     * @throws CMFException 
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
    
    private static final Pattern PREFIX_PAT = Pattern.compile(
        "^@prefix\\s+(" + PREFIX + "):\\s+<([^>]+)>\\s*\\.$");
    
    public static final Pattern TURTLE_TRIPLE = Pattern.compile(
        "^\\s*" +
        "(?<subject>"   + PREFIXED_NAME + ")\\s+" +
        "(?<predicate>" + PREFIXED_NAME + ")\\s+" +
        "(?<object>"    + PREFIXED_NAME + ")" +
        "\\s*\\.\\s*" +           // must end with .
        "(?:#.*)?$",              // optional comment
        Pattern.UNICODE_CHARACTER_CLASS
    );
    
    /**
     * Reads a mapping object from a reader in RDF/Turtle format.
     * @param r - source reader
     * @return mapping object
     * @throws IOException
     * @throws CMFException 
     */
    public static Mapping read (Reader r) throws IOException, CMFException {
        var map  = new Mapping();
        var br = (r instanceof BufferedReader) ? (BufferedReader) r : new BufferedReader(r);
        var lnum = 0;
        var line = "";
        while ((line = br.readLine()) != null) {
            lnum++;
            if (line.length() > 0 && '\uFEFF' == line.charAt(0)) {
                line = line.substring(1);
            }
            line = line.strip();
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) continue;
            var preM = PREFIX_PAT.matcher(line);
            if (preM.matches()) {
                var pre = preM.group(1);
                var uri = preM.group(2);
                var preMap = map.nsmap.getURI(pre);
                if (null != preMap && !uri.equals(preMap)) {
                    throw new CMFException(String.format(
                        "Invalid mapping file (prefix \"%s\" is mapped to %s and %s)", pre, preMap, uri));
                }
                var assigned = map.assignPrefix(pre, uri);
                if (!assigned.equals(pre)) {
                    throw new CMFException(String.format(
                        "Invalid mapping file (prefix \"%s\" could not be assigned exactly)", pre));
                }
                continue;
            }
            var tripleM = TURTLE_TRIPLE.matcher(line);
            if (tripleM.matches()) {               
                var sub  = tripleM.group("subject");
                var pred = tripleM.group("predicate");
                var obj  = tripleM.group("object");
                var subPrefix = qnToPrefix(sub);
                var predPrefix = qnToPrefix(pred);
                var objPrefix = qnToPrefix(obj);
                if (map.nsmap.getURI(subPrefix) == null) {
                    throw new CMFException(String.format(
                        "Invalid mapping file (undeclared prefix %s at line %d)", subPrefix, lnum));
                }
                if (map.nsmap.getURI(predPrefix) == null) {
                    throw new CMFException(String.format(
                        "Invalid mapping file (undeclared prefix %s at line %d)", predPrefix, lnum));
                }
                if (map.nsmap.getURI(objPrefix) == null) {
                    throw new CMFException(String.format(
                        "Invalid mapping file (undeclared prefix %s at line %d)", objPrefix, lnum));
                }
                if (!"equivalentProperty".equals(qnToName(pred)) ||
                    !OWL_NS_URI.equals(map.nsmap.getURI(qnToPrefix(pred)))) {
                    throw new CMFException(String.format(
                        "Invalid mapping file (invalid predicate at line %d)", lnum));
                }

                map.addMapping(sub, obj);
            }
            else throw new CMFException(String.format(
                "Invalid mapping file (line %d is not a triple)", lnum));
        }
        return map;
    }
}
