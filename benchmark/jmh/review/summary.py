#!/usr/bin/env python3
"""Rank the changes between two merged JMH result files.

Usage: summary.py BASE.json OTHER.json [LABEL_BASE] [LABEL_OTHER] [NOISE_PCT]

A change counts as significant when it exceeds the combined 99.9% confidence intervals of both sides and
NOISE_PCT (default 3, the fork-to-fork variation seen on this machine). Prints the counts, then the
significant regressions and improvements, largest first.
"""
import json, sys

def load(path):
    out = {}
    for r in json.load(open(path)):
        name = r["benchmark"].rsplit(".", 2)
        key = (name[-2] + "." + name[-1], ",".join(str(v) for _, v in sorted(r.get("params", {}).items())))
        m = r["primaryMetric"]
        err = m["scoreError"] if isinstance(m["scoreError"], (int, float)) else 0.0
        out[key] = (m["score"], err)
    return out

base, other = load(sys.argv[1]), load(sys.argv[2])
lb = sys.argv[3] if len(sys.argv) > 3 else "base"
lo = sys.argv[4] if len(sys.argv) > 4 else "other"
noise = float(sys.argv[5]) if len(sys.argv) > 5 else 3.0

rows = []
for key in sorted(base):
    if key not in other:
        continue
    (b, be), (o, oe) = base[key], other[key]
    change = (o - b) / b * 100
    threshold = max((be / b + oe / o) * 100, noise)
    rows.append((change, threshold, key, b, o))

regressions = sorted([r for r in rows if r[0] < -r[1]])
improvements = sorted([r for r in rows if r[0] > r[1]], reverse=True)
print(f"{len(rows)} results: {len(improvements)} faster, {len(regressions)} slower, "
      f"{len(rows) - len(improvements) - len(regressions)} within the noise (the larger of the combined error and {noise:g}%).\n")
for title, items in (("Slower", regressions), ("Faster", improvements)):
    if not items:
        continue
    print(f"{title} ({lb} → {lo}):\n")
    print(f"| Benchmark | Document | {lb} ops/s | {lo} ops/s | Change | Threshold |")
    print("|---|---|---|---|---|---|")
    for change, threshold, key, b, o in items:
        print(f"| {key[0]} | {key[1]} | {b:,.0f} | {o:,.0f} | {change:+.1f}% | ±{threshold:.1f}% |")
    print()
