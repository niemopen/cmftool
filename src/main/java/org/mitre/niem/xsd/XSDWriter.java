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
import java.io.OutputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;

/**
 * A class to generate an output stream of readable NIEM XSD from a Document.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XSDWriter extends XMLWriter {
    
    // process string by lines to do what XSLT won't do :-(
    // For <xs:schema>, namespace decls and attributes on separate indented lines.
    // For <xs:element>, order as @ref, @minOccurs, @maxOccurs, @name, @type, @substitutionGroup, then others
    // For <xs:import>, order as @namespace, @schemaLocation, then others
    private static final Pattern linePat = Pattern.compile("^(\\s*)<([^\\s>]+)(.*)");
    private static final String[][]reorder = {
                { "xs:element", "name", "ref", "type", "minOccurs", "maxOccurs", "substitutionGroup" },
                { "xs:import", "namespace", "schemaLocation" },
                { "xs:complexType", "name", "type" },
                { "xs:attribute", "name", "ref", "type", "use" },
                { "appinfo:LocalTerm", "term" },
                { "xs:choice", "minOccurs", "maxOccurs" }
        };
    
    public XSDWriter (Document d, OutputStream s) {
        super(d, s);
    }
    
    @Override
    protected void handleLine (String line) throws IOException  {    
        if (line.isBlank()) return;
        Matcher lineM = linePat.matcher(line);
        if (!lineM.matches()) ow.write(line);
        else {
            String indent = lineM.group(1);
            String tag = lineM.group(2);
            String res = lineM.group(3);
            String end;
            res = res.stripTrailing();
            if (res.endsWith("/>")) end = "/>";
                else end = ">";
                res = res.substring(0, res.length() - end.length());
                for (int i = 0; i < reorder.length; i++) {
                    if (tag.equals(reorder[i][0])) {
                        ow.write(indent);
                        ow.write("<" + tag);
                        Map<String,String>tmap = keyValMap(res);
                        for (int j = 1; j < reorder[i].length; j++) {
                            String key = reorder[i][j];
                            if (null != tmap.get(key)) {
                                ow.write(String.format(" %s=\"%s\"", key, tmap.get(key)));
                                tmap.remove(key);
                            }
                        }
                        for (String key : tmap.keySet()) 
                            ow.write(String.format(" %s=\"%s\"", key, tmap.get(key)));                     
                        ow.write(end);
                        line = null;
                        i = reorder.length;
                    }
                }
                // Rewrite the xs:schema element to make it pretty.
                // targetNamespace comes first
                // then namespace declarations, in prefix order, except xs and xsi are last
                // then everything else, in alphabetical order
                if (null != line && "xs:schema".equals(tag)) {
                    ow.write("<xs:schema");
                    Map<String,String>tmap = keyValMap(res);
                    var tns = tmap.get("targetNamespace");
                    if (null != tns) {
                        ow.write(String.format("\n  targetNamespace=\"%s\"", tns));
                        tmap.remove("targetNamespace");
                    }
                    var xsURI  = tmap.remove("xmlns:xs");
                    var xsiURI = tmap.remove("xmlns:xsi");
                    for (Map.Entry<String,String>me : tmap.entrySet()) {
                        if (me.getKey().startsWith("xmlns:")) 
                            ow.write(String.format("\n  %s=\"%s\"", me.getKey(), me.getValue()));
                    }
                    if (null != xsURI)  ow.write("\n  xmlns:xs=\"" + xsURI + "\"");
                    if (null != xsiURI) ow.write("\n  xmlns:xsi=\"" + xsiURI + "\"");
                    for (Map.Entry<String,String>me : tmap.entrySet()) {
                        if (!me.getKey().startsWith("xmlns:")) 
                            ow.write(String.format("\n  %s=\"%s\"", me.getKey(), me.getValue()));
                    }     
                    ow.write(end);
                    line = null;
                }
                if (null != line) ow.write(line);
            }
            ow.write("\n");
        }        
   
}
