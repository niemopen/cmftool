## Simple content, no metadata or model attributes

Attributes from the `structures` namespace are not part of the model and don't count when converting to CMF.  So a complex type with simple content like this is converted to a CMF Datatype object.  

```
<xs:complexType name="TextType">         | <Datatype structures:id="nc.TextType">          
  <xs:simpleContent>                     |   <Name>TextType</Name>                         
    <xs:extension base="niem-xs:string"> |   <Namespace s:ref="nc" xsi:nil="true"/
    </xs:extension>                      |   <RestrictionOf>                               
  </xs:simpleContent>                    |     <Datatype s:ref="xs.string" xsi:nil
</xs:complexType>                        |   </RestrictionOf>                              
                                         | </Datatype                                      
```

## Simple content with model attributes

Add a model attribute to the complex type, and we get a CMF Class object with a created literal property. 

```
<xs:complexType name="TextType">                 | <Property s:id="nc.TextLiteral">                
  <xs:simpleContent>                             |   <Name>TextLiteral</Name>                      
    <xs:extension base="niem-xs:string">         |   <Namespace s:ref="nc" xsi:nil="true"/>        
      <xs:attribute ref="nc:partialIndicator" us |   <Datatype s:ref="xs.string" xsi:nil="true"/>  
    </xs:extension>                              | </Property>                                     
  </xs:simpleContent>                            | <Property s:id="nc.partialIndicator">           
</xs:complexType>                                |   <Name>partialIndicator</Name>                 
<xs:attribute name="partialIndicator" type="xs:b |   <Namespace s:ref="nc" xsi:nil="true"/>        
                                                 |   <Datatype s:ref="xs.boolean" xsi:nil="true"/> 
                                                 |   <AttributeIndicator>true</AttributeIndicator> 
                                                 | </Property>                                     
                                                 | <Class s:id="nc.TextType">                      
                                                 |   <Name>TextType</Name>                         
                                                 |   <Namespace s:ref="nc" xsi:nil="true"/>        
                                                 |   <HasProperty>                                 
                                                 |     <Property s:ref="nc.TextLiteral" xsi:nil="tr
                                                 |     <MinOccursQuantity>1</MinOccursQuantity>    
                                                 |     <MaxOccursQuantity>1</MaxOccursQuantity>    
                                                 |   </HasProperty>                                
                                                 |   <HasProperty>                                 
                                                 |     <Property s:ref="nc.partialIndicator" xsi:ni
                                                 |     <MinOccursQuantity>0</MinOccursQuantity>    
                                                 |     <MaxOccursQuantity>1</MaxOccursQuantity>    
                                                 |   </HasProperty>                                
                                                 | </Class>
```

## Simple content with metadata

Same thing happens if we use appinfo to indicate simple content with metadata.  (In NIEM 6 you have to declare this -- simple content does not have metadata by default.)  

```
<xs:complexType name="TextType"          | <Property structures:id="nc.TextLiteral">       
  appinfo:metadataIndicator="true">      |   <Name>TextLiteral</Name>                      
  <xs:simpleContent>                     |   <Namespace s:ref="nc" xsi:nil="true"/
    <xs:extension base="niem-xs:string"> |   <Datatype s:ref="xs.string" xsi:nil="
    </xs:extension>                      | </Property>                                     
  </xs:simpleContent>                    | <Class s:id="nc.TextType">             
</xs:complexType>                        |   <Name>TextType</Name>                         
                                         |   <Namespace s:ref="nc" xsi:nil="true"/
                                         |   <HasProperty>                                 
                                         |     <Property s:ref="nc.TextLiteral" xs
                                         |     <MinOccursQuantity>1</MinOccursQuantity>    
                                         |     <MaxOccursQuantity>1</MaxOccursQuantity>    
                                         |   </HasProperty>                                
                                         |   <MetadataIndicator>true</MetadataIndicator>   
                                         | </Class>
```

## Inheritance and created literal properties

