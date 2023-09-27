<img src="https://github.com/niemopen/oasis-open-project/blob/main/artwork/NIEM-NO-Logo-v5.png" width="200">

# Common Model Format Tool (CMFTool)

This repository is part of the NIEMOpen project.  It contains the NIEMOpen Common Model Format Tool (CMF). 

The NIEM [*Common Model Format (CMF)*](https://github.com/niemopen/common-model-format) is a data modeling formalism for NIEM-conforming data exchange specifications.  CMFTool is a command-line tool for the designers of those specifications.

For more information on NIEMOpen, see the project's website at [www.niemopen.org](https://github.com/niemopen/cmftool/blob/main/www.niemopen.org).

General questions about OASIS Open Projects may be directed to OASIS staff at [project-admin@lists.oasis-open-projects.org](mailto:project-admin@lists.oasis-open-projects.org)

## What's new in version 0.7-alpha.4 (26 September)

* Many many bugs fixed
* Entire NIEM 5 model converts to CMF and back to XSD
* The usual builtin schema documents now obtained from the JAR file
* NIEM 6 XSD generation still doesn't work

## What's new in version 0.7-alpha.3 (9 August)

* New features in CMF to XSD commands
  * `cmftool m2xref` renamed to `cmftool m2xsrc`
  * Option to generate xml-catalog.xsd file
  * Option to specify name of generated XML Catalog file
  * Option to specify message schema "root namespace" with all needed import elements

## What's new in version 0.7-alpha.2

* Now generates CMF version 0.8, which is based on NIEM 6
* There are NIEM 6 builtin schema documents in *src/main/dist/share*.  These are based on the Wildcard Augmentation discussions.
* **n5to6** command converts a NIEM 5 model (CMF or XSD) to NIEM 6, rewriting namespace URIs
* CMF to XSD command split into three
  * **m2xn5** command generates NIEM 5 XSD
  * **m2xref** command generates NIEM 6 reference XSD
  * **m2xmsg** command will generate NIEM 6 message XSD (not implemented yet)

## What's new in version 0.7-alpha.1

* Handling code-list-instance (CLI) properties
* Handling code-list-schema-attribute (CLSA) appinfo
* Augmentations are now primarily recorded as properties in the Namespace object
* The CMF submodule is now in the OASIS repo

## What's working now

- NIEM XML schema pile to Common Model Format
- Common Model Format to NIEM XML schema pile (no catalog file yet)
- NIEM 5.2 niem-core model converts from XSD to CMF and back to XSD with no significant change
  (except LocalTerm appinfo is not handled yet)
- Entire NIEM 5.2 model converts from XSD to CMF, and vice versa.  (Haven't looked for significant changes yet.)

## Quick start

After a build, the directory "build/install/cmftool" contains a working installation.
Put "build/install/cmftool/bin" in your PATH and cmftool will run from 
the command line.

The release ZIP file has all the scripts, resources, and libraries, without the source code.  You could use that instead.

Run "cmftool x2m Foo.xsd > Foo.cmf" to generate CMF model from XSD.
Run "cmftool m2x -o /tmp/xsd Foo.cmf" to generate XSD from CMF.
Run "cmftool help" for a list of commands.

## Examples

There is an "examples" directory, with... examples.

## Testing

The directory "src/test/resources" contains resources for the JUnit tests.

## Building

This project was built with NetBeans 17.0, Gradle 8.0.2, and Oracle JDK 17.0.7
Try "gradle installDist" 

## Other assets

In addition to this GitHub repository, this project also makes use of other assets.

- The NIEMOpen website is at [www.niemopen.org](http://www.niemopen.org/). The website contains news, announcements, and other information of interest about the project.
- The [General purpose mailing list](https://lists.oasis-open-projects.org/g/niemopen). To subscribe, send an empty email message to [niemopen+subscribe@lists.oasis-open-projects.org](mailto:niemopen+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe and send email to the list. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen/messages).
- The [Project Governing Board mailing list](https://lists.oasis-open-projects.org/g/niemopen-pgb). This is the discussion list for use by the members of the PGB. To subscribe, send an empty email message to [niemopen-pgb+subscribe@lists.oasis-open-projects.org](mailto:niemopen-pgb+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe read-only. Only PGB members can post. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen-pgb/messages).
- [NBAC Technical Steering Committee mailing list](https://lists.oasis-open-projects.org/g/niemopen-nbactsc). This is the discussion list for use by the members of the NIEM Business Architecture Committee TSC. To subscribe, send an empty email message to [niemopen-nbactsc+subscribe@lists.oasis-open-projects.org](mailto:niemopen-nbactsc+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe read-only. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen-nbactsc/messages).
- [NTAC Technical Steering Committee mailing list](https://lists.oasis-open-projects.org/g/niemopen-ntactsc). This is the discussion list for use by the members of the NIEM Technical Architecture Committee TSC. To subscribe, send an empty email message to [niemopen-ntactsc+subscribe@lists.oasis-open-projects.org](mailto:niemopen-ntactsc+subscribe@lists.oasis-open-projects.org). Anyone interested is welcome to subscribe read-only. The list maintains an [archive](https://lists.oasis-open-projects.org/g/niemopen-ntactsc/messages).

## Contributing

Please read [CONTRIBUTING.md](https://github.com/niemopen/cmftool/blob/main/CONTRIBUTING.md) for details how to join the project, contribute changes to our repositories and communicate with the rest of the project contributors. Please become familiar with and follow the [code of conduct](https://github.com/niemopen/cmftool/blob/main/CODE-OF-CONDUCT.md).

## Governance

NIEM Open operates under the terms of the [Open Project Rules](https://www.oasis-open.org/policies-guidelines/open-projects-process) and the applicable license(s) specified in [LICENSE.md](https://github.com/niemopen/cmftool/blob/main/LICENSE.md). Further details can be found in [GOVERNANCE.md](https://github.com/niemopen/cmftool/blob/main/GOVERNANCE.md), [GOVERNANCE-NBAC.md](https://github.com/niemopen/cmftool/blob/main/GOVERNANCE-NBAC.md), and [GOVERNANCE-NTAC.md](https://github.com/niemopen/cmftool/blob/main/GOVERNANCE-NTAC.md).

## CLA & Non-assert signatures required

All technical contributions must be covered by a Contributor's License Agreement. This requirement allows our work to advance through OASIS standards development stages and potentially be submitted to de jure organizations such as ISO. You will get a prompt to sign this document when you submit your first pull request to a project repository, or you can sign [here](https://cla-assistant.io/niemopen/oasis-open-project). If you are contributing on behalf of your employer, you must also sign the ECLA [here](https://www.oasis-open.org/open-projects/cla/entity-cla-20210630/).q
