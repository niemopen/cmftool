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
package org.mitre.niem.xml;

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.stream.StreamSource;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.mitre.niem.utility.URIfuncs.FileToCanonicalURI;
import org.xml.sax.InputSource;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class SchematronTest {
    
    public static List<LogCaptor> logs;    
    private static final String resDN  = "src/test/resources/sch/";
    private static final File resDF    = new File(resDN);
    private static final String resDUs = FileToCanonicalURI(resDF).toString();    
    
    @BeforeAll
    public static void setupLogCaptor () {
        logs = new ArrayList<>();
        logs.add(LogCaptor.forClass(XMLSchema.class));
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
    
    
    public SchematronTest() {
    }

    @Test
    public void testCompileSchematron () throws Exception {
        var schF = new File(resDF, "refTarget.sch");
        var schS = new StreamSource(schF);
        var strW = new StringWriter();
        var s    = new Schematron();
        schS.setSystemId(schF);
        s.compileSchematron(schS, strW);
        var res = strW.toString();
        assertTrue(res.contains("xsl:stylesheet"));
    }
    
    @Test
    public void testApplySchematron () throws Exception {
        var schF = new File(resDF, "refTarget.sch");
        var schS = new StreamSource(schF);
        var strW = new StringWriter();
        var s    = new Schematron();
        schS.setSystemId(schF);
        var xslt = s.compileSchematron(schS);      
        
        var xmlF = new File(resDF, "7-10.xsd");
        var xmlR = new FileReader(xmlF);
        var xmlS = new StreamSource(xmlR);
        var ow   = new StringWriter();
        xmlS.setSystemId(schF);
        s.applyXslt(xmlS, xslt, ow);
        
        var svrlR = new StringReader(ow.toString());
        var svrlS = new InputSource(svrlR);
        var msgW  = new StringWriter();
        xmlR      = new FileReader(xmlF);
        var xS    = new InputSource(xmlR);
        xS.setSystemId(xmlF.toURI().toString());
        s.SVRLtoMessages(svrlS, xS, msgW);
        var res = msgW.toString();
        assertTrue(res.contains("WARN  7-10.xsd:19:54"));
    }
    
}
