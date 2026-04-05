# Reflexion: iter-004-whitespace-filter-branch-reduction

## What this iteration taught

**Reordering branches in `ByteArrayWhitespaceFilter.process()` hurt by -2.7%.**

The CVE benchmark JSON is **compact/minified** (0 external whitespace, all spaces inside quoted strings). This means:
- The `c <= 0x20` outer-loop branch fires **0 times** during the benchmark
- The `do { offset++ } while(chars[offset] <= 0x20)` inner loop runs **0 times**
- The outer loop alternates between `c == '"'` (calls `scanBeyondQuotedValue`) and structural bytes (1–2 per string)

The "fast path `c > '"'`" restructuring added one extra comparison per quote encounter (changing from 1-compare quote path to 2-compare quote path). Quote encounters are ~40–50% of outer loop iterations, and this slight overhead was enough to outweigh the saved comparison for structural bytes.

**The JIT C2 on JDK 25 / ARM64 is already highly effective at the original two-branch structure.** The original code may compile to a `csel`/`csinc` or `cmov`-style instruction sequence on ARM64 that avoids branch misprediction penalties. Restructuring the branches may disable this optimization.

## Concrete lessons for next iteration

1. **`ByteArrayWhitespaceFilter.process()` is NOT the right target for compact JSON.**  
   None of its whitespace-handling code runs on compact CVE JSON. The only effect is structural byte scan overhead which is dominated by `scanBeyondQuotedValue` call setup.

2. **The benchmark's 1-of-7 whitespace contribution is small.**  
   Optimizing the whitespace filter only affects ~14% of the benchmark score. Even a 2× speedup of `core_remove_whitespace` alone would add only ~(current_whitespace_score / 7) ≈ 1,400–2,000 ops/sec to the average.

3. **Outer scan loops resist optimization for compact CVE JSON.**  
   Both iter-002 (quote scan in MaxStringLengthJsonFilter) and iter-004 (branch reorder in process()) confirmed that the inter-string structural spans (1–3 bytes) are too short for word-at-a-time or branch tricks to pay off.

4. **The remaining 60–75% of runtime is in `scanBeyondQuotedValue` orchestration overhead, output writing, and JVM overhead.**  
   After iter-001's `scanQuotedValue` word-at-a-time win (+3.5%), the low-hanging fruit is gone. The remaining gains require different classes of optimization.

## Most promising next directions

- **`sun.misc.Unsafe` or `VarHandle` on `char[]` for the `maxStringLengthMaxSize_core` benchmark path** — the only char[] benchmark (1/7). `benchmarkCharacters()` uses `CharArrayRangesFilter`. Using raw memory reads (`Unsafe.getLong`) on char[] backing would give single-instruction word load (no manual 4-char packing as failed in iter-003).

- **`ByteArrayRangesSizeFilter` outer scan optimization** — the `maxSize_core` benchmark uses a size-limiting filter with its own scan loop. This filter has a *different* scan structure from `ByteArrayRangesFilter` and may not have received the word-at-a-time treatment. Check `ByteArrayRangesSizeFilter` and its scan methods.

- **Output batching / write coalescing** — for filters that call `output.write()` many times (like `filter()` methods with many ranges), each `write` call is a `System.arraycopy`. Batching into fewer, larger copies could reduce overhead.

- **`AnyPathJsonFilter` / `FullPathJsonFilter` key-matching optimization** — `anon_any_core` and `anon_full_core` (2/7 benchmarks) traverse JSON and match against key paths. The key-comparison loop may be hot. If key strings are short (most are), the comparison overhead dominates.
