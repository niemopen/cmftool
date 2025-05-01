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

/**
 * A class to record a namespace declaration in a XML Schema document.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLNamespaceDeclaration implements Comparable<XMLNamespaceDeclaration> {
    private final String decPrefix;           // xmlns:decPrefix="decURI"
    private final String decURI;              // xmlns:decPrefix="decURI"
    private final int line;                   // decl appears on this line number
    private final int elementDepth;           // decl in root element has depth 0
    
    public String prefix ()         { return decPrefix; }
    public String ns ()             { return decURI; }
    public int line ()              { return line; }
    public int depth ()             { return elementDepth; }
    
    public XMLNamespaceDeclaration (
            String decPrefix,
            String decURI,
            int lineNum,
            int elementDepth) 
    {
        this.decPrefix = decPrefix;
        this.decURI = decURI;
        this.line = lineNum;
        this.elementDepth = elementDepth;
    }

    @Override
    public int compareTo(XMLNamespaceDeclaration o) {
        if (this.elementDepth < o.elementDepth) return -1;
        else if(this.elementDepth > o.elementDepth) return 1;
        else return this.line - o.line;
    }

}
