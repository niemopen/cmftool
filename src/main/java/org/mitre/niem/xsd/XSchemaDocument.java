/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2022 The MITRE Corporation.
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
import org.mitre.niem.cmf.ObjectType;
import org.mitre.niem.cmf.SchemaDocument;
import org.xml.sax.Attributes;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XSchemaDocument extends XObjectType {
    private SchemaDocument obj = null;
    
    @Override
    public void setObject (ObjectType o) { 
        obj = SchemaDocument.class == o.getClass() ? (SchemaDocument)o : null;
    }

    @Override
    public SchemaDocument getObject () { return obj; }
    
    XSchemaDocument (Model m, XObjectType p, String ens, String eln, Attributes a, int line) {
        super(m, p, ens, eln, a, line);
        obj = new SchemaDocument();
    }    
    
    @Override
    public void addAsChild (XObjectType child) {
        child.addToSchemaDocument(this);
        super.addAsChild(child);  
    }      
    
    @Override
    public void addToModel (XModel x) {
        x.getObject().addSchemaDoc(this.getObject().targetNS(), this.getObject());
    }
}
