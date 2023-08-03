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
import java.io.PrintWriter;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.rdf.ModelToOWL;
import org.mitre.niem.xsd.ModelXMLReader;
import org.mitre.niem.xsd.ParserBootstrap;
import static org.mitre.niem.xsd.ParserBootstrap.BOOTSTRAP_ALL;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Parameters(commandDescription = "convert a NIEM model to OWL")
        
class CmdCMFtoOWL implements JCCommand {
    
    @Parameter(names = "-o", description = "file for converter output")
    private String objFile = "";
     
    @Parameter(names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
        
    @Parameter(description = "modelFile.cmf")
    private List<String> mainArgs;    
    
    CmdCMFtoOWL () {
    }
  
    CmdCMFtoOWL (JCommander jc) {
    }

    public static void main (String[] args) {       
        CmdCMFtoOWL obj = new CmdCMFtoOWL();
        obj.runMain(args);
    }
    
    @Override
    public void runMain (String[] args) {
        JCommander jc = new JCommander(this);
        CMFUsageFormatter uf = new CMFUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.setProgramName("generateOWL");
        jc.parse(args);
        run(jc);
    }
    
    @Override
    public void runCommand (JCommander cob) {
        cob.setProgramName("cmftool m2o");
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
        // Make sure output file is writable
        PrintWriter ow = new PrintWriter(System.out);
        if (!"".equals(objFile)) {
            try {
                File of = new File(objFile);
                ow = new PrintWriter(of);
            } catch (FileNotFoundException ex) {
                System.err.println(String.format("Can't write to output file %s: %s", objFile, ex.getMessage()));
                System.exit(1);
            }
        }
        // Make sure the Xerces parser can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        // Single argument should be the model instance file
        if (mainArgs.size() != 1) {
            cob.usage();
            System.exit(1);
        }
        // Read the model object from the model instance file
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
        // Write the NIEM model instance to the output stream
        ModelToOWL rdfw = new ModelToOWL(m);
        rdfw.writeRDF(ow);
        ow.close();
        System.exit(0);
    }    
}
