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
package org.mitre.niem.xml;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A class to create an OASIS XML Catalog file from a mapping of namespace URIs
 * to the relative paths of the schema documents.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLCatalogCreator {
    
    private final static String XML_CATALOG_NS_URI = "urn:oasis:names:tc:entity:xmlns:xml:catalog";
    
    /**
     * Constructs a XMLCatalogCreator object for the specified map.
     */
    public XMLCatalogCreator () { }
    
    /**
     * Writes an XML Catalog with the specified mappings to the File
     */
    public void writeCatalog (Map<String,String> map, Path catP, Writer w) throws IOException, ParserConfigurationException {        
        var db   = ParserBootstrap.docBuilder();
        var dom  = db.newDocument();
        var root = dom.createElementNS(XML_CATALOG_NS_URI, "catalog");
        dom.appendChild(root);

        for (var nsuri : map.keySet()) {
            var sfn = map.get(nsuri);             // name of schema document
            var sff = new File(sfn);
            var sfp = sff.toPath();
            var relp = catP.relativize(sfp);         // path relative to catalog's directory
            var uris = relp.toString();
            uris = uris.replace('\\', '/');
            
            var entry = dom.createElementNS(XML_CATALOG_NS_URI, "uri");
            entry.setAttribute("name", nsuri);
            entry.setAttribute("uri", uris);
            root.appendChild(entry);
        }
        var xw = new XMLWriter();
        xw.writeXML(dom, w);
        
    }
}
