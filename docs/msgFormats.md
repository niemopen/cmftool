# Building message formats with CMFTool

This article describes how to use CMFTool to construct a *message format*. That process looks like this:

* Begin with a *message type* specification
* Create mappings to simple property names, if desired
* Generate a *message schema* and perhaps a JSON-LD context
* Test the message schema by validating example *messages*

This article defines all of those terms and provides illustrative examples of the process.

### 1.1 Messages

A *message* is a package of data that can be exchanged at runtime.  Here are two NIEM messages, one in XML, the other in JSON format.  (For brevity and clarity, the lines in the following examples may be truncated, closing tags and brackets omitted, etc.  Complete examples are usually available through a link. For example, [xml-msg](examples/request/formats/xcanon/examples/valid01.xml) and [json-msg](examples/request/formats/jcanon/examples/valid01.json) are the complete messages shown partially in example 1 below.)

```
<msg:Request                                                            | {
  xmlns:msg="http://example.com/Request/1.0/"                           |   "@context": "http://example.com/Request/1.0/
  xmlns:nc="https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.  |   "msg:Request": {
  <msg:RequestID>R012</msg:RequestID>                                   |     "msg:RequestID": "R012",
  <msg:RequestedItem>                                                   |     "msg:RequestedItem": [
    <nc:ItemName>Wrench</nc:ItemName>                                   |       {
    <nc:ItemQuantity>1</nc:ItemQuantity>                                |         "nc:ItemName": "Wrench",
  </msg:RequestedItem>                                                  |         "nc:ItemQuantity": {
  <msg:RequestedItem>                                                   |           "nc:QuantityLiteral": 1
    <nc:ItemName>Screw</nc:ItemName>                                    |         }
    <nc:ItemQuantity nc:quantityUnitText="dozen">5</nc:ItemQuantity     |       },
  </msg:RequestedItem>                                                  |       {
</msg:Request>                                                          |         "nc:ItemName": "Screw",
                                                                        |         "nc:ItemQuantity": {
                                                                        |           "nc:quantityUnitText": "dozen",
                                                                        |           "nc:QuantityLiteral": 5
                                                                        |         }
```
<figcaption><a name="ex1">Example 1: Equivalent NIEM messages in XML and JSON syntax</a></figcaption>

### 1.2 Message types

Both of those messages are instances of the same *message type*.  A message type specifies the mandatory and optional content of conforming messages and defines the meaning of that content.  That specification is captured in a *message model*, which may be represented as a CMF file or as an XML Schema (XSD) document pile.  A message type also specifies the *message property*, the root node of the message data; in this message type, `msg:Request`.

<pre class="blk"><code>
$ tree request
├── <a href="examples/request/README.md">README.md</a>
├── formats                      <i>-- message formats will be created under this directory</i>
├── <a href="examples/request/model.cmf">model.cmf</a>                    <i>-- the message model in CMF</i>
└── model.xsd                    <i>-- same message model in XSD</i>
    ├── niem
    │   ├── adapters
    │   │   └── niem-xs.xsd
    │   ├── <a href="examples/request/model.xsd/niem/niem-core.xsd">niem-core.xsd</a>        <i>-- subset schema of the NIEM Core namespace</i>
    │   └── utility
    │       ├── appinfo.xsd
    │       └── structures.xsd
    └── <a href="examples/request/model.xsd/request.xsd">request.xsd</a>              <i>-- the extension schema for the Request message type</i>
</code></pre>
<figcaption><a name="ex2">Example 2: The "Request" message type</a></figcaption>

### 1.3 Message formats

