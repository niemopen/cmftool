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
package org.mitre.niem.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.impl.xs.XSImplementationImpl;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * A class for bootstrapping the Xerces XML Schema parser and other javax
 * builders that can throw an exception when you initialize the factory object.
 * Useful if you want to bootstrap once, perhaps at the start of execution, 
 * and handle the possible exceptions then and there.
 *
 * <p>The following are provided:
 * <ul>
 * <li>Xerces {@link XSLoader}</li>
 * <li>Xerces {@link SAXParserFactory}</li>
 * <li>Xerces {@link DocumentBuilderFactory}</li>
 * <li>a configured JAXP {@link TransformerFactory}</li>
 * </ul>
 *
 * <p>Factories are cached. Parsers and builders are created fresh for each call.
 */
public final class ParserBootstrap {

    private static final Logger LOG = LogManager.getLogger(ParserBootstrap.class);

    public static final int BOOTSTRAP_XERCES_XS = 1;
    public static final int BOOTSTRAP_SAX2 = 2;
    public static final int BOOTSTRAP_DOCUMENTBUILDER = 4;
    public static final int BOOTSTRAP_TRANSFORMERFACTORY = 8;
    public static final int BOOTSTRAP_ALL = 15;

    private static final ParserBootstrap INSTANCE = new ParserBootstrap();

    private XSImplementation xsImpl;
    private SAXParserFactory saxFactory;
    private DocumentBuilderFactory docBuilderFactory;
    private TransformerFactory transformerFactory;

    private ParserBootstrap() {}

    /**
     * Initializes all configured factories.
     *
     * @throws ParserConfigurationException if any parser factory cannot be initialized
     */
    public static void init() throws ParserConfigurationException {
        init(BOOTSTRAP_ALL);
    }

    /**
     * Initializes selected factories.
     *
     * @param which bitmask of BOOTSTRAP_* constants
     * @throws ParserConfigurationException if a selected factory cannot be initialized
     */
    public static void init(int which) throws ParserConfigurationException {
        if (0 != (which & BOOTSTRAP_XERCES_XS)) {
            INSTANCE.ensureXsImplementation();
        }
        if (0 != (which & BOOTSTRAP_SAX2)) {
            INSTANCE.ensureSaxFactory();
        }
        if (0 != (which & BOOTSTRAP_DOCUMENTBUILDER)) {
            INSTANCE.ensureDocumentBuilderFactory();
        }
        if (0 != (which & BOOTSTRAP_TRANSFORMERFACTORY)) {
            INSTANCE.ensureTransformerFactory();
        }
    }

    /**
     * Returns a new Xerces XSLoader.
     *
     * <p>Do not reuse XSLoader instances when document origin matters; loaders may
     * cache schemas they have already seen.
     *
     * @return new Xerces XSLoader
     * @throws ParserConfigurationException if Xerces XS support cannot be initialized
     */
    public static XSLoader xsLoader() throws ParserConfigurationException {
        return INSTANCE.ensureXsImplementation().createXSLoader(null);
    }

    /**
     * Returns a new namespace-aware Xerces SAXParser.
     *
     * @return new SAXParser
     * @throws ParserConfigurationException if the parser cannot be configured
     * @throws SAXException if the parser cannot be created
     */
    public static SAXParser sax2Parser() throws ParserConfigurationException, SAXException {
        return INSTANCE.ensureSaxFactory().newSAXParser();
    }

    /**
     * Returns a new namespace-aware Xerces DocumentBuilder.
     *
     * @return new DocumentBuilder
     * @throws ParserConfigurationException if the builder cannot be configured
     */
    public static DocumentBuilder docBuilder() throws ParserConfigurationException {
        return INSTANCE.ensureDocumentBuilderFactory().newDocumentBuilder();
    }

    /**
     * Returns a configured TransformerFactory.
     *
     * <p>This factory is not a Xerces implementation, because Xerces does not
     * provide JAXP XSLT transformation. The platform-selected JAXP implementation
     * is returned and configured for secure processing where supported.
     *
     * @return configured TransformerFactory
     */
    public static TransformerFactory transFactory() {
        try {
            return INSTANCE.ensureTransformerFactory();
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException("Can't initialize TransformerFactory", ex);
        }
    }

