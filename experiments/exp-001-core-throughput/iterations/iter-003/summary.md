# Summary: iter-003-word-at-a-time-char-quote-scan

## Result
- **Measured:** 120,940 ops/sec
- **Delta vs current best (124,295):** −3,355 ops/sec (−2.7%)
- **Delta vs baseline (120,093):** +847 ops/sec (+0.7%)
- **Decision:** ❌ Dropped

## Strategy
Applied word-at-a-time scanning to `CharArrayRangesFilter.scanQuotedValue` and
`scanEscapedValue` (char[] path), mirroring the iter-001 byte[] optimization.
Packs 4 consecutive `char` values into a `long` using explicit shifts, then applies
the Hacker's Delight "has-zero-short" trick to detect `'"'` (0x0022) in any of the
4 x 16-bit lanes, falling back to scalar on a match.

## Why it regressed
The manual 4-char packing (`((long)chars[i]) | ((long)chars[i+1] << 16) | …`) incurs
4 array loads + 3 shifts + 3 ORs per iteration. Unlike the byte[] approach which used a
single VarHandle intrinsic (`ldr` on ARM64), there is no JVM API to read a long from a
char[] directly. The JIT C2 did not coalesce the 4 separate char loads into a vectorized
word read, so the overhead of the packing exceeded the gain from reducing iterations.
