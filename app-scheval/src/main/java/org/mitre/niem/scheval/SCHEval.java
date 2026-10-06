/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2025 The MITRE Corporation.
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

package org.mitre.niem.scheval;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.utility.AtomicPathWriter;
import org.mitre.niem.utility.BuildInfo;
import org.mitre.niem.xml.Schematron;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
@Command(
    name = "scheval",
    description = "validate XML with Schematron rules",
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class SCHEval implements java.util.concurrent.Callable<Integer> {

    @Spec
    CommandSpec spec;

    static class RuleSource {
        @Option(
            names = {"-s", "--schema"},
            description = "apply rules from this schematron file"
        )
        private Path schPath = null;

        @Option(
            names = {"-x", "--xslt"},
            description = "apply rules from this compiled schematron file"
        )
        private Path xsltPath = null;
    }

    @ArgGroup(exclusive = true, multiplicity = "1")
    private RuleSource rules;

    @Option(
        names = {"-o", "--output"},
        description = "write output to this file, or '-' for stdout (default = stdout)"
    )
    private Path outPath = null;

    @Option(
        names = {"--svrl"},
        description = "write output in SVRL format"
    )
    private boolean svrlFlag = false;

    @Option(
        names = {"--compile"},
        description = "compile schema and write output in XSLT format"
    )
    private boolean compileFlag = false;

    @Option(
        names = {"-c", "--catalog"},
        description = "provide this XML catalog file as $xml-catalog parameter"
    )
    private Path catPath = null;

    @Option(
        names = {"-k", "--keep"},
        description = "keep temporary files"
    )
    private boolean keepTemp = false;

    @Option(
        names = {"-d", "--debug"},
        description = "turn on debug logging"
    )
    private boolean debugFlag = false;

    @Parameters(
        arity = "0..*",
        paramLabel = "input.xml...",
        description = "[input.xml...]"
    )
    private List<Path> mainArgs = new ArrayList<>();

    SCHEval() { }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new SCHEval());
        cmd.getCommandSpec().usageMessage().width(100);

//        if (args.length == 0) { // debug args
//            args = new String[]{
//                "-s", "../lib-util/src/test/resources/sch/refTarget.sch",
//                "-c", "../lib-util/src/test/resources/sch/xml-catalog.xml",
//                "../lib-util/src/test/resources/sch/7-10.xsd" };
//        }

        if (args.length < 1) {
            printBanner();
            cmd.usage(System.out);
            System.exit(2);
        }

        int rc = cmd.execute(args);
        System.exit(rc);
    }

    @Override
    public Integer call() {
        if (debugFlag) {
            Configurator.setAllLevels(
                LogManager.getRootLogger().getName(),
                org.apache.logging.log4j.Level.DEBUG
            );
        }

        if (!compileFlag && mainArgs.isEmpty()) {
            System.err.println("Error: must supply at least one input XML file");
            spec.commandLine().usage(System.err);
            return 2;
        }
        if (compileFlag && !mainArgs.isEmpty()) {
            System.err.println("Error: can't supply input XML files with --compile flag");
            spec.commandLine().usage(System.err);
            return 2;
        }
        if (compileFlag && rules.xsltPath != null) {
            System.err.println("Error: can't have both --compile and --xslt");
            spec.commandLine().usage(System.err);
            return 2;
        }
        if (svrlFlag && rules.xsltPath != null) {
            System.err.println("Error: can't have both --svrl and --xslt");
            spec.commandLine().usage(System.err);
            return 2;
        }

        // keepTemp is preserved for compatibility, but currently unused.
        if (keepTemp) {
            // no-op
        }

        if (rules.schPath != null) {
            int rc = validateReadableFile(rules.schPath, "schematron file");
            if (rc != 0) {
                return rc;
            }
        }
        if (rules.xsltPath != null) {
            int rc = validateReadableFile(rules.xsltPath, "XSLT file");
            if (rc != 0) {
                return rc;
            }
        }
        if (catPath != null) {
            int rc = validateReadableFile(catPath, "catalog file");
            if (rc != 0) {
                return rc;
            }
        }
        for (var xmlPath : mainArgs) {
            int rc = validateReadableFile(xmlPath, "input XML file");
            if (rc != 0) {
                return rc;
            }
        }
        if (!isStdout(outPath)) {
            int rc = validateOutputPath(outPath);
            if (rc != 0) {
                return rc;
            }
        }

        // Initialize Schematron object
        final Schematron s;
        try {
            s = new Schematron();
        } catch (SaxonApiException ex) {
            System.err.println("Can't initialize schematron processor: " + ex.getMessage());
            return 1;
        }

        // Read catalog file into XdmNode object (to become a Saxon transformer parameter)
        XdmNode catNode = null;
        var saxonProc = new Processor(false);
        var saxonComp = saxonProc.newXsltCompiler();
        if (catPath != null) {
            var bld = saxonProc.newDocumentBuilder();
            try {
                catNode = bld.build(catPath.toFile());
            } catch (SaxonApiException ex) {
                System.err.println("Error: can't parse catalog file: " + ex.getMessage());
                return 1;
            }
        }

        // If we have Schematron rules, compile to XSLT
        final String xslt;
        final Path srcId;
        if (rules.schPath != null) {
            var schS = new StreamSource(rules.schPath.toFile());
            var xslW = new StringWriter();
            schS.setSystemId(rules.schPath.toFile());
            srcId = rules.schPath;
            try {
                s.compileSchematron(schS, xslW);
                xslt = xslW.toString();
            } catch (SaxonApiException ex) {
                System.err.println("Error: can't parse schematron file: " + ex.getMessage());
                return 1;
            }
        }
        // Otherwise read XSLT from file
        else {
            try {
                xslt = Files.readString(rules.xsltPath, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                System.err.println("Error: can't read XSLT from file: " + ex.getMessage());
                return 1;
            }
            srcId = rules.xsltPath;
        }

        // Write XSLT to output if we are just compiling
        if (compileFlag) {
            try {
                if (isStdout(outPath)) {
                    var outW = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
                    outW.write(xslt);
                    outW.flush();
                } else {
                    AtomicPathWriter.writeAtomically(outPath, StandardCharsets.UTF_8, outW -> {
                        outW.write(xslt);
                    });
                }
            } catch (IOException ex) {
                System.err.println("Error writing output: " + ex.getMessage());
                return 1;
            }
            return 0;
        }

        // Create transformer from XSLT text
        final XsltExecutable xsltExec;
        try {
            var xsltR = new StringReader(xslt);
            var xsltS = new StreamSource(xsltR);
            xsltS.setSystemId(srcId.toFile());
            xsltExec = saxonComp.compile(xsltS);
        } catch (SaxonApiException ex) {
            System.err.println("Error: can't compile XSLT: " + ex.getMessage());
            return 1;
        }

        final Schematron schematron = s;
        final XsltExecutable executable = xsltExec;
        final XdmNode catalogNode = catNode;

        // Run the compiled SCH on each input XML file
        // Maybe write SVRL, maybe parse it and write the result
        try {
            if (isStdout(outPath)) {
                var outW = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
                processInputs(schematron, executable, catalogNode, outW);
                outW.flush();
            } else {
                AtomicPathWriter.writeAtomically(outPath, StandardCharsets.UTF_8, outW -> {
                    try {
                        processInputs(schematron, executable, catalogNode, outW);
                    } catch (ParserConfigurationException | SAXException | TransformerException | SaxonApiException ex) {
                        throw new WrappedSchevalException(ex);
                    }
                });
            }
        } catch (WrappedSchevalException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ParserConfigurationException pce) {
                System.err.println("Error: parser configuration error: " + pce.getMessage());
            } else if (cause instanceof SAXException se) {
                System.err.println("Error turning SVRL into messages: " + se.getMessage());
            } else if (cause instanceof TransformerException te) {
                System.err.println("Error turning SVRL into messages: " + te.getMessage());
            } else if (cause instanceof SaxonApiException sae) {
                System.err.println("Error applying Schematron/XSLT: " + sae.getMessage());
            } else {
                System.err.println("Error: " + ex.getMessage());
            }
            return 1;
        } catch (IOException ex) {
            System.err.println("Error writing output: " + ex.getMessage());
            return 1;
        } catch (ParserConfigurationException ex) {
            System.getLogger(SCHEval.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } catch (SAXException ex) {
            System.getLogger(SCHEval.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } catch (TransformerException ex) {
            System.getLogger(SCHEval.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } catch (SaxonApiException ex) {
            System.getLogger(SCHEval.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

        return 0;
    }

    private void processInputs(
        Schematron s,
        XsltExecutable xsltExec,
        XdmNode catNode,
        Writer outW
    ) throws IOException, ParserConfigurationException, SAXException, TransformerException, SaxonApiException {

        for (var xmlPath : mainArgs) {
            var svrlW = new StringWriter();
            var xmlFile = xmlPath.toFile();

            XsltTransformer trans = xsltExec.load();
            trans.setParameter(new QName("allow-foreign"), new XdmAtomicValue("true"));
            if (catNode != null) {
                trans.setParameter(new QName("xml-catalog"), catNode);
            }

            try (var xmlR = Files.newBufferedReader(xmlPath, StandardCharsets.UTF_8)) {
                var xmlS = new StreamSource(xmlR);
                xmlS.setSystemId(xmlFile.toURI().toString());
                s.applyXslt(xmlS, trans, svrlW);
            }

            if (svrlFlag) {
                outW.write(svrlW.toString());
                continue;
            }

            try (
                var svrlR = new StringReader(svrlW.toString());
                var xmlR = Files.newBufferedReader(xmlPath, StandardCharsets.UTF_8)
            ) {
                var svrlS = new InputSource(svrlR);
                var xmlIS = new InputSource(xmlR);
                xmlIS.setSystemId(xmlFile.toURI().toString());
                s.SVRLtoMessages(svrlS, xmlIS, outW);
            }
        }
    }

    private static void printBanner() {
        BuildInfo info = BuildInfo.forClass(SCHEval.class);
        String version = String.format(
            "Version: %s (%s)", info.getImplementationVersion(), info.getBuildDate());
        System.out.println("Version: " + version);
        System.out.println("Suggestions and bug reports: https://github.com/niemopen/cmftool/issues");
    }

    private boolean isStdout(Path path) {
        return path == null || "".equals(path.toString()) || "-".equals(path.toString());
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

    private static final class WrappedSchevalException extends IOException {
        WrappedSchevalException(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }
}
