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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static javax.xml.XMLConstants.XML_NS_URI;
import javax.xml.parsers.ParserConfigurationException;
import org.mitre.niem.xml.XMLSchemaDocument;
import static org.mitre.niem.xsd.NIEMConstants.CONFORMANCE_ATTRIBUTE_NAME;
import static org.mitre.niem.xsd.NIEMConstants.CTAS30;
import static org.mitre.niem.xsd.NIEMConstants.CTAS60;
import static org.mitre.niem.xsd.NamespaceKind.namespaceToKindCode;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import static org.mitre.niem.xsd.NamespaceKind.namespaceToArchVersion;

/**
 * A class to represent a schema document in a NIEM XML schema document pile.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NIEMSchemaDocument extends XMLSchemaDocument {
    
    private String ctasNSuri = null;
    private List<String> ctaList = null;
    private String niemVersion = null;
    private final Map<String,String> builtinNS = new HashMap<>();

    public NIEMSchemaDocument(File sdF) throws SAXException, IOException, ParserConfigurationException {
        super(sdF);
    }
    
    /**
     * Returns the namespace URI for the conformance target attributes specification
     * used in this schema document.  Returns the empty string if no CTAS namespace
     * is declared.
     * @return conformance target attribute specification NS URI string
     */
    public String ctasNS () {
        if (null != ctasNSuri) return ctasNSuri;
        for (var nsd : namespaceDeclarations()) {
            if (CTAS30.equals(nsd.ns()) || CTAS60.equals(nsd.ns())) {
                ctasNSuri = nsd.ns();
                return ctasNSuri;
            }
        }
        ctasNSuri = "";
        return "";
    }
    
    /**
     * Returns a list of the conformance target assertions in the schema document.
     * Returns an empty list if no CTAs.
     * @return list of CTAs
     */
    public List<String> ctAssertions () {
        if (null != ctaList) return ctaList;
        ctaList   = new ArrayList<>();
        var ctasU = ctasNS();
        var cstr  = documentElement().getAttributeNS(ctasU, CONFORMANCE_ATTRIBUTE_NAME);
        if (cstr.isEmpty()) return ctaList;
        var spl   = cstr.split("\\s+");
        for (int i = 0; i < spl.length; i++) ctaList.add(spl[i]);
        return ctaList;
    }         
    
    /**
     * Returns the NIEM version for this schema document; eg. "NIEM6.0", "NIEM5.0".
     * Work this out from a builtin document's target namespace.  For other documents,
     * use the structures namespace import, and cross-check against the conformance
     * target assertions.  Returns empty string if not a NIEM schema document.
     * @return NIEM version (eg. "NIEM6.0")
     */
    public String niemVersion () { 
        if (null != niemVersion) return niemVersion; 
        niemVersion = namespaceToArchVersion(targetNamespace());
        if (!niemVersion.isEmpty()) return niemVersion;
        for (var imp : importElements ()) {
            var insU  = imp.nsU();
            var kcode = namespaceToKindCode(insU);
            if ("STRUCTURES".equals(kcode)) {
                niemVersion = namespaceToArchVersion(insU);
                return niemVersion;
            }
        }
        return "";
    }
    
    /**
     * Returns the URI for the component with the specified name in the namespace
     * defined by this schema document.
     * @param name
     * @return 
     */
    public String nameToURI (String name) {
        var nsU = targetNamespace();
        if (nsU.endsWith("/")) return nsU + name;
        return nsU + "/" + name;
    }
    
    /**
     * Returns the URI for the component with the specified QName.  The namespace
     * portion of the URI is determined by the prefix assignment in scope at the
     * specified Element object -- which may or may not be the namespace assigned
     * to that prefix in the model.
     * @param e - Element object
     * @param qn - component QName; eg. "foo:bar"
     * @return component URI
     */
    public static String qnToURI (Element e, String qn) {
        var indx = qn.indexOf(":");
        if (indx < 1 || indx >= qn.length()-1) return "";
        var pre = qn.substring(0, indx);
        var ln  = qn.substring(indx+1);
        var nsU = e.lookupNamespaceURI(pre);
        if ("xml".equals(pre)) nsU = XML_NS_URI;
        if (null == nsU) return "";
        if (nsU.endsWith("/")) return nsU + ln;
        return nsU + "/" + ln;
    }

}
