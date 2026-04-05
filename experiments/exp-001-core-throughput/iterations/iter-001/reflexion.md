# Reflexion: iter-001-word-at-a-time-quote-scan

## What this iteration taught

**+3.5% throughput** from replacing the byte-by-byte `scanQuotedValue` loop with a VarHandle 8-byte word scan.

- The gain is real but modest (3.5% vs. ~8× loop reduction estimate). This suggests `scanQuotedValue` accounts for roughly 25–40% of total runtime — the rest is spent elsewhere (outer scanning loops, JSON structural parsing, output writing).
- VarHandle `byteArrayViewVarHandle` works cleanly with Java 17 source level and JDK 25 runtime — no module or compiler config needed.
- The improvement is consistent across all filter types since `scanQuotedValue` is a shared utility called by every filter.

## Concrete lessons for next iteration

1. **The outer scan loop is also a target.** In `MaxStringLengthJsonFilter.ranges(byte[], ...)` and similar filters, the outer loop iterates byte-by-byte looking for `"`:
   ```java
   while(offset < limit) {
       if(chars[offset] != '"') { offset++; continue; }
   ```
   This scan over non-string regions (structural characters, booleans, numbers) is equally hot and not yet vectorized. A word-at-a-time scan for `"` in this loop could yield another 5–10%.

2. **`ByteArrayWhitespaceFilter.process`** scans all bytes for whitespace (≤ 0x20) and quotes simultaneously. This is a branch-heavy loop that runs for every whitespace-removal filter. A vectorized `"` + whitespace detector could compress two passes into one.

3. **Look at the `char[]` path too.** `CharArrayRangesFilter.scanQuotedValue` still uses the byte-by-byte loop (`while(chars[++offset] != '"')`). Applying the same word-at-a-time idea using a `ShortVector` or char-pair masking could help the `char[]`-based benchmarks.

4. **Remaining headroom:** Need ~+7,807 more ops/sec to hit the 132,102 target (+10%). Multiple smaller wins (outer scan, whitespace filters, char path) could combine to reach it.

## Unexplored promising directions

- **Outer quote-scan loop vectorization** in `MaxStringLengthJsonFilter`, `AnyPathJsonFilter`, `FullPathJsonFilter` — likely highest remaining gain
- **`ByteArrayWhitespaceFilter.process` SIMD** for the remove-whitespace benchmarks
- **`CharArrayRangesFilter.scanQuotedValue`** word-at-a-time using long reads over char[] raw memory
- **`ByteArrayRangesSizeFilter`** — has its own scanning loops that may not benefit from the `ByteArrayRangesFilter` changes
