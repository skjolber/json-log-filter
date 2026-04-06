# iter-003-arrays-mismatch

## Strategy
Replace the manual element-wise comparison loop in `AbstractPathJsonFilter.matchPath(char[]/byte[])`
with `Arrays.mismatch()` — a JDK intrinsic that HotSpot SIMD-accelerates on AMD64.

The manual loop with an early-exit (`return false` inside) cannot be auto-vectorized.
`Arrays.mismatch` avoids this limitation.

## Result
- **iter3 total: 19,119,862 ops/s**
- vs iter2 (21,267,629): **-10.1% — REGRESSION**
- vs master: +45.8%

## Key per-variant changes vs iter2

| Variant | iter2 | iter3 | Δ |
|---------|-------|-------|---|
| MultiPath.chars count=1 | 2,346,770 | 889,292 | -62.1% ❌ |
| StarMultiPath.chars count=1 | 1,861,470 | 861,751 | -53.7% ❌ |
| AnyPath.bytes count=1 | 531,365 | 745,504 | +40.3% ✅ |
| AnyPath.chars count=1 | 659,449 | 976,191 | +48.0% ✅ |

## Root cause of regression
The iter2 count=1 bypass calls `matchPath` on ALL ~100 Shopify field names per parse (no
pre-filtering). For the 99% that don't match, `matchPath` returns after a cheap length check.
The `Arrays.mismatch` intrinsic has per-call setup overhead that dominates for short strings
(2-8 chars typical) and nullifies the SIMD benefit.

AnyPath benefited because its 3D dispatch pre-filters to candidates with matching length AND
first byte, so `matchPath` is only called on genuine candidates — the SIMD speedup on actual
comparisons outweighs setup overhead.

## Lesson for iter4
`Arrays.mismatch` helps when comparisons are between near-matches (same length, many chars to
compare). It hurts when called on many non-matching-length keys. The Multi/StarMultiPath
single-entry bypass must keep the fast manual loop.

Alternative: apply `Arrays.mismatch` selectively, only inside AnyPath's exact match path
(not in the globally-shared `matchPath` static method). But that adds complexity.

## Decision
**DROP** — revert `AbstractPathJsonFilter.java`.
