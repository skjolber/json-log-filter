# Iteration Plan: iter-003

## Strategy Name
word-at-a-time-char-quote-scan

## Step-Back: What type of bottleneck?
CPU-bound char scanning. The filter pipeline processes char[] JSON documents (the `process(String, ...)` code path).
The innermost hot loop in `CharArrayRangesFilter.scanQuotedValue` does scalar char-by-char scanning:
```java
while(chars[++offset] != '"');
```
This is the char[] equivalent of the byte[] hot loop fixed in iter-001.

## Profiling evidence
- iter-001 applied word-at-a-time to `ByteArrayRangesFilter.scanQuotedValue`, gaining +3.5%.
- The char[] path (`CharArrayRangesFilter`) was not touched. Both byte[] and char[] paths are exercised by the benchmark.
- `scanQuotedValue` in `CharArrayRangesFilter` is the primary callee for all char[] filters:
  `CharArrayWhitespaceFilter`, `CharArrayWhitespaceSizeFilter`, `CharArrayRangesSizeFilter`, etc.
- Reflexion from iter-002 explicitly identified this as the most promising next target.

## Approach: Manual 4-char word-at-a-time (4 × 2 = 8 bytes per iteration)

Java char[] cannot be read via `MethodHandles.byteArrayViewVarHandle` (that API is for byte[]).
Instead, we pack 4 consecutive chars into a `long` manually (zero-extension is free in Java for char):
```java
long word = ((long)chars[i]) |
            ((long)chars[i+1] << 16) |
            ((long)chars[i+2] << 32) |
            ((long)chars[i+3] << 48);
```
Then apply the Hacker's Delight "has-zero-short" trick to detect `'"'` (0x0022) in any of the 4 lanes:
```java
long x = word ^ 0x0022002200220022L;  // zero lanes that equal '"'
long y = (x - 0x0001000100010001L) & ~x & 0x8000800080008000L;
// y != 0 iff any 16-bit lane was '"'
// lane index = Long.numberOfTrailingZeros(y) >>> 4  (gives 0, 1, 2, or 3)
```

**Expected improvement:** ~4x reduction in iterations for strings > 4 chars. String scan is the dominant
inner loop; this directly mirrors the iter-001 benefit for the char[] path. Estimated: **2–6% gain**.

## Alternatives considered
1. **VarHandle on char[] backing bytes** — No standard API (only `byteArrayViewVarHandle(long[], …)` on
   `byte[]`). Could use Unsafe, but breaks portability and is unnecessary given option 3.
2. **Vector API (jdk.incubator.vector)** — 8–16 chars per step but requires module changes & adds
   compiler dependency; more risk, more code complexity for this iteration.
3. **Chosen: Manual 4-char packing + has-zero-short** — Standard Java, no imports needed, processes 4×
   more chars per iteration, directly parallelises the iter-001 insight to the char[] path.

## Self-consistency re-evaluation
- Manual packing: low risk (no new APIs), high readability, 4× iteration reduction
- VarHandle Unsafe: higher throughput potential but non-standard, fragile
- Vector API: best ceiling but module changes break isolation constraint
→ Manual packing is the right balance of correctness, portability, and expected gain.

## Pre-mortem
- **Most likely failure:** Char strings in the benchmark are short (< 4 chars), so the word-at-a-time
  path never executes. Early signal: benchmark throughput unchanged.
- **Mitigation:** If strings are short, revisit path. But reflexion from iter-002 noted string bodies
  are 10–200 bytes (5–100 chars), so this should fire.

## Files to modify
- `impl/core/src/main/java/com/github/skjolber/jsonfilter/core/util/CharArrayRangesFilter.java`
  - Add static constant fields (QUOTE_MASK_CHAR, MAGIC1_CHAR, MAGIC2_CHAR)
  - Replace `scanQuotedValue(char[], int)` with word-at-a-time version
  - Replace `scanEscapedValue(char[], int)` inner loop with word-at-a-time version
