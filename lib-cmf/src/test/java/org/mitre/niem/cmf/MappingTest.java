/*--------------------------------------------------------------------
 *  MappingTest.java
 *
 *  Revised JUnit 5 test suite for org.mitre.niem.cmf.Mapping
 *
 *  Copyright 2020‑2026 The MITRE Corporation.
 *--------------------------------------------------------------------*/
package org.mitre.niem.cmf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Mapping}.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>prefix assignment (including munging when a prefix is already used)</li>
 *   <li>basic mapping addition and lookup</li>
 *   <li>the {@code noPrefix} flag behaviour</li>
 *   <li>that a mapping can be written to Turtle and read back unchanged</li>
 *   <li>error handling for duplicate or conflicting mappings</li>
 * </ul>
 */
class MappingTest {

    private Mapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new Mapping();
    }

    /* -----------------------------------------------------------------
     *  Prefix handling
     * ----------------------------------------------------------------- */
    @Test
    @DisplayName("assignPrefix returns the requested prefix when unused")
    void assignPrefixSimple() {
        String p = mapping.assignPrefix("ex", "http://example.org/");
        assertEquals("ex", p);
        assertEquals("http://example.org/", mapping.nsmap.getURI("ex"));
    }

    @Test
    @DisplayName("assignPrefix munges a duplicate prefix")
    void assignPrefixMunge() {
        mapping.assignPrefix("ex", "http://example.org/1");
        String p2 = mapping.assignPrefix("ex", "http://example.org/2");
        // The second call must not return the original "ex"
        assertNotEquals("ex", p2);
        // The munged prefix must be unique and map to the second URI
        assertEquals("http://example.org/2", mapping.nsmap.getURI(p2));
    }

    /* -----------------------------------------------------------------
     *  Basic mapping operations
     * ----------------------------------------------------------------- */
    @Test
    @DisplayName("addMapping stores and retrieves a simple mapping")
    void addAndLookupMapping() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt", "http://tgt/");

        String srcQN = "src:propA";
        String tgtQN = "tgt:propB";

        mapping.addMapping(srcQN, tgtQN);

        // qnToQ returns the full target QN
        assertEquals(tgtQN, mapping.qnToQ(srcQN));

        // qnToN returns the full target QN when noPrefix == false
        assertEquals(tgtQN, mapping.qnToN(srcQN));
    }

    @Test
    @DisplayName("qnToN returns only the local name when noPrefix is true")
    void noPrefixBehaviour() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt", "http://tgt/");

        String srcQN = "src:propA";
        String tgtQN = "tgt:propB";

        mapping.addMapping(srcQN, tgtQN);
        mapping.setNoPrefix(true);

        // The target QN uses only the local name part
        assertEquals("propB", mapping.qnToN(srcQN));
    }

    @Test
    @DisplayName("setNoPrefix throws when more than one target prefix exists")
    void setNoPrefixTooManyPrefixes() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt1", "http://tgt1/");
        mapping.assignPrefix("tgt2", "http://tgt2/");

        mapping.addMapping("src:propA", "tgt1:propB");
        mapping.addMapping("src:propC", "tgt2:propD");

        CMFException ex = assertThrows(CMFException.class,
                () -> mapping.setNoPrefix(true));
        assertTrue(ex.getMessage().contains("Can't set noPrefix"));
    }

    @Test
    @DisplayName("addMapping rejects duplicate source with different target")
    void duplicateSourceConflict() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt", "http://tgt/");

        mapping.addMapping("src:propA", "tgt:propB");

        CMFException ex = assertThrows(CMFException.class,
                () -> mapping.addMapping("src:propA", "tgt:propC"));
        assertTrue(ex.getMessage().contains("already mapped"));
    }

    @Test
    @DisplayName("addMapping rejects duplicate target with different source")
    void duplicateTargetConflict() throws CMFException {
        mapping.assignPrefix("src", "http://src/");
        mapping.assignPrefix("tgt", "http://tgt/");

        mapping.addMapping("src:propA", "tgt:propB");

        CMFException ex = assertThrows(CMFException.class,
                () -> mapping.addMapping("src:propC", "tgt:propB"));
        assertTrue(ex.getMessage().contains("already mapped"));
    }

    /* -----------------------------------------------------------------
     *  Turtle write / read round‑trip
     * ----------------------------------------------------------------- */
    @Nested
    @DisplayName("Turtle serialization")
    class TurtleRoundTrip {

        private final String srcPrefix = "src";
        private final String tgtPrefix = "tgt";

        @BeforeEach
        void initPrefixes() {
            mapping.assignPrefix(srcPrefix, "http://src/");
            mapping.assignPrefix(tgtPrefix, "http://tgt/");
        }

        @Test
        @DisplayName("write produces valid Turtle and read restores the mapping")
        void writeReadRoundTrip() throws IOException, CMFException {
            // create a few mappings
            mapping.addMapping(srcPrefix + ":a", tgtPrefix + ":A");
            mapping.addMapping(srcPrefix + ":b", tgtPrefix + ":B");
            mapping.addMapping(srcPrefix + ":c", tgtPrefix + ":C");

            // write to a StringWriter
            StringWriter sw = new StringWriter();
            mapping.write(sw);
            String turtle = sw.toString();

            // sanity‑check that the Turtle contains the expected prefixes
            assertAll(
                () -> assertTrue(turtle.contains("@prefix src: <http://src/>")),
                () -> assertTrue(turtle.contains("@prefix tgt: <http://tgt/>")),
                () -> assertTrue(turtle.contains("@prefix owl:"))
            );

            // read the Turtle back into a new Mapping instance
            Reader r = new StringReader(turtle);
            Mapping readBack = Mapping.read(r);

            // verify that every mapping survived the round‑trip
            List<String> srcs = List.of(srcPrefix + ":a", srcPrefix + ":b", srcPrefix + ":c");
            for (String srcQN : srcs) {
                assertEquals(mapping.qnToQ(srcQN), readBack.qnToQ(srcQN),
                        "Round‑trip mapping for " + srcQN);
            }

            // verify that the prefixes were also restored
            assertEquals("http://src/", readBack.nsmap.getURI(srcPrefix));
            assertEquals("http://tgt/", readBack.nsmap.getURI(tgtPrefix));
        }

        @Test
        @DisplayName("read fails on malformed Turtle line")
        void readMalformedTurtle() {
            String badTurtle = """
                @prefix src: <http://src/> .
                src:a owl:equivalentProperty tgt:B .
                NOT A VALID TRIPLE .
                """;

            Reader r = new StringReader(badTurtle);
            CMFException ex = assertThrows(CMFException.class,
                    () -> Mapping.read(r));
            assertTrue(ex.getMessage().contains("Invalid mapping file"));
        }
    }
}
