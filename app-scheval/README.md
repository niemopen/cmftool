<img src="https://github.com/niemopen/oasis-open-project/blob/main/artwork/NIEM-NO-Logo-v5.png" width="200">

# Schematron Evaluation (SCHEval) tool, version 1.0

This subproject is part of the CMFTool project repository.  It contains the NIEMOpen Schematron evaluation tool (SCHEval).

NIEM models can be represented in a profile of XML Schema.  Many of the conformance rules for models in XSD are tested through Schematron rules.  Executing those rules with free or commercial tools can be difficult, because the NIEM rules depend on providing an XML Catalog file as a document node in the XSLT evaluation stage, as follows:

```
 <xsl:param name="xml-catalog" as="document-node()?"/>
```

Some tools provide Schematron results in SVRL format, like this:

```
   <svrl:successful-report test="not(exists(@abstract[xs:boolean(.) = true()]) eq (ends-with(@name, 'Abstract') or ends-with(@name, 'AugmentationPoint') or ends-with(@name, 'Representation')))"
                           role="warning"
                           location="/*[local-name()='schema' and namespace-uri()='http://www.w3.org/2001/XMLSchema'][1]/*[local-name()='element' and namespace-uri()='http://www.w3.org/2001/XMLSchema'][1]">
      <svrl:text>Rule 7-10: A Property object having an AbstractIndicator property with the value true SHOULD have a name ending in "Abstract" or "Representation"; all other components SHOULD NOT.</svrl:text>
   </svrl:successful-report>
```

That is difficult to interpret.  SCHEval can produce a more convenient result, clearly linking each message to a line and column in the XML input file.



```
WARN  7-10.xsd:20:55 -- Rule 7-10: A Property object having an AbstractIndicator property with the value true SHOULD have a name ending in "Abstract" or "Representation"; all other components SHOULD NOT.
WARN  7-10.xsd:21:59 -- Rule 7-10: A Property object having an AbstractIndicator property with the value true SHOULD have a name ending in "Abstract" or "Representation"; all other components SHOULD NOT.
```

*Usage:* **scheval** *[options]* *input.xml ...*

| Options: | |
| -- | -- |
| `-s, --schema` |  apply rules from this schematron file |
| `-x, --xslt`   |  apply rules from this compiled schematron file |
| `-o, --output` |  write output to this file (default = stdout) |
| `--svrl`       |  write output in SVRL format |
| `--compile`    | compile schema and write output in XSLT format |
| `-c, --catalog`|  provide this XML catalog file as $xml-catalog parameter |
| `-k, --keep`   |  keep temporary files |
| `-d, --debug`  |  turn on debug logging |
| `-h, --help`   | display this usage message |

Examples:

* `scheval --compile -s rules.sch -o rules.xslt`
* `scheval -x rules.xslt input.xml`
* `scheval -s rules.sch input.xml`

## Getting started

1. You must have a Java runtime environment.  JRE21 or later will work.  JRE17 might work.  
   - Try `java –-version` from the command line.  If that works, you should be OK
   - Otherwise make sure your `JAVA_HOME` environment variable points to your JRE

2. Unpack the executable distribution from the Assets tab on the [Release page](https://github.com/niemopen/cmftool/releases)
   - The *scheval* program by itself is in *scheval-1.0.zip*
   - All three programs are in *cmftool-allApps-1.0.zip*

3. Put the *bin* directory into your PATH, create a shell alias, etc.
4. Try `cmftool help` from the command line

## Building

This project was built with NetBeans 24, Gradle 8.12, and Oracle JDK 21.
Try `./gradlew build`

## Software Bill of Materials

SCHEval depends on the *lib-cmf* and *lib-util* subprojects in this repository.  It also depends on the following libraries, all of which are unmodified, and can be found at [mvnrepository.com](https://mvnrepository.com):

| Library                  | Version        | License                        |
|--------------------------|---------------|-------------------------------|
| commons-io               | 2.18.0        | Apache-2.0                    |
| commons-lang3            | 3.17.0        | Apache-2.0                    |
| error_prone_annotations  | 2.38.0        | Apache-2.0                    |
| javatuples               | 1.2           | Apache-2.0                    |
| jcommander               | 2.0           | Apache-2.0                    |
| log4j-api                | 2.24.3        | Apache-2.0                    |
| log4j-core               | 2.24.3        | Apache-2.0                    |
| Saxon-HE                 | 12.5          | MPL-2.0                       |
| xalan                    | 2.7.3         | Apache-2.0                    |
| xercesImpl               | 2.12.2        | Apache-2.0                    |
| xml-apis                 | 1.4.01        | Apache-2.0                    |
| xmlresolver              | 6.0.14        | Apache-2.0                    |

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