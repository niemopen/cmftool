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
package org.mitre.niem.cmf;

import java.util.ArrayList;
import java.util.List;
import static org.mitre.niem.cmf.NamespaceKind.NSK_EXTERNAL;
import static org.mitre.niem.cmf.NamespaceKind.NSK_UNKNOWN;
import static org.mitre.niem.cmf.NamespaceKind.namespaceCode2Kind;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class Namespace extends ObjectType implements Comparable<Namespace> {
    
    private Model model = null;             // Namespace objects know the Model they are part of     
    private final List<AugmentRecord> augmentList = new ArrayList<>();
    private final List<LocalTerm> localTermList   = new ArrayList<>();
    private String namespaceURI = null;
    private String namespacePrefix = null;
    private String documentation = null;
    private int nsKind = NSK_UNKNOWN;
    private String confTargs = null;            // conformance target assertions
    private String filepath = null;             // relative path from pile root
    private String niemVersion = null;          // eg. "3","4","5","6"
    private String schemaVersion = null;        // from @version
    private String language = null;             // from @xml:lang
    private String importDoc = null;            // documentation on xs:import of this namespace;
                                                // if different imports have different doc, too bad!
    
    public Namespace () { super(); }
     
    public Namespace (String p, String nsuri) {
        super();
        namespacePrefix = p ;
        namespaceURI = nsuri;
    }
    
    @Override
    public boolean isModelChild ()            { return true; }      // Namespace objects are model children
    
    public void setNamespaceURI (String nuri) throws CMFException {
        var m = getModel();
        if (null != m) m.namespaceURIChange(namespacePrefix, namespaceURI, nuri);
        namespaceURI = nuri;
    }
    
    public void setNamespacePrefix (String npre) throws CMFException { 
        var m = getModel();
        if (null != m) m.namespacePrefixChange(namespaceURI, namespacePrefix, npre);
        namespacePrefix = npre;
    }
    
    void setModel (Model m)                   { model = m; }
    public void setDocumentation (String s)   { documentation = s.strip().replaceAll("\\s+", " "); }
    public void setKind (int k)               { nsKind = k; }
    public void setKind (String c)            { nsKind = namespaceCode2Kind(c); }
    public void setConfTargets (String s)     { confTargs = s; }
    public void setFilePath (String s)        { filepath = s; }
    public void setNIEMversion (String s)     { niemVersion = s; }
    public void setSchemaVersion (String s)   { schemaVersion = s; }
    public void setLanguage (String s)        { language = s; }
    
    public Model getModel ()                  { return model; }
    public String getNamespaceURI ()          { return namespaceURI; }
    public String getNamespacePrefix ()       { return namespacePrefix; }
    public String getDocumentation ()         { return documentation; }
    public int getKind ()                     { return nsKind; }
    public boolean isExternal ()              { return nsKind == NSK_EXTERNAL; }
    public String getConfTargets ()           { return confTargs; }
    public String getFilePath ()              { return filepath; }
    public String getNIEMVersion ()           { return niemVersion; }
    public String getSchemaVersion ()         { return schemaVersion; }
    public String getLanguage ()              { return language; }
    public String getImportDoc ()             { return importDoc; }
    public List<AugmentRecord> augmentList()  { return augmentList; }
    public List<LocalTerm> localTermList()    { return localTermList; }
    
    public void addAugmentRecord (AugmentRecord r) {
        this.augmentList.add(r);
    }
    
    public void removeAugmentRecord (AugmentRecord r) {
        this.augmentList.remove(r);
    }
    
    public AugmentRecord findAugmentRecord (ClassType ct, Property p) {
        for (var ar : augmentList)
            if (ar.getClassType() == ct && ar.getProperty() == p)
                return ar;
        return null;
    }
    
    public void replaceAugmentRecord (AugmentRecord or, AugmentRecord nr) {
        int index = this.augmentList.indexOf(or);
        if (index < 0) return;
        this.augmentList.set(index, nr);
    }    
    
    public void addLocalTerm (LocalTerm lt) {
        this.localTermList.add(lt);
    }
    
    // Enforces guarantee that each namespace in a model has a unique prefix
    @Override
    public void addToModel (Model m) throws CMFException {
        m.addNamespace(this);
    }   
    
    @Override
    public int compareTo (Namespace o) {
        return this.namespacePrefix.compareToIgnoreCase(o.namespacePrefix);
    }    
}
