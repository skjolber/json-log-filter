# Reflexion: iter-007

## Hypothesis vs Reality
- Hypothesis: 16-byte loop would reduce outer iterations by ~50%, yielding a moderate +1–3% gain over iter-001
- Reality: +22.6% over iter-001 — dramatically exceeded expectations

## Why the gap?
1. ARM64 pipeline effect was much larger than estimated. Two back-to-back `ldr` at `i` and `i+8` from the same base register are effectively free in parallel on Apple Silicon's out-of-order execution units.
2. The benchmark CVE JSON strings are longer than estimated (likely many strings >16 bytes), so the 16-byte loop path dominates.
3. The 16-byte loop body has the same number of branches as the old 8-byte loop but covers twice the bytes — branch-predictor pressure is halved.

## Lessons learned
- Word-at-a-time techniques have non-linear scaling on modern OOO processors — the second load is nearly free if it's issued together with the first.
- Doubling the granularity of the inner loop had a compounding effect beyond the theoretical 2× loop overhead reduction.

## Next directions (if target not yet reached)
- 32-byte loop (four VarHandle loads) — may hit diminishing returns, but worth trying
- Apply same dual-load technique to other inner loops in `skipObject` / `skipArray`
- Target reached at 152,380 — experiment can be considered successful
