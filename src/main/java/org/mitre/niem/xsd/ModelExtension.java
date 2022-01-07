/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2021 The MITRE Corporation.
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
package org.mitre.niem.xsd;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.mitre.niem.NIEMConstants.DEFAULT_NIEM_VERSION;
import static org.mitre.niem.NIEMConstants.XSD_NS_URI;
import org.mitre.niem.cmf.Model;
import static org.mitre.niem.cmf.Namespace.mungedPrefix;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A class for information within a NIEM XML schema that is not part of the
 * Common Model Format, specifically providing:<ul>
 * <li> relative path to schema document for each namespace></li>
 * <li> conformance targets asserted for each namespace</li>
 * <li> schema version attribute for each namespace</li>
 * <li> namespace prefix for appinfo, proxy, structures namespaces</li>
 * <li> indicator to say if a QName is an XML attribute</li></ul>
 * 
 * The contents of this object may be read from a ModelExtension document
 * created by ModelFromXSD processing a NIEM schema pile. In this case, each
 * builtin namespace will have a prefix that is unique among all model and
 * builtin namespaces.
 *
 * The contents of this object may be read from a ModelExtension document
 * created by hand.  In this case the class must generate correct default values
 * for any data not provided.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

public class ModelExtension {
    static final Logger LOG = LogManager.getLogger(ModelExtension.class);    
    private static final String ME_URI = "http://reference.niem.gov/specification/cmf/XMLExtension/1.0/";

    private final Model m;                                                    // Model object being extended
    private final Map<String,String> prefixMap;                               // nsPrefix -> nsURI, copied from Model
    private final TreeMap<String,SchemaDocRec> nsData = new TreeMap<>();      // info about each namespace
    private final TreeSet<String> attQNames = new TreeSet<>();                // set of attribute QNames
    
    public String getCatalogFilepath (String ns)    { return getRec(ns).cpath; }
      
    public boolean isAttribute (String qname) { return attQNames.contains(qname); }
    public void setIsAttribute (String qname) { attQNames.add(qname); }
    
    public void setCatalogFilepath (String ns, String fp)     { getRec(ns).cpath = fp; }
    public void setConformanceTargets (String ns, String cts) { getRec(ns).ctarg = cts; }
    public void setDocumentFilepath (String ns, String fp)    { getRec(ns).fpath = fp; }
    public void setNIEMVersion(String ns, String v)           { getRec(ns).nversion = v; }
    public void setPrefix (String ns, String p)               { getRec(ns).prefix = p; }    // to set a utility namespace prefix
    public void setNSVersion (String ns, String v)            { getRec(ns).sversion = v; }
    
    public ModelExtension (Model m) {
        this.m = m;
        prefixMap = m.getPrefixMap();       // a modifiable copy
    }
    
    /**
     * Returns the conformance target URIs asserted for this namespace, or null if none.
     * @param ns namespace URI
     * @return conformance target URIs
     */
    public String getConformanceTargets (String ns) {
        return getRec(ns).ctarg;
    }
    
    /**
     * Returns the relative file path for the schema document corresponding to the
     * specified namespace.  The default value is based on the namespace prefix.
     * Since every namespace has a unique prefix, this works.
     * @param ns namespace URI
     * @return file path
     */
    public String getDocumentFilepath (String ns) {
        String fp = getRec(ns).fpath;
        if (null == fp) fp = String.format("./%s.xsd", getRec(ns).prefix);
        return fp;
    }
    
    /**
     * Returns the NIEM version of the specified namespace, or the default.
     * @param ns namespace URI
     * @return NIEM version string (e.g. "5.0")
     */
    public String getNIEMVersion (String ns) {
        String rv = getRec(ns).nversion;
        if (null != rv) return rv;
        return DEFAULT_NIEM_VERSION;
    }
    
    /**
     * Returns the schema version (i.e. the @version attribute) for this namespace,
     * or null if none specified.
     * @param ns namespace URI
     * @return schema version
     */
    public String getNSVersion (String ns) {
        return getRec(ns).sversion;
    }
       
    /**
     * Returns the namespace prefix for the builtin namespace with the specified URI.
     * Possibly creates a default prefix if none specified.  That prefix could be
     * munged to be unique.
     * @param ns builtin namespace URI
     * @return namespace prefix
     */
    public String getBuiltinPrefix (String ns) {
        String prefix = getRec(ns).prefix;
        if (null != prefix) return prefix;
        prefix = mungedPrefix(m.getPrefixMap(), prefix);
        return prefix;
    }
            
    public void writeXML (Writer w) throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        DocumentBuilder db = ParserBootstrap.docBuilder();
        Document dom = db.newDocument();
        Element root = dom.createElementNS(ME_URI, "ModelExtension");
        dom.appendChild(root);
        
