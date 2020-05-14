[![Build Status](https://travis-ci.org/skjolber/json-log-filter.svg?branch=master)](https://travis-ci.org/skjolber/json-log-filter) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=skjolber_json-log-filter&metric=coverage)](https://sonarcloud.io/dashboard?id=skjolber_json-log-filter)

# json-log-filter
High-performance filtering of to-be-logged JSON. Reads, filters and writes JSON in a single step - drastically increasing throughput. Typical use-cases

  * Filter sensitive values from logs 
     * technical details like passwords and so on
     * sensitive personal information, for [GDPR](https://en.wikipedia.org/wiki/General_Data_Protection_Regulation) compliance and such
  * Improve log readability, filtering
     * large String elements like base64-encoded binary data, or
     * whole JSON subtrees with low informational value
  * Reduce amount of data sent to log accumulation tools
    * lower cost, and
    * potentially reduce search / visualization latency

Features:

 * Truncate large text values
 * Mask (anonymize) scalar values like String, Number, Boolean and so on.
 * Remove (prune) whole subtrees
  
Bugs, feature suggestions and help requests can be filed with the [issue-tracker].

## License
[Apache 2.0]

## Obtain
The project is built with [Maven] and is available on the central Maven repository.

```xml
<dependency>
    <groupId>com.github.skjolber.json-log-filter</groupId>
    <artifactId>core</artifactId>
    <version>1.0.0</version>
</dependency>
```

or

```xml
<dependency>
    <groupId>com.github.skjolber.json-log-filter</groupId>
    <artifactId>jackson</artifactId>
    <version>1.0.0</version>
</dependency>
```

# Usage
Use a `JsonLogFilterBuilder` or `JacksonJsonLogFilterBuilder` to configure a filter instance (all filters are thread safe): 

```java
JsonFilter filter = JsonLogFilterBuilder.createInstance()
                       .withMaxStringLength(127) // cuts long texts
                       .withAnonymize("/customer/email") // inserts ***** for values
                       .withPrune("/customer/account") // removes whole subtree
                       .withMaxPathMatches(16) // halt filtering after a number of hits
                       .build();
                       
String json = ...; // obtain JSON

String filtered = filter.process(json); // perform filtering                       
```

### Max string sizes
Configure max string length for output like

```json
{
    "key": "QUJDREVGR0hJSktMTU5PUFFSU1...TRUNCATED BY 46"
}
```

### Anonymizing attributes and/or elements
Configure anonymize for output like

```json
{
    "key": "*****"
}
```

See below for supported path syntax.

### Removing subtrees (prune)
Configure prune for outputs like

```json
{
    "key": "SUBTREE REMOVED"
}
```

See below for supported path expression syntax.

### Max path matches
Configure max path matches; so that anonymize and/or prune filtering stops after a number of matches. This means the filter speed can be increased considerably if the number of matches is known to be a fixed number; and those matches are in the beginning of the document.

For example if the to-be filtereded JSON document has a schema definition with a header + body structure, and the target value is in the header.   

### Path expressions
A simple syntax is supported, where each path segment corresponds to a `field name`. Expressions are case-sensitive. Supported syntax:

    /my/field/name

with support for wildcards; 

    /my/field/*

or a simple any-level element search 

    //myFieldName

The filters within this library support using multiple expressions at once.

## Post-processing
Depending on your service stack and architecture, performing two additional operations might be necessary:

 * removing linebreaks (and possibly all whitespace)
   * for `one line per log-statement`, typically for console- and/or file logging
 * validate document syntax (as [JSON])
   * for raw inlining of JSON from untrusted sources in log statements
 
The `Jackson`-based processors in this project do both of these automatically. 

Most frameworks do databinding and/or schema-validation, so ideally this happens before filtering; logging as text if the databinding fails, otherwise logging as JSON.

## Performance
The `core` processors within this project are faster than the `Jackson`-based processors. This is expected as parser/serializer features have been traded for performance. 

Performance summary:

 * `core` is between 2x to 6x as fast as `Jackson` processors, where
 * skipping large parts of JSON documents (prune) decreases the difference, and
 * small documents increase the difference, as `Jackson` is more expensive to initialize.

Note that both processors can parse __at least one thousand 100KB documents per second__, and that the fastest filters will improve overall system performance by no more than a few percent. 

Memory use will be approximately two times the JSON string size.

See the benchmark results ([JDK 8](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/benchmark/jmh/results/jmh-results-1.0.1.jdk8.json&topBar=off), [JDK 11](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/benchmark/jmh/results/jmh-results-1.0.1.jdk11.json&topBar=off)) and the [JMH] module for running detailed benchmarks.

## Background
The project is intended as a complimentary tool for use alongside JSON frameworks, such as JSON-based REST stacks. Its primary use-case is processing to-be logged JSON. The project relies on the fact that such frameworks have very good error handling, like schema validation and databinding, to apply a simplified view of the JSON syntax, basically handling only the happy-case of a well-formed document. The frameworks themselves detect invalid documents and handle them as raw content. 

# See also
See the [xml-log-filter] for corresponding high-performance filtering of XML. 

# History
- [1.0.0]: Initial version.

[1.0.0]:				releases
[Apache 2.0]:			https://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:		https://github.com/skjolber/json-log-filter/issues
[Maven]:				https://maven.apache.org/
[JMH]:					benchmark/jmh
[xml-log-filter]:       https://github.com/skjolber/xml-log-filter
[High-performance]:		https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/docs/benchmark/jmh-result.json&topBar=off
[Jackson]:				https://github.com/FasterXML/jackson-core
[JSON]:					https://www.json.org/json-en.html
