<img src="https://github.com/niemopen/oasis-open-project/blob/main/artwork/NIEM-NO-Logo-v5.png" width="200">

# Common Model Format Tool (CMFTool)

This repository is part of the NIEMOpen project.  It contains the NIEMOpen Common Model Format Tool (CMF). 

The NIEM [*Common Model Format (CMF)*](https://github.com/niemopen/common-model-format) is a data modeling formalism for NIEM-conforming data exchange specifications.  CMFTool is a command-line tool for the designers of those specifications.

For more information on NIEMOpen, see the project's website at [www.niemopen.org](https://github.com/niemopen/cmftool/blob/main/www.niemopen.org).

General questions about OASIS Open Projects may be directed to OASIS staff at [project-admin@lists.oasis-open-projects.org](mailto:project-admin@lists.oasis-open-projects.org)

## What's new in version 1.0-alpha.4

* `cmftool m2jmsg` generates JSON Schema to validate a NIEM JSON message.
* `cmftool m2xmsg` generates an XML Schema pile to validate a NIEM XML message.  This schema is much simpler than the XSD representation of a model.
  * Abstract elements with no substitutions are removed
  * Abstract elements with one substituting element are replaced by that element
  * Abstract elements with more than one substituting element are replaced by `xs:choice`
  * Datatypes are represented by `xs:simpleType`
  * Proxy types are replaced by the corresponding XSD primitive
  * Type definitions do not inherit from the structures namespace.  Reference attributes (`structures:id`, `ref`, and `uri` are explicitly added to type definitions according to `referenceCode`.

## What's new in version 1.0-alpha.3

The code base is reorganized into four subprojects:

* *app-cmftool:*  Code for the *cmftool* command-line program
* *app-scheval:*  Code for the *scheval* command-line program.
* *lib-cmf:*  Library for all things CMF and NIEM-XSD
* *lib-util:*  Utility library for people who don't necessarily care about CMF

The CMF specification has changed significantly since the alpha.2 release.  

* The representation of augmentations is completely different (and much simpler)
* Wildcards (xs:any and xs:anyAttribute) are supported
* Internationalization support added for all documentation strings

This version of CMFTool is not compatible with older CMF files.  Older versions of CMFTool are not compatible with current CMF files.

## Getting started

1. You must have a Java runtime environment.  JRE21 or later will work.  JRE17 might work.  
   - Try `java –version` from the command line.  If that works, you should be OK
   - Otherwise make sure your `JAVA_HOME` environment variable points to your JRE
2. Unpack the executable distribution from the Assets tab on the [Release page](https://github.com/niemopen/cmftool/releases)
   - The *cmftool* program is in *cmftool-1.0-alpha.3.zip*
   - The *scheval* program is in *scheval-1.0-beta.1.zip*
   - It's OK to combine the *bin* and *lib* directories from both.

3. Put the *bin* directory into your PATH
4. Try `cmftool help` from the command line

## Examples

The *CrashDriver* message specification is in *lib-cmf/examples/CrashDriver*.  The XSD representation of the message model in the *model.xsd* directory exercises most of the features of the NIEM 6 naming and design rules. 

* `cmftool x2m -o tmp.cmf model.xsd/cmf.xsd` will create the CMF version of the model in *tmp.cmf*.  This should be the same as *model.cmf*.
* `cmftool m2x -o tmp tmp.cmf` will create the XSD version of the model in the *tmp* directory.  This should be the same as *model.xsd*.

## Testing

The *src/test/resources* directories contains resources for the JUnit tests.  Many, many examples there.

## Building

This project was built with NetBeans 24, Gradle 8.12, and Oracle JDK 21.
Try `./gradlew build`

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
