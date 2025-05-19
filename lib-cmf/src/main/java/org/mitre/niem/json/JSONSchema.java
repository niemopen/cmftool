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

import static org.mitre.niem.NIEMConstants.XSD_NS_URI;

public class JSONSchema {
  public LinkedHashMap<String, Object> items;
  public transient TreeMap<String, JSONDefinition> definitions = new TreeMap<>();
  public transient ArrayList<QualifiedName> relatedDefinitions;
  private transient Model m;
  private final String DRAFT_7_SCHEMA = "http://json-schema.org/draft-07/schema#";

  private LinkedList<Property> definitionsToParse;
  private ArrayList<RestrictionOf> restrictions;
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
    for (Namespace ns : m.getNamespaceList()) {
      namespaceHashMap.put(ns.getNamespacePrefix(), ns.getNamespaceURI());
    }
    items.put("@context", namespaceHashMap);
  }

  public void addProperties() {
    LinkedHashMap<String, JSONProperty> properties = new LinkedHashMap<String, JSONProperty>();
    int val = 0;

    int addedCount = 0;
    for (Component c : m.getComponentList()) {
      Property p = c.asProperty();
      if (null == p)
        continue;

      JSONProperty jProperty = new JSONProperty(p);

      // What kind of property? For abstracts, subproperties decide
      // Abstract with no subproperty is omitted
      String propKind = getPropertyKind(p);
      if (null == p)
        continue;

      // Handle a class type
      if (null != p.getClassType()) {
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
        if (null != p.getSubPropertyOf()) {
          // System.out.println(" ;\n rdfs:subPropertyOf " +
          // p.getSubPropertyOf().getQName());
        }
      }
      // Handle a data type
      else if (null != p.getDatatype()) {
        // add a class reference
        if (properties.containsKey(jProperty.getClassQName())) {
          continue;
        } else {
          properties.put(jProperty.getClassQName(), jProperty);
        }

        // System.out.println("\n a owl:DataProperty");
        // System.out.println(" ;\n rdfs:range " + componentQName(p.getDatatype()));
      }
      // Handle a subproperty
      else if (null != p.getSubPropertyOf()) {
      }
      // handle abstract
      else if (p.isAbstract()) {

      } else {
        // System.out.println("\n a " + propKind);
      }

      if (null != p.getDefinition()) {
        // System.out.println(" ;\n rdfs:comment \"" + p.getDefinition() + "\"");
      }
      // System.out.println(" .");
    }

    items.put("properties", properties);
  }

  public void addDefinitions() {
    int val = 0;

    // Separate out the properties
    for (Component c : m.getComponentList()) {
      Property p = c.asProperty();

      if (null == p)
        continue;

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

      if (null != p.getClassType()) {
        processDefinitions(p, p.getClassType());
      } else if (null != p.getDatatype()) {
        processDefinitions(p, p.getDatatype());
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
    for (var c : m.getComponentList()) {

      // Check properties
      if (c instanceof Property && c.getQName().equals(qName.toString())) {
        System.out.println("property " + c.getQName() + " not handled in addRelatedDefintions");
      } else if (c instanceof Datatype && c.getQName().equals(qName.toString())) {
        System.out.println("Datatype " + c.getQName() + " not handled in addRelatedDefintions");
      } else if (c instanceof ClassType && c.getQName().equals(qName.toString())) {
        var newDef = new JSONDefinition(m, c.asClassType());
        if (!definitions.containsKey(qName.toString())) {
          definitions.put(qName.toString(), newDef);
          // System.out.println("ClassType " + c.getQName() + " in addRelatedDefintions
          // processed");
        }
      } else {
        // System.out.println("?");
      }
    }

  }

  private void processDefinitions(Property p, Datatype dataType) {
    if (JSONSchemaHelper.isIntrinsicType(dataType.getName()))
      return;

    var jDefinition = new JSONDefinition(m, p);
    jDefinition.description = dataType.getDefinition();
    String label = String.format("%s:%s", dataType.getNamespace().getNamespacePrefix(), dataType.getName());

    if (null != dataType.getRestrictionOf()) {
      var restriction = dataType.getRestrictionOf();
      if (restriction != null) {
        var nameToAdd = jDefinition.setType(restriction);
        addSubDefinition(nameToAdd, jDefinition);
      }

      jDefinition.addFacets(dataType.getRestrictionOf());

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
    String label = String.format("%s:%s", classType.getNamespace().getNamespacePrefix(), classType.getName());
    jDefinition.description = classType.getDefinition();

    // For the situation where the property has extensions AND property lists
    if (null != classType.getExtensionOfClass() && null != classType.hasPropertyList()) {
      // Add the extension and property list
      jDefinition.allOf = new ArrayList<>();

      if (null != classType.getExtensionOfClass()) {
        addRelatedDefinitions(classType);
        var extDef = new JSONDefinition(m, classType);
        // If this is in an allOf, remove the description
        extDef.description = null;
        extDef.properties = null;
        jDefinition.allOf = extDef.allOf;
      }
      // var propListDef = processHasProperties(classType);
      // jDefinition.allOf.add(propListDef);
    } else if (null != classType.getExtensionOfClass() && null == classType.hasPropertyList()) {
      addExtension(classType, jDefinition);
    } else if (null == classType.getExtensionOfClass() && null != classType.hasPropertyList()) {
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
    for (HasProperty property : classType.hasPropertyList()) {
      var prefix = property.getProperty().getNamespace().getNamespacePrefix();
      var name = property.getProperty().getName();

      if (!property.getProperty().isAbstract()) {
        // get the non-abstract properties
        propListDef.addProperty(property.getProperty());

        // Generate the required portion when applicable
        propListDef.setRequired(property);
      } else {
        // Find the non-abstract properties
        var naps = JSONSchemaHelper.getSubpropertiesOf(m, property.getProperty());
        propListDef.addProperties(naps);
        // Generate the required portion when applicable
        propListDef.setRequired(naps);
      }
    }
    return propListDef;
  }

  private void addRelatedDefinitions(ClassType classType) {
    var currentExtOf = classType.getExtensionOfClass();
    while (null != currentExtOf) {
      relatedDefinitions.add(new QualifiedName(currentExtOf));
      // addExtension2(currentExtOf);
      currentExtOf = currentExtOf.getExtensionOfClass();
    }
  }

  private void addSubDefinition(QualifiedName nameToAdd, JSONDefinition mainDefinition) {
    if (nameToAdd == null)
      return;
    if (JSONSchemaHelper.isIntrinsicType(nameToAdd.name))
      return;
    if (definitions.containsKey(nameToAdd.toString()))
      return;

    var newClassTypeToAdd = getClassTypeByName(nameToAdd.name);
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
      var jDefNewType = new JSONDefinition(m, newDataTypeToAdd);
      definitions.put(nameToAdd.toString(), jDefNewType);
    }
  }

  private ClassType getClassTypeByName(String name) {
    for (var c : m.getComponentList()) {
      if (null != c.asClassType()) {
        var cl = c.asClassType();
        if (cl.getName().equals(name))
          return cl;
      }
    }
    return null;
  }

  private Datatype getDataTypeByName(String name) {
    for (var c : m.getComponentList()) {
      if (null != c.asDatatype()) {
        var dt = c.asDatatype();
        if (dt.getName().equals(name))
          return dt;
      }
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

  private String getPropertyKind(Property p) {
    String rv = null;
    if (null != p.getClassType())
      rv = "owl:ObjectProperty";
    else if (null != p.getDatatype())
      rv = "owl:DataProperty";
    else {
      for (Component oc : m.getComponentList()) {
        Property op = oc.asProperty();
        if (null == op)
          continue;
        if (op.getSubPropertyOf() == p) {
          String rv2 = getPropertyKind(op);
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
    if (XSD_NS_URI.equals(c.getNamespace().getNamespaceURI()))
      return ("xsd:" + c.getName());
    else
      return c.getQName();
  }

  /*
   * private String getClassQName(Property p){
   * return p.getNamespace().getNamespacePrefix() + ":" + p.getName();
   * }
   */

  public LinkedHashMap<String, Object> getSchemas() {
    return items;
  }

  public void addExtension2(ClassType classType) {
    if (null != classType.getExtensionOfClass()) {
      var ext = classType.getExtensionOfClass();
      var jDef = new JSONDefinition(m, ext);
      jDef.description = classType.getDefinition();
      definitions.put(JSONSchemaHelper.generateLabel(classType), jDef);
    }
  }

  public void addExtension(ClassType classType, JSONDefinition jDefinition) {
    var extensionOf = classType.getExtensionOfClass();
    ClassType currentExtension = extensionOf.getExtensionOfClass();

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
          new OfRef(String.format("%s%s", JSONSchemaHelper.DEFINITIONS_TEXT, extensionOf.getQName())));

      var newDef = new OfDefinition("object");
      for (HasProperty property : classType.hasPropertyList()) {
        var prefix = property.getProperty().getNamespace().getNamespacePrefix();
        var name = property.getProperty().getName();

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
