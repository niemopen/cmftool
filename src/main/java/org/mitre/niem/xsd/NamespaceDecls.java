/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2021 The MITRE Corporation.
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
package org.mitre.niem.xsd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.apache.logging.log4j.LogManager;
import static org.mitre.niem.NIEMConstants.CONFORMANCE_ATTRIBUTE_NAME;
import static org.mitre.niem.NIEMConstants.CONFORMANCE_TARGET_NS_URI_PREFIX;
import static org.mitre.niem.NIEMConstants.NDR_CT_URI_PREFIX;
import static org.mitre.niem.NIEMConstants.NIEM_RELEASE_PREFIX;
import static org.mitre.niem.NIEMConstants.RDF_NS_URI;
import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI_PREFIX;
import static org.mitre.niem.NIEMConstants.XSD_NS_URI;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A class for information about namespaces derived from parsing a schema 
 * document set. The class provides <ul>
 * 
 * <li> The documentation string for each namespace </li>
 * <li> The NIEM version of each namespace </li>
 * <li> The preferred, unique prefix for each namespace </li>
 * <li> A list of warning messages about namespace declarations: </li><ul>
 *      <li> One prefix defined for multiple namespace URIs </li>
 *      <li> One namespace URI with multiple prefixes defined </li>
 *      <li> NIEM model namespace declared without its well-known prefix </li></ul></ul>
 * 
 * @author SAR
  * <a href="mailto:sar@mitre.org">sar@mitre.org</a>* 
 */

public class NamespaceDecls {   
    
    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(ModelFromXSD.class);    
    
    public final static int SK_EXTENSION  = 0;
    public final static int SK_NIEM_MODEL = 1;
    public final static int SK_EXTERNAL   = 2;
    public final static int SK_STRUCTURES = 3;
    
    static List<NSDeclRec> emptyList = new ArrayList<>();
    

    private Map<String,List<NSDeclRec>> pfmap = null;   // pfmap.get(P)=  list of decls with prefix P
    private Map<String,List<NSDeclRec>> urimap = null;  // urimap.get(U)= list of decls with URI U
    private List<String> warnMsgs = null;
    
    /////////////////
    private final List<NSDeclRec> maps         = new ArrayList<>();   // array of namespace declaration records
    private final Map<String,Integer> nsType   = new HashMap<>();     // nsURI -> type (extension, niem model, ...)
    private final Map<String,String> nsVersion = new HashMap<>();     // nsURI -> NIEM version string
    private final Map<String,String> nsDoc     = new HashMap<>();     // nsURI -> schema documentation string
    private Map<String,String> nsPrefix        = null;                // nsURI -> unique preferred prefix string
    private Map<String,String> prefixURI       = null;                // ns prefix -> nsURI
    
    NamespaceDecls () {
    }
    
    public int getNSType (String nsuri)           { return nsType.get(nsuri); }
    public String getNSVersion (String nsuri)     { return nsVersion.get(nsuri); }
    public String getDocumentation (String nsuri) { return nsDoc.get(nsuri); }
    
    /**
     * Returns the preferred prefix for this namespace, guaranteed to be unique
     * for all namespaces in the schema document set. This is complicated. Each
     * namespace defined in the schema pile is assigned a unique prefix. But
     * there's nothing to keep the schema documents from mapping the same prefix
     * to multiple namespace URIs. So we can't assign the prefixes until all of
     * the namespaces have been processed. Conflicts are resolved in the following
     * priority order: <ol>
     * 
     * <li> We assume the guy writing an extension schema knows what prefix he 
     *      wants to use, so these mappings are chosen first.</li>
     * <li> For namespaces in the NIEM model, we prefer the prefix mapped to
     *      the target namespace.</li>
     * <li> Everything else is an external namespace. Those guys choose last.</li></ol>
     * 
     * Within those categories, ties go first to the namespace declaration at the
     * highest level (or lowest depth) in the schema document.  If that's a tie,
     * the first declaration in document order wins.  The loser gets a munged
     * prefix; eg. "nc1:" instead of "nc:"
     * @param nsuri
     * @return 
     */
    public String getPrefix (String nsuri)  {
        if (null != nsPrefix) return nsPrefix.get(nsuri);
        nsPrefix  = new HashMap<>();
        prefixURI = new HashMap<>();
        Collections.sort(maps);       
        for (NSDeclRec nr : maps) {
            if (!nsPrefix.containsKey(nr.uri)) {
                String oprefix = nr.prefix;
                String prefix = oprefix;
                int ctr = 1;
                // if the preferred prefix is already claimed, mung until we find an unclaimed prefix
                while (prefixURI.containsKey(prefix)) {
                    prefix = String.format("%s%d", oprefix, ctr++);
                }
                nsPrefix.put(nr.uri, prefix);
                prefixURI.put(prefix, nr.uri);
            }
        }
        return nsPrefix.get(nsuri);
    }  
    