It is the lowest type with model attributes or metadata that gets a created literal property.  Derived types may add attributes but don't get or need a literal property.

```
<xs:complexType name="PersonNameTextType">       | <Class s:id="nc.PersonNameTextType">            
  <xs:simpleContent>                             |   <Name>PersonNameTextType</Name>               
    <xs:extension base="nc:ProperNameTextType">  |   <Namespace s:ref="nc" xsi:nil="true"/>        
      <xs:attribute ref="nc:personNameInitialInd |   <ExtensionOfClass s:ref="nc.ProperNameTextType
    </xs:extension>                              |   <HasProperty>                                 
  </xs:simpleContent>                            |     <Property s:ref="nc.personNameInitialIndicat
</xs:complexType>                                |     <MinOccursQuantity>0</MinOccursQuantity>    
<xs:complexType name="ProperNameTextType">       |     <MaxOccursQuantity>1</MaxOccursQuantity>    
  <xs:simpleContent>                             |   </HasProperty>                                
    <xs:extension base="nc:TextType"/>           | </Class>                                        
  </xs:simpleContent>                            | <Class s:id="nc.ProperNameTextType">            
</xs:complexType>                                |   <Name>ProperNameTextType</Name>               
<xs:complexType name="TextType">                 |   <Namespace s:ref="nc" xsi:nil="true"/>        
  <xs:simpleContent>                             |   <ExtensionOfClass s:ref="nc.TextType" xsi:nil=
    <xs:extension base="niem-xs:string">         | </Class>                                        
      <xs:attribute ref="nc:partialIndicator" us | <Class s:id="nc.TextType">                      
    </xs:extension>                              |   <Name>TextType</Name>                         
  </xs:simpleContent>                            |   <Namespace s:ref="nc" xsi:nil="true"/>        
</xs:complexType>                                |   <HasProperty>                                 
                                                 |     <Property s:ref="nc.TextLiteral" xsi:nil="tr
                                                 |     <MinOccursQuantity>1</MinOccursQuantity>    
                                                 |     <MaxOccursQuantity>1</MaxOccursQuantity>    
                                                 |   </HasProperty>                                
                                                 |   <HasProperty>                                 
                                                 |     <Property s:ref="nc.partialIndicator" xsi:ni
                                                 |     <MinOccursQuantity>0</MinOccursQuantity>    
                                                 |     <MaxOccursQuantity>1</MaxOccursQuantity>    
                                                 |   </HasProperty>                                
                                                 | </Class>
```

In the following example, `TextType` and `ProperNameType` are Datatype objects, because they don't have model attributes.  So it's `PersonNameType` that gets the literal property. 

```
<xs:complexType name="PersonNameTextType">       | <Property s:id="nc.PersonNameTextLiteral">      
  <xs:simpleContent>                             |   <Name>PersonNameTextLiteral</Name>            
    <xs:extension base="nc:ProperNameTextType">  |   <Namespace s:ref="nc" xsi:nil="true"/>        
      <xs:attribute ref="nc:personNameInitialInd |   <Datatype s:ref="nc.ProperNameTextType" xsi:ni
    </xs:extension>                              | </Property>                                     
  </xs:simpleContent>                            | <Class s:id="nc.PersonNameTextType">            
</xs:complexType>                                |   <Name>PersonNameTextType</Name>               
<xs:complexType name="ProperNameTextType">       |   <Namespace s:ref="nc" xsi:nil="true"/>        
  <xs:simpleContent>                             |   <HasProperty>                                 
    <xs:extension base="nc:TextType"/>           |     <Property s:ref="nc.PersonNameTextLiteral" x
  </xs:simpleContent>                            |     <MinOccursQuantity>1</MinOccursQuantity>    
</xs:complexType>                                |     <MaxOccursQuantity>1</MaxOccursQuantity>    
<xs:complexType name="TextType">                 |   </HasProperty>                                
  <xs:simpleContent>                             |   <HasProperty>                                 
    <xs:extension base="niem-xs:string">         |     <Property s:ref="nc.personNameInitialIndicat
    </xs:extension>                              |     <MinOccursQuantity>0</MinOccursQuantity>    
  </xs:simpleContent>                            |     <MaxOccursQuantity>1</MaxOccursQuantity>    
</xs:complexType>                                |   </HasProperty>                                
                                                 | </Class>                                        
                                                 | <Datatype s:id="nc.ProperNameTextType">         
                                                 |   <Name>ProperNameTextType</Name>               
                                                 |   <Namespace s:ref="nc" xsi:nil="true"/>        
                                                 |   <RestrictionOf>                               
                                                 |     <Datatype s:ref="nc.TextType" xsi:nil="true"
                                                 |   </RestrictionOf>                              
                                                 | </Datatype>                                     
                                                 | <Datatype s:id="nc.TextType">                   
                                                 |   <Name>TextType</Name>                         
                                                 |   <Namespace s:ref="nc" xsi:nil="true"/>        
                                                 |   <RestrictionOf>                               
                                                 |     <Datatype s:ref="xs.string" xsi:nil="true"/>
                                                 |   </RestrictionOf>                              
                                                 | </Datatype>
```

