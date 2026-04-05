# Iteration Plan: iter-001

## Strategy Name
word-at-a-time-quote-scan

## Step-Back: What type of bottleneck?
CPU-bound byte scanning. The filter pipeline processes 8–22 KB JSON documents, traversing every byte to locate JSON string boundaries (i.e., find closing `"` after each opening `"`).

## General principles for CPU-bound byte scanning on JDK 25 / ARM64
1. Process multiple bytes per CPU instruction (SIMD or word-at-a-time).
2. Reduce loop iteration count → fewer branch mispredictions and fewer bounds checks.
3. JIT C2 cannot auto-vectorize data-dependent exit loops (`while(chars[i] != '"')`), so manual word-at-a-time is required.
4. `java.lang.invoke.VarHandle.byteArrayViewVarHandle(long[].class, LITTLE_ENDIAN)` creates an intrinsified unaligned 8-byte load — on ARM64 this compiles to a single `ldr` instruction.

## Profiling evidence
All benchmark methods call `process(byte[], ...)`, which flows through:
- `AbstractRangesJsonFilter.process(byte[], ...)` → `ranges(byte[], ...)` → `ByteArrayRangesFilter`
- The innermost hot loop is `ByteArrayRangesFilter.scanQuotedValue(byte[], offset)`:
  ```java
  while(chars[++offset] != '"');  // 1 byte per iteration
  ```
- Called from:
  - `ByteArrayRangesFilter.skipObject`, `skipArray`, `skipObjectOrArray`, `anonymizeObjectOrArray`, `skipObjectMaxStringLength`
  - `ByteArrayWhitespaceFilter.process`, `skipObjectMaxStringLength`, `anonymizeObjectOrArray`
  - `ByteArrayWhitespaceSizeFilter` methods
  - Via `scanBeyondQuotedValue` everywhere

## Approach: VarHandle word-at-a-time (8 bytes per iteration)

Using the Hacker's Delight "has-zero-byte" trick to find byte value `0x22` (`"`) in a 64-bit word:
```java
private static final VarHandle LONG_LE =
    MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
private static final long QUOTE_MASK = 0x2222222222222222L;

// For a word `w`, detect if any byte equals 0x22:
long x = w ^ QUOTE_MASK;          // zero out bytes that match '"'
long y = (x - 0x0101010101010101L) & ~x & 0x8080808080808080L;
// y != 0 iff at least one byte was '"'
// position: Long.numberOfTrailingZeros(y) >>> 3
```

**Expected improvement:** ~7–8x reduction in iterations for strings >8 bytes. For JSON with average string length ~10–20 bytes, this translates to ~30–50% reduction in cycles spent in `scanQuotedValue`. Net throughput gain estimate: **5–15%** depending on fraction of time spent in `scanQuotedValue`.

## Alternatives considered
1. **Vector API (jdk.incubator.vector)** — 16–32 bytes per iteration, but requires adding Vector API module to compiler config for `impl/core`, and complex setup. Higher overhead for short strings.
2. **Loop unrolling (4× scalar)** — Reduces loop overhead but doesn't eliminate bounds checks; JIT would need to prove bounds. Likely less effective than VarHandle.
3. **Chosen: VarHandle getLong** — Standard Java 9+ API, intrinsified by HotSpot to single `ldr` on ARM64, no dependency changes, processes 8× more bytes per iteration.

## Self-consistency re-evaluation
- VarHandle approach: clean, standard API, directly reduces iteration count by 8×
- Vector API: higher ceiling but more risk (compiler config change, potential JIT overhead for short strings)
- Unrolling: marginal improvement, more code complexity
→ VarHandle approach is the clear winner for this iteration

## Pre-mortem (if this strategy failed)
- Most likely failure: most strings in the benchmark data are very short (<8 bytes), meaning the vectorized path never executes and we only add overhead from the bounds check `i <= end`.
- Early signal: test benchmarks with `-verbose:class` to check if VarHandle is loaded; verify by running with small string lengths.
- Mitigation for next iteration: if strings are short, focus on the outer scan loop (searching for the opening `"`) which spans longer non-string regions.

## Files to modify
- `impl/core/src/main/java/com/github/skjolber/jsonfilter/core/util/ByteArrayRangesFilter.java`
  - Add static VarHandle field
  - Replace `scanQuotedValue(byte[], int)` with word-at-a-time version
