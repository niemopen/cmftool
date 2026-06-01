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
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import static org.apache.commons.io.FilenameUtils.normalize;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.utility.ResourceManager;
import static org.mitre.niem.utility.URIfuncs.URIStringToFile;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "mval",
    description = "validate a CMF model file",
    mixinStandardHelpOptions = true
)
public class CmdCMFValidate implements Callable<Integer> {

    private final ResourceManager rmgr = new ResourceManager(Model.class);

    @Parameters(
        arity = "1..*",
        paramLabel = "model.cmf",
        description = "model.cmf ..."
    )
    private List<String> mainArgs = new ArrayList<>();

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new CmdCMFValidate());
        cmd.setCommandName("cmfvalidate");
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        Schema schema = loadSchema();
        if (schema == null) {
            return 1;
        }

        boolean hadErrors = false;

        for (String cmfN : mainArgs) {
            File cmfFile = new File(cmfN);
            if (!cmfFile.isFile() || !cmfFile.canRead()) {
                System.err.printf("%s: file not found or not readable%n", cmfN);
                hadErrors = true;
                continue;
            }

            Handler fileHandler = new Handler();
            try {
                var validator = schema.newValidator();
                validator.setErrorHandler(fileHandler);
                validator.validate(new StreamSource(cmfFile));
            } catch (SAXException ex) {
                System.err.printf("%s: SAX exception: %s%n", cmfN, ex.getMessage());
                hadErrors = true;
                continue;
            } catch (IOException ex) {
                System.err.printf("%s: IO exception: %s%n", cmfN, ex.getMessage());
                hadErrors = true;
                continue;
            }

            String msgs = fileHandler.messages();
            if (msgs.isEmpty()) {
                System.out.printf("%s: OK%n", cmfN);
            } else {
                System.err.printf("%s:%n%s", cmfN, msgs);
                hadErrors = true;
            }
        }

        return hadErrors ? 1 : 0;
    }

    private Schema loadSchema() {
        SchemaFactory sfact = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        configureSchemaFactory(sfact);

        Handler schemaHandler = new Handler();
        sfact.setErrorHandler(schemaHandler);
        sfact.setResourceResolver(new ResourceResolver(rmgr));

        try (InputStream cmfIS = rmgr.getResourceStream("/xsd/cmf/cmf.xsd")) {
            StreamSource cmfSS = new StreamSource(cmfIS);
            cmfSS.setSystemId("/xsd/cmf/cmf.xsd");

            Schema schema = sfact.newSchema(cmfSS);

            String msgs = schemaHandler.messages();
            if (!msgs.isEmpty()) {
                System.err.print(msgs);
                return null;
            }

            return schema;
        } catch (IOException ex) {
            System.err.println("Can't get cmf.xsd: " + ex.getMessage());
            return null;
        } catch (SAXException ex) {
            System.err.println("Can't create CMF schema: " + ex.getMessage());
            return null;
        }
    }

    private void configureSchemaFactory(SchemaFactory sfact) {
        try {
            sfact.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Exception ex) {
            // ignore if unsupported
        }
        try {
            sfact.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (Exception ex) {
            // ignore if unsupported
        }
        try {
            sfact.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (Exception ex) {
            // ignore if unsupported
        }
    }

    private static class Handler extends DefaultHandler {
        private StringBuilder msgs = new StringBuilder();

        @Override
        public void error(SAXParseException e) {
            addMessage("ERROR", e);
        }

        @Override
        public void fatalError(SAXParseException e) {
            addMessage("FATAL", e);
        }

        @Override
        public void warning(SAXParseException e) {
            addMessage("WARN", e);
        }

        private void addMessage(String label, SAXParseException e) {
            String fileName = "<unknown>";
            try {
                if (e.getSystemId() != null) {
                    fileName = URIStringToFile(e.getSystemId()).getName();
                }
            } catch (Exception ex) {
                if (e.getSystemId() != null) {
                    fileName = e.getSystemId();
                }
            }

            msgs.append(String.format(
                "[%s] %s:%d: %s%n",
                label,
                fileName,
                e.getLineNumber(),
                e.getMessage()
            ));
        }

        public String messages() {
            return msgs.toString();
        }
    }

    private static class ResourceResolver implements LSResourceResolver {

        private static DOMImplementationLS domLSI;
        private final ResourceManager rmgr;

        ResourceResolver(ResourceManager rmgr) {
            this.rmgr = rmgr;
        }

        @Override
        public LSInput resolveResource(
            String type,
            String namespaceURI,
            String publicId,
            String systemId,
            String baseURI
        ) {
            try {
                String nsid = resolveResourcePath(systemId, baseURI);
                InputStream is = rmgr.getResourceStream(nsid);

                LSInput lsi = createLSInput();
                lsi.setByteStream(is);
                lsi.setPublicId(publicId);
                lsi.setSystemId(nsid);
                lsi.setBaseURI(baseURI);
                return lsi;
            } catch (Exception ex) {
                System.err.println("Can't get " + systemId + ": " + ex.getMessage());
                return null;
            }
        }

        private String resolveResourcePath(String systemId, String baseURI) {
            if (systemId == null || systemId.isBlank()) {
                throw new IllegalArgumentException("Missing systemId");
            }

            String path;
            if (baseURI == null || baseURI.isBlank()) {
                path = systemId;
            } else {
                URI base = URI.create(baseURI);
                URI resolved = base.resolve(systemId);
                path = resolved.getPath();
            }

            String normalized = normalize(path, true);
            if (normalized == null || normalized.isBlank()) {
                throw new IllegalArgumentException("Can't normalize path: " + path);
            }

            return normalized.startsWith("/") ? normalized : "/" + normalized;
        }

        private static LSInput createLSInput() throws Exception {
            if (domLSI == null) {
                domLSI = (DOMImplementationLS) DOMImplementationRegistry.newInstance()
                    .getDOMImplementation("LS");
            }
            return domLSI.createLSInput();
        }
    }
}
