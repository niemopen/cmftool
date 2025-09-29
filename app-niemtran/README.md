<img src="https://github.com/niemopen/oasis-open-project/blob/main/artwork/NIEM-NO-Logo-v5.png" width="200">

# NIEM Message Translation (NIEMTran) tool, version 1.0

This subproject is part of the CMFTool project repository.  It contains the NIEMOpen message translation tool (NIEMTran).

At present, NIEM supports two message serializations:  XML and JSON.  A message in one can be transformed to the equivalent message in the other.  NIEMTran uses the information in the message model to drive the transformation.  It is a multi-level command-line tool; at present, one subcommand is implemeted:

*  [*x2j*](#convert-niem-xml-to-json) -- convert a NIEM message from XML to JSON

### Convert NIEM XML to JSON

*Usage:* **niemtran x2j** *[options]* *model.cmf message.xml ...*

Converts each *message.xml* file to the equivalent *message.json*.

| Options: | |
| -- | -- |
| `-c, --context` |  generate complete @context in the result |
| `--curi URI`   |  include "@context:" URI pair in the result |
| `-f, --force` |  overwrite existing .json files |

## Getting started

1. You must have a Java runtime environment.  JRE21 or later will work.  JRE17 might work.  
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

NIEMTran depends on the *lib-cmf* and *lib-util* subprojects in this repository.  It also depends on the following libraries, all of which are unmodified, and can be found at [mvnrepository.com](https://mvnrepository.com):

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