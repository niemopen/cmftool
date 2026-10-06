# NIEM Message Translation (NIEMTran) tool

At present, NIEM supports two message serializations:  XML and JSON.  A message in one can be transformed to the equivalent message in the other.  NIEMTran uses the information in the message model to drive the transformation.  It is a multi-level command-line tool.  The subcommands are:

*  [*x2j*](#convert-niem-xml-to-json) -- convert a NIEM message from XML to JSON
*  [*j2x*](#convert-niem-json-to-xml) -- convert NIEM JSON message to NIEM XML
*  [*j2r*](#convert-niem-json-to-rdf) --  convert NIEM JSON message to RDF

### Convert NIEM XML to JSON

*Usage:* **niemtran x2j** *[options]* *model.cmf message.xml ...*

With one msg.xml and no -o/--output, writes JSON to standard output.\
With multiple msg.xml files, writes multiple msg.json output files

Options:

```text
  -c, --context           generate complete @context in result
      --curi=uri          include "@context": URI in result
  -f, --force             overwrite existing output files
  -o, --output=out.json   write output to out.json; only valid when there is a single msg.xml argument
```

### Convert NIEM JSON to XML

*Usage:* **niemtran j2x** *[options]* *model.cmf message.json ...*

With one msg.json and no -o/--output, writes XML to standard output.\
With multiple msg.json files, writes multiple msg.xml output files

Options:

```text
  -c, --context=context.json
                         JSON-LD context file used to interpret input messages
  -f, --force            overwrite existing output files
  -o, --output=out.xml   write output to out.xml; only valid when there is a single msg.json argument
```

### Convert NIEM JSON to RDF

*Usage:* **niemtran j2r** *[options]* *model.cmf message.json ...*

With one msg.json and no -o/--output, writes RDF to standard output.\
With multiple msg.json files, writes multiple msg.rdf output files

Options:

```text
  -c, --context=context.json
                         JSON-LD context file used to interpret input messages
  -f, --force            overwrite existing output files
  -o, --output=out.rdf   write output to out.rdf; only valid when there is a single msg.json argument
```

## Getting started

1. You must have a Java runtime environment.  JRE25 or later will work.  JRE17 might work.  
   - Try `java –-version` from the command line.  If that works, you should be OK
   - Otherwise make sure your `JAVA_HOME` environment variable points to your JRE

2. Unpack the executable distribution from the Assets tab on the [Release page](https://github.com/niemopen/cmftool/releases)
   - The *niemtran* program by itself is in *niemtran-1.0.zip*
   - All three programs from this repo are in *cmftool-allApps-1.0.zip*

3. Put the *bin* directory into your PATH, create a shell alias, etc.
4. Try `cmftool help` from the command line

## Examples

The [*Crash Driver Report*](https://github.com/iamdrscott/CrashDriver) message specification is designed to test and describe the features of the NIEM technical architecture.  Try `make -n all` to see some of the things you can to with *niemtran*.

