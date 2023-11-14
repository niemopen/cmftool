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
    private String definition = null;
    private int nsKind = NSK_UNKNOWN;
    
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
    public void setDefinition (String s)      { definition = s; }
    public void setKind (int k)               { nsKind = k; }
    public void setKind (String c)            { nsKind = namespaceCode2Kind(c); }

    public Model getModel ()                  { return model; }
    public String getNamespaceURI ()          { return namespaceURI; }
    public String getNamespacePrefix ()       { return namespacePrefix; }
    public String getDefinition ()            { return definition; }
    public int getKind ()                     { return nsKind; }
    public boolean isExternal ()              { return nsKind == NSK_EXTERNAL; }
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
