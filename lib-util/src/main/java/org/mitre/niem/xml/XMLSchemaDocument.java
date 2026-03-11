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
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
 * available through the Xerces XML Schema (XSModel) API.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

public class XMLSchemaDocument extends XMLDocument {
    static final Logger LOG = LogManager.getLogger(XMLSchemaDocument.class);
    
    private String targetNS = null;                 // document element @targetNamespace
    private String lang = null;                     // document element @xml:lang
    private String version = null;                  // document element @version
    private List<LanguageString> docL =  null;
    private List<XMLSchemaImport> importL = null;
    private List<XMLNamespaceDeclaration> nsdecls = null;

    /**
     * Constructor for XSD document specified by a File
     * @param sdF - File object for XSD document
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     */
    public XMLSchemaDocument (File sdF) throws ParserConfigurationException, SAXException, IOException {
        super(sdF);
//        var db = ParserBootstrap.docBuilder();
//        docURI   = sdF.toURI();
//        docF     = sdF;
////        dom      = db.parse(docF);
////        docE     = dom.getDocumentElement();
////        targetNS = docE.getAttribute("targetNamespace");
////        lang     = docE.getAttributeNS(XML_NS_URI, "lang");
////        version  = docE.getAttribute("version");
//        initNSdecls();
    }
    
    /**
     * Returns the language attribute (xml:lang) of the schema document.
     * Returns empty string on parsing error, or if language not declared.
     * @return version
     */
    public String language () { 
        if (null != lang) return lang;
        var docE = documentElement();
        if (null == docE) lang = "";
        else lang = docE.getAttribute("xml:lang");
        return lang;
    }

    /**
     * Returns the target namespace of the schema document.
     * Returns empty string on parsing error, or if no @targetNamespace attribute.
     * @return target namespace URI string
     */
    public String targetNamespace () { 
        if (null != targetNS) return targetNS;
        var docE = documentElement();
        if (null == docE) targetNS = "";
        else targetNS = docE.getAttribute("targetNamespace");
        return targetNS;
    }
    
    /**
     * Returns the version attribute of the schema document.
     * Returns empty string if no @version attribute.
     * @return version
     */
    public String version () { 
        if (null != version) return version;
        var docE = documentElement();
        if (null == docE) version = "";
        else version = docE.getAttribute("version");
        return version;
}  
    
    /**
     * Returns a list of LanguageString objects for the schema-level documentation
     * in this schema document.
     * @return list of documentation objects
     */
    public List<LanguageString> documentation () {
        if (null == docL) docL = getDocumentation(documentElement());
        return docL;
    }
    
    /**
     * Returns a list of top-level xs:import elements in the schema document.
     * @return list of xs:import element objects
     */
    public List<XMLSchemaImport> importElements () {
        if (null != importL) return importL;
        importL = new ArrayList<>();
        var docE = documentElement();
        if (null == docE) return importL;
        var nls = docE.getChildNodes();
        for (int i = 0; i < nls.getLength(); i++) {
            var node = nls.item(i);
            if (ELEMENT_NODE != node.getNodeType()) continue;
            if (!W3C_XML_SCHEMA_NS_URI.equals(node.getNamespaceURI())) continue;
            if (!"import".equals(node.getLocalName())) continue;
            var e = (Element)node;
            var nsU  = e.getAttribute("namespace");
            var sloc = e.getAttribute("schemaLocation");
            var docL = getDocumentation(e);
            var atts = e.getAttributes();
            var attL = new ArrayList<XMLAttribute>();
            for (var j = 0; j < atts.getLength(); j++) {
                var att   = (Attr)atts.item(j);
                var ansU  = att.getNamespaceURI();
                var attQ  = att.getName();
                var aname = qnToName(attQ);
                var aval  = att.getValue();
                if (null == ansU) continue;
                var arec = new XMLAttribute(ansU, aname, aval);
                attL.add(arec);
            }
            var imp  = new XMLSchemaImport(nsU, sloc, attL, docL);
            importL.add(imp);
        }
        return importL;
    }

