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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.utility.StagedDirectoryWriter;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_ALL;
import org.mitre.niem.xsd.ModelToXSDModel;
import org.mitre.niem.xsd.NamespaceKind;
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
    name = "m2x",
    description = {
        "convert a NIEM model from CMF to XSD",
        "Use '--' before model filenames beginning with '-'."
    },
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class CmdCMFtoXSDModel implements Callable<Integer> {

    @Option(
        names = {"-o", "--output-dir"},
        paramLabel = "<dir>",
        description = "write schema pile into this directory",
        defaultValue = "."
    )
    private Path outputDir;

    @Option(
        names = {"-c"},
        description = "generate XML catalog into xml-catalog.xml file"
    )
    private boolean catFlag = false;

    @Option(
        names = {"--catalog"},
        description = "write XML catalog into this file"
    )
    private Path catPath = null;

    @Option(
        names = {"-r", "--root"},
        description = "make this schema document have all necessary imports"
    )
    private String rootNSarg = null;

    @Option(
        names = {"-v", "--arch-version"},
        paramLabel = "<vers>",
        description = "builtins from this architecture (eg. \"-v NIEM5.0\")"
    )
    private String archVers = null;

    @Option(
        names = {"-d", "--debug"},
        description = "turn on debug logging"
    )
    private boolean debugFlag = false;

    @Option(
        names = {"--force"},
        description = "replace existing output directory by moving it aside and promoting staged output"
    )
    private boolean force = false;

    @Parameters(
        arity = "1..*",
        paramLabel = "modelFile.cmf...",
        description = "one or more model files; use '--' before filenames beginning with '-'"
    )
    private List<Path> modelPaths;

    public static void main(String[] args) {
        int rc = new CommandLine(new CmdCMFtoXSDModel()).execute(args);
        System.exit(rc);
    }

    @Override
    public Integer call() {
        // Set debug logging
        if (debugFlag) {
            Configurator.setAllLevels(
                LogManager.getRootLogger().getName(),
                org.apache.logging.log4j.Level.DEBUG
            );
        }

        // Sanity checking
        if (null != archVers && !NamespaceKind.knownVersions().contains(archVers)) {
            System.err.println("Unknown architecture version " + archVers);
            return 2;
        }
        if (catFlag && null != catPath && !"xml-catalog.xml".equals(catPath.toString())) {
            System.err.println("-c and --catalog options are in conflict");
            return 2;
        }
        if (catFlag) {
            catPath = Path.of("xml-catalog.xml");
        }

        // Validate output directory policy
        try {
            int odValidation = validateOutputDirectory(outputDir, force);
            if (odValidation != 0) {
                return odValidation;
            }
        } catch (IOException ex) {
            System.err.println(String.format("I/O error: %s", ex.getMessage()));
            return 1;
        }

        // Make sure the Xerces parser can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println(ex.getMessage());
            return 1;
        }

        // Read the model object from the model file(s)
        int pathValidation = validateModelPaths(modelPaths);
        if (pathValidation != 0) {
            return pathValidation;
        }

        var mr = new ModelXMLReader();
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

        // Validate requested root namespace against the model
        if (rootNSarg != null
            && model.namespaceSet().stream().noneMatch(ns -> rootNSarg.equals(ns.uri()))) {
            System.err.println(
                "Root namespace is not in the model: " + rootNSarg
                    + System.lineSeparator()
                    + "Known model namespaces:"
                    + System.lineSeparator()
                    + model.namespaceSet().stream()
                        .map(ns -> "  " + ns.uri())
                        .sorted()
                        .collect(Collectors.joining(System.lineSeparator()))
            );
            return 2;
        }

        var m2x = new ModelToXSDModel(model);
        m2x.setArchVersion(archVers);
        m2x.setCatalogPath(catPath == null ? null : catPath.toString());
        m2x.setRootNamespace(rootNSarg);

        try {
            Path target = outputDir.toAbsolutePath().normalize();
            if (Files.exists(target)) {
                StagedDirectoryWriter.replaceDirectory(target, "bak", stagingDir ->
                    m2x.writeModelXSD(stagingDir.toFile())
                );
            } else {
                StagedDirectoryWriter.writeToNewDirectory(target, stagingDir ->
                    m2x.writeModelXSD(stagingDir.toFile())
                );
            }
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            return 1;
        }

        // Tell user to provide external schema documents
        for (var ns : model.namespaceSet()) {
            if ("EXTERNAL".equals(ns.kindCode())) {
                System.out.println(String.format(
                    "You must copy all schema documents required for %s to %s",
                    ns.uri(),
                    ns.documentFilePath()
                ));
            }
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

    private int validateOutputDirectory(Path dir, boolean forceReplace) throws IOException {
        Path target = dir.toAbsolutePath().normalize();

        if (!Files.exists(target)) {
            return 0;
        }
        if (!Files.isDirectory(target)) {
            System.err.println("Output path is not a directory: " + target);
            return 2;
        }
        if (!forceReplace) {
            if (StagedDirectoryWriter.isNonEmptyDirectory(target)) {
                System.err.println(
                    "Output directory already exists and is not empty: "
                        + target
                        + System.lineSeparator()
                        + "Use --force to replace it."
                );
            } else {
                System.err.println(
                    "Output directory already exists: "
                        + target
                        + System.lineSeparator()
                        + "Use --force to replace it."
                );
            }
            return 2;
        }
        return 0;
    }
}
