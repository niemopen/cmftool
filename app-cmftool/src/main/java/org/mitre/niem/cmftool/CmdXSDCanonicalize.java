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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.mitre.niem.utility.JCUsageFormatter;
import org.mitre.niem.xml.CanonicalXSD;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_ALL;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
@Parameters(commandDescription = "canonicalize an XML Schema document")

public class CmdXSDCanonicalize implements JCCommand {
    
    private boolean inPlace = false;
    private String backSuf = "";

    @Parameter(order = 1, names = "-i", description = "canonicalize in place (-ibak = keep original with .bak suffix)")
    private boolean inPlaceOpt =  false;
    
    @Parameter(order = 0, names = "-o", description = "file for converter output")
    private String objFile = "";
     
    @Parameter(order = 2, names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
        
    @Parameter(description = "schemaDoc.xsd ...")
    private List<String> mainArgs;
    
    CmdXSDCanonicalize () {
    }
  
    CmdXSDCanonicalize (JCommander jc) {
    }

    public static void main (String[] args) {       
        var obj = new CmdXSDCanonicalize();
        obj.runMain(args);
    }
    
    @Override
    public void runMain (String[] args) {
        var jc = new JCommander(this);
        var uf = new JCUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.setProgramName("XSDcanonicalize");
        jc.parse(args);
        run(jc);
    }
    
    @Override
    public void runCommand (JCommander cob) {
        cob.setProgramName("cmftool xcanon");
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

        // Look for -ibak option
       inPlace = inPlaceOpt;        // handle naked -i option 
       if (mainArgs.get(0).startsWith("-i")) {
            var iarg = mainArgs.remove(0);
            inPlace = true;
            backSuf = iarg.substring(2);
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
        // Should be one argument now, the XML Schema document
        if (mainArgs.size() != 1) {
            cob.usage();
        }
        // Make sure the Xerces parser can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        // Make sure the input document can be read
        File inF = new File(mainArgs.get(0));
        FileInputStream is = null;
        try {
            is = new FileInputStream(inF);
        } catch (FileNotFoundException ex) {
            System.err.println(String.format("Error reading model file: %s", ex.getMessage()));
            System.exit(1);
        }
        // Create filenames for inplace operation
        var outDir = inF.getAbsoluteFile().getParentFile();
        var outF   = createSufFile(inF, "tmp");
        File backF = null;
        if (!backSuf.isBlank()) backF = createSufFile(inF, backSuf);
        
        // Create output stream
        OutputStream os = System.out;
        if (inPlace) {
            try {
                os = new FileOutputStream(outF);;
            } catch (FileNotFoundException ex) {
                System.err.println(String.format("Can't write to output file %s: %s", outF.getAbsolutePath(), ex.getMessage()));
                System.exit(1);
            }
        }
        // Write canonical XSD to output writer
        try {
            var ow = new OutputStreamWriter(os, "UTF-8");
            CanonicalXSD.canonicalize(is, ow);
            os.close();
        } catch (ParserConfigurationException | SAXException | TransformerException ex) {
            System.err.println(String.format("%s error: %s", ex.getClass().getName(), ex.getMessage()));
            System.exit(1);
        } catch (IOException ex) {
            System.err.println(String.format("IO error: %s", ex.getMessage()));
            System.exit(1);
        }
        // Not inplace? All done!
        if (!inPlace) System.exit(0);
        
        // Rename input file to backup file, if one is desired
        if (null != backF) {
            if (!inF.renameTo(backF)) {
                System.err.println(String.format("Could not rename input file %s to %s", inF.getAbsolutePath(), backF.getAbsolutePath()));
                if (!outF.delete()) {
                    System.err.println("Could not delete temporary output file " + outF.getAbsolutePath());
                }
                System.exit(1);
            }
        }
        // Otherwise delete the input file
        else {
            if (!inF.delete()) {
                System.err.println("Could not delete input file " + inF.getAbsolutePath());            
                System.exit(1);
            }
        }
        // Rename temp file to input file
        if (!outF.renameTo(inF)) {
            System.err.println(String.format("Could not rename output file %s to %s", outF.getAbsolutePath(), inF.getAbsolutePath()));    
            System.exit(1);
        }
        System.exit(0);        
    }   
    
    private File createSufFile (File f, String suf) {
        int tries = 0;
        var ipath = f.getAbsolutePath();
        var rpath = ipath + "." + suf;
        var rfile = new File(rpath);
        while (rfile.exists()) {
            rpath = String.format("%s.%s%02d", ipath, suf, tries++);
            rfile = new File(rpath);
        }
        return rfile;
    }
}

