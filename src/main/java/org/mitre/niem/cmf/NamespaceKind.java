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

import java.util.regex.Pattern;
import static org.mitre.niem.NIEMConstants.NIEM_CORE_PATTERN;
import static org.mitre.niem.NIEMConstants.NIEM_DOMAIN_PATTERN;
import static org.mitre.niem.NIEMConstants.NIEM_MODEL_PATTERN;
import static org.mitre.niem.NIEMConstants.XML_NS_URI;
import static org.mitre.niem.NIEMConstants.XSD_NS_URI;
import static org.mitre.niem.xsd.NIEMBuiltins.isBuiltinNamespace;

/**
 * A class to determine and represent the kind of a CMF namespace.  Is it from
 * the NIEM model, a NIEM builtin, etc? 
 * *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NamespaceKind {
    
    // The nine kinds of namespaces in CMF.  Order is signficant, because it controls
    // priority when normalizing namespace prefix assignment: extension, niem-model, builtin,
    // XSD, XML, external, unknown.  
    public final static int NSK_EXTENSION  = 0;     // has conformance assertion, not in NIEM model
    public final static int NSK_DOMAIN     = 1;     // domain schema
    public final static int NSK_CORE       = 2;     // niem core schema
    public final static int NSK_OTHERNIEM  = 3;     // other niem model; starts with release or publication prefix
    public final static int NSK_BUILTIN    = 4;     // appinfo, code-lists, conformance, proxy, structures
    public final static int NSK_XSD        = 5;     // namespace for XSD datatypes
    public final static int NSK_XML        = 6;     // namespace for xml: attributes
    public final static int NSK_EXTERNAL   = 7;     // imported with appinfo:externalImportIndicator
    public final static int NSK_UNKNOWN    = 8;     // no conformance assertion; not any of the above
    public final static int NSK_NUMKINDS   = 9;     // this many kinds of namespaces   
    
    private final static String[] namespaceCode = { "EXTENSION", "DOMAIN", "CORE", "OTHERNIEM", "BUILTIN", "XSD", "XML", "EXTERNAL", "UNKNOWN" };
    private final static boolean[] nskInCMF     = { true,        true,     true,   true,        false,     true,  true,  true,       false};
    
    public static String namespaceKind2Code (int kind) { 
        return kind < 0 || kind > NSK_NUMKINDS ? "UNKNOWN" : namespaceCode[kind];
    }
    
    public static int namespaceCode2Kind (String s) {
        for (int k = 0; k < NSK_NUMKINDS; k++)
            if (s.equals(namespaceCode[k]))
                return k;
        return NSK_UNKNOWN;        
    }
    
    public static boolean isNamespaceKindInCMF (int kind) {
        return kind < 0 || kind > NSK_NUMKINDS ? false : nskInCMF[kind]; 
    }    
    
    /**
     * Determines the kind of a namespace from its URI.  This will return UNKNOWN
     * for extension and external namespaces.  Those can only be recognized by 
     * looking at the schema pile, or by being told.
     * @param uri
     * @return 
     */
    private static final Pattern niemModelPat   = Pattern.compile(NIEM_MODEL_PATTERN);
    private static final Pattern niemCorePat    = Pattern.compile(NIEM_CORE_PATTERN);
    private static final Pattern niemDomainPat  = Pattern.compile(NIEM_DOMAIN_PATTERN);
        
    public static int namespaceKindFromURI (String uri) {
        if (isBuiltinNamespace(uri))                     return NSK_BUILTIN;
        else if (XML_NS_URI.equals(uri))                 return NSK_XML;
        else if (XSD_NS_URI.equals(uri))                 return NSK_XSD;
        else if (niemCorePat.matcher(uri).lookingAt())   return NSK_CORE;
        else if (niemDomainPat.matcher(uri).lookingAt()) return NSK_DOMAIN;
        else if (niemModelPat.matcher(uri).lookingAt())  return NSK_OTHERNIEM;
        
        // Could be EXTENSION or EXTERNAL; can't tell w/o looking through the schema pile.
        else return NSK_UNKNOWN;          
    }
    
}
