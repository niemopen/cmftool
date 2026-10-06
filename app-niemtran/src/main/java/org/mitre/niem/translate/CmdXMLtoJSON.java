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
import java.io.OutputStreamWriter;
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
import picocli.CommandLine.ArgGroup;
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
    description = {
        "convert NIEM XML message to NIEM JSON",
        "With one msg.xml and no -o/--output, writes JSON to standard output.",
        "With multiple msg.xml files, writes multiple msg.json output files"
    },
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class CmdXMLtoJSON implements Callable<Integer> {

    static class ContextOptions {
        @Option(
            names = {"-c", "--context"},
            description = "generate complete @context in result"
        )
        boolean contextF;

        @Option(
            names = {"--curi"},
            paramLabel = "uri",
            description = "include \"@context\": URI in result"
        )
        String contextU;
    }

    @ArgGroup(exclusive = true, multiplicity = "0..1")
    ContextOptions contextOptions;

    @Option(
        names = {"-f", "--force"},
        description = "overwrite existing output files"
    )
    boolean force = false;

    @Option(
        names = {"-o", "--output"},
        paramLabel = "out.json",
        description = "write output to out.json; only valid when there is a single msg.xml argument"
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
        paramLabel = "msg.xml",
        description = "one or more NIEM XML message files"
    )
    private List<Path> xmlPaths;

    CmdXMLtoJSON() {
    }

    public static void main(String[] args) {
        int rc = new CommandLine(new CmdXMLtoJSON()).execute(args);
        System.exit(rc);
    }

    @Override
    public Integer call() {
        try {
            ParserBootstrap.init(BOOTSTRAP_SAX2);
        } catch (ParserConfigurationException ex) {
            System.err.println("Parser configuration error: " + ex.getMessage());
            return 1;
        }

        if (outputPath != null && xmlPaths.size() != 1) {
            System.err.println("Option -o/--output may only be used with a single msg.xml argument");
            return 2;
        }

        boolean writeToStdout = (xmlPaths.size() == 1 && outputPath == null);

        int rc = validateReadableFile(modelPath, "model file");
        if (rc != 0) {
            return rc;
        }

        if (outputPath != null) {
            rc = validateWritableOutputPath(outputPath);
            if (rc != 0) {
                return rc;
            }
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

        JsonObject fullContext = null;
        String contextUri = null;
        if (contextOptions != null) {
            if (contextOptions.contextF) {
                try {
                    fullContext = new Context(model).jsonObject();
                } catch (CMFException ex) {
                    System.err.println("Can't create context: " + ex.getMessage());
                    return 1;
                }
            } else if (contextOptions.contextU != null && !contextOptions.contextU.isBlank()) {
                contextUri = contextOptions.contextU;
            }
        }

        var tran = new XMLMsgToJSON(model);
        boolean hadError = false;

        for (var xmlPath : xmlPaths) {
            rc = validateReadableFile(xmlPath, "XML file");
            if (rc != 0) {
                hadError = true;
                continue;
            }

            Path jsonPath = null;
            if (!writeToStdout) {
                jsonPath = (outputPath != null) ? outputPath : toJsonPath(xmlPath);
                if (outputPath == null) {
                    rc = validateWritableOutputPath(jsonPath);
                    if (rc != 0) {
                        hadError = true;
                        continue;
                    }
                }
            }

            var jobj = new JsonObject();
            try (InputStream xmlIn = Files.newInputStream(xmlPath)) {
                var xmlIS = new InputSource(xmlIn);
                xmlIS.setSystemId(xmlPath.toUri().toString());
                tran.convert(xmlIS, jobj);
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

            if (fullContext != null) {
                jobj.add("@context", fullContext.deepCopy());
            } else if (contextUri != null) {
                jobj.addProperty("@context", contextUri);
            }

            if (writeToStdout) {
                try {
                    writeJsonToStdout(jobj);
                } catch (IOException ex) {
                    System.err.println(String.format("Error writing stdout for %s: %s", xmlPath, ex.getMessage()));
                    hadError = true;
                }
            } else {
                try {
                    AtomicPathWriter.writeAtomically(jsonPath, StandardCharsets.UTF_8, jsonW -> {
                        JSONWriter.write(jobj, jsonW);
                    });
                } catch (IOException ex) {
                    System.err.println(String.format("Error writing %s: %s", jsonPath, ex.getMessage()));
                    hadError = true;
                }
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

    private void writeJsonToStdout(JsonObject jobj) throws IOException {
        var out = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
        JSONWriter.write(jobj, out);
        out.flush();
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

