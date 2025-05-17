package org.mitre.niem.json;

import java.util.ArrayList;

public class OneOf extends Of{
  public ArrayList<String> required = null;

  public OneOf(){
  }

  public void addRequired(QualifiedName qName){
    if (required == null) required = new ArrayList<>();
    required.add(qName.toString());
  }
}
