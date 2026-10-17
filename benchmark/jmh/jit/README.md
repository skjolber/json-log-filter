# JIT-guided performance work

Scripts for finding and checking performance changes on JDK 27 (and JDK 25, which CI uses): A/B benchmarks of a base commit against the working tree, JIT snapshots, and machine-code profiles. Results go to `benchmark/jmh/target/jit/`, which is ignored.

## The loop

Each iteration targets one benchmark configuration:

1. **Profile** the base: `PROF=perfnorm` or `PROF=perfasm` with `ab.sh`, or a JIT snapshot with `probe.sh`. Find where the time goes, and what HotSpot does differently from what the code intends.
2. **Hypothesis,** from the profile and the code. One idea per iteration.
3. **Change** in a worktree, as small as possible.
4. **A/B** with `ab.sh`: 3 forks per side, interleaved, at 2, 22 and 200 KB, char[] and byte[]. Use 6 forks where a benchmark is bimodal, and look at per-fork scores.
5. **Guard set:** the benchmarks of every other filter that uses the changed code, also A/B.
6. **Decide:** keep it if it is faster beyond the noise at one size or more, with no loss above about 2% in the guard set. Otherwise discard it and keep the notes.

## Tools

- **`ab.sh <name> <base-ref> <regex> [params]`:**
  - **Builds:** the base jar from a commit, cached in `target/jit/jars` (`JARS=<dir>` to share it between worktrees), and the candidate jar from the working tree. Never `mvn install`. `MVN=mvnd` uses the Maven Daemon, with one daemon per CPU set.
  - **New benchmarks on old commits:** benchmark classes which the working tree has and the base commit lacks are copied into the base build. The cached jar's name includes a hash of them. `BACKPORT=0` turns this off.
  - **Fork JVM:** `JVM_ARGS`, by default `-Xms1g -Xmx1g -XX:+AlwaysPreTouch`, so that forks don't differ in heap growth. `JAVA=<path>` picks the JVM, by default Temurin 27.
  - **Runs:** `FORKS` forks per side (default 3), interleaved, merged as JMH would merge them (`merge_forks.py`), and compared (`compare.py`) in `target/jit/<name>/comparison.md`.
  - **Profilers:** `PROF=gc` adds the allocation profiler, compared by `secondary.py`; `PROF=perfnorm` gives instructions, cycles and branches per operation; `PROF=perfasm` the hottest machine code, which needs hsdis (`HSDIS_DIR`, default `~/.local/share/hsdis`). Profilers cost time, so don't use them in timing runs.
  - **Compilation logs:** `LOG_COMPILATION=1` writes a HotSpot compilation log per fork, for comparing the JIT decisions of fast and slow forks.
  - **Pinning:** `CPUS=0,1,16,17` pins builds and forks with `taskset`, and lets several pinned runs use JMH at the same time.
- **`probe.sh <jar> <out> <config>...`:** a JIT snapshot with `JitProbe.java`, from a JFR recording: C2 compilations and their size, inlining decisions with HotSpot's reasons, deoptimizations, and sampled allocations. The output is sorted, so two builds can be compared with `diff -u`.
- **hsdis:** built from the JDK's `src/utils/hsdis` sources with the LLVM backend: `g++ -shared -fPIC -O2 -o hsdis-amd64.so -I. -I$JDK/include -I$JDK/include/linux -DLIBARCH_amd64 -DLIBARCH='"amd64"' -DLLVM_DEFAULT_TRIPLET="\"$(llvm-config --host-target)\"" hsdis-llvm.cpp $(llvm-config --cxxflags --ldflags --libs)`.

## What worked

- **Per-character loops without a limit** pay a bounds check, a safepoint poll and a loop test per character, and their speed depends on where the JIT places them: one such loop ran 20% slower only because it crossed a 64-byte boundary. Checking four characters per iteration fixed both (#326, #328, #334, #335). Where a limit is at hand, bounding the loop lets C2 count it instead (#336).
- **One branch profile, two loops:** HotSpot profiles branches per method, not per caller. When two hot loops inline the same helper, they are compiled from one mixed profile. A copy of the helper per loop separates them (#327).
- **Skip work the output doesn't need:** return the input when the filter changes nothing or the document fits (#330, #332, #333), and size byte[] output exactly so it can be returned without a copy (#329).
- **Reject early:** most keys match no any-path filter, and the first character tells (#331).

## What didn't

- **Eight characters per iteration:** faster for long runs of strings, slower in the filters' main loops, where most strings are short. Checking all eight with one combined branch was much slower.
- **byte[] tricks on top of the long-word (SWAR) quote search:** reading eight bytes at a time backward, unrolling it like char[], or a different zero-byte test. Each was faster almost everywhere, but lost 5–27% in one or two configurations, most often the whitespace filter, and which one changed between JDK 25 and 27.

## Measuring

- **Two JIT states:** some benchmarks land in a fast or a slow state per fork, 10–30% apart, with identical code. Use 6 forks and per-fork scores before deciding on a difference there.
- **Parallel runs:** pin each run to its own CPUs, at most one run per two real cores. Runs sharing a chiplet are noisier.
- **Waiting for a background run:** wait for its PID (`kill -0`), not for `pgrep -f <name>`, which matches the waiting shell itself.
