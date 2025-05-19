package org.mitre.niem.json;

import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.PropertyAssociation;

public class Cardinality {
  // public String propertyName;
  public Property property;
  private int minOccurs;
  private int maxOccurs;
  private boolean isMaxUnbounded;

  public Cardinality(Property property){
    this.property = property;
  }

  public Cardinality(Property property, int minItems, int maxItems, boolean isMaxUnbounded){
    this.property = property;

    this.minOccurs = minItems;
    this.maxOccurs = maxItems;
    this.isMaxUnbounded = isMaxUnbounded;
  }

  public Cardinality(Property property, PropertyAssociation hasProperty){
    this.property = property;

    minOccurs = hasProperty.minOccursVal();
    maxOccurs = hasProperty.maxOccursVal();
    isMaxUnbounded = hasProperty.isMaxUnbounded();
  }

  public String getPropertyName() {
    if (property.classType() != null){
      return property.classType().name();
    }

    return property.name();
  }

  public String prefix(){
    return property.namespace().prefix();
  }

  public Property getProperty(){
    return this.property;
  }

  public void setProperty(Property property) {
    this.property = property;
  }

  public int getMinOccurs() {
    return minOccurs;
  }

  public void setMinOccurs(int minOccurs) {
    this.minOccurs = minOccurs;
  }

  public int getMaxOccurs() {
    return maxOccurs;
  }

  public void setMaxOccurs(int maxOccurs) {
    this.maxOccurs = maxOccurs;
  }

  public boolean isMaxUnbounded() {
    return isMaxUnbounded;
  }

  public void setMaxUnbounded(boolean maxUnbounded) {
    isMaxUnbounded = maxUnbounded;
  }
}
