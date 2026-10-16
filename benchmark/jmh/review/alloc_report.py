#!/usr/bin/env python3
"""Allocation per operation from JMH JSON results run with -prof gc.

Usage: alloc_report.py DIR BASE_SIDE OTHER_SIDE [FILTER]

Averages gc.alloc.rate.norm (bytes allocated per benchmark operation) over the per-fork files
DIR/<side>-fork*.json of both sides (merge_forks.py keeps only the primary metric), and prints the change.
"""
import glob, json, os, statistics, sys

def load(directory, side):
    out = {}
    for path in glob.glob(os.path.join(directory, f"{side}-fork*.json")):
        for r in json.load(open(path)):
            name = r["benchmark"].rsplit(".", 2)
            key = (name[-2] + "." + name[-1], ",".join(str(v) for _, v in sorted(r.get("params", {}).items())))
            norm = r.get("secondaryMetrics", {}).get("gc.alloc.rate.norm")
            if norm:
                out.setdefault(key, []).append(norm["score"])
    return {k: statistics.mean(v) for k, v in out.items()}

directory, b_side, o_side = sys.argv[1:4]
filt = sys.argv[4] if len(sys.argv) > 4 else ""
base, other = load(directory, b_side), load(directory, o_side)
print(f"| Benchmark | Document | {b_side} B/op | {o_side} B/op | Change |")
print("|---|---|---|---|---|")
for key in sorted(base):
    if key not in other or filt not in key[0]:
        continue
    b, o = base[key], other[key]
    change = (o - b) / b * 100 if b else 0.0
    print(f"| {key[0]} | {key[1]} | {b:,.0f} | {o:,.0f} | {change:+.1f}% |")
