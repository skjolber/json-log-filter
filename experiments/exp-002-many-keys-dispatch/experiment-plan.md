# Experiment Plan

## Experiment ID
exp-002-many-keys-dispatch

## Optimization Goal
Evaluate and improve throughput of key-name matching in JSON path filters
(AnyPathFilters, MultiPathItem, StarMultiPathItem) when filtering with many
(1–20) path expressions simultaneously on the `optimizeLengthCheck` branch
compared to master.

## Scope
- `base/src/main/java/com/github/skjolber/jsonfilter/base/path/`
- `impl/core/src/main/java/com/github/skjolber/jsonfilter/core/`
- `benchmark/jmh/src/main/java/com/github/skjolber/jsonfilter/jmh/path/`
- Excluded: `experiments/`, `*/test/`

## Metric
- Name: Combined key-matching throughput (ops/sec)
- Direction: higher is better
- Measurement: median of 5 JMH runs; combined average across AnyPathFiltersBenchmark,
  MultiPathBenchmark, and StarMultiPathBenchmark at key counts 1, 5, 10, 15, 20.

## Success Criteria (BINDING)
- Target: run 5 iterations, keep changes that improve throughput
- Iteration threshold: any positive improvement over the previous best

## Regression Test Command
`JAVA_HOME=/usr/lib/jvm/java-25-openjdk-oracle mvn test -pl base,impl/core -q`

## Benchmark Test Command
`bash experiments/exp-002-many-keys-dispatch/bench/run.sh`

## Iterations
5

## Strategies to Explore
- Extract duplicate dispatch-table builders to a shared utility (code quality + JIT-friendliness)
- Increase dispatch table granularity (first two bytes instead of one)
- Length pre-filter before first-byte check in MultiPathItem / StarMultiPathItem
- Avoid boxing/unboxing in dispatch table hot path
- Cache-friendly layout: interleave length + name bytes

## Created
2026-04-06
