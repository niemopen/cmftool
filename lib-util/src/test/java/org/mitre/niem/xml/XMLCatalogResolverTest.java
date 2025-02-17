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
package org.mitre.niem.xml;

import java.io.File;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.niem.xml.XMLCatalogResolver.NO_MAP;
import static org.mitre.niem.xml.XMLCatalogResolver.REMOTE_MAP;
import static org.mitre.niem.utility.URIfuncs.FileToCanonicalURI;
import static org.mitre.niem.utility.URIfuncs.URIStringToFile;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLCatalogResolverTest {
    private final static String resDN  = "src/test/resources/";
    private final static File resDF  = new File(resDN);
    private final static String resDUs = FileToCanonicalURI(resDF).toString();
        
    public XMLCatalogResolverTest() {
    }

    @Test
    public void testCat1() throws Exception {
        String[] args = { resDN + "cat/cat1.xml" };
        var r    = new XMLCatalogResolver(args);
        var msg  = r.allMessages();
        var cats = r.allCatalogs();
        var maps = r.allResolutions();
        var res = r.resolveURI("http://example.com/goodXsTest/");
        var rF  = URIStringToFile(res);
        assertTrue(msg.isEmpty());
        assertTrue(rF.canRead());
        assertEquals(REMOTE_MAP, r.resolveURI("http://example.com/remote-resource/"));
        assertEquals(REMOTE_MAP, r.resolveURI("http://example.com/other-remote/"));
        assertEquals(NO_MAP, r.resolveURI("boogla"));
        assertEquals(r.resolveURI("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/"),
                resDUs + "xsd/niem/utility/structures.xsd");
        assertThat(cats).containsExactlyInAnyOrder(
                resDUs + "cat/cat1.xml",
                resDUs + "xsd/niem/xml-catalog.xml",
                resDUs + "xsd/niem/codes/genc/xml-catalog.xml" );
        assertEquals(maps.get("http://example.com/remote-resource/"), "REMOTE MAP");
        assertEquals(maps.get("http://example.com/other-remote/"), "REMOTE MAP");
        assertEquals(maps.get("boogla"), "NO MAP");
        assertEquals(5, maps.size());   
    }
    
    @Test
    public void testBad1 () throws Exception {
        String[] args = { resDN + "cat/bad1.xml" };
        var r   = new XMLCatalogResolver(args);
        var cm  = r.allMessages();
        assertEquals(0, r.allCatalogs().size());
        assertThat(cm).anySatisfy(s -> s.contains("Invalid content"));
    }
    
    @Test
    public void testBad2 () throws Exception {
        String[] args = { resDN + "cat/bad2.xml" };
        var r   = new XMLCatalogResolver(args);
        var cm  = r.allMessages();
        var ac  = new ArrayList<>(r.allCatalogs());
        var c   = ac.get(0);    
        assertEquals(1, ac.size());
        assertTrue(c.endsWith("bad2.xml"));
        assertThat(cm).anySatisfy(s -> s.contains("not found"));
    }
    
}
