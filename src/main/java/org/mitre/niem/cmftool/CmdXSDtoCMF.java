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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Model;
import static org.mitre.niem.cmf.NamespaceKind.NSK_CORE;
import static org.mitre.niem.cmf.NamespaceKind.NSK_DOMAIN;
import static org.mitre.niem.cmf.NamespaceKind.NSK_EXTENSION;
import static org.mitre.niem.cmf.NamespaceKind.NSK_EXTERNAL;
import static org.mitre.niem.cmf.NamespaceKind.NSK_OTHERNIEM;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UNKNOWN;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XML;
import static org.mitre.niem.cmf.NamespaceKind.NSK_XSD;
import org.mitre.niem.xsd.ModelFromXSD;
import org.mitre.niem.xsd.ModelXMLWriter;
import org.mitre.niem.xsd.ParserBootstrap;
import static org.mitre.niem.xsd.ParserBootstrap.BOOTSTRAP_ALL;
import org.mitre.niem.xsd.XMLSchema;
import org.mitre.niem.xsd.XMLSchemaDocument;
import org.xml.sax.SAXException;
import static org.mitre.niem.cmf.NamespaceKind.NSK_BUILTIN;
import static org.mitre.niem.cmf.NamespaceKind.cta2Arch;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Parameters(commandDescription = "convert a NIEM schema into a NIEM model instance")

class CmdXSDtoCMF implements JCCommand {

    @Parameter(names = "-o", description = "output model file")
    private String modelFN = null;

    @Parameter(names = {"-d","--debug"}, description = "turn on debug logging")
    private boolean debugFlag = false;
    
    @Parameter(names = {"-q", "--quiet"}, description = "no output, exit status only")
    private boolean quietFlag = false;
     
    @Parameter(names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
        
    @Parameter(description = "{schema, namespace URI, XML catalog}...")
    private List<String> mainArgs;
    
    CmdXSDtoCMF () {
    }
  
    CmdXSDtoCMF (JCommander jc) {
    }

    public static void main (String[] args) {       
        CmdXSDtoCMF obj = new CmdXSDtoCMF();
        obj.runMain(args);
    }
    
    @Override
    public void runMain (String[] args) {
        JCommander jc = new JCommander(this);
        CMFUsageFormatter uf = new CMFUsageFormatter(jc); 
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
        if (null == modelFN) {
            var obase = "Model";
            for (String a : mainArgs) {
                if (a.endsWith(".xsd")) { obase = FilenameUtils.getBaseName(a); break; }
            }
            modelFN = obase + ".cmf";
        }
        // Make sure output model file is writable      
        FileOutputStream os = null;        
        try {
            os = new FileOutputStream(modelFN);        
        } catch (FileNotFoundException ex) {
            System.err.println(String.format("Can't write to output file %s: %s", modelFN, ex.getMessage()));
            System.exit(1);            
        }       
        // Make sure the Xerces parsers can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Parser configuration error: " + ex.getMessage());
            System.exit(1);
        }
        // Construct the schema object from arguments
        String[] aa = mainArgs.toArray(new String[0]);
        XMLSchema s = null;
        Model m = null;
        Map<String,XMLSchemaDocument> sdoc = null;
        try {
            ModelFromXSD mfact = new ModelFromXSD();
            s = new XMLSchema(aa);
            m = mfact.createModel(s);
            sdoc = s.schemaDocuments();
        } catch (IOException ex) {
            System.err.println(String.format("IO error reading schema documents: %s", ex.getMessage()));
            System.exit(1);
        } catch (XMLSchema.XMLSchemaException | CMFException | SAXException ex) {
            System.err.println(String.format("Error building XML schema: %s", ex.getMessage()));
            System.exit(1);
        } catch (ParserConfigurationException ex) {
            System.err.println("Parser configuration error: " + ex.getMessage());
            System.exit(1);
        }
        // Report namespaces processed
        if (!quietFlag) {
            if (!s.xsModelMsgs().isEmpty()) {
                System.out.println("Schema assembly messages:");
                for (var msg : s.xsModelMsgs()) System.out.println("  " + msg);                
            }
            List<String> model      = new ArrayList<>();
            List<String> conforming = new ArrayList<>();
            List<String> external   = new ArrayList<>();
            List<String> builtins   = new ArrayList<>();
            List<String> unknown    = new ArrayList<>();
            sdoc.forEach((nsuri, sd) -> { 
                switch(sd.schemaKind()) {
                    case NSK_EXTENSION:
                    case NSK_DOMAIN:
                    case NSK_CORE:
                    case NSK_OTHERNIEM:
                        model.add(nsuri);
                        break;
                    case NSK_BUILTIN:
                    case NSK_XML:
                    case NSK_XSD:
                        builtins.add(nsuri);
                        break;
                    case NSK_EXTERNAL: external.add(nsuri); break;
                    case NSK_UNKNOWN:  unknown.add(nsuri); break; 
                }
            });
            var hdr = "Namespaces with problems:\n";
            for (var ns : model) {
                var sd   = sdoc.get(ns);
                var arch = sd.niemArch();
                if (arch.isBlank()) {
                    System.out.print(String.format("%s  %s [unknown NIEM architecture]\n", hdr, ns));
                    hdr = "";
                    continue;
                }
                var mflg = false;
                var cts  = sd.conformanceTargets();
                if (null != cts) {
                    var ctl = cts.split("\\s+");
                    for (int i = 0; !mflg && i < ctl.length; i++) {
                        var ct = ctl[i];
                        if (arch.equals(cta2Arch(ct))) {
                            conforming.add(ns);
                            mflg = true;
                        }
                    }
                }
                if (!mflg) {
                    System.out.print(String.format("%s  %s [no valid conformance assertion]\n", hdr, ns));
                    hdr = "";
                }
            }
            if (!conforming.isEmpty()) {
                Collections.sort(conforming);
                System.out.println("Conforming namespaces:");
                for (var ns : conforming) {
                    var sd  = sdoc.get(ns);
                    var ver = sd.niemVersion();
                    System.out.println(String.format("  %s [NIEM version='%s']", ns, ver));
                }
            }
            if (!external.isEmpty()) {
                Collections.sort(external);
                System.out.println("External namespaces (imported with appinfo:externalNamespaceIndicator):");
                external.forEach((ns) -> { System.out.println("  " + ns);});
            }
            if (!builtins.isEmpty()) {
                Collections.sort(builtins);
                System.out.println("Built-in namespaces:");
                builtins.forEach((ns) -> { System.out.println("  "+ns);});
            }
            if (!unknown.isEmpty()) {
                Collections.sort(unknown);
                System.out.println("Unknown namespaces (no conformance assertion found):");
                unknown.forEach((ns) -> { System.out.println("  " + ns);});
            }
        }
        // Write the NIEM model instance to the output stream
        ModelXMLWriter mw = new ModelXMLWriter();
        try {            
            mw.writeXML(m, os); 
            os.close();
        } catch (TransformerException | IOException ex) {
            System.err.println(String.format("Output error: %s", ex.getMessage()));
            System.exit(1);
        } catch (ParserConfigurationException ex) {
            // CAN'T HAPPEN
        }
        System.exit(0);
    }
}
