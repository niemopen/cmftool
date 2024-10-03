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
package org.mitre.niem.cmf;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for a local term definition within a namespace.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class LocalTerm extends ObjectType implements Comparable<LocalTerm> {
    private String term = null;                                     // local term token
    private String definition = null;                               // definition text
    private String literal = null;                                  // literal expansion of term
    private final List<String> sourceURIs = new ArrayList<>();      // list of source documents URIs
    private final List<String> citationList =  new ArrayList<>();   // list of source citation text
    
    public String getTerm()                 { return term; }
    public String getDefinition()           { return definition; }
    public String getLiteral()              { return literal; }
    public List<String> sourceURIs()        { return sourceURIs; }
    public List<String> citationList()      { return citationList; }
    
    public void setTerm(String x)           { term = x; }
    public void setDefinition(String x)     { definition = x; }
    public void setLiteral(String x)        { literal = x; }
    
    public void addSourceURI(String x)      { sourceURIs.add(x); }
    public void addCitation(String x)       { citationList.add(x); }
    
    public LocalTerm () { }
    
    public LocalTerm (String t, String d, String l, String s) {
        term = t;
        definition = d;
        literal = l;
    }
    
    public String getSourceURIs () {
        return String.join(" ", sourceURIs);
    }
    
    public void setSourceURIs (String s) {
        sourceURIs.clear();
        var uris = s.split("\\s+");
        for (var u : uris) sourceURIs.add(u);
    }
    
    @Override
    public int compareTo (LocalTerm o) {
        return this.term.compareToIgnoreCase(o.term);
    }
}
