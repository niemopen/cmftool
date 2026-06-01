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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Helper for commands that write a directory tree by first staging output in a
 * temporary sibling directory and then promoting that directory into place.
 *
 * <p>This is safer than writing directly into the target directory, but note
 * that replacing an existing non-empty directory is not fully atomic on all
 * platforms and filesystems.</p>
 */
public final class StagedDirectoryWriter {

    private StagedDirectoryWriter() { }

    @FunctionalInterface
    public interface DirectoryAction {
        void write(Path stagingDir) throws Exception;
    }

    /**
     * Write a new directory tree. The target directory must not already exist.
     *
     * @param targetDir destination directory
     * @param action callback that writes the directory contents into the staging directory
     * @throws Exception on write, move, or cleanup failure
     */
    public static void writeToNewDirectory(Path targetDir, DirectoryAction action) throws Exception {
        Path absoluteTarget = normalizeTarget(targetDir);
        Path parent = targetParent(absoluteTarget);

        if (Files.exists(absoluteTarget)) {
            throw new IOException("Target directory already exists: " + absoluteTarget);
        }

        Files.createDirectories(parent);
        Path stagingDir = createSiblingTempDirectory(absoluteTarget);

        boolean promoted = false;
        try {
            action.write(stagingDir);
            moveDirectory(stagingDir, absoluteTarget);
            promoted = true;
        } finally {
            if (!promoted) {
                deleteRecursivelyIfExists(stagingDir);
            }
        }
    }

    /**
     * Replace a target directory by first writing a staged directory and then
     * promoting it into place.
     *
     * <p>If the target does not exist, this behaves like {@link #writeToNewDirectory(Path, DirectoryAction)}.</p>
     *
     * <p>If {@code backupSuffix} is non-null and non-blank, the displaced target
     * directory is preserved with that suffix. Otherwise the displaced directory
     * is deleted after successful promotion.</p>
     *
     * @param targetDir destination directory
     * @param backupSuffix suffix for preserved backup, for example "bak"; null/blank means delete old target after success
     * @param action callback that writes the directory contents into the staging directory
     * @throws Exception on write, move, rollback, or cleanup failure
     */
    public static void replaceDirectory(Path targetDir, String backupSuffix, DirectoryAction action) throws Exception {
        Path absoluteTarget = normalizeTarget(targetDir);
        Path parent = targetParent(absoluteTarget);

        Files.createDirectories(parent);

        if (!Files.exists(absoluteTarget)) {
            writeToNewDirectory(absoluteTarget, action);
            return;
        }
        if (!Files.isDirectory(absoluteTarget)) {
            throw new IOException("Target path is not a directory: " + absoluteTarget);
        }

        Path stagingDir = createSiblingTempDirectory(absoluteTarget);
        Path displacedDir = null;
        boolean promoted = false;
        boolean keepBackup = backupSuffix != null && !backupSuffix.isBlank();

        try {
            action.write(stagingDir);

            displacedDir = keepBackup
                ? createSiblingBackupPath(absoluteTarget, backupSuffix)
                : createUniqueSiblingPath(absoluteTarget, "old");

            moveDirectory(absoluteTarget, displacedDir);

            try {
                moveDirectory(stagingDir, absoluteTarget);
                promoted = true;
            } catch (Exception ex) {
                // Best-effort rollback
                try {
                    if (!Files.exists(absoluteTarget) && Files.exists(displacedDir)) {
                        moveDirectory(displacedDir, absoluteTarget);
                    }
                } catch (Exception rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                throw ex;
            }

            if (!keepBackup) {
                deleteRecursivelyIfExists(displacedDir);
            }
        } finally {
            if (!promoted) {
                deleteRecursivelyIfExists(stagingDir);
            }
        }
    }

    /**
     * Returns true if the directory exists and contains at least one entry.
     *
     * @param dir directory path
     * @return true if non-empty, false otherwise
     * @throws IOException on I/O failure
     */
    public static boolean isNonEmptyDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (var s = Files.list(dir)) {
            return s.findAny().isPresent();
        }
    }

    /**
     * Recursively deletes a directory tree if it exists.
     *
     * @param path file or directory to delete
     * @throws IOException on delete failure
     */
    public static void deleteRecursivelyIfExists(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Moves a directory into place. Uses atomic move when supported, with
     * fallback to a normal move.
     *
     * @param source source directory
     * @param target target directory
     * @throws IOException on move failure
     */
    public static void moveDirectory(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }

    /**
     * Creates a temporary sibling directory near the target.
     *
     * @param targetPath target path whose parent directory will contain the temp directory
     * @return staging directory path
     * @throws IOException on failure
     */
    public static Path createSiblingTempDirectory(Path targetPath) throws IOException {
        Path absoluteTarget = normalizeTarget(targetPath);
        Path parent = targetParent(absoluteTarget);
        Files.createDirectories(parent);

        String base = absoluteTarget.getFileName().toString();
        return Files.createTempDirectory(parent, base + ".");
    }

    /**
     * Creates a unique sibling backup path such as "name.bak", "name.bak00", etc.
     *
     * @param targetPath target being backed up
     * @param suffix backup suffix
     * @return unique backup path
     * @throws IOException on failure
     */
    public static Path createSiblingBackupPath(Path targetPath, String suffix) throws IOException {
        return createUniqueSiblingPath(targetPath, suffix);
    }

    /**
     * Creates a unique sibling path such as "name.suffix", "name.suffix00", etc.
     * The returned path does not exist.
     *
     * @param targetPath target path
     * @param suffix suffix to append
     * @return unique sibling path that does not exist
     * @throws IOException on failure
     */
    public static Path createUniqueSiblingPath(Path targetPath, String suffix) throws IOException {
        Path absoluteTarget = normalizeTarget(targetPath);
        Path parent = targetParent(absoluteTarget);
        Files.createDirectories(parent);

        String base = absoluteTarget.getFileName().toString();
        int tries = 0;
        Path candidate = parent.resolve(base + "." + suffix);

        while (Files.exists(candidate)) {
            candidate = parent.resolve(String.format("%s.%s%02d", base, suffix, tries++));
        }
        return candidate;
    }

    private static Path normalizeTarget(Path targetPath) {
        return targetPath.toAbsolutePath().normalize();
    }

    private static Path targetParent(Path absoluteTarget) {
        Path parent = absoluteTarget.getParent();
        return parent != null ? parent : Path.of(".").toAbsolutePath().normalize();
    }
}
