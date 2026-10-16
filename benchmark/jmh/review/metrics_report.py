#!/usr/bin/env python3
"""The cost of metrics, from TmpMetricsBenchmark results: each X_off against X_on.

Usage: metrics_report.py RESULT.json
"""
import json, sys

rows = {}
for r in json.load(open(sys.argv[1])):
    method = r["benchmark"].rsplit(".", 1)[-1]
    name, _, mode = method.rpartition("_")
    doc = ",".join(str(v) for _, v in sorted(r.get("params", {}).items()))
    m = r["primaryMetric"]
    err = m["scoreError"] if isinstance(m["scoreError"], (int, float)) else 0.0
    rows.setdefault((name, doc), {})[mode] = (m["score"], err)

print("| Filter | Document | metrics off ops/s | metrics on ops/s | Cost |")
print("|---|---|---|---|---|")
for (name, doc), modes in sorted(rows.items()):
    if "off" not in modes or "on" not in modes:
        continue
    (off, offe), (on, one) = modes["off"], modes["on"]
    cost = (off - on) / off * 100
    noise = (offe / off + one / on) * 100
    flag = "" if abs(cost) <= noise else " ▼" if cost > 0 else " ▲"
    print(f"| {name} | {doc} | {off:,.0f} ± {offe / off * 100:.1f}% | {on:,.0f} ± {one / on * 100:.1f}% | {cost:+.1f}%{flag} |")
