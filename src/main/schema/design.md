---
title Design notes for NIEM Model Format Tool (NMFTool)
---

# What NMFTool does

The first capability for NMFTool is a converter that builds an instance of
the NIEM model format from a NIEM XML schema.

The input schema is defined by a pile of conforming XML Schema documents.  
An XML Catalog may also be provided to map from namespace identifier to
schema document.

The output is an XML document that follows the NIEM model format.  The
metamodel is the data model for the NIEM model format,

# Goals

1. Conversion round-tripping: The NIEM model format must contain all
   information needed to recreate the NIEM XML schema.  An exact
   character-by-character match is not required.
   
2. Minimize NIEM-specific concepts and data structures.  Omit schema
   components that carry no meaning.  Align with RDFS+OWL where
   possible.

# Changes to metamodel (alpha version)

## Classes have properties

In the initial version of the metamodel, object and data properties
are defined for ExtensionOf objects, not Class objects.  As a result,
every class with properties must be ultimately derived from a class
having no properties.  That is a NIEM-specific design; it works for
NIEM XML because every complex type directly or indirectly extends
SimpleObjectType.  However, that design is not _necessary_ for
conversion round-tripping, and it's not a _customary pattern_ in data
modeling.  In the NIEM model format produced by NMFTool, Class objects
have properties.

## Simple content is a DataProperty

In the initial version of the metamodel, almost every property is an
object property.  That is a NIEM-specific design.  Every element in
NIEM XML has complex content because of the attributes from
SimpleObjectAttributeGroup.  With the exception of @sequenceID, those
attributes are structural, not semantic.

NMFTool represents elements with simple content and no semantic
attributes as a DataProperty.  Proxy types are omitted.
SimpleCodeType types are omitted.

## Class has an ordered sequence of HasProperty elements

A child element can be a data property or an object property, so we
need a sequence of HasProperty elements that can represent either.

## HasProperty elements have a single Property element

In the initial version a HasObjectProperty element can have multiple
ObjectProperty children.  Likewise for HasDataProperty.  This supports
constructs like

    <xs:sequence minOccurs="2">
      <xs:element ref="foo"/>
      <xs:elsment ref="bar"/>
      
But you can't do that in NIEM.

