package org.mitre.niem.json;

public class OfClass extends Of{
  private String $ref;


  public OfClass(String classRef){
    $ref = classRef;
  }

  public String getReference() {
    return $ref;
  }

  public void setReference(String reference){
    this.$ref = reference;
  }
}
