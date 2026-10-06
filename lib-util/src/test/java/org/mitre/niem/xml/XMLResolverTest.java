/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2022 The MITRE Corporation.
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.ls.LSInput;

class XMLResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveUriReturnsNoMapForUnknownNamespace() {
        var resolver = new XMLResolver(List.of());

        var result = resolver.resolveURI("http://example.com/ns/unknown");

        assertEquals(XMLResolver.NO_MAP, result);
        assertEquals(XMLResolver.NO_MAP,
            resolver.allResolutions().get("http://example.com/ns/unknown"));
    }

    @Test
    void resolveUriReturnsLocalFileUriForMappedNamespace() throws Exception {
        var schemaFile = writeFile(
            "mapped.xsd",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                       targetNamespace="http://example.com/ns/test"/>
            """
        );

        var catalogFile = writeFile(
            "catalog.xml",
            catalogForLocalUri("http://example.com/ns/test", schemaFile.toUri().toString())
        );

        var resolver = new XMLResolver(List.of(catalogFile.toUri().toString()));

        var result = resolver.resolveURI("http://example.com/ns/test");

        assertEquals(schemaFile.toUri().toString(), result);
        assertEquals(result, resolver.allResolutions().get("http://example.com/ns/test"));
    }

    @Test
    void resolveUriReturnsRemoteMapForRemoteResolution() throws Exception {
        var catalogFile = writeFile(
            "catalog.xml",
            catalogForLocalUri("http://example.com/ns/remote", "https://example.com/remote.xsd")
        );

        var resolver = new XMLResolver(List.of(catalogFile.toUri().toString()));

        var result = resolver.resolveURI("http://example.com/ns/remote");

        assertEquals(XMLResolver.REMOTE_MAP, result);
        assertEquals(XMLResolver.REMOTE_MAP,
            resolver.allResolutions().get("http://example.com/ns/remote"));
    }

    @Test
    void resolveResourceReturnsNullForBlankOrUnknownNamespace() {
        var resolver = new XMLResolver(List.of());

        assertNull(resolver.resolveResource(null, null, null, null, null));
        assertNull(resolver.resolveResource(null, " ", null, null, null));
        assertNull(resolver.resolveResource(null, "http://example.com/ns/missing", null, null, null));
    }

    @Test
    void resolveResourceReturnsLsInputForMappedNamespace() throws Exception {
        var schemaFile = writeFile(
            "mapped.xsd",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                       targetNamespace="http://example.com/ns/test"/>
            """
        );

        var catalogFile = writeFile(
            "catalog.xml",
            catalogForLocalUri("http://example.com/ns/test", schemaFile.toUri().toString())
        );

        var resolver = new XMLResolver(List.of(catalogFile.toUri().toString()));

        LSInput input = resolver.resolveResource(
            "http://www.w3.org/2001/XMLSchema",
            "http://example.com/ns/test",
            "pub-id",
            "sys-id",
            "file:/base/catalog.xml"
        );

        assertNotNull(input);
        assertEquals("pub-id", input.getPublicId());
        assertEquals(schemaFile.toUri().toString(), input.getSystemId());
        assertEquals("file:/base/catalog.xml", input.getBaseURI());
    }

    @Test
    void allCatalogsReturnsInitialCatalogs() throws Exception {
        var catalog1 = writeFile(
            "catalog1.xml",
            catalogForLocalUri("http://example.com/ns/one", "file:/tmp/one.xsd")
        );
        var catalog2 = writeFile(
            "catalog2.xml",
            catalogForLocalUri("http://example.com/ns/two", "file:/tmp/two.xsd")
        );

        var resolver = new XMLResolver(List.of(
            catalog1.toUri().toString(),
            catalog2.toUri().toString()
        ));

        var catalogs = resolver.allCatalogs();

        assertEquals(2, catalogs.size());
        assertTrue(catalogs.contains(catalog1.toUri().toString()));
        assertTrue(catalogs.contains(catalog2.toUri().toString()));
    }

    @Test
    void allMessagesIsInitiallyEmpty() {
        var resolver = new XMLResolver(List.of());

        assertTrue(resolver.allMessages().isEmpty());
    }

    @Test
    void allResolutionsReturnsImmutableMap() {
        var resolver = new XMLResolver(List.of());
        resolver.resolveURI("http://example.com/ns/unknown");

        var map = resolver.allResolutions();

        assertThrows(UnsupportedOperationException.class,
            () -> map.put("http://example.com/ns/new", "file:/tmp/new.xsd"));
    }

    @Test
    void allCatalogsReturnsImmutableSet() {
        var resolver = new XMLResolver(List.of());

        var catalogs = resolver.allCatalogs();

        assertThrows(UnsupportedOperationException.class,
            () -> catalogs.add("file:/tmp/catalog.xml"));
    }

    @Test
    void allMessagesReturnsImmutableList() {
        var resolver = new XMLResolver(List.of());

        var messages = resolver.allMessages();

        assertThrows(UnsupportedOperationException.class,
            () -> messages.add("message"));
    }

    @Test
    void repeatedResolutionUsesCachedValue() throws Exception {
        var schemaFile = writeFile(
            "mapped.xsd",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                       targetNamespace="http://example.com/ns/test"/>
            """
        );

        var catalogFile = writeFile(
            "catalog.xml",
            catalogForLocalUri("http://example.com/ns/test", schemaFile.toUri().toString())
        );

        var resolver = new XMLResolver(List.of(catalogFile.toUri().toString()));

        var first = resolver.resolveURI("http://example.com/ns/test");
        var second = resolver.resolveURI("http://example.com/ns/test");

        assertEquals(first, second);
        assertEquals(1, resolver.allResolutions().size());
    }

    @Test
    void protectedNoArgConstructorWorksForSamePackageTests() {
        var resolver = new XMLResolver();

        assertNotNull(resolver);
        assertTrue(resolver.allCatalogs().isEmpty());
        assertTrue(resolver.allMessages().isEmpty());
        assertFalse(resolver.allResolutions().containsKey("http://example.com/ns/test"));
    }

    private Path writeFile(String name, String content) throws Exception {
        var path = tempDir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    private static String catalogForLocalUri(String namespaceUri, String resolvedUri) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <catalog xmlns="urn:oasis:names:tc:entity:xmlns:xml:catalog">
              <uri name="%s" uri="%s"/>
            </catalog>
            """.formatted(namespaceUri, resolvedUri);
    }
}
