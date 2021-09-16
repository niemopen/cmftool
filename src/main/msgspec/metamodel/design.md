---
title Design notes for NIEM Model Format Tool (NMFTool)
---

# What NMFTool does

The first capability for NMFTool is a converter that builds a NIEM model
instance (that is, an instance of the NIEM model format) from a NIEM XML schema.

The input schema is defined by a pile of conforming XML Schema documents.  
An XML Catalog may also be provided to map from namespace identifier to
schema document.

The output is an XML document that follows the NIEM model format.  The
metamodel is the data model for the NIEM model format.

# Goals

1. Conversion round-tripping: The NIEM model instance must contain all
   information needed to recreate the NIEM XML schema.  (An exact
   character-by-character match in the schema documents or schema pile
   directory structure is not required.)
   
2. Minimize NIEM-specific concepts and data structures.  Omit schema
   components that carry no meaning.  Align with RDFS+OWL where
   possible.

# Changes to the metamodel and model instance

## Classes have properties

In the initial version of the metamodel, object and data properties
are defined for ExtensionOf objects, not Class objects.  That design
might have been driven by XML Schema, in which the elements of a
complex type are defined inside the <xs:extension> element.  But it's
strange for every other kind of modeling I can think of -- you have an
IS-A relationship between class and subclass, but the new properties
belong to the subclass, not the relationship.  So I changed this.  The
resulting metamodel is simpler and made my software easier to write.

## Attributes from the structures namespace are omitted

With the exception of @sequenceID, the attributes in the structures
namespace are there for NIEM mechanics; none of them contribute any
semantics to the data model.  (And I want to replace @sequenceID
anyway in NIEM 6.)  So I leave them out of the model instance.  We can
put them back in when we generate XML schema documents from the model
instance.

## FooSimpleType types are omitted

We have FooType and FooSimpleType in order to attach the attributes
from the structures namespace, and we don't need those in the model 
instance.  So instead the model instance contains a Datatype object 
named FooType that represents the simple type definition.  

## FooCodeSimpleType types are retained

I thought about omitting these as well.  But in RDF, a code list is a 
class, and the code values are individuals of that class.  So FooCodeType
is a ClassType object in the model instance, and its HasValue property
is the Datatype object for FooCodeSimpleType.

## Augmentations are preserved

I thought about omitting augmentation points and augmentation types, because 
these don't convey data semantics.  But if they aren't in the model instance,
then we can't recreate the XML schema documents.  Also, augmentations do
convey _some_ meaning; specifically, an augmentation in namespace Foo tells
us that the Foo community thinks this property is important.

## No separate classes for object and data properties

I'm puzzled by the DataProperty class in the version 1 metamodel.
There aren't any DataProperty elements in Webb's insurance claim
example.  If I'm understanding correctly, the only DataProperties in a
model instance would be for the schema primitives -- xs:string and so
forth.  That isn't very useful.

In RDF land, an object property has a URI or blank node for a value; a
data property has a scalar value, like this:

    _:b1 ns:ObjProp  _:b2 .
    _:b2 ns:DataProp "Hello" .

I was able to collapse DataProperty and ObjectProperty into one, a
Property class and a HasProperty class.  A data property is then a
Property that either has a Datatype, or has a ClassType with a
HasValue and no HasProperty elements.  An object property has a
ClassType with HasProperty elements.  This corresponds to the usage in
RDF land.

## HasProperty elements have a single Property element

In the initial version a HasObjectProperty element can have multiple
ObjectProperty children.  Likewise for HasDataProperty.  This supports
constructs like

    <xs:sequence minOccurs="2">
      <xs:element ref="foo"/>
      <xs:element ref="bar"/>
      
But you can't do that in NIEM, so I simplified it away.

## Datatype has a ListOf property

It has to go somewhere...

## Other changes

* Updated to NIEM 5.0
* Don't need ContentStyleCode
* Don't need ExtensionOfType
* Don't need HasValueType
* Don't need SubPropertyOfType
* DefinitionText is optional in facets
* WhiteSpaceFacetCodeType became WhiteSpaceValueCodeType

