package org.mitre.niem.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.mitre.niem.cmf.ClassType;
import org.mitre.niem.cmf.Datatype;
import org.mitre.niem.cmf.Facet;
import org.mitre.niem.cmf.Model;
import org.mitre.niem.cmf.Property;
import org.mitre.niem.cmf.PropertyAssociation;
import org.mitre.niem.cmf.Restriction;

public class JSONDefinition extends OfDefinition {
  private transient Property property;
  private transient Datatype dataType;
  private transient ClassType classType;
  public String pattern;
  public Double multipleOf = null;
  public String format = null;
  private transient Model model;

  public JSONDefinition(){
  }
  public JSONDefinition(Model model, Property property){
    this.model = model;
    this.property = property;
    description = property.definition();
    type = null;
  }

//  public JSONDefinition(Model model){
//    model = model;
//    type = null;
//  }

  public JSONDefinition(Model model, Datatype dataType){
    this.model = model;
    this.dataType = dataType;
    super.description = dataType.definition();
    this.type = null;
    if (null != dataType.asRestriction()){
      this.$ref = JSONSchemaHelper.generateRef(dataType.asRestriction());
    }
  }

  public JSONDefinition(Model model, ClassType classType){
    this.model = model;
    this.classType = classType;
    this.description = classType.definition();
    this.type = null;

    // Has extensions and properties
    if (null != classType.subClassOf() && classType.propL().size() > 0){
      if (this.allOf == null) this.allOf = new ArrayList<>();

      // Add $ref
      if (null != classType.subClassOf()){
        this.allOf.add(new OfRef(JSONSchemaHelper.generateRef(classType)));
      }
      // Add HasProperty
      var propList = classType.propL();
      if (null != propList && propList.size() > 0){
        var propListDef = new OfDefinition();
        propListDef.type = "object";

        propListDef.properties = new LinkedHashMap<>();
        for (var hasProperty: propList) {
          if (hasProperty instanceof PropertyAssociation) {
            Property property = ((PropertyAssociation) hasProperty).property();
            if (property.isAbstract()) {
              // Get the subproperties of this property
              var subProperties = JSONSchemaHelper.getSubpropertiesOf(model, hasProperty.property());
              if (subProperties.size() > 0) {
                for (var subProp: subProperties) {
                  propListDef.properties.put(JSONSchemaHelper.generateLabel(subProp), new JSONPropertyType(subProp));
                  //propListDef.setRequired(subProp);
                }
              }
            } else {
              propListDef.properties.put(JSONSchemaHelper.generateLabel(hasProperty), new JSONPropertyType(property));
              propListDef.setRequired(hasProperty);
            }
          }
        }
        this.allOf.add(propListDef);
      }
    }
    // has extensions but no properties
    else if (null != classType.subClassOf() && classType.propL().size() == 0){
      this.$ref = JSONSchemaHelper.generateRef(classType);
    }
    // has no extensions but has properties
    else if (null == classType.subClassOf() && classType.propL().size() > 0){
      var propList = classType.propL();
      var propListDef = new OfDefinition();
      this.type = "object";

      this.properties = new LinkedHashMap<>();
      for (var hasProperty: propList) {
        if (hasProperty instanceof PropertyAssociation) {
          Property property = ((PropertyAssociation) hasProperty).property();
          if (property.isAbstract()) {
            // Get the subproperties of this property
            var subProperties = JSONSchemaHelper.getSubpropertiesOf(model, hasProperty.property());
            if (subProperties.size() > 0) {
              for (var subProp: subProperties) {
                this.properties.put(JSONSchemaHelper.generateLabel(subProp), new JSONPropertyType(subProp));
               //this.setRequired(subProp);
              }
           }
          } else {
              this.properties.put(JSONSchemaHelper.generateLabel(hasProperty), new JSONPropertyType(property));
              this.setRequired(hasProperty);
          }
        }
      }
    }
  }

