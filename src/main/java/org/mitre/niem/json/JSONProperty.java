package org.mitre.niem.json;

import com.google.gson.annotations.SerializedName;
import org.mitre.niem.cmf.Property;

import java.util.ArrayList;

public class JSONProperty {
  private transient Property property;
  public String description;
  @SerializedName("$ref")
  public String ref;
  public String type;
  public JSONPropertyType items = null;

  public JSONProperty(Property property){
    this.property = property;
    this.description = property.getDefinition();

    if (property.getDatatype() != null){
      type = "array";
      items = new JSONPropertyType(this.getClassQName());
    }
  }

  public void setRef(String ref){
    this.ref = "#/definitions/" + ref;
  }

  public boolean isSubproperty(){
    return (property.getSubPropertyOf() == null);
  }
  public boolean isAbstract(){ return property.isAbstract();}


  public String getClassQName(){
    if (property.getClassType() == null && property.getDatatype() == null){
      return "";
    }

    String name = property.getClassType() != null ? property.getClassType().getName() : property.getName();
    return property.getNamespace().getNamespacePrefix() + ":" + name;
  }

}
