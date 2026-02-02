package org.mitre.niem.cmf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * JUnit‑5 tests for {@link Mapping}.
 *
 * <p>These tests exercise the public API only; they do not depend on the
 * full NIEM model implementation.  For the few tests that need a {@code Model}
 * we provide a very small in‑memory stub (see {@link FakeModel}) that mimics
 * the methods used by {@code Mapping}.
 */
class MappingTest {

    /* --------------------------------------------------------------------- */
    /*  Helper stub for the Model interface used by Mapping.create*()      */
    /* --------------------------------------------------------------------- */
    /**
     * Very small fake implementation of the {@code Model} contract that
     * {@link Mapping#createDefault} and {@link Mapping#createTemplate} rely on.
     *
     * <p>Only the methods that the Mapping class calls are implemented:
     * <ul>
     *   <li>{@code namespaceList()}</li>
     *   <li>{@code propertyL()}</li>
     * </ul>
     *
     * <p>The stub uses simple POJOs for {@code Namespace} and {@code Property}
     * that expose the subset of getters needed by the Mapping code.
     */
//    private static class FakeModel implements Model {
//
//        /** Simple namespace representation */
//        static final class Ns {
//            private final String prefix;
//            private final String uri;
//            private final boolean modelNS;
//
//            Ns(String prefix, String uri, boolean modelNS) {
//                this.prefix = prefix;
//                this.uri = uri;
//                this.modelNS = modelNS;
//            }
//
//            String prefix() { return prefix; }
//            String uri()    { return uri; }
//            boolean isModelNS() { return modelNS; }
//        }
//
//        /** Simple property representation */
//        static final class Prop implements Comparable<Prop> {
//            private final Ns ns;
//            private final String name;
//            private final boolean abstractProp;
//
//            Prop(Ns ns, String name, boolean abstractProp) {
//                this.ns = ns;
//                this.name = name;
//                this.abstractProp = abstractProp;
//            }
//
//            Ns namespace() { return ns; }
//            String name()   { return name; }
//            boolean isAbstract() { return abstractProp; }
//            String qname() { return ns.prefix() + ":" + name; }
//            @Override public int compareTo(Prop o) { return qname().compareTo(o.qname()); }
//        }
//
//        private final List<Ns> namespaces = new ArrayList<>();
//        private final List<Prop> properties = new ArrayList<>();
//
//        void addNamespace(String prefix, String uri, boolean modelNS) {
//            namespaces.add(new Ns(prefix, uri, modelNS));
//        }
//
//        void addProperty(Ns ns, String name, boolean abstractProp) {
//            properties.add(new Prop(ns, name, abstractProp));
//        }
//
//        @Override public List<?> namespaceList() { return namespaces; }
//        @Override public List<?> propertyL()    { return properties; }
//    }

    /* --------------------------------------------------------------------- */
    /*  1. Basic prefix handling                                            */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("assignPrefix returns the requested prefix when free and munges when taken")
    void testAssignPrefixAndReserved() throws CMFException {
        Mapping map = new Mapping();

        // Assign a fresh prefix – should be returned unchanged
        String p1 = map.assignPrefix("ex", "http://example.org/");
        assertEquals("ex", p1);

        // Assign the same prefix for a different URI – should be munged (ex_1)
        String p2 = map.assignPrefix("ex", "http://other.org/");
        assertNotEquals("ex", p2);
        assertTrue(p2.startsWith("ex_"));

        // Reserved prefixes are allowed to be added; they are later skipped in write()
        String owl = map.assignPrefix("owl", "http://www.w3.org/2002/07/owl#");
        assertEquals("owl", owl);
    }

    /* --------------------------------------------------------------------- */
    /*  2. Name‑only mapping                                                */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("addNameMapping stores and retrieves a simple local‑name mapping")
    void testAddNameMappingAndLookup() throws CMFException {
        Mapping map = new Mapping();
        map.setTargetNS("tgt", "http://target.org/");

        map.addNameMapping("src:prop", "myProp");

        // qnToN returns the local name (no prefix)
        assertEquals("myProp", map.qnToN("src:prop"));

        // qnToQ returns a qualified name using the target prefix
        assertEquals("tgt:myProp", map.qnToQ("src:prop"));
    }

    /* --------------------------------------------------------------------- */
    /*  3. Full QName mapping                                               */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("addQNmapping sets target namespace automatically and resolves correctly")
    void testAddQNmappingAndLookup() throws CMFException {
        Mapping map = new Mapping();
        
        map.assignPrefix("src", "http://example.com/Source/");
        map.assignPrefix("tgt", "http://example.com/Target/");
        
        // First mapping defines the target namespace/prefix
        map.addQNmapping("src:age", "tgt:Age");
        assertEquals("tgt", map.qnToQ("src:age").split(":")[0]);

        // Subsequent mapping must use the same target prefix
        map.addQNmapping("src:name", "tgt:Name");

        // Look‑ups
        assertEquals("Age", map.qnToN("src:age"));
        assertEquals("tgt:Age", map.qnToQ("src:age"));
    }

    /* --------------------------------------------------------------------- */
    /*  4. Duplicate mapping detection                                      */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("adding a duplicate source or duplicate target throws CMFException")
    void testDuplicateMappingsThrow() throws CMFException {
        Mapping map = new Mapping();
        map.assignPrefix("src", "http://example.com/Source/");
        map.setTargetNS("t", "http://t.org/");

        map.addNameMapping("src:a", "A");

        // Duplicate source with same target – should be a no‑op (allowed)
        map.addNameMapping("src:a", "A");

        // Duplicate source with *different* target – error
        CMFException ex1 = assertThrows(CMFException.class,
                () -> map.addNameMapping("src:a", "B"));
        assertTrue(ex1.getMessage().contains("already mapped"));

        // Duplicate target with a *different* source – error
        CMFException ex2 = assertThrows(CMFException.class,
                () -> map.addNameMapping("src:b", "A"));
        assertTrue(ex2.getMessage().contains("already mapped"));
    }

    /* --------------------------------------------------------------------- */
    /*  5. Write → read round‑trip                                          */
    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("write() produces valid Turtle that read() can parse back")
    void testWriteAndReadRoundTrip() throws Exception {
        Mapping original = new Mapping();
        original.assignPrefix("src", "http://src.org/");
        original.assignPrefix("tgt", "http://tgt.org/");
        original.setTargetNS("tgt", "http://tgt.org/");
        original.addNameMapping("src:one", "One");
        original.addNameMapping("src:two", "Two");

        // Serialize to a string
        StringWriter sw = new StringWriter();
        original.write(sw);
        String turtle = sw.toString();

        // Parse back
        Mapping parsed = Mapping.read(new StringReader(turtle));

        // Verify that the parsed mapping behaves the same
        assertEquals(original.qnToN("src:one"), parsed.qnToN("src:one"));
        assertEquals(original.qnToQ("src:two"), parsed.qnToQ("src:two"));
        assertEquals(original.qnToQ("src:one"), parsed.qnToQ("src:one"));
    }
}
