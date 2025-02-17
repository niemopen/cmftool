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

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NamespaceMapTest {
    
    public NamespaceMapTest() {
    }
    
    @Test
    public void testMap () {
        var map = new NamespaceMap();
        assertEquals("xml", map.getPrefix(XML_NS_URI));  
        assertEquals("xmlns", map.getPrefix(XMLNS_ATTRIBUTE_NS_URI));        
        assertEquals("xs", map.getPrefix(W3C_XML_SCHEMA_NS_URI)); 
        assertEquals(XML_NS_URI, map.getURI("xml"));  
        assertEquals(XMLNS_ATTRIBUTE_NS_URI, map.getURI("xmlns"));        
        assertEquals(W3C_XML_SCHEMA_NS_URI, map.getURI("xs")); 
        assertEquals(W3C_XML_SCHEMA_NS_URI, map.getURI("xsd"));
        
        assertEquals("nc", map.assignPrefix("nc", "http://example.com/nc/6.0/"));
        assertEquals("ct", map.assignPrefix("ct", "http://example.com/ct/6.0/"));
        assertEquals("nc_5", map.assignPrefix("nc", "http://example.com/nc/5.0/"));
        assertEquals("ct_4", map.assignPrefix("ct", "http://examplelcom/ct/4.0"));
        assertEquals("xml", map.assignPrefix("xml", XML_NS_URI));
        assertEquals("xmlns", map.assignPrefix("xmlns", XMLNS_ATTRIBUTE_NS_URI));        
        assertEquals("xs", map.assignPrefix("xs", W3C_XML_SCHEMA_NS_URI));
        assertEquals("xxmlt", map.assignPrefix("xxmlt", "http://example.com/xmlt/1.0/"));
        assertEquals("foo", map.assignPrefix("foo", "http://example.com/foo"));
        assertEquals("foo", map.assignPrefix("foo", "http://example.com/foo"));
        assertEquals("foo_1", map.assignPrefix("foo", "http://example.com/foo1"));
        assertEquals("foo_2", map.assignPrefix("foo", "http://example.com/foo2"));  
    
        assertEquals("nc", map.getPrefix("http://example.com/nc/6.0/"));
        assertEquals("ct", map.getPrefix("http://example.com/ct/6.0/"));
        assertEquals("nc_5" ,map.getPrefix("http://example.com/nc/5.0/"));
        assertEquals("ct_4", map.getPrefix("http://examplelcom/ct/4.0"));
        assertEquals("xml", map.getPrefix(XML_NS_URI));  
        assertEquals("xmlns", map.getPrefix(XMLNS_ATTRIBUTE_NS_URI));        
        assertEquals("xs", map.getPrefix(W3C_XML_SCHEMA_NS_URI));        
        assertEquals("xxmlt", map.getPrefix("http://example.com/xmlt/1.0/"));
        assertEquals("foo", map.getPrefix("http://example.com/foo"));
        assertEquals("foo_1", map.getPrefix("http://example.com/foo1"));
        assertEquals("foo_2", map.getPrefix("http://example.com/foo2"));

        assertEquals("http://example.com/nc/6.0/", map.getURI("nc"));
        assertEquals("http://example.com/ct/6.0/", map.getURI("ct"));
        assertEquals("http://example.com/nc/5.0/", map.getURI("nc_5"));
        assertEquals("http://examplelcom/ct/4.0", map.getURI("ct_4"));
        assertEquals(XML_NS_URI, map.getURI("xml"));  
        assertEquals(XMLNS_ATTRIBUTE_NS_URI, map.getURI("xmlns"));        
        assertEquals(W3C_XML_SCHEMA_NS_URI, map.getURI("xs"));        
        assertEquals("http://example.com/xmlt/1.0/", map.getURI("xxmlt"));
        assertEquals("http://example.com/foo", map.getURI("foo"));
        assertEquals("http://example.com/foo1", map.getURI("foo_1"));
        assertEquals("http://example.com/foo2", map.getURI("foo_2"));
    }
    
}
