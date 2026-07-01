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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
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
import org.mitre.niem.utility.AtomicPathWriter;
import org.mitre.niem.xml.XMLWriter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "j2x",
    description = {
        "convert NIEM JSON message to NIEM XML",
        "With one msg.json and no -o/--output, writes XML to standard output.",
        "With multiple msg.json files, writes multiple msg.xml output files"
    },
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class CmdJSONtoXML implements Callable<Integer> {

    @Option(
        names = {"-c", "--context"},
        paramLabel = "context.json",
        description = "JSON-LD context file used to interpret input messages"
    )
    Path contextPath;

    @Option(
        names = {"-f", "--force"},
        description = "overwrite existing output files"
    )
    boolean force = false;

    @Option(
        names = {"-o", "--output"},
        paramLabel = "out.xml",
        description = "write output to out.xml; only valid when there is a single msg.json argument"
    )
    Path outputPath;

    @Parameters(
        index = "0",
        paramLabel = "model.cmf",
        description = "NIEM model file"
    )
    private Path modelPath;

    @Parameters(
        index = "1..*",
        arity = "1..*",
        paramLabel = "msg.json",
        description = "one or more NIEM JSON message files"
    )
    private List<Path> jsonPaths;

    CmdJSONtoXML() {
    }

    public static void main(String[] args) {
        int rc = new CommandLine(new CmdJSONtoXML()).execute(args);
        System.exit(rc);
    }

    @Override
    public Integer call() {
        if (outputPath != null && jsonPaths.size() != 1) {
            System.err.println("Option -o/--output may only be used with a single msg.json argument");
            return 2;
        }

        boolean writeToStdout = (jsonPaths.size() == 1 && outputPath == null);

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
        if (model == null) {
            System.err.println("Can't read model from " + modelPath);
            return 1;
        }

        Context context = null;
        if (contextPath != null) {
            rc = validateReadableFile(contextPath, "context file");
            if (rc != 0) {
                return rc;
            }

            try {
                var rdr = Files.newBufferedReader(contextPath, StandardCharsets.UTF_8);
                context = new Context(rdr);
            } catch (IOException | JsonParseException ex) {
                System.err.println(String.format("Error reading %s: %s", contextPath, ex.getMessage()));
                return 1;
            } catch (IllegalArgumentException | CMFException ex) {
                System.err.println(String.format("Invalid context file %s: %s", contextPath, ex.getMessage()));
                return 1;
            }
        }
        else try {
            context = new Context(model);
        } catch (CMFException ex) {
                System.err.println(String.format("Error constructing context from model %s", ex.getMessage()));
                return 1;
        }
        final JSONMsgToXML converter = new JSONMsgToXML(model, context);

        if (outputPath != null) {
            rc = validateWritableOutputPath(outputPath);
            if (rc != 0) {
                return rc;
            }
        }

        boolean hadError = false;

        for (var jsonPath : jsonPaths) {
            rc = validateReadableFile(jsonPath, "JSON file");
            if (rc != 0) {
                hadError = true;
                continue;
            }

            Path xmlPath = null;
            if (!writeToStdout) {
                xmlPath = (outputPath != null) ? outputPath : toXmlPath(jsonPath);
                if (outputPath == null) {
                    rc = validateWritableOutputPath(xmlPath);
                    if (rc != 0) {
                        hadError = true;
                        continue;
                    }
                }
            }

            final JsonObject msgObj;
            try {
                msgObj = readJsonObject(jsonPath, "JSON file");
            } catch (IOException | JsonParseException ex) {
                System.err.println(String.format("Error reading %s: %s", jsonPath, ex.getMessage()));
                hadError = true;
                continue;
            } catch (IllegalArgumentException ex) {
                System.err.println(String.format("Invalid JSON file %s: %s", jsonPath, ex.getMessage()));
                hadError = true;
                continue;
            }

            if (writeToStdout) {
                try {
                    writeXmlToStdout(converter, msgObj, jsonPath);
                } catch (IOException ex) {
                    System.err.println(String.format("Error writing stdout for %s: %s", jsonPath, ex.getMessage()));
                    hadError = true;
                } catch (UnsupportedOperationException | ParserConfigurationException ex) {
                    System.err.println(ex.getMessage());
                    return 1;
                } catch (NIEMTranException ex) {
                    System.err.println("Conversion error: " + ex.getMessage());
                    hadError = true;
                }
            } else {
                final Path finalXmlPath = xmlPath;
                try {
                    AtomicPathWriter.writeAtomically(finalXmlPath, StandardCharsets.UTF_8, xmlW -> {
                        try {
                            writeXMLMessage(converter, msgObj, jsonPath, xmlW);
                        } catch (IOException | NIEMTranException | ParserConfigurationException ex) {
                            throw new IOException(ex.getMessage());
                        }
                    });
                } catch (IOException ex) {
                    System.err.println(String.format("Error writing %s: %s", finalXmlPath, ex.getMessage()));
                    hadError = true;
                } catch (UnsupportedOperationException ex) {
                    System.err.println(ex.getMessage());
                    return 1;
                }
            }
        }

        return hadError ? 1 : 0;
    }

    private JsonObject readJsonObject(Path path, String label) throws IOException, JsonParseException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var elem = JsonParser.parseReader(reader);
            if (elem == null || !elem.isJsonObject()) {
                throw new IllegalArgumentException(label + " must contain a JSON object");
            }
            return elem.getAsJsonObject();
        }
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

    private int validateWritableOutputPath(Path path) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                System.err.println("Output path is a directory: " + path);
                return 2;
            }
            if (!Files.isRegularFile(path)) {
                System.err.println("Output path is not a regular file: " + path);
                return 2;
            }
            if (!force) {
                System.err.println(path + ": file exists");
                return 2;
            }
            if (!Files.isWritable(path)) {
                System.err.println("Output file is not writable: " + path);
                return 2;
            }
            return 0;
        }

        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            if (!Files.exists(parent)) {
                System.err.println("Output directory does not exist: " + parent);
                return 2;
            }
            if (!Files.isDirectory(parent)) {
                System.err.println("Output parent is not a directory: " + parent);
                return 2;
            }
            if (!Files.isWritable(parent)) {
                System.err.println("Output directory is not writable: " + parent);
                return 2;
            }
        }
        return 0;
    }

    private void writeXmlToStdout(JSONMsgToXML converter, JsonObject msgObj, Path sourcePath) throws IOException, NIEMTranException, ParserConfigurationException {
        var out = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
        writeXMLMessage(converter, msgObj, sourcePath, out);
        out.flush();
    }

    private Path toXmlPath(Path jsonPath) {
        Path fileName = jsonPath.getFileName();
        String name = fileName == null ? jsonPath.toString() : fileName.toString();

        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String xmlName = base + ".xml";

        Path parent = jsonPath.getParent();
        return parent == null ? Path.of(xmlName) : parent.resolve(xmlName);
    }

    private void writeXMLMessage(JSONMsgToXML converter, JsonObject msgObj, Path sourcePath, Writer xmlW) throws IOException, NIEMTranException, ParserConfigurationException {
        var doc = converter.convert(msgObj);
        var xw  = new XMLWriter();
        xw.writeXML(doc, xmlW);
    }
}

