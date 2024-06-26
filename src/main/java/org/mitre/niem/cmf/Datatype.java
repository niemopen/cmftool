/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2021 The MITRE Corporation.
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
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Datatype extends Component {   
    private RestrictionOf restrictionOf = null;
    private UnionOf unionOf = null;
    private Datatype listOf = null;
    private CodeListBinding clb = null;
    
    public Datatype () { super(); type = C_DATATYPE; }    
    
    public Datatype (Namespace ns, String lname) {
        super(ns, lname);
        type = C_DATATYPE;
    }
    
    public void setRestrictionOf (RestrictionOf r)  { restrictionOf = r; }
    public void setUnionOf (UnionOf u)              { unionOf = u; }
    public void setListOf (Datatype d)              { listOf = d; }
    public void setCodeListBinding (CodeListBinding b) { clb = b; }
    
    public RestrictionOf getRestrictionOf()         { return restrictionOf; }
    public UnionOf getUnionOf()                     { return unionOf; }
    public Datatype getListOf()                     { return listOf; }
    public CodeListBinding getCodeListBinding()     { return clb; }
   
    @Override
    public void addToModel (Model m) {
        m.addComponent(this);
    }
    
    public void addCodeListBinding (CodeListBinding cb) {
        clb = cb;
    }
}
