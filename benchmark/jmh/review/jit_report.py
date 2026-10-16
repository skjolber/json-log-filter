#!/usr/bin/env python3
"""Summarize -XX:+PrintCompilation and -XX:+PrintInlining output of JMH forks (campaign.sh jit_log).

Usage: jit_report.py LOG...

Per log, for the library's methods (com.github.skjolber.jsonfilter, outside the jmh package):
- the time (ms since JVM start) of each method's first C1 (tiers 1-3) and C2 (tier 4) compilation,
  and whether the first C2 compilation was on stack replacement (OSR, %),
- how often C2 code was made not entrant, i.e. deoptimized or replaced (C1 code being replaced by C2 is
  normal and not counted),
- inlining failures of library callees within C2 compilations, by reason (e.g. "hot method too big",
  "already compiled into a big method", "inlining too deep", "NodeCountInliningCutoff").
"""
import collections, re, sys

LIB = "com.github.skjolber.jsonfilter."
COMPILE = re.compile(r"^\s*(\d+)\s+(\d+)\s+([%sbn! ]{0,6})\s*(\d)\s+(\S+::\S+)\s+\((\d+) bytes\)(.*)$")
INLINE = re.compile(r"@\s*\d+\s+(\S+::\S+)\s+\((\d+) bytes\)\s+(.*)$")

def short(method):
    return method.replace(LIB, "")

def lib(method):
    return method.startswith(LIB) and ".jmh." not in method

for path in sys.argv[1:]:
    first_c1, first_c2 = {}, {}
    c2_not_entrant = collections.Counter()
    failures = collections.defaultdict(collections.Counter)
    sizes = {}
    tier = None
    for line in open(path, errors="replace"):
        m = COMPILE.match(line)
        if m:
            ts, _, flags, t, method, size, rest = m.groups()
            if "made not entrant" in rest or "made zombie" in rest:
                if lib(method) and t == "4":
                    c2_not_entrant[method] += 1
                continue
            tier = t
            if not lib(method):
                continue
            sizes[method] = int(size)
            target = first_c2 if t == "4" else first_c1
            if method not in target:
                target[method] = (int(ts), "%" in flags)
            continue
        m = INLINE.search(line)
        if m and tier == "4":
            callee, size, decision = m.groups()
            sizes.setdefault(callee, int(size))
            if lib(callee) and not decision.startswith(("inline", "accessor")) and "intrinsic" not in decision:
                reason = re.sub(r"^failed to inline:\s*", "", decision.strip())
                failures[callee][reason] += 1
    print(f"## {path.rsplit('/', 1)[-1]}\n")
    print("| Method | Bytes | First C1 ms | First C2 ms | C2 code made not entrant |")
    print("|---|---|---|---|---|")
    methods = sorted(set(first_c1) | set(first_c2), key=lambda m: -sizes.get(m, 0))
    for method in methods:
        c1, c2 = first_c1.get(method), first_c2.get(method)
        fmt = lambda c: "" if c is None else f"{c[0]}{' (OSR)' if c[1] else ''}"
        print(f"| {short(method)} | {sizes.get(method, '')} | {fmt(c1)} | {fmt(c2) or 'never'} | {c2_not_entrant.get(method, 0)} |")
    if failures:
        print("\nLibrary callees not inlined in C2 compilations:\n")
        print("| Callee | Bytes | Reason | Count |")
        print("|---|---|---|---|")
        for callee in sorted(failures, key=lambda c: -sum(failures[c].values())):
            for reason, count in failures[callee].most_common():
                print(f"| {short(callee)} | {sizes.get(callee, '')} | {reason} | {count} |")
    print()
