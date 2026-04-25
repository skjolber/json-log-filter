# json-log-filter

High-performance JSON filtering for logging. Reads, filters, and writes JSON in a single pass — no intermediate object model.

## Repository layout

```
api/                  Public interfaces (JsonFilter, JsonFilterFactory, …)
base/                 Abstract base classes and builder DSL
impl/
  core/               Zero-dependency single-pass filters (trusted JSON)
  path/               JSONPath-aware request/response filter
frameworks/
  jackson/            Jackson-based filters (untrusted / externally sourced JSON)
support/
  test/               Shared test utilities used by all modules
benchmark/
  jmh/                JMH micro-benchmarks
```

Each directory contains its own `AGENTS.md` with module-specific details.

## Prerequisites

- Java 17+ (CI runs Java 25 / Temurin)
- Maven 3.6.3+

## Build

```bash
# compile, test, and verify all modules
mvn verify --no-transfer-progress

# tests only (faster)
mvn test --no-transfer-progress

# single module (example)
mvn test -pl impl/core --no-transfer-progress
```

## Module dependency order

```
api  →  base  →  core  ─┐
                jackson  ├→  path
                         └→  jmh
support/test  (test-scoped by core, jackson, path)
```

## Key entry points

| Use case | Class |
|---|---|
| Trusted JSON (no Jackson) | `DefaultJsonLogFilterBuilder` in `impl/core` |
| Untrusted / external JSON | `JacksonJsonLogFilterBuilder` in `frameworks/jackson` |
| Request/response pair | `RequestResponseJsonFilterFactory` in `impl/path` |

## Quickstart

```java
// All filters are thread-safe — create once, reuse everywhere
JsonFilter filter = DefaultJsonLogFilterBuilder.anonymizeKeys("ssn", "token");
String filtered = filter.process(inputJson);
```

## Testing conventions

- JUnit 5 with Google Truth assertions
- Directory-based tests: `AbstractJsonFilterDirectoryUnitTest` (see `support/test`)
- JaCoCo enforces ≥ 80% instruction coverage on `impl/core`
- Run benchmarks via the shaded jar in `benchmark/jmh/target/benchmarks.jar`

## License

Apache 2.0
