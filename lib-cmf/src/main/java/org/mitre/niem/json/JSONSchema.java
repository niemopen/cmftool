package org.mitre.niem.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.mitre.niem.cmf.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TreeMap;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;


public class JSONSchema {
  public LinkedHashMap<String, Object> items;
  public transient TreeMap<String, JSONDefinition> definitions = new TreeMap<>();
  public transient ArrayList<QualifiedName> relatedDefinitions;
  private transient Model m;
  private final String DRAFT_7_SCHEMA = "http://json-schema.org/draft-07/schema#";

  private LinkedList<Property> definitionsToParse;
  private ArrayList<Restriction> restrictions;
  private ArrayList<ClassType> extensions;

  public JSONSchema(Model m) {
    this.m = m;
    items = new LinkedHashMap<String, Object>();
    definitionsToParse = new LinkedList<>();
    relatedDefinitions = new ArrayList<>();

    items.put("$schema", DRAFT_7_SCHEMA);
    items.put("type", "object");
  }

  public void addNamespaces() {
    LinkedHashMap<String, String> namespaceHashMap = new LinkedHashMap<String, String>();
    for (Namespace ns : m.namespaceList()) {
      namespaceHashMap.put(ns.prefix(), ns.uri());
    }
    items.put("@context", namespaceHashMap);
  }

  public void addProperties() {
    LinkedHashMap<String, JSONProperty> properties = new LinkedHashMap<String, JSONProperty>();
    int val = 0;

    int addedCount = 0;
    for (var p : m.propertyL()) {

      JSONProperty jProperty = new JSONProperty(p);

      // What kind of property? For abstracts, subproperties decide
      // Abstract with no subproperty is omitted
      String propKind = propertyKind(p);
      if (null == p)
        continue;

      // Handle a class type
      if (null != p.classType()) {
        // add a class reference
        // TODO: Uncomment this if the concept of a ROOT property can be established
        // jProperty.setRef(jProperty.getClassQName());
        if (properties.containsKey(jProperty.getClassQName())) {
          continue;
        } else {
          properties.put(jProperty.getClassQName(), jProperty);
          addedCount++;
        }

        // System.out.println("\n a owl:ObjectProperty");
        if (null != p.subPropertyOf()) {
          // System.out.println(" ;\n rdfs:subPropertyOf " +
          // p.subProperty().qname());
        }
      }
      // Handle a data type
      else if (null != p.datatype()) {
        // add a class reference
        if (properties.containsKey(jProperty.getClassQName())) {
          continue;
        } else {
          properties.put(jProperty.getClassQName(), jProperty);
        }

        // System.out.println("\n a owl:DataProperty");
        // System.out.println(" ;\n rdfs:range " + componentQName(p.datatype()));
      }
      // Handle a subproperty
      else if (null != p.subPropertyOf()) {
      }
      // handle abstract
      else if (p.isAbstract()) {

      } else {
        // System.out.println("\n a " + propKind);
      }

      if (null != p.definition()) {
        // System.out.println(" ;\n rdfs:comment \"" + p.definition() + "\"");
      }
      // System.out.println(" .");
    }

    items.put("properties", properties);
  }

  public void addDefinitions() {
    int val = 0;

    // Separate out the properties
    for (var p : m.propertyL()) {

      // put together a list of definitions that need to be parsed
      definitionsToParse.add(p);
    }

    ListIterator<Property> itr = definitionsToParse.listIterator();
    while (itr.hasNext()) {
      var p = itr.next();
      // What kind of property? For abstracts, subproperties decide
      // Abstract with no subproperty is omitted
      // var jd = new JSONDefinition(m, p);
      // QualifiedName qn = jd.process();
      //
      // if (!definitions.containsKey(qn)){
      // definitions.put(qn, jd);
      // }

      if (null != p.classType()) {
        processDefinitions(p, p.classType());
      } else if (null != p.datatype()) {
        processDefinitions(p, p.datatype());
      }
    }

    items.put("definitions", definitions);
  }

  public void addRelatedDefinitions() {
    for (var name : relatedDefinitions) {
      addRelatedDefinition(name);
    }
  }

