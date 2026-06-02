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
package org.mitre.niem.translate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.utility.AtomicPathWriter;
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
    name = "j2r",
    description = "convert NIEM JSON message to RDF",
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class CmdJSONtoRDF implements Callable<Integer> {

    @Option(
        names = {"-c", "--context"},
        paramLabel = "<path>",
        description = "use context from this JSON file"
    )
    private Path contextPath = null;

    @Option(
        names = {"-m", "--model"},
        paramLabel = "<path>",
        description = "use message model in this CMF file"
    )
    private Path modelPath = null;

    @Option(
        names = {"-o", "--output"},
        paramLabel = "<path>",
        description = "write RDF output to this file, or '-' for stdout"
    )
    private Path outputPath = null;

    @Parameters(
        index = "0",
        arity = "1",
        paramLabel = "message.json",
        description = "message JSON file"
    )
    private Path msgPath;

    CmdJSONtoRDF() {
    }

    public static void main(String[] args) {
        int rc = new CommandLine(new CmdJSONtoRDF()).execute(args);
        System.exit(rc);
    }

    @Override
    public Integer call() {
        // Read context if provided
        String contextS = null;
        if (contextPath != null) {
            int rc = validateReadableFile(contextPath, "context file");
            if (rc != 0) {
                return rc;
            }
            try {
                contextS = Files.readString(contextPath, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                System.err.println(String.format(
                    "Can't read context file %s: %s",
                    contextPath,
                    ex.getMessage()
                ));
                return 1;
            }
        }

        // Read model if provided
        Model model = null;
        if (modelPath != null) {
            int rc = validateReadableFile(modelPath, "model file");
            if (rc != 0) {
                return rc;
            }
            try {
                var mr = new ModelXMLReader();
                model = mr.readFiles(modelPath.toFile());
            } catch (Exception ex) {
                System.err.println(String.format(
                    "Can't read model from %s: %s",
                    modelPath,
                    ex.getMessage()
                ));
                return 1;
            }
            if (model == null) {
                System.err.println("Can't read model from " + modelPath);
                return 1;
            }
        }

        // Read message
        int rc = validateReadableFile(msgPath, "message file");
        if (rc != 0) {
            return rc;
        }

        // Make sure output file is writable
        if (!isStdout(outputPath)) {
            rc = validateOutputPath(outputPath);
            if (rc != 0) {
                return rc;
            }
        }

        // Convert and write to output
        var cvt = new JSONMsgToRDF();
        try {
            cvt.setContext(contextS);
            cvt.setModel(model);

            if (isStdout(outputPath)) {
                try (BufferedReader msgR = Files.newBufferedReader(msgPath, StandardCharsets.UTF_8)) {
                    var ow = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
                    cvt.convert(msgR, ow);
                    ow.flush();
                }
            } else {
                try {
                    AtomicPathWriter.writeAtomically(outputPath, StandardCharsets.UTF_8, ow -> {
                        try (BufferedReader msgR = Files.newBufferedReader(msgPath, StandardCharsets.UTF_8)) {
                            cvt.convert(msgR, ow);
                        } catch (NIEMTranException ex) {
                            throw new WrappedNIEMTranException(ex);
                        }
                    });
                } catch (WrappedNIEMTranException ex) {
                    throw (NIEMTranException) ex.getCause();
                }
            }
        } catch (IOException ex) {
            System.err.println("IO error writing RDF output: " + ex.getMessage());
            return 1;
        } catch (NIEMTranException ex) {
            System.err.println("Conversion error: " + ex.getMessage());
            return 1;
        }

        return 0;
    }

    private boolean isStdout(Path path) {
        return path == null || "-".equals(path.toString());
    }

    private int validateReadableFile(Path path, String label) {
        if (!Files.exists(path)) {
            System.err.println(label + " does not exist: " + path);
            return 2;
        }
        if (!Files.isRegularFile(path)) {
            System.err.println(label + " is not a regular file: " + path);
            return 2;
        }
        if (!Files.isReadable(path)) {
            System.err.println(label + " is not readable: " + path);
            return 2;
        }
        return 0;
    }

    private int validateOutputPath(Path path) {
        Path abs = path.toAbsolutePath().normalize();
        Path parent = abs.getParent();
        if (parent != null && !Files.exists(parent)) {
            System.err.println("Output directory does not exist: " + parent);
            return 2;
        }
        if (Files.exists(abs) && Files.isDirectory(abs)) {
            System.err.println("Output path is a directory: " + abs);
            return 2;
        }
        return 0;
    }

    private static final class WrappedNIEMTranException extends IOException {
        WrappedNIEMTranException(NIEMTranException cause) {
            super(cause.getMessage(), cause);
        }
    }
}
