![Build Status](https://github.com/skjolber/json-log-filter/actions/workflows/maven.yml/badge.svg) 
[![Maven Central](https://img.shields.io/maven-central/v/com.github.skjolber.json-log-filter/parent.svg)](https://mvnrepository.com/artifact/com.github.skjolber.json-log-filter)
[![codecov](https://codecov.io/gh/skjolber/json-log-filter/graph/badge.svg?token=8mCiHxVFbz)](https://codecov.io/gh/skjolber/json-log-filter)

# json-log-filter
High-performance filtering of JSON. Reads, filters and writes JSON in a single pass — drastically increasing throughput compared to parse-then-serialize approaches.

## Quick Example

```java
// Create once, reuse freely — all filters are thread-safe
JsonFilter filter = DefaultJsonLogFilterBuilder.newBuilder()
    .withAnonymize("$.customer.email", "$.customer.ssn")  // replace with "*"
    .withPrune("$.internal.stackTrace")                   // replace with "PRUNED"
    .withMaxStringLength(256)                             // truncate long strings
    .withMaxSize(128 * 1024)                              // drop content beyond 128 KB
    .build();

String filtered = filter.process(inputJson);
```

Input:
```json
{
  "customer": { "name": "Alice", "email": "alice@example.com", "ssn": "123-45-6789" },
  "internal": { "stackTrace": "java.lang.Exception: ...(very long)..." }
}
```

Output:
```json
{
  "customer": { "name": "Alice", "email": "*", "ssn": "*" },
  "internal": { "stackTrace": "PRUNED" }
}
```

## Why json-log-filter?

| Feature | json-log-filter | Jackson + Manual | Regex |
|---|---|---|---|
| **Performance** | 🏎️ Single-pass, no object model | 🐢 Full parse + serialize | 🐌 No structure awareness |
| **Zero Dependencies** | ✅ (core) | ❌ | ✅ |
| **JSONPath Support** | ✅ | ⚠️ Extra lib | ❌ |
| **Multiple paths at once** | ✅ | ❌ Manual | ❌ |
| **Prune whole subtrees** | ✅ | ❌ Manual | ❌ |
| **Max document size** | ✅ | ❌ | ❌ |
| **Configurable output text** | ✅ | ❌ Manual | ❌ |

Typical use-cases:
- **Log sanitization**: strip passwords, tokens, PII before writing to logs
- **GDPR compliance**: anonymize personal data in request/response logging
- **Log readability**: prune large base64 blobs or low-value subtrees
- **Log size control**: stay within GCP (256 KB) or Azure (64 KB) limits

The library selects the most efficient filter implementation for the configured combination of features automatically. No external dependencies are required for the `core` module.

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
    <artifactId>api</artifactId>
    <version>${json-log-filter.version}</version>
</dependency>
<dependency>
    <groupId>com.github.skjolber.json-log-filter</groupId>
    <artifactId>core</artifactId>
    <version>${json-log-filter.version}</version>
</dependency>
```

and optionally

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
api("com.github.skjolber.json-log-filter:api:${jsonLogFilterVersion}")
api("com.github.skjolber.json-log-filter:core:${jsonLogFilterVersion}")
```

and optionally

```groovy
api("com.github.skjolber.json-log-filter:jackson:${jsonLogFilterVersion}")
```
</details>

# Usage

Use `DefaultJsonLogFilterBuilder.newBuilder()` to configure a filter. All produced filters are thread-safe and should be created once and reused.

```java
JsonFilter filter = DefaultJsonLogFilterBuilder.newBuilder()
    .withMaxStringLength(127)          // truncate long string values
    .withAnonymize("$.customer.email") // replace with "*"
    .withPrune("$.customer.account")   // replace whole subtree with "PRUNED"
    .withMaxSize(128 * 1024)           // limit output to 128 KB
    .build();

String filtered = filter.process(inputJson);
```

## Adding multiple path filters

Multiple paths can be specified in a single call or across several calls — both are equivalent:

```java
// All in one call (varargs)
filter = DefaultJsonLogFilterBuilder.newBuilder()
    .withAnonymize("$.customer.email", "$.customer.phone", "$.customer.ssn")
    .withPrune("$.internal.debug", "$.internal.stackTrace")
    .build();

// Chained calls
filter = DefaultJsonLogFilterBuilder.newBuilder()
    .withAnonymize("$.customer.email")
    .withAnonymize("$.customer.phone")
    .withAnonymize("$.customer.ssn")
    .withPrune("$.internal.debug")
    .withPrune("$.internal.stackTrace")
    .build();

// From a collection (List, Set, or any Collection<String>)
List<String> sensitiveFields = loadSensitiveFieldsFromConfig();
filter = DefaultJsonLogFilterBuilder.newBuilder()
    .withAnonymize(sensitiveFields)
    .build();
```

## Anonymize (mask values)

`withAnonymize(...)` replaces matching scalar values with `"*"`. For objects and arrays, all nested scalars are replaced recursively.

Input:
```json
{
  "username": "alice",
  "password": "s3cr3t",
  "credentials": { "token": "abc123", "key": "xyz789" }
}
```

After `.withAnonymize("$.password", "$.credentials")`:
```json
{
  "username": "alice",
  "password": "*",
  "credentials": { "token": "*", "key": "*" }
}
```

## Prune (remove subtrees)

`withPrune(...)` removes entire values — scalars, objects, or arrays — and replaces them with `"PRUNED"`.

Input:
```json
{
  "header": { "requestId": "abc-123" },
  "context": {
    "boringData": { "flag1": true, "flag2": false },
    "staticData": [1, 2, 3, 4, 5]
  }
}
```

After `.withPrune("$.context")`:
```json
{
  "header": { "requestId": "abc-123" },
  "context": "PRUNED"
}
```

## Truncate long strings

`withMaxStringLength(n)` cuts string values that exceed `n` characters and appends a suffix indicating how many characters were omitted.

Input:
```json
{ "icon": "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5eg==" }
```

After `.withMaxStringLength(32)`:
```json
{ "icon": "QUJDREVGR0hJSktMTU5PUFFSU1RVVl... + 46" }
```

## Customizing output text

The replacement text for all three operations can be overridden:

```java
JsonFilter filter = DefaultJsonLogFilterBuilder.newBuilder()
    .withAnonymize("$.password")
    .withPrune("$.debugContext")
    .withAnonymizeMessage("[redacted]")   // default: "*"
    .withPruneMessage("[removed]")        // default: "PRUNED"
    .withTruncateMessage("…")             // default: "... + <count>"
    .build();
```

Output:
```json
{
  "password": "[redacted]",
  "debugContext": "[removed]"
}
```

## Path syntax

A simple syntax is supported where each path segment corresponds to a field name. Expressions are case-sensitive.

| Syntax | Description | Example |
|---|---|---|
| `$.a.b.c` | Exact path | `$.customer.email` |
| `$.a.b.*` | Wildcard (any field at this level) | `$.customer.*` |
| `$..fieldName` | Any-level search (all depths) | `$..password` |

> [!NOTE]
> Path expressions pass through arrays transparently — `$.items.price` matches `price` inside each element of the `items` array.

## Max path matches

`withMaxPathMatches(n)` stops path-based filtering after `n` matches. When the target field appears a known fixed number of times near the start of the document, this lets the filter skip the rest at near pass-through speed.

```java
// Stop after finding the single "requestId" field in the header
filter = DefaultJsonLogFilterBuilder.newBuilder()
    .withAnonymize("$.header.requestId")
    .withMaxPathMatches(1)
    .build();
```

## Max size

`withMaxSize(bytes)` limits the size of the output document. Content beyond the limit is dropped.

## Metrics

Pass a `JsonFilterMetrics` to `process` to measure filtering impact:

```java
JsonFilterMetrics metrics = new DefaultJsonFilterMetrics();
String filtered = filter.process(inputJson, metrics);
// log or forward metrics.getAnonymizeCount(), metrics.getPruneCount(), etc.
```

Useful for:
- Verifying filters are actually matching fields
- Measuring data reduction sent to log aggregators
- Feeding [Micrometer](https://micrometer.io/) counters

## Request/response path module
See the opt-in [path](impl/path) module for per-path filter selection in request/response-logging pipelines, for further performance gains.

## [Jackson] module

For **untrusted or externally produced JSON**, use `JacksonJsonLogFilterBuilder` instead. It validates document structure during filtering at the cost of some throughput.

```java
JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
    .withAnonymize("$.customer.email", "$.customer.ssn")
    .withPrune("$.internal.debug")
    .withAnonymizeMessage("[redacted]")
    .build();
```

Typical dual-filter setup:
- Locally produced JSON → `DefaultJsonLogFilterBuilder` (fast, no validation)
- Externally received JSON → `JacksonJsonLogFilterBuilder` (validates structure)

## Performance
This project trades parser/serializer features for performance, running multiple times faster than a traditional parse-then-serialize approach.

See the benchmark results ([JDK 25](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/benchmark/jmh/results/jmh-results-5.0.0.jdk25.json&topBar=off)) and the [JMH] module for running detailed benchmarks.

# See also
See the [xml-log-filter] for corresponding high-performance filtering of XML, and [JsonPath](https://github.com/json-path/JsonPath) for more advanced filtering.

Using SIMD for parsing JSON: 
 * [simdjson](https://github.com/simdjson/simdjson)
 * [sparser](https://blog.acolyer.org/2018/08/20/filter-before-you-parse-faster-analytics-on-raw-data-with-sparser/)
 
Alternative JSON filters:

 * [json-masker](https://github.com/Breus/json-masker) slightly different use-case, included in some of the benchmarks.

[Apache 2.0]:			https://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:		https://github.com/skjolber/json-log-filter/issues
[Maven]:				https://maven.apache.org/
[JMH]:					benchmark/jmh
[xml-log-filter]:      	https://github.com/skjolber/xml-log-filter
[High-performance]:		https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/docs/benchmark/jmh-result.json&topBar=off
[Jackson]:				https://github.com/FasterXML/jackson-core
[JSON]:					https://www.json.org/json-en.html
