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
package org.mitre.niem.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import nl.altindag.log.LogCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.mitre.niem.utility.URIfuncs.FileToCanonicalURI;
import org.mitre.niem.xml.XMLSchemaException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLSchemaTest {
    
    public static List<LogCaptor> logs;    
    private static final String resDN  = "src/test/resources/";
    private static final File resDF    = new File(resDN);
    private static final String resDUs = FileToCanonicalURI(resDF).toString();    
    
    @BeforeAll
    public static void setupLogCaptor () {
        logs = new ArrayList<>();
        logs.add(LogCaptor.forClass(XMLSchema.class));
        logs.add(LogCaptor.forClass(XMLSchemaDocument.class));
    }
    
    @AfterEach
    public void clearLogs () {
        for (var log : logs) log.clearLogs();;
    }
    
    @AfterAll
    public static void tearDown () {
        for (var log : logs) log.close();
    } 
    
    public XMLSchemaTest() { 
    }

    @Test
    public void testGoodXs () throws Exception {
        String[] args = new String[]{
            resDN + "xsd/goodXsTest.xsd",
            resDN + "xsd/niem/xml-catalog.xml",
            resDN + "cat/cat1.xml"} ;
        var sch = new XMLSchema(args);
        assertEquals(sch.pileRoot(), resDUs);
        assertThat(sch.initialSchemaDocs())
                .containsExactly(resDUs + "xsd/goodXsTest.xsd");
        assertThat(sch.initialCatalogs())
                .containsExactlyInAnyOrder(
                    resDUs + "xsd/niem/xml-catalog.xml",
                    resDUs + "cat/cat1.xml");
        assertNotNull(sch.resolver());
        assertTrue(sch.initialNS().isEmpty());
        assertNotNull(sch.xsmodel());
        assertTrue(sch.xsModelMsgs().isEmpty());
        assertNotNull(sch.javaxSchema());
        assertTrue(sch.javaXMsgs().isEmpty());
        assertThat(sch.schemaNamespaceUs())
                .hasSize(10);
        for (var nsuri : sch.schemaNamespaceUs()) {
            assertEquals(nsuri, sch.schemaDocument(nsuri).targetNamespace());
        }
    }
    
    @Test
    public void testWithCatalog () throws Exception {
        String[] args = new String[]{
            resDN + "xsd/withCatalog.xsd",
            resDN + "xsd/niem/xml-catalog.xml"};
        var sch = new XMLSchema(args);
        assertThat(sch.schemaNamespaceUs()).hasSize(10);
    }
    
    @Test
    public void testBadXs () throws Exception {
        String[] args = new String[]{
            resDN + "xsd/badXsTest.xsd"};
        var sch = new XMLSchema(args);
        assertFalse(sch.xsModelMsgs().isEmpty());
        assertFalse(sch.javaXMsgs().isEmpty());
    }
    
    
    @Test
    public void testInitialNS () throws Exception {
        var s = new XMLSchema(ga("cat/cat1.xml", "http://example.com/goodXsTest/"));
        var pr = s.pileRoot();
//        assertEquals(3, s.resolver().allCatalogs().size());
        assertEquals(1, s.initialSchemaDocs().size());
        assertEquals(1, s.initialNS().size());   
        assertEmptyLogs();
    }    
    
    @Test
    public void testFileURI () throws Exception {
        var s = new XMLSchema(ga(resDUs + "xsd/goodXsTest.xsd"));
        assertEquals(0, s.resolver().allCatalogs().size());
        assertEquals(1, s.initialSchemaDocs().size());
        assertEquals(0, s.initialNS().size());
        assertEmptyLogs();
    }    
    
    @Test
    public void testBadFileURI () throws Exception {  
        var thrown = Assertions.assertThrows(XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga(resDUs + "nosuchfile"));
        });
        assertThat(thrown.getMessage()).contains("cannot find the file");         
    }
    
    @Test
    public void testBadURIWithHostname () throws Exception {    
        var thrown = Assertions.assertThrows(XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("file://google.com/some/file/path"));
        });
        assertThat(thrown.getMessage()).contains("A hostname is not allowed");   
    }
    
    @Test
    public void testNotCatalogOrSchema () throws Exception {    
        var thrown = Assertions.assertThrows(XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("/xsd/00-README.txt"));
        });
        assertThat(thrown.getMessage()).contains("not a schema document or XML catalog"); 
    }
    
    @Test
    public void testNoCatForInitialNS () throws Exception {
        var thrown = Assertions.assertThrows(XMLSchemaException.class, () -> {
           var s = new XMLSchema("http://example.com/goodXsTest/");
        });
        assertThat(thrown.getMessage()).contains("Can't resolve"); 
    }    
    
    @Test
    public void testCantResolveInitialNS () throws Exception {
        var thrown = Assertions.assertThrows(XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("cat/cat1.xml", "http://example.com/not-in-catalog/"));
        });
        assertThat(thrown.getMessage()).contains("Can't resolve");         
    }    

    @Test
    public void testResolvesToRemote () throws Exception {
        var thrown = Assertions.assertThrows(XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("cat/cat1.xml", "http://example.com/remote-resource/"));
        });
        assertThat(thrown.getMessage()).contains("not a local URI");         
    }   
    
    @Test
    public void testResolvesToNotXSD () throws Exception {
        var thrown = Assertions.assertThrows(XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("cat/cat1.xml", "http://example.com/not-xsd/"));
        });
        assertThat(thrown.getMessage()).contains("not a schema document");   
    }
    
    @Test
    public void testResolvesToNoSuchFile () throws Exception {
        var thrown = Assertions.assertThrows(XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("cat/cat1.xml", "http://example.com/no-such-file/"));
        });
        assertThat(thrown.getMessage()).contains("cannot find the file"); 
    }    
    
    @Test
    public void testResolvesToWrongTargetNS () throws Exception {
        var thrown = Assertions.assertThrows(XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("cat/cat1.xml", "http://example.com/Foo/1.0/"));
        });
        assertThat(thrown.getMessage()).contains("wrong target namespace");
    }   
                        
    public void assertEmptyLogs () {
        for (var log : logs) {
            var errors = log.getErrorLogs();
            var warns  = log.getWarnLogs();
            assertThat(errors.isEmpty());
            assertThat(warns.isEmpty());
        }
    }    
        
    // Builds a String[] array from a list of String arguments. Strings that aren't
    // URIs are assumed to be file names in the test directory.
    private String[] ga (String ... args) {
        String[] rv = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("http:") || a.startsWith("file:")) rv[i] = a;
            else rv[i] = resDN + a;
        }
        return rv;
    }    
}