    /**
     * Returns a new Xerces DocumentBuilderFactory configured for secure XML parsing.
     *
     * <p>Call this if you need to set a schema or other options before creating a
     * builder.
     *
     * @return configured Xerces DocumentBuilderFactory
     * @throws ParserConfigurationException if the factory cannot be configured
     */
    public static DocumentBuilderFactory docBuilderFactory() throws ParserConfigurationException {
        var dbf = new DocumentBuilderFactoryImpl();
        configureDocumentBuilderFactory(dbf);
        return dbf;
    }

    /**
     * Returns a new Xerces SAXParserFactory configured for secure XML parsing.
     *
     * @return configured Xerces SAXParserFactory
     * @throws ParserConfigurationException if the factory cannot be configured
     */
    public static SAXParserFactory sax2Factory() throws ParserConfigurationException {
        var spf = new SAXParserFactoryImpl();
        configureSaxParserFactory(spf);
        return spf;
    }

    private synchronized XSImplementation ensureXsImplementation() throws ParserConfigurationException {
        if (xsImpl == null) {
            try {
                xsImpl = (XSImplementation) XSImplementationImpl.getDOMImplementation();
                if (xsImpl == null) {
                    throw new ParserConfigurationException("Xerces XSImplementation is unavailable");
                }
            } catch (RuntimeException ex) {
                var pce = new ParserConfigurationException(
                    "Can't initialize Xerces XML Schema parser implementation: " + ex.getMessage());
                pce.initCause(ex);
                throw pce;
            }
        }
        return xsImpl;
    }

    private synchronized SAXParserFactory ensureSaxFactory() throws ParserConfigurationException {
        if (saxFactory == null) {
            saxFactory = sax2Factory();
            try {
                saxFactory.newSAXParser();
            } catch (SAXException ex) {
                var pce = new ParserConfigurationException(
                    "Can't initialize SAX2 parser: " + ex.getMessage());
                pce.initCause(ex);
                throw pce;
            }
        }
        return saxFactory;
    }

    private synchronized DocumentBuilderFactory ensureDocumentBuilderFactory()
        throws ParserConfigurationException {
        if (docBuilderFactory == null) {
            docBuilderFactory = docBuilderFactory();
            docBuilderFactory.newDocumentBuilder();
        }
        return docBuilderFactory;
    }

    private synchronized TransformerFactory ensureTransformerFactory()
        throws ParserConfigurationException {
        if (transformerFactory == null) {
            try {
                transformerFactory = TransformerFactory.newInstance();
                configureTransformerFactory(transformerFactory);
            } catch (TransformerConfigurationException | IllegalArgumentException ex) {
                var pce = new ParserConfigurationException(
                    "Can't initialize TransformerFactory: " + ex.getMessage());
                pce.initCause(ex);
                throw pce;
            }
        }
        return transformerFactory;
    }

    private static void configureDocumentBuilderFactory(DocumentBuilderFactory dbf)
        throws ParserConfigurationException {
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    }

    private static void configureSaxParserFactory(SAXParserFactory spf)
        throws ParserConfigurationException {
        try {
            spf.setNamespaceAware(true);
            spf.setValidating(false);
            
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (SAXNotRecognizedException ex) {
            throw new ParserConfigurationException(ex.getMessage());
        } catch (SAXNotSupportedException ex) {
            throw new ParserConfigurationException(ex.getMessage());
        }
    }

    private static void configureTransformerFactory(TransformerFactory tf)
        throws TransformerConfigurationException {
        tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        try {
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        } catch (IllegalArgumentException ex) {
            LOG.debug("TransformerFactory does not support ACCESS_EXTERNAL_DTD: {}", ex.getMessage());
        }

        try {
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException ex) {
            LOG.debug("TransformerFactory does not support ACCESS_EXTERNAL_STYLESHEET: {}", ex.getMessage());
        }
    }
}
