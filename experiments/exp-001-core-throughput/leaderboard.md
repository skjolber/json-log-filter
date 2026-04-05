# Experiment Leaderboard: exp-001-core-throughput
Last updated: 2026-04-05

| Rank | Iteration | Result (ops/s) | Delta | Strategy | Status |
|------|-----------|----------------|-------|----------|--------|
| 1 | iter-001-word-at-a-time-quote-scan | 124,295 | +4,202 (+3.5%) | Word-at-a-time byte scanning via VarHandle | ✅ |
| 2 | baseline | 120,093 | — | — | Baseline |
| 3 | iter-002-outer-quote-scan-vectorization | 122,804 | -1,491 vs best (-1.2%) | scanToQuote() word-at-a-time for outer `"` scan in MaxStringLengthJsonFilter | ❌ Dropped |
