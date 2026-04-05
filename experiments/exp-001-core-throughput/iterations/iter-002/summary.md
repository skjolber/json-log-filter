# Summary: iter-002-outer-quote-scan-vectorization

## Result
**122,804 ops/sec**

## Delta vs Current Best (iter-001: 124,295)
**-1,491 ops/sec (-1.2%)** — REGRESSION

## Delta vs Baseline (120,093)
**+2,711 ops/sec (+2.3%)** — still above baseline, but below current best

## Decision
❌ **Dropped** — result did not meet the minimum +1,201 ops/sec improvement over current best

## Strategy Description
Added `ByteArrayRangesFilter.scanToQuote(byte[], int, int)` — a word-at-a-time (VarHandle 8-byte read + Hacker's Delight has-zero-byte trick) scanner for the next `"` in a bounded range. Refactored `MaxStringLengthJsonFilter.ranges(byte[], int, int, int, ByteArrayRangesFilter)` to call `scanToQuote` instead of incrementing byte-by-byte through the outer structural scan loop.

## Why It Regressed
The overhead of calling `scanToQuote` (method dispatch, bounds computation `Math.min(limit-8, chars.length-8)`, VarHandle warm-up path) is not amortized when inter-string gaps are short (typically 1–5 bytes between structural chars like `:`, `,`, `{`, `}` and the next `"`). The word-at-a-time benefit only pays off when scanning over many consecutive non-quote bytes; for compact JSON, the scalar fallback path in `scanToQuote` runs almost every call.
