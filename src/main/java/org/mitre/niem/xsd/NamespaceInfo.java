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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.apache.logging.log4j.LogManager;
import static org.mitre.niem.NIEMConstants.*;
import static org.mitre.niem.cmf.Namespace.mungedPrefix;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_APPINFO;
import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_STRUCTURES;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinNamespaceVersion;
import static org.mitre.niem.xsd.NIEMBuiltins.isBuiltinNamespace;
import static org.mitre.niem.xsd.NIEMBuiltins.orderedBuiltinURIs;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinNamespaceKind;

/**
 * A class for information about namespaces derived from parsing a schema 
 * document set. The class provides <ul>
 * 
 * <li> A set of all target namespace identifiers (generally one per schema document)</li>
 * <li> The documentation string for each namespace </li>
 * <li> The NIEM version of each namespace </li>
 * <li> The @version attribute value from each namespace </li>
 * <li> The preferred, unique prefix for each namespace </li>
 * <li> The conformance target assertions for each namespace </li>
 * <li> The type of schema (extension, NIEM model, builtin, external, unknown) </li>
 * <li> Attributes from the appinfo namespace (not available through Xerces XSD API </li>
 * <li> A list of warning messages about namespace declarations: </li><ul>
 *      <li> One prefix defined for multiple namespace URIs </li>
 *      <li> One namespace URI with multiple prefixes defined </li>
 *      <li> NIEM model namespace declared without its well-known prefix </li></ul></ul>
 * 
 * @author SAR
  * <a href="mailto:sar@mitre.org">sar@mitre.org</a>* 
 */

public class NamespaceInfo {   
    
    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(ModelFromXSD.class);    
       
    // The six kinds of namespace.  Note that some EXTERNAL namespaces may be
    // marked as UNKNOWN until all schema documents have been processed.  That
    // doesn't make a material difference in prefix assignment.
    public final static int NSK_EXTENSION  = 0;     // has conformance assertion, not in NIEM model
    public final static int NSK_NIEM_MODEL = 1;     // namespace starts with release or publication prefix
    public final static int NSK_BUILTIN    = 2;     // appinfo, code-lists, conformance, proxy, structures
    public final static int NSK_EXTERNAL   = 3;     // imported with appinfo:externalImportIndicator
    public final static int NSK_UNKNOWN    = 4;     // no conformance assertion
    public final static int NSK_XSD        = 5;     // namespace for XSD datatypes
    public final static int NSK_NUMKINDS   = 6;     // this many kinds of schemas

    static List<NSDeclRec> emptyList = new ArrayList<>();
    

    private Map<String,List<NSDeclRec>> pfmap = null;   // pfmap.get(P)=  list of decls with prefix P
    private Map<String,List<NSDeclRec>> urimap = null;  // urimap.get(U)= list of decls with URI U
    private List<String> warnMsgs = null;
    
    private final Set<String> targetNS             = new HashSet<>();       // set of target namespace identifers found
    private final List<NSDeclRec> maps             = new ArrayList<>();     // array of namespace declaration records
    private final Map<String,String> nsCtargs      = new HashMap<>();       // nsURI -> conformance targets
    private final Map<String,String> nsDoc         = new HashMap<>();       // nsURI -> schema documentation string
    private final Map<String,String> nsNIEMVersion = new HashMap<>();       // nsURI -> NIEM version string
    private final Map<String,Integer> nsType       = new HashMap<>();       // nsURI -> type (extension, niem model, ...)
    private final Map<String,String> nsVersion     = new HashMap<>();       // nsURI -> schema version attribute
    private final Set<String> importedExternal     = new HashSet<>();       // nsURI of schemas imported with appinfo:externalImportIndicator
    private final List<AppinfoRec> appinfo         = new ArrayList<>();     // list of appinfo attributes encountered
    
    private Map<String,String> nsPrefix            = null;                  // nsURI -> unique preferred prefix string
    private Map<String,String> prefixURI           = null;                  // ns prefix -> nsURI
    
    public NamespaceInfo () {
    }
    
