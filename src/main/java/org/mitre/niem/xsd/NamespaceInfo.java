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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.apache.logging.log4j.LogManager;
import static org.mitre.niem.NIEMConstants.*;
//import static org.mitre.niem.xsd.NIEMBuiltins.NIEM_STRUCTURES;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
//import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinKind;
//import static org.mitre.niem.xsd.NIEMBuiltins.isBuiltin;
//import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinVersion;

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
 * <li> A list of warning messages about namespace declarations: <ul>
 *      <li> One prefix defined for multiple namespace URIs </li>
 *      <li> One namespace URI with multiple prefixes defined </li>
 *      <li> NIEM model namespace declared without its well-known prefix </ul></li></ul>
 * 
 * @author SAR
  * <a href="mailto:sar@mitre.org">sar@mitre.org</a>* 
 */

public class NamespaceInfo {   
//    
//    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(NamespaceInfo.class);    
//    
//    private final Set<String> targetNamespaces     = new HashSet<>();       // set of target namespace identifers found
//    private final Map<String,NSRec> nsrecs         = new HashMap<>();       // nsURI -> namespace record
//    private final List<NSDeclRec> maps             = new ArrayList<>();     // array of namespace declaration records
//    private final Set<String> importedExternal     = new HashSet<>();       // nsURI of schemas imported with appinfo:externalImportIndicator
//    private final List<AppinfoRec> appinfo         = new ArrayList<>();     // list of appinfo attributes encountered
//    
//    private Map<String,String> nsPrefix            = null;                  // nsURI -> unique preferred prefix string
//    private Map<String,String> prefixURI           = null;                  // ns prefix -> nsURI
//    
//    public NamespaceInfo () {
//        // XSD namespace is always known
//        // This record harmlessly overwritten if XSD schema is imported
//        NSRec xsdr = new NSRec();
//        xsdr.skind = NSK_XSD;
//        nsrecs.put(XSD_NS_URI, xsdr);
//    }
//    
//    public Set<String> targetNamespaces ( )             { return targetNamespaces; }
//    public List<AppinfoRec> appinfoList ()              { return appinfo; }
//    public String getConformanceTargets (String nsuri)  { return getRec(nsuri).ctargs; }
//    public String getDocumentation (String nsuri)       { return getRec(nsuri).doc; }
//    public String getNIEMVersion (String nsuri)         { return getRec(nsuri).NIEMver; }
//    public int getNSKind (String nsuri)                 { return getRec(nsuri).skind; }
//    public String getNSVersion (String nsuri)           { return getRec(nsuri).sver; }
//    
//    private final NSRec emptyRec = new NSRec();
//    private NSRec getRec (String nsuri)          { return nsrecs.getOrDefault(nsuri, emptyRec); }
//    
//    // Returns a list of all the namespace declarations in the schema pile,
//    // sorted into priority order:
//    // 1. Declarations in an extension schema come first
//    // 2. Then declarations in the NIEM model (domain, core, other)
//    // 3. Then declarations in NIEM builtins (appinfo, cli, clsa, ct, proxy, structures)
//    // 4. Then declarations in external namespaces
//    // Within a schema document:
//    // 1. Declarations nested deep come after shallow.
//    // 2. Then declarations first in document before those later.
//    
//    List<NSDeclRec> getNSdecls () {
//        Collections.sort(maps);
////        for (var nr : maps) System.out.println(nr.toString());
//        return maps;
//    }
//    
//    // Returns a new list of all the URIs from all the namespace declarations
//    // found in the schema document for the specified namespace
//    public List<String> getAllNamespacesDeclared (String nsuri) {
//        List<String>res = new ArrayList<>();
//        for (NSDeclRec rec : maps) {
//            if (rec.targetNS.equals(nsuri))
//                res.add(rec.uri);
//        }
//        return res;
//    }
//    
//
//    // Parses a schema document:
//    // 1. Records all namespace declarations in the document
//    // 2. Records documentation in the first top-level annotation
//    // 3. Records the schema version (from @version attribute)
//    // 4. Finds conformance target assertions, if any
//    // 5. Determines the NIEM version (from conformance assertions, or builtin namespace
//    // 6. Determines the kind of namespace (extension, domain, core, etc.)
//    
//    void processSchemaDocument (String furi) {
//        if (null == furi) return;
//        try {
//            SAXParser saxp = ParserBootstrap.sax2Parser();
//            XSDHandler h = new XSDHandler(furi);
//            saxp.parse(furi, h);
//        } catch (SAXException | IOException | ParserConfigurationException ex) {
//            LOG.error("Error processing schema document {}: {}", furi, ex.getMessage());
//        }
//    }
//
//    private class XSDHandler extends DefaultHandler {     
//        private String furi;                    // document file URI (for log messages)
//        private String nsuri = null;            // target namespace URI of document being parsed
//        private NSRec nsr = new NSRec();        // properties of current namespace
//        private String ctypeName = null;        // local name of current complexType definition (or null)
//        private int declCt = 0;                 // number of declarations in schema document
//        private int depth = 0;                  // current element nest depth
//        private Locator loc;                    // line number in document
//        private StringBuilder chars = new StringBuilder();  // current element simple content
//
//        XSDHandler (String furi) { this.furi = furi; }
//        
//        @Override
//        public void startPrefixMapping (String prefix, String uri) {
//            if (prefix.isEmpty()) return;
//            String fn = loc.getSystemId();
//            int ln = loc.getLineNumber();
//            maps.add(new NSDeclRec(prefix, uri, nsuri, fn, ln, depth));      
//            declCt++;
//        }
//        
//        private static final Pattern niemCorePat   = Pattern.compile(NIEM_CORE_PATTERN);
//        private static final Pattern niemDomainPat = Pattern.compile(NIEM_DOMAIN_PATTERN);
//        
//        @Override
//        public void startElement(String ns, String ln, String qn, Attributes atts) {
//            if ("schema".equals(ln)) {  
//                nsr.sver = atts.getValue("version");
//                nsuri = atts.getValue("targetNamespace");
//                if (null == nsuri) {
//                    LOG.warn("No target namespace in {}", furi);
//                    nsuri = "noTargetNamespace";
//                }
//                if (isBuiltin(nsuri)) nsr.skind = NSK_BUILTIN;
//                else if (XML_NS_URI.equals(nsuri)) nsr.skind = NSK_XML;
//                else if (XSD_NS_URI.equals(nsuri)) nsr.skind = NSK_XSD;
//                else {     
//                    // Get NIEM version from conformance assertion
//                    for (int i = 0; i < atts.getLength(); i++) {                 
//                        if (atts.getURI(i).startsWith(CONFORMANCE_TARGET_NS_URI_PREFIX)) {
//                            if (CONFORMANCE_ATTRIBUTE_NAME.equals(atts.getLocalName(i))) {
//                                nsr.ctargs = atts.getValue(i);
//                                if (!nsuri.startsWith(NIEM_RELEASE_PREFIX) && !nsuri.startsWith(NIEM_PUBLICATION_PREFIX))
//                                    nsr.skind = NSK_EXTENSION;
//                                else {
//                                    Matcher coreM = niemCorePat.matcher(nsuri);
//                                    Matcher domM  = niemDomainPat.matcher(nsuri);
//                                    if (coreM.lookingAt())     nsr.skind = NSK_CORE;
//                                    else if (domM.lookingAt()) nsr.skind = NSK_DOMAIN;
//                                    else                       nsr.skind = NSK_OTHERNIEM;
//                                }
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//            else if ("import".equals(ln)) {
//                String importNS = atts.getValue("namespace");
//                for (int i = 0; i < atts.getLength(); i++) {
//                    if (atts.getURI(i).startsWith(APPINFO_NS_URI_PREFIX)
//                            && "externalImportIndicator".equals(atts.getLocalName(i))
//                            && "true".equals(atts.getValue(i))) {
//                        importedExternal.add(importNS);
//                    }
//                        
//                }
//            }
//            // Look for appinfo attributes on global definitions and declarations
//            else if (1 == depth) {
//                if ("attribute".equals(ln) || "complexType".equals(ln) || "element".equals(ln)) {
//                    String compName = atts.getValue("name");
//                    for (int i = 0; i < atts.getLength(); i++) {
//                        if (atts.getURI(i).startsWith(APPINFO_NS_URI_PREFIX))
//                            appinfo.add(new AppinfoRec(nsuri, compName, null, atts.getLocalName(i), atts.getValue(i)));
//                    }
//                }
//                if ("complexType".equals(ln)) ctypeName = atts.getValue("name");
//            }
//            depth++;
//        }
//        
//        @Override
//        public void endElement(String ns, String ln, String qn) {
//            depth--;
//            if (1 == depth && "complexType".equals(ln)) ctypeName = null;
//            
//            // The first <documentation> element in document order at depth 2 
//            // has to be the schema documentation string
//            if (2 == depth) {
//                if (null == nsr.doc && XSD_NS_URI.equals(ns) && "documentation".equals(ln)) {
//                    nsr.doc = chars.toString().trim();
//                }
//            }
//            if (chars.length() > 0) chars = new StringBuilder();
//        }
//        
//        @Override
//        public void endDocument () {
//            int msize = maps.size();
//            String nvers = null;
//            
//            // Get the version for a builtin namespace from the namespace URI
//            if (NSK_BUILTIN == nsr.skind) {
//                nvers = getBuiltinVersion(nsuri);
//            }
//            // For other namespaces, look through all of their namespace declarations
//            // to find the declaration for the structures namespace.  The version 
//            // of structures declared is the NIEM version of the namespace.
//            else {
//                for (int i = msize - declCt; i < msize; i++) {
//                    String uri = maps.get(i).uri;
//                    int whichBuiltin = getBuiltinKind(uri);
//                    if (NIEM_STRUCTURES == whichBuiltin) {
//                        nvers = getBuiltinVersion(uri);
//                        break;
//                    }
//                }
//            }
//            nsr.NIEMver = nvers;
//            targetNamespaces.add(nsuri);
//            nsrecs.put(nsuri, nsr);            
//            for (int i = msize - declCt; i < msize; i++) {
//                maps.get(i).targetNS = nsuri;
//                maps.get(i).sKind = nsr.skind;
//             }
//            // Recognize external namespaces at end of each schema document
//            // Kludgy, but it works
//            for (String uri : importedExternal) {
//                NSRec nr = nsrecs.get(uri);
//                if (null != nr) nr.skind = NSK_EXTERNAL;
//            }
//        }
//        
//        @Override
//        public void characters (char[] ch, int start, int length) {
//            chars.append(ch, start, length);
//        }
//                
//        @Override
//        public void setDocumentLocator (Locator l) {
//            loc = l;
//        }
//    }    
//
//       
//    //
//    // A structure for recording a namespace declaration
//    //
//    class NSDeclRec implements Comparable<NSDeclRec> {
//        String prefix;           // xmlns:prefix="uri"
//        String uri;              // xmlns:prefix="uri"
//        String targetNS;         // target namespace of document containing this declaration
//        String fp;               // path of schema document containing this declaration
//        int linenum;             // line number of declaration in schema document
//        int nestLevel;           // depth of this element in schema document
//        int sKind = NSK_UNKNOWN; // kind of schema document containing this declaration
//        
//        NSDeclRec (String p, String u, String pns, String f, int n, int lvl) {
//            prefix = p;
//            uri = u;
//            targetNS = pns;
//            fp = f;
//            linenum =  n;
//            nestLevel = lvl;
//        }
//        
//        // This sort order determines the priority of namespace prefix assignment.
//        // Schema document type comes first: extension, reference, builtin, external
//        // Within a namespace, prefer declarations in outer schema elements,
//        // and then prefer earlier declarations before later.
//        @Override
//        public int compareTo (NSDeclRec o) {
//            if (sKind < o.sKind) return -1; 
//            else if (sKind > o.sKind) return 1; 
//            else if (targetNS.equals(o.targetNS)) {
//                if (nestLevel < o.nestLevel) return -1; 
//                else if (nestLevel > o.nestLevel) return 1;
//                if (linenum < o.linenum) return -1;
//                else if (linenum > o.linenum) return 1;
//            }
//            return 0;
//        }
//        
//        @Override
//        public String toString () {
//            return String.format("%-10.10s %-50.50s %4d %4d %8s in %s",
//                    prefix, uri, linenum, nestLevel, kindCode(sKind), targetNS
//            );
//        }
//    }
//    
//    // A record for a namespace
//    class NSRec {
//        String ctargs = null;
//        String doc = null;
//        String NIEMver = null;
//        String sver = null;
//        int skind = NSK_UNKNOWN;
//    }
//    
//    // A structure for recording an appinfo attribute
//    class AppinfoRec {
//        String nsuri;           // namespace URI 
//        String lname;           // top-level component with appinfo (complexType, element, attribute)
//        String elementRef;      // element QName for appinfo within complexType
//        String attribute;       // appinfo attribute name
//        String value;           // appinfo attribute value
//        
//        AppinfoRec (String ns, String ln, String er, String an, String av) {
//            nsuri = ns;
//            lname = ln;
//            elementRef = er;
//            attribute = an;
//            value = av;
//        }
//    }
//    
//    static String mungedPrefix (Map<String,String>pm, String op) {
//       if (!pm.containsKey(op)) return op;
//       int count = 0;
//       String pat = op.replaceFirst("_\\d+$", "");    // remove existing suffix if any
//       pat = pat + "_%d";
//       while (pm.containsKey(op)) {
//           op = String.format(pat, ++count);
//       }
//       return op;        
//    }
}
