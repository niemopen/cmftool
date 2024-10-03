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
import java.io.FileOutputStream;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.mitre.niem.xsd.FileCompare.compareIgnoringTrailingWhitespace;

/**
 * Read a model from CMF, write to CMF, see if it's valid and the same CMF.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelXMLWriterTest {
    
    public ModelXMLWriterTest() {
    }

    private static final String cmfDirPath = "src/test/resources/cmf";   
    
    @Test
    public void testWriteCMF () throws Exception {
        var cmfXSD = new String[]{"src/main/CMF/model.xsd/cmf.xsd"};
        var cmfxs  = new XMLSchema(cmfXSD);
        var cmfjx  = cmfxs.javaxSchema();
        var val    = cmfjx.newValidator();        
        var of = File.createTempFile("testWriteXML", ".cmf");
            
        var cmfDir = new File(cmfDirPath);
        var files  = FileUtils.listFiles(cmfDir, new String[]{"cmf"}, false);
        for (var f : files) {
            var fn = f.getName();
            if ("mismatchIDRef.cmf".equals(fn)) continue;
            if ("missingIDRef.cmf".equals(fn)) continue;
            var is = new FileInputStream(f);
            var mr = new ModelXMLReader();
            var m  = mr.readXML(is);
            System.out.println(fn);
            var os = new FileOutputStream(of);
            var mw = new ModelXMLWriter();
            mw.writeXML(m, os);
            os.close();
            var comp = compareIgnoringTrailingWhitespace(f, of);
            assertNull(comp);
            var vs = new StreamSource(f);
            val.reset();
            val.validate(vs);
        }
        of.delete();       
    }

}
