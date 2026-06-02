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
package org.mitre.niem.translate;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.json.Context;
import org.mitre.niem.json.JSONWriter;
import org.mitre.niem.utility.AtomicPathWriter;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_SAX2;
import org.xml.sax.InputSource;
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
    name = "x2j",
    description = "convert NIEM XML message to NIEM JSON",
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class CmdXMLtoJSON implements Callable<Integer> {

    @Option(
        names = {"-c", "--context"},
        description = "generate complete @context in result"
    )
    boolean contextF = false;

    @Option(
        names = {"--curi"},
        description = "include \"@context\": URI in result"
    )
    String contextU = "";

    @Option(
        names = {"-f", "--force"},
        description = "overwrite existing .json files"
    )
    boolean force = false;

    @Parameters(
        arity = "2..*",
        paramLabel = "model.cmf msg.xml ...",
        description = "model.cmf msg.xml ..."
    )
    private List<Path> mainArgs;

    CmdXMLtoJSON() {
    }

    public static void main(String[] args) {
        int rc = new CommandLine(new CmdXMLtoJSON()).execute(args);
        System.exit(rc);
    }

    @Override
    public Integer call() {
        // Check for parser config errors now
        try {
            ParserBootstrap.init(BOOTSTRAP_SAX2);
        } catch (ParserConfigurationException ex) {
            System.err.println("Parser configuration error: " + ex.getMessage());
            return 1;
        }

        // Read the model object from the model instance file
        // Read the model object from the model file(s)
        var modelPath = mainArgs.get(0);
        int rc = validateReadableFile(modelPath, "model file");
        if (rc != 0) {
            return rc;
        }

        final Model model;
        try {
            var mr = new ModelXMLReader();
            model = mr.readFiles(modelPath.toFile());
        } catch (Exception ex) {
            System.err.println("Can't read model from " + modelPath + ": " + ex.getMessage());
            return 1;
        }
        if (null == model) {
            System.err.println("Can't read model from " + modelPath);
            return 1;
        }

        var tran = new XMLMsgToJSON(model);
        boolean hadError = false;

        for (int i = 1; i < mainArgs.size(); i++) {
            var xmlPath = mainArgs.get(i);
            rc = validateReadableFile(xmlPath, "XML file");
            if (rc != 0) {
                hadError = true;
                continue;
            }

            var jsonPath = toJsonPath(xmlPath);
            if (Files.exists(jsonPath) && !force) {
                System.err.println(jsonPath + ": file exists");
                hadError = true;
                continue;
            }

            var jobj = new JsonObject();
            try (InputStream xmlIn = Files.newInputStream(xmlPath)) {
                var xmlIS = new InputSource(xmlIn);
                xmlIS.setSystemId(xmlPath.toUri().toString());
                var status = tran.convert(xmlIS, jobj);
            } catch (ParserConfigurationException ex) {
                System.err.println("Parser configuration error: " + ex.getMessage());
                return 1;
            } catch (SAXException ex) {
                System.err.println(String.format("Error parsing %s: %s", xmlPath, ex.getMessage()));
                hadError = true;
                continue;
            } catch (IOException ex) {
                System.err.println(String.format("Error reading %s: %s", xmlPath, ex.getMessage()));
                hadError = true;
                continue;
            }

            if (contextF) {
                JsonObject cobj = null;
                try {
                    cobj = Context.create(model);
                } catch (CMFException ex) {
                    System.err.println("Can't create context: " + ex.getMessage());
                    hadError = true;
                    continue;
                }
                jobj.add("@context", cobj);
            } else if (!contextU.isBlank()) {
                jobj.addProperty("@context", contextU);
            }

            try {
                AtomicPathWriter.writeAtomically(jsonPath, StandardCharsets.UTF_8, jsonW -> {
                    JSONWriter.write(jobj, jsonW);
                });
            } catch (IOException ex) {
                System.err.println(String.format("Error writing %s: %s", jsonPath, ex.getMessage()));
                hadError = true;
            }
        }

        return hadError ? 1 : 0;
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

    private Path toJsonPath(Path xmlPath) {
        Path fileName = xmlPath.getFileName();
        String name = fileName == null ? xmlPath.toString() : fileName.toString();

        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String jsonName = base + ".json";

        Path parent = xmlPath.getParent();
        return parent == null ? Path.of(jsonName) : parent.resolve(jsonName);
    }
}
