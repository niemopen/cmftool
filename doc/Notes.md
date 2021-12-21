* 10 December 2021

* Not Done Yet 

- External namespaces and adapters:  No namespace object created at present for
  externals, so you'll get a NullPointerException at runtime.

* 9 November 2021

* Model extensions

Separate extension file, or extension elements in Model document?
Perhaps separate message files in the same message specification,
with the same extension; eg. Model.cmf and ModelXML.cmf

* ModelToXSD

- Not keeping track of specific utility schema versions, or of the proxy schema.
  Just use the latest version.  Later on, the version can be a program option.

- CodeType becomes a Datatype, not a ClassType

* Not Done Yet

- Appinfo
- Code lists-instance
- Code-lists-schema-appinfo
