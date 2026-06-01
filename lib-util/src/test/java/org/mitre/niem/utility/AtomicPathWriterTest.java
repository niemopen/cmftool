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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AtomicPathWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writeAtomically_writerWritesTextFile() throws IOException {
        Path target = tempDir.resolve("out.txt");

        AtomicPathWriter.writeAtomically(target, StandardCharsets.UTF_8, writer -> {
            writer.write("hello");
            writer.write(System.lineSeparator());
            writer.write("world");
        });

        assertEquals("hello" + System.lineSeparator() + "world", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void writeAtomically_streamWritesBinaryFile() throws IOException {
        Path target = tempDir.resolve("out.bin");
        byte[] expected = new byte[] {0, 1, 2, 3, 127, -1};

        AtomicPathWriter.writeAtomically(target, out -> {
            out.write(expected);
        });

        assertArrayEquals(expected, Files.readAllBytes(target));
    }

    @Test
    void writeAtomically_replacesExistingTarget() throws IOException {
        Path target = tempDir.resolve("replace.txt");
        Files.writeString(target, "old", StandardCharsets.UTF_8);

        AtomicPathWriter.writeAtomically(target, StandardCharsets.UTF_8, writer -> {
            writer.write("new");
        });

        assertEquals("new", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void writeAtomically_writerFailureLeavesExistingTargetUnchangedAndDeletesTempFile() throws IOException {
        Path target = tempDir.resolve("fail.txt");
        Files.writeString(target, "original", StandardCharsets.UTF_8);

        long before = countTempFilesForTarget(target);

        IOException ex = assertThrows(IOException.class, () ->
            AtomicPathWriter.writeAtomically(target, StandardCharsets.UTF_8, writer -> {
                writer.write("partial");
                throw new IOException("boom");
            })
        );

        assertEquals("boom", ex.getMessage());
        assertEquals("original", Files.readString(target, StandardCharsets.UTF_8));
        assertEquals(before, countTempFilesForTarget(target));
    }

    @Test
    void writeAtomically_streamFailureDoesNotCreateTargetAndDeletesTempFile() throws IOException {
        Path target = tempDir.resolve("fail.bin");

        long before = countTempFilesForTarget(target);

        IOException ex = assertThrows(IOException.class, () ->
            AtomicPathWriter.writeAtomically(target, out -> {
                out.write(new byte[] {1, 2, 3});
                throw new IOException("boom");
            })
        );

        assertEquals("boom", ex.getMessage());
        assertFalse(Files.exists(target));
        assertEquals(before, countTempFilesForTarget(target));
    }

    @Test
    void moveReplace_replacesExistingTarget() throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");

        Files.writeString(source, "from-source", StandardCharsets.UTF_8);
        Files.writeString(target, "from-target", StandardCharsets.UTF_8);

        AtomicPathWriter.moveReplace(source, target);

        assertFalse(Files.exists(source));
        assertEquals("from-source", Files.readString(target, StandardCharsets.UTF_8));
    }

    private long countTempFilesForTarget(Path target) throws IOException {
        String prefix = target.getFileName().toString() + ".";
        try (Stream<Path> s = Files.list(target.getParent())) {
            return s
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.startsWith(prefix) && name.endsWith(".tmp"))
                .count();
        }
    }
}
