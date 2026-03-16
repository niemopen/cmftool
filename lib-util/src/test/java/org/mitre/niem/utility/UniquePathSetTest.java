package org.mitre.niem.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class UniquePathSetTest {

    @Test
    void addsDistinctPathsUnchanged() {
        UniquePathSet set = new UniquePathSet();

        assertEquals("foo/bar/file.xsd", set.add("foo/bar/file.xsd"));
        assertEquals("foo/bar/otherfile.xsd", set.add("foo/bar/otherfile.xsd"));

        assertEquals(2, set.size());
        assertTrue(set.contains("foo/bar/file.xsd"));
        assertTrue(set.contains("foo/bar/otherfile.xsd"));
    }

    @Test
    void duplicatesAreRenamedWithIncrementingSuffix() {
        UniquePathSet set = new UniquePathSet();

        assertEquals("foo/bar/file.xsd", set.add("foo/bar/file.xsd"));
        assertEquals("foo/bar/file_1.xsd", set.add("foo/bar/file.xsd"));
        assertEquals("foo/bar/file_2.xsd", set.add("foo/bar/file.xsd"));

        assertEquals(3, set.size());
        assertTrue(set.contains("foo/bar/file.xsd"));
        assertTrue(set.contains("foo/bar/file_1.xsd"));
        assertTrue(set.contains("foo/bar/file_2.xsd"));
    }

    @Test
    void noExtensionSuffixAppendedToEnd() {
        UniquePathSet set = new UniquePathSet();

        assertEquals("foo/bar/file", set.add("foo/bar/file"));
        assertEquals("foo/bar/file_1", set.add("foo/bar/file"));

        assertEquals(2, set.size());
        assertTrue(set.contains("foo/bar/file"));
        assertTrue(set.contains("foo/bar/file_1"));
    }

    @Test
    void windowsSeparatorsSupported() {
        UniquePathSet set = new UniquePathSet();

        assertEquals("foo\\bar\\file.xsd", set.add("foo\\bar\\file.xsd"));
        assertEquals("foo\\bar\\file_1.xsd", set.add("foo\\bar\\file.xsd"));
    }

    @Test
    void fragmentIsModifiedInsteadOfFilenameWhenPresent() {
        UniquePathSet set = new UniquePathSet();

        assertEquals("http://h/a/file.xsd#type", set.add("http://h/a/file.xsd#type"));
        assertEquals("http://h/a/file.xsd#type_1", set.add("http://h/a/file.xsd#type"));
        assertEquals("http://h/a/file.xsd#type_2", set.add("http://h/a/file.xsd#type"));

        // Ensure base URI part stays unchanged when fragment exists
        assertTrue(set.contains("http://h/a/file.xsd#type"));
        assertTrue(set.contains("http://h/a/file.xsd#type_1"));
        assertTrue(set.contains("http://h/a/file.xsd#type_2"));
    }

    @Test
    void emptyFragmentIsSupported() {
        UniquePathSet set = new UniquePathSet();

        assertEquals("http://h/a/file.xsd#", set.add("http://h/a/file.xsd#"));
        assertEquals("http://h/a/file.xsd#_1", set.add("http://h/a/file.xsd#"));
    }

    @Test
    void skipsOverExistingGeneratedNames() {
        UniquePathSet set = new UniquePathSet();

        assertEquals("foo/bar/file.xsd", set.add("foo/bar/file.xsd"));
        assertEquals("foo/bar/file_1.xsd", set.add("foo/bar/file.xsd"));

        // Pre-insert a name that the generator would otherwise pick next
        assertEquals("foo/bar/file_2.xsd", set.add("foo/bar/file_2.xsd"));

        // Next duplicate should skip _2 and land on _3
        assertEquals("foo/bar/file_3.xsd", set.add("foo/bar/file.xsd"));
    }

    @Test
    void sizeReflectsStoredUniqueVariants() {
        UniquePathSet set = new UniquePathSet();

        set.add("a");
        set.add("a");
        set.add("a");

        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("a_1"));
        assertTrue(set.contains("a_2"));
    }
}
