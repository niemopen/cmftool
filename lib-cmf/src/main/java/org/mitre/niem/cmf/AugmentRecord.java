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

import java.util.HashSet;
import java.util.Set;

/**
 * A class for an AugmentationRecord object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class AugmentRecord extends PropertyAssociation implements Comparable<AugmentRecord> {
    
    public AugmentRecord () { }
    public AugmentRecord (PropertyAssociation cpa) {
        this.setProperty(cpa.property());
        this.setMaxOccurs(cpa.maxOccurs());
        this.setMinOccurs(cpa.minOccurs());
        this.setIsOrdered(cpa.isOrdered());
        this.setDocumentation(cpa.docL());
    }
    
    private ClassType classType = null;                 // cmf:Class
    private String index = "";                          // cmf:AugmentationIndex
    private Set<String> codeS = new HashSet<>();        // cmf:GlobalClassCode
    
    public ClassType classType ()                   { return classType; }
    public String index ()                          { return index; }
    public Set<String> codeS ()                     { return codeS; }
    
    public void setClassType (ClassType ct)         { classType = ct; }
    public void setIndex (String s)                 { index = s; }
    public void addCode (String s)                  { codeS.add(s); }
    public void removeCode (String s)               { codeS.remove(s); }
    public void clearCodes ()                       { codeS.clear(); }
        
    
    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToAugmentRecord(eln, loc, this);
    }
    
    @Override
    public boolean addToNamespace (String eln, String loc, Namespace ns) {
        ns.addAugmentRecord(this);
        return true;
    }

    @Override
    public int compareTo(AugmentRecord o) {
        int rv = 0;
        if (null != this.classType && null != o.classType) rv = this.classType.compareTo(o.classType);
        if (0 == rv) rv = this.property().qname().compareTo(o.property().qname());
        return rv;}
}
