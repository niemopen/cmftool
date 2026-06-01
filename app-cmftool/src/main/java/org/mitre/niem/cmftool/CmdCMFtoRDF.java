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

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.rdf.ModelToRDF;
import org.mitre.niem.utility.AtomicPathWriter;
import org.mitre.niem.xml.ParserBootstrap;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_ALL;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Command(
    name = "m2m",
    description = {
        "generate model RDF from CMF",
        "Use '--' before model filenames beginning with '-'.",
        "Use '-o -' to write RDF to standard output."
    },
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class CmdCMFtoRDF implements Callable<Integer> {

    @Option(
        names = {"-o", "--output"},
        paramLabel = "<path>",
        description = "output file, or '-' for stdout (the default)"
    )
    private Path outputPath = null;

    @Parameters(
        arity = "1..*",
        paramLabel = "modelFile.cmf...",
        description = "one or more model files; use '--' before filenames beginning with '-'"
    )
    private List<Path> modelPaths;

    @Override
    public Integer call() {
        // Make sure the Xerces parsers can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Internal parser error: " + ex.getMessage());
            return 1;
        }

        // Validate model input paths before attempting to read them
        int pathValidation = validateModelPaths(modelPaths);
        if (pathValidation != 0) {
            return pathValidation;
        }

        // Read the model object from the model instance file
        // Read the model object from the model file(s)
        final var mr = new ModelXMLReader();
        final Model model;
        try {
            model = mr.readFiles(
                modelPaths.stream()
                    .map(Path::toFile)
                    .collect(Collectors.toList())
            );
        } catch (Exception ex) {
            System.err.println(
                "Can't read model file(s) "
                    + modelPaths.stream().map(Path::toString).collect(Collectors.joining(", "))
                    + ": " + ex.getMessage()
            );
            return 1;
        }

        // Generate model RDF
        try {
            var rdf = new ModelToRDF(model);
            if (isStdout(outputPath)) {
                // Write directly to stdout; do not close System.out.
                var ow = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
                rdf.writeRDF(ow);
                ow.flush();
            } else {
                AtomicPathWriter.writeAtomically(outputPath, StandardCharsets.UTF_8, ow -> {
                    rdf.writeRDF(ow);
                });
            }
        } catch (Exception ex) {
            System.err.println("Can't generate RDF: " + ex.getMessage());
            return 1;
        }

        return 0;
    }

    private int validateModelPaths(List<Path> paths) {
        for (var path : paths) {
            if (!Files.exists(path)) {
                System.err.println("Model file does not exist: " + path);
                return 2;
            }
            if (!Files.isRegularFile(path)) {
                System.err.println("Model path is not a regular file: " + path);
                return 2;
            }
            if (!Files.isReadable(path)) {
                System.err.println("Model file is not readable: " + path);
                return 2;
            }
        }
        return 0;
    }

    private boolean isStdout(Path path) {
        return path == null || "-".equals(path.toString());
    }

}
