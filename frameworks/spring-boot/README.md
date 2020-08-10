# spring-boot-starter
Spring Boot starter configuration for per-path JSON filtering for request and/or responses.

```yaml
jsonfilter:
   paths:
     - antMatcher: /*
       request:
           maxStringLength: 127
           anonymizes:
             - /key1/key2
           prunes:
             - /key3/key4
       response:
           maxStringLength: 127 

```

For untrusted requests, add `compact` and `validate` 

```yaml
jsonfilter:
   paths:
     - antMatcher: /*
       request:
           compact: true
           validate: true
```
