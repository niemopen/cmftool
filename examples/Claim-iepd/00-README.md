## Insurance claim example

This is the example Webb used to illustrate the very first version of CMF.  It has been updated to NIEM 6.

* The model file *claim.cmf* is CMF version 0.8
* The *subset-xsd* directory holds a subset schema.
  * Attribute wildcards in *structures.xsd* support attribute augmentations, if wanted
* The *message-xsd* directory holds a message schema. 
  * No attribute wildcards in *structures.xsd*
  * Datatypes are represented as simple type declarations (e.g. `nc:TextType`)