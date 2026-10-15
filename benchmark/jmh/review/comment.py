#!/usr/bin/env python3
"""Build a PR comment with JMH results: summary, method, and a table per result pair."""
import json, os, sys, subprocess

HERE = os.path.dirname(os.path.abspath(__file__))

def table(base, other, filt=""):
    return subprocess.run(["python3", "-I", os.path.join(HERE, "compare.py"), base, other, filt], capture_output=True, text=True).stdout

summary = sys.argv[1]
pairs = sys.argv[2:]
out = ["**Benchmark results**", "", summary, "",
       "Method: JMH, " + os.environ.get("FORKS", "1 fork") + ", 3 warmup and 5 measurement iterations of 1 s, CVE documents of 2 KB, 22 KB and 200 KB, Java 25, CPU boost off, nothing else running. "
       "Master is " + subprocess.run(["git", "-C", HERE, "rev-parse", "--short", os.environ.get("TARGET", "origin/master")], capture_output=True, text=True).stdout.strip() + ". " +
       (os.environ.get("ERRNOTE") or "The ± is JMH's error within a fork. Between forks, results on this machine vary by about ±3%, and by up to ±10% for a few benchmarks. ") + (os.environ.get("NOTE", "") + " " if os.environ.get("NOTE") else "") + 
       ("" if os.environ.get("ERRNOTE") else "▲ and ▼ mark changes beyond the within-fork error, which can still be fork-to-fork noise."), ""]
for p in pairs:
    title, base, other, *f = p.split("|")
    out.append(f"<details><summary>{title}</summary>\n")
    out.append(table(base, other, f[0] if f else ""))
    out.append("</details>\n")
out.append("🤖 Generated with [Claude Code](https://claude.com/claude-code)")
print("\n".join(out))
