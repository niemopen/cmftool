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

import java.util.ArrayList;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.mitre.niem.xml.LanguageString;
import org.mitre.niem.xml.XMLSchemaDocument;
import static org.mitre.niem.xsd.NamespaceKind.*;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NIEMSchemaTest {
    private final static String resDN = "src/test/resources/";
        
    public NIEMSchemaTest() {
    }

    @Test
    public void testGetNamespaceKind () throws Exception {
        var args = new String[]{resDN + "xsd/imports.xsd"};
        var sch = new NIEMSchema(args);
        assertEquals(NSK_EXTENSION, sch.namespaceKind("http://example.com/test/"));
        assertEquals(NSK_DOMAIN, sch.namespaceKind("https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/"));
        assertEquals(NSK_CORE, sch.namespaceKind("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"));
        assertEquals(NSK_OTHERNIEM, sch.namespaceKind("https://docs.oasis-open.org/niemopen/ns/model/codes/aamva_d20/6.0/"));
        assertEquals(NSK_APPINFO, sch.namespaceKind("https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/"));
        assertEquals(NSK_CLI, sch.namespaceKind("https://docs.oasis-open.org/niemopen/ns/specification/code-lists/6.0/instance/"));
        var x = sch.namespaceKind("https://docs.oasis-open.org/niemopen/ns/model/adapters/niem-xs/6.0/");

        assertEquals(NSK_NIEM_XS, sch.namespaceKind("https://docs.oasis-open.org/niemopen/ns/model/adapters/niem-xs/6.0/"));
        assertEquals(NSK_STRUCTURES, sch.namespaceKind("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/"));
        assertEquals(NSK_XSD, sch.namespaceKind("http://www.w3.org/2001/XMLSchema"));
        assertEquals(NSK_XML, sch.namespaceKind("http://www.w3.org/XML/1998/namespace"));
        assertEquals(NSK_EXTERNAL, sch.namespaceKind("http://www.opengis.net/gml/3.2"));
        assertEquals(NSK_NOTNIEM, sch.namespaceKind("http://www.w3.org/1999/xlink"));
        assertEmptyLogs();
        assertEquals(NSK_UNKNOWN,   sch.namespaceKind("http://boogala"));
        assertThat(logs).anySatisfy(
            log -> { assertThat(log.getErrorLogs()).anyMatch(s -> s.contains("no schema document for namespace URI")); }
        );    
    }

    @Test
    public void testNamespaceMap() throws Exception {
        var args = new String[]{resDN + "xsd/namespaceMap.xsd"};
        var sch  = new NIEMSchema(args);
        var nmap = sch.namespaceMap();
        assertEquals("nc", nmap.getPrefix("http://example.com/nsOverride/"));
        assertEquals("j", nmap.getPrefix("http://example.com/jOverride/"));
        assertEquals("gml", nmap.getPrefix("http://example.com/gmlOverride/"));
        assertEquals("http://example.com/nsOverride/", nmap.getURI("nc"));
        assertEquals("http://example.com/jOverride/", nmap.getURI("j"));
        assertEquals("http://example.com/gmlOverride/", nmap.getURI("gml"));
        
        assertEquals("nc_6", nmap.getPrefix("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"));
        assertEquals("j_6", nmap.getPrefix("https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/"));
        assertEquals("gml_3", nmap.getPrefix("http://www.opengis.net/gml/3.2"));
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/", nmap.getURI("nc_6"));
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/", nmap.getURI("j_6"));
        assertEquals("http://www.opengis.net/gml/3.2", nmap.getURI("gml_3"));
        assertEmptyLogs();
    }
    
    
    public static List<LogCaptor> logs;      
    @BeforeAll
    public static void setupLogCaptor () {
        logs = new ArrayList<>();
        logs.add(LogCaptor.forClass(NIEMSchema.class));
        logs.add(LogCaptor.forClass(NIEMSchemaDocument.class));
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
    public void assertEmptyLogs () {
        for (var log : logs) {
            var errors = log.getErrorLogs();
            var warns  = log.getWarnLogs();
            assertThat(errors.isEmpty());
            assertThat(warns.isEmpty());
        }
    }  
}
