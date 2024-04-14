![Build Status](https://github.com/skjolber/json-log-filter/actions/workflows/maven.yml/badge.svg) 
[![Maven Central](https://img.shields.io/maven-central/v/com.github.skjolber.json-log-filter/parent.svg)](https://mvnrepository.com/artifact/com.github.skjolber.json-log-filter)

# json-log-filter
High-performance filtering of to-be-logged JSON. Reads, filters and writes JSON in a single step - drastically increasing throughput (by ~3x-5x). Typical use-cases:

  * Filter sensitive values from logs (i.e. on request-/response-logging)
     * technical details like passwords and so on
     * sensitive personal information, for [GDPR](https://en.wikipedia.org/wiki/General_Data_Protection_Regulation) compliance and such
  * Improve log readability, filtering
     * large String elements like base64-encoded binary data, or
     * whole JSON subtrees with low informational value
  * Reduce amount of data sent to log accumulation tools
    * lower cost
    * potentially reduce search / visualization latency
    * keep within max log-statement size
       * GCP: [256 KB](https://cloud.google.com/logging/quotas)
       * Azure: 32 KB
    
Features:

 * Truncate large text values
 * Mask (anonymize) scalar values like String, Number, Boolean and so on.
 * Remove (prune) whole subtrees
 * Truncate large documents (max total output size)
 * Skip or speed up filtering for remainder of document after a number of anonymize and/or prune hits 
 * Remove whitespace (for pretty-printed documents)
 * Metrics for the above operations + total input and output size

The library contains multiple filter implementations as to accommodate combinations of the above features with as little overhead as possible. The equivalent filters are also implemented using [Jackson]. 

There is also a `path` artifact which helps facilitate per-path filters for request/response-logging applications.

Bugs, feature suggestions and help requests can be filed with the [issue-tracker].

## License
[Apache 2.0]

## Obtain
The project is built with [Maven] and is available on the central Maven repository. 

<details>
  <summary>Maven coordinates</summary>

Add the property
```xml
<json-log-filter.version>x.x.x</json-log-filter.version>
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

</details>

or

<details>
  <summary>Gradle coordinates</summary>

For

```groovy
ext {
  jsonLogFilterVersion = 'x.x.x'
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
</details>

# Usage
Use a `DefaultJsonLogFilterBuilder` or `JacksonJsonLogFilterBuilder` to configure a filter instance (all filters are thread safe): 

```java
JsonFilter filter = DefaultJsonLogFilterBuilder.createInstance()
                       .withMaxStringLength(127) // cuts long texts
                       .withAnonymize("$.customer.email") // inserts ***** for values
                       .withPrune("$.customer.account") // removes whole subtree
                       .withMaxPathMatches(16) // halt anon/prune after a number of hits
                       .withMaxSize(128*1024)
                       .build();
                       
String json = ...; // obtain JSON

String filtered = filter.process(json); // perform filtering                       
```

### Max string sizes
Configure max string length for output like

```json
{
    "icon": "QUJDREVGR0hJSktMTU5PUFFSU1... + 46"
}
```

### Mask (anonymize)
Configure anonymize for output like

```json
{
    "password": "*****"
}
```

for scalar values, and/or for objects / arrays all contained scalar values:

```json
{
    "credentials": {
        "username": "*****",
        "password": "*****"
    }
}
```

### Remove (prune) arrays or objects
Configure prune to turn input

```json
{
    "context": {
        "boringData": {
        ...
        },
        "staticData": [ ... ]
    }
}
```

to outputs like

```json
{
    "context": "PRUNED"
}
```

### Path syntax
A simple syntax is supported, where each path segment corresponds to a `field name`. Expressions are case-sensitive. Supported syntax:

    $.my.field.name
    
    or
    
    /my/field/name

with support for wildcards; 

    $.my.field.*
    
    or
    
    /my/field/*

or a simple any-level field name search 

    $..myFieldName
    
    or
    
    //myFieldName

The filters within this library support using multiple expressions at once.

### Max path matches
Configure max path matches; so that filtering stops after a number of matches. This means the __filter speed can be increased considerably if the number of matches is known to be a fixed number__; and will approach pass-through performance if those matches are in the beginning of the document.

For example if the to-be filtered JSON document has a schema definition with a header + body structure, and the target value is in the header.   

### Max size
Configure max size to limit the size of the resulting document. This reduces the size of the document by (silently) deleting the JSON content after the limit is reached.

### Metrics
Pass in a `JsonFilterMetrics` argument to the `process` method like so:

```
JsonFilterMetrics myMetrics = new DefaultJsonFilterMetrics();
String filtered = filter.process(json, myMetrics); // perform filtering
```

The resulting metrics could be logged as metadata alongside the JSON payload or passed to sensors like [Micrometer](https://micrometer.io/) for further processing.

## Performance
The `core` processors within this project are faster than the `Jackson`-based processors. This is expected as parser/serializer features have been traded for performance:

 * `core` is something like 3x-9x as fast as `Jackson` processors, where
 * skipping large parts of JSON documents (prune) decreases the difference, and
 * small documents increase the difference, as `Jackson` is more expensive to initialize.
 * working directly on bytes is faster than working on characters for the `core` processors.

For a typical, light-weight web service, the overall system performance improvement for using the `core` filters over the `Jackson`-based filters will most likely be a few percent.

Memory use will be at 2-8 times the raw JSON byte size; depending on the invoked `JsonFilter` method (some accept string, other raw bytes or chars).

See the benchmark results ([JDK 8](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/benchmark/jmh/results/jmh-results-3.0.1.jdk8.json&topBar=off), [JDK 11](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/benchmark/jmh/results/jmh-results-3.0.1.jdk11.json&topBar=off)) and the [JMH] module for running detailed benchmarks.

# See also
See the [xml-log-filter] for corresponding high-performance filtering of XML, and [JsonPath](https://github.com/json-path/JsonPath) for more advanced filtering.

Using SIMD for parsing JSON: 
 * [simdjson](https://github.com/simdjson/simdjson)
 * [sparser](https://blog.acolyer.org/2018/08/20/filter-before-you-parse-faster-analytics-on-raw-data-with-sparser/)
 
Alternative JSON filters:

 * [json-masker](https://github.com/Breus/json-masker) (included in benchmark).

[Apache 2.0]:			https://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:		https://github.com/skjolber/json-log-filter/issues
[Maven]:				https://maven.apache.org/
[JMH]:					benchmark/jmh
[xml-log-filter]:      	https://github.com/skjolber/xml-log-filter
[High-performance]:		https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/docs/benchmark/jmh-result.json&topBar=off
[Jackson]:				https://github.com/FasterXML/jackson-core
[JSON]:					https://www.json.org/json-en.html
