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
package org.mitre.niem.json;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Pretty-print a JsonObject to a Writer.<ul>
 * <li>Array of scalars goes on a single line; e.g. <code>[ "@context", "msg:Request" ]</code></li>
 * <li>Object with one key goes on a single line; e.g. <code>{ "required": [ "@context", "msg:Request" ] }</code</li>
 * </ul>
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

public class JSONWriter {

    private static final Gson GSON = new Gson();
    private static final String INDENT_UNIT = "  ";

    /**
     * Pretty-print a JsonElement to the given Writer, starting at indent level 0.
     */
    public static void write(JsonElement element, Writer writer) throws IOException {
        write(element, writer, 0);
    }

    /**
     * Pretty-print a JsonElement to the given Writer, starting at the given indent level.
     * Rules:
     *  - arrays of only scalars on one line: [ "f1", "v2", "v3" ]
     *  - objects with a single scalar value on one line: { "foo": "bar" }
     */
    private static void write(JsonElement el, Writer writer, int level) throws IOException {
        if (el == null || el.isJsonNull() || el.isJsonPrimitive()) {
            writer.write(GSON.toJson(el));
        } else if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            if (arr.size() == 0) {
                writer.write("[]");
                return;
            }

            boolean allScalars = isAllScalars(arr);

            if (allScalars) {
                writer.write("[ ");
                for (int i = 0; i < arr.size(); i++) {
                    if (i > 0) writer.write(", ");
                    writer.write(GSON.toJson(arr.get(i)));
                }
                writer.write(" ]");
            } else {
                writer.write("[\n");
                for (int i = 0; i < arr.size(); i++) {
                    indent(writer, level + 1);
                    write(arr.get(i), writer, level + 1);
                    if (i < arr.size() - 1) writer.write(",");
                    writer.write("\n");
                }
                indent(writer, level);
                writer.write("]");
            }
        } else if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.size() == 0) {
                writer.write("{}");
                return;
            }

            // Single-key object:
            //   - if value is scalar, OR
            //   - if value is an array of scalars
            // then print on one line:
            //   { "key": value }
            if (obj.size() == 1) {
                Map.Entry<String, JsonElement> entry = obj.entrySet().iterator().next();
                JsonElement val = entry.getValue();

                boolean scalar = (val == null || val.isJsonNull() || val.isJsonPrimitive());
                boolean scalarArray = val != null && val.isJsonArray() && isAllScalars(val.getAsJsonArray());

                if (scalar || scalarArray) {
                    writer.write("{ ");
                    writer.write(GSON.toJson(entry.getKey()));
                    writer.write(": ");
                    // For scalar array, write() will already render it on one line
                    write(val, writer, level);
                    writer.write(" }");
                    return;
                }
            }

            writer.write("{\n");
            int i = 0;
            int size = obj.entrySet().size();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                indent(writer, level + 1);
                writer.write(GSON.toJson(entry.getKey()));
                writer.write(": ");
                write(entry.getValue(), writer, level + 1);
                if (i < size - 1) writer.write(",");
                writer.write("\n");
                i++;
            }
            indent(writer, level);
            writer.write("}");
        }
    }

    private static boolean isAllScalars(JsonArray arr) {
        for (JsonElement e : arr) {
            if (!(e == null || e.isJsonNull() || e.isJsonPrimitive())) {
                return false;
            }
        }
        return true;
    }

    private static void indent(Writer writer, int level) throws IOException {
        for (int i = 0; i < level; i++) {
            writer.write(INDENT_UNIT);
        }
    }
}

    

