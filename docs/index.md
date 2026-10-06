# CMFTool Project

This distribution contains command-line tools and supporting libraries for working with NIEM and CMF artifacts.

## Contents

- `bin/` — launch scripts
- `lib/` — application and dependency jars
- `docs/` — HTML documentation and examples
- `docs/examples/` — sample input files and scripts

## Applications

This distribution may include one or more of these tools:

* [*cmftool*](./cmftool.html) is a Java application project for *cmftool*, a command-line tool for transforming NIEM XSD into CMF, and vice versa.  *cmftool* can also generate useful artifacts for message developers; for example, message schemas in XSD and JSON Schema to validate XML and JSON messages.

* [*niemtran*](./niemtran.html) is a Java application project for *niemtran*, a command-line tool for converting NIEM messages from one format to another; for example, converting a NIEM XML message into the equivalent NIEM JSON message.

* [*scheval*](./scheval.html) is a Java application project for *scheval*, a command-line tool for compiling and evaluating Schematron rules.  It has special features required for evaluating NIEM naming and design rules written in Schematron.

Use the launcher scripts in `bin/` to run the tools.

## Documentation

* [Building message formats with CMFTool](./msgFormats.html) \
  This article describes how to use CMFTool to construct a *message format*.

## Examples

* [Request message specification](./examples/request) \
  A simple message type asking for a quantity of one or more items.
