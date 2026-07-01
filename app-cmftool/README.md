<img src="https://github.com/niemopen/oasis-open-project/blob/main/artwork/NIEM-NO-Logo-v5.png" width="200">

# Common Model Format Tool (CMFTool), version 1.1-alpha.7

This subproject is part of the CMFTool project repository.  It contains the NIEMOpen Common Model Format Tool (CMF). 

The NIEM [*Common Model Format (CMF)*](https://github.com/niemopen/common-model-format) is a data modeling formalism for NIEM-conforming data exchange specifications.  CMFTool is a multi-level command-line tool for the designers of those specifications. CMFTool subcommands are:

*  [*x2m*](#convert-a-niem-model-from-xsd-to-cmf) -- convert a NIEM model from XSD to CMF
*  [*m2x*](#convert-a-niem-model-from-cmf-to-xsd) -- convert a NIEM model from CMF to XSD
*  [*m2xmsg*](#generate-an-xml-message-schema-from-cmf) -- generate an XML message schema from CMF
*  [*m2jmsg*](#generate-a-json-message-schema-from-cmf) -- generate a JSON message chema from CMF
*  [*m2m*](#canonicalize-cmf-or-extract-namespaces-from-cmf) -- canonicalize or extract CMF from CMF
*  [*m2r*](#generate-model-rdf-from-cmf-experimental) -- generate model RDF from CMF (Experimental)
*  [*m2map*](#create-a-mapping-template-file) -- create a mapping template from CMF
*  [*m2context*](#create-a-json-ld-context) -- create a JSON-LD context from a model and mapping
*  [*mval*](#validate-a-cmf-model-file) -- validate a CMF model file
*  [*xval*](#validate-xml-documents) -- validate XML documents
*  [*xcanon*](#canonicalize-an-xml-schema-document) -- canonicalize an XML Schema document

Documentation for the subcommands appears below.  Jump to [Getting Started](#getting-started) to see how to download and run the software.

### Convert a NIEM model from XSD to CMF

*Usage:* **cmftool x2m** *[options]* *{XSD file, namespace URI, or XML catalog} ...*

This subcommand converts the NIEM model represented by an XML schema to the equivalent CMF representation.  The XML schema is formed by assembling a schema document pile as follows:

* Beginning with the empty set
* Add one or more specified initial schema documents
* As each schema document is added, find each <xs:import> element contained therein, and add the schema document specified by that element to the set, which MUST be a local resource.

The initial schema documents are specified by the command arguments, which may be any of:

* a file containing a XML schema document
* a namespace URI, which will be resolved to a XSD file using...
* an XML Catalog file

Options:

```text
-o, --output=<path>     output model file, or '-' for stdout
    --only=p1[,p2...]   include only these namespace URIs or prefixes; eg. '--only nc,j'
```

Examples:

* `cmftool x2m CrashDriver.xsd`
* `cmftool x2m http://example.com/CrashDriver/1.3/ catalog.xml`

### Convert a NIEM model from CMF to XSD

*Usage:* **cmftool m2x** *[options]* *modelFile.cmf*

This subcommand converts the CMF representation of a NIEM model to the equivalent XSD representation.

Options:

```text
  -o, --output-dir=<dir>    write schema pile into this directory
  -c                        generate XML catalog into xml-catalog.xml file
      --catalog=<catPath>   write XML catalog into this file
  -r, --root=<rootNSarg>    make this schema document have all necessary imports
  -v, --arch-version=<vers> builtins from this architecture (eg. "-v NIEM5.0")
  -d, --debug               turn on debug logging
      --force               replace existing output directory by moving it aside and promoting staged output
```                              

The `-r` option causes the schema document for the specified namespace to include `xs:import` elements as needed to ensure the entire model will be assembled from this document alone.

The `-v` option causes *cmftool* to ignore the default NIEM version (`NIEM6.0`) and any version information in the CMF model, and instead use the builtin schema documents from the specified version.  For example, `-v NIEM4.0` will cause the schema document for each model namespace to import `http://release.niem.gov/niem/structures/4.0/`.

### Generate an XML message schema from CMF

*Usage:* **cmftool m2xmsg** *[options]* *modelFile.cmf*

This subcommand creates an XML message schema from a CMF model.  The message schema is not a model representation, but is suitable for validating NIEM XML messages that conform to the model, or for driving XML code binding tools.

Options:

```text
  -o, --output-dir=<dir>     write schema pile into this directory
  -c                         generate XML catalog into xml-catalog.xml file
      --catalog=<catPath>    write XML catalog into this file
  -r, --root=<prefixOrURI>   make schema document for this namespace have all necessary imports
  -v, --arch-version=<vers>  builtins from this architecture (eg. "-v NIEM5.0")
  -d, --debug                turn on debug logging
      --force                replace existing output directory by moving it aside and promoting staged output
```

### Generate a JSON message schema from CMF

*Usage:* **cmftool m2jmsg** *[options]* *modelFile.cmf*

This subcommand creates a JSON message schema from a CMF model.  The result is a JSON Schema file that is suitable for validating NIEM JSON messages that conform to the model.

Options:
```text
  -m, --msg=<QName>[,<QName>...]
                            build schema to validate these message properties
  -c, --context=<URI>       schema will require this @context URI
      --map=<mapPath>       mapping file for property keys
  -a, --alldefs             generate definition for all model classes and datatypes
  -o, --output=<outputPath> name of output file
      --noprefix            don't use prefix in property keys
      --noformat            don't include format properties in built-in types
      --nopattern           don't include pattern properties in built-in types
      --nominmax            don't include minimum/maximum properties in built-in types
      --version=<version>   use this Schematron version {draft-07,2019-09,2020-12}
      --versionUri=<URI>    use this Schematron version URI (eg. http://json-schema.org/draft-07/schema#)
```

### Canonicalize CMF, or extract namespaces from CMF

*Usage:* **cmftool m2m** *[options]* *modelFile.cmf*

This subcommand converts a CMF model into a standard (canonical) CMF format.  It can also be used to extract specified namespaces from a CMF model.  The resulting CMF file contains only components from the specified namespaces; other components appear only as URI references.

Options:

```text
  -o, --output=FILE|-     name of output model file; use '-' for stdout
      --only=p1[,p2...]   include only these namespace URIs or prefixes; eg. '--only nc,j'
```

### Generate model RDF from CMF

*Usage:* **cmftool m2r** *[options]* *modelFile.cmf*

This subcommand creates an RDF file (in Turtle syntax) containing the triples entailed by the CMF model file.  *(See [NDR 6.1 §14.1](https://docs.oasis-open.org/niemopen/ndr/v6.0/ndr-v6.0.html#141-rdf-interpretation-of-niem-models).)*

Options:

```text
  -o, --output=<path>       output file, or '-' for stdout (the default)
```

### Create a mapping template file

*Usage:* **cmftool m2map** *[options]* *modelFile.cmf*

The *m2map* subcommand generates a mapping template from a model.  This template is then edited by the message designer to define the desired mappings.

When given the `-s` or `--single` option, *m2map* produces a template that maps every component in the model to a synonym in a single namespace with the same local name.  For example, `-s msg=http://example.com/my/msg/` produces a template in which `nc:Person` is mapped to `msg:Person`.

Options:

```text
  -m, --msg=<QName>[,<QName>...]
                         build schema to validate these message properties
  -s, --single=<p=URI>   prefix=URI of single target namespace
  -t, --types            also map class and datatype QNames
  -o, --output=<path>    name of output mapping file
```

### Create a JSON-LD context

*Usage:* **cmftool m2context** *[options]* *model.cmf*

Options:

```text
      --map=<path>      mapping file for property keys
  -m, --msg=<QName>[,<QName>...]
                        create context for these message properties
      --noprefix        include all property keys; use no prefixes
  -o, --output=<path>   name of output context file; use '-' for stdout (the default)
      --force           overwrite output file if it already exists
```

### Validate a CMF model file

*Usage:* **cmftool mval** *modelFile.cmf*

This subcommand tests a CMF file for conformance.

### Canonicalize an XML Schema document

*Usage:* **cmftool xcanon** *[options]* *schemaDoc.xsd ...*

This subcommand converts the XML schema document for a NIEM model namespace into a readable format.

Options:

```text
  -i                       canonicalize in place
      --in-place=suffix    canonicalize in place with backup suffix, eg. '--in-place=bak'
  -o, --output=<objFile>   file for converter output, or '-' for stdout
```

## Getting started

1. You must have a Java runtime environment.  JRE25 or later will work.  JRE17 might work.  
   - Try `java –-version` from the command line.  If that works, you should be OK
   - Otherwise make sure your `JAVA_HOME` environment variable points to your JRE

2. Unpack the executable distribution from the Assets tab on the [Release page](https://github.com/niemopen/cmftool/releases)
   - The *cmftool* program is in *cmftool-1.0.zip*
   - The *niemtran* program is in *niemtran-1.0.zip*
   - The *scheval* program is in *scheval-1.0.zip*
   - You only need the *cmftool* zip file.  But it's OK to combine the *bin* and *lib* directories from all three.

3. Put the *bin* directory into your PATH, create a shell alias, etc.
4. Try `cmftool help` from the command line

## Examples

The [*Crash Driver Report*](https://github.com/iamdrscott/CrashDriver) message specification is designed to test and describe the features of the NIEM technical architecture.  Try `make -n all` to see some of the things you can to with *cmftool*.

The *src/test/resources* directory in the [lib-cmf](../lib-cmf/README.md) subproject contains resources for JUnit tests.  Many, many examples there.

## Building

This project was built with NetBeans 24, Gradle 8.12, and Oracle JDK 21.
Try `./gradlew build`

## Software Bill of Materials

CMFTool depends on the *lib-cmf* and *lib-util* subprojects in this repository.  It also depends on the following libraries, all of which are unmodified, and can be found at [mvnrepository.com](https://mvnrepository.com):

| Library                  | Version        | License                        |
|--------------------------|---------------|-------------------------------|
| commons-io | 2.18.0 | Apache-2.0 |
| commons-lang3 | 3.20.0 | Apache-2.0 |
| cyclonedx-gradle-plugin | 3.2.4 | - |
| error_prone_annotations | 2.38.0 | Apache-2.0 |
| gson | 2.13.1 | Apache-2.0 |
| javatuples | 1.2 | Apache-2.0 |
| log4j-api | 2.24.3 | Apache-2.0 |
| log4j-core | 2.24.3 | Apache-2.0 |
| picocli | 4.7.7 | Apache-2.0 |
| Saxon-HE | 12.5 | MPL-2.0 |
| serializer | 2.7.3 | - |
| xalan | 2.7.3 | - |
| xercesImpl | 2.12.2 | Apache-2.0 |
| xml-apis | 1.4.01 | Apache-2.0, SAX-PD, The W3C License |
| xmlresolver | 6.0.14 | Apache-2.0 |

## About NIEMOpen

- The NIEMOpen project page: [www.niemopen.org](http://www.niemopen.org/). The website contains news, announcements, and other information of interest about the project.

- [NIEM Technical Architecture Committee (NTAC)](https://github.com/niemopen/ntac-admin):  
The NTAC is a Technical Steering Committee within the OASIS Open Project known as NIEMOpen. The NTAC is responsible for transforming the business requirements of NIEM into its technical architecture.

- [NTAC mailing list](https://lists.oasis-open-projects.org/g/niemopen-ntactsc). This is the discussion list for use by the members of the NIEM Technical Architecture Committee TSC. To subscribe, send an empty email message to [niemopen-ntactsc+subscribe@lists.oasis-open-projects.org](mailto:niemopen-ntactsc+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe read-only. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen-ntactsc/messages).

- The [General purpose mailing list](https://lists.oasis-open-projects.org/g/niemopen). To subscribe, send an empty email message to [niemopen+subscribe@lists.oasis-open-projects.org](mailto:niemopen+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe and send email to the list. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen/messages).

- The [Project Governing Board mailing list](https://lists.oasis-open-projects.org/g/niemopen-pgb). This is the discussion list for use by the members of the PGB. To subscribe, send an empty email message to [niemopen-pgb+subscribe@lists.oasis-open-projects.org](mailto:niemopen-pgb+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe read-only. Only PGB members can post. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen-pgb/messages).

General questions about OASIS Open Projects may be directed to OASIS staff at [project-admin@lists.oasis-open-projects.org](mailto:project-admin@lists.oasis-open-projects.org)

## Contributing

Please read [CONTRIBUTING.md](https://github.com/niemopen/cmftool/blob/main/CONTRIBUTING.md) for details how to join the project, contribute changes to our repositories and communicate with the rest of the project contributors.

## Governance

NIEM Open operates under the terms of the [Open Project Rules](https://www.oasis-open.org/policies-guidelines/open-projects-process) and the applicable license(s) specified in [LICENSE.md](https://github.com/niemopen/cmftool/blob/main/LICENSE.md). Further details can be found in [GOVERNANCE.md](https://github.com/niemopen/cmftool/blob/main/GOVERNANCE.md), [GOVERNANCE-NBAC.md](https://github.com/niemopen/cmftool/blob/main/GOVERNANCE-NBAC.md), and [GOVERNANCE-NTAC.md](https://github.com/niemopen/cmftool/blob/main/GOVERNANCE-NTAC.md).

## CLA & Non-assert signatures required

All technical contributions must be covered by a Contributor's License Agreement. This requirement allows our work to advance through OASIS standards development stages and potentially be submitted to de jure organizations such as ISO. You will get a prompt to sign this document when you submit your first pull request to a project repository, or you can sign [here](https://cla-assistant.io/niemopen/oasis-open-project). If you are contributing on behalf of your employer, you must also sign the ECLA [here](https://www.oasis-open.org/open-projects/cla/entity-cla-20210630/).q
