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

import org.mitre.niem.cmf.Model;
import org.xml.sax.Attributes;

/**
 * A class for the string value of a simple element.  Simple elements don't have
 * objects in the NIEM model representation -- they become the values of
 * properties of model objects.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XStringObject extends XObjectType {
    
    XStringObject(Model m, XObjectType p, String ens, String eln, Attributes a, int line) {
        super(m, p, ens, eln, a, line);
    }

    @Override
    public void addToClassType(XClassType c) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "AbstractIndicator":   c.getObject().setAbstractIndicator(val); break;
        case "DefinitionText":      c.getObject().setDefinition(val); break;
        case "Name":                c.getObject().setName(val); break;
        default:
            break;
        }
    }
    
    @Override
    public void addToDatatype(XDatatype dt) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "DefinitionText":      dt.getObject().setDefinition(val); break;
        case "Name":                dt.getObject().setName(val); break;
        default:
            break;
        }
    } 
    
    @Override
    public void addToFacet(XFacet dt) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "DefinitionText":      dt.getObject().setDefinition(val); break;
        case "NonNegativeValue":
        case "PositiveValue":
        case "StringValue":
        case "WhiteSpaceValueCode": dt.getObject().setStringVal(val); break;
        default:
            break;
        }
    } 
    
    @Override
    public void addToHasProperty (XHasProperty hp) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "MaxOccursQuantity":    hp.getObject().setMaxOccursQuantity(val); break;
        case "MinOccursQuantity":    hp.getObject().setMinOccursQuantity(val); break;
        default:
            break;
        }        
    }
        
    @Override
    public void addToNamespace(XNamespace ns) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "DefinitionText":       ns.getObject().setDefinition(val) ; break;
        case "NamespacePrefixName":  ns.getObject().setNamespacePrefix(val); break;
        case "NamespaceURI":         ns.getObject().setNamespaceURI(val); break;
        default:
            break;
        }
     } 
    @Override
    public void addToProperty (XProperty op) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "AbstractIndicator":   op.getObject().setAbstractIndicator(val); break;
        case "DefinitionText":      op.getObject().setDefinition(val); break;
        case "Name":                op.getObject().setName(val); break;
        default:
            break;
        }
    }

}
