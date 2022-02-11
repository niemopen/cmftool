# What's working

- NIEM XML schema pile to Common Model Format
- Common Model Format to NIEM XML schema pile (no catalog file yet)

# Quick start

After a build, the directory "build/install/cmftool" contains a working installation.
Put "build/install/cmftool/bin" in your PATH and cmftool will run from 
the command line.

The release ZIP file has all the scripts, resources, and libraries, without the source code.  You could use that instead.

Run "cmftool x2m Foo.xsd > Foo.cmf" to generate CMF model from XSD.
Run "cmftool m2x -o /tmp/cmf Foo.cmf" to generate XSD from CMF.

# Examples

There is an "examples" directory, with... examples.

# Testing

The directory "src/test/resources" contains resources for the JUnit tests.

# Building

This project was built with NetBeans 12.7, Gradle 7.3.3, and OpenJDK 17.0.2
Try "gradle installDist" 
