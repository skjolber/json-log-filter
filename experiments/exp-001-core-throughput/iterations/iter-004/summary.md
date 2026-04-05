# Summary: iter-004-whitespace-filter-branch-reduction

## Strategy
Reorder outer loop checks in `ByteArrayWhitespaceFilter.process()` to test `c > '"'` (0x22) first, covering all structural JSON bytes (`:`, `,`, `{`, `}`, `[`, `]`, digits, keywords) in one comparison instead of two. Also added VarHandle word-at-a-time to the whitespace-skipping inner loop for pretty-printed JSON.

## Result
**120,900 ops/sec** — ❌ DROPPED

| Metric | Value |
|--------|-------|
| Result | 120,900 ops/sec |
| vs current best (iter-001: 124,295) | -3,395 ops/sec (-2.7%) |
| vs baseline (120,093) | +807 ops/sec (+0.7%) |
| Threshold (≥ 125,496) | NOT MET |

## What happened

The optimization hurt performance by -2.7% relative to the current best.

### Root cause analysis

The benchmark data (CVE JSON, compact/minified, 8KB and 22KB files) has:
- **Zero external whitespace**: the `c <= 0x20` path in the outer loop never triggers
- **Spaces only inside quoted strings**: handled by `scanBeyondQuotedValue`, not the outer loop
- **1–2 structural bytes between strings**: `:`, `,`, `{`, `}`, `[`, `]` between consecutive quoted values

The reordering from:
```java
if(c == '"') { ... }
else if(c <= 0x20) { ... }
else { offset++; }
```
to:
```java
if(c > '"') { offset++; continue; }  // NEW fast path first
if(c == '"') { ... }
// whitespace fallthrough
```

**Adds** one comparison for every quote encounter (which is ~40–50% of outer iterations) without saving anything significant for structural bytes (which are short 1–2 byte spans anyway). The net effect is more branch pressure, not less.

The VarHandle whitespace inner loop adds zero benefit (the whitespace path never runs on compact JSON) but adds class initialization overhead.

## Key lesson

Even a small branch-count reduction in the outer loop can hurt if:
1. It adds overhead to the *other* common path (quote encounters)
2. The branch predictor was already handling the original two-branch structure well
3. The JIT C2 may fuse or specialize the original pattern more efficiently than the restructured one

The benchmark data characteristics (compact JSON, short inter-string spans) mean that **any** optimization targeting the outer structural-byte scan or whitespace inner loop has limited headroom and high risk of adding overhead.
