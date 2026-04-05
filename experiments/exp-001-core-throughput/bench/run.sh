#!/usr/bin/env bash
# Benchmark script for exp-001-core-throughput
# Runs JMH on the core filters, outputs a single throughput number (ops/sec) to stdout.
# Usage: bash experiments/exp-001-core-throughput/bench/run.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
JMH_DIR="$REPO_ROOT/benchmark/jmh"
JAR="$JMH_DIR/target/benchmarks.jar"

# Rebuild if needed
if [ ! -f "$JAR" ]; then
  echo "Building benchmarks jar..." >&2
  (cd "$REPO_ROOT" && mvn -pl benchmark/jmh package -am -DskipTests -q --no-transfer-progress) >&2
fi

RESULT_JSON=$(mktemp /tmp/jmh-result-XXXXXX.json)
trap "rm -f $RESULT_JSON" EXIT

# Run CveFilterBenchmark for core-only benchmarks (must run from jmh dir — test resources use relative paths)
# -f 1: 1 fork, -wi 3: 3 warmup iters x 1s, -i 3: 3 measurement iters x 1s
# Restrict to a stable, representative subset of file sizes for speed
# Pattern matches: all_core, anon_any_core, anon_full_core, maxSize_core, maxStringLength_core, etc.
cd "$JMH_DIR"
java --add-modules jdk.incubator.vector \
  -jar "$JAR" \
  "CveFilterBenchmark\.(all_core|anon_any_core|anon_full_core|maxSize_core|maxStringLength_core|maxStringLengthMaxSize_core|core_remove_whitespace)" \
  -p fileName=8KB,22KB \
  -f 1 -wi 3 -i 3 \
  -rf json -rff "$RESULT_JSON" \
  >/dev/null 2>&1

# Extract average score across all benchmarks
python3 -c "
import json, sys
with open('$RESULT_JSON') as f:
    data = json.load(f)
scores = [r['primaryMetric']['score'] for r in data]
avg = sum(scores) / len(scores)
print(f'{avg:.0f}')
"
