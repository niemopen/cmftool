package org.mitre.niem.json;

import org.mitre.niem.cmf.*;

import java.util.ArrayList;
import java.util.HashMap;

public class JSONSchemaHelper {
  public static final String DEFINITIONS_TEXT = "#/definitions/";
  public static final String PROPERTIES_TEXT = "#/properties/";

  public static boolean isIntrinsicType(String dataTypeName) {
    // Look for known data types
    String name = dataTypeName.toLowerCase();
    if (name.equals("boolean") ||
            name.equals("date") ||
            name.equals("token") ||
            name.equals("decimal") ||
            name.equals("string")) return true;

    return false;
  }

  public static ArrayList<Property> getSubpropertiesOf(Model m, Property property){
    ArrayList<Property> subproperties = new ArrayList<>();

    for (var p : m.propertyL()) {
      if (p.subPropertyOf() != null && p.subPropertyOf().equals(property)){
        subproperties.add(p);
      }
    }

    return subproperties;
  }

  public static String generateLabel(Property property){
    return String.format("%s:%s", property.namespace().prefix(), property.name());
  }

  public static String generateLabel(ClassType classType){
    return String.format("%s:%s", classType.namespace().prefix(), classType.name());
  }

  public static String generateLabel(PropertyAssociation pa){
    return String.format("%s:%s", pa.property().namespace().prefix(),
                                  pa.property().name());
  }

  public static String generateRef(Restriction r){
    QualifiedName qName = new QualifiedName(r);
    return String.format("%s%s", DEFINITIONS_TEXT, qName);
  }

  public static String generateRef(ClassType classType){
    QualifiedName qName = null;
    if (null != classType.subClassOf()){
      qName = new QualifiedName(classType.subClassOf());
    }
    else{
      qName = new QualifiedName(classType);
    }

    return String.format("%s%s", DEFINITIONS_TEXT, qName);
  }

  public static HashMap<String, Cardinality> getCardinalities(Property property){
    HashMap<String, Cardinality> cardinalities = new HashMap<>();

    // First, determine what property to search on for cardinalities
    // Whenever a property is a subproperty of another property
    // search on the "subpropertyOf" name
    ArrayList<Property> propsToCheck = new ArrayList<>();

    if (property.subPropertyOf() != null){
      propsToCheck.add(property.subPropertyOf());
      propsToCheck.add(property);
    }
    else{
      propsToCheck.add(property);
    }

    for (var prop: propsToCheck){
      var searchName = prop.name();

      // Look through the components for a class with this property
      // if you find one, grab its min/max
      for (var cl : prop.model().classTypeL()) {

        // Skip any non-class components
        var propsList = cl.propL();
        for (var p : propsList) {
          if (p.property().name().equals(searchName)) {
            Cardinality c = new Cardinality(prop, p);
            cardinalities.put(cl.name(), c);
          }
        }
      }
    }

    return cardinalities;
  }
}
