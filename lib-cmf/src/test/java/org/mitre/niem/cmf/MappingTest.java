package org.mitre.niem.cmf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class MappingTest {

    @Test
    @DisplayName("read(): parses valid SSSOM and builds bijective mappings")
    void testReadValidSSSOM() throws Exception {
        String sssom =
                "# curie_map:\n" +
                "#   src: http://example.com/src#\n" +
                "#   tgt: http://example.com/tgt#\n" +
                "#   owl: http://www.w3.org/2002/07/owl#\n" +
                "subject_id\tpredicate_id\tobject_id\n" +
                "src:Foo\towl:sameAs\ttgt:Bar\n" +
                "src:Baz\towl:sameAs\ttgt:Qux\n";

        Mapping m = Mapping.read(new StringReader(sssom));

        // QName-to-QName lookups
        Assertions.assertEquals("tgt:Bar", m.qnToQ("src:Foo"));
        Assertions.assertEquals("tgt:Qux", m.qnToQ("src:Baz"));

        // URI-to-URI lookups (based on provided base URIs and local names)
        Assertions.assertEquals("http://example.com/tgt#Bar", m.uriToU("http://example.com/src#Foo"));
        Assertions.assertEquals("http://example.com/tgt#Qux", m.uriToU("http://example.com/src#Baz"));
    }

    @Test
    @DisplayName("write(): outputs SSSOM header and sameAs rows")
    void testWriteProducesSSSOM() throws IOException, MappingException {
        Mapping m = new Mapping();
        m.addPrefixMapping("src", "http://example.com/src#");
        m.addPrefixMapping("tgt", "http://example.com/tgt#");
        m.addMapping("src:Foo", "tgt:Bar");

        StringWriter out = new StringWriter();
        m.write(out);
        String s = out.toString();

        // Must include header
        Assertions.assertTrue(s.contains("subject_id\tpredicate_id\tobject_id"));
        // Must include mapping row with sameAs predicate
        Assertions.assertTrue(s.contains("src:Foo"));
        Assertions.assertTrue(s.contains("sameAs"));
        Assertions.assertTrue(s.contains("tgt:Bar"));
        // Must include curie_map section
        Assertions.assertTrue(s.contains("# curie_map:"));
        Assertions.assertTrue(s.contains("#   src: http://example.com/src#"));
        Assertions.assertTrue(s.contains("#   tgt: http://example.com/tgt#"));
    }

    @Test
    @DisplayName("addMapping(): enforces bijection (source or target cannot map to multiple values)")
    void testBijectionConstraints() throws MappingException {
        Mapping m = new Mapping();
        m.addPrefixMapping("a", "http://example.com/a#");
        m.addPrefixMapping("b", "http://example.com/b#");

        m.addMapping("a:X", "b:Y");

        // Same source to different target -> error
        Assertions.assertThrows(MappingException.class, () -> m.addMapping("a:X", "b:Z"));

        // Different source to same target -> error
        Assertions.assertThrows(MappingException.class, () -> m.addMapping("a:W", "b:Y"));
    }

    @Test
    @DisplayName("addMapping(): idempotent when adding identical mapping twice")
    void testAddMappingIdempotent() throws MappingException {
        Mapping m = new Mapping();
        m.addPrefixMapping("a", "http://example.com/a#");
        m.addPrefixMapping("b", "http://example.com/b#");

        m.addMapping("a:X", "b:Y");
        // Adding the exact same mapping again should not throw
        m.addMapping("a:X", "b:Y");

        Assertions.assertEquals("b:Y", m.qnToQ("a:X"));
        Assertions.assertEquals("http://example.com/b#Y", m.uriToU("http://example.com/a#X"));
    }

    @Test
    @DisplayName("addMapping(): throws when prefix not defined or not a QName")
    void testAddMappingInvalidPrefixOrQName() throws MappingException {
        Mapping m = new Mapping();
        m.addPrefixMapping("tgt", "http://example.com/tgt#");

        // Undefined prefix "bad" should fail
        Assertions.assertThrows(MappingException.class, () -> m.addMapping("bad:Foo", "tgt:Bar"));

        // Not a QName (missing colon) should fail
        m.addPrefixMapping("src", "http://example.com/src#");
        Assertions.assertThrows(MappingException.class, () -> m.addMapping("notAQName", "tgt:Bar"));
    }

    @Test
    @DisplayName("read(): rejects non-owl:sameAs predicate")
    void testReadInvalidPredicate() {
        String sssom =
                "# curie_map:\n" +
                "#   src: http://example.com/src#\n" +
                "#   tgt: http://example.com/tgt#\n" +
                "#   owl: http://www.w3.org/2002/07/owl#\n" +
                "subject_id\tpredicate_id\tobject_id\n" +
                "src:Foo\towl:differentFrom\ttgt:Bar\n";

        Assertions.assertThrows(MappingException.class, () -> Mapping.read(new StringReader(sssom)));
    }

    @Test
    @DisplayName("read(): requires curie_map section")
    void testReadMissingCurieMap() {
        String sssom =
                "subject_id\tpredicate_id\tobject_id\n" +
                "src:Foo\towl:sameAs\ttgt:Bar\n";

        Assertions.assertThrows(MappingException.class, () -> Mapping.read(new StringReader(sssom)));
    }

    @Test
    @DisplayName("read(): detects bad SSSOM header")
    void testReadBadHeader() {
        String sssom =
                "# curie_map:\n" +
                "#   src: http://example.com/src#\n" +
                "#   tgt: http://example.com/tgt#\n" +
                "#   owl: http://www.w3.org/2002/07/owl#\n" +
                "subject_id\tobject_id\n" + // missing predicate_id
                "src:Foo\towl:sameAs\ttgt:Bar\n";

        Assertions.assertThrows(MappingException.class, () -> Mapping.read(new StringReader(sssom)));
    }

    @Test
    @DisplayName("read(): detects bad data line with insufficient columns")
    void testReadBadDataLine() {
        String sssom =
                "# curie_map:\n" +
                "#   src: http://example.com/src#\n" +
                "#   tgt: http://example.com/tgt#\n" +
                "#   owl: http://www.w3.org/2002/07/owl#\n" +
                "subject_id\tpredicate_id\tobject_id\n" +
                "src:Foo\towl:sameAs\n"; // missing object_id

        Assertions.assertThrows(MappingException.class, () -> Mapping.read(new StringReader(sssom)));
    }

    @Test
    @DisplayName("read(): detects conflicting curie_map assignments for the same URI with different prefixes")
    void testCurieMapConflictingAssignment() {
        String sssom =
                "# curie_map:\n" +
                "#   p1: http://example.com/uri#\n" +
                "#   p2: http://example.com/uri#\n" + // same URI, different prefix -> conflict
                "subject_id\tpredicate_id\tobject_id\n";

        Assertions.assertThrows(MappingException.class, () -> Mapping.read(new StringReader(sssom)));
    }

    @Test
    @DisplayName("read(): tolerates UTF-8 BOM on first line")
    void testReadWithBOM() throws Exception {
        String sssom =
                "\uFEFF# curie_map:\n" +
                "#   src: http://example.com/src#\n" +
                "#   tgt: http://example.com/tgt#\n" +
                "#   owl: http://www.w3.org/2002/07/owl#\n" +
                "subject_id\tpredicate_id\tobject_id\n" +
                "src:Foo\towl:sameAs\ttgt:Bar\n";

        Mapping m = Mapping.read(new StringReader(sssom));
        Assertions.assertEquals("tgt:Bar", m.qnToQ("src:Foo"));
    }
}
