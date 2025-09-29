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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import static org.apache.commons.io.FilenameUtils.getPath;
import static org.apache.commons.io.FilenameUtils.normalize;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.utility.JCUsageFormatter;
import org.mitre.niem.utility.ResourceManager;
import static org.mitre.niem.utility.URIfuncs.URIStringToFile;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
@Parameters(commandDescription = "validate a CMF model file")

class CmdCMFValidate implements JCCommand {
    
    private ResourceManager rmgr = new ResourceManager(Model.class);
    
    @Parameter(names = {"-h","--help"}, description = "display this usage message", help = true)
    boolean help = false;
    
    @Parameter(description = "model.cmf ...")
    private List<String> mainArgs;
    
    CmdCMFValidate () { }
  
    CmdCMFValidate (JCommander jc) { }

    public static void main (String[] args) {       
        var obj = new CmdCMFValidate();
        obj.runMain(args);
    }
    
    @Override
    public void runMain (String[] args) {
        var jc = new JCommander(this);
        var uf = new JCUsageFormatter(jc); 
        jc.setUsageFormatter(uf);
        jc.setProgramName("cmfvalidate");
        jc.parse(args);
        run(jc);
    }
    
    @Override
    public void runCommand (JCommander cob) {
        cob.setProgramName("cmftool mval");
        run(cob);
    }    
    
    private void run (JCommander cob) {
        
        if (help) {
            cob.usage();
            System.exit(0);
        }        
        var sfact = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        var hndlr = new Handler();
        var resv  = new ResourceResolver();
        sfact.setErrorHandler(hndlr);
        sfact.setResourceResolver(resv);
        
        InputStream cmfIS = null;
        try {
            cmfIS = rmgr.getResourceStream("/xsd/cmf/cmf.xsd");
        } catch (IOException ex) {
            System.err.println("Can't get cmf.xsd: " + ex.getMessage());
            System.exit(1);
        }
        var cmfSS  = new StreamSource(cmfIS);
        cmfSS.setSystemId("/xsd/cmf/cmf.xsd");
        
        Schema s = null;
        try {
            s = sfact.newSchema(cmfSS);
        } catch (SAXException ex) {
            System.err.println("Can't create CMF schema: " + ex.getMessage());
            System.exit(1);
        }
        var msgs = hndlr.messages();
        if (!msgs.isEmpty()) {
            System.err.println(msgs);
            System.exit(1);
        }
      
        var val = s.newValidator();
        val.setErrorHandler(hndlr);
        for (var cmfN : mainArgs) {
            System.out.print(cmfN + ": ");
            var cmfF = new File(cmfN);
            var cmfS = new StreamSource(cmfF);
            try {
                val.validate(cmfS);
            } catch (SAXException ex) {
                System.out.println("  SAX exception: " + ex.getMessage());
            } catch (IOException ex) {
                System.out.println("  IO exception: " + ex.getMessage());            
            }
            msgs = hndlr.messages();
            if (msgs.isEmpty()) System.out.println("OK");
            else System.out.println("\n" + msgs);
        }
        
    }
    
    private class Handler extends DefaultHandler {
        private StringBuilder msgs = new StringBuilder();
        @Override
        public void error (SAXParseException e) {
            addMessage("ERROR", e);
        }
        @Override
        public void fatalError (SAXParseException e) {
            addMessage("FATAL", e);
        }
        @Override
        public void warning (SAXParseException e) {
            addMessage("WARN", e);
        }
        private void addMessage(String label, SAXParseException e) {
            var xmlF = URIStringToFile(e.getSystemId());
            msgs.append(String.format("[%s] %s:%d: %s\n", 
                label, xmlF.getName(), e.getLineNumber(), e.getMessage()));
        }
        public String messages () { return msgs.toString(); }
        public void clear () { msgs = new StringBuilder(); }
    }
    
    private class ResourceResolver implements LSResourceResolver {
        
        private static DOMImplementationLS domLSI = null;
        
        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            
            var base = baseURI.replaceFirst("file://", "");
            var path = getPath(base);
            var sid  = path + systemId;
            var nsid = "/" + normalize(sid, true);            
            
            InputStream is = null;
            LSInput lsi = null;
            try {
                is = rmgr.getResourceStream(nsid);
            } catch (Exception ex) {
                System.err.println("Can't get " + systemId + ": " + ex.getMessage());
                return null;
            }
            try {
                lsi = createLSInput();
            } catch (Exception ex) {
                System.err.println("Can't create LSInput: " + ex.getMessage());
                return null;
            }
            lsi.setByteStream(is);
            lsi.setSystemId(nsid);
            return lsi;
        }
        
        public static LSInput createLSInput() throws Exception {
            if (null == domLSI) {
                domLSI = (DOMImplementationLS) DOMImplementationRegistry.newInstance()
                    .getDOMImplementation("LS");
            }
            return domLSI.createLSInput();
        }

        private LSInput createLSInput(InputStream is) {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
        
    }
 
    
}