  public String getClassQName(){
    if (null != property) return property.namespace().prefix() + ":" + property.classType().name();
    if (null != classType) return classType.namespace().prefix() + ":" + classType.name();
    if (null != dataType) return dataType.namespace().prefix() + ":" + dataType.name();
    return "";
  }
  public void addProperty(Property p){
    Property subpropertyOf = null;
    var nsPrefix = p.namespace().prefix();
    var name = p.name();

    /*
    subpropertyOf = p.getSubPropertyOf();
    if (null != subpropertyOf){
      name = subpropertyOf.name();
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
      var nsPrefix = nap.namespace().prefix();
      var name = nap.name();
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
  public QualifiedName setType(Restriction restriction){
    // return a type to consider adding
    QualifiedName name = new QualifiedName(restriction);

    if (JSONSchemaHelper.isXMLPrimitiveType(restriction.base())) {
      processXMLPrimitiveDataType(restriction.base().name());
      return name;
    }

    // unset the type value
    type = null;
    // name.nsPrefix = restriction.base().namespace().prefix();
    this.$ref = String.format("%s%s", JSONSchemaHelper.DEFINITIONS_TEXT, name.toString());

    // add this type in the list
    return name;
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
    super.description = classType.definition();

    if (null != classType.subClassOf()){
      var extClass = classType.subClassOf();
      if (null != extClass.propL()){
        this.type = "object";
        for (var prop: extClass.propL()) {
          var label = JSONSchemaHelper.generateLabel(prop.property());
          var jp = new JSONPropertyType(prop.property().name());
          if (this.properties == null) properties = new LinkedHashMap<>();
          this.properties.put(label, jp);
        }
      }
      else{
        this.$ref = String.format("%s%s", JSONSchemaHelper.DEFINITIONS_TEXT, extClass.qname());
      }
      return new QualifiedName(extClass);
    }
    return null;
  }

  public void addFacets(Restriction restriction){
    Facet minFacet = null;
    Facet maxFacet = null;

    // Before adding facets, check them to see what the data type is
    if (restriction.base().name().equals("decimal")){
      var dt = restriction.base();
      for (Facet facet : restriction.facetL()) {
        if (facet.category().equals("MinInclusive") || facet.category().equals("MinExclusive")){
          minFacet = facet;
        }
        if (facet.category().equals("MaxInclusive") || facet.category().equals("MaxExclusive")){
          maxFacet = facet;
        }
      }
      if (minFacet != null){
        try{
          super.minimum = Double.parseDouble(minFacet.value());
        }
        catch (Exception ex){
          System.out.println(String.format("Unable to convert string value %s to double", minFacet.value()));
        }
      }

      if (minFacet != null){
        try{
          super.maximum = Double.parseDouble(maxFacet.value());
        }
        catch (Exception ex){
          System.out.println(String.format("Unable to convert string value %s to double", maxFacet.value()));
        }
      }
    }
    else{
      for (Facet facet : restriction.facetL()) {
        addFacet(facet);
      }
    }
  }

  protected void processXMLPrimitiveDataType(String dataTypeName) {

    type = "string"; // Default type

    switch (dataTypeName) {
      case "boolean" ->
        type = "boolean";
      case "decimal", "double", "float" ->
        type = "number";
      case "int" -> {
        type = "number";
        multipleOf = 1.0;
        minimum = -2147483648D;
        maximum = 2147483647D;
      }
      case "integer" -> {
        type = "number";
        multipleOf = 1.0;
      }
      case "long" -> {
        type = "number";
        multipleOf = 1.0;
        minimum = -9223372036854775808D;
        maximum = 9223372036854775807D;
      }
      case "unsignedLong" -> {
        type = "number";
        multipleOf = 1.0;
        minimum = 0D;
        maximum = 9223372036854775807D;
      }
      case "unsignedInt" -> {
        type = "number";
        multipleOf = 1.0;
        minimum = 0D;
        maximum = 4294967295D;
      }
      case "short" -> {
        type = "number";
        multipleOf = 1.0;
        minimum = -32768D;
        maximum = 32767D;
      }
      case "unsignedShort" -> {
        type = "number";
        multipleOf = 1.0;
        minimum = 0D;
        maximum = 65535D;
      }
      case "byte" -> {
        type = "number";
        multipleOf = 1.0;
        minimum = -128D;
        maximum = 127D;
      }
      case "unsignedByte" -> {
        type = "number";
        multipleOf = 1.0;
        minimum = 0D;
        maximum = 255D;
      }
      case "negativeInteger" -> {
        type = "number";
        multipleOf = 1.0;
        maximum = -1D;
      }
      case "nonNegativeInteger" -> {
        type = "number";
        multipleOf = 1.0;
        minimum = 0D;
      }
      case "nonPositiveInteger" -> {
        type = "number";
        multipleOf = 1.0;
        maximum = 0D;
      }
      case "positiveInteger" -> {
        type = "number";
        multipleOf = 1.0;
        minimum = 1D;
      }
      case "date", "dateTime" ->
        format = "date-time";
      case "time" ->
        pattern = "^([0-9]{2}):([0-9]{2}):([0-9]{2}([.][0-9]{1,6})?)([+-]([0-9]{2}):([0-9]{2}))?$";
      case "duration" ->
        pattern = "^[-+]?P(([0-9]+Y)|([0-9]+M)|([0-9]+D)|(T([0-9]+H)|([0-9]+M)|([0-9]+([.][0-9]{1,6})?S)))$";
      case "gDay" ->
        pattern = "^---[0-3][0-9]$";
      case "gMonth" ->
        pattern = "^--[0-1][0-9]$";
      case "gMonthDay" ->
        pattern = "^--[0-1][0-9]-[0-3][0-9]$";
      case "gYear" ->
        pattern = "^[0-9]{4}$";
      case "gYearMonth" ->
        pattern = "^[0-9]{4}-[0-1][0-9]$";
      case "token" ->
        pattern = "^\\S*$"; // Non-whitespace characters
      case "normalizedString" ->
        pattern = "^\\s?(\\S+\\s?)+\\s?$"; // Non-empty string with optional leading/trailing whitespace
      case "NMTOKEN" ->
        pattern = "^[-.:_A-Za-z0-9]+$"; // Matches NMTOKEN pattern
      case "NMTOKENS" -> {
        type = "array";
        items = new LinkedHashMap<>();
        items.put("type", "string");
        items.put("pattern", "^([-.:_A-Za-z0-9]+\\s)+$");
      }
      case "NAME" ->
        pattern = "^[_:A-Za-z][-.:_A-Za-z0-9]*$";
      case "language" ->
        pattern = "^[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*$";
      case "hexBinary" ->
        pattern = "^([A-Z0-9]{2})*$";
      case "base64Binary" ->
        pattern = "^[A-Za-z0-9+/=\\\\s]*$";
      case "anyURI" ->
        format = "uri";
      case "ID", "IDREF", "NCNAME", "ENTITY" ->
        pattern = "^[_A-Za-z][-._A-Za-z0-9]*$";
      case "IDREFS", "ENTITIES" -> {
        type = "array";
        items = new LinkedHashMap<>();
        items.put("type", "string");
        items.put("pattern", "^[_A-Za-z][-._A-Za-z0-9]*$");
      }
      case "NOTATION", "QName" ->
        pattern = "^[_A-Za-z][-._A-Za-z0-9]*:[_A-Za-z][-._A-Za-z0-9]*$";
      default -> {}
    }
  }
}
