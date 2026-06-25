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

package org.mitre.niem.utility;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MapToSetTest {

    private MapToSet<String, Integer> mapToSet;

    @BeforeEach
    void setUp() {
        mapToSet = new MapToSet<>();
    }

    @Test
    void constructor_startsEmpty() {
        assertTrue(mapToSet.isEmpty());
        assertEquals(0, mapToSet.size());
        assertEquals(0, mapToSet.totalValueCount());
        assertTrue(mapToSet.keySet().isEmpty());
    }

    @Test
    void add_createsSetAndAddsValue() {
        assertTrue(mapToSet.add("a", 1));

        assertTrue(mapToSet.containsKey("a"));
        assertEquals(Set.of(1), mapToSet.get("a"));
        assertEquals(1, mapToSet.valueCount("a"));
        assertEquals(1, mapToSet.totalValueCount());
    }

    @Test
    void add_duplicateValueReturnsFalseAndDoesNotChangeSet() {
        assertTrue(mapToSet.add("a", 1));
        assertFalse(mapToSet.add("a", 1));

        assertEquals(Set.of(1), mapToSet.get("a"));
        assertEquals(1, mapToSet.valueCount("a"));
        assertEquals(1, mapToSet.totalValueCount());
    }

    @Test
    void addAll_addsAllDistinctValues() {
        assertTrue(mapToSet.addAll("a", Set.of(1, 2, 3)));

        assertEquals(Set.of(1, 2, 3), mapToSet.get("a"));
        assertEquals(3, mapToSet.valueCount("a"));
        assertEquals(3, mapToSet.totalValueCount());
    }

    @Test
    void addAll_returnsFalseWhenEmptyCollectionForMissingKey() {
        assertFalse(mapToSet.addAll("a", Set.of()));

        assertFalse(mapToSet.containsKey("a"));
        assertEquals(0, mapToSet.size());
    }

    @Test
    void addAll_returnsFalseWhenAllValuesAlreadyPresent() {
        mapToSet.addAll("a", Set.of(1, 2, 3));

        assertFalse(mapToSet.addAll("a", Set.of(1, 2)));

        assertEquals(Set.of(1, 2, 3), mapToSet.get("a"));
    }

    @Test
    void addAll_returnsTrueWhenAtLeastOneNewValueIsAdded() {
        mapToSet.addAll("a", Set.of(1, 2));

        assertTrue(mapToSet.addAll("a", Set.of(2, 3)));

        assertEquals(Set.of(1, 2, 3), mapToSet.get("a"));
    }

    @Test
    void get_hasSideEffectAndCreatesEmptySetForMissingKey() {
        Set<Integer> values = mapToSet.get("a");

        assertNotNull(values);
        assertTrue(values.isEmpty());
        assertTrue(mapToSet.containsKey("a"));
        assertEquals(1, mapToSet.size());
        assertEquals(0, mapToSet.valueCount("a"));
    }

    @Test
    void get_returnsLiveMutableInternalSet() {
        Set<Integer> values = mapToSet.get("a");
        values.add(10);
        values.add(20);

        assertEquals(Set.of(10, 20), mapToSet.get("a"));
        assertEquals(2, mapToSet.valueCount("a"));
        assertEquals(2, mapToSet.totalValueCount());
    }

    @Test
    void get_existingKeyReturnsSameSetInstance() {
        Set<Integer> first = mapToSet.get("a");
        Set<Integer> second = mapToSet.get("a");

        assertSame(first, second);
    }

    @Test
    void peek_returnsNullForMissingKeyWithoutSideEffect() {
        assertNull(mapToSet.peek("a"));
        assertFalse(mapToSet.containsKey("a"));
        assertEquals(0, mapToSet.size());
    }

    @Test
    void peek_returnsExistingMutableSet() {
        mapToSet.addAll("a", Set.of(1, 2));

        Set<Integer> values = mapToSet.peek("a");

        assertNotNull(values);
        assertEquals(Set.of(1, 2), values);

        values.add(3);
        assertEquals(Set.of(1, 2, 3), mapToSet.get("a"));
    }

    @Test
    void getOrEmpty_returnsImmutableEmptySetForMissingKeyWithoutSideEffect() {
        Set<Integer> values = mapToSet.getOrEmpty("a");

        assertTrue(values.isEmpty());
        assertFalse(mapToSet.containsKey("a"));
        assertThrows(UnsupportedOperationException.class, () -> values.add(1));
    }

    @Test
    void getOrEmpty_returnsExistingLiveSetForPresentKey() {
        mapToSet.add("a", 1);

        Set<Integer> values = mapToSet.getOrEmpty("a");
        values.add(2);

        assertEquals(Set.of(1, 2), mapToSet.get("a"));
    }

    @Test
    void remove_existingValueRemovesIt() {
        mapToSet.addAll("a", Set.of(1, 2, 3));

        assertTrue(mapToSet.remove("a", 2));

        assertEquals(Set.of(1, 3), mapToSet.get("a"));
        assertTrue(mapToSet.containsKey("a"));
        assertEquals(2, mapToSet.valueCount("a"));
    }

    @Test
    void remove_lastValueAlsoRemovesKey() {
        mapToSet.add("a", 1);

        assertTrue(mapToSet.remove("a", 1));

        assertFalse(mapToSet.containsKey("a"));
        assertNull(mapToSet.peek("a"));
        assertEquals(0, mapToSet.size());
        assertEquals(0, mapToSet.totalValueCount());
    }

    @Test
    void remove_missingKeyReturnsFalse() {
        assertFalse(mapToSet.remove("a", 1));
    }

    @Test
    void remove_missingValueReturnsFalseAndKeepsKey() {
        mapToSet.add("a", 1);

        assertFalse(mapToSet.remove("a", 2));
        assertTrue(mapToSet.containsKey("a"));
        assertEquals(Set.of(1), mapToSet.get("a"));
    }

    @Test
    void removeAll_removesKeyAndValues() {
        mapToSet.addAll("a", Set.of(1, 2, 3));

        assertTrue(mapToSet.removeAll("a"));

        assertFalse(mapToSet.containsKey("a"));
        assertNull(mapToSet.peek("a"));
        assertEquals(0, mapToSet.totalValueCount());
    }

    @Test
    void removeAll_missingKeyReturnsFalse() {
        assertFalse(mapToSet.removeAll("a"));
    }

    @Test
    void removeKey_returnsRemovedSet() {
        mapToSet.addAll("a", Set.of(1, 2, 3));

        Set<Integer> removed = mapToSet.removeKey("a");

        assertEquals(Set.of(1, 2, 3), removed);
        assertFalse(mapToSet.containsKey("a"));
    }

    @Test
    void containsValue_checksWithinKeyOnly() {
        mapToSet.add("a", 1);
        mapToSet.add("b", 1);

        assertTrue(mapToSet.containsValue("a", 1));
        assertTrue(mapToSet.containsValue("b", 1));
        assertFalse(mapToSet.containsValue("a", 2));
    }

    @Test
    void hasValues_falseForMissingKey() {
        assertFalse(mapToSet.hasValues("a"));
    }

    @Test
    void hasValues_falseForPresentButEmptyKey() {
        mapToSet.get("a");

        assertTrue(mapToSet.containsKey("a"));
        assertFalse(mapToSet.hasValues("a"));
    }

    @Test
    void hasValues_trueWhenKeyHasAtLeastOneValue() {
        mapToSet.add("a", 1);

        assertTrue(mapToSet.hasValues("a"));
    }

    @Test
    void valueCount_zeroForMissingKey() {
        assertEquals(0, mapToSet.valueCount("a"));
    }

    @Test
    void totalValueCount_countsAcrossKeys() {
        mapToSet.addAll("a", Set.of(1, 2));
        mapToSet.addAll("b", Set.of(3, 4, 5));

        assertEquals(5, mapToSet.totalValueCount());
    }

    @Test
    void keySet_isLiveView() {
        mapToSet.add("a", 1);
        Set<String> keys = mapToSet.keySet();

        assertTrue(keys.contains("a"));

        mapToSet.add("b", 2);
        assertTrue(keys.contains("b"));

        keys.remove("a");
        assertFalse(mapToSet.containsKey("a"));
    }

    @Test
    void asMap_returnsLiveViewOfUnderlyingMap() {
        Map<String, Set<Integer>> map = mapToSet.asMap();

        map.put("a", new HashSet<>(Set.of(1, 2)));
        assertEquals(Set.of(1, 2), mapToSet.get("a"));

        map.get("a").add(3);
        assertEquals(Set.of(1, 2, 3), mapToSet.get("a"));

        map.remove("a");
        assertFalse(mapToSet.containsKey("a"));
    }

    @Test
    void clear_removesEverything() {
        mapToSet.addAll("a", Set.of(1, 2));
        mapToSet.add("b", 3);

        mapToSet.clear();

        assertTrue(mapToSet.isEmpty());
        assertEquals(0, mapToSet.size());
        assertEquals(0, mapToSet.totalValueCount());
        assertTrue(mapToSet.keySet().isEmpty());
    }

    @Test
    void directMutationOfReturnedSetCanLeaveEmptyKeyPresent() {
        Set<Integer> values = mapToSet.get("a");
        values.add(1);

        values.clear();

        assertTrue(mapToSet.containsKey("a"));
        assertEquals(0, mapToSet.valueCount("a"));
        assertFalse(mapToSet.hasValues("a"));
    }

    @Test
    void nullKeyIsForbiddenEverywhere() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> mapToSet.add(null, 1)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.addAll(null, Set.of(1))),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.get(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.peek(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.getOrEmpty(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.remove(null, 1)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.removeKey(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.removeAll(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.containsKey(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.containsValue(null, 1)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.hasValues(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.valueCount(null))
        );
    }

    @Test
    void nullValueIsForbiddenWhereApplicable() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> mapToSet.add("a", null)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.remove("a", null)),
            () -> assertThrows(NullPointerException.class, () -> mapToSet.containsValue("a", null))
        );
    }

    @Test
    void nullCollectionIsForbidden() {
        assertThrows(NullPointerException.class, () -> mapToSet.addAll("a", null));
    }

    @Test
    void addAll_forbidsNullElements() {
        Set<Integer> values = new HashSet<>();
        values.add(1);
        values.add(null);

        assertThrows(NullPointerException.class, () -> mapToSet.addAll("a", values));
    }
}
