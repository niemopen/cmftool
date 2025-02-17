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
package org.mitre.niem.xml;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.w3c.dom.Node.ELEMENT_NODE;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A class for providing information from a schema document that is not
 * available through the Xerces XML Schema (XSModel) API. Originally written
 * to extract attributes from component declarations; eg.
 *   xs:element ref="foo:bar" appinfo:deprecated="true"
 * 
 * We parse the XSD document and execute XPath to get those attributes and
 * a few other things:
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

public class XMLSchemaDocument {
    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(XMLSchemaDocument.class);
    
    private final URI docU;                         // file URI object for this schema document
    private final File docF;                        // schema document file
    private final File docFD;                       // parent directory of schema document
    private final Document doc;                     // parsed schema document
    private String targetNS = null;                 // target namespace URI
    private List<XMLNamespaceDeclaration> nsdecls = null;

    /**
     * Constructor for XSD document specified by a File
     * @param sdF - File object for XSD document
     */
    public XMLSchemaDocument (File sdF) throws ParserConfigurationException, SAXException, IOException {
        var db = ParserBootstrap.docBuilder();
        docU  = sdF.toURI();
        docF  = sdF;
        docFD = docF.getParentFile();
        doc   = db.parse(docF);
        initNSdecls();
    }
    
    /**
     * Returns a URI object for the schema document.
     * @return URI object
     */
    public URI docURI ()    { return docU; }
    
    /**
     * Returns a File object for the schema document.
     * @return File object
     */
    public File docFile ()  { return docF; }
    
    /**
     * Returns the target namespace of the schema document.
     * Returns empty string if no @targetNamespace attribute.
     * @return target namespace URI string
     */
    public String targetNamespace () {
        if (null != targetNS) return targetNS;
        targetNS = "";
        var xpe = "/*/@targetNamespace";
        try {
            targetNS = evalForString(xpe);
        } catch (XPathExpressionException ex) {
            LOG.error("bad XPath expression {}: {}", xpe, ex.getMessage());
        }
        return targetNS;        
    }
    
    /**
     * Returns the version attribute of the schema document.
     * Returns empty string if no @version attribute.
     * @return version
     */
    public String version () {
        String res = "";
        var xpe = "/*/@version";
        try {
            res = evalForString(xpe);
        } catch (XPathExpressionException ex) {
            LOG.error("bad XPath expression {}: {}", xpe, ex.getMessage());
        }
        return res;              
    }    
    
    /**
     * Returns the parsed document object model for the schema document.
     * @return Document object
     */
    public Document dom () {
        return doc;
    }
    
    /**
     * Evaluate XPath expression against schema document for a string.
     * @param exp - XPath expression
     * @return string result
     * @throws XPathExpressionException 
     */
    public String evalForString (String exp) throws XPathExpressionException {
        var xpf = XPathFactory.newInstance();
        var xp  = xpf.newXPath();
        var xpr = xp.compile(exp);
        var res = (String)xpr.evaluate(doc, XPathConstants.STRING);
        return res;        
    }
    
    /**
     * Evaluate XPath expression against schema document for a list of Nodes
     * @param exp - XPath expression
     * @return NodeList
     * @throws XPathExpressionException 
     */
    public NodeList evalForNodes (String exp) throws XPathExpressionException {
        var xpf = XPathFactory.newInstance();
        var xp  = xpf.newXPath();
        var xpr = xp.compile(exp);
        var res = (NodeList)xpr.evaluate(doc, XPathConstants.NODESET);
        return res;
    }
    
    private static final String importXpath = 
            "/*/*[namespace-uri()='" + W3C_XML_SCHEMA_NS_URI + "' and local-name()='import']";
    
    /**
     * Returns a list of top-level xs:import elements in the schema document.
     */
    public NodeList importElements () {
        try {
            return evalForNodes(importXpath);
        } catch (XPathExpressionException ex) {
            LOG.error("bad xpath {} for import elements: {}", importXpath, ex.getMessage());
            return null;
        }
    }
    
