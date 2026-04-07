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
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.json.Context;
import org.mitre.niem.utility.JCUsageFormatter;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_ALL;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Parameters(commandDescription = "create a JSON-LD context from a model file and mapping")

public class CmdCMFtoContext implements JCCommand {
    
    @Parameter(order = 1, names = {"-m", "--map"}, description = "mapping file for property keys")
    File mapF = null;
    
    @Parameter(order = 1, names = {"-s","--single"}, description = "map to single namespace with no prefixes")
    private boolean noPrefix = false;
         
    @Parameter(order = 1, names = "-o", description = "name of output mapping file")
    private String mapFN = null;

    @Parameter(order = 2, names = {"-h","--help"}, description = "display this usage message", help = true)
    private boolean help = false;
        
    @Parameter(description = "model.cmf [map.sssom]")
    private List<String> mainArgs;        
    
    CmdCMFtoContext () {
    }
  
    CmdCMFtoContext (JCommander jc) {
    }

    public static void main (String[] args) {       
        var obj = new CmdCMFtoMapping();
        obj.runMain(args);
    }
    
    @Override
    public void runMain (String[] args) {
        var jc = new JCommander(this);
        var uf = new JCUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.setProgramName("compile");
        jc.parse(args);
        run(jc);
    }
    
    @Override
    public void runCommand (JCommander cob) {
        cob.setProgramName("cmftool m2context");
        run(cob);
    }     
    
    private void run (JCommander cob) {
        if (help) {
            cob.usage();
            System.exit(0);
        }
        if (mainArgs == null || mainArgs.isEmpty() || mainArgs.size() > 1) {
            cob.usage();
            System.exit(1);
        }
        // Argument of "-" signals end of arguments, allows "-foo" filenames
        var na = mainArgs.get(0);
        if (na.startsWith("-")) {
            if (na.length() == 1) {
                mainArgs.remove(0);
            } else {
                System.err.println("Unknown option: " + na);
                cob.usage();
                System.exit(1);
            }
        }     
        // Make sure output mapping file is writable      
        var ow = new OutputStreamWriter(System.out);
        if (null != mapFN) try {
            var os = new FileOutputStream(mapFN);
            ow = new OutputStreamWriter(os, "UTF-8");
        } catch (IOException ex) {
            System.err.println(String.format("Can't write to output file %s: %s", mapFN, ex.getMessage()));
            System.exit(1);            
        }      
        // Make sure the Xerces parsers can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Internal parser error: " + ex.getMessage());
            System.exit(1);
        }
        // Read the model object from the model file
        var mr    = new ModelXMLReader();  
        var model = mr.readFiles(new File(mainArgs.get(0)));
        if (null == model) {
            System.err.println("Could not read model from CMF file " + mainArgs.get(0));
            System.exit(1);
        }
        // Read the mapping file if one was provided
        Mapping map = null;
        if (null != mapF) {
            try {
                map = Mapping.readFile(mapF);
            } catch (IOException | CMFException ex) {
                System.err.println(String.format("Can't read mapping file %s: %s", mapF.toString(), ex.getMessage()));
                System.exit(1);
            }
        }
        // Create context and write to output stream
        try {
            if (null == map) Context.createTo(ow, model);
            else {
                map.setNoPrefix(noPrefix);
                Context.createTo(ow, model, map);
            }
            ow.write("\n");
            ow.close();
        } catch (RuntimeException | IOException ex) {
            System.err.println("Can't write context: " + ex.getMessage());
            System.exit(1);
        } catch (CMFException ex) {
            System.err.println("Can't create context: " + ex.getMessage());
            System.exit(1);
        }
        System.exit(0);            
    }    
}
