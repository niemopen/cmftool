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
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLWriter;
import org.mitre.niem.utility.AtomicPathWriter;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_ALL;
import org.mitre.niem.xml.XMLSchemaException;
import org.mitre.niem.xsd.ModelFromXSD;
import org.mitre.niem.xsd.NIEMSchema;
import static org.mitre.niem.xsd.NamespaceKind.*;
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
    name = "x2m",
    description = {
        "convert a NIEM model from XSD to CMF",
        "Use '--' before schema arguments beginning with '-'."
    },
    mixinStandardHelpOptions = true,
    sortOptions = false
)
    
class CmdXSDtoCMF implements Callable<Integer> {

    @Option(
        names = {"-o", "--output"},
        paramLabel = "<path>",
        description = "output model file, or '-' for stdout"
    )
    private Path outputPath = null;

    @Option(
        names = "--only",
        paramLabel = "p1[,p2...]",
        hideParamSyntax = true,
        description = "include only these namespace URIs or prefixes; eg. '--only nc,j'"
    )
    private String onlyArg = null;

    @Option(
        names = {"-d", "--debug"},
        description = "turn on debug logging"
    )
    private boolean debugFlag = false;

    @Option(
        names = {"-p", "--profile"},
        description = "pause for profiler attachment",
        hidden = true
    )
    private boolean profileFlag = false;

    @Option(
        names = {"-q", "--quiet"},
        description = "no output, exit status only"
    )
    private boolean quietFlag = false;

    @Parameters(
        arity = "1..*",
        paramLabel = "{schema|URI|catalog}...",
        description = "{schema document, namespace URI, or XML catalog}..."
    )
    private List<String> mainArgs;

    public static void main(String[] args) {
        int rc = new CommandLine(new CmdXSDtoCMF()).execute(args);
        System.exit(rc);
    }

    @Override
    public Integer call() {
        if (profileFlag) {
            try {
                System.err.println("sleeping");
                Thread.sleep(5009);
                System.err.println("resuming");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                System.err.println("Profiler wait interrupted");
                return 1;
            }
        }

        // Set debug logging
        // FIXME quiet
        if (debugFlag) {
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
        }

        // Figure out file names for model and extension
        if (outputPath == null) {
            var obase = "Model";
            for (var a : mainArgs) {
                if (a.endsWith(".xsd")) {
                    obase = FilenameUtils.getBaseName(a);
                    break;
                }
            }
            outputPath = Path.of(obase + ".cmf");
        }

        // Make sure output model file is writable
        if (!isStdout(outputPath)) {
            try {
                validateOutputPath(outputPath);
            } catch (IOException ex) {
                System.err.println(String.format("Can't write to output file %s: %s", outputPath, ex.getMessage()));
                return 1;
            }
        }

        // Make sure the Xerces parsers can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Internal parser error: " + ex.getMessage());
            return 1;
        }

        // Construct the schema object from arguments
        String[] aa = mainArgs.toArray(new String[0]);
        NIEMSchema s;
        Model m;
        try {
            var mfact = new ModelFromXSD();
            s = new NIEMSchema(aa);
            m = mfact.createModel(s);
        } catch (XMLSchemaException | CMFException ex) {
            System.err.println(String.format("Error building XML schema: %s", ex.getMessage()));
            return 1;
        }

        // Convert onlyArg to list of namespace URIs/prefixes
        var onlyL = new ArrayList<String>();
        if (onlyArg != null && !onlyArg.isBlank()) {
            onlyL.addAll(List.of(onlyArg.split("\\s*,\\s*")));
        }

        // Validate --only values against model namespace prefixes/URIs
        int onlyValidation = validateOnlyArgs(m, onlyL);
        if (onlyValidation != 0) {
            return onlyValidation;
        }

        // Write the NIEM model instance to the output stream
        var mw = new ModelXMLWriter();
        try {
            if (isStdout(outputPath)) {
                var ow = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
                if (onlyL.isEmpty()) {
                    mw.writeXML(m, ow);
                } else {
                    mw.writeXML(m, onlyL, ow);
                }
                ow.flush();
            } else {
                AtomicPathWriter.writeAtomically(outputPath, StandardCharsets.UTF_8, ow -> {
                    if (onlyL.isEmpty()) {
                        mw.writeXML(m, ow);
                    } else {
                        mw.writeXML(m, onlyL, ow);
                    }
                });
            }
        } catch (IOException ex) {
            System.err.println("Output error: " + ex.getMessage());
            return 1;
        }

