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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.mitre.niem.NIEMConstants.NMF_NS_URI_PREFIX;
import org.mitre.niem.xsd.XMLDataRecord;
import org.xml.sax.Attributes;


/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class HasDataProperty extends ObjectType {
    
    protected List<DataProperty> dataPropertyList = new ArrayList<>();
    protected String minOccursQuantity = null;
    protected String maxOccursQuantity = null;
    
    public void setMinOccursQuantity (String s) { minOccursQuantity = s; }
    public void setMaxOccursQuantity (String s) { maxOccursQuantity = s; }
    
    public String minOccursQuantity() { return minOccursQuantity; }
    public String maxOccursQuantity() { return maxOccursQuantity; }
    
    public List<DataProperty> dataPropertyList () { return dataPropertyList; }
    
    public HasDataProperty (Model m, String ens, String eln, Attributes a) {
        super(m, ens, eln, a);
        for (int i = 0; i < a.getLength(); i++) {
            if (a.getURI(i).startsWith(NMF_NS_URI_PREFIX)) {
                if ("minOccursQuantity".equals(a.getLocalName(i))) {
                    minOccursQuantity = a.getValue(i);
                }
                else if ("maxOccursQuantity".equals(a.getLocalName(i))) {
                    maxOccursQuantity = a.getValue(i);
                }
            }
        }
    }
    
    @Override
    public int addChild (XMLDataRecord cdat) {
        return cdat.obj.addToHasDataProperty(this, cdat);
    }

    @Override
    public int addToExtensionOf (ExtensionOf e, XMLDataRecord cdat) { 
        if (cdat.index < 0) {
            e.hasDataPropertyList().add(this);
            return e.hasDataPropertyList().size()-1;  
        }
        // Replace @ref placeholder with @id object
        e.hasDataPropertyList().set(cdat.index, this);
        return -1;
    }
    
    
    @Override
    public void countRefs (Map<ObjectType,Integer> rc) {
        if (rc.containsKey(this)) {
            int count = rc.get(this);
            rc.put(this, count+1);
        }
        else {
            rc.put(this, 1);
            if (null != dataPropertyList) {
                for (var hv : dataPropertyList) { hv.countRefs(rc); }
            }          
        }
    }     
}
