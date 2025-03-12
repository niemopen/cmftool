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
import org.apache.logging.log4j.LogManager;
import org.mitre.niem.cmf.LocalTerm;
import org.mitre.niem.xml.LanguageString;
import org.mitre.niem.xml.XMLSchemaDocument;
import static org.mitre.niem.xml.XMLWriter.nodeToText;
import static org.mitre.niem.xsd.NIEMConstants.CONFORMANCE_ATTRIBUTE_NAME;
import static org.mitre.niem.xsd.NIEMConstants.CTAS30;
import static org.mitre.niem.xsd.NIEMConstants.CTAS60;
import static org.mitre.niem.xsd.NamespaceKind.NSK_STRUCTURES;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class NIEMSchemaDocument extends XMLSchemaDocument {
    static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(NIEMSchemaDocument.class);
    
    private String ctasNSuri = null;
    private List<String> ctaList = null;
    private String niemVersion = null;
    private List<LocalTerm> locTermL = null;
    private List<ImportRec> imports = null;
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
        for (var nsd : nsdecls()) {
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
        ctaList = new ArrayList<>();
        var ctasUstr = ctasNS();
        var xpath = String.format("/*[@*[namespace-uri()='%s' and local-name()='%s']][1]",
                ctasUstr, CONFORMANCE_ATTRIBUTE_NAME);
        var nodes = evalForNodes(dom().getDocumentElement(), xpath);
        if (null !=  nodes && nodes.getLength() > 0) {
            var el   = (Element)nodes.item(0);
            var cstr = el.getAttributeNS(ctasUstr, CONFORMANCE_ATTRIBUTE_NAME);
            var spl  = cstr.split("\\s+");
            for (int i = 0; i < spl.length; i++) ctaList.add(spl[i]);
        }
        return ctaList;
    }         
    
    /**
     * Returns the NIEM version for this schema document; eg. "NIEM6.0", "NIEM5.0".
     * Work this out from a builtin document's target namespace.  For other documents,
     * use the conformance target assertions, and cross-check against structures 
     * namespace import.  Returns empty string if not a NIEM schema document.
     * @return NIEM version (eg. "NIEM6.0")
     */
    public String niemVersion () {
        if (null != niemVersion) return niemVersion;
        niemVersion = NamespaceKind.namespaceToNIEMVersion(targetNamespace());
        if (!niemVersion.isEmpty()) return niemVersion;
        
        // Not a built-in, get NIEM version from CTAs
        var ctas = ctAssertions();
        var ovrs = "";
        var vers = "";
        for (var cta: ctas) {
            vers = NamespaceKind.ctaToVersion(cta);
            if (vers.isEmpty()) continue;
            if (!vers.equals(ovrs) && !ovrs.isEmpty()) {
                LOG.warn("{}: conflicting versions from CTAs ({} and {})", docFile().getName(), vers, ovrs);
            }
            if (niemVersion.isEmpty()) niemVersion = vers;  // first CTA wins
            ovrs = vers;
        }
        // Check version from CTAs against structures import
          for (var irec : allImports()) {
            var kind = NamespaceKind.namespaceToKind(irec.imported());
            var nver = NamespaceKind.namespaceToNIEMVersion(irec.imported());
            if (NSK_STRUCTURES == kind && !nver.equals(niemVersion))
                LOG.warn("{}: conflicting versions from CTA and structures import ({} and {})",
                    docFile().getName(), niemVersion, nver);            
        }
        return niemVersion;
    }
    
    /**
     * Returns the requested builtin namespace URI string for this schema document.
     * Returns empty string for unknown builtin kind, or if the NIEM version for this
     * schema document does not have the requested builtin.  Warning log messages if
     * the requested namespace is not declared, or if there are conflicting namespace
     * declarations for the requested builtin kind.
     * @param kindCode - kind of builtin (eg. "STRUCTURES")
     * @return namespace URI string
     */
    public String builtinNS (String kindCode) {
        if (!NamespaceKind.builtins().contains(kindCode)) {
            LOG.error("builtinNS: unknown builtin code '{}'", kindCode);
            return "";
        }
        var res = builtinNS.get(kindCode);
        if (null != res) return res;
        
        var vers = niemVersion();
        res = NamespaceKind.builtinNamespace(vers, kindCode);
        builtinNS.put(kindCode, res);
        
        var nsUstr = "";                // last namespace decl for this builtin seen
        var nsdL = nsdecls();           // list of all namespace declarations
        for (var nsd : nsdL) {
            var nk = NamespaceKind.namespaceToKindCode(nsd.ns());
            if (nk.equals(kindCode)) {
                if (!nsUstr.isEmpty() && !nsUstr.equals(nsd.ns())) {
                    LOG.warn("namespace {} has conflicting {} namespace declarations ({} and {})", 
                        docURI().toString(), kindCode, nsUstr, nsd.ns());
                }
                nsUstr = nsd.ns();
            }
        }
        return res;
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
    public String qnToURI (Element e, String qn) {
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
    
    /**
     * Returns the prefix portion of a QName
     * @param qn
     * @return 
     */
    public static String qnToPrefix (String qn) {
        var indx = qn.indexOf(":");
        if (indx < 1 || indx >= qn.length()-1) return "";
        return qn.substring(0, indx);        
    }
    
    /**
     * Returns the local name portion of a QName
     * @param qn
     * @return 
     */
    public static String qnToName (String qn) {
        var indx = qn.indexOf(":");
        if (indx < 1 || indx >= qn.length()-1) return "";        
        return qn.substring(indx+1);
    }
    
    /**
     * Returns a list of the local term objects defined in this schema document.
     * @return 
     */
    public List<LocalTerm> localTerms () {
        if (null != locTermL) return locTermL;
        locTermL = new ArrayList<>();
        var appinfo = builtinNS("APPINFO");
        var ltermXP = "//*[namespace-uri()='" + appinfo + "' and local-name()='LocalTerm']";
        var ltermNL = evalForNodes(dom().getDocumentElement(), ltermXP);
        for (int i = 0; i < ltermNL.getLength(); i++) {
            var e = (Element)ltermNL.item(i);
            var lt = genLocalTerm(e);
            locTermL.add(lt);
        }
        return locTermL;
    }
    
    private LocalTerm genLocalTerm (Element e) {
        var res = new LocalTerm();
        var appinfo = builtinNS("APPINFO");
        res.setTerm(e.getAttribute("term"));
        res.setLiteral(e.getAttribute("literal"));
        res.setDocumentation(e.getAttribute("definition"));
        var suris = e.getAttribute("sourceURIs");
        if (!suris.isBlank())
            for (var su : suris.split("\\s+")) res.addSource(su);
        var stxtNL = e.getElementsByTagNameNS(appinfo, "SourceText");
        for (int i = 0; i < stxtNL.getLength(); i++) {
            var stxtE = (Element)stxtNL.item(i);
            res.addCitation(stxtE.getTextContent());
        }
        return res;
    }
    
    /**
     * Returns a list of appinfo:Augmentation elements in the schema document.
     * @return 
     */
    public List<Element> attributeAugmentations () {
        var res = new ArrayList<Element>();
        var appinfo = builtinNS("APPINFO");
        var augXP = "//*[namespace-uri()='" + appinfo + "' and local-name()='Augmentation']";
        var augNL = evalForNodes(dom().getDocumentElement(), augXP);
        for (int i = 0; i < augNL.getLength(); i++) {
            var e = (Element)augNL.item(i);
            var estr = nodeToText(e);
            var classU = e.getAttribute("class");
            var codes  = e.getAttribute("globalClassCode");
            if (classU.isBlank() && codes.isBlank())
                LOG.error("{}: missing @class or @globalClassCode in {}", docFile().getName(), estr);
            else res.add(e);
        }
        return res;
    }    
    
    /**
     * Returns a list of records for all top-level import elements in the
     * schema document. The import records include external import indicator
     * appinfo, plus any documentation.
     */
    public List<ImportRec> allImports () {
        if (null != imports) return imports;
        imports = new ArrayList<>();
        
        var nvers  = niemVersion();
        var appiNS = NamespaceKind.builtinNamespace(nvers, "APPINFO");
        var iels   = importElements();
        if (null == iels) return imports;       // bad XPath, shouldn't happen
        
        for (int i = 0; i < iels.getLength(); i++) {
            var ie    = (Element)iels.item(i);
            var ns    = ie.getAttribute("namespace");
            var docL  = getDocumentation(ie);
            var isExt = false;
            if (!appiNS.isEmpty())
                isExt = "true".equals(ie.getAttributeNS(appiNS, "externalImportIndicator"));
            imports.add(new ImportRec(this.targetNamespace(), ns, docL, isExt));
        }
        return imports;
    }
    
    public record ImportRec (String importing, String imported, List<LanguageString> docL, boolean isExternal) {}
}