  private void addRelatedDefinition(QualifiedName qName) {
    for (var c : m.componentList()) {

      // Check properties
      if (c instanceof Property && c.qname().equals(qName.toString())) {
        System.out.println("property " + c.qname() + " not handled in addRelatedDefintions");
      } else if (c instanceof Datatype && c.qname().equals(qName.toString())) {
        System.out.println("Datatype " + c.qname() + " not handled in addRelatedDefintions");
      } else if (c instanceof ClassType && c.qname().equals(qName.toString())) {
        var newDef = new JSONDefinition(m, (ClassType)c);
        if (!definitions.containsKey(qName.toString())) {
          definitions.put(qName.toString(), newDef);
          // System.out.println("ClassType " + c.qname() + " in addRelatedDefintions
          // processed");
        }
      } else {
        // System.out.println("?");
      }
    }

  }

  private void processDefinitions(Property p, Datatype dataType) {
    if (JSONSchemaHelper.isIntrinsicType(dataType.name()))
      return;

    var jDefinition = new JSONDefinition(m, p);
    jDefinition.description = dataType.definition();
    String label = String.format("%s:%s", dataType.namespace().prefix(), dataType.name());

    if (null != dataType.asRestriction()) {
      var restriction = dataType.asRestriction();
      if (restriction != null) {
        var nameToAdd = jDefinition.setType(restriction);
        addSubDefinition(nameToAdd, jDefinition);
      }

      jDefinition.addFacets(dataType.asRestriction());

      if (!definitions.containsKey(label)) {
        definitions.put(label, jDefinition);
      } else {
        // System.out.println("Duplicate: " + label);
      }
    }
  }

  private void processDefinitions(Property p, ClassType classType) {
    var jDefinition = new JSONDefinition(m, p);
    // add a class reference
    String label = String.format("%s:%s", classType.namespace().prefix(), classType.name());
    jDefinition.description = classType.definition();

    // For the situation where the property has extensions AND property lists
    if (null != classType.subClassOf() && null != classType.propL()) {
      // Add the extension and property list
      jDefinition.allOf = new ArrayList<>();

      if (null != classType.subClassOf()) {
        addRelatedDefinitions(classType);
        var extDef = new JSONDefinition(m, classType);
        // If this is in an allOf, remove the description
        extDef.description = null;
        extDef.properties = null;
        jDefinition.allOf = extDef.allOf;
        jDefinition.$ref = extDef.$ref;
      }
      // var propListDef = processHasProperties(classType);
      // jDefinition.allOf.add(propListDef);
    } else if (null != classType.subClassOf() && null == classType.propL()) {
      addExtension(classType, jDefinition);
    } else if (null == classType.subClassOf() && null != classType.propL()) {
      var propListDef = processHasProperties(classType);
      jDefinition.type = propListDef.type;
      jDefinition.properties = propListDef.properties;
      jDefinition.required = propListDef.required;
      jDefinition.allOf = propListDef.allOf;
    }

    if (!definitions.containsKey(label)) {
      definitions.put(label, jDefinition);
    } else {
      // System.out.println("Duplicate " + label);
    }
  }

  private JSONDefinition processHasProperties(ClassType classType) {
    JSONDefinition propListDef = new JSONDefinition();
    propListDef.type = "object";
    for (PropertyAssociation property : classType.propL()) {
      var prefix = property.property().namespace().prefix();
      var name = property.property().name();

      if (!property.property().isAbstract()) {
        // get the non-abstract properties
        propListDef.addProperty(property.property());

        // Generate the required portion when applicable
        propListDef.setRequired(property);
      } else {
        // Find the non-abstract properties
        var naps = JSONSchemaHelper.getSubpropertiesOf(m, property.property());
        propListDef.addProperties(naps);
        // Generate the required portion when applicable
        propListDef.setRequired(naps);
      }
    }
    return propListDef;
  }

  private void addRelatedDefinitions(ClassType classType) {
    var currentExtOf = classType.subClassOf();
    while (null != currentExtOf) {
      relatedDefinitions.add(new QualifiedName(currentExtOf));
      // addExtension2(currentExtOf);
      currentExtOf = currentExtOf.subClassOf();
    }
  }

