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
import java.util.ArrayList;
import java.util.List;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.mitre.niem.NIEMConstants.OWL_NS_URI;
import static org.mitre.niem.NIEMConstants.RDFS_NS_URI;
import static org.mitre.niem.NIEMConstants.RDF_NS_URI;
import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.RestrictionOf;

/**
 *
 * @author Scott Renner
 * <a href="mailto:sar@mitre.org">sar@mitre.org</a>
 */
public class ModelToOWL {
    
    private final Model m;
    
    public ModelToOWL (Model m) {
        this.m = m;
    }
    
    public void writeRDF (PrintWriter ow) {
        writeNamespaces(ow);
        writeProperties(ow);
        writeClasses(ow);
        writeDatatypes(ow);
    }
    
    private void writeNamespaces (PrintWriter ow) {
        for (Namespace ns : m.getNamespaceList()) {
            ow.print(String.format("@prefix %-15s <%s#> .\n", 
                    ns.getNamespacePrefix()+":", 
                    ns.getNamespaceURI()));
        }
        ow.print("\n");
//        ow.print(String.format("@prefix %-15s <%s#> .\n", "cmf:", CMF_NS_URI));
        ow.print(String.format("@prefix %-15s <%s#> .\n", "owl:", OWL_NS_URI));
        ow.print(String.format("@prefix %-15s <%s#> .\n", "rdfs:", RDFS_NS_URI));
        ow.print(String.format("@prefix %-15s <%s#> .\n", "rdf:", RDF_NS_URI));        
        ow.print(String.format("@prefix %-15s <%s#> .\n", "xsd:", W3C_XML_SCHEMA_NS_URI)); 
    }
    
    private String getPropertyKind (Property p) {
        String rv = null;
        if (null != p.getClassType()) rv = "owl:ObjectProperty";
        else if (null != p.getDatatype()) rv = "owl:DataProperty";
        else {
            for (Component oc : m.getComponentList()) {
                Property op = oc.asProperty();
                if (null == op) continue;
                if (op.getSubPropertyOf() == p) {
                    String rv2 = getPropertyKind(op);
                    if (null != rv2) {
                        rv = rv2;
                        break;
                    }
                }
            }
        }
        return rv;
    }
    
    private void writeProperties (PrintWriter ow) {
        for (Component c : m.getComponentList()) {
            Property p = c.asProperty();
            if (null == p) continue;
            
            // What kind of property? For abstracts, subproperties decide
            // Abstract with no subproperty is omitted
            String propKind = getPropertyKind(p);
            if (null == p) continue;
            ow.print("\n");
            ow.print(p.getQName());            
            if (null != p.getClassType()) {
                ow.print("\n    a owl:ObjectProperty");
                if (null != p.getSubPropertyOf()) {
                    ow.print(" ;\n    rdfs:subPropertyOf " + p.getSubPropertyOf().getQName());
                }
                ow.print(" ;\n    rdfs:range " + componentQName(p.getClassType()));
            }
            else if (null != p.getDatatype()) {
                ow.print("\n    a owl:DataProperty");
                ow.print(" ;\n    rdfs:range " + componentQName(p.getDatatype()));
            }
            else {
                ow.print("\n    a " + propKind);
            }
            if (null != p.getDocumentation()) {
                ow.print(" ;\n    rdfs:comment \"" + p.getDocumentation() + "\"");               
            }
            ow.println(" .");
        }
    }
    
