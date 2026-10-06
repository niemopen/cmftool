<img src="https://github.com/niemopen/oasis-open-project/blob/main/artwork/NIEM-NO-Logo-v5.png" width="200">

# Common Model Format Tool (CMFTool), version 1.1.0

This subproject is part of the CMFTool project repository.  It contains the NIEMOpen Common Model Format Tool (CMF). 

The NIEM [*Common Model Format (CMF)*](https://github.com/niemopen/common-model-format) is a data modeling formalism for NIEM-conforming data exchange specifications.  CMFTool is a multi-level command-line tool for the designers of those specifications.

## Getting started

1. You must have a Java runtime environment.  JRE25 or later will work.  JRE17 might work.  
   - Try `java –-version` from the command line.  If that works, you should be OK
   - Otherwise make sure your `JAVA_HOME` environment variable points to your JRE

2. Unpack the executable distribution from the Assets tab on the [Release page](https://github.com/niemopen/cmftool/releases)
   - The *cmftool* program is in *cmftool-1.1.0.zip*
   - The *niemtran* program is in *niemtran-1.1.0.zip*
   - The *scheval* program is in *scheval-1.1.0.zip*
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

| Type | Group | Name | Version | License | PURL |
|---|---|---|---|---|---|
| library | com.google.code.gson | gson | 2.13.2 | Apache-2.0 | `pkg:maven/com.google.code.gson/gson@2.13.2?type=jar` |
| library | com.google.errorprone | error_prone_annotations | 2.41.0 | Apache-2.0 | `pkg:maven/com.google.errorprone/error_prone_annotations@2.41.0?type=jar` |
| library | commons-io | commons-io | 2.22.0 | Apache-2.0 | `pkg:maven/commons-io/commons-io@2.22.0?type=jar` |
| library | info.picocli | picocli | 4.7.7 | Apache-2.0 | `pkg:maven/info.picocli/picocli@4.7.7?type=jar` |
| library | net.sf.saxon | Saxon-HE | 12.5 | MPL-2.0 | `pkg:maven/net.sf.saxon/Saxon-HE@12.5?type=jar` |
| library | org.apache.commons | commons-lang3 | 3.20.0 | Apache-2.0 | `pkg:maven/org.apache.commons/commons-lang3@3.20.0?type=jar` |
| library | org.apache.logging.log4j | log4j-api | 2.24.3 | Apache-2.0 | `pkg:maven/org.apache.logging.log4j/log4j-api@2.24.3?type=jar` |
| library | org.apache.logging.log4j | log4j-core | 2.24.3 | Apache-2.0 | `pkg:maven/org.apache.logging.log4j/log4j-core@2.24.3?type=jar` |
| library | org.apiguardian | apiguardian-api | 1.1.2 | Apache-2.0 | `pkg:maven/org.apiguardian/apiguardian-api@1.1.2?type=jar` |
| library | org.javatuples | javatuples | 1.2 | Apache-2.0 | `pkg:maven/org.javatuples/javatuples@1.2?type=jar` |
| library | org.mitre.niem | lib-cmf | 1.1.0 |  | `pkg:maven/org.mitre.niem/lib-cmf@1.1.0?project_path=%3Alib-cmf` |
| library | org.mitre.niem | lib-util | 1.1.0 |  | `pkg:maven/org.mitre.niem/lib-util@1.1.0?project_path=%3Alib-util` |
| library | org.opentest4j | opentest4j | 1.3.0 | Apache-2.0 | `pkg:maven/org.opentest4j/opentest4j@1.3.0?type=jar` |
| library | org.xmlresolver | xmlresolver | 6.0.14 | Apache-2.0 | `pkg:maven/org.xmlresolver/xmlresolver@6.0.14?type=jar` |
| library | xalan | serializer | 2.7.3 |  | `pkg:maven/xalan/serializer@2.7.3?type=jar` |
| library | xalan | xalan | 2.7.3 |  | `pkg:maven/xalan/xalan@2.7.3?type=jar` |
| library | xerces | xercesImpl | 2.12.2 | Apache-2.0 | `pkg:maven/xerces/xercesImpl@2.12.2?type=jar` |
| library | xml-apis | xml-apis | 1.4.01 | Apache-2.0, SAX-PD, The W3C License | `pkg:maven/xml-apis/xml-apis@1.4.01?type=jar` |

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
