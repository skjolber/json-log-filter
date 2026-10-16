# Benchmark campaign: the review series, the latest release, JIT and experiments

Results of one benchmark campaign on master after the review series (#295 to #318). The scripts are in
[`review/`](review/): `run.sh` for the before/after comparison (see [`review/README.md`](review/README.md)),
and `campaign.sh` for the other runs.

**Status: planned, not yet run.** Sections marked _(pending)_ are filled in by the run.

## Setup

- Machine: _(pending: CPU, cores, memory)_, CPU boost off, nothing else running (an IDE, if open, is noted).
- Java: _(pending: java -version)_, JMH 1.37 from the shaded benchmark jar.
- Documents: the CVE documents in `src/test/resources/benchmark/cves`, 2 KB, 22 KB and 200 KB.
- Compared builds:
  - **release:** 6.0.8 (`parent-6.0.8`, `c6c0dc16`), the latest release.
  - **before:** `91da7f4` (#288), master just before the review series.
  - **master:** _(pending: commit)_, after the review series.
- Unless noted: 3 warmup and 5 measurement iterations of 1 s, 3 forks per build, interleaved so that each
  fork rotates the order of the builds. The ± is the 99.9% confidence interval over all iterations.

## 1. Before and after the review series

_(pending: `run.sh`, 47 benchmarks × 3 documents, 3 forks, release, before and master interleaved)_

## 2. Regressions against the latest release

_(pending: the release side of the same run)_

## 3. Cold start: the first calls

Single-shot times from fresh JVMs (`-bm ss -wi 0`): the first call, calls 2 to 5 and the last of 20, the
median over forks. Logging filters a few documents per process early on, so the first calls matter as much
as the steady state.

_(pending)_

## 4. Long runs

A few representative benchmarks with 10 s iterations (5 warmup, 10 measurement), to see whether the 1 s
iterations of the main run reach the steady state, and whether throughput drifts (recompilation, GC).

_(pending)_

## 5. JIT report

### Bytecode sizes

HotSpot never compiles a method larger than `HugeMethodLimit` (8000 bytes of bytecode), never inlines a hot
method larger than `FreqInlineSize` (325 bytes) and inlines a cold one only up to `MaxInlineSize` (35 bytes).
From `review/bytecode_sizes.py` on master `2118a4de` (approximate, from javap offsets):

- 792 library methods; none above 8000 bytes, so every method can be compiled; 51 above 325 bytes.
- The largest are the scan loops, which are entry points of their own and need no inlining:

| Bytes | Method |
|---|---|
| 1538 | `ws.PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter.processMaxSize` (char[] and byte[]) |
| 1191 | `PathMaxSizeMaxStringLengthJsonFilter.ranges` (char[] and byte[]) |
| 884 | `JacksonPathMaxSizeMaxStringLengthJsonFilter.process(JsonParser, ..)` |
| 815–818 | `ws.PathMaxStringLengthRemoveWhitespaceJsonFilter.process` |
| 790 | `AbstractPathJsonFilter.matchesEncoded(byte[], ..)` |
| 782–789 | `ws.MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter.processMaxStringLengthMaxSize` |
| 681 | `AnyPathMaxSizeJsonFilter.rangesAnyPathMaxSize` |
| 657–663 | `Char/ByteArrayWhitespaceSizeFilter.anonymizeObjectOrArrayMaxSize` |
| 570–572 | `Char/ByteArrayWhitespaceSizeFilter.skipObjectOrArrayMaxSizeMaxStringLength` |
| 551–567 | `AnyPathJsonFilter.rangesAnyPath`, `Char/ByteArrayRangesSizeFilter.anonymizeSubtree` |

_(pending: the same for the release, and which callees of these loops are too big to inline)_

### Compilation, inlining and deoptimization

From `-XX:+PrintCompilation -XX:+PrintInlining` (`campaign.sh jit_log`, `review/jit_report.py`): when the
hot methods reach C1 and C2, whether any is compiled only on stack replacement (OSR), how often compiled code
is thrown away (made not entrant), and which library callees fail to inline and why.

_(pending)_

## 6. Experiments: accuracy and metrics against speed

Each experiment is a local branch on top of master, not pushed, measured against master with 3 interleaved
forks. Library code on master is unchanged.

### F1. The cost of metrics

No benchmark passed metrics before. `review/TmpMetricsBenchmark.java` runs eight filters with metrics off and
on (`DefaultJsonFilterMetrics`), with max size at half the smallest document.

_(pending)_

### F2. Jackson: estimate the token size without the parser location

The Jackson max-size filters estimate the size of each token from the input offset, which takes
`parser.currentLocation()` through a `LongSupplier` per token, and compute the exact size only near the limit.
The experiment (`exp/jackson-token-estimate`, in `JacksonMaxSizeJsonFilter` and
`JacksonMaxSizeMaxStringLengthJsonFilter`) estimates from the token instead: text length plus quotes and
separators. That is a bit less accurate, as escapes and multi-byte characters are only counted once the
estimate reaches the limit.

_(pending)_

### F3. Whitespace max-size filters without the per-value fit check

#315 checks that a string value fits before the whitespace after it is credited. The experiment
(`exp/ws-no-value-fit-check`) removes the check, so the output may exceed max size by one value: the
cheapest possible accuracy, as a control.

_(pending)_

### F4. Jackson: no 16K-character builder per call

Five Jackson filters allocate a `StringBuilder` of at least 16K characters (about 32 KB) on every call,
for truncating strings, also when nothing is truncated; 6.0.8 does the same. The experiment
(`exp/jackson-small-builder`) sizes it for one truncated string.

_(pending)_

## 7. Findings and recommendations

_(pending)_

## Reproducing

```bash
# before/after and release, about 3 hours
RELEASE=parent-6.0.8 benchmark/jmh/review/run.sh

source benchmark/jmh/review/campaign.sh
build_jar <worktree> <jar>
interleave <name> '<regex>' 22KB,200KB 3 '-wi 3 -i 5 -w 1 -r 1' master=<jar> experiment=<jar>
oneshot <name> '<regex>' 2KB 10 20 release=<jar> master=<jar>
jit_log <name> <jar> '<regex>' 22KB && python3 -I benchmark/jmh/review/jit_report.py <log>
python3 -I benchmark/jmh/review/bytecode_sizes.py <jar>
```
