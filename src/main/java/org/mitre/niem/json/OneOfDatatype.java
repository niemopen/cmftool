package org.mitre.niem.json;

public class OneOfDatatype extends OneOf{
  private String datatype;

  public OneOfDatatype(String datatype){
    this.datatype = datatype;
  }

  public String getDatatype() {
    return datatype;
  }

  public void setDatatype(String datatype){
    this.datatype = datatype;
  }
}
