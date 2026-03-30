/*
 * NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * Copyright 2020-2028 The MITRE Corporation.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.XMLConstants;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.w3c.dom.Node.ELEMENT_NODE;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A utility class for working with XML documents.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLDocument {

    static final Logger LOG = LogManager.getLogger(XMLDocument.class);
    
    private static NamespaceContext nsContext;
    private static XPath xp;
    
    private final URI docURI;                       // file URI object for this schema document
    private final File docF;                        // schema document file
    private Document doc = null;
    private List<XMLNamespaceDeclaration> nsdecls = null;
    
    static {
        nsContext = new NamespaceContext() {
            @Override public String getNamespaceURI(String prefix) {
                switch(prefix) {
                    case XML_NS_PREFIX:     return XML_NS_URI;
                    case XMLNS_ATTRIBUTE:   return XMLNS_ATTRIBUTE_NS_URI;
                    case "xs":              return W3C_XML_SCHEMA_NS_URI;
                    case "xsi":             return "http://www.w3.org/2001/XMLSchema-instance";
                    default:                return XMLConstants.NULL_NS_URI;
                }
            }
            @Override public String getPrefix(String namespaceURI) { return null; }
            @Override public Iterator<String> getPrefixes(String namespaceURI) { return null; }            
        };
        xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(nsContext);
    }
    
    public XMLDocument (File f) throws ParserConfigurationException, SAXException, IOException {
        docF = f;
        docURI = docF.toURI();
    }
    
    /**
     * Returns a URI object for the schema document.
     * @return URI object
     */
    public URI docURI ()    { return docURI; }
    
    /**
     * Returns a File object for the schema document.
     * @return File object
     */
    public File docFile ()  { return docF; }
    
    /**
     * Returns the document object model for the schema document.
     * Returns null if parsing fails.
     * @return Document
     */
    public Document dom ()  { 
        if (null == doc) {
            try {
                var db = ParserBootstrap.docBuilder();
                doc = db.parse(docF);
            } catch (ParserConfigurationException ex) {
                LOG.error("Parser configuration error: {}", ex.getMessage());
            } catch (SAXException ex) {
                LOG.error("Error parsing {}: {}", docF.getName(), ex.getMessage());
            } catch (IOException ex) {
                LOG.error("I/O error on {}: {}", docF.getName(), ex.getMessage());
            }
        }
        return doc; 
    }
    
    /**
     * Returns the document element of the schema document.
     * Returns null if the schema document can't be parsed.
     * @return document element
     */    
    public Element documentElement ()  { return dom().getDocumentElement(); }
    
    private record Frame (Element el, int depth) {}
    
    /**
     * Return a list of all namespace declarations in schema document.
     * @return list of XMLNamespaceDeclaration objects
     */
    public List<XMLNamespaceDeclaration> namespaceDeclarations () {
        if (null != nsdecls) return nsdecls;
        var todo = new ArrayDeque<Frame>();
        nsdecls  = new ArrayList<>();
        todo.push(new Frame(documentElement(), 0));
        
        while (!todo.isEmpty()) {
            var fr  = todo.pop();
            var el  = fr.el();
            var dep = fr.depth();
            var ats = el.getAttributes();
            for (int i = 0; i < ats.getLength(); i++) {
                var n = ats.item(i);
                if (XMLNS_ATTRIBUTE_NS_URI.equals(n.getNamespaceURI())) {
                    var pre = XMLNS_ATTRIBUTE.equals(n.getNodeName())? "" : n.getLocalName();
                    var uri = n.getNodeValue();
                    nsdecls.add(new XMLNamespaceDeclaration(pre, uri, 0, dep));
                }
            }
            var chs = el.getChildNodes();
            for (int i = chs.getLength()-1; i >= 0; i--) {
                var n = chs.item(i);
                if (ELEMENT_NODE == n.getNodeType())
                    todo.add(new Frame((Element)n, dep+1));
            }
        }
        return nsdecls;
    }
        
    public static boolean evalForBoolean (Element e, String xpath) {
        xp.reset();
        try {
            var xpe = xp.compile(xpath);
            return (Boolean) xp.evaluate(xpath, e, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException ex) {
            LOG.error("Invalid XPath expression {}: {}", xpath, ex.getMessage());
            return false;
        }
    }
    
    /**
     * Evaluate XPath expression against a schema document element to return a string.
     * Returns empty string for invalid XPath.
     * @param e - schema document element
     * @param exp - XPath expression
     * @return string result
     */
    public static String evalForString (Element e, String exp) {
        try {
            var xpr = xp.compile(exp);
            return evalForString(e, xpr);
        } catch (XPathExpressionException ex) { 
            LOG.error("Invalid XPath expression {}: {}", exp, ex.getMessage());
        }
        return "";        
    }
    
    /**
     * Evaluate a compiled XPathExpression against a schema document element to
     * return a string.
     * @param e - schema document element
     * @param xpr - compiled XPathExpression
     * @return string result
     */
    public static String evalForString (Element e, XPathExpression xpr) {
        try {
            var res = (String)xpr.evaluate(e, XPathConstants.STRING);
            return res;
        } catch (XPathExpressionException ex) {
           LOG.error("Invalid XPath expression {}: {}", xpr.toString(), ex.getMessage());
        }
        return "";
    }
    
    /**
     * Evaluate XPath expression against a schema document element to return a 
     * list of Nodes.
     * Returns null for invalid XPath.
     * @param e - schema document element
     * @param exp - XPath expression
     * @return NodeList object or null
     */
    public static NodeList evalForNodes (Element e, String exp) {
        try {
            var xpr = xp.compile(exp);
            return evalForNodes(e, xpr);
        } catch (XPathExpressionException ex) {
             LOG.error("Invalid XPath expression {}: {}", exp, ex.getMessage());
        }
        return null;
    }
    
    /**
     * Evaluate a compiled XPathExpression against a schema document element 
     * to return a list of Nodes.
     * Returns null for invalid XPath.
     * @param e - schema document element
     * @param xpr - compiled XPathExpression
     * @return NodeList object or null
     */    
    public static NodeList evalForNodes (Element e, XPathExpression xpr) {
        try {
            var res = (NodeList)xpr.evaluate(e, XPathConstants.NODESET);
            for (int i = 0; i < res.getLength(); i++) {
                var n = res.item(i);
                if (ELEMENT_NODE != n.getNodeType()) continue;
            }
            return res;
        } catch (XPathExpressionException ex) {
            LOG.error("Invalid XPath expression {}: {}", xpr.toString(), ex.getMessage());            
        }
       return null;
    }
    
    /**
     * Returns the local name portion of a QName
     * Returns the input string if no prefix.
     * @param qn
     * @return 
     */
    public static String qnToName (String qn) {
        var indx = qn.indexOf(":");
        if (indx < 1 || indx >= qn.length()-1) return qn;        
        return qn.substring(indx+1);
    }   
    
    /**
     * Returns the prefix portion of a QName.
     * Returns the empty string if no ":" in the input string.
     * @param qn
     * @return 
     */
    public static String qnToPrefix (String qn) {
        var indx = qn.indexOf(":");
        if (indx < 1 || indx >= qn.length()-1) return "";
        return qn.substring(0, indx);        
    }
    
    /**
     * Creates a QName from a prefix and local name.
     * @param prefix
     * @param name
     * @return 
     */
    public static String  makeQN (String prefix, String name) {
        return prefix + ":" + name;
    }
        
    /**
     * Constructs a component URI from a namespace URI and local name.
     * Prefers slash URIs, respects hash URIs and URNs.
     * @param nsU
     * @param lname
     * @return 
     */
    public static String makeURI (String nsU, String lname) {
        if (nsU.startsWith("urn:")) return nsU + ":" + lname;   // urn:some:NS:lname
        if (nsU.endsWith("/"))      return nsU + lname;         // http://someNS/lname
        if (nsU.endsWith("#"))      return nsU + lname;         // http://someNS#lname
        return nsU + "/" + lname;
    }    
    
    /**
     * Reads an XML document to obtain the namespace URI, of the document element.
     * Returns the empty string if the document element does not have a namespace.
     * or if the file is not an XML document.
     * @param path - file name of XML document
     * @return namespace URI or empty string
     * @throws IOException 
     */
    public static String getXMLDocumentElementNamespace (String path) throws IOException {
        String ns = null;
        try {
            FileInputStream is = new FileInputStream(path);
            XMLEventReader er = XMLInputFactory.newFactory().createXMLEventReader(is);
            while (null == ns && er.hasNext()) {
                XMLEvent e = er.nextEvent();
                if (e.isStartElement()) {
                    StartElement se = e.asStartElement();
                    QName qn = se.getName();
                    ns = qn.getNamespaceURI();
                }
            }
            is.close();
        } catch (XMLStreamException ex) {
            LOG.warn("parse error at {} line {}:  {}", path, ex.getLocation().getLineNumber(), ex.getMessage());
        }
        if (null == ns) return "";
        return ns;
    }
    
    /**
     * Reads an XML document to obtain the namespace URI, of the document element.
     * Returns the empty string if the document element does not have a namespace.
     * or if the file is not an XML document.
     * @param xmlF - File object for XML document
     * @return namespace URI or empty string
     * @throws IOException 
     */
    public static String getXMLDocumentElementNamespace (File xmlF) throws IOException {
        return getXMLDocumentElementNamespace(xmlF.getPath());
    }
    
    /**
     * Reads an XML document to obtain @targetNamespace from an XSD document element.
     * Returns the empty string if the document is not XSD, or the document element 
     * does not have @targetNamespace.
     * @param path - file name of XML document
     * @return @targetNamespace or empty string
     * @throws IOException 
     */
    public static String getXSDTargetNamespace (String path) throws IOException {
        String tns = null;
        try {
            FileInputStream is = new FileInputStream(path);
            XMLEventReader er = XMLInputFactory.newFactory().createXMLEventReader(is);
            while (null == tns && er.hasNext()) {
                XMLEvent e = er.nextEvent();
                if (e.isStartElement()) {
                    var se   = e.asStartElement();
                    var seqn = se.getName();
                    if (!W3C_XML_SCHEMA_NS_URI.equals(seqn.getNamespaceURI())
                            || !"schema".equals(seqn.getLocalPart())) return "";
                    var tnsa = new QName("targetNamespace");
                    var a    = se.getAttributeByName(tnsa);
                    tns = a.getValue();
                }
            }
            is.close();
        } catch (XMLStreamException ex) {
            LOG.warn("parse error at {} line {}:  {}", path, ex.getLocation().getLineNumber(), ex.getMessage());
        }
        if (null == tns) return "";
        return tns;
    } 
    
    /**
     * Reads an XML document to obtain @targetNamespace from an XSD document element.
     * Returns the empty string if the document is not XSD, or the document element 
     * does not have @targetNamespace.
     * @param xsdF - File object for XML document
     * @return @targetNamespace or empty string
     * @throws IOException 
     */
    public static String getXSDTargetNamespace (File xsdF) throws IOException {
        return getXSDTargetNamespace(xsdF.getPath());
    }
        
}
