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
package org.mitre.niem.rdf;

import java.io.PrintWriter;
import static org.mitre.niem.NIEMConstants.OWL_NS_URI;
import static org.mitre.niem.NIEMConstants.RDFS_NS_URI;
import static org.mitre.niem.NIEMConstants.RDF_NS_URI;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToRDF {
    
    private final Model m;
    
    public ModelToRDF (Model m) {
        this.m = m;
    }
    
    public void writeRDF (PrintWriter ow) {
        writeNamespaces(ow);
        writeProperties(ow);
        writeClasses(ow);
    }
    
    private void writeNamespaces (PrintWriter ow) {
        for (Namespace ns : m.namespaceSet()) {
            ow.print(String.format("@prefix %-15s <%s> .\n", 
                    ns.getNamespacePrefix()+":", 
                    ns.getNamespaceURI()));
        }
        ow.print("\n");
        ow.print(String.format("@prefix %-15s <%s> .\n", "owl:", OWL_NS_URI));
        ow.print(String.format("@prefix %-15s <%s> .\n", "rdfs:", RDFS_NS_URI));
        ow.print(String.format("@prefix %-15s <%s> .\n", "rdf:", RDF_NS_URI));        
    }
    
    private void writeProperties (PrintWriter ow) {
        for (Property p : m.propertySet()) {
            ow.print("\n");
            ow.print(p.getQName());
            if (null != p.getClassType()) {
                ow.print("\n    a owl:ObjectProperty");
                if (null != p.getSubPropertyOf()) {
                    ow.print(" ;\n    rdfs:subPropertyOf " + p.getSubPropertyOf().getQName());
                }
                ow.print(" ;\n    rdfs:range " + p.getClassType().getQName());
            }
            else if (null != p.getDatatype()) {
                ow.print("\n    a owl:DataProperty");
                ow.print(" ;\n    rdfs:range " + p.getDatatype().getBaseType().getQName());
            }
            if (null != p.getDefinition()) {
                ow.print(" ;\n    rdfs:comment \"" + p.getDefinition() + "\"");               
            }
            ow.println(" .");
        }
    }
    
    private void writeClasses (PrintWriter ow) {
        for (ClassType ct : m.classTypeSet()) {
            ow.print("\n");
            ow.print(ct.getQName());
            ow.print("\n    a owl:Class");
            if (null != ct.getExtensionOfClass()) {
                ow.print(" ;\n    rdfs:subClassOf " + ct.getExtensionOfClass().getQName());
            }
            if (null != ct.getDefinition()) {
                ow.print(" ;\n    rdfs:comment \"" + ct.getDefinition() + "\"");               
            }       
            ow.println(" .");
        }
    }
    

    
}
