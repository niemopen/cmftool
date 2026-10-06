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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A set-like collection of strings that guarantees uniqueness by 
 * renaming on collision.  For example:
 * 
 * <pre>
 * foo/bar/file.xsd   (first add)  -> foo/bar/file.xsd
 * foo/bar/file.xsd   (second add) -> foo/bar/file_1.xsd
 * foo/bar/file.xsd   (third add)  -> foo/bar/file_2.xsd
 * 
 * http://h/a/file.xsd#type  -> http://h/a/file.xsd#type
 * http://h/a/file.xsd#type  -> http://h/a/file.xsd#type_1
 * </pre>
 *
 * If the filename has no extension, the suffix is appended to the end:
 * If a generated name also collides (e.g., {@code file_1.xsd} already exists), the suffix
 * number is incremented until a free name is found.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class UniquePathSet implements Iterable<String> {
    
    private final Set<String> values = new LinkedHashSet<>();
    private final String sep;
    
    public UniquePathSet ()             { sep = "_"; }
    public UniquePathSet (String s)     { sep = s; }
    
    public String add (String val) {
        if (null == val)     return null;
        if (values.add(val)) return val;
        var rename = val;
        int n = 1;
        while (values.contains(rename)) {
            rename = withSuffix(val, n++);
        }
        values.add(rename);
        return rename;
    }
    
    /**
     * @param value value to check
     * @return true if this set already contains {@code value}
     */
    public boolean contains(String value) {
        return values.contains(value);
    }

    /**
     * @return number of stored values
     */
    public int size() {
        return values.size();
    }

    /**
     * Removes all values from this set.
     */
    public void clear() {
        values.clear();
    }

    @Override
    public Iterator<String> iterator() {
        return Collections.unmodifiableSet(values).iterator();
    }   
    
    private String withSuffix (String input, int n) {
        int hash = input.indexOf('#');
        if (hash >= 0) {
            String base = input.substring(0, hash + 1);   // includes '#'
            String frag = input.substring(hash + 1);      // fragment content (may be empty)
            return base + frag + sep + n;
        }

        // No fragment: modify filename/last segment
        int lastSep = Math.max(input.lastIndexOf('/'), input.lastIndexOf('\\'));
        int lastDot = input.lastIndexOf('.');

        boolean hasExtension = lastDot > lastSep; // dot must be inside last segment
        if (!hasExtension) {
            return input + sep + n;
        }

        String prefix = input.substring(0, lastDot);
        String ext = input.substring(lastDot); // includes '.'
        return prefix + sep + n + ext;        
    }
}
