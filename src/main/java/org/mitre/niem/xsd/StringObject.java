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
import static org.mitre.niem.xsd.ModelXMLReader.LOG;
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
    
    StringObject(Model m, String ens, String eln, Attributes a) {
        super(m, ens, eln, a);
    }
    
    // Don't add StringObjects to the model object list
    @Override
    public void addToModelObjectList () {  
    }

    @Override
    public int addToClass(ClassType c, XMLDataRecord cdat) {
        ObjectType child = cdat.obj;
        switch (child.getComponentLname()) {
        case "AbstractIndicator":   c.setAbstractIndicator(cdat.stringVal); break;
        case "ContentStyleCode":    c.setContentStyleCode(cdat.stringVal);  break;
        case "DefinitionText":      c.setDefinition(cdat.stringVal); break;
        case "Name":                c.setName(cdat.stringVal); break;
        default:
            LOG.warn("Unknown Class property {} at line {} (ignored)", child.getComponentLname(), cdat.lineNumber);
            break;
        }
        return -1;
    }
    
    @Override
    public int addToDataProperty(DataProperty dp, XMLDataRecord cdat) {
        ObjectType child = cdat.obj;
        switch (child.getComponentLname()) {
        case "DefinitionText":      dp.setDefinition(cdat.stringVal); break;
        case "Name":                dp.setName(cdat.stringVal); break;
        default:
            LOG.warn("Unknown DataProperty property {} at line {} (ignored)", child.getComponentLname(), cdat.lineNumber);
            break;
        }
        return -1;
    }    
    
    @Override
    public int addToDatatype(Datatype dt, XMLDataRecord cdat) {
        ObjectType child = cdat.obj;
        switch (child.getComponentLname()) {
        case "DefinitionText":      dt.setDefinition(cdat.stringVal); break;
        case "Name":                dt.setName(cdat.stringVal); break;
        default:
            LOG.warn("Unknown Datatype property {} at line {} (ignored)", child.getComponentLname(), cdat.lineNumber);
            break;
        }
        return -1;
    } 
    
    @Override
    public int addToNamespace(Namespace ns, XMLDataRecord cdat) {
        ObjectType child = cdat.obj;
        switch (child.getComponentLname()) {
        case "DefinitionText":       ns.setDefinition(cdat.stringVal) ; break;
        case "NamespacePrefixName":  ns.setNamespacePrefix(cdat.stringVal); break;
        case "NamespaceURI":         ns.setNamespaceURI(cdat.stringVal); break;
        default:
            LOG.warn("Unknown Namespace property {} at line {} (ignored)", child.getComponentLname(), cdat.lineNumber);
            break;
        }
        return -1;
    } 
    @Override
    public int addToObjectProperty (ObjectProperty op, XMLDataRecord cdat) {
        ObjectType child = cdat.obj;
        switch (child.getComponentLname()) {
        case "AbstractIndicator":   op.setAbstractIndicator(cdat.stringVal); break;
        case "DefinitionText":      op.setDefinition(cdat.stringVal); break;
        case "Name":                op.setName(cdat.stringVal); break;
        default:
            LOG.warn("Unknown Class property {} at line {} (ignored)", child.getComponentLname(), cdat.lineNumber);
            break;
        }
        return -1;
    }}
