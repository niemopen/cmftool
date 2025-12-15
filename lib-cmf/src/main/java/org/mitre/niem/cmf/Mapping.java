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
package org.mitre.niem.cmf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.math.NumberUtils;
import org.mitre.niem.utility.NaturalOrderIgnoreCaseComparator;
import static org.mitre.niem.xml.XMLSchemaDocument.makeURI;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToName;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToPrefix;
import static org.mitre.niem.xsd.NIEMConstants.OWL_NS_URI;

/**
 * A class for representing same-as mappings between data component URIs.
 * Used to specify component names for simple (vs. canonical) message formats; 
 * for example, using "msg:lname" instead of "nc:PersonSurName".
 * It might be good for something else.
 * 
 * You can create an empty Mapping object and set all your mappings one by one.
 * Mappings are always a bijection.  That is, URI A maps to at most one B, 
 * and that B always maps to only that A.
 * 
 * Reads and writes in SSSOM (Simple Standard for Sharing Ontology Mappings) format
 * for persistent store.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Mapping {

    private final NamespaceMap nsmap       = new NamespaceMap();    // prefix/URI pairs known in mapping
    private final Map<String,String> qn2Q  = new HashMap<>();       // QName -> mapped QName
    private final Map<String,String> uri2U = new HashMap<>();       // URI -> mapped URI
    
    public Mapping () { }
    
    public String uriToU (String fromU)  { return uri2U.get(fromU); }
    public String qnToQ (String fromQ)   { return qn2Q.get(fromQ); }
    
    /**
     * Suppose you want a simple message format with a single namespace.  This method
     * accepts a default namespace URI, and creates a mapping from every component URI
     * in the model to a URI with that default URI base plus the property name.
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
                map.addPrefixMapping(ns.prefix(), ns.uri());
        }
        map.addPrefixMapping(defaultPrefix, defaultURI);
        
        // How many times does a local name appear in the model?
        var lnct = new HashMap<String,Integer>();
        for (var c : m.componentList()) {
            if (!c.namespace().isModelNS()) continue;
            var lct = lnct.getOrDefault(c.name(), 0);
            lnct.put(c.name(), lct + 1);
        }
        // Add mappings; munged mapping when a local name appears more than once.
        for (var c : m.componentList()) {
            if (!c.namespace().isModelNS()) continue;
            var lct = lnct.get(c.name());
            if (lct > 1) {
                var prefix = qnToPrefix(c.qname());
                map.addQNameMapping(c.qname(), defaultPrefix + ":" + prefix + c.name());
            }
            else map.addQNameMapping(c.qname(), defaultPrefix + ":" + c.name());
        }
        return map;
    }

    /**
     * Creates a mapping object with a dummy "to" URI for each component in a model.
     * @param m
     */
    public static Mapping createTemplate (Model m) throws MappingException {
        var map   = new Mapping();
        for (var ns : m.namespaceList()) {
            if (ns.isModelNS())
                map.addPrefixMapping(ns.prefix(), ns.uri());
        }
        var tPre = map.addPrefixMapping("T", "http://example.com/YourTargetNamespace");   
        var num = 0;
        for (var c : m.componentList()) {
            if (c.namespace().isModelNS())
                map.addQNameMapping(c.qname(), String.format("%s:TEMP%04d", tPre, num++));
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
        w.write("subject_id\tpredicate_id\tobject_id\n");
        var srcList = qn2Q.keySet().stream()
            .sorted(new NaturalOrderIgnoreCaseComparator())
            .collect(Collectors.toList());
        for (String srcQ : srcList) {
            w.write(String.format("%s\t%s:sameAs\t%s\n", srcQ, owlP, qn2Q.get(srcQ)));
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
                if (comment.startsWith("curie_map:")) {
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
                        m.addPrefixMapping(prefix, uri);
                    }
                    else inMap = false;
                }
                continue;
            }
            // First non-comment line must be the header.
            if (!hdrF) {
                var cols = line.split("\t", -1);
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
                var cols = line.split("\t", -1);
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
    public String addPrefixMapping (String prefix, String uri) {
        return nsmap.assignPrefix(prefix, uri);
    }
    
    /**
     * Adds a mapping from the subject QName to the object QName, and vice versa.
     * Subject and object prefixes must be known.  Subject and object must be 
     * either unmapped, or already mapped to each other.
     * @param subQ
     * @param objQ
     * @throws MappingException 
     */
    public void addQNameMapping (String subQ, String objQ) throws MappingException {
        var subU = makeResourceU(subQ);
        var objU = makeResourceU(objQ);
        var sub2U = uri2U.get(subU);
        var obj2U = uri2U.get(objU);
        if (null != sub2U && !sub2U.equals(objU))
            throw new MappingException(subU + " already mapped to " + sub2U);
        if (null != obj2U && !obj2U.equals(subU))
            throw new MappingException(objU + " already mapped to " + obj2U);
        qn2Q.put(subQ, objQ);
        uri2U.put(subU, objU);
        qn2Q.put(objQ, subQ);
        uri2U.put(objU, subU);
    }
    
    private String makeResourceU (String qname) throws MappingException {
        var prefix = qnToPrefix(qname);
        var lname  = qnToName(qname);
        var base   = nsmap.getURI(prefix);
        if (null == prefix)
            throw new MappingException("bad resource ID \"" + qname + "\" (not a QName)");
        if (null == base)
            throw new MappingException("bad resource ID \"" + qname + "\" (prefix not defined)");
        return makeURI(base, lname);
    }

}
