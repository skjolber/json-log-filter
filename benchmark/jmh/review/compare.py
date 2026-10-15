#!/usr/bin/env python3
"""Compare two JMH JSON result files: prints a markdown table of ops/s and the change vs baseline."""
import json, sys

def load(path):
    out = {}
    for r in json.load(open(path)):
        name = r["benchmark"].rsplit(".", 2)
        bench = name[-2] + "." + name[-1]
        param = ",".join(f"{v}" for k, v in sorted(r.get("params", {}).items()))
        m = r["primaryMetric"]
        out[(bench, param)] = (m["score"], m["scoreError"] if isinstance(m["scoreError"], (int, float)) else 0.0)
    return out

base, other = load(sys.argv[1]), load(sys.argv[2])
filt = sys.argv[3] if len(sys.argv) > 3 else ""
print("| Benchmark | Document | master ops/s | PR ops/s | Change |")
print("|---|---|---|---|---|")
for key in sorted(base):
    if key not in other or filt not in key[0]:
        continue
    (b, be), (o, oe) = base[key], other[key]
    change = (o - b) / b * 100
    noise = (be / b + oe / o) * 100
    flag = "" if abs(change) <= noise else (" ▲" if change > 0 else " ▼")
    print(f"| {key[0]} | {key[1]} | {b:,.0f} ± {be/b*100:.1f}% | {o:,.0f} ± {oe/o*100:.1f}% | {change:+.1f}%{flag} |")
