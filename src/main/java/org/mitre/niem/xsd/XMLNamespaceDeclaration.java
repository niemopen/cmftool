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

import static org.mitre.niem.cmf.NamespaceKind.namespaceKind2Code;

/**
 * A class to record a namespace declaration in a XML Schema document
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLNamespaceDeclaration implements Comparable<XMLNamespaceDeclaration>  {
    private String decPrefix;           // xmlns:decPrefix="decURI"
    private String decURI;              // xmlns:decPrefix="decURI"
    private int lineNumber;             // line number of dec in schema document
    private int elementDepth;           // dec in root element has depth 0
    private String targetNS;            // declaration appears in schema with this target namespace
    private int targetKind;             // kind of schema containg declaration
    
    public String decPrefix ()      { return decPrefix; }
    public String decURI ()         { return decURI; }
    public int lineNumber ()        { return lineNumber; }
    public int elementDepth ()      { return elementDepth; }
    public String targetNS ()       { return targetNS; }
    public int targetKind ()        { return targetKind; }
    
    public void setTargetNS (String ns)     { targetNS = ns; }
    public void setTargetKind (int k)       { targetKind = k; }
    
    public XMLNamespaceDeclaration (
            String decPrefix,
            String decURI,
            int lineNumber,
            int elementDepth,
            String targetNS,
            int targetKind) 
    {
        this.decPrefix = decPrefix;
        this.decURI = decURI;
        this.lineNumber = lineNumber;
        this.elementDepth = elementDepth;
        this.targetNS = targetNS;
        this.targetKind = targetKind;
    }

    // For sorting a list of namespace declarations into priority order.
    // Used when normalizing the declarations in a schema pile.
    @Override
    public int compareTo(XMLNamespaceDeclaration o) {
        if (this.targetKind < o.targetKind)         return -1;
        else if (this.targetKind > o.targetKind)    return 1;
        else if (null == this.targetNS)             return 0;
        else if (null == o.targetNS)                return 0;
        else if (this.targetNS.equals(o.targetNS)) {
            if (this.elementDepth - o.elementDepth < 0) return -1;
            if (this.elementDepth - o.elementDepth > 0) return 1;
            if (this.lineNumber - o.lineNumber < 0)     return -1;
            if (this.lineNumber - o.lineNumber < 0)     return -1;
        }
        return o.targetNS.compareTo(this.targetNS); // higher versions first
    }
       
    @Override
    public String toString () {
        return String.format("%-8.8s %-40.40s %4d %4d %-9s %s", 
                decPrefix, decURI, lineNumber, elementDepth, namespaceKind2Code(targetKind), (null == targetNS ? "null" : targetNS));
    }

}
