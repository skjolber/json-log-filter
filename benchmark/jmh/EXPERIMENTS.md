# Benchmark campaign: the review series, the latest release, JIT and experiments

Results of one benchmark campaign on master after the review series (#295 to #318). The scripts are in
[`review/`](review/): `run.sh` for the before/after comparison (see [`review/README.md`](review/README.md)),
and `campaign.sh` for the other runs.

Run on 2026-10-16, from 02:17 to 07:18.

## Setup

- Machine: AMD Ryzen 9 9950X (16 cores, 32 threads), 91 GB, Linux, governor `performance`, CPU boost off.
  Nothing else ran, except an idle IDE (Android Studio, a Java process), so the scripts ran with `FORCE=1`.
- Java: OpenJDK 25.0.4.1 (Red Hat build), default flags (G1, tiered compilation); JMH 1.37 from the shaded jar.
- Documents: the CVE documents in `src/test/resources/benchmark/cves`, one document each of 2 KB, 22 KB and
  200 KB, so one benchmark operation filters one document.
- Compared builds:
  - **release:** 6.0.8 (`parent-6.0.8`, `c6c0dc16`), the latest release.
  - **before:** `91da7f4` (#288), master just before the review series.
  - **master:** `746368a7` (#318), after the review series.
- Unless noted: 3 warmup and 5 measurement iterations of 1 s, 3 forks per build, interleaved so that each
  fork rotates the order of the builds. The ± is the 99.9% confidence interval over all iterations.

## 1. Before and after the review series

`run.sh` with the release as a third build: 47 benchmarks × 3 documents, 3 forks per build, each fork
rotating the order of release, before and master. Measured from 02:17 to 05:10. The full tables are in the
comment on #319; this ranks the changes beyond the noise.

141 results: 12 faster, 18 slower, 111 within the noise (the larger of the combined error and 3%).

Slower (before → master):

| Benchmark | Document | before ops/s | master ops/s | Change | Threshold |
|---|---|---|---|---|---|
| TmpMaxSizeFromEndBenchmark.composite_bytes | 22KB | 483,806 | 326,792 | -32.5% | ±3.0% |
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 22KB | 481,956 | 327,740 | -32.0% | ±3.9% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_char | 2KB | 1,582,829 | 1,221,218 | -22.8% | ±3.0% |
| TmpMaxSizeFromEndBenchmark.composite_char | 2KB | 2,494,630 | 2,023,263 | -18.9% | ±13.3% |
| TmpMaxSizeFromEndBenchmark.from_end_char | 2KB | 2,506,942 | 2,036,034 | -18.8% | ±13.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_keep_whitespace_char | 22KB | 162,009 | 131,858 | -18.6% | ±3.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_keep_whitespace_char | 200KB | 15,841 | 14,329 | -9.5% | ±8.1% |
| TmpMaxSizeFromEndBenchmark.composite_bytes | 2KB | 2,571,472 | 2,377,583 | -7.5% | ±3.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_char | 200KB | 16,794 | 15,627 | -6.9% | ±3.0% |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_bytes | 2KB | 1,606,275 | 1,506,344 | -6.2% | ±3.0% |
| TmpPrettyInputBenchmark.path_remove_whitespace_char | 200KB | 6,392 | 6,109 | -4.4% | ±3.0% |
| CveMaxSizeFilterBenchmark.jackson_char | 22KB | 35,637 | 34,235 | -3.9% | ±3.0% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_bytes | 200KB | 8,711 | 8,385 | -3.7% | ±3.0% |
| TmpPrettyInputBenchmark.path_max_size_remove_whitespace_bytes | 200KB | 9,460 | 9,117 | -3.6% | ±3.0% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_char | 2KB | 473,961 | 456,973 | -3.6% | ±3.0% |
| CveMaxSizeFilterBenchmark.jackson_bytes | 22KB | 38,547 | 37,168 | -3.6% | ±3.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_bytes | 200KB | 3,953 | 3,813 | -3.5% | ±3.1% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_keep_whitespace_char | 2KB | 1,583,367 | 1,527,765 | -3.5% | ±3.0% |

Faster (before → master):

| Benchmark | Document | before ops/s | master ops/s | Change | Threshold |
|---|---|---|---|---|---|
| TmpPrettyInputBenchmark.pretty_print_char | 200KB | 1,553 | 4,326 | +178.6% | ±3.0% |
| TmpPrettyInputBenchmark.pretty_print_char | 22KB | 18,300 | 33,985 | +85.7% | ±3.0% |
| TmpPrettyInputBenchmark.pretty_print_char | 2KB | 140,403 | 243,042 | +73.1% | ±3.0% |
| TmpPrettyInputBenchmark.pretty_print_bytes | 200KB | 3,022 | 5,227 | +73.0% | ±3.0% |
| TmpPrettyInputBenchmark.pretty_print_bytes | 22KB | 32,514 | 56,000 | +72.2% | ±5.6% |
| TmpPrettyInputBenchmark.pretty_print_bytes | 2KB | 252,567 | 376,781 | +49.2% | ±6.3% |
| CveMaxSizeFilterBenchmark.core_keep_whitespace_char | 22KB | 140,162 | 187,432 | +33.7% | ±3.0% |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_char | 200KB | 14,497 | 17,551 | +21.1% | ±3.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_bytes | 2KB | 1,516,174 | 1,608,692 | +6.1% | ±3.0% |
| CveFilterBenchmark.maxStringLengthMaxSize_jackson | 200KB | 1,817 | 1,891 | +4.1% | ±3.0% |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_char | 2KB | 1,500,193 | 1,555,836 | +3.7% | ±3.0% |
| TmpMaxSizeFromEndBenchmark.from_end_char | 22KB | 315,067 | 324,645 | +3.0% | ±3.0% |

For reference, 6.0.8 against `91da7f4` (the changes before the review series), from the same run:

141 results: 7 faster, 3 slower, 131 within the noise (the larger of the combined error and 3%).

Slower (6.0.8 → before):

| Benchmark | Document | 6.0.8 ops/s | before ops/s | Change | Threshold |
|---|---|---|---|---|---|
| TmpPrettyInputBenchmark.path_remove_whitespace_char | 2KB | 556,259 | 520,584 | -6.4% | ±3.0% |
| TmpPrettyInputBenchmark.path_remove_whitespace_bytes | 2KB | 762,179 | 729,039 | -4.3% | ±3.0% |
| CveFilterBenchmark.maxStringLengthMaxSize_jackson | 200KB | 1,882 | 1,817 | -3.4% | ±3.4% |

Faster (6.0.8 → before):

| Benchmark | Document | 6.0.8 ops/s | before ops/s | Change | Threshold |
|---|---|---|---|---|---|
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 2KB | 2,329,982 | 2,786,597 | +19.6% | ±18.5% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_char | 22KB | 132,732 | 152,898 | +15.2% | ±10.9% |
| TmpPrettyInputBenchmark.pretty_print_bytes | 200KB | 2,774 | 3,022 | +8.9% | ±3.9% |
| TmpMaxSizeFromEndBenchmark.composite_char | 22KB | 302,327 | 319,452 | +5.7% | ±3.1% |
| CveFilterBenchmark.all_jackson | 22KB | 28,068 | 29,317 | +4.5% | ±3.0% |
| CveMaxSizeFilterBenchmark.jackson_bytes | 200KB | 4,369 | 4,535 | +3.8% | ±3.0% |
| CveFilterBenchmark.maxSizeJackson | 200KB | 2,229 | 2,309 | +3.6% | ±3.1% |

## 2. Regressions against the latest release

6.0.8 against master, from the same interleaved run, 3 forks each. A change counts when it exceeds both
the combined 99.9% intervals and 3%, the fork-to-fork variation on this machine. 6.0.8 and `91da7f4` are
within the noise of each other for all but a few benchmarks, so these changes come from the review series.

141 results: 14 faster, 21 slower, 106 within the noise (the larger of the combined error and 3%).

Slower (6.0.8 → master):

| Benchmark | Document | 6.0.8 ops/s | master ops/s | Change | Threshold |
|---|---|---|---|---|---|
| TmpMaxSizeFromEndBenchmark.composite_bytes | 22KB | 475,958 | 326,792 | -31.3% | ±3.2% |
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 22KB | 477,254 | 327,740 | -31.3% | ±3.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_char | 2KB | 1,578,156 | 1,221,218 | -22.6% | ±3.0% |
| TmpMaxSizeFromEndBenchmark.composite_char | 2KB | 2,496,238 | 2,023,263 | -18.9% | ±13.3% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_keep_whitespace_char | 22KB | 162,195 | 131,858 | -18.7% | ±3.0% |
| TmpMaxSizeFromEndBenchmark.from_end_char | 2KB | 2,495,129 | 2,036,034 | -18.4% | ±12.9% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_keep_whitespace_char | 200KB | 15,874 | 14,329 | -9.7% | ±7.3% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_char | 200KB | 16,855 | 15,627 | -7.3% | ±3.0% |
| TmpMaxSizeFromEndBenchmark.composite_bytes | 2KB | 2,557,354 | 2,377,583 | -7.0% | ±3.0% |
| TmpPrettyInputBenchmark.path_remove_whitespace_char | 2KB | 556,259 | 524,968 | -5.6% | ±3.0% |
| CveFilterBenchmark.maxStringLength_jackson | 2KB | 134,429 | 127,517 | -5.1% | ±3.3% |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_bytes | 2KB | 1,586,652 | 1,506,344 | -5.1% | ±3.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_bytes | 2KB | 195,570 | 187,029 | -4.4% | ±3.0% |
| CveMaxSizeFilterBenchmark.jackson_bytes | 22KB | 38,722 | 37,168 | -4.0% | ±3.0% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_bytes | 200KB | 8,724 | 8,385 | -3.9% | ±3.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_bytes | 200KB | 3,967 | 3,813 | -3.9% | ±3.0% |
| TmpPrettyInputBenchmark.path_remove_whitespace_char | 200KB | 6,337 | 6,109 | -3.6% | ±3.0% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_char | 2KB | 473,137 | 456,973 | -3.4% | ±3.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_keep_whitespace_char | 2KB | 1,581,263 | 1,527,765 | -3.4% | ±3.0% |
| TmpPrettyInputBenchmark.path_max_size_remove_whitespace_bytes | 200KB | 9,433 | 9,117 | -3.4% | ±3.0% |
| CveMaxSizeFilterBenchmark.jackson_char | 22KB | 35,298 | 34,235 | -3.0% | ±3.0% |

Faster (6.0.8 → master):

| Benchmark | Document | 6.0.8 ops/s | master ops/s | Change | Threshold |
|---|---|---|---|---|---|
| TmpPrettyInputBenchmark.pretty_print_char | 200KB | 1,687 | 4,326 | +156.4% | ±12.5% |
| TmpPrettyInputBenchmark.pretty_print_bytes | 200KB | 2,774 | 5,227 | +88.4% | ±3.7% |
| TmpPrettyInputBenchmark.pretty_print_char | 22KB | 18,569 | 33,985 | +83.0% | ±3.0% |
| TmpPrettyInputBenchmark.pretty_print_char | 2KB | 139,557 | 243,042 | +74.2% | ±3.0% |
| TmpPrettyInputBenchmark.pretty_print_bytes | 22KB | 33,647 | 56,000 | +66.4% | ±3.0% |
| TmpPrettyInputBenchmark.pretty_print_bytes | 2KB | 256,428 | 376,781 | +46.9% | ±5.9% |
| CveMaxSizeFilterBenchmark.core_keep_whitespace_char | 22KB | 155,023 | 187,432 | +20.9% | ±15.0% |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_char | 200KB | 14,577 | 17,551 | +20.4% | ±3.0% |
| TmpMaxSizeFromEndBenchmark.composite_char | 22KB | 302,327 | 324,845 | +7.4% | ±3.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_bytes | 2KB | 1,526,635 | 1,608,692 | +5.4% | ±3.0% |
| CveFilterBenchmark.maxStringLengthMaxSize_jackson | 22KB | 15,855 | 16,611 | +4.8% | ±3.3% |
| TmpMaxSizeFromEndBenchmark.from_end_char | 22KB | 311,409 | 324,645 | +4.3% | ±3.2% |
| CveFilterBenchmark.all_jackson | 22KB | 28,068 | 29,237 | +4.2% | ±3.5% |
| CveFilterBenchmark.maxSizeJackson | 200KB | 2,229 | 2,311 | +3.7% | ±3.0% |

**Real regressions**, confirmed in every fork:

- **Backward max-size scan, byte[] (−31% at 22 KB).** `MaxSizeFromEndJsonFilter`, which the default
  factory uses through `CompositeMaxSizeJsonFilter` when the document is less than 1.5 times the max size.
  #307 added a short forward scan and a trailing-whitespace case after the scan loop of
  `processMaxSizeFromEnd(byte[], ..)`, which grew from 324 to 359 bytes.
- **Max string length + max size, char[] (−19% to −23%).** `MaxStringLengthMaxSizeJsonFilter` (keep
  whitespace) and `MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter`, char[] only; byte[] is unchanged or
  faster. #307 and #315 added a check per closing bracket and per string value to their scan loops.

In all three, C2 makes the same compilation and inlining decisions before and after (section 5): the scan
method is compiled on its own, with the same callees inlined. What changed is the code C2 generates for the
scan loop itself, which is known to swing 10–30% on small changes to these loops. Experiment F5 tries to
undo it for the backward scan.
- **Smaller, 3–6%:** the Jackson max-size and max-string-length filters on small documents, and some
  whitespace-removing filters at 2 KB.

**Improvements:** the pretty printer is 1.5 to 2.5 times faster (#314 split `process(..)` into a small
wrapper and the loop), and the core max-size char[] filters are up to 20% faster at some sizes.

## 3. Cold start: the first calls

Single-shot times from fresh JVMs (`-bm ss -wi 0 -i 20`), 9 `CveFilterBenchmark` filters, 6.0.8 and
master, 5 JVMs each per document size: the median of the first call, of calls 2 to 5 and of the 20th call.
Logging filters a few documents per process early on, so the first calls matter as much as the steady state.

| Benchmark | Document | Side | Forks | 1st call µs | calls 2-5 µs | last call µs | 1st / last |
|---|---|---|---|---|---|---|---|
| CveFilterBenchmark.all_core | 22KB | master | 5 | 1,056 | 187 | 125 | 8.5x |
| CveFilterBenchmark.all_core | 22KB | release | 5 | 1,018 | 181 | 97 | 10.5x |
| CveFilterBenchmark.all_core | 2KB | master | 5 | 513 | 62 | 28 | 18.3x |
| CveFilterBenchmark.all_core | 2KB | release | 5 | 468 | 59 | 34 | 13.9x |
| CveFilterBenchmark.all_jackson | 22KB | master | 5 | 15,892 | 665 | 349 | 45.6x |
| CveFilterBenchmark.all_jackson | 22KB | release | 5 | 17,887 | 820 | 430 | 41.6x |
| CveFilterBenchmark.all_jackson | 2KB | master | 5 | 13,830 | 357 | 127 | 108.6x |
| CveFilterBenchmark.all_jackson | 2KB | release | 5 | 13,900 | 324 | 116 | 120.1x |
| CveFilterBenchmark.all_maxsize_core | 22KB | master | 5 | 1,113 | 196 | 113 | 9.8x |
| CveFilterBenchmark.all_maxsize_core | 22KB | release | 5 | 1,040 | 196 | 101 | 10.3x |
| CveFilterBenchmark.all_maxsize_core | 2KB | master | 5 | 572 | 67 | 32 | 17.7x |
| CveFilterBenchmark.all_maxsize_core | 2KB | release | 5 | 507 | 66 | 32 | 15.9x |
| CveFilterBenchmark.all_maxsize_jackson | 22KB | master | 5 | 17,802 | 764 | 437 | 40.7x |
| CveFilterBenchmark.all_maxsize_jackson | 22KB | release | 5 | 19,061 | 874 | 507 | 37.6x |
| CveFilterBenchmark.all_maxsize_jackson | 2KB | master | 5 | 15,220 | 387 | 124 | 122.9x |
| CveFilterBenchmark.all_maxsize_jackson | 2KB | release | 5 | 14,713 | 413 | 114 | 128.7x |
| CveFilterBenchmark.anon_any_core | 22KB | master | 5 | 993 | 180 | 106 | 9.3x |
| CveFilterBenchmark.anon_any_core | 22KB | release | 5 | 960 | 173 | 147 | 6.5x |
| CveFilterBenchmark.anon_any_core | 2KB | master | 5 | 490 | 61 | 27 | 18.2x |
| CveFilterBenchmark.anon_any_core | 2KB | release | 5 | 432 | 60 | 22 | 19.7x |
| CveFilterBenchmark.core_remove_whitespace | 22KB | master | 5 | 1,065 | 116 | 40 | 26.8x |
| CveFilterBenchmark.core_remove_whitespace | 22KB | release | 5 | 1,003 | 115 | 44 | 22.8x |
| CveFilterBenchmark.core_remove_whitespace | 2KB | master | 5 | 596 | 45 | 16 | 37.7x |
| CveFilterBenchmark.core_remove_whitespace | 2KB | release | 5 | 570 | 42 | 17 | 34.3x |
| CveFilterBenchmark.maxSizeJackson | 22KB | master | 5 | 18,327 | 804 | 469 | 39.1x |
| CveFilterBenchmark.maxSizeJackson | 22KB | release | 5 | 19,398 | 891 | 513 | 37.8x |
| CveFilterBenchmark.maxSizeJackson | 2KB | master | 5 | 15,159 | 521 | 158 | 96.0x |
| CveFilterBenchmark.maxSizeJackson | 2KB | release | 5 | 14,860 | 490 | 164 | 90.6x |
| CveFilterBenchmark.maxSize_core | 22KB | master | 5 | 915 | 124 | 50 | 18.2x |
| CveFilterBenchmark.maxSize_core | 22KB | release | 5 | 892 | 123 | 37 | 23.9x |
| CveFilterBenchmark.maxSize_core | 2KB | master | 5 | 416 | 37 | 18 | 22.8x |
| CveFilterBenchmark.maxSize_core | 2KB | release | 5 | 362 | 42 | 24 | 15.1x |
| CveFilterBenchmark.maxStringLength_core | 22KB | master | 5 | 904 | 128 | 44 | 20.4x |
| CveFilterBenchmark.maxStringLength_core | 22KB | release | 5 | 863 | 131 | 42 | 20.3x |
| CveFilterBenchmark.maxStringLength_core | 2KB | master | 5 | 467 | 45 | 28 | 16.5x |
| CveFilterBenchmark.maxStringLength_core | 2KB | release | 5 | 410 | 43 | 24 | 17.3x |

- **The first call costs 0.4–0.6 ms for a 2 KB document with the core filters, about 1 ms at 22 KB**: class
  loading and interpretation. By the 20th call it is 16–35 µs at 2 KB, still 20 to 50 times the steady state
  (about 0.6–2 µs per 2 KB document), so the core filters need hundreds of documents to reach C2.
- **The Jackson filters' first call costs 14–19 ms, 30 times the core filters**, nearly all of it Jackson's own
  class loading and initialization. For logging a few documents per process, the core filters are much cheaper.
- **Master is 5–15% slower than 6.0.8 on the first call of the core filters at 2 KB** (e.g. `maxSize_core`
  416 µs against 362 µs, `anon_any_core` 490 µs against 432 µs): about 50 µs more code to load and interpret,
  once per process. From the second call on, they are level within the variation of 5 JVMs.

## 4. Long runs

Four representative benchmarks at 22 KB with 10 s iterations (5 warmup, 10 measurement, so 150 s per JVM),
2 forks each of 6.0.8 and master, interleaved. Do the 1 s iterations of the main run reach the steady state,
and does throughput drift over minutes (recompilation, GC)?

| Benchmark (22 KB, master) | 10 s iterations, range over 2 forks | Drift, first to last iteration | Mean of the long runs | Main run, 1 s iterations |
|---|---|---|---|---|
| CveFilterBenchmark.all_core | 112,022–112,663 | at most 0.0% | 112,275 | 111,258 |
| CveFilterBenchmark.all_maxsize_core | 111,922–114,209 | at most 0.1% | 113,119 | 112,615 |
| CveFilterBenchmark.all_maxsize_jackson | 26,573–26,765 | at most 0.1% | 26,675 | 26,507 |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_bytes | 86,541–87,043 | at most 0.1% | 86,799 | 86,940 |

6.0.8 against master in the long runs:

| Benchmark | Document | release ops/s | master ops/s | Change |
|---|---|---|---|---|
| CveFilterBenchmark.all_core | 22KB | 113,618 ± 0.2% | 112,275 ± 0.1% | -1.2% ▼ |
| CveFilterBenchmark.all_maxsize_core | 22KB | 112,343 ± 0.4% | 113,119 ± 0.8% | +0.7% |
| CveFilterBenchmark.all_maxsize_jackson | 22KB | 26,224 ± 0.3% | 26,675 ± 0.2% | +1.7% ▲ |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_bytes | 22KB | 88,291 ± 0.2% | 86,799 ± 0.1% | -1.7% ▼ |

- **The main run measures the steady state**: the long runs agree with its 1 s iterations within 1.5%.
- **No drift**: throughput changes by at most a few tenths of a percent from the first to the last 10 s
  iteration; there is no late recompilation or GC effect.
- **6.0.8 and master are within 2%** on these four, as in the main run, where none of them changed.

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

Methods whose size crossed an inlining limit between 6.0.8 and master (`91da7f4` gives the same list,
except `JacksonJsonFilterFactory.newJsonFilter()`, which is construction only):

| Limit | 6.0.8 | master | Method | Effect |
|---|---|---|---|---|
| 325 | 324 | 359 | `MaxSizeFromEndJsonFilter.processMaxSizeFromEnd(byte[], ..)` | none in practice: it was compiled on its own before as well ("already compiled into a big method"); #307 |
| 325 | 320 | 326 | `ByteArrayRangesFilter.filter(byte[], .., JsonFilterMetrics)` | none in practice, as above; only with metrics (#314 moved `closeStructure(..)` in) |
| 325 | 307 | 348 | `JacksonMaxSizeMaxStringLengthJsonFilter.process(JsonParser, ..)` | none in practice, as above (#313) |
| 325 | 352–358 | 58 | `PrettyPrintingJsonFilter.process(.., JsonFilterMetrics)` | now a small wrapper around the loop (#314), inlined |
| 35 | 28–30 | 47–49 | `AbstractRangesJsonFilter.process(byte[], ..)`, `AbstractRangesPathJsonFilter.process(byte[], ..)` | inlined only when hot (#298's check for documents which are not an object or array) |
| 35 | 109–258 | 8–22 | `JacksonMaxSize*.getAccurate*Size`, `JacksonPathMaxSizeMaxStringLengthJsonFilter.anonymizeChildren` | now small delegates to `JacksonOutputSize` (#313) |

Crossing `FreqInlineSize` matters less than it seems here: a method that C2 has already compiled on its own into
more than `InlineSmallCode` (2500 bytes of machine code) is not inlined anyway, which is the case for these
scan methods in both versions (section 5, targeted logs).

### Compilation, inlining and deoptimization

From `-XX:+PrintCompilation -XX:+PrintInlining`, one JVM per benchmark at 22 KB with 3 warmup and 2
measurement iterations, for master and 6.0.8 (`campaign.sh jit_log`, summarized with `review/jit_report.py`).
Times are milliseconds since the JVM started; the benchmark starts at about 100 ms. Method names are shown
without parameters, so overloads share a row.

| Benchmark (22 KB) | Build | Largest C2-compiled method | Bytes | Its first C2 (ms) | Last library C2 (ms) | Library methods in C2 | C2 code discarded |
|---|---|---|---|---|---|---|---|
| CveFilter-all_core | master | AbstractPathJsonFilter::matchesEncoded | 790 | 159 | 232 | 25 | 1 (scanQuotedValue) |
| CveFilter-all_jackson | master | JacksonPathMaxStringLengthJsonFilter::process | 215 | 218 | 405 | 11 | 0 |
| CveFilter-all_maxsize_core | master | PathMaxSizeMaxStringLengthJsonFilter::ranges | 1191 | 195 | 753 | 29 | 1 (scanQuotedValue) |
| CveFilter-all_maxsize_jackson | master | JacksonPathMaxSizeMaxStringLengthJsonFilter::process | 288 | 336 | 537 | 16 | 0 |
| CveFilter-anon_any_core | master | AnyPathJsonFilter::rangesAnyPath | 567 | 172 | 227 | 12 | 0 |
| CveFilter-core_remove_whitespace | master | ByteArrayRangesFilter::scanQuotedValue | 294 | 145 | 195 | 5 | 1 (scanQuotedValue) |
| CveFilter-maxSize_core | master | ByteArrayRangesFilter::scanQuotedValue | 294 | 143 | 208 | 7 | 0 |
| CveFilter-maxStringLength_core | master | ByteArrayRangesFilter::getStringAlignment | 323 | 155 | 568 | 14 | 0 |
| CveMaxSizeFilter-core_keep_whitespace_bytes | master | ByteArrayRangesFilter::scanQuotedValue | 294 | 124 | 175 | 7 | 0 |
| CvePathMaxSizeMaxStringLengthFilter-core_keep_whitespace_bytes | master | PathMaxSizeMaxStringLengthJsonFilter::ranges | 1191 | 182 | 240 | 23 | 0 |
| CvePathMaxSizeMaxStringLengthFilter-core_remove_whitespace_bytes | master | PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter::processMaxSize | 1538 | 183 | 248 | 21 | 1 (scanQuotedValue) |
| CvePathMaxSizeMaxStringLengthFilter-jackson_bytes | master | JacksonPathMaxSizeMaxStringLengthJsonFilter::process | 288 | 323 | 486 | 17 | 0 |
| CveFilter-all_core | release | AbstractPathJsonFilter::matchesEncoded | 790 | 162 | 232 | 24 | 0 |
| CveFilter-all_jackson | release | JacksonMaxStringLengthJsonFilter::writeMaxStringLength | 136 | 247 | 468 | 8 | 0 |
| CveFilter-all_maxsize_core | release | PathMaxSizeMaxStringLengthJsonFilter::ranges | 1125 | 200 | 755 | 28 | 1 (scanQuotedValue) |
| CveFilter-all_maxsize_jackson | release | JacksonPathMaxSizeMaxStringLengthJsonFilter::process | 171 | 300 | 510 | 10 | 0 |
| CveFilter-anon_any_core | release | AnyPathJsonFilter::rangesAnyPath | 567 | 170 | 227 | 13 | 1 (scanQuotedValue) |
| CveFilter-core_remove_whitespace | release | ByteArrayRangesFilter::scanQuotedValue | 294 | 146 | 201 | 4 | 0 |
| CveFilter-maxSize_core | release | ByteArrayRangesFilter::scanQuotedValue | 294 | 148 | 205 | 6 | 0 |
| CveFilter-maxStringLength_core | release | ByteArrayRangesFilter::scanQuotedValue | 294 | 147 | 575 | 13 | 1 (scanQuotedValue) |
| CveMaxSizeFilter-core_keep_whitespace_bytes | release | ByteArrayRangesFilter::scanQuotedValue | 294 | 126 | 176 | 6 | 0 |
| CvePathMaxSizeMaxStringLengthFilter-core_keep_whitespace_bytes | release | PathMaxSizeMaxStringLengthJsonFilter::ranges | 1125 | 186 | 960 | 26 | 1 (scanQuotedValue) |
| CvePathMaxSizeMaxStringLengthFilter-core_remove_whitespace_bytes | release | PathMaxStringLengthMaxSizeRemoveWhitespaceJsonFilter::processMaxSize | 1501 | 181 | 244 | 22 | 1 (scanQuotedValue) |
| CvePathMaxSizeMaxStringLengthFilter-jackson_bytes | release | JacksonPathMaxSizeMaxStringLengthJsonFilter::process | 804 | 294 | 476 | 10 | 0 |

- **Everything hot reaches C2 within 0.12–0.34 s**: the core filters after about 0.15–0.2 s, the Jackson
  filters after about 0.3 s, and all library compilation is done within one second. No library method
  stays in the interpreter or in C1; methods without a C2 compilation of their own were inlined into a
  caller.
- **Deoptimization is rare**: at most one C2 compilation per benchmark is thrown away, nearly always
  `ByteArrayRangesFilter.scanQuotedValue` being recompiled once with a fuller profile, in both builds alike.
  There are no deoptimization loops.
- **Inlining**: the scan loops are compiled as units of their own in both builds (callers report "hot method
  too big"), which is fine for loops this size. Their helpers sit near the limits:
  `ByteArrayRangesFilter.scanQuotedValue` (294 bytes), `scanEscapedValue` (279), `getStringAlignment` (323)
  and `addMaxLength` (83) are inlined only where C2 considers the call hot. The few size changes that crossed a
  limit are in the table above.
- **Jackson**: `JacksonEvents.copyCurrentEvent(..)` (45 bytes, added in #287 to copy numbers exactly) is not
  inlined into the Jackson loops ("already compiled into a big method"), as its compiled code includes
  Jackson's whole `copyCurrentEvent`. That is one extra call per token; checking for numbers in the loop and
  calling the generator directly otherwise would avoid it.

**The regressions, targeted.** `jit_log` for `TmpMaxSizeFromEndBenchmark.from_end_bytes` (22 KB) and the
two `CveMaxStringLengthMaxSizeFilterBenchmark` char[] benchmarks, for `91da7f4` and master:

- `processMaxSizeFromEnd(byte[], ..)` is compiled on its own in both: before, its call site reports
  "already compiled into a big method" (324 bytes); on master, "hot method too big" (359 bytes). Neither is
  inlined, and both reach C2 at about 125 ms, after an OSR compilation of the loop.
- `MaxStringLengthMaxSizeJsonFilter.rangesMaxSizeMaxStringLength` and
  `MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter.processMaxStringLengthMaxSize` are compiled on their own in
  both, with `scanQuotedValue` and `addMaxLength` inlined where hot, in both.

So the slowdowns are not missed compilations or inlining: the same methods are compiled the same way, and the
generated code for the changed loops is slower. Finding out exactly why needs the generated assembly
(`-XX:+PrintAssembly`, JITWatch), which this campaign did not do.

## 6. Experiments: accuracy and metrics against speed

Each experiment is a local branch on top of master, not pushed, measured against master with 3 interleaved
forks. Library code on master is unchanged.

### F1. The cost of metrics

No benchmark passed metrics before. `review/TmpMetricsBenchmark.java` runs eight filters with metrics off and
on (`DefaultJsonFilterMetrics`) on byte[] input, with max size at half the smallest document, so the max-size
filters cut every document. 3 forks; the cost is the throughput lost with metrics on.

| Filter | Document | metrics off ops/s | metrics on ops/s | Cost |
|---|---|---|---|---|
| anyPath | 22KB | 123,494 ± 1.2% | 122,754 ± 1.1% | +0.6% |
| anyPath | 2KB | 854,750 ± 1.2% | 852,797 ± 1.2% | +0.2% |
| jacksonPathMaxSize | 22KB | 27,055 ± 1.4% | 26,879 ± 1.1% | +0.6% |
| jacksonPathMaxSize | 2KB | 186,274 ± 1.4% | 185,974 ± 0.8% | +0.2% |
| maxSize | 22KB | 294,800 ± 0.4% | 294,358 ± 0.3% | +0.1% |
| maxSize | 2KB | 1,895,808 ± 0.4% | 1,896,338 ± 0.8% | -0.0% |
| maxSizeRemoveWhitespace | 22KB | 275,823 ± 0.4% | 274,306 ± 0.7% | +0.5% |
| maxSizeRemoveWhitespace | 2KB | 1,755,184 ± 0.4% | 1,760,965 ± 0.3% | -0.3% |
| maxStringLength | 22KB | 155,977 ± 0.9% | 155,852 ± 0.9% | +0.1% |
| maxStringLength | 2KB | 1,059,407 ± 1.5% | 1,058,092 ± 1.5% | +0.1% |
| path | 22KB | 111,225 ± 1.3% | 110,893 ± 1.3% | +0.3% |
| path | 2KB | 848,887 ± 1.2% | 851,789 ± 1.2% | -0.3% |
| pathMaxSize | 22KB | 103,697 ± 1.6% | 103,469 ± 1.6% | +0.2% |
| pathMaxSize | 2KB | 808,779 ± 1.2% | 799,874 ± 1.5% | +1.1% |
| pathMaxSizeRemoveWhitespace | 22KB | 93,436 ± 0.4% | 93,918 ± 0.4% | -0.5% |
| pathMaxSizeRemoveWhitespace | 2KB | 720,419 ± 1.0% | 712,312 ± 1.3% | +1.1% |

**Metrics cost nothing measurable**: at most 1.1%, within the noise, for every filter and size. The core
filters record metrics once per document plus once per pruned, anonymized or truncated value, and the
Jackson filters likewise; there is nothing to gain from cheaper or less accurate metrics. This includes
`ByteArrayRangesFilter.filter(.., JsonFilterMetrics)`, which is 1 byte past the inlining limit since #314.

Two findings about the metrics themselves, not their speed:

- **The max-size metric means different things in the two frameworks.** The Jackson max-size filters record
  `onMaxSize(-1)`, "unknown count" per the `JsonFilterMetrics` Javadoc, which makes
  `DefaultJsonFilterMetrics.getMaxSize()` go down. The core filters record the number of characters or bytes
  dropped, while the Javadoc says "number of values truncated".
- **`DefaultJsonFilterMetrics` counts in `int`s**, so a long-lived instance overflows after 2^31 characters or
  bytes of input or output, about 2 GB. Counting in `long`s would avoid that.

### F2. Jackson: estimate the token size without the parser location

The Jackson max-size filters estimate the size of each token from the input offset, which takes
`parser.currentLocation()` through a `LongSupplier` per token, and compute the exact size only near the limit.
The experiment (`exp/jackson-token-estimate`, in `JacksonMaxSizeJsonFilter` and
`JacksonMaxSizeMaxStringLengthJsonFilter`) estimates from the token instead: text length plus quotes and
separators. That is a bit less accurate, as escapes and multi-byte characters are only counted once the
estimate reaches the limit. 3 forks, with `-prof gc`.

| Benchmark | Document | master ops/s | f2 ops/s | Change |
|---|---|---|---|---|
| CveFilterBenchmark.maxSizeJackson | 200KB | 2,309 ± 1.1% | 2,290 ± 0.8% | -0.8% |
| CveFilterBenchmark.maxSizeJackson | 22KB | 19,518 ± 0.6% | 19,269 ± 0.6% | -1.3% ▼ |
| CveFilterBenchmark.maxStringLengthMaxSize_jackson | 200KB | 1,888 ± 0.7% | 1,899 ± 1.0% | +0.6% |
| CveFilterBenchmark.maxStringLengthMaxSize_jackson | 22KB | 16,379 ± 1.9% | 16,256 ± 1.8% | -0.8% |
| CveMaxSizeFilterBenchmark.jackson_bytes | 200KB | 4,546 ± 0.6% | 4,472 ± 0.7% | -1.6% ▼ |
| CveMaxSizeFilterBenchmark.jackson_bytes | 22KB | 36,936 ± 1.0% | 37,251 ± 1.3% | +0.9% |
| CveMaxSizeFilterBenchmark.jackson_char | 200KB | 4,180 ± 2.5% | 4,181 ± 0.5% | +0.0% |
| CveMaxSizeFilterBenchmark.jackson_char | 22KB | 34,462 ± 0.4% | 34,677 ± 0.7% | +0.6% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_bytes | 200KB | 3,890 ± 1.0% | 3,802 ± 4.3% | -2.3% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_bytes | 22KB | 32,734 ± 0.6% | 33,072 ± 2.0% | +1.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_char | 200KB | 3,504 ± 1.0% | 3,625 ± 1.1% | +3.5% ▲ |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_char | 22KB | 29,959 ± 1.7% | 30,671 ± 0.6% | +2.4% ▲ |

**No gain**: within ±3.5%, and the allocation per operation (`-prof gc`) is identical to the byte. C2 inlines
the `LongSupplier` lambdas and `currentLocation()`, and escape analysis removes the location object, so the
per-token offset costs next to nothing. The exact size computation from #313 only runs near the limit, so a
less accurate size estimate has nothing to save either.

### F3. Whitespace max-size filters without the per-value fit check

Since #315, the whitespace max-size filters check that a string value fits before the whitespace after it is
credited. The experiment (`exp/ws-no-value-fit-check`) removes the check, so the output may exceed max size by
one value: the cheapest possible accuracy, as a control. It also tests whether the check is what slowed down the
max-string-length max-size char[] filters (section 2). 3 forks.

| Benchmark | Document | master ops/s | f3 ops/s | Change |
|---|---|---|---|---|
| CveFilterBenchmark.maxSize_core_remove_whitespace | 200KB | 14,682 ± 0.2% | 14,664 ± 0.4% | -0.1% |
| CveFilterBenchmark.maxSize_core_remove_whitespace | 22KB | 136,018 ± 0.5% | 136,471 ± 0.6% | +0.3% |
| CveFilterBenchmark.maxSize_core_remove_whitespace | 2KB | 1,362,329 ± 0.2% | 1,364,763 ± 0.1% | +0.2% |
| CveFilterBenchmark.maxStringLengthMaxSize_remove_whitespace_core | 200KB | 11,622 ± 0.8% | 11,967 ± 0.9% | +3.0% ▲ |
| CveFilterBenchmark.maxStringLengthMaxSize_remove_whitespace_core | 22KB | 96,090 ± 1.2% | 96,996 ± 0.8% | +0.9% |
| CveFilterBenchmark.maxStringLengthMaxSize_remove_whitespace_core | 2KB | 1,108,547 ± 1.9% | 865,390 ± 0.5% | -21.9% ▼ |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_bytes | 200KB | 24,322 ± 0.6% | 24,383 ± 0.4% | +0.2% |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_bytes | 22KB | 227,310 ± 0.4% | 226,921 ± 0.4% | -0.2% |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_bytes | 2KB | 1,508,922 ± 0.4% | 1,505,553 ± 0.3% | -0.2% |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_char | 200KB | 17,447 ± 1.2% | 17,766 ± 1.0% | +1.8% |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_char | 22KB | 164,946 ± 1.1% | 168,370 ± 2.2% | +2.1% |
| CveMaxSizeFilterBenchmark.core_remove_whitespace_char | 2KB | 1,544,752 ± 1.1% | 1,550,367 ± 1.2% | +0.4% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_bytes | 200KB | 20,623 ± 0.7% | 20,995 ± 0.6% | +1.8% ▲ |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_bytes | 22KB | 197,132 ± 0.6% | 200,107 ± 0.7% | +1.5% ▲ |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_bytes | 2KB | 1,602,714 ± 1.1% | 1,604,334 ± 0.9% | +0.1% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_char | 200KB | 15,640 ± 0.2% | 15,613 ± 1.2% | -0.2% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_char | 22KB | 138,489 ± 20.6% | 137,757 ± 9.3% | -0.5% |
| CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_char | 2KB | 1,224,180 ± 0.5% | 1,221,176 ± 1.2% | -0.2% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_bytes | 200KB | 8,422 ± 0.2% | 8,444 ± 0.3% | +0.3% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_bytes | 22KB | 86,678 ± 0.4% | 86,038 ± 0.3% | -0.7% ▼ |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_bytes | 2KB | 675,624 ± 0.3% | 676,445 ± 0.5% | +0.1% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_char | 200KB | 5,919 ± 0.3% | 5,866 ± 0.6% | -0.9% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_char | 22KB | 56,244 ± 1.1% | 56,054 ± 0.3% | -0.3% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.core_remove_whitespace_char | 2KB | 457,584 ± 0.5% | 464,308 ± 0.9% | +1.5% ▲ |

**No gain, and not the trigger.** Removing the check changes nothing beyond ±3%, including
`CveMaxStringLengthMaxSizeFilterBenchmark.core_remove_whitespace_char` at 2 KB, which section 2 found 23%
slower than before the review series. The exception points the other way: without the check,
`CveFilterBenchmark.maxStringLengthMaxSize_remove_whitespace_core` gets 22% slower at 2 KB. A comparison per
string value costs nothing in itself, but removing it changes the generated loop, which is the same
sensitivity as in section 2. Keep the check.

### F4. Jackson: no 16K-character builder per call

Five Jackson filters allocate a `StringBuilder` of at least 16K characters (about 32 KB) on every call,
for truncating strings, also when nothing is truncated; 6.0.8 does the same. The experiment
(`exp/jackson-small-builder`) sizes it for one truncated string: `new StringBuilder(maxStringLength + 11 +
truncateStringValue.length + 2)` instead of `new StringBuilder(Math.max(16 * 1024, ..))`. 3 forks, `-prof gc`.

| Benchmark | Document | master ops/s | f4 ops/s | Change |
|---|---|---|---|---|
| CveFilterBenchmark.all_jackson | 22KB | 29,061 ± 3.2% | 29,515 ± 0.2% | +1.6% |
| CveFilterBenchmark.all_jackson | 2KB | 194,693 ± 1.1% | 220,701 ± 1.9% | +13.4% ▲ |
| CveFilterBenchmark.all_maxsize_jackson | 22KB | 26,331 ± 2.0% | 27,436 ± 0.3% | +4.2% ▲ |
| CveFilterBenchmark.all_maxsize_jackson | 2KB | 180,414 ± 3.4% | 207,641 ± 1.7% | +15.1% ▲ |
| CveFilterBenchmark.maxStringLengthMaxSize_jackson | 22KB | 16,463 ± 1.7% | 16,268 ± 1.6% | -1.2% |
| CveFilterBenchmark.maxStringLengthMaxSize_jackson | 2KB | 145,739 ± 0.8% | 158,380 ± 2.3% | +8.7% ▲ |
| CveFilterBenchmark.maxStringLength_jackson | 22KB | 18,649 ± 1.9% | 18,836 ± 0.8% | +1.0% |
| CveFilterBenchmark.maxStringLength_jackson | 2KB | 126,862 ± 0.5% | 140,044 ± 2.0% | +10.4% ▲ |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_bytes | 22KB | 33,010 ± 1.4% | 33,862 ± 1.5% | +2.6% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_bytes | 2KB | 188,383 ± 1.3% | 211,517 ± 1.7% | +12.3% ▲ |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_char | 22KB | 30,110 ± 1.1% | 30,369 ± 1.0% | +0.9% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_char | 2KB | 186,084 ± 0.6% | 204,817 ± 0.4% | +10.1% ▲ |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.jackson_bytes | 22KB | 26,162 ± 0.9% | 26,809 ± 2.2% | +2.5% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.jackson_bytes | 2KB | 178,506 ± 1.1% | 201,515 ± 0.9% | +12.9% ▲ |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.jackson_char | 22KB | 22,665 ± 1.5% | 23,544 ± 0.6% | +3.9% ▲ |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.jackson_char | 2KB | 162,054 ± 1.3% | 178,601 ± 1.2% | +10.2% ▲ |

Allocation per operation (one document):

| Benchmark | Document | master B/op | f4 B/op | Change |
|---|---|---|---|---|
| CveFilterBenchmark.all_jackson | 22KB | 19,117 | 2,835 | -85.2% |
| CveFilterBenchmark.all_jackson | 2KB | 18,720 | 2,424 | -87.1% |
| CveFilterBenchmark.all_maxsize_jackson | 22KB | 19,168 | 2,872 | -85.0% |
| CveFilterBenchmark.all_maxsize_jackson | 2KB | 18,784 | 2,488 | -86.8% |
| CveFilterBenchmark.maxStringLengthMaxSize_jackson | 22KB | 21,424 | 5,128 | -76.1% |
| CveFilterBenchmark.maxStringLengthMaxSize_jackson | 2KB | 18,824 | 2,528 | -86.6% |
| CveFilterBenchmark.maxStringLength_jackson | 22KB | 21,480 | 5,184 | -75.9% |
| CveFilterBenchmark.maxStringLength_jackson | 2KB | 19,160 | 2,880 | -85.0% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_bytes | 22KB | 43,064 | 26,768 | -37.8% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_bytes | 2KB | 21,808 | 5,512 | -74.7% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_char | 22KB | 42,968 | 26,672 | -37.9% |
| CveMaxStringLengthMaxSizeFilterBenchmark.jackson_char | 2KB | 21,712 | 5,416 | -75.1% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.jackson_bytes | 22KB | 42,056 | 25,760 | -38.7% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.jackson_bytes | 2KB | 21,784 | 5,488 | -74.8% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.jackson_char | 22KB | 41,960 | 25,664 | -38.8% |
| CvePathMaxSizeMaxStringLengthFilterBenchmark.jackson_char | 2KB | 21,688 | 5,392 | -75.1% |

**A clear win**: every Jackson filter with max string length is 9–15% faster on 2 KB documents, and 1–4% at
22 KB, with 16 KB less garbage per document (75–87% of the allocation for small documents). The builder only
holds one truncated string at a time, so it can start small and grow when needed; allocating it lazily, on the
first truncation, would remove the rest.

### F5. The backward max-size scan, small enough to inline again

Section 2 traces the 31% regression of the backward scan on byte[] to `processMaxSizeFromEnd(byte[], ..)`
growing past 325 bytes. The experiment (`exp/from-end-inline`) moves the code after the scan loop, which runs
once per document, into its own method, so both scan methods (char[] and byte[]) are small enough to inline.

| Benchmark | Document | master ops/s | f5 ops/s | Change |
|---|---|---|---|---|
| TmpMaxSizeFromEndBenchmark.composite_bytes | 200KB | 45,139 ± 1.0% | 43,935 ± 0.8% | -2.7% ▼ |
| TmpMaxSizeFromEndBenchmark.composite_bytes | 22KB | 328,158 ± 0.3% | 432,751 ± 6.8% | +31.9% ▲ |
| TmpMaxSizeFromEndBenchmark.composite_bytes | 2KB | 2,362,620 ± 1.2% | 2,833,418 ± 12.4% | +19.9% ▲ |
| TmpMaxSizeFromEndBenchmark.composite_char | 200KB | 33,745 ± 0.5% | 29,970 ± 0.8% | -11.2% ▼ |
| TmpMaxSizeFromEndBenchmark.composite_char | 22KB | 324,970 ± 0.8% | 272,712 ± 2.7% | -16.1% ▼ |
| TmpMaxSizeFromEndBenchmark.composite_char | 2KB | 1,880,797 ± 0.3% | 1,850,037 ± 6.6% | -1.6% |
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 200KB | 45,480 ± 1.5% | 33,601 ± 1.2% | -26.1% ▼ |
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 22KB | 326,339 ± 1.1% | 473,546 ± 0.6% | +45.1% ▲ |
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 2KB | 2,376,542 ± 1.4% | 2,947,266 ± 7.6% | +24.0% ▲ |
| TmpMaxSizeFromEndBenchmark.from_end_char | 200KB | 33,950 ± 0.5% | 25,088 ± 1.6% | -26.1% ▼ |
| TmpMaxSizeFromEndBenchmark.from_end_char | 22KB | 323,912 ± 0.4% | 253,292 ± 5.1% | -21.8% ▼ |
| TmpMaxSizeFromEndBenchmark.from_end_char | 2KB | 2,031,255 ± 11.6% | 2,134,753 ± 14.4% | +5.1% |

**Mixed.** byte[] gets back to 6.0.8's speed at 22 KB (+45%) and gains at 2 KB, but loses 26% at 200 KB,
and char[] (which was not inlinable in 6.0.8 either) loses up to 26%. So inlining the scan into its caller is
not simply better: it helps small documents and hurts long scans. F5b and F5c below force and forbid the
inlining on unchanged code, to separate the inlining from the code change.

**F5b: master as is, against master with the method forced inline** (`-XX:CompileCommand=inline,..`), no
code change:

| Benchmark | Document | master ops/s | inline ops/s | Change |
|---|---|---|---|---|
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 200KB | 45,677 ± 0.7% | 31,601 ± 2.3% | -30.8% ▼ |
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 22KB | 328,076 ± 1.0% | 431,480 ± 15.7% | +31.5% ▲ |
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 2KB | 2,358,218 ± 1.0% | 2,548,632 ± 1.8% | +8.1% ▲ |
| TmpMaxSizeFromEndBenchmark.from_end_char | 200KB | 33,671 ± 0.6% | 27,941 ± 1.1% | -17.0% ▼ |
| TmpMaxSizeFromEndBenchmark.from_end_char | 22KB | 325,385 ± 0.7% | 247,210 ± 0.5% | -24.0% ▼ |
| TmpMaxSizeFromEndBenchmark.from_end_char | 2KB | 1,876,984 ± 0.5% | 2,223,145 ± 13.4% | +18.4% ▲ |

**F5c: 6.0.8 as is, against 6.0.8 with inlining of the method forbidden** (`-XX:CompileCommand=dontinline,..`):

| Benchmark | Document | release ops/s | dontinline ops/s | Change |
|---|---|---|---|---|
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 200KB | 46,129 ± 0.7% | 45,626 ± 1.3% | -1.1% |
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 22KB | 476,825 ± 1.7% | 489,491 ± 0.4% | +2.7% ▲ |
| TmpMaxSizeFromEndBenchmark.from_end_bytes | 2KB | 2,339,245 ± 1.2% | 3,204,262 ± 2.0% | +37.0% ▲ |
| TmpMaxSizeFromEndBenchmark.from_end_char | 200KB | 33,341 ± 0.2% | 33,834 ± 0.6% | +1.5% ▲ |
| TmpMaxSizeFromEndBenchmark.from_end_char | 22KB | 309,743 ± 2.5% | 317,626 ± 1.5% | +2.5% |
| TmpMaxSizeFromEndBenchmark.from_end_char | 2KB | 2,496,345 ± 1.5% | 2,478,279 ± 2.1% | -0.7% |

Forcing the inlining on master reproduces F5 (+31% at 22 KB, −31% at 200 KB for byte[], char[] slower), so
F5's effects come from the inlining, not from moving code. Forbidding the inlining on 6.0.8 makes it no slower
(it is 2.7% faster at 22 KB): 6.0.8 is fast without being inlined, as the JIT logs show it is not. **So the
regression is in the code C2 generates for master's scan method, not in the inlining.** Making the method
small does not help by itself; the next step would be to compare the generated assembly of 6.0.8's and
master's `processMaxSizeFromEnd(byte[], ..)` (`-XX:+PrintAssembly` or JITWatch) and change the loop until it
compiles as before, measured at 2, 22 and 200 KB for both input types.

## 7. Findings and recommendations

**Against 6.0.8, master is as fast or faster for most filters, with two real regressions, both in max-size
scan loops changed by the review series:**

1. **The backward max-size scan, byte[], is 31% slower at 22 KB** (`MaxSizeFromEndJsonFilter`, used by the
   default max-size filter for documents up to 1.5 times the max size). C2 compiles it the same way as
   before; the code added after the scan loop in #307 changed the generated loop code. F5, F5b and F5c show
   that inlining is not the cause: forcing it on master trades +31% at 22 KB for −31% at 200 KB, and 6.0.8 is
   as fast with inlining forbidden. Recommendation: compare the generated assembly of the two versions, change
   the loop until it compiles as before, and benchmark any change at 2, 22 and 200 KB, char[] and byte[].
2. **Max string length + max size, char[], is 19–23% slower at some sizes**
   (`MaxStringLengthMaxSizeJsonFilter`, `MaxStringLengthMaxSizeRemoveWhitespaceJsonFilter`), again with the
   same compilation and inlining as before. Removing #315's per-value check does not undo it (F3).
3. Smaller, 3–6%: some Jackson and whitespace filters at 2 KB, and the core filters' first call (+5–15%,
   about 50 µs once per process).

**Improvements:**

4. **Size the Jackson truncation builder for one string (F4): +9–15% for every Jackson filter with max string
   length on 2 KB documents, and 16 KB less garbage per document.** A one-line change in five filters, and
   the clearest win of this campaign. Allocating it lazily would remove the rest.
5. The pretty printer is 1.5 to 2.8 times faster since #314 (a small wrapper around the loop); the same split
   may help other filters whose `process(..)` holds both the loop and per-call work.
6. `JacksonEvents.copyCurrentEvent(..)` is not inlined into the Jackson loops; testing for numbers in the loop
   and calling the generator directly otherwise saves a call per token (not measured).

**Accuracy and metrics against speed: nothing to gain.**

7. Metrics cost at most 1.1%, within the noise (F1). A less accurate Jackson size estimate gains nothing (F2),
   as C2 already removes the cost of the parser location per token. Removing the whitespace filters'
   per-value fit check gains nothing either, and makes one benchmark 22% slower (F3). The correctness fixes of
   the review series cost nothing measurable in themselves; the cost shows up only as changed code
   generation in two scan loops.
8. Two issues with the metrics themselves: the max-size metric counts different things in core (characters
   dropped) and Jackson (-1, "unknown"), and `DefaultJsonFilterMetrics` overflows its `int` counters after
   about 2 GB.

**Correctness, from a separate parity check run before the campaign:** char[] and byte[] input give the same
result within each filter. 3.84 million comparisons covered every core and Jackson filter from the builders
and the pretty printer, all rules and features, every max size, whole documents and slices with prefilled
output, and the output plus all metrics. For ASCII input, including escapes, the two are identical. The only
differences are the Jackson filters decoding `\uXXXX` escapes into non-ASCII characters, whose byte sizes
legitimately differ. The same check found the max-size metric difference above.

**JIT:** no method is too big to compile; everything hot reaches C2 within 0.12–0.34 s of JVM start;
deoptimization is rare and identical in both builds. The first call costs 0.4–0.6 ms (core, 2 KB) and
14–19 ms (Jackson), and the core filters need hundreds of documents to reach full speed. The main risk is
the sensitivity of the scan loops' generated code to small changes, which only benchmarks across document
sizes and both input types catch.

**Long runs** (10 s iterations, 150 s per JVM) agree with the main run's 1 s iterations within 1%, with no
drift, so the main run's numbers are steady-state numbers.

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
