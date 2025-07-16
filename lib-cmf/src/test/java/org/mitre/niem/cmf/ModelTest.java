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
package org.mitre.niem.cmf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.niem.cmf.Model.uriToName;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelTest {
    
    public ModelTest() {
    }

    @Test
    public void testURItoName () throws Exception {
        assertEquals("LocalName", uriToName("urn:some:NS:LocalName"));
        assertEquals("Name", uriToName("http://someNS/Name"));
        assertEquals("name", uriToName("http://some/other/NS/name"));
        assertEquals("ObjPoint", uriToName("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/ObjPoint"));
    }

    @Test
    public void testURItoNSU () throws Exception {
        var m = new Model();
        var ns1 = new Namespace("u", "urn:some:NS");
        var ns2 = new Namespace("n2", "http://someNS/");
        var ns3 = new Namespace("n3", "http://some/other/NS");
        m.addNamespace(ns1);
        m.addNamespace(ns2);
        m.addNamespace(ns3);
        assertEquals("urn:some:NS", m.uriToNSU("urn:some:NS:LocalName"));
        assertEquals("http://someNS/", m.uriToNSU("http://someNS/Name"));
        assertEquals("http://some/other/NS", m.uriToNSU("http://some/other/NS/name"));
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/",
            m.uriToNSU("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/ObjPoint"));
    }

    @Test
    public void testURItoNSObj () throws Exception {
        var m = new Model();
        var ns1 = new Namespace("u", "urn:some:NS");
        var ns2 = new Namespace("n2", "http://someNS/");
        var ns3 = new Namespace("n3", "http://some/other/NS");
        m.addNamespace(ns1);
        m.addNamespace(ns2);
        m.addNamespace(ns3);
        assertEquals(ns1, m.uriToNSObj("urn:some:NS:LocalName"));
        assertEquals(ns2, m.uriToNSObj("http://someNS/Name"));
        assertEquals(ns3, m.uriToNSObj("http://some/other/NS/name"));
        assertEquals(null, m.uriToNSObj("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/ObjPoint"));
    }
    
    @Test
    public void testURItoQN () throws Exception {
        var m = new Model();
        var ns1 = new Namespace("u", "urn:some:NS");
        var ns2 = new Namespace("n2", "http://someNS/");
        var ns3 = new Namespace("n3", "http://some/other/NS");
        m.addNamespace(ns1);
        m.addNamespace(ns2);
        m.addNamespace(ns3);
        assertEquals("u:LocalName", m.uriToQN("urn:some:NS:LocalName"));
        assertEquals("n2:Name", m.uriToQN("http://someNS/Name"));
        assertEquals("n3:name", m.uriToQN("http://some/other/NS/name"));
        assertEquals("", m.uriToQN("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/ObjPoint"));
    }

    @Test
    public void testQNtoURI () throws Exception {
        var m = new Model();
        var ns1 = new Namespace("u", "urn:some:NS");
        var ns2 = new Namespace("n2", "http://someNS/");
        var ns3 = new Namespace("n3", "http://some/other/NS");
        m.addNamespace(ns1);
        m.addNamespace(ns2);
        m.addNamespace(ns3);
        assertEquals("urn:some:NS:LocalName", m.qnToURI("u:LocalName"));
        assertEquals("http://someNS/Name", m.qnToURI("n2:Name"));
        assertEquals("http://some/other/NS/name", m.qnToURI("n3:name"));
        assertEquals("", m.qnToURI("structures:ObjPoint"));
    }
    
}
