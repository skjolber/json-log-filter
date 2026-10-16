#!/usr/bin/env python3
"""Bytecode size of the library's methods in a benchmark jar, against the HotSpot limits.

Usage: bytecode_sizes.py JAR [MIN_BYTES]

Methods larger than HugeMethodLimit (8000 bytes) are never compiled (unless -XX:-DontCompileHugeMethods).
Methods larger than FreqInlineSize (325) are not inlined even when hot, and larger than MaxInlineSize (35)
only when hot. Lists methods of at least MIN_BYTES (default 325).
"""
import re, subprocess, sys, zipfile

jar = sys.argv[1]
minimum = int(sys.argv[2]) if len(sys.argv) > 2 else 325
classes = [n[:-6].replace("/", ".") for n in zipfile.ZipFile(jar).namelist()
           if n.startswith("com/github/skjolber/jsonfilter/") and n.endswith(".class") and "/jmh/" not in n]
text = subprocess.run(["javap", "-c", "-p", "-classpath", jar] + classes, capture_output=True, text=True).stdout

sizes = []
cls = method = None
last = -1
for line in text.splitlines():
    if re.match(r"^(public |protected |private |abstract |final |static |sealed |non-sealed )*(class|interface|enum|record) ", line) or line.startswith("Compiled from"):
        if method is not None and last >= 0:
            sizes.append((last + 1, cls, method))
        method, last = None, -1
        m = re.search(r"(?:class|interface|enum|record) ([\w.$]+)", line)
        if m:
            cls = m.group(1)
        continue
    if re.match(r"^  \S.*\(.*\).*;$", line) or line.startswith("  static {};"):
        if method is not None and last >= 0:
            sizes.append((last + 1, cls, method))
        method, last = line.strip(), -1
        continue
    m = re.match(r"^\s+(\d+): \w+", line)
    if m and method is not None:
        last = int(m.group(1))
if method is not None and last >= 0:
    sizes.append((last + 1, cls, method))

sizes.sort(reverse=True)
print(f"{len(sizes)} methods; > 8000 (HugeMethodLimit): {sum(s > 8000 for s, _, _ in sizes)}; "
      f"> 325 (FreqInlineSize): {sum(s > 325 for s, _, _ in sizes)}; > 35 (MaxInlineSize): {sum(s > 35 for s, _, _ in sizes)}\n")
print("| Bytes (approx.) | Method |")
print("|---|---|")
for size, c, m in sizes:
    if size < minimum:
        break
    name = re.search(r"([\w$]+)\((.*)\)", m)
    if name is None:  # static initializer
        print(f"| {size} | {c.replace('com.github.skjolber.jsonfilter.', '')}.<clinit> |")
        continue
    args = ", ".join(a.strip().split(".")[-1] for a in name.group(2).split(",")) if name.group(2) else ""
    print(f"| {size} | {c.replace('com.github.skjolber.jsonfilter.', '')}.{name.group(1)}({args}) |")
