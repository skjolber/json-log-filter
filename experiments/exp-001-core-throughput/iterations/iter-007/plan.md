# Iteration Plan: iter-007

## Strategy Name
dual-varhandle-16-bytes-iter

## Step-Back: What type of bottleneck?
CPU-bound byte scanning. iter-001 proved that reducing loop iterations in `scanQuotedValue` via VarHandle word-at-a-time (8 bytes/iter) gave +3.5%. The natural extension is 16 bytes/iter by reading two consecutive longs per outer iteration.

## Approach: Dual VarHandle reads (16 bytes per outer iteration)

Extend the 8-bytes/iter loop to first try 16 bytes/iter:
1. Read `word1` at `i`, `word2` at `i+8`
2. If `word1` contains a quote, handle it (same as before)
3. Only if `word1` is clean, check `word2`
4. Advance by 16 if both words are clean
5. Fall through to 8-byte loop for 8–15 byte tail, then scalar for <8

**Rationale:**
- For strings of average length 20–30 bytes: ~50% reduction in outer loop iterations
- On ARM64 (Apple Silicon): JIT can pipeline two consecutive `ldr` from same base register → ~1–2 cycles for both loads
- No new dependencies, same VarHandle infrastructure from iter-001

## Files Modified
- `impl/core/src/main/java/com/github/skjolber/jsonfilter/core/util/ByteArrayRangesFilter.java`
  - `scanQuotedValue`: add 16-byte outer loop before existing 8-byte loop
  - `scanEscapedValue`: same 16-byte outer loop extension

## Self-consistency re-evaluation
- Low risk: the 8-byte fallback ensures correctness for short strings
- If strings are shorter than 16 bytes on average, the 16-byte loop body never executes and we add minimal overhead (one extra comparison)
- Expected win: moderate (+1–3% over iter-001) if strings ≥16 bytes are common in benchmark data

## Pre-mortem
- Most likely failure: benchmark strings average <16 bytes, so 16-byte loop never fires
- Next step if this fails: look at outer structural scanner or try 32-byte approach
