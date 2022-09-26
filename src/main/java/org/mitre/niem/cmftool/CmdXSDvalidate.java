/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2022 The MITRE Corporation.
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.xsd.SAXErrorHandler;
import org.mitre.niem.xsd.XMLSchema;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Parameters(commandDescription = "validate XML documents")

class CmdXSDvalidate implements JCCommand {
   
    @Parameter(names = {"-d","--debug"}, description = "turn on debug logging")
    private boolean debugFlag = false;
     
    @Parameter(names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
        
    @Parameter(description = "--schema file... -file doc...")
    private List<String> mainArgs;
    
    CmdXSDvalidate () {
    }
  
    CmdXSDvalidate (JCommander jc) {
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

        // Divide arguments into schema and document
        List<String> schemaArgs = new ArrayList<>();
        List<String> documents  = new ArrayList<>();
        List<String> unknownYet = new ArrayList<>();
        enum Akind { SCHEMA, DOC, UNKNOWN };
        Akind lookingAt = Akind.UNKNOWN;
        for (String arg : mainArgs) {
            switch (arg) {
                case "-s":
                case "--schema": lookingAt = Akind.SCHEMA; schemaArgs.addAll(unknownYet); break;
                case "-f":
                case "--file":   lookingAt = Akind.DOC; documents.addAll(unknownYet); break;
                default: {
                    switch (lookingAt) {
                        case SCHEMA: schemaArgs.add(arg); break;
                        case DOC:    documents.add(arg); break;
                        default:     unknownYet.add(arg); break;
                    }
                }
            }
        }
        if (Akind.UNKNOWN == lookingAt) {
            schemaArgs.addAll(unknownYet);
            if (schemaArgs.size() > 1) documents.add(schemaArgs.remove(0));          
        }
        if (schemaArgs.isEmpty()) {
            cob.usage();
            System.exit(1);
        }
        // Assemble the javax schema, die on errors and warnings
        String[] args = schemaArgs.toArray(new String[0]);
        XMLSchema xmls;
        Schema vals;
        Validator validator = null;
        try {
            xmls = new XMLSchema(args);
            vals = xmls.javaxSchema();
            List<String> res = xmls.javaXMsgs();
            if (!res.isEmpty()) {
                for (String msg : res) System.out.println(msg);
                System.exit(1);
            }
            validator = vals.newValidator();
        } catch (XMLSchema.XMLSchemaException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        } catch (SAXException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
        // Validate documents, if any
        String indent = documents.size() > 1 ? "  ": "";
        for (String doc : documents) {
            SAXErrorHandler h = new SAXErrorHandler();
            Source s = new StreamSource(doc);
            if (!indent.isEmpty()) System.out.println(doc + ":");
            validator.setErrorHandler(h);
            try {
                validator.validate(s);
                for (String msg : h.messages()) {
                    System.out.println(indent + msg);
                }
            } catch (SAXException | IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
        
        System.exit(0);
    }    
}
