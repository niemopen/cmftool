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
package org.mitre.niem.nmftool;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.xsd.ModelExtension;
import org.mitre.niem.xsd.ModelFromXSD;
import org.mitre.niem.xsd.ModelXMLWriter;
import org.mitre.niem.xsd.ParserBootstrap;
import static org.mitre.niem.xsd.ParserBootstrap.BOOTSTRAP_ALL;
import org.mitre.niem.xsd.Schema;
import org.mitre.niem.xsd.SchemaException;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Parameters(commandDescription = "convert a NIEM schema into a NIEM model instance")

class CmdXSDtoNMI implements JCCommand {

    @Parameter(names = "-o", description = "output directory for model files")
    private String outputDir = ".";
    
    @Parameter(names = "-m", description = "base name for model files")
    private String obase = null;
    
    @Parameter(names = "--cmf", description = "output model file")
    private String modelFN = null;
    
    @Parameter(names = "--cmx", description = "output model extension file")
    private String mextFN = null;
    
    @Parameter(names = {"-d","--debug"}, description = "turn on debug logging")
    private boolean debugFlag = false;
     
    @Parameter(names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
        
    @Parameter(description = "{schema, namespace URI, XML catalog}...")
    private List<String> mainArgs;
    
    CmdXSDtoNMI () {
    }
  
    CmdXSDtoNMI (JCommander jc) {
    }

    public static void main (String[] args) {       
        CmdXSDtoNMI obj = new CmdXSDtoNMI();
        obj.runMain(args);
    }
    
    @Override
    public void runMain (String[] args) {
        JCommander jc = new JCommander(this);
        NMFUsageFormatter uf = new NMFUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.setProgramName("compile");
        jc.parse(args);
        run(jc);
    }
    
    @Override
    public void runCommand (JCommander cob) {
        cob.setProgramName("nmftool x2m");
        run(cob);
    }    
    
    private void run (JCommander cob) {
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
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
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
        // Figure out file names for model and extension
        if (null == modelFN || null == mextFN) {
            if (null == obase) {
                obase = "Model";
                for (String a : mainArgs) {
                    if (a.endsWith(".xsd")) { obase = FilenameUtils.getBaseName(a); break; }
                }
            }
            if (null == modelFN) modelFN = obase + ".cmf";
            if (null == mextFN)  mextFN  = obase + ".cmx";
        }
        // Make sure output files are writable
        String modelFP = String.format("%s/%s", outputDir, modelFN);
        String mextFP  = String.format("%s/%s", outputDir, mextFN);        
        PrintWriter modelPW = null;
        PrintWriter mextPW = null;
        try {
            modelPW = new PrintWriter(modelFP);
        } catch (FileNotFoundException ex) {
            System.err.println(String.format("Can't write output file %s: %s", modelFP, ex.getMessage()));
            System.exit(1);
        }
        try {
            mextPW =  new PrintWriter(mextFP);
        } catch (FileNotFoundException ex) {
            System.err.println(String.format("Can't write output file %s: %s", mextFP, ex.getMessage()));
            System.exit(1);
        }        
        // Make sure the Xerces parser can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Parser configuration error: " + ex.getMessage());
            System.exit(1);
        }
        // Construct the schema object from arguments
        String[] aa = mainArgs.toArray(new String[0]);
        Schema s = null;
        try {
            s = Schema.genSchema(aa);
        } catch (IOException ex) {
            System.err.println(String.format("IO error reading schema documents: %s", ex.getMessage()));
            System.exit(1);
        } catch (SchemaException | ParserConfigurationException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        // Build the NIEM model object from the schema
        ModelFromXSD mfact = new ModelFromXSD();
        Model m = new Model();
        ModelExtension me = new ModelExtension(m);
        try {
            mfact.createModel(m, me, s);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        // Write the NIEM model instance to the output stream
        ModelXMLWriter mw = new ModelXMLWriter();
        try {            
            mw.writeXML(m, modelPW); modelPW.close();
            me.writeXML(mextPW);   mextPW.close();
        } catch (TransformerException ex) {
            System.err.println(String.format("Output error: %s", ex.getMessage()));
            System.exit(1);
        } catch (ParserConfigurationException ex) {
            // CAN'T HAPPEN
        }
        System.exit(0);
    }
}
