package org.mitre.niem.json;

import java.util.TreeMap;

public class OfItem extends Of {
  public String type;
  public TreeMap<String, String> items = new TreeMap<>();

  public OfItem(String type, TreeMap<String, String> items){
    this.type = type;
    this.items = items;
  }

  public OfItem(String type, String qClassRef, Cardinality cardinality){
    this.type = type;
    this.items.put("$ref", qClassRef);
    this.cardinality = cardinality;
  }

  public OfItem(String type, String refString, String namespacePrefix, String classRef){
    this.type = type;
    this.items.put("$ref", String.format("%s%s:%s",refString ,namespacePrefix, classRef));
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public TreeMap<String, String> getItems() {
    return items;
  }

  public void setItems(TreeMap<String, String> items) {
    this.items = items;
  }
}
