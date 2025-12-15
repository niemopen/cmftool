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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import java.io.Writer;
import java.util.HashSet;
import java.util.TreeSet;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToName;
import static org.mitre.niem.xml.XMLSchemaDocument.qnToPrefix;

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
        var nsS = new TreeSet<Namespace>();
        for (var c : m.componentList()) {
            var ns = c.namespace();
            if (ns.isModelNS()) nsS.add(ns);
        }
        for (var ns : nsS) {
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
    
    public static JsonObject create (Model m, Mapping map) {
        try { return create(m, map, false); } catch (CMFException ex) { return null; } // CAN'T HAPPEN
    }
    
    public static JsonObject create (Model m, Mapping map, boolean noPrefix) throws CMFException {
        var res = create(m);
        
        // Check mapped compnents for duplicate local name, or a match
        // with the prefix of a model namespace
        var needP  = false;
        var lnameS = new HashSet<String>();
        for (var c : m.componentList()) {           // model component; eg. nc:PersonSurName
            var mcQ = map.qnToQ(c.qname());         // mapped QN for component; eg. foo:lname
            if (null == mcQ) continue;
            var mln = qnToName(mcQ);                // local name of mapped QN; eg. lname
            var err = "";
            if (null != m.namespaceObj(mln)) err = "mapped local name matches namespace prefix " + mln;
            else if (lnameS.contains(mln))   err = "multiple mappings with local name " + mln;
            if (!err.isEmpty()) {
                if (noPrefix) throw new CMFException(err);
                else needP = true;
            }
            lnameS.add(mln);
        }
        // Now add context entries for each mapped component
        for (var c : m.componentList()) {
            var mcQ = map.qnToQ(c.qname());         // mapped QN for component; eg. foo:lname
            if (null == mcQ) continue;
            var mlp = qnToPrefix(mcQ);              // prefix of mapped QN; eg. foo
            var mln = qnToName(mcQ);                // local name of mapped QN; eg. lname            
            if (needP) res.addProperty(mcQ, c.qname());
            else res.addProperty(mln, c.qname());
        }
        return res;
    }
    
    public static void createTo (Writer w, Model m) {
        write(create(m), w);
    }
    
    public static void createTo (Writer w, Model m, Mapping map) {
        write(create(m, map), w);
    }
    
    public static void createTo (Writer w, Model m, Mapping map, boolean noPrefix) throws CMFException {
        write(create(m, map, noPrefix), w);
    }
    
    public static void write (JsonObject res, Writer w) {
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var cxt = new JsonObject();
        cxt.add("@context", res);
        gson.toJson(cxt, w);
    }
}
