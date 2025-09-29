<img src="https://github.com/niemopen/oasis-open-project/blob/main/artwork/NIEM-NO-Logo-v5.png" width="200">

# Common Model Format Tool (CMFTool)

This repository is part of the OASIS Open Project named [NIEMOpen](https://github.com/niemopen).  NIEMOpen is a community-driven, standards-based approach to defining information exchange packages for multiple business domains.

The NIEM [*Common Model Format (CMF)*](https://github.com/niemopen/common-model-format) is a data modeling formalism for NIEM-conforming data exchange specifications.

This repository contains the NIEMOpen Common Model Format Tool (CMF) software project.  CMFTool provides command-line applications to support message developers and designers working with NIEM models in XSD and CMF format.  It provides software libraries for other tool developers wishing to work with CMF

There are five subprojects in this repository:

* [*app-cmftool*](app-cmftool/README.md) is a Java application project for *cmftool*, a command-line tool for transforming NIEM XSD into CMF, and vice versa.  *cmftool* can also generate useful artifacts for message developers; for example, message schemas in XSD and JSON Schema to validate XML and JSON messages.

* [*app-niemtran*](app-niemtran/README.md) is a Java application project for *niemtran*, a command-line tool for converting NIEM messages from one format to another; for example, converting a NIEM XML message into the equivalent NIEM JSON message.

* [*app-scheval*](app-scheval/README.md) is a Java application project for *scheval*, a command-line tool for compiling and evaluating Schematron rules.  It has special features required for evaluating NIEM naming and design rules written in Schematron.

* [*lib-cmf*](lib-cmf/README.md) is a Java library project providing classes for working with NIEM models in XSD and CMF.  Both *cmftool* and *niemtran* depend on *lib-cmf*.  Other tool developers may find useful functionality here.

* [*lib-util*](lib-util/README.md) is a Java library project providing classes that are used in all subprojects, and which may be useful in other projects.

## Building CMFTool 

This project was built with NetBeans 26, Gradle 8.12, and Oracle JDK 21.\
Try `./gradlew build`

## About NIEMOpen

For more information on NIEMOpen, see the project's website at [www.niemopen.org](https://github.com/niemopen/cmftool/blob/main/www.niemopen.org).

General questions about OASIS Open Projects may be directed to OASIS staff at [project-admin@lists.oasis-open-projects.org](mailto:project-admin@lists.oasis-open-projects.org)

## Other assets

In addition to this GitHub repository, this project also makes use of other assets.

- The NIEMOpen website is at [www.niemopen.org](http://www.niemopen.org/). The website contains news, announcements, and other information of interest about the project.
- The [General purpose mailing list](https://lists.oasis-open-projects.org/g/niemopen). To subscribe, send an empty email message to [niemopen+subscribe@lists.oasis-open-projects.org](mailto:niemopen+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe and send email to the list. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen/messages).
- The [Project Governing Board mailing list](https://lists.oasis-open-projects.org/g/niemopen-pgb). This is the discussion list for use by the members of the PGB. To subscribe, send an empty email message to [niemopen-pgb+subscribe@lists.oasis-open-projects.org](mailto:niemopen-pgb+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe read-only. Only PGB members can post. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen-pgb/messages).
- [NBAC Technical Steering Committee mailing list](https://lists.oasis-open-projects.org/g/niemopen-nbactsc). This is the discussion list for use by the members of the NIEM Business Architecture Committee TSC. To subscribe, send an empty email message to [niemopen-nbactsc+subscribe@lists.oasis-open-projects.org](mailto:niemopen-nbactsc+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe read-only. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen-nbactsc/messages).
- [NTAC Technical Steering Committee mailing list](https://lists.oasis-open-projects.org/g/niemopen-ntactsc). This is the discussion list for use by the members of the NIEM Technical Architecture Committee TSC. To subscribe, send an empty email message to [niemopen-ntactsc+subscribe@lists.oasis-open-projects.org](mailto:niemopen-ntactsc+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe read-only. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen-ntactsc/messages).

## Contributing

Please read [CONTRIBUTING.md](https://github.com/niemopen/cmftool/blob/main/CONTRIBUTING.md) for details how to join the NIEMOpen project, contribute changes to our repositories and communicate with the rest of the project contributors. Please become familiar with and follow the [code of conduct](https://github.com/niemopen/cmftool/blob/main/CODE-OF-CONDUCT.md).

## Governance

NIEMOpen operates under the terms of the [Open Project Rules](https://www.oasis-open.org/policies-guidelines/open-projects-process) and the applicable license(s) specified in [LICENSE.md](https://github.com/niemopen/cmftool/blob/main/LICENSE.md). Further details can be found in [GOVERNANCE.md](https://github.com/niemopen/cmftool/blob/main/GOVERNANCE.md), [GOVERNANCE-NBAC.md](https://github.com/niemopen/cmftool/blob/main/GOVERNANCE-NBAC.md), and [GOVERNANCE-NTAC.md](https://github.com/niemopen/cmftool/blob/main/GOVERNANCE-NTAC.md).

## CLA & Non-assert signatures required

All technical contributions must be covered by a Contributor's License Agreement. This requirement allows our work to advance through OASIS standards development stages and potentially be submitted to de jure organizations such as ISO. You will get a prompt to sign this document when you submit your first pull request to a project repository, or you can sign [here](https://cla-assistant.io/niemopen/oasis-open-project). If you are contributing on behalf of your employer, you must also sign the ECLA [here](https://www.oasis-open.org/open-projects/cla/entity-cla-20210630/).
