package org.mitre.niem.json;

import org.mitre.niem.cmf.Property;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.mitre.niem.cmf.PropertyAssociation;

public class OfDefinition extends Of {
  protected String description;
  protected String type = "object";
  protected String $ref = null;
  protected Double minimum = null;
  protected Double maximum = null;

  protected LinkedHashMap<String, JSONPropertyType> properties = null;
  protected ArrayList<String> required = null;
  protected ArrayList<Of> allOf = null;
  protected ArrayList<JSONDefinitionConst> oneOf = null;
  public LinkedHashMap<String, String> items;

  public OfDefinition() {
  }

  public OfDefinition(String type) {
    this.type = type;
  }

  public void addProperty(String defString, String namespacePrefix, String refName) {
    if (properties == null)
      properties = new LinkedHashMap<>();
    properties.put(String.format("%s:%s", namespacePrefix, refName),
        new JSONPropertyType(defString, namespacePrefix, refName));
  }

  private void addRequired(String refName) {
    if (required == null) {
      required = new ArrayList<>();
    }
    required.add(refName);
  }

  public void setRequired(PropertyAssociation property) {
    String label = String.format("%s:%s",
        property.property().namespace().prefix(),
        property.property().name());
    // 0..1, 0..n
    if (property.minOccursVal() == 0) {
      // do nothing
    }

    // 1..1, 1..n
    if (property.minOccursVal() == 1) {
      addRequired(label);
    }

    // min > 1 and max < unbounded
    if (property.minOccursVal() > 1 && !property.isMaxUnbounded()) {
      addRequired(label);
      //
    }
  }

  public void setRequired(ArrayList<Property> nonAbstractProperties) {
    ArrayList<Cardinality> cardinalities = new ArrayList<>();

    for (var property : nonAbstractProperties) {
      // Get the cardinalities
      var cards = JSONSchemaHelper.getCardinalities(property);
      for (var c : cards.values()) {
        cardinalities.add(c);
      }
    }

    if (cardinalities.size() == 1) {
      var property = cardinalities.get(0).getProperty();
      if (property.isAbstract()) {
        property = nonAbstractProperties.get(0);
      }
      String label = String.format("%s:%s",
          property.namespace().prefix(),
          property.name());

      addRequired(label);
    } else {
      // get the first entry
      var cardinality = cardinalities.get(0);

      // In this situation, this becomes an allOf with a oneOf block in it
      if (cardinality.getMinOccurs() == 1 && cardinality.getMaxOccurs() == 1) {
        if (allOf == null)
          allOf = new ArrayList<>();
        var ao = new AllOf();
        ao.oneOf = new ArrayList<>();
        for (int i = 0; i < cardinalities.size(); i++) {
          var name = new QualifiedName(nonAbstractProperties.get(i));
          var oo = new OneOf();
          oo.addRequired(name);
          ao.oneOf.add(oo);
        }

        this.allOf.add(ao);
      } else {
        var property = cardinalities.get(0).getProperty();
        String label = String.format("%s:%s",
            property.namespace().prefix(),
            property.name());

        addRequired(label);
      }
      // System.out.println("!");
    }
  }
}