A message type does not itself specify the syntax of conforming messages.  That is done by a message format.  A message format has a *message schema*, which may be used to validate instance messages.  That schema will be in XSD for XML messages, in JSON Schema for JSON messages, etc.  [Example 3](#ex3) shows the message format for the XML message in [example 1](#ex1).  (Examples showing the CMFTool commands to generate a message format from a message type are provided later on.)

<pre class="blk"><code>
$ tree formats/xcanon
├── examples
│   ├── <a href="examples/request/formats/xcanon/examples/invalid01.xml">invalid01.xml</a>
│   └── <a href="examples/request/formats/xcanon/examples/valid01.xml">valid01.xml</a>
└── message.xsd
    ├── niem
    │   └── <a href="examples/request/formats/xcanon/message.xsd/niem/niem-core.xsd">niem-core.xsd</a>    <i>-- a message schema document, doesn't have to follow NDR</i>
    └── <a href="examples/request/formats/xcanon/message.xsd/request.xsd">request.xsd</a>          <i>-- a message schema document, doesn't have to follow NDR</i>
</code></pre>
<figcaption><a name="ex3">Example 3: An XML message format</a></figcaption>

A message schema is entirely about syntax, and therefore is not required to capture all of the information in a NIEM model.  For this reason, the XSD for a message schema can be much simpler than the XSD for the message model.  That XSD can also be used for code binding tools (for example, JAXB).  The message schema is most often used to validate messages during development and testing.

### 1.4 Equivalent messages

Any NIEM message can be converted from one message format to another format of the same message type, without changing the information content.  The two messages in [example 1](#ex1) are equivalent in that way.  

RDF is the interpretive framework for NIEM model and message semantics. The information content of a message is defined as the RDF graph entailed by that message.  (Details provided for the curious in section 14 of the Naming and Design Rules v6.0.)  The messages in [example 1](#ex1) are equivalent because they both entail the following RDF:

```
@prefix msg: http://example.com/Request/1.0/ . 
@prefix nc: https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/ . 
_:b0 msg:Request _:b1 .
_:b1 a msg:RequestType; msg:RequestID "R012" ; msg:RequestedItem _:b2, _:b4 .
_:b2 a nc:ItemType; nc:ItemName "Wrench" ; nc:ItemQuantity _:b3 .
_:b3 a nc:QuantityType; nc:QuantityLiteral 1 .
_:b4 a nc:ItemType; nc:ItemName "Screw" ; nc:ItemQuantity _:b5 .
_:b5 a nc:QuantityType; nc:QuantityLiteral 5 ; nc:quantityUnitText "dozen" .
```

### 1.5 Simple and canonical message formats

Component names in a NIEM model are composed of a namespace prefix, a colon, and a local name that follows ISO 11179 naming rules; for example, `nc:ItemName`, `nc:PersonSurName`.  Using ISO 11179 naming helps to ensure that model terms are precise, unambiguous, and consistently interpreted in models that are constructed and used by many developers in many organizations. Names of this form are also easily converted into resource identifiers in RDF graphs.  Messages like those in [example 1](#ex1), which use NIEM model names as XML element tags or JSON object keys are known as *canonical messages*, and are instances of a *canonical message format*.

Developers often prefer to work with short, simplified tags and keys; for example, `lname` instead of `nc:PersonSurName`.  These names are quicker to type, easier to scan on the screen, and often map more naturally to variables and fields in application code.  The developer community around a message type first learns how their preferred names correspond to the message model, and then relies on that shared understanding, without needing to encode semantics into every tag or key.

NIEM allows a message designer to define mappings between the properties in the message model and the simple names preferred by developers.  These mappings are then used to create a *simple message format*.  A message of that format uses the simple names, but has an equivalent canonical message, and can be transformed to or from the canonical format at need.  The message in [example 4](#ex4) below are simple messages equivalent to the canonical messages in [example 1](#ex1).

```
<request                                                    | {
  xmlns="http://example.com/Request/1.0/xsimple"            |   "@context": "http://example.com/Request/1.0/jsimple",
  xmlns:smsg="http://example.com/Request/1.0/xsimple">      |   "request": {
  <id>R012</id>                                             |     "id": "R012",
  <item>                                                    |     "item": [
    <name>Wrench</name>                                     |       {
    <quantity>1</quantity>                                  |         "name": "Wrench",
  </item>                                                   |         "quantity": {
  <item>                                                    |           "number": 1
    <name>Screw</name>                                      |         }
    <quantity smsg:units="dozen">5</quantity>               |       },
  </item>                                                   |       {
</request>                                                  |         "name": "Screw",
                                                            |         "quantity": {
                                                            |           "units": "dozen",
                                                            |           "number": 5
                                                            |         }
```
<figcaption><a name="ex4">Example 4:  Simple NIEM messages</a></figcaption>

The following sections provide a walk-through of constructing canonical and simple message formats in XML and JSON, using CMFTool.

## 2. Building a canonical JSON message format

Every message format begins with the message model.  For the time being, message designers will usually build their message model in XSD.  However, most CMFTool operations work on CMF, so the first step is to convert the XSD schema document pile into a CMF model file.  The **cmftool x2m** command builds a model from a list of XSD files, and writes the model as CMF.

<pre class="blk"><code>
$ cmftool x2m model.xsd/request.xsd -o model.cmf
Namespaces claiming conformance:
  http://example.com/Request/1.0/ [NIEM version='NIEM6.0']
  https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/ [NIEM version='NIEM6.0']
Utililty and predefined namespaces:
  https://docs.oasis-open.org/niemopen/ns/model/adapters/niem-xs/6.0/
  https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/
$ tree .
├── README.md
├── <a href="examples/request/model.cmf">model.cmf</a>                    <i>-- the message model in CMF</i>
└── model.xsd                    <i>-- same message model in XSD</i>
    ├── niem
    │   ├── adapters
    │   │   └── niem-xs.xsd
    │   ├── <a href="examples/request/model.xsd/niem/niem-core.xsd">niem-core.xsd</a>        <i>-- subset schema of NIEM Core namespace</i>
    │   └── utility
    │       ├── appinfo.xsd
    │       └── structures.xsd
    └── <a href="examples/request/model.xsd/request.xsd">request.xsd</a>              <i>-- extension schema for the Request message type</i>
</code></pre>

### 2.1 Building the JSON-LD context file

This is done with the **cmftool m2context** command.

<pre class="blk"><code>
$ cd formats/jcanon
$ cmftool m2context ../../model.cmf -o context.json
$ cat context.json
{
  "@context": {
    "msg": "http://example.com/Request/1.0/",
    "nc": "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"
  }
}
</code></pre>

### 2.2 Building the message schema

The **cmftool m2jmsg** command creates JSON Schema from the message model.  You supply the QName of the message property with the `-m` option.  You may supply the URI of the context resource with the `-c` option.

<pre class="blk"><code>
$ cmftool m2jmsg ../../model.cmf -m msg:Request -c http://example.com/Request/1.0/@context -o message.schema.json 
$ tree .
├── <a href="examples/request/formats/jcanon/context.json">context.json</a>
├── examples
│   ├── <a href="examples/request/formats/jcanon/examples/invalid01.json">invalid01.json</a>
│   ├── <a href="examples/request/formats/jcanon/examples/invalid02.json">invalid02.json</a>
│   └── <a href="examples/request/formats/jcanon/examples/valid01.json">valid01.json</a>
│   └── <a href="examples/request/formats/jcanon/examples/valid02.json">valid02.json</a>
└── <a href="examples/request/formats/jcanon/message.schema.json">message.schema.json</a>
</code></pre>

The generated message schema expects an object with exactly two keys.  One key is the message property provided with `-m`.  The other is `@context`, which can always have an object value.  If `-c` is provided, then `@context` can also have a string value that is the context resource URI.

### 2.3 Validating example messages

You can test the message schema by validating good and bad messages.

```
$ check-jsonschema --schemafile message.schema.json examples/valid01.json
ok -- validation done
$ check-jsonschema --schemafile message.schema.json examples/valid02.json
ok -- validation done
$ check-jsonschema --schemafile message.schema.json examples/invalid01.json
Schema validation errors were encountered.
  examples/invalid01.json::$['msg:Request']['msg:RequestID']: ['R012', 'bogus'] is not of type 'string'
$ check-jsonschema --schemafile message.schema.json examples/invalid02.json
Schema validation errors were encountered.
  examples/invalid02.json::$['msg:Request']['msg:RequestedItem'][0]: 'nc:ItemName' is a required property
  examples/invalid02.json::$['msg:Request']['msg:RequestedItem'][0]: Additional properties are not allowed ('nc:ItemNameXX' was unexpected)
```

### 2.4 Converting messages to XML

The **niemtran j2x** command uses the message model to convert a JSON message to the equivalent XML.  For example:

```
$ niemtran j2x ../../model.cmf examples/valid01.json
<?xml version="1.0" encoding="UTF-8"?>
<msg:Request
  xmlns:msg="http://example.com/Request/1.0/"
  xmlns:nc="https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/">
  <msg:RequestID>R012</msg:RequestID>
  <msg:RequestedItem>
    <nc:ItemName>Wrench</nc:ItemName>
    <nc:ItemQuantity>1</nc:ItemQuantity>
  </msg:RequestedItem>
  <msg:RequestedItem>
    <nc:ItemName>Screw</nc:ItemName>
    <nc:ItemQuantity nc:quantityUnitText="dozen">5</nc:ItemQuantity>
  </msg:RequestedItem>
</msg:Request>
```

### 2.5 Converting messages to RDF

NIEM JSON messages are JSON-LD data, and so several open-source tools can convert a JSON message to RDF.  For example:
```
$ riot --syntax=jsonld --output=ttl examples/valid02.json
PREFIX msg: <http://example.com/ReqRes/1.0/>
PREFIX nc: <https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/>
_:b0    msg:Request  _:b1 .
_:b1    msg:RequestID      "R012";
        msg:RequestedItem  _:b2;
        msg:RequestedItem  _:b3 .
_:b2    nc:ItemName      "Wrench";
        nc:ItemQuantity  _:b4 .
_:b4    nc:QuantityLiteral  1 .
_:b3    nc:ItemName      "Screw";
        nc:ItemQuantity  _:b5 .
_:b5    nc:QuantityLiteral   5;
        nc:quantityUnitText  "dozen" .
```

The **niemtran j2r** command uses the context and the message model to include `rdf:type` triples in the RDF output.  For example:

```
$ niemtran j2r -c context.json ../../model.cmf examples/valid01.json
@prefix msg: <http://example.com/Request/1.0/> .
@prefix nc: <https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/> .
_:b00 msg:Request _:b01 .
_:b01 a msg:RequestType .
_:b01 msg:RequestID "R012" .
_:b01 msg:RequestedItem _:b04 .
_:b01 msg:RequestedItem _:b02 .
_:b02 a nc:ItemType .
_:b02 nc:ItemName "Wrench" .
_:b02 nc:ItemQuantity _:b03 .
_:b03 a nc:QuantityType .
_:b03 nc:QuantityLiteral 1 .
_:b04 a nc:ItemType .
_:b04 nc:ItemName "Screw" .
_:b04 nc:ItemQuantity _:b05 .
_:b05 a nc:QuantityType .
_:b05 nc:QuantityLiteral 5 .
_:b05 nc:quantityUnitText "dozen" .
```

## 3. Building a simple JSON message format

The steps are the same, with one addition:  providing the mappings from canonical property names to simple, developer-convenient names.  The mappings then become an additional input to CMFTool.

### 3.1 Defining the property mappings

Property mappings are recorded in a text file.  The *from* field must be the QName of a model property.  The *to* field must be a QName.  Mappings must be unique in both directions.

The **cmftool m2map** command generates a mapping file template, which can then be edited to include the desired mapping targets.  For example:

```
$ cmftool m2map ../../model.cmf -o map.txt
$ cat map.txt
PREFIX T     http://example.com/YourTargetNSURI/
PREFIX msg   http://example.com/Request/1.0/
PREFIX nc    https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/
# FromQName            ToQName
msg:Request            T:TEMP0
msg:RequestedItem      T:TEMP1
msg:RequestID          T:TEMP2
nc:CommentText         T:TEMP3
nc:ItemName            T:TEMP4
nc:ItemQuantity        T:TEMP5
nc:QuantityLiteral     T:TEMP6
nc:quantityUnitText    T:TEMP7
```

An option to **cmftool m2map** creates a mapping file in which each property is mapped to a single target namespace.  The local name part of each source component is preserved, unless there are collisions.  (For instance, if the source properties include both `nc:PersonName` and `extns:PersonName`, that is a collision.)  

```
$ cmftool m2map ../../model.cmf -s sj=http://example.com/Request/1.0/jsimple -o map.txt
$ cat map.txt
PREFIX msg   http://example.com/Request/1.0/
PREFIX nc    https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/
PREFIX sj    http://example.com/Request/1.0/jsimple
# FromQName            ToQName
msg:Request            sj:Request
msg:RequestedItem      sj:RequestedItem
msg:RequestID          sj:RequestID
nc:CommentText         sj:CommentText
nc:ItemName            sj:ItemName
nc:ItemQuantity        sj:ItemQuantity
nc:QuantityLiteral     sj:QuantityLiteral
nc:quantityUnitText    sj:quantityUnitText
```

This mapping file can also be edited to insert developer-friendly names, as shown in [example 5](#ex5) below.  Each property in the message type is mapped to the local name that will appear in messages. When all properties are mapped to the same target namespace, the keys in messages do not require a namespace prefix; that is, you will see `number` in the context and in messages, not `sj:number`.  Developers often prefer this additional simplicity.

```
PREFIX msg   http://example.com/Request/1.0/
PREFIX nc    https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/
PREFIX sj    http://example.com/Request/1.0/jsimple
# FromQName            ToQName
msg:Request            sj:request
msg:RequestID          sj:id
msg:RequestedItem      sj:item
nc:ItemName            sj:name
nc:ItemQuantity        sj:quantity
nc:QuantityLiteral     sj:number
nc:quantityUnitText    sj:units
```
<figcaption><a name="ex5">Example 5:  Property mapping file for the simple JSON message in example 4</a></figcaption>

### 3.2 Building the JSON-LD context file

The **cmftool m2context** command accepts a mapping file as a parameter, and produces a context object that resolves each message key to the URI of the mapped property.

```
$ cmftool m2context ../../model.cmf -m msg:Request --map map.txt --noprefix -o context.json
$ cat context.json
{
  "@context": {
    "msg": "http://example.com/Request/1.0/",
    "nc": "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/",
    "request": "msg:Request",
    "item": "msg:RequestedItem",
    "id": "msg:RequestID",
    "name": "nc:ItemName",
    "quantity": "nc:ItemQuantity",
    "number": "nc:QuantityLiteral",
    "units": "nc:quantityUnitText"
  }
}
```
### 3.3 Building the message schema

The **cmftool m2jmsg** command accepts a mapping file as an argument, and uses those mappings to build JSON Schema that expects the mapped keys.

<pre class="blk"><code>
$ cmftool m2jmsg ../../model.cmf --map=map.txt -m msg:Request -c http://example.com/Request/1.0/jsimple --noprefix -o message.schema.json 
$ tree .
├── <a href="examples/request/formats/jsimple/context.json">context.json</a>
├── examples
│   ├── <a href="examples/request/formats/jsimple/examples/invalid01.json">invalid01.json</a>
│   └── <a href="examples/request/formats/jsimple/examples/valid01.json">valid01.json</a>
│   └── <a href="examples/request/formats/jsimple/examples/valid02.json">valid02.json</a>
└── <a href="examples/request/formats/jsimple/message.schema.json">message.schema.json</a>
</code></pre>

### 3.4 Validating example messages

You can test the message schema by validating good and bad messages.

```
$ check-jsonschema --schemafile message.schema.json examples/valid01.json
ok -- validation done
$ check-jsonschema --schemafile message.schema.json examples/valid02.json
ok -- validation done
$ check-jsonschema --schemafile message.schema.json examples/invalid01.json
Schema validation errors were encountered.
  examples/invalid01.json::$['msg:Request']['msg:RequestID']: ['R012', 'bogus'] is not of type 'string'
$ check-jsonschema --schemafile message.schema.json examples/invalid02.json
Schema validation errors were encountered.
  examples/invalid02.json::$['msg:Request']['msg:RequestedItem'][0]: 'nc:ItemName' is a required property
  examples/invalid02.json::$['msg:Request']['msg:RequestedItem'][0]: Additional properties are not allowed ('nc:ItemNameXX' was unexpected)
```

### 3.5 Converting messages to RDF

Open-source tools use the context to convert a simple JSON message to RDF.  For example:

```
$ riot --syntax=jsonld --output=ttl examples/valid01.json
PREFIX msg: <http://example.com/ReqRes/1.0/>
PREFIX nc: <https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/>
_:b0    msg:Request  _:b1 .
_:b1    msg:RequestID      "R012";
        msg:RequestedItem  _:b2;
        msg:RequestedItem  _:b3 .
_:b2    nc:ItemName      "Wrench";
        nc:ItemQuantity  _:b4 .
_:b4    nc:QuantityLiteral  1 .
_:b3    nc:ItemName      "Screw";
        nc:ItemQuantity  _:b5 .
_:b5    nc:QuantityLiteral   5;
        nc:quantityUnitText  "dozen" .
```

The **niemtran j2r** command uses the context and the message model to include `rdf:type` triples in the output.  For example:

```
$ niemtran j2r -c context.json ../../model.cmf examples/valid02.json
@prefix msg: <http://example.com/ReqRes/1.0/> .
@prefix nc: <https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/> .
_:b00 msg:Request _:b01 .
_:b01 msg:RequestID "R099" .
_:b01 msg:RequestedItem _:b02 .
_:b02 nc:ItemName "LugNut" .
_:b02 nc:ItemQuantity _:b03 .
_:b03 a nc:QuantityType .
_:b03 nc:QuantityLiteral 4 .
_:b03 nc:quantityUnitText "dozen" .
```

## 4. Building a canonical XML message format

We begin with the message model in CMF.  First construct a message schema, then validate example messages.

### 4.1 Building the message schema

The **cmftool m2xmsg** command creates the message schema in the form of an XML Schema document pile.

<pre class="blk"><code>
$ cmftool m2xmsg ../../model.cmf -o message.xsd
$ tree .
├── examples
│   ├── <a href="examples/request/formats/xcanon/examples/invalid01.xml">invalid01.xml</a>
│   └── <a href="examples/request/formats/xcanon/examples/valid01.xml">valid01.xml</a>
└── message.xsd
    ├── niem
    │   └── <a href="examples/request/formats/xcanon/message.xsd/niem/niem-core.xsd">niem-core.xsd</a>
    └── <a href="examples/request/formats/xcanon/message.xsd/request.xsd">request.xsd</a>
</code></pre>

The message schema created by CMFTool is not a model representation, and so does not need to conform to the [NIEM Naming and Design Rules](https://docs.oasis-open.org/niemopen/ndr/v6.0/ndr-v6.0.html).  It is suitable for validating XML messages, and for code binding tools like JAXB.

### 4.2 Validating example messages

You can test the message schema by validating good and bad messages.

```
$ xmllint --noout --schema message.xsd/request.xsd examples/valid01.xml
examples/valid01.xml validates
$ xmllint --noout --schema message.xsd/request.xsd examples/invalid01.xml
examples/invalid01.xml:5: element RequestedItem: Schemas validity error : Element '{http://example.com/Request/1.0/}RequestedItem': This element is not expected. Expected is ( {http://example.com/Request/1.0/}RequestID ).
examples/invalid01.xml fails to validate
```

### 4.3 Converting messages to JSON

The **niemtran j2x** command uses the message model to convert an XML message to the equivalent JSON.  For example:

```
$ niemtran x2j ../../model.cmf examples/valid01.xml
{
  "msg:Request": {
    "msg:RequestID": "R012",
    "msg:RequestedItem": [
      {
        "nc:ItemName": "Wrench",
        "nc:ItemQuantity": { "nc:QuantityLiteral": 1 }
      },
      {
        "nc:ItemName": "Screw",
        "nc:ItemQuantity": {
          "nc:quantityUnitText": "dozen",
          "nc:QuantityLiteral": 5
        }
      }
    ]
  }
}
```

### 4.4 Converting messages to RDF

At present, this is accomplished by first converting to JSON, then to RDF.

## 5. Building a simple XML message format

The steps are the same, with one addition:  defining the mappings from canonical property names to simple, developer-convenient names.  The mappings then become an additional input to CMFTool.

### 5.1 Defining the property mappings

With a change to the target namespace, the [map.txt](examples/request/formats/jsimple/map.txt) file from the simple JSON format can be used here as well, or a different mapping file created using the steps in [section 3.1](#31-defining-the-property-mappings).  Because all properties are mapped to a single target namespace, elements in messages will not require a namespace prefix. (Attributes in NIEM XML always require a namespace prefix.)

### 5.2 Building the message schema

The mapping file becomes an argument to the **cmftool m2xmsg** command.

<pre class="blk"><code>
$ cmftool m2xmsg ../../model.cmf --map=map.txt -o message.xsd
$ tree .
.
├── examples
│   ├── <a href="examples/request/formats/xsimple/examples/invalid01.xml">invalid01.xml</a>
│   └── <a href="examples/request/formats/xsimple/examples/valid01.xml">valid01.xml</a>
├── <a href="examples/request/formats/xsimple/map.txt">map.txt</a>
└── message.xsd
    ├── niem
    │   └── <a href="examples/request/formats/xsimple/message.xsd/niem/niem-core.xsd">niem-core.xsd</a>
    ├── <a href="examples/request/formats/xsimple/message.xsd/request.xsd">request.xsd</a>
    └── <a href="examples/request/formats/xsimple/message.xsd/sj.xsd">sj.xsd</a>
</code></pre>

### 5.3 Validating example messages

You can test the message schema by validating good and bad messages.

```
$ xmllint --noout --schema message.xsd/request.xsd examples/valid01.xml
examples/valid01.xml validates
$ xmllint --noout --schema message.xsd/request.xsd examples/invalid01.xml
examples/invalid01.xml:4: Schemas validity error : Element '{http://example.com/Request/1.0/}Request': No matching global declaration available for the validation root.
examples/invalid01.xml fails to validate
```

### 5.4 Converting simple XML to canonical XML

TBD

## 6. Building a simpler XML message format

A mapping file can contain mappings for type declarations.  When every type declaration is mapped to the same target namespace, the resulting message schema fits into a single schema document.

### 6.1 Creating the mapping file

The **cmftool m2map** command can generate a mapping template with entries for type declarations.  For example:

```
$ cmftool m2map ../../model.cmf -t -s sj=http://example.com/Request/1.0/xsimpler
PREFIX msg   http://example.com/Request/1.0/
PREFIX nc    https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/
PREFIX sj    http://example.com/Request/1.0/xsimpler
# FromQName            ToQName
msg:Request            sj:Request
msg:RequestedItem      sj:RequestedItem
msg:RequestID          sj:RequestID
nc:CommentText         sj:CommentText
nc:ItemName            sj:ItemName
nc:ItemQuantity        sj:ItemQuantity
nc:QuantityLiteral     sj:QuantityLiteral
nc:quantityUnitText    sj:quantityUnitText
msg:RequestType        sj:RequestType
nc:ItemType            sj:ItemType
nc:NumericType         sj:NumericType
nc:QuantityType        sj:QuantityType
nc:TextType            sj:TextType
```

This mapping file can be used as is, or edited for simple property names (that is, `sj:item` instead of `sj:RequestedItem`.)

### 6.2 Creating the message schema

This new mapping file becomes an argument to the **cmftool m2xmsg** command. The resulting message schema is written into a single schema document (*sj.xsd*) under the *message.xsd* directory.

<pre class="blk"><code>
$ cmftool m2xmsg ../../model.cmf -m msg:Request --map=map.txt -o message.xsd
$ tree .
.
├── examples
│   ├── <a href="examples/request/formats/xsimple/examples/invalid01.xml">invalid01.xml</a>
│   └── <a href="examples/request/formats/xsimple/examples/valid01.xml">valid01.xml</a>
├── <a href="examples/request/formats/xsimple/map.txt">map.txt</a>
└── message.xsd
    └── <a href="examples/request/formats/xsimple/message.xsd/sj.xsd">sj.xsd</a>
</code></pre>

```
.........1.........2.........3.........4.........5.........6.........7.........8.........9.........0.........1.........2.........3......X
This is the width of a code block in the PDF version
```

Author: Scott Renner\
Date: 2026-10-02

<style>
h1 { font-size: 14pt; }
h2,h3,h4 { font-size: 12pt;  }
h3,h4 { font-style: italic; font-weight: normal }
h1,h2 { margin-top: 1em; }
h3 { margin-top: 0.5em; }
code { font-family: "Source Code Pro", "Liberation Mono", monospace; font-size: 11pt; }
pre { background-color:#f0f0f0; padding: 6px; page-break-after: avoid; }
pre > code { font-size: 9pt; margin-left:auto; margin-right:auto; page-break-after: avoid; }
pre.blk code { display: block; transform: translateY(-1.5em); }
pre.blk { padding-bottom: 0; }
figcaption { text-align:center; font-style:italic; margin-top: 10pt; margin-bottom:10pt;  page-break-before: avoid;  }
figcaption > a { color: #000 }
body { font-family: LiberationSans, Arial, Helvetica, sans-serif; font-size: 12pt; line-height: 1.2; }
</style>