package org.mitre.niem.json;

public class OneOfItem extends OneOf {
  public String type;
  public String items;

  public OneOfItem(String type, String items){
    this.type = type;
    this.items = items;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getItems() {
    return items;
  }

  public void setItems(String items) {
    this.items = items;
  }
}
