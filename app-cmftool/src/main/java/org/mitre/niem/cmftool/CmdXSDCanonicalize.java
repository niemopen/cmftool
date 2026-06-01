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
package org.mitre.niem.cmftool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.mitre.niem.utility.AtomicPathWriter;
import org.mitre.niem.xml.CanonicalXSD;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_ALL;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
@Command(
    name = "xcanon",
    description = {
        "canonicalize an XML Schema document",
        "Use '--' before filenames beginning with '-'.",
        "Use '-i' for in-place canonicalization, or '-i bak' to keep the original as a backup."
    },
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class CmdXSDCanonicalize implements Callable<Integer> {

    @Option(
        names = {"-i", "--in-place"},
        arity = "0..1",
        fallbackValue = "",
        paramLabel = "bak",
        description = "canonicalize in place; optional backup suffix, eg. '-i bak'"
    )
    private String backSuf = null;

    @Option(
        names = {"-o", "--output"},
        description = "file for converter output, or '-' for stdout"
    )
    private Path objFile = null;

    @Parameters(
        index = "0",
        arity = "1",
        paramLabel = "schemaDoc.xsd",
        description = "XML Schema document; use '--' before filenames beginning with '-'"
    )
    private Path inputPath;

    public static void main(String[] args) {
        int rc = new CommandLine(new CmdXSDCanonicalize()).execute(args);
        System.exit(rc);
    }

    @Override
    public Integer call() {

        boolean inPlace = backSuf != null;

        if (inPlace && objFile != null) {
            System.err.println("-i and -o options are in conflict");
            return 2;
        }

        // Make sure the Xerces parser can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println(ex.getMessage());
            return 1;
        }

        // Make sure the input document can be read
        int inputValidation = validateReadableFile(inputPath, "schema document");
        if (inputValidation != 0) {
            return inputValidation;
        }

        // Reject explicit output path equal to input path
        if (!inPlace && !isStdout(objFile)) {
            try {
                if (samePath(inputPath, objFile)) {
                    System.err.println("Input and output files must be different: " + inputPath);
                    return 2;
                }
            } catch (IOException ex) {
                System.err.println("IO error: " + ex.getMessage());
                return 1;
            }
        }

        try {
            if (inPlace) {
                writeCanonicalInPlace(inputPath, backSuf);
            } else if (isStdout(objFile)) {
                writeCanonicalToStdout(inputPath);
            } else {
                writeCanonicalAtomically(inputPath, objFile);
            }
        } catch (ParserConfigurationException | SAXException | TransformerException ex) {
            System.err.println(String.format("%s error: %s", ex.getClass().getName(), ex.getMessage()));
            return 1;
        } catch (IOException ex) {
            System.err.println(String.format("IO error: %s", ex.getMessage()));
            return 1;
        }

        return 0;
    }

    private void writeCanonicalToStdout(Path inPath)
        throws IOException, ParserConfigurationException, SAXException, TransformerException {

        try (InputStream is = Files.newInputStream(inPath)) {
            var ow = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
            CanonicalXSD.canonicalize(is, ow);
            ow.flush();
        }
    }

    private void writeCanonicalAtomically(Path inPath, Path outPath)
        throws IOException, ParserConfigurationException, SAXException, TransformerException {

        try {
            AtomicPathWriter.writeAtomically(outPath, StandardCharsets.UTF_8, ow -> {
                try (InputStream is = Files.newInputStream(inPath)) {
                    CanonicalXSD.canonicalize(is, ow);
                } catch (ParserConfigurationException | SAXException | TransformerException ex) {
                    throw new WrappedCanonicalizeException(ex);
                }
            });
        } catch (WrappedCanonicalizeException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ParserConfigurationException pce) {
                throw pce;
            }
            if (cause instanceof SAXException se) {
                throw se;
            }
            if (cause instanceof TransformerException te) {
                throw te;
            }
            throw ex;
        }
    }

    private void writeCanonicalInPlace(Path inPath, String suffix)
        throws IOException, ParserConfigurationException, SAXException, TransformerException {

        Path absoluteInput = inPath.toAbsolutePath();
        Path dir = absoluteInput.getParent();
        if (dir == null) {
            dir = Path.of(".").toAbsolutePath().normalize();
        }

        String prefix = absoluteInput.getFileName().toString() + ".";
        Path tempPath = Files.createTempFile(dir, prefix, ".tmp");
        boolean moved = false;

        try {
            // Write canonical XSD to output writer
            try (InputStream is = Files.newInputStream(absoluteInput);
                 Writer ow = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                CanonicalXSD.canonicalize(is, ow);
            }

            // Rename input file to backup file, if one is desired
            if (!suffix.isBlank()) {
                Path backPath = createSufPath(absoluteInput, suffix);
                AtomicPathWriter.moveReplace(absoluteInput, backPath);

                try {
                    AtomicPathWriter.moveReplace(tempPath, absoluteInput);
                } catch (IOException ex) {
                    try {
                        AtomicPathWriter.moveReplace(backPath, absoluteInput);
                    } catch (IOException ignored) {
                    }
                    throw ex;
                }
            }
            // Otherwise replace the input file
            else {
                AtomicPathWriter.moveReplace(tempPath, absoluteInput);
            }

            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(tempPath);
            }
        }
    }

    private int validateReadableFile(Path path, String label) {
        if (!Files.exists(path)) {
            System.err.println("Error reading " + label + ": file does not exist: " + path);
            return 2;
        }
        if (!Files.isRegularFile(path)) {
            System.err.println("Error reading " + label + ": not a regular file: " + path);
            return 2;
        }
        if (!Files.isReadable(path)) {
            System.err.println("Error reading " + label + ": file is not readable: " + path);
            return 2;
        }
        return 0;
    }

    private boolean isStdout(Path path) {
        return path == null || "-".equals(path.toString());
    }

    private boolean samePath(Path p1, Path p2) throws IOException {
        Path a1 = p1.toAbsolutePath().normalize();
        Path a2 = p2.toAbsolutePath().normalize();

        if (a1.equals(a2)) {
            return true;
        }
        if (Files.exists(a1) && Files.exists(a2)) {
            return Files.isSameFile(a1, a2);
        }
        return false;
    }

    private Path createSufPath(Path path, String suf) {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) {
            parent = Path.of(".").toAbsolutePath().normalize();
        }

        String base = absolute.getFileName().toString();
        int tries = 0;
        Path candidate = parent.resolve(base + "." + suf);

        while (Files.exists(candidate)) {
            candidate = parent.resolve(String.format("%s.%s%02d", base, suf, tries++));
        }
        return candidate;
    }

    private static final class WrappedCanonicalizeException extends IOException {
        WrappedCanonicalizeException(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }
}
