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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import nl.altindag.log.LogCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.mitre.niem.cmf.LocalTerm;
import org.mitre.niem.xml.XMLSchemaDocument;
import static org.mitre.niem.xsd.NIEMConstants.CTAS30;
import static org.mitre.niem.xsd.NIEMConstants.CTAS60;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NIEMSchemaDocumentTest {    
    private final static File resDF    = new File("src/test/resources/");
    
    public NIEMSchemaDocumentTest() {
    }
    
    @Test
    public void testCtasNS () throws Exception {
        var sd = new NIEMSchemaDocument(new File(resDF, "xsd6/class.xsd"));
        var ct = sd.ctasNS();
        assertEquals(CTAS60, ct);
        
        sd = new NIEMSchemaDocument(new File(resDF, "xsd5/class.xsd"));
        ct = sd.ctasNS();
        assertEquals(CTAS30, ct);
        
        sd = new NIEMSchemaDocument(new File(resDF, "xsd6/niem/utility/structures.xsd"));
        ct = sd.ctasNS();
        assertEquals("", ct);
        assertEmptyLogs();
    }

    @Test
    public void testCtAssertions () throws Exception {
        var sd   = new NIEMSchemaDocument(new File(resDF, "xsd6/class.xsd"));
        var ctaL = sd.ctAssertions();
        assertThat(ctaL).containsExactly(
            "https://docs.oasis-open.org/niemopen/ns/specification/NDR/6.0/#SubsetSchemaDocument");
        
        sd = new NIEMSchemaDocument(new File(resDF, "xsd/ctas.xsd"));
        ctaL = sd.ctAssertions();
        assertThat(ctaL).containsExactlyInAnyOrder(
            "http://example.com/#ONE", "http://example.com/#TWO");
        
        sd = new NIEMSchemaDocument(new File(resDF, "xsd6/niem/utility/structures.xsd"));
        ctaL= sd.ctAssertions();
        assertTrue(ctaL.isEmpty());
        assertEmptyLogs();
    }
    
    @Test
    public void testNIEMVersion () throws Exception {
        var sd = new NIEMSchemaDocument(new File(resDF, "xsd6/niem/utility/structures.xsd"));
        assertEquals("NIEM6.0", sd.niemVersion());
        assertEmptyLogs();
        
        sd = new NIEMSchemaDocument(new File(resDF, "xsd/imports.xsd"));
        assertEquals("NIEM6.0", sd.niemVersion());
        assertEmptyLogs();
        
        sd = new NIEMSchemaDocument(new File(resDF, "xsd/version-1.xsd"));
        assertEquals("NIEM6.0", sd.niemVersion());
        assertThat(logs).anySatisfy(
            log -> { assertThat(log.getWarnLogs()).anyMatch(s -> s.contains("conflicting versions from CTAs (")); }
        );
        clearLogs();
        
        sd = new NIEMSchemaDocument(new File(resDF, "xsd/version-2.xsd"));
        assertEquals("NIEM6.0", sd.niemVersion());
        assertThat(logs).anySatisfy(
            log -> { assertThat(log.getWarnLogs()).anyMatch(s -> s.contains("conflicting versions from CTA and")); }
        );        
    }
    
    @Test
    public void testLocalTerms () throws Exception {
        var sd = new NIEMSchemaDocument(new File(resDF, "xsd6/localTerm.xsd"));
        var ltL = sd.localTerms();
        assertThat(ltL).extracting(LocalTerm::term).containsExactly("2D", "3D", "Test");
        assertThat(ltL).extracting(LocalTerm::literal).containsExactly("Two-dimensional", "", "");
        assertThat(ltL).extracting(LocalTerm::documentation)
            .containsExactly("", "Three-dimensional", "only for test purposes");
        assertTrue(ltL.get(0).sourceL().isEmpty());
        assertTrue(ltL.get(1).sourceL().isEmpty());   
        assertTrue(ltL.get(0).citationL().isEmpty());
        assertTrue(ltL.get(1).citationL().isEmpty()); 
        assertThat(ltL.get(2).sourceL())
            .containsExactly("http://example.com/1", "http://example.com/2");
        assertThat(ltL.get(2).citationL())
            .containsExactly("citation #1", "citation #2");
    }
    
    @Test
    public void testAllImports () throws Exception {
        var sd   = new NIEMSchemaDocument(new File(resDF, "xsd/imports.xsd"));
        var impL = sd.allImports();
        assertThat(impL)
            .satisfiesExactlyInAnyOrder(
                obj -> { assertThat("https://docs.oasis-open.org/niemopen/ns/model/domains/justice/6.0/".equals(obj.imported())); },
                obj -> { assertThat("https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/".equals(obj.imported())); },
                obj -> { assertThat("https://docs.oasis-open.org/niemopen/ns/model/adapters/niem-xs/6.0/".equals(obj.imported())); },
                obj -> { assertThat("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/".equals(obj.imported())); 
                         assertThat(!obj.isExternal()); },
                obj -> { assertThat("http://www.opengis.net/gml/3.2".equals(obj.imported()));
                         assertThat(obj.isExternal());
                         assertThat(obj.docL())
                             .hasSize(1)
                             .satisfiesExactly(
                                 ls -> { ls.text().contains("Geography");
                                         ls.lang().equals("en-US"); }
                             );
                });
        assertEmptyLogs();
    }
    
    @Test
    public void testBuiltinNS () throws Exception {
        var sd = new NIEMSchemaDocument(new File(resDF, "xsd/builtin-1.xsd"));
        var strNS = sd.builtinNS("STRUCTURES");
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/", strNS);
        assertEmptyLogs();
        
        strNS = sd.builtinNS("nosuch");
        assertEquals("", strNS);
        assertThat(logs).anySatisfy(
            log -> { assertThat(log.getErrorLogs()).anyMatch(s -> s.contains("unknown builtin")); }
        );
        for (var log : logs) log.clearLogs();;
        strNS = sd.builtinNS("APPINFO");
        assertEquals("https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/", strNS);
        assertThat(logs).anySatisfy(
            log -> { assertThat(log.getWarnLogs()).anyMatch(s -> s.contains("has conflicting")); }
        );
        
    }
    
    
    public static List<LogCaptor> logs;      
    @BeforeAll
    public static void setupLogCaptor () {
        logs = new ArrayList<>();
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
