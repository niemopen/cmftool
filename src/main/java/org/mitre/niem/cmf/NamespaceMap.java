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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.mitre.niem.NIEMConstants.CMF_NS_URI;
import static org.mitre.niem.NIEMConstants.OWL_NS_URI;
import static org.mitre.niem.NIEMConstants.RDFS_NS_URI;
import static org.mitre.niem.NIEMConstants.RDF_NS_URI;
import static org.mitre.niem.NIEMConstants.XML_NS_URI;
import static org.mitre.niem.NIEMConstants.XSD_NS_URI;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinVersion;



/**
 *
 * A class for the mappings from namespace prefix to namespace URI.
 * Ensures that each prefix has at most one corresponding URI.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NamespaceMap {
    
    private final HashMap<String,String> prefix2URI = new HashMap<>();
    private final HashMap<String,String> uri2Prefix = new HashMap<>();

    
    public NamespaceMap () { 
        // Initialize the reserved namespace prefixes: cmf, owl, rdf, rdfs, xs, xsd, xml
        prefix2URI.put("cmf", CMF_NS_URI);
        prefix2URI.put("owl", OWL_NS_URI);
        prefix2URI.put("rdf", RDF_NS_URI);
        prefix2URI.put("rdfs", RDFS_NS_URI);
        prefix2URI.put("xml", XML_NS_URI);  
        prefix2URI.put("xs", XSD_NS_URI);
        prefix2URI.put("xsd", XSD_NS_URI);

        // Map those namespaces to their reserved prefix
        // Not quite the inverse:  XSD_NS_URI is only mapped to "xs"
        uri2Prefix.put(CMF_NS_URI, "cmf");
        uri2Prefix.put(OWL_NS_URI, "owl");
        uri2Prefix.put(RDF_NS_URI, "rdf");
        uri2Prefix.put(RDFS_NS_URI, "rdfs");
        uri2Prefix.put(XSD_NS_URI, "xs");
        uri2Prefix.put(XML_NS_URI, "xml");
    }
    
    public String getPrefix (String nsuri) { return uri2Prefix.get(nsuri); }
    public String getURI (String prefix)   { return prefix2URI.get(prefix); }    
    
    private static final Pattern mungPat = Pattern.compile("(.*)_\\d+$");
    private static final Pattern versPat = Pattern.compile(".*/(\\d+)(\\.\\d+)*/?$");
    
    public String assignPrefix (String prefix, String nsuri) {
        if (uri2Prefix.containsKey(nsuri)) return prefix;   // already assigned
        if (!prefix2URI.containsKey(prefix)) {              // this prefix is available
            prefix2URI.put(prefix, nsuri);
            uri2Prefix.put(nsuri, prefix);
            return prefix;
        }
        // Avoid twice-munged prefix; eg. "foo_1_2"
        String mungBase = prefix;
        String mungP = prefix;
        Matcher m = mungPat.matcher(nsuri);
        if (m.matches()) {
            mungBase = m.group(1);
        }
        // If it's a builtin, try eg. "appinfo_4" for version 4
        String bvers = getBuiltinVersion(nsuri);
        if (null != bvers) {
            int dp = bvers.indexOf(".");
            if (dp > 0) bvers = bvers.substring(0, dp);
            mungP = mungBase + "_" + bvers;
        }
        // If the URI seems to end in a version number, try eg. "nc_4" for version 4.0
        else {
            m = versPat.matcher(nsuri);
            if (m.matches()) {
                mungP = mungBase + "_" + m.group(1);
            }
        }
        int mct = 0;
        while (prefix2URI.containsKey(mungP)) {
            mungP = String.format("%s_%d", mungBase, ++mct);
        }
        prefix2URI.put(mungP, nsuri);
        uri2Prefix.put(nsuri, mungP);
        return mungP;
    }
    
    public String changePrefix (String newPrefix, String nsuri) {
        String oldPrefix = uri2Prefix.get(nsuri);
        if (null != oldPrefix)  {
            prefix2URI.remove(oldPrefix);
            uri2Prefix.remove(nsuri);
        }
        return assignPrefix(newPrefix, nsuri);
    }
    
}
