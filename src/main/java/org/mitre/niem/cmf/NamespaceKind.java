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
 * Those facts are: architecture, kind, utilityKind, and version.
 * 
 * Architecture is about the differences in model XSD documents
 * for NIEM2, NIEM5, NIEM6, and NCDF.  Something we'll need later.
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
    // Namespace kinds EXTENSION and EXTERNAL cannot be determined without parsing 
    // schema documents, because they depend on a conformance assertion or an xs:import
    // with appinfo.  The others can be determined from the namespaced URI alone.
    // So a namespace kind UNKNOWN may change after parsing.
    
    public final static int NSK_EXTENSION  = 0;     // has conformance assertion, not in NIEM model
    public final static int NSK_DOMAIN     = 1;     // domain schema
    public final static int NSK_CORE       = 2;     // niem core schema
    public final static int NSK_OTHERNIEM  = 3;     // other niem model; code-lists-instance, or starts with release or publication prefix
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

    // The six kinds of utility namespace.  Note that while code-lists-instance is 
    // a utility namespace, it has kind NSK_OTHERNIEM (not NSK_UTITLTY), because 
    // it defines components that belong in a CMF model.
    // The code for a utility namespace is also the preferred namespace prefix.
    public final static int NIEM_APPINFO = 0;
    public final static int NIEM_CLI = 1;
    public final static int NIEM_CLSA = 2;
    public final static int NIEM_CTAS = 3;
    public final static int NIEM_PROXY = 4;
    public final static int NIEM_STRUCTURES = 5;
    public final static int NIEM_NOTUTILITY = 6;
    public final static int NIEM_UTILITY_COUNT = 7;
    
    private final static Map<String,Integer> nscode2util = Map.of(
            "appinfo",    NIEM_APPINFO,
            "cli",        NIEM_CLI,
            "clsa",       NIEM_CLSA,
            "ct",         NIEM_CTAS,
            "xs-proxy",   NIEM_PROXY,
            "structures", NIEM_STRUCTURES,
            "notutility", NIEM_NOTUTILITY
    );
    private final static String[] nsutil2code = {
        "appinfo",      // NIEM_APPINFO,
        "cli",          // NIEM_CLI,
        "clsa",         // NIEM_CLSA,
        "ct",           // NIEM_CTAS,
        "xs-proxy",     // NIEM_PROXY,
        "structures",   // NIEM_STRUCTURES,
        "notutility",   // NIEM_NOTUTILITY        
    };
    // Default schema document filenames are property not part of CMF, but it's
    // way too much trouble to put them anywhere else.
    private final static String[] defUtilFN = {
        "appinfo.xsd",                      //NIEM_APPINFO,
        "code-lists-instance.xsd",          // NIEM_CLI,
        "code-lists-schema-appinfo.xsd",    // NIEM_CLSA,
        "conformanceTargets.xsd",           // NIEM_CTAS,
        "niem-xs.xsd",                      // NIEM_PROXY,
        "structures.xsd",                   // NIEM_STRUCTURES,
        "notutility",                       // NIEM_NOTUTILITY        
    };

    // Definitions for all the utility namespaces in all known versions
    private final static String[] utility = { 
      "NIEM6", "UTILITY",   "appinfo",    "6", "https://docs.oasis-open.org/niemopen/appinfo/6.0/#",   
      "NIEM6", "OTHERNIEM", "cli",        "6", "https://docs.oasis-open.org/niemopen/specification/code-lists/6.0/code-lists-instance/#",
      "NIEM6", "UTILITY",   "clsa",       "6", "https://docs.oasis-open.org/niemopen/specification/code-lists/6.0/code-lists-schema-appinfo/#",
      "NIEM6", "UTILITY",   "ct",         "6", "https://docs.oasis-open.org/niemopen/conformanceTargets/3.0/#",
      "NIEM6", "UTILITY",   "xs-proxy",   "6", "https://docs.oasis-open.org/niemopen/proxy/niem-xs/6.0/#",
      "NIEM6", "UTILITY",   "structures", "6", "https://docs.oasis-open.org/niemopen/structures/6.0/#",
      
      "NIEM5", "UTILITY",   "appinfo",    "5", "http://release.niem.gov/niem/appinfo/5.0/",   
      "NIEM5", "OTHERNIEM", "cli",        "5", "http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-instance/",
      "NIEM5", "UTILITY",   "clsa",       "5", "http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-schema-appinfo/",
      "NIEM5", "UTILITY",   "ct",         "5", "http://release.niem.gov/niem/conformanceTargets/3.0/",
      "NIEM5", "UTILITY",   "xs-proxy",   "5", "http://release.niem.gov/niem/proxy/niem-xs/5.0/",
      "NIEM5", "UTILITY",   "structures", "5", "http://release.niem.gov/niem/structures/5.0/",

      "NIEM5", "UTILITY",   "appinfo",    "4", "http://release.niem.gov/niem/appinfo/4.0/",   
      "NIEM5", "OTHERNIEM", "cli",        "4", "http://reference.niem.gov/niem/specification/code-lists/4.0/code-lists-instance/",
      "NIEM5", "UTILITY",   "clsa",       "4", "http://reference.niem.gov/niem/specification/code-lists/4.0/code-lists-schema-appinfo/",
      "NIEM5", "UTILITY",   "ct",         "4", "http://release.niem.gov/niem/conformanceTargets/3.0/",
      "NIEM5", "UTILITY",   "xs-proxy",   "4", "http://release.niem.gov/niem/proxy/niem-xs/4.0/",
      "NIEM5", "UTILITY",   "structures", "4", "http://release.niem.gov/niem/structures/4.0/",    
      
      "NIEM5", "UTILITY",   "appinfo",    "3", "http://release.niem.gov/niem/appinfo/3.0/",   
      "NIEM5", "OTHERNIEM", "cli",        "3", "http://reference.niem.gov/niem/specification/code-lists/3.0/code-lists-instance/",
      "NIEM5", "UTILITY",   "clsa",       "3", "http://reference.niem.gov/niem/specification/code-lists/3.0/code-lists-schema-appinfo/",
      "NIEM5", "UTILITY",   "ct",         "3", "http://release.niem.gov/niem/conformanceTargets/3.0/",
      "NIEM5", "UTILITY",   "xs-proxy",   "3", "http://release.niem.gov/niem/proxy/niem-xs3.0/",
      "NIEM5", "UTILITY",   "structures", "3", "http://release.niem.gov/niem/structures/3.0/"             
    };
    
    // Patterns for recognizing NIEM model namespaces
    private final static String[] nspats = {
      "NIEM6", "DOMAIN",     "https://docs\\.oasis-open\\.org/niemopen/domains/.*/[\\d]+([\\d.]+)/#?",
      "NIEM6", "CORE",       "https://docs\\.oasis-open\\.org/niemopen/niem-core/[\\d]+([\\d.]+)/#?",
      "NIEM6", "OTHERNIEM",  "https://docs\\.oasis-open\\.org/niemopen/.*/[\\d]+([\\d.]+)/#?",

      "NIEM5", "DOMAIN",     "http://((publication)|(release))\\.niem\\.gov/niem/domains/.*/[\\d]+([\\d.]+)/#?",
      "NIEM5", "CORE",       "http://((publication)|(release))\\.niem\\.gov/niem/niem-core/[\\d]+([\\d.]+)/#?",
      "NIEM5", "OTHERNIEM",  "http://((publication)|(release))\\.niem\\.gov/niem/.*/[\\d]+([\\d.]+)/#?",
    };

    // Patterns for recognizing architecture from conformance target assertions
    private final static String[] arches = {
        "NIEM6", "https://docs.oasis-open.org/niemopen/specification/naming-and-design-rules",
        "NIEM5", "http://reference.niem.gov/niem/specification/naming-and-design-rules"
    };
    
    private record NSuridat (String arch, int kind, int util, String version) {};
    private static Map<String,NSuridat> uridat = null; 
    private static List<Triplet<String,Integer,Pattern>> uripat = null;
    private static List<Pair<String,Pattern>> archpat = null;
    
    public static void reset () {
        uridat = new HashMap<>();
        for (int i = 0; i < utility.length; i += 5) {
            String arch = utility[i];
            int kind    = namespaceCode2Kind(utility[i+1]);
            int util    = namespaceCode2Util(utility[i+2]);
            String vers = utility[i+3];
            String uri  = utility[i+4];
            var rec     = new NSuridat(arch, kind, util, vers);
            uridat.put(uri, rec);
        }
        uridat.put(XML_NS_URI, new NSuridat("UNKNOWN", NSK_XML, NIEM_NOTUTILITY, ""));
        uridat.put(W3C_XML_SCHEMA_NS_URI, new NSuridat("UNKNOWN", NSK_XSD, NIEM_NOTUTILITY, ""));

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
                var rec   = new NSuridat(arch, kind, NIEM_NOTUTILITY, "");
                uridat.put(nsuri, rec);
                return rec;
            }
        }
        var rec = new NSuridat("UNKNOWN", NSK_UNKNOWN, NIEM_NOTUTILITY, "");
        uridat.put(nsuri, rec);
        return rec;
    }
    
    public static String architecture (String nsuri) {
        var rec = lookup(nsuri);
        return rec.arch();
    }
    
    public static int kind (String nsuri) {
        var rec = lookup(nsuri);
        return rec.kind();
    }
    
    public static int utilityKind (String nsuri) {
        var rec = lookup(nsuri);
        return rec.util();
    }
    
    public static String version (String nsuri) {
        var rec = lookup(nsuri);
        return rec.version();
    }

    public static void set (String nsuri, String arch, int kind, int util, String version) {
        var rec = lookup(nsuri);
        if (rec.arch.equals(arch) && rec.kind == kind && rec.util == util && rec.version.equals(version)) return;
        var nrec = new NSuridat(arch, kind, util, version);
        uridat.put(nsuri, nrec);
    }
    
    public static void setKind (String nsuri, int kind) {
        var rec = lookup(nsuri);
        if (rec.kind == kind) return;
        var nrec = new NSuridat(rec.arch, kind, rec.util, rec.version);
        uridat.put(nsuri, nrec);
    }
    
    public static void setArchitecture (String nsuri, String arch) {
        var rec = lookup(nsuri);
        if (rec.arch == arch) return;
        var nrec = new NSuridat(arch, rec.kind, rec.util, rec.version);
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
        return null;        
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

    public static int namespaceCode2Util (String code) {
        Integer util = nscode2util.get(code);
        if (null == util) {
            LOG.error(String.format("invalid namespace kind code '%s'", code));
            return NSK_NOTNIEM;
        }
        return util;        
    }
    
    public static String namespaceUtil2Code (int util) { 
        if (util < 0 || util > NSK_NUMKINDS) {
            LOG.error(String.format("invalid namespace kind '%d'", util));
            return "NOTNIEM";
        }
        return nsutil2code[util];
    }
      
    public static String getUtilityNS(int util, String version) {
        String ustr = namespaceUtil2Code(util);
        for (int i = 0; i < utility.length; i += 5) {
            if (utility[i + 2].equals(ustr) && utility[i + 3].equals(version))
                return utility[i + 4];
          }
        LOG.error(String.format("could not find URI for '%s' version '%s'", ustr, version));
        return null;
    }
    
    public static String defaultUtilityFN (int util) {
        if (util < 0 || util > NSK_NUMKINDS) {
            LOG.error(String.format("invalid namespace kind '%d'", util));
            return "NOTNIEM";
        }
        return defUtilFN[util];        
    }
}
