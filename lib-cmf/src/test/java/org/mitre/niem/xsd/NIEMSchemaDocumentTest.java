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
        assertEquals("NIEM5.0", sd.niemVersion());
        assertEmptyLogs();
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
    public void writeLogs () {
        for (var log : logs) {
            var errors = log.getErrorLogs();
            var warns  = log.getWarnLogs();
            for (var msg : errors) System.out.println(msg);
            for (var msg : warns) System.out.println(msg);            
        }
    }

}
