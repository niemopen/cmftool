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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.niem.utility.ResourceManager;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.Attributes2Impl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Executes ISO Schematron rules on an XML document and converts SVRL output
 * into line-oriented messages.
 *
 * Relative references inside a Schematron source require the source systemId
 * to be set.
 */
public class Schematron {

    private static final Logger LOG = LogManager.getLogger(Schematron.class);

    public static final String ISO_DSDL = "iso_dsdl_include.xsl";
    public static final String ISO_ABSTRACT = "iso_abstract_expand.xsl";
    public static final String ISO_SVRL = "iso_svrl_for_xslt2.xsl";
    public static final String ISO_SKEL = "iso_schematron_skeleton_for_saxon.xsl";
    public static final String XSLT_PP = "xsltpp.xsl";
    public static final String SVRL_NS = "http://purl.oclc.org/dsdl/svrl";
    public static final String SCHEVAL_NS = "http://xml.niem.mitre.org/LOC/";

    private final Processor saxonProc;
    private final XsltCompiler saxonComp;
    private final SAXParserFactory saxFact;
    private final TransformerFactory transFact;
    private final DocumentBuilderFactory docbldFact;
    private final XPathFactory xpathFact;

    // Cache executables; load a fresh transformer for each use.
    private volatile XsltExecutable dsdlExec;
    private volatile XsltExecutable abstractExec;
    private volatile XsltExecutable svrlExec;
    private volatile XsltExecutable xsltPpExec;

    public Schematron() throws SaxonApiException {
        saxonProc = new Processor(false);
        saxonComp = saxonProc.newXsltCompiler();
        saxFact = SAXParserFactory.newInstance();
        transFact = TransformerFactory.newInstance();
        docbldFact = DocumentBuilderFactory.newInstance();
        xpathFact = XPathFactory.newInstance();

        configureFactories();
    }

    /**
     * Compiles a Schematron document into a transformer object.
     * The source systemId must be set if the Schematron uses relative includes.
     *
     * @param src Schematron source
     * @return compiled transformer
     * @throws SaxonApiException on compilation failure
     */
    public XsltTransformer compileSchematron(StreamSource src) throws SaxonApiException {
        return compileSchematronExecutable(src).load();
    }

    /**
     * Compiles a Schematron document into an XSLT executable.
     * The source systemId must be set if the Schematron uses relative includes.
     *
     * @param src Schematron source
     * @return compiled executable
     * @throws SaxonApiException on compilation failure
     */
    public XsltExecutable compileSchematronExecutable(StreamSource src) throws SaxonApiException {
        Objects.requireNonNull(src, "src must not be null");
        requireSystemId(src, "Schematron source");

        var sw = new StringWriter();
        compileSchematron(src, sw);

        var ss = new StreamSource(new StringReader(sw.toString()));
        ss.setSystemId(src.getSystemId());
        return saxonComp.compile(ss);
    }

    /**
     * Compiles a Schematron document into XSLT.
     * The source systemId must be set if the Schematron uses relative includes.
     *
     * @param src Schematron source
     * @param ow writer to receive XSLT output
     * @throws SaxonApiException on compilation failure
     */
    public void compileSchematron(StreamSource src, Writer ow) throws SaxonApiException {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(ow, "ow must not be null");

        prepareSCHTransforms();
        var srcId = requireSystemId(src, "Schematron source");

        var stage1 = transformToString(src, dsdlExec);
        var stage2 = transformToString(asSource(stage1, srcId), abstractExec);
        var stage3 = transformToString(asSource(stage2, srcId), svrlExec);
        transform(asSource(stage3, srcId), xsltPpExec, ow);
    }

    /**
     * Applies an XSLT transformation to an XML document.
     *
     * Note: the supplied transformer should be considered single-use.
     *
     * @param src XML source
     * @param trans loaded XSLT transformer
     * @param ow writer to receive output
     * @throws SaxonApiException on transform failure
     */
    public void applyXslt(StreamSource src, XsltTransformer trans, Writer ow) throws SaxonApiException {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(trans, "trans must not be null");
        Objects.requireNonNull(ow, "ow must not be null");

        Serializer ser = saxonProc.newSerializer(ow);
        trans.setDestination(ser);
        trans.setSource(src);
        trans.transform();
    }

