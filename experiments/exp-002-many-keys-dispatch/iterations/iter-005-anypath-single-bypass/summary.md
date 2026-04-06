# iter-005-anypath-single-bypass

## Strategy
When `AnyPathFilters` has exactly one filter (count==1), bypass the 3D dispatch table and
the encoding-scan loop, calling `AbstractPathJsonFilter.matchPath()` directly.

Rationale: the encoding scan runs O(key_length) per non-matching key. For count=1, replacing
dispatch + scan with a direct `matchPath` call (which handles encoding via `matchesEncoded`
internally) should eliminate the scan overhead while maintaining correctness.

## Result
- **iter5 total: 21,160,959 ops/s**
- vs iter2 (21,267,629): **-0.5% — below iteration threshold**
- vs master: +61.4%

## Key per-variant changes vs iter2

| Variant | iter2 | iter5 | Δ |
|---------|-------|-------|---|
| AnyPath.bytes count=1 | 531,365 | 529,588 | -0.3% |
| AnyPath.chars count=1 | 659,449 | 654,599 | -0.7% |

All other variants are within ±5% noise.

## Root cause
The existing dispatch table null-check (`exactFiltersBytes[length] == null → skip`) is already
near-zero cost for non-matching keys — a single array read + null check. The encoding scan is a
simple tight loop that JIT compiles to a fast scalar loop; the Shopify JSON has no backslashes
in field names so the loop body never executes. Both fast paths are already JIT-optimised.

Replacing them with a direct `matchPath` call adds an extra call-site + method prologue overhead
that roughly cancels any scan savings.

## Decision
**DROP** — -0.5% is below the 1% iteration threshold. Revert `AnyPathFilters.java`.
