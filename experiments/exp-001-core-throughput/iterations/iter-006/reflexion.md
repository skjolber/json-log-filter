# Reflexion: iter-006-arrays-equals-matchpath

## What this iteration taught

**`Arrays.equals` range overload hurt by -0.65% on short field names.**

### Why the optimization was expected to work
`Arrays.equals(byte[], int, int, byte[], int, int)` is documented as intrinsified on JDK 9+. On ARM64 with JDK 25, the intrinsic emits a vectorized comparison kernel. For the `matchPath` hot path — fired hundreds of times per CVE document — vectorization was expected to eliminate the per-byte loop overhead and branch mispredictions.

### Why it actually hurt
1. **JSON field names are too short for SIMD payoff.** CVE JSON field names are 3–15 bytes long. SIMD vectorization only outperforms scalar comparison when inputs are ≥16 bytes (one NEON register). For shorter inputs, the vector path includes a scalar tail loop or a masked load, making it slower than the direct scalar loop.

2. **BCE cost on range parameters.** The JIT cannot statically prove that `start + attribute.length <= source.length` because `start` is runtime-dynamic. This prevents the compiler from hoisting the bounds check out of the hot path, adding ~1 cycle per call that the inline manual loop avoids (the loop's first iteration bounds-checks `start + 0`, and subsequent checks are eliminated by induction analysis).

3. **The manual loop already compiles well.** A short counted loop over 3–15 iterations on sequential memory is exactly the pattern C2's loop optimizer handles best: it unrolls, eliminates branches, and generates sequential `ldrb` / `cmp` chains. The result is nearly identical to the intrinsic for this input size.

## Concrete lessons for next iteration

1. **`Arrays.equals` intrinsic only helps for strings ≥16 bytes.** For typical JSON field names (≤15 bytes), the manual loop is at least as fast. Do not attempt any bulk-comparison replacement on `matchPath`.

2. **`matchPath` is not a bottleneck despite call frequency.** The per-call cost is 3–15 scalar comparisons with early exit. On a mismatch at byte 0 (most CVE field names mismatch immediately against target paths), total cost is 1 comparison + 2 bounds checks. Micro-optimizing this is futile.

3. **Remaining unexplored directions (ranked by potential):**
   - **Output write coalescing in `ByteArrayRangesFilter.filter()`** — `buffer.write()` is called 3× per truncation range. If `ResizableByteArrayOutputStream.write()` has capacity-check overhead, batching may help. This is a pure base/ change.
   - **`AnyPathJsonFilter` outer `"` scan** — `anon_any_core` / `anon_full_core` scan for `"` byte-by-byte in the outer document loop. iter-001 only vectorized the inner quoted-value body. Applying word-at-a-time to the outer `"` search (as iter-002 tried for MaxStringLength) might help if the CVE outer structure has longer inter-key gaps.
   - **Thread-local / pre-allocated ranges array** — if `ByteArrayRangesFilter` allocates a new `int[]` per call for the ranges buffer, replacing with a thread-local pre-allocated array could reduce GC pressure. Check `AbstractRangesJsonFilter`.
