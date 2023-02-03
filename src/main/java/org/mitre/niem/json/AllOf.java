package org.mitre.niem.json;

import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Property;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class AllOf {
  private final String type = "object";
  private LinkedHashMap<String, AllOfClassType> properties;

  public AllOf(){
    properties = new LinkedHashMap<>();
  }

  public void addAllOfProperty(String propertyName, AllOfClassType allOfClassType){
    properties.put(propertyName, allOfClassType);
  }

  public void addClassTypeProperties(ClassType classType){
    if (null != classType) {
      if (null != classType.getNamespace()){
        // add a class reference
        for (var hp: classType.hasPropertyList()){
          Property p = hp.getProperty();
          if (null != p && null != p.getNamespace()){
            String classRef = p.getNamespace().getNamespacePrefix() + ":" + p.getName();
            var allOfClassType = new AllOfClassType(classRef);
            properties.put(classRef, allOfClassType);
          }
        }
      }
    }
  }
}
