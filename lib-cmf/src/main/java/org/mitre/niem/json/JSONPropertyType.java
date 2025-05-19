package org.mitre.niem.json;

import com.google.gson.annotations.SerializedName;
import org.mitre.niem.cmf.Property;

public class JSONPropertyType{
  private transient String namespacePrefix;
  @SerializedName("$ref")
  private String ref = null;

  private String type = null;

  public JSONPropertyType(String defString, String namespacePrefix, String name) {
    this.namespacePrefix = namespacePrefix;
    if (JSONSchemaHelper.isIntrinsicType(name)) {
      this.type = name;
    } else {
      this.ref = String.format("%s%s:%s", defString, namespacePrefix, name);
    }
  }

  public JSONPropertyType(String name){
    if (JSONSchemaHelper.isIntrinsicType(name)){
      this.type = name;
    }
    else{
      this.ref = name;
    }
  }

  public JSONPropertyType(Property property){
    var nsPrefix = property.namespace().prefix();
    var name = property.name();
    ref = String.format("%s%s:%s", JSONSchemaHelper.PROPERTIES_TEXT, nsPrefix, name);
  }
}
