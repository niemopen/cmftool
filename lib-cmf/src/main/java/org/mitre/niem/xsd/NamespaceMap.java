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

import java.util.HashMap;
import java.util.regex.Pattern;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;

/**
 *
 * A class for the mappings from namespace prefix to namespace URI.
 * Ensures that each prefix has at most one corresponding URI. First mapping
 * wins, subsequent mappings of the same prefix are munged to ensure uniqueness.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NamespaceMap {
    
    private final HashMap<String,String> prefix2URI;
    private final HashMap<String,String> uri2Prefix;

    public NamespaceMap () { 
        // Initialize reserved namespace prefixes: xs, xsd, xml, xmlns
        prefix2URI = new HashMap<>();
        uri2Prefix = new HashMap<>();
        prefix2URI.put("xml", XML_NS_URI);  
        prefix2URI.put("xmlns", XMLNS_ATTRIBUTE_NS_URI);
        prefix2URI.put("xs", W3C_XML_SCHEMA_NS_URI);
        prefix2URI.put("xsd", W3C_XML_SCHEMA_NS_URI);
        prefix2URI.put("xsi", W3C_XML_SCHEMA_INSTANCE_NS_URI);

        // Map those namespaces to their reserved prefix
        // Not quite the inverse:  W3C_XML_SCHEMA_NS_URI is only mapped to "xs"
        uri2Prefix.put(W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi");
        uri2Prefix.put(W3C_XML_SCHEMA_NS_URI, "xs");
        uri2Prefix.put(XMLNS_ATTRIBUTE_NS_URI, "xmlns");
        uri2Prefix.put(XML_NS_URI, "xml");
    }
    
    public NamespaceMap (NamespaceMap nm) {
        prefix2URI = new HashMap<>(nm.prefix2URI);
        uri2Prefix = new HashMap<>(nm.uri2Prefix);
    }
    
    public String getPrefix (String nsuri) { return uri2Prefix.get(nsuri); }
    public String getURI (String prefix)   { return prefix2URI.get(prefix); }    
    
    private static final Pattern mungPat = Pattern.compile("(.*)_\\d+$");
    private static final Pattern versPat = Pattern.compile(".*/(\\d+)(\\.\\d+)*/?$");    

    /**
     * Assign a prefix to a namespace, if that prefix is not already bound.
     * Assigns a munged prefix ("foo_1" instead of "foo") if prefix is already bound.
     * Returns the prefix actually assigned to the namespace.
     * 
     * @param prefix -- desired prefix
     * @param nsuri  -- namespace URI
     * @return assigned prefix
     */
    public String assignPrefix (String prefix, String nsuri) {
        if (uri2Prefix.containsKey(nsuri)) return prefix;   // already assigned       
        if (!prefix2URI.containsKey(prefix)) {              // this prefix is available
            prefix2URI.put(prefix, nsuri);
            uri2Prefix.put(nsuri, prefix);
            return prefix;
        }
        // Desired prefix is already assigned to a different namespace
        // So they get a munged prefix; eg. "foo_1" instead of "foo"
        // First, get the munging base, to void twice-munged prefix; eg. "foo_1_2"
        var mungBase = prefix;
        var mungP    = prefix;
        var m = mungPat.matcher(nsuri);
        if (m.matches()) {
            mungBase = m.group(1);
        }
        // If the URI has a known version, or seems to end in a version number, 
        // try eg. "nc_4" for version 4.0
        var vm = versPat.matcher(nsuri);
        if (vm.matches()) {
            mungP = mungBase + "_" + vm.group(1);
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
    
    public void removePrefix (String prefix) {
        var oldURI = prefix2URI.get(prefix);
        prefix2URI.remove(prefix);
        uri2Prefix.remove(oldURI);
    }
    
}
    
