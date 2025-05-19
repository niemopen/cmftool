package org.mitre.niem.json;

public class OfDatatype extends Of{
  private String datatype;

  public OfDatatype(String datatype){
    this.datatype = datatype;
  }

  public String getDatatype() {
    return datatype;
  }

  public void setDatatype(String datatype){
    this.datatype = datatype;
  }
}
