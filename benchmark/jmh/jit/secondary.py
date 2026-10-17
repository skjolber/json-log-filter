#!/usr/bin/env python3
"""Compare a JMH profiler's secondary metrics (e.g. gc.alloc.rate.norm from -prof gc) between two merged result files.

Usage: secondary.py BASE.json CANDIDATE.json [metric substring, default alloc.rate.norm]
"""
import json, sys


def load(path, metric):
    out = {}
    for r in json.load(open(path)):
        name = r["benchmark"].rsplit(".", 2)
        bench = name[-2] + "." + name[-1]
        param = ",".join(f"{v}" for k, v in sorted(r.get("params", {}).items()))
        for key, m in r.get("secondaryMetrics", {}).items():
            if metric in key:
                out[(bench, param, key)] = (m["score"], m.get("scoreUnit", ""))
    return out


metric = sys.argv[3] if len(sys.argv) > 3 else "alloc.rate.norm"
base, cand = load(sys.argv[1], metric), load(sys.argv[2], metric)
print()
print(f"| Benchmark | Document | {metric} base | candidate |")
print("|---|---|---|---|")
for key in sorted(base):
    if key in cand:
        (b, unit), (c, _) = base[key], cand[key]
        print(f"| {key[0]} | {key[1]} | {b:,.1f} {unit} | {c:,.1f} {unit} |")
