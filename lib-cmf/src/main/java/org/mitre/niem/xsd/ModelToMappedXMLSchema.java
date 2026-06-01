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
package org.mitre.niem.xsd;

import java.io.File;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import org.mitre.niem.cmf.CMFException;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.DataProperty;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.ObjectProperty;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToMappedXMLSchema {
    
    private final Model m;
    private final Mapping map;
    private Set<String> namespaceURIs = null;
    private ObjectProperty messageProp = null;
    boolean singleNamespace = false;
    
    public ModelToMappedXMLSchema (Model m) {
        this.m = m;
        this.map = new Mapping();
    }
    
    public ModelToMappedXMLSchema(Model m, Mapping map) {
        this.m = m;
        this.map = map;
    }
    
    // The URI of a model component ia either a URN or contains "://".
    public void setMessageElement (String qnOrURI) throws CMFException {
        if (null == qnOrURI) {
            messageProp = null;
            return;
        }
        namespaceURIs = null;
        if (qnOrURI.regionMatches(true, 0, "urn:", 0, 4) || qnOrURI.contains("://")) {
//            var mQorU = map.mappedUtoU(qnOrURI);
//            messageProp = m.uriToObjectProperty(mQorU);
        }
        else {
//            var mQorU = map.mappedQNtoQN(qnOrURI);
//            messageProp = m.qnToObjectProperty(mQorU);
        }
        if (null == messageProp)
            throw new CMFException(qnOrURI + " is not a model QName or URI");
    }
    
//    public void setSingleNamespace (boolean f) throws CMFException  {
//        if (f) {
//        computeNamespaceSet();
//        if (namespaceURIs.size() > 1)
//            throw new CMFException("More than one namespace after mapping");
//        }
//        singleNamespace = f;
//    }
    
    public void writeModelXSD (File outD) {
        
    }
    
    protected void computeNamespaceSet () {
        if (null != namespaceURIs) return;
        namespaceURIs = new HashSet<>();
        var done = new HashSet<Component>();
        var todo = new ArrayDeque<Component>();
        if (null != messageProp) todo.add(messageProp);
        else {
            for (var c : m.componentList()) todo.add(c);
        }
        while (!todo.isEmpty()) {
            var c  = todo.pop();
            if (done.contains(c)) continue;
            if (c.isAbstract()) continue;
            var cU = c.uri();
//            var cnsU = map.uriToMappedNS(cU);
//            namespaceURIs.add(cnsU);
            done.add(c);
            if (c instanceof DataProperty dp)        todo.add(dp.datatype());
            else if (c instanceof ObjectProperty op) todo.add(op.classType());
            else if (c instanceof Datatype dt)       todo.add(dt.base());
            else if (c instanceof ClassType ct) {
                for (var pa : ct.propL()) {
                    todo.add(pa.property());
                }
            }
        }
    }
    
}
