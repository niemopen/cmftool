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

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A class to generate an output stream of lovely XSD from a Document object.
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
    protected static final String[][]reorder = {
                { "xs:element", "name", "ref", "type", "minOccurs", "maxOccurs", "substitutionGroup" },
                { "xs:import", "namespace", "schemaLocation" },
                { "xs:complexType", "name", "type" },
                { "xs:attribute", "name", "ref", "type", "use" },
                { "xs:choice", "minOccurs", "maxOccurs" },
                { "appinfo:LocalTerm", "term" }     // different prefix? too bad.
        };
    
    public XSDWriter () { }
    
    @Override
    protected void handleOtherLines (String line, Writer w) throws IOException  {    
        var lineM = linePat.matcher(line);
        if (!lineM.matches()) { 
            w.write(line + "\n");
            return;
        }
        var indent = lineM.group(1);
        var tag    = lineM.group(2);
        var res    = lineM.group(3);
        var end    = ">";
        res = res.stripTrailing();
        if (res.endsWith("/>")) end = "/>";
        res = res.substring(0, res.length() - end.length());
        
        for (int i = 0; i < reorder.length; i++) {
            if (tag.equals(reorder[i][0])) {
                w.write(indent);
                w.write("<" + tag);
                var tmap = keyValMap(res);
                for (int j = 1; j < reorder[i].length; j++) {
                    var key = reorder[i][j];
                    if (null != tmap.get(key)) {
                        w.write(String.format(" %s=\"%s\"", key, tmap.get(key)));
                        tmap.remove(key);
                    }
                }
                for (var key : tmap.keySet()) 
                    w.write(String.format(" %s=\"%s\"", key, tmap.get(key)));
                
                w.write(end + "\n");
                line = null;            // this line was reordered
                i = reorder.length;
            }
        }
        // Write unchanged line if not reordered
        if (null != line) w.write(line + "\n");
    }

    // Rewrite the xs:schema element to make it pretty.
    // targetNamespace comes first
    // then namespace declarations, in prefix order, except xs and xsi are last
    // then everything else, in alphabetical order    
    @Override
    protected void handleFirstLine (String line, Writer w) throws IOException {
        var lineM = linePat.matcher(line);
        if (!lineM.matches()) { 
            w.write(line + "\n");
            return;
        }
        var indent = lineM.group(1);
        var tag    = lineM.group(2);
        var res    = lineM.group(3);
        var end    = ">";
        res = res.stripTrailing();
        if (res.endsWith("/>")) end = "/>";
        res = res.substring(0, res.length() - end.length());
        
        w.write("<xs:schema");
        var tmap = keyValMap(res);
        var tns = tmap.get("targetNamespace");
        if (null != tns) {
            w.write(String.format("\n  targetNamespace=\"%s\"", tns));
            tmap.remove("targetNamespace");
        }
        var xsURI = tmap.remove("xmlns:xs");
        var xsiURI = tmap.remove("xmlns:xsi");
        for (var me : tmap.entrySet()) {
            if (me.getKey().startsWith("xmlns:")) {
                w.write(String.format("\n  %s=\"%s\"", me.getKey(), me.getValue()));
            }
        }
        if (null != xsURI) {
            w.write("\n  xmlns:xs=\"" + xsURI + "\"");
        }
        if (null != xsiURI) {
            w.write("\n  xmlns:xsi=\"" + xsiURI + "\"");
        }
        for (Map.Entry<String, String> me : tmap.entrySet()) {
            if (!me.getKey().startsWith("xmlns:")) {
                w.write(String.format("\n  %s=\"%s\"", me.getKey(), me.getValue()));
            }
        }
        w.write(end + "\n");
     }

}
