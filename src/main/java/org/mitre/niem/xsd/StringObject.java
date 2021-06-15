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

import org.mitre.niem.nmf.ClassType;
import org.mitre.niem.nmf.DataProperty;
import org.mitre.niem.nmf.Datatype;
import org.mitre.niem.nmf.Model;
import org.mitre.niem.nmf.Namespace;
import org.mitre.niem.nmf.ObjectProperty;
import org.mitre.niem.nmf.ObjectType;
import org.xml.sax.Attributes;

/**
 * A class for the string value of a simple element.  Simple elements don't have
 * objects in the NIEM model representation -- they become the values of
 * properties of model objects.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class StringObject extends ObjectType {
    
    protected String stringVal = null;
    
    @Override
    public void setStringVal (String s) { stringVal = s; }
    
    StringObject(Model m, String ens, String eln, Attributes a) {
        super(m, ens, eln, a);
    }

    @Override
    public int addToClass(ClassType c, int index) {
        switch (this.getComponentLname()) {
        case "AbstractIndicator":   c.setAbstractIndicator(stringVal); break;
        case "ContentStyleCode":    c.setContentStyleCode(stringVal);  break;
        case "DefinitionText":      c.setDefinition(stringVal); break;
        case "Name":                c.setName(stringVal); break;
        default:
            break;
        }
        return -1;
    }
    
    @Override
    public int addToDataProperty(DataProperty dp, int index) {
        switch (this.getComponentLname()) {
        case "DefinitionText":      dp.setDefinition(stringVal); break;
        case "Name":                dp.setName(stringVal); break;
        default:
            break;
        }
        return -1;
    }    
    
    @Override
    public int addToDatatype(Datatype dt, int index) {
        switch (this.getComponentLname()) {
        case "DefinitionText":      dt.setDefinition(stringVal); break;
        case "Name":                dt.setName(stringVal); break;
        default:
            break;
        }
        return -1;
    } 
    
    @Override
    public int addToNamespace(Namespace ns, int index) {
        switch (this.getComponentLname()) {
        case "DefinitionText":       ns.setDefinition(stringVal) ; break;
        case "NamespacePrefixName":  ns.setNamespacePrefix(stringVal); break;
        case "NamespaceURI":         ns.setNamespaceURI(stringVal); break;
        default:
            break;
        }
        return -1;
    } 
    @Override
    public int addToObjectProperty (ObjectProperty op, int index) {
         switch (this.getComponentLname()) {
        case "AbstractIndicator":   op.setAbstractIndicator(stringVal); break;
        case "DefinitionText":      op.setDefinition(stringVal); break;
        case "Name":                op.setName(stringVal); break;
        default:
            break;
        }
        return -1;
    }}