    /**
     * Produces line-oriented messages from Schematron SVRL output.
     *
     * @param svrl SVRL results
     * @param xml XML document from which the SVRL was generated
     * @param msgs writer to receive messages
     * @throws ParserConfigurationException on parser configuration failure
     * @throws SAXException on parse failure
     * @throws IOException on I/O failure
     * @throws TransformerException on transform failure
     */
    public void SVRLtoMessages(InputSource svrl, InputSource xml, Writer msgs)
        throws ParserConfigurationException, SAXException, IOException, TransformerException {

        Objects.requireNonNull(svrl, "svrl must not be null");
        Objects.requireNonNull(xml, "xml must not be null");
        Objects.requireNonNull(msgs, "msgs must not be null");

        var db = docbldFact.newDocumentBuilder();
        var sdoc = db.parse(svrl);
        var sroot = sdoc.getDocumentElement();
        var adoc = annotateDocument(xml);

        var asserts = sroot.getElementsByTagNameNS(SVRL_NS, "failed-assert");
        var reports = sroot.getElementsByTagNameNS(SVRL_NS, "successful-report");

        for (int i = 0; i < asserts.getLength(); i++) {
            processSvrlElement(adoc, asserts.item(i), "ERROR", msgs);
        }
        for (int i = 0; i < reports.getLength(); i++) {
            processSvrlElement(adoc, reports.item(i), "WARN ", msgs);
        }
    }

    /**
     * Returns a DOM document in which each element from the source XML has a
     * synthetic location attribute containing source file, line, and column.
     *
     * @param src XML source
     * @return annotated DOM document
     * @throws ParserConfigurationException on parser configuration failure
     * @throws TransformerConfigurationException on transformer configuration failure
     * @throws SAXException on parse failure
     * @throws IOException on I/O failure
     * @throws TransformerException on transform failure
     */
    public Document annotateDocument(InputSource src)
        throws ParserConfigurationException, TransformerConfigurationException,
               SAXException, IOException, TransformerException {

        Objects.requireNonNull(src, "src must not be null");

        var saxp = saxFact.newSAXParser();
        var trans = transFact.newTransformer();
        var xmlrd = saxp.getXMLReader();
        var locfl = new LocationFilter(xmlrd, src.getSystemId());
        var saxs = new SAXSource(locfl, src);
        var domrs = new DOMResult();
        trans.transform(saxs, domrs);
        return (Document) domrs.getNode();
    }

