flatworm: Flat File Parser
=====================================
Flatworm is a Java library intended to allow a developer to describe the format of a flat file using an XML definition file, and then to be able to automatically read lines from that file, and have one or more beans be instantiated for each logical record (original description from James M. Turner). 

Requires JDK 1.8 or higher (as 4.0.0-SNAPSHOT).

Latest release
--------------
The most recent release is [flatworm 3.0.2][], which was released by Josh Brackett. Work on [flatworm 4.0.0-SNAPSHOT][] is currently underway, but is in SNAPSHOT release so it is not yet available via Maven Central repository.

To add a dependency on Guava using Maven, use the following:

```xml
<dependency>
  <groupId>com.blackbear</groupId>
  <artifactId>flatworm</artifactId>
  <version>3.0.3</version>
</dependency>
```

The actively developed release is [flatworm 4.0.0-SNAPSHOT][]. flatworm 4.0.0 will not be backwards compatible with as there was far too much new functionality added to maintain the original structure and support, but the change will be worth it.

Background
----------
The flatworm project is inherited from the efforts of multiple others. The original source code was authored by James M. Turner in what appears to be 2004. The last contributions from Mr. Turner appear to be 2.0.x and from there it appears that Josh Brackett took over and iterated it to the 3.0.x releases. There are also remnants of edits from another developer, Dave Berry, but it's unclear at what point those contributions were made.

Alan Henson has since taken over the code base and has modified it to be compatible with Maven from a build and package management sense. Additionally, where applicable, code has been converted to use Java 8 constructs. 

License
-------
flatworm is open source and is licensed under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0).

Documentation
-------------
There is an *.html file in the docs folder that explains usage, but it's a bit out of date. The hope is to get this updated soon. In the mean time, have a look at the test cases for the best examples of usage. The examples are also out of date, there is a TODO item for that as well.

Work Completed
--------------
* Added support for Maven
* Cleaned up exceptions to have two main checked-exceptions: 
 * FlatwormConfigurationException - used for indicating issues with parsing configuration data
 * FlatwormParserException - used for indicating issues with parsing the actual data
* Removed Generics type declaration where the type could be inferred: `List<String> result = new ArrayList<>();`
* Added use of the foreach construct `for(String item : collection)`
* Added use of `Streams` and `Lambdas` where it mae code cleaners
* Cleaned up some comments
* Removed com.blackbear.flatworm.Callback - use ExceptionCallback or RecordCallback instead (moved to new callbacks package)
* Added ability to use a JavaScript snippet to see if a line should be processed by a given Record (can also specify a script file and method name to keep scripts external to code)
* Added ability to specify ignore-field on a record-element to explicitly ignore it
* Changed the record-element "type" attribute to "converter-name" as that's what it's really linked to
* Changed the minlength/maxlength attributes for the length-ident element to min-length/max-length for consistency
* On Field Identity (field-ident) - added ignore-case tag to indicate whether or not case should play a factor in comparison
* Added support for single segment-element configurations where the child doesn't have to be a collection
* Added support for non-delimited segment-elements - a "child" line can be a non-delimited line
* Added line identifiers
* Added annotation support
* Added ability to auto-resolve the converter type based upon the field's type (given that it's a common type in String, Double, Float, Long, or Integer).
* Added more constants where appropriate. There is likely more that can be done here
* Added support for scripts to be executed before a record is read and after a record is read - which allows for dynamic reconfiguration of a FileFile during parsing - some files specify their parsing rules within the file so static configuration must be updated at run time
	* Record
		* Before record is read:
			* Parameters: `(FileFormat fileFormat, String line)`
			* Return: `ignored`
		* After record is read:
			* Parameters: `(FileFormat fileFormat)`
			* Return: `ignored`
	* Line
		* Before line is read:
			* Parameters: `(LineBO line, String inputLine, Map<String, Object> beans, ConversionHelper conversionHelper)`
			* Return: `ignored`
		* After line is read:
			* Parameters: `(LineBO line, String inputLine, Map<String, Object> beans, ConversionHelper conversionHelper)`
			* Return: `ignored`
* Added ability to specify multiple configuration options and then specify the preferred one at run time
* Added ability to create Line identifiers (vs. them inheriting purely from the record along). The Script Identity script will take three parameters:
	* Parameters: `(FileFormat fileFormat, LineBO line, String line)`
* Added support to "optional" lines. Meaning, the parser doesn't "skip" a line if a LineBO has an Identity set for lineIdentity but the line has no data present for a given record.
* When using the Field Identity, start position is no longer required for Record Elements as it can be auto-derived from the Field Identity's fieldLength property
* Added ability to instruct the parser to trim the results read for a Record Element.
* Added ability to instruct the parser to not enforce the last record element to be of a certain length if the line ends - `enforce-field-length = false` in the XML and `enforceFieldLength = false` in the RecordElement annotation.

TODOs
-------
* Update the "Field Guide" to reflect the latest changes and usages
* Update the examples to reflect the latest capabilities and provide more guidance within the examples
* Add the ability to specify a type in addition-to or instead-of a converter
* Complete checklist for deploying production jar to Maven Central repository
* Refactor the parsing logic from the beans that hold the configuration data
* Add missing JavaDocs
* Add ability for folks to write their own Identity implementations and make them annotation enabled. Right now they would have to build their own annotation configuration loader and extend the relevant parts - which is fine, but this can be done more cleanly I think.
* Add more verbose logging
* Is the field-length attribute really needed on Field Identity? We know the length by the matching strings. They should all be the same length else the match will always fail
* Refactor the concept of start/end records and how LineBO's are used for broader-scale records - meaning, when a LineBO acts more like a record then a line.

[flatworm 3.0.2]: https://github.com/trx/flatworm
[flatworm 4.0.0-SNAPSHOT]: https://github.com/ahenson/flatworm