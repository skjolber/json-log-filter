#!/bin/bash
# JIT snapshots on JDK 27, see PLAN.md and JitProbe.java.
#
#   [SECONDS_PER_CONFIG=6] ./probe.sh <jar> <out dir> <config>[-chars|-bytes]...
#   ./probe.sh benchmark/jmh/target/jit/jars/base-f74d151a.jar /tmp/base maxSizeFromEnd-bytes maxStringLengthMaxSize-chars
#
# The jar is a benchmarks.jar from ab.sh (it holds the library and its dependencies). Writes <out dir>/<config>.txt,
# which can be diffed against another build's: diff -u base/<config>.txt candidate/<config>.txt
# CPUS=<list> pins the probe to these CPUs (taskset), for parallel tracks. JVM_ARGS replaces the default
# "-Xms1g -Xmx1g -XX:+AlwaysPreTouch", the same as ab.sh's forks.
set -euo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO=$(git -C "$HERE" rev-parse --show-toplevel)
JAVA=${JAVA:-/usr/lib/jvm/temurin-27-jdk/bin/java}
DOCS=${DOCS:-$REPO/benchmark/jmh/src/test/resources/benchmark/cves/22KB}
SECONDS_PER_CONFIG=${SECONDS_PER_CONFIG:-6}
# hsdis, for -prof perfasm and -XX:+PrintAssembly: HotSpot loads hsdis-amd64.so from LD_LIBRARY_PATH
HSDIS_DIR=${HSDIS_DIR:-$HOME/.local/share/hsdis}
if [ -f "$HSDIS_DIR/hsdis-amd64.so" ]; then
	export LD_LIBRARY_PATH=$HSDIS_DIR${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
fi
PIN=()
if [ -n "${CPUS:-}" ]; then
	PIN=(taskset -c "$CPUS")
fi
JAR=$(realpath "$1")
OUT=$2
shift 2
mkdir -p "$OUT"
for config in "$@"; do
	"${PIN[@]}" "$JAVA" ${JVM_ARGS:--Xms1g -Xmx1g -XX:+AlwaysPreTouch} -cp "$JAR" "$HERE/JitProbe.java" "$config" "$DOCS" "$SECONDS_PER_CONFIG" \
		> "$OUT/$config.txt" 2> "$OUT/$config.err" || { echo "FAILED $config, see $OUT/$config.err" >&2; continue; }
	echo "$OUT/$config.txt: $(head -1 "$OUT/$config.txt" | cut -c3-)"
done
