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
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Facet extends ObjectType implements Comparable<Facet> {
    private String definition = null;
    private String facetKind = null;
    private String stringVal = null;
    
    public void setDefinition (String s)  { definition = s; }
    public void setFacetKind (String s)   { facetKind = s; }
    public void setStringVal (String s)   { stringVal = s; }
    
    public String getDefinition ()  { return definition; }
    public String getFacetKind()    { return facetKind; }
    public String getStringVal()    { return stringVal; }
    public String getXSDFacet() {
        return facetKind.substring(0,1).toLowerCase() + facetKind.substring(1);
    }
    
    public Facet () { super(); }
    
    @Override
    public int compareTo (Facet o) {
        int rv = this.facetKind.compareTo(o.facetKind);
        if (0 == rv && "Enumeration".equals(facetKind)) {
            rv = this.stringVal.compareToIgnoreCase(o.stringVal);
        }
        return rv;
    }

}
