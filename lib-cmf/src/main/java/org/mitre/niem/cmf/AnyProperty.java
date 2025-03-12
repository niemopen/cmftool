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

/**
 * A class for a property wildcard.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class AnyProperty extends CMFObject {
    
    public AnyProperty () { }
    
    @Override
    public int getType ()           { return CMF_ANYPROP; }
    @Override
    public String cmfElement ()     { return "AnyProperty"; }

    private String minOccurs = "1";
    private String maxOccurs = "1";
    private boolean isAttribute = false;    // true for xs:anyAttribute
    private String nsConstraint = "";       // eg. "##other", "http://someNS/ http://otherNS"
    private String procCode = "";           // LAX, SKIP, STRICT
    
    public String minOccurs ()              { return minOccurs; }
    public String maxOccurs ()              { return maxOccurs; }
    public boolean isAttribute ()           { return isAttribute; }
    public String nsConstraint ()           { return nsConstraint; }
    public String processCode ()            { return procCode; }
    
    public void setMinOccurs (String s)     { minOccurs = s; }
    public void setMaxOccurs (String s)     { maxOccurs = s; }
    public void setIsAttribute (boolean f)  { isAttribute = f; }
    public void setNsConstraint (String s)  { nsConstraint = s; }
    public void setProcessCode (String c)   { procCode = c; }    

    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToAnyProperty(eln, loc, this);
    }
        
    @Override
    public boolean addToClassType (String eln, String loc, ClassType c) {
        c.addAnyProperty(this);
        return true;
    }
    
}
