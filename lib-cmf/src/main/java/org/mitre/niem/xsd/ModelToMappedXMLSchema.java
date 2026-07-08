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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Mapping;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.NamespaceMap;
import org.mitre.niem.cmf.ObjectProperty;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.utility.MapToSet;
import static org.mitre.niem.xml.XMLDocument.qnToName;
import static org.mitre.niem.xml.XMLDocument.qnToPrefix;
import org.w3c.dom.Document;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToMappedXMLSchema {

    private final Model model;                                      // actual model object; don't change
    private final Mapping map;                                      // canonical to simple name map, if provided
    private final Set<Component> compS;                             // model component subset for schema pile
    private final NamespaceMap nsmap;
    
    public ModelToMappedXMLSchema (Model m, Mapping map) {
        this.model = m;
        this.map = map;
        this.compS = m.componentSet();
        this.nsmap = new NamespaceMap(m.nsmap());
    }
    
    public ModelToMappedXMLSchema (Model m, Mapping map, Set<ObjectProperty> propS) {
        this.model = m;
        this.map = map;
        this.compS = m.messageComponents(propS);
        this.nsmap = new NamespaceMap(m.nsmap());
    }
    
    private final MapToSet<String,Component> nsCompS = new MapToSet<>();
    

    public void writeModelXSD (File outD) {
//        
//        // What namespaces do we need?
//        // What components are in those namespaces?
//        var nsNeedS = new HashSet<String>();
//        for (var c : compS) {
//            var cU = c.uri();
//            if (cU.endsWith("Literal")) continue;
//            if (map.isMappedU(cU)) {
//                var mapNSU = map.uriToTargetNSU(cU);
//                var mapQN  = map.uriToTargetQN(cU);
//                var mapPre = qnToPrefix(mapQN);
//                if (!mapPre.isBlank()) {
//                    nsmap.assignPrefix(mapPre, mapNSU);
//                }
//                nsNeedS.add(mapNSU);
//                nsCompS.add(mapNSU, c);
//            }
//            else {
//                nsmap.assignPrefix(c.namespace().prefix(), c.namespaceURI());
//                nsNeedS.add(c.namespaceURI());
//                nsCompS.add(c.namespaceURI(), c);                    
//            }
//        }
//
//        // Create schema document for namespaces
//        var nsDocs = new HashMap<String,Document>();
//        for (var nsU : nsNeedS) {
//            var doc = createSchemaDocument(nsU);
//            nsDocs.put(nsU, doc);
//        }
    }
    
    private Document createSchemaDocument (String nsU) {
        
        
        return null;
    }

    private void createComplexTypes (Document doc, String nsU) {
        for (var c : nsCompS.get(nsU)) {
            if (c instanceof ClassType ct) {
                if (ct.hasSimpleContent()) createCSCtype(doc, ct);
//                else createCCCtype(doc, ct);
            }
        }
    }
    
    private void createCSCtype (Document doc, ClassType ct) {
        
    }
    
}
