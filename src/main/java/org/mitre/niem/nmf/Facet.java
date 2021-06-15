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
package org.mitre.niem.nmf;

import java.util.Map;
import java.util.Set;
import org.mitre.niem.xsd.XMLDataRecord;
import org.xml.sax.Attributes;

/**
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Facet extends ObjectType {
    protected String definition = null;
    protected String facetKind = null;
    protected String stringVal = null;
    
    public void setDefinition (String s)  { definition = s; }
    public void setFacetKind (String s)   { facetKind = s; }
    @Override
    public void setStringVal (String s)   { stringVal = s; }
    
    public String getDefinition ()  { return definition; }
    public String getFacetKind()    { return facetKind; }
    public String getStringVal()    { return stringVal; }
    
    public Facet (Model m, String ens, String eln, Attributes a) {
        super(m, ens, eln, a);
        facetKind = eln;
    }
    
    @Override
    public int addToRestrictionOf (RestrictionOf r, int index) {
        if (index < 0) {
            r.facetList().add(this);
            return r.facetList().size()-1;  
        }
        // Replace @ref placeholder with @id object
        r.facetList().set(index, this);
        return -1;
    }    
    
    @Override
    public int addToUnionOf (UnionOf u, int index) {
        if (index < 0) {
            u.facetList().add(this);
            return u.facetList().size()-1;  
        }
        // Replace @ref placeholder with @id object
        u.facetList().set(index, this);
        return -1;
    } 

    @Override
    void traverse (Set<ObjectType> seen, TraverseFunc f) {
        f.func(this);
        if (seen.contains(this)) return;
        seen.add(this);
    }    
}
