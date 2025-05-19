package org.mitre.niem.json;

import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.Restriction;

public class QualifiedName {
  public String name;
  public String nsPrefix;

  public QualifiedName(){

  }

  public QualifiedName(ClassType classType){
    this.nsPrefix = classType.namespace().prefix();
    this.name = classType.name();
  }

  public QualifiedName(Restriction restrictionOf){
    this.nsPrefix = restrictionOf.base().namespace().prefix();
    this.name = restrictionOf.base().name();
  }

  public QualifiedName(Property property){
    this.nsPrefix = property.namespace().prefix();
    this.name = property.name();
  }


  public String toString(){
    return String.format("%s:%s", nsPrefix, name);
  }
}
