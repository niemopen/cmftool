# What Is CMFTool?

[NIEM](https://www.niemopen.org) is an [OASIS Open Project](https://www.oasis-open.org/open-projects/) providing a framework for creating machine-to-machine data exchange specifications.  The NIEM [*Common Model Format (CMF)*](https://github.com/niemopen/common-model-format) is a data modeling formalism for NIEM-conforming data exchange specifications.  CMFTool is a command-line tool for the designers of those specifications.

# What's working in version 0.6

- NIEM XML schema pile to Common Model Format
- Common Model Format to NIEM XML schema pile (no catalog file yet)

# Quick start

After a build, the directory "build/install/cmftool" contains a working installation.
Put "build/install/cmftool/bin" in your PATH and cmftool will run from 
the command line.

The release ZIP file has all the scripts, resources, and libraries, without the source code.  You could use that instead.

Run "cmftool x2m Foo.xsd > Foo.cmf" to generate CMF model from XSD.
Run "cmftool m2x -o /tmp/xsd Foo.cmf" to generate XSD from CMF.

# Examples

There is an "examples" directory, with... examples.

# Testing

The directory "src/test/resources" contains resources for the JUnit tests.

# Building

This project was built with NetBeans 15.0, Gradle 7.3.3, and Oracle JDK 17.0.6
Try "gradle installDist" 
