# Iteration 5: Indexed Loop instead of Enhanced For

## Strategy

Convert the enhanced for-loop over `candidates` to an indexed loop to help JIT optimization:

```java
// Before (enhanced for)
for(int idx : candidates) {
    if(AbstractPathJsonFilter.matchPath(...)) {
        return next[idx];
    }
}

// After (indexed loop)
for(int i = 0; i < candidates.length; i++) {
    int idx = candidates[i];
    if(AbstractPathJsonFilter.matchPath(...)) {
        return next[idx];
    }
}
```

Applied to both byte[] and char[] variants in `MultiPathItem` and `StarMultiPathItem`.

## Result

| Metric | Value |
|--------|-------|
| Combined Average | 704,111 ops/sec |
| Current Best | 708,226 ops/sec |
| Delta | -0.6% |

### Changes by Benchmark

Mixed results - some benchmarks improved slightly (+8.9%), others regressed slightly (-4.3%). The overall effect is within measurement noise.

## Analysis

Modern JVMs (especially Java 25) handle both loop styles equally well. The HotSpot JIT compiler can recognize and optimize both patterns. The indexed loop doesn't provide a consistent advantage over the enhanced for-loop.

## Decision

**DROP** - No meaningful improvement. Keep the cleaner enhanced for-loop syntax.
