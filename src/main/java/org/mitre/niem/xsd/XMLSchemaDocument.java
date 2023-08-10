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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.apache.logging.log4j.LogManager;
import org.javatuples.Pair;
import static org.mitre.niem.NIEMConstants.CONFORMANCE_ATTRIBUTE_NAME;
import org.mitre.niem.cmf.NamespaceKind;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_APPINFO;
import static org.mitre.niem.cmf.NamespaceKind.NIEM_CTAS;
import static org.mitre.niem.cmf.NamespaceKind.NSK_EXTENSION;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UNKNOWN;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A class for providing information from a schema document that is not
 * available through the Xerces XML Schema (XSModel) API. Specifically, you
 * can't get @appinfo attributes from XSD elements like
 *   xs:element ref="foo:bar" appinfo:deprecated="true"
 * 
 * So we have to parse the XSD document to get those.  We record appinfo attributes
 * on global type definitions and component declarations, and on element references
 * within global type definitions.
 * 
 * While we're at it, we collect these other things:<ul>
 *
 * <li> The target namespace attribute value</li>
 * <li> All namespace declarations encountered in the document </li>
 * <li> NIEM conformance target assertions </li>
 * <li> NIEM architecture (eg. NIEM5, NIEM6)</li>
 * <li> NIEM version (eg. 4, 5, 6)</li>
 * <li> xs:schema @version attribute </li></ul>
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLSchemaDocument {
    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(XMLSchemaDocument.class); 
    
    private final List<XMLNamespaceDeclaration> nsdels = new ArrayList<>();
    private final List<String> externalImports = new ArrayList<>();
    private final List<AppinfoAttribute> appinfo = new ArrayList<>();
    private String targetNS = null;
    private String confTargs = null;
    private String niemArch = null;
    private String niemVersion = null;
    private String schemaVersion = null;
    private String filepath = null;
    private String language = null;
    private int kind = NSK_UNKNOWN;
    
    public List<XMLNamespaceDeclaration> namespaceDecls ()  { return nsdels; }          // all namespace declarations in document
    public List<String> externalImports ()                  { return externalImports; } // external namespaces imported in document
    public List<AppinfoAttribute> appinfoAtts ()            { return appinfo; }         // appinfo attributes in document
    public String targetNamespace ()                        { return targetNS; }        // target namespace URI of document
    public String conformanceTargets ()                     { return confTargs; }       // value of @ct:conformanceTargets in <xs:schema>
    public String niemArch ()                               { return niemArch; }        // derived from conformance assertions or target NS
    public String niemVersion ()                            { return niemVersion; }     // derived from conformance assertions or target NS
    public String schemaVersion ()                          { return schemaVersion; }   // from @version in <xs:schema>
    public String filepath ()                               { return filepath; }        // path used to open document for parsing
    public String language ()                               { return language; }        // from @xml:lang in <xs:schema>
    public int schemaKind ()                                { return kind; }            // from namespace URI and conformance assertions
    
    public void setSchemaKind (int k) { kind = k; }
    

    public XMLSchemaDocument (String sdfuri, String sdpath) throws SAXException, ParserConfigurationException, IOException {
        SAXParser saxp = ParserBootstrap.sax2Parser();
        XSDHandler h = new XSDHandler();
        saxp.parse(sdfuri, h);
        for (var nsd : nsdels) {
            nsd.setTargetNS(targetNS);
            nsd.setTargetKind(kind);
        }
        filepath = sdpath;
      }
    
    private class XSDHandler extends DefaultHandler {  
        private final XMLNamespaceScope nsm = new XMLNamespaceScope();
        private Pair<String,String> ctypeEN = null;     // namespace and lname of complex type being parsed
        private Locator docloc = null;                  // for document line number in log messages
        private int depth = 0;                          // root element has depth = 0
        
        XSDHandler () { } 
        
        // Do two things with prefix maps.  Keep a list of all mappings in the document
        // for prefix normalization occuring later.  Also keep a stack of the current 
        // mappings to interpret QName in <xs:element ref="q:name" appinfo:foo="bar"/>
        @Override
        public void startPrefixMapping (String prefix, String uri) {
            if (prefix.isEmpty()) return;
            nsm.onStartPrefixMapping(prefix, uri);
            int line = docloc.getLineNumber();
            var nsd = new XMLNamespaceDeclaration(prefix, uri, line, depth, null, NSK_UNKNOWN);  // don't know target NS or kind yet
            nsdels.add(nsd);
        }        
        
        private static final Pattern xsComponentPat = Pattern.compile("(attribute)|(element)|(complexType)");
        
        @Override
        public void startElement (String ens, String elname, String eQN, Attributes atts) { 
            
            // Handle xs:schema element
            nsm.onStartElement();
            if ("schema".equals(elname)) {
                targetNS = atts.getValue("targetNamespace");
                String tns = targetNS;
                if (null == targetNS) LOG.warn("found no target namespace while parsing {}", docloc.getSystemId());
                
                // Get schema version attribute and language
                schemaVersion = atts.getValue("version");
                language = atts.getValue(XML_NS_URI, "lang");
                
                // Get schema kind and niem version (maybe)
                kind        = NamespaceKind.kind(targetNS);
                niemArch    = NamespaceKind.architecture(targetNS);
                niemVersion = NamespaceKind.version(targetNS);

                // Get conformance target assertion
                for (int i = 0; i < atts.getLength(); i++) {
                    var auri = atts.getURI(i);
                    int util = NamespaceKind.builtin(auri);
                    if (NIEM_CTAS == util) {
                        if (CONFORMANCE_ATTRIBUTE_NAME.equals(atts.getLocalName(i))) {                        
                            confTargs = atts.getValue(i);
                            if (niemArch.isBlank() || niemVersion.isBlank()) {
                                var ctlist = confTargs.split("\\s+");
                                for (int j = 0; j < ctlist.length; j++) {
                                    var ct = ctlist[j];
                                    if (niemArch.isBlank())    niemArch = NamespaceKind.archFromCTA(ct);
                                    if (niemVersion.isBlank()) niemVersion = NamespaceKind.versionFromCTA(ct);
                                    if (!niemArch.isBlank() && !niemVersion.isBlank()) break;
                                }                               
                            }
                        }
                    }
                }
                // Unknown namespace kind with conformance assertion is an extension namespace
                if (NSK_UNKNOWN == kind && null != confTargs && !confTargs.isEmpty()) {
                    kind = NSK_EXTENSION;
                    NamespaceKind.setKind(targetNS, kind);
                }    
            }
            // Handle xs:import elements
            else if ("import".equals(elname)) {
                String importNS = atts.getValue("namespace");
                for (int i = 0; i < atts.getLength(); i++) {
                    if (NIEM_APPINFO == NamespaceKind.builtin(atts.getURI(i)))
                        if ("externalImportIndicator".equals(atts.getLocalName(i))) {
                            if ("true".equals(atts.getValue(i))) externalImports.add(importNS);
                            break;
                        }   
                }
            }
            // Remember the QName of the global complex type we are defining
            else if (1 == depth && "complexType".equals(elname)) ctypeEN = Pair.with(targetNS, atts.getValue("name"));
                
            // Look for appinfo attributes on global definitions and declarations
            if (1 == depth && xsComponentPat.matcher(elname).lookingAt()) {   
                for (int i = 0; i < atts.getLength(); i++) {
                    if (NIEM_APPINFO == NamespaceKind.builtin(atts.getURI(i))) {
                        var een = Pair.with(targetNS, atts.getValue("name"));
                        appinfo.add(new AppinfoAttribute(atts.getLocalName(i), atts.getValue(i), een, null));
                    }
                } 
            }
            // Look for appinfo attributes on attribute and element references within complex type definition
            if ("element".equals(elname) || "attribute".equals(elname)) {
                String eref = atts.getValue("ref");
                Pair<String, String> een = nsm.expandQName(eref);
                if (null != ctypeEN && null != eref && null != een) {
                    for (int i = 0; i < atts.getLength(); i++) {
                        if (NIEM_APPINFO == NamespaceKind.builtin(atts.getURI(i))) {
                            AppinfoAttribute a = new AppinfoAttribute(atts.getLocalName(i), atts.getValue(i), ctypeEN, een);
                            appinfo.add(a);
                        }
                    }
                }
            }
            depth++;
        }
    
        @Override
        public void endElement (String ens, String elname, String eQN) {
            depth--;
            nsm.onEndElement();
            if (1 == depth && "complexType".equals(elname)) ctypeEN = null;    // finished global type defn
        }
        
        @Override
        public void setDocumentLocator (Locator l) {
            docloc = l;
        }
    }
    
}


