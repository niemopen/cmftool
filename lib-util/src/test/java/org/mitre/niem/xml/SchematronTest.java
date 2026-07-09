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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;

class SchematronTest {

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

    @Test
    void compileSchematronRejectsMissingSystemId() throws Exception {
        var sch = new Schematron();
        var src = new StreamSource(new StringReader(SIMPLE_SCHEMATRON));

        var ex = assertThrows(IllegalArgumentException.class, () -> sch.compileSchematron(src));
        assertTrue(ex.getMessage().contains("systemId"));
    }

    @Test
    void compileSchematronToWriterProducesXslt(@TempDir Path tempDir) throws Exception {
        var schFile = writeFile(tempDir, "simple.sch", SIMPLE_SCHEMATRON);
        var sch = new Schematron();
        var src = new StreamSource(schFile.toFile());
        src.setSystemId(schFile.toUri().toString());

        var out = new StringWriter();
        sch.compileSchematron(src, out);

        var xslt = out.toString();
        assertTrue(xslt.contains("xsl:stylesheet") || xslt.contains("xsl:transform"));
    }

    @Test
    void annotateDocumentAddsLocationAttribute(@TempDir Path tempDir) throws Exception {
        var xmlFile = writeFile(tempDir, "sample.xml", FAILING_XML);
        var sch = new Schematron();

        var doc = sch.annotateDocument(new InputSource(xmlFile.toUri().toString()));

        var root = doc.getDocumentElement();
        assertNotNull(root);

        var loc = root.getAttributeNS(Schematron.SCHEVAL_NS, "location");
        assertTrue(loc.matches("sample\\.xml:\\d+:\\d+"), "location was: " + loc);

        var child = (org.w3c.dom.Element) root.getElementsByTagName("item").item(0);
        var childLoc = child.getAttributeNS(Schematron.SCHEVAL_NS, "location");
        assertTrue(childLoc.matches("sample\\.xml:\\d+:\\d+"), "child location was: " + childLoc);
    }

    @Test
    void endToEndCompileApplyAndFormatMessages(@TempDir Path tempDir) throws Exception {
        var schFile = writeFile(tempDir, "simple.sch", SIMPLE_SCHEMATRON);
        var xmlFile = writeFile(tempDir, "sample.xml", FAILING_XML);

        var schematron = new Schematron();

        var schSrc = new StreamSource(schFile.toFile());
        schSrc.setSystemId(schFile.toUri().toString());

        var compiled = schematron.compileSchematron(schSrc);
        assertNotNull(compiled);

        var xmlSrc = new StreamSource(xmlFile.toFile());
        xmlSrc.setSystemId(xmlFile.toUri().toString());

        var svrlOut = new StringWriter();
        assertDoesNotThrow(() -> schematron.applyXslt(xmlSrc, compiled, svrlOut));

        var svrl = svrlOut.toString();
        assertTrue(svrl.contains("failed-assert"));
        assertTrue(svrl.contains("successful-report"));
        assertTrue(svrl.contains("root must have ok='true'"));
        assertTrue(svrl.contains("item encountered"));

        var msgOut = new StringWriter();
        var svrlInput = new InputSource(new StringReader(svrl));
        var xmlInput = new InputSource(xmlFile.toUri().toString());

        schematron.SVRLtoMessages(svrlInput, xmlInput, msgOut);

        var msgs = msgOut.toString();
        assertTrue(msgs.contains("ERROR"));
        assertTrue(msgs.contains("WARN "));
        assertTrue(msgs.contains("sample.xml:"));
        assertTrue(msgs.contains("root must have ok='true'"));
        assertTrue(msgs.contains("item encountered"));
    }

    private static Path writeFile(Path dir, String name, String content) throws Exception {
        var path = dir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }
}
