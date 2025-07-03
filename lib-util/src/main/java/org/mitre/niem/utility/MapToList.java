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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class for a HashMap from keys of class K to values which are an ArrayList
 * of class V.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 * @param <K>
 * @param <V>
 */
public class MapToList<K,V> {
    private final Map<K,List<V>> map;
    
    public MapToList () { 
        this.map = new HashMap<>(); 
    }
    
    public void add (K key, V value) {
        var lst = get(key);
        lst.add(value);
    }
    
    public void addAll (K key, Collection<V> values) {
        var lst = get(key);
        lst.addAll(values);
    }
    
    public List<V> get (K key) {
        var lst = map.get(key);
        if (null == lst) {
            lst = new ArrayList<>();
            map.put(key, lst);          
        }
        return lst;
    }
    
    public V get (K key, int pos) {
        var lst = get(key);
        if (null == lst || pos < 0 || pos >= lst.size()) return null;
        return lst.get(pos);
    }
    
    public boolean remove (K key, V value) {
        var lst = map.get(key);
        if (null == lst) return false;
        return lst.remove(value);
    }
    
    public void removeKey (K key) {
        map.remove(key);
    }
    
    public boolean containsKey (K key) {
        return map.containsKey(key);
    }
        
    public boolean containsValue (K key, V value) {
        var s = map.get(key);
        return s != null && s.contains(value);
    }
    
    public Set<K> keySet ()     { return map.keySet(); }
    public int size ()          { return map.size(); }
    public void clear ()        { map.clear(); }    
}
