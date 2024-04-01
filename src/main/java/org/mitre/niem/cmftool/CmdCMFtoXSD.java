/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2021 The MITRE Corporation.
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.xsd.ModelToMsgXSD;
import org.mitre.niem.xsd.ModelToN5XSD;
import org.mitre.niem.xsd.ModelToSrcXSD;
import org.mitre.niem.xsd.ModelToXSD;
import org.mitre.niem.xsd.ModelXMLReader;
import org.mitre.niem.xsd.ParserBootstrap;
import static org.mitre.niem.xsd.ParserBootstrap.BOOTSTRAP_ALL;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Parameters(commandDescription = "write a NIEM model as a NIEM schema")
        
class CmdCMFtoXSD implements JCCommand {
    
    @Parameter(order = 0, names = "-o", description = "write schema pile into this directory")
    private String outputDir = "";

    @Parameter(order = 1, names = "-c", description = "generate xml-catalog.xml file")
    private boolean catFlag = false;
    
    @Parameter(order = 2, names = "--catalog", description = "write XML catalog into this file")
    private String catPath = null;
    
    @Parameter(order = 3, names = "--msgNS", description = "set namespace with this prefix or URI as message schema root")
    private String messageNS = null;
    
    @Parameter(names = {"-d","--debug"}, description = "turn on debug logging")
    private boolean debugFlag = false;
    
    @Parameter(names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
        
    @Parameter(description = "modelFile.cmf")
    private List<String> mainArgs;
    
    CmdCMFtoXSD () {
    }
  
    CmdCMFtoXSD (JCommander jc) {
    }

    public static void main (String[] args) {       
        CmdCMFtoCMF obj = new CmdCMFtoCMF();
        obj.runMain(args);
    }
    
    @Override
    public void runMain (String[] args) {
        JCommander jc = new JCommander(this);
        CMFUsageFormatter uf = new CMFUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
//        jc.setProgramName("generateXSD");
        jc.parse(args);
        run(jc);
    }
    
    @Override
    public void runCommand (JCommander cob) {
//        cob.setProgramName("cmftool m2x");
        run(cob);
    }    
    
    protected void run (JCommander cob) {
        // Make sure the command is valid
        String cmdName = cob.getProgramName();
        switch (cmdName) {
            case "m2x5":           // model to N5 schema
            case "m2xs":          // model to N6 source schema (ref or ext)
            case "m2xm": break;   // model to N6 message schema
            default:
                System.err.println("unknown command: cmftool " + cmdName);
                System.exit(1);
        }
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
                System.err.println(String.format("Output directory %s is not empty", outputDir));
                System.exit(1);
            }
        } catch (IOException ex) {
            System.err.println(String.format("Error checking output directory: %s", ex.getMessage()));
        }
        // Make sure the Xerces parser can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        // One or two arguments: cmf, maybe cmx
        if (mainArgs.size() < 1 || mainArgs.size() > 2) {
            cob.usage();
        }
        // Read the model object from the model file
        Model m = null;
        File ifile = new File(mainArgs.get(0));
        FileInputStream is = null;
        try {
            is = new FileInputStream(ifile);
        } catch (FileNotFoundException ex) {
            System.err.println(String.format("Error reading model file: %s", ex.getMessage()));
            System.exit(1);
        }
        ModelXMLReader mr = new ModelXMLReader();
        m = mr.readXML(is);
        if (null == m) {
            List<String> msgs = mr.getMessages();
            System.err.print("Could not construct model object:");
            if (1 == msgs.size()) System.err.print(msgs.get(0));
            else msgs.forEach((xm) -> { System.err.print("\n  "+xm); });
            System.err.println();
            System.exit(1);         
        }        
        // Create the output directory if necessary
        if (!od.exists()) {
            try {
                FileUtils.forceMkdir(od);
            } catch (IOException ex) {
                System.err.println(String.format("Can't create output directory: %s", ex.getMessage()));
                System.exit(1);
            }
        }
        // Write the proper kind of NIEM model XSD to the output stream
        ModelToXSD mw = null;
        switch (cmdName) {
            case "m2x5" -> mw = new ModelToN5XSD(m);
            case "m2xs" -> mw = new ModelToSrcXSD(m);
            case "m2xm" -> mw = new ModelToMsgXSD(m);
            default -> {
                System.err.println("unknown command: cmftool " + cmdName);
                System.exit(1);
            }        
        }
        try {
            mw.setCatalog(catPath);
            mw.setMessageNamespace(messageNS);
            mw.writeXSD(od);
        } catch(Exception ex) {
            System.err.println("error: " + ex.getMessage());
            var trace = ex.getStackTrace();
            for (int i = 0; i < trace.length; i++) {
                var tr = trace[i];
                System.err.println("  at " + tr.getFileName() + ", line " + tr.getLineNumber());
            }
            System.exit(1);
        }
        System.exit(0);
    }    
}
