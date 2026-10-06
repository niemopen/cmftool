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
package org.mitre.niem.utility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourceManagerTest {

    private static final String TEXT_RESOURCE =
        "/org/mitre/niem/utility/testdata/sample.txt";
    private static final String BINARY_RESOURCE =
        "/org/mitre/niem/utility/testdata/sample.bin";
    private static final String MISSING_RESOURCE =
        "/org/mitre/niem/utility/testdata/does-not-exist.txt";

    private ResourceManager newManager() {
        return new ResourceManager(ResourceManagerTest.class);
    }

    @Test
    void constructorRejectsNullAnchorClass() {
        assertThrows(NullPointerException.class, () -> new ResourceManager(null));
    }

    @Test
    void getResourceStreamReadsTextResourceWithLeadingSlash() throws Exception {
        var rm = newManager();

        try (var in = rm.getResourceStream(TEXT_RESOURCE)) {
            var text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("hello resource\n", text);
        }
    }

    @Test
    void getResourceStreamReadsTextResourceWithoutLeadingSlash() throws Exception {
        var rm = newManager();

        try (var in = rm.getResourceStream(TEXT_RESOURCE.substring(1))) {
            var text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("hello resource\n", text);
        }
    }

    @Test
    void getResourceStreamReadsBinaryResource() throws Exception {
        var rm = newManager();

        try (var in = rm.getResourceStream(BINARY_RESOURCE)) {
            var bytes = in.readAllBytes();
            assertArrayEquals(new byte[] {0x00, 0x01, 0x02, 0x7F, (byte) 0x80, (byte) 0xFF}, bytes);
        }
    }

    @Test
    void getResourceStreamThrowsForMissingResource() {
        var rm = newManager();

        assertThrows(FileNotFoundException.class, () -> rm.getResourceStream(MISSING_RESOURCE));
    }

    @Test
    void copyResourceToFileCopiesTextResource(@TempDir Path tempDir) throws Exception {
        var rm = newManager();
        var out = tempDir.resolve("nested/out.txt").toFile();

        rm.copyResourceToFile(TEXT_RESOURCE, out);

        assertTrue(out.isFile());
        var text = Files.readString(out.toPath(), StandardCharsets.UTF_8);
        assertEquals("hello resource\n", text);
    }

    @Test
    void copyResourceToFileCopiesBinaryResource(@TempDir Path tempDir) throws Exception {
        var rm = newManager();
        var out = tempDir.resolve("sample-copy.bin").toFile();

        rm.copyResourceToFile(BINARY_RESOURCE, out);

        assertTrue(out.isFile());
        var bytes = Files.readAllBytes(out.toPath());
        assertArrayEquals(new byte[] {0x00, 0x01, 0x02, 0x7F, (byte) 0x80, (byte) 0xFF}, bytes);
    }

    @Test
    void copyResourceToFileCreatesParentDirectories(@TempDir Path tempDir) throws Exception {
        var rm = newManager();
        var out = tempDir.resolve("a/b/c/sample.txt").toFile();

        rm.copyResourceToFile(TEXT_RESOURCE, out);

        assertTrue(out.isFile());
        assertEquals("hello resource\n", Files.readString(out.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void copyResourceToFileThrowsForMissingResource(@TempDir Path tempDir) {
        var rm = newManager();
        var out = tempDir.resolve("missing.txt").toFile();

        assertThrows(FileNotFoundException.class, () -> rm.copyResourceToFile(MISSING_RESOURCE, out));
    }

    @Test
    void copyResourceToFileRejectsNullOutputFile() {
        var rm = newManager();

        assertThrows(NullPointerException.class, () -> rm.copyResourceToFile(TEXT_RESOURCE, null));
    }

    @Test
    void getResourceUriReturnsUriForExistingResource() {
        var rm = newManager();

        var uri = rm.getResourceURI(TEXT_RESOURCE);

        assertNotNull(uri);
        assertNotNull(uri.getScheme());
    }

    @Test
    void getResourceUriReturnsNullForMissingResource() {
        var rm = newManager();

        var uri = rm.getResourceURI(MISSING_RESOURCE);

        assertNull(uri);
    }

    @Test
    void methodsRejectBlankResourceNames(@TempDir Path tempDir) {
        var rm = newManager();

        assertThrows(IllegalArgumentException.class, () -> rm.getResourceURI(" "));
        assertThrows(IllegalArgumentException.class, () -> rm.getResourceStream(" "));
        assertThrows(IllegalArgumentException.class,
            () -> rm.copyResourceToFile(" ", tempDir.resolve("x.txt").toFile()));
    }
}
