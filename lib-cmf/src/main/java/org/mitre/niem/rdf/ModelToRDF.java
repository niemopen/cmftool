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
package org.mitre.niem.rdf;

import java.io.IOException;
import java.io.OutputStreamWriter;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.PropertyAssociation;
import static org.mitre.niem.xsd.NIEMConstants.OWL_NS_URI;
import static org.mitre.niem.xsd.NIEMConstants.RDFS_NS_URI;
import static org.mitre.niem.xsd.NIEMConstants.RDF_NS_URI;

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
    
    public void writeRDF (OutputStreamWriter ow) throws IOException {
        writeNamespaces(ow);
        writeProperties(ow);
        writeClasses(ow);
        writeDatatypes(ow);
    }
    
    private void writeNamespaces (OutputStreamWriter ow) throws IOException {
        for (Namespace ns : m.namespaceList()) {
            ow.write(String.format("@prefix %-15s <%s#> .\n", 
                    ns.prefix()+":", 
                    ns.uri()));
        }
        ow.write("\n");
//        ow.write(String.format("@prefix %-15s <%s#> .\n", "cmf:", CMF_NS_URI));
        ow.write(String.format("@prefix %-15s <%s#> .\n", "owl:", OWL_NS_URI));
        ow.write(String.format("@prefix %-15s <%s#> .\n", "rdfs:", RDFS_NS_URI));
        ow.write(String.format("@prefix %-15s <%s#> .\n", "rdf:", RDF_NS_URI));        
        ow.write(String.format("@prefix %-15s <%s#> .\n", "xsd:", W3C_XML_SCHEMA_NS_URI)); 
    }
    
    private String propertyKind (Property p) {
        String rv = null;
        if (null != p.classType()) rv = "owl:ObjectProperty";
        else if (null != p.datatype()) rv = "owl:DataProperty";
        else {
            for (var op : m.propertyL()) {
                if (op.subPropertyOf() == p) {
                    String rv2 = propertyKind(op);
                    if (null != rv2) {
                        rv = rv2;
                        break;
                    }
                }
            }
        }
        return rv;
    }
    
    private void writeProperties (OutputStreamWriter ow) throws IOException {
        for (var p : m.propertyL()) {

            // What kind of property? For abstracts, subproperties decide
            // Abstract with no subproperty is omitted
            String propKind = propertyKind(p);
            if (null == p) continue;
            ow.write("\n");
            ow.write(p.qname());            
            if (null != p.classType()) {
                ow.write("\n    a owl:ObjectProperty");
                if (null != p.subPropertyOf()) {
                    ow.write(" ;\n    rdfs:subPropertyOf " + p.subPropertyOf().qname());
                }
                ow.write(" ;\n    rdfs:range " + componentQName(p.classType()));
            }
            else if (null != p.datatype()) {
                ow.write("\n    a owl:DataProperty");
                ow.write(" ;\n    rdfs:range " + componentQName(p.datatype()));
            }
            else {
                ow.write("\n    a " + propKind);
            }
            if (null != p.definition()) {
                ow.write(" ;\n    rdfs:comment \"" + p.definition() + "\"");               
            }
            ow.write(" .\n");
        }
    }
    
    private void writeClasses (OutputStreamWriter ow) throws IOException {
        for (var ct : m.classTypeL()) {
            ow.write("\n");
            ow.write(ct.qname());
            ow.write("\n    a owl:Class");
            if (null != ct.subClassOf()) {
                ow.write(" ;\n    rdfs:subClass " + ct.subClassOf().qname());
            }
            if (null != ct.definition()) {
                ow.write(" ;\n    rdfs:comment \"" + ct.definition() + "\"");               
            }
            for (PropertyAssociation hp : ct.propL()) {
                if (0 < hp.minOccursVal()) {
                    ow.write(" ;\n    owl:subclassOf [");
                    ow.write("\n        a owl:Restriction");
                    if (hp.minOccursVal() == hp.maxOccursVal()) 
                        ow.write(" ;\n        owl:cardinality \"" + hp.minOccursVal() + "\"^^xsd:nonNegativeInteger");
                    else {
                        ow.write(" ;\n        owl:minCardinality \"" + hp.minOccursVal() + "\"^^xsd:nonNegativeInteger");
                        if (!hp.isMaxUnbounded()) 
                            ow.write(" ;\n        owl:maxCardinality \"" + hp.maxOccursVal() + "\"^^xsd:nonNegativeInteger");
                    }
                    ow.write(" ;\n        owl:onProperty " + hp.property().qname());
                    ow.write("\n    ]");
                }
            }
            ow.write(" .\n");
        }
    }
    
    private void writeDatatypes (OutputStreamWriter ow) {
        for (var dt : m.datatypeL()) {
            if (W3C_XML_SCHEMA_NS_URI.equals(dt.namespace().uri())) continue;
//            if (null != dt.getListOf()) writeListOfDatatype(dt, ow);
//            else if (null != dt.unionOf()) writeUnionOfDatatype(dt, ow);
//            else if (null != dt.getRestrictionOf()) writeRestrictionOfDatatype(dt, ow); FIXME
        }
    }
    
    private void writeListOfDatatype (Datatype dt, OutputStreamWriter ow) {
        
    }
    
    private void writeUnionOfDatatype (Datatype dt, OutputStreamWriter ow) {
        
    }    
    
