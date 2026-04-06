#!/usr/bin/env bash
# Runs AnyPathFiltersBenchmark, MultiPathBenchmark, and StarMultiPathBenchmark
# for count=1,5,10,15,20 and outputs the combined average ops/sec.
# Usage: bash experiments/exp-002-many-keys-dispatch/bench/run.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
JMH_DIR="$REPO_ROOT/benchmark/jmh"
JAR="$JMH_DIR/target/benchmarks.jar"

if [ ! -f "$JAR" ]; then
  echo "Building benchmarks jar..." >&2
  (cd "$REPO_ROOT" && JAVA_HOME=/usr/lib/jvm/java-25-openjdk-oracle \
    mvn -pl benchmark/jmh package -am -DskipTests -q \
    --no-transfer-progress -Dmoditect.skip=true -Djacoco.skip=true \
    -Dmaven.javadoc.skip=true) >&2
fi

RESULT_JSON="$JMH_DIR/exp002_results.json"

cd "$JMH_DIR"

echo "Running AnyPathFiltersBenchmark, MultiPathBenchmark, StarMultiPathBenchmark..." >&2
java -jar "$JAR" \
  "AnyPathFiltersBenchmark\.(chars|bytes)|MultiPathBenchmark\.(chars|bytes)|StarMultiPathBenchmark\.(chars|bytes)" \
  -p count=1,5,10,15,20 \
  -f 1 -wi 3 -i 5 \
  -rf json -rff "$RESULT_JSON" >/dev/null 2>&1

python3 -c "
import json

with open('$RESULT_JSON') as fp:
    data = json.load(fp)

scores = [r['primaryMetric']['score'] for r in data]
combined = sum(scores) / len(scores)
print(f'Combined average: {combined:.0f} ops/sec ({len(scores)} variants)', file=__import__('sys').stderr)
print(f'{combined:.0f}')
"
