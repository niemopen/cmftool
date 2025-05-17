package org.mitre.niem.json;

import org.mitre.niem.cmf.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class JSONDefinition extends OfDefinition {
  private transient Property property;
  private transient Datatype dataType;
  private transient ClassType classType;
  private transient Model model;

  public JSONDefinition(){
  }
  public JSONDefinition(Model model, Property property){
    this.model = model;
    this.property = property;
    description = property.getDefinition();
    type = null;
  }

//  public JSONDefinition(Model model){
//    model = model;
//    type = null;
//  }

  public JSONDefinition(Model model, Datatype dataType){
    this.model = model;
    this.dataType = dataType;
    super.description = dataType.getDefinition();
    this.type = null;
    if (null != dataType.getRestrictionOf()){
      this.$ref = JSONSchemaHelper.generateRef(dataType.getRestrictionOf());
    }
  }

  public JSONDefinition(Model model, ClassType classType){
    this.model = model;
    this.classType = classType;
    this.description = classType.getDefinition();
    this.type = null;

    // Has extensions and properties
    if (null != classType.getExtensionOfClass() && classType.hasPropertyList().size() > 0){
      if (this.allOf == null) this.allOf = new ArrayList<>();

      // Add $ref
      if (null != classType.getExtensionOfClass()){
        this.allOf.add(new OfRef(JSONSchemaHelper.generateRef(classType)));
      }
      // Add HasProperty
      var propList = classType.hasPropertyList();
      if (null != propList && propList.size() > 0){
        var propListDef = new OfDefinition();
        propListDef.type = "object";

        propListDef.properties = new LinkedHashMap<>();
        for (var hasProperty: propList) {
          propListDef.properties.put(JSONSchemaHelper.generateLabel(hasProperty), new JSONPropertyType(hasProperty.getProperty()));
          propListDef.setRequired(hasProperty);
        }
        this.allOf.add(propListDef);
      }
    }
    // has extensions but no properties
    else if (null != classType.getExtensionOfClass() && classType.hasPropertyList().size() == 0){
      this.$ref = JSONSchemaHelper.generateRef(classType);
    }
    // has no extensions but has properties
    else if (null == classType.getExtensionOfClass() && classType.hasPropertyList().size() > 0){
      var propList = classType.hasPropertyList();
      var propListDef = new OfDefinition();
      this.type = "object";

      this.properties = new LinkedHashMap<>();
      for (var hasProperty: propList) {
        this.properties.put(JSONSchemaHelper.generateLabel(hasProperty), new JSONPropertyType(hasProperty.getProperty()));
        this.setRequired(hasProperty);
      }
    }
  }

  public String getClassQName(){
    if (null != property) return property.getNamespace().getNamespacePrefix() + ":" + property.getClassType().getName();
    if (null != classType) return classType.getNamespace().getNamespacePrefix() + ":" + classType.getName();
    if (null != dataType) return dataType.getNamespace().getNamespacePrefix() + ":" + dataType.getName();
    return "";
  }
  public void addProperty(Property p){
    Property subpropertyOf = null;
    var nsPrefix = p.getNamespace().getNamespacePrefix();
    var name = p.getName();

    /*
    subpropertyOf = p.getSubPropertyOf();
    if (null != subpropertyOf){
      name = subpropertyOf.getName();
    }
    */

    // allocate space for properties when needed
    if (properties == null) properties = new LinkedHashMap<>();

    properties.put(String.format("%s:%s", nsPrefix, name),
            new JSONPropertyType(JSONSchemaHelper.PROPERTIES_TEXT, nsPrefix, name));
  }

  public void addProperties(ArrayList<Property> nonAbstractProperties){

    if (nonAbstractProperties.size() == 0){
      System.out.println("?");
    }

    if (properties == null) properties = new LinkedHashMap<>();

    for (var nap: nonAbstractProperties) {
      var nsPrefix = nap.getNamespace().getNamespacePrefix();
      var name = nap.getName();
      var label = String.format("%s:%s", nsPrefix, name);
      properties.put(label, new JSONPropertyType(nap));
    }
  }

  public void addRequired(String refName){
    if (required == null){
      required = new ArrayList<>();
    }
    required.add(refName);
  }
  public QualifiedName setType(RestrictionOf restriction){
    // return a type to consider adding
    QualifiedName name = new QualifiedName(restriction);

    if (name.name.equals("token") || name.name.equals("string")){
      this.type = "string";
    }
    else if (name.name.equals("decimal") || name.name.equals("integer") ||
             name.name.equals("double")  || name.name.equals("single")){
      this.type = "number";
    }
    else{
      // unset the type value
      type = null;
      // name.nsPrefix = restriction.getDatatype().getNamespace().getNamespacePrefix();
      this.$ref = String.format("%s%s", JSONSchemaHelper.DEFINITIONS_TEXT, name.toString());

      // add this type in the list
      return name;
    }
    return null;
  }
  public void addItem(String ref){
    items.put("$ref", JSONSchemaHelper.PROPERTIES_TEXT + ref);
  }
  public void addFacet(Facet facet){
    if (oneOf == null){
      oneOf = new ArrayList<>();
    }
    oneOf.add(new JSONDefinitionConst(facet));
  }

  /*
    Returns the name of a class type if it
    needs to be added or null
   */
  public QualifiedName process(ClassType classType){
    this.classType = classType;
    super.description = classType.getDefinition();

    if (null != classType.getExtensionOfClass()){
      var extClass = classType.getExtensionOfClass();
      if (null != extClass.hasPropertyList()){
        this.type = "object";
        for (var prop: extClass.hasPropertyList()) {
          var label = JSONSchemaHelper.generateLabel(prop.getProperty());
          var jp = new JSONPropertyType(prop.getProperty().getName());
          if (this.properties == null) properties = new LinkedHashMap<>();
          this.properties.put(label, jp);
        }
      }
      else{
        this.$ref = String.format("%s%s", JSONSchemaHelper.DEFINITIONS_TEXT, extClass.getQName());
      }
      return new QualifiedName(extClass);
    }
    return null;
  }

  public void addFacets(RestrictionOf restrictionOf){
    Facet minFacet = null;
    Facet maxFacet = null;

    // Before adding facets, check them to see what the data type is
    if (restrictionOf.getDatatype().getName().equals("decimal")){
      var dt = restrictionOf.getDatatype();
      for (Facet facet : restrictionOf.getFacetList()) {
        if (facet.getFacetKind().equals("MinInclusive") || facet.getFacetKind().equals("MinExclusive")){
          minFacet = facet;
        }
        if (facet.getFacetKind().equals("MaxInclusive") || facet.getFacetKind().equals("MaxExclusive")){
          maxFacet = facet;
        }
      }
      if (minFacet != null){
        try{
          super.minimum = Double.parseDouble(minFacet.getStringVal());
        }
        catch (Exception ex){
          System.out.println(String.format("Unable to convert string value %s to double", minFacet.getStringVal()));
        }
      }

      if (minFacet != null){
        try{
          super.maximum = Double.parseDouble(maxFacet.getStringVal());
        }
        catch (Exception ex){
          System.out.println(String.format("Unable to convert string value %s to double", maxFacet.getStringVal()));
        }
      }
    }
    else{
      for (Facet facet : restrictionOf.getFacetList()) {
        addFacet(facet);
      }
    }
  }
}
