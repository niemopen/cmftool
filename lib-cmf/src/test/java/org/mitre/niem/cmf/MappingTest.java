package org.mitre.niem.cmf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

public class MappingTest {

    @Test
    void qnToQReturnsMappedOrOriginal() throws Exception {
        Mapping mapping = new Mapping();
        mapping.assignPrefix("src", "http://example.com/src");
        mapping.assignPrefix("tgt", "http://example.com/tgt");

        mapping.addQNameMapping("src:PersonSurName", "tgt:lname");

        // Mapped value
        assertEquals("tgt:lname", mapping.qnToQ("src:PersonSurName"));
        // Unmapped value returns original
        assertEquals("src:OtherName", mapping.qnToQ("src:OtherName"));
    }

    @Test
    void qnToNHonorsNoPrefixFlag() throws Exception {
        Mapping mapping = new Mapping();
        mapping.assignPrefix("src", "http://example.com/src");
        mapping.assignPrefix("tgt", "http://example.com/tgt");

        mapping.addQNameMapping("src:PersonSurName", "tgt:lname");

        // Default: noPrefix == false -> returns QName
        assertEquals("tgt:lname", mapping.qnToN("src:PersonSurName"));

        // After enabling noPrefix -> returns local name
        mapping.setNoPrefix(true);
        assertEquals("lname", mapping.qnToN("src:PersonSurName"));

        // Unmapped still returns original, even with noPrefix
        assertEquals("src:OtherName", mapping.qnToN("src:OtherName"));
    }

    @Test
    void addQNameMappingRejectsConflictingFromMapping() throws Exception {
        Mapping mapping = new Mapping();
        mapping.assignPrefix("src", "http://example.com/src");
        mapping.assignPrefix("tgt", "http://example.com/tgt");
        mapping.assignPrefix("tgt2", "http://example.com/tgt2");

        mapping.addQNameMapping("src:PersonSurName", "tgt:lname");

        // Same fromQ mapped to different toQ should throw
        assertThrows(
            MappingException.class,
            () -> mapping.addQNameMapping("src:PersonSurName", "tgt2:otherLname")
        );
    }

    @Test
    void addQNameMappingRejectsConflictingToMapping() throws Exception {
        Mapping mapping = new Mapping();
        mapping.assignPrefix("src1", "http://example.com/src1");
        mapping.assignPrefix("src2", "http://example.com/src2");
        mapping.assignPrefix("tgt", "http://example.com/tgt");

        mapping.addQNameMapping("src1:PersonSurName", "tgt:lname");

        // Different fromQ mapping to same toQ should throw
        assertThrows(
            MappingException.class,
            () -> mapping.addQNameMapping("src2:PersonSurName", "tgt:lname")
        );
    }

    @Test
    void setNoPrefixFailsWhenMultipleTargetNamespacesExist() throws Exception {
        Mapping mapping = new Mapping();
        mapping.assignPrefix("src", "http://example.com/src");
        mapping.assignPrefix("tgt1", "http://example.com/tgt1");
        mapping.assignPrefix("tgt2", "http://example.com/tgt2");

        mapping.addQNameMapping("src:One", "tgt1:One");
        mapping.addQNameMapping("src:Two", "tgt2:Two");

        assertThrows(
            MappingException.class,
            () -> mapping.setNoPrefix(true)
        );
    }

    @Test
    void addQNameMappingFailsWhenNoPrefixTrueAndNewTargetNamespaceIntroduced() throws Exception {
        Mapping mapping = new Mapping();
        mapping.assignPrefix("src", "http://example.com/src");
        mapping.assignPrefix("tgt1", "http://example.com/tgt1");
        mapping.assignPrefix("tgt2", "http://example.com/tgt2");

        mapping.addQNameMapping("src:One", "tgt1:One");
        mapping.setNoPrefix(true); // ok, only one target prefix so far

        // Adding a second target prefix should fail because noPrefix is true
        assertThrows(
            MappingException.class,
            () -> mapping.addQNameMapping("src:Two", "tgt2:Two")
        );
    }

    @Test
    void getPrefixReturnsPrefixForKnownQName() throws Exception {
        Mapping mapping = new Mapping();
        mapping.assignPrefix("ex", "http://example.com/ex");

        assertEquals("ex", mapping.getPrefix("ex:LocalName"));
    }

    @Test
    void getPrefixThrowsForUnknownPrefix() {
        Mapping mapping = new Mapping();
        mapping.assignPrefix("ex", "http://example.com/ex");

        assertThrows(
            MappingException.class,
            () -> mapping.getPrefix("unknown:LocalName")
        );
    }

    @Test
    void writeAndReadRoundTripPreservesMappingsAndNoPrefix() throws Exception {
        Mapping original = new Mapping();
        original.assignPrefix("src", "http://example.com/src");
        original.assignPrefix("tgt", "http://example.com/tgt");

        original.addQNameMapping("src:PersonSurName", "tgt:lname");
        original.addQNameMapping("src:PersonGivenName", "tgt:fname");
        original.setNoPrefix(true);

        StringWriter writer = new StringWriter();
        original.write(writer);
        String serialized = writer.toString();

        Mapping parsed = Mapping.read(new StringReader(serialized));

        // Mapping preserved
        assertEquals("tgt:lname", parsed.qnToQ("src:PersonSurName"));
        assertEquals("tgt:fname", parsed.qnToQ("src:PersonGivenName"));

        // noPrefix flag preserved: qnToN returns local name only
        assertEquals("lname", parsed.qnToN("src:PersonSurName"));
        assertEquals("fname", parsed.qnToN("src:PersonGivenName"));
    }

    @Test
    void readWithoutCurieMapThrowsMappingException() {
        // Missing curie_map/comment block
        String sssom =
            "subject_id\tpredicate_id\tobject_id\n" +
            "src:PersonSurName\towl:sameAs\ttgt:lname\n";

        assertThrows(
            MappingException.class,
            () -> Mapping.read(new StringReader(sssom))
        );
    }
}