## SimpleType definitions

NIEM 5 XSD often requires `FooType` and `FooSimpleType` definitions.  The `FooSimpleType` definitions usually do not appear in the CMF, and never under that name.  There is usually only a Datatype object for `FooType`.

```
<xs:simpleType name="AngularMinuteSimpleType">   | <Datatype structures:id="nc.AngularMinuteType"> 
  <xs:restriction base="xs:decimal">             |    <Name>AngularMinuteType</Name>               
    <xs:minInclusive value="0"/>                 |    <Namespace structures:ref="nc" xsi:nil="true"
    <xs:maxExclusive value="60"/>                |    <DefinitionText>A data type for a minute of a
  </xs:restriction>                              |    <RestrictionOf>                              
</xs:simpleType>                                 |      <Datatype structures:ref="xs.decimal" xsi:n
<xs:complexType name="AngularMinuteType">        |      <MaxExclusive>                             
  <xs:simpleContent>                             |        <StringValue>60.0</StringValue>          
    <xs:extension base="nc:AngularMinuteSimpleTy |        <DefinitionText>The highest value allowed
      <xs:attributeGroup ref="structures:SimpleO |      </MaxExclusive>                            
    </xs:extension>                              |      <MinInclusive>                             
  </xs:simpleContent>                            |        <StringValue>0.0</StringValue>           
</xs:complexType>                                |        <DefinitionText>The lowest value allowed 
                                                 |      </MinInclusive>                            
                                                 |    </RestrictionOf>                             
                                                 |  </Datatype>
```

But if the `FooType` has a model attribute or metadata, then we're going to get a ClassType object with a created literal property.  We may need a Datatype object for that literal.  Note the name of that Datatype object:  `AngularMinuteDatatype`.

