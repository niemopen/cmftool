/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2023 The MITRE Corporation.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import nl.altindag.log.LogCaptor;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.mitre.niem.cmf.Model;

/**
 * Common routines for model to schema pile tests.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToXSDTest {
    
   static List<LogCaptor> logs;
      
    @TempDir
    File tempD1;
    @TempDir
    File tempD2;
    @TempDir
    File tempD3;
    
    public ModelToXSDTest() {
    }
    
    
    @BeforeAll
    public static void setupLogCaptor () {
        logs = new ArrayList<>();
        logs.add(LogCaptor.forClass(ModelToXSD.class));
        logs.add(LogCaptor.forClass(XMLSchema.class));
        logs.add(LogCaptor.forClass(XMLSchemaDocument.class));
        logs.add(LogCaptor.forClass(ModelXMLWriter.class));
    }
    
    @AfterEach
    public void clearLogs () {
        for (var log : logs) log.clearLogs();;
    }
    
    @AfterAll
    public static void tearDown () {
        for (var log : logs) log.close();
    }
    
    public void testRT(String testDir, String sourceXSD) throws Exception {
    
        // Create CMF from input schema, write to temp directory #1
        String[] schemaArgs = { testDir + sourceXSD };
        File modelFP = new File(tempD1, "model.cmf");
        createCMF(schemaArgs, modelFP);
        
        // Create schema from that CMF, write to temp directory #2
        createXSD(modelFP, tempD2);

        // Valid XSD?
        File newSchema = new File(tempD2, sourceXSD);
        assertTrue(isValidXSD(newSchema));
        
        // Now create CMF from the schema in temp directory #2        
        File newModelFP = new File(tempD1, "newModel.cmf");
        createCMF(schemaArgs, newModelFP);
        
        // First and second model files should be the same
        assertTrue(FileUtils.contentEquals(modelFP, newModelFP));

        // Now create second schema from new CMF & CMF in temp directory #3
        createXSD(newModelFP, tempD3);
        
        // Second and third temp directory should have same content
        List<File> sfl2 = FileUtils.listFiles(tempD2, null, true).stream().sorted().collect(Collectors.toList());
        List<File> sfl3 = FileUtils.listFiles(tempD3, null, true).stream().sorted().collect(Collectors.toList());
        int i2 = 0;
        int i3 = 0;
        assertEquals(sfl2.size(), sfl3.size());
        while (i2 < sfl2.size() && i3 < sfl3.size()) {
            File f2 = sfl2.get(i2++);
            File f3 = sfl3.get(i3++);
            String n2 = f2.getName();
            String n3 = f3.getName();
            assertEquals(n2,n3);
            assertTrue(FileUtils.contentEquals(f2, f3));
            if (!FileUtils.contentEquals(f2, f3)) {
                int i = 0;
            }
        }
        assertEmptyLogs();
    }    
    
    public void assertEmptyLogs () {
        for (var log : logs) {
            var errors = log.getErrorLogs();
            var warns  = log.getWarnLogs();
            assertThat(errors.isEmpty());
            assertThat(warns.isEmpty());
        }
    }      
    
    public void createCMF (String[] schemaArgs, File modelFP) throws Exception {    
        FileOutputStream os = new FileOutputStream(modelFP);
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(schemaArgs);     
        ModelXMLWriter mw = new ModelXMLWriter();
        mw.writeXML(m, os); 
        os.close();   
    }
    
    public void createXSD (File modelFP, File outDir) throws Exception { }    
    
    public boolean isValidXSD (File sdoc) throws Exception {
        String[] args = { sdoc.toString() };
        XMLSchema s = new XMLSchema(args);
        var xsdMsgs = s.javaXMsgs();
        return (xsdMsgs.isEmpty());        
    }
    
    public List<String> compareDirectories (File one, File two) throws Exception {
        var result = new ArrayList<String>();
        var sfone = FileUtils.listFiles(one, null, true).stream().sorted().collect(Collectors.toList());
        var sftwo = FileUtils.listFiles(two, null, true).stream().sorted().collect(Collectors.toList());
        int i2 = 0;
        int i3 = 0;
        while (i2 < sfone.size() && i3 < sftwo.size()) {
            var f2 = sfone.get(i2);
            var f3 = sftwo.get(i3);
            var n2 = f2.getName();
            var n3 = f3.getName();
            int i = n2.compareTo(n3);
            if (i < 0) {
                result.add(String.format("Only in DIR1: %s", n2));
                i2++;
            }
            else if (i > 0) {
                result.add(String.format("Only in DIR2: %s", n3));
                i3++;
            }
            else {
                if (!FileUtils.contentEquals(f2, f3)) result.add(String.format("Different: %s", n2));
                i2++;
                i3++;
            }
        }
        while (i2 < sfone.size()) result.add(String.format("Only in DIR1: %s", sfone.get(i2++).toString()));
        while (i3 < sftwo.size()) result.add(String.format("Only in DIR2: %s", sftwo.get(i3++).toString()));
        return result;
    }
    
    public boolean fileContains (File f, String regex) throws Exception {
        var pat  = Pattern.compile(regex);
        var fr   = new FileReader(f);
        var br   = new BufferedReader(fr);
        var line = br.readLine();      
        while (null != line) {
            var mat = pat.matcher(line);
            if (mat.find()) {
                br.close();
                return true;
            }
            line = br.readLine();
        }
        br.close();
        return false;
    }

}
