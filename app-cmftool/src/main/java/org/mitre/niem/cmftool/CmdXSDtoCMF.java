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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLWriter;
import org.mitre.niem.utility.JCUsageFormatter;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_ALL;
import org.mitre.niem.xml.XMLSchemaException;
import org.mitre.niem.xsd.ModelFromXSD;
import org.mitre.niem.xsd.NIEMSchema;
import static org.mitre.niem.xsd.NamespaceKind.*;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
@Parameters(commandDescription = "convert a NIEM model from XSD to CMF")
class CmdXSDtoCMF implements JCCommand {

    @Parameter(order = 1, names = "-o", description = "name of output model file")
    private String modelFN = null;

    @Parameter(order = 2, names = "--only", description = "include only these namespace URIs or prefixes; eg. \"--only nc,j\"")
    private String onlyArg = null;
        
    @Parameter(order = 3, names = {"-d","--debug"}, description = "turn on debug logging")
    private boolean debugFlag = false;
    
    @Parameter(names = {"-p","--profile"}, description = "pause for profiler attachment", hidden = true)
    private boolean profileFlag = false;
//
//    @Parameter(names = {"-q", "--quiet"}, description = "no output, exit status only")
//    private boolean quietFlag = false;
     
    @Parameter(order = 4, names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
        
    @Parameter(description = "{schema, namespace URI, XML catalog}...")
    private List<String> mainArgs;
    
    CmdXSDtoCMF () {
    }
  
    CmdXSDtoCMF (JCommander jc) {
    }

    public static void main (String[] args) {       
        var obj = new CmdXSDtoCMF();
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
        cob.setProgramName("cmftool x2m");
        run(cob);
    }    
    
    private void run (JCommander cob) {
        if (profileFlag) {
            try {
                System.err.println("sleeping");
                Thread.sleep(5009);
                System.err.println("resuming");
            } catch (InterruptedException ex) {
                Logger.getLogger(CmdXSDtoCMF.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }
        if (help) {
            cob.usage();
            System.exit(0);
        }
        if (mainArgs == null || mainArgs.isEmpty()) {
            cob.usage();
            System.exit(1);
        }
        // Set debug logging 
        // FIXME quiet
        if (debugFlag) {
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
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
        // Figure out file names for model and extension
        if (null == modelFN) {
            var obase = "Model";
            for (var a : mainArgs) {
                if (a.endsWith(".xsd")) { obase = FilenameUtils.getBaseName(a); break; }
            }
            modelFN = obase + ".cmf";
        }
        // Make sure output model file is writable      
        OutputStreamWriter ow = null;
        try {
            var os = new FileOutputStream(modelFN);
            ow = new OutputStreamWriter(os, "UTF-8");
        } catch (IOException ex) {
            System.err.println(String.format("Can't write to output file %s: %s", modelFN, ex.getMessage()));
            System.exit(1);            
        }       
        // Make sure the Xerces parsers can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Internal parser error: " + ex.getMessage());
            System.exit(1);
        }
        // Construct the schema object from arguments
        String[] aa = mainArgs.toArray(new String[0]);
        NIEMSchema s = null;
        Model m = null;        
        try {           
            var mfact = new ModelFromXSD();
            s = new NIEMSchema(aa);
            m = mfact.createModel(s);
        } catch (XMLSchemaException | CMFException ex) {
            System.err.println(String.format("Error building XML schema: %s", ex.getMessage()));
            System.exit(1);
        }
        // Convert onlyArg to list of namespace URIs/prefixes
        var onlyL = new ArrayList<String>();
        if (null != onlyArg) {
            onlyL.addAll(Arrays.asList(onlyArg.split("\\s*,\\s*")));
        }
        // Write the NIEM model instance to the output stream
        var mw = new ModelXMLWriter();
        try {            
            if (onlyL.isEmpty()) mw.writeXML(m, ow); 
            else mw.writeXML(m, onlyL, ow);
            ow.close();
        }catch (IOException ex) {
            System.err.println("Output error: " + ex.getMessage());
            System.exit(1);
        }
        // Report various error and warning messages captured in the schema object
//        if (quietFlag) System.exit(0);
        var catmsgL = s.resolver().allMessages();
        var schmsgL = s.xsModelMsgs();
        if (!catmsgL.isEmpty()) {
            System.err.println("Catalog resolver messages:");
            for (var msg : catmsgL) System.err.println("  " + msg);
        }
        if (!schmsgL.isEmpty()) {
            System.err.println("Schema assembly messages:");
            for (var msg : schmsgL) System.err.println("  " + msg);
        }
        // Categorize namespaces in the pile, report by kind
        var model      = new ArrayList<String>();
        var conforming = new ArrayList<String>();
        var external   = new ArrayList<String>();
        var builtins   = new ArrayList<String>();
        var unknown    = new ArrayList<String>();          
        for (var sdU: s.schemaNamespaceUs()) {
            var kind = s.namespaceKind(sdU);
            switch(kind) {
            case NSK_EXTENSION:
            case NSK_DOMAIN:
            case NSK_CORE:
            case NSK_OTHERNIEM:
            case NSK_CLI:
                model.add(sdU);
                break;
            case NSK_APPINFO:
            case NSK_CLSA:
            case NSK_NIEM_XS:
            case NSK_STRUCTURES:
            case NSK_XML:
            case NSK_XSD:
                builtins.add(sdU);
                break;
            case NSK_EXTERNAL: 
                external.add(sdU); 
                break;
            case NSK_NOTNIEM:
            case NSK_UNKNOWN:  
                unknown.add(sdU); 
                break; 
            }
        }
        var hdr = "Namespaces with problems:\n";
        for (var nsU : model) {
            var sd   = s.schemaDocument(nsU);
            var ctas = sd.ctasNS();
            var ctaL = sd.ctAssertions();
            var vers = sd.niemVersion();
            if (ctas.isBlank()) {
                System.out.println(String.format("%s  %s [no CTAS namespace]", hdr, nsU));
                hdr = "";
            }
            if (ctaL.isEmpty()) {
                System.out.println(String.format("%s  %s [no conformance target assertion]", hdr, nsU));
                hdr = "";
            }
            if (vers.isBlank()) {
                System.out.println(String.format("%s  %s [can't determine NIEM architecture version]", hdr, nsU));
                hdr = "";
            }
            else conforming.add(nsU);
        }
        Collections.sort(conforming);
        hdr = "Namespaces claiming conformance:\n";
        for (var nsU : conforming) {
            var sd   = s.schemaDocument(nsU);
            var vers = sd.niemVersion();
            System.out.println(String.format("%s  %s [NIEM version='%s']", hdr, nsU, vers));
            hdr = "";
        }
        Collections.sort(external);
        hdr = "External namespaces (imported with appinfo:externalNamespaceIndicator):\n";
        for (var nsU : external) {
            System.out.println(String.format("%s  %s", hdr, nsU));
            hdr = "";
        }
        Collections.sort(builtins);
        hdr = "Utililty and predefined namespaces:\n";
        for (var nsU : builtins) {
            System.out.println(String.format("%s  %s", hdr, nsU));
            hdr = "";
        }
        Collections.sort(unknown);
        hdr = "Unknown namespaces (no conformance assertion found):\n";
        for (var nsU : unknown) {
            System.out.println(String.format("%s  %s", hdr, nsU));
            hdr = "";
        }
        System.exit(0);
    }
}
