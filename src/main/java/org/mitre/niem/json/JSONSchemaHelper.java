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

    for (var c: m.getComponentList()){
      var p = c.asProperty();

      if (null == p) continue;

      if (p.getSubPropertyOf() != null && p.getSubPropertyOf().equals(property)){
        subproperties.add(p);
      }
    }

    return subproperties;
  }

  public static String generateLabel(Property property){
    return String.format("%s:%s", property.getNamespace().getNamespacePrefix(), property.getName());
  }

  public static String generateLabel(ClassType classType){
    return String.format("%s:%s", classType.getNamespace().getNamespacePrefix(), classType.getName());
  }

  public static String generateLabel(HasProperty hasProperty){
    return String.format("%s:%s", hasProperty.getProperty().getNamespace().getNamespacePrefix(),
                                  hasProperty.getProperty().getName());
  }

  public static String generateRef(RestrictionOf restrictionOf){
    QualifiedName qName = new QualifiedName(restrictionOf);
    return String.format("%s%s", DEFINITIONS_TEXT, qName);
  }

  public static String generateRef(ClassType classType){
    QualifiedName qName = null;
    if (null != classType.getExtensionOfClass()){
      qName = new QualifiedName(classType.getExtensionOfClass());
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

    if (property.getSubPropertyOf() != null){
      propsToCheck.add(property.getSubPropertyOf());
      propsToCheck.add(property);
    }
    else{
      propsToCheck.add(property);
    }

    for (var prop: propsToCheck){
      var searchName = prop.getName();

      // Look through the components for a class with this property
      // if you find one, grab its min/max
      for (var comp: prop.getModel().getComponentList()) {
        if (comp.asClassType() == null) {
          continue;
        }

        // Skip any non-class components
        var propsList = comp.asClassType().hasPropertyList();
        for (var p : propsList) {
          if (p.getProperty().getName().equals(searchName)) {
            Cardinality c = new Cardinality(prop, p);
            cardinalities.put(comp.getName(), c);
          }
        }
      }
    }

    return cardinalities;
  }
}
