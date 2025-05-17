package org.mitre.niem.json;

import com.google.gson.annotations.SerializedName;

public class OfClassType extends Of{
  @SerializedName("$ref")
  private String propertyReference;


  public OfClassType(String classRef){
    propertyReference = "#properties/" + classRef;
  }

  public String getReference() {
    return propertyReference;
  }

  public void setReference(String reference){
    this.propertyReference = reference;
  }
}
