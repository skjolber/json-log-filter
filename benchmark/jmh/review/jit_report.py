#!/usr/bin/env python3
"""Summarize -XX:+PrintCompilation and -XX:+PrintInlining output of JMH forks (campaign.sh jit_log).

Usage: jit_report.py LOG...

Per log, for the library's methods (com.github.skjolber.jsonfilter, outside the jmh package):
- the time (ms since JVM start) and tier of each method's first C1 (tiers 1-3) and C2 (tier 4) compilation,
  and whether it was an OSR compilation (%),
- how often a compiled version was made not entrant (deoptimized or replaced),
- inlining failures of library callees, by reason (e.g. "too big", "hot method too big",
  "already compiled into a big method", "callee is too large").
"""
import collections, re, sys

LIB = "com.github.skjolber.jsonfilter."
COMPILE = re.compile(r"^\s*(\d+)\s+(\d+)\s+([%sbn! ]{0,6})\s*(\d)\s+(\S+::\S+)\s+\((\d+) bytes\)(.*)$")
INLINE = re.compile(r"@\s*\d+\s+(\S+::\S+)\s+\((\d+) bytes\)\s+(.*)$")

def short(method):
    return method.replace(LIB, "")

for path in sys.argv[1:]:
    first_c1, first_c2, not_entrant = {}, {}, collections.Counter()
    failures = collections.defaultdict(collections.Counter)
    sizes = {}
    for line in open(path, errors="replace"):
        m = COMPILE.match(line)
        if m:
            ts, _, flags, tier, method, size, rest = m.groups()
            if not method.startswith(LIB) or ".jmh." in method:
                continue
            sizes[method] = int(size)
            if "made not entrant" in rest:
                not_entrant[method] += 1
                continue
            target = first_c2 if tier == "4" else first_c1
            if method not in target:
                target[method] = (int(ts), tier, "%" in flags)
            continue
        m = INLINE.search(line)
        if m:
            callee, size, decision = m.groups()
            if callee.startswith(LIB) and ".jmh." not in callee and ("fail" in decision or "too" in decision or "not inline" in decision):
                reason = re.sub(r"^(failed to inline:|failed to inline \(intrinsic\):)\s*", "", decision.strip())
                failures[callee][reason] += 1
    print(f"## {path.rsplit('/', 1)[-1]}\n")
    print("| Method | Bytes | First C1 ms | First C2 ms | Made not entrant |")
    print("|---|---|---|---|---|")
    methods = sorted(set(first_c1) | set(first_c2) | set(not_entrant), key=lambda m: -sizes.get(m, 0))
    for method in methods:
        c1 = first_c1.get(method)
        c2 = first_c2.get(method)
        fmt = lambda c: "" if c is None else f"{c[0]}{' (OSR)' if c[2] else ''}"
        print(f"| {short(method)} | {sizes.get(method, '')} | {fmt(c1)} | {fmt(c2)} | {not_entrant.get(method, 0)} |")
    if failures:
        print("\nInlining failures of library methods:\n")
        print("| Callee | Reason | Count |")
        print("|---|---|---|")
        for callee in sorted(failures, key=lambda c: -sum(failures[c].values())):
            for reason, count in failures[callee].most_common():
                print(f"| {short(callee)} | {reason} | {count} |")
    print()
