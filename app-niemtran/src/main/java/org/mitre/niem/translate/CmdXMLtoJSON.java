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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.json.Context;
import org.mitre.niem.utility.JCUsageFormatter;
import static org.mitre.niem.utility.URIfuncs.FileToCanonicalURI;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_SAX2;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
        
@Parameters(commandDescription = "convert NIEM XML message to NIEM JSON")
    
public class CmdXMLtoJSON implements JCCommand {
    
    @Parameter(names = {"-c", "--context"}, description = "generate complete @context in result")
    boolean contextF = false;
    
    @Parameter(names = {"--curi"}, description = "include \"@context\": URI in result")
    String contextU = "";
    
    @Parameter(names = {"-f","--force"}, description = "overwrite existing .json files")
    boolean force = false;
    
    @Parameter(names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;

    @Parameter(description = "model.cmf msg.xml ...")
    private List<String> mainArgs;
    
    CmdXMLtoJSON () {
    }
  
    CmdXMLtoJSON (JCommander jc) {
    }

    public static void main (String[] args) {       
        var obj = new CmdXMLtoJSON();
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
        cob.setProgramName("niemtran x2j");
        run(cob);
    }        
    
    private void run (JCommander cob) {
        if (help) {
            cob.usage();
            System.exit(0);
        }
        if (mainArgs == null || mainArgs.size() < 2) {
            cob.usage();
            System.exit(1);
        }
        
        // Check for parser config errors now
        try {
            ParserBootstrap.init(BOOTSTRAP_SAX2);
        } catch (ParserConfigurationException ex) {
            System.err.println("Parser configuration error: " + ex.getMessage());
            System.exit(1);
        }
        // Read the model object from the model instance file
        // Read the model object from the model file(s)
        var mr = new ModelXMLReader();  
        var mF = new File(mainArgs.get(0));
        var model = mr.readFiles(mF);    
        if (null == model) {
            System.err.println("Can't read model from " + mF.toString());
            System.exit(1);
        }
        
        var tran = new XMLMsgToJSON(model);
        var gson = new GsonBuilder().setPrettyPrinting().create();
            
        for (int i = 1; i < mainArgs.size(); i++) {
            var xmlFN  = mainArgs.get(i);
            var jsonFN = removeExtension(xmlFN) + ".json";
            var jsonF  = new File(jsonFN);
            if (jsonF.exists() && !force) {
                System.err.println(jsonFN + ": file exists");
                continue;
            }
            InputSource xmlIS = null;          
            try {
                var xmlF = new File(xmlFN);
                var fis  = new FileInputStream(xmlF);
                xmlIS =    new InputSource(fis);
                xmlIS.setSystemId(xmlF.toURI().toString());
            } catch (FileNotFoundException ex) {
                System.err.println(String.format("Can't open XML file %s: %s", xmlFN, ex.getMessage()));
                continue;
            }
            Writer jsonW = null;
            try {
                var jsonOS = new FileOutputStream(jsonFN);
                var jsonSW = new OutputStreamWriter(jsonOS, "UTF-8");
                jsonW = new BufferedWriter(jsonSW);
            } catch (FileNotFoundException ex) {
                System.err.println(String.format("Can't open JSON file %s: %s", jsonFN, ex.getMessage()));
                continue;
            } catch (UnsupportedEncodingException ex) {
                System.err.println("Can't write UTF-8??: " + ex.getMessage());
                System.exit(1);
            }
            var jobj = new JsonObject();
            try {
                var status = tran.convert(xmlIS, jobj);
            } catch (ParserConfigurationException ex) {
                System.err.println("Parser configuration error: " + ex.getMessage());
                System.exit(1);
            } catch (SAXException ex) {
                System.err.println(String.format("Error parsing %s: %s", xmlFN, ex.getMessage()));
            } catch (IOException ex) {
                System.err.println(String.format("Error reading %s: %s", xmlFN, ex.getMessage()));
            }     
            if (contextF) {
                var cobj = Context.create(model);
                jobj.add("@context", cobj);
            }
            else if (!contextU.isBlank()) {
                jobj.addProperty("@context", contextU);
            }
            
            var jmsg = gson.toJson(jobj);
            try {
                jsonW.write(jmsg);
                jsonW.close();
            } catch (IOException ex) {
                System.err.println(String.format("Error writing %s: %s", jsonFN, ex.getMessage()));
            }
           
        }
    }
    
}
