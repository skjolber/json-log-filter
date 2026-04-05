# Plan: iter-006-arrays-equals-matchpath

## Target
`AbstractPathJsonFilter.matchPath(byte[], int, int, byte[])` and `matchPath(char[], int, int, char[])` in `base/src/main/java/com/github/skjolber/jsonfilter/base/AbstractPathJsonFilter.java`

## Hypothesis
The byte-by-byte loop in `matchPath` fires hundreds of times per CVE document (every JSON field name comparison). On JDK 25 / ARM64, `Arrays.equals(byte[], int, int, byte[], int, int)` is intrinsified to a vectorized SIMD comparison, equivalent to a single `memcmp`-style instruction sequence. Replacing the manual loop eliminates loop overhead and branch mispredictions.

Same applies to the char[] overload — `Arrays.equals(char[], int, int, char[], int, int)` is also intrinsified on JDK 25.

## Change
Replace in both `matchPath` overloads:
```java
for(int i = 0; i < attribute.length; i++) {
    if(attribute[i] != source[start + i]) {
        return false;
    }
}
return true;
```
With:
```java
return Arrays.equals(attribute, 0, attribute.length, source, start, start + attribute.length);
```

## Caveat
Only branch 1 (`attribute.length == l`) is touched. `matchesEncoded()` is NOT changed.
`Arrays` is already imported in the file.

## Expected gain
~1500–3000 ops/sec. The `anon_any_core` and `anon_full_core` benchmarks each spend significant time in key matching for CVE JSON with ~10+ nested field names per record.
