# Quick start

The directory "build/install/nmftool" contains a working installation.
Put "build/install/nmftool/bin" in your PATH and nmftool will run from 
the command line.

# Testing

The directory "src/test/resources/Test" contains a number of XSD examples.
Run "nmftool x2m Foo.xsd > Foo.cmf" to generate CMF model from XSD.
Run "nmftool m2x -o /tmp/cmf Foo.cmf" to generate XSD from CMF.

# Building

This project was built with NetBeans 12.0, Gradle 6.8.3, and OpenJDK 11.0.12
Try "gradle installDist" 
