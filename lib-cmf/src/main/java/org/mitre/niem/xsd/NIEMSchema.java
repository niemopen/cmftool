
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
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.niem.xml.LanguageString;
import org.mitre.niem.xml.XMLNamespaceDeclaration;
import org.mitre.niem.xml.XMLSchema;
import org.mitre.niem.xml.XMLSchemaDocument;
import static org.mitre.niem.xsd.NamespaceKind.NSK_EXTENSION;
import static org.mitre.niem.xsd.NamespaceKind.NSK_EXTERNAL;
import static org.mitre.niem.xsd.NamespaceKind.NSK_NOTNIEM;
import static org.mitre.niem.xsd.NamespaceKind.NSK_UNKNOWN;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NIEMSchema extends XMLSchema {
    static final Logger LOG = LogManager.getLogger(NIEMSchema.class);
    
    private final Map<String,NIEMSchemaDocument> nsdocs = new HashMap<>();
    private final NamespaceMap nsmap = new NamespaceMap();
    private final List<String> nsURIList = new ArrayList<>();  
    private final Map<String,Integer> nsKind = new HashMap<>();             // cached namespace kinds
    private final Map<String,List<LanguageString>> extImportDoc = new HashMap<>();
    
    protected NIEMSchema () { }     // no public default constructor
    
    public NIEMSchema (String... args) throws XMLSchemaException {
        super(args);
        initSchemaDocuments();      // create NIEMSchemaDocument objects
        initExternalImports();      // find all external imports
        initNamespaceMap();         // resolve any namespace prefix collisions
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
            if (extImportDoc.containsKey(nsuri)) kind = NSK_EXTERNAL;
            else if (null == nsdocs.get(nsuri)) LOG.error("no schema document for namespace URI {}", nsuri);
            else {
              for (var cta : nsdocs.get(nsuri).ctAssertions()) 
                if (!NamespaceKind.ctaToVersion(cta).isEmpty()) kind = NSK_EXTENSION;
              if (NSK_UNKNOWN == kind) kind = NSK_NOTNIEM;
            }
        }
        nsKind.put(nsuri, kind);
        return kind;
    }
    
    /**
     * Returns true if and only if the URI identifes a NIEM model namespace.
     * @param nsuri - namespace URI
     * @return true for a model namespace
     */
    public boolean isModelNamespace (String nsuri) {
        return NamespaceKind.isModelKind(namespaceKind(nsuri));
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
    
    /**
     * Returns a list of all documentation strings from all xs:import elements
     * that import an external namespace (ie. have @appinfo:externalImportIndicator="true").
     * Returns null if the specified namespace is not an external namespace.
     * @param nsuri - namespace uri of external namespace
     * @return list of documentation LanguageString objects.
     */
    public List<LanguageString> importDocumentation (String nsuri) {
        return extImportDoc.get(nsuri);
    }

    
    // The XMLSchema class has the code to parse the schema documents in thie pile.
    // The objects created will be NIEMSchemaDocuments, because this class overrides
    // the newSchemaDocument method.  Cast them properly and remember them here.
    private void initSchemaDocuments () throws XMLSchemaException {
        for (var nsuri : schemaNamespaceURIs()) {
            var sd = super.schemaDocument(nsuri);
            nsdocs.put(nsuri, (NIEMSchemaDocument)sd);
        }
    }
   
    // Look through all the imports in the pile and remember the external 
    // namespaces and their documentation.
    private void initExternalImports () {
        for (var sd : nsdocs.values()) {
            var impL = sd.allImports();
            for (var irec : impL) {
                if (irec.isExternal()) {
                    var impDocL = irec.docL();
                    var docL = extImportDoc.get(irec.imported());
                    if (null == docL) {
                        docL = new ArrayList<>();
                        extImportDoc.put(irec.imported(), docL);                        
                    }
                    docL.addAll(impDocL);
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
            for (var dec : sd.nsdecls()) {
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

    // Override in derived class to generate a derived XMLSchemaDocument class.
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
