# What's working

- NIEM XML schema pile to Common Model Format
- Common Model Format to NIEM XML schema pile (no catalog file yet)

# Quick start

The directory "build/install/cmftool" contains a working installation.
Put "build/install/cmftool/bin" in your PATH and cmftool will run from 
the command line.

# Testing

The directory "src/test/resources/Test" contains a number of XSD examples.
Run "cmftool x2m Foo.xsd > Foo.cmf" to generate CMF model from XSD.
Run "cmftool m2x -o /tmp/cmf Foo.cmf" to generate XSD from CMF.

# Building

This project was built with NetBeans 12.0, Gradle 6.8.3, and OpenJDK 11.0.12
Try "gradle installDist" 
