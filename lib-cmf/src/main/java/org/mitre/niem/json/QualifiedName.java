package org.mitre.niem.json;

import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.RestrictionOf;

public class QualifiedName {
  public String name;
  public String nsPrefix;

  public QualifiedName(){

  }

  public QualifiedName(ClassType classType){
    this.nsPrefix = classType.getNamespace().getNamespacePrefix();
    this.name = classType.getName();
  }

  public QualifiedName(RestrictionOf restrictionOf){
    this.nsPrefix = restrictionOf.getDatatype().getNamespace().getNamespacePrefix();
    this.name = restrictionOf.getDatatype().getName();
  }

  public QualifiedName(Property property){
    this.nsPrefix = property.getNamespace().getNamespacePrefix();
    this.name = property.getName();
  }


  public String toString(){
    return String.format("%s:%s", nsPrefix, name);
  }
}
