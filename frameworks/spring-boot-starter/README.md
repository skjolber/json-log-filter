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

## Untrusted sources
When requests or responses come from untrusted sources, add the parameters:

 * `validate`: parse the document to see whether its JSON structure is valid. 
 * `compact`: remove all linebreaks

```yaml
jsonfilter:
   paths:
     - antMatcher: /*
       request:
           compact: true
           validate: true
```

The output can be safely appended (as raw JSON) to any log output.