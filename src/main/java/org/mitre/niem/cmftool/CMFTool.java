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
    String dir = "tmp/02-NoAug/";
    if (0 == args.length) {
        args = new String[]{"x2m", "-o", "tmp/claim/t.cmf", "tmp/claim/xsd/claim.xsd/", "tmp/claim/xsd/niem-core.xsd",
            "tmp/claim/xsd/codes/iso_4217.xsd"};

//        args = new String[]{"m2xm", "-o", "examples/Claim-iepd/tmp", "examples/Claim-iepd/claim.cmf"};
//        args = new String[]{"x2m", "-o", "tmp/aug/x.cmf", "tmp/aug/messageModel.xsd", "tmp/aug/niem-core.xsd" };
    }
    

        JCommander jc = new JCommander();
        CMFUsageFormatter uf = new CMFUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.setProgramName("cmftool");
        
        var cmfToCmfCmd    = new CmdCMFtoCMF(jc);
        var cmfToN5XsdCmd  = new CmdCMFtoN5XSD(jc);
        var cmfToMsgXsdCmd = new CmdCMFtoMsgXSD(jc);
        var cmfToSrcXsdCmd = new CmdCMFtoSrcXSD(jc);
        var cmfToOwlCmd    = new CmdCMFtoOWL(jc);
        var n5To6Cmd       = new CmdN5To6(jc);
        var xsdToCmfCmd    = new CmdXSDtoCMF(jc);       
        var xsdCanon       = new CmdXSDcanonicalize(jc);
        var xsdCmpCmd      = new CmdXSDcmp(jc);
        var xsValCmd       = new CmdXSDvalidate(jc);
        var helpCmd        = new CommandHelp(jc);    
        jc.addCommand("m2m",    cmfToCmfCmd);
        jc.addCommand("m2o",    cmfToOwlCmd);
        jc.addCommand("m2xs",   cmfToSrcXsdCmd);
        jc.addCommand("m2xm",   cmfToMsgXsdCmd);
        jc.addCommand("m2x5",   cmfToN5XsdCmd);
        jc.addCommand("n5to6",  n5To6Cmd);
        jc.addCommand("x2m",    xsdToCmfCmd);  
        jc.addCommand("xcanon", xsdCanon);
        jc.addCommand("xcmp",   xsdCmpCmd);
        jc.addCommand("xval",   xsValCmd);
        jc.addCommand("help",   helpCmd);
        
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
    
    @Parameters(commandDescription = "this list of cmftool commands")
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
                cmd.runCommand(cob);
            }
            else {
                jc.usage();
                System.exit(0);
            }
        }
    }
    
}
