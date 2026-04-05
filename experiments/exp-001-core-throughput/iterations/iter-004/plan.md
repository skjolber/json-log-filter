# Iteration Plan: iter-004

## Strategy Name
whitespace-filter-branch-reduction

## Target
`ByteArrayWhitespaceFilter.process()` — the only method called by `core_remove_whitespace`, the 1-of-7 benchmark using a whitespace filter.

## Data analysis
CVE JSON benchmark files are **compact** (0 newlines, 281 spaces inside quoted strings). This means:
- External whitespace path (`c <= 0x20` branch) is **never taken** in the outer loop
- The `do { offset++; } while(chars[offset] <= 0x20)` inner loop **never runs**
- The outer loop alternates between: (a) `c == '"'` → `scanBeyondQuotedValue` call, and (b) structural byte (`:`,`,`,`{`,`}`,`[`,`]`) → `offset++`
- Inter-string structural spans are **1–3 bytes** (too short for word-at-a-time to skip 8 at once)

## Why previous approaches don't work here
- **Word-at-a-time outer scan**: spans of 1–3 structural bytes → the 8-byte word always contains a `"` within positions 0–2 → check always fails → adds overhead (same reason iter-002 failed)
- **Word-at-a-time whitespace inner loop**: never runs on compact JSON → zero impact on benchmark
- **Direct bulk copy (Option B/C)**: `output.write` already does `System.arraycopy`; the write side is already optimal

## Chosen optimization: branch-count reduction via reordering

The current inner loop body has **2 comparisons** for every structural byte:
```java
if(c == '"') { ... }       // compare 1 (fails 2/3 of time)
else if(c <= 0x20) { ... } // compare 2 (fails 2/3 of time, always for compact JSON)
else { offset++; }
```

**Restructured loop** (`c > '"'` fast path first):
```java
if(c > '"') {              // 1 compare for structural bytes (all > 0x22): fast path
    offset++;
    continue;
}
if(c == '"') { ... }       // 1 compare for quote (reached 1/3 of time)
// else: whitespace (c <= 0x20) or '!' — compact JSON never reaches this
```

Comparison count per iteration:
| Path          | Current | New   |
|---------------|---------|-------|
| Structural (c > '"') | 2 | 1 |
| Quote (c == '"')     | 1 | 2 |
| Whitespace           | 2 | 3 |

With structural ≈ 50–60% of iterations, quote ≈ 40–50%, whitespace ≈ 0% for compact JSON:
- **Old**: (0.55 × 2) + (0.45 × 1) = 1.55 compares/iter  
- **New**: (0.55 × 1) + (0.45 × 2) = 1.45 compares/iter  
→ ~6% fewer comparisons in the outer loop, which is ~15–20% of total work → ~1–2% net gain

Additionally, word-at-a-time is added to the **whitespace inner loop** (for pretty-printed JSON, zero overhead for compact JSON).

## Files to modify
- `impl/core/src/main/java/com/github/skjolber/jsonfilter/core/util/ByteArrayWhitespaceFilter.java`
  - Reorder outer loop checks: fast path `c > '"'` first
  - Add VarHandle + constants for word-at-a-time whitespace skip (inner loop)
