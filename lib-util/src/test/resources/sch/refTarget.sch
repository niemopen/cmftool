<?xml version="1.0" encoding="UTF-8"?>
<schema 
  xmlns="http://purl.oclc.org/dsdl/schematron" 
  xmlns:xs="http://www.w3.org/2001/XMLSchema" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  queryBinding="xslt2">
  
  <title>Rules for reference schema documents</title>
  
  <xsl:include href="ndr-functions.xsl"/>
  
  <ns prefix="xs" uri="http://www.w3.org/2001/XMLSchema"/>
  <ns prefix="xsl" uri="http://www.w3.org/1999/XSL/Transform"/>
  <ns prefix="nf" uri="https://docs.oasis-open.org/niemopen/ns/specification/NDR/6.0/#NDRFunctions"/>
  <ns prefix="ct" uri="https://docs.oasis-open.org/niemopen/ns/specification/conformanceTargets/6.0/"/>
  <ns prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance"/>
  <ns prefix="appinfo" uri="https://docs.oasis-open.org/niemopen/ns/model/appinfo/6.0/"/>
  <ns prefix="structures" uri="https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/"/>
  
  <!-- NDR6 rule 7-10 (same as NDR5 rule 10-42, 11-14) -->
  <pattern id="rule7-10" xmlns="http://purl.oclc.org/dsdl/schematron">
    <title>Name of abstract properties</title>
    <rule context="xs:element[@name]">
      <report role="warning" test="not(exists(@abstract[xs:boolean(.) = true()])
        eq (ends-with(@name, 'Abstract')
        or ends-with(@name, 'AugmentationPoint')
        or ends-with(@name, 'Representation')))"
        >Rule 7-10: A Property object having an AbstractIndicator property with the value true SHOULD have a name ending in "Abstract" or "Representation"; all other components SHOULD NOT.</report>
    </rule>
  </pattern>
 
</schema>