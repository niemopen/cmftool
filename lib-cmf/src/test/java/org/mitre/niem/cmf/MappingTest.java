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
package org.mitre.niem.cmf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MappingTest {

    private Mapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new Mapping();
    }

    @Test
    @DisplayName("readFile reads an existing mapping file")
    void readFile() throws Exception {
        var map = Mapping.readFile(new File("src/test/resources/json/map-request.ttl"));
        assertNotNull(map);
    }

    @Test
    @DisplayName("createTemplate creates writable Turtle with TEMP mappings")
    void testCreateTemplate() throws Exception {
        var rdr = new ModelXMLReader();
        var m = rdr.readFiles(new File("src/test/resources/json/request.cmf"));
        var map = Mapping.createTemplate(m, "sj", "http://example.com/ReqRes/1.0/simpleJSON");

        var ow = new StringWriter();
        map.write(ow);
        var s = ow.toString();

        assertNotNull(map);
        assertTrue(Pattern.compile("@prefix sj: +<http://example.com/ReqRes/1.0/simpleJSON>")
            .matcher(s)
            .find());
        assertTrue(Pattern.compile("msg:Request +owl:equivalentProperty sj:TEMP")
            .matcher(s)
            .find());        
        assertTrue(s.contains("owl:equivalentProperty"));
        assertTrue(s.contains("sj:TEMP"));
    }

    @Test
    @DisplayName("assignPrefix returns the requested prefix when unused")
    void assignPrefixSimple() throws CMFException {
        String p = mapping.assignPrefix("ex", "http://example.org/");
        assertEquals("ex", p);
        assertEquals("http://example.org/", mapping.nsmap.getURI("ex"));
    }

    @Test
    @DisplayName("assignPrefix munges a duplicate prefix")
    void assignPrefixMunge() throws CMFException {
        mapping.assignPrefix("ex", "http://example.org/1");
        String p2 = mapping.assignPrefix("ex", "http://example.org/2");

        assertNotEquals("ex", p2);
        assertEquals("http://example.org/2", mapping.nsmap.getURI(p2));
    }

    @Test
    @DisplayName("addMapping stores and retrieves a simple mapping")
    void addAndLookupMapping() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt", "http://tgt/");

        String srcQN = "src:propA";
        String tgtQN = "tgt:propB";

        mapping.addMapping(srcQN, tgtQN);

        assertEquals(tgtQN, mapping.qnToMappedQN(srcQN));
        assertEquals(tgtQN, mapping.qnToMappedName(srcQN));
    }

    @Test
    @DisplayName("qnToMappedName returns only the local name when noPrefix is true")
    void noPrefixBehaviour() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt", "http://tgt/");

        String srcQN = "src:propA";
        String tgtQN = "tgt:propB";

        mapping.addMapping(srcQN, tgtQN);
        assertTrue(mapping.setNoPrefix(true));
        assertEquals("propB", mapping.qnToMappedName(srcQN));
    }

    @Test
    @DisplayName("setNoPrefix returns false when more than one target namespace exists")
    void setNoPrefixTooManyNamespaces() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt1", "http://tgt1/");
        mapping.assignPrefix("tgt2", "http://tgt2/");

        mapping.addMapping("src:propA", "tgt1:propB");
        mapping.addMapping("src:propC", "tgt2:propD");

        assertFalse(mapping.setNoPrefix(true));
    }

    @Test
    @DisplayName("assignPrefix rejects different target prefixes for the same target namespace URI")
    void rejectDifferentTargetPrefixesSameNamespace() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt1", "http://same-target/");
        CMFException ex = assertThrows(
            CMFException.class,
            () -> mapping.assignPrefix("tgt2", "http://same-target/"));
        assertTrue(ex.getMessage().contains("already assigned"));
    }

    @Test
    @DisplayName("noPrefix becomes false when a second target namespace is added")
    void setNoPrefixBecomesFalse() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt1", "http://tgt1/");
        mapping.assignPrefix("tgt2", "http://tgt2/");

        mapping.addMapping("src:propA", "tgt1:propB");
        assertTrue(mapping.setNoPrefix(true));
        assertTrue(mapping.noPrefix());

        mapping.addMapping("src:propC", "tgt2:propD");
        assertFalse(mapping.noPrefix());
    }

    @Test
    @DisplayName("addMapping rejects undeclared source prefix")
    void undeclaredSourcePrefix() throws CMFException {
        mapping.assignPrefix("tgt", "http://tgt/");

        CMFException ex = assertThrows(
            CMFException.class,
            () -> mapping.addMapping("src:propA", "tgt:propB")
        );
        assertTrue(ex.getMessage().contains("Undeclared source prefix"));
    }

    @Test
    @DisplayName("addMapping rejects undeclared target prefix")
    void undeclaredTargetPrefix() throws CMFException {
        mapping.assignPrefix("src", "http://src/");

        CMFException ex = assertThrows(
            CMFException.class,
            () -> mapping.addMapping("src:propA", "tgt:propB")
        );
        assertTrue(ex.getMessage().contains("Undeclared target prefix"));
    }

    @Test
    @DisplayName("addMapping rejects duplicate source with different target")
    void duplicateSourceConflict() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt", "http://tgt/");

        mapping.addMapping("src:propA", "tgt:propB");

        CMFException ex = assertThrows(
            CMFException.class,
            () -> mapping.addMapping("src:propA", "tgt:propC")
        );
        assertTrue(ex.getMessage().contains("already mapped"));
    }

    @Test
    @DisplayName("addMapping rejects duplicate target with different source")
    void duplicateTargetConflict() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt", "http://tgt/");

        mapping.addMapping("src:propA", "tgt:propB");

        CMFException ex = assertThrows(
            CMFException.class,
            () -> mapping.addMapping("src:propC", "tgt:propB")
        );
        assertTrue(ex.getMessage().contains("already mapped"));
    }

    @Nested
    @DisplayName("Turtle serialization")
    class TurtleRoundTrip {

        private final String srcPrefix = "src";
        private final String tgtPrefix = "tgt";

        @BeforeEach
        void initPrefixes() throws CMFException {
            mapping.assignPrefix(srcPrefix, "http://src/");
            mapping.assignPrefix(tgtPrefix, "http://tgt/");
        }

        @Test
        @DisplayName("write produces valid Turtle and read restores the mapping")
        void writeReadRoundTrip() throws IOException, CMFException {
            mapping.addMapping(srcPrefix + ":a", tgtPrefix + ":A");
            mapping.addMapping(srcPrefix + ":b", tgtPrefix + ":B");
            mapping.addMapping(srcPrefix + ":c", tgtPrefix + ":C");

            StringWriter sw = new StringWriter();
            mapping.write(sw);
            String turtle = sw.toString();

            assertAll(
                () -> assertTrue(turtle.contains("@prefix src: <http://src/>")),
                () -> assertTrue(turtle.contains("@prefix tgt: <http://tgt/>")),
                () -> assertTrue(turtle.contains("@prefix owl:")),
                () -> assertTrue(turtle.contains("owl:equivalentProperty"))
            );

            Reader r = new StringReader(turtle);
            Mapping readBack = Mapping.read(r);

            List<String> srcs = List.of(srcPrefix + ":a", srcPrefix + ":b", srcPrefix + ":c");
            for (String srcQN : srcs) {
                assertEquals(mapping.qnToMappedQN(srcQN), readBack.qnToMappedQN(srcQN));
            }

            assertEquals("http://src/", readBack.nsmap.getURI(srcPrefix));
            assertEquals("http://tgt/", readBack.nsmap.getURI(tgtPrefix));
        }

        @Test
        @DisplayName("read fails on malformed Turtle line")
        void readMalformedTurtle() {
            String badTurtle = """
                @prefix src: <http://src/> .
                @prefix tgt: <http://tgt/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                src:a owl:equivalentProperty tgt:B .
                NOT A VALID TRIPLE .
                """;

            Reader r = new StringReader(badTurtle);
            CMFException ex = assertThrows(CMFException.class, () -> Mapping.read(r));
            assertTrue(ex.getMessage().contains("Invalid mapping file"));
        }

        @Test
        @DisplayName("read fails when a triple uses an undeclared prefix")
        void readUndeclaredPrefix() {
            String badTurtle = """
                @prefix src: <http://src/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                src:a owl:equivalentProperty tgt:B .
                """;

            Reader r = new StringReader(badTurtle);
            CMFException ex = assertThrows(CMFException.class, () -> Mapping.read(r));
            assertTrue(ex.getMessage().contains("undeclared prefix"));
        }

        @Test
        @DisplayName("read accepts equivalentProperty under any prefix bound to the OWL namespace")
        void readAlternateOwlPrefix() throws Exception {
            String turtle = """
                @prefix src: <http://src/> .
                @prefix tgt: <http://tgt/> .
                @prefix o: <http://www.w3.org/2002/07/owl#> .
                src:a o:equivalentProperty tgt:A .
                """;

            Mapping readBack = Mapping.read(new StringReader(turtle));
            assertEquals("tgt:A", readBack.qnToMappedQN("src:a"));
        }

        @Test
        @DisplayName("read rejects invalid predicate")
        void readInvalidPredicate() {
            String badTurtle = """
                @prefix src: <http://src/> .
                @prefix tgt: <http://tgt/> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                src:a owl:sameAs tgt:A .
                """;

            Reader r = new StringReader(badTurtle);
            CMFException ex = assertThrows(CMFException.class, () -> Mapping.read(r));
            assertTrue(ex.getMessage().contains("invalid predicate"));
        }

        @Test
        @DisplayName("read rejects conflicting prefix redefinition")
        void readConflictingPrefixRedefinition() {
            String badTurtle = """
                @prefix src: <http://src/one/> .
                @prefix src: <http://src/two/> .
                """;

            Reader r = new StringReader(badTurtle);
            CMFException ex = assertThrows(CMFException.class, () -> Mapping.read(r));
            assertTrue(ex.getMessage().contains("prefix \"src\" is mapped"));
        }
    }
}
