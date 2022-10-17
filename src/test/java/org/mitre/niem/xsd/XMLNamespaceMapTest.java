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
package org.mitre.niem.xsd;

import java.io.IOException;
import org.apache.xerces.util.XMLCatalogResolver;
import static org.assertj.core.api.Assertions.assertThat;
import org.javatuples.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLNamespaceMapTest {
    
    public XMLNamespaceMapTest() {
    }
    

    @Test
    public void test () throws XMLNamespaceScope.XMLNamespaceMapException {
        var nsm = new XMLNamespaceScope();
        nsm.onStartPrefixMapping("p1", "foo");
        nsm.onStartPrefixMapping("p2", "bar");
        assertEquals(Pair.with("foo", "fred"), nsm.expandQName("p1:fred"));
        assertEquals(Pair.with("bar", "dick"), nsm.expandQName("p2:dick"));
        nsm.onStartElement();
        assertEquals(Pair.with("foo", "fred"), nsm.expandQName("p1:fred"));
        assertEquals(Pair.with("bar", "dick"), nsm.expandQName("p2:dick")); 
        
        nsm.onStartPrefixMapping("p1", "xyz");
        assertEquals(Pair.with("xyz", "fred"), nsm.expandQName("p1:fred"));
        assertEquals(Pair.with("bar", "dick"), nsm.expandQName("p2:dick"));        
        
        nsm.onStartElement();
        assertEquals(Pair.with("xyz", "fred"), nsm.expandQName("p1:fred"));
        assertEquals(Pair.with("bar", "dick"), nsm.expandQName("p2:dick"));   
        
        nsm.onEndElement();
        assertEquals(Pair.with("xyz", "fred"), nsm.expandQName("p1:fred"));
        assertEquals(Pair.with("bar", "dick"), nsm.expandQName("p2:dick"));   
        
        nsm.onEndElement();
        assertEquals(Pair.with("foo", "fred"), nsm.expandQName("p1:fred"));
        assertEquals(Pair.with("bar", "dick"), nsm.expandQName("p2:dick"));   
    }
    
    @Test
    public void testException () throws XMLNamespaceScope.XMLNamespaceMapException {
      var nsm = new XMLNamespaceScope();  
      
      Pair<String,String> res = nsm.expandQName("unprefixed");
      assertEquals(null, res.getValue0());
      assertEquals("unprefixed", res.getValue1());
      
      var thrown = Assertions.assertThrows(XMLNamespaceScope.XMLNamespaceMapException.class, () -> {
           Pair<String,String> eqn = nsm.expandQName("pr:");
      });
      assertThat(thrown.getMessage()).contains("not a valid QName");      
      
      thrown = Assertions.assertThrows(XMLNamespaceScope.XMLNamespaceMapException.class, () -> {
           Pair<String,String> eqn = nsm.expandQName("nc:foo:bzr");
      });
      assertThat(thrown.getMessage()).contains("not a valid QName"); 
      
      thrown = Assertions.assertThrows(XMLNamespaceScope.XMLNamespaceMapException.class, () -> {
           Pair<String,String> eqn = nsm.expandQName("pr:name");
      });
      assertThat(thrown.getMessage()).contains("no binding in scope"); 
    }
}
