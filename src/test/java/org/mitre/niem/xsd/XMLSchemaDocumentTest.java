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

import java.util.ArrayList;
import java.util.List;
import nl.altindag.log.LogCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import org.javatuples.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.mitre.niem.cmf.LocalTerm;
import org.mitre.niem.cmf.NamespaceKind;
import static org.mitre.niem.cmf.NamespaceKind.NSK_CORE;
import static org.mitre.niem.cmf.NamespaceKind.NSK_DOMAIN;
import static org.mitre.niem.cmf.NamespaceKind.NSK_EXTENSION;
import static org.mitre.niem.cmf.NamespaceKind.NSK_OTHERNIEM;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UTILITY;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XML;


/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLSchemaDocumentTest {
    
    public XMLSchemaDocumentTest () {
    }
    
    public static List<LogCaptor> logs;
    
    @BeforeAll
    public static void setupLogCaptor () {
        logs = new ArrayList<>();
        logs.add(LogCaptor.forClass(XMLSchemaDocument.class));
        logs.add(LogCaptor.forClass(NamespaceKind.class));
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
    
    @Test
    public void testParse () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd5/xmlschemadoc/niem-core.xsd", "");
        
        assertEquals(8, sd.namespaceDecls().size());
        assertThat(sd.namespaceDecls())
                .hasSize(8)
                .extracting(XMLNamespaceDeclaration::decPrefix)
                .containsOnly("appinfo", "ct", "nc", "niem-xs", "structures", "xs", "xsi", "foo");
        assertThat(sd.namespaceDecls())
                .filteredOn(nsd -> nsd.decPrefix().equals("foo"))
                .extracting(nsd -> nsd.elementDepth())
                .containsOnly(2);
        
        assertThat(sd.externalImports())
                .hasSize(1)
                .containsOnly("http://www.opengis.net/gml/3.2");
        
        assertThat(sd.appinfoAtts())
                .hasSize(3)
                .extracting(a -> a.attLname()).containsOnly("deprecated", "orderedPropertyIndicator")
                ;
        for (AppinfoAttribute a : sd.appinfoAtts()) {
            if ("orderedPropertyIndicator".equals(a.attLname())) {
                var ceqn = a.componentEQN();
                var eeqn = a.elementEQN();
                assertEquals(Pair.with("http://release.niem.gov/niem/niem-core/5.0/", "PersonNameType"), ceqn);
                assertEquals(Pair.with("http://example.com/redefined/nc/", "PersonGivenName"), eeqn);
            }
        }
        String targetNS = sd.targetNamespace();
        for (var nsd : sd.namespaceDecls()) {
            assertEquals(targetNS, nsd.targetNS());
        }
        
        assertEquals("http://release.niem.gov/niem/niem-core/5.0/", targetNS);
        assertEquals("http://reference.niem.gov/niem/specification/naming-and-design-rules/5.0/#ReferenceSchemaDocument", sd.conformanceTargets());
        assertEquals("5", sd.niemVersion());
        assertEquals("99", sd.schemaVersion());
        assertEquals(2, sd.schemaKind());
        assertEmptyLogs();
    }
    
    @Test
    public void testIsCore () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd5/niem/niem-core.xsd", "");
        assertEquals(NSK_CORE, sd.schemaKind());
        assertEmptyLogs();
    }
    
    @Test
    public void testIsDomain () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd5/xmlschemadoc/hs.xsd", "");
        assertEquals(NSK_DOMAIN, sd.schemaKind());
        assertEmptyLogs();
    }
    
    @Test
    public void testIsExtension () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd5/xmlschemadoc/CrashDriver.xsd", "");
        assertEquals(NSK_EXTENSION, sd.schemaKind());
        assertEmptyLogs();
    }    
    
    @Test
    public void testIsStructures () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd5/niem/utility/structures.xsd", "");
        assertEquals(NSK_UTILITY, sd.schemaKind());
        assertEmptyLogs();
    } 
    
    @Test
    public void testIsUtility () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd5/niem/utility/appinfo.xsd", "");
        assertEquals(NSK_UTILITY, sd.schemaKind());
        assertEmptyLogs();
    }    
    
    @Test
    public void testIsOtherNIEM () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd5/externals-niem/adapters/geospatial.xsd", "");
        assertEquals(NSK_OTHERNIEM, sd.schemaKind());
        assertEmptyLogs();
    }
    
    @Test
    public void testIsXML () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd5/niem/external/xml.xsd", "");
        assertEquals(NSK_XML, sd.schemaKind());
        assertEmptyLogs();
    }
    
    @Test
    public void testLocalTerm () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd5/localTerm.xsd", "");        
        List<LocalTerm> lsl = sd.localTerms();
        assertEquals(3, lsl.size());
        assertThat(lsl).extracting(LocalTerm::getTerm)
                .containsOnly("2D", "3D", "Test");
        assertNotNull(lsl.get(0).getLiteral());
        assertNull(lsl.get(0).getDefinition());
        assertNull(lsl.get(0).getSourceURIs());
        assertEquals(0, lsl.get(0).citationList().size());
        assertNull(lsl.get(1).getLiteral());
        assertNotNull(lsl.get(1).getDefinition());
        assertNull(lsl.get(1).getSourceURIs());
        assertEquals(0, lsl.get(1).citationList().size());
        assertNull(lsl.get(2).getLiteral());
        assertNotNull(lsl.get(2).getDefinition());
        assertNotNull(lsl.get(2).getSourceURIs());
        assertEquals(2, lsl.get(2).citationList().size());
    }

    @Test
    public void testNoPrefix () throws Exception {
        XMLSchemaDocument sd = new XMLSchemaDocument("src/test/resources/xsd5/noprefix.xsd", "");
        assertEquals("http://example.com/nopr#e-fix/5.0/", sd.targetNamespace());
        assertEmptyLogs();
    }
}
