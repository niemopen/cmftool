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
package org.mitre.niem.translate;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.util.List;
import java.util.Map;
import org.mitre.niem.utility.JCUsageFormatter;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NIEMTran {
    
    @Parameter(names = {"-h","--help"},    description = "display this usage message", help = true)
    boolean help = false;     
    
    private List<String> mainArgs;
    
    NIEMTran () { }

    public static void main(String[] args) {
        var obj = new NIEMTran();
        obj.run(args);
    }

    public void run (String[] args) {

        var jc = new JCommander();
        var uf = new JCUsageFormatter(jc);
        jc.setUsageFormatter(uf);
        jc.setProgramName("niemtran");

        var xml2jsonCmd    = new CmdXMLtoJSON(jc);
        var helpCmd        = new CommandHelp(jc); 
        
        jc.addCommand("x2j", xml2jsonCmd);
        jc.addCommand("help", helpCmd);

        if (args.length < 1) {
            System.out.println("Version: " + NIEMTran.class.getPackage().getImplementationVersion());
            jc.usage();
            System.exit(2);
        }
        try {
            jc.parse(args);
        }
        catch (Exception ex) {
            System.out.println("Version: " + NIEMTran.class.getPackage().getImplementationVersion());
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
            System.out.println("Version: " + NIEMTran.class.getPackage().getImplementationVersion());
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
