#!/usr/bin/env python3
"""Merge JMH JSON results from separate single-fork runs into one result file.

Usage: merge_forks.py OUT.json IN1.json IN2.json ...

For each benchmark and params, the per-fork iteration values (primaryMetric.rawData) are concatenated,
and score and scoreError are recomputed like JMH does for a multi-fork run: the mean of all iterations,
and the 99.9% confidence interval half-width, t(0.9995, n - 1) * stddev / sqrt(n).
"""
import json, math, sys


def betacf(a, b, x):
    # continued fraction for the incomplete beta function (Numerical Recipes)
    MAXIT, EPS, FPMIN = 300, 3e-16, 1e-300
    qab, qap, qam = a + b, a + 1.0, a - 1.0
    c, d = 1.0, 1.0 - qab * x / qap
    d = 1.0 / (d if abs(d) > FPMIN else FPMIN)
    h = d
    for m in range(1, MAXIT + 1):
        m2 = 2 * m
        aa = m * (b - m) * x / ((qam + m2) * (a + m2))
        d = 1.0 + aa * d
        d = 1.0 / (d if abs(d) > FPMIN else FPMIN)
        c = 1.0 + aa / c
        c = c if abs(c) > FPMIN else FPMIN
        h *= d * c
        aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
        d = 1.0 + aa * d
        d = 1.0 / (d if abs(d) > FPMIN else FPMIN)
        c = 1.0 + aa / c
        c = c if abs(c) > FPMIN else FPMIN
        de = d * c
        h *= de
        if abs(de - 1.0) < EPS:
            break
    return h


def betai(a, b, x):
    # regularized incomplete beta function I_x(a, b)
    if x <= 0.0:
        return 0.0
    if x >= 1.0:
        return 1.0
    lbt = math.lgamma(a + b) - math.lgamma(a) - math.lgamma(b) + a * math.log(x) + b * math.log(1.0 - x)
    if x < (a + 1.0) / (a + b + 2.0):
        return math.exp(lbt) * betacf(a, b, x) / a
    return 1.0 - math.exp(lbt) * betacf(b, a, 1.0 - x) / b


def t_cdf(t, df):
    x = df / (df + t * t)
    tail = 0.5 * betai(df / 2.0, 0.5, x)
    return 1.0 - tail if t >= 0 else tail


def t_quantile(p, df):
    lo, hi = 0.0, 1000.0
    for _ in range(200):
        mid = (lo + hi) / 2
        if t_cdf(mid, df) < p:
            lo = mid
        else:
            hi = mid
    return (lo + hi) / 2


def score_error(values, confidence=0.999):
    n = len(values)
    if n <= 2:
        return float("nan")
    mean = sum(values) / n
    stddev = math.sqrt(sum((v - mean) ** 2 for v in values) / (n - 1))
    return t_quantile(1 - (1 - confidence) / 2, n - 1) * stddev / math.sqrt(n)


def key(result):
    return (result["benchmark"], json.dumps(result.get("params", {}), sort_keys=True))


def merge(paths):
    merged, order = {}, []
    for path in paths:
        for result in json.load(open(path)):
            k = key(result)
            if k not in merged:
                merged[k] = json.loads(json.dumps(result))
                merged[k]["primaryMetric"]["rawData"] = []
                merged[k]["forks"] = 0
                order.append(k)
            merged[k]["primaryMetric"]["rawData"].extend(result["primaryMetric"]["rawData"])
            merged[k]["forks"] += len(result["primaryMetric"]["rawData"])
    out = []
    for k in order:
        result = merged[k]
        values = [v for fork in result["primaryMetric"]["rawData"] for v in fork]
        metric = result["primaryMetric"]
        metric["score"] = sum(values) / len(values)
        metric["scoreError"] = score_error(values)
        metric["scoreConfidence"] = [metric["score"] - metric["scoreError"], metric["score"] + metric["scoreError"]]
        metric.pop("scorePercentiles", None)
        result.pop("secondaryMetrics", None)
        out.append(result)
    return out


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    json.dump(merge(sys.argv[2:]), open(sys.argv[1], "w"), indent=2)
