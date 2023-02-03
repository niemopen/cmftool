package org.mitre.niem.json;

import com.google.gson.annotations.SerializedName;

public class JSONPropertyType {
  @SerializedName("$ref")
  private String ref;

  public JSONPropertyType(String refName){
    this.ref = refName;
  }

  public void setRef(String refName){
    ref = "#definitions/" + refName;
  }

}
