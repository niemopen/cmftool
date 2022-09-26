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
import javax.xml.parsers.ParserConfigurationException;
import nl.altindag.log.LogCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import org.javatuples.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLSchemaTest {
    private static final String testDirPath = "src/test/resources";
//    private static LogCaptor logCaptor;
//
//    @BeforeAll
//    public static void setupLogCaptor() {
//        logCaptor = LogCaptor.forClass(XMLSchema.class);
//    }
//    
//    @AfterEach
//    public void clearLogs() {
//        logCaptor.clearLogs();
//    }
//    
//    @AfterAll
//    public static void tearDown() {
//        logCaptor.close();
//    }  
    
    public XMLSchemaTest() { }
   
    @Test
    public void testGGS () throws XMLSchema.XMLSchemaException, IOException, SAXException, ParserConfigurationException {
        var s = new XMLSchema(ga("cat/cat1.xml", "xsd/union.xsd", "http://example.com/external-content/"));
        assertEquals(1, s.catalogs().size());
        assertEquals(2, s.schemaDocs().size());
        assertEquals(1, s.initialNS().size());   
    }
    
    @Test
    @DisplayName("file URI")
    public void testGS01 () throws XMLSchema.XMLSchemaException, IOException {
        var s = new XMLSchema(ga("file:/C:/Work/NetBeans/CMFTool/src/test/resources/xsd/list.xsd"));
        assertEquals(0, s.catalogs().size());
        assertEquals(1, s.schemaDocs().size());
        assertEquals(0, s.initialNS().size());
    }
    
    @Test
    @DisplayName("no such file URI")
    public void testGS02 () throws XMLSchema.XMLSchemaException, IOException {  
        var thrown = Assertions.assertThrows(IOException.class, () -> {
           var s = new XMLSchema(ga("file:/C:/Work/NetBeans/CMFTool/src/test/nosuchfile"));
        });
        assertThat(thrown.getMessage()).contains("cannot find the file");         
    }
    
    @Test
    @DisplayName("file URI with hostname")
    public void testGS03 () throws XMLSchema.XMLSchemaException, IOException {    
        var thrown = Assertions.assertThrows(XMLSchema.XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("file://google.com/some/file/path"));
        });
        assertThat(thrown.getMessage()).contains("hostname not allowed");   
    }
    
    @Test
    @DisplayName("not a schema or catalog")
    public void testGS04 () throws XMLSchema.XMLSchemaException, IOException {    
        var thrown = Assertions.assertThrows(XMLSchema.XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("/cmf/codeType.cmf"));
        });
        assertThat(thrown.getMessage()).contains("not a schema document or XML catalog"); 
    }
    
    @Test
    @DisplayName("no catalog for initial NS")
    public void testGS05 () throws XMLSchema.XMLSchemaException, IOException {
        var thrown = Assertions.assertThrows(XMLSchema.XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("xsd/union.xsd", "http://example.com/external-content/"));
        });
        assertThat(thrown.getMessage()).contains("can't resolve"); 
    }    
    
    @Test
    @DisplayName("can't resolve initial NS")
    public void testGS06 () throws XMLSchema.XMLSchemaException, IOException {
        var thrown = Assertions.assertThrows(XMLSchema.XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("cat/cat1.xml", "xsd/union.xsd", "http://example.com/not-in-catalog/"));
        });
        assertThat(thrown.getMessage()).contains("can't resolve");         
    }    

    @Test
    @DisplayName("resolves to remote resource")
    public void testGS08 () throws XMLSchema.XMLSchemaException, IOException {
        var thrown = Assertions.assertThrows(XMLSchema.XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("cat/cat1.xml", "xsd/union.xsd", "http://example.com/remote-resource/"));
        });
        assertThat(thrown.getMessage()).contains("not a local URI");         
    }   
    
    @Test
    @DisplayName("resolved intial NS not XSD")
    public void testGS09 () throws XMLSchema.XMLSchemaException, IOException {
        var thrown = Assertions.assertThrows(XMLSchema.XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("cat/cat1.xml", "xsd/union.xsd", "http://example.com/not-xsd/"));
        });
        assertThat(thrown.getMessage()).contains("not a schema document"); 
     
    }
    
    @Test
    @DisplayName("resolved intial NS doesn't exist")
    public void testGS10 () throws XMLSchema.XMLSchemaException, IOException {
        var thrown = Assertions.assertThrows(IOException.class, () -> {
           var s = new XMLSchema(ga("cat/cat1.xml", "xsd/union.xsd", "http://example.com/no-such-file/"));
        });
        assertThat(thrown.getMessage()).contains("cannot find the file"); 
    }    
    
    @Test
    @DisplayName("resolved initial NS has wrong target namespace")
    public void testGS11 () throws XMLSchema.XMLSchemaException, IOException {
        var thrown = Assertions.assertThrows(XMLSchema.XMLSchemaException.class, () -> {
           var s = new XMLSchema(ga("cat/cat1.xml", "xsd/union.xsd", "http://example.com/Foo/1.0/"));
        });
        assertThat(thrown.getMessage()).contains("wrong target namespace");
    }   
    
    @Test
    public void testParseSchemaPile () throws XMLSchema.XMLSchemaException, IOException, SAXException, ParserConfigurationException {
        var s = new XMLSchema(ga("xsd/externals.xsd"));
        String pileRoot = s.pileRoot();
        assertEquals("file:/C:/Work/NetBeans/CMFTool/src/test/resources/xsd/", pileRoot);
        
        var sd = s.schemaDocuments().get("http://example.com/external-content/");
        assertEquals(0, sd.schemaKind());
        assertEquals("externals.xsd", sd.filepath());
        
        sd = s.schemaDocuments().get("http://release.niem.gov/niem/proxy/niem-xs/5.0/");
        assertEquals(4, sd.schemaKind());
        assertEquals("externals-niem/adapters/niem-xs.xsd", sd.filepath());
        
        sd = s.schemaDocuments().get("http://www.w3.org/1999/xlink");
        assertEquals(8, sd.schemaKind());   
        assertEquals("externals-niem/external/ogc/xlink/1.0.0/xlinks.xsd", sd.filepath());
        assertEquals(null, sd.niemVersion());
        
        sd = s.schemaDocuments().get("http://www.opengis.net/gml/3.2");
        assertEquals(7, sd.schemaKind());  
        assertEquals("externals-niem/external/ogc/gml/3.2.1/gml.xsd", sd.filepath());
        assertEquals(null, sd.niemVersion());
    }
    
    private String[] ga (String ... args) {
        String[] rv = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("http:") || a.startsWith("file:")) rv[i] = a;
            else rv[i] = testDirPath + "/" + a;
        }
        return rv;
    }
    
}
