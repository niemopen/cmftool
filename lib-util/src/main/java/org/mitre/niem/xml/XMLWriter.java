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
package org.mitre.niem.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A class to generate an output stream of readable XML from a Document.  
 * Namespace declarations and attributes of the root element appear in order
 * (namespaces, then attributes) on separate indented lines.  Much easier to
 * read than a single very long line.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>;llll8
 */
public class XMLWriter {
    
    static final Logger LOG = LogManager.getLogger(XMLWriter.class);    

    public XMLWriter () { }
    
    /**
     * Write the Document to the output writer.
     * @throws IOException 
     */
    public void writeXML (Document dom, Writer w) throws IOException {

        // Generate XML text from the document
        var ostr      = new StringWriter();
        var transFact = ParserBootstrap.transFactory();
        Transformer trans = null;
        try {
            dom.getDocumentElement().normalize();
            trans = transFact.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty(OutputKeys.METHOD, "xml");
            trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            trans.transform(new DOMSource(dom), new StreamResult(ostr));      
        } catch (TransformerConfigurationException ex) {
            LOG.error("can't configure Transformer: " + ex.getMessage());
            return;
        } catch (TransformerException ex) {
            LOG.error("DOM transformation error: " + ex.getMessage());
            return;
        }
        //Write our own XML declaration, without @standalone
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        
        // If the input document begins with a comment, then there may be
        // no newline between the --> and the start of the first element.
        // Why is that? No fscking idea. Also sometimes the Transformer
        // double-spaces the XML output, sometimes it doesn't.
        var br = new BufferedReader(new StringReader(ostr.toString()));        
        var ln = br.readLine();
        while (null != ln) {
            var m = commentTagPat.matcher(ln);
            if (m.find()) {
                var s = ln.substring(0, m.end()-2);
                ln = ln.substring(m.end()-2);
                w.write(s);
                w.write("\n");
                break;
            }
            m = tagPat.matcher(ln);
            if (m.lookingAt()) break;
            w.write(ln);
            w.write("\n");
            ln = br.readLine();
        }
        // Now we should have the line with the document element
        handleFirstLine(ln, w);
        
        while (null != (ln = br.readLine())) {
            if (ln.isBlank()) continue;
            handleOtherLines(ln, w);
        }
    }
    
    private static Pattern commentTagPat = Pattern.compile("-->\\s*<\\w");
    private static Pattern tagPat = Pattern.compile("^\\s*<\\w\\S+\\s*");
    
    // First line after the XML declaration and comments is the root element.
    // Rewrite to have namespace declarations and attributes on separate lines.
    // You can't handle arbitrary XML with regexes, but it works here.
    protected void handleFirstLine (String ln, Writer w) throws IOException {
        var m = tagPat.matcher(ln);
        if (m.lookingAt()) {
            var tag = m.group(0);
            var rest = ln.substring(tag.length());
            var amap = keyValMap(rest);
            w.write(tag);
            
            // First write all the namespace declarations
            for (var me : amap.entrySet()) {
                var key = me.getKey();
                if ("xmlns".equals(key) || key.startsWith("xmlns:")) {
                    w.write(String.format("\n  %s=\"%s\"", me.getKey(), me.getValue()));
                }
            }
            // Then write all the attributes
            for (var me : amap.entrySet()) {
                var key = me.getKey();
                if (!"xmlns".equals(key) && !key.startsWith("xmlns:")) {
                    w.write(String.format("\n  %s=\"%s\"", me.getKey(), me.getValue()));
                }
            }
            w.write(">\n");
        }
    }
    
    protected void handleOtherLines (String ln, Writer w) throws IOException {
        w.write(ln + "\n");
    }
    
    // Breaks a string of key="value" pairs into a sorted map
    private static Pattern attributePat = Pattern.compile("\\s*(\\S+)\\s*=\\s*\"([^\"]*)\"");
    protected static TreeMap<String,String> keyValMap (String s) {
        TreeMap<String,String> kvm = new TreeMap<>();
        Matcher m = attributePat.matcher(s);
        while (m.find()) {
            kvm.put(m.group(1), m.group(2));
        }
        return kvm;
    }   
    
    /**
     * Transforms a DOM node to its text representation.
     * @param n - Node object
     * @return text representation
     */
    public static String nodeToText (Node n) {
        var os = new StringWriter();
        try {
            var tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            tr.setOutputProperty(OutputKeys.INDENT, "no");
            tr.transform(new DOMSource(n), new StreamResult(os));
            return os.toString();
        } catch (Exception ex) { } // IGNORE
        return "";
    }
}
