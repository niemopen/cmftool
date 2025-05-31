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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.utility.JCUsageFormatter;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_ALL;
import org.mitre.niem.xsd.ModelToXMLSchema;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Parameters(commandDescription = "create an XSD message schema from a model")

public class CmdCMFtoXMLSchema implements JCCommand {
    
    @Parameter(order = 0, names = "-o", description = "write schema pile into this directory")
    private String outputDir = "";

    @Parameter(order = 1, names = "-c", description = "generate xml-catalog.xml file")
    private boolean catFlag = false;
    
    @Parameter(order = 2, names = "--catalog", description = "write XML catalog into this file")
    private String catPath = null;
    
    @Parameter(order = 3, names = {"-r", "-root"}, description = "make this schema document have all necessary imports")
    private String rootNSarg = null;
    
    @Parameter(names = {"-d","--debug"}, description = "turn on debug logging")
    private boolean debugFlag = false;
    
    @Parameter(names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
        
    @Parameter(description = "modelFile.cmf...")
    private List<String> mainArgs;    
    
    CmdCMFtoXMLSchema () { }
    CmdCMFtoXMLSchema (JCommander jc) { }
    
    public static void main (String[] args) {       
        var obj = new CmdCMFtoXSDModel();
        obj.runMain(args);
    }
    
    @Override
    public void runMain (String[] args) {
        var jc = new JCommander(this);
        var uf = new JCUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.parse(args);
        run(jc);
    }
    
    @Override
    public void runCommand (JCommander cob) {
        run(cob);
    }      
    
    protected void run (JCommander cob) {
        var cmdName = cob.getProgramName();        
        cob.setProgramName("cmftool " + cmdName);
        
        if (help) {
            cob.usage();
            System.exit(0);
        }
        if (mainArgs == null || mainArgs.isEmpty()) {
            cob.usage();
            System.exit(1);
        }
        // Set debug logging
        if (debugFlag) {
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), org.apache.logging.log4j.Level.DEBUG);
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
        // Sanity checking
        if (catFlag && null != catPath && !"xml-catalog.xml".equals(catPath)) {
            System.err.println("-c and --catalog options are in conflict");
            System.exit(1);
        }
        if (catFlag) catPath = "xml-catalog.xml";
        
        // If output directory exists, make sure it's empty
        File od = new File(outputDir);
        try {
            if (od.exists() && (!FileUtils.isDirectory(od) || !FileUtils.isEmptyDirectory(od))) {
                System.err.println("Warning: output directory is not empty.");
            }
        } catch (IOException ex) {
            System.err.println(String.format("I/O error: %s", ex.getMessage()));
        }
        // Make sure the Xerces parser can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        // Read the model object from the model file(s)
        var mr = new ModelXMLReader();  
        var fileL = new ArrayList<File>();
        for (var str : mainArgs) fileL.add(new File(str));
        var model = mr.readFiles(fileL);
        
        var m2x =  new ModelToXMLSchema(model);
        m2x.setCatalogPath(catPath);
        m2x.setRootNamespace(rootNSarg);
        try {
            m2x.writeModelXSD(od);
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            System.exit(1);
        }
        
        // Tell user to provide external schema documents
        for (var ns : model.namespaceSet()) {
            if ("EXTERNAL".equals(ns.kindCode())) {
                System.out.println(String.format(
                    "Copy schema document for %s to %s", ns.uri(), ns.documentFilePath()));
            }
        }
        System.exit(0);        
        
    }
        
}
