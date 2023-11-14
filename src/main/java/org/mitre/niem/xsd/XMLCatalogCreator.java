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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Scanner;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import static org.mitre.niem.NIEMConstants.XML_CATALOG_NS_URI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class to create an OASIS XML Catalog file from a mapping of namespace URIs
 * to the relative paths of the schema documents.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLCatalogCreator {
    
    private final Map<String,String> nsmap;
    
    XMLCatalogCreator (Map<String,String> map) {
        nsmap = map;
    }
    
    public void writeCatalog (File cf) throws IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException {
        var cfp = cf.toPath();
        if (null != cfp.getParent()) cfp = cfp.getParent();
        
        var dbf  = DocumentBuilderFactory.newInstance();
        var db   = dbf.newDocumentBuilder();
        var dom  = db.newDocument();
        var root = dom.createElementNS(XML_CATALOG_NS_URI, "catalog");
        dom.appendChild(root);

        for (var nsuri : nsmap.keySet()) {
            var sfn = nsmap.get(nsuri);             // name of schema document
            var sff = new File(sfn);
            var sfp = sff.toPath();
            var relp = cfp.relativize(sfp);         // path relative to catalog's directory
            var uris = relp.toString();
            uris = uris.replace('\\', '/');
            
            var entry = dom.createElementNS(XML_CATALOG_NS_URI, "uri");
            entry.setAttribute("name", nsuri);
            entry.setAttribute("uri", uris);
            root.appendChild(entry);
        }
        var ofs = new FileOutputStream(cf);
        var xw = new XMLWriter(dom, ofs);
        xw.writeXML();;
    }
}
