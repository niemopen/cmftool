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
import java.util.Set;
import org.mitre.niem.xml.LanguageString;

/**
 * A class for a ChildPropertyAssociation object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class PropertyAssociation extends CMFObject implements Comparable<PropertyAssociation> {
    
    public PropertyAssociation () { }
    
    private Property property = null;                               // cmf:Property
    private String minOccurs = "1";                                 // cmf:MinOccursQuantity
    private String maxOccurs = "1";                                 // cmf:MaxOccursQuantity
    private final List<LanguageString> docL = new ArrayList<>();    // cmf:DocumentationText
    
    public Property property ()         { return property; }
    public String minOccurs ()          { return minOccurs; }
    public String maxOccurs ()          { return maxOccurs; }
    public List<LanguageString> docL () { return docL; }
    
    public boolean isMaxUnbounded ()    { return "unbounded".equals(maxOccurs); }
    public int minOccursVal ()          { return stringToInt(minOccurs); }
    public int maxOccursVal () { 
        return "unbounded".equals(maxOccurs) ? -1 : stringToInt(maxOccurs);
    }
    
    public ClassType classType ()       { return null; }
    public String index ()              { return ""; }
    public Set<String> codeS ()         { return Set.of(); }    
    
    public void setProperty (Property p)    { property = p; }
    public void setMinOccurs (String s)     { minOccurs = s; }
    public void setMaxOccurs (String s)     { maxOccurs = s; }
    
    public void addDocumentation (String doc, String lang) {
        docL.add(new LanguageString(doc, lang));
    }
    public void setDocumentation (List<LanguageString> dL) {
        docL.clear();
        docL.addAll(dL);
    }   
    
    public int stringToInt (String s) {
        int res = 0;
        try { res = Integer.parseInt(s); }
        catch (Exception ex) { }
        return res;
    }
    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        return child.addToPropertyAssociation(eln, loc, this);
    }    
    
    @Override
    public boolean addToClassType (String eln, String loc, ClassType c) {
        c.addPropertyAssociation(this);
        return true;
    }
    
    @Override
    public int compareTo(PropertyAssociation o) {
        int rv = 0;
        if (null != this.classType() && null != o.classType()) rv = this.classType().compareTo(o.classType());
        if (0 == rv) {
            var tx = "0" + this.index();
            var ox = "0" + o.index();
            try {
                var ti = Integer.parseInt(tx);
                var oi = Integer.parseInt(ox);
                rv = ti - oi;
            }
            catch(NumberFormatException ex) { } // IGNORE
        }
        if (0 == rv) rv = this.property().compareTo(o.property());
        return rv;}    
}
