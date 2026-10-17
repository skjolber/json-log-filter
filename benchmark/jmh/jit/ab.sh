#!/bin/bash
# A/B benchmark on JDK 27: a base commit against the working tree of this checkout. See PLAN.md.
#
#   ./ab.sh <name> <base-ref> <benchmark regex> [jmh params]
#   ./ab.sh from-end origin/master 'CveMaxSizeFromEndFilterBenchmark\.from_end_bytes$' fileName=2KB,22KB,200KB
#
# Builds the base jar from <base-ref> in a worktree next to the checkout, and the candidate jar from this checkout's
# working tree, one after the other (never mvn install). Benchmark classes which the working tree has and the base
# commit lacks are copied into the base build, so new benchmarks can measure old commits.
# Then runs FORKS forks per side, interleaved (B C, C B, ...), and writes benchmark/jmh/target/jit/<name>/:
# comparison.md, the merged base.json and candidate.json, and the per-fork results and logs.
#
# Environment:
#   FORKS=<n>       forks per side (default 3); 0 only builds the base jar and prints its path
#   ITERATIONS=...  JMH iteration options (default "-wi 3 -i 5 -w 1 -r 1")
#   PROF=<name>     a JMH profiler for every fork, e.g. gc, perfnorm, or perfasm (machine code, with hsdis; for analysis,
#                   not timing)
#   HSDIS_DIR=<dir> where hsdis-amd64.so is (default ~/.local/share/hsdis)
#   JAVA=<path>     the JVM to run on (default Temurin 27)
#   JVM_ARGS=...    options for every benchmark fork (default "-Xms1g -Xmx1g -XX:+AlwaysPreTouch": a fixed, pre-touched
#                   heap, so that forks do not differ in heap growth)
#   BACKPORT=0      do not copy benchmark classes into the base build
#   MVN=mvnd        build with the Maven Daemon, which stays warm between builds; one daemon per CPU set, started
#                   under the same pinning, so that parallel tracks never share one
#   CPUS=<list>     pin builds and benchmarks to these CPUs (taskset), for parallel tracks; skips the busy check
#   JARS=<dir>      where base jars are cached, to share them between checkouts (default benchmark/jmh/target/jit/jars)
#   LOG_COMPILATION=1  write a HotSpot compilation log (LogCompilation) per forked JVM; use one benchmark and one param per run
#   FORCE=1         start even if another JMH or Maven process is running
set -euo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO=$(git -C "$HERE" rev-parse --show-toplevel)
NAME=$1
BASE_REF=$2
REGEX=$3
PARAMS=${4:-fileName=2KB,22KB,200KB}
FORKS=${FORKS:-3}
read -r -a ITER <<< "${ITERATIONS:--wi 3 -i 5 -w 1 -r 1}"
JAVA=${JAVA:-/usr/lib/jvm/temurin-27-jdk/bin/java}
BENCH_SRC=benchmark/jmh/src/main/java
JVM_ARGS=${JVM_ARGS:--Xms1g -Xmx1g -XX:+AlwaysPreTouch}
JARS=${JARS:-$REPO/benchmark/jmh/target/jit/jars}
QUICK=(-Dmaven.javadoc.skip=true -Dmaven.source.skip=true -Dmoditect.skip=true -Djacoco.skip=true -Denforcer.skip=true -Dmaven.test.skip=true)
# hsdis, for -prof perfasm and -XX:+PrintAssembly: HotSpot loads hsdis-amd64.so from LD_LIBRARY_PATH
HSDIS_DIR=${HSDIS_DIR:-$HOME/.local/share/hsdis}
if [ -f "$HSDIS_DIR/hsdis-amd64.so" ]; then
	export LD_LIBRARY_PATH=$HSDIS_DIR${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
fi
PIN=()
JMH_LOCK=()
if [ -n "${CPUS:-}" ]; then
	PIN=(taskset -c "$CPUS")
	# parallel tracks run JMH at the same time, each on its own CPUs
	JMH_LOCK=(-Djmh.ignoreLock=true)
fi
MVN_CMD=(mvn)
if [ "${MVN:-mvn}" = mvnd ]; then
	MVN_CMD=(mvnd "-Dmvnd.daemonStorage=$HOME/.m2/mvnd/ab-${CPUS:-all}")
fi
OUT=$REPO/benchmark/jmh/target/jit/$NAME
PROF_ARGS=()
if [ -n "${PROF:-}" ]; then
	PROF_ARGS=(-prof "$PROF")
fi

busy() {
	{ pgrep -af 'benchmarks.jar' || true; pgrep -af '[s]urefire|[p]lexus-classworlds' || true; } | grep -v "$$" | cut -c1-160
}
if [ -z "${CPUS:-}" ] && [ -n "$(busy)" ] && [ "${FORCE:-0}" != 1 ]; then
	echo "Other JMH or Maven processes are running:" >&2
	busy >&2
	exit 1
fi

mkdir -p "$JARS" "$OUT"
rm -f "$OUT"/*-fork*.json "$OUT"/*-fork*.log

backport() { # base checkout: copies the benchmark classes which this checkout has and the base lacks
	for f in $BACKPORT_FILES; do
		mkdir -p "$(dirname "$1/$f")"
		cp "$REPO/$f" "$1/$f"
	done
}

build() { # checkout jar log
	# a jar from an earlier build in the same checkout is already modular, which the module-info step rejects
	find "$1" -path "$1/benchmark/jmh/target/jit" -prune -o -path "*/target/*.jar" -type f -print0 | xargs -0 -r rm -f
	# the quick profile's properties, passed directly so that they also apply to commits without the profile
	(cd "$1" && "${PIN[@]}" "${MVN_CMD[@]}" -q package -pl benchmark/jmh -am --no-transfer-progress "${QUICK[@]}") > "$3" 2>&1 || { echo "Build failed, see $3" >&2; exit 1; }
	cp "$1/benchmark/jmh/target/benchmarks.jar" "$2"
}

BASE_SHA=$(git -C "$REPO" rev-parse --short "$BASE_REF^{commit}")
# the benchmark classes to copy into the base build, and a cache key which covers them
BACKPORT_FILES=""
if [ "${BACKPORT:-1}" != 0 ]; then
	BACKPORT_FILES=$(comm -13 <(git -C "$REPO" ls-tree -r --name-only "$BASE_SHA" -- "$BENCH_SRC" | grep '\.java$' | sort) <(cd "$REPO" && find "$BENCH_SRC" -name '*.java' | sort))
fi
BASE_KEY=$BASE_SHA
if [ -n "$BACKPORT_FILES" ]; then
	BASE_KEY=$BASE_SHA-$( (cd "$REPO" && cat $BACKPORT_FILES) | sha1sum | cut -c1-8)
fi
BASE_JAR=$JARS/base-$BASE_KEY.jar
if [ ! -f "$BASE_JAR" ]; then
	WT=$REPO-ab-base
	git -C "$REPO" worktree add -q --detach "$WT" "$BASE_SHA"
	backport "$WT"
	if [ -n "$BACKPORT_FILES" ]; then
		echo "$(date +%T) copied into the base build: $(echo $BACKPORT_FILES | sed "s#$BENCH_SRC/##g")"
	fi
	echo "$(date +%T) building base $BASE_SHA"
	build "$WT" "$BASE_JAR" "$OUT/build-base.log"
	git -C "$REPO" worktree remove --force "$WT"
fi
if [ "$FORKS" = 0 ]; then
	echo "$BASE_JAR"
	exit 0
fi
CAND_JAR=$OUT/candidate.jar
echo "$(date +%T) building candidate (working tree of $(git -C "$REPO" rev-parse --short HEAD))"
build "$REPO" "$CAND_JAR" "$OUT/build-candidate.log"

{
	echo "base:      $BASE_SHA $(git -C "$REPO" log -1 --format=%s "$BASE_SHA")"
	echo "copied into the base build: ${BACKPORT_FILES:-none}"
	echo "candidate: working tree of $(git -C "$REPO" rev-parse --short HEAD), $(git -C "$REPO" status --porcelain -- . ':!benchmark/jmh/jit' | wc -l) changed files"
	echo "regex: $REGEX; params: $PARAMS; forks per side: $FORKS, interleaved; ${ITER[*]} ${PROF_ARGS[*]}; fork JVM: $JVM_ARGS"
	"$JAVA" -version 2>&1 | head -2
	echo "cpu boost: $(cat /sys/devices/system/cpu/cpufreq/boost 2>/dev/null || echo unknown)"
	echo "other java processes: $(pgrep -c java || true); cpus: ${CPUS:-all}"
} > "$OUT/run-info.txt"

run() { # jar side fork
	local jvm_args="$JVM_ARGS"
	if [ "${LOG_COMPILATION:-0}" = 1 ]; then
		# one HotSpot compilation log per forked JVM, to compare the JIT decisions of fast and slow forks
		jvm_args="$jvm_args -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:LogFile=$OUT/$2-fork$3-%p.xml"
	fi
	echo "$(date +%T) $2 fork $3"
	(cd "$REPO/benchmark/jmh" && "${PIN[@]}" "$JAVA" "${JMH_LOCK[@]}" -jar "$1" "$REGEX" -f 1 "${ITER[@]}" -p "$PARAMS" -jvmArgsAppend "$jvm_args" "${PROF_ARGS[@]}" -rf json -rff "$OUT/$2-fork$3.json") > "$OUT/$2-fork$3.log" 2>&1 || echo "FAILED $2 fork $3, see $OUT/$2-fork$3.log"
}
for fork in $(seq 1 "$FORKS"); do
	if [ $((fork % 2)) = 1 ]; then
		run "$BASE_JAR" base "$fork"
		run "$CAND_JAR" candidate "$fork"
	else
		run "$CAND_JAR" candidate "$fork"
		run "$BASE_JAR" base "$fork"
	fi
done

python3 -I "$HERE/merge_forks.py" "$OUT/base.json" "$OUT"/base-fork*.json
python3 -I "$HERE/merge_forks.py" "$OUT/candidate.json" "$OUT"/candidate-fork*.json
python3 -I "$HERE/compare.py" "$OUT/base.json" "$OUT/candidate.json" | sed 's/| master ops\/s | PR ops\/s |/| base ops\/s | candidate ops\/s |/' > "$OUT/comparison.md"
if [ -n "${PROF:-}" ]; then
	python3 -I "$HERE/secondary.py" "$OUT/base.json" "$OUT/candidate.json" >> "$OUT/comparison.md" || true
fi
cat "$OUT/comparison.md"
