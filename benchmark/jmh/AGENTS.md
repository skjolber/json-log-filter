# Module: benchmark/jmh

JMH micro-benchmarks for json-log-filter.

## Purpose

Measures throughput and latency of all filter implementations under realistic workloads. Used to validate that performance improvements in `impl/core` and `frameworks/jackson` are real, and to catch regressions.

Compared implementations include:
- `impl/core` filters (trusted JSON, zero dependencies)
- `frameworks/jackson` filters (untrusted JSON, Jackson tokenizer)
- `json-masker` (third-party reference implementation)
- `simdjson-java` (SIMD-accelerated reference)

## Key types

| Type | Description |
|---|---|
| `AllFilterBenchmark` | Runs all filter types against standard payloads |
| `AbstractMaxStringLengthFilterBenchmark` | Base for string-length benchmarks |
| `AbstractPathMaxStringLengthFilterBenchmark` | Base for path + string-length benchmarks |
| `AbstractMaxSizeFilterBenchmark` | Base for max-size truncation benchmarks |
| `RemoveWhitespaceBenchmark` | Whitespace stripping throughput |
| `CveFilterBenchmark` | Filters against CVE-corpus payloads |
| `BenchmarkRunner` / `JacksonBenchmarkRunner` | Main entry points for ad-hoc runs |

Payload directories: `entur/`, `fda/`, `shopify/`, `usgs/`, `cve/`

## Build & run

```bash
# Build the benchmark uberjar (from repo root)
mvn package -pl benchmark/jmh -am -DskipTests

# Run all benchmarks
java -jar benchmark/jmh/target/benchmarks.jar

# Run a specific benchmark
java -jar benchmark/jmh/target/benchmarks.jar AllFilterBenchmark -wi 3 -i 5
```

## Notes

- Requires `--add-modules jdk.incubator.vector` (already set in the surefire config).
- Publishing to Maven Central is skipped (`skipPublishing=true`).
- Do not add production logic here; this module is benchmarks only.