//    private void writeRestrictionOfDatatype (Datatype dt, OutputStreamWriter ow) { FIXME
//        RestrictionOf r = dt.getRestrictionOf();
//        Datatype bdt    = r.datatype();
//        List<Facet> fl  = r.getFacetList();
//        ow.write("\n");
//        ow.write(dt.qname());
//        ow.write("\n    a rdfs:Datatype");
//        if (null != dt.definition()) ow.write(" ;\n    rdfs:comment \"" + dt.definition() + "\"");
//        if (null != bdt) {
//            ow.write(" ;\n    owl:equivalentClass ");
//            if (fl.isEmpty()) ow.write(componentQName(bdt));
//            else {
//                ow.write("[");
//                ow.write("\n        a rdfs:Datatype");
//                ow.write(" ;\n        owl:onDatatype " + componentQName(bdt));
//
//                List<Facet> enums = new ArrayList<>();
//                List<Facet> resl  = new ArrayList<>();
//                for (Facet f : fl) {
//                    if ("Enumeration".equals(f.getFacetKind())) enums.add(f);
//                    else resl.add(f);
//                }
//                if (!enums.isEmpty()) {
//                    ow.write("  ;\n        owl:oneOf (");
//                    for (Facet f : enums) {
//                        ow.write("\n            \"" + f.getStringVal() + "\"");
//                    }
//                    ow.write("\n        )");
//                }
//                if (!resl.isEmpty()) {
//                    ow.write(" ;\n        owl:withRestrictions (");
//                    for (Facet f : resl) {
//                        ow.write(String.format("\n            [ xsd:%s \"%s\"", f.getXSDFacet(), f.getStringVal()));
//                        switch(f.getFacetKind()) {
//                            case "MaxExclusive":
//                            case "MaxInclusive":
//                            case "MinExclusive":
//                            case "MinInclusive":    ow.write("^^xsd:decimal"); break;
//                            case "FractionDigits":
//                            case "Length":
//                            case "MaxLength":
//                            case "MinLength":
//                            case "TotalDigits":     ow.write("^^xsd:nonNegativeInteger"); break;   
//                        }
//                        ow.write(" ]");
//                    }                           
//                    ow.write("\n        )");                        
//                }                
//                ow.write("\n    ]");
//            }
//        }
//        ow.writeln(" .");
//    }
    
    private static String componentQName (Component c) {
        if (W3C_XML_SCHEMA_NS_URI.equals(c.namespace().uri())) return("xsd:" + c.name());
        else return c.qname();
    }
    
}
