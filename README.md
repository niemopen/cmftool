<img src="https://github.com/niemopen/oasis-open-project/blob/main/artwork/NIEM-NO-Logo-v5.png" width="200">

# Common Model Format Tool (CMFTool)

This repository is part of the NIEMOpen project.  It contains the NIEMOpen Common Model Format Tool (CMF). 

The NIEM [*Common Model Format (CMF)*](https://github.com/niemopen/common-model-format) is a data modeling formalism for NIEM-conforming data exchange specifications.  CMFTool is a command-line tool for the designers of those specifications.

For more information on NIEMOpen, see the project's website at [www.niemopen.org](https://github.com/niemopen/cmftool/blob/main/www.niemopen.org).

General questions about OASIS Open Projects may be directed to OASIS staff at [project-admin@lists.oasis-open-projects.org](mailto:project-admin@lists.oasis-open-projects.org)

## CMFTool commands

* `x2m` command generates CMF from a source or message XSD pile
* `m2m` command generates a canonical version of a CMF file
* `m2o` command generates OWL from CMF (needs work)
* `m2xn5` command generates NIEM 5 XSD from CMF
* `m2xs` command generates NIEM 6 source XSD from CMF
* `m2xm` command generates NIEM 6 message XSD from CMF
* `n5to6` command converts a NIEM 5 model (CMF or XSD) to NIEM 6 CMF, rewriting namespace URIs (needs fixing)
* `xcanon` command generates a canonical version of an XSD document
* `xcmp` command compares two XML schemas

Every command accepts a `–help` argument, for providing, well… help.

## What's new in version 1.0-alpha.2

* CMF now has `ObjectProperty` and `DataProperty`
* `cmf:ReferenceCode` and `appinfo:referenceCode` is working
* Global element augmentation is working
* Message schema generation is working
* Builtin schema documents are updated to version PS02
  * Add `structures:appliesToParent` to `AssociationType` and `ObjectType` in *structures.xsd*
  * Add proxies for xs:Name, NCName, QName to *niem-xs.xsd*

## Still to be done

* Writing partial models in CMF, composing a model from parts
* Handle `appinfo:relationshipPropertyIndicator`
* Implement `appinfo:AttributeAugmentation` 
* Corner cases for `xs:simpleType`
* Ordered properties and xs:list
* Definitions
  * Repeated (with `xml:lang`), including inside `LocalTerm` elements
  * On element/attribute references (and HasProperty objects)
  * On `xs:import elements`

## Quick start

After a build, the directory "build/install/cmftool" contains a working installation.
Put "build/install/cmftool/bin" in your PATH and cmftool will run from the command line.

The release ZIP file has all the scripts, resources, and libraries, without the source code.  You could use that instead.

Run "cmftool x2m foo.xsd" to generate CMF model (foo.cmf) from XSD.
Run "cmftool m2xsrc -o tmp foo.cmf" to generate XSD from CMF.
Run "cmftool help" for a list of commands.

## Examples

There is an "examples" directory, with... examples.  But they are still NIEM 5 based. 

## Testing

The directory "src/test/resources" contains resources for the JUnit tests.  Many, many examples there.

## Building

This project was built with NetBeans 21.0, Gradle 8.6, and Oracle JDK 21.0.2
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
