# spring-boot-starter-logbook
Spring Boot starter configuration for per-path JSON filtering for request and/or response logging with Logbook. 

```yaml
jsonfilter:
  logbook:
    compactRequests: true
    validateRequests: true
    paths:
      - matcher: /**
        request:
            maxStringLength: 127 
            anonymizes:
              - /key1/key2
            prunes:
              - /key3/key4
        response:
            maxStringLength: 127 
            anonymizes:
              - /name

```

## Untrusted sources
When requests or responses come from untrusted sources, add the parameters:

 * `validateRequests`: parse the document to see whether its JSON structure is valid in requests.
 * `compactRequests`: remove all linebreaks in requests
 * `validateResponses`: parse the document to see whether its JSON structure is valid in responses
 * `compactResponses`: remove all linebreaks in responses

```yaml
jsonfilter:
  logbook:
    compactRequests: true
    validateRequests: true
    paths:
      - matcher: /**
        request:
            maxStringLength: 127 
            anonymizes:
              - /key1/key2
            prunes:
              - /key3/key4
        response:
            maxStringLength: 127 
            anonymizes:
              - /name

```

If validate is enabled, the output can be safely appended (as raw JSON) to any log output. Invalid JSON payloads are appended as escaped JSON strings.

Note: In the current implementation, validate also means removing linebreaks.

[Logbook]:		https://github.com/zalando/logbook
