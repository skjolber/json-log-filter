# Reflexion: iter-003-word-at-a-time-char-quote-scan

## What this iteration taught

**Manual char packing is expensive — the JIT does not coalesce 4 separate char[] loads into a vectorized word read.**

The byte[] approach in iter-001 succeeded because `MethodHandles.byteArrayViewVarHandle(long[].class, LITTLE_ENDIAN)` is a JVM intrinsic: C2 emits a single `ldr` (ARM64) or `mov` (x86-64) instruction. There is no equivalent for char[]. The manual `((long)chars[i]) | ((long)chars[i+1] << 16) | ...` requires 4 separate array bounds checks, 4 loads, 3 shifts, and 3 ORs per iteration. C2 on JDK 25 / ARM64 does not fuse these into a single word load, so the overhead exceeds the benefit of halving the number of inner-loop iterations.

## Concrete lessons for next iteration

1. **Avoid manual scalar packing for char[] word-at-a-time.** Without a direct VarHandle intrinsic or Unsafe, the cost of packing nullifies the gain.

2. **If targeting char[] paths, consider converting to byte[] first.** The benchmark `process(String, ...)` path likely calls `String.toCharArray()` or operates on the char[] equivalent. If we can avoid the char[] path and route through byte[] (e.g., by encoding to UTF-8 early), the already-optimized byte[] path would apply. This is an architectural change that may impact multiple filters.

3. **Alternative: `Unsafe.getLong(Object, long)` on char[] backing.** Using `sun.misc.Unsafe.objectFieldOffset` + `arrayBaseOffset` to read a long directly from the char[] backing memory would behave exactly like the byte[] VarHandle (single load). This is non-standard but stable across JDKs and would give the same single-instruction load. It's the most likely way to achieve a real gain on the char[] path.

4. **`ByteArrayWhitespaceFilter.process` is still unoptimized.** This covers both the `core_remove_whitespace` benchmark and every whitespace-bearing filter. The byte[] VarHandle approach (already proven) applies there.

5. **Profile which benchmark methods exercise the char[] path.** If the benchmark is dominated by byte[] calls, char[] optimization has limited headroom regardless of implementation quality.

## Most promising next directions

- **`ByteArrayWhitespaceFilter.process` optimization** — still uses scalar byte[] scan; byte[] VarHandle approach applies directly (no packing cost)
- **`Unsafe`-based char[] word load** — could achieve single-instruction load and replicate iter-001 gains on the char[] path without the packing overhead
- **AnyPathJsonFilter key-matching optimization** — path traversal/key comparison overhead
