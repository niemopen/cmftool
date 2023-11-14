/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2022 The MITRE Corporation.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.javatuples.Triplet;

/**
 * A class to determine facts about a CMF namespace from a namespace URI
 * (and possibly also from parsing a schema document pile).
 * Or to generate a namespace URI from known facts.
 * Those facts are: architecture, kind, builtin, and version.
 * 
 * Architecture is about the differences in model XSD documents
 * for NIEM2, NIEM5, NIEM6, and NCDF.  CMF is the same, but the XSD is 
 * different.  This is something we'll need later.
 * 
 * Kind: Extension, Domain, Core, OtherNIEM, Utility, XSD, XML, External, Unknown
 * This is determined from
 * - target namespace URI gives you Domain, Core, OtherNIEM, Utility, XSD, and XML
 * - conformance target assertion gives you External (must parse schema document)
 * - appinfo:externalImportIndicator gives you External (must parse whole pile)
 * 
 * Builtin: appinfo, cli, clsa, ct, niem-xs, structures
 * Determined by namespace URI
 * Note that while CLI is a builtin, it has kind == OtherNIEM
 *
 * Version: The NIEM version for this namespace
 * Determined by namespace URI (for builtins) or from conformance target assertion
 *
 * Everything is static. If you need to work with two models or schema piles at
 * once, this must be rewritten.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NamespaceKind {
    
    static final Logger LOG = LogManager.getLogger(NamespaceKind.class);
      
    // The ten kinds of namespaces in CMF.  Order is signficant, because it controls
    // priority when normalizing namespace prefix assignment: extension, niem-model, builtin,
    // XSD, XML, external, unknown.
    //
    // Namespace kinds EXTENSION and EXTERNAL cannot be determined without parsing all the
    // schema documents, because they depend on a conformance assertion or an xs:import
    // with appinfo.  The others can be determined from the namespaced URI alone.
    // So a namespace kind UNKNOWN may change after all the documents are parsed.
    
    public final static int NSK_EXTENSION  = 0;     // has conformance assertion, not in NIEM model
    public final static int NSK_DOMAIN     = 1;     // domain schema
    public final static int NSK_CORE       = 2;     // niem core schema
    public final static int NSK_OTHERNIEM  = 3;     // code-lists-instance, or namespace URI starts with a model prefix 
    public final static int NSK_UTILITY    = 4;     // appinfo, code-lists-schema-attributes, conformance, proxy, structures
    public final static int NSK_XSD        = 5;     // namespace for XSD datatypes
    public final static int NSK_XML        = 6;     // namespace for xml: attributes
    public final static int NSK_EXTERNAL   = 7;     // imported with appinfo:externalImportIndicator
    public final static int NSK_UNKNOWN    = 8;     // haven't checked for conformance assertion or external appinfo yet
    public final static int NSK_NOTNIEM    = 9;     // none of the above; no conformance assertion or external appinfo
    public final static int NSK_NUMKINDS   = 10;    // this many kinds of namespaces   
    
    private final static Map<String,Integer> nscode2kind = Map.of(
            "EXTENSION", NSK_EXTENSION,
            "DOMAIN",    NSK_DOMAIN,
            "CORE",      NSK_CORE,
            "OTHERNIEM", NSK_OTHERNIEM,
            "UTILITY",   NSK_UTILITY,
            "XSD",       NSK_XSD,
            "XML",       NSK_XML,
            "EXTERNAL",  NSK_EXTERNAL,
            "UNKNOWN",   NSK_UNKNOWN,
            "NOTNIEM",   NSK_NOTNIEM
    );
    private static final String[] nskind2code = { 
            "EXTENSION",    // NSK_EXTENSION
            "DOMAIN",       // NSK_DOMAIN
            "CORE",         // NSK_CORE
            "OTHERNIEM",    // NSK_OTHERNIEM
            "UTILITY",      // NSK_UTILITY
            "XSD",          // NSK_XSD
            "XML",          // NSK_XML
            "EXTERNAL",     // NSK_EXTERNAL
            "UNKNOWN",      // NSK_UNKNOWN
            "NOTNIEM"       // NSK_NOTNIEM
    };
    private final static boolean[] nskindInCMF = { 
            true,           // NSK_EXTENSION
            true,           // NSK_DOMAIN
            true,           // NSK_CORE
            true,           // NSK_OTHERNIEM
            false,          // NSK_UTILITY
            true,           // NSK_XSD
            true,           // NSK_XML
            true,           // NSK_EXTERNAL
            false,          // NSK_UNKNOWN
            false           // NSK_NOTNIEM
    };

    // The seven kinds of builtin namespace.  Note that while code-lists-instance is 
    // a builtin namespace, it has kind NSK_OTHERNIEM (not NSK_UTITLTY), because 
    // it defines components that belong in a CMF model. The xml namespace isn't
    // defined by NIEM, but we treat it as a builtin anyway.
    // The code for a builtin namespace is also the preferred namespace prefix.
    public final static int NIEM_APPINFO = 0;
    public final static int NIEM_CLI = 1;
    public final static int NIEM_CLSA = 2;
    public final static int NIEM_CTAS = 3;
    public final static int NIEM_PROXY = 4;
    public final static int NIEM_STRUCTURES = 5;
    public final static int NIEM_XML = 6;
    public final static int NIEM_NOTBUILTIN = 7;
    public final static int NIEM_BUILTIN_COUNT = 8;
    
    private final static Map<String,Integer> nscode2builtin = Map.of(
        "appinfo",    NIEM_APPINFO,
        "cli",        NIEM_CLI,
        "clsa",       NIEM_CLSA,
        "ct",         NIEM_CTAS,
        "niem-xs",    NIEM_PROXY,
        "structures", NIEM_STRUCTURES,
        "xml",        NIEM_XML,
        "notbuiltin", NIEM_NOTBUILTIN
    );
    private final static String[] nsbuiltin2code = {
        "appinfo",      // NIEM_APPINFO,
        "cli",          // NIEM_CLI,
        "clsa",         // NIEM_CLSA,
        "ct",           // NIEM_CTAS,
        "niem-xs",      // NIEM_PROXY,
        "structures",   // NIEM_STRUCTURES,
        "xml",          // NIEM_XML,
        "notbuiltin",   // NIEM_NOTBUILTIN 
    };
    // Default schema document filenames are properly not part of CMF, but it's
    // way too much trouble to put them anywhere else.
    private final static String[] defBuiltinPath = {
        "utility/appinfo.xsd",                      //NIEM_APPINFO,
        "utility/code-lists-instance.xsd",          // NIEM_CLI,
        "utility/code-lists-schema-appinfo.xsd",    // NIEM_CLSA,
        "utility/conformanceTargets.xsd",           // NIEM_CTAS,
        "adapters/niem-xs.xsd",                     // NIEM_PROXY,
        "utility/structures.xsd",                   // NIEM_STRUCTURES,
        "external/xml.xsd",                         // NIEM_XML,
        "notbuiltin",                               // NIEM_NOTBUILTIN     
    };

    // Definitions for all the builtin namespaces in all known versions
    // arch: how to generate XSD from CMF, different for NIEM 2, 3-5, 6, and NCDF
    // kind: of namespace
    // builtin: which builtin?
    // vers: this URI -> this NIEM version (can't tell from CT uri)
    private final static int BUILTIN_TAB_WIDTH = 5;
    private final static String[] builtinTab = { 
    // arch    kind         builtin      vers  uri
    // ----    ----         -------      ----  ---
      "NIEM6", "UTILITY",   "appinfo",    "6", "https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/",   
      "NIEM6", "OTHERNIEM", "cli",        "6", "https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-instance/",
      "NIEM6", "UTILITY",   "clsa",       "6", "https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-schema-appinfo/",
      "NIEM6", "UTILITY",   "niem-xs" ,   "6", "https://docs.oasis-open.org/niemopen/ns/model/adapters/niem-xs/6.0/",
      "NIEM6", "UTILITY",   "structures", "6", "https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/",
      
      "NIEM5", "UTILITY",   "appinfo",    "5", "http://release.niem.gov/niem/appinfo/5.0/",   
      "NIEM5", "OTHERNIEM", "cli",        "5", "http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-instance/",
      "NIEM5", "UTILITY",   "clsa",       "5", "http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-schema-appinfo/",
      "NIEM5", "UTILITY",   "niem-xs",    "5", "http://release.niem.gov/niem/proxy/niem-xs/5.0/",
      "NIEM5", "UTILITY",   "structures", "5", "http://release.niem.gov/niem/structures/5.0/",

      "NIEM5", "UTILITY",   "appinfo",    "4", "http://release.niem.gov/niem/appinfo/4.0/",   
      "NIEM5", "OTHERNIEM", "cli",        "4", "http://reference.niem.gov/niem/specification/code-lists/4.0/code-lists-instance/",
      "NIEM5", "UTILITY",   "clsa",       "4", "http://reference.niem.gov/niem/specification/code-lists/4.0/code-lists-schema-appinfo/",
      "NIEM5", "UTILITY",   "niem-xs" ,   "4", "http://release.niem.gov/niem/proxy/xsd/4.0/",
      "NIEM5", "UTILITY",   "structures", "4", "http://release.niem.gov/niem/structures/4.0/",    
      
      "NIEM5", "UTILITY",   "appinfo",    "3", "http://release.niem.gov/niem/appinfo/3.0/",   
      "NIEM5", "OTHERNIEM", "cli",        "3", "http://reference.niem.gov/niem/specification/code-lists/3.0/code-lists-instance/",
      "NIEM5", "UTILITY",   "clsa",       "3", "http://reference.niem.gov/niem/specification/code-lists/3.0/code-lists-schema-appinfo/",
      "NIEM5", "UTILITY",   "niem-xs",    "3", "http://release.niem.gov/niem/proxy/xsd/3.0/",
      "NIEM5", "UTILITY",   "structures", "3", "http://release.niem.gov/niem/structures/3.0/",            
      
      // These must come at the end of the list
      "NIEM6", "UTILITY",   "ct",         "",  "https://docs.oasis-open.org/niemopen/ns/specification/conformanceTargets/6.0/",
      "NIEM5", "UTILITY",   "ct",         "",  "http://release.niem.gov/niem/conformanceTargets/3.0/",
    };
    
    // Patterns for recognizing NIEM model namespaces
    private final static String[] nspats = {
      "NIEM6", "DOMAIN",     "https://docs\\.oasis-open\\.org/niemopen/ns/model/domains/.*/[\\d]+([\\d.]+)/",
      "NIEM6", "CORE",       "https://docs\\.oasis-open\\.org/niemopen/ns/model/niem-core/[\\d]+([\\d.]+)/",
      "NIEM6", "OTHERNIEM",  "https://docs\\.oasis-open\\.org/niemopen/ns/model/.*/[\\d]+([\\d.]+)/",

      "NIEM5", "DOMAIN",     "http://((publication)|(release))\\.niem\\.gov/niem/domains/.*/[\\d]+([\\d.]+)/#?",
      "NIEM5", "CORE",       "http://((publication)|(release))\\.niem\\.gov/niem/niem-core/[\\d]+([\\d.]+)/#?",
      "NIEM5", "OTHERNIEM",  "http://((publication)|(release))\\.niem\\.gov/niem/.*/[\\d]+([\\d.]+)/#?",
    };

    // Patterns for recognizing architecture from conformance target assertions
    private final static String[] arches = {
        "NIEM6", "https://docs.oasis-open.org/niemopen/ns/specification/XNDR(?=/)",
        "NIEM5", "http://reference.niem.gov/niem/specification/naming-and-design-rules(?=/)"
    };
    
    // Compiled regexs for recognizing namespace URIs and architecture
    private static List<Triplet<String,Integer,Pattern>> uripat = null;     // built from nspats
    private static List<Pair<String,Pattern>> archpat = null;               // built from arches

    // Map NSURI -> its architecture, ns kind, utility kind (if any) and NIEM version (if known)
    // May be updated during schema parsing as CTAs and external namespaces are recognized
    private record NSuridat (String arch, int kind, int builtin, String version) {};
    private static Map<String,NSuridat> uridat = null; 
      
    public static void reset () {
        uridat = new HashMap<>();
        for (int i = 0; i < builtinTab.length; i += BUILTIN_TAB_WIDTH) {
            String arch = builtinTab[i];
            int kind    = namespaceCode2Kind(builtinTab[i+1]);
            int util    = namespaceCode2Builtin(builtinTab[i+2]);
            String vers = builtinTab[i+3];
            String uri  = builtinTab[i+4];
            var rec     = new NSuridat(arch, kind, util, vers);
            uridat.put(uri, rec);
        }
        uridat.put(XML_NS_URI, new NSuridat("", NSK_XML, NIEM_XML, ""));
        uridat.put(W3C_XML_SCHEMA_NS_URI, new NSuridat("", NSK_XSD, NIEM_NOTBUILTIN, ""));

        uripat = new ArrayList<>();
        for (int i = 0; i < nspats.length; i += 3) {
            try {
                int kind = namespaceCode2Kind(nspats[i + 1]);
                var upat = Pattern.compile(nspats[i + 2]);
                var rec = new Triplet<String,Integer,Pattern>(nspats[i], kind, upat);
                uripat.add(rec);
            }
            catch(Exception ex) {
                LOG.error(String.format("invalid regex '%s'", nspats[i+2]));
            }
        }
        archpat = new ArrayList<>();
        for (int i = 0; i < arches.length; i += 2) {
            try {
                var arch = arches[i];
                var apat = Pattern.compile(arches[i+1]);
                var rec  = new Pair<String,Pattern>(arch, apat);
                archpat.add(rec);
            }
            catch(Exception ex) {
                LOG.error(String.format("invalid regex '%s'", nspats[i+2]));
            }            
        }
    }
    
    private static NSuridat lookup (String nsuri) {
        if (null == uridat) reset();
        if (uridat.containsKey(nsuri)) {
            var rec = uridat.get(nsuri);
            return rec;
        }
        for (var prec : uripat) {
            Pattern p = prec.getValue2();
            Matcher m = p.matcher(nsuri);
            if (m.matches()) {
                var arch = prec.getValue0();
                int kind = prec.getValue1();
                var rec   = new NSuridat(arch, kind, NIEM_NOTBUILTIN, "");
                uridat.put(nsuri, rec);
                return rec;
            }
        }
        var rec = new NSuridat("", NSK_UNKNOWN, NIEM_NOTBUILTIN, "");
        uridat.put(nsuri, rec);
        return rec;
    }
    
    // Returns "" if NIEM architecture is unknown; never returns null
    public static String architecture (String nsuri) {
        var rec = lookup(nsuri);
        return rec.arch();
    }
    
    public static int kind (String nsuri) {
        var rec = lookup(nsuri);
        return rec.kind();
    }
    
    public static int builtin (String nsuri) {
        var rec = lookup(nsuri);
        return rec.builtin();
    }
    
    // Returns "" if NIEM version is unknown; never returns null
    public static String version (String nsuri) {
        var rec = lookup(nsuri);
        return rec.version();
    }

    public static void set (String nsuri, String arch, int kind, int builtin, String version) {
        if (null == arch) arch = "";
        if (null == version) version = "";
        var rec = lookup(nsuri);
        if (rec.arch.equals(arch) && rec.kind == kind && rec.builtin == builtin && rec.version.equals(version)) return;
        var nrec = new NSuridat(arch, kind, builtin, version);
        uridat.put(nsuri, nrec);
    }
    
    public static void setKind (String nsuri, int kind) {
        var rec = lookup(nsuri);
        if (rec.kind == kind) return;
        var nrec = new NSuridat(rec.arch, kind, rec.builtin, rec.version);
        uridat.put(nsuri, nrec);
    }
    
    public static void setArchitecture (String nsuri, String arch) {
        if (null == arch) arch = "";
        var rec = lookup(nsuri);
        if (rec.arch.equals(arch)) return;
        var nrec = new NSuridat(arch, rec.kind, rec.builtin, rec.version);
        uridat.put(nsuri, nrec);
    }
    
    // Determine architecture from conformance target assertions
    public static String archFromCTA (String cta) {
        for (var arec : archpat) {
            var arch = arec.getValue0();
            var apat = arec.getValue1();
            Matcher m = apat.matcher(cta);
            if (m.lookingAt()) return arch;
        }
        LOG.error(String.format("can't determine architecture from CTA '%s'", cta));
        return "";
    }
    
    // Determine NIEM version from conformance target assertions
    // Returns "" if version is unknown; never returns null
    private static final Pattern versPat = Pattern.compile("/(\\d)(\\.\\d+)?/");
    public static String versionFromCTA (String cta) {
        for (var arec : archpat) {
            var apat = arec.getValue1();
            Matcher m = apat.matcher(cta);
            if (m.lookingAt()) {
                String rest = cta.substring(m.end());
                Matcher vm = versPat.matcher(rest);
                if (vm.find()) return vm.group(1);
            }
        }
        return "";        
    }
   
    public static int namespaceCode2Kind (String code) {
        Integer kind = nscode2kind.get(code);
        if (null == kind) {
            LOG.error(String.format("invalid namespace kind code '%s'", code));
            return NSK_NOTNIEM;
        }
        return kind;
    }
    
    public static String namespaceKind2Code (int kind) { 
        if (kind < 0 || kind > NSK_NUMKINDS) {
            LOG.error(String.format("invalid namespace kind '%d'", kind));
            return "NOTNIEM";
        }
        return nskind2code[kind];
    }
    
    public static boolean isKindInCMF (int kind) {
        return (kind >= 0 && kind <= NSK_NUMKINDS ? nskindInCMF[kind] : false);
    }

    public static int namespaceCode2Builtin (String code) {
        Integer util = nscode2builtin.get(code);
        if (null == util) {
            LOG.error(String.format("invalid namespace kind code '%s'", code));
            return NSK_NOTNIEM;
        }
        return util;        
    }
    
    public static String namespaceUtil2Builtin (int util) { 
        if (util < 0 || util > NSK_NUMKINDS) {
            LOG.error(String.format("invalid namespace kind '%d'", util));
            return "NOTNIEM";
        }
        return nsbuiltin2code[util];
    }

    // Returns the namespace URI for a utility namespace given the architecture
    // and version.  
    public static String getBuiltinNS(int util, String arch, String version) {
        if (NIEM_XML == util) return XML_NS_URI;
        String ustr = namespaceUtil2Builtin(util);
        for (int i = 0; i < builtinTab.length; i += BUILTIN_TAB_WIDTH) {
            if (builtinTab[i + 0].equals(arch) 
                    && builtinTab[i + 2].equals(ustr) 
                    && (builtinTab[i + 3].equals(version) || builtinTab[i + 3].isBlank()))
                return builtinTab[i + 4];
          }
        LOG.error(String.format("could not find URI for '%s' version '%s'", ustr, version));
        return null;
    }
    
    public static String defaultBuiltinPath (int util) {
        if (util < 0 || util > NSK_NUMKINDS) {
            LOG.error(String.format("invalid namespace kind '%d'", util));
            return "NOTNIEM";
        }
        return defBuiltinPath[util];        
    }
}
