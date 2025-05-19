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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class for a Restriction object in a CMF model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Restriction extends Datatype {
    
    public Restriction () { super(); }
    public Restriction (String outsideURI) { super(outsideURI); }
    public Restriction (Namespace ns, String name) { super(ns,name); }       
    
    @Override
    public int getType ()           { return CMF_RESTRICTION; }
    @Override
    public boolean isDatatype ()    { return true; }
    @Override
    public String cmfElement ()     { return "Restriction"; }
    
    private Datatype base = null;                           // cmf:Datatype
    private final List<Facet> facetL = new ArrayList<>();   // cmf:Facet
    private CodeListBinding clb = null;                     // cmf:CodeListBinding
    
    @Override
    public Restriction asRestriction ()                 { return this; }
    @Override
    public Datatype base ()                             { return base; }
    @Override
    public List<Facet> facetL()                         { return facetL; }
    @Override
    public CodeListBinding codeListBinding ()           { return clb; }
    
    public void setBase (Datatype b)                    { base = b; }
    public void setCodeListBinding (CodeListBinding c)  { clb = c; }
    public void addFacet (Facet f)                      { facetL.add(f); }

    
    @Override
    public boolean addChild (String eln, String loc, CMFObject child) throws CMFException {
        if (super.addChild(eln, loc, child)) return true;
        return child.addToRestriction(eln, loc, this);
    }
    
    @Override
    public void addComponentCMFChildren (ModelXMLWriter w, Document doc, Element c, Set<Namespace>nsS)  { 
        super.addComponentCMFChildren(w, doc, c, nsS);
        w.addRestrictionChildren(doc, c, this, nsS);
    }
    
}
