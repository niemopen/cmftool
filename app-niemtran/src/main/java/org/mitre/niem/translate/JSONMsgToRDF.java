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

package org.mitre.niem.translate;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.util.FmtUtils;
import org.apache.jena.vocabulary.RDF;
import org.mitre.niem.cmf.Model;

/**
 * A class for transforming a NIEM JSON message to RDF/Turtle.
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class JSONMsgToRDF {

    private static final String SEQ_URI = "http://niemtran.niem.mitre.org/SeqURI";
    private static final String RDF_TYPE_URI = RDF.type.getURI();

    private Model nModel = null;           // NIEM model for rdf:type links
    private JsonObject context = null;     // context object holding prefix,uri pairs
    private boolean prettyPrint = true;    // make the output RDF pretty?

    public JSONMsgToRDF() { }

    /**
     * Sets the model for the messages to be converted.  Bad things will happen
     * if the model and message do not correspond.
     * @param m NIEM model object
     */
    public void setModel(Model m) { nModel = m; }

    /**
     * The converter will produce readable RDF if pretty-printing is true.
     * @param p pretty-printing flag
     */
    public void setPrettyPrint(boolean p) { prettyPrint = p; }

    /**
     * Supplies a context object for messages that do not contain a full context.
     * The parameter can either be a <code>{ "@context": { ... } }</code> object,
     * or can be the object value of such a pair.
     * @param o
     */
    public void setContext(JsonObject o) { context = o; }

    public void setContext(String s) throws NIEMTranException {
        if (null == s) {
            context = null;
            return;
        }
        try {
            context = JsonParser.parseString(s).getAsJsonObject();
            if (context.has("@context")) {
                context = context.get("@context").getAsJsonObject();
            }
        } catch (JsonSyntaxException ex) {
            throw new NIEMTranException("Can't parse context JSON: " + jsonSyntaxExMsg(ex));
        } catch (IllegalStateException ex) {
            throw new NIEMTranException("Context JSON is not an object");
        }
    }

    public void convert(String msg, Writer w) throws IOException, NIEMTranException {
        if (msg == null) {
            throw new NIEMTranException("JSONMsgToRDF.convert: Message is null");
        }
        convert(new StringReader(msg), w);
    }

    public void convert(Reader msgR, Writer w) throws IOException, NIEMTranException {
        if (null == msgR) {
            throw new NIEMTranException("JSONMsgToRDF.convert: Reader is null");
        }

        JsonObject msgO = null;
        try {
            var me = JsonParser.parseReader(msgR);
            msgO = me.getAsJsonObject();
        } catch (JsonSyntaxException ex) {
            throw new NIEMTranException("Can't parse message JSON: " + jsonSyntaxExMsg(ex));
        } catch (IllegalStateException ex) {
            throw new NIEMTranException("Message JSON is not an object");
        }

        convert(msgO, w);
    }

    public void convert(JsonObject msg, Writer w) throws IOException, NIEMTranException {
        if (null == nModel) {
            throw new NIEMTranException("JSONMsgToRDF.convert: Message model is null");
        }
        if (null == msg) {
            throw new NIEMTranException("JSONMsgToRDF.convert: Message is null");
        }
        if (null == w) {
            throw new NIEMTranException("JSONMsgToRDF.convert: Writer is null");
        }

        // Make sure we have a context object; use converter's context if setContext was used.
        // Make sure there's a rdf entry in the context if we have a NIEM model.
        // Remove context from message object for now.
        if (null != context) {
            msg.add("@context", context);
        }
        var cxtE = msg.remove("@context");
        if (null == cxtE || !cxtE.isJsonObject()) {
            throw new NIEMTranException("Can't convert JSON message (no context provided)");
        }
        var cxtO = cxtE.getAsJsonObject();

        // Traverse JSON message breadth-first, add sequence numbers and rdf:type links
        // to each object
        var onum = 0;
        var deq = new ArrayDeque<JsonObject>();
        deq.add(msg);
        while (!deq.isEmpty()) {
            var obj = deq.removeFirst();
            obj.addProperty(SEQ_URI, onum++);
            var entries = new ArrayList<Map.Entry<String, JsonElement>>(obj.entrySet());
            for (int i = entries.size() - 1; i >= 0; i--) {
                var e = entries.get(i);
                var key = e.getKey();
                var val = e.getValue();
                var ctQ = "";
                if (SEQ_URI.equals(key)) {
                    continue;
                }
                if ("@type".equals(key)) {
                    continue;
                }
                if (null != nModel) {
                    var kI = expandCompactIRI(cxtO, key);
                    if (null != kI) {
                        var op = nModel.uriToObjectProperty(kI);
                        if (null != op) {
                            var ctU = op.classType().uri();
                            ctQ = createCompactIRI(cxtO, ctU);
                        }
                    }
                }
                if (val.isJsonObject()) {
                    var vobj = val.getAsJsonObject();
                    if (!ctQ.isEmpty()) {
                        vobj.addProperty("@type", ctQ);
                    }
                    deq.addFirst(vobj);
                } else if (val.isJsonArray()) {
                    var ja = val.getAsJsonArray();
                    for (int j = ja.size() - 1; j >= 0; j--) {
                        var ae = ja.get(j);
                        if (ae.isJsonObject()) {
                            var ao = ae.getAsJsonObject();
                            if (!ctQ.isEmpty()) {
                                ao.addProperty("@type", ctQ);
                            }
                            deq.addFirst(ao);
                        }
                    }
                }
            }
        }

        // Put context back into message object
        msg.add("@context", cxtO);

        // Parse JSON-LD into RDF model
        var gson = prettyPrint
            ? new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
            : new GsonBuilder().disableHtmlEscaping().create();
        var mtxt = gson.toJson(msg);
        var mrdr = new StringReader(mtxt);
        var rMod = ModelFactory.createDefaultModel();
        try {
            RDFParser.create().source(mrdr).lang(Lang.JSONLD).parse(rMod);
        } catch (RiotException ex) {
            throw new NIEMTranException("Couldn't convert JSON to RDF: " + ex.getMessage());
        }

        // Iterate over statements to get subject sequence numbers
        var stmtL = new ArrayList<Statement>();
        var subSeq = new HashMap<String, Integer>();
        var sit = rMod.listStatements();
        while (sit.hasNext()) {
            var stmt = sit.nextStatement();
            var subj = stmt.getSubject();
            var sid = subj.toString();
            var pred = stmt.getPredicate();
            if (SEQ_URI.equals(pred.getURI())) {
                var obj = stmt.getObject();
                var seq = obj.asLiteral().getInt();
                subSeq.put(sid, seq);
            }
            stmtL.add(stmt);
        }

        // Sort statement list by subject sequence, then predicate
        Comparator<Statement> cmp =
            Comparator.comparingInt((Statement st) ->
                subSeq.getOrDefault(st.getSubject().toString(), Integer.MAX_VALUE))
                .thenComparing(st -> st.getPredicate().equals(RDF.type) ? 0 : 1)
                .thenComparing(st -> st.getPredicate().toString());

        stmtL.sort(cmp);

        // Create map of new blank node IDs
        var pm = (PrefixMapping) rMod;
        var bnid = new HashMap<String, String>();
        for (var s : stmtL) {
            var pred = s.getPredicate();
            if (SEQ_URI.equals(pred.getURI())) {
                var subj = s.getSubject();
                var sid = FmtUtils.stringForNode(subj.asNode(), pm);
                var seq = s.getObject().asLiteral().getInt();
                bnid.put(sid, String.format("_:b%02d", seq));
            }
        }

        // Write @prefix lines
        var pmap = new TreeMap<>(rMod.getNsPrefixMap());
        for (var e : pmap.entrySet()) {
            var p = e.getKey();
            var i = e.getValue();
            w.write("@prefix " + p + ": <" + i + "> .\n");
        }

        // Write statements
        for (var stmt : stmtL) {
            var subj = stmt.getSubject();
            var pred = stmt.getPredicate();
            var obj = stmt.getObject();
            if (SEQ_URI.equals(pred.getURI())) {
                continue;
            }

            var sid = FmtUtils.stringForNode(subj.asNode(), pm);
            if (subj.isAnon()) {
                sid = bnid.getOrDefault(sid, sid);
            }

            var pnam = FmtUtils.stringForNode(pred.asNode(), pm);
            if (pred.equals(RDF.type)) {
                pnam = "a";
            }

            var objS = FmtUtils.stringForNode(obj.asNode(), pm);
            if (obj.isAnon()) {
                objS = bnid.getOrDefault(objS, objS);
            }

            w.write(sid + " " + pnam + " " + objS + " .\n");
        }
    }

    public static String expandCompactIRI(JsonObject cxtO, String key) throws NIEMTranException {
        var term = key;
        var colon = key.indexOf(':');
        if (colon < 0) {
            var kmap = cxtO.get(key);
            if (null == kmap) {
                return key;
            }
            if (!cxtO.get(key).isJsonPrimitive()) {
                throw new NIEMTranException("Term " + key + " in NIEM @context does not map to a compact IRI");
            }
            term = cxtO.get(key).getAsString();
            colon = term.indexOf(':');
            if (colon < 0) {
                throw new NIEMTranException("Term " + key + " in NIEM @context does not map to a compact IRI");
            }
        }
        if (colon == term.length() - 1) {
            throw new NIEMTranException("Key " + key + " in NIEM @context is not a valid compact IRI (no suffix)");
        }
        var prefix = term.substring(0, colon);
        var suffix = term.substring(colon + 1);
        var base = cxtO.get(prefix);
        if (null == base) {
            return null;              // prefix isn't mapped, oh well
        }
        if (!base.isJsonPrimitive()) {
            throw new NIEMTranException("Prefix " + key + " in NIEM @context is not mapped to a string");
        }
        var baseU = base.getAsString();
        return baseU + suffix;
    }

    public static String createCompactIRI(JsonObject cxtO, String iri) {
        var prefix = "";
        var base = "";
        for (var e : cxtO.entrySet()) {
            var p = e.getKey();
            var b = e.getValue().getAsString();
            if (iri.startsWith(b)) {
                if (b.length() > base.length()) {
                    prefix = p;
                    base = b;
                }
            }
        }
        if (base.isEmpty()) {
            return iri;
        }
        return prefix + ":" + iri.substring(base.length());
    }

    private String jsonSyntaxExMsg(JsonSyntaxException ex) {
        var exm = ex.getMessage();
        var pos = exm.indexOf("malformed");
        if (pos > 0) {
            exm = exm.substring(pos);
        }
        pos = exm.indexOf(" path");
        if (pos > 0) {
            exm = exm.substring(0, pos);
        }
        return exm;
    }
}