    /**
     * Adds all of the namespace declarations in the specified namespace.
     * Extracts the /schema/annotation/documentation element from the 
     * namespace document.  Extracts the NIEM version from the conformance
     * assertion in the schema document.  If you have already asked for the 
     * prefix of any namespace, that prefix is now potentially invalidated,
     * so beware.
     * @param nsuri target namespace URI of schema document
     * @param furi  file URI of schema document location
     */
    public void processNamespace (String nsuri, String furi) {
        if (null == furi) return;
        try {
            SAXParser saxp = ParserBootstrap.sax2Parser();
            XSDHandler h = new XSDHandler(this, nsuri);
            saxp.parse(furi, h);
            nsPrefix = null;        // invalidate all old mappings
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            LOG.error("Processing namespace {} ({}: {}", nsuri, furi, ex.getMessage());
        }
    }

    private class XSDHandler extends DefaultHandler {
        private final NamespaceDecls nsd;       // object for tracking ns decls
        private final String nsuri;             // target namespace URI of document being parsed

        private String niemVersion = "";        // NIEM version of schema document
        private String docStr = null;           // value of first documentation element in first annotation element
        private int skind = SK_EXTERNAL;        // is this an extension, niem model, or external schema ns?
        private int declCt = 0;                 // number of declarations in schema document
        private int depth = 0;
        private StringBuilder chars = new StringBuilder();
        private Locator loc;
        
        XSDHandler (NamespaceDecls n, String nsu) { 
            nsd = n;
            nsuri = nsu;
        }
        
        @Override
        public void startPrefixMapping (String prefix, String uri) {
            if (prefix.isEmpty()) return;
            String fn = loc.getSystemId();
            int ln = loc.getLineNumber();
            maps.add(new NSDeclRec(prefix, uri, nsuri, fn, ln, depth, declCt));      
            declCt++;
        }
        
