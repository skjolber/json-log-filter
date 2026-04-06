#!/usr/bin/env bash
# Runs CVE, FDA adverse events, and USGS earthquake benchmarks on the current build.
# Outputs per-dataset average ops/sec and a combined average.
# Usage: bash experiments/exp-001-core-throughput/bench/compare-datasets.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
JMH_DIR="$REPO_ROOT/benchmark/jmh"
JAR="$JMH_DIR/target/benchmarks.jar"

if [ ! -f "$JAR" ]; then
  echo "Building benchmarks jar..." >&2
  (cd "$REPO_ROOT" && mvn -pl benchmark/jmh package -am -DskipTests -q \
    --no-transfer-progress -Dmoditect.skip=true -Djacoco.skip=true \
    -Dmaven.javadoc.skip=true) >&2
fi

CVE_JSON="$JMH_DIR/compare_cve.json"
FDA_JSON="$JMH_DIR/compare_fda.json"
USGS_JSON="$JMH_DIR/compare_usgs.json"
SHOPIFY_JSON="$JMH_DIR/compare_shopify.json"
ENTUR_JSON="$JMH_DIR/compare_entur.json"

cd "$JMH_DIR"

echo "Running CVE benchmark..." >&2
java --add-modules jdk.incubator.vector \
  -jar "$JAR" \
  "CveFilterBenchmark\.(all_core|anon_any_core|anon_full_core|maxSize_core|maxStringLength_core|maxStringLengthMaxSize_core|core_remove_whitespace)" \
  -p fileName=8KB,22KB -f 1 -wi 3 -i 3 \
  -rf json -rff "$CVE_JSON" >/dev/null 2>&1

echo "Running FDA adverse events benchmark..." >&2
java --add-modules jdk.incubator.vector \
  -jar "$JAR" \
  "FdaFilterBenchmark\.(all_core|anon_any_core|anon_full_core|maxSize_core|maxStringLength_core|maxStringLengthMaxSize_core|core_remove_whitespace)" \
  -p fileName=4KB,29KB -f 1 -wi 3 -i 3 \
  -rf json -rff "$FDA_JSON" >/dev/null 2>&1

echo "Running USGS earthquake benchmark..." >&2
java --add-modules jdk.incubator.vector \
  -jar "$JAR" \
  "UsgsFilterBenchmark\.(all_core|anon_any_core|anon_full_core|maxSize_core|maxStringLength_core|maxStringLengthMaxSize_core|core_remove_whitespace)" \
  -p fileName=12KB,200KB -f 1 -wi 3 -i 3 \
  -rf json -rff "$USGS_JSON" >/dev/null 2>&1

echo "Running Shopify orders benchmark..." >&2
java --add-modules jdk.incubator.vector \
  -jar "$JAR" \
  "ShopifyFilterBenchmark\.(all_core|anon_any_core|anon_full_core|maxSize_core|maxStringLength_core|maxStringLengthMaxSize_core|core_remove_whitespace)" \
  -p fileName=9KB,19KB -f 1 -wi 3 -i 3 \
  -rf json -rff "$SHOPIFY_JSON" >/dev/null 2>&1

echo "Running Entur public transport benchmark..." >&2
java --add-modules jdk.incubator.vector \
  -jar "$JAR" \
  "EnturFilterBenchmark\.(all_core|anon_any_core|anon_full_core|maxSize_core|maxStringLength_core|maxStringLengthMaxSize_core|core_remove_whitespace)" \
  -p fileName=6KB,20KB -f 1 -wi 3 -i 3 \
  -rf json -rff "$ENTUR_JSON" >/dev/null 2>&1

python3 -c "
import json

def avg(f):
    with open(f) as fp: d = json.load(fp)
    scores = [r['primaryMetric']['score'] for r in d]
    return sum(scores)/len(scores), len(scores)

cve_avg, cve_n = avg('$CVE_JSON')
fda_avg, fda_n = avg('$FDA_JSON')
usgs_avg, usgs_n = avg('$USGS_JSON')
shopify_avg, shopify_n = avg('$SHOPIFY_JSON')
entur_avg, entur_n = avg('$ENTUR_JSON')
combined = (cve_avg + fda_avg + usgs_avg + shopify_avg + entur_avg) / 5
print(f'CVE benchmark:         {cve_avg:.0f} ops/sec ({cve_n} variants)')
print(f'FDA benchmark:         {fda_avg:.0f} ops/sec ({fda_n} variants)')
print(f'USGS benchmark:        {usgs_avg:.0f} ops/sec ({usgs_n} variants)')
print(f'Shopify benchmark:     {shopify_avg:.0f} ops/sec ({shopify_n} variants)')
print(f'Entur benchmark:       {entur_avg:.0f} ops/sec ({entur_n} variants)')
print(f'Combined average:      {combined:.0f} ops/sec')
print(f'{combined:.0f}')
"
