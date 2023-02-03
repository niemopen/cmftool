package org.mitre.niem.json;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.mitre.niem.cmf.*;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.mitre.niem.NIEMConstants.XSD_NS_URI;

public class JSONSchema {
  public LinkedHashMap<String, Object> items;
  private transient Model m;

  public JSONSchema(Model m){
    this.m = m;
    items = new LinkedHashMap();

    items.put("$schema", "http://json-schema.org/draft-07/schema#");
  }

  public void addNamespaces(){
    LinkedHashMap namespaceHashMap = new LinkedHashMap();
    for (Namespace ns: m.getNamespaceList()){
      namespaceHashMap.put(ns.getNamespacePrefix(), ns.getNamespaceURI());
    }
    items.put("@context", namespaceHashMap);
  }

  public void addProperties(){
    LinkedHashMap propertiesHashMap = new LinkedHashMap<>();
    int val = 0;

    for (Component c : m.getComponentList()) {
      Property p = c.asProperty();
      if (null == p) continue;

      JSONProperty jProperty = new JSONProperty(p);

      // What kind of property? For abstracts, subproperties decide
      // Abstract with no subproperty is omitted
      String propKind = getPropertyKind(p);
      if (null == p) continue;

      // Handle a class type
      if (null != p.getClassType()) {
        // add a class reference
        jProperty.setRef(jProperty.getClassQName());

        if (propertiesHashMap.containsKey(jProperty.getClassQName())){
          continue;
        }
        else{
          propertiesHashMap.put(jProperty.getClassQName(), jProperty);
        }

        // System.out.println("\n    a owl:ObjectProperty");
        if (null != p.getSubPropertyOf()) {
          // System.out.println(" ;\n    rdfs:subPropertyOf " + p.getSubPropertyOf().getQName());
        }
        // System.out.println(" ;\n    rdfs:range " + componentQName(p.getClassType()));
      }
      // Handle a data type
      else if (null != p.getDatatype()) {
        // add a class reference
        if (propertiesHashMap.containsKey(jProperty.getClassQName())){
          continue;
        }
        else{
          propertiesHashMap.put(jProperty.getClassQName(), jProperty);
        }

//        System.out.println("\n    a owl:DataProperty");
//        System.out.println(" ;\n    rdfs:range " + componentQName(p.getDatatype()));
      }
      // Handle a subproperty
      else if (null != p.getSubPropertyOf()){
      }
      // handle abstract
      else if (p.isAbstract()){

      }
      else {
//        System.out.println("\n    a " + propKind);
      }

      if (null != p.getDefinition()) {
  //      System.out.println(" ;\n    rdfs:comment \"" + p.getDefinition() + "\"");
      }
//      System.out.println(" .");
    }

    items.put("properties", propertiesHashMap);
  }

  public void addDefinitions(){
    var definitionsHashMap = new LinkedHashMap<>();
    int val = 0;

    for (Component c : m.getComponentList()) {
      Property p = c.asProperty();
      if (null == p) continue;

      var jDefintion = new JSONDefinition(p);

      // What kind of property? For abstracts, subproperties decide
      // Abstract with no subproperty is omitted
      String propKind = getPropertyKind(p);
      if (null == p) continue;

      var classType = p.getClassType();
      if (null != classType) {
        // add a class reference
        String label = "";
        for (HasProperty property: classType.hasPropertyList()) {
          var prefix = property.getProperty().getNamespace().getNamespacePrefix();
          var name = property.getProperty().getName();
          label = prefix + ":" + name;
          jDefintion.addProperty(label);

          // Generate the required portion when applicable
          setRequired(jDefintion, property, label);
        }
        definitionsHashMap.put(label, jDefintion);
      }

      // System.out.println("\n    a owl:ObjectProperty");
      if (null != p.getSubPropertyOf()) {
        // System.out.println(" ;\n    rdfs:subPropertyOf " + p.getSubPropertyOf().getQName());
      }
        // System.out.println(" ;\n    rdfs:range " + componentQName(p.getClassType()));
      else if (null != p.getDatatype()) {
        // add a class reference
//        System.out.println("\n    a owl:DataProperty");
//        System.out.println(" ;\n    rdfs:range " + componentQName(p.getDatatype()));
      }
      else {
//        System.out.println("\n    a " + propKind);
      }
      if (null != p.getDefinition()) {
        //      System.out.println(" ;\n    rdfs:comment \"" + p.getDefinition() + "\"");
      }
//      System.out.println(" .");
    }

    items.put("definitions", definitionsHashMap);
  }

  public void addTitle(String title){
    items.put("title", title);
  }

  public void addDescription(String description){
    items.put("description", description);
  }

  public String toJSON(){
    Gson gson = new Gson();
    return gson.toJson(this);
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

  private static String componentQName (Component c) {
    if (XSD_NS_URI.equals(c.getNamespace().getNamespaceURI())) return("xsd:" + c.getName());
    else return c.getQName();
  }

  /*
  private String getClassQName(Property p){
    return p.getNamespace().getNamespacePrefix() + ":" + p.getName();
  }
   */

  public LinkedHashMap<String, Object> getSchemas()
  {
    return items;
  }

  private void setRequired(JSONDefinition jDefintion, HasProperty property, String label){
    // 0..1, 0..n
    if (property.minOccurs() == 0){
      // do nothing
    }

    // 1..1, 1..n
    if (property.minOccurs() == 1){
      jDefintion.addRequired(label);
    }

    // min > 1 and max < unbounded
    if (property.minOccurs() > 1 && !property.maxUnbounded()){
        jDefintion.addRequired(label);
        //
    }
  }
}