    public Set<String> getTargetNS ()                   { return targetNS; }
    public List<AppinfoRec> getAppinfo()                { return appinfo; }
    public String getConformanceTargets (String nsuri)  { return nsCtargs.get(nsuri); }
    public String getDocumentation (String nsuri)       { return nsDoc.get(nsuri); }
    public String getNIEMVersion (String nsuri)         { return nsNIEMVersion.get(nsuri); }
    public int getNSType (String nsuri)                 { return nsType.getOrDefault(nsuri, NSK_UNKNOWN); }
    public String getNSVersion (String nsuri)           { return nsVersion.get(nsuri); }


    
    /**
     * Returns the preferred prefix for this namespace, guaranteed to be unique
     * for all namespaces in the schema document set. This is complicated. This
     * code assigns a unique prefix to each namespace defined in the schema pile.
     * But there's nothing to keep the schema documents from mapping the same prefix
     * to multiple namespace URIs. So we can't assign the prefixes until all of
     * the namespaces have been processed. Conflicts are resolved in the following
     * priority order: <ol>
     * 
     * <li> "rdf" is mapped to "http://www.w3.org/1999/02/22-rdf-syntax-ns#", period.</li>
     * <li> We assume the guy writing an extension schema knows what prefix he 
     *      wants to use, so these mappings are chosen first.</li>
     * <li> For namespaces in the NIEM model, we prefer the prefix mapped to
     *      the target namespace.</li>
     * <li> Everything else is an external namespace. Those guys choose last.</li></ol>
     * 
     * Within those categories, ties go first to the namespace declaration at the
     * highest level (or lowest depth) in the schema document.  If that's a tie,
     * the first declaration in document order wins.  In all cases, the loser 
     * gets a munged prefix; eg. "nc1:" instead of "nc:"
     * @param nsuri
     * @return 
     */
    public String getPrefix (String nsuri)  {
        buildPrefixMaps();
        return nsPrefix.get(nsuri);
    }
    
    public String getNamespaceFromPrefix (String prefix) {
        buildPrefixMaps();
        return prefixURI.get(prefix);
    }
    
    public Set<String> getAllNamespaceURIs () {
        buildPrefixMaps();
        return nsPrefix.keySet();
    }
    
    // Iterate through all the namespace mappings and assign a prefix to exactly
    // one namespace, in namespace priority order.  An amazing amount of complexity
    // to assign the unmunged prefix to the builtin with the highest version.
    private void buildPrefixMaps () {
        if (null != nsPrefix) return;
        nsPrefix  = new HashMap<>();
        prefixURI = new HashMap<>();
        
        // CMF reserves these namespace prefixes:  owl, rdf, rdfs, xs
        // You don't have to use them, but you can't assign them to anything else.
        nsPrefix.put(OWL_NS_URI, "owl");
        nsPrefix.put(RDF_NS_URI, "rdf");
        nsPrefix.put(RDFS_NS_URI, "rdfs");
        nsPrefix.put(XSD_NS_URI, "xs");
        prefixURI.put("owl", OWL_NS_URI);
        prefixURI.put("rdf", RDF_NS_URI);
        prefixURI.put("rdfs", RDFS_NS_URI);
        prefixURI.put("xs", XSD_NS_URI);
        
        nsType.put(XSD_NS_URI, NSK_XSD);
        
        // Mark all the known EXTERNAL namespaces now
        importedExternal.forEach((ens) -> {
            nsType.put(ens, NSK_EXTERNAL);
        });
        
        // Sort by namespace kind, and within namespaces
        // Then process namespaces by kind order: extension, model, builtin, external
        Collections.sort(maps);
        for (int nskind = 0; nskind < NSK_NUMKINDS; nskind++) {
            HashMap<String,String> builtinMap = new HashMap<>();    // ns -> prefix
            for (NSDeclRec nr : maps) {
                if (nskind != nr.schemaKind) continue;
                String p = nr.prefix;
                String u = nr.uri;
                if (nsPrefix.containsKey(u)) continue;
                if (isBuiltinNamespace(u)) builtinMap.put(u,p);     // pull out the builtin namespaces
                else {
                    p =  mungedPrefix(prefixURI, p);
                    nsPrefix.put(u, p);
                    prefixURI.put(p,u);
                }
            }
            // Sort builtins by version (highest first), assign prefixes
            List<String>buris = orderedBuiltinURIs(builtinMap.keySet());
            for (String u : buris) {
                if (nsPrefix.containsKey(u)) continue;
                String p = mungedPrefix(prefixURI, builtinMap.get(u));
                nsPrefix.put(u, p);
                prefixURI.put(p, u);
            }
        }
    }  
    
