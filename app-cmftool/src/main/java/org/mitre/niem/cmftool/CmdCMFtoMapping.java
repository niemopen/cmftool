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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
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
    name = "m2map",
    description = {
        "create a mapping template from CMF",
        "Use '--' before model filenames beginning with '-'."
    },
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class CmdCMFtoMapping implements Callable<Integer> {

    @Option(
        names = {"-s", "--single"},
        paramLabel = "<p=URI>",
        description = "prefix=URI of single target namespace"
    )
    private String targetMap = null;

    @Option(
        names = {"-t", "--types"},
        description = "also map class and datatype QNames"
    )
    private boolean includeTypes = false;

    @Option(
        names = {"-o", "--output"},
        paramLabel = "<path>",
        description = "name of output mapping file"
    )
    private Path outputPath = null;

    @Parameters(
        arity = "1..*",
        paramLabel = "model.cmf ...",
        description = "one or more CMF model files; use '--' before filenames beginning with '-'"
    )
    private List<Path> modelPaths;

    private static final Pattern SPLIT = Pattern.compile("^\\s*(.*?)\\s*=\\s*(.+)\\s*$");
    private static final Pattern NCNAME = Pattern.compile("^[A-Za-z_][A-Za-z0-9._-]*$");

    @Override
    public Integer call() {
        // If single target namespace specified, make sure prefix and uri are valid
        var targetP = "";
        var targetU = "";
        if (null != targetMap) {
            var m = SPLIT.matcher(targetMap);
            if (!m.matches()) {
                System.err.println("--single must have form prefix=URI");
                return 2;
            }
            targetP = m.group(1).strip();
            targetU = m.group(2).strip();
            if (!NCNAME.matcher(targetP).matches() || targetP.toLowerCase().startsWith("xml")) {
                System.err.println("--single " + targetMap + ": invalid prefix");
                return 2;
            }
            URI u = null;
            try {
                u = new URI(targetU);
            } catch (Exception ex) {
                // Keep validation behavior simple and report below
            }
            if (null == u || !u.isAbsolute()) {
                System.err.println("--single " + targetMap + ": " + targetU + " is not an absolute URI");
                return 2;
            }
        }

        // Make sure the Xerces parsers can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Internal parser error: " + ex.getMessage());
            return 1;
        }

        // Read the model object from the model file(s)
        final var mr = new ModelXMLReader();
        final Model model;
        try {
            model = mr.readFiles(modelPaths.stream()
                .map(Path::toFile)
                .collect(Collectors.toList()));
        } catch (Exception ex) {
            System.err.println(
                "Can't read model file(s) "
                    + modelPaths.stream().map(Path::toString).collect(Collectors.joining(", "))
                    + ": " + ex.getMessage()
            );
            return 1;
        }

        // Create mapping object from model, write to output
        try {
            final Mapping map;
            if (null != targetMap) {
                map = Mapping.createOneNamespaceMapping(model, null, targetP, targetU);
            } else {
                map = Mapping.createTemplate(model, "T", "http://example.com/YourTargetNSURI/");
            }

            // Suggested fix from original code: --types was parsed but not used.
            // If Mapping has or gains an API for this, wire includeTypes into the
            // mapping generation here.
            if (includeTypes) {
                // TODO: apply includeTypes to mapping generation when supported by Mapping API.
            }

            if (outputPath != null) {
                AtomicPathWriter.writeAtomically(outputPath, StandardCharsets.UTF_8, ow -> {
                    map.write(ow);
                });
            } else {
                // Write directly to stdout; do not close System.out.
                var ow = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
                map.write(ow);
                ow.flush();
            }
        } catch (Exception ex) {
            System.err.println("Can't create mapping template: " + ex.getMessage());
            return 1;
        }

        return 0;
    }

}
