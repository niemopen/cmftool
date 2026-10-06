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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.apache.xerces.xs.XSModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XMLSchemaTest {

    private static final String TEST_NS = "http://example.com/test";

@Test
void constructsFromSchemaFile(@TempDir Path tempDir) throws Exception {
    var xsdFile = writeFile(tempDir, "test.xsd", simpleSchema(TEST_NS));

    var xs = new XMLSchema(xsdFile.toString());

    assertTrue(xs.initialCatalogs().isEmpty());
    assertTrue(xs.initialNS().isEmpty());
    assertEquals(1, xs.initialSchemaDocs().size());
    assertEquals(xsdFile.toUri(), URI.create(xs.initialSchemaDocs().get(0)));

    assertNotNull(xs.resolver());
    assertTrue(xs.resolverMessages().isEmpty());

    assertEquals(1, xs.schemaDocumentL().size());
    assertEquals(1, xs.schemaDocuments(TEST_NS).size());
    assertNotNull(xs.schemaDocument(TEST_NS));
    assertTrue(xs.schemaNamespaceUs().contains(TEST_NS));

    assertEquals(
        tempDir.toAbsolutePath().normalize(),
        Path.of(URI.create(xs.pileRoot())).toAbsolutePath().normalize()
    );
    assertEquals("test.xsd", xs.fileUtoPath(xsdFile.toUri().toString()));
    assertEquals("test.xsd", xs.docFilePath(xs.schemaDocument(TEST_NS)));
}

    @Test
    void constructsFromCatalogAndNamespace(@TempDir Path tempDir) throws Exception {
        var xsdFile = writeFile(tempDir, "test.xsd", simpleSchema(TEST_NS));
        var catalogFile = writeFile(
            tempDir,
            "catalog.xml",
            catalogForNamespace(TEST_NS, xsdFile.toUri().toString())
        );

        var xs = new XMLSchema(catalogFile.toString(), TEST_NS);

        assertEquals(1, xs.initialCatalogs().size());
        assertEquals(catalogFile.toUri(), URI.create(xs.initialCatalogs().get(0)));

        assertEquals(1, xs.initialNS().size());
        assertEquals(TEST_NS, xs.initialNS().get(0));

        assertEquals(1, xs.initialSchemaDocs().size());
        assertEquals(xsdFile.toUri(), URI.create(xs.initialSchemaDocs().get(0)));

        assertEquals(xsdFile.toUri(), URI.create(xs.resolveURI(TEST_NS)));
        assertNotNull(xs.schemaDocument(TEST_NS));
        assertEquals(1, xs.schemaDocuments(TEST_NS).size());
    }

    @Test
    void xsmodelAndJavaxSchemaCanBeCreated(@TempDir Path tempDir) throws Exception {
        var xsdFile = writeFile(tempDir, "test.xsd", simpleSchema(TEST_NS));
        var xschema = new XMLSchema(xsdFile.toString());

        XSModel model = xschema.xsmodel();
        assertNotNull(model);
        assertNotNull(xschema.xsModelMsgs());

        var jxSchema = xschema.javaxSchema();
        assertNotNull(jxSchema);
        assertNotNull(xschema.javaXMsgs());
    }

    @Test
    void validateStringReturnsEmptyForValidAndMessagesForInvalid(@TempDir Path tempDir) throws Exception {
        var xsdFile = writeFile(tempDir, "test.xsd", simpleSchema(TEST_NS));
        var xschema = new XMLSchema(xsdFile.toString());

        var validXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <t:Root xmlns:t="http://example.com/test">okay</t:Root>
            """;

        var invalidXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <t:Other xmlns:t="http://example.com/test"/>
            """;

        var validMsgs = xschema.validate(validXml);
        var invalidMsgs = xschema.validate(invalidXml);

        assertTrue(validMsgs.isEmpty());
        assertFalse(invalidMsgs.isEmpty());
    }

    @Test
    void validateFileWorks(@TempDir Path tempDir) throws Exception {
        var xsdFile = writeFile(tempDir, "test.xsd", simpleSchema(TEST_NS));
        var xmlFile = writeFile(
            tempDir,
            "instance.xml",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <t:Root xmlns:t="http://example.com/test">text</t:Root>
            """
        );

        var xschema = new XMLSchema(xsdFile.toString());

        var msgs = xschema.validate(xmlFile.toFile());

        assertTrue(msgs.isEmpty());
    }

    @Test
    void xsmodelFromStreamBuildsModel() throws Exception {
        var msgs = new ArrayList<String>();
        try (var in = new ByteArrayInputStream(simpleSchema(TEST_NS).getBytes(StandardCharsets.UTF_8))) {
            var model = XMLSchema.xsmodelFromStream(in, msgs);
            assertNotNull(model);
            assertNotNull(msgs);
        }
    }

    @Test
    void schemaDocumentReturnsNullForUnknownNamespace(@TempDir Path tempDir) throws Exception {
        var xsdFile = writeFile(tempDir, "test.xsd", simpleSchema(TEST_NS));
        var xschema = new XMLSchema(xsdFile.toString());

        assertNull(xschema.schemaDocument("http://example.com/unknown"));
        assertTrue(xschema.schemaDocuments("http://example.com/unknown").isEmpty());
    }

    private static Path writeFile(Path dir, String name, String content) throws Exception {
        var file = dir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private static String simpleSchema(String targetNs) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:t="%s"
                targetNamespace="%s"
                elementFormDefault="qualified">

              <xs:element name="Root" type="xs:string"/>

            </xs:schema>
            """.formatted(targetNs, targetNs);
    }

    private static String catalogForNamespace(String ns, String resolvedUri) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog">
              <uri name="%s" uri="%s"/>
            </catalog>
            """.formatted(ns, resolvedUri);
    }
}

