<img src="https://github.com/niemopen/oasis-open-project/blob/main/artwork/NIEM-NO-Logo-v5.png" width="200">

# NIEM Message Translation (NIEMTran) tool, version 1.0

This subproject is part of the CMFTool project repository.  It contains the NIEMOpen message translation tool (NIEMTran).

At present, NIEM supports two message serializations:  XML and JSON.  A message in one can be transformed to the equivalent message in the other.  NIEMTran uses the information in the message model to drive the transformation.  It is a multi-level command-line tool.  The subcommands are:

*  [*x2j*](#convert-niem-xml-to-json) -- convert a NIEM message from XML to JSON
*  [*j2x*](#convert-niem-json-to-xml) -- convert NIEM JSON message to NIEM XML
*  [*j2r*](#convert-niem-json-to-rdf) --  convert NIEM JSON message to RDF

### Convert NIEM XML to JSON

*Usage:* **niemtran x2j** *[options]* *model.cmf message.xml ...*

With one msg.xml and no -o/--output, writes JSON to standard output.\
With multiple msg.xml files, writes multiple msg.json output files

Options:

```text
  -c, --context           generate complete @context in result
      --curi=uri          include "@context": URI in result
  -f, --force             overwrite existing output files
  -o, --output=out.json   write output to out.json; only valid when there is a single msg.xml argument
```

### Convert NIEM JSON to XML

*Usage:* **niemtran j2x** *[options]* *model.cmf message.json ...*

With one msg.json and no -o/--output, writes XML to standard output.\
With multiple msg.json files, writes multiple msg.xml output files

Options:

```text
  -c, --context=context.json
                         JSON-LD context file used to interpret input messages
  -f, --force            overwrite existing output files
  -o, --output=out.xml   write output to out.xml; only valid when there is a single msg.json argument
```

### Convert NIEM JSON to RDF

*Usage:* **niemtran j2r** *[options]* *model.cmf message.json ...*

With one msg.json and no -o/--output, writes RDF to standard output.\
With multiple msg.json files, writes multiple msg.rdf output files

Options:

```text
  -c, --context=context.json
                         JSON-LD context file used to interpret input messages
  -f, --force            overwrite existing output files
  -o, --output=out.rdf   write output to out.rdf; only valid when there is a single msg.json argument
```

## Getting started

1. You must have a Java runtime environment.  JRE25 or later will work.  JRE17 might work.  
   - Try `java –-version` from the command line.  If that works, you should be OK
   - Otherwise make sure your `JAVA_HOME` environment variable points to your JRE

2. Unpack the executable distribution from the Assets tab on the [Release page](https://github.com/niemopen/cmftool/releases)
   - The *niemtran* program by itself is in *niemtran-1.0.zip*
   - All three programs from this repo are in *cmftool-allApps-1.0.zip*

3. Put the *bin* directory into your PATH, create a shell alias, etc.
4. Try `cmftool help` from the command line

## Examples

The [*Crash Driver Report*](https://github.com/iamdrscott/CrashDriver) message specification is designed to test and describe the features of the NIEM technical architecture.  Try `make -n all` to see some of the things you can to with *niemtran*.

## Building

This project was built with NetBeans 24, Gradle 8.12, and Oracle JDK 21.
Try `./gradlew build`

## Software Bill of Materials

NIEMTran depends on the *lib-cmf* and *lib-util* subprojects in this repository.  It also depends on the following libraries:

| Library                  | Version        | License                        |
|--------------------------|---------------|-------------------------------|
| caffeine | 3.2.2 | Apache-2.0 |
| collection | 0.7 | MIT |
| commons-codec | 1.19.0 | Apache-2.0 |
| commons-collections4 | 4.5.0 | Apache-2.0 |
| commons-compress | 1.28.0 | Apache-2.0 |
| commons-csv | 1.14.1 | Apache-2.0 |
| commons-io | 2.20.0 | Apache-2.0 |
| commons-lang3 | 3.20.0 | Apache-2.0 |
| error_prone_annotations | 2.41.0 | Apache-2.0 |
| gson | 2.13.2 | Apache-2.0 |
| jakarta.json | 2.0.1 | EPL-2.0, GPL-2.0-with-classpath-exception |
| javatuples | 1.2 | Apache-2.0 |
| jcl-over-slf4j | 2.0.17 | Apache-2.0 |
| jena-arq | 5.6.0 | Apache-2.0 |
| jena-base | 5.6.0 | Apache-2.0 |
| jena-core | 5.6.0 | Apache-2.0 |
| jena-iri | 5.6.0 | Apache-2.0 |
| jena-iri3986 | 5.6.0 | Apache-2.0 |
| jena-langtag | 5.6.0 | Apache-2.0 |
| jspecify | 1.0.0 | Apache-2.0 |
| libthrift | 0.22.0 | Apache-2.0 |
| log4j-api | 2.24.3 | Apache-2.0 |
| log4j-core | 2.24.3 | Apache-2.0 |
| logback-classic | 1.5.16 | EPL-1.0, GNU Lesser General Public License |
| logback-core | 1.5.16 | EPL-1.0, GNU Lesser General Public License |
| picocli | 4.7.7 | Apache-2.0 |
| protobuf-java | 4.32.1 | BSD-3-Clause |
| RoaringBitmap | 1.3.0 | Apache-2.0 |
| Saxon-HE | 12.5 | MPL-2.0 |
| serializer | 2.7.3 | - |
| slf4j-api | 2.0.17 | MIT |
| titanium-jcs | 1.1.1 | Apache-2.0 |
| titanium-json-ld | 1.7.0 | Apache-2.0 |
| titanium-rdf-api | 1.0.0 | Apache-2.0 |
| titanium-rdf-n-quads | 1.0.2 | Apache-2.0 |
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

All technical contributions must be covered by a Contributor's License Agreement. This requirement allows our work to advance through OASIS standards development stages and potentially be submitted to de jure organizations such as ISO. You will get a prompt to sign this document when you submit your first pull request to a project repository, or you can sign [here](https://cla-assistant.io/niemopen/oasis-open-project). If you are contributing on behalf of your employer, you must also sign the ECLA [here](https://www.oasis-open.org/open-projects/cla/entity-cla-20210630/).