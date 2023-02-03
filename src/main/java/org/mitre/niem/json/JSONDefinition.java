package org.mitre.niem.json;

import org.mitre.niem.cmf.Property;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class JSONDefinition {
  private transient Property property;
  public String description;
  public String type = "object";
  public LinkedHashMap<String, String> items;
  public LinkedHashMap<String, JSONPropertyType> properties;
  public ArrayList<String> required;

  public JSONDefinition(Property property){
    this.property = property;
    this.description = property.getDefinition();
    properties = new LinkedHashMap<String, JSONPropertyType>();
    items = new LinkedHashMap<String, String>();
  }

  public String getClassQName(){
    return property.getNamespace().getNamespacePrefix() + ":" + property.getClassType().getName();
  }


  public void addProperty(String refName){
    properties.put(refName, new JSONPropertyType(refName));
  }
  public void addRequired(String refName){
    if (required == null){
      required = new ArrayList<>();
    }
    required.add(refName);
  }

  public void setType(String type){
    this.type = type;
  }

  public void addItem(String ref){
    items.put("$ref", "#/definitions/" + ref);
  }

}
