# Iteration 4: Bytes Single-Entry Bypass

## Strategy

Add the same single-entry bypass to byte[] variants that was successful for char[] in iteration 1:

```java
if(fieldNameBytes.length == 1) {
    return AbstractPathJsonFilter.matchPath(source, start, end, fieldNameBytes[0]) ? next[0] : this;
}
```

Applied to `MultiPathItem.matchPath(byte[])` and `StarMultiPathItem.matchPath(byte[])`.

## Result

| Metric | Value |
|--------|-------|
| Combined Average | 566,951 ops/sec |
| Current Best | 708,226 ops/sec |
| Delta | -19.9% |

### Major Regressions

| Benchmark | Count | Iter1 | Iter4 | Change |
|-----------|-------|-------|-------|--------|
| MultiPath.bytes | 1 | 1,370k | 655k | -52.2% |
| StarMultiPath.bytes | 1 | 1,317k | 637k | -51.7% |
| MultiPath.chars | 1 | 2,361k | 894k | -62.1% |
| StarMultiPath.chars | 1 | 1,862k | 879k | -52.8% |

The addition of `if(fieldNameBytes.length == 1)` check in the byte methods causes severe JIT de-optimization across all benchmarks. Even the chars variants (which already have this check) regress, suggesting the combined code pattern triggers compiler heuristic changes.

## Analysis

This is a classic JIT compilation issue. The same optimization pattern that works for char[] causes massive regression for byte[]. The JVM's JIT compiler handles char[] and byte[] methods differently due to:
- Different inlining decisions
- Different escape analysis
- Different loop unrolling heuristics

Adding the `if` branch to byte methods may push the method over inlining thresholds or prevent profitable optimizations.

## Decision

**DROP** - Severe regression (-19.9%). Do not add single-entry bypass to byte methods.
