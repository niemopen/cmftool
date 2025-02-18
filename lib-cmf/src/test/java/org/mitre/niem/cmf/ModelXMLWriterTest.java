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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import nl.altindag.log.LogCaptor;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.mitre.niem.xml.XMLSchema;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLWriterTest {
    private final static String resDN = "src/test/resources/cmf/"; 
    
    private static XMLSchema cmfSch =  null;
    
    public ModelXMLWriterTest() {  }
    
    @BeforeAll
    public static void getCMFSchema () throws XMLSchema.XMLSchemaException {
        var cmfXSD = new String[]{"src/main/CMF/model.xsd/cmf.xsd"};
        cmfSch = new XMLSchema(cmfXSD);        
    }
    
    @Test
    public void testRoundTrip () throws Exception {
        var outF  = File.createTempFile("ModelXMLWriterTest", ".cmf");
        var rdr   = new ModelXMLReader();
        var wr    = new ModelXMLWriter();
        var resDF = new File(resDN);
        var cmfFL = FileUtils.listFiles(resDF, new String[]{"cmf"}, false);
        for (var cmfF : cmfFL) {
            var model = rdr.readFiles(cmfF);
            var os    = new FileOutputStream(outF);
            wr.writeXML(model, os);
            os.close();
            var same = FileUtils.contentEqualsIgnoreEOL(cmfF, outF, "UTF-8");
            outF.delete();
            assertTrue(same);
        }

    }

    @Test
    public void testAugmentRecord () throws FileNotFoundException, IOException {
        var rdr   = new ModelXMLReader();
        var wr    = new ModelXMLWriter();
        var model = rdr.readFiles(new File(resDN, "objectProperty.cmf"));
        var os    = new FileOutputStream(new File(resDN, "out.cmf"));
        wr.writeXML(model, os);
        os.close();;
        assertEmptyLogs();
    }


    
    public static List<LogCaptor> logs;      
    @BeforeAll
    public static void setupLogCaptor () {
        logs = new ArrayList<>();
        logs.add(LogCaptor.forClass(ModelXMLReader.class));
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
}