  private void addSubDefinition(QualifiedName nameToAdd, JSONDefinition mainDefinition) {
    if (nameToAdd == null)
      return;
    if (JSONSchemaHelper.isIntrinsicType(nameToAdd.name))
      return;
    if (definitions.containsKey(nameToAdd.toString()))
      return;

    var newClassTypeToAdd = classTypeByName(nameToAdd.name);
    var newDataTypeToAdd = getDataTypeByName(nameToAdd.name);

    // Handle a new class type
    if (null != newClassTypeToAdd) {
      var jDefNewType = new JSONDefinition(m, newClassTypeToAdd);
      QualifiedName nameToAdd2 = jDefNewType.process(newClassTypeToAdd);
      mainDefinition.type = null;
      mainDefinition.$ref = JSONSchemaHelper.generateRef(newClassTypeToAdd);

      definitions.put(nameToAdd.toString(), jDefNewType);
    }
    // Handle a new data type
    else if (null != newDataTypeToAdd) {
      if (JSONSchemaHelper.isXMLPrimitiveType(newDataTypeToAdd))
        return; // Skip XML primitive data types
      var jDefNewType = new JSONDefinition(m, newDataTypeToAdd);
      definitions.put(nameToAdd.toString(), jDefNewType);
    }
  }

  private ClassType classTypeByName(String name) {
    for (var cl : m.classTypeL()) {
        if (cl.name().equals(name))
          return cl;
      }
    return null;
  }

  private Datatype getDataTypeByName(String name) {
    for (var dt : m.datatypeL()) {
        if (dt.name().equals(name))
          return dt;
      }
    
    return null;
  }

  public void addTitle(String title) {
    items.put("title", title);
  }

  public void addDescription(String description) {
    items.put("description", description);
  }

  public String toJSON() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String tmp = gson.toJson(this.getSchemas());
    JsonElement jsonElement = new JsonParser().parse(tmp);
    return gson.toJson(jsonElement);
  }

  private String propertyKind(Property p) {
    String rv = null;
    if (null != p.classType())
      rv = "owl:ObjectProperty";
    else if (null != p.datatype())
      rv = "owl:DataProperty";
    else {
      for (var op : m.propertyL()) {
        if (op.subPropertyOf() == p) {
          String rv2 = propertyKind(op);
          if (null != rv2) {
            rv = rv2;
            break;
          }
        }
      }
    }
    return rv;
  }

  private static String componentQName(Component c) {
    if (W3C_XML_SCHEMA_NS_URI.equals(c.namespace().uri()))
      return ("xsd:" + c.name());
    else
      return c.qname();
  }

  /*
   * private String getClassQName(Property p){
   * return p.namespace().prefix() + ":" + p.name();
   * }
   */

  public LinkedHashMap<String, Object> getSchemas() {
    return items;
  }

  public void addExtension2(ClassType classType) {
    if (null != classType.subClassOf()) {
      var ext = classType.subClassOf();
      var jDef = new JSONDefinition(m, ext);
      jDef.description = classType.definition();
      definitions.put(JSONSchemaHelper.generateLabel(classType), jDef);
    }
  }

  public void addExtension(ClassType classType, JSONDefinition jDefinition) {
    var extensionOf = classType.subClassOf();
    ClassType currentExtension = extensionOf.subClassOf();

    if (null != currentExtension) {
      var extDef = new JSONDefinition(m, currentExtension);
      var nameToAdd = extDef.process(currentExtension);
      if (null != nameToAdd) {
        addSubDefinition(nameToAdd, extDef);
      }

      definitions.put(extDef.getClassQName(), extDef);

      // use the allOf construct
      // For the allOf construct, leave the type blank
      jDefinition.type = null;
      jDefinition.allOf = new ArrayList<>();
      jDefinition.allOf.add(
          new OfRef(String.format("%s%s", JSONSchemaHelper.DEFINITIONS_TEXT, extensionOf.qname())));

      var newDef = new OfDefinition("object");
      for (PropertyAssociation property : classType.propL()) {
        var prefix = property.property().namespace().prefix();
        var name = property.property().name();

        newDef.addProperty(JSONSchemaHelper.PROPERTIES_TEXT, prefix, name);

        // Generate the required portion when applicable
        newDef.setRequired(property);
      }
      jDefinition.allOf.add(newDef);
    } else {
      var extDef = new JSONDefinition(m, extensionOf);
      extDef.$ref = null;
      definitions.put(extDef.getClassQName(), extDef);
    }
  }
}
