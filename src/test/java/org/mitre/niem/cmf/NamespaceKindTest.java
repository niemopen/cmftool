/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2023 The MITRE Corporation.
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

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_APPINFO;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CLI;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CLSA;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CTAS;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_PROXY;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_STRUCTURES;
import static org.mitre.niem.cmf.NamespaceKind.NSK_CORE;
import static org.mitre.niem.cmf.NamespaceKind.NSK_DOMAIN;
import static org.mitre.niem.cmf.NamespaceKind.NSK_EXTENSION;
import static org.mitre.niem.cmf.NamespaceKind.NSK_OTHERNIEM;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UNKNOWN;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UTILITY;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XML;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XSD;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_NOTBUILTIN;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_XML;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NamespaceKindTest {

    private int NIEM_CT;
    
    public NamespaceKindTest() {
    }
    
    @BeforeEach
    public void initEqch() {
        NamespaceKind.reset();
    }

    @Test
    public void testNIEM5() {
        assertEquals("", NamespaceKind.architecture("http://example.com/foo"));
        assertEquals("", NamespaceKind.architecture("http://example.com/foo/5.0/"));
        assertEquals("", NamespaceKind.architecture(XML_NS_URI));
        assertEquals("", NamespaceKind.architecture(W3C_XML_SCHEMA_NS_URI));
        assertEquals("NIEM5", NamespaceKind.architecture("http://release.niem.gov/niem/niem-core/5.0/"));
        assertEquals("NIEM5", NamespaceKind.architecture("http://release.niem.gov/niem/domains/humanServices/5.2/"));
        assertEquals("NIEM5", NamespaceKind.architecture("http://release.niem.gov/niem/auxiliary/cui/5.1/"));
        assertEquals("NIEM5", NamespaceKind.architecture("http://release.niem.gov/niem/codes/genc/5.0/"));        
        assertEquals("NIEM5", NamespaceKind.architecture("http://release.niem.gov/niem/codes/genc/5.0/"));   
        assertEquals("NIEM5", NamespaceKind.architecture("http://release.niem.gov/niem/appinfo/5.0/"));   
        assertEquals("NIEM5", NamespaceKind.architecture("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-instance/"));   
        assertEquals("NIEM5", NamespaceKind.architecture("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-schema-appinfo/")); 
        assertEquals("NIEM5", NamespaceKind.architecture("http://release.niem.gov/niem/conformanceTargets/3.0/"));
        assertEquals("NIEM5", NamespaceKind.architecture("http://release.niem.gov/niem/proxy/niem-xs/5.0/"));
        assertEquals("NIEM5", NamespaceKind.architecture("http://release.niem.gov/niem/structures/5.0/"));   
        
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("http://example.com/foo"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("http://example.com/foo/5.0/"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin(W3C_XML_SCHEMA_NS_URI));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("http://release.niem.gov/niem/niem-core/5.0/"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("http://release.niem.gov/niem/domains/humanServices/5.2/"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("http://release.niem.gov/niem/auxiliary/cui/5.1/"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("http://release.niem.gov/niem/codes/genc/5.0/"));         
        assertEquals(NIEM_APPINFO, NamespaceKind.builtin("http://release.niem.gov/niem/appinfo/5.0/"));   
        assertEquals(NIEM_CLI, NamespaceKind.builtin("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-instance/"));   
        assertEquals(NIEM_CLSA, NamespaceKind.builtin("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-schema-appinfo/"));    
        assertEquals(NIEM_CTAS, NamespaceKind.builtin("http://release.niem.gov/niem/conformanceTargets/3.0/"));
        assertEquals(NIEM_PROXY, NamespaceKind.builtin("http://release.niem.gov/niem/proxy/niem-xs/5.0/"));
        assertEquals(NIEM_STRUCTURES, NamespaceKind.builtin("http://release.niem.gov/niem/structures/5.0/")); 
        assertEquals(NIEM_XML, NamespaceKind.builtin(XML_NS_URI));
        
        assertEquals(NSK_UNKNOWN, NamespaceKind.kind("http://example.com/foo"));
        assertEquals(NSK_UNKNOWN, NamespaceKind.kind("http://example.com/foo/5.0/"));
        assertEquals(NSK_XML, NamespaceKind.kind(XML_NS_URI));
        assertEquals(NSK_XSD, NamespaceKind.kind(W3C_XML_SCHEMA_NS_URI));
        assertEquals(NSK_CORE, NamespaceKind.kind("http://release.niem.gov/niem/niem-core/5.0/"));
        assertEquals(NSK_DOMAIN, NamespaceKind.kind("http://release.niem.gov/niem/domains/humanServices/5.2/"));
        assertEquals(NSK_OTHERNIEM, NamespaceKind.kind("http://release.niem.gov/niem/auxiliary/cui/5.1/"));
        assertEquals(NSK_OTHERNIEM, NamespaceKind.kind("http://release.niem.gov/niem/codes/genc/5.0/"));         
        assertEquals(NSK_UTILITY, NamespaceKind.kind("http://release.niem.gov/niem/appinfo/5.0/"));   
        assertEquals(NSK_OTHERNIEM, NamespaceKind.kind("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-instance/"));   
        assertEquals(NSK_UTILITY, NamespaceKind.kind("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-schema-appinfo/"));    
        assertEquals(NSK_UTILITY, NamespaceKind.kind("http://release.niem.gov/niem/conformanceTargets/3.0/"));
        assertEquals(NSK_UTILITY, NamespaceKind.kind("http://release.niem.gov/niem/proxy/niem-xs/5.0/"));
        assertEquals(NSK_UTILITY, NamespaceKind.kind("http://release.niem.gov/niem/structures/5.0/")); 
        
        assertEquals("", NamespaceKind.version("http://example.com/foo"));
        assertEquals("", NamespaceKind.version("http://example.com/foo/5.0/"));
        assertEquals("", NamespaceKind.version(XML_NS_URI));
        assertEquals("", NamespaceKind.version(W3C_XML_SCHEMA_NS_URI));
        assertEquals("", NamespaceKind.version("http://release.niem.gov/niem/niem-core/5.0/"));
        assertEquals("", NamespaceKind.version("http://release.niem.gov/niem/domains/humanServices/5.2/"));
        assertEquals("", NamespaceKind.version("http://release.niem.gov/niem/auxiliary/cui/5.1/"));
        assertEquals("", NamespaceKind.version("http://release.niem.gov/niem/codes/genc/5.0/"));        
        assertEquals("", NamespaceKind.version("http://release.niem.gov/niem/codes/genc/5.0/"));   
        assertEquals("5", NamespaceKind.version("http://release.niem.gov/niem/appinfo/5.0/"));   
        assertEquals("5", NamespaceKind.version("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-instance/"));   
        assertEquals("5", NamespaceKind.version("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-schema-appinfo/"));    
        assertEquals("", NamespaceKind.version("http://release.niem.gov/niem/conformanceTargets/3.0/"));
        assertEquals("5", NamespaceKind.version("http://release.niem.gov/niem/proxy/niem-xs/5.0/"));
        assertEquals("5", NamespaceKind.version("http://release.niem.gov/niem/structures/5.0/"));
        
        NamespaceKind.set("http://example.com/foo", "NIEM5", NSK_EXTENSION, NIEM_NOTBUILTIN, "4");
        assertEquals("NIEM5", NamespaceKind.architecture("http://example.com/foo"));
        assertEquals(NSK_EXTENSION, NamespaceKind.kind("http://example.com/foo"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("http://example.com/foo"));
        assertEquals("4", NamespaceKind.version("http://example.com/foo"));
    }

    @Test
    public void testNIEM6() {
        assertEquals("", NamespaceKind.architecture("http://example.com/foo"));
        assertEquals("", NamespaceKind.architecture("http://example.com/foo/6.0/"));
        assertEquals("", NamespaceKind.architecture(XML_NS_URI));
        assertEquals("", NamespaceKind.architecture(W3C_XML_SCHEMA_NS_URI));
        assertEquals("NIEM6", NamespaceKind.architecture("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"));
        assertEquals("NIEM6", NamespaceKind.architecture("https://docs.oasis-open.org/niemopen/ns/model/domains/humanServices/6.0/"));
        assertEquals("NIEM6", NamespaceKind.architecture("https://docs.oasis-open.org/niemopen/ns/model/cui/5.1/"));
        assertEquals("NIEM6", NamespaceKind.architecture("https://docs.oasis-open.org/niemopen/ns/model/codes/genc/6.0/"));        
        assertEquals("NIEM6", NamespaceKind.architecture("https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/"));   
        assertEquals("NIEM6", NamespaceKind.architecture("https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-instance/"));   
        assertEquals("NIEM6", NamespaceKind.architecture("https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-schema-appinfo/")); 
        assertEquals("NIEM6", NamespaceKind.architecture("https://docs.oasis-open.org/niemopen/ns/specification/conformanceTargets/3.0/"));
        assertEquals("NIEM6", NamespaceKind.architecture("https://docs.oasis-open.org/niemopen/ns/model/proxy/niem-xs/6.0/"));
        assertEquals("NIEM6", NamespaceKind.architecture("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/"));   
        
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("http://example.com/foo"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("http://example.com/foo/6.0/"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin(W3C_XML_SCHEMA_NS_URI));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("https://docs.oasis-open.org/niemopen/ns/model/domains/humanServices/6.0/"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("https://docs.oasis-open.org/niemopen/ns/model/cui/5.1/"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("https://docs.oasis-open.org/niemopen/ns/model/codes/genc/6.0/"));         
        assertEquals(NIEM_APPINFO, NamespaceKind.builtin("https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/"));   
        assertEquals(NIEM_CLI, NamespaceKind.builtin("https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-instance/"));   
        assertEquals(NIEM_CLSA, NamespaceKind.builtin("https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-schema-appinfo/"));    
        assertEquals(NIEM_CTAS, NamespaceKind.builtin("https://docs.oasis-open.org/niemopen/ns/specification/conformanceTargets/3.0/"));
        assertEquals(NIEM_PROXY, NamespaceKind.builtin("https://docs.oasis-open.org/niemopen/ns/model/proxy/niem-xs/6.0/"));
        assertEquals(NIEM_STRUCTURES, NamespaceKind.builtin("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/")); 
        assertEquals(NIEM_XML, NamespaceKind.builtin(XML_NS_URI));
        
        assertEquals(NSK_UNKNOWN, NamespaceKind.kind("http://example.com/foo"));
        assertEquals(NSK_UNKNOWN, NamespaceKind.kind("http://example.com/foo/6.0/"));
        assertEquals(NSK_XML, NamespaceKind.kind(XML_NS_URI));
        assertEquals(NSK_XSD, NamespaceKind.kind(W3C_XML_SCHEMA_NS_URI));
        assertEquals(NSK_CORE, NamespaceKind.kind("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"));
        assertEquals(NSK_DOMAIN, NamespaceKind.kind("https://docs.oasis-open.org/niemopen/ns/model/domains/humanServices/6.0/"));
        assertEquals(NSK_OTHERNIEM, NamespaceKind.kind("https://docs.oasis-open.org/niemopen/ns/model/cui/5.1/"));
        assertEquals(NSK_OTHERNIEM, NamespaceKind.kind("https://docs.oasis-open.org/niemopen/ns/model/codes/genc/6.0/"));         
        assertEquals(NSK_UTILITY, NamespaceKind.kind("https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/"));   
        assertEquals(NSK_OTHERNIEM, NamespaceKind.kind("https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-instance/"));   
        assertEquals(NSK_UTILITY, NamespaceKind.kind("https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-schema-appinfo/"));    
        assertEquals(NSK_UTILITY, NamespaceKind.kind("https://docs.oasis-open.org/niemopen/ns/specification/conformanceTargets/3.0/"));
        assertEquals(NSK_UTILITY, NamespaceKind.kind("https://docs.oasis-open.org/niemopen/ns/model/proxy/niem-xs/6.0/"));
        assertEquals(NSK_UTILITY, NamespaceKind.kind("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/")); 
        
        assertEquals("", NamespaceKind.version("http://example.com/foo"));
        assertEquals("", NamespaceKind.version("http://example.com/foo/6.0/"));
        assertEquals("", NamespaceKind.version(XML_NS_URI));
        assertEquals("", NamespaceKind.version(W3C_XML_SCHEMA_NS_URI));
        assertEquals("", NamespaceKind.version("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"));
        assertEquals("", NamespaceKind.version("https://docs.oasis-open.org/niemopen/ns/model/domains/humanServices/6.0/"));
        assertEquals("", NamespaceKind.version("https://docs.oasis-open.org/niemopen/ns/model/cui/5.1/"));
        assertEquals("", NamespaceKind.version("https://docs.oasis-open.org/niemopen/ns/model/codes/genc/6.0/"));        
        assertEquals("", NamespaceKind.version("http://release.niem.gov/niem/codes/genc/6.0/"));   
        assertEquals("6", NamespaceKind.version("https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/"));   
        assertEquals("6", NamespaceKind.version("https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-instance/"));   
        assertEquals("6", NamespaceKind.version("https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/code-lists-schema-appinfo/"));    
        assertEquals("", NamespaceKind.version("https://docs.oasis-open.org/niemopen/ns/specification/conformanceTargets/3.0/"));
        assertEquals("6", NamespaceKind.version("https://docs.oasis-open.org/niemopen/ns/model/proxy/niem-xs/6.0/"));
        assertEquals("6", NamespaceKind.version("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/"));
        
        NamespaceKind.set("http://example.com/foo", "NIEM6", NSK_EXTENSION, NIEM_NOTBUILTIN, "4");
        assertEquals("NIEM6", NamespaceKind.architecture("http://example.com/foo"));
        assertEquals(NSK_EXTENSION, NamespaceKind.kind("http://example.com/foo"));
        assertEquals(NIEM_NOTBUILTIN, NamespaceKind.builtin("http://example.com/foo"));
        assertEquals("4", NamespaceKind.version("http://example.com/foo"));
    }
}
