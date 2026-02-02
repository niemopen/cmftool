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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import static org.mitre.niem.xml.XMLSchemaDocument.makeQN;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToName;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToPrefix;
import static org.mitre.niem.xsd.NIEMConstants.OWL_NS_URI;

/**
 * A class for representing same-as mappings from a data model component QName
 * to a simple synonym local name.  Used to specify property names for simple 
 * (vs. canonical) message formats; for example, a simple format
 * might have "msg:lname" or just "lname" instead of "nc:PersonSurName".
 * 
 * You can create an empty Mapping object and set all your mappings one by one.
 * URI A maps to at most one B, and only A maps to that B.  All of the target
 * mappings must have the same namespace.  You get an exception if you try 
 * anything else.
 * 
 * You can create and then edit a mapping template file.  The "createTemplate"
 * method writes a mapping file with a dummy mapping for every property in
 * a model.
 * 
 * Sometimes you are content with the property local names and just want to
 * squash everything into a single namespace.  (Can be handy for a simple XML
 * message format.)  The "createDefault" method will do that.
 * 
 * Mapping files are written in RDF/Turtle with owl:equivalentProperty.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Mapping {

    private final NamespaceMap nsmap          = new NamespaceMap(); // prefix/URI pairs known in mapping
    private final Map<String,String> qn2lname = new HashMap<>();    // property QN -> mapped local name
    private final Map<String,String> lname2qn = new HashMap<>();    // mapped local name -> property QN
    private final Set<String> lnameS          = new HashSet<>();    // set of all mapped local names
    private String targetNSuri                = null;               // all map targets have this namespace
    private String targetPrefix               = null;
    
    
//    private final Map<String,String> qn2mapQ  = new HashMap<>();    // QName -> mapped QName
//    private final Map<String,String> mapQ2qn  = new HashMap<>();    // mapped QName -> component QName
//    private final Set<String> toPrefixS       = new HashSet<>();    // set of target prefixes
//    private boolean noPrefix = false;                               // mapping returns name part by default     
    
    public Mapping () { }
    
    /**
     * Returns the QName mapping for the argument QName.  Returns the argument
     * QName if it is not mapped.
     * @param fromQ
     * @return mapped QName
     */
    public String qnToQ (String fromQ) { 
        var ln = qn2lname.get(fromQ);
        if (null == ln) return fromQ;
        return targetPrefix + ":" + ln;
    }
    
    /**
     * Returns the name mapped to the argument QName.  This could be a QName, or
     * just the local name (no prefix) if noPrefix is set.  Returns the argument 
     * QName if not mapped.
     * @param fromQ
     * @return 
     */
    public String qnToN (String fromQ)   { 
        return qn2lname.getOrDefault(fromQ, fromQ);
    }
    
    public String setTargetNS (String defPrefix, String defURI) {
        targetNSuri  = defURI;
        targetPrefix = nsmap.assignPrefix(defPrefix, defURI);
        return targetPrefix;
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
     * Adds a mapping from the model property QName to a mapped QName.
     * Sets the target namespace from the mapped QName if that hasn't happened yet.
     * Throws an exception if the target namespace and mapped namespace don't match.
     * @param fromQ - model property QName
     * @param toQ - mapped QName
     * @throws CMFException 
     */
    public void addQNmapping (String fromQ, String toQ) throws CMFException {
        var prefix = qnToPrefix(toQ);
        var lname  = qnToName(toQ);
        if (null == targetPrefix) {
            targetPrefix = prefix;
            targetNSuri  = nsmap.getURI(targetPrefix);
            if (null == targetNSuri) 
                throw new CMFException(String.format(
                    "Can't add mapping %s -> %s (prefix %s not assigned)", fromQ, toQ, prefix));
        }
        else if (!targetPrefix.equals(prefix)) {
            throw new CMFException(String.format(
                "Can't add mapping %s -> %s (%s not in target namespace)", fromQ, toQ, toQ));
        }
        addNameMapping(fromQ, lname);
    }
    
    /**
     * Adds a mapping from the model property QName to a mapped local name.
     * @param fromQ
     * @param toName
     * @throws CMFException 
     */
    public void addNameMapping (String fromQ, String toName) throws CMFException {
        if (null == targetPrefix) {
            throw new CMFException(
                String.format("Can't add mapping %s -> %s (target namespace not set)", fromQ, toName));
        }
        var xmap = qn2lname.get(fromQ);
        var mapx = lname2qn.get(toName);
        if (null != xmap) {
            if (xmap.equals(toName)) return;
            throw new CMFException(String.format(
                "Can't add mapping %s -> %s (%s already mapped to %s)", fromQ, toName, fromQ, xmap));
        }
        if (null != mapx) {
            throw new CMFException(String.format(
                "Can't add mapping %s -> %s (%s already mapped to %s)", fromQ, toName, mapx, toName));            
        }
        qn2lname.put(fromQ, toName);
        lname2qn.put(toName, fromQ);        
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
     * @param m Model object
     * @param defPrefix 
     * @param defURI 
     * @return new Mapping object
     */
    public static Mapping createDefault (Model m, String defPrefix, String defURI) throws CMFException {
        var map = new Mapping();
        for (var ns : m.namespaceList()) {
            if (ns.isModelNS())
                map.assignPrefix(ns.prefix(), ns.uri());
        }
        map.targetNSuri  = defURI;
        map.targetPrefix = defPrefix;
        map.assignPrefix(defPrefix, defURI);
        
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
                map.addNameMapping(p.qname(), prefix + p.name());
            }
            else map.addNameMapping(p.qname(), p.name());
        }
        return map;
    }
    
    public static Mapping createTemplate (Model m) throws CMFException {
        return createTemplate(m, "T", "http://example.com/YourNamespaceURIGoesHere/");
    }

    /**
     * Creates a mapping object with a dummy "to" QName for each property in a model.
     * @param m Model object
     * @return new Mapping object
     */
    public static Mapping createTemplate (Model m, String defPrefix, String defURI) throws CMFException {
        var map   = new Mapping();
        for (var ns : m.namespaceList()) {
            if (ns.isModelNS())
                map.assignPrefix(ns.prefix(), ns.uri());
        }
        map.targetNSuri  = defURI;
        map.targetPrefix = defPrefix;   
        var num = 0;
        var spL = new ArrayList<>(m.propertyL());
        Collections.sort(spL);
        for (var p : spL) {
            if (p.namespace().isModelNS() && !p.isAbstract())
                map.addNameMapping(p.qname(), String.format("TEMP%04d", num++));
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
        var fmt = "@prefix %-" + maxLen + "s: <%s> .\n";
        for (var pre : prefixL) {
            if (nsmap.isReserved(pre)) continue;     // do not emit reserved prefixes
            w.write(String.format(fmt, pre, nsmap.getURI(pre)));
        }        
        var srcL = new ArrayList<>(qn2lname.keySet());
        maxLen   = 0;
        Collections.sort(srcL);
        for (var src : srcL) {
            maxLen = Math.max(maxLen, src.length());
        }
        fmt = "%-" + maxLen + "s " + owlPre + ":equivalentProperty %s:%s .\n";
        for (var srcQ : srcL) {
            var ln = qn2lname.get(srcQ);
            w.write(String.format(fmt, srcQ, targetPrefix, ln));
        }        
    }
    
    /**
     * Reads a mapping object from a File in RDF/Turtle format
     * @param f
     * @return
     * @throws IOException
     * @throws MappingException 
     */
    public static Mapping readFile (File f) throws IOException, CMFException {
        var rdr = new BufferedReader(new FileReader(f));
        return read(rdr);
    }
    
    private static final Pattern PREFIX_PAT = Pattern.compile(
        "^@prefix\\s+([A-Za-z0-9_\\-]+):\\s+<([^>]+)>\\s*\\.$");

    private static final String COMPACT_IRI = 
        "(?:<[^>]+>|[a-zA-Z][a-zA-Z0-9_-]*:[a-zA-Z0-9_.-]*)";

    public static final Pattern TURTLE_TRIPLE = Pattern.compile(
        "^\\s*" +
        "(?<subject>"   + COMPACT_IRI + ")\\s+" +
        "(?<predicate>" + COMPACT_IRI + ")\\s+" +
        "(?<object>"    + COMPACT_IRI + ")" +
        "\\s*\\.\\s*" +           // must end with .
        "(?:#.*)?$",              // optional comment
        Pattern.UNICODE_CHARACTER_CLASS
    );
    
    /**
     * Reads a mapping object from a reader in RDF/Turtle format.
     * @param r
     * @return
     * @throws IOException
     * @throws MappingException 
     */
    public static Mapping read (Reader r) throws IOException, CMFException {
        var map  = new Mapping();
        var lnum = 0;
        var line = "";
        var br   = new BufferedReader(r);
        while ((line = br.readLine()) != null) {
            lnum++;
            if (line.length() > 0 && '\uFEFF' == line.charAt(0)) {
                line = line.substring(1);
            }
            line.trim();
            if (line.isBlank()) continue;
            if (line.startsWith("#")) continue;
            var preM = PREFIX_PAT.matcher(line);
            if (preM.matches()) {
                map.assignPrefix(preM.group(1), preM.group(2));
                continue;
            }
            var owlP = map.nsmap.getPrefix(OWL_NS_URI);
            if (null == owlP) {
                throw new CMFException("Invalid mapping file (no @prefix for OWL namespace)");
            }
            var opred = makeQN(owlP, "equivalentProperty");
            var tripleM = TURTLE_TRIPLE.matcher(line);
            if (tripleM.matches()) {               
                var sub  = tripleM.group("subject");
                var pred = tripleM.group("predicate");
                var obj  = tripleM.group("object");
                if (!opred.equals(pred)) {
                    throw new CMFException(String.format(
                        "Invalid mapping file (bad predicate %s at line %d", pred, lnum));
                }
                map.addQNmapping(sub, obj);
            }
            else throw new CMFException(String.format(
                "Invalid mapping file (line %d is not a triple)", lnum));
        }
        return map;
    }
}
