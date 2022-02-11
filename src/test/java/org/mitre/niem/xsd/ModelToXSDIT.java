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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mitre.niem.cmf.Model;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXSDIT {
    
    static String testDir = "src/test/resources/xsd/";
      
    @TempDir
    File tempD1;
    @TempDir
    File tempD2;
    @TempDir
    File tempD3;
            
    public ModelToXSDIT() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    @ParameterizedTest
    @ValueSource(strings = { 
            "augment-0.xsd",
            "codelist.xsd",
            "codelistClassType.xsd",
            "codelistUnion.xsd",
            "complexContent.xsd",
            "deprecated.xsd",
//            "externals.xsd",          // can't automate this one; m2x doesn't generate external documents
            "hasValue.xsd",
            "list.xsd",
            "nameinfo.xsd",
            "namespace-1.xsd",
            "proxy.xsd",
            "restriction.xsd",
//            "twoversions-0.xsd",      // can't automate this one; m2x doesn't create unneeded imports
            "union.xsd",
            "xml-lang.xsd"
        })
    public void testRoundTrip(String sourceXSD) throws Exception {
    
        // Create CMF and CMX from input schema, weite to temp directory #1
        String[] schemaArgs = { testDir + sourceXSD };
        File modelFP = new File(tempD1, "model.cmf");
        File mextFP  = new File(tempD1, "model.cmx");
        createModel(schemaArgs, modelFP, mextFP);
        
        // Create schema from that CMF & CMX, write to temp directory #2
        createXSD(modelFP, mextFP, tempD2);

        // Now create CMF and CMX from the schema in temp directory #2
        File newSchema = new File(tempD2, sourceXSD);
        schemaArgs[0] = newSchema.toString();
        File newModelFP = new File(tempD1, "newModel.cmf");
        File newMextFP  = new File(tempD1, "newModel.cmx");
        createModel(schemaArgs, newModelFP, newMextFP);
        
        // First and second model files should be the same
        assertTrue(FileUtils.contentEquals(modelFP, newModelFP));

//        // Now create second schema from new CMF & CMF in temp directory #3
//        createXSD(newModelFP, newMextFP, tempD3);
//        
//        // Second and third temp directory should have same content
//        List<File> sfl2 = FileUtils.listFiles(tempD2, null, true).stream().sorted().collect(Collectors.toList());
//        List<File> sfl3 = FileUtils.listFiles(tempD3, null, true).stream().sorted().collect(Collectors.toList());
//        int i2 = 0;
//        int i3 = 0;
//        assertEquals(sfl2.size(), sfl3.size());
//        while (i2 < sfl2.size() && i3 < sfl3.size()) {
//            File f2 = sfl2.get(i2++);
//            File f3 = sfl3.get(i3++);
//            String n2 = f2.getName();
//            String n3 = f3.getName();
//            assertEquals(n2,n3);
//            assertTrue(FileUtils.contentEquals(f2, f3));
//        }
    }
    
    private void createModel (String[] schemaArgs, File modelFP, File mextFP) throws Exception {      
        PrintWriter modelPW = new PrintWriter(modelFP);
        PrintWriter mextPW  = new PrintWriter(mextFP);
        Schema s = Schema.genSchema(schemaArgs);
        ModelFromXSD mfact = new ModelFromXSD(s);
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        NamespaceInfo nsi = new NamespaceInfo();
        mfact.createModel(m, me, nsi);       
        ModelXMLWriter mw = new ModelXMLWriter();
        mw.writeXML(m, modelPW); 
        modelPW.close();
        me.writeXML(mextPW);   
        mextPW.close();    
    }
    
    private void createXSD (File modelFP, File mextFP, File outDir) throws Exception {
        FileInputStream mis = new FileInputStream(modelFP);
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(mis);
        FileInputStream xis = new FileInputStream(mextFP);
        ModelExtension me = new ModelExtension(m);
        me.readXML(xis);
        ModelToXSD mw = new ModelToXSD(m, me);
        mw.writeXSD(outDir);
    }
    
}
