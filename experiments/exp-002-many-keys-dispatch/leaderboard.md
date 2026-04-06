# Experiment Leaderboard: exp-002-many-keys-dispatch
Last updated: 2026-04-06

| Rank | Iteration | Total ops/s | Delta vs master | Status |
|------|-----------|-------------|----------------|--------|
| 1 | iter-002-chars-bypass | 21,267,629 | +62.2% | ✅ Current best |
| 2 | branch-initial | 20,388,016 | +55.5% | ✅ Kept |
| — | baseline (master) | 13,112,215 | — | Baseline |
| — | iter-005-anypath-single-bypass | 21,160,959 | +61.4% | ❌ Dropped (-0.5% vs iter2) |
| — | iter-004-anypath-mismatch | 18,825,351 | +43.6% | ❌ Dropped (-11.5% vs iter2, JIT noise) |
| — | iter-003-arrays-mismatch | 19,119,862 | +45.8% | ❌ Dropped (-10.1% vs iter2) |
| — | iter-001-single-entry-bypass | — | — | ❌ Dropped (byte[] regressed badly) |

## Summary of kept changes
1. **branch-initial**: First-byte dispatch table in `MultiPathItem`, `StarMultiPathItem`, `AnyPathFilters` (+55.5% vs master)
2. **iter-002**: Chars-only single-entry bypass in `MultiPathItem`/`StarMultiPathItem` (+4.3% on top, fixed count=1 regression)

## Final result vs master (iter2, 30-variant total)
| Benchmark group | Delta vs master |
|----------------|----------------|
| MultiPath bytes | +141% to +360% |
| MultiPath chars | +44% to +144% |
| StarMultiPath bytes | +141% to +376% |
| StarMultiPath chars | +27% to +156% |
| AnyPath bytes/chars | −6% to +8% (essentially neutral) |
