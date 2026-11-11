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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Utility methods for loading resources from the runtime classpath.
 *
 * <p>This class delegates directly to {@link Class#getResource(String)} and
 * {@link Class#getResourceAsStream(String)} using the supplied anchor class.
 * Resource name semantics are therefore exactly those defined by the JDK:
 *
 * <ul>
 *   <li>If the resource name begins with {@code /}, it is treated as absolute
 *       from the classpath root.</li>
 *   <li>If the resource name does not begin with {@code /}, it is treated as
 *       relative to the package of the anchor class.</li>
 * </ul>
 *
 * <p>No resource-name normalization is performed by this class.
 *
 * <p>This works both when running from an IDE/classes directory and from a JAR.
 * However, callers should not assume that a resolved resource corresponds to a
 * regular filesystem file. For example, a resource inside a JAR may have a
 * {@code jar:} URL or URI rather than a {@code file:} URL or URI.
 *
 * <p>Use {@link #openStream(Class, String)} when the resource only needs to be
 * read. If a real filesystem file is required by another API, use
 * {@link #copyTo(Class, String, Path)} to copy the resource to disk.
 */
public final class ClasspathResources {

    private ClasspathResources() {
        // Utility class; not instantiable.
    }

    /**
     * Opens the named resource as an input stream using the supplied anchor class.
     *
     * <p>The resource name is passed unchanged to
     * {@link Class#getResourceAsStream(String)}.
     *
     * @param anchorClass class used to resolve the resource
     * @param name resource name; semantics are those of {@link Class#getResource(String)}
     * @return input stream for the resource
     * @throws NullPointerException if {@code anchorClass} or {@code name} is null
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws FileNotFoundException if the resource cannot be found
     */
    public static InputStream openStream(Class<?> anchorClass, String name) throws IOException {
        Objects.requireNonNull(anchorClass, "anchorClass must not be null");
        requireNonBlank(name, "name must not be blank");

        InputStream in = anchorClass.getResourceAsStream(name);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + name);
        }
        return in;
    }

    /**
     * Returns the URL of the named resource using the supplied anchor class.
     *
     * <p>The resource name is passed unchanged to {@link Class#getResource(String)}.
     *
     * @param anchorClass class used to resolve the resource
     * @param name resource name; semantics are those of {@link Class#getResource(String)}
     * @return URL for the resource
     * @throws NullPointerException if {@code anchorClass} or {@code name} is null
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws FileNotFoundException if the resource cannot be found
     */
    public static URL requireUrl(Class<?> anchorClass, String name) throws IOException {
        Objects.requireNonNull(anchorClass, "anchorClass must not be null");
        requireNonBlank(name, "name must not be blank");

        URL url = anchorClass.getResource(name);
        if (url == null) {
            throw new FileNotFoundException("Resource not found: " + name);
        }
        return url;
    }

    /**
     * Returns the URI of the named resource using the supplied anchor class.
     *
     * <p>The resource name is passed unchanged to {@link Class#getResource(String)}.
     *
     * <p>Note that the returned URI is not guaranteed to identify a regular
     * filesystem path. For example, resources packaged in a JAR commonly resolve
     * to a {@code jar:} URI.
     *
     * @param anchorClass class used to resolve the resource
     * @param name resource name; semantics are those of {@link Class#getResource(String)}
     * @return URI for the resource
     * @throws NullPointerException if {@code anchorClass} or {@code name} is null
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws FileNotFoundException if the resource cannot be found
     * @throws IOException if the resource URL cannot be converted to a URI
     */
    public static URI requireUri(Class<?> anchorClass, String name) throws IOException {
        try {
            return requireUrl(anchorClass, name).toURI();
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid URI for resource: " + name, ex);
        }
    }

    /**
     * Copies the named resource to the given destination path.
     *
     * <p>The resource name is passed unchanged to
     * {@link Class#getResourceAsStream(String)}. Parent directories for the
     * destination path are created if necessary. If the destination already
     * exists, it is replaced.
     *
     * @param anchorClass class used to resolve the resource
     * @param name resource name; semantics are those of {@link Class#getResource(String)}
     * @param destination destination path
     * @return the destination path
     * @throws NullPointerException if {@code anchorClass}, {@code name}, or
     *         {@code destination} is null
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IOException if the resource cannot be found or copied
     */
    public static Path copyTo(Class<?> anchorClass, String name, Path destination) throws IOException {
        Objects.requireNonNull(destination, "destination must not be null");

        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (InputStream in = openStream(anchorClass, name)) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        return destination;
    }

    /**
     * Returns whether the named resource can be resolved using the supplied anchor class.
     *
     * <p>The resource name is passed unchanged to {@link Class#getResource(String)}.
     *
     * @param anchorClass class used to resolve the resource
     * @param name resource name; semantics are those of {@link Class#getResource(String)}
     * @return {@code true} if the resource exists; otherwise {@code false}
     * @throws NullPointerException if {@code anchorClass} or {@code name} is null
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public static boolean exists(Class<?> anchorClass, String name) {
        Objects.requireNonNull(anchorClass, "anchorClass must not be null");
        requireNonBlank(name, "name must not be blank");
        return anchorClass.getResource(name) != null;
    }

    private static void requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}

