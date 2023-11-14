/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2023 The MITRE Corporation.
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
package org.mitre.niem.xsd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
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
import org.w3c.dom.Document;

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
    
    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(XMLWriter.class);    
    
    protected final Document dom;
    protected final OutputStream os;
    protected BufferedWriter ow = null;
    private boolean rootLine = true;
    
    XMLWriter(Document dom, OutputStream os) {
        this.dom = dom;
        this.os = os;
    }
    
    public void writeXML () throws IOException {
          
        // Make sure the output encoding will be UTF-8
        try {
            var osw = new OutputStreamWriter(os, "UTF-8");
            ow = new BufferedWriter(osw);
        } catch (UnsupportedEncodingException ex) {     // SHOULDN'T HAPPEN
            LOG.error("can't write UTF-8 to output stream: " + ex.getMessage());
            return;
        }
        // Generate XML text from the document
        StringWriter ostr = new StringWriter();
        Transformer tr = null;
        try {
            tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            tr.transform(new DOMSource(dom), new StreamResult(ostr));      
        } catch (TransformerConfigurationException ex) {
            LOG.error("can't configure Transformer: " + ex.getMessage());
            return;
        } catch (TransformerException ex) {
            LOG.error("DOM transformation error: " + ex.getMessage());
            return;
        }
        // First line in transformer output is the XML prolog.
        // We don't want or need the standalone="no" in the prolog.
        var br = new BufferedReader(new StringReader(ostr.toString()));
        var ln = br.readLine();
        ow.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        
        ln = br.readLine();
        do {
            handleLine(ln);
            ln = br.readLine();
        } while (null != ln);
        ow.flush();
    }    
    // Second line in the transformer output is the root element.
    // Rewrite to have namespace declarations and attributes on separate lines.
    // You can't handle arbitrary XML with regexes, but it works here.
    private static Pattern tagPat = Pattern.compile("^\\s*<\\S+\\s*");
    protected void handleLine (String ln) throws IOException {
        if (!rootLine) { 
            ow.write(ln);
            ow.write("\n");
        }
        else {
            var m = tagPat.matcher(ln);
            if (m.lookingAt()) {
                var tag = m.group(0);
                var rest = ln.substring(tag.length());
                var amap = keyValMap(rest);
                ow.write(tag);
                for (Map.Entry<String,String>me : amap.entrySet()) {
                    var key = me.getKey();
                    if ("xmlns".equals(key) || key.startsWith("xmlns:"))
                        ow.write(String.format("\n  %s=\"%s\"", me.getKey(), me.getValue()));
                }                
                for (Map.Entry<String,String>me : amap.entrySet()) {
                    var key = me.getKey();
                    if (!"xmlns".equals(key) && !key.startsWith("xmlns:"))
                        ow.write(String.format("\n  %s=\"%s\"", me.getKey(), me.getValue()));
                }
                ow.write(">\n");
                int i = 0;
                rootLine = false;
            }
        }
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
}
