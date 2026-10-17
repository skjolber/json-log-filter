# Changelog

## 7.0.0

A review of every filter for correctness, followed by a round of performance work: most filters are now faster than in
6.0.8, char[] and `String` input by the most. Some changes are not backwards compatible, see below.

### Breaking changes

* `DefaultJsonFilterMetrics` getters return `long` instead of `int`, so that the totals do not overflow (#323). Code
  compiled against 6.0.8 must be recompiled, and `int` variables holding the totals must become `long`.
* Configurations which could never work are rejected when the filter is created, with an `IllegalArgumentException`:
  * zero max path matches (#292),
  * path expressions which can never match (#294),
  * path expressions and keys with a backslash (#300),
  * zero prepared levels in the pretty printer's `Indent` builder (#301).
* The Jackson filters validate the input by default (#293), and reject input with more than one root value (#305).
* When both prune and anonymize match the same value, prune now takes precedence (#288).
* Building from source requires Maven 3.9.10 or later; the Maven Wrapper (`./mvnw`) is included (#324).

### New

* `ThreadSafeJsonFilterMetrics`, for metrics shared between threads, which can also add up other metrics, for example
  a `DefaultJsonFilterMetrics` per request. Both metrics classes have an informative `toString()` (#343).
* The Jackson filters support mixing key rules and full path rules (#291).
* The Jackson max size metric records the size of the input left out (#322).

### Fixes

* Bug fixes in max size, string truncation, whitespace removal, pretty printing, metrics and the Jackson filters.

### Performance

Approximate gains, measured on JDK 25 and 27:

| Area | Approximately | How |
|---|---|---|
| char[] and `String` input, most core filters | 15% to 90% faster than 6.0.8 | Four characters per iteration in the quote searches (#326, #328), own truncation helper for max size (#327) |
| `process(String)` when the filter changes nothing | 75% to 150% faster than 6.0.8 | The input is returned as it is (#330, #332, #333, #340) |
| `process(byte[])` | 9% to 16% faster, up to 2.5 times when there is nothing to do | No extra copy of the output (#329, #333), a single copy when there is nothing to do (#340) |
| Any-path filters | 10% to 20% faster | Most keys are rejected on their first character (#331) |
| Whitespace removal on pretty-printed input | up to 170% faster | Runs of whitespace are skipped several characters at a time (#334, #335, #336, #339, #341) |
| byte[] input, most filters | 1% to 6% faster | One operation less per eight bytes in the quote search (#338) |
| Jackson, when truncating strings | 6% to 15% faster on 2 KB documents | The truncation builder is allocated only when a value is truncated (#321) |
| byte[] max string length and max size with whitespace removal, 2 KB documents | about 13% slower than 6.0.8 | |
