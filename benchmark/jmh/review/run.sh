#!/bin/bash
# Before/after benchmark of the review series (#295 onwards). See README.md in this directory.
#
#   ./run.sh                   full run: baseline 91da7f4 against origin/master
#   SMOKE=1 FORCE=1 ./run.sh   pipeline check: two benchmarks, 2 KB, one fork, one iteration
#
# Output goes to benchmark/jmh/target/review/ in the checkout this script is in.
#
# Environment:
#   FORCE=1       start even if another java/mvn process is running or CPU boost is on
#   ALLOW_OPEN=1  start even if review PRs are still open
#   TARGET=<ref>  target commit (default origin/master, fetched first)
#   FORKS=<n>     forks per side (default 3), run interleaved
set -euo pipefail

BENCH=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO=$(git -C "$BENCH" rev-parse --show-toplevel)
OUT_ROOT=$REPO/benchmark/jmh/target/review
BASE_COMMIT=91da7f4
BASE_WT=$REPO-bench-base
FINAL_WT=$REPO-bench-final
TMP_SOURCES="TmpPrettyInputBenchmark.java TmpMaxSizeFromEndBenchmark.java"
TMP_DIR=benchmark/jmh/src/main/java/com/github/skjolber/jsonfilter/jmh/tmp
# head branches of the review PRs which change main code (the max-size assertion PR only changes tests)
REVIEW_BRANCHES="fix/jackson-max-size-estimate fix/metrics fix/whitespace-max-size-overshoot fix/max-size-replacement-growth fix/max-size-path-filters"

SMOKE=${SMOKE:-0}
FORCE=${FORCE:-0}
ALLOW_OPEN=${ALLOW_OPEN:-0}
TARGET=${TARGET:-origin/master}
if [ "$SMOKE" = 1 ]; then
	FORKS=${FORKS:-1}
	REGEX='CveFilterBenchmark\.maxSize_core$|TmpMaxSizeFromEndBenchmark\.from_end_char$'
	PARAMS='fileName=2KB'
	ITERATIONS=(-wi 1 -i 1 -w 1 -r 1)
	OUT=$OUT_ROOT/smoke-$(date +%Y%m%d-%H%M%S)
else
	FORKS=${FORKS:-3}
	REGEX='CveFilterBenchmark\.|CvePathMaxSizeMaxStringLengthFilterBenchmark\.|CveMaxSizeFilterBenchmark\.|CveMaxStringLengthMaxSizeFilterBenchmark\.|TmpPrettyInputBenchmark\.|TmpMaxSizeFromEndBenchmark\.'
	PARAMS='fileName=2KB,22KB,200KB'
	ITERATIONS=(-wi 3 -i 5 -w 1 -r 1)
	OUT=$OUT_ROOT/run-$(date +%Y%m%d-%H%M%S)
fi

busy() {
	{ pgrep -a -x java || true; pgrep -a -x mvnd || true; pgrep -a -f '[M]avenDaemon' || true; } | sort -u | cut -c1-160
}

# --- checks before starting -------------------------------------------------------------------------

if [ "$SMOKE" != 1 ] && [ "$ALLOW_OPEN" != 1 ]; then
	if ! open=$(gh pr list --repo skjolber/json-log-filter --state open --limit 100 --json number,headRefName --jq '.[] | "\(.number) \(.headRefName)"'); then
		echo "Could not list open PRs with gh. Set ALLOW_OPEN=1 to skip this check." >&2
		exit 1
	fi
	still_open=""
	for b in $REVIEW_BRANCHES; do
		line=$(echo "$open" | awk -v b="$b" '$2 == b' || true)
		if [ -n "$line" ]; then
			still_open="$still_open  #$line"$'\n'
		fi
	done
	if [ -n "$still_open" ]; then
		printf 'Review PRs are still open:\n%sMerge them first, or set ALLOW_OPEN=1.\n' "$still_open" >&2
		exit 1
	fi
fi

if [ -n "$(busy)" ] && [ "$FORCE" != 1 ]; then
	echo "Other java/mvn processes are running; benchmarks need a quiet machine:" >&2
	busy >&2
	echo "Stop them, or set FORCE=1." >&2
	exit 1
fi

