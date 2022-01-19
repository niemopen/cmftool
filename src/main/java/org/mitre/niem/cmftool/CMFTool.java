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
import java.util.List;
import java.util.Map;

/**
 * The "cmftool" command line program
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class CMFTool {
    
    public static String VERSION_ID = "cmftool 0.2.1 (10 November 2021)";
    
    public static void main (String[] args) {
        CMFTool obj = new CMFTool();
        obj.run(args);
    }    
    
    private void run (String[] args) {
        
// Uncomment arguments for debugging:

        args = new String[]{"m2r", "examples/CrashDriver-iepd/CrashDriver.cmf"};

//        args = new String[]{"x2m", "-d", "-o", "src/test/resources/xsd", "src/test/resources/xsd/augment-0.xsd"};
//        args = new String[]{"m2x", "-d", "-o", "examples/Test/cmf", "examples/Test/deprecated.cmf", "examples/Test/deprecated.cmx"};
//        args = new String[]{"x2m", "-o", "examples/CrashDriver-iepd", "examples/CrashDriver-iepd/xsd/extension/CrashDriver.xsd",
//             "examples/CrashDriver-iepd/xsd/xml-catalog.xml"};

//        args = new String[]{"x2m", "-o", "examples/TwoVersions", "examples/TwoVersions/PersonName.xsd",
//             "examples/TwoVersions/PercentType.xsd"};      

//        args = new String[]{"x2m", "-d", "-o", "examples/Test", "examples/Test/CodeType.xsd" };

//        args = new String[]{"x2m", "-d", "-o", "examples/Test", "examples/Test/Degree90Type.xsd" };

//        args = new String[]{"x2m", "-o", "/tmp/cmf", "/Work/Stuff/NIEM/Releases/niem-5.0/xsd/niem-core.xsd",
//            "/Work/Stuff/NIEM/Releases/niem-5.0/xsd/xml-catalog.xml" };
         
//        args = new String[]{"m2x",  "-o", "/tmp/cmf", 
//          "examples/CrashDriver-iepd/CrashDriver.cmf", "examples/CrashDriver-iepd/CrashDriver.cmx"};

//        args = new String[]{"m2x", "-o", "/tmp/cmf",
//            "examples/TwoVersions/PersonName.cmf", "examples/TwoVersions/PersonName.cmx"};

//        args = new String[]{"m2x", "-o", "/tmp/cmf",
//            "examples/Test/CodeType.cmf", "examples/Test/CodeType.cmx"};


        JCommander jc = new JCommander();
        CMFUsageFormatter uf = new CMFUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.setProgramName("cmftool");
        
        CmdCMFtoCMF nmiToNmiCmd = new CmdCMFtoCMF(jc);
        CmdCMFtoRDF nmiToRdfCmd = new CmdCMFtoRDF(jc);
        CmdCMFtoXSD nmiToXsdCmd = new CmdCMFtoXSD(jc);
        CmdXSDtoCMF xsdToNmiCmd = new CmdXSDtoCMF(jc);        
        CommandHelp helpCmd     = new CommandHelp(jc);    
        jc.addCommand("m2m", nmiToNmiCmd);
        jc.addCommand("m2r", nmiToRdfCmd);
        jc.addCommand("m2x", nmiToXsdCmd);
        jc.addCommand("x2m", xsdToNmiCmd);        
        jc.addCommand("help", helpCmd, "usage");
        
        if (args.length < 1) {
            jc.usage();
            System.exit(2);
        }
        try {
            jc.parse(args);
        }
        catch (Exception ex) {
            jc.usage();
            System.exit(2);
        }
        String command = jc.getParsedCommand();      
        Map<String,JCommander> cmdMap = jc.getCommands();
        JCommander cob = cmdMap.get(command);
        List<Object> objs = cob.getObjects();
        JCCommand cmd = (JCCommand)objs.get(0);
        CMFUsageFormatter cobuf = new CMFUsageFormatter(cob);        
        cob.setUsageFormatter(cobuf);
        cmd.runCommand(cob);               
    }    
    
    @Parameters(commandDescription = "list of niemtran commands")
    private class CommandHelp implements JCCommand {
        
        @Parameter(description = "display help for this command")
        List<String> helpArgs;
        
        private final JCommander jc;
        
        CommandHelp (JCommander jc) {
            this.jc = jc;
        }
        
        @Override
        public void runMain (String[] args) {
        }

        @Override
        public void runCommand(JCommander helpOb) {
            System.out.println("Version: " + VERSION_ID);
            if (helpArgs != null && !helpArgs.isEmpty()) {
                String cmdName = helpArgs.get(0);
                Map<String, JCommander> cmdMap = jc.getCommands();
                JCommander cob = cmdMap.get(cmdName);
                List<Object> objs = cob.getObjects();
                JCCommand cmd = (JCCommand) objs.get(0);  
                CMFUsageFormatter cobuf = new CMFUsageFormatter(cob);        
                cob.setUsageFormatter(cobuf);    
                String[] ha = {"--help"};
                cmd.runMain(ha);
            }
            else {
                jc.usage();
                System.exit(0);
            }
        }
    }
    
}
