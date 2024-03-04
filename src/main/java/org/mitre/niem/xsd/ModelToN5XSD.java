/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2023 The MITRE Corporation.
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

import org.mitre.niem.cmf.Model;

/**
 * Generates a NIEM 5 schema document pile from a CMF model
 * that is based on NIEM 3, 4, or 5.0 namespaces.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToN5XSD extends ModelToXSD {
    
    public ModelToN5XSD () { super(); }
    public ModelToN5XSD (Model m) { super(m); }
    
    @Override
    protected String fixConformanceTargets (String ctas) {
        return ctas;
    }

    @Override
    protected String getArchitecture ()       { return "NIEM5"; }
    
    @Override
    protected String getDefaultNIEMVersion()  { return "5"; }

    @Override
    protected String structuresBaseType (String compName) {
        var bt = structPrefix + ":";
        if (compName.endsWith("Association") || compName.endsWith("AssociationType")) bt = bt + "AssociationType";
        else if (compName.endsWith("Augmentation") || compName.endsWith("AugmentationType")) bt = bt + "AugmentationType";        
        else if (compName.endsWith("Metadata") || compName.endsWith("MetadataType")) bt = bt + "MetadataType";
        else bt = bt + "ObjectType";
        return bt;
    }    
}
