# Reflexion: iter-002-outer-quote-scan-vectorization

## What this iteration taught

**The outer quote-scan loop is NOT a good vectorization target for this benchmark.**

The pre-mortem hypothesis came true: the benchmark uses compact JSON where inter-string gaps are very short (1–5 bytes: `:`, `,`, `{`, `}`, whitespace). In that regime, `scanToQuote` almost always falls through to the scalar tail loop immediately, adding overhead (method call, `Math.min` bounds check, loop entry) without delivering the 8× iteration reduction that makes word-at-a-time worthwhile.

Key lesson: **word-at-a-time pays off only for long scans (≥ ~16-24 bytes of contiguous non-target bytes)**. The inner `scanQuotedValue` is a good target because JSON string bodies can be 10–200 bytes. The outer structural scan between strings is a bad target because structural characters are dense.

## Concrete lessons for next iteration

1. **Don't re-try outer scan vectorization in this form.** The benchmark JSON has dense structure; the inter-string gap is rarely > 8 bytes.

2. **`CharArrayRangesFilter.scanQuotedValue` is still using byte-by-byte scanning.** The char[] path was not touched in iter-001. The benchmark exercises both byte[] and char[] paths (`process(String, ...)` uses char[]). Applying the word-at-a-time trick to `CharArrayRangesFilter.scanQuotedValue` using a VarHandle on the underlying char[] memory (read 4 chars = 8 bytes as a long) could deliver a similar gain to iter-001. This requires a `byteArrayViewVarHandle`-equivalent for char[] — feasible via `MethodHandles.privateLookupIn` or treating a char[] as raw memory.

3. **Alternative for char[] scan:** For char[], the VarHandle approach requires reading 8 bytes covering 4 chars. The target pattern is `0x0022` (LE: `0x22 0x00`). The 2-byte zero detection trick: XOR with `0x0022002200220022L`, then apply `hasZeroShort` = `(x - 0x0001000100010001L) & ~x & 0x8000800080008000L`. This is a clean extension of the byte-level approach.

4. **`ByteArrayWhitespaceFilter.process`** was not touched and still has branch-heavy byte-by-byte scanning. It covers both the `core_remove_whitespace` benchmark and every whitespace-bearing filter. A vectorized version could impact multiple benchmark categories.

5. **Profile the actual benchmark data** to understand the distribution of string lengths and inter-string gaps before targeting the next optimization.

## Most promising next directions

- **`CharArrayRangesFilter.scanQuotedValue` word-at-a-time** — analogous to iter-001 for the char[] path (most promising: same fix, different path)
- **`ByteArrayWhitespaceFilter.process` SIMD** — impacts multiple benchmark categories
- **`AnyPathJsonFilter` inner path matching** — look at key comparison / path traversal overhead
