package org.mitre.niem.json;

import java.io.IOException;
import java.io.OutputStreamWriter;
import org.mitre.niem.cmf.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

public class ModelToJSON {
  private JSONSchema jsonSchema;

  public ModelToJSON (Model m) {
    jsonSchema = new JSONSchema(m);
  }

  public JSONSchema getJsonSchema(){
    return jsonSchema;
  }

  public String writeJSON () {
    addNamespaces();
    addProperties();
    addDefinitions();
    jsonSchema.addRelatedDefinitions();
//    writeClasses(ow);
//    writeDatatypes(ow);
    return this.getJsonSchema().toJSON();
  }

  public String writeJSON (OutputStreamWriter writer) throws IOException {
    String json = this.writeJSON();
    writer.write(json);
    return json;
  }

  private void addNamespaces () {
    jsonSchema.addNamespaces();
  }

  private void addProperties(){
    jsonSchema.addProperties();
  }

  private void addDefinitions(){
    jsonSchema.addDefinitions();
  }

//  private void writeProperties (PrintWriter ow) {
//    for (Component c : m.getComponentList()) {
//      Property p = c.asProperty();
//      if (null == p) continue;
//
//      // What kind of property? For abstracts, subproperties decide
//      // Abstract with no subproperty is omitted
//      String propKind = getPropertyKind(p);
//      if (null == p) continue;
//      ow.print("\n");
//      ow.print(p.qname());
//      if (null != p.getClassType()) {
//        ow.print("\n    a owl:ObjectProperty");
//        if (null != p.getSubPropertyOf()) {
//          ow.print(" ;\n    rdfs:subPropertyOf " + p.getSubPropertyOf().qname());
//        }
//        ow.print(" ;\n    rdfs:range " + componentQName(p.getClassType()));
//      }
//      else if (null != p.datatype()) {
//        ow.print("\n    a owl:DataProperty");
//        ow.print(" ;\n    rdfs:range " + componentQName(p.datatype()));
//      }
//      else {
//        ow.print("\n    a " + propKind);
//      }
//      if (null != p.definition()) {
//        ow.print(" ;\n    rdfs:comment \"" + p.definition() + "\"");
//      }
//      ow.println(" .");
//    }
//  }

//  private void writeClasses (PrintWriter ow) {
//    for (Component c : m.getComponentList()) {
//      ClassType ct = c.asClassType();
//      if (null == ct) continue;
//      ow.print("\n");
//      ow.print(ct.qname());
//      ow.print("\n    a owl:Class");
//      if (null != ct.getExtensionOfClass()) {
//        ow.print(" ;\n    rdfs:subClassOf " + ct.getExtensionOfClass().qname());
//      }
//      if (null != ct.definition()) {
//        ow.print(" ;\n    rdfs:comment \"" + ct.definition() + "\"");
//      }
//      for (HasProperty hp : ct.hasPropertyList()) {
//        if (0 < hp.minOccurs()) {
//          ow.print(" ;\n    owl:subclassOf [");
//          ow.print("\n        a owl:Restriction");
//          if (hp.minOccurs() == hp.maxOccurs())
//            ow.print(" ;\n        owl:cardinality \"" + hp.minOccurs() + "\"^^xsd:nonNegativeInteger");
//          else {
//            ow.print(" ;\n        owl:minCardinality \"" + hp.minOccurs() + "\"^^xsd:nonNegativeInteger");
//            if (!hp.maxUnbounded())
//              ow.print(" ;\n        owl:maxCardinality \"" + hp.maxOccurs() + "\"^^xsd:nonNegativeInteger");
//          }
//          ow.print(" ;\n        owl:onProperty " + hp.getProperty().qname());
//          ow.print("\n    ]");
//        }
//      }
//      ow.println(" .");
//    }
//  }

//  private void writeDatatypes (PrintWriter ow) {
//    for (Component c : m.getComponentList()) {
//      Datatype dt = c.asDatatype();
//      if (null == dt) continue;
//      if (XSD_NS_URI.equals(dt.getNamespace().getNamespaceURI())) continue;
//      if (null != dt.getListOf()) writeListOfDatatype(dt, ow);
//      else if (null != dt.getUnionOf()) writeUnionOfDatatype(dt, ow);
//      else if (null != dt.getRestrictionOf()) writeRestrictionOfDatatype(dt, ow);
//    }
//  }

//  private void writeListOfDatatype (Datatype dt, PrintWriter ow) {
//
//  }

  private void writeUnionOfDatatype (Datatype dt, PrintWriter ow) {

  }

  private void writeRestrictionOfDatatype (Datatype dt, PrintWriter ow) {
    Restriction r = dt.asRestriction();
    Datatype bdt    = r.base();
    List<Facet> fl  = r.facetL();
    ow.print("\n");
    ow.print(dt.qname());
    ow.print("\n    a rdfs:Datatype");
    if (null != dt.definition()) ow.print(" ;\n    rdfs:comment \"" + dt.definition() + "\"");
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
          if ("Enumeration".equals(f.category())) enums.add(f);
          else resl.add(f);
        }
        if (!enums.isEmpty()) {
          ow.print("  ;\n        owl:oneOf (");
          for (Facet f : enums) {
            ow.print("\n            \"" + f.value() + "\"");
          }
          ow.print("\n        )");
        }
        if (!resl.isEmpty()) {
          ow.print(" ;\n        owl:withRestrictions (");
          for (Facet f : resl) {
            ow.print(String.format("\n            [ xsd:%s \"%s\"", f.xsdFacetName(), f.value()));
            switch(f.category()) {
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
    if (W3C_XML_SCHEMA_NS_URI.equals(c.namespace().uri())) return("xsd:" + c.name());
    else return c.qname();
  }
}