BOOST_FILE=/sys/devices/system/cpu/cpufreq/boost
BOOST=$(cat $BOOST_FILE 2>/dev/null || echo unknown)
if [ "$BOOST" != 0 ] && [ "$FORCE" != 1 ]; then
	echo "CPU boost is '$BOOST'. Turn it off first: echo 0 | sudo tee $BOOST_FILE (or set FORCE=1)." >&2
	exit 1
fi

for f in $TMP_SOURCES; do
	[ -f "$BENCH/$f" ] || { echo "Missing $BENCH/$f" >&2; exit 1; }
done

mkdir -p "$OUT"
INFO=$OUT/run-info.txt
log() { echo "$(date +%T) $*" | tee -a "$INFO"; }

git -C "$REPO" fetch -q origin
TARGET_SHA=$(git -C "$REPO" rev-parse --short "$TARGET")
BASE_SHA=$(git -C "$REPO" rev-parse --short "$BASE_COMMIT")
if [ -n "$(git -C "$REPO" diff --stat "$BASE_SHA" "$TARGET_SHA" -- benchmark/jmh/src benchmark/jmh/pom.xml)" ]; then
	echo "WARNING: the benchmark module differs between $BASE_SHA and $TARGET_SHA; differences may come from benchmark code:" | tee -a "$INFO"
	git -C "$REPO" diff --stat "$BASE_SHA" "$TARGET_SHA" -- benchmark/jmh/src benchmark/jmh/pom.xml | tee -a "$INFO"
fi

BASE_JAR=$OUT_ROOT/base-$BASE_SHA.jar
if [ "$SMOKE" = 1 ]; then
	TARGET_JAR=$OUT/target-$TARGET_SHA.jar
else
	TARGET_JAR=$OUT_ROOT/target-$TARGET_SHA.jar
fi

{
	echo "mode: $([ "$SMOKE" = 1 ] && echo smoke || echo full)"
	echo "baseline: $(git -C "$REPO" log -1 --format='%h %s' "$BASE_SHA")"
	echo "target:   $(git -C "$REPO" log -1 --format='%h %s' "$TARGET_SHA") ($TARGET)"
	echo "forks per side: $FORKS, interleaved; iterations: ${ITERATIONS[*]}; params: $PARAMS"
	echo "regex: $REGEX"
	echo "cpu boost: $BOOST; governor: $(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null || echo unknown); cpus: $(nproc)"
	java -version 2>&1 | head -2
} >> "$INFO"

# --- build the jars, one at a time ------------------------------------------------------------------

worktree() { # path commit
	if [ -d "$1" ]; then
		if [ "$(git -C "$1" rev-parse HEAD)" != "$(git -C "$REPO" rev-parse "$2^{commit}")" ]; then
			echo "$1 exists at another commit; remove it first (git worktree remove $1)" >&2
			exit 1
		fi
	else
		git -C "$REPO" worktree add -q --detach "$1" "$2"
	fi
	mkdir -p "$1/$TMP_DIR"
	for f in $TMP_SOURCES; do
		cp "$BENCH/$f" "$1/$TMP_DIR/$f"
	done
}

has_tmp_benchmarks() { # jar
	python3 -I -c 'import sys, zipfile; names = zipfile.ZipFile(sys.argv[1]).namelist(); sys.exit(0 if all(any(n.endswith("/" + c + ".class") for n in names) for c in sys.argv[2:]) else 1)' "$1" TmpPrettyInputBenchmark TmpMaxSizeFromEndBenchmark
}

build() { # worktree commit jar label
	if [ -f "$3" ] && has_tmp_benchmarks "$3"; then
		log "using existing $3"
		return
	fi
	worktree "$1" "$2"
	log "building $4 jar at $2"
	if ! (cd "$1" && mvn -q package -pl benchmark/jmh -am -DskipTests --no-transfer-progress) > "$OUT/build-$4.log" 2>&1; then
		echo "Build failed, see $OUT/build-$4.log" >&2
		exit 1
	fi
	cp "$1/benchmark/jmh/target/benchmarks.jar" "$3"
	has_tmp_benchmarks "$3" || { echo "$3 lacks the temporary benchmarks" >&2; exit 1; }
}

build "$BASE_WT" "$BASE_SHA" "$BASE_JAR" base
if [ -d "$BASE_WT" ]; then
	git -C "$REPO" worktree remove --force "$BASE_WT"
fi
build "$FINAL_WT" "$TARGET_SHA" "$TARGET_JAR" target
# the benchmarks read their documents relative to the benchmark module directory
worktree "$FINAL_WT" "$TARGET_SHA"
RUN_DIR=$FINAL_WT/benchmark/jmh

