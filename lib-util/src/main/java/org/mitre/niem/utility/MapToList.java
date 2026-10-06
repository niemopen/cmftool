/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2025 The MITRE Corporation.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A utility class that maps keys of type {@code K} to zero or more values of
 * type {@code V}.
 *
 * <p>Internally this class stores a {@code Map<K, List<V>>}. Each key is
 * associated with a mutable {@link List} of values.
 *
 * <p><b>Important:</b> the {@link #get(Object)} method has side effects. If the
 * supplied key is not already present, a new empty list is created, stored in
 * the map, and returned.
 *
 * <p>The lists returned by {@link #get(Object)} are live, mutable internal
 * lists. Callers are allowed to modify these lists directly. Changes made
 * through a returned list are reflected in this object.
 *
 * <p>Null keys, null values, and null collections are not permitted. Methods
 * that receive null arguments throw {@link NullPointerException}.
 *
 * <p>This class is not thread-safe.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MapToList<K, V> {

    private final Map<K, List<V>> map;

    /**
     * Constructs an empty map-to-list structure.
     */
    public MapToList() {
        this.map = new HashMap<>();
    }

    /**
     * Adds a value to the list associated with a key.
     *
     * <p>If the key is not already present, this method creates a new list for
     * that key.
     *
     * @param key the key
     * @param value the value to append
     * @throws NullPointerException if {@code key} or {@code value} is null
     */
    public void add(K key, V value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }

    /**
     * Adds all values to the list associated with a key.
     *
     * <p>If the key is not already present, this method creates a new list for
     * that key.
     *
     * @param key the key
     * @param values the values to append
     * @throws NullPointerException if {@code key}, {@code values}, or any
     *         element of {@code values} is null
     */
    public void addAll(K key, Collection<? extends V> values) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(values, "values must not be null");
        for (var value : values) {
            Objects.requireNonNull(value, "values must not contain null elements");
        }
        if (values.isEmpty()) return;
        map.computeIfAbsent(key, k -> new ArrayList<>()).addAll(values);
    }

    /**
     * Returns the mutable list associated with a key.
     *
     * <p>This method has side effects. If the key is not present, a new empty
     * list is created, stored in the map, and returned.
     *
     * <p>The returned list is the live internal list for the key. Callers may
     * modify it directly.
     *
     * @param key the key
     * @return the mutable list for the key, never null
     * @throws NullPointerException if {@code key} is null
     */
    public List<V> get(K key) {
        Objects.requireNonNull(key, "key must not be null");
        return map.computeIfAbsent(key, k -> new ArrayList<>());
    }

    /**
     * Returns the value at the specified position in the list associated with a
     * key.
     *
     * <p>If the key is not present, this method behaves like {@link #get(Object)}
     * and creates a new empty list for the key.
     *
     * @param key the key
     * @param pos the zero-based position
     * @return the value at {@code pos}, or null if {@code pos} is out of range
     * @throws NullPointerException if {@code key} is null
     */
    public V get(K key, int pos) {
        var lst = get(key);
        if (pos < 0 || pos >= lst.size()) return null;
        return lst.get(pos);
    }

    /**
     * Returns the existing list associated with a key without creating one.
     *
     * <p>Unlike {@link #get(Object)}, this method has no side effects.
     *
     * @param key the key
     * @return the existing mutable list for the key, or null if the key is not
     *         present
     * @throws NullPointerException if {@code key} is null
     */
    public List<V> peek(K key) {
        Objects.requireNonNull(key, "key must not be null");
        return map.get(key);
    }

    /**
     * Returns the existing list associated with a key, or an empty immutable
     * list if the key is not present.
     *
     * <p>This method has no side effects.
     *
     * @param key the key
     * @return the existing list, or an empty list if the key is not present
     * @throws NullPointerException if {@code key} is null
     */
    public List<V> getOrEmpty(K key) {
        Objects.requireNonNull(key, "key must not be null");
        var lst = map.get(key);
        return lst != null ? lst : Collections.emptyList();
    }

    /**
     * Removes the first occurrence of a value from the list associated with a key.
     *
     * <p>If removal leaves the list empty, the key is removed from the map.
     *
     * @param key the key
     * @param value the value to remove
     * @return true if a value was removed, false otherwise
     * @throws NullPointerException if {@code key} or {@code value} is null
     */
    public boolean remove(K key, V value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        var lst = map.get(key);
        if (lst == null) return false;
        boolean removed = lst.remove(value);
        if (lst.isEmpty()) map.remove(key);
        return removed;
    }

    /**
     * Removes and returns the list associated with a key.
     *
     * @param key the key
     * @return the removed list, or null if the key was not present
     * @throws NullPointerException if {@code key} is null
     */
    public List<V> removeKey(K key) {
        Objects.requireNonNull(key, "key must not be null");
        return map.remove(key);
    }

    /**
     * Removes all values associated with a key.
     *
     * @param key the key
     * @return true if the key was present and removed, false otherwise
     * @throws NullPointerException if {@code key} is null
     */
    public boolean removeAll(K key) {
        Objects.requireNonNull(key, "key must not be null");
        return map.remove(key) != null;
    }

    /**
     * Removes the value at the specified position from the list associated with
     * a key.
     *
     * <p>If removal leaves the list empty, the key is removed from the map.
     *
     * @param key the key
     * @param pos the zero-based position
     * @return the removed value, or null if the key is not present or the
     *         position is out of range
     * @throws NullPointerException if {@code key} is null
     */
    public V removeAt(K key, int pos) {
        Objects.requireNonNull(key, "key must not be null");
        var lst = map.get(key);
        if (lst == null || pos < 0 || pos >= lst.size()) return null;
        V removed = lst.remove(pos);
        if (lst.isEmpty()) map.remove(key);
        return removed;
    }

    /**
     * Returns true if the map contains the specified key.
     *
     * @param key the key
     * @return true if the key is present
     * @throws NullPointerException if {@code key} is null
     */
    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "key must not be null");
        return map.containsKey(key);
    }

    /**
     * Returns true if the list for a key contains the specified value.
     *
     * @param key the key
     * @param value the value to test
     * @return true if the key exists and its list contains the value
     * @throws NullPointerException if {@code key} or {@code value} is null
     */
    public boolean containsValue(K key, V value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        var lst = map.get(key);
        return lst != null && lst.contains(value);
    }

    /**
     * Returns true if the key exists and has at least one associated value.
     *
     * @param key the key
     * @return true if the key exists and its list is non-empty
     * @throws NullPointerException if {@code key} is null
     */
    public boolean hasValues(K key) {
        Objects.requireNonNull(key, "key must not be null");
        var lst = map.get(key);
        return lst != null && !lst.isEmpty();
    }

    /**
     * Returns the number of keys currently in the map.
     *
     * @return the key count
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns true if the map contains no keys.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns the number of values associated with a key.
     *
     * @param key the key
     * @return the number of values for the key, or 0 if the key is not present
     * @throws NullPointerException if {@code key} is null
     */
    public int valueCount(K key) {
        Objects.requireNonNull(key, "key must not be null");
        var lst = map.get(key);
        return lst == null ? 0 : lst.size();
    }

    /**
     * Returns the total number of values stored across all keys.
     *
     * @return total value count
     */
    public int totalValueCount() {
        int total = 0;
        for (var lst : map.values()) total += lst.size();
        return total;
    }

    /**
     * Returns the set of keys currently present in the map.
     *
     * <p>The returned set is backed by the underlying map.
     *
     * @return the key set
     */
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * Returns a live view of the underlying map.
     *
     * <p>The returned map is the internal map used by this object. Modifications
     * to the returned map or its lists affect this object directly.
     *
     * @return the underlying map
     */
    public Map<K, List<V>> asMap() {
        return map;
    }

    /**
     * Removes all keys and values from this object.
     */
    public void clear() {
        map.clear();
    }
}
