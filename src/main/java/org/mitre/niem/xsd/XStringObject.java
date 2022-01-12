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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.mitre.niem.cmf.CMFException;
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
    public void addToClassType(XClassType xc) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "AbstractIndicator":            xc.getObject().setIsAbstract(val); break;
        case "DefinitionText":               xc.getObject().setDefinition(val); break;
        case "DeprecatedIndicator":          xc.getObject().setIsDeprecated(val); break;
        case "ExternalAdapterTypeIndicator": xc.getObject().setIsExternal(val); break;
        case "Name":                         xc.getObject().setName(val); break;
        default:
            break;
        }
    }
    
    @Override
    public void addToDatatype(XDatatype xdt) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "DefinitionText":      xdt.getObject().setDefinition(val); break;
        case "DeprecatedIndicator": xdt.getObject().setIsDeprecated(val); break;        
        case "Name":                xdt.getObject().setName(val); break;
        default:
            break;
        }
    } 
    
    @Override
    public void addToFacet(XFacet xf) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "DefinitionText":      xf.getObject().setDefinition(val); break;
        case "NonNegativeValue":
        case "PositiveValue":
        case "StringValue":
        case "WhiteSpaceValueCode": xf.getObject().setStringVal(val); break;
        default:
            break;
        }
    } 
    
    @Override
    public void addToHasProperty (XHasProperty xhp) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "MaxOccursQuantity":    xhp.getObject().setMaxOccursQuantity(val); break;
        case "MinOccursQuantity":    xhp.getObject().setMinOccursQuantity(val); break;
        default:
            break;
        }        
    }
        
    @Override
    public void addToNamespace(XNamespace xns) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "DefinitionText":             xns.getObject().setDefinition(val) ; break;
        case "ExternalNamespaceIndicator": xns.getObject().setIsExternal(val); break;
        case "NamespaceURI":               xns.getObject().setNamespaceURI(val); break;
        case "NamespacePrefixName": 
            try {
                xns.getObject().setNamespacePrefix(val);
            } catch (CMFException ex) {
                // can't be a duplicate prefix now -- not part of a model yet
            }
            break;
        default:
            break;
        }
     } 
    @Override
    public void addToProperty (XProperty xop) {
        String val = (null != getIDRepl() ? getIDRepl().getStringVal() : getStringVal());
        switch (this.getComponentLname()) {
        case "AbstractIndicator":   xop.getObject().setIsAbstract(val); break;
        case "DefinitionText":      xop.getObject().setDefinition(val); break;
        case "DeprecatedIndicator": xop.getObject().setIsDeprecated(val); break;        
        case "Name":                xop.getObject().setName(val); break;
        default:
            break;
        }
    }

}
