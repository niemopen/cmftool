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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import nl.altindag.log.LogCaptor;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.mitre.niem.cmf.Model;

/**
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
    
    public void assertEmptyLogs () {
        for (var log : logs) {
            var errors = log.getErrorLogs();
            var warns  = log.getWarnLogs();
            assertThat(errors.isEmpty());
            assertThat(warns.isEmpty());
        }
    }      
    
    public void createCMF (String[] schemaArgs, File modelFP) throws Exception {    
        PrintWriter modelPW = new PrintWriter(modelFP, "UTF-8");
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = mfact.createModel(schemaArgs);     
        ModelXMLWriter mw = new ModelXMLWriter();
        mw.writeXML(m, modelPW); 
        modelPW.close();   
    }
    
    public void createXSD (File modelFP, File outDir) throws Exception {
        FileInputStream mis = new FileInputStream(modelFP);
        ModelXMLReader mr = new ModelXMLReader();
        Model m = mr.readXML(mis);
        ModelToXSD mw = new ModelToN5XSD(m);
        mw.writeXSD(outDir);
    }    
    
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
