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
package org.mitre.niem.xsd;

import org.mitre.niem.nmf.Facet;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.nmf.NMFException;
import org.mitre.niem.nmf.RestrictionOf;
import static org.mitre.niem.xsd.ModelXMLReader.LOG;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XFacet extends XObjectType {
    private Facet obj = null;
    
    @Override
    public Facet getObject() { return obj; }
    
    XFacet (Model m, String ens, String eln, Attributes a, int line) {
        super(m, ens, eln, a, line);
        obj = new Facet(m);
        obj.setFacetKind(eln);
    }   
    
    // Facet object needs to remember the string value
    @Override
    public void setStringVal (String s) { 
        obj.setStringVal(s);
    }
    
    @Override
    public void addToRestrictionOf (XRestrictionOf x) {
        XObjectType r    = x.getIDRepl();      // null unless replacing IDREF/URI placeholder
        RestrictionOf po = x.getObject();      // parent object
        Facet co         = this.getObject();   // child object, perhaps an IDREF/URI placeholder
        try {
            if (null != r) {
                Facet ro = (Facet)r.getObject();    // object with desired ID
                po.replaceFacet(co, ro);            // replace the placeholder
            } else {
                po.addFacet(co);
            }
        } catch (NMFException e) {
            LOG.error("line {}: {}", r.getLineNumber(), e.getMessage());
        } catch (ClassCastException e) {
            LOG.error("line {}: ID/REF type mismatch", r.getLineNumber());
        }
    }    
   
}
