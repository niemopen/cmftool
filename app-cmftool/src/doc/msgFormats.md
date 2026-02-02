# Building message formats with CMFTool

This article describes how to use CMFTool to construct a *message format*. That process looks like this:

* Begin with a *message type* specification
* Create mappings to simple property names, if desired
* Generate a *message schema* and perhaps a JSON-LD context
* Test the message schema by validating example *messages*

We will define all of those terms and provide illustrative examples of the process.

### 1.1 Messages

A *message* is a package of data that can be exchanged at runtime.  Here are two NIEM messages, one in XML, the other in JSON format.  (For brevity and clarity, lines in an example may be truncated, closing tags and brackets omitted, etc. )

```
<msg:Request                                                            | {
  xmlns:msg="http://example.com/ReqRes/1.0/"                            |   "@context": "http://example.com/ReqRes/1.0/",
  xmlns:nc="https://docs.oasis-open.org/niemopen/ns/model/niem-core     |   "msg:Request": {
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
<figcaption><a name="ex1">Example 1: NIEM messages in XML and JSON syntax</a></figcaption>

### 1.2 Message types

Both of those messages are instances of the same *message type*, which is shown in [example 2](#ex2) below.  A message type specifies the mandatory and optional content of conforming messages, and also defines the meaning of that content.  That specification is captured in a *message model*, which may be represented as a CMF file or as an XML Schema (XSD) document pile.  A message type also specifies the *message property*, the root node of the message data; in this message type, `msg:Request`.

<pre class="blk"><code>
$ tree request
├── <a href="../examples/request/README.md">README.md</a>
├── formats
├── <a href="../examples/request/model.cmf">model.cmf</a>                    <i>-- the message model in CMF</i>
└── model.xsd                    <i>-- same message model in XSD</i>
    ├── niem
    │   ├── adapters
    │   │   └── niem-xs.xsd
    │   ├── <a href="../examples/request/model.xsd/niem/niem-core.xsd">niem-core.xsd</a>        <i>-- subset schema of NIEM Core namespace</i>
    │   └── utility
    │       ├── appinfo.xsd
    │       └── structures.xsd
    └── <a href="../examples/request/model.xsd/request.xsd">request.xsd</a>              <i>-- extension schema for the Request message type</i>
</code></pre>
<figcaption><a name="ex2">Example 2: The "Request" message type</a></figcaption>

### 1.3 Message formats

A message type does not specify the syntax of conforming messages.  That is done by a message format.  A message format has a *message schema*, which may be used to validate instance messages.  That schema will be in XSD for XML messages, in JSON Schema for JSON messages, etc.  [Example 3](#ex3) shows the message format for the XML message in [example 1](#ex1).  (Examples showing the CMFTool commands to generate a message format from a message type are provided later on.)

<pre class="blk"><code>
$ tree formats/xcanon
├── examples
│   ├── <a href="../examples/request/formats/xcanon/examples/invalid01.xml">invalid01.xml</a>
│   └── <a href="../examples/request/formats/xcanon/examples/valid01.xml">valid01.xml</a>
└── message.xsd
    ├── niem
    │   └── <a href="../examples/request/formats/xcanon/message.xsd/niem/niem-core.xsd">niem-core.xsd</a>    <i>-- a message schema document, doesn't have to follow NDR</i>
    └── <a href="../examples/request/formats/xcanon/message.xsd/request.xsd">request.xsd</a>          <i>-- a message schema document, doesn't have to follow NDR</i>
</code></pre>
<figcaption><a name="ex3">Example 3: An XML message format</a></figcaption>

A message schema is all about syntax.  It is not required to capture all of the information in a NIEM model.  For this reason, the XSD for a message schema can be much simpler than the XSD for the message model.  It can also be used for code binding tools (for example, JAXB).  It is usually used to validate messages during development and testing.  For instance:

```
$ xmllint --schema message.xsd/request.xsd examples/valid01.xml --noout
valid01.xml validates
$ xmllint --schema message.xsd/request.xsd examples/invalid01.xml --noout
invalid01.xml:5: element RequestedItem: Schemas validity error : Element '{http://example.com/ReqRes/1.0/}RequestedItem': This element is not expected. Expected is ( {http://example.com/ReqRes/1.0/}RequestID ).
invalid01.xml fails to validate
```

### 1.4 Equivalent messages

Any NIEM message can be converted from one message format to another format of the same message type, without changing the information content.  The two messages in [example 1](#ex1) are equivalent in that way.  

RDF is the interpretive framework for NIEM model and message semantics. The information content of a message is defined as the RDF graph entailed by that message.  (Details provided for the curious in section 14 of the Naming and Design Rules v6.0.)  The messages in [example 1](#ex1) are equivalent because they both entail the following RDF:

```
@prefix msg: http://example.com/ReqRes/1.0/ . 
@prefix nc: https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/ . 
@prefix xsd: http://www.w3.org/2001/XMLSchema# .
_:b0 msg:Request _:b1 .
_:b1 a msg:RequesType; msg:RequestID "R012" ; msg:RequestedItem _:b2, _:b4 .
_:b2 a nc:ItemType; nc:ItemName "Wrench" ; nc:ItemQuantity _:b3 .
_:b3 a nc:QuantityType; nc:QuantityLiteral "1"^^xsd:integer .
_:b4 a nc:ItemType; nc:ItemName "Screw" ; nc:ItemQuantity _:b5 .
_:b5 a nc:QuantityType; nc:QuantityLiteral "5"^^xsd:integer ; nc:quantityUnitText "dozen" .
```

### 1.5 Simple and canonical property names

Component names in a NIEM model are composed of a namespace prefix, a colon, and a local name that follows ISO 11179 naming rules; for example, `nc:ItemName`, `nc:PersonSurName`.  Using ISO 11179 naming helps to ensure that model terms are precise, unambiguous, and consistently interpreted in models that are constructed and used by many developers in many organizations. Names of this form are also easily converted into resource identifiers in RDF graphs.  Messages like those in [example 1](#ex1), which use NIEM model names as XML element tags or JSON object keys are known as *canonical messages*.

Developers often prefer to work with short, simplified tags and keys; for example, `lname` instead of `nc:PersonSurName`.  These names are quicker to type, easier to scan on the screen, and often map more naturally to variables and fields in application code.  The developer community around a message type first learns how their preferred names correspond to the message model, and then relies on that shared understanding, without needing to encode semantics into every tag or key.

NIEM allows a message designer to define mappings between the properties in the message model and the simple names preferred by developers.  These mappings are then used to create a *simple message format*.  A message of that format uses the simple names, but has an equivalent canonical message, and can be transformed to or from the canonical format at need.  The message in [example 4](#ex4) below are simple messages equivalent to the canonical messages in [example 1](#ex1).

```
<request                                                    | {
  xmlns="http://example.com/ReqRes/1.0/simpleXML"           |   "@context": "http://example.com/ReqRes/1.0/simpleJSON",
  xmlns:smsg="http://example.com/ReqRes/1.0/simpleXML">     |   "request": {
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

The next section is a walk-through of constructing canonical and simple message formats in XML and JSON, using CMFTool.

## 2. Building a canonical JSON message format

Every message format begins with the message model.  At this time, most message designers will build their message model in XSD.  However, CMFTool needs CMF, so the first step is to convert the XSD schema document pile into a CMF model file.  The **cmftool x2m** command builds a model from a list of XSD files plus every imported file, and writes the model as CMF.

```
$ cmftool x2m -o model.cmf model.xsd/request.xsd
Namespaces claiming conformance:
  http://example.com/ReqRes/1.0/ [NIEM version='NIEM6.0']
  https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/ [NIEM version='NIEM6.0']
Utililty and predefined namespaces:
  https://docs.oasis-open.org/niemopen/ns/model/adapters/niem-xs/6.0/
  https://docs.oasis-open.org/niemopen/ns/model/structures/6.0/
```

### 2.1 Building the JSON-LD context file

This is done with the **cmftool m2context** command.

```
$ cmftool m2context model.cmf -o formats/jcanon/context.json
$ cat formats/jcanon/context.json
{
  "@context": {
    "msg": "http://example.com/ReqRes/1.0/",
    "nc": "https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/"
  }
}
```

### 2.2 Building the message schema

The **cmftool m2jmsg** command creates JSON Schema from the message model.  You supply the QName of the message property with the `-p` option.  You supply the URI of the context resource with the `-c` option.

<pre class="blk"><code>
$ cmftool m2jmsg model.cmf -p msg:Request -c http://example.com/Request/1.0/@context -o formats/jcanon/message.schema.json 
$ tree formats/jcanon
├── <a href="../examples/request/formats/jcanon/context.json">context.json</a>
├── examples
│   ├── <a href="../examples/request/formats/jcanon/examples/invalid01.json">invalid01.json</a>
│   ├── <a href="../examples/request/formats/jcanon/examples/invalid02.json">invalid02.json</a>
│   └── <a href="../examples/request/formats/jcanon/examples/valid01.json">valid01.json</a>
└── <a href="../examples/request/formats/jcanon/message.schema.json">message.schema.json</a>
</code></pre>

The generated message schema expects an object with exactly two keys.  One key is the message property provided with `-p`.  The other is `@context`, which can always have an object value; if `-c` is provided, then `@context` can also have a string value that is the context resource URI.

### 2.3 Validating messages

You can test the message schema by validating good and bad messages.

```
$ jsonschema -i examples/valid01.json message.schema.json
$ jsonschema -i examples/invalid01.json message.schema.json
['R012', 'bogus']: ['R012', 'bogus'] is not of type 'string'
$ jsonschema -i examples/invalid02.json message.schema.json
{'nc:ItemNameXX': 'Wrench', 'nc:ItemQuantity': {'nc:QuantityLiteral': 1}}: 'nc:ItemName' is a required property
{'nc:ItemNameXX': 'Wrench', 'nc:ItemQuantity': {'nc:QuantityLiteral': 1}}: Additional properties are not allowed ('nc:ItemNameXX' was unexpected)
```

### 2.4 Converting messages to RDF

NIEM JSON messages are conforming JSON-LD, so in fact they are already in RDF.  There are free open-source programs that convert JSON-LD to Turtle syntax, or you could write your own.

## 3. Building a simple JSON message format

The steps are the same, with one addition:  defining the mappings from canonical property names to simple, developer-convenient names.  The mappings then become an additional input to CMFTool.

### 3.1 Defining the property mappings

CMFTool uses RDF/Turtle to record property mappings in a file.  The mapping file for the simple message format illustrated in [example 4](#ex4) is shown below.

```
@prefix msg: <http://example.com/ReqRes/1.0/> .
@prefix nc:  <https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix sj:  <http://example.com/ReqRes/1.0/simpleJSON> .

msg:Request         owl:equivalentProperty sj:request .
msg:RequestedItem   owl:equivalentProperty sj:item .
msg:RequestID       owl:equivalentProperty sj:id .
nc:ItemName         owl:equivalentProperty sj:name .
nc:ItemQuantity     owl:equivalentProperty sj:quantity .
nc:QuantityLiteral  owl:equivalentProperty sj:number .
nc:quantityUnitText owl:equivalentProperty sj:units .
```
<figcaption><a name="ex5">Example 5:  Simple property mappings in SSSOM</a></figcaption>

A mapping file must satisfy the following constraints:

1. The predicate is always `owl:equivalentProperty`.
2. Every mapping has a subject ID that is a QName of a model property.
3. The object IDs in every mapping have the same QName prefix.
4. No two mappings have an object ID with the same local name.

By convention, the URI of the target namespace is the URI of the simple message format (`http://example.com/ReqRes/1.0/simpleJSON`); however, that URI is not used for anything in a JSON format.

For convenience, CMFTool will generate a template mapping file from a message model, which you can then edit.

```
$ ct m2map model.cmf
@prefix msg: <http://example.com/ReqRes/1.0/> .
@prefix nc : <https://docs.oasis-open.org/niemopen/ns/model/niem-core/6.0/> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
msg:Request         owl:equivalentProperty T:TEMP0000 .
msg:RequestID       owl:equivalentProperty T:TEMP0002 .
msg:RequestedItem   owl:equivalentProperty T:TEMP0001 .
nc:ItemName         owl:equivalentProperty T:TEMP0003 .
nc:ItemQuantity     owl:equivalentProperty T:TEMP0004 .
nc:QuantityLiteral  owl:equivalentProperty T:TEMP0005 .
nc:quantityUnitText owl:equivalentProperty T:TEMP0006 .
```

### 3.2 Building the JSON-LD context file

The **cmftool m2context** command accepts a mapping file as a parameter.  The resulting context includes 




```
.........1.........2.........3.........4.........5.........6.........7.........8.........9.........0.........1.........2.........3......X
This is the width of a code block in the PDF version
```

Author: Scott Renner\
Date: 2025-06-16

<style>
h1 { font-size: 14pt; }
h2,h3,h4 { font-size: 12pt;  }
h3,h4 { font-style: italic; font-weight: normal }
code { font-family: "Source Code Pro", "Liberation Mono", monospace; font-size: 11pt; }
pre { background-color:#f0f0f0; padding: 6px; page-break-after: avoid; }
pre > code { font-size: 9pt; margin-left:auto; margin-right:auto; page-break-after: avoid; }
pre.blk code { display: block; transform: translateY(-1.5em); }
pre.blk { padding-bottom: 0; }
figcaption { text-align:center; font-style:italic; margin-top: 10pt; margin-bottom:10pt;  page-break-before: avoid;  }
figcaption > a { color: #000 }
body { font-family: LiberationSans, Arial, Helvetica, sans-serif; font-size: 12pt; line-height: 1.2; }
</style>