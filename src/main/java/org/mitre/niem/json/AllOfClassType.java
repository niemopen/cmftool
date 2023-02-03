package org.mitre.niem.json;

import com.google.gson.annotations.SerializedName;

public class AllOfClassType {
  @SerializedName("$ref")
  private String propertyReference;


  public AllOfClassType(String classRef){
    propertyReference = "#properties/" + classRef;
  }

  public String getReference() {
    return propertyReference;
  }

  public void setReference(String reference){
    this.propertyReference = reference;
  }
}
