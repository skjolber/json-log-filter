# Iteration Plan: iter-005

## Strategy Name
inline-structural-byte-skip

## Step-Back: What type of bottleneck?
CPU-bound switch-dispatch overhead. After iter-001 vectorized the inner string scan
(`scanQuotedValue`), the remaining overhead in `ByteArrayRangesSizeFilter` is in the
outer structural-byte dispatch loop. Every string encounter in
`skipObjectOrArrayMaxSizeMaxStringLength` is followed by going back to the top of
`switch(chars[offset])` to process the next byte (`:` or `,`). That switch spans
cases 0x22–0x7D — a ~90-entry jump table — and fires needlessly for `:` and `,`.

## Analysis

Both strategies' "unoptimized scan loops" were eliminated by iter-001:
- Strategy A: `ByteArrayRangesSizeFilter` delegates all string scanning to
  `scanBeyondQuotedValue` → `scanQuotedValue` (already word-at-a-time).
- Strategy B: `MaxStringLengthJsonFilter` also calls `scanQuotedValue` (already done).

However, Strategy A has a remaining opportunity in the outer structural loop of
`skipObjectOrArrayMaxSizeMaxStringLength`. The short-string fast path is:
```java
if(nextOffset - offset <= maxStringLength) {
    offset = nextOffset;   // nextOffset = byte after closing '"'
    continue;              // → switch dispatch on ':' or ',' next
}
```

After a short string:
- ~50% of strings are KEYS → next byte is `:` (switch default, just `offset++`)
- ~40% of strings are VALUES → next byte is `,` (switch case, `mark=offset; offset++`)
- ~10% end structures → next byte is `}` or `]` (need full switch handling)

The switch `switch(chars[offset])` over cases `{`(0x7B), `[`(0x5B), `}`(0x7D), `]`(0x5D),
`,`(0x2C), `"`(0x22) creates a jump table from 0x22 to 0x7D — 90 entries, mostly
`default`. Inlining the `:` and `,` cases eliminates ~90% of those switch dispatches
that occur after short strings.

## Approach: Inline ':' and ',' after short-string fast-path

Change the short-string path in `skipObjectOrArrayMaxSizeMaxStringLength` from:
```java
if(nextOffset - offset <= maxStringLength) {
    offset = nextOffset;
    continue;
}
```
to:
```java
if(nextOffset - offset <= maxStringLength) {
    offset = nextOffset;
    if(chars[offset] == ':') {
        offset++;
    } else if(chars[offset] == ',') {
        mark = offset++;
    }
    // '}', ']', '{', '[' fall through to switch in next iteration
    continue;
}
```

**Semantic equivalence:**
- `:` in original: switch `default` → `offset++`. Inline: `offset++`. ✓
- `,` in original: switch `case ','` → `mark = offset; offset++`. Inline: `mark = offset++`. ✓
- `}`, `]`, `{`, `[`: neither branch taken, falls through to next switch iteration. ✓

**Expected improvement:**
- 90% of post-string switch dispatches eliminated for short strings
- Each 90-entry sparse jump-table dispatch costs ~3–5 cycles on ARM64
- CVE JSON: ~500–1000 strings per document, ~90% are short strings
- Estimate: 900 strings × 5 cycles = 4500 cycles per document
- At ~400,000 cycles/document: ~1.1% gain → ~+1,370 ops/sec over current best (124,295)
- Target threshold: +1,201 → estimate clears it

## Files to modify
- `impl/core/src/main/java/com/github/skjolber/jsonfilter/core/util/ByteArrayRangesSizeFilter.java`
  - `skipObjectOrArrayMaxSizeMaxStringLength`: inline `:` and `,` in short-string path

## Benchmarks affected
- `maxSize_core` — primary user of `ByteArrayRangesSizeFilter`
- `all_core` — also uses `ByteArrayRangesSizeFilter`
- `maxStringLengthMaxSize_core` — combined filter

## Pre-mortem (if this strategy failed)
- JIT C2 may already convert the sparse switch to a series of `cmp+branch` instructions,
  making the inline fast-path equivalent overhead-wise.
- The inlined `chars[offset]` load is an extra memory read; if JIT merges the switch
  dispatch load with it, there may be no net savings.
- Short string rate may be lower than estimated (long CVE descriptions are the majority
  of payload bytes, but key names dominate string COUNT).
