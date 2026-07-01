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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.json.ModelToJSONSchema;
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
    name = "m2jmsg",
    description = "generate a JSON message schema from CMF",
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class CmdCMFtoJSONSchema implements Callable<Integer> {

    private static final Set<String> VALID_SCHEMA_VERSIONS =
        Set.of("draft-07", "2019-09", "2020-12");

    @Option(
        names = {"-m", "--msg"},
        split = ",",
        paramLabel = "<QName>",
        description = "build schema to validate these message properties"
    )
    private List<String> msgQA = new ArrayList<>();

    @Option(
        names = {"-c", "--context"},
        paramLabel = "<URI>",
        description = "schema will require this @context URI"
    )
    private String contextU = null;

    @Option(
        names = {"--map"},
        description = "mapping file for property keys"
    )
    private Path mapPath = null;

    @Option(
        names = {"-a", "--alldefs"},
        description = "generate definition for all model classes and datatypes"
    )
    private boolean allDefs = false;

    @Option(
        names = {"-o", "--output"},
        description = "name of output file"
    )
    private Path outputPath = null;

    @Option(
        names = "--noprefix",
        description = "don't use prefix in property keys"
    )
    private boolean noPrefix = false;

    @Option(
        names = "--noformat",
        description = "don't include format properties in built-in types"
    )
    private boolean noFormat = false;

    @Option(
        names = "--nopattern",
        description = "don't include pattern properties in built-in types"
    )
    private boolean noPattern = false;

    @Option(
        names = "--nominmax",
        description = "don't include minimum/maximum properties in built-in types"
    )
    private boolean noMinMax = false;

    @Option(
        names = "--version",
        description = "use this Schematron version {draft-07,2019-09,2020-12}",
        defaultValue = "draft-07"
    )
    private String version = "draft-07";

    @Option(
        names = {"--versionUri"},
        paramLabel = "<URI>",
        description = "use this Schematron version URI (eg. http://json-schema.org/draft-07/schema#)"
    )
    private String versionURI = null;

    @Parameters(
        index = "0",
        arity = "1",
        paramLabel = "modelFile.cmf",
        description = "model file"
    )
    private Path modelPath;

    @Override
    public Integer call() {
        // Make sure the Xerces parsers can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Internal parser error: " + ex.getMessage());
            return 1;
        }

        if (versionURI == null && !VALID_SCHEMA_VERSIONS.contains(version)) {
            System.err.println(
                "Invalid --version value: " + version
                    + " (expected one of: draft-07, 2019-09, 2020-12)"
            );
            return 2;
        }

        // Read the model object from the model instance file
        var mr = new ModelXMLReader();
        final var model = mr.readFiles(modelPath.toFile());

        // Read the mapping file if one was provided
        Mapping map = null;
        if (null != mapPath) {
            try {
                map = Mapping.readFile(mapPath.toFile());
            } catch (IOException | CMFException ex) {
                System.err.println(
                    String.format("Can't read mapping file %s: %s", mapPath, ex.getMessage())
                );
                return 1;
            }
        }

        // Get message property object (if specified)
        List<Property> msgPropA = new ArrayList<>();
        if (null != msgQA) {
            for (var msgQ : msgQA) {
                var p = model.qnToObjectProperty(msgQ);
                if (null == p) {
                    System.err.println("Property " + msgQ + " is not in model");
                    return 1;
                }
                msgPropA.add(p);
            }
        }

        // Generate JSON Schema
        try {
            var js = new ModelToJSONSchema(model);
            js.setMessageProperties(msgPropA);
            js.setContextURI(contextU);
            js.setMapping(map);
            js.setNoPrefix(noPrefix);
            js.setAllDefinitions(allDefs);
            js.setNoFormat(noFormat);
            js.setNoPattern(noPattern);
            js.setNoMinMax(noMinMax);

            if (null != versionURI) {
                js.setSchemaURI(versionURI);
            } else {
                js.setSchemaVersion(version);
            }

            if (outputPath != null) {
                AtomicPathWriter.writeAtomically(outputPath, StandardCharsets.UTF_8, ow -> {
                    js.writeSchema(ow);
                });
            } else {
                var ow = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
                js.writeSchema(ow);
                ow.flush();
            }
        } catch (CMFException | IOException ex) {
            System.err.println(ex.getMessage());
            return 1;
        }

        return 0;
    }

}
