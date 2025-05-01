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
package org.mitre.niem.xsd;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.xsd.NIEMConstants.CTAS30;
import static org.mitre.niem.xsd.NIEMConstants.CTAS60;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NamespaceKind {
    static final Logger LOG = LogManager.getLogger(NamespaceKind.class);
    
    private static final String[] ctaNSU = {
      "NIEM6.0", CTAS60,
      "NIEM5.0", CTAS30,
      "NIEM4.0", CTAS30,
      "NIEM3.0", CTAS30
    };
    
    private static final String[] ctaPrefix = {
      "NIEM6.0", "https://docs.oasis-open.org/niemopen/ns/specification/NDR/6.0/",
      "NIEM5.0", "http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/",
      "NIEM4.0", "http://reference.niem.gov/niem/specification/naming-and-design-rules/4.0/",
      "NIEM3.0", "http://reference.niem.gov/niem/specification/naming-and-design-rules/3.0/" 
    };
    
    private static final String[] ctaSuffix = {
        "#ReferenceSchemaDocument", "#ExtensionSchemaDocument", "#SubsetSchemaDocument" 
    };
    
    public static String versionToCtNsURI (String version) {
        for (int i = 0; i < ctaNSU.length; i += 2) {
            if (ctaNSU[i].equals(version)) return ctaNSU[i+1];
        }
        return "";
    }
    
    /**
     * Returns true for a URI that is a conformance target identifier for a schema
     * document defining a NIEM model namespace.
     * @param u
     * @return 
     */
    public static boolean isModelCTA (String u) {
        for (int i = 0; i < ctaPrefix.length; i++) {
            if (!u.startsWith(ctaPrefix[i])) continue;
            for (int j = 0; j < ctaSuffix.length; j++) {
                if (u.equals(ctaPrefix[i]+ctaSuffix[j])) return true;
            }
        }
        return false;
    }
    
    /**
     * Returns a NIEM version ("NIEM6.0", etc.) from a conformance target assertion.
     * Returns empty string if CTA doesn't define a unique NIEM version.
     * @param cta - conformance target URI string
     * @return NIEM version
     */
    public static String ctaToVersion (String cta) {
        for (int i = 0; i < ctaPrefix.length; i += 2) {
            if (cta.startsWith(ctaPrefix[i+1])) return ctaPrefix[i];
        }
        LOG.warn("can't get NIEM version from conformance target assertion {}", cta);
        return "";
    }
    
    /**
     * Returns a conformance target assertion URI string for the specified NIEM version and
     * conformance target ("#ReferenceSchemaDocument", etc.).  Returns empty string
     * for an unknown NIEM version.
     * @param version - NIEME version
     * @param kind - conformance target
     * @return URI string for CTA
     */
    public static String versionToCTA (String version, String kind) {
        for (int i = 0; i < ctaPrefix.length; i += 2) {
            if (ctaPrefix[i].equals(version)) return ctaPrefix[i+1] + kind;
        }
        LOG.warn("no conformance target assertion for unknown version {}", version);
        return "";
    }
    
    // The kinds of namespaces in CMF.  Order is signficant, because it controls
    // priority when resolving namespace prefix collisions: 
    // 1. Prefix assignments in extension schema documents come first.
    //    We assume the model designer knows what he wants.
    // 2. Prefix assignments in the NIEM model schema documents come next.
    //    Everyone is expecting these.
    // 3. Prefix assignments in external schema documents come last.
    //    Who cares what those guys want?
    //
    //
    // Namespace kinds EXTENSION and EXTERNAL cannot be determined without parsing all the
    // schema documents, because they depend on a conformance assertion or an xs:import
    // with @appinfo.  The others can be determined from the namespaced URI alone.
    public final static int NSK_EXTENSION  = 0;     // has conformance assertion, not in NIEM model
    public final static int NSK_DOMAIN     = 1;     // domain schema
    public final static int NSK_CORE       = 2;     // niem core schema
    public final static int NSK_OTHERNIEM  = 3;     // auxillary, codes; has model NS prefix
    public final static int NSK_APPINFO    = 4;     // appinfo
    public final static int NSK_CLSA       = 5;     // code lists schema appinfo
    public final static int NSK_CLI        = 6;     // code lists instance 
    public final static int NSK_NIEM_XS    = 7;     // proxy
    public final static int NSK_STRUCTURES = 8;     // structures
    public final static int NSK_XSD        = 9;     // namespace for XSD datatypes
    public final static int NSK_XML        = 10;    // namespace for xml: attributes
    public final static int NSK_EXTERNAL   = 11;    // was imported with appinfo:externalImportIndicator
    public final static int NSK_NOTNIEM    = 12;    // none of the above; no conformance assertion or external appinfo
    public final static int NSK_UNKNOWN    = 13;    // can't figure it out; probably an error
    public final static int NSK_NUMKINDS   = 14;    // this many kinds of namespaces   
    
    private static final String[] kindCodes = {
            "EXTENSION",
            "DOMAIN", 
            "CORE",
            "OTHERNIEM",
            "APPINFO",
            "CLSA",
            "CLI", 
            "NIEM-XS",
            "STRUCTURES",
            "XSD",
            "XML",
            "EXTERNAL",
            "NOTNIEM",
            "UNKNOWN",
    };
    
    /**
     * Returns the namespace kind code for the namespace kind enumeration.
     * @param kind - namespace kind enumeration value
     * @return namespace kind code
     */
    public static String kindToCode (int kind) {
        if (kind < 0 || kind >= NSK_NUMKINDS) {
            LOG.error("unkown namespace kind #{}", kind);
            return "UNKNOWN";
        }
        else return kindCodes[kind];
    }
    
    /**
     * Returns the namespace kind enumeration value for the namespace kind code.
     * @param code - namespace kind code
     * @return namespace enumeration value
     */
    public static int codeToKind (String code) {
        if (code.isEmpty()) return NSK_UNKNOWN;
        for (int i = 0; i < NSK_NUMKINDS; i++)
            if (kindCodes[i].equals(code)) return i;
        LOG.error("unknown namespace kind code {}", code);
        return NSK_UNKNOWN;
    }
    
    /**
     * Returns true for a namespace kind that is part of a NIEM model.
     * @param kind - namespace kind 
     * @return true for model namespace kind
     */
    public static boolean isModelKind (int kind) {
        switch (kind) {
            case NSK_EXTENSION:
            case NSK_DOMAIN:
            case NSK_CORE:
            case NSK_OTHERNIEM:
            case NSK_CLI: return true;
        }
        return false;
    }
    
    // The known versions and builtin codes.  Builtin codes are the same as the
    // preferred namespace prefix when made lowercase.
    private static Set<String> versions = Set.of("NIEM6.0", "NIEM5.0", "NIEM4.0", "NIEM3.0");
    private static Set<String> builtins = Set.of("APPINFO", "CLSA", "CLI", "NIEM-XS", "STRUCTURES", "XML");
    public static Set<String> knownVersions() { return versions; }
    public static Set<String> builtins() { return builtins; }
    
    // A model with namespaces from different versions needs different directories
    // for the builtin schema documents.  Each builtin document has a usual path
    // within the version directory.
    private static final Map<String,String> versDir = Map.of(
        "NIEM6.0", "niem6/",
        "NIEM5.0", "niem5/",
        "NIEM4.0", "niem4/",
        "NIEM3.0", "niem3/");
    private static final Map<String,String> builtinPath = Map.of(
        "APPINFO",    "utility/appinfo.xsd",
        "CLSA",       "utility/code-lists-schema-appinfo.xsd",
        "CLI",        "utility/code-lists-instance.xsd",
        "NIEM-XS",    "adapters/niem-xs.xsd",
        "STRUCTURES", "utility/structures.xsd",
        "XML",        "external/xml.xsd"
    );
    public static Map<String,String> versionDirName ()  { return versDir; }
    public static Map<String,String> builtinPath ()     { return builtinPath; }

    private static final String[] builtinTab = { 
      "NIEM6.0", "APPINFO",    "https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/",   
      "NIEM6.0", "CLSA",       "https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/appinfo/",
      "NIEM6.0", "CLI",        "https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/instance/",
      "NIEM6.0", "NIEM-XS",    "https://docs.oasis-open.org/niemopen/ns/model/adapters/niem-xs/6.0/",
      "NIEM6.0", "STRUCTURES", "https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/",

      "NIEM5.0", "APPINFO",    "http://release.niem.gov/niem/appinfo/5.0/",   
      "NIEM5.0", "CLSA",       "http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-schema-appinfo/",
      "NIEM5.0", "CLI",        "http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-instance/",
      "NIEM5.0", "NIEM-XS",    "http://release.niem.gov/niem/proxy/niem-xs/5.0/",
      "NIEM5.0", "STRUCTURES", "http://release.niem.gov/niem/structures/5.0/",

      "NIEM4.0", "APPINFO",     "http://release.niem.gov/niem/appinfo/4.0/",   
      "NIEM4.0", "CLSA",        "http://reference.niem.gov/niem/specification/code-lists/4.0/code-lists-schema-appinfo/",
      "NIEM4.0", "CLI",         "http://reference.niem.gov/niem/specification/code-lists/4.0/code-lists-instance/",
      "NIEM4.0", "NIEM-XS",     "http://release.niem.gov/niem/proxy/xsd/4.0/",
      "NIEM4.0", "STRUCTURES",  "http://release.niem.gov/niem/structures/4.0/",    
      
      "NIEM3.0", "APPINFO",    "http://release.niem.gov/niem/appinfo/3.0/",   
      "NIEM3.0", "NIEM-XS",    "http://release.niem.gov/niem/proxy/xsd/3.0/",
      "NIEM3.0", "STRUCTURES", "http://release.niem.gov/niem/structures/3.0/" 
    };
    
    private static final List<Pattern> otherNIEMPat = List.of(
        Pattern.compile("https://docs.oasis-open.org/niemopen/ns/model/(?!external/)"),
        Pattern.compile("http://release.niem.gov/niem/(?!external/)")
    );

    private static final String[] modelPrefixTab = {
        "CORE",      "https://docs.oasis-open.org/niemopen/ns/model/niem-core/",
        "CORE",      "http://release.niem.gov/niem/niem-core/",
        "DOMAIN",    "https://docs.oasis-open.org/niemopen/ns/model/domains/",
        "DOMAIN",    "http://release.niem.gov/niem/domains/"
    };
    
    /**
     * Returns the namespace URI for the specified NIEM version and builtin kind.
     * Returns empty string if no such builtin namespace.
     */ 
    public static String builtinNSU (String version, String kindCode) {
        if (!version.isEmpty() && !versions.contains(version)) LOG.error("unknown NIEM version {}", version);
        if (!builtins.contains(kindCode)) LOG.error("unknown builtin kind code {}", kindCode);
        if ("XML".equals(kindCode)) return XML_NS_URI;
        for (int i = 0; i < builtinTab.length; i += 3) {
            if (builtinTab[i].equals(version) && builtinTab[i+1].equals(kindCode))
                return builtinTab[i+2];
        }
        return "";
    }
    
    /**
     * Returns the NIEM version when this can be determined from a namespace URI
     * (which you can do for the builtin namespaces).
     * @param ns - namespace URI string
     * @return NIEM version
     */
    public static String namespaceToNIEMVersion (String ns) {
        for (int i = 0; i < builtinTab.length; i += 3) {
            if (builtinTab[i+2].equals(ns)) return builtinTab[i];
        }
        return "";
    }
    
    /**
     * Returns the namespace kind code when this can be determined
     * from the namespace URI.
     * @param ns - namespace URI string
     * @return namespace kind code
     */
    public static String namespaceToKindCode (String ns) {
        if (XML_NS_URI.equals(ns)) return "XML";
        if (W3C_XML_SCHEMA_NS_URI.equals(ns)) return "XSD";
        for (int i = 0; i < builtinTab.length; i += 3) {
            if (builtinTab[i+2].equals(ns)) return builtinTab[i+1];
        }
        for (int i = 0; i < modelPrefixTab.length; i += 2) {
            if (ns.startsWith(modelPrefixTab[i+1])) return modelPrefixTab[i];
        }
        for (var pat : otherNIEMPat) {
            var m = pat.matcher(ns);
            if (m.lookingAt()) return "OTHERNIEM";
        }
        return "";
    }
    
    /**
     * Returns the namespace kind enumeration value when this can be determined
     * from the namespace URI.
     * @param ns - namespace URI string
     * @return namespace kind
     */    
    public static int namespaceToKind (String ns) {
        return codeToKind(namespaceToKindCode(ns));
    }
    
}
