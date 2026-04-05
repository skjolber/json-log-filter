# Summary: iter-006-arrays-equals-matchpath

## Strategy
Replace byte-by-byte comparison loops in `AbstractPathJsonFilter.matchPath(byte[],…)` and `matchPath(char[],…)` with `Arrays.equals(…, int, int, …, int, int)` range overloads, which are intrinsified on JDK 25 to SIMD vectorized comparisons.

## Result
- **123,486 ops/sec** (vs current best 124,295; vs baseline 120,093)
- Delta vs current best: **-809 ops/sec (-0.65%)**
- Decision: **❌ Dropped**

## What went wrong
The `Arrays.equals` range overload introduces a bounds-check-elimination (BCE) barrier: the JIT cannot prove that `start + attribute.length <= source.length` at compile time (because `start` is a dynamic parameter), so it inserts an array bounds check on every call that the manual loop avoids after the first iteration. On short field names (3–15 bytes, typical in CVE JSON), the manual loop with early-exit is already branchless after JIT, while `Arrays.equals` adds a stub call and argument boxing overhead. The intrinsic only helps for strings longer than ~16 bytes where vectorization pays off; JSON field names are never that long.