        // Report various error and warning messages captured in the schema object
        if (!quietFlag) {
            var report = System.err;
            var catmsgL = s.resolver().allMessages();
            var schmsgL = s.xsModelMsgs();
            if (!catmsgL.isEmpty()) {
                report.println("Catalog resolver messages:");
                for (var msg : catmsgL) {
                    report.println("  " + msg);
                }
            }
            if (!schmsgL.isEmpty()) {
                report.println("Schema assembly messages:");
                for (var msg : schmsgL) {
                    report.println("  " + msg);
                }
            }

            // Categorize namespaces in the pile, report by kind
            var model = new ArrayList<String>();
            var conforming = new ArrayList<String>();
            var external = new ArrayList<String>();
            var builtins = new ArrayList<String>();
            var unknown = new ArrayList<String>();

            for (var sdU : s.schemaNamespaceUs()) {
                var kind = s.namespaceKind(sdU);
                switch (kind) {
                    case NSK_EXTENSION:
                    case NSK_DOMAIN:
                    case NSK_CORE:
                    case NSK_OTHERNIEM:
                    case NSK_CLI:
                        model.add(sdU);
                        break;
                    case NSK_APPINFO:
                    case NSK_CLSA:
                    case NSK_NIEM_XS:
                    case NSK_STRUCTURES:
                    case NSK_XML:
                    case NSK_XSD:
                        builtins.add(sdU);
                        break;
                    case NSK_EXTERNAL:
                        external.add(sdU);
                        break;
                    case NSK_NOTNIEM:
                    case NSK_UNKNOWN:
                        unknown.add(sdU);
                        break;
                }
            }

            reportNamespaceSummary(report, s, model, conforming, external, builtins, unknown);
        }

        return 0;
    }

    private void reportNamespaceSummary(
        PrintStream report,
        NIEMSchema s,
        List<String> model,
        List<String> conforming,
        List<String> external,
        List<String> builtins,
        List<String> unknown
    ) {
        var hdr = "Namespaces with problems:\n";
        for (var nsU : model) {
            var sd = s.schemaDocument(nsU);
            var ctas = sd.ctasNS();
            var ctaL = sd.ctAssertions();
            var vers = sd.niemVersion();

            boolean hasProblem = false;
            if (ctas.isBlank()) {
                report.println(String.format("%s  %s [no CTAS namespace]", hdr, nsU));
                hdr = "";
                hasProblem = true;
            }
            if (ctaL.isEmpty()) {
                report.println(String.format("%s  %s [no conformance target assertion]", hdr, nsU));
                hdr = "";
                hasProblem = true;
            }
            if (vers.isBlank()) {
                report.println(String.format("%s  %s [can't determine NIEM architecture version]", hdr, nsU));
                hdr = "";
                hasProblem = true;
            }
            if (!hasProblem) {
                conforming.add(nsU);
            }
        }

        Collections.sort(conforming);
        hdr = "Namespaces claiming conformance:\n";
        for (var nsU : conforming) {
            var sd = s.schemaDocument(nsU);
            var vers = sd.niemVersion();
            report.println(String.format("%s  %s [NIEM version='%s']", hdr, nsU, vers));
            hdr = "";
        }

        Collections.sort(external);
        hdr = "External namespaces (imported with appinfo:externalNamespaceIndicator):\n";
        for (var nsU : external) {
            report.println(String.format("%s  %s", hdr, nsU));
            hdr = "";
        }

        Collections.sort(builtins);
        hdr = "Utililty and predefined namespaces:\n";
        for (var nsU : builtins) {
            report.println(String.format("%s  %s", hdr, nsU));
            hdr = "";
        }

        Collections.sort(unknown);
        hdr = "Unknown namespaces (no conformance assertion found):\n";
        for (var nsU : unknown) {
            report.println(String.format("%s  %s", hdr, nsU));
            hdr = "";
        }
    }

    private boolean isStdout(Path path) {
        return path != null && "-".equals(path.toString());
    }

    private void validateOutputPath(Path path) throws IOException {
        Path abs = path.toAbsolutePath().normalize();
        Path parent = abs.getParent();
        if (parent != null && !Files.exists(parent)) {
            throw new IOException("output directory does not exist: " + parent);
        }
        if (Files.exists(abs) && Files.isDirectory(abs)) {
            throw new IOException("output path is a directory: " + abs);
        }
    }

    private int validateOnlyArgs(Model model, List<String> onlyArgs) {
        if (onlyArgs == null || onlyArgs.isEmpty()) {
            return 0;
        }

        List<String> unknown = new ArrayList<>();
        for (var only : onlyArgs) {
            boolean found = model.namespaceSet().stream().anyMatch(ns ->
                only.equals(ns.uri()) || only.equals(ns.prefix())
            );
            if (!found) {
                unknown.add(only);
            }
        }

        if (!unknown.isEmpty()) {
            System.err.println("Unknown namespace prefix/URI in --only: " + String.join(", ", unknown));
            return 2;
        }
        return 0;
    }
}
