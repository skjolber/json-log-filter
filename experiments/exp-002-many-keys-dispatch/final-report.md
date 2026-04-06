# Final Optimization Report: exp-002-many-keys-dispatch

## Best Configuration

**Branch `optimizeLengthCheck` at commit `7aeefd00`**  
Two accumulated changes over master:

### 1. First-byte dispatch tables (branch-initial)
`MultiPathItem`, `StarMultiPathItem`, and `AnyPathFilters` build 256-slot dispatch tables at
construction time. On each key lookup:
- `byteDispatch[firstByte]` / `charDispatch[firstChar]` — one array read
- `null` → instant return; non-null → iterate only the O(N/256) sub-bucket
- Reduces average candidates from N to N/256 for diverse first-byte filter sets

### 2. Chars-only single-entry bypass (iter-002)
In `MultiPathItem.matchPath(char[])` and `StarMultiPathItem.matchPath(char[])`:
when `fieldNameChars.length == 1`, skip the dispatch table and call `matchPath` directly.
Not applied to `byte[]` — byte dispatch null-checks already reject ~99% of non-matching keys
instantly.

## Experiment Comparison Table

| Rank | Iteration | Total ops/s | vs master | Decision |
|------|-----------|-------------|-----------|----------|
| 1 | **iter-002-chars-bypass** | **21,267,629** | **+62.2%** | ✅ Kept |
| 2 | branch-initial | 20,388,016 | +55.5% | ✅ Kept |
| — | baseline (master) | 13,112,215 | — | Baseline |
| — | iter-005-anypath-single-bypass | 21,160,959 | +61.4% | ❌ Dropped (-0.5%) |
| — | iter-004-anypath-mismatch | 18,825,351 | +43.6% | ❌ Dropped (-11.5%) |
| — | iter-003-arrays-mismatch | 19,119,862 | +45.8% | ❌ Dropped (-10.1%) |
| — | iter-001-single-entry-bypass | ~18.9M | ~+44% | ❌ Dropped (-7.1%) |

## Performance Analysis (iter2 vs master, 30-variant total)

| Variant | count=1 | count=5 | count=10 | count=15 | count=20 |
|---------|---------|---------|----------|----------|----------|
| MultiPath bytes | +141% | +261% | +360% | +262% | +255% |
| MultiPath chars | +43% | +24% | +144% | +130% | +126% |
| StarMultiPath bytes | +141% | +260% | +376% | +268% | +271% |
| StarMultiPath chars | +27% | +26% | +144% | +109% | +117% |
| AnyPath bytes | +1% | -2% | +1% | +8% | +6% |
| AnyPath chars | -3% | -5% | 0% | +2% | +3% |

**Overall: +62.2% throughput improvement** vs master.

## Insights

**What worked:**
- First-byte dispatch eliminates O(N) scan; O(1) null-check rejects ~99% of non-matching keys
- Chars-only count=1 bypass fixes dispatch overhead penalty; JIT inlines char[] comparison
  better than int[]-indirection for single-entry buckets

**What didn't work:**
- `Arrays.mismatch` in global `matchPath`: per-call setup overhead dominates for short strings
  called on many non-matching keys; -62% on count=1 chars bypass path
- `Arrays.mismatch` in `unencodedMatchBytes/Chars`: matching path rarely exercised (~1% of keys)
- AnyPath single-filter bypass: dispatch null-check + encoding scan already JIT-optimised
- JIT interference between benchmarks in same session caused misleading regression signals

## Recommended Next Steps

1. Add `hasEncodedFilters` flag to skip AnyPath encoding scan when all filter keys are ASCII
2. Add benchmarks for `SinglePathItem`/`StarPathItem`
3. Use 2-3 JMH forks to reduce JIT interference between benchmark variants
4. Add a pathological-case benchmark (all filter keys share the same first byte)
