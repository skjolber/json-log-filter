[![Build Status](https://travis-ci.org/skjolber/json-log-filter.svg?branch=master)](https://travis-ci.org/skjolber/json-log-filter) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=skjolber_json-log-filter&metric=coverage)](https://sonarcloud.io/dashboard?id=skjolber_json-log-filter)

# json-log-filter
High-performance filtering of to-be-logged JSON. Reads, filters and writes JSON in a single step - drastically increasing throughput. Typical use-cases:

  * Filter sensitive values from logs (i.e. on request-/response-logging)
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
 * Skip or speed up filtering for remainder of document after a number of anonymize and/or prune hits

Bugs, feature suggestions and help requests can be filed with the [issue-tracker].

## License
[Apache 2.0]

## Obtain
The project is built with [Maven] and is available on the central Maven repository. 

### Maven

Add the property
```xml
<json-log-filter.version>1.0.20</json-log-filter.version>
```

then add

```xml
<dependency>
    <groupId>com.github.skjolber.json-log-filter</groupId>
    <artifactId>core</artifactId>
    <version>${json-log-filter.version}</version>
</dependency>
```

or

```xml
<dependency>
    <groupId>com.github.skjolber.json-log-filter</groupId>
    <artifactId>jackson</artifactId>
    <version>${json-log-filter.version}</version>
</dependency>
```

### Gradle
For

```groovy
ext {
  jsonLogFilterVersion = '1.0.20'
}
```

add

```groovy
api("com.github.skjolber.json-log-filter:core:${jsonLogFilterVersion}")
```

or

```groovy
api("com.github.skjolber.json-log-filter:jackson:${jsonLogFilterVersion}")
```


# Usage
Use a `DefaultJsonLogFilterBuilder` or `JacksonJsonLogFilterBuilder` to configure a filter instance (all filters are thread safe): 

```java
JsonFilter filter = DefaultJsonLogFilterBuilder.createInstance()
                       .withMaxStringLength(127) // cuts long texts
                       .withAnonymize("/customer/email") // inserts ***** for values
                       .withPrune("/customer/account") // removes whole subtree
                       .withMaxPathMatches(16) // halt anon/prune after a number of hits
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
Configure max path matches; so that anonymize and/or prune filtering stops after a number of matches. This means the __filter speed can be increased considerably if the number of matches is known to be a fixed number__; and will approach pass-through performance if those matches are in the beginning of the document.

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

For a typical REST service, the above operations might be necessary for the (untrusted) incoming request payload, but not the (trusted) outgoing response payload. 

Note that 
  
 * the `Jackson`-based processors in this project do both of these automatically, and 
 * most frameworks do databinding and/or schema-validation, so at some point the incoming request is known to be valid JSON. An ideal implementation takes advantage of this, logging as text if the databinding fails, otherwise logging as (filtered) JSON.

## Performance
The `core` processors within this project are faster than the `Jackson`-based processors. This is expected as parser/serializer features have been traded for performance. 

Performance summary:

 * `core` is between 2x to 6x as fast as `Jackson` processors, where
 * skipping large parts of JSON documents (prune) decreases the difference, and
 * small documents increase the difference, as `Jackson` is more expensive to initialize.

Note that both processors can parse __at least one thousand 100KB documents per second__. For a typical, light-weight web service, the overall performance improvement for using the `core` filters over the `Jackson`-based filters, will most likely be in the order of a few percent.

Memory use will be at 2x-8x the raw JSON byte size; depending on the invoked `JsonFilter` method (some accept string, other raw bytes or chars).

See the benchmark results ([JDK 8](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/benchmark/jmh/results/jmh-results-1.0.17.jdk8.json&topBar=off), [JDK 11](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/benchmark/jmh/results/jmh-results-1.0.17.jdk11.json&topBar=off)) and the [JMH] module for running detailed benchmarks.

Please consider refactoring your JSON structure(s) if you do a lot of filtering of static data and such.

## Framework support
See the [spring-boot-starter-logbook](frameworks/spring-boot-starter-logbook) module for request-/response logging. Filtering can be specified per path and request/response.

## Background
The project is intended as a complimentary tool for use alongside JSON frameworks, such as JSON-based REST stacks. Its primary use-case is processing to-be logged JSON. The project relies on the fact that such frameworks have very good error handling, like schema validation and databinding, to apply a simplified view of the JSON syntax, basically handling only the happy-case of a well-formed document. The frameworks themselves detect invalid documents and handle them as raw content. 

# See also
See the [xml-log-filter] for corresponding high-performance filtering of XML. 

Using SIMD for parsing JSON: 
 * [simdjson](https://github.com/simdjson/simdjson)
 * [sparser](https://blog.acolyer.org/2018/08/20/filter-before-you-parse-faster-analytics-on-raw-data-with-sparser/)

[Apache 2.0]:			https://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:		https://github.com/skjolber/json-log-filter/issues
[Maven]:				https://maven.apache.org/
[JMH]:					benchmark/jmh
[xml-log-filter]:      	https://github.com/skjolber/xml-log-filter
[High-performance]:		https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/docs/benchmark/jmh-result.json&topBar=off
[Jackson]:				https://github.com/FasterXML/jackson-core
[JSON]:					https://www.json.org/json-en.html
[Logbook]:				https://github.com/zalando/logbook






