![Build Status](https://github.com/skjolber/json-log-filter/actions/workflows/maven.yml/badge.svg) 
[![Maven Central](https://img.shields.io/maven-central/v/com.github.skjolber.json-log-filter/parent.svg)](https://mvnrepository.com/artifact/com.github.skjolber.json-log-filter)
[![codecov](https://codecov.io/gh/skjolber/json-log-filter/graph/badge.svg?token=8mCiHxVFbz)](https://codecov.io/gh/skjolber/json-log-filter)

# json-log-filter
High-performance filtering of JSON. Reads, filters and writes JSON in a single pass — drastically increasing throughput compared to parse-then-serialize approaches.

## Quickstart

```java
// One-liner — all filters are thread-safe, create once and reuse
JsonFilter filter = DefaultJsonLogFilterBuilder.anonymizeKeys("password", "ssn", "token");

String filtered = filter.process(inputJson);
```

Input:

```json
{
  "user": {
    "type": "member",
    "ssn": "123-45-6789"
  },
  "auth": {
    "token": "eyJhbGci..."
  }
}
```

Output:

```json
{
  "user": {
    "type": "member",
    "ssn": "*"
  },
  "auth": {
    "token": "*"
  }
}
```

> [!TIP]
> Use [`newBuilder()`](#builder--multiple-filters-and-extra-options) when you need to combine filter types or set extra options.

## Why json-log-filter?

| Feature | **json-log-filter core** (trusted JSON) | **json-log-filter jackson** (untrusted JSON) | Jackson + Manual | Regex |
|---|---|---|---|---|
| **Performance** | 🏎️ Single-pass, no object model | 🐢 Full parse + serialize | 🐢 Full parse + serialize | 🐌 No structure awareness |
| **Zero Dependencies** | ✅ | ❌ (Jackson) | ❌ (Jackson) | ✅ |
| **JSONPath Support** | ✅ subset¹ | ✅ subset¹ | ⚠️ Extra lib | ❌ |
| **Prune whole subtrees** | ✅ | ✅ | ❌ Manual | ❌ |
| **Max string length** | ✅ | ✅ | ❌ Manual | ❌ |
| **Max document size** | ✅ | ✅ | ❌ Manual | ❌ Manual |
| **Configurable output text** | ✅ | ✅ | ❌ Manual | ❌ Manual |
| **Structural validation** | ❌ (trusted input) | ✅ | ✅ | ❌ |

> ¹ Supported path syntax: `$.a.b.c` (exact), `$.a.b.*` / `$.a.*.c` (wildcard), `$..c` (`c` at any depth).

Typical use-cases:
- **Log sanitization**: strip passwords, tokens, and PII before writing to logs
- **GDPR compliance**: anonymize personal data in request/response logging
- **Log readability**: prune large base64 blobs or low-value subtrees
- **Log size control**: stay within GCP (256 KB) or Azure (64 KB) per-entry limits
- **Safe structured logging**: selectively apply core/jacksons filters (to trusted/untrusted JSON) so to always output valid JSON. Then inline filtered JSON directly into a JSON log line — log parsers and ingestors never choke on it

The library automatically selects the most efficient filter implementation for the configured combination of features. 

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

## One-liner factory methods

For simple cases, create a ready-to-use filter in a single call.

Use **varargs** when you know the field names up front:

```java
// Anonymize or prune fields by name at any depth
JsonFilter f = DefaultJsonLogFilterBuilder.anonymizeKeys("password", "ssn", "token");
JsonFilter f = DefaultJsonLogFilterBuilder.pruneKeys("appMeta", "diagnostics");

// By precise JSONPath
JsonFilter f = DefaultJsonLogFilterBuilder.anonymizePaths("$.customer.email", "$..token");
JsonFilter f = DefaultJsonLogFilterBuilder.prunePaths("$.context.appMeta");
```

Use **`Set.of(...)`** when you also need `maxStringLength` or `maxSize`:

```java
// Anonymize by name — with optional size limits
JsonFilter f = DefaultJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"));
JsonFilter f = DefaultJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"), 256);              // truncate strings > 256 chars
JsonFilter f = DefaultJsonLogFilterBuilder.anonymizeKeys(Set.of("password", "ssn"), 256, 128 * 1024);  // + cap output at 128 KB

// Prune whole subtrees — with optional size limits
JsonFilter f = DefaultJsonLogFilterBuilder.pruneKeys(Set.of("appMeta", "diagnostics"));
JsonFilter f = DefaultJsonLogFilterBuilder.pruneKeys(Set.of("appMeta"), 256, 128 * 1024);

// By JSONPath — with optional size limits
JsonFilter f = DefaultJsonLogFilterBuilder.anonymizePaths(Set.of("$.customer.email"), 256);
JsonFilter f = DefaultJsonLogFilterBuilder.prunePaths(Set.of("$.context.appMeta"), 256, 128 * 1024);
```

The same one-liners are available on `JacksonJsonLogFilterBuilder` for untrusted JSON — see the [Jackson module](#jackson-module) section.

## Builder — multiple filters and extra options

Use `newBuilder()` when you need to combine filter types, customize output text, or set size limits:

```java
JsonFilter filter = DefaultJsonLogFilterBuilder.newBuilder()
    .withAnonymizeKeys("password", "ssn")         // any depth, by field name
    .withAnonymizePaths("$.customer.email")       // precise path
    .withPrunePaths("$.context.rawPayload")       // remove whole subtree
    .withAnonymizeMessage("[redacted]")           // custom replacement text
    .withMaxStringLength(256)                     // truncate long strings
    .withMaxSize(128 * 1024)                      // limit output to 128 KB
    .build();

String filtered = filter.process(inputJson);
```

## Keys vs paths

There are two ways to target fields:

| Method | Input | Matches |
|---|---|---|
| `withAnonymizeKeys("password")` | bare field name | `password` at **any depth** |
| `withAnonymizePaths("$.user.password")` | JSONPath expression | `password` only at that **exact path** |
| `withPruneKeys("rawPayload")` | bare field name | `rawPayload` at **any depth** |
| `withPrunePaths("$.context.rawPayload")` | JSONPath expression | only at that **exact path** |

`withAnonymizeKeys("password")` is equivalent to `withAnonymizePaths("$..password")`.

## Adding multiple path filters

Multiple paths can be provided in one call, across several calls, or from a collection:

```java
// All in one call (varargs)
builder.withAnonymizeKeys("password", "ssn", "token")
       .withPrunePaths("$.context.diagnostics", "$.internal.trace");

// From a collection (List, Set, or any Collection<String>)
Set<String> sensitiveKeys = loadSensitiveKeysFromConfig();
builder.withAnonymizeKeys(sensitiveKeys);
```

## Anonymize (mask values)

`withAnonymizeKeys(...)` and `withAnonymizePaths(...)` replace matching scalar values with `"*"`. For objects and arrays, all nested scalars are replaced recursively.

Input:
```json
{
  "username": "alice",
  "password": "s3cr3t",
  "credentials": {
    "token": "abc123",
    "key": "xyz789"
  }
}
```

After `.withAnonymizeKeys("password", "credentials")`:
```json
{
  "username": "alice",
  "password": "*",
  "credentials": {
    "token": "*",
    "key": "*"
  }
}
```

## Prune (remove subtrees)

`withPruneKeys(...)` and `withPrunePaths(...)` remove entire values — scalars, objects, or arrays — replacing them with `"[removed]"`. The whole subtree is gone in a single replacement.

A common use-case is pruning fields that are always the same across requests (e.g. static application metadata) so they don't waste log space on every request:

Input:
```json
{
  "request": {
    "method": "POST",
    "path": "/api/orders"
  },
  "appMeta": {
    "version": "2.3.1",
    "region": "eu-west-1",
    "buildTime": "2026-01-15T08:30:00Z",
    "dependencies": ["service-A", "service-B", "service-C"]
  }
}
```

After `.withPruneKeys("appMeta")`:
```json
{
  "request": {
    "method": "POST",
    "path": "/api/orders"
  },
  "appMeta": "[removed]"
}
```

The entire `appMeta` object — including its nested array — is replaced by a single string token.

## Truncate long strings

`withMaxStringLength(n)` cuts string values that exceed `n` characters and appends a suffix indicating how many characters were omitted.

Input:
```json
{
  "icon": "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5eg=="
}
```

After `.withMaxStringLength(32)`:
```json
{
  "icon": "QUJDREVGR0hJSktMTU5PUFFSU1RVVl... + 46"
}
```

## Customizing output text

The replacement text for all three operations can be overridden:

```java
JsonFilter filter = DefaultJsonLogFilterBuilder.newBuilder()
    .withAnonymizeKeys("password")
    .withPruneKeys("debugContext")
    .withAnonymizeMessage("[redacted]")   // default: "*"
    .withPruneMessage("[skipped]")        // default: "[removed]"
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
// Stop after finding the single "jwt" field
filter = DefaultJsonLogFilterBuilder.newBuilder()
    .withAnonymizeKeys("jwt")
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

Useful for measuring data reduction;
- Flagging that data has been removed
- Feeding [Micrometer](https://micrometer.io/) counters

## Request/response path module
See the opt-in [path](impl/path) module for per-path filter selection in request/response-logging pipelines, for further performance gains.

## [Jackson] module

For **untrusted or externally produced JSON**, use `JacksonJsonLogFilterBuilder` instead. It validates document structure during filtering at the cost of some throughput.

```java
JsonFilter filter = JacksonJsonLogFilterBuilder.newBuilder()
    .withAnonymizeKeys("password", "ssn")
    .withPrunePaths("$.internal.debug")
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

[Apache 2.0]:						https://www.apache.org/licenses/LICENSE-2.0.html
[issue-tracker]:				https://github.com/skjolber/json-log-filter/issues
[Maven]:								https://maven.apache.org/
[JMH]:									benchmark/jmh
[xml-log-filter]:				https://github.com/skjolber/xml-log-filter
[High-performance]:			https://jmh.morethan.io/?source=https://raw.githubusercontent.com/skjolber/json-log-filter/master/docs/benchmark/jmh-result.json&topBar=off
[Jackson]:							https://github.com/FasterXML/jackson-core
[JSON]:									https://www.json.org/json-en.html
