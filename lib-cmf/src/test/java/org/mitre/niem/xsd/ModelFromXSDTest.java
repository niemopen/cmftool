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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import nl.altindag.log.LogCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.mitre.niem.cmf.Model;
import static org.mitre.niem.cmf.ModelAssertions.*;
import org.mitre.niem.cmf.ModelXMLWriter;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelFromXSDTest {
    private final static String resDN = "src/test/resources/";
    
    public ModelFromXSDTest() {
    }
    
    @Test
    public void testAny () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/any.xsd");
        var model = mb.createModel(sch);
        checkAny(model);
        assertEmptyLogs();
    }

    @Test
    public void testArchVersions () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/archVersions.xsd");
        var model = mb.createModel(sch);
        checkArchVersions(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testAttAugment () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/attAugment.xsd");
        var model = mb.createModel(sch);
        checkAttAugment(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testAugment () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/augment.xsd");
        var model = mb.createModel(sch);
        checkAugment(model);
        assertEmptyLogs();
    }
  
    @Test
    public void testClass () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/class.xsd");
        var model = mb.createModel(sch);  
        checkClass(model);
        assertEmptyLogs();
    }

    @Test
    public void testCodeListBindings () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/codeListBinding.xsd");
        var model = mb.createModel(sch);
        checkCodeListBinding(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testComponent () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/component.xsd");
        var model = mb.createModel(sch);
        checkComponent(model);
        assertThat(logs)
            .anyMatch(lc -> lc.getWarnLogs().contains("ignored unknown appinfo:bogus attribute"));
    }
    
    @Test
    public void testDataProperty () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/dataProperty.xsd");
        var model = mb.createModel(sch);
        checkDataProperty(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testDatatypes () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/datatypes.xsd");
        var model = mb.createModel(sch);
        checkDatatypes(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testExternals () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/externals.xsd");
        var model = mb.createModel(sch);
        checkExternals(model);
        assertEmptyLogs();
    }

    @Test
    public void testGaLitAtt () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/gaLitAtt.xsd");
        var model = mb.createModel(sch);
        checkGaLitAtt(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testGaObjAtt () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/gaObjAtt.xsd");
        var model = mb.createModel(sch);
        checkGaObjAtt(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testGaObjObj () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/gaObjObj.xsd");
        var model = mb.createModel(sch);
        checkGaObjObj(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testImports () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/imports.xsd");
        var model = mb.createModel(sch);
        checkImports(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testList () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/list.xsd");
        var model = mb.createModel(sch);
        checkList(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testLiteralClass () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/literalClass.xsd");
        var model = mb.createModel(sch);
        checkLiteralClass(model);
        assertEmptyLogs();
    }
        
    @Test
    public void testLiteralProps () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/literalProps.xsd");
        var model = mb.createModel(sch); 
        checkLiteralProps(model);
        assertEmptyLogs();
    }
        
    @Test
    public void testLocalTerm () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/localTerm.xsd");
        var model = mb.createModel(sch); 
        checkLocalTerm(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testNamespace () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/namespace.xsd");
        var model = mb.createModel(sch);
        checkNamespace(model);        
    }
    
    @Test
    public void testObjectProperty () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/objectProperty.xsd");
        var model = mb.createModel(sch);     
        checkObjectProperty(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testPropAssoc () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/propAssoc.xsd");
        var model = mb.createModel(sch);     
        checkPropAssoc(model);
        assertEmptyLogs();
    }
        
    @Test
    public void testRefCode () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/refCode.xsd");
        var model = mb.createModel(sch);     
        checkRefCode(model);
        assertEmptyLogs();
    }

    @Test
    public void testSimpleTypes () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/simpleTypes.xsd");
        var model = mb.createModel(sch);
        checkSimpleTypes(model);
        assertEmptyLogs();
    }
    
    @Test
    public void testUnion () throws Exception {
        var mb  = new ModelFromXSD();
        var sch = new NIEMSchema(resDN + "xsd6/union.xsd");
        var model = mb.createModel(sch);
        checkUnion(model);
        assertEmptyLogs();        
    }
    
    private void debugModelWriter (Model m) throws Exception {
        var outF = new File(resDN + "out.cmf");
        var outS = new FileOutputStream(outF);
        var outW = new OutputStreamWriter(outS, "UTF-8");
        var mw   = new ModelXMLWriter();
        mw.writeXML(m, outW);
        outS.close();      
    }
    
    public static List<LogCaptor> logs;      
    @BeforeAll
    public static void setupLogCaptor () {
        logs = new ArrayList<>();
        logs.add(LogCaptor.forClass(ModelFromXSD.class));
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
    }}