```
<xs:simpleType name="AngularMinuteSimpleType">   | <Property s:id="nc.AttributedAngularMinuteLitera
  <xs:restriction base="xs:decimal">             |   <Name>AttributedAngularMinuteLiteral</Name>   
    <xs:minInclusive value="0"/>                 |   <Namespace s:ref="nc" xsi:nil="true"/>        
    <xs:maxExclusive value="60"/>                |   <Datatype s:ref="nc.AngularMinuteLiteralType" 
  </xs:restriction>                              | </Property>                                     
</xs:simpleType>                                 | <Class s:id="nc.AttributedAngularMinuteType">   
<xs:complexType name="AttributedAngularMinuteTyp |   <Name>AttributedAngularMinuteType</Name>      
  <xs:simpleContent>                             |   <Namespace s:ref="nc" xsi:nil="true"/>        
    <xs:extension base="nc:AngularMinuteSimpleTy |   <HasProperty>                                 
      <xs:attributeGroup ref="s:SimpleObjectAttr |     <Property s:ref="nc.AttributedAngularMinuteL
      <xs:attribute ref="nc:SomeAtt"/>           |     <MinOccursQuantity>1</MinOccursQuantity>    
    </xs:extension>                              |     <MaxOccursQuantity>1</MaxOccursQuantity>    
  </xs:simpleContent>                            |   </HasProperty>                                
</xs:complexType>                                |   <HasProperty>                                 
<xs:attribute name="SomeAtt" type="xs:token"/>   |     <Property s:ref="nc.SomeAtt" xsi:nil="true"/
                                                 |     <MinOccursQuantity>0</MinOccursQuantity>    
                                                 |     <MaxOccursQuantity>1</MaxOccursQuantity>    
                                                 |   </HasProperty>                                
                                                 | </Class>                                        
                                                 | <Datatype s:id="nc.AngularMinuteDatatype">   
                                                 |   <Name>AngularMinuteLiteralType</Name>         
                                                 |   <Namespace s:ref="nc" xsi:nil="true"/>        
                                                 |   <RestrictionOf>                               
                                                 |     <Datatype s:ref="xs.decimal" xsi:nil="true"/
                                                 |     <MaxExclusive>                              
                                                 |       <StringValue>60.0</StringValue>           
                                                 |     </MaxExclusive>                             
                                                 |     <MinInclusive>                              
                                                 |       <StringValue>0.0</StringValue>            
                                                 |     </MinInclusive>                             
                                                 |   </RestrictionOf>                              
                                                 | </Datatype>
```

## NIEM 6 XSD and simple content

We don't need proxy types in NIEM 6.   We usually don't need complex types with simple content.  Datatype objects can be represented by simple type definitions, like this:

```
<Datatype s:id="nc.AngularMinuteType">           | <xs:simpleType name="AngularMinuteType">
  <Name>AngularMinuteLiteralType</Name>          |   <xs:restriction base="xs:decimal">    
  <Namespace s:ref="nc" xsi:nil="true"/>         |     <xs:minInclusive value="0"/>        
  <RestrictionOf>                                |     <xs:maxExclusive value="60"/>       
    <Datatype s:ref="xs.decimal" xsi:nil="true"/ |   </xs:restriction>                     
    <MaxExclusive>                               | </xs:simpleType>                        
      <StringValue>60.0</StringValue>            |                                         
    </MaxExclusive>                              |                                         
    <MinInclusive>                               |                                         
      <StringValue>0.0</StringValue>             |                                         
    </MinInclusive>                              |                                         
  </RestrictionOf>                               |                                         
</Datatype>                                      |                                         
```

The practical effect of including the complex type declaration with `SimpleObjectAttributeGroup` was to apply the six attributes in the structures namespace to all simple content.  But of those six, four are not allowed on simple content in NIEM 6 (`id`, `ref`, `uri`, and `referenceMetadata`).  A fifth does not appear in NIEM 6 at all (`sequenceID`).  You can have `metadata` on simple content, but you have to declare it in XSD, and in NIEM 6 you could do it like this:

```
<xs:complexType name="TextType"        | <Class structures:id="nc.TextType">             
  appinfo:metadataIndicator="true">    |   <Name>TextType</Name>                         
  <xs:simpleContent>                   |   <Namespace structures:ref="nc" xsi:nil="true"/
    <xs:extension base="xs:string">    |   <HasProperty>                                 
      <xs:attribute ref="s:metadata"/> |     <Property structures:ref="nc.TextLiteral" xs
    </xs:extension>                    |     <MinOccursQuantity>1</MinOccursQuantity>    
  </xs:simpleContent>                  |     <MaxOccursQuantity>1</MaxOccursQuantity>    
</xs:complexType>                      |   </HasProperty>                                
                                       |   <MetadataIndicator>true</MetadataIndicator>   
                                       | </Class>                                        
```

It doesn't look like `appinfo:metadataIndicator` is doing much work.  Perhaps we don't need it in NIEM 6.