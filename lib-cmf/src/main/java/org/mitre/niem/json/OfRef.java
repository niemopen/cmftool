package org.mitre.niem.json;

// This is a special case for the root property
public class OfRef extends Of {
  public String $ref;

  public OfRef(String classRef) {
    $ref = classRef;
  }

  @Override
  public String toString() {
    return $ref;
  }
}
