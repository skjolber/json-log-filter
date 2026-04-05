# Summary: iter-001-word-at-a-time-quote-scan

## Result
- **Measured throughput:** 124,295 ops/sec
- **Baseline:** 120,093 ops/sec
- **Delta:** +4,202 ops/sec (+3.5%)
- **Decision:** ✅ Keep (threshold: +1,201 ops/sec / +1%)

## Strategy
Replaced the byte-by-byte `scanQuotedValue` loop in `ByteArrayRangesFilter` with a
word-at-a-time scan using `VarHandle.byteArrayViewVarHandle(long[].class, LITTLE_ENDIAN)`.

Each iteration reads 8 bytes as a 64-bit long and applies the Hacker's Delight has-zero-byte
trick to locate `"` (0x22) within the word:
```java
long x = word ^ 0x2222222222222222L;
long y = (x - 0x0101010101010101L) & ~x & 0x8080808080808080L;
// y != 0 → some byte in word was '"'
// position: Long.numberOfTrailingZeros(y) >>> 3
```

Also applied to `scanEscapedValue` (the inner scan after an escaped quote).

## Why it worked
- All 11 benchmark methods call `process(byte[], ...)`, flowing through `ByteArrayRangesFilter.scanQuotedValue`.
- 8× reduction in loop iterations for strings ≥ 8 bytes.
- VarHandle is intrinsified by HotSpot on ARM64 to a single `ldr` instruction.
- Standard Java 9+ API — no JVM flags or module changes needed.
