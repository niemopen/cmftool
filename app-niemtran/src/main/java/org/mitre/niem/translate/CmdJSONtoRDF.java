package org.mitre.niem.translate;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ModelXMLReader;
import org.mitre.niem.utility.AtomicPathWriter;
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
    name = "j2r",
    description = {
        "convert NIEM JSON message to RDF",
        "With one msg.json and no -o/--output, writes RDF to standard output.",
        "With multiple msg.json files, writes multiple msg.rdf output files"
    },
    mixinStandardHelpOptions = true,
    sortOptions = false
)
public class CmdJSONtoRDF implements Callable<Integer> {

    @Option(
        names = {"-c", "--context"},
        paramLabel = "context.json",
        description = "JSON-LD context file used to interpret input messages"
    )
    private Path contextPath;

    @Option(
        names = {"-f", "--force"},
        description = "overwrite existing output files"
    )
    private boolean force = false;

    @Option(
        names = {"-o", "--output"},
        paramLabel = "out.rdf",
        description = "write output to out.rdf; only valid when there is a single msg.json argument"
    )
    private Path outputPath;

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

    CmdJSONtoRDF() {
    }

    public static void main(String[] args) {
        int rc = new CommandLine(new CmdJSONtoRDF()).execute(args);
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

        String contextS = null;
        if (contextPath != null) {
            rc = validateReadableFile(contextPath, "context file");
            if (rc != 0) {
                return rc;
            }
            try {
                contextS = Files.readString(contextPath, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                System.err.println(String.format("Error reading %s: %s", contextPath, ex.getMessage()));
                return 1;
            }
        }

        final JSONMsgToRDF converter = new JSONMsgToRDF();
        try {
            converter.setContext(contextS);
            converter.setModel(model);
        } catch (NIEMTranException ex) {
            System.err.println("Conversion setup error: " + ex.getMessage());
            return 1;
        }

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

            Path rdfPath = null;
            if (!writeToStdout) {
                rdfPath = (outputPath != null) ? outputPath : toRdfPath(jsonPath);
                if (outputPath == null) {
                    rc = validateWritableOutputPath(rdfPath);
                    if (rc != 0) {
                        hadError = true;
                        continue;
                    }
                }
            }

            if (writeToStdout) {
                try {
                    writeRdfToStdout(converter, jsonPath);
                } catch (IOException ex) {
                    System.err.println(String.format("Error writing stdout for %s: %s", jsonPath, ex.getMessage()));
                    hadError = true;
                } catch (NIEMTranException ex) {
                    System.err.println("Conversion error: " + ex.getMessage());
                    hadError = true;
                }
            } else {
                final Path finalRdfPath = rdfPath;
                try {
                    AtomicPathWriter.writeAtomically(finalRdfPath, StandardCharsets.UTF_8, rdfW -> {
                        try {
                            writeRDFMessage(converter, jsonPath, rdfW);
                        } catch (IOException | NIEMTranException ex) {
                            throw new WrappedNIEMTranException(ex);
                        }
                    });
                } catch (WrappedNIEMTranException ex) {
                    var cause = ex.getCause();
                    System.err.println(String.format("Error writing %s: %s", finalRdfPath, cause.getMessage()));
                    hadError = true;
                } catch (IOException ex) {
                    System.err.println(String.format("Error writing %s: %s", finalRdfPath, ex.getMessage()));
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

    private void writeRdfToStdout(JSONMsgToRDF converter, Path sourcePath) throws IOException, NIEMTranException {
        var out = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
        writeRDFMessage(converter, sourcePath, out);
        out.flush();
    }

    private Path toRdfPath(Path jsonPath) {
        Path fileName = jsonPath.getFileName();
        String name = fileName == null ? jsonPath.toString() : fileName.toString();

        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String rdfName = base + ".rdf";

        Path parent = jsonPath.getParent();
        return parent == null ? Path.of(rdfName) : parent.resolve(rdfName);
    }

    private void writeRDFMessage(JSONMsgToRDF converter, Path sourcePath, Writer rdfW) throws IOException, NIEMTranException {
        try (Reader msgR = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            converter.convert(msgR, rdfW);
        }
    }

    private static final class WrappedNIEMTranException extends IOException {
        WrappedNIEMTranException(Exception cause) {
            super(cause.getMessage(), cause);
        }
    }
}
