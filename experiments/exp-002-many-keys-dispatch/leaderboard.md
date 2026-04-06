# Experiment Leaderboard: exp-002-many-keys-dispatch
Last updated: 2026-04-06

| Rank | Iteration | Combined avg | Delta vs master | AnyPath | MultiPath | StarMultiPath | Status |
|------|-----------|-------------|----------------|---------|-----------|---------------|--------|
| 1 | branch-initial | 679,601 ops/s | +55.5% | -0.6% | +89.1% | +97.2% | ✅ Current best |
| — | baseline (master) | 437,074 ops/s | — | — | — | — | Baseline |

## Per-count summary (branch vs master)
| Count | Master avg | Branch avg | Delta |
|-------|-----------|-----------|-------|
| 1  | 1,115,756 | 1,115,811 | +0.0% |
| 5  |   447,940 |   794,582 | +77.4% |
| 10 |   324,041 |   652,622 | +101.4% |
| 15 |   240,339 |   463,733 | +93.0% |
| 20 |   176,451 |   371,255 | +110.4% |

## Known issues to address
- MultiPath chars count=1: **-11.1%** regression (dispatch overhead > linear scan for 1 entry)
- StarMultiPath chars count=1: **-4.8%** regression (same cause)
- AnyPath bytes/chars count=1,5: **-2% to -6%** slight regression