        // Iterate through namespace records
        nsData.forEach((ns,rec) -> {
            if (!XSD_NS_URI.equals(ns)) {
                Element sde = dom.createElementNS(ME_URI, "SchemaDocument");
                addChild(dom, sde, "NamespaceURI", ns);
                addChild(dom, sde, "CatalogFilePathText", rec.cpath);
                addChild(dom, sde, "ConformanceTargetURIList", rec.ctarg);
                addChild(dom, sde, "DocumentFilePathText", rec.fpath);
                addChild(dom, sde, "NIEMVersionText", rec.nversion);
                addChild(dom, sde, "SchemaVersionText", rec.sversion);                
                addChild(dom, sde, "NamespacePrefixText", rec.prefix);                
                root.appendChild(sde);
            }
        });
        for (String aqn : attQNames) {
            Element ae = dom.createElementNS(ME_URI, "AttributeQName");
            ae.setTextContent(aqn);
            root.appendChild(ae);
        }
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter ostr = new StringWriter();
        tr.transform(new DOMSource(dom), new StreamResult(w));        
    }
    
    private void addChild (Document dom, Element p, String name, String val) {
        if (null == val) return;
        Element c = dom.createElementNS(ME_URI, name);
        c.setTextContent(val);
        p.appendChild(c);
    }
    
    public void readXML (InputStream is) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory sf = SAXParserFactory.newInstance();
        sf.setNamespaceAware(true);
        sf.setValidating(false);
        SAXParser saxp = sf.newSAXParser();
        ModelExtension.Handler h = new ModelExtension.Handler(this);
        saxp.parse(is, h);
        
        // Look for duplicate file paths and duplicate namespace prefixes
        Map<String,String> fpmap = new HashMap<>();     // fpath -> nsURI
        nsData.forEach((ns,rec) -> {
            if (null != rec.fpath && fpmap.containsKey(rec.fpath)) {
                String ons = fpmap.get(rec.fpath);
                LOG.warn("readXML: Duplicate filepaths: {} and {} both map to {}", ns, ons, rec.fpath);
                LOG.warn("readXML: Changing file path of {} to the default", ons);
                getRec(ons).fpath = null;
            }
            fpmap.put(rec.fpath, ns);
            String opr = rec.prefix;
            if (null != opr && prefixMap.containsKey(opr) && !prefixMap.get(opr).equals(ns)) {
                String npr = mungedPrefix(prefixMap, opr);
                LOG.warn("readXML: Can't assign prefix {} to {} (already bound to {})", opr, ns, prefixMap.get(opr));
                LOG.warn("readXML: Assigned prefix {} to {} instead", npr, ns);
                rec.prefix = npr;
            }
        });
        
     }
    
    private class Handler extends DefaultHandler {
        ModelExtension me;
        String cns = null;
        SchemaDocRec nrec = null;
        StringBuilder sb = null;
        
        Handler (ModelExtension x) { this.me = x; }
        
        @Override
        public void startElement(String eNamespace, String eLocalName, String eQName, Attributes atts) {
            switch(eLocalName) {
                case "AttributeQName":
                case "ConformanceTargetURIList":
                case "DocumentFilePathText":
                case "NamespacePrefixText":
                case "NamespaceURI":
                case "NIEMVersionText":
                case "SchemaVersionText":
                    sb = new StringBuilder();
                    break;
                case "SchemaDocument":
                    nrec = new SchemaDocRec();
                    break;
            }
        }
        
        @Override
        public void endElement(String eNamespace, String eLocalName, String eQName) {
            switch(eLocalName) {
                case "AttributeQName":              me.setIsAttribute(sb.toString()); break;
                case "ConformanceTargetURIList":    nrec.ctarg = sb.toString(); break;
                case "DocumentFilePathText":        nrec.fpath = sb.toString(); break;
                case "NamespacePrefixText":         nrec.prefix = sb.toString(); break;
                case "NamespaceURI":                cns = sb.toString(); break;
                case "NIEMVersionText":             nrec.nversion = sb.toString(); break;
                case "SchemaVersionText":           nrec.sversion = sb.toString(); break;
                case "SchemaDocument":              me.nsData.put(cns, nrec); break;
            }            
        }
        
        @Override
        public void characters (char[] ch, int start, int length) {
            if (null != sb) sb.append(ch, start, length);
        }
    }
    
    private SchemaDocRec getRec (String ns) {
        SchemaDocRec r = nsData.get(ns);
        if (null == r) {
            r = new SchemaDocRec();
            nsData.put(ns, r);
        }
        return r;   
    }
    
    private class SchemaDocRec {
        String cpath = null;     // file path to catalog with NS entry
        String ctarg = null;     // namespace conformance targets
        String fpath = null;     // file path to schema document
        String nversion = null;  // NIEM version 
        String prefix = null;    // namespace declaration prefix; for utility namespaces
        String sversion = null;  // schema element version attribute  
        
        @Override
        public String toString() {
            return String.format("  ctarg= %s\n  fpath= %s\n  nversion= %s\n  prefix= %s\n  sversion= %s\n",
                    ctarg, fpath, nversion, prefix, sversion);
        }
    }
}
