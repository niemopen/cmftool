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
package org.mitre.niem.cmf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mitre.niem.xml.LanguageString;
import org.mitre.niem.xsd.NamespaceKind;

/**
 * A class for a Namespace object in a NIEM model.
 * 
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Namespace extends CMFObject implements Comparable<Namespace> {
    
    private static final List<LanguageString> EMPTY_LSL = List.of();

    public Namespace () { }
    public Namespace (String prefix, String uri) { 
        super(); this.prefix = prefix; this.uri = uri; 
    }
    
    @Override
    public int getType () { return CMF_NAMESPACE; }
    
    private Model model =  null;
    private String prefix = "";                                     // cmf:NamespacePrefixText
    private String uri = "";                                        // cmf:NamespaceURI
    private String docFP = "";                                      // cmf:DocumentFilePathText
    private String kindCode = "";                                   // cmf:NamespaceCategoryCode
    private String version = "";                                    // cmf:NamespaceVersionText
    private String archVersion = "";                                // cmf:ArchitectureVersionName
    private String lang = "";                                       // cmf:NamespaceLanguageName
    private boolean isModelF = false;                               // has a model schema document CTA
    private final List<LanguageString> docL = new ArrayList<>();    // cmf:DocumentationText
    private final List<String> ctargL = new ArrayList<>();          // cmf:ConformanceTargetURI
    private final List<LocalTerm> locTermL = new ArrayList<>();     // cmf:LocalTerm
    private final List<AugmentRecord> augL = new ArrayList<>();     // cmf:AugmentationRecord
    private final Map<String,List<LanguageString>> idocs = new HashMap<>();      // cmf:ImportDocumentation
    
    public Model model ()                           { return model; }
    public String prefix ()                         { return prefix; }
    @Override
    public String uri ()                            { return uri; }
    public String documentFilePath ()               { return docFP; }
    public String kindCode ()                       { return kindCode; }
    public String version ()                        { return version; }
    public String archVersion ()                    { return archVersion; }
    public String language ()                       { return lang; }
    public List<LanguageString> docL ()             { return docL; }
    public List<LanguageString> idocL (String nsU)  { return idocs.getOrDefault(nsU, EMPTY_LSL); }
    public List<String> ctargL ()                   { return ctargL; }
    public List<LocalTerm> locTermL ()              { return locTermL; }
    public List<AugmentRecord> augL ()              { return augL; }
    public boolean isModelNS ()                     { return isModelF; }
    public boolean isExternal ()                    { return "EXTERNAL".equals(kindCode); }
    public Map<String,List<LanguageString>> idocs() { return idocs; }
    
    public void setDocumentFilePath (String s)      { docFP = s; }
    public void setKindCode (String s)              { kindCode = s; }
    public void setVersion (String s)               { version = s; }
    public void setArchVersion (String s)           { archVersion = s; }
    public void setLanguage (String s)              { lang = s; }
    
    public void addDocumentation (String doc, String lang) {
        docL.add(new LanguageString(doc, lang));
    }
    public void addImportDocumentation (ImportDoc idoc) {
        var nsU  = idoc.nsU();
        for (var ls : idoc.docL())
            addImportDocumentation(nsU, ls);
    }
    
    public void addImportDocumentation (String nsU, LanguageString ls) {
        var dL = idocs.get(nsU);
        if (null == dL) {
            dL = new ArrayList<>();
            idocs.put(nsU, dL);
        }
        dL.add(ls);
    }
    
    public void addConformanceTarget (String ct) {
        ctargL.add(ct);
        if (NamespaceKind.isModelCTA(ct)) isModelF = true;
    }
    public void setConformanceTargets (String ctarg) {
        ctargL.clear();
        for (var ct : ctarg.trim().split("\\s+")) addConformanceTarget(ct);
    }
    public void setConformanceTargets (List<String> ctL) {
        ctargL.clear();
        isModelF = false;
        for (var ct : ctL) addConformanceTarget(ct);
    }
    public void addLocalTerm (LocalTerm lt)     { locTermL.add(lt); }
    public void addAugmentRecord (AugmentRecord a) { augL.add(a); }
    
    
    // Following routines ensure each namespace prefix maps to at most one URI.
    public void setModel (Model m) throws CMFException {
        if (null == m) return;
        if (null == prefix || null == uri) 
            throw new CMFException("can't add uninitialzed Namespace to a Model");
        var mpre = m.nsUToNSprefix(uri);
        if (null != mpre && !mpre.equals(prefix)) {
            throw new CMFException(String.format(
                "can't add namespace %s:%s to model (%s already assigned to %s)",
                prefix, uri, prefix, m.prefixToNSuri(prefix)));
        }
        model = m;
    }    
    
    public void setPrefix (String p) throws CMFException {
        prefix = p;
        if (null == model || null == uri) return;
        var muri = model.prefixToNSuri(p);
        if (null != muri && !uri.equals(muri))
            throw new CMFException(String.format(
                "can't change prefix of URI %s to %s (already assigned to %s)",
                uri, p, model.prefixToNSuri(p)));
    }
    
    public void setURI (String u) throws CMFException {
        uri = u;
        if (null == model ||  null == prefix) return;
        var mpr = model.nsUToNSprefix(u);
        if (null != mpr && !prefix.equals(mpr))
            throw new CMFException(String.format(
                "can't change URI of prefix %s to %s (already assigned to %s)",
                prefix, u, model.nsUToNSprefix(u)));    
    }
    
    
    @Override
    public boolean addChild(String eln, String loc, CMFObject child) throws CMFException {
        return child.addToNamespace(eln, loc, this);
    }
    
    @Override
    public boolean addToComponent (String eln, String loc, Component c) {
        c.setNamespace(this);
        return true;
    }
    
    @Override
    public boolean addToDatatype (String eln, String loc, Datatype dt) {
        return addToComponent(eln, loc, dt);
    }
    
    @Override
    public boolean addToListType (String eln, String loc, ListType lt) {
        return addToDatatype(eln, loc, lt);
    }

    @Override
    public boolean addToModel (String eln, String loc, Model m) throws CMFException { 
        m.addNamespace(this);
        return true;
    }
    
    @Override
    public boolean addToRestriction (String eln, String loc, Restriction r) {
        return addToDatatype(eln, loc, r);
    }
    
    @Override
    public boolean addToUnion (String eln, String loc, Union u) {
        return addToDatatype(eln, loc, u);
    }

    @Override
    public int compareTo(Namespace o) {
        return this.prefix.compareToIgnoreCase(o.prefix);
    }

}
