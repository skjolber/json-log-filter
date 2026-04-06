# Iteration 2: Length Pre-filter

## Strategy

Add a length check inside the candidate loop before calling `matchPath()` to skip candidates with mismatched lengths early:

```java
int len = end - start;
for(int idx : candidates) {
    byte[] fn = fieldNameBytes[idx];
    if(fn.length == len && AbstractPathJsonFilter.matchPath(source, start, end, fn)) {
        return next[idx];
    }
}
```

## Result

**TESTS FAILED**

The length pre-filter breaks encoded key matching. For example, a JSON key like `na\u006de` (which decodes to "name") has a source length of 9 but the field name length is 4. The fast path is entered because the first character is 'n' (not '\'), but the length check incorrectly rejects the match.

The fundamental issue: we cannot know if a key contains backslash escapes until we've fully scanned it. The existing `matchPath` method handles this, but a simple length pre-filter does not.

## Decision

**DROP** - Breaks encoded key matching; tests fail.
