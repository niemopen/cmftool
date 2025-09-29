/*    static final Logger LOG = LogManager.getLogger(NamespaceKind.class);
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
package org.mitre.niem.xml;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mitre.niem.utility.ResourceManager;
import static org.mitre.niem.utility.URIfuncs.URIStringToFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import static org.w3c.dom.Node.ELEMENT_NODE;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.Attributes2Impl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * A class to execute ISO Schematron rules on an XML document, and to transform
 * the SVRL output into useful text including document line numbers.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Schematron {
    static final Logger LOG = LogManager.getLogger(Schematron.class);
    
    public static String ISO_DSDL     = "iso_dsdl_include.xsl";
    public static String ISO_ABSTRACT = "iso_abstract_expand.xsl";
    public static String ISO_SVRL     = "iso_svrl_for_xslt2.xsl";
    public static String ISO_SKEL     = "iso_schematron_skeleton_for_saxon.xsl";
    public static String XSLT_PP      = "xsltpp.xsl";
    public static String SVRL_NS      = "http://purl.oclc.org/dsdl/svrl";
    public static String SCHEVAL_NS   = "http://xml.niem.mitre.org/LOC/";
    
    private final Processor saxonProc;
    private final XsltCompiler  saxonComp;
    private final SAXParserFactory saxFact;
    private final TransformerFactory transFact;
    private final DocumentBuilderFactory docbldFact;
    private final XPathFactory xpathFact;
    private XsltTransformer dsdlTrans = null;           // expand sch:include elements
    private XsltTransformer abstractTrans = null;       // expand abstract patterns and rules
    private XsltTransformer svrlTrans = null;           // final stage in SCH to XSLT
    private XsltTransformer xsltPPTrans = null;         // expand xsl:include in the compiled XSLT
    
    public Schematron () throws SaxonApiException {
        saxonProc  = new Processor(false);               // free version; license not required
        saxonComp  = saxonProc.newXsltCompiler();
        saxFact    = SAXParserFactory.newInstance();
        transFact  = TransformerFactory.newInstance();
        docbldFact = DocumentBuilderFactory.newInstance();
        xpathFact  = XPathFactory.newInstance();
        saxFact.setNamespaceAware(true); 
        docbldFact.setNamespaceAware(true); 
    }
    
    /**
     * Compiles a Schematron document into a transformer object.  It is often important
     * to set the system ID of the source.
     * @param src - Schematron source
     * @return XsltTransformer
     * @throws SaxonApiException 
     */
    public XsltTransformer compileSchematron (StreamSource src) throws SaxonApiException {
        var ow = new StringWriter();
        compileSchematron(src, ow);
        var sr = new StringReader(ow.toString());
        var ss = new StreamSource(sr);
        ss.setSystemId(src.getSystemId());
        var tr = saxonComp.compile(ss).load();
        return tr;
    }
    
    /**
     * Compiles a Schematron document into XSLT.  It is often important
     * to set the system ID of the source.
     * @param src - Schematron source
     * @param ow - Writer  to recieve XSLT output
     * @throws SaxonApiException 
     */
    public void compileSchematron (StreamSource src, Writer ow) throws SaxonApiException {
        prepareSCHTransforms();
        var srcID = src.getSystemId();
        var strW  = new StringWriter();
        applyXslt(src, dsdlTrans, strW);
        var res = strW.toString();    
        
        var srcR  = new StringReader(strW.toString());
        var nsrc  = new StreamSource(srcR);
        nsrc.setSystemId(srcID);
        strW = new StringWriter();
        applyXslt(nsrc, abstractTrans, strW);

        srcR = new StringReader(strW.toString());
        nsrc = new StreamSource(srcR);
        nsrc.setSystemId(srcID);
        strW = new StringWriter();
        applyXslt(nsrc, svrlTrans, strW);
        
        srcR = new StringReader(strW.toString());
        nsrc = new StreamSource(srcR);
        nsrc.setSystemId(srcID);
        applyXslt(nsrc, xsltPPTrans, ow);
    }
    
    /**
     * Applies an XSLT transformation to an XML document, producing SVRL.
     * @param src - XML document
     * @param trans - XSLT transformation
     * @param ow - Writer to receive SVRL output
     * @throws SaxonApiException 
     */
    public void applyXslt (StreamSource src, XsltTransformer trans, Writer ow) throws SaxonApiException {
        var oser = saxonProc.newSerializer(ow);
        trans.setDestination(oser);
        trans.setSource(src);
        trans.transform();
    }
    
    /**
     * Produces useful messages from the SVRL generated by Schematron applied to the 
     * XML document. Each assertion or message in the SVRL contains the XPath to the 
     * corresponding element in the XML document, which is then transformed into a
     * readable message that includes the line and column number of that element.
     * @param svrl - SVRL results
     * @param xml - XML document from which SVRL was generated
     * @param msgs - Writer to receive useful messages
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws TransformerException 
     */
    public void SVRLtoMessages (InputSource svrl, InputSource xml, Writer msgs) throws ParserConfigurationException, SAXException, IOException, TransformerException {       
        var db    = docbldFact.newDocumentBuilder();
        var sdoc  = db.parse(svrl);
        var sroot = sdoc.getDocumentElement();
        var adoc  = annotateDocument(xml);

        var asserts = sroot.getElementsByTagNameNS(SVRL_NS, "failed-assert");
        var reports = sroot.getElementsByTagNameNS(SVRL_NS, "successful-report");
        for (int i = 0; i < asserts.getLength(); i++) processSVRLelement(adoc, asserts.item(i), "ERROR", msgs);
        for (int i = 0; i < reports.getLength(); i++) processSVRLelement(adoc, reports.item(i), "WARN ", msgs);
    }
    
    private void processSVRLelement (Document adoc, Node node, String kind, Writer msgs) throws IOException {
        var e     = (Element)node;                  // assert or report element in SVRL document
        var locXP = e.getAttribute("location");     // XPath to element in evaluated XML document
        var txts  = e.getElementsByTagNameNS(SVRL_NS, "text");
        var xpath = xpathFact.newXPath();
        NodeList nds = null;
        try {
            var xpr = xpath.compile(locXP);
            nds = (NodeList)xpr.evaluate(adoc, XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            LOG.warn("Can't parse XPath '{} in SVRL", locXP, ex.getMessage());
            return;
        }
        var locS = "";
        for (int i = 0; locS.isEmpty() && i < nds.getLength(); i++) {
            var inode = nds.item(i);
            if (ELEMENT_NODE != inode.getNodeType()) continue;
            var ie = (Element) inode;
            locS = ie.getAttributeNS(SCHEVAL_NS, "location");
        }
        for (int i = 0; i < txts.getLength(); i++) {
            var te = (Element)txts.item(i);
            var ts = te.getTextContent();
            msgs.write(String.format("%s %s -- %s\n", kind, locS, ts));
        }
    }

    /**
     * Returns a Document object in which each element in the source document is
     * annotated with an attribute containing its line number.
     * @param src - XML source
     * @return - annotated Document
     * @throws ParserConfigurationException
     * @throws TransformerConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws TransformerException 
     */
    public Document annotateDocument (InputSource src) throws ParserConfigurationException, TransformerConfigurationException, SAXException, IOException, TransformerException {       
        var saxp  = saxFact.newSAXParser();
        var trans = transFact.newTransformer();
        var srcID = src.getSystemId();
        var xmlrd = saxp.getXMLReader();
        var locfl = new LocationFilter(xmlrd, srcID);
        var saxs  = new SAXSource(locfl, src);
        var domrs = new DOMResult();
        trans.transform(saxs, domrs);
        var root  = domrs.getNode();
        return (Document)root;
    }      
    
    private class LocationFilter extends XMLFilterImpl {
        private Locator loc = null;
        private String srcFileName;

        LocationFilter(XMLReader r, String srcID) {
            super(r);
            srcFileName = URIStringToFile(srcID).getName();
        }
        @Override
        public void setDocumentLocator(Locator loc) {
            super.setDocumentLocator(loc);
            this.loc = loc;
        }
        @Override
        public void startElement(String uri, String lname, String qname, Attributes atts) throws SAXException {
            var locstr = srcFileName + ":" + loc.getLineNumber() + ":" + loc.getColumnNumber();
            var attrs  = new Attributes2Impl(atts);
            attrs.addAttribute(SCHEVAL_NS, "location", "scheval:location", "CDATA", locstr);
            super.startElement(uri, lname, qname, attrs);
        }
    }    

    // Prepare XSLT transformers from the four ISO schematron rule files.
    // The ISO schematron files need to be in the same directory so that the 
    // xsl:includes will work, but all we have are resources in the JAR.  So
    // begin by copying thos resource streams into a temp directory.  Blech.
    private void prepareSCHTransforms () throws SaxonApiException {
        if (null != dsdlTrans) return;
        Path tmpP = null;
        try {
            tmpP = Files.createTempDirectory("Schematron");
        } catch (IOException ex) {
            LOG.error("Can't create temp directory for ISO XSLT files: {}", ex.getMessage());
        }
        var tmpF  = tmpP.toFile();
        var dsdlF = new File(tmpF, ISO_DSDL);
        var absF =  new File(tmpF, ISO_ABSTRACT);
        var svrlF = new File(tmpF, ISO_SVRL);
        var skelF = new File(tmpF, ISO_SKEL);
        var xsltF = new File(tmpF, XSLT_PP);
        var rmgr  = new ResourceManager(this.getClass());
        try {
            rmgr.copyResourceToFile("/sch/" + ISO_DSDL, dsdlF);
            rmgr.copyResourceToFile("/sch/" + ISO_ABSTRACT, absF);
            rmgr.copyResourceToFile("/sch/" + ISO_SVRL, svrlF);
            rmgr.copyResourceToFile("/sch/" + ISO_SKEL, skelF);
            rmgr.copyResourceToFile("/sch/" + XSLT_PP, xsltF);
        } catch (IOException ex) {
            LOG.error("Can't create schematron transform files: {}", ex.getMessage());
        }

        
        dsdlTrans = saxonComp.compile(dsdlF).load();
        abstractTrans = saxonComp.compile(absF).load();
        svrlTrans = saxonComp.compile(svrlF).load();    
        svrlTrans.setParameter(new QName("allow-foreign"), new XdmAtomicValue("true"));
        xsltPPTrans = saxonComp.compile(xsltF).load();

        try {
            FileUtils.deleteDirectory(tmpF);
        } catch (IOException ex) {
            LOG.warn("Could not delete ISO XSLT temp directory {}: {}",
                    tmpF.toString(), ex.getMessage());
        }
    }
}
