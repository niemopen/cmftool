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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.xml.sax.SAXException;

/**
 *
 * A class for bootstrapping the Xerces XML Schema parser and other javax
 * builders that can throw an exception when you initialize the factory object.
 * Useful if you want to bootstrap once, perhaps at the start of execution, 
 * and handle the possible exceptions then and there.  
 * 
 * All of the builders returned by this class are namespace aware.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

public class ParserBootstrap {
    public static final int BOOTSTRAP_XERCES_XS = 1;
    public static final int BOOTSTRAP_SAX2 = 2;
    public static final int BOOTSTRAP_DOCUMENTBUILDER = 4;
    public static final int BOOTSTRAP_TRANSFORMERFACTORY = 8;
    public static final int BOOTSTRAP_ALL = 15;
    
    private XSImplementation xsimpl = null;          // Xerces XSImplementation, for creating XSLoader object
    private SAXParserFactory sax2Fact = null;
    private DocumentBuilder db = null;
    private TransformerFactory tfact = null;
    
    private ParserBootstrap () { }
    
    private static class Holder {
        private static final ParserBootstrap instance = new ParserBootstrap();
    }
    
    /**
     * Initialize all the parser factories.  Let's find the exceptions now!
     * @throws ParserConfigurationException 
     */
    public static void init () throws ParserConfigurationException {
        init(BOOTSTRAP_ALL);
    }
    
    /**
     * Initialize a particular parser factory.
     * @param which
     * @throws ParserConfigurationException 
     */
    public static void init (int which) throws ParserConfigurationException {
        System.setProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl");
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");            
        System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
        
        if (0 != (which & BOOTSTRAP_XERCES_XS) && null == Holder.instance.xsimpl) {
            DOMImplementationRegistry direg;
            try {
                direg = DOMImplementationRegistry.newInstance();
                Holder.instance.xsimpl = (XSImplementation) direg.getDOMImplementation("XS-Loader");
            } catch (ClassCastException | ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
                throw new ParserConfigurationException("Can't initializte Xerces XML Schema parser implementation: " + ex.getMessage());
            }
        }
        if (0 != (which & BOOTSTRAP_SAX2) && null == Holder.instance.sax2Fact) {
            try {
                Holder.instance.sax2Fact = SAXParserFactory.newInstance();
                Holder.instance.sax2Fact.setNamespaceAware(true);
                Holder.instance.sax2Fact.setValidating(false);
                SAXParser saxp = Holder.instance.sax2Fact.newSAXParser();
            } catch (ParserConfigurationException | SAXException ex) {
                throw new ParserConfigurationException("Can't initialize SAX2 parser: " + ex.getMessage());
            }
        }       
        if (0 != (which & BOOTSTRAP_DOCUMENTBUILDER) && null == Holder.instance.db) {
            var dbf = docBuilderFactory();
            try {
                Holder.instance.db = dbf.newDocumentBuilder();
            }
            catch (ParserConfigurationException ex) {
                throw new ParserConfigurationException("Can't initialize DocumentBuilder: " + ex.getMessage());
            }
        }
        if (0 != (which & BOOTSTRAP_TRANSFORMERFACTORY) || null == Holder.instance.tfact) {           
            Holder.instance.tfact = TransformerFactory.newInstance();
        }
    }
    
    /**
     * Returns a new XSLoader object.Don't reuse these if you need to control
     * where the schema documents come from -- the loader object remembers and
     * happily reuses any document it has already seen.
     * @return namespace aware XSLoader object
     * @throws ParserConfigurationException
     */
    public static XSLoader xsLoader () throws ParserConfigurationException {
        init(BOOTSTRAP_XERCES_XS);
        return Holder.instance.xsimpl.createXSLoader(null);
    }
    
    /**
     * Returns a SAXParser object.OK to reuse these after a reset() call.
     * @return namespace aware SAXParser object
     * @throws org.xml.sax.SAXException
     * @throws ParserConfigurationException
     */
    public static SAXParser sax2Parser () throws ParserConfigurationException, SAXException {
        init(BOOTSTRAP_SAX2);
        return Holder.instance.sax2Fact.newSAXParser();
    }
    
    /**
     * Returns a DocumentBuilder object. OK to reuse these after a reset() call.
     * @return namespace aware DocumentBuilder object
     * @throws ParserConfigurationException
     */
    public static DocumentBuilder docBuilder () throws ParserConfigurationException {
        init(BOOTSTRAP_DOCUMENTBUILDER);
        Holder.instance.db.reset();
        return Holder.instance.db;
    }
    
    public static TransformerFactory transFactory () {
        try {
            init(BOOTSTRAP_TRANSFORMERFACTORY);
        } catch (ParserConfigurationException ex) { } // CAN'T HAPPEN
        return Holder.instance.tfact;
    }
    
    /**
     * Returns a DocumentBuilderFactory configured to produce a DocumentBuilder that
     * ignores entities and DTDs. Call this if you need to set the XSD schema for a parser.
     * @return DocumentBuilderFactory
     * @throws ParserConfigurationException 
     */
    public static DocumentBuilderFactory docBuilderFactory () throws ParserConfigurationException {
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setNamespaceAware(true);   
        return dbf;
    }

}