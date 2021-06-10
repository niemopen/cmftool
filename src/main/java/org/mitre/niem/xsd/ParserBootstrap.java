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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.xml.sax.SAXException;

/**
 *
 * A class for bootstrapping the Xerces XML Schema parser and the
 * default SAX parser.  Useful if you want to bootstrap once, perhaps
 * at the start of execution, and handle the possible exceptions then
 * and there.
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */

public class ParserBootstrap {
    public static final int BOOTSTRAP_XERCES_XS = 1;
    public static final int BOOTSTRAP_SAX2 = 2;
    public static final int BOOTSTRAP_ALL = 3;
    
    private SAXParserFactory sfact = null;
    private XSImplementation xsimpl = null;          // Xerces XSImplementation, for creating XSLoader object
    
    private static class Holder {
        private static final ParserBootstrap instance = new ParserBootstrap();
    }
    
    public static void init () throws ParserConfigurationException {
        init(BOOTSTRAP_ALL);
    }
    
    public synchronized static void init (int which) throws ParserConfigurationException {
 
        if (0 != (which | BOOTSTRAP_SAX2) && Holder.instance.sfact == null) {
            try {
                Holder.instance.sfact = SAXParserFactory.newInstance();
                Holder.instance.sfact.setNamespaceAware(true);
                Holder.instance.sfact.setValidating(false);
                SAXParser saxp = Holder.instance.sfact.newSAXParser();
            } catch (ParserConfigurationException | SAXException ex) {
                throw (new ParserConfigurationException("Can't initialize suitable SAX2 parser" + ex.getMessage()));
            }
        }
        if (0 != (which | BOOTSTRAP_XERCES_XS) && Holder.instance.xsimpl == null) {
            System.setProperty(DOMImplementationRegistry.PROPERTY, "org.apache.xerces.dom.DOMXSImplementationSourceImpl");
            DOMImplementationRegistry direg;
            try {
                direg = DOMImplementationRegistry.newInstance();
                Holder.instance.xsimpl = (XSImplementation) direg.getDOMImplementation("XS-Loader");
            } catch (ClassCastException | ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
                throw (new ParserConfigurationException("Can't initializte Xerces XML Schema parser implementation" + ex.getMessage()));
            }
        }
    }
    
    /**
     * Returns a SAXParser object.  OK to reuse these.
     */
    public static SAXParser sax2Parser () throws ParserConfigurationException, SAXException {
        init(BOOTSTRAP_SAX2);
        return Holder.instance.sfact.newSAXParser();
    }
    
    /**
     * Returns a new XSLoader object.  Don't reuse these if you need to control
     * where the schema documents come from -- the loader object remembers and
     * happily reuses any document it has already seen.
     */
    public static XSLoader xsLoader () throws ParserConfigurationException {
        init(BOOTSTRAP_XERCES_XS);
        return Holder.instance.xsimpl.createXSLoader(null);
    }
}