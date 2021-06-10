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

import org.mitre.niem.xsd.XMLDataRecord;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class SubPropertyOf extends ObjectType {
    
    protected ObjectProperty objectProperty = null;
    
    public void setObjectProperty (ObjectProperty o) { objectProperty = o; }
    public ObjectProperty getObjectProperty ()       { return objectProperty; }
    
    public SubPropertyOf (Model m, String ens, String eln, Attributes a) {
        super(m, ens, eln, a);
    }
    
    @Override
    public int addChild (XMLDataRecord cdat) {
        return cdat.obj.addToSubPropertyOf(this, cdat);
    }
        
    @Override
    public int addToObjectProperty (ObjectProperty op, XMLDataRecord cdat) {
        op.setSubPropertyOf(this);
        return -1;
    }
}
