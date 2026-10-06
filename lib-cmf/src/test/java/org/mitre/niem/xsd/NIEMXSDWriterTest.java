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
package org.mitre.niem.xsd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mitre.niem.xml.ParserBootstrap;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

class NIEMXSDWriterTest {

    @Test
    void ordersLocalTermAttributes() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:appinfo="http://release.niem.gov/niem/appinfo/5.0/">
              <xs:annotation>
                <xs:appinfo>
                  <appinfo:LocalTerm literal="x" definition="y" term="Alpha"/>
                </xs:appinfo>
              </xs:annotation>
            </xs:schema>
            """);

        var out = new StringWriter();
        new NIEMXSDWriter("appinfo").writeXML(dom, out);
        var text = out.toString();

        assertTrue(text.contains(
            "<appinfo:LocalTerm definition=\"y\" literal=\"x\" term=\"Alpha\"/>")
            || text.contains(
            "<appinfo:LocalTerm term=\"Alpha\" definition=\"y\" literal=\"x\"/>"));

        int termPos = text.indexOf("term=\"Alpha\"");
        int defPos = text.indexOf("definition=\"y\"");
        int litPos = text.indexOf("literal=\"x\"");
        assertTrue(termPos >= 0);
        assertTrue(defPos >= 0);
        assertTrue(litPos >= 0);
        assertTrue(termPos < defPos);
        assertTrue(termPos < litPos);
    }

    @Test
    void ordersAugmentationAttributes() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:appinfo="http://release.niem.gov/niem/appinfo/5.0/">
              <xs:annotation>
                <xs:appinfo>
                  <appinfo:Augmentation
                      orderedPropertyIndicator="true"
                      use="optional"
                      property="nc:SomeProperty"
                      class="nc:SomeType"
                      globalClassCode="GCC"/>
                </xs:appinfo>
              </xs:annotation>
            </xs:schema>
            """);

        var out = new StringWriter();
        new NIEMXSDWriter("appinfo").writeXML(dom, out);
        var text = out.toString();

        int classPos = text.indexOf("class=\"nc:SomeType\"");
        int propertyPos = text.indexOf("property=\"nc:SomeProperty\"");
        int usePos = text.indexOf("use=\"optional\"");
        int gccPos = text.indexOf("globalClassCode=\"GCC\"");
        int otherPos = text.indexOf("orderedPropertyIndicator=\"true\"");

        assertTrue(classPos >= 0);
        assertTrue(propertyPos >= 0);
        assertTrue(usePos >= 0);
        assertTrue(gccPos >= 0);
        assertTrue(otherPos >= 0);

        assertTrue(classPos < propertyPos);
        assertTrue(propertyPos < usePos);
        assertTrue(usePos < gccPos);
        assertTrue(gccPos < otherPos);
    }

    @Test
    void stillAppliesNormalXsdOrderingRules() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
              <xs:element substitutionGroup="ex:Base"
                          maxOccurs="2"
                          type="xs:string"
                          name="Root"
                          minOccurs="0"
                          xmlns:ex="http://example.com/test"/>
            </xs:schema>
            """);

        var out = new StringWriter();
        new NIEMXSDWriter("appinfo").writeXML(dom, out);
        var text = out.toString();

        assertTrue(text.contains(
            "<xs:element xmlns:ex=\"http://example.com/test\" name=\"Root\" type=\"xs:string\" minOccurs=\"0\" maxOccurs=\"2\" substitutionGroup=\"ex:Base\"/>"
        ));
    }

    @Test
    void differentConfiguredPrefixDoesNotTriggerNiemOrdering() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:appinfo="http://release.niem.gov/niem/appinfo/5.0/">
              <xs:annotation>
                <xs:appinfo>
                  <appinfo:LocalTerm literal="x" definition="y" term="Alpha"/>
                </xs:appinfo>
              </xs:annotation>
            </xs:schema>
            """);

        var out = new StringWriter();
        new NIEMXSDWriter("other").writeXML(dom, out);
        var text = out.toString();

        int termPos = text.indexOf("term=\"Alpha\"");
        int defPos = text.indexOf("definition=\"y\"");
        int litPos = text.indexOf("literal=\"x\"");

        assertTrue(termPos >= 0);
        assertTrue(defPos >= 0);
        assertTrue(litPos >= 0);

        // Without NIEM-specific ordering, natural ordering puts definition and literal before term.
        assertTrue(defPos < termPos);
        assertTrue(litPos < termPos);
    }

    @Test
    void writesToFile(@TempDir Path tempDir) throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:appinfo="http://release.niem.gov/niem/appinfo/5.0/">
              <xs:annotation>
                <xs:appinfo>
                  <appinfo:Augmentation property="p" class="c" use="u" globalClassCode="g"/>
                </xs:appinfo>
              </xs:annotation>
            </xs:schema>
            """);

        var outFile = tempDir.resolve("niem.xsd").toFile();
        new NIEMXSDWriter("appinfo").writeXML(dom, outFile);

        var text = Files.readString(outFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(text.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"));
        assertTrue(text.contains("class=\"c\" property=\"p\" use=\"u\" globalClassCode=\"g\""));
    }

    @Test
    void nodeToTextWorks() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:appinfo="http://release.niem.gov/niem/appinfo/5.0/">
              <xs:annotation>
                <xs:appinfo>
                  <appinfo:LocalTerm term="Alpha" literal="x"/>
                </xs:appinfo>
              </xs:annotation>
            </xs:schema>
            """);

        var text = new NIEMXSDWriter("appinfo").nodeToText(dom);

        assertTrue(text.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"));
        assertTrue(text.contains("<appinfo:LocalTerm"));
    }

    @Test
    void serializesStandaloneElement() throws Exception {
        var dom = parseXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:appinfo="http://release.niem.gov/niem/appinfo/5.0/">
              <xs:annotation>
                <xs:appinfo>
                  <appinfo:Augmentation property="p" class="c" use="u" globalClassCode="g"/>
                </xs:appinfo>
              </xs:annotation>
            </xs:schema>
            """);

        var aug = (org.w3c.dom.Element) dom.getDocumentElement()
            .getElementsByTagNameNS("http://release.niem.gov/niem/appinfo/5.0/", "Augmentation")
            .item(0);

        var out = new StringWriter();
        new NIEMXSDWriter("appinfo").writeXML(aug, out);

        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <appinfo:Augmentation
              xmlns:appinfo="http://release.niem.gov/niem/appinfo/5.0/"
              xmlns:xs="http://www.w3.org/2001/XMLSchema"
              class="c"
              property="p"
              use="u"
              globalClassCode="g"/>
            """, out.toString());
    }

    private static Document parseXml(String xml) throws Exception {
        var db = ParserBootstrap.docBuilder();
        return db.parse(new InputSource(new StringReader(xml)));
    }
}
