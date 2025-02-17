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
import java.io.FileInputStream;
import java.io.IOException;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A utility class for working with XML documents.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLDocument {

    static final Logger LOG = LogManager.getLogger(XMLDocument.class);
    
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