    /**
     * Given a schema document Element, return a list of LanguageString objects
     * created from the xs:annotation/xs:documentation children, each containing
     * the xs:documentation text plus the in-scope @xml:lang.
     * @param e - XSD component element
     * @return list of documentation strings
     */
    public static List<LanguageString> getDocumentation (Element e) {
        var res   = new ArrayList<LanguageString>();
        var nodeL = e.getChildNodes();
        for (var i = 0; i < nodeL.getLength(); i++) {
            var node = nodeL.item(i);
            if (ELEMENT_NODE != node.getNodeType()) continue;
            var ce = (Element)node;
            if (W3C_XML_SCHEMA_NS_URI != ce.getNamespaceURI()) continue;
            if (!"annotation".equals(ce.getLocalName())) continue;
            var dlist = ce.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "documentation");
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
     * Returns a LanguageString object containing the text content of a schema 
     * document Element, plus the in-scope xml:lang attribute.
     * @param e
     * @return 
     */
    public static LanguageString getLanguageString (Element e) {
        return new LanguageString(e.getTextContent(), getXMLLang(e));
    }
    
    /**
     * Returns the in-scope value of @xml:lang for a schema document Element.
     * Returns "en-US" if no @xml:lang in scope.
     */
    public static String getXMLLang (Element e) {
        var lang = e.getAttributeNS(XML_NS_URI, "lang");      
        while (lang.isEmpty() && null != e.getParentNode()) {
            if (ELEMENT_NODE != e.getParentNode().getNodeType()) break;
            e = (Element)e.getParentNode();
            lang = e.getAttributeNS(XML_NS_URI, "lang");
        }
        if (lang.isEmpty()) return "en-US";
        return lang;
    }

    
//    // Parse the schema document to construct list of all namespace declarations.
//    // Can't get these from the DOM, must use SAX.
//    private void initNSdecls () {
//        nsdecls = new ArrayList<>();
//        try {
//            SAXParser saxp = ParserBootstrap.sax2Parser();
//            XSDHandler h = new XSDHandler(nsdecls);
//            saxp.parse(docF, h);
//            Collections.sort(nsdecls);
//        } catch (ParserConfigurationException ex) {
//            LOG.error("Can't create SAX parser: {}", ex.getMessage());
//        } catch (SAXException | IOException ex) {
//            LOG.error("Can't get namespace declarations from {}: {}", docURI().toString(), ex.getMessage());
//        }
//    }
//    
//    // SAX handler to construct a list of all namespace declarations found in 
//    // the document.  Can't get these from DOM.
//    private class XSDHandler extends DefaultHandler {
//        private List<XMLNamespaceDeclaration> decls = null;
//        private Locator docloc = null;
//        private int depth = 0;
//        
//        XSDHandler (List<XMLNamespaceDeclaration> decL) { decls = decL; } 
//        
//        @Override
//        public void startPrefixMapping (String prefix, String uri) {
//            if (prefix.isEmpty()) return;
//            int line = docloc.getLineNumber();
//            var nsd = new XMLNamespaceDeclaration(prefix, uri, line, depth);
//            decls.add(nsd);
//        }
//        
//        @Override
//        public void startElement (String ns, String ln, String qn, Attributes atts) {
//            if (0 == depth) {
//                targetNS = atts.getValue("targetNamespace");
//                lang     = atts.getValue("xml:lang");
//                version  = atts.getValue("version");
//                if (null == targetNS) targetNS = "";
//                if (null == lang) lang = "";
//                if (null == version) version = "";
//            }
//            depth++;
//        }
//        
//        @Override
//        public void endElement (String ns, String ln, String qn) {
//            depth--;
//        }
//        
//        @Override
//        public void setDocumentLocator (Locator loc) {
//            docloc = loc;
//        }
//    }

}
