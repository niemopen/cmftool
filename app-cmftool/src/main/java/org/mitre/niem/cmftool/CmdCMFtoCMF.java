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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.cmf.ModelXMLWriter;
import org.mitre.niem.cmf.Namespace;
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
    name = "m2m",
    description = "canonicalize or extract CMF from CMF",
    mixinStandardHelpOptions = true
)
public class CmdCMFtoCMF implements Callable<Integer> {
    
    @Option(
        names = {"-o", "--output"},
        paramLabel = "FILE|-",
        description = "name of output model file; use '-' for stdout"
    )
    private String modelFN = null;
    
    @Option(
        names = "--force",
        description = "overwrite output file if it already exists"
    )
    private boolean force = false;
    
    @Option(
        names = "--only",
        paramLabel = "p1[,p2...]",
        hideParamSyntax = true,
        description = "include only these namespace URIs or prefixes; eg. '--only nc,j'"
    )
    private List<String> onlyArg = new ArrayList<>();
        
    @Parameters(description = "modelFile.cmf...", arity = "1..*")
    private List<Path> mainArgs = new ArrayList<>();
    
    public static void main (String[] args) {       
        var cmd = new CommandLine(new CmdCMFtoCMF());
        cmd.setCommandName("m2m");
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call ()  {

        // Make sure output model file is writable      
        int rc = validateOutputTarget();
        if (0 != rc) return rc;
        
        // Make sure the Xerces parsers can be initialized
        try {
            ParserBootstrap.init(BOOTSTRAP_ALL);
        } catch (ParserConfigurationException ex) {
            System.err.println("Internal parser error: " + ex.getMessage());
            return 1;
        }
        
        // Read the model object from the model instance file
        // Read the model object from the model file(s)
        Model model;
        try {
            var mr = new ModelXMLReader();  
            var fileL = new ArrayList<File>();
            for (var path : mainArgs) {
                if (!Files.exists(path)) {
                    System.err.println("Can't read input file " + path + ": file does not exist");
                    return 1;
                }
                if (!Files.isRegularFile(path)) {
                    System.err.println("Can't read input file " + path + ": not a regular file");
                    return 1;
                }
                if (!Files.isReadable(path)) {
                    System.err.println("Can't read input file " + path + ": file is not readable");
                    return 1;
                }
                fileL.add(path.toFile());
            }
            model = mr.readFiles(fileL);
        } catch (Exception ex) {
            System.err.println("CMF processing error: " + ex.getMessage());
            return 1;
        }
        
        // Convert onlyArg to list of namespace URIs/prefixes
        var onlyL = normalizeOnlyArgs(onlyArg);
        rc = validateOnlyArgs(model, onlyL);
        if (0 != rc) return rc;
        
        // Write model to output
        try {
            var mw = new ModelXMLWriter();
            if (isStdoutTarget()) {
                return writeToStdout(mw, model, onlyL);
            } else {
                return writeToFileAtomically(mw, model, onlyL, Path.of(modelFN));
            }
        } catch (Exception ex) {
            System.err.println("Output error: " + ex.getMessage());
            return 1;
        }
    }
    
    private int validateOutputTarget() {
        if (isStdoutTarget()) return 0;
        
        Path outPath = Path.of(modelFN).toAbsolutePath().normalize();
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
    
    private List<String> normalizeOnlyArgs(List<String> raw) {
        var vals = new LinkedHashSet<String>();
        for (var s : raw) {
            if (null == s) continue;
            var v = s.strip();
            if (!v.isEmpty()) vals.add(v);
        }
        return new ArrayList<>(vals);
    }
    
    private int validateOnlyArgs(Model model, List<String> onlyL) {
        if (onlyL.isEmpty()) return 0;
        
        Set<String> known = new LinkedHashSet<>();
        for (Namespace ns : model.namespaceList()) {
            if (null != ns.prefix() && !ns.prefix().isBlank()) known.add(ns.prefix());
            if (null != ns.uri() && !ns.uri().isBlank()) known.add(ns.uri());
        }
        
        var bad = onlyL.stream()
            .filter(v -> !known.contains(v))
            .collect(Collectors.toList());
        
        if (!bad.isEmpty()) {
            System.err.println("Unknown --only value(s): " + String.join(", ", bad));
            System.err.println("Known namespace prefixes/URIs: " + String.join(", ", known));
            return 1;
        }
        return 0;
    }
    
    private int writeToStdout(ModelXMLWriter mw, Model model, List<String> onlyL) throws IOException {
        var ow = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
        boolean good;
        if (onlyL.isEmpty()) good = mw.writeXML(model, ow);
        else good = mw.writeXML(model, onlyL, ow);
        ow.flush();
        if (!good) {
            System.err.println("Failed to write output model");
            return 1;
        }
        return 0;
    }
    
    private int writeToFileAtomically(ModelXMLWriter mw, Model model, List<String> onlyL, Path target) throws IOException {
        Path absTarget = target.toAbsolutePath().normalize();
        Path dir = absTarget.getParent();
        if (null == dir) dir = Path.of(".").toAbsolutePath().normalize();
        
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(dir, absTarget.getFileName().toString() + ".", ".tmp");
            try (Writer ow = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                boolean good;
                if (onlyL.isEmpty()) good = mw.writeXML(model, ow);
                else good = mw.writeXML(model, onlyL, ow);
                if (!good) {
                    System.err.println("Failed to write output model");
                    return 1;
                }
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
        return null == modelFN || modelFN.isBlank() || "-".equals(modelFN);
    }
    
}
