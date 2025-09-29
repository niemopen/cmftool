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

import com.google.gson.JsonObject;
import org.mitre.niem.cmf.Model;

/**
 * A class to create a JSON-LD context object from a Model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Context {

    public Context () { }
    
    public static JsonObject create (Model m) {
        var res = new JsonObject();
        for (var ns : m.namespaceList()) {
            var pre = ns.prefix();
            var uri = ns.uri();
            res.addProperty(pre, uri);
        }
        for (var p : m.propertyL()) {
            if (!p.isOrdered()) continue;
            var obj = new JsonObject();
            obj.addProperty("@container", "@list");
            res.add(p.qname(), obj);
        }
        return res;        
    }
}
