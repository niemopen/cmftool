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

import java.io.IOException;
import org.apache.xerces.dom.DOMInputImpl;
import org.apache.xerces.util.URI;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class XMLCatalogResolver implements XMLEntityResolver, EntityResolver2, LSResourceResolver {
    
    org.apache.xerces.util.XMLCatalogResolver del;
    
    XMLCatalogResolver (String[] catalogs) {
        del = new org.apache.xerces.util.XMLCatalogResolver();
        del.setCatalogList(catalogs);
    }
    
    @Override
    public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier)
        throws XNIException, IOException {
        return del.resolveEntity(resourceIdentifier);
    }
    
    public final synchronized String resolveURI (String uri) throws IOException {
        String res = del.resolveURI(uri);
//        System.out.println(String.format("resolveURI(%s)->%s", uri, res));
        return res;
    }

    @Override
    public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
        return del.getExternalSubset(name, baseURI);
    }

    @Override
    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
        return del.resolveEntity(name, publicId, baseURI, systemId);
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return del.resolveEntity(publicId, systemId);
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
//        return del.resolveResource(type, namespaceURI, publicId, systemId, baseURI);
        String resolvedId = null;
        
        try {
            // The namespace is useful for resolving namespace aware
            // grammars such as XML schema. Let it take precedence over
            // the external identifier if one exists.
            if (namespaceURI != null) {
                resolvedId = resolveURI(namespaceURI);
            }
            
            if (!del.getUseLiteralSystemId() && baseURI != null) {
                // Attempt to resolve the system identifier against the base URI.
                try {
                    URI uri = new URI(new URI(baseURI), systemId);
                    systemId = uri.toString();
                }
                // Ignore the exception. Fallback to the literal system identifier.
                catch (URI.MalformedURIException ex) {}
            }
        
            // Resolve against an external identifier if one exists. This
            // is useful for resolving DTD external subsets and other 
            // external entities. For XML schemas if there was no namespace 
            // mapping we might be able to resolve a system identifier 
            // specified as a location hint.
            if (resolvedId == null) {
                if (publicId != null && systemId != null) {
                    resolvedId = del.resolvePublic(publicId, systemId);
                }
                else if (systemId != null) {
                    resolvedId = del.resolveSystem(systemId);
                }
            }
        }
        // Ignore IOException. It cannot be thrown from this method.
        catch (IOException ex) {}
        
        if (resolvedId != null) {
            return new DOMInputImpl(publicId, resolvedId, baseURI);
        }  
        return null;        
    }
}
