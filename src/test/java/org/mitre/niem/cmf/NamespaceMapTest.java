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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.niem.NIEMConstants.APPINFO_NS_URI_PREFIX;
import static org.mitre.niem.NIEMConstants.OWL_NS_URI;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NamespaceMapTest {
    
    private NamespaceMap nm;
    
    public NamespaceMapTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
        nm = new NamespaceMap();
    }
    
    @AfterEach
    public void tearDown() {
    }
    
    @Test
    public void testGetPrefix() {
    }

    @Test
    public void testGetURI() {
    }

    @Test
    public void testAssignPrefix() {
        assertEquals("foo", nm.assignPrefix("foo", "http://example.com/foo/"));
        assertEquals("foo", nm.getPrefix("http://example.com/foo/"));
        assertEquals("http://example.com/foo/", nm.getURI("foo"));
        
        assertNull(nm.getURI("bar"));
        assertNull(nm.getPrefix("http://example.com/bar/"));
        
        String p = nm.assignPrefix("owl", "frogs");
        assertEquals("owl_1", p);
        assertEquals("owl_1", nm.getPrefix("frogs"));
        assertEquals("frogs", nm.getURI("owl_1"));
        assertEquals(OWL_NS_URI, nm.getURI("owl"));
        
        String a5 = APPINFO_NS_URI_PREFIX + "5.0/";
        p = nm.assignPrefix("appinfo", a5);
        assertEquals("appinfo", p);
        
        String a4 = APPINFO_NS_URI_PREFIX + "4.0/";
        p = nm.assignPrefix("appinfo", a4);
        assertEquals("appinfo_4", p);
        assertEquals("appinfo_4", nm.getPrefix(a4));
        assertEquals(a4, nm.getURI("appinfo_4"));
        
        String nc5 = "http://release.niem.gov/niem/niem-core/5.0/";
        p = nm.assignPrefix("nc", nc5);
        assertEquals("nc", p);
        
        String nc4 = "http://release.niem.gov/niem/niem-core/4.0/";
        p = nm.assignPrefix("nc", nc4);
        assertEquals("nc_4", p);
        assertEquals("nc_4", nm.getPrefix(nc4));
        assertEquals(nc4, nm.getURI("nc_4"));
    }
    
    @Test
    public void testChangePrefix() {
        String nc5 = "http://release.niem.gov/niem/niem-core/5.0/";
        String p = nm.assignPrefix("nc", nc5);
        assertEquals("nc", p);
        
        p = nm.changePrefix("nc5", nc5);
        assertEquals("nc5", p);
        assertEquals("nc5", nm.getPrefix(nc5));
        assertEquals(nc5, nm.getURI("nc5"));
        assertNull(nm.getPrefix("nc"));
    }
    
}
