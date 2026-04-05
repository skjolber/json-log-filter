# Summary: iter-007

## Strategy
dual-varhandle-16-bytes-iter

## Result
152,380 ops/sec

## Delta vs current best (iter-001: 124,295)
+28,085 ops/sec (+22.6%) ✅ TARGET REACHED

## Delta vs baseline (120,093)
+32,287 ops/sec (+26.9%)

## Decision
**Keep** — well above the +1,201 threshold (125,496), and exceeds the +10% success target (132,102)

## What worked
Extending `scanQuotedValue` and `scanEscapedValue` from 8 bytes/iter to 16 bytes/iter via two consecutive VarHandle `getLong` calls per outer loop iteration. For strings ≥16 bytes in the CVE JSON benchmark data, this roughly halves the number of outer loop iterations and leverages ARM64's ability to pipeline two consecutive `ldr` instructions.

## Why it worked better than expected
The benchmark strings appear to be long enough that the 16-byte loop fires frequently. The ~22.6% gain over iter-001 (which was itself 8-byte) suggests significant pipeline benefit on Apple Silicon — the JIT likely fused the two `ldr` loads.
