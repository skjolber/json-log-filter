# Iteration Plan: iter-002

## Strategy Name
outer-quote-scan-vectorization

## Step-Back: What type of bottleneck?
CPU-bound byte scanning. After iter-001 optimized the inner `scanQuotedValue` loop (finding the closing `"`), the outer loop in `MaxStringLengthJsonFilter.ranges(byte[], ...)` still scans one byte at a time looking for the opening `"` of each JSON string. This loop traverses all structural JSON content (brackets, braces, colons, commas, numbers, booleans) byte-by-byte.

## General principles for CPU-bound byte scanning on JDK 25 / ARM64
1. Word-at-a-time: process 8 bytes per iteration (VarHandle `ldr` on ARM64).
2. Hacker's Delight has-zero-byte trick: detect target byte in 64-bit word without branching.
3. The inner and outer scan loops both traverse bytes; the outer loop covers structural content (JSON overhead), which can be a large fraction of the document.

## Profiling evidence
- iter-001 gained +3.5% from vectorizing `scanQuotedValue` (closing quote scan).
- The outer loop in `MaxStringLengthJsonFilter.ranges(byte[], ...)`:
  ```java
  while(offset < limit) {
      if(chars[offset] != '"') { offset++; continue; }
      // ... handle string
  }
  ```
  scans ALL non-string content one byte at a time looking for the next opening `"`. For typical JSON with many keys, numbers, and structural chars, this loop processes the majority of bytes.
- reflexion.md from iter-001 explicitly identifies this as the highest remaining gain target.

## Chosen approach: `scanToQuote` helper method

Add a bounded word-at-a-time scanner `ByteArrayRangesFilter.scanToQuote(byte[], int, int)` that finds the next `"` in `[offset, limit)`:

```java
public static int scanToQuote(final byte[] chars, int offset, int limit) {
    final int safeEnd = Math.min(limit - 8, chars.length - 8);
    while (offset <= safeEnd) {
        long word = (long) LONG_LE.get(chars, offset);
        long x = word ^ QUOTE_MASK;
        long y = (x - MAGIC1) & ~x & MAGIC2;
        if (y != 0) {
            return offset + (Long.numberOfTrailingZeros(y) >>> 3);
        }
        offset += 8;
    }
    while (offset < limit && chars[offset] != '"') offset++;
    return offset;
}
```

Then refactor `MaxStringLengthJsonFilter.ranges(byte[], ...)` to use it:
```java
while(offset < limit) {
    offset = ByteArrayRangesFilter.scanToQuote(chars, offset, limit);
    if(offset >= limit) break;
    // chars[offset] == '"', handle string...
}
```

**Expected improvement:** Replaces N iterations (one per non-string byte) with N/8 iterations for the outer scan. For JSON with typical key/value/structural overhead, the outer loop processes ~50% of bytes. An 8× reduction in iteration count for that portion → ~25-35% reduction in outer-loop cycles → estimated **3-6%** total throughput improvement.

## Alternatives considered
1. **Apply to char[] path too** — `CharArrayRangesFilter.scanQuotedValue` still uses byte-by-byte. However, there's no standard `byteArrayViewVarHandle` for `char[]`, requiring unsafe APIs. Deferred to a later iteration.
2. **Apply to AnyPathJsonFilter outer loop** — Same pattern at lines 61 and 171. Broader change, more risk. Defer after validating approach in MaxStringLengthJsonFilter.
3. **Vector API (jdk.incubator.vector)** — Higher ceiling (16-32 bytes per iteration), but requires module config changes and more code. The VarHandle approach is cleaner for now.

## Self-consistency re-evaluation
Both evaluations agree: `scanToQuote` for byte[] in `MaxStringLengthJsonFilter` is the clearest, lowest-risk, highest-expected-value change for this iteration. The VarHandle constants are already in `ByteArrayRangesFilter`, making the implementation straightforward.

## Pre-mortem
- **Most likely failure:** JSON documents in the benchmark have very short inter-string gaps (e.g., compact `{"k":"v","k2":"v2"}`), so the outer loop almost never runs more than 2-3 byte-by-byte iterations before hitting `"`. In that case, the word-at-a-time scan adds overhead (bounds check + VarHandle call) without proportional savings.
- **Early signal:** Benchmark result < +1201 ops/sec improvement.
- **Mitigation:** If dropped, next focus is `CharArrayRangesFilter.scanQuotedValue` or `ByteArrayWhitespaceFilter.process`.

## Files to modify
- `impl/core/src/main/java/com/github/skjolber/jsonfilter/core/util/ByteArrayRangesFilter.java`
  - Add `scanToQuote(byte[], int, int)` static method
- `impl/core/src/main/java/com/github/skjolber/jsonfilter/core/MaxStringLengthJsonFilter.java`
  - Refactor `ranges(byte[], int, int, int, ByteArrayRangesFilter)` to use `scanToQuote`
