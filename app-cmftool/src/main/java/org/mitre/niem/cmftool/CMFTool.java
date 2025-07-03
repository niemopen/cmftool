/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2025 The MITRE Corporation.
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

import java.util.List;
import java.util.Map;

import org.mitre.niem.utility.JCUsageFormatter;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class CMFTool {
    
   public static void main (String[] args) {
        CMFTool obj = new CMFTool();
        obj.run(args);
    }    

    private void run (String[] args) {
    
    // check environment
    final boolean DEBUG = false; // Set to true to enable debug prints
    if (DEBUG) {
        System.out.println("JDK: " + System.getProperty("java.version"));
        System.out.println("APP_HOME: " + System.getenv("APP_HOME"));
        System.out.println("user.dir: " + System.getProperty("user.dir"));
        System.out.println("java.class.path: " + System.getProperty("java.class.path"));
        System.out.println("Thread Context ClassLoader: " + Thread.currentThread().getContextClassLoader());
        System.out.println("java.io.tmpdir: " + System.getProperty("java.io.tmpdir"));
        try {
            System.out.println("STDIN Available: " + System.in.available());
        } catch (Exception ex) {
            System.out.println("STDIN not available: " + ex.getMessage());
        }
        // System.out.println("Resolved resource: " + AppEnvironmentHelper.class.getResource("/config/app.yaml"));
    }
    
        // Uncomment arguments for debugging:
//    String res = "../lib-cmf/src/test/resources/xsd5/";     
//    String res = "../lib-cmf/src/test/resources/xsd6/";
    String res = "../../../NIEM/ntac-admin/examples/src/Augmentation/09-ASCwA/";
    if (0 == args.length) {
//        args = new String[]{"m2x", "-o", res+"tmp", res+"globObjAug.cmf"};
//        args = new String[]{"m2xmsg", "-o", res+"message.xsd", res+"model.cmf"};
        args = new String[]{"x2m", "-o", res+"model.cmf", res+"model.xsd/msg.xsd"};
    }
    

        var jc = new JCommander();
        var uf = new JCUsageFormatter(jc);
        jc.setUsageFormatter(uf);
        jc.setProgramName("cmftool");
        
        var cmfToJSONSchemaCmd = new CmdCMFtoJSONSchema(jc);
        var cmfToCmfCmd  = new CmdCMFtoCMF(jc);
        var cmfToRDFCmd  = new CmdCMFtoRDF(jc);
        var cmfToXsdModelCmd  = new CmdCMFtoXSDModel(jc);
        var cmfToXMLSchemaCmd = new CmdCMFtoXMLSchema(jc);
//        var cmfToSrcXsdCmd = new CmdCMFtoSrcXSD(jc);
//        var cmfToOwlCmd    = new CmdCMFtoOWL(jc);
//        var n5To6Cmd       = new CmdN5To6(jc);
        var xsdToCmfCmd    = new CmdXSDtoCMF(jc);       
//        var xsdCanon       = new CmdXSDcanonicalize(jc);
//        var xsdCmpCmd      = new CmdXSDcmp(jc);
        var cmfValCmd      = new CmdCMFValidate(jc);
        var xsValCmd       = new CmdXSDValidate(jc);
        var xsCanonCmd     = new CmdXSDCanonicalize(jc);
        var helpCmd        = new CommandHelp(jc); 
        jc.addCommand("m2m",    cmfToCmfCmd);
        jc.addCommand("m2r",    cmfToRDFCmd);
        jc.addCommand("m2x",    cmfToXsdModelCmd);
        jc.addCommand("m2jmsg",    cmfToJSONSchemaCmd);
        jc.addCommand("m2xmsg", cmfToXMLSchemaCmd);
//        jc.addCommand("m2xs",   cmfToSrcXsdCmd);
//        jc.addCommand("m2xm",   cmfToMsgXsdCmd);
//        jc.addCommand("m2x5",   cmfToN5XsdCmd);
//        jc.addCommand("n5to6",  n5To6Cmd);
        jc.addCommand("x2m",    xsdToCmfCmd);  
//        jc.addCommand("xcanon", xsdCanon);
//        jc.addCommand("xcmp",   xsdCmpCmd);
        jc.addCommand("mval",   cmfValCmd);
        jc.addCommand("xval",   xsValCmd);
        jc.addCommand("xcanon", xsCanonCmd);
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
        var cmd = (JCCommand)objs.get(0);
        var cobuf = new JCUsageFormatter(cob);        
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
                var cmd = (JCCommand) objs.get(0);  
                var cobuf = new JCUsageFormatter(cob);        
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
