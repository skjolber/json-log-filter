# Iteration 1: Single-Entry Bypass (Chars Only)

## Strategy

For `MultiPathItem` and `StarMultiPathItem`, when `fieldNameChars.length == 1`, skip the dispatch table lookup and perform a direct comparison. This avoids dispatch overhead for single-filter cases where the table lookup is more expensive than a direct match.

Applied only to `matchPath(int level, char[] source, ...)` methods, NOT to byte variants (which showed regression when the bypass was added).

## Changes

- `MultiPathItem.matchPath(char[])`: Added single-entry bypass before dispatch
- `StarMultiPathItem.matchPath(char[])`: Added single-entry bypass before dispatch

## Result

| Metric | Value |
|--------|-------|
| Combined Average | 708,226 ops/sec |
| Baseline | 679,601 ops/sec |
| Delta | +4.2% |

### Key Improvements

| Benchmark | Count | Before | After | Change |
|-----------|-------|--------|-------|--------|
| MultiPath.chars | 1 | 1,467k | 2,357k | +60% |
| StarMultiPath.chars | 1 | 1,392k | 2,041k | +46% |

### Unchanged (bytes variants)

| Benchmark | Count | Before | After | Change |
|-----------|-------|--------|-------|--------|
| MultiPath.bytes | 1 | 1,367k | 1,358k | -0.6% |
| StarMultiPath.bytes | 1 | 1,301k | 1,303k | +0.2% |

## Decision

**KEEP** - Clear improvement in combined average (+4.2%)
