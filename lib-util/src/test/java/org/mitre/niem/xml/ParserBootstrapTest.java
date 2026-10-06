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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class ParserBootstrapTest {

    private static final String SIMPLE_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <root xmlns="urn:test">
          <child/>
        </root>
        """;

    private static final String XML_WITH_DOCTYPE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE root [
          <!ELEMENT root ANY>
        ]>
        <root/>
        """;

    @Test
    void initAllDoesNotThrow() {
        assertDoesNotThrow(() -> ParserBootstrap.init());
    }

    @Test
    void initSelectedDoesNotThrow() {
        assertDoesNotThrow(() -> ParserBootstrap.init(ParserBootstrap.BOOTSTRAP_XERCES_XS));
        assertDoesNotThrow(() -> ParserBootstrap.init(ParserBootstrap.BOOTSTRAP_SAX2));
        assertDoesNotThrow(() -> ParserBootstrap.init(ParserBootstrap.BOOTSTRAP_DOCUMENTBUILDER));
        assertDoesNotThrow(() -> ParserBootstrap.init(ParserBootstrap.BOOTSTRAP_TRANSFORMERFACTORY));
    }

    @Test
    void docBuilderFactoryReturnsXercesImplementation() throws Exception {
        DocumentBuilderFactory dbf = ParserBootstrap.docBuilderFactory();

        assertNotNull(dbf);
        assertTrue(dbf instanceof DocumentBuilderFactoryImpl);
        assertTrue(dbf.isNamespaceAware());
        assertFalse(dbf.isValidating());
    }

    @Test
    void sax2FactoryReturnsXercesImplementation() throws Exception {
        SAXParserFactory spf = ParserBootstrap.sax2Factory();

        assertNotNull(spf);
        assertTrue(spf instanceof SAXParserFactoryImpl);
        assertTrue(spf.isNamespaceAware());
        assertFalse(spf.isValidating());
    }

    @Test
    void docBuilderReturnsFreshBuilders() throws Exception {
        var db1 = ParserBootstrap.docBuilder();
        var db2 = ParserBootstrap.docBuilder();

        assertNotNull(db1);
        assertNotNull(db2);
        assertNotSame(db1, db2);
        assertTrue(db1.getClass().getName().startsWith("org.apache.xerces."));
        assertTrue(db2.getClass().getName().startsWith("org.apache.xerces."));
    }

    @Test
    void sax2ParserReturnsFreshParsers() throws Exception {
        var p1 = ParserBootstrap.sax2Parser();
        var p2 = ParserBootstrap.sax2Parser();

        assertNotNull(p1);
        assertNotNull(p2);
        assertNotSame(p1, p2);
        assertTrue(p1.getClass().getName().startsWith("org.apache.xerces."));
        assertTrue(p2.getClass().getName().startsWith("org.apache.xerces."));
    }

    @Test
    void xsLoaderReturnsXercesImplementation() throws Exception {
        var loader1 = ParserBootstrap.xsLoader();
        var loader2 = ParserBootstrap.xsLoader();

        assertNotNull(loader1);
        assertNotNull(loader2);
        assertNotSame(loader1, loader2);
        assertTrue(loader1.getClass().getName().startsWith("org.apache.xerces."));
    }

    @Test
    void transFactoryReturnsCachedFactory() {
        TransformerFactory tf1 = ParserBootstrap.transFactory();
        TransformerFactory tf2 = ParserBootstrap.transFactory();

        assertNotNull(tf1);
        assertNotNull(tf2);
        assertSame(tf1, tf2);
    }

    @Test
    void docBuilderParsesSimpleXml() throws Exception {
        var db = ParserBootstrap.docBuilder();
        var doc = db.parse(new InputSource(new StringReader(SIMPLE_XML)));

        assertNotNull(doc);
        assertNotNull(doc.getDocumentElement());
        assertTrue("root".equals(doc.getDocumentElement().getLocalName()));
    }

    @Test
    void sax2ParserParsesSimpleXml() throws Exception {
        var parser = ParserBootstrap.sax2Parser();

        assertDoesNotThrow(() ->
            parser.parse(new InputSource(new StringReader(SIMPLE_XML)), new DefaultHandler())
        );
    }

    @Test
    void docBuilderRejectsDoctype() throws Exception {
        var db = ParserBootstrap.docBuilder();

        assertThrows(SAXException.class,
            () -> db.parse(new InputSource(new StringReader(XML_WITH_DOCTYPE))));
    }

    @Test
    void sax2ParserRejectsDoctype() throws Exception {
        var parser = ParserBootstrap.sax2Parser();

        assertThrows(SAXException.class,
            () -> parser.parse(new InputSource(new StringReader(XML_WITH_DOCTYPE)), new DefaultHandler()));
    }
}
