# Schematron Evaluation (SCHEval) tool

NIEM models can be represented in a profile of XML Schema.  Many of the conformance rules for models in XSD are tested through Schematron rules.  Executing those rules with free or commercial tools can be difficult, because the NIEM rules depend on providing an XML Catalog file as a document node in the XSLT evaluation stage, as follows:

```
 <xsl:param name="xml-catalog" as="document-node()?"/>
```

Some tools provide Schematron results in SVRL format, like this:

```
   <svrl:successful-report test="not(exists(@abstract[xs:boolean(.) = true()]) eq (ends-with(@name, 'Abstract') or ends-with(@name, 'AugmentationPoint') or ends-with(@name, 'Representation')))"
                           role="warning"
                           location="/*[local-name()='schema' and namespace-uri()='http://www.w3.org/2001/XMLSchema'][1]/*[local-name()='element' and namespace-uri()='http://www.w3.org/2001/XMLSchema'][1]">
      <svrl:text>Rule 7-10: A Property object having an AbstractIndicator property with the value true SHOULD have a name ending in "Abstract" or "Representation"; all other components SHOULD NOT.</svrl:text>
   </svrl:successful-report>
```

That is difficult to interpret.  SCHEval can produce a more convenient result, clearly linking each message to a line and column in the XML input file.



```
WARN  7-10.xsd:20:55 -- Rule 7-10: A Property object having an AbstractIndicator property with the value true SHOULD have a name ending in "Abstract" or "Representation"; all other components SHOULD NOT.
WARN  7-10.xsd:21:59 -- Rule 7-10: A Property object having an AbstractIndicator property with the value true SHOULD have a name ending in "Abstract" or "Representation"; all other components SHOULD NOT.
```

*Usage:* **scheval** *[options]* *input.xml ...*

Options: 

```text
  -s, --schema=<schPath>    apply rules from this schematron file
  -x, --xslt=<xsltPath>     apply rules from this compiled schematron file
  -o, --output=<outPath>    write output to this file, or '-' for stdout (default = stdout)
      --svrl                write output in SVRL format
      --compile             compile schema and write output in XSLT format
  -c, --catalog=<catPath>   provide this XML catalog file as $xml-catalog parameter
  -k, --keep                keep temporary files
```

Examples:

* `scheval --compile -s rules.sch -o rules.xslt`
* `scheval -x rules.xslt input.xml`
* `scheval -s rules.sch input.xml`

## Getting started

1. You must have a Java runtime environment.  JRE21 or later will work.  JRE17 might work.  
   - Try `java –-version` from the command line.  If that works, you should be OK
   - Otherwise make sure your `JAVA_HOME` environment variable points to your JRE

2. Unpack the executable distribution from the Assets tab on the [Release page](https://github.com/niemopen/cmftool/releases)
   - The *scheval* program by itself is in *scheval-1.0.zip*
   - All three programs are in *cmftool-allApps-1.0.zip*

3. Put the *bin* directory into your PATH, create a shell alias, etc.
4. Try `cmftool help` from the command line

