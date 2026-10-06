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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.mitre.niem.xml.SAXErrorHandler;
import org.mitre.niem.xml.XMLSchema;
import org.mitre.niem.xml.XMLSchemaException;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

@Command(
    name = "xval",
    description = {
        "validate XML documents",
        "Use --schema f.xsd [...] and optional --file doc.xml [...].",
        "Use '--' before filenames beginning with '-'."
    },
    mixinStandardHelpOptions = true,
    sortOptions = false
)
class CmdXSDValidate implements Callable<Integer> {

    @Option(
        names = {"-d", "--debug"},
        description = "turn on debug logging"
    )
    private boolean debugFlag = false;

    @Option(
        names = {"-q", "--quiet"},
        description = "suppress non-error output"
    )
    private boolean quiet = false;

    @Option(
        names = {"-s", "--schema"},
        arity = "1..*",
        required = true,
        paramLabel = "<f.xsd>",
        description = "schema document(s)"
    )
    private List<Path> schemaArgs = new ArrayList<>();

    @Option(
        names = {"-f", "--file"},
        arity = "1..*",
        paramLabel = "<doc.xml>",
        description = "XML document(s) to validate"
    )
    private List<Path> documents = new ArrayList<>();

    public static void main(String[] args) {
        int rc = new CommandLine(new CmdXSDValidate()).execute(args);
        System.exit(rc);
    }

    @Override
    public Integer call() {
        // Set debug logging
        // FIXME quiet
        if (debugFlag) {
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
        }

        // Validate schema and document files up front
        int schemaValidation = validateReadableFiles(schemaArgs, "Schema file");
        if (schemaValidation != 0) {
            return schemaValidation;
        }
        int documentValidation = validateReadableFiles(documents, "Document file");
        if (documentValidation != 0) {
            return documentValidation;
        }

        // Assemble the javax schema, die on errors and warnings
        String[] args = schemaArgs.stream()
            .map(Path::toString)
            .toArray(String[]::new);

        XMLSchema xmls;
        Schema vals;
        try {
            xmls = new XMLSchema(args);
            vals = xmls.javaxSchema();
            List<String> res = xmls.javaXMsgs();
            if (!res.isEmpty()) {
                for (String msg : res) {
                    System.err.println(msg);
                }
                return 1;
            }
        } catch (XMLSchemaException ex) {
            System.err.println(ex.getMessage());
            return 1;
        } catch (SAXException ex) {
            System.err.println(ex.getMessage());
            return 1;
        }

        // Validate documents, if any
        boolean hadDocumentErrors = false;
        String indent = documents.size() > 1 ? "  " : "";

        for (Path doc : documents) {
            SAXErrorHandler h = new SAXErrorHandler();
            List<String> issues = new ArrayList<>();
            Source s = new StreamSource(doc.toFile());
            Validator validator = vals.newValidator();
            validator.setErrorHandler(h);

            try {
                validator.validate(s);
            } catch (SAXException | IOException ex) {
                if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
                    issues.add(ex.getMessage());
                } else {
                    issues.add(ex.getClass().getName());
                }
            }

            issues.addAll(h.messages());

            if (!issues.isEmpty()) {
                hadDocumentErrors = true;
                if (!indent.isEmpty()) {
                    System.err.println(doc + ":");
                }
                for (String msg : issues) {
                    System.err.println(indent + msg);
                }
            }
        }

        if (hadDocumentErrors) {
            return 1;
        }

        if (!quiet) {
            if (documents.isEmpty()) {
                System.out.println("Schema compiled successfully.");
            } else if (documents.size() == 1) {
                System.out.println("Document is valid.");
            } else {
                System.out.println("All documents are valid.");
            }
        }

        return 0;
    }

    private int validateReadableFiles(List<Path> paths, String label) {
        for (var path : paths) {
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
        }
        return 0;
    }
}