    private synchronized void prepareSCHTransforms() throws SaxonApiException {
        if (dsdlExec != null) {
            return;
        }

        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("Schematron");
            var rmgr = new ResourceManager(getClass());

            var dsdlF = copySchResource(rmgr, ISO_DSDL, tmpDir);
            var absF = copySchResource(rmgr, ISO_ABSTRACT, tmpDir);
            var svrlF = copySchResource(rmgr, ISO_SVRL, tmpDir);
            var skelF = copySchResource(rmgr, ISO_SKEL, tmpDir);
            var xsltF = copySchResource(rmgr, XSLT_PP, tmpDir);

            dsdlExec = saxonComp.compile(dsdlF);
            abstractExec = saxonComp.compile(absF);
            svrlExec = saxonComp.compile(svrlF);
            xsltPpExec = saxonComp.compile(xsltF);

            // skelF is copied intentionally because the ISO stylesheets may include it.
            if (skelF == null) {
                throw new IOException("Failed to prepare " + ISO_SKEL);
            }
        } catch (IOException ex) {
            LOG.error("Unable to prepare Schematron transforms: {}", ex.getMessage());
            throw new SaxonApiException(ex);
        } finally {
            if (tmpDir != null) {
                deleteRecursively(tmpDir);
            }
        }
    }

    private File copySchResource(ResourceManager rmgr, String resourceName, Path tmpDir) throws IOException {
        var out = tmpDir.resolve(resourceName).toFile();
        rmgr.copyResourceToFile("/sch/" + resourceName, out);
        return out;
    }

    private String transformToString(StreamSource src, XsltExecutable exec) throws SaxonApiException {
        var sw = new StringWriter();
        transform(src, exec, sw);
        return sw.toString();
    }

    private void transform(StreamSource src, XsltExecutable exec, Writer ow) throws SaxonApiException {
        var trans = exec.load();
        if (exec == svrlExec) {
            trans.setParameter(new QName("allow-foreign"), new XdmAtomicValue("true"));
        }
        applyXslt(src, trans, ow);
    }

    private StreamSource asSource(String text, String systemId) {
        var ss = new StreamSource(new StringReader(text));
        ss.setSystemId(systemId);
        return ss;
    }

    private String requireSystemId(StreamSource src, String what) {
        var systemId = src.getSystemId();
        if (systemId == null || systemId.isBlank()) {
            throw new IllegalArgumentException(what + " must have a non-blank systemId");
        }
        return systemId;
    }

    private void processSvrlElement(Document adoc, Node node, String kind, Writer msgs) throws IOException {
        var e = (Element) node;
        var locXP = e.getAttribute("location");
        var txts = e.getElementsByTagNameNS(SVRL_NS, "text");
        var xpath = xpathFact.newXPath();

        String locS = "<unknown>";
        try {
            var xpr = xpath.compile(locXP);
            var nds = (NodeList) xpr.evaluate(adoc, XPathConstants.NODESET);
            for (int i = 0; i < nds.getLength(); i++) {
                var candidate = extractLocation(nds.item(i));
                if (candidate != null && !candidate.isBlank()) {
                    locS = candidate;
                    break;
                }
            }
        } catch (XPathExpressionException ex) {
            LOG.warn("Can't parse XPath '{}' in SVRL", locXP, ex);
        }

        for (int i = 0; i < txts.getLength(); i++) {
            var te = (Element) txts.item(i);
            var ts = te.getTextContent();
            msgs.write(String.format("%s %s -- %s%n", kind, locS, ts));
        }
    }

    private String extractLocation(Node node) {
        if (node == null) {
            return "";
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            return ((Element) node).getAttributeNS(SCHEVAL_NS, "location");
        }
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
            var owner = ((Attr) node).getOwnerElement();
            return owner == null ? "" : owner.getAttributeNS(SCHEVAL_NS, "location");
        }
        var parent = node.getParentNode();
        if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            return ((Element) parent).getAttributeNS(SCHEVAL_NS, "location");
        }
        return "";
    }

    private void configureFactories() {
        saxFact.setNamespaceAware(true);
        docbldFact.setNamespaceAware(true);
        docbldFact.setXIncludeAware(false);
        docbldFact.setExpandEntityReferences(false);

        setFeature(saxFact, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeature(docbldFact, XMLConstants.FEATURE_SECURE_PROCESSING, true);

        setFeature(saxFact, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setFeature(saxFact, "http://xml.org/sax/features/external-general-entities", false);
        setFeature(saxFact, "http://xml.org/sax/features/external-parameter-entities", false);
        setFeature(saxFact, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        setFeature(docbldFact, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setFeature(docbldFact, "http://xml.org/sax/features/external-general-entities", false);
        setFeature(docbldFact, "http://xml.org/sax/features/external-parameter-entities", false);
        setFeature(docbldFact, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        setTransformerFeature(transFact, XMLConstants.FEATURE_SECURE_PROCESSING, true);
    }


    private void setFeature(SAXParserFactory fact, String feature, boolean value) {
        try {
            fact.setFeature(feature, value);
        } catch (Exception ex) {
            LOG.debug("Unable to set SAXParserFactory feature {}={} : {}", feature, value, ex.getMessage());
        }
    }

    private void setFeature(DocumentBuilderFactory fact, String feature, boolean value) {
        try {
            fact.setFeature(feature, value);
        } catch (Exception ex) {
            LOG.debug("Unable to set DocumentBuilderFactory feature {}={} : {}", feature, value, ex.getMessage());
        }
    }

    private void setTransformerFeature(TransformerFactory fact, String feature, boolean value) {
        try {
            fact.setFeature(feature, value);
        } catch (Exception ex) {
            LOG.debug("Unable to set TransformerFactory feature {}={} : {}", feature, value, ex.getMessage());
        }
    }

    private void setAttribute(TransformerFactory fact, String name, String value) {
        try {
            fact.setAttribute(name, value);
        } catch (Exception ex) {
            LOG.debug("Unable to set TransformerFactory attribute {}={} : {}", name, value, ex.getMessage());
        }
    }

    private void deleteRecursively(Path root) {
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ex) {
                        LOG.warn("Could not delete temporary file {}: {}", p, ex.getMessage());
                    }
                });
        } catch (IOException ex) {
            LOG.warn("Could not walk temporary directory {}: {}", root, ex.getMessage());
        }
    }

    private static String displayNameForSystemId(String systemId) {
        if (systemId == null || systemId.isBlank()) {
            return "<unknown>";
        }
        try {
            var uri = URI.create(systemId);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                var path = Path.of(uri);
                var name = path.getFileName();
                return name == null ? path.toString() : name.toString();
            }
        } catch (Exception ex) {
            // fall through and use raw systemId
        }
        return systemId;
    }

    private static class LocationFilter extends XMLFilterImpl {
        private Locator loc;
        private final String srcDisplayName;

        LocationFilter(XMLReader reader, String systemId) {
            super(reader);
            this.srcDisplayName = displayNameForSystemId(systemId);
        }

        @Override
        public void setDocumentLocator(Locator loc) {
            super.setDocumentLocator(loc);
            this.loc = loc;
        }

        @Override
        public void startElement(String uri, String lname, String qname, Attributes atts) throws SAXException {
            int line = loc == null ? -1 : loc.getLineNumber();
            int col = loc == null ? -1 : loc.getColumnNumber();
            var locstr = srcDisplayName + ":" + line + ":" + col;
            var attrs = new Attributes2Impl(atts);
            attrs.addAttribute(SCHEVAL_NS, "location", "scheval:location", "CDATA", locstr);
            super.startElement(uri, lname, qname, attrs);
        }
    }
}
