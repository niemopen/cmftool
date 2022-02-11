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
package org.mitre.niem.xsd;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Scanner;
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
import org.w3c.dom.Document;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XSDWriter {
    
    // Writes the XSD document model.  Post-processing of XSLT output to do
    // what XSLT should do, but doesn't.  You can't process arbitrary XML in
    // this way, but we know what the XSLT output of a NIEM conforming
    // schema document is going to be , so it works.
    public static void writeDOM (Document dom, Writer w) 
            throws TransformerConfigurationException, TransformerException, IOException {
        
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter ostr = new StringWriter();
        tr.transform(new DOMSource(dom), new StreamResult(ostr));
        
        // process string by lines to do what XSLT won't do :-(
        // For <xs:schema>, namespace decls and attributes on separate indented lines.
        // For <xs:element>, order as @ref, @minOccurs, @maxOccurs, @name, @type, @substitutionGroup, then others
        // For <xs:import>, order as @namespace, @schemaLocation, then others
        Pattern linePat = Pattern.compile("^(\\s*)<([^\\s>]+)(.*)");
        String[][]reorder = {
                { "xs:element", "name", "ref", "type", "minOccurs", "maxOccurs", "substitutionGroup" },
                { "xs:import", "namespace", "schemaLocation" },
                { "xs:complexType", "name", "type" },
                { "xs:attribute", "name", "type" }
        };
        Scanner scn = new Scanner(ostr.toString());
        String line = scn.nextLine();
        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");    // don't want/need standalone="no"
        while (scn.hasNextLine()) {           
            line = scn.nextLine();
            if (line.isBlank()) continue;
            Matcher lineM = linePat.matcher(line);
//            System.out.print(String.format("line:   '%s'\n", line));
            if (!lineM.matches()) w.write(line);
            else {
                String indent = lineM.group(1);
                String tag = lineM.group(2);
                String res = lineM.group(3);
                String end;
                res = res.stripTrailing();
                if (res.endsWith("/>")) end = "/>";
                else end = ">";
                res = res.substring(0, res.length() - end.length());
//                System.out.print(String.format("indent: '%s'\n", indent));
//                System.out.print(String.format("tag:    '%s'\n", tag));
//                System.out.print(String.format("rest:   '%s'\n", res));
//                System.out.print(String.format("end:    '%s'\n", end));
                for (int i = 0; i < reorder.length; i++) {
                    if (tag.equals(reorder[i][0])) {
                        w.write(indent);
                        w.write("<" + tag);
                        Map<String,String>tmap = keyValMap(res);
                        for (int j = 1; j < reorder[i].length; j++) {
                            String key = reorder[i][j];
                            if (null != tmap.get(key)) {
                                w.write(" " + tmap.get(key));
                                tmap.remove(key);
                            }
                        }
                        for (String str : tmap.values()) w.write(" " + str);
                        w.write(end);
                        line = null;
                        i = reorder.length;
                    }
                }
                if (null != line && "xs:schema".equals(tag)) {
                    w.write("<xs:schema");
                    Map<String,String>tmap = keyValMap(res);
                    if (null != tmap.get("targetNamespace")) {
                        w.write("\n  " + tmap.get("targetNamespace"));
                        tmap.remove("targetNamespace");
                    }
                    for (Map.Entry<String,String>me : tmap.entrySet()) {
                        if (me.getKey().startsWith("xmlns:")) w.write("\n  " + me.getValue());
                    }
                    for (Map.Entry<String,String>me : tmap.entrySet()) {
                        if (!me.getKey().startsWith("xmlns:")) w.write("\n  " + me.getValue());
                    }     
                    w.write(end);
                    line = null;
                }
                if (null != line) w.write(line);
            }
            w.write("\n");
        }        
    }
    
    private static final Pattern pairPat = Pattern.compile("\\s*([\\w:-]+)\\s*=\\s*(\"[^\"]*\")");
    // Breaks a string of key="value" pairs into a sorted map
    private static TreeMap<String,String> keyValMap (String s) {
        TreeMap<String,String> kvm = new TreeMap<>();
        Matcher pairMatch = pairPat.matcher(s);
        while (pairMatch.lookingAt()) {
            String key = pairMatch.group(1);
            String val = pairMatch.group(2);
            int ei = pairMatch.end();
            pairMatch.region(ei, pairMatch.regionEnd());
            kvm.put(key, key + "=" + val);
        }
        return kvm;
    }    
}
