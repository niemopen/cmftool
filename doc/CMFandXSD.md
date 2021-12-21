## Complex content

A type definition with complex content always maps to a Class object in CMF.

**NIEM XSD:**

```xml
<xs:complexType name="PersonNameType">
  <xs:annotation>
    <xs:documentation>A data type for a combination...
  </xs:annotation>
  <xs:complexContent>
    <xs:extension base="structures:ObjectType">
      <xs:sequence>
        <xs:element ref="nc:PersonGivenName" minOccurs="0" maxOccurs="unbounded"/>
        <xs:element ref="nc:PersonMiddleName" minOccurs="0  maxOccurs="unbounded"/>
        <xs:element ref="nc:PersonSurName" minOccurs="0" maxOccurs="unbounded"/>
      </xs:sequence>
      <xs:attribute ref="nc:personNameCommentText" use="optional"/>
    </xs:extension>
  </xs:complexContent>
</xs:complexType>
```

**NIEM CMF:**

```xml
<Class structures:uri="nc:PersonNameType">
  <Name>PersonNameType</Name>
  <Namespace structures:uri="nc" xsi:nil="true"/>
  <DefinitionText>A data type for a combination of names and/or titles by which a person is known.</DefinitionText>
  <HasProperty mm:maxOccursQuantity="unbounded" mm:minOccursQuantity="0" structures:sequenceID="1">
    <Property structures:uri="nc:PersonGivenName" xsi:nil="true"/>
  </HasProperty>
  <HasProperty mm:maxOccursQuantity="unbounded" mm:minOccursQuantity="0" structures:sequenceID="2">
    <Property structures:uri="nc:PersonMiddleName" xsi:nil="true"/>
  </HasProperty>
  <HasProperty mm:maxOccursQuantity="unbounded" mm:minOccursQuantity="0" structures:sequenceID="3">
    <Property structures:uri="nc:PersonSurName" xsi:nil="true"/>
  </HasProperty>
  <HasProperty mm:maxOccursQuantity="1" mm:minOccursQuantity="0">
    <Property structures:uri="nc:personNameCommentText" xsi:nil="true"/>
  </HasProperty>
</Class>
```

