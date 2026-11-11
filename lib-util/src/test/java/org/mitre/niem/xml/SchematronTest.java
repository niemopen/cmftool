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
package org.mitre.niem.xml;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XsltCompiler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mitre.niem.utility.ClasspathResources;
import org.xml.sax.InputSource;

class SchematronTest {

    private static final Class<?> ANCHOR = Schematron.class;

    private static final String SIMPLE_SCHEMATRON = """
        <?xml version="1.0" encoding="UTF-8"?>
        <schema xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
          <pattern id="p1">
            <rule context="/root">
              <assert test="@ok='true'">root must have ok='true'</assert>
            </rule>
            <rule context="/root/item">
              <report test="true()">item encountered</report>
            </rule>
          </pattern>
        </schema>
        """;

    private static final String FAILING_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
          <item/>
        </root>
        """;

    private static final String MAIN_SCHEMATRON_WITH_INCLUDE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <schema xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
          <title>Include test</title>
          <include href="included-pattern.sch"/>
        </schema>
        """;

    private static final String INCLUDED_PATTERN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <pattern xmlns="http://purl.oclc.org/dsdl/schematron" id="included-pattern">
          <rule context="/root">
            <assert test="@ok='true'">root must have ok='true'</assert>
          </rule>
          <rule context="/root/item">
            <report test="true()">item encountered from include</report>
          </rule>
        </pattern>
        """;

    static Stream<Named<String>> bundledStylesheets() {
        return Stream.of(
            Named.of(Schematron.ISO_DSDL, "/sch/" + Schematron.ISO_DSDL),
            Named.of(Schematron.ISO_ABSTRACT, "/sch/" + Schematron.ISO_ABSTRACT),
            Named.of(Schematron.ISO_SVRL, "/sch/" + Schematron.ISO_SVRL),
            Named.of(Schematron.XSLT_PP, "/sch/" + Schematron.XSLT_PP)
        );
    }

    @Test
    void compileSchematronRejectsMissingSystemId() throws Exception {
        var sch = new Schematron();
        var src = new StreamSource(new StringReader(SIMPLE_SCHEMATRON));

        var ex = assertThrows(IllegalArgumentException.class, () -> sch.compileSchematron(src));
        assertTrue(ex.getMessage().contains("systemId"));
    }

    @Test
    void compileSchematronExecutableFromInMemorySourceUsesBundledClasspathXslt() throws Exception {
        var sch = new Schematron();
        var src = schematronSource(SIMPLE_SCHEMATRON, "file:/virtual/simple.sch");

        var exec = sch.compileSchematronExecutable(src);

        assertNotNull(exec);
    }

    @Test
    void compileSchematronToWriterProducesXslt() throws Exception {
        var sch = new Schematron();
        var src = schematronSource(SIMPLE_SCHEMATRON, "file:/virtual/simple.sch");

        var out = new StringWriter();
        sch.compileSchematron(src, out);

        var xslt = out.toString();
        assertAll(
            () -> assertTrue(xslt.contains("xsl:stylesheet") || xslt.contains("xsl:transform")),
            () -> assertTrue(xslt.contains("root must have ok='true'")),
            () -> assertTrue(xslt.contains("item encountered"))
        );
    }

    @Test
    void annotateDocumentAddsLocationAttribute(@TempDir Path tempDir) throws Exception {
        var xmlFile = writeFile(tempDir, "sample.xml", FAILING_XML);
        var sch = new Schematron();

        var doc = sch.annotateDocument(new InputSource(xmlFile.toUri().toString()));

        var root = doc.getDocumentElement();
        assertNotNull(root);

        var loc = root.getAttributeNS(Schematron.SCHEVAL_NS, "location");
        var child = (org.w3c.dom.Element) root.getElementsByTagName("item").item(0);
        var childLoc = child.getAttributeNS(Schematron.SCHEVAL_NS, "location");

        assertAll(
            () -> assertTrue(loc.matches("sample\\.xml:\\d+:\\d+"), "location was: " + loc),
            () -> assertTrue(childLoc.matches("sample\\.xml:\\d+:\\d+"), "child location was: " + childLoc)
        );
    }

    @Test
    void endToEndCompileApplyAndFormatMessages(@TempDir Path tempDir) throws Exception {
        var xmlFile = writeFile(tempDir, "sample.xml", FAILING_XML);

        var schematron = new Schematron();

        var schSrc = schematronSource(SIMPLE_SCHEMATRON, "file:/virtual/simple.sch");
        var compiled = schematron.compileSchematron(schSrc);
        assertNotNull(compiled);

        var xmlSrc = new StreamSource(xmlFile.toFile());
        xmlSrc.setSystemId(xmlFile.toUri().toString());

        var svrlOut = new StringWriter();
        schematron.applyXslt(xmlSrc, compiled, svrlOut);

        var svrl = svrlOut.toString();
        assertAll(
            () -> assertTrue(svrl.contains("failed-assert")),
            () -> assertTrue(svrl.contains("successful-report")),
            () -> assertTrue(svrl.contains("root must have ok='true'")),
            () -> assertTrue(svrl.contains("item encountered"))
        );

        var msgOut = new StringWriter();
        var svrlInput = new InputSource(new StringReader(svrl));
        var xmlInput = new InputSource(xmlFile.toUri().toString());

        schematron.SVRLtoMessages(svrlInput, xmlInput, msgOut);

        var msgs = msgOut.toString();
        assertAll(
            () -> assertTrue(msgs.contains("ERROR")),
            () -> assertTrue(msgs.contains("WARN ")),
            () -> assertTrue(msgs.contains("sample.xml:")),
            () -> assertTrue(msgs.contains("root must have ok='true'")),
            () -> assertTrue(msgs.contains("item encountered"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bundledStylesheets")
    @DisplayName("Bundled Schematron XSLT resources exist on the classpath")
    void bundledStylesheetExists(String resourceName) {
        assertTrue(ClasspathResources.exists(ANCHOR, resourceName));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bundledStylesheets")
    @DisplayName("Bundled Schematron XSLT resources compile from the classpath")
    void bundledStylesheetCompilesFromClasspath(String resourceName) throws Exception {
        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();

        var url = ClasspathResources.requireUrl(ANCHOR, resourceName);
        assertNotNull(url);

        try (InputStream in = ClasspathResources.openStream(ANCHOR, resourceName)) {
            var source = new StreamSource(in, url.toExternalForm());
            var executable = compiler.compile(source);
            assertNotNull(executable);
        }
    }

    @Test
    void compileSchematronResolvesRelativeIncludeFromSystemId(@TempDir Path tempDir) throws Exception {
        Path mainFile = writeFile(tempDir, "main.sch", MAIN_SCHEMATRON_WITH_INCLUDE);
        writeFile(tempDir, "included-pattern.sch", INCLUDED_PATTERN);
        Path xmlFile = writeFile(tempDir, "sample.xml", FAILING_XML);

        var schematron = new Schematron();

        var schSrc = new StreamSource(mainFile.toFile());
        schSrc.setSystemId(mainFile.toUri().toString());

        var compiled = schematron.compileSchematron(schSrc);
        assertNotNull(compiled);

        var xmlSrc = new StreamSource(xmlFile.toFile());
        xmlSrc.setSystemId(xmlFile.toUri().toString());

        var svrlOut = new StringWriter();
        schematron.applyXslt(xmlSrc, compiled, svrlOut);

        var svrl = svrlOut.toString();
        assertAll(
            () -> assertTrue(svrl.contains("failed-assert")),
            () -> assertTrue(svrl.contains("successful-report")),
            () -> assertTrue(svrl.contains("root must have ok='true'")),
            () -> assertTrue(svrl.contains("item encountered from include"))
        );
    }

    private static StreamSource schematronSource(String text, String systemId) {
        var src = new StreamSource(new StringReader(text));
        src.setSystemId(systemId);
        return src;
    }

    private static Path writeFile(Path dir, String name, String content) throws Exception {
        var path = dir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }
}
