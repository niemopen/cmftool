/*
 * NOTICE
 * 
 * This software was produced for the U. S. Government
 * under Basic Contract No. W56KGU-18-D-0004, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 * 
 * Copyright 2020-2022 The MITRE Corporation.
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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import static org.w3c.dom.Node.ELEMENT_NODE;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class CanonicalXSD {

    public static void canonicalize(InputStream is, OutputStream os) throws ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, TransformerException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document idom = db.parse(is);
        Document odom = db.newDocument();

        Element iroot = idom.getDocumentElement();
        String ns = iroot.getNamespaceURI();
        Element oroot = odom.createElementNS(iroot.getNamespaceURI(), iroot.getTagName());
        odom.appendChild(oroot);

        NamedNodeMap atts = iroot.getAttributes();
        for (int i = 0; i < atts.getLength(); i++) {
            Attr a = (Attr) atts.item(i);
            oroot.setAttributeNS(a.getNamespaceURI(), a.getName(), a.getValue());
        }

        List<Element> imports      = new ArrayList<>();
        List<Element> definitions  = new ArrayList<>();
        List<Element> attributes   = new ArrayList<>();
        List<Element> elements     = new ArrayList<>();
        
        NodeList icl = iroot.getChildNodes();
        for (int i = 0; i < icl.getLength(); i++) {
            if (ELEMENT_NODE != icl.item(i).getNodeType()) continue;
            Element e = (Element)odom.importNode(icl.item(i), true);
            String kind = e.getLocalName();
            switch (kind) {
                case "annotation":
                    oroot.appendChild(e);
                    break;
                case "import":
                    imports.add(e);
                    break;
                case "complexType":
                case "simpleType":
                    definitions.add(e);
                    break;
                case "element":
                    elements.add(e);
                    break;
                case "attribute":
                case "attributeGroup":
                    attributes.add(e);
                    break;
            }
        }
        // Write xs:import elements in namespace order
        Comparator<Element> namespaceCmp = new Comparator<Element> () {
            @Override
            public int compare (Element one, Element two) {
                String ns1 = one.getAttribute("namespace");
                String ns2 = two.getAttribute("namespace");
                return ns1.compareTo(ns2);
            }
         };
        Collections.sort(imports, namespaceCmp);
        for (Element e : imports) oroot.appendChild(e);
        
        Comparator<Element> nameCmp = new Comparator<Element> () {
            @Override
            public int compare (Element one, Element two) {
                String ns1 = one.getAttribute("name");
                String ns2 = two.getAttribute("name");
                return ns1.compareTo(ns2);
            }            
        };
        System.out.println("before sort");
        for (var e : elements) {
            var name = e.getAttribute("name");
            System.out.println("name="+name);
        }
        Collections.sort(definitions, nameCmp);
        Collections.sort(attributes, nameCmp);
        Collections.sort(elements, nameCmp);
        System.out.println("after sort");
      
        for (var e : definitions) oroot.appendChild(e);
        for (var e : attributes)  oroot.appendChild(e);
        for (var e : elements)    oroot.appendChild(e);

        var xsdw = new XSDWriter(odom, os);
        xsdw.writeXML();
    }

}
