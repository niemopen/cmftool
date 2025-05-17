package org.mitre.niem.json;

import org.mitre.niem.cmf.HasProperty;
import org.mitre.niem.cmf.Property;

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

  public Cardinality(Property property, HasProperty hasProperty){
    this.property = property;

    minOccurs = hasProperty.minOccurs();
    maxOccurs = hasProperty.maxOccurs();
    isMaxUnbounded = hasProperty.maxUnbounded();
  }

  public String getPropertyName() {
    if (property.getClassType() != null){
      return property.getClassType().getName();
    }

    return property.getName();
  }

  public String getNamespacePrefix(){
    return property.getNamespace().getNamespacePrefix();
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
