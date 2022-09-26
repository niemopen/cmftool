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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.apache.logging.log4j.LogManager;
import org.javatuples.Pair;
import static org.mitre.niem.NIEMConstants.APPINFO_NS_URI_PREFIX;
import static org.mitre.niem.NIEMConstants.CONFORMANCE_ATTRIBUTE_NAME;
import static org.mitre.niem.NIEMConstants.CONFORMANCE_TARGET_NS_URI_PREFIX;
import static org.mitre.niem.NIEMConstants.STRUCTURES_NS_URI_PREFIX;
import static org.mitre.niem.cmf.NamespaceKind.NSK_EXTENSION;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UNKNOWN;
import static org.mitre.niem.cmf.NamespaceKind.namespaceKindFromURI;
import static org.mitre.niem.xsd.NIEMBuiltins.getBuiltinVersion;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A class for providing information from a schema document that is not
 * available through the Xerces XML Schema (XSModel) API:<ul>
 *
 * <li> The target namespace attribute value</li>
 * <li> All appinfo attributes in declarations, definitions, and imports </li>
 * <li> All namespace declarations encountered in the document </li>
 * <li> NIEM conformance target assertions </li>
 * <li> NIEM version from structures namespace URI </li>
 * <li> xs:schema @version attribute </li></ul>
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLSchemaDocument {
    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(XMLSchemaDocument.class);    
    
    private final List<XMLNamespaceDeclaration> nsdels = new ArrayList<>();
    private final List<String> externalImports = new ArrayList<>();
    private final List<Appinfo> appinfo = new ArrayList<>();
    private String targetNS = null;
    private String confTargs = null;
    private String niemVersion = null;
    private String schemaVersion = null;
    private String documentation = null;
    private String filepath = null;
    private int kind = NSK_UNKNOWN;
    
    public List<XMLNamespaceDeclaration> namespaceDecls ()  { return nsdels; }
    public List<String> externalImports ()                  { return externalImports; }
    public List<Appinfo> appinfo ()                         { return appinfo; }
    public String targetNamespace ()                        { return targetNS; }
    public String conformanceTargets ()                     { return confTargs; }
    public String niemVersion ()                            { return niemVersion; }
    public String schemaVersion ()                          { return schemaVersion; }
    public String documentation ()                          { return documentation; }
    public String filepath ()                               { return filepath; }
    public int schemaKind ()                                { return kind; }
    
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
        private XMLNamespaceScope nsm = new XMLNamespaceScope();
        private Pair<String,String> ctypeEQN = null;    // namespace and lname of complex type being parsed
        private Locator docloc = null;                  // for document line number in log messages
        private StringBuilder chars;                    // current element simple content
        private int depth = 0;                          // root element has depth = 0
        
        XSDHandler () { 
            chars = new StringBuilder();
        } 
        
        @Override
        // Do two things with prefix maps.  Keep a list of all mappings in the document
        // for a later prefix normalization.  Also keep a stack of the current mappings
        // to interpret QName in <xs:element ref="q:name" appinfo:foo="bar"/>
        public void startPrefixMapping (String prefix, String uri) {
            if (prefix.isEmpty()) return;
            nsm.onStartPrefixMapping(prefix, uri);
            int line = docloc.getLineNumber();
            var nsd = new XMLNamespaceDeclaration(prefix, uri, line, depth, null, NSK_UNKNOWN);  // don't know target NS or kind yet
            nsdels.add(nsd);
            if (uri.startsWith(STRUCTURES_NS_URI_PREFIX)) {
                niemVersion = getBuiltinVersion(uri);   // import of structures namespace determines NIEM version of this doc
            }
        }        
        
        private static final Pattern xsComponentPat = Pattern.compile("(attribute)|(element)|(complexType)");
        
        @Override
        public void startElement (String ens, String elname, String eQN, Attributes atts) { 
            
            // Handle xs:schema element
            nsm.onStartElement();
            if ("schema".equals(elname)) {
                targetNS = atts.getValue("targetNamespace");
                if (null == targetNS) LOG.warn("found no target namespace while parsing {}", docloc.getSystemId());
                
                // Get schema version attribute
                schemaVersion = atts.getValue("version");

                // Get conformance target assertion
                for (int i = 0; i < atts.getLength(); i++) {
                    if (atts.getURI(i).startsWith(CONFORMANCE_TARGET_NS_URI_PREFIX)) {
                        if (CONFORMANCE_ATTRIBUTE_NAME.equals(atts.getLocalName(i))) {                        
                            confTargs = atts.getValue(i);
                            break;
                        }
                    }
                }
                // What kind of schema is this?
                kind = namespaceKindFromURI(targetNS);
                if (NSK_UNKNOWN == kind && null != confTargs && !confTargs.isEmpty()) kind = NSK_EXTENSION;    
            }
            // Handle xs:import elements
            else if ("import".equals(elname)) {
                String importNS = atts.getValue("namespace");
                for (int i = 0; i < atts.getLength(); i++) {
                    if (atts.getURI(i).startsWith(APPINFO_NS_URI_PREFIX))
                        if ("externalImportIndicator".equals(atts.getLocalName(i))) {
                            if ("true".equals(atts.getValue(i))) externalImports.add(importNS);
                            break;
                        }   
                }
            }
            // Remember the QName of the complex type we are defining
            else if (1 == depth && "complexType".equals(elname)) ctypeEQN = Pair.with(targetNS, atts.getValue("name"));
                
            // Look for appinfo attributes on global definitions and declarations, or on xs:element within a complex type definition
            if (1 == depth && xsComponentPat.matcher(elname).lookingAt()) {   
                for (int i = 0; i < atts.getLength(); i++) {
                    if (atts.getURI(i).startsWith(APPINFO_NS_URI_PREFIX)) {
                        var eqn = Pair.with(targetNS, atts.getValue("name"));
                        appinfo.add(new Appinfo(targetNS, atts.getLocalName(i), atts.getValue(i), eqn, null));
                    }
                } 
            }
            // Look for appinfo attributes on element declarations within complex type definition
            if (null != ctypeEQN && "element".equals(elname)) {
                for (int i = 0; i < atts.getLength(); i++) {
                    if (atts.getURI(i).startsWith(APPINFO_NS_URI_PREFIX)) {
                        String eref = atts.getValue("ref");
                        if (null != eref) {
                            Pair<String,String> eqn;
                            try {
                                eqn = nsm.resolve(eref);
                                if (null == eqn.getValue0()) eqn.setAt0(targetNS);
                                Appinfo a = new Appinfo(targetNS, atts.getLocalName(i), atts.getValue(i), ctypeEQN, eqn);
                                appinfo.add(a);                                
                            } catch (XMLNamespaceScope.XMLNamespaceMapException ex) {
                                LOG.warn(String.format("%s, line %d: unable to resolve %s", 
                                        docloc.getSystemId(), docloc.getLineNumber(), eref));
                            }
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
            if (1 == depth && "complexType".equals(elname)) ctypeEQN = null;    // finished global type defn
            
            // The first <documentation> element in document order at depth 2 
            // has to be the schema documentation string
            if (2 == depth && null == documentation && "documentation".equals(elname)) {
                documentation = chars.toString().trim();
            }
            if (chars.length() > 0) chars = new StringBuilder();
        }    

        @Override
        public void characters (char[] ch, int start, int length) {
            chars.append(ch, start, length);
        }
        
        @Override
        public void setDocumentLocator (Locator l) {
            docloc = l;
        }
    }
    
}


