# Experiment Leaderboard: exp-002-many-keys-dispatch
Last updated: 2026-04-06

| Rank | Iteration | Total ops/s | Delta vs master | Status |
|------|-----------|-------------|----------------|--------|
| 1 | iter-002-chars-bypass | 21,267,629 | +62.2% | ✅ Current best |
| 2 | branch-initial | 20,388,016 | +55.5% | ✅ Kept |
| — | baseline (master) | 13,112,215 | — | Baseline |
| — | iter-001-single-entry-bypass | 18,934,320 | +44.4% | ❌ Dropped |

## iter-002 highlights
- MultiPath.chars count=1: +61% vs branch-initial (fixed count=1 regression)
- StarMultiPath.chars count=1: +33% vs branch-initial (fixed count=1 regression)
- Overall +4.3% vs branch-initial

## Known remaining issues
- AnyPath bytes/chars count=1,5: -2% to -5% vs master (slight regression)
