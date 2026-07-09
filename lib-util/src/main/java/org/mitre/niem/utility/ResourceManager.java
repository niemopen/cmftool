/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2025-6 The MITRE Corporation.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility for loading project resources from the runtime classpath.
 *
 * Resource names may be supplied with or without a leading "/".
 * Internally they are resolved as absolute classpath resources.
 *
 * This works both when running from an IDE/classes directory and from a JAR.
 */
public class ResourceManager {

    private static final Logger LOG = LogManager.getLogger(ResourceManager.class);

    private final Class<?> anchorClass;

    public ResourceManager() {
        this(ResourceManager.class);
    }

    public ResourceManager(Class<?> anchorClass) {
        this.anchorClass = Objects.requireNonNull(anchorClass, "anchorClass must not be null");
    }

    /**
     * Returns an InputStream for the named classpath resource.
     *
     * @param name resource name, with or without leading "/"
     * @return input stream for the resource
     * @throws IOException if the resource cannot be found or opened
     */
    public InputStream getResourceStream(String name) throws IOException {
        var url = requireResourceUrl(name);
        return url.openStream();
    }

    /**
     * Copies the named resource to the given output file.
     *
     * @param name resource name, with or without leading "/"
     * @param outFile destination file
     * @throws IOException if the resource cannot be found or copied
     */
    public void copyResourceToFile(String name, File outFile) throws IOException {
        Objects.requireNonNull(outFile, "outFile must not be null");

        var outPath = outFile.toPath();
        var parent = outPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (var in = getResourceStream(name)) {
            Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Returns a URI for the named resource, or null if it cannot be resolved.
     *
     * @param name resource name, with or without leading "/"
     * @return resource URI, or null on failure
     */
    public URI getResourceURI(String name) {
        try {
            return requireResourceUrl(name).toURI();
        } catch (IOException | URISyntaxException ex) {
            LOG.error("Unable to resolve resource URI for {}: {}", name, ex.getMessage());
            return null;
        }
    }

    private URL requireResourceUrl(String name) throws IOException {
        var normName = normalizeName(name);
        var url = anchorClass.getResource(normName);
        if (url == null) {
            throw new FileNotFoundException("Resource not found: " + normName);
        }
        return url;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("resource name must not be null or blank");
        }
        return name.startsWith("/") ? name : "/" + name;
    }
}