    /**
     * Given a schema document Element, return a list of LanguageString objects
     * created from the xs:annotation/xs:documentation children, each containing
     * the xs:documentation text plus the in-scope @xml:lang.
     * @return 
     */
    public static List<LanguageString> getDocumentation (Element e) {
        var res   = new ArrayList<LanguageString>();
        var alist = e.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "annotation");
        for (var i = 0; i < alist.getLength(); i++) {
            var ae    = (Element)alist.item(i);
            var dlist = ae.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "documentation");
            for (int j = 0; j < dlist.getLength(); j++) {
                var de = (Element)dlist.item(j);
                var text = de.getTextContent();
                var lang = getXMLLang(de);
                res.add(new LanguageString(text, lang));
            }
        }
        return res;
    }
    
    /**
     * Returns the in-scope value of @xml:lang for a schema document Element.
     * Returns empty string if no @xml:lang in scope.
     */
    public static String getXMLLang (Element e) {
        var lang = e.getAttributeNS(XML_NS_URI, "lang");      
        while (lang.isEmpty() && null != e.getParentNode()) {
            if (ELEMENT_NODE != e.getParentNode().getNodeType()) break;
            e = (Element)e.getParentNode();
            lang = e.getAttributeNS(XML_NS_URI, "lang");
        }
        return lang;
    }
    
    public static void dumpElement (Element e) {
        var ens  = e.getNamespaceURI();
        var eln  = e.getLocalName();
        var etxt = e.getTextContent();
        var atts = e.getAttributes();
        System.err.println(String.format("{%s}%s = %s\n", ens, eln, etxt));
        for (int i = 0; i < atts.getLength(); i++) {
            var attr = (Attr)atts.item(i);
            var ans  = attr.getNamespaceURI();
            var aln  = attr.getLocalName();
            var aval = attr.getValue();
            System.err.println(String.format("  @{%s}%s = %s\n", ans, aln, aval));
        }
    }
    
    /**
     * Return a list of all namespace declarations in schema document.
     * @return list of XMLNamespaceDeclaration objects
     */
    public List<XMLNamespaceDeclaration> nsdecls () {
        if (null == nsdecls) initNSdecls();
        return nsdecls;
    }
    
    // Parse the schema document to construct list of all namespace declarations.
    // Can't get these from the DOM.
    private void initNSdecls () {
        nsdecls = new ArrayList<>();
        try {
            SAXParser saxp = ParserBootstrap.sax2Parser();
            XSDHandler h = new XSDHandler(nsdecls);
            saxp.parse(docF, h);
            Collections.sort(nsdecls);
        } catch (ParserConfigurationException ex) {
            LOG.error("can't create SAX parser: {}", ex.getMessage());
        } catch (SAXException | IOException ex) {
            LOG.error("can't get namespace declarations from {}: {}", docURI().toString(), ex.getMessage());
        }
    }
    
    // SAX handler to construct a list of all namespace declarations found in 
    // the document.  Can't get these from DOM.
    private class XSDHandler extends DefaultHandler {
        private List<XMLNamespaceDeclaration> decls = null;
        private Locator docloc = null;
        private int depth = 0;
        
        XSDHandler (List<XMLNamespaceDeclaration> decL) { decls = decL; } 
        
        @Override
        public void startPrefixMapping (String prefix, String uri) {
            if (prefix.isEmpty()) return;
            int line = docloc.getLineNumber();
            var nsd = new XMLNamespaceDeclaration(prefix, uri, line, depth);
            decls.add(nsd);
        }
        
        @Override
        public void startElement (String ns, String ln, String qn, Attributes atts) {
            depth++;
        }
        
        @Override
        public void endElement (String ns, String ln, String qn) {
            depth--;
        }
        
        @Override
        public void setDocumentLocator (Locator loc) {
            docloc = loc;
        }
    }
}
