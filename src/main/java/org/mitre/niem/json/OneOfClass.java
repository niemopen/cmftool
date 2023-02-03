package org.mitre.niem.json;

import javax.swing.plaf.ActionMapUIResource;

public class OneOfClass extends OneOf{
  private String reference;


  public OneOfClass(String classRef){
    reference = "#definitions/" + classRef;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference){
    this.reference = reference;
  }

}
