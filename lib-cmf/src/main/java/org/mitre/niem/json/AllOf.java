package org.mitre.niem.json;

import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Property;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class AllOf extends Of{
  // private final String type = "object";
  private LinkedHashMap<String, OfClassType> properties = null;
  public ArrayList<OneOf> oneOf = null;

  public AllOf(){
    properties = null;
  }

  public void addAllOfProperty(String propertyName, OfClassType allOfClassType){
    properties.put(propertyName, allOfClassType);
  }

  public void addClassTypeProperties(ClassType classType){
    if (null != classType) {
      if (null != classType.namespace()){
        // add a class reference
        for (var hp: classType.propL()){
          Property p = hp.property();
          if (null != p && null != p.namespace()){
            String classRef = p.namespace().prefix() + ":" + p.name();
            var allOfClassType = new OfClassType(classRef);
            properties.put(classRef, allOfClassType);
          }
        }
      }
    }
  }
}
