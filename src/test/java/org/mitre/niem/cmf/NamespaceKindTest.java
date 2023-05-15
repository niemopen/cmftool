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
import static org.mitre.niem.cmf.NamespaceKind.NIEM_APPINFO;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CLI;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CLSA;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CTAS;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_NOTUTILITY;
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

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NamespaceKindTest {

    private int NIEM_CT;
    
    public NamespaceKindTest() {
    }

    @Test
    public void testNIEM5() {
        assertEquals("UNKNOWN", NamespaceKind.architecture("http://example.com/foo"));
        assertEquals("UNKNOWN", NamespaceKind.architecture("http://example.com/foo/5.0/#"));
        assertEquals("UNKNOWN", NamespaceKind.architecture(XML_NS_URI));
        assertEquals("UNKNOWN", NamespaceKind.architecture(W3C_XML_SCHEMA_NS_URI));
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
        
        assertEquals(NIEM_NOTUTILITY, NamespaceKind.utilityKind("http://example.com/foo"));
        assertEquals(NIEM_NOTUTILITY, NamespaceKind.utilityKind("http://example.com/foo/5.0/#"));
        assertEquals(NIEM_NOTUTILITY, NamespaceKind.utilityKind(XML_NS_URI));
        assertEquals(NIEM_NOTUTILITY, NamespaceKind.utilityKind(W3C_XML_SCHEMA_NS_URI));
        assertEquals(NIEM_NOTUTILITY, NamespaceKind.utilityKind("http://release.niem.gov/niem/niem-core/5.0/"));
        assertEquals(NIEM_NOTUTILITY, NamespaceKind.utilityKind("http://release.niem.gov/niem/domains/humanServices/5.2/"));
        assertEquals(NIEM_NOTUTILITY, NamespaceKind.utilityKind("http://release.niem.gov/niem/auxiliary/cui/5.1/"));
        assertEquals(NIEM_NOTUTILITY, NamespaceKind.utilityKind("http://release.niem.gov/niem/codes/genc/5.0/"));         
        assertEquals(NIEM_APPINFO, NamespaceKind.utilityKind("http://release.niem.gov/niem/appinfo/5.0/"));   
        assertEquals(NIEM_CLI, NamespaceKind.utilityKind("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-instance/"));   
        assertEquals(NIEM_CLSA, NamespaceKind.utilityKind("http://reference.niem.gov/niem/specification/code-lists/5.0/code-lists-schema-appinfo/"));    
        assertEquals(NIEM_CTAS, NamespaceKind.utilityKind("http://release.niem.gov/niem/conformanceTargets/3.0/"));
        assertEquals(NIEM_PROXY, NamespaceKind.utilityKind("http://release.niem.gov/niem/proxy/niem-xs/5.0/"));
        assertEquals(NIEM_STRUCTURES, NamespaceKind.utilityKind("http://release.niem.gov/niem/structures/5.0/")); 

        assertEquals(NSK_UNKNOWN, NamespaceKind.kind("http://example.com/foo"));
        assertEquals(NSK_UNKNOWN, NamespaceKind.kind("http://example.com/foo/5.0/#"));
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
        assertEquals("", NamespaceKind.version("http://example.com/foo/5.0/#"));
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
        assertEquals("3", NamespaceKind.version("http://release.niem.gov/niem/conformanceTargets/3.0/"));
        assertEquals("5", NamespaceKind.version("http://release.niem.gov/niem/proxy/niem-xs/5.0/"));
        assertEquals("5", NamespaceKind.version("http://release.niem.gov/niem/structures/5.0/"));
        
        NamespaceKind.set("http://example.com/foo", "NIEM5", NSK_EXTENSION, NIEM_NOTUTILITY, "4");
        assertEquals("NIEM5", NamespaceKind.architecture("http://example.com/foo"));
        assertEquals(NSK_EXTENSION, NamespaceKind.kind("http://example.com/foo"));
        assertEquals(NIEM_NOTUTILITY, NamespaceKind.utilityKind("http://example.com/foo"));
        assertEquals("4", NamespaceKind.version("http://example.com/foo"));
    }
}
