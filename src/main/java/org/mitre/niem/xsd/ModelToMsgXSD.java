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

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Property;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class to generate a NIEM 6 message schema from a Model
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToMsgXSD extends ModelToXSD {
    
    public ModelToMsgXSD () { super(); }
    public ModelToMsgXSD (Model m) { super(m); }

    @Override
    protected String getArchitecture ()       { return "NIEM6"; }

    @Override
    protected String getShareVersionSuffix () { return ".0-msg"; }    
    
    @Override
    protected void addReferenceAttributes (Document dom, Element pe, Property p) { 
        if (!p.isReferenceable()) return;
        var e = dom.createElementNS(W3C_XML_SCHEMA_NS_URI, "xs:attributeGroup");
        e.setAttribute("ref", "structures:ReferenceableObjectAttributeGroup");
        pe.appendChild(e);
    }    
    
    // Don't convert "xs:foo" to "xs-proxy:foo" in message schema documents
    @Override
    protected String proxifiedDatatypeQName (Datatype dt) {
        return dt.getQName();
    }    
}
