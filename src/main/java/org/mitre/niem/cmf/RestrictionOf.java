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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class RestrictionOf extends ObjectType {
    private Datatype datatype = null;
    private final List<Facet> facetList = new ArrayList<>();
    
    public void setDatatype (Datatype dt) { datatype = dt; }
    public Datatype getDatatype ()        { return datatype; }
    
    public List<Facet> getFacetList()     { return facetList; }
    
    public RestrictionOf (Model m) {
        super(m);
    }
      
    public void addFacet (Facet c) {
        this.facetList.add(c);
    }
    
    public void removeFacet (Facet c) {
        int index = this.facetList.indexOf(c);
        if (index < 0) return;
        this.facetList.remove(index);
    }
    
    public void replaceFacet (Facet oc, Facet nc) {
        int index = this.facetList.indexOf(oc);
        if (index < 0) return;
        this.facetList.set(index, nc);        
    }    
   
}
