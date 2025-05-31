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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import nl.altindag.log.LogCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLReaderTest extends ModelAssertions {
    private final static String resDN = "src/test/resources/cmf/";    
    
    public ModelXMLReaderTest() {
    }
    
    @Test
    public void testAny () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "any.cmf"));
        checkAny(model);
        assertEmptyLogs();        
    }
    
    @Test
    public void testAttAugment () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "attAugment.cmf"));
        checkAttAugment(model);
        assertEmptyLogs();        
    }
    
    // AugmentRecord test
    @Test
    public void testAugment () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "augment.cmf"));
        checkAugment(model);
        assertEmptyLogs();
    }
    
    // ClassType object test
    // Test abstract, augmentable, reference code.
    @Test
    public void testClass () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "class.cmf"));
        checkClass(model);
        assertEmptyLogs();
    }
           
    // CodeListBinding object test
    // Test codeListURI, column name, isConstraining
    @Test
    public void testCodeListBinding () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "codeListBinding.cmf"));
        checkCodeListBinding(model);
        assertEmptyLogs();
    }
            
    // Component object test. 
    // Test name, namespace, documentation, deprecated.
    // Test bogus appinfo.
    @Test
    public void testComponent () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "component.cmf"));
        checkComponent(model);
        assertEmptyLogs();
    }
    
    // DataProperty object test. 
    // Test abstract, relationship, reference code, attribute, refAttribute.
    @Test
    public void testDataProperty () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "dataProperty.cmf"));
        checkDataProperty(model);
        assertEmptyLogs();
    }
    
    // DataProperty object test. 
    // Test abstract, relationship, reference code, attribute, refAttribute.
    @Test
    public void testDatatypes () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "datatypes.cmf"));
        checkDatatypes(model);
        assertEmptyLogs();
    }

    @Test
    public void testExternals () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "externals.cmf"));
        checkExternals(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testGlobalAttAugment () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "globalAttAugment.cmf"));
        checkGlobalAttAugment(model);
        assertEmptyLogs();        
    }
    
    @Test
    public void testImports () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "imports.cmf"));
        checkImports(model);
        assertEmptyLogs();        
    }
    
    @Test
    public void testList () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "list.cmf"));
        checkList(model);
        assertEmptyLogs();        
    }
    
    @Test
    public void testLiteralClass () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "literalClass.cmf"));
        checkLiteralClass(model);
        assertEmptyLogs();        
    }  
    
    @Test
    public void testLiteralProps () throws Exception {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "literalProps.cmf"));
        checkLiteralProps(model);
        assertEmptyLogs();        
    }

    // LocalTerm object test
    // Test term, literal, sourceURI, citation, documentation
    @Test
    public void testLocalTerm () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "localTerm.cmf"));
        checkLocalTerm(model);
        assertEmptyLogs();        
    }
                
    // Namespace object test
    // Test CTAs, filepath, version, language
    @Test
    public void testNamespace () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "namespace.cmf"));
        checkNamespace(model);
        assertEmptyLogs();        
    }
                                     
    @Test
    public void testNiemVersions () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "niemVersions.cmf"));
        checkNiemVersions(model);
        assertEmptyLogs();        
    }

    // ObjectProperty object test
    // Test abstract, relationship, reference code
    @Test
    public void testObjectProperty () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "objectProperty.cmf"));
        checkObjectProperty(model);
        assertEmptyLogs();        
    }
        
    // Partial CMF file test.
    // Test outside component.
    // Test complete model formed from two CMF files.
    @Test
    public void testPartialCMF () {
        var cf1 = new File(resDN, "partial-1.cmf");
        var cf2 = new File(resDN, "partial-2.cmf");
        var rdr = new ModelXMLReader();

        var mod1 = rdr.readFiles(cf1);
        var dp = mod1.uriToProperty("http://example.com/part2/AnElement");
        assertEquals("", dp.name());
        assertNull(dp.namespace());
        assertEquals("http://example.com/part2/AnElement", dp.uri());
        assertEmptyLogs();
        
        var mod2 = rdr.readFiles(cf2);
        dp = mod2.uriToProperty("http://example.com/part2/AnElement");
        assertEquals("AnElement", dp.name());
        assertNotNull(dp.namespace());
        assertEquals("http://example.com/part2/AnElement", dp.uri());
        assertEmptyLogs();
        
        var mod3 = rdr.addFile(mod1, cf2);
        dp = mod2.uriToProperty("http://example.com/part2/AnElement");
        assertTrue(dp.outsideURI().isEmpty());
        
        var mod4 = rdr.readFiles(List.of(cf1, cf2));
        dp = mod2.uriToProperty("http://example.com/part2/AnElement");
        assertTrue(dp.outsideURI().isEmpty());        
    }
                   
    // ClassType object test
    // Test minoccurs, maxoccurs, documentation, orderedProp.
    @Test
    public void testPropAssoc () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "propAssoc.cmf"));
        checkPropAssoc (model);
        assertEmptyLogs();
    }
    
    @Test
    public void testSimpleTypes () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "simpleTypes.cmf"));
        checkSimpleTypes (model);
        assertEmptyLogs();
    }
    
    @Test
    public void testRefCode () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "refCode.cmf"));
        checkRefCode (model);
        assertEmptyLogs();
    }
    
    @Test
    public void testUnion () {
        var rdr   = new ModelXMLReader();
        var model = rdr.readFiles(new File(resDN, "union.cmf"));
        checkUnion (model);
        assertEmptyLogs();
    }
    
    
    public static List<LogCaptor> logs;      
    @BeforeAll
    public static void setupLogCaptor () {
        logs = new ArrayList<>();
        logs.add(LogCaptor.forClass(ModelXMLReader.class));
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
