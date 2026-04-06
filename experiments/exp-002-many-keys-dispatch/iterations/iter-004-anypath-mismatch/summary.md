# iter-004-anypath-mismatch

## Strategy
Apply `Arrays.mismatch` only to `AnyPathFilters.unencodedMatchBytes` and `unencodedMatchChars`
(candidates pre-filtered by length+firstByte) to avoid the regression seen in iter3.

## Result
- **iter4 total: 18,825,351 ops/s**
- vs iter2 (21,267,629): **-11.5% — REGRESSION**
- vs master: +43.6%

## Root cause
`unencodedMatchBytes/Chars` is on the **matching path** — called only when the dispatch table
finds candidates with matching length AND first byte. In the Shopify benchmark ~99% of keys
don't match any filter, so these methods are almost never called. Optimising them has near-zero
impact on throughput.

The Multi/StarMultiPath regressions (-52% bytes count=1, -62% chars count=1) are JIT noise
from benchmark interference — the code change in AnyPathFilters could not logically affect those
benchmarks. This illustrates that individual run variance can be large when JIT compilation is
influenced by previously-run benchmarks in the same session.

## Decision
**DROP** — revert `AnyPathFilters.java`.
