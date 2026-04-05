# Summary: iter-005-inline-structural-byte-skip

## Result
**123,563 ops/sec** — ❌ Dropped (below keep threshold of 125,496)

## What was attempted
In `ByteArrayRangesSizeFilter.skipObjectOrArrayMaxSizeMaxStringLength`, the short-string
fast-path unconditionally returned to the outer `switch(chars[offset])` after each string.
That switch spans cases 0x22–0x7D (~90 entries, mostly `default`), creating a jump-table
dispatch for the two most common post-string bytes:
- `:` (after key strings, ~50%) → switch default, just `offset++`
- `,` (after value strings, ~40%) → switch case, `mark = offset; offset++`

The optimization inlined both handlers directly in the fast-path:
```java
if(offset < maxSizeLimit) {
    if(chars[offset] == ':')      { offset++;          }
    else if(chars[offset] == ',') { mark = offset++;   }
}
```

A bounds guard (`offset < maxSizeLimit`) was required to avoid incorrect `mark` updates
when a string ends at exactly the size boundary (found by failing tests on first attempt).

## Performance
| Metric | Value |
|--------|-------|
| Result | 123,563 ops/sec |
| vs current best (124,295) | **-732 (-0.59%)** |
| vs baseline (120,093) | +3,470 (+2.89%) |
| Keep threshold | ≥ 125,496 |

## Conclusion
The optimization regressed by ~0.6%. The added bounds check (`offset < maxSizeLimit`) and
the two `cmp+branch` instructions offset any savings from eliminating the jump-table dispatch.
The JIT C2 compiler on JDK 25 / ARM64 likely already compiles the sparse switch to a
linear comparison chain rather than a 90-entry jump table, making the inline form equivalent
or slightly worse.
