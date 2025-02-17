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
import org.apache.commons.lang3.StringUtils;
import org.mitre.niem.xml.LanguageString;

/**
 * A class for a Facet object in a CMF model.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Facet extends CMFObject {
    
    public Facet () { }
    
    private String category = "";                                   // cmf:FacetCategoryCode
    private String value = "";                                      // cmf:FacetValue
    private final List<LanguageString> docL = new ArrayList<>();    // cmf:DocumentationText
    
    public String category ()                       { return category; }
    public String value ()                          { return value; }
    public List<LanguageString> docL ()             { return docL; }
    public String xsdFacetName ()                   { return StringUtils.uncapitalize(category); }
    
    public void setCategory (String k)              { category = k; }
    public void setValue (String v)                 { value = v; }
    
    public void addDocumentation (String doc, String lang) {
        docL.add(new LanguageString(doc, lang));
    }

    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToFacet(eln, loc, this);
    }
    
    @Override
    public boolean addToRestriction (String eln, String loc, Restriction r) {
        r.addFacet(this);
        return true;
    }

}
