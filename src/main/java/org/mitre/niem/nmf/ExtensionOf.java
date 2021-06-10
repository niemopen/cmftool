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
import org.mitre.niem.xsd.XMLDataRecord;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ExtensionOf extends ObjectType {
    protected ClassType classType = null;
    protected List<HasValue> hasValueList = new ArrayList<>();
    protected List<HasDataProperty> hasDataPropertyList = new ArrayList<>();
    protected List<HasObjectProperty> hasObjectPropertyList = new ArrayList<>();
    
    public ClassType getClassType ()       { return classType; }
    public void setClassType (ClassType c) { classType = c; }
    
    public List<HasValue> hasValueList ()                   { return hasValueList; }
    public List<HasDataProperty> hasDataPropertyList ()     { return hasDataPropertyList; }
    public List<HasObjectProperty> hasObjectPropertyList () { return hasObjectPropertyList; }
    
    public ExtensionOf (Model m, String ens, String eln, Attributes a) {
        super(m, ens, eln, a);
    }
    
    @Override
    public int addChild (XMLDataRecord cdat) {
        return cdat.obj.addToExtensionOf(this, cdat);
    }
    
    @Override
    public int addToClass (ClassType c, XMLDataRecord cdat) {
        c.setExtensionOf(this);
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
            if (null != classType) classType.countRefs(rc);
            if (null != hasValueList) {
                for (var hv : hasValueList) { hv.countRefs(rc); }
            }
            if (null != hasDataPropertyList) {
                for (var hv : hasDataPropertyList) { hv.countRefs(rc); }
            }
            if (null != hasObjectPropertyList) {
                for (var hv : hasObjectPropertyList) { hv.countRefs(rc); }
            }                
        }
    }    
}
