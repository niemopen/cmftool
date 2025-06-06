/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2025 The MITRE Corporation.
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

package org.mitre.niem.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A class to obtain a File, URI, or InputStream object for a project resource.  
 * Does the right thing when running from the IDE and when running from the JAR.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ResourceManager {
    private static final Logger LOG = LogManager.getLogger(ResourceManager.class);
    private final String jarPath;
    private final String resPath;
    private final String resPath2;
    
    public ResourceManager () {
        jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        //resPath = FilenameUtils.concat(jarPath, "../../../../src/main/resources/");
        resPath = FilenameUtils.concat(jarPath, ""); //DEBUG  
        resPath2 = FilenameUtils.concat(jarPath, "../../../resources/main"); //TESTING
    }
    
    public ResourceManager (Class c) {
        jarPath = c.getProtectionDomain().getCodeSource().getLocation().getPath();
        //resPath = FilenameUtils.concat(jarPath, "../../../../src/main/resources/");
        resPath = FilenameUtils.concat(jarPath, ""); //DEBUG
        resPath2 = FilenameUtils.concat(jarPath, "../../../resources/main"); //TESTING
    }
    
    public InputStream getResourceStream (String name) throws IOException {
        InputStream res = null;
        
        // Running from IDE?  Open stream on resource file in project directory
        if (!jarPath.endsWith(".jar")) {
            //var rf = getResourceFile(name);
            var rf = new File(resPath, name);
            if (null == rf) return null;
            try {
                res = new FileInputStream(rf);
            } catch (FileNotFoundException ex) {
                try {
                    rf = new File(resPath2, name);
                    if (null == rf) return null;
                    res = new FileInputStream(rf);
                } catch (FileNotFoundException ex2) {
                // LOG.error("Can't find resource {}", name); //IGNORE
                }
            }
            return res;
        }
        // Running from JAR? Get resource stream
        res = ResourceManager.class.getResourceAsStream(name);
        return res;
    }
    
    
    public File getResourceFile (String name) throws IOException {     
        var rF = File.createTempFile("cmfTool", "resource");
        copyResourceToFile(name, rF);
        return rF;
    }
    
    
    public void copyResourceToFile (String name, File outF) throws IOException {
        var istr = getResourceStream(name);
        var outW = new FileWriter(outF);
        if (null == istr) {
            LOG.error("Can't find resource {}", name);
            return;
        }
        IOUtils.copy(istr, outW, "UTF-8");
        istr.close();
        outW.close();    
    }    
    
    public URI getResourceURI (String name) {
        return uri(name);
    }
    
    private URI uri (String name) {
        if (jarPath.endsWith(".jar")) {
            try {
                var cls    = getClass();
                var cldr   = cls.getClassLoader();
                var res    = cldr.getResource(name);
//                System.err.println("class="+cls);
//                System.err.println("cldr="+cldr);
//                System.err.println("res="+res);
                var result = res.toURI();
                return result;
            } catch (URISyntaxException ex) {
                LOG.error(ex.getMessage());
            }
            return null;
        }
        var resF = new File(resPath, name);
        return resF.toURI();
    }
}
