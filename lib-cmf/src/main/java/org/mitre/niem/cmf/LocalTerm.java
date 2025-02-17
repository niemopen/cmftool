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
package org.mitre.niem.cmf;

import java.util.ArrayList;
import java.util.List;
import org.mitre.niem.xml.LanguageString;

/**
 * A class for a LocalTerm object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class LocalTerm extends CMFObject {
    
    public LocalTerm () { }
    
    private String term = "";                                           // cmf:TermName
    private String literal = "";                                        // cmf:TermLiteralText
    private final List<LanguageString> docL = new ArrayList<>();        // cmf:DocumentationText
    private final List<String> sourceL = new ArrayList<>();             // cmf:SourceURI
    private final List<LanguageString> citationL = new ArrayList<>();   // cmf:SourceCitationText

    public String term ()                           { return term; }
    public String literal ()                        { return literal; }
    public List<LanguageString> docL ()             { return docL; }
    public List<String> sourceL ()                  { return sourceL; }
    public List<LanguageString> citationL ()        { return citationL; }
    
    public void setTerm (String s)                  { term = s; }
    public void setLiteral (String s)               { literal = s; }
    public void addDocumentation (String s, String l) {
        docL.add(new LanguageString(s, l));
    }
    public void addSource (String s)                { sourceL.add(s); }
    public void addCitation (String s, String l) {
        citationL.add(new LanguageString(s, l));
    }
    
    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToLocalTerm(eln, loc, this);
    }
    
    @Override
    public boolean addToNamespace (String eln, String loc, Namespace ns) {
        ns.addLocalTerm(this);
        return true;
    }
        
}
