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

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BuildInfoTest {

    @Test
    void readsImplementationTitleVersionAndBuildDateFromJarManifest() throws Exception {
        Path jarFile = Files.createTempFile("buildinfo-test-", ".jar");

        Class<?> fixtureType = ManifestFixture.class;
        String classEntryName = fixtureType.getName().replace('.', '/') + ".class";

        byte[] classBytes;
        try (InputStream in = fixtureType.getClassLoader().getResourceAsStream(classEntryName)) {
            assertNotNull(in, "Could not find test fixture class bytes: " + classEntryName);
            classBytes = in.readAllBytes();
        }

        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("Implementation-Title", "SCHEval");
        attrs.putValue("Implementation-Version", "1.1-alpha.7");
        attrs.putValue("Build-Date", "2026-07-01");

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            JarEntry entry = new JarEntry(classEntryName);
            jos.putNextEntry(entry);
            jos.write(classBytes);
            jos.closeEntry();
        }

        try (URLClassLoader loader = new URLClassLoader(
            new URL[] { jarFile.toUri().toURL() },
            ClassLoader.getPlatformClassLoader()
        )) {
            Class<?> fixtureClass = loader.loadClass(fixtureType.getName());

            BuildInfo info = BuildInfo.forClass(fixtureClass);

            assertEquals("SCHEval", info.getImplementationTitle());
            assertEquals("1.1-alpha.7", info.getImplementationVersion());
            assertEquals("2026-07-01", info.getBuildDate());
        }
    }
}


