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
 * A class for a CodeListBinding object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class CodeListBinding extends CMFObject {
    
    public CodeListBinding () { }
    public CodeListBinding (String u, String col, boolean consFlag) {
        codeListURI = u;
        column = col;
        isConstraining = consFlag;
    }
    
    private String codeListURI = "";                    // cmf:CodeListURI
    private String column = "";                         // cmf:CodeListColumnName
    private boolean isConstraining = false;             // cmf:CodeListConstrainingIndicator
    
    public String codeListURI ()                { return codeListURI; }
    public String column ()                     { return column; }
    public boolean isConstraining ()            { return isConstraining; }
    
    public void setCodeListURI (String u)       { codeListURI = u; }
    public void setColumn (String s)            { column = s; }
    public void setIsConstraining (boolean f)   { isConstraining = f; }    

    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToCodeListBinding(eln, loc, this);
    }
    
    @Override
    public boolean addToRestriction (String eln, String loc, Restriction r) {
        r.setCodeListBinding(this);
        return true;
    }
    
}
