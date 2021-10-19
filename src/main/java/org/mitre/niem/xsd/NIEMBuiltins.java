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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 * A class for recognizing and generating namespace URIs for the built-in
 * NIEM namespaces:  appinfo, code-lists-instance, code-lists-schema-appinfo,
 * conformanceTargets, proxy, and structures.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NIEMBuiltins {
    
    public static int NIEM_APPINFO = 0;
    public static int NIEM_CODE_LISTS_INSTANCE = 1;
    public static int NIEM_CODE_LISTS_SCHEMA_APPINFO = 2;
    public static int NIEM_CONFORMANCE_TARGETS = 3 ;
    public static int NIEM_PROXY = 4;
    public static int NIEM_STRUCTURES = 5;
    public static int NIEM_BUILTINS_COUNT = 6;
    
    private static Pattern[] builtinNSPatterns= {
        Pattern.compile("http://release.niem.gov/niem/appinfo/([\\d.]+)/"),
        Pattern.compile("http://reference.niem.gov/niem/specification/code-lists/([\\d.]+)/code-lists-instance/"),
        Pattern.compile("http://reference.niem.gov/niem/specification/code-lists/([\\d.]+)/code-lists-schema-appinfo/"),        
        Pattern.compile("http://release.niem.gov/niem/conformanceTargets/([\\d.]+)/"),
        Pattern.compile("http://release.niem.gov/niem/proxy/[^/]+/([\\d.]+)/"),
        Pattern.compile("http://release.niem.gov/niem/structures/([\\d.]+)/")
    };    
    private static String[] builtinNSURI= {
        "http://release.niem.gov/niem/appinfo/VERSION/",
        "http://reference.niem.gov/niem/specification/code-lists/VERSION/code-lists-instance/",
        "http://reference.niem.gov/niem/specification/code-lists/VERSION/code-lists-schema-appinfo/",        
        "http://release.niem.gov/niem/conformanceTargets/VERSION/",
        "http://release.niem.gov/niem/proxy/PROXY/VERSION/",
        "http://release.niem.gov/niem/structures/VERSION/"
    };
    private static String[] builtinFilename = {
        "appinfo.xsd",
        "code-list-instance.xsd",
        "code-list-schema-appinfo.xsd",
        "conformanceTargets.xsd",
        "niem-xs.xsd",
        "structures.xsd"
    };
    
    public static boolean isBuiltinNamespace (String nsuri) {
        return getBuiltinNamespaceKind(nsuri) >= 0;
    }
    
    public static int getBuiltinNamespaceKind (String nsuri) {
        for (int i = builtinNSPatterns.length - 1; i >= 0; i--) {
            Matcher m = builtinNSPatterns[i].matcher(nsuri);
            if (m.matches()) return i;
        }
        return -1;
    }
    
    public static String getBuiltinNamespaceVersion (String nsuri) {
        for (int i = builtinNSPatterns.length - 1; i >= 0; i--) {
            Matcher m = builtinNSPatterns[i].matcher(nsuri);
            if (m.matches()) return m.group(1);
        }
        return null;        
    }

    /**
     * Returns the namespace URI of the specified builtin and version.
     * Minor versions are ignored; e.g. "5.1" becomes "5.0"
     * The version of the conformance target namespace is always "3.0"
     * @param which builtin 
     * @param version NIEM version
     * @return builtin namespace uri
     */
    public static String getBuiltinNamespaceURI (int which, String version) {
        String res = builtinNSURI[which];
        version = version.replaceFirst("\\.[\\d.]+$", ".0");   // ignore minor version numbers
        if (NIEM_CONFORMANCE_TARGETS == which) version = "3.0";
        res = res.replace("VERSION", version);
        if ("5.0".compareTo(version) <= 0) res = res.replace("PROXY", "niem-xs");
        else res = res.replace("PROXY", "xsd");
        return res;
    }
    
    public static String getBuiltinNamespaceURI (String nsuri, String version) {
        int which = getBuiltinNamespaceKind(nsuri);
        if (which < 0) return null;
        return getBuiltinNamespaceURI(which, version);
    }
    
    public static File getBuiltinDocumentFile (String nsuri) {
        String appdir = NIEMBuiltins.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String version = getBuiltinNamespaceVersion(nsuri);
        String res;
        int kind = getBuiltinNamespaceKind(nsuri);
        if (appdir.endsWith(".jar")) res = FilenameUtils.concat(appdir, "../../share/xsd");
        else res = FilenameUtils.concat(appdir, "../../../../src/main/dist/share/xsd");
        res = FilenameUtils.concat(res, version);
        res = FilenameUtils.concat(res, builtinFilename[kind]);
        return new File(res);
    }
    
    public static List<String> orderedBuiltinURIs (Collection<String> uris) {
        List<String> res = new ArrayList<>();
        for (String s: uris) { res.add(s); }
        res.sort((String s, String o) -> {
            String tv = getBuiltinNamespaceVersion(s);
            String ov = getBuiltinNamespaceVersion(o);
            return ov.compareTo(tv);
        });
        return res;
    }
    
}
