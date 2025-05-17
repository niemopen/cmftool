package org.mitre.niem.json;

import java.util.TreeMap;

public class OfProperty extends Of{

  public String type;
  public TreeMap<String, TreeMap> properties = new TreeMap<>();

  public OfProperty(String type, TreeMap<String, TreeMap> properties){
    this.type = type;
    this.properties = properties;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public TreeMap<String, TreeMap> getProperties() {
    return properties;
  }

  public void setProperties(TreeMap<String, TreeMap> properties) {
    this.properties = properties;
  }
}
