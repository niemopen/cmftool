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

package org.mitre.niem.scheval;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.utility.JCUsageFormatter;
import org.mitre.niem.xml.Schematron;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class SCHEval {
    
    @Parameter(order = 1, names = {"-s","--schema"},    description = "apply rules from this schematron file")
    private String schFN = null;
    
    @Parameter(order = 2, names = {"-x","--xslt"},      description = "apply rules from this compiled schematron file")
    private String xsltFN = null;
    
    @Parameter(order = 3, names = {"-o","--output"},    description = "write output to this file (default = stdout)")
    private String outFN = null;
    
    @Parameter(order = 4, names = {"--svrl"},           description = "write output in SVRL format")
    private boolean svrlFlag = false;
    
    @Parameter(order = 5, names = {"--compile"},        description = "compile schema and write output in XSLT format")
    private boolean compileFlag = false;
    
    @Parameter(order = 6, names = {"-c","--catalog"},   description = "provide this XML catalog file as $xml-catalog parameter")
    String catFN = null;
    
    @Parameter(order = 7, names = {"-k", "--keep"},     description = "keep temporary files")
    private boolean keepTemp = false;
            
    @Parameter(order = 8, names = {"-d","--debug"},     description = "turn on debug logging")
    private boolean debugFlag = false;

    @Parameter(order = 9, names = {"-h","--help"},      description = "display this usage message", help = true)
    boolean help = false; 
    
    @Parameter(description = "[input.xml...]")
    private List<String> mainArgs = new ArrayList<>();
    
    SCHEval () { }

    public static void main(String[] args) {
        SCHEval obj = new SCHEval();
        obj.runMain(args);
    }

    public void runMain (String[] args) {
//        if (args.length == 0) { // debug args
//            args = new String[]{
//                "-s", "../lib-util/src/test/resources/sch/refTarget.sch",
//                "-c", "../lib-util/src/test/resources/sch/xml-catalog.xml",
//                "../lib-util/src/test/resources/sch/7-10.xsd" };
//        }        
        var jc = new JCommander(this);
        var uf = new JCUsageFormatter(jc);
        jc.setUsageFormatter(uf);
        jc.setProgramName("scheval");
        if (args.length < 1) {
            System.out.println("Version: " + SCHEval.class.getPackage().getImplementationVersion());
            jc.usage();
            System.exit(2);
        }
        jc.parse(args);
        run(jc);
    }
    
    private void run (JCommander cob) {
        
        if (help) {
            System.out.println("Version: " + SCHEval.class.getPackage().getImplementationVersion());
            cob.usage();
            System.exit(0);
        }       
        if (debugFlag) {
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), org.apache.logging.log4j.Level.DEBUG);
        }
        if (null == schFN && null == xsltFN) {
            System.err.println("Error: must supply either schema or compiled schema file");
            cob.usage();
            System.exit(1);
        }
        if (!compileFlag && mainArgs.isEmpty()) {
            System.err.println("Error: must supply at least one input XML file");
            cob.usage();
            System.exit(1);
        }
        if (compileFlag && !mainArgs.isEmpty()) {
            System.err.println("Error: can't supply input XML files with --compile flag");
            cob.usage();
            System.exit(1);
        }
        if (compileFlag && null != xsltFN) {
            System.err.println("Error: can't have both --compile and --xslt");
            cob.usage();
            System.exit(1);
        }
        if (svrlFlag && null != xsltFN) {
            System.err.println("Error: can't have both --svrl and --xslt");
            cob.usage();
            System.exit(1);
        }        
        // Initialize Schematron object
        Schematron s = null;
        try {
            s = new Schematron();
        } catch (SaxonApiException ex) {
            System.err.println("Can't initialize schematron processor: " + ex.getMessage());
            System.exit(1);
        }
        // Make sure output file is writeable
        BufferedWriter outW = null;
        if (null == outFN || "".equals(outFN)) {
            var osW = new OutputStreamWriter(System.out);
            outW = new BufferedWriter(osW);
            outFN = "stdout";
        }
        else try {
            var outFW = new FileWriter(outFN);
            outW = new BufferedWriter(outFW);
        } catch (IOException ex) {
            System.err.println("Can't write to output file: " + ex.getMessage());
            System.exit(1);    
        }      
        
        // Read catalog file into XdmNode object (to become a Saxon transformer parameter)
        XdmNode catNode = null;
        var saxonProc  = new Processor(false);
        var saxonComp  = saxonProc.newXsltCompiler();
        if (null != catFN) {
            var bld = saxonProc.newDocumentBuilder();
            var catF = new File(catFN);
            try {
                catNode = bld.build(catF);
            } catch (SaxonApiException ex) {
                System.err.println("Error: can't parse catalog file: " + ex.getMessage()); 
                System.exit(1);
            }
        }
        
        // If we have Schematron rules, compile to XSLT
        File srcId;
        var xslt = "";
        if (null != schFN) {
            var schF = new File(schFN);
            var schS = new StreamSource(schF);
            var xslW = new StringWriter();
            schS.setSystemId(schF);
            srcId = schF;
            try {
                s.compileSchematron(schS, xslW);
                xslt = xslW.toString();
            } catch (SaxonApiException ex) {
                System.err.println("Error: can't parse schematron file: " + ex.getMessage());
                System.exit(1);
            }
        }
        // Otherwise read XSLT from file
        else {
            var xsltF = new File(xsltFN);
            try {
                xslt = FileUtils.readFileToString(xsltF, "UTF-8");
            } catch (IOException ex) {
                System.err.println("Error: can't read XSLT from file: " + ex.getMessage());
                System.exit(1);
            }
            srcId = xsltF;
        }
        
        // Write XSLT to output if we are just compiling
        if (compileFlag) {
            try {
                outW.write(xslt);
                outW.close();
            } catch (IOException ex) {
                System.err.println("Error writing output: " + ex.getMessage());
                System.exit(1);
            }
            System.exit(0);
        }
        
        // Create transformer from XSLT text
        var xsltR = new StringReader(xslt);
        var xsltS = new StreamSource(xsltR);
        xsltS.setSystemId(srcId);
        XsltTransformer trans = null;
        try {
            trans = saxonComp.compile(xsltS).load();
            trans.setParameter(new QName("allow-foreign"), new XdmAtomicValue("true"));
            if (null != catNode) 
                trans.setParameter(new QName("xml-catalog"), catNode); 
        } catch (SaxonApiException ex) {
            System.err.println("Error: can't compile XSLT: " + ex.getMessage());
            System.exit(1);
        }
        
        // Run the compiled SCH on each input XML file
        // Maybe write SVRL, maybe parse it and write the result
        for (var xmlFN : mainArgs) {
            var svrlW = new StringWriter();
            var xmlF  = new File(xmlFN);
            try {
                var xmlR = new FileReader(xmlF);
                var xmlS = new StreamSource(xmlR);
                xmlS.setSystemId(xmlF.toURI().toString());
                s.applyXslt(xmlS, trans, svrlW);
            } catch (SaxonApiException ex) {
                Logger.getLogger(SCHEval.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(SCHEval.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (svrlFlag) {
                try {
                    outW.write(svrlW.toString());
                } catch (IOException ex) {
                    System.err.println("Error writing SVRL: " + ex.getMessage());
                    System.exit(1);
                }
                continue;
            }
            try {
                var svrlR = new StringReader(svrlW.toString());
                var svrlS = new InputSource(svrlR);
                var xmlR  = new FileReader(xmlF);
                var xmlIS = new InputSource(xmlR);
                xmlIS.setSystemId(xmlF.toURI().toString());
                s.SVRLtoMessages(svrlS, xmlIS, outW);
            } catch (ParserConfigurationException ex) {
                System.err.println("Error: parser configuration error: " + ex.getMessage());
                System.exit(1);
            } catch (SAXException | IOException | TransformerException ex) {
                System.err.println("Error turning SVRL into messages: " + ex.getMessage());
                System.exit(1);
            }
        }      
        try {
            outW.close();
        } catch (IOException ex) {
            Logger.getLogger(SCHEval.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.exit(0);        
    }

}
