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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utility methods for writing files via a temporary sibling file and then
 * moving the completed file into place.
 *
 * This reduces duplication in commands that want safer output behavior than
 * writing directly to the destination path.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a> 
 */
public final class AtomicPathWriter {

    private AtomicPathWriter() { }

    @FunctionalInterface
    public interface IOWriterAction {
        void write(Writer writer) throws IOException;
    }

    @FunctionalInterface
    public interface IOStreamAction {
        void write(OutputStream stream) throws IOException;
    }

    /**
     * Write text to a temporary file in the target directory and then move it
     * into place, replacing any existing target.
     *
     * @param targetPath destination path
     * @param charset output character set
     * @param action callback that writes the file content
     * @throws IOException on write or move failure
     */
    public static void writeAtomically(
        Path targetPath,
        Charset charset,
        IOWriterAction action
    ) throws IOException {

        Path absoluteTarget = targetPath.toAbsolutePath();
        Path dir = absoluteTarget.getParent();
        if (dir == null) {
            dir = Path.of(".").toAbsolutePath().normalize();
        }

        String prefix = absoluteTarget.getFileName().toString() + ".";
        Path tempPath = Files.createTempFile(dir, prefix, ".tmp");

        boolean moved = false;
        try {
            try (Writer writer = Files.newBufferedWriter(tempPath, charset)) {
                action.write(writer);
            }
            moveReplace(tempPath, absoluteTarget);
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(tempPath);
            }
        }
    }

    /**
     * Write binary content to a temporary file in the target directory and then
     * move it into place, replacing any existing target.
     *
     * @param targetPath destination path
     * @param action callback that writes the file content
     * @throws IOException on write or move failure
     */
    public static void writeAtomically(
        Path targetPath,
        IOStreamAction action
    ) throws IOException {

        Path absoluteTarget = targetPath.toAbsolutePath();
        Path dir = absoluteTarget.getParent();
        if (dir == null) {
            dir = Path.of(".").toAbsolutePath().normalize();
        }

        String prefix = absoluteTarget.getFileName().toString() + ".";
        Path tempPath = Files.createTempFile(dir, prefix, ".tmp");

        boolean moved = false;
        try {
            try (OutputStream stream = Files.newOutputStream(tempPath)) {
                action.write(stream);
            }
            moveReplace(tempPath, absoluteTarget);
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(tempPath);
            }
        }
    }

    /**
     * Move source to target, replacing any existing target. Uses atomic move
     * when supported by the platform/filesystem, with fallback to a normal
     * replace-existing move.
     *
     * @param source source path
     * @param target target path
     * @throws IOException on move failure
     */
    public static void moveReplace(Path source, Path target) throws IOException {
        try {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING
            );
        }
    }
}
