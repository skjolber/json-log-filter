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

Compared with 6.0.8 on JDK 25 and 27, approximately:

* char[] and `String` input: 15% to 90% faster for most core filters. The search for the end of a string checks four
  characters per iteration (#328), as does the backward max size scan (#326), and the max size loop has its own copy
  of the string truncation helper (#327).
* `process(String)` on documents the filter leaves unchanged: 75% to 150% faster, as the input is returned as it is
  (#330, #333). The max size filters return the input when the document fits (#332), and whitespace removal when
  there is no whitespace (#340).
* `process(byte[])`: the ranges filters return their output without an extra copy (#329, #333), the max size filters
  and whitespace removal a single copy when there is nothing to do (#340).
* Any-path filters: 10% to 20% faster, as most keys are rejected on their first character (#331).
* Whitespace removal on pretty-printed input: up to 170% faster, as runs of whitespace are skipped several characters
  at a time (#334, #335, #336, #339, #341).
* byte[] quote search: one operation less per eight bytes (#338).
* Jackson: allocate the truncation builder only when a value is truncated (#321).

Still slower than 6.0.8: byte[] max string length and max size with whitespace removal, by about 13% on 2 KB
documents.

### Build and other

* Code is indented with tabs, and lines are at most 200 columns (#342).
* Webapp improvements, including a path helper, loading from a file or URL and a download button (#275, #277, #278,
  #325).
* Jackson 3.1.7 (#276, #283, #284, #285), and updated GitHub Actions (#271 to #274, #280, #282).
