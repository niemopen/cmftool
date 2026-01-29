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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import static java.lang.Integer.max;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.math.NumberUtils;
import org.mitre.niem.utility.NaturalOrderIgnoreCaseComparator;
import static org.mitre.niem.xml.XMLSchemaDocument.makeURI;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToName;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToPrefix;
import static org.mitre.niem.xsd.NIEMConstants.OWL_NS_URI;

/**
 * A class for representing same-as mappings from a data model component QName
 * to a simple synonym QName or local name.  Used to specify component names 
 * for simple (vs. canonical) message formats; for example, a simple format
 * might have "msg:lname" or just "lname" instead of "nc:PersonSurName".
 * 
 * You can create an empty Mapping object and set all your mappings one by one.
 * URI A maps to at most one B, and only A maps to that B.
 * You get an exception if you try anything else.
 * 
 * Suppose all of your mapping targets have the same namespace.  Then you
 * may set the "noPrefix" property.  When you ask for the mapping of,
 * for example, "nc:PersonSurName", you will get "lname" instead of "msg:lname".
 * This is useful for single-namespace simple XML formats.
 * 
 * Reads and writes in SSSOM (Simple Standard for Sharing Ontology Mappings) format
 * for persistent store.  There are two convenience methods for generating a mapping
 * template in this format, which you then edit elsewhere.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Mapping {

    private final NamespaceMap nsmap          = new NamespaceMap(); // prefix/URI pairs known in mapping
    private final Map<String,String> qn2mapQ  = new HashMap<>();    // QName -> mapped QName
    private final Map<String,String> mapQ2qn  = new HashMap<>();    // mapped QName -> component QName
    private final Set<String> toPrefixS       = new HashSet<>();    // set of target prefixes
    private boolean noPrefix = false;                               // mapping returns name part by default     
    
    public Mapping () { }
    
    /**
     * Sets the "noPrefix" property for this mapping.  When noPrefix is true,
     * every mapping must be to a single namespace.
     * @param noPrefix
     * @throws MappingException 
     */
    public void setNoPrefix (boolean noPrefix) throws MappingException {
        this.noPrefix = noPrefix;
        if (!noPrefix) return;
        if (toPrefixS.size() > 1)
            throw new MappingException("Can't set noPrefix; map has more than one target namespace");
    }
    
    /**
     * Returns the QName mapping for the argument QName.  Returns the argument
     * QName if it is not mapped.  Not affected by the noPrefix property.
     * @param fromQ
     * @return 
     */
    public String qnToQ (String fromQ) { return qn2mapQ.getOrDefault(fromQ, fromQ); }
    
    /**
     * Returns the name mapped to the argument QName.  This could be a QName, or
     * just the local name (no prefix) if noPrefix is set.  Returns the argument 
     * QName if not mapped.
     * @param fromQ
     * @return 
     */
    public String qnToN (String fromQ)   { 
        var res = qn2mapQ.get(fromQ);
        if (null == res) return fromQ;
        if (noPrefix) return qnToName(res);
        return res;
    }
    
    /**
     * Suppose you want a simple message format with a single namespace.  This method
     * accepts a default namespace prefix and URI, and creates a mapping from 
     * every component URI in the model to a URI with that default URI base plus 
     * the property name.
     * 
     * For example, https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/PersonSurName
     * might be mapped to http://my.default/PersonSurName.
     *  
     * Uses the namespace prefix to mung property names in case of collision; for example,
     * http://my.default/ncPersonSurName and http://my.default/extPersonSurName.
     *
     * @param m 
     * @param defaultPrefix 
     * @param defaultURI 
     * @return new Mapping object
     */
    public static Mapping createDefault (Model m, String defaultPrefix, String defaultURI) throws MappingException {
        var map = new Mapping();
        for (var ns : m.namespaceList()) {
            if (ns.isModelNS())
                map.assignPrefix(ns.prefix(), ns.uri());
        }
        map.assignPrefix(defaultPrefix, defaultURI);
        
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
            var lct = lnct.get(p.name());
            if (lct > 1) {
                var prefix = qnToPrefix(p.qname());
                map.addQNameMapping(p.qname(), defaultPrefix + ":" + prefix + p.name());
            }
            else map.addQNameMapping(p.qname(), defaultPrefix + ":" + p.name());
        }
        map.setNoPrefix(true);
        return map;
    }

    /**
     * Creates a mapping object with a dummy "to" URI for each property in a model.
     * @param m
     */
    public static Mapping createTemplate (Model m) throws MappingException {
        var map   = new Mapping();
        for (var ns : m.namespaceList()) {
            if (ns.isModelNS())
                map.assignPrefix(ns.prefix(), ns.uri());
        }
        var tPre = map.assignPrefix("T", "http://example.com/YourTargetNamespace");   
        var num = 0;
        var spL = new ArrayList<>(m.propertyL());
        Collections.sort(spL);
        for (var p : spL) {
            if (p.namespace().isModelNS() && !p.isAbstract())
                map.addQNameMapping(p.qname(), String.format("%s:TEMP%04d", tPre, num++));
        }
        return map;
    }
    
    /**
     * Writes the mapping object in SSSOM format.
     * @param w
     * @throws IOException 
     */
    public void write (Writer w) throws IOException {
        var owlP = nsmap.assignPrefix("owl", OWL_NS_URI);
        w.write("# curie_map:\n");
        for (var prefix : nsmap.prefixList()) {
            if (!nsmap.isReserved(prefix))
                w.write(String.format("#   %s: %s\n", prefix, nsmap.getURI(prefix)));
        }
        if (noPrefix) w.write("# NoPrefix: true\n");
        w.write("subject_id\tpredicate_id\tobject_id\n");
        var srcList = qn2mapQ.keySet().stream()
            .sorted(new NaturalOrderIgnoreCaseComparator())
            .collect(Collectors.toList());
        int maxlen = 0;
        for (String srcQ : srcList) maxlen = max(maxlen, srcQ.length());
        for (String srcQ : srcList) {
            w.write(String.format("%-" + maxlen + "s %s:sameAs %s\n", srcQ, owlP, qn2mapQ.get(srcQ)));
        }
    }
    
    /**
     * Reads a mapping object from a File in SSSOM format
     * @param f
     * @return
     * @throws IOException
     * @throws MappingException 
     */
    public static Mapping readFile (File f) throws IOException, MappingException {
        var rdr = new BufferedReader(new FileReader(f));
        return read(rdr);
    }
    
    private static final String OWL_SAMEAS = OWL_NS_URI + "sameAs";
    private static final Pattern CURIE_PAT = Pattern.compile("^#\\s\\s\\s+([A-Za-z0-9_\\-]+):\\s+(\\S+)\\s*$");
    
    /**
     * Reads a mapping object from a reader in SSSOM format.
     * @param r
     * @return
     * @throws IOException
     * @throws MappingException 
     */
    public static Mapping read (Reader r) throws IOException, MappingException {
        var m     = new Mapping();
        var subI  = -1;             // index of subject column
        var prdI  = -1;             // index of predicate column
        var objI  = -1;             // index of object column
        var inMap = false;          // are we inside the curie_map?
        var mapF  = false;          // have we seen the curie map?
        var hdrF  = false;          // have we seen the columm header line?
        var lnum  = 0;              // current line number
        var line  = "";
        var br    = new BufferedReader(r);
        while ((line = br.readLine()) != null) {
            // Strip unicode byte order mark
            lnum++;
            if (line.length() > 0 && '\uFEFF' == line.charAt(0)) {
                line = line.substring(1);
            }
            // Handle comment line
            if (line.startsWith("#")) {
                var comment = line.substring(1).trim();
                if ("# NoPrefix: true".equals(line)) m.noPrefix = true;
                else if (comment.startsWith("curie_map:")) {
                    inMap = true;
                    mapF = true;
                }
                else if (inMap) {
                    var mat = CURIE_PAT.matcher(line);
                    if (mat.matches()) {
                        var prefix = mat.group(1).trim();
                        var uri    = mat.group(2).trim();
                        var opre   = m.nsmap.getPrefix(uri);
                        if (null != opre && !prefix.equals(opre))
                            throw new MappingException("line " + lnum + ": conflicting assignment for prefix " + prefix);
                        m.assignPrefix(prefix, uri);
                    }
                    else inMap = false;
                }
                continue;
            }
            // First non-comment line must be the header.
            if (!hdrF) {
                var cols = line.split("\\s+", -1);
                for (int i = 0; i < cols.length; i++) {
                    var hdr = cols[i].trim();
                    if ("subject_id".equals(hdr)) subI = i;
                    else if ("predicate_id".equals(hdr)) prdI = i;
                    else if ("object_id".equals(hdr)) objI = i;
                }
                if (subI < 0 || prdI < 0 || objI < 0)
                    throw new MappingException("line " + lnum + ": bad SSSOM header line");
                hdrF = true;
            }
            // Other non-comment lines are data rows
            else {
                var cols = line.split("\\s+", -1);
                if (0 == cols.length) continue;     // blank line
                if (NumberUtils.max(subI, prdI, objI) >= cols.length)
                    throw new MappingException("line " + lnum + ": bad data line (not enough columns)");
                var subQ = cols[subI].trim();
                var prdQ = cols[prdI].trim();
                var objQ = cols[objI].trim();
                var prdU = "";
                try {
                    prdU = m.makeResourceU(prdQ);
                    m.addQNameMapping(subQ, objQ);
                }
                catch (MappingException ex) {
                    throw new MappingException("line " + lnum + ": " + ex.getMessage());
                }
                if (!OWL_SAMEAS.equals(prdU))
                    throw new MappingException("line " + lnum + ": predicate \"" + prdQ + "\" not allowed (must be owl:sameAs");
            }
        }
        // Error if we haven't seen the curie_map.
        if (!mapF) throw new MappingException("no curie_map section (namespace prefix to URI)");
        return m;
    }
    
    /**
     * Adds a prefix/URI pair to the Mapping object.  The prefix may be munged
     * if the desired prefix is already assigned.
     * @param prefix -- desired prefix
     * @param uri -- URI for prefix
     * @return the assigned prefix (possibly munged)
     */
    public String assignPrefix (String prefix, String uri) {
        return nsmap.assignPrefix(prefix, uri);
    }
    
    /**
     * Adds a mapping from the subject QName to the object QName, and vice versa.
     * Subject and object prefixes must be known.  Subject and object must be 
     * either unmapped, or already mapped to each other.
     * @param fromQ
     * @param toQ
     * @throws MappingException 
     */
    public void addQNameMapping (String fromQ, String toQ) throws MappingException {
        var fpre = getPrefix(fromQ);
        var tpre = getPrefix(toQ);
        var fm2  = qn2mapQ.get(fromQ);
        var tm2  = mapQ2qn.get(toQ);
        if (null != fm2 && !fm2.equals(toQ))
            throw new MappingException(
                String.format("Can't map %s to %s (%s already mapped to %s)", fromQ, toQ, fromQ, fm2));
        if (null != tm2 && !tm2.equals(fromQ))
            throw new MappingException(
                String.format("Can't map %s to %s (%s already mapped to %s", fromQ, toQ, tm2, toQ));
        qn2mapQ.put(fromQ, toQ);
        mapQ2qn.put(toQ, fromQ);
        toPrefixS.add(tpre);
        if (noPrefix && toPrefixS.size() > 1)
            throw new MappingException(
                String.format("Can't map %s to %s (too many target prefixes", fromQ, toQ));
    }
    
    // Make sure the argument is a QName and that the prefix is known to the mapping.
    public String getPrefix (String qn) throws MappingException {
        var prefix = qnToPrefix(qn);
        var lname  = qnToName(qn);
        var baseU  = nsmap.getURI(prefix);
        if (null == prefix)
            throw new MappingException("bad QName \"" + qn + "\" (not a QName)");
        if (null == baseU)
            throw new MappingException("bad QName \"" + qn + "\" (prefix not defined)");     
        return prefix;
    }
    
    // Turn a QName into a URI
    private String makeResourceU (String qn) throws MappingException {
        var prefix = qnToPrefix(qn);
        var lname  = qnToName(qn);
        var base   = nsmap.getURI(prefix);
        if (null == prefix)
            throw new MappingException("bad resource ID \"" + qn + "\" (not a QName)");
        if (null == base)
            throw new MappingException("bad resource ID \"" + qn + "\" (prefix not defined)");
        return makeURI(base, lname);
    }    

}