    private void writeClasses (PrintWriter ow) {
        for (Component c : m.getComponentList()) {
            ClassType ct = c.asClassType();
            if (null == ct) continue;
            ow.print("\n");
            ow.print(ct.getQName());
            ow.print("\n    a owl:Class");
            if (null != ct.getExtensionOfClass()) {
                ow.print(" ;\n    rdfs:subClassOf " + ct.getExtensionOfClass().getQName());
            }
            if (null != ct.getDocumentation()) {
                ow.print(" ;\n    rdfs:comment \"" + ct.getDocumentation() + "\"");               
            }
            for (HasProperty hp : ct.hasPropertyList()) {
                if (0 < hp.minOccurs()) {
                    ow.print(" ;\n    owl:subclassOf [");
                    ow.print("\n        a owl:Restriction");
                    if (hp.minOccurs() == hp.maxOccurs()) 
                        ow.print(" ;\n        owl:cardinality \"" + hp.minOccurs() + "\"^^xsd:nonNegativeInteger");
                    else {
                        ow.print(" ;\n        owl:minCardinality \"" + hp.minOccurs() + "\"^^xsd:nonNegativeInteger");
                        if (!hp.maxUnbounded()) 
                            ow.print(" ;\n        owl:maxCardinality \"" + hp.maxOccurs() + "\"^^xsd:nonNegativeInteger");
                    }
                    ow.print(" ;\n        owl:onProperty " + hp.getProperty().getQName());
                    ow.print("\n    ]");
                }
            }
            ow.println(" .");
        }
    }
    
    private void writeDatatypes (PrintWriter ow) {
        for (Component c : m.getComponentList()) {
            Datatype dt = c.asDatatype();
            if (null == dt) continue;
            if (W3C_XML_SCHEMA_NS_URI.equals(dt.getNamespace().getNamespaceURI())) continue;
            if (null != dt.getListOf()) writeListOfDatatype(dt, ow);
            else if (null != dt.getUnionOf()) writeUnionOfDatatype(dt, ow);
            else if (null != dt.getRestrictionOf()) writeRestrictionOfDatatype(dt, ow);
        }
    }
    
    private void writeListOfDatatype (Datatype dt, PrintWriter ow) {
        
    }
    
    private void writeUnionOfDatatype (Datatype dt, PrintWriter ow) {
        
    }    
    
    private void writeRestrictionOfDatatype (Datatype dt, PrintWriter ow) {
        RestrictionOf r = dt.getRestrictionOf();
        Datatype bdt    = r.getDatatype();
        List<Facet> fl  = r.getFacetList();
        ow.print("\n");
        ow.print(dt.getQName());
        ow.print("\n    a rdfs:Datatype");
        if (null != dt.getDocumentation()) ow.print(" ;\n    rdfs:comment \"" + dt.getDocumentation() + "\"");
        if (null != bdt) {
            ow.print(" ;\n    owl:equivalentClass ");
            if (fl.isEmpty()) ow.print(componentQName(bdt));
            else {
                ow.print("[");
                ow.print("\n        a rdfs:Datatype");
                ow.print(" ;\n        owl:onDatatype " + componentQName(bdt));

                List<Facet> enums = new ArrayList<>();
                List<Facet> resl  = new ArrayList<>();
                for (Facet f : fl) {
                    if ("Enumeration".equals(f.getFacetKind())) enums.add(f);
                    else resl.add(f);
                }
                if (!enums.isEmpty()) {
                    ow.print("  ;\n        owl:oneOf (");
                    for (Facet f : enums) {
                        ow.print("\n            \"" + f.getStringVal() + "\"");
                    }
                    ow.print("\n        )");
                }
                if (!resl.isEmpty()) {
                    ow.print(" ;\n        owl:withRestrictions (");
                    for (Facet f : resl) {
                        ow.print(String.format("\n            [ xsd:%s \"%s\"", f.getXSDFacet(), f.getStringVal()));
                        switch(f.getFacetKind()) {
                            case "MaxExclusive":
                            case "MaxInclusive":
                            case "MinExclusive":
                            case "MinInclusive":    ow.print("^^xsd:decimal"); break;
                            case "FractionDigits":
                            case "Length":
                            case "MaxLength":
                            case "MinLength":
                            case "TotalDigits":     ow.print("^^xsd:nonNegativeInteger"); break;   
                        }
                        ow.print(" ]");
                    }                           
                    ow.print("\n        )");                        
                }                
                ow.print("\n    ]");
            }
        }
        ow.println(" .");
    }
    
    private static String componentQName (Component c) {
        if (W3C_XML_SCHEMA_NS_URI.equals(c.getNamespace().getNamespaceURI())) return("xsd:" + c.getName());
        else return c.getQName();
    }
    
}
