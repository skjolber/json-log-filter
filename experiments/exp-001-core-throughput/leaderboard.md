# Experiment Leaderboard: exp-001-core-throughput
Last updated: 2026-04-05 (iter-005)

| Rank | Iteration | Result (ops/s) | Delta | Strategy | Status |
|------|-----------|----------------|-------|----------|--------|
| 1 | iter-001-word-at-a-time-quote-scan | 124,295 | +4,202 (+3.5%) | Word-at-a-time byte scanning via VarHandle | ✅ |
| 2 | baseline | 120,093 | — | — | Baseline |
| 5 | iter-002-outer-quote-scan-vectorization | 122,804 | -1,491 vs best (-1.2%) | scanToQuote() word-at-a-time for outer `"` scan in MaxStringLengthJsonFilter | ❌ Dropped |
| 6 | iter-003-word-at-a-time-char-quote-scan | 120,940 | -3,355 vs best (-2.7%) | Manual 4-char packing + has-zero-short in CharArrayRangesFilter | ❌ Dropped |
| 7 | iter-004-whitespace-filter-branch-reduction | 120,900 | -3,395 vs best (-2.7%) | Branch reorder + VarHandle whitespace inner loop in ByteArrayWhitespaceFilter.process() | ❌ Dropped |
| 8 | iter-005-inline-structural-byte-skip | 123,563 | -732 vs best (-0.6%) | Inline ':' and ',' after short strings in ByteArrayRangesSizeFilter to skip switch dispatch | ❌ Dropped |