    // Returns a list of all the URIs from all the namespace declarations
    // found in the schema document for the specified namespace
    public List<String> getAllNamespacesDeclared (String nsuri) {
        List<String>res = new ArrayList<>();
        for (NSDeclRec rec : maps) {
            if (rec.targetNS.equals(nsuri))
                res.add(rec.uri);
        }
        return res;
    }
    
    // Look through the prefix mappings and return the prefix for the latest
    // (that is, last in sort order) namespace that begins with uriPrefix.
    public String getLatestVersionPrefix (String uriPrefix) {
        String p = getPrefix(uriPrefix);
        if (null != p) return p;        // exact match
        String last = "";
        for (String ns : nsPrefix.keySet()) {
            if (ns.startsWith(uriPrefix) && ns.compareTo(last) > 0) {
                last = ns;
                p = nsPrefix.get(ns);
            }
        }
        return p;
    }
    
    /**
     * Adds all of the namespace declarations in the specified schema document.
     * Extracts the /schema/annotation/documentation element from the 
     * namespace document.  Extracts the NIEM version from the conformance
     * assertion in the schema document.  If you have already asked for the 
     * prefix of any namespace, that prefix is now potentially invalidated,
     * so beware.
     * @param furi  file URI of schema document location
     */
    public void processSchemaDocument (String furi) {
        if (null == furi) return;
        try {
            SAXParser saxp = ParserBootstrap.sax2Parser();
            XSDHandler h = new XSDHandler(this);
            saxp.parse(furi, h);
            nsPrefix = null;        // invalidate all old mappings
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            LOG.error("Error processing schema document {}: {}", furi, ex.getMessage());
        }
    }

    private class XSDHandler extends DefaultHandler {
        private final NamespaceInfo nsd;       // object for tracking ns decls
        
        private String nsuri;                   // target namespace URI of document being parsed
        private String ctargs = null;           // conformance targets
        private String nsVersion = null;        // schema element version attribute
        private String docStr = null;           // value of first documentation element in first annotation element
        private String ctypeName = null;        // local name of current complexType definition (or null)
        private int nskind = NSK_UNKNOWN;       // is this an extension, niem model, builtin, or external schema ns?
        private int declCt = 0;                 // number of declarations in schema document
        private int depth = 0;
        private StringBuilder chars = new StringBuilder();
        private Locator loc;
        
        XSDHandler (NamespaceInfo n) { 
            nsd = n;
        }
        
        @Override
        public void startPrefixMapping (String prefix, String uri) {
            if (prefix.isEmpty()) return;
            String fn = loc.getSystemId();
            int ln = loc.getLineNumber();
            maps.add(new NSDeclRec(prefix, uri, nsuri, fn, ln, depth));      
            declCt++;
        }
        
        @Override
        public void startElement(String ns, String ln, String qn, Attributes atts) {
            if ("schema".equals(ln)) {  
                nsVersion = atts.getValue("version");
                nsuri = atts.getValue("targetNamespace");
                if (null == nsuri) nsuri = "noTargetNamespace";
                      
                // Get NIEM version from conformance assertion
                for (int i = 0; i < atts.getLength(); i++) {                 
                    if (atts.getURI(i).startsWith(CONFORMANCE_TARGET_NS_URI_PREFIX)) {
                        if (CONFORMANCE_ATTRIBUTE_NAME.equals(atts.getLocalName(i))) {
                            ctargs = atts.getValue(i);
                            if (nsuri.startsWith(NIEM_RELEASE_PREFIX) 
                                    || nsuri.startsWith(NIEM_PUBLICATION_PREFIX)) nskind = NSK_NIEM_MODEL;
                            else nskind = NSK_EXTENSION;
                        }
                    }
                }
            }
            else if ("import".equals(ln)) {
                String importNS = atts.getValue("namespace");
                for (int i = 0; i < atts.getLength(); i++) {
                    if (atts.getURI(i).startsWith(APPINFO_NS_URI_PREFIX)
                            && "externalImportIndicator".equals(atts.getLocalName(i))
                            && "true".equals(atts.getValue(i))) {
                        nsd.importedExternal.add(importNS);
                    }
                        
                }
            }
            // Look for appinfo attributes on global definitions and declarations
            else if (1 == depth) {
                if ("attribute".equals(ln) || "complexType".equals(ln) || "element".equals(ln)) {
                    String compName = atts.getValue("name");
                    for (int i = 0; i < atts.getLength(); i++) {
                        if (atts.getURI(i).startsWith(APPINFO_NS_URI_PREFIX))
                            nsd.appinfo.add(new AppinfoRec(nsuri, compName, null, atts.getLocalName(i), atts.getValue(i)));
                    }
                }
                if ("complexType".equals(ln)) ctypeName = atts.getValue("name");
            }
            depth++;
        }
        
