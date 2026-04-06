# Experiment Leaderboard: exp-002-many-keys-dispatch
Last updated: 2026-04-06

## Combined Average ops/sec (30 benchmarks)

| Rank | Iteration | ops/sec | Delta vs Branch | Status |
|------|-----------|---------|-----------------|--------|
| 1 | **iter-001-single-entry-bypass** | 708,226 | **+4.2%** | ✅ **KEPT** |
| — | branch-initial (baseline) | 679,601 | — | Baseline |
| 2 | iter-005-loop-unroll | 704,111 | -0.6% | ❌ Dropped |
| 3 | iter-003-anypath-single-bypass | 635,808 | -6.4% | ❌ Dropped |
| 4 | iter-004-bytes-single-bypass | 566,951 | -16.6% | ❌ Dropped |
| — | iter-002-length-prefilter | — | — | ❌ Tests failed |

## Absolute Comparison to Master

| Metric | Master | Branch | Iter-001 (Final) |
|--------|--------|--------|------------------|
| Combined avg | 437,074 | 679,601 (+55.5%) | 708,226 (+62.0%) |

## Final Winning Strategy

**iter-001-single-entry-bypass**: Add single-filter bypass for char[] variants only.

For `MultiPathItem` and `StarMultiPathItem`, when `fieldNameChars.length == 1`, skip the dispatch table lookup and perform a direct comparison. Applied only to char[] methods, NOT byte[] (which showed regression).

Key improvements:
- MultiPath.chars count=1: +60% vs branch baseline
- StarMultiPath.chars count=1: +46% vs branch baseline

