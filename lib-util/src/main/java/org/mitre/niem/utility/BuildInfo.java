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
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Provides access to build metadata stored in a jar manifest.
 * <p>
 * This class reads the manifest associated with the jar containing a given class
 * and returns common implementation metadata such as:
 * </p>
 * <ul>
 *   <li>{@code Implementation-Title}</li>
 *   <li>{@code Implementation-Version}</li>
 *   <li>{@code Build-Date}</li>
 * </ul>
 * <p>
 * If the class is not loaded from a jar, this class falls back to package metadata
 * for {@code Implementation-Title} and {@code Implementation-Version}. In that case,
 * {@code Build-Date} is not available and will be {@code null}.
 * </p>
 */
public final class BuildInfo {
    private final String implementationTitle;
    private final String implementationVersion;
    private final String buildDate;

    /**
     * Creates a new build info instance.
     *
     * @param implementationTitle the implementation title, or {@code null}
     * @param implementationVersion the implementation version, or {@code null}
     * @param buildDate the build date, or {@code null}
     */
    private BuildInfo(String implementationTitle, String implementationVersion, String buildDate) {
        this.implementationTitle = implementationTitle;
        this.implementationVersion = implementationVersion;
        this.buildDate = buildDate;
    }

    /**
     * Returns the manifest {@code Implementation-Title} value.
     *
     * @return the implementation title, or {@code null} if not available
     */
    public String getImplementationTitle() {
        return implementationTitle;
    }

    /**
     * Returns the manifest {@code Implementation-Version} value.
     *
     * @return the implementation version, or {@code null} if not available
     */
    public String getImplementationVersion() {
        return implementationVersion;
    }

    /**
     * Returns the manifest {@code Build-Date} value.
     *
     * @return the build date, or {@code null} if not available
     */
    public String getBuildDate() {
        return buildDate;
    }

    /**
     * Loads build information for the jar containing the given class.
     * <p>
     * If the class was loaded from a jar file, this method reads the jar manifest.
     * If the class was not loaded from a jar, this method falls back to package metadata
     * for implementation title and version.
     * </p>
     *
     * @param type the class whose containing jar should be inspected
     * @return a {@code BuildInfo} instance, never {@code null}
     */
    public static BuildInfo forClass(Class<?> type) {
        Manifest manifest = loadManifest(type);
        if (manifest == null) {
            Package pkg = type.getPackage();
            return new BuildInfo(
                pkg != null ? pkg.getImplementationTitle() : null,
                pkg != null ? pkg.getImplementationVersion() : null,
                null
            );
        }

        Attributes attrs = manifest.getMainAttributes();
        return new BuildInfo(
            attrs.getValue("Implementation-Title"),
            attrs.getValue("Implementation-Version"),
            attrs.getValue("Build-Date")
        );
    }

    /**
     * Loads the jar manifest for the jar containing the given class.
     *
     * @param type the class whose jar manifest should be loaded
     * @return the manifest, or {@code null} if the class is not loaded from a jar
     *         or if the manifest cannot be read
     */
    private static Manifest loadManifest(Class<?> type) {
        try {
            String classFileName = type.getSimpleName() + ".class";
            URL classUrl = type.getResource(classFileName);
            if (classUrl == null) {
                return null;
            }

            URLConnection connection = classUrl.openConnection();
            if (connection instanceof JarURLConnection jarConnection) {
                return jarConnection.getManifest();
            }

            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Returns a string representation of this build info object.
     *
     * @return a string containing the implementation title, version, and build date
     */
    @Override
    public String toString() {
        return "BuildInfo{" +
            "implementationTitle='" + implementationTitle + '\'' +
            ", implementationVersion='" + implementationVersion + '\'' +
            ", buildDate='" + buildDate + '\'' +
            '}';
    }
}


