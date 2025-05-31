package org.mitre.niem.json;

import com.google.gson.annotations.SerializedName;
import org.mitre.niem.cmf.Component;
import org.mitre.niem.cmf.Namespace;
import org.mitre.niem.cmf.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class JSONProperty {
  private transient Property property;
  public String description;
  @SerializedName("$ref")
  public String ref;
  public String type;
  public String format;
  public JSONPropertyType items = null;
  public Integer minItems = null;
  public Integer maxItems = null;
  public ArrayList<Of> oneOf = null;
  public ArrayList<Of> anyOf = null;
  private transient HashMap<String, OfRef> anyOfHashMap = new HashMap<>();
  private transient HashMap<String, Cardinality> cardinalities;
  private transient Namespace namespace = null;
  private transient boolean isIntrinsicType = false;
  // Used to track anyOfs to prevent duplicates

  public transient HashMap<Cardinality, String> anyOfs = new HashMap<>();

  public transient boolean usedInClasses = false;

  public JSONProperty(Property property) {
    this.property = property;
    this.namespace = property.namespace();
    this.description = property.definition();
    usedInClasses = isPropertyUsedInClasses();
    cardinalities = getCardinalities();

    // TODO: Do I need to do something special when there are multiple
    // cardinalities?
    for (var card : cardinalities.keySet()) {
      setMinAndMaxItemValues(cardinalities.get(card));
    }

    process();
  }

  public boolean isAbstract() {
    return property.isAbstract();
  }

  public String getClassQName() {
    return property.qname();
  }

  private HashMap<String, Cardinality> getCardinalities() {
    HashMap<String, Cardinality> cardinalities = new HashMap<>();

    // First, determine what property to search on for cardinalities
    // Whenever a property is a subproperty of another property
    // search on the "subpropertyOf" name
    ArrayList<Property> propsToCheck = new ArrayList<>();

    if (property.subPropertyOf() != null) {
      propsToCheck.add(property.subPropertyOf());
      propsToCheck.add(property);
    } else {
      propsToCheck.add(property);
    }

    for (var prop : propsToCheck) {
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

  private void setMinAndMaxItemValues(Cardinality c) {
    if (c == null)
      return;

    // do not set values for the following situations
    // cardinality of 1 to 1
    // cardinality of 0 to unbounded
    // cardinality of 1 to unbounded
    if (c.isMaxUnbounded())
      return;
    if (c.getMinOccurs() == 1 && c.getMaxOccurs() == 1)
      return;

    // cardinality min > 1 and max > min
    if (c.getMinOccurs() > 1 && c.getMaxOccurs() > c.getMinOccurs()) {
      minItems = c.getMinOccurs();
      maxItems = c.getMaxOccurs();
      return;
    }

    // Cardinality of n to m
    if (c.getMinOccurs() == 0 && c.getMaxOccurs() > 1) {
      maxItems = c.getMaxOccurs();
      return;
    }

  }

  private String genRef() {
    String nsPrefix = property.namespace().prefix();

    if (property.subPropertyOf() != null) {
      nsPrefix = property.subPropertyOf().namespace().prefix();
      if (property.classType() != null && property.subPropertyOf().isAbstract()) {
        return String.format("%s%s:%s", JSONSchemaHelper.DEFINITIONS_TEXT, nsPrefix, property.classType().name());
      }
      return String.format("%s%s:%s", JSONSchemaHelper.DEFINITIONS_TEXT, nsPrefix,
          property.subPropertyOf().name());
    } else if (property.datatype() != null) {
      if (isIntrinsicType) {
        return property.datatype().name();
      } else {
        nsPrefix = property.datatype().namespace().prefix();
        return String.format("%s%s:%s", JSONSchemaHelper.DEFINITIONS_TEXT, nsPrefix, property.datatype().name());
      }
    } else if (property.classType() != null) {
      nsPrefix = property.classType().namespace().prefix();
      return String.format("%s%s:%s", JSONSchemaHelper.DEFINITIONS_TEXT, nsPrefix, property.classType().name());
    }
    return "";
    // throw new Exception("Property " + property.name() + " has no data type or
    // class defined");
  }

  private boolean isPropertyUsedInClasses() {
    String searchName = property.name();

    if (property.subPropertyOf() != null) {
      searchName = property.subPropertyOf().name();
    }

    // See if the property is used in any classes
    for (var cl : property.model().classTypeL()) {

        for (var cls : cl.propL()) {
          if (cls.property().name().equals(searchName)) {
            return true;
          }
        }
      
    }
    return false;
  }

  private void processDataType() {
    var dataType = property.datatype();
    String dataTypeName = dataType.name();
    isIntrinsicType = JSONSchemaHelper.isIntrinsicType(dataTypeName);

    var propName = (property.subPropertyOf() != null) ? property.subPropertyOf().name() : property.name();

    // intrinsic types are handled differently than others, regardless of
    // cardinality
    if (cardinalities.size() == 0) {
      // default cardinality
      ref = genRef();
    } else if (cardinalities.size() > 0) {
      // Check for cardinality
      for (var cardKey : cardinalities.keySet()) {
        var card = cardinalities.get(cardKey);
        if (card != null && card.getPropertyName() == propName) {

          // Unbounded scenarios
          if (card.isMaxUnbounded()) {
            if (card.getMinOccurs() == 0) {
              type = "array";
              items = new JSONPropertyType(JSONSchemaHelper.DEFINITIONS_TEXT,
                  property.namespace().prefix(), dataTypeName);
            } else if (card.getMinOccurs() == 1) {
              type = "array";
              items = new JSONPropertyType(JSONSchemaHelper.DEFINITIONS_TEXT,
                  property.namespace().prefix(), dataTypeName);
            } else {
              System.out.println("???");
            }
          } else {
            /* 0 to 1 cardinality */
            if (card.getMinOccurs() == 0 && card.getMaxOccurs() == 1) {
              if (isIntrinsicType) {
                type = genRef();
              } else {
                ref = genRef();
              }
            }
            /* 1 to 1 */
            else if (card.getMinOccurs() == 1 && card.getMaxOccurs() == 1) {
              if (isIntrinsicType) {
                if (dataTypeName.equals("date")) {
                  type = "string";
                  format = "date";
                } else {
                  type = dataTypeName;
                }
              } else {
                ref = JSONSchemaHelper.DEFINITIONS_TEXT + dataType.qname();
              }
              /* 1 to unbounded */
            } else if (card.getMinOccurs() < card.getMaxOccurs()) {
              type = "array";
              items = new JSONPropertyType(JSONSchemaHelper.DEFINITIONS_TEXT,
                  property.namespace().prefix(), dataTypeName);
            }
          }
        }
      }
    } else {
      type = "array";
      items = new JSONPropertyType(JSONSchemaHelper.DEFINITIONS_TEXT, property.namespace().prefix(),
          dataTypeName);
    }
  }

  private void processClassType() {
    String classTypeName = property.classType().name();
    isIntrinsicType = JSONSchemaHelper.isIntrinsicType(classTypeName);

    if (property.isReferenceable()) {
      TreeMap<String, TreeMap> properties = new TreeMap<>();
      TreeMap<String, String> typeMap = new TreeMap<>();
      typeMap.put("type", "string");
      properties.put("@id", typeMap);

      anyOf = new ArrayList<>();
      anyOf.add(new OfProperty("object", properties));
    }

    if (!usedInClasses) {
      /**********************************************************************
       * It also covers the situation where there are classes not used
       * in other classes.
       *********************************************************************/
      if (anyOf == null) {
        anyOf = new ArrayList<>();
      }

      var ofRef = new OfRef(genRef());
      anyOfHashMap.put(ofRef.toString(), ofRef);
      anyOf.add(ofRef);
      anyOf.add(new OfItem("array", genRef(), null));

      for (var cardKey : cardinalities.keySet()) {
        var propName = cardinalities.get(cardKey).getPropertyName();
        var nsPrefix = cardinalities.get(cardKey).prefix();
        // keep out duplicate references
        ofRef = new OfRef(String.format("%s%s:%s", JSONSchemaHelper.DEFINITIONS_TEXT, nsPrefix, propName));
        if (!anyOfHashMap.containsKey(ofRef.toString())) {
          anyOf.add(ofRef);
          anyOfHashMap.put(ofRef.toString(), ofRef);
        }
      }
    } else {
      // iterate through the cardinalities and add those that apply
      var ofs = getStuff(property, cardinalities);
      for (Of of : ofs) {
        if (of instanceof OfItem) {
          var oi = (OfItem) of;
          var items = oi.getItems();
          for (var item : items.keySet()) {
            if (anyOf != null) {
              anyOf.add(new OfItem("array", items.get(item), oi.getCardinality()));
            } else {
              this.type = "array";
              this.items = new JSONPropertyType(items.get(item));
            }
          }
        } else if (of instanceof OfClass) {
          var oc = (OfClass) of;
          if (anyOf != null) {
            anyOf.add(new OfClass(oc.getReference()));
          } else {
            this.ref = oc.getReference();
            // this.items =
            // new JSONPropertyType(JSONSchemaHelper.DEFINITIONS_TEXT,
            // property.namespace().prefix(), oc.getReference());
          }
        } else {
          System.out.println("???");
        }
      }
    }
  }

  private void process() {
    /*****************************************
     * Rules for data types
     *****************************************/
    if (property.datatype() != null) {
      processDataType();
    }
    /*****************************************
     * Rules for classes
     *****************************************/
    else if (property.classType() != null) {
      processClassType();
    }
  }

  private ArrayList<Of> getStuff(Property property, HashMap<String, Cardinality> cardinalities) {
    ArrayList<Of> ofs = new ArrayList<>();
    for (var cardKey : cardinalities.keySet()) {
      var c = cardinalities.get(cardKey);
      /**************************************************************************
       * Class Types
       *************************************************************************/
      if (property.classType() != null) {
        if (c.isMaxUnbounded()) {
          if (c.getMinOccurs() == 0) {
            // zero to unbounded
            ofs.add(new OfItem("array", genRef(), c));
          } else if (c.getMinOccurs() == 1) {
            ofs.add(new OfItem("array", genRef(), c));
          }
        }
        // Fixed upper limit
        else {
          if (c.getMinOccurs() == 0 && c.getMaxOccurs() == 1) {
            ofs.add(new OfClass(genRef()));
          } else if (c.getMinOccurs() == 1 && c.getMaxOccurs() == 1) {
            ofs.add(new OfClass(genRef()));
          } else if (c.getMinOccurs() < c.getMaxOccurs()) {
            System.out.println("???");
          }
        }
      }
      /**************************************************************************
       * Data Types
       *************************************************************************/
      else if (property.datatype() != null) {
        // Unbounded
        if (c.isMaxUnbounded()) {
          if (c.getMinOccurs() == 0) {
            ofs.add(new OfItem("array", genRef(), c));
            return ofs;
          }
        }
        // Fixed upper limit
        else {
          if (c.getMinOccurs() == 0 && c.getMaxOccurs() == 1) {
            System.out.println("getStuff:getDataType:0-to-1");
          } else if (c.getMinOccurs() == 1 && c.getMaxOccurs() == 1) {
            System.out.println("getStuff:getDataType:1-to-1");
          } else if (c.getMinOccurs() < c.getMaxOccurs()) {
            System.out.println("getStuff:getDataType:min<max");
          }
        }
      }
      /**************************************************************************
       * Neither ClassType nor DataType
       *************************************************************************/
      else {
        return null;
      }
    }
    return ofs;
  }

}
