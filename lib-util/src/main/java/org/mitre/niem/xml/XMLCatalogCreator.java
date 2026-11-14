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
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A class to create an OASIS XML Catalog file from a mapping of namespace URIs
 * to the relative paths of the schema documents.
 */
public class XMLCatalogCreator {
    
    static final String XML_CATALOG_NS_URI = "urn:oasis:names:tc:entity:xmlns:xml:catalog";
    
    public XMLCatalogCreator() { }
    
    /**
     * Writes an XML Catalog with the specified mappings to the writer.
     *
     * @param map namespace URI to schema document path
     * @param catP path to the catalog file being written
     * @param w destination writer
     */
    public void writeCatalog(Map<String, String> map, Path catP, Writer w)
            throws IOException, ParserConfigurationException {
        
        Objects.requireNonNull(map, "map must not be null");
        Objects.requireNonNull(catP, "catP must not be null");
        Objects.requireNonNull(w, "writer must not be null");

        Path catalogFile = catP.toAbsolutePath().normalize();
        Path catalogDir = catalogFile.getParent();
        if (catalogDir == null) {
            catalogDir = Path.of(".").toAbsolutePath().normalize();
        }

        var db = ParserBootstrap.docBuilder();
        var dom = db.newDocument();

        var root = dom.createElementNS(XML_CATALOG_NS_URI, "catalog");
        root.setAttribute("prefer", "public");
        root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", XML_CATALOG_NS_URI);
        dom.appendChild(root);

        var entries = new ArrayList<>(map.entrySet());
        entries.sort(Map.Entry.comparingByKey());

        for (var me : entries) {
            String nsuri = Objects.requireNonNull(me.getKey(), "namespace URI must not be null");
            String schemaPathString = Objects.requireNonNull(me.getValue(), "schema path must not be null");

            String catalogUri = toCatalogRelativeUri(catalogDir, schemaPathString);

            var entry = dom.createElementNS(XML_CATALOG_NS_URI, "uri");
            entry.setAttribute("name", nsuri);
            entry.setAttribute("uri", catalogUri);
            root.appendChild(entry);
        }

        XMLWriter.writeXML(dom, w);
    }

    private static String toCatalogRelativeUri(Path catalogDir, String schemaPathString) {
        Path schemaPath = Path.of(schemaPathString).normalize();

        if (!schemaPath.isAbsolute()) {
            return schemaPath.toString().replace(File.separatorChar, '/');
        }

        Path absoluteSchemaPath = schemaPath.toAbsolutePath().normalize();
        try {
            Path relPath = catalogDir.relativize(absoluteSchemaPath);
            return relPath.toString().replace(File.separatorChar, '/');
        } catch (IllegalArgumentException ex) {
            return absoluteSchemaPath.toUri().toString();
        }
    }
}
