# Iteration 3: AnyPath Single-Filter Bypass

## Strategy

For `AnyPathFilters`, when there's only one filter, store it directly and use a fast path that avoids the 3D array lookups (`exactFiltersBytes[length][firstByte]`).

Added `singleFilter` field and direct comparison logic in `matchPath(byte[])` and `matchPath(char[])`.

## Result

| Metric | Value |
|--------|-------|
| Combined Average | 635,808 ops/sec |
| Current Best | 708,226 ops/sec |
| Delta | -10.2% |

### AnyPath Improvements (isolated)

| Benchmark | Count | Iter1 | Iter3 | Change |
|-----------|-------|-------|-------|--------|
| AnyPath.bytes | 1 | 530k | 731k | +37.7% |
| AnyPath.chars | 1 | 658k | 956k | +45.2% |

### Unexpected Regressions

| Benchmark | Count | Iter1 | Iter3 | Change |
|-----------|-------|-------|-------|--------|
| MultiPath.chars | 1 | 2,361k | 852k | -63.9% |
| StarMultiPath.chars | 1 | 1,862k | 881k | -52.7% |

The AnyPath changes somehow caused massive regression in unrelated MultiPath/StarMultiPath benchmarks. This is likely due to JIT compilation effects - the added code may have pushed inlining thresholds or caused de-optimization in hot paths.

## Decision

**DROP** - Net negative impact despite isolated improvements. JIT interference with other benchmarks.
