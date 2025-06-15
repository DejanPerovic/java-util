package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompactCIHashSetTest {

    @Test
    void defaultConstructorIsCaseInsensitive() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        assertTrue(set.isEmpty());
        set.add("Foo");
        assertTrue(set.contains("foo"));
        assertTrue(set.contains("FOO"));

        set.add("fOo");
        assertEquals(1, set.size());
    }

    @Test
    void collectionConstructorDeduplicates() {
        List<String> values = Arrays.asList("one", "Two", "tWo");
        CompactCIHashSet<String> set = new CompactCIHashSet<>(values);

        assertEquals(2, set.size());
        assertTrue(set.contains("ONE"));
        assertTrue(set.contains("two"));
    }
}
