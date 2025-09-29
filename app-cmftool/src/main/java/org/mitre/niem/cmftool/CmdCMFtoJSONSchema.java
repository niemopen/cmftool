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
package org.mitre.niem.cmftool;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.json.ModelToJSON;
import org.mitre.niem.utility.JCUsageFormatter;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_ALL;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Parameters(commandDescription = "generate a JSON message schema from CMF")
    
public class CmdCMFtoJSONSchema implements JCCommand {

    @Parameter(order = 1, names = "-o", description = "name of output file")
    private String modelFN = null;
     
    @Parameter(order = 2, names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
        
    @Parameter(description = "modelFile.cmf...")
    private List<String> mainArgs;
    
    CmdCMFtoJSONSchema () {
    }
  
    CmdCMFtoJSONSchema (JCommander jc) {
    }

    public static void main (String[] args) {       
        CmdCMFtoCMF obj = new CmdCMFtoCMF();
        obj.runMain(args);
    }
    
    @Override
    public void runMain (String[] args) {
        var jc = new JCommander(this);
        var uf = new JCUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.setProgramName("m2m");
        jc.parse(args);
        run(jc);
    }
    
    @Override
    public void runCommand (JCommander cob) {
        cob.setProgramName("cmftool m2m");
        run(cob);
    }    
    
    private void run (JCommander cob)  {

        if (help) {
            cob.usage();
            System.exit(0);
        }
        if (mainArgs == null || mainArgs.isEmpty()) {
            cob.usage();
            System.exit(1);
        }
        // Argument of "-" signals end of arguments, allows "-foo" filenames
        String na = mainArgs.get(0);
        if (na.startsWith("-")) {
            if (na.length() == 1) {
                mainArgs.remove(0);
            } else {
                System.err.println("Unknown option: " + na);
                cob.usage();
                System.exit(1);
            }
        }       
        // Make sure the Xerces parsers can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Internal parser error: " + ex.getMessage());
            System.exit(1);
        }
        // Make sure output model file is writable      
        var ow = new OutputStreamWriter(System.out);
        if (null != modelFN) try {
            var os = new FileOutputStream(modelFN);
            ow = new OutputStreamWriter(os, "UTF-8");
        } catch (IOException ex) {
            System.err.println(String.format("Can't write to output file %s: %s", modelFN, ex.getMessage()));
            System.exit(1);            
        }       
        // Read the model object from the model instance file
        // Read the model object from the model file(s)
        var mr = new ModelXMLReader();  
        var fileL = new ArrayList<File>();
        for (var str : mainArgs) fileL.add(new File(str));
        var model = mr.readFiles(fileL);
        
        // Generate JSON Schema
        try {
            var js = new ModelToJSON(model);
            js.writeJSON(ow);
            ow.close();
        }
        catch (IOException ex) {}

        System.exit(0);
    }    
}
