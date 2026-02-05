/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.utility.JCUsageFormatter;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Parameters(commandDescription = "convert NIEM JSON message to RDF")

public class CmdJSONtoRDF implements JCCommand {
    
    @Parameter(names = {"-c", "--context"}, description = "use context from this JSON file")
    File contextF = null;
    
    @Parameter(names = {"-m", "--model"}, description = "use message model in this CMF file")
    File modelF = null;
    
    @Parameter(names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;

    @Parameter(description = "message.json")
    private List<File> msgFL = null;
    
    CmdJSONtoRDF () {
    }
  
    CmdJSONtoRDF (JCommander jc) {
    }

    public static void main (String[] args) {       
        var obj = new CmdJSONtoRDF();
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
        cob.setProgramName("niemtran j2r");
        run(cob);
    }      
    
    private void run (JCommander cob) {
        if (help) {
            cob.usage();
            System.exit(0);
        }
        if (msgFL.size() < 1) {
            cob.usage();
            System.exit(0);
        }
        // Read context if provided
        String contextS = null;
        if (null != contextF) {
            try {
                contextS = Files.readString(contextF.toPath(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                System.err.println(String.format("Can't read context file %s: %s",
                    contextF.toString(), ex.getMessage()));
                System.exit(1);
            }
        }
        // Read model if provided
        Model model = null;
        if (null != modelF) {
            var mr = new ModelXMLReader();  
            model = mr.readFiles(modelF);    
            if (null == model) {
                System.err.println("Can't read model from " + modelF.toString());
                System.exit(1);
            }            
        }
        // Read message
        String msgS = null;
        var msgF = msgFL.get(0);
        try {
            msgS = Files.readString(msgF.toPath(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println(String.format("Can't read context file %s: %s",
                msgF.toString(), ex.getMessage()));
            System.exit(1);
        }
        // Convert and write to output
        var ow  = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        var cvt = new JSONMsgToRDF();
        try {
            cvt.setContext(contextS);
            cvt.setModel(model);            
            cvt.convert(msgS, ow);
            ow.close();
        } catch (IOException ex) {
            System.err.println("IO error writing RDF output: " + ex.getMessage());
            System.exit(1);
        } catch (NIEMTranException ex) {
            System.err.println("Conversion error: " + ex.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }    
}
