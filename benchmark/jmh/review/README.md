# Benchmark: before and after the review series

Compares the library before the review fixes (#295 onwards) with master after all of them are merged, in one run, on a quiet machine. `run.sh` in this directory does the whole run.

## When

- **Open PRs:** after the review PRs which change main code are merged (#313 to #317). `run.sh` refuses to start while any of their branches still has an open PR. The max-size assertion PR (#318) only changes tests, so it doesn't gate the run.
- **Quiet machine:** no IDE, Maven or other Java processes, and CPU boost off (`/sys/devices/system/cpu/cpufreq/boost` reads 0). The script refuses otherwise. IDEs such as Android Studio run as `java` processes, so close them rather than using `FORCE=1`.

## Before and after

- **Before:** `91da7f47` ("Prune takes precedence over anonymize when both match (#288)"). It is the first-parent parent of `1f110bb` (#295), the first merge of the review series. Every per-PR comparison in the review PRs used the same baseline.
- **After:** `origin/master` at the time of the run. The script fetches it first.
- **Benchmark code:** `benchmark/` (sources and CVE documents) is identical at both commits, and none of the open PRs change it. The script warns if that changes, since differences could then come from benchmark code.

## Benchmarks

All on the CVE documents of 2 KB, 22 KB and 200 KB, like the per-PR runs: 47 benchmarks × 3 sizes = 141 results per side.

| Group | Class | Methods | Settings | Covers |
|---|---|---|---|---|
| Core filters | `CveFilterBenchmark` | all 18 | max size = bucket size − 1 byte (mostly fits), max string length 64; mixed char[]/byte[] | every core and Jackson filter type, incl. path, any-path, max string length, max size, whitespace removal. `anon_*_jsonMask` (third-party JsonMasker, unchanged) serve as noise controls. |
| Path, limited | `cve.CvePathMaxSizeMaxStringLengthFilterBenchmark` | all 6 | max size = half the smallest document, max string length 64; char[] and byte[] | path max-size filters, keep/remove whitespace and Jackson (#304, #307, #313, #315, #316, #317) |
| Max size, limited | `cve.CveMaxSizeFilterBenchmark` | all 6 | half the smallest document; char[] and byte[] | truncation in `MaxSizeJsonFilter`, `MaxSizeRemoveWhitespaceJsonFilter`, `JacksonMaxSizeJsonFilter` (#306, #308, #307, #315, #313). Not in the earlier runs. |
| Max string length + max size, limited | `cve.CveMaxStringLengthMaxSizeFilterBenchmark` | all 6 | half the smallest document, max string length 64; char[] and byte[] | `MaxStringLengthMaxSize*` core, whitespace and Jackson; the byte[] paths aren't in `CveFilterBenchmark`. Not in the earlier runs. |
| Backward max-size scan | `tmp.TmpMaxSizeFromEndBenchmark` (temporary, new) | 4 | max size = 3/4 of the smallest document; char[] and byte[] | `MaxSizeFromEndJsonFilter` (#307 items 7 and 8) and `CompositeMaxSizeJsonFilter`, which the default factory creates for max size only. No committed benchmark covers them. |
| Pretty-printed input | `tmp.TmpPrettyInputBenchmark` (temporary, as in the per-PR runs) | 7 | path and max string length filters removing whitespace from pretty-printed input; the pretty printer | #295, #297, #304, #310, #311, #314 |

The two `tmp` benchmarks aren't part of the benchmark module. They are `TmpPrettyInputBenchmark.java` and `TmpMaxSizeFromEndBenchmark.java` in this directory, and the script copies them into both builds, so both sides run the same benchmark code.

**Left out:**
- **The other CVE sizes (8–100 KB):** the per-PR runs used 2, 22 and 200 KB, so the final numbers stay comparable with them, and the run stays at about 2 hours.
- **`cve.CveAnyPathFilterBenchmark`, `cve.CvePathMaxStringLengthFilterBenchmark`, `cve.CveMaxStringLengthFilterBenchmark`:** they measure the same filters as `CveFilterBenchmark` (`anon_any_core`, `all_core`, `maxStringLength_*`). Those review changes (#296, #298, #299, #300) are per-document checks or construction-time only.
- **`AllFilterBenchmark`, `ScratchFilterBenchmark`, `RemoveWhitespaceBenchmark`, and the Entur, FDA and Shopify datasets:** comparisons with other libraries, scratch code, or other documents, which none of the per-PR runs used. They could follow if the CVE results show something worth checking on other shapes.
- **Metrics:** no benchmark passes `JsonFilterMetrics`, so the metrics-on paths changed by #314 and #303 aren't measured. The metrics-off paths are covered above.
- **Construction-time changes:** #294, #300 and #301 (path and builder validation) and #309 (path module).

## Method

- **JMH:** 1.37 from the shaded jar, Java 25 (currently openjdk 25.0.4.1). 3 warmup and 5 measurement iterations of 1 s, as in all per-PR runs.
- **Forks:** 3 per side (see the benchmark-jit-sensitivity note: hot loops swing 10–30% between builds, so single forks mislead). Each fork is a separate JMH run with `-f 1`, so before and after can alternate: B A, A B, B A. Drift during the run then affects both sides.
- **Merging the forks:** `merge_forks.py` merges each side's forks. It concatenates the iterations and recomputes the score and 99.9% confidence interval as JMH does. On the per-PR 2-fork result (`result-master-ALL2.json`), splitting and merging reproduces JMH's own score and error to 1e-14.
- **Quiet machine:** CPU boost off and nothing else running. The script checks both at the start, and logs any other Java process it sees between forks.

## Build

- **Jars:** `base-91da7f4.jar` and `target-<sha>.jar` in `benchmark/jmh/target/review/`, built from `91da7f4` and the fetched `origin/master`. An existing base jar is reused.
- **Worktrees:** `<checkout>-bench-base` and `<checkout>-bench-final`, detached, next to the checkout the script runs from. The two `tmp` benchmarks are copied into `benchmark/jmh/src/main/java/com/github/skjolber/jsonfilter/jmh/tmp/` there.
- **Command:** `mvn -q package -pl benchmark/jmh -am -DskipTests --no-transfer-progress`, never `mvn install`, and never in the checkout itself. The two jars are built one after the other, and nothing builds while JMH runs.
- **Cleanup:** the base worktree is removed after its build. The final worktree is the working directory for JMH, which reads the CVE documents by relative path, and it is removed after the runs.

## Duration

- **Measurement:** 141 results × 3 forks × 2 sides at about 8.2 s each (the per-PR full run took 25 min for 93 results × 2 forks), so about 1 h 55 min.
- **Build:** under a minute; each jar took about 15 s in the smoke run.
- **Total:** about 2 hours.

## Output and presentation

Everything goes to `benchmark/jmh/target/review/run-<timestamp>/`:
- `run-info.txt`: commits, Java version, boost and governor state, regex, and timestamps
- per-fork JSON and logs
- `base.json` and `target.json`, the merged forks
- `comparison.md`: one table, before and after
- `comment.md`: from `comment.py`, with `FORKS` and `ERRNOTE` set by the script. It has one collapsible table per group and a `SUMMARY (to be written)` placeholder to replace.

`compare.py`'s "master" and "PR" columns are relabelled "before" and "after". `merge_forks.py`, `compare.py` and `comment.py` are in this directory. ▲ and ▼ mark changes beyond the combined error. Use the JsonMasker rows as a check on the noise level before reading anything into ±3%.

## Where to post

`comment.md` is ready to post as a comment, e.g. on the PR which added this directory, once its summary placeholder is written. `target.json` can also go into `benchmark/jmh/results/` with the next release, as the repo does for releases.

## Running

```bash
benchmark/jmh/review/run.sh
```

- **Overrides:** `FORCE=1` skips the process and boost checks; `ALLOW_OPEN=1` skips the open-PR check; `TARGET=<ref>` picks another target; `FORKS=<n>` changes the forks per side.
- **Smoke test:** `SMOKE=1 FORCE=1 benchmark/jmh/review/run.sh` runs the whole pipeline with two benchmarks on 2 KB, one fork and one iteration, and writes to `benchmark/jmh/target/review/smoke-<timestamp>/`. With one iteration the error is `nan`, which flags every row; the numbers are only a pipeline check.

## The benchmark campaign

`campaign.sh` has the helpers used for the other measurements in [`../EXPERIMENTS.md`](../EXPERIMENTS.md):
interleaved runs of experiment builds, single-shot cold starts, long runs and JIT logs. Its reports come from
`summary.py`, `oneshot_report.py`, `metrics_report.py`, `alloc_report.py`, `jit_report.py` and
`bytecode_sizes.py`, and `TmpMetricsBenchmark.java` measures the cost of metrics. `run.sh` also takes the
latest release as a third build (`RELEASE`, default `parent-6.0.8`).
