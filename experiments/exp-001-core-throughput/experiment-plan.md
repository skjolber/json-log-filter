# Experiment Plan

## Experiment ID
exp-001-core-throughput

## Optimization Goal
Improve throughput of the core JSON filter implementations, targeting JDK 25 optimizations.

## Scope
`impl/core/src/main/java/**`

## Metric
- Name: Throughput (ops/sec)
- Direction: higher is better

## Success Criteria (BINDING — the loop does not start without these numbers)
- Target: +10% overall throughput improvement over baseline
- Iteration threshold: minimum +1% improvement to keep a change

## Regression Test Command
`mvn -pl impl/core test -q --no-transfer-progress`

## Benchmark Test Command
`bash experiments/exp-001-core-throughput/bench/run.sh 2>&1 | tail -1`

## Iterations
Run until target is reached or manually stopped

## Strategies to Explore
- (populated during Phase 2 loop)

## Created
2026-04-05
