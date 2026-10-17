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
| `cve.CveMaxSizeFromEndFilterBenchmark` | The backward max-size scan and the default composite max-size filter |
| `cve.CveProcessStringFilterBenchmark`, `entur.EnturProcessStringFilterBenchmark` | `process(String)` with filters which change every document and with filters which leave them as they are |
| `cve.CveProcessStringMaxSizeFilterBenchmark` | `process(String)` with the max-size filters, for documents which fit and documents which are cut |
| `cve.CveMetricsFilterBenchmark` | The metrics variants, `process(String, metrics)` and `process(byte[], metrics)` |
| `cve.CvePrettyPrintedRemoveWhitespaceBenchmark` | Whitespace removal on pretty-printed documents; the other CVE benchmarks run on compact ones |
| `cve.CvePrettyPrintedPathFilterBenchmark` | The path filters on pretty-printed documents, anonymizing a whole object |
| `cve.CveUnchangedOutputFilterBenchmark` | Filters with nothing to do: whitespace removal on compacted documents, max size on documents which fit |
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

## Automated performance testing

For agents measuring and improving performance, see `jit/README.md`.

- **Building the benchmark jar:** use `mvn -Pquick package -pl benchmark/jmh -am`.
  - The `quick` profile skips javadoc, source jars, module descriptors, coverage, the enforcer and test compilation, none of which the benchmark jar needs.
  - Never run `mvn install`.
  - **mvnd is faster still:** the Maven Daemon (`mvnd`, if installed) stays warm between builds. `jit/ab.sh` uses it with `MVN=mvnd`, with one daemon per CPU set.
  - **Measured on 2 real cores:** a full build takes 15.7 s and `-Pquick` 7.6 s from clean. Rebuilding after a change to one core file takes 7.1 s with `mvn` and 3.4 s with a warm `mvnd`.
- **A/B comparisons:** `jit/ab.sh` builds a base commit and the working tree, and runs interleaved forks on JDK 27. Don't run single forks: hot loops move 10–30% between builds on small changes.
  - **New benchmarks on old commits:** a benchmark class added to the working tree is copied into the base build, so it can measure commits from before it existed.
  - **Fork JVM:** forks get a fixed, pre-touched heap (`JVM_ARGS`, default `-Xms1g -Xmx1g -XX:+AlwaysPreTouch`), so they don't differ in heap growth. Pass the same `-jvmArgsAppend` to manual runs you want to compare.
- **JIT snapshots:** `jit/probe.sh` records C2 compilations, inlining decisions, deoptimizations and allocation sites, for diffing two builds.
- **Machine:**
  - **Cores:** use at most 4 of the 16 real cores at a time unless permitted. Pin builds and benchmarks with `taskset`, or `CPUS=...` for the scripts, e.g. `CPUS=0,1,16,17`. A core and its SMT sibling are one real core.
  - **Quiet machine:** nothing else should build or benchmark on the same cores during a measurement.

## Notes

- Requires `--add-modules jdk.incubator.vector` (already set in the surefire config).
- Publishing to Maven Central is skipped (`skipPublishing=true`).
- Do not add production logic here; this module is benchmarks only.