# --- run, interleaved: base target, target base, base target, ... -------------------------------------

COUNT=$(cd "$RUN_DIR" && java -jar "$BASE_JAR" -l "$REGEX" 2>/dev/null | grep -c '^com\.' || true)
SIZES=$(echo "$PARAMS" | tr ',' '\n' | wc -l)
log "$COUNT benchmarks x $SIZES sizes x $FORKS forks x 2 sides; estimated $(( COUNT * SIZES * FORKS * 2 * 82 / 10 / 60 )) min at ~8.2 s per benchmark, size and fork"

run() { # jar side fork
	if [ -n "$(busy)" ]; then
		log "WARNING: other java/mvn processes running before $2 fork $3:"
		busy | tee -a "$INFO"
	fi
	log "$2 fork $3"
	(cd "$RUN_DIR" && java -jar "$1" "$REGEX" -f 1 "${ITERATIONS[@]}" -p "$PARAMS" -rf json -rff "$OUT/$2-fork$3.json") > "$OUT/log-$2-fork$3.txt" 2>&1 || log "FAILED $2 fork $3, see $OUT/log-$2-fork$3.txt"
}

for fork in $(seq 1 "$FORKS"); do
	if [ $((fork % 2)) = 1 ]; then
		run "$BASE_JAR" base "$fork"
		run "$TARGET_JAR" target "$fork"
	else
		run "$TARGET_JAR" target "$fork"
		run "$BASE_JAR" base "$fork"
	fi
done
log "runs done"

git -C "$REPO" worktree remove --force "$FINAL_WT"

# --- merge, compare and draft the comment ---------------------------------------------------------

for side in base target; do
	if ! ls "$OUT/$side"-fork*.json > /dev/null 2>&1; then
		echo "No $side results in $OUT, see the logs there" >&2
		exit 1
	fi
done
python3 -I "$BENCH/merge_forks.py" "$OUT/base.json" "$OUT"/base-fork*.json
python3 -I "$BENCH/merge_forks.py" "$OUT/target.json" "$OUT"/target-fork*.json
python3 -I - "$OUT/base.json" "$OUT/target.json" <<'PY' | tee -a "$INFO"
import json, sys
keys = lambda p: {(r["benchmark"], json.dumps(r.get("params"), sort_keys=True)) for r in json.load(open(p))}
base, target = keys(sys.argv[1]), keys(sys.argv[2])
print(f"results: base {len(base)}, target {len(target)}")
for k in sorted(base ^ target):
    print("  only in", "base" if k in base else "target", k)
PY

relabel() { sed 's/| master ops\/s | PR ops\/s |/| before ops\/s | after ops\/s |/'; }
python3 -I "$BENCH/compare.py" "$OUT/base.json" "$OUT/target.json" | relabel > "$OUT/comparison.md"

FORKS_TEXT="$FORKS forks per side, run interleaved"
ERRNOTE="Before is $BASE_SHA, master just before the review series (#295 onwards). The ± is the 99.9% confidence interval over all measurement iterations of the $FORKS forks, computed as JMH does. Before and after forks were interleaved, so drift during the run affects both sides. ▲ and ▼ mark changes beyond the combined error. "
TARGET="$TARGET" FORKS="$FORKS_TEXT" ERRNOTE="$ERRNOTE" python3 -I "$BENCH/comment.py" "SUMMARY (to be written)" \
	"Core filters|$OUT/base.json|$OUT/target.json|CveFilterBenchmark." \
	"Path filters with max size and max string length|$OUT/base.json|$OUT/target.json|CvePathMaxSizeMaxStringLengthFilterBenchmark." \
	"Max size, limited to half the smallest document|$OUT/base.json|$OUT/target.json|CveMaxSizeFilterBenchmark." \
	"Max string length and max size, limited|$OUT/base.json|$OUT/target.json|CveMaxStringLengthMaxSizeFilterBenchmark." \
	"Backward max-size scan and the default composite filter|$OUT/base.json|$OUT/target.json|TmpMaxSizeFromEndBenchmark." \
	"Pretty-printed input and the pretty printer|$OUT/base.json|$OUT/target.json|TmpPrettyInputBenchmark." \
	| relabel > "$OUT/comment.md"

log "done: $OUT/comparison.md and $OUT/comment.md"
