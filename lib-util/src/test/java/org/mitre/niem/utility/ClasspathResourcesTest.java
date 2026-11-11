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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClasspathResourcesTest {

    private static final Class<?> ANCHOR = ClasspathResourcesTest.class;

    @TempDir
    Path tempDir;

    @Test
    void openStream_absoluteResource_returnsContent() throws Exception {
        try (InputStream in = ClasspathResources.openStream(ANCHOR, "/classpathresources-absolute.txt")) {
            String text = new String(in.readAllBytes());
            assertEquals("absolute resource\n", text);
        }
    }

    @Test
    void openStream_relativeResource_returnsContent() throws Exception {
        try (InputStream in = ClasspathResources.openStream(ANCHOR, "relative-test.txt")) {
            String text = new String(in.readAllBytes());
            assertEquals("relative resource\n", text);
        }
    }

    @Test
    void requireUrl_absoluteResource_returnsUrl() throws Exception {
        URL url = ClasspathResources.requireUrl(ANCHOR, "/classpathresources-absolute.txt");
        assertNotNull(url);
    }

    @Test
    void requireUri_absoluteResource_returnsUri() throws Exception {
        URI uri = ClasspathResources.requireUri(ANCHOR, "/classpathresources-absolute.txt");
        assertNotNull(uri);
    }

    @Test
    void copyTo_absoluteResource_copiesFile() throws Exception {
        Path target = tempDir.resolve("copied.txt");

        Path result = ClasspathResources.copyTo(ANCHOR, "/classpathresources-absolute.txt", target);

        assertEquals(target, result);
        assertTrue(Files.exists(target));
        assertEquals("absolute resource\n", Files.readString(target));
    }

    @Test
    void exists_returnsTrueForPresentResource() {
        assertTrue(ClasspathResources.exists(ANCHOR, "/classpathresources-absolute.txt"));
        assertTrue(ClasspathResources.exists(ANCHOR, "relative-test.txt"));
    }

    @Test
    void exists_returnsFalseForMissingResource() {
        assertFalse(ClasspathResources.exists(ANCHOR, "/does-not-exist.txt"));
    }

    @Test
    void openStream_missingResource_throwsFileNotFoundException() {
        assertThrows(
            FileNotFoundException.class,
            () -> ClasspathResources.openStream(ANCHOR, "/does-not-exist.txt")
        );
    }

    @Test
    void requireUrl_missingResource_throwsFileNotFoundException() {
        assertThrows(
            FileNotFoundException.class,
            () -> ClasspathResources.requireUrl(ANCHOR, "/does-not-exist.txt")
        );
    }

    @Test
    void requireUri_missingResource_throwsFileNotFoundException() {
        assertThrows(
            FileNotFoundException.class,
            () -> ClasspathResources.requireUri(ANCHOR, "/does-not-exist.txt")
        );
    }

    @Test
    void openStream_nullAnchor_throwsNullPointerException() {
        assertThrows(
            NullPointerException.class,
            () -> ClasspathResources.openStream(null, "/classpathresources-absolute.txt")
        );
    }

    @Test
    void openStream_nullName_throwsNullPointerException() {
        assertThrows(
            NullPointerException.class,
            () -> ClasspathResources.openStream(ANCHOR, null)
        );
    }

    @Test
    void openStream_blankName_throwsIllegalArgumentException() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ClasspathResources.openStream(ANCHOR, "   ")
        );
    }

    @Test
    void copyTo_nullDestination_throwsNullPointerException() {
        assertThrows(
            NullPointerException.class,
            () -> ClasspathResources.copyTo(ANCHOR, "/classpathresources-absolute.txt", null)
        );
    }
}
