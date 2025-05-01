
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
package org.mitre.niem.xsd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.niem.xml.XMLNamespaceDeclaration;
import org.mitre.niem.xml.XMLSchema;
import org.mitre.niem.xml.XMLSchemaDocument;
import org.mitre.niem.xml.XMLSchemaException;
import static org.mitre.niem.xsd.NamespaceKind.NSK_EXTENSION;
import static org.mitre.niem.xsd.NamespaceKind.NSK_EXTERNAL;
import static org.mitre.niem.xsd.NamespaceKind.NSK_NOTNIEM;
import static org.mitre.niem.xsd.NamespaceKind.NSK_UNKNOWN;
import static org.mitre.niem.xsd.NamespaceKind.namespaceToKindCode;
import org.xml.sax.SAXException;

/**
 *
 * A class to represent a NIEM schema document pile.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NIEMSchema extends XMLSchema {
    static final Logger LOG = LogManager.getLogger(NIEMSchema.class);
    
    private final Map<String,NIEMSchemaDocument> nsdocs = new HashMap<>();
    private final NamespaceMap nsmap         = new NamespaceMap();
    private final Set<String> extNSs         = new HashSet<>();             // external namespace URIs
    private final Map<String,Integer> nsKind = new HashMap<>();             // cached namespace kinds
    
    protected NIEMSchema () { }     // no public default constructor
    
    public NIEMSchema (File xsdF) throws XMLSchemaException {
        this(xsdF.toString());
    }
    
    public NIEMSchema (String... args) throws XMLSchemaException {
        super(args);
        initSchemaDocuments();              // create NIEMSchemaDocument objects
        identifyExternalNamespaces();       // find all external namespaces
        initNamespaceMap();                 // resolve any namespace prefix collisions
    }
    
    /**
     * Returns a list of NIEMSchemaDocument objects for this schema.
     * @return 
     */
    @SuppressWarnings("unchecked")
    public List<NIEMSchemaDocument> schemaDocL () {
        // The base class method thinks that schemaDocumentL returns a list of 
        // XMLSchemaDocument objects, but we know it's actually a list of derived 
        // NIEMSchemaDocument objects; see newSchemaDocument method below.
        return (List<NIEMSchemaDocument>)(List<?>)schemaDocumentL();
    }
        
    /**
     * Returns the NIEMSchemaDocument object for the schema document with the
     * specified target namespace.  Returns null if no such document in the pile.
     * @param ns
     * @return 
     */
    @Override
    public NIEMSchemaDocument schemaDocument (String ns) {
        return nsdocs.get(ns);
    }
    
    /**
     * Returns the namespace portion of a component URI.  Copes with namespace
     * URIs that do not end in a slash (grr.)
     * @param u
     * @return 
     */
    public String uriToNamespaceU (String u) {
        if (u.startsWith(W3C_XML_SCHEMA_NS_URI)) return W3C_XML_SCHEMA_NS_URI;
        else if (u.startsWith(XML_NS_URI)) return XML_NS_URI;
        int indx = u.lastIndexOf("/");
        if (indx < 0) return "";
        var res = u.substring(0,indx+1);
        if (nsdocs.containsKey(res)) return res;
        res = res.substring(0, res.length()-1);
        if (nsdocs.containsKey(res)) return res;
        return "";
    }
    
    /**
     * Returns the local name portion of a component URI.
     * @param u
     * @return 
     */
    public String uriToLocalName (String u) {
        int indx = u.lastIndexOf("/");
        if (indx < 0) return "";
        return u.substring(indx+1);
    }
    
    /**
     * Returns the namespace kind given a namespace URI.
     * Utility namespaces and NIEM model namespaces are identified from URI alone.
     * External namespaces are identified by one or more xs:import in the pile.
     * Extension namespaces have a conformance target assertion that maps to a NIEM version.
     * 
     * @param nsuri - namespace URI
     * @return 
     */
    public int namespaceKind (String nsuri) {
        if (nsKind.containsKey(nsuri)) return nsKind.get(nsuri);
        var kind = NamespaceKind.namespaceToKind(nsuri);
        if (NSK_UNKNOWN == kind) {
            var sd = nsdocs.get(nsuri);
            if (extNSs.contains(nsuri)) kind = NSK_EXTERNAL;
            else if (null == sd) 
                LOG.error("no schema document for namespace URI '{}'", nsuri);
            else {
              for (var cta : nsdocs.get(nsuri).ctAssertions()) 
                if (!NamespaceKind.ctaToVersion(cta).isEmpty()) kind = NSK_EXTENSION;
              if (NSK_UNKNOWN == kind) kind = NSK_NOTNIEM;
            }
        }
        nsKind.put(nsuri, kind);
        return kind;
    }
    
    public int namespaceKind (NIEMSchemaDocument sd) {
        return namespaceKind(sd.targetNamespace());
    }
    
    /**
     * Returns true if and only if the URI identifes a NIEM model namespace.
     * @param nsuri - namespace URI
     * @return true for a model namespace
     */
    public boolean isModelNamespace (String nsuri) {
        return NamespaceKind.isModelKind(namespaceKind(nsuri));
    }
    
    public boolean isModelNamespace (NIEMSchemaDocument sd) {
        return isModelNamespace(sd.targetNamespace());
    }
    
    public boolean isModelComponentU (String u) {
        if (u.isEmpty()) return false;
        var nsU = uriToNamespaceU(u);
        return isModelNamespace(nsU);
    }
    
    /**
     * Returns true if and only if the URI identifies an external namespace.
     * 
     */
    public boolean isExternal (String nsuri) {
        return extNSs.contains(nsuri);
    }
    
    public boolean isExternal (NIEMSchemaDocument sd) {
        return isExternal(sd.targetNamespace());
    }
    
    /**
     * Returns a reconciled map of all the namespace declarations in all of the
     * schema documents. The reconciled map guarantees that each namespace prefix
     * is bound to exactly one namespace URI. Some of the prefixes in the namespace
     * declarations found in the schema documents may be changed to ensure thls.
     * @return NamespaceMap object
     */
    public NamespaceMap namespaceMap () {
        return nsmap;
    }

    // The XMLSchema class has the code to parse the schema documents in thie pile.
    // The objects created there will be NIEMSchemaDocuments, because this class overrides
    // the newSchemaDocument method.  Cast them properly and remember them here.
    private void initSchemaDocuments () throws XMLSchemaException {
        for (var nsuri : schemaNamespaceUs()) {
            var sd  = super.schemaDocument(nsuri);
            var nsd = (NIEMSchemaDocument)sd;
            nsdocs.put(nsuri, nsd);
        }
    }
    
    // Find all the external namespaces.
    private void identifyExternalNamespaces () {
        for (var sd : schemaDocumentL()) {
            for (var impr : sd.importElements()) {
                if (impr.attL().isEmpty()) continue;
                for (var att : impr.attL()) {
                    var aU    = att.namespace();
                    var aname = att.name();
                    var aval  = att.value();
                    if (!"externalImportIndicator".equals(aname)) continue;
                    if (!"true".equals(aval)) continue;
                    if (!"APPINFO".equals(namespaceToKindCode(aU))) continue;
                    extNSs.add(impr.nsU());
                }
            }
        }
    }

    // Now that we know the kind of each namespace, we can make an ordered list of
    // the namespace declarations in the pile and use it to initialize the namespace
    // map.  We give highest priority to the declarations in extension namespaces 
    // (and subsets of extension namespaces) on the assumption that the message
    // designer knows what he is doing.  Next, namespace declarations in the NIEM
    // model namespaces.  External namespaces are lowest priority.
    private void initNamespaceMap () {
        var decL = new ArrayList<NDrec>();
        nsdocs.forEach((ns,sd) -> {
            var kind = namespaceKind(ns);
            for (var dec : sd.namespaceDeclarations()) {
                decL.add(new NDrec(kind, dec));
            }
        });
        Collections.sort(decL);
        for (var dec : decL) {
            var uri = dec.nd.ns();
            var pre = dec.nd.prefix();
            nsmap.assignPrefix(pre, uri);
        }
    }

    // The base class creates XMLSchemaDocument objects, but we want NIEMSchemaDocument
    // objects, so we override this method to get what we want.
    @Override
    protected XMLSchemaDocument newSchemaDocument (File sdF) throws SAXException, IOException, ParserConfigurationException {
        return new NIEMSchemaDocument(sdF);
    }    
    
    // For sorting namespace declarations into priority order.
    // Once the namespace map is created we don't need these records any more.
    private record NDrec (int kind, XMLNamespaceDeclaration nd) implements Comparable<NDrec> {
        @Override
        public int compareTo(NDrec o) {
            if (this.kind() < o.kind()) return -1;
            else if (this.kind() > o.kind()) return 1;
            else return o.nd().compareTo(this.nd());
        }
    }
        
}
