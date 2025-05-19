package org.mitre.niem.json;

public class OneOfClass extends Of{
  private String $ref;


  public OneOfClass(String classRef){
    $ref = "#/definitions/" + classRef;
  }

  public String getReference() {
    return $ref;
  }

  public void setReference(String reference){
    this.$ref = reference;
  }

}
