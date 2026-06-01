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

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.json.Context;
import org.mitre.niem.xml.ParserBootstrap;
import static org.mitre.niem.xml.ParserBootstrap.BOOTSTRAP_ALL;
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
    name = "m2context",
    description = "create a JSON-LD context from a model file and mapping",
    mixinStandardHelpOptions = true
)
public class CmdCMFtoContext implements Callable<Integer> {
    
    @Option(
        names = {"-m", "--map"},
        paramLabel = "<path>",
        description = "mapping file for property keys"
    )
    private Path mapF = null;
         
    @Option(
        names = {"-o", "--output"},
        paramLabel = "<path>",
        description = "name of output context file; use '-' for stdout (the default)"
    )
    private String outName = null;

    @Option(
        names = "--force",
        description = "overwrite output file if it already exists"
    )
    private boolean force = false;

    @Option(
        names = "--validate-only",
        description = "validate model and mapping for context generation, but do not write output"
    )
    private boolean validateOnly = false;
        
    @Parameters(
        index = "0",
        paramLabel = "model.cmf",
        description = "model.cmf"
    )
    private Path modelF;
    
    public static void main (String[] args) {       
        var cmd = new CommandLine(new CmdCMFtoContext());
        cmd.setCommandName("m2context");
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call () {
        if (validateOnly && null != outName && !outName.isBlank()) {
            System.err.println("--validate-only cannot be combined with --output");
            return 1;
        }
        if (validateOnly && force) {
            System.err.println("--validate-only cannot be combined with --force");
            return 1;
        }

        // Make sure output context file is writable
        if (!validateOnly) {
            int rc = validateOutputTarget();
            if (0 != rc) return rc;
        }
        
        // Make sure the Xerces parsers can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Internal parser error: " + ex.getMessage());
            return 1;
        }
        
        // Read the model object from the model file
        if (null == modelF || !Files.exists(modelF) || !Files.isRegularFile(modelF) || !Files.isReadable(modelF)) {
            System.err.println("Could not read model from CMF file " + modelF);
            return 1;
        }

        Model model;
        try {
            var mr = new ModelXMLReader();  
            model = mr.readFiles(modelF.toFile());
        } catch (Exception ex) {
            System.err.println("Could not read model from CMF file " + modelF + ": " + ex.getMessage());
            return 1;
        }
        if (null == model) {
            System.err.println("Could not read model from CMF file " + modelF);
            return 1;
        }

        // Read the mapping file if one was provided
        Mapping map = null;
        if (null != mapF) {
            if (!Files.exists(mapF) || !Files.isRegularFile(mapF) || !Files.isReadable(mapF)) {
                System.err.println(String.format("Can't read mapping file %s", mapF));
                return 1;
            }
            try {
                map = Mapping.readFile(mapF.toFile());
            } catch (IOException | CMFException ex) {
                System.err.println(String.format("Can't read mapping file %s: %s", mapF, ex.getMessage()));
                return 1;
            }
        }

        // Validate mapping against the model if one was provided
        if (null != map) {
            try {
                map.validateAgainstModel(model);
            } catch (CMFException ex) {
                System.err.println("Invalid mapping for model: " + ex.getMessage());
                return 1;
            }
        }

        // Create context
        JsonObject cxt;
        try {
            cxt = Context.create(model, map);
        } catch (CMFException ex) {
            System.err.println("Can't create context: " + ex.getMessage());
            return 1;
        } catch (RuntimeException ex) {
            System.err.println("Can't create context: " + ex.getMessage());
            return 1;
        }

        if (validateOnly) {
            System.out.println("OK");
            return 0;
        }

        // Create context and write to output stream
        try {
            if (isStdoutTarget()) {
                return writeToStdout(cxt);
            } else {
                return writeToFileAtomically(cxt, Path.of(outName));
            }
        } catch (IOException | RuntimeException ex) {
            System.err.println("Can't write context: " + ex.getMessage());
            return 1;
        }
    }
    
    private int validateOutputTarget() {
        if (isStdoutTarget()) return 0;
        
        Path outPath = Path.of(outName).toAbsolutePath().normalize();
        if (Files.exists(outPath) && Files.isDirectory(outPath)) {
            System.err.println("Can't write to output file " + outPath + ": is a directory");
            return 1;
        }
        if (Files.exists(outPath) && !force) {
            System.err.println("Can't write to output file " + outPath + ": file exists (use --force to overwrite)");
            return 1;
        }
        Path parent = outPath.getParent();
        if (null != parent) {
            if (!Files.exists(parent)) {
                System.err.println("Can't write to output file " + outPath + ": parent directory does not exist");
                return 1;
            }
            if (!Files.isDirectory(parent)) {
                System.err.println("Can't write to output file " + outPath + ": parent is not a directory");
                return 1;
            }
            if (!Files.isWritable(parent)) {
                System.err.println("Can't write to output file " + outPath + ": parent directory is not writable");
                return 1;
            }
        }
        return 0;
    }
    
    private int writeToStdout(JsonObject cxt) throws IOException {
        var ow = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
        Context.write(cxt, ow);
        ow.write("\n");
        ow.flush();
        return 0;
    }
    
    private int writeToFileAtomically(JsonObject cxt, Path target) throws IOException {
        Path absTarget = target.toAbsolutePath().normalize();
        Path dir = absTarget.getParent();
        if (null == dir) dir = Path.of(".").toAbsolutePath().normalize();
        
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(dir, absTarget.getFileName().toString() + ".", ".tmp");
            try (Writer ow = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                Context.write(cxt, ow);
                ow.write("\n");
            }
            moveIntoPlace(tempFile, absTarget);
            tempFile = null;
            return 0;
        } finally {
            if (null != tempFile) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ex) {
                    // ignore cleanup failure
                }
            }
        }
    }
    
    private void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            if (force) {
                Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } else {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException ex) {
            if (force) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
        }
    }
    
    private boolean isStdoutTarget() {
        return null == outName || outName.isBlank() || "-".equals(outName);
    }
}
