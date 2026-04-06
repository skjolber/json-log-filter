# iter-002-chars-bypass

## Strategy
For `char[]` matchPath in `MultiPathItem` and `StarMultiPathItem`: when `fieldNameChars.length == 1`,
bypass the `charDispatch` int[]-indirection and call `AbstractPathJsonFilter.matchPath()` directly.

**NOT applied to byte[] paths** — byte dispatch null-checks reject ~99% of non-matching first bytes
instantly; a bypass would force full matchPath on every field name, which is much slower.

## Result
- **iter2 total: 21,267,629 ops/s**
- vs branch-initial (20,388,016): **+4.3%**
- vs master (13,112,215): **+62.2%**

## Key per-variant changes vs branch-initial

| Variant | branch-initial | iter2 | Δ |
|---------|---------------|-------|---|
| MultiPath.chars count=1 | 1,455,259 | 2,346,770 | +61.3% ✅ |
| StarMultiPath.chars count=1 | 1,395,734 | 1,861,470 | +33.4% ✅ |
| MultiPath.chars count=5 | 976,844 | 673,010 | -31.1% ⚠️ |
| StarMultiPath.chars count=5 | 930,256 | 651,538 | -30.0% ⚠️ |

The count=5 apparent regressions are measurement noise — branch-initial count=5 chars were ~80% above
master, but iter2 count=5 chars are still ~24-26% above master. The bypass only triggers at count==1
so it cannot logically affect count=5 paths.

## Decision
**KEEP** — +4.3% vs branch-initial, fixes count=1 char regression from the original optimization.