        @Override
        public void startElement(String ns, String ln, String qn, Attributes atts) {
            if (0 == depth) {   // <schema>          
                // Get NIEM version from conformance assertion
                for (int i = 0; i < atts.getLength(); i++) {
                    if (atts.getURI(i).startsWith(CONFORMANCE_TARGET_NS_URI_PREFIX)) {
                        if (CONFORMANCE_ATTRIBUTE_NAME.equals(atts.getLocalName(i))) {
                            if (nsuri.startsWith(NIEM_RELEASE_PREFIX)) skind = SK_NIEM_MODEL;
                            else skind = SK_EXTENSION;
                            for (String ctv : atts.getValue(i).split("\\s+")) {
                                if (ctv.startsWith(NDR_CT_URI_PREFIX)) {
                                    ctv = ctv.substring(NDR_CT_URI_PREFIX.length());
                                    int sp = ctv.indexOf('/');
                                    if (sp >= 0) {
                                        niemVersion = ctv.substring(0, sp);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            depth++;
        }
        
        @Override
        public void endElement(String ns, String ln, String qn) {
            depth--;
            // The first <documentation> element in document order at depth 2 
            // has to be the schema documentation string
            if (2 == depth) {
                if (null == docStr && XSD_NS_URI.equals(ns) && "documentation".equals(ln)) {
                    docStr = chars.toString().trim();
                }
            }
            if (chars.length() > 0) chars = new StringBuilder();
        }
        
        @Override
        public void endDocument () {
            nsd.nsDoc.put(nsuri, docStr);
            nsd.nsVersion.put(nsuri, niemVersion);
            int msize = maps.size();
            if (nsuri.startsWith(STRUCTURES_NS_URI_PREFIX)) skind = SK_STRUCTURES;
            nsType.put(nsuri, skind);
            for (int i = msize - declCt; i < msize; i++) nsd.maps.get(i).schemaKind = skind;
        }
        
        @Override
        public void characters (char[] ch, int start, int length) {
            chars.append(ch, start, length);
        }
                
        @Override
        public void setDocumentLocator (Locator l) {
            loc = l;
        }
    }    

    
    /**
     * Constructs and returns various warnings based on the set of 
     * namespace declarations found in a schema document set.
     * @return 
     */
    public List<String> nsDeclWarnings () {
        if (warnMsgs != null) { 
            return warnMsgs; 
        }
        warnMsgs = new ArrayList<>();
        createIndex();
        pfmap.keySet().stream().sorted().forEach((p) -> {
            List<NSDeclRec> ml = pfmap.get(p);
            long mapct = ml.stream().map(m -> m.uri).distinct().count();
            if (mapct > 1) {
                warnMsgs.add(String.format("prefix \"%s\" mapped to multiple URIs:\n", p));
                ml.forEach((m) -> {
                    warnMsgs.add(String.format("  to \"%s\" at *%s:%d\n", m.uri, m.fp, m.linenum));
                });
            }
        });
        urimap.keySet().stream().sorted().forEach((u) -> {
            List<NSDeclRec> ml = urimap.get(u);
            long mapct = ml.stream().map(m -> m.prefix).distinct().count();
            if (mapct > 1) {
                warnMsgs.add(String.format("uri \"%s\" mapped to multiple prefixes:\n", u));
                ml.forEach((m) -> {
                    warnMsgs.add(String.format("  to \"%s\" at *%s:%d\n", m.prefix, m.fp, m.linenum));
                });
            }
        });
//        ContextMap cmap = ContextMap.getInstance();
//        urimap.keySet().stream().sorted().forEach((u) -> {
//            String niemPrefix = cmap.wellKnownPrefix(u);
//            if (niemPrefix != null) {
//                List<NSDeclRec> ml = urimap.get(u);
//                long nonstd = ml.stream().filter(m -> !m.prefix.equals(niemPrefix)).count();
//                if (nonstd > 0) {
//                    warnMsgs.add(String.format("NIEM namespace \"%s\" bound to non-standard prefix:\n", u));
//                    ml.forEach((m) -> {
//                        if (!niemPrefix.equals(m.prefix)) {
//                            warnMsgs.add(String.format("  to \"%s\" at *%s:%d\n", m.prefix, m.fp, m.linenum));
//                        }
//                    });
//                }
//            }
//        });
        String hdr = "Well-known \"rdf\" prefix bound to non-standard namespace URI:\n";
        for (NSDeclRec m: pfmap.getOrDefault("rdf", emptyList)) {
            if (!RDF_NS_URI.equals(m.uri)) {
                if (hdr != null) warnMsgs.add(hdr);
                warnMsgs.add(String.format("  to \"%s\" at *%s:%d\n", m.uri, m.fp, m.linenum));
                hdr = null;
            }
        }
        hdr = "RDF namespace URI bound to non-standard prefix:\n";
        for (NSDeclRec m: urimap.getOrDefault(RDF_NS_URI, emptyList)) {
            if (!"rdf".equals(m.prefix)) {
                if (hdr != null) warnMsgs.add(hdr);
                warnMsgs.add(String.format("  to \"%s\" at *%s:%d\n", m.prefix, m.fp, m.linenum));
                hdr = null;
            }
        }
        return warnMsgs;
    }
    
    public Map<String,List<NSDeclRec>> prefixDecls () {
        createIndex();
        return pfmap;
    }
    
    public List<NSDeclRec> nsDecls () {
        createIndex();
        return maps;
    }

    private void createIndex () {
        if (pfmap != null) { return; }
        pfmap  = new HashMap<>();
        urimap = new HashMap<>();
        maps.forEach((m) -> {
            if (pfmap.get(m.prefix) == null) { pfmap.put(m.prefix, new ArrayList<>()); }
            if (urimap.get(m.uri) == null)   { urimap.put(m.uri, new ArrayList<>()); }
            pfmap.get(m.prefix).add(m);
            urimap.get(m.uri).add(m);
        });
        // Sort the namespace declarations into priority order for prefix mapping
        // First, declarations in extension schemas (assume designers know what they want)
        // Second, declarations in NIEM reference schemas (prefer well-known prefixes)
        // Last, declarations in external schemas.
        // For declarations in the same namespace, those from outer elements before inner.
        // Break ties by retaining the order in which declarations were found.
        Collections.sort(maps);
    }
       
    //
    // A structure for recording a namespace declaration
    //
    class NSDeclRec implements Comparable<NSDeclRec> {
        String prefix;          // xmlns:prefix="uri"
        String uri;             // xmlns:prefix="uri"
        String targetNS;        // target namespace of document containing this declaration
        String fp;              // path of schema document containing this declaration
        int linenum;            // line number
        int nestLevel;          // depth of this element in schema document
        int decNum;             // nth declaration found in schema document
        int schemaKind = 0;
        
        NSDeclRec (String p, String u, String pns, String f, int n, int lvl, int ct) {
            prefix = p;
            uri = u;
            targetNS = pns;
            fp = f;
            linenum =  n;
            nestLevel = lvl;
            decNum = ct;
        }
        
        // Schema document type comes first: extension, reference, external
        // Within a namespace, declarations in outer schema elements come before inner
        // For namespaces of sort priority, sort in order of parsing
        @Override
        public int compareTo (NSDeclRec o) {
            if (schemaKind < o.schemaKind) { return -1; }
            else if (schemaKind > o.schemaKind) { return 1; }
            else if (targetNS.equals(o.targetNS)) {
                if (nestLevel < o.nestLevel) { return -1; }
                else { return 1; }
            }
            else if (decNum < o.decNum) { return -1; }
            return 1;
        }
        
        @Override
        public String toString () {
            return String.format(
                    "%-8.8s %-40.40s %4d %4d %4d %4d",
                    prefix, uri, linenum, nestLevel, decNum, schemaKind
            );
        }
    }
}
