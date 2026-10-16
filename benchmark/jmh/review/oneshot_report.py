#!/usr/bin/env python3
"""Summarize single-shot JMH runs (one JSON file per side and fork) from campaign.sh oneshot.

Usage: oneshot_report.py DIR

For each benchmark, params and side: the median over forks of the first call (iteration 1, a fresh JVM),
of iterations 2-5, and of the last iteration, in microseconds.
"""
import glob, json, os, re, statistics, sys

out = {}
for path in sorted(glob.glob(os.path.join(sys.argv[1], "*-fork*.json"))):
    side = re.sub(r"-fork\d+\.json$", "", os.path.basename(path))
    for r in json.load(open(path)):
        name = r["benchmark"].rsplit(".", 2)
        key = (name[-2] + "." + name[-1], ",".join(str(v) for _, v in sorted(r.get("params", {}).items())))
        for fork in r["primaryMetric"]["rawData"]:
            out.setdefault(key, {}).setdefault(side, []).append(fork)

def med(values):
    return statistics.median(values) if values else float("nan")

sides = sorted({s for v in out.values() for s in v})
print("| Benchmark | Document | Side | Forks | 1st call µs | calls 2-5 µs | last call µs | 1st / last |")
print("|---|---|---|---|---|---|---|---|")
for key in sorted(out):
    for side in sides:
        forks = out[key].get(side, [])
        if not forks:
            continue
        first = med([f[0] for f in forks])
        early = med([v for f in forks for v in f[1:5]])
        last = med([f[-1] for f in forks])
        print(f"| {key[0]} | {key[1]} | {side} | {len(forks)} | {first:,.0f} | {early:,.0f} | {last:,.0f} | {first / last:,.1f}x |")
