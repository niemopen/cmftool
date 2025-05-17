package org.mitre.niem.json;

/***
 * This is a base class used for allowing
 * all subclasses to be stored in an array of AnyOf
 */
public abstract class Of
{
  protected transient Cardinality cardinality;

  public Cardinality getCardinality() {
    return cardinality;
  }

  public void setCardinality(Cardinality cardinality) {
    this.cardinality = cardinality;
  }
}
