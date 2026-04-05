# Reflexion: iter-005-inline-structural-byte-skip

## What this iteration taught

**Inlining ':' and ',' after short strings in `skipObjectOrArrayMaxSizeMaxStringLength` hurt by -0.6%.**

### Why the optimization was expected to work
The switch `switch(chars[offset])` with cases at 0x22, 0x2C, 0x5B, 0x5D, 0x7B, 0x7D
(range 0x22–0x7D, ~90 entries) was expected to compile to a 90-entry jump table. Inlining
the two most common cases (`:` and `,`) would replace a table lookup with two
`cmp+branch` instructions, saving 3–5 cycles per string encounter.

### Why it actually hurt
1. **JIT already eliminates sparse jump tables.** C2 on JDK 25 / ARM64 detects sparse
   switches and compiles them as a sequence of comparisons (`cmp+beq`), not a
   table. A 6-case switch becomes 6 comparisons — the same overhead as the inlined code.
2. **The bounds guard adds net overhead.** The required `if(offset < maxSizeLimit)`
   guard adds one comparison per short string that cannot be hoisted (because `maxSizeLimit`
   changes inside the loop). This extra comparison negates any savings.
3. **The optimization targets the wrong bottleneck.** Structural byte dispatching is
   ~10–15% of total loop work. The dominant cost remains the `scanBeyondQuotedValue`
   call and method call overhead, both of which are already highly optimized.

## Concrete lessons for next iteration

1. **JIT C2 eliminates sparse switches — do not try to beat it with manual inlining.**
   Any hand-optimization that the JIT already performs will add overhead from guards
   and added instruction count with no throughput benefit.

2. **The `ByteArrayRangesSizeFilter` structural loop offers no further micro-optimization.**
   After iter-001 vectorized the inner string scan, and this iteration confirmed that
   structural byte dispatch is already optimal, the `skipObjectOrArrayMaxSizeMaxStringLength`
   loop has no remaining single-digit-cycle savings to extract.

3. **`ByteArrayRangesSizeFilter` and `MaxStringLengthJsonFilter` hot paths are exhausted**
   for micro-optimizations. Both strategies A and B as described have been thoroughly
   investigated across iter-002 through iter-005.

## Most promising next directions

These are the remaining unexplored areas from the reflexion recommendations:

- **`AnyPathJsonFilter` key-matching path** — `anon_any_core` and `anon_full_core` are
  2/7 benchmark benchmarks. Their `rangesAnyPath` outer loop scans for `"` byte-by-byte
  (same pattern as iter-002), but the `matchPath` call is the unique per-key cost. If key
  names in CVE JSON are compared byte-by-byte in `AnyPathFilters.matchPath`, a bulk
  comparison (SWAR or `Arrays.mismatch`) might help. Check `base/` module's `AnyPathFilters`.

- **Output write coalescing in `ByteArrayRangesFilter.filter()`** — for filters with many
  truncation ranges (e.g., `maxStringLength_core` on a CVE doc with 20+ long strings),
  `buffer.write()` is called 3× per range. If `ResizableByteArrayOutputStream.write()`
  has synchronization or capacity-check overhead, batching ranges into fewer writes might
  help. Inspect `ResizableByteArrayOutputStream` in the `base` module.

- **`ByteArrayWhitespaceSizeFilter`** — `core_remove_whitespace` is 1/7 of the benchmark.
  While iter-004 showed whitespace path is dead for compact JSON, the whitespace filter
  may have a `scanBeyondQuotedValue` call that needs checking for VarHandle coverage.

- **Reducing allocations per invocation** — if `ByteArrayRangesFilter` or `ByteArrayRangesSizeFilter`
  instances are created per call, eliminating the allocation (thread-local or pool) might
  help. Check factory methods in `AbstractRangesJsonFilter`.
