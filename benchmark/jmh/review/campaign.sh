#!/bin/bash
# Helpers for the benchmark campaign described in benchmark/jmh/EXPERIMENTS.md. Source this file:
#
#   source benchmark/jmh/review/campaign.sh
#
#   build_jar <worktree> <jar>                      build a benchmark jar with the Tmp*.java sources of this directory
#   interleave <name> <regex> <params> <forks> <jmh options> <side>=<jar> ...
#                                                   forks per side, rotating the order of the sides per fork,
#                                                   then merged forks and a comparison against the first side;
#                                                   SIDE_ARGS_<side> adds JMH options for one side
#   oneshot <name> <regex> <params> <forks> <iterations> <side>=<jar> ...
#                                                   single-shot times from fresh JVMs, iteration 1 being the first call
#   jit_log <name> <jar> <regex> <params>           one fork with PrintCompilation and PrintInlining
#
# One JMH or Maven process at a time; never mvn install. Output goes to $CAMPAIGN (default
# benchmark/jmh/target/review/campaign).

CAMPAIGN_BENCH=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
CAMPAIGN_REPO=$(git -C "$CAMPAIGN_BENCH" rev-parse --show-toplevel)
# the benchmarks read their documents relative to the benchmark module
CAMPAIGN_RUN_DIR=$CAMPAIGN_REPO/benchmark/jmh
CAMPAIGN=${CAMPAIGN:-$CAMPAIGN_REPO/benchmark/jmh/target/review/campaign}
CAMPAIGN_TMP_DIR=benchmark/jmh/src/main/java/com/github/skjolber/jsonfilter/jmh/tmp
mkdir -p "$CAMPAIGN"

campaign_log() {
	echo "$(date +%T) $*" | tee -a "$CAMPAIGN/campaign.log"
}

campaign_busy() {
	{ pgrep -a -x java || true; pgrep -a -x mvnd || true; pgrep -a -f '[M]avenDaemon' || true; } | sort -u | cut -c1-160
}

build_jar() { # worktree jar
	local wt=$1 jar=$2
	mkdir -p "$wt/$CAMPAIGN_TMP_DIR"
	cp "$CAMPAIGN_BENCH"/Tmp*.java "$wt/$CAMPAIGN_TMP_DIR/"
	campaign_log "building $jar from $wt at $(git -C "$wt" log -1 --format='%h %s' | cut -c1-80)"
	if ! (cd "$wt" && mvn -q package -pl benchmark/jmh -am -DskipTests --no-transfer-progress) > "$jar.build.log" 2>&1; then
		campaign_log "BUILD FAILED, see $jar.build.log"
		return 1
	fi
	cp "$wt/benchmark/jmh/target/benchmarks.jar" "$jar"
}

campaign_run() { # jar output-json log jmh-args...
	local jar=$1 json=$2 log=$3
	shift 3
	if [ -n "$(campaign_busy)" ]; then
		campaign_log "note: other java processes: $(campaign_busy | tr '\n' ';' | cut -c1-200)"
	fi
	(cd "$CAMPAIGN_RUN_DIR" && java -jar "$jar" "$@" -rf json -rff "$json") > "$log" 2>&1 || campaign_log "FAILED: $log"
}

interleave() { # name regex params forks "jmh options" side=jar ...
	local name=$1 regex=$2 params=$3 forks=$4 opts=$5 out=$CAMPAIGN/$1
	shift 5
	local sides=("$@") n=$#
	mkdir -p "$out"
	campaign_log "interleave $name: $regex ($params), $forks forks x $n sides, $opts"
	for fork in $(seq 1 "$forks"); do
		for k in $(seq 0 $((n - 1))); do
			local entry=${sides[$(( (fork - 1 + k) % n ))]}
			local side=${entry%%=*} jar=${entry#*=}
			campaign_log "  $name $side fork $fork"
			# extra JMH options for one side, e.g. SIDE_ARGS_inline="-jvmArgsAppend -XX:CompileCommand=inline,.."
			local extra_var="SIDE_ARGS_$side"
			# shellcheck disable=SC2086
			campaign_run "$jar" "$out/$side-fork$fork.json" "$out/log-$side-fork$fork.txt" "$regex" -f 1 $opts ${!extra_var:-} -p "fileName=$params"
		done
	done
	for entry in "${sides[@]}"; do
		local side=${entry%%=*}
		python3 -I "$CAMPAIGN_BENCH/merge_forks.py" "$out/$side.json" "$out/$side"-fork*.json
	done
	local first=${sides[0]%%=*}
	for entry in "${sides[@]:1}"; do
		local side=${entry%%=*}
		python3 -I "$CAMPAIGN_BENCH/compare.py" "$out/$first.json" "$out/$side.json" \
			| sed "s/| master ops\/s | PR ops\/s |/| $first ops\/s | $side ops\/s |/" > "$out/compare-$first-$side.md"
	done
	campaign_log "interleave $name done: $out"
}

oneshot() { # name regex params forks iterations side=jar ...
	local name=$1 regex=$2 params=$3 forks=$4 iterations=$5 out=$CAMPAIGN/$1
	shift 5
	local sides=("$@") n=$#
	mkdir -p "$out"
	campaign_log "oneshot $name: $regex ($params), $forks forks x $n sides, $iterations single shots per fork"
	for fork in $(seq 1 "$forks"); do
		for k in $(seq 0 $((n - 1))); do
			local entry=${sides[$(( (fork - 1 + k) % n ))]}
			local side=${entry%%=*} jar=${entry#*=}
			campaign_run "$jar" "$out/$side-fork$fork.json" "$out/log-$side-fork$fork.txt" "$regex" -f 1 -bm ss -tu us -wi 0 -i "$iterations" -p "fileName=$params"
		done
	done
	python3 -I "$CAMPAIGN_BENCH/oneshot_report.py" "$out" > "$out/report.md"
	campaign_log "oneshot $name done: $out/report.md"
}

jit_log() { # name jar regex params
	local name=$1 jar=$2 regex=$3 params=$4 out=$CAMPAIGN/jit
	mkdir -p "$out"
	campaign_log "jit_log $name: $regex ($params)"
	(cd "$CAMPAIGN_RUN_DIR" && java -jar "$jar" "$regex" -f 1 -wi 3 -i 2 -p "fileName=$params" \
		-jvmArgsAppend "-XX:+UnlockDiagnosticVMOptions -XX:+PrintCompilation -XX:+PrintInlining") > "$out/$name.log" 2>&1 \
		|| campaign_log "FAILED: $out/$name.log"
}
