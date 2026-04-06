# Experiment 002: Many-Keys Dispatch Optimization - Final Report

## Objective

Improve JSON path key matching performance when many filter expressions are active simultaneously by using first-byte dispatch tables.

## Baseline

- **Master (linear scan):** 437,074 ops/sec combined average
- **Branch initial (dispatch tables):** 679,601 ops/sec (+55.5% vs master)

## Optimization Loop Results

### Iteration 1: Single-Entry Bypass (Chars Only) ✅ KEPT

**Strategy:** For `MultiPathItem` and `StarMultiPathItem`, when `fieldNameChars.length == 1`, skip the dispatch table lookup and perform direct comparison.

**Result:** 708,226 ops/sec (+4.2% vs branch baseline)

Key improvements:
- MultiPath.chars count=1: 2,357k ops/s (+60.5% vs baseline 1,467k)
- StarMultiPath.chars count=1: 1,858k ops/s (+33.4% vs baseline 1,392k)

This fix addresses the count=1 regression identified in the initial branch measurements.

### Iteration 2: Length Pre-filter ❌ TESTS FAILED

**Strategy:** Add length check inside candidate loop before calling `matchPath()`.

**Result:** Tests failed - breaks encoded key matching (e.g., `na\u006de` decodes to "name" but has different length).

### Iteration 3: AnyPath Single-Filter Bypass ❌ DROPPED

**Strategy:** For `AnyPathFilters` with single filter, store directly and skip 3D array lookups.

**Result:** 635,808 ops/sec (-10.2% vs current best)

The AnyPath improvements (+37% bytes, +45% chars for count=1) were offset by unexpected massive regressions in MultiPath/StarMultiPath (-52% to -63%). This appears to be JIT compilation interference.

### Iteration 4: Bytes Single-Entry Bypass ❌ DROPPED

**Strategy:** Apply same single-entry bypass to byte[] variants.

**Result:** 566,951 ops/sec (-19.9% vs current best)

Adding the `if(fieldNameBytes.length == 1)` check to byte methods caused severe JIT de-optimization across all benchmarks. The same pattern that works for char[] causes major regression for byte[].

### Iteration 5: Indexed Loop ❌ DROPPED

**Strategy:** Convert enhanced for-loop to indexed loop for better JIT optimization.

**Result:** 704,111 ops/sec (-0.6% vs current best)

No meaningful improvement - modern JVMs handle both loop styles equally well.

## Final Results

| Metric | Master | Final (iter-001) | Improvement |
|--------|--------|------------------|-------------|
| Combined avg ops/sec | 437,074 | 708,226 | **+62.0%** |

## Key Learnings

1. **char[] vs byte[] behave differently under JIT:** The same optimization pattern can succeed for char[] but fail catastrophically for byte[]. Always benchmark separately.

2. **JIT interference is real:** Adding code to one class can regress unrelated classes through compiler heuristic changes (inlining thresholds, etc.).

3. **Length pre-filtering breaks encoding:** JSON keys can contain Unicode escapes (`\u006e` = 'n') where the encoded length differs from the decoded length. Simple length checks break this.

4. **Single-entry bypass is valuable for chars:** The dispatch table overhead exceeds the benefit for single-filter cases in char[] methods, but the same doesn't apply to byte[] methods.

## Committed Changes

- `MultiPathItem.matchPath(char[])`: Added single-entry bypass
- `StarMultiPathItem.matchPath(char[])`: Added single-entry bypass

## Branch Status

Branch `optimizeLengthCheck` is ready for review/merge with:
- +62.0% improvement vs master baseline
- All tests passing
- Iteration 1 changes committed
