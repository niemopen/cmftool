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
      
    public static void main (String[] args) {
        CMFTool obj = new CMFTool();
        obj.run(args);
    }    
    
    private void run (String[] args) {
         
    // Uncomment arguments for debugging:
     
    String xtd = "src/test/resources/xsd/";
    if (0 == args.length) {
        args = new String[]{"x2m", "-d", xtd+"createdProp.xsd"};
//        args = new String[]{"xval", "-d", "-s", "examples/Claim-iepd/extension/claim.xsd", "examples/Claim-iepd/xml-catalog.xml"};
//        args = new String[]{"xval", "-d", "-o", xtd, xtd+"twoversions-0.xsd" };
//        args = new String[]{"xcmp", xtd+"nameinfo.xsd", xtd+"out/nameinfo.xsd"};
//        args = new String[]{"m2x", "-d", "-o", xtd+"out", xtd+"twoversions-0.cmf", xtd+"twoversions-0.cmx"};
    }
    

        JCommander jc = new JCommander();
        CMFUsageFormatter uf = new CMFUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.setProgramName("cmftool");
        
        CmdCMFtoCMF cmfToCmfCmd = new CmdCMFtoCMF(jc);
        CmdCMFtoOWL cmfToOwlCmd = new CmdCMFtoOWL(jc);
        CmdCMFtoXSD cmfToXsdCmd = new CmdCMFtoXSD(jc);
        CmdXSDtoCMF xsdToCmfCmd = new CmdXSDtoCMF(jc);       
        CmdXSDcanonicalize xsdCanon = new CmdXSDcanonicalize(jc);
        CmdXSDcmp xsdCmpCmd         = new CmdXSDcmp(jc);
        CmdXSDvalidate xsValCmd     = new CmdXSDvalidate(jc);
        CommandHelp helpCmd         = new CommandHelp(jc);    
        jc.addCommand("m2m", cmfToCmfCmd);
        jc.addCommand("m2o", cmfToOwlCmd);
        jc.addCommand("m2x", cmfToXsdCmd);
        jc.addCommand("x2m", xsdToCmfCmd);  
        jc.addCommand("xcanon", xsdCanon);
        jc.addCommand("xcmp", xsdCmpCmd);
        jc.addCommand("xval", xsValCmd);
        jc.addCommand("help", helpCmd, "usage");
        
        if (args.length < 1) {
            System.out.println("Version: " + CMFTool.class.getPackage().getImplementationVersion());
            jc.usage();
            System.exit(2);
        }
        try {
            jc.parse(args);
        }
        catch (Exception ex) {
            System.out.println("Version: " + CMFTool.class.getPackage().getImplementationVersion());
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
    
    @Parameters(commandDescription = "list of cmftool commands")
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
            System.out.println("Version: " + CMFTool.class.getPackage().getImplementationVersion());
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
