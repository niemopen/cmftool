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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MapToListTest {

    private MapToList<String, Integer> mapToList;

    @BeforeEach
    void setUp() {
        mapToList = new MapToList<>();
    }

    @Test
    void constructor_startsEmpty() {
        assertTrue(mapToList.isEmpty());
        assertEquals(0, mapToList.size());
        assertEquals(0, mapToList.totalValueCount());
        assertTrue(mapToList.keySet().isEmpty());
    }

    @Test
    void add_createsListAndAppendsValue() {
        mapToList.add("a", 1);

        assertTrue(mapToList.containsKey("a"));
        assertEquals(List.of(1), mapToList.get("a"));
        assertEquals(1, mapToList.valueCount("a"));
        assertEquals(1, mapToList.totalValueCount());
    }

    @Test
    void add_multipleValuesPreserveOrder() {
        mapToList.add("a", 1);
        mapToList.add("a", 2);
        mapToList.add("a", 3);

        assertEquals(List.of(1, 2, 3), mapToList.get("a"));
        assertEquals(3, mapToList.valueCount("a"));
    }

    @Test
    void addAll_appendsValues() {
        mapToList.add("a", 1);
        mapToList.addAll("a", List.of(2, 3, 4));

        assertEquals(List.of(1, 2, 3, 4), mapToList.get("a"));
        assertEquals(4, mapToList.valueCount("a"));
        assertEquals(4, mapToList.totalValueCount());
    }

    @Test
    void addAll_emptyCollectionDoesNothingForMissingKey() {
        mapToList.addAll("a", List.of());

        assertFalse(mapToList.containsKey("a"));
        assertEquals(0, mapToList.size());
    }

    @Test
    void get_hasSideEffectAndCreatesEmptyListForMissingKey() {
        List<Integer> values = mapToList.get("a");

        assertNotNull(values);
        assertTrue(values.isEmpty());
        assertTrue(mapToList.containsKey("a"));
        assertEquals(1, mapToList.size());
        assertEquals(0, mapToList.valueCount("a"));
    }

    @Test
    void get_returnsLiveMutableInternalList() {
        List<Integer> values = mapToList.get("a");
        values.add(10);
        values.add(20);

        assertEquals(List.of(10, 20), mapToList.get("a"));
        assertEquals(2, mapToList.valueCount("a"));
        assertEquals(2, mapToList.totalValueCount());
    }

    @Test
    void get_existingKeyReturnsSameListInstance() {
        List<Integer> first = mapToList.get("a");
        List<Integer> second = mapToList.get("a");

        assertSame(first, second);
    }

    @Test
    void getByPosition_returnsValueWhenInRange() {
        mapToList.addAll("a", List.of(5, 6, 7));

        assertEquals(5, mapToList.get("a", 0));
        assertEquals(6, mapToList.get("a", 1));
        assertEquals(7, mapToList.get("a", 2));
    }

    @Test
    void getByPosition_returnsNullWhenOutOfRange() {
        mapToList.addAll("a", List.of(5, 6));

        assertNull(mapToList.get("a", -1));
        assertNull(mapToList.get("a", 2));
        assertNull(mapToList.get("a", 99));
    }

    @Test
    void getByPosition_hasSideEffectForMissingKey() {
        assertNull(mapToList.get("a", 0));
        assertTrue(mapToList.containsKey("a"));
        assertEquals(1, mapToList.size());
        assertEquals(0, mapToList.valueCount("a"));
    }

    @Test
    void peek_returnsNullForMissingKeyWithoutSideEffect() {
        assertNull(mapToList.peek("a"));
        assertFalse(mapToList.containsKey("a"));
        assertEquals(0, mapToList.size());
    }

    @Test
    void peek_returnsExistingMutableList() {
        mapToList.addAll("a", List.of(1, 2));

        List<Integer> values = mapToList.peek("a");

        assertNotNull(values);
        assertEquals(List.of(1, 2), values);

        values.add(3);
        assertEquals(List.of(1, 2, 3), mapToList.get("a"));
    }

    @Test
    void getOrEmpty_returnsImmutableEmptyListForMissingKeyWithoutSideEffect() {
        List<Integer> values = mapToList.getOrEmpty("a");

        assertTrue(values.isEmpty());
        assertFalse(mapToList.containsKey("a"));
        assertThrows(UnsupportedOperationException.class, () -> values.add(1));
    }

    @Test
    void getOrEmpty_returnsExistingLiveListForPresentKey() {
        mapToList.add("a", 1);

        List<Integer> values = mapToList.getOrEmpty("a");
        values.add(2);

        assertEquals(List.of(1, 2), mapToList.get("a"));
    }

    @Test
    void remove_existingValueRemovesIt() {
        mapToList.addAll("a", List.of(1, 2, 3));

        assertTrue(mapToList.remove("a", 2));

        assertEquals(List.of(1, 3), mapToList.get("a"));
        assertTrue(mapToList.containsKey("a"));
        assertEquals(2, mapToList.valueCount("a"));
    }

    @Test
    void remove_lastValueAlsoRemovesKey() {
        mapToList.add("a", 1);

        assertTrue(mapToList.remove("a", 1));

        assertFalse(mapToList.containsKey("a"));
        assertNull(mapToList.peek("a"));
        assertEquals(0, mapToList.size());
        assertEquals(0, mapToList.totalValueCount());
    }

    @Test
    void remove_missingKeyReturnsFalse() {
        assertFalse(mapToList.remove("a", 1));
    }

    @Test
    void remove_missingValueReturnsFalseAndKeepsKey() {
        mapToList.add("a", 1);

        assertFalse(mapToList.remove("a", 2));
        assertTrue(mapToList.containsKey("a"));
        assertEquals(List.of(1), mapToList.get("a"));
    }

    @Test
    void removeAt_removesValueAtPosition() {
        mapToList.addAll("a", List.of(10, 20, 30));

        Integer removed = mapToList.removeAt("a", 1);

        assertEquals(20, removed);
        assertEquals(List.of(10, 30), mapToList.get("a"));
    }

    @Test
    void removeAt_lastValueAlsoRemovesKey() {
        mapToList.add("a", 10);

        Integer removed = mapToList.removeAt("a", 0);

        assertEquals(10, removed);
        assertFalse(mapToList.containsKey("a"));
        assertNull(mapToList.peek("a"));
    }

    @Test
    void removeAt_missingKeyOrBadIndexReturnsNull() {
        assertNull(mapToList.removeAt("a", 0));

        mapToList.addAll("a", List.of(10, 20));
        assertNull(mapToList.removeAt("a", -1));
        assertNull(mapToList.removeAt("a", 2));
    }

    @Test
    void removeAll_removesKeyAndValues() {
        mapToList.addAll("a", List.of(1, 2, 3));

        assertTrue(mapToList.removeAll("a"));

        assertFalse(mapToList.containsKey("a"));
        assertNull(mapToList.peek("a"));
        assertEquals(0, mapToList.totalValueCount());
    }

    @Test
    void removeAll_missingKeyReturnsFalse() {
        assertFalse(mapToList.removeAll("a"));
    }

    @Test
    void removeKey_returnsRemovedList() {
        mapToList.addAll("a", List.of(1, 2, 3));

        List<Integer> removed = mapToList.removeKey("a");

        assertEquals(List.of(1, 2, 3), removed);
        assertFalse(mapToList.containsKey("a"));
    }

    @Test
    void containsValue_checksWithinKeyOnly() {
        mapToList.add("a", 1);
        mapToList.add("b", 1);

        assertTrue(mapToList.containsValue("a", 1));
        assertTrue(mapToList.containsValue("b", 1));
        assertFalse(mapToList.containsValue("a", 2));
    }

    @Test
    void hasValues_falseForMissingKey() {
        assertFalse(mapToList.hasValues("a"));
    }

    @Test
    void hasValues_falseForPresentButEmptyKey() {
        mapToList.get("a");

        assertTrue(mapToList.containsKey("a"));
        assertFalse(mapToList.hasValues("a"));
    }

    @Test
    void hasValues_trueWhenKeyHasAtLeastOneValue() {
        mapToList.add("a", 1);

        assertTrue(mapToList.hasValues("a"));
    }

    @Test
    void valueCount_zeroForMissingKey() {
        assertEquals(0, mapToList.valueCount("a"));
    }

    @Test
    void totalValueCount_countsAcrossKeys() {
        mapToList.addAll("a", List.of(1, 2));
        mapToList.addAll("b", List.of(3, 4, 5));

        assertEquals(5, mapToList.totalValueCount());
    }

    @Test
    void keySet_isLiveView() {
        mapToList.add("a", 1);
        Set<String> keys = mapToList.keySet();

        assertTrue(keys.contains("a"));

        mapToList.add("b", 2);
        assertTrue(keys.contains("b"));

        keys.remove("a");
        assertFalse(mapToList.containsKey("a"));
    }

    @Test
    void asMap_returnsLiveViewOfUnderlyingMap() {
        Map<String, List<Integer>> map = mapToList.asMap();

        map.put("a", new java.util.ArrayList<>(List.of(1, 2)));
        assertEquals(List.of(1, 2), mapToList.get("a"));

        map.get("a").add(3);
        assertEquals(List.of(1, 2, 3), mapToList.get("a"));

        map.remove("a");
        assertFalse(mapToList.containsKey("a"));
    }

    @Test
    void clear_removesEverything() {
        mapToList.addAll("a", List.of(1, 2));
        mapToList.add("b", 3);

        mapToList.clear();

        assertTrue(mapToList.isEmpty());
        assertEquals(0, mapToList.size());
        assertEquals(0, mapToList.totalValueCount());
        assertTrue(mapToList.keySet().isEmpty());
    }

    @Test
    void directMutationOfReturnedListCanLeaveEmptyKeyPresent() {
        List<Integer> values = mapToList.get("a");
        values.add(1);

        values.clear();

        assertTrue(mapToList.containsKey("a"));
        assertEquals(0, mapToList.valueCount("a"));
        assertFalse(mapToList.hasValues("a"));
    }

    @Test
    void nullKeyIsForbiddenEverywhere() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> mapToList.add(null, 1)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.addAll(null, List.of(1))),
            () -> assertThrows(NullPointerException.class, () -> mapToList.get(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.get(null, 0)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.peek(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.getOrEmpty(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.remove(null, 1)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.removeKey(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.removeAll(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.removeAt(null, 0)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.containsKey(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.containsValue(null, 1)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.hasValues(null)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.valueCount(null))
        );
    }

    @Test
    void nullValueIsForbiddenWhereApplicable() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> mapToList.add("a", null)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.remove("a", null)),
            () -> assertThrows(NullPointerException.class, () -> mapToList.containsValue("a", null))
        );
    }

    @Test
    void nullCollectionIsForbidden() {
        assertThrows(NullPointerException.class, () -> mapToList.addAll("a", null));
    }

    @Test
    void addAll_forbidsNullElements() {
        List<Integer> values = new java.util.ArrayList<>();
        values.add(1);
        values.add(null);

        assertThrows(NullPointerException.class, () -> mapToList.addAll("a", values));
    }
}