        @Override
        public void endElement(String ns, String ln, String qn) {
            depth--;
            if (1 == depth && "complexType".equals(ln)) ctypeName = null;
            
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
            // Get the version for a builtin namespace from the namespace URI
            int msize = maps.size();
            String nvers = null;
            int whichBuiltin = getBuiltinNamespaceKind(nsuri);
            if (whichBuiltin >= 0) {
                nskind = NSK_BUILTIN;
                nvers = getBuiltinNamespaceVersion(nsuri);
            }
            // For other namespaces, look through all of their namespace declarations
            // to find one for proxy or structures.  The version of proxy or structures
            // declared is the NIEM version of the namespace.
            if (NSK_BUILTIN != nskind) {
                for (int i = msize - declCt; i < msize; i++) {
                    String uri = nsd.maps.get(i).uri;
                    whichBuiltin = getBuiltinNamespaceKind(uri);
                    if (NIEM_APPINFO == whichBuiltin || NIEM_STRUCTURES == whichBuiltin) {
                        nvers = getBuiltinNamespaceVersion(uri);
                    }
                }
            }
            if (null != nvers)  nsd.nsNIEMVersion.put(nsuri, nvers);
            if (null != ctargs) nsd.nsCtargs.put(nsuri, ctargs);
            if (null != docStr) nsd.nsDoc.put(nsuri, docStr);
            if (null != nsVersion) nsd.nsVersion.put(nsuri, nsVersion);
            nsType.put(nsuri, nskind);
            targetNS.add(nsuri);
            for (int i = msize - declCt; i < msize; i++) {
                nsd.maps.get(i).targetNS = nsuri;
                nsd.maps.get(i).schemaKind = nskind;
             }
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
        buildPrefixMaps();
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
        int schemaKind = 0;
        
        NSDeclRec (String p, String u, String pns, String f, int n, int lvl) {
            prefix = p;
            uri = u;
            targetNS = pns;
            fp = f;
            linenum =  n;
            nestLevel = lvl;
        }
        
        // This sort order determines the priority of namespace prefix assignment.
        // Schema document type comes first: extension, reference, builtin, external
        // Within a namespace, declarations in outer schema elements come before inner,
        // and then prefer earlier declarations before later.
        @Override
        public int compareTo (NSDeclRec o) {
            if (schemaKind < o.schemaKind) return -1; 
            else if (schemaKind > o.schemaKind) return 1; 
            else if (targetNS.equals(o.targetNS)) {
                if (nestLevel < o.nestLevel) return -1; 
                else if (nestLevel > o.nestLevel) return 1;
                if (linenum < o.linenum) return -1;
                else if (linenum > o.linenum) return 1;
            }
            return 0;
        }
        
        @Override
        public String toString () {
            return String.format(
                    "%-10.10s %-50.50s %4d %4d %4d in %s",
                    prefix, uri, linenum, nestLevel, schemaKind, targetNS
            );
        }
    }
    
    // A structure for recording an appinfo attribute
    public class AppinfoRec {
        String nsuri;           // namespace URI 
        String lname;           // top-level component with appinfo (complexType, element, attribute)
        String elementRef;      // element QName for appinfo within complexType
        String attribute;       // appinfo attribute name
        String value;           // appinfo attribute value
        
        AppinfoRec (String ns, String ln, String er, String an, String av) {
            nsuri = ns;
            lname = ln;
            elementRef = er;
            attribute = an;
            value = av;
        }
    }
}
