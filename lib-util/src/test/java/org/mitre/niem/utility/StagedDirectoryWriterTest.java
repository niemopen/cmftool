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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StagedDirectoryWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writeToNewDirectory_writesDirectoryTree() throws Exception {
        Path target = tempDir.resolve("out");

        StagedDirectoryWriter.writeToNewDirectory(target, stagingDir -> {
            writeText(stagingDir.resolve("a.txt"), "alpha");
            writeText(stagingDir.resolve("sub").resolve("b.txt"), "beta");
        });

        assertTrue(Files.isDirectory(target));
        assertEquals("alpha", Files.readString(target.resolve("a.txt")));
        assertEquals("beta", Files.readString(target.resolve("sub").resolve("b.txt")));
    }

    @Test
    void writeToNewDirectory_failsIfTargetAlreadyExists() throws Exception {
        Path target = tempDir.resolve("out");
        Files.createDirectories(target);

        IOException ex = assertThrows(IOException.class, () ->
            StagedDirectoryWriter.writeToNewDirectory(target, stagingDir -> {
                writeText(stagingDir.resolve("a.txt"), "alpha");
            })
        );

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void writeToNewDirectory_cleansUpOnActionFailure() throws Exception {
        Path target = tempDir.resolve("out");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            StagedDirectoryWriter.writeToNewDirectory(target, stagingDir -> {
                writeText(stagingDir.resolve("a.txt"), "alpha");
                throw new RuntimeException("boom");
            })
        );

        assertEquals("boom", ex.getMessage());
        assertFalse(Files.exists(target));
    }

    @Test
    void replaceDirectory_createsTargetWhenAbsent() throws Exception {
        Path target = tempDir.resolve("out");

        StagedDirectoryWriter.replaceDirectory(target, "bak", stagingDir -> {
            writeText(stagingDir.resolve("new.txt"), "new");
        });

        assertTrue(Files.isDirectory(target));
        assertEquals("new", Files.readString(target.resolve("new.txt")));
    }

    @Test
    void replaceDirectory_replacesExistingTargetAndKeepsBackup() throws Exception {
        Path target = tempDir.resolve("out");
        Files.createDirectories(target);
        writeText(target.resolve("old.txt"), "old");

        StagedDirectoryWriter.replaceDirectory(target, "bak", stagingDir -> {
            writeText(stagingDir.resolve("new.txt"), "new");
        });

        assertTrue(Files.isDirectory(target));
        assertFalse(Files.exists(target.resolve("old.txt")));
        assertEquals("new", Files.readString(target.resolve("new.txt")));

        Path backup = tempDir.resolve("out.bak");
        assertTrue(Files.isDirectory(backup));
        assertEquals("old", Files.readString(backup.resolve("old.txt")));
    }

    @Test
    void replaceDirectory_replacesExistingTargetAndDeletesOldTreeWhenNoBackupRequested() throws Exception {
        Path target = tempDir.resolve("out");
        Files.createDirectories(target);
        writeText(target.resolve("old.txt"), "old");

        StagedDirectoryWriter.replaceDirectory(target, "", stagingDir -> {
            writeText(stagingDir.resolve("new.txt"), "new");
        });

        assertTrue(Files.isDirectory(target));
        assertEquals("new", Files.readString(target.resolve("new.txt")));
        assertFalse(Files.exists(target.resolve("old.txt")));

        try (var s = Files.list(tempDir)) {
            long oldDirs = s
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.startsWith("out.old"))
                .count();
            assertEquals(0, oldDirs);
        }
    }

    @Test
    void replaceDirectory_actionFailureLeavesOriginalTargetUnchanged() throws Exception {
        Path target = tempDir.resolve("out");
        Files.createDirectories(target);
        writeText(target.resolve("old.txt"), "old");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            StagedDirectoryWriter.replaceDirectory(target, "bak", stagingDir -> {
                writeText(stagingDir.resolve("new.txt"), "new");
                throw new RuntimeException("boom");
            })
        );

        assertEquals("boom", ex.getMessage());
        assertTrue(Files.isDirectory(target));
        assertEquals("old", Files.readString(target.resolve("old.txt")));
        assertFalse(Files.exists(target.resolve("new.txt")));
        assertFalse(Files.exists(tempDir.resolve("out.bak")));
    }

    @Test
    void isNonEmptyDirectory_reportsExpectedValues() throws Exception {
        Path emptyDir = tempDir.resolve("empty");
        Path nonEmptyDir = tempDir.resolve("nonempty");
        Path file = tempDir.resolve("file.txt");

        Files.createDirectories(emptyDir);
        Files.createDirectories(nonEmptyDir);
        writeText(nonEmptyDir.resolve("x.txt"), "x");
        writeText(file, "f");

        assertFalse(StagedDirectoryWriter.isNonEmptyDirectory(emptyDir));
        assertTrue(StagedDirectoryWriter.isNonEmptyDirectory(nonEmptyDir));
        assertFalse(StagedDirectoryWriter.isNonEmptyDirectory(file));
    }

    @Test
    void deleteRecursivelyIfExists_deletesDirectoryTree() throws Exception {
        Path dir = tempDir.resolve("tree");
        writeText(dir.resolve("a.txt"), "alpha");
        writeText(dir.resolve("sub").resolve("b.txt"), "beta");

        assertTrue(Files.exists(dir));
        StagedDirectoryWriter.deleteRecursivelyIfExists(dir);
        assertFalse(Files.exists(dir));

        StagedDirectoryWriter.deleteRecursivelyIfExists(dir);
        assertFalse(Files.exists(dir));
    }

    @Test
    void createSiblingBackupPath_returnsUniqueNames() throws Exception {
        Path target = tempDir.resolve("out");
        Files.createDirectories(target);
        Files.createDirectories(tempDir.resolve("out.bak"));

        Path backup = StagedDirectoryWriter.createSiblingBackupPath(target, "bak");

        assertEquals(tempDir, backup.getParent());
        assertTrue(backup.getFileName().toString().startsWith("out.bak"));
        assertFalse(Files.exists(backup));
        assertFalse(backup.equals(tempDir.resolve("out.bak")));
    }

    @Test
    void createUniqueSiblingPath_returnsUniqueNames() throws Exception {
        Path target = tempDir.resolve("out");
        Files.createDirectories(target);
        Files.createDirectories(tempDir.resolve("out.old"));

        Path sibling = StagedDirectoryWriter.createUniqueSiblingPath(target, "old");

        assertEquals(tempDir, sibling.getParent());
        assertTrue(sibling.getFileName().toString().startsWith("out.old"));
        assertFalse(Files.exists(sibling));
        assertFalse(sibling.equals(tempDir.resolve("out.old")));
    }

    @Test
    void createSiblingTempDirectory_createsDirectoryBesideTarget() throws Exception {
        Path target = tempDir.resolve("out");

        Path staging = StagedDirectoryWriter.createSiblingTempDirectory(target);
        try {
            assertTrue(Files.isDirectory(staging));
            assertEquals(tempDir, staging.getParent());
            assertTrue(staging.getFileName().toString().startsWith("out."));
        } finally {
            StagedDirectoryWriter.deleteRecursivelyIfExists(staging);
        }
    }

    private static void writeText(Path path, String value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, value);
    }
}
