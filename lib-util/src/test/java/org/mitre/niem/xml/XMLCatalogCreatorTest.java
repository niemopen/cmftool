/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2026 The MITRE Corporation.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

class XMLCatalogCreatorTest {

    @TempDir
    Path tempDir;

    @Test
    void writesPathsRelativeToCatalogFileParentDirectory() throws Exception {
        Path catalogDir = Files.createDirectories(tempDir.resolve("catalog"));
        Path schemaDir = Files.createDirectories(tempDir.resolve("schemas"));
        Path schema = Files.createFile(schemaDir.resolve("test.xsd"));
        Path catalogFile = catalogDir.resolve("catalog.xml");

        Map<String, String> map = Map.of("urn:test", schema.toString());

        StringWriter sw = new StringWriter();
        new XMLCatalogCreator().writeCatalog(map, catalogFile, sw);

        var doc = parse(sw.toString());
        NodeList uriNodes = doc.getElementsByTagNameNS(XMLCatalogCreator.XML_CATALOG_NS_URI, "uri");
        assertEquals(1, uriNodes.getLength());

        Element uri = (Element) uriNodes.item(0);
        assertEquals("urn:test", uri.getAttribute("name"));
        assertEquals("../schemas/test.xsd", uri.getAttribute("uri"));
    }

    @Test
    void preservesRelativeSchemaPaths() throws Exception {
        Path catalogFile = tempDir.resolve("catalog.xml");
        Map<String, String> map = Map.of("urn:test", "schemas/test.xsd");

        StringWriter sw = new StringWriter();
        new XMLCatalogCreator().writeCatalog(map, catalogFile, sw);

        var doc = parse(sw.toString());
        NodeList uriNodes = doc.getElementsByTagNameNS(XMLCatalogCreator.XML_CATALOG_NS_URI, "uri");
        assertEquals(1, uriNodes.getLength());

        Element uri = (Element) uriNodes.item(0);
        assertEquals("schemas/test.xsd", uri.getAttribute("uri"));
    }

    @Test
    void sortsEntriesByNamespaceUri() throws Exception {
        Path catalogFile = tempDir.resolve("catalog.xml");

        Map<String, String> map = new LinkedHashMap<>();
        map.put("urn:z", "z.xsd");
        map.put("urn:a", "a.xsd");

        StringWriter sw = new StringWriter();
        new XMLCatalogCreator().writeCatalog(map, catalogFile, sw);

        var doc = parse(sw.toString());
        NodeList uriNodes = doc.getElementsByTagNameNS(XMLCatalogCreator.XML_CATALOG_NS_URI, "uri");
        assertEquals(2, uriNodes.getLength());

        Element first = (Element) uriNodes.item(0);
        Element second = (Element) uriNodes.item(1);

        assertEquals("urn:a", first.getAttribute("name"));
        assertEquals("urn:z", second.getAttribute("name"));
    }

    @Test
    void writesCatalogRootAttributes() throws Exception {
        Path catalogFile = tempDir.resolve("catalog.xml");
        Map<String, String> map = Map.of("urn:test", "test.xsd");

        StringWriter sw = new StringWriter();
        new XMLCatalogCreator().writeCatalog(map, catalogFile, sw);

        var doc = parse(sw.toString());
        Element root = doc.getDocumentElement();

        assertEquals("catalog", root.getLocalName());
        assertEquals(XMLCatalogCreator.XML_CATALOG_NS_URI, root.getNamespaceURI());
        assertEquals("public", root.getAttribute("prefer"));
    }

    @Test
    void throwsForNullArguments() {
        XMLCatalogCreator creator = new XMLCatalogCreator();

        assertThrows(NullPointerException.class,
                () -> creator.writeCatalog(null, tempDir.resolve("catalog.xml"), new StringWriter()));

        assertThrows(NullPointerException.class,
                () -> creator.writeCatalog(Map.of("urn:test", "test.xsd"), null, new StringWriter()));

        assertThrows(NullPointerException.class,
                () -> creator.writeCatalog(Map.of("urn:test", "test.xsd"), tempDir.resolve("catalog.xml"), null));
    }

    @Test
    void throwsForNullMapKey() {
        XMLCatalogCreator creator = new XMLCatalogCreator();
        Map<String, String> map = new LinkedHashMap<>();
        map.put(null, "test.xsd");

        assertThrows(NullPointerException.class,
                () -> creator.writeCatalog(map, tempDir.resolve("catalog.xml"), new StringWriter()));
    }

    @Test
    void throwsForNullMapValue() {
        XMLCatalogCreator creator = new XMLCatalogCreator();
        Map<String, String> map = new LinkedHashMap<>();
        map.put("urn:test", null);

        assertThrows(NullPointerException.class,
                () -> creator.writeCatalog(map, tempDir.resolve("catalog.xml"), new StringWriter()));
    }

    private static org.w3c.dom.Document parse(String xml) throws Exception {
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        var db = dbf.newDocumentBuilder();
        return db.parse(new InputSource(new StringReader(xml)));
    }
}

