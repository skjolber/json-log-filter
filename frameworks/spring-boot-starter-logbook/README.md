# spring-boot-starter-logbook
Spring Boot starter configuration for high-performance request-response-logging using Logbook:

 * per-path JSON filtering
   * filter only requests / responses with sensitive data, pass through the rest
 * reduced workload
   * awaits framework data-binding so to avoid unnecessary revalidation of JSON payloads when construction request log statement.

Configuration example:

```yaml
jsonfilter:
  logbook:
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

## Example log configuration for JSON logging
Add the following configuration:

```java
@Configuration
public class LogConfiguration {

	@Bean
	public FastJsonHttpLogFormatter formatter() {
		return new FastJsonHttpLogFormatter();
	}
	
	@Bean
	public Sink configure(HttpLogFormatter f) {
		return new LogstashLogbackSink(f);
	}
	
	// XXX your filter here. The default compacting filter can be omitted.
	@Bean
	public BodyFilter createBodyFilter() {
		return BodyFilter.none(); 
	}
}
```

## Performance
In general, incoming requests should be checked for well-formed and compacted before logging, 
so that the resulting log statement is valid JSON.

Invalid log statements which will typically cause the log accumulation tool to treat structured data as raw text.
So that means indexing the log statement will not work very well, thus various metrics and searches will suffer.

This implementation uses the built-in REST service data-binding to detect whether the incoming requests are well-formed.
It then avoids parsing the data an additional time just for JSON logging, and can also use the much faster
JSON filters provided within this project. 

__This is a considerable reduction in complexity / cost for request-response logging to JSON.__

Responses produced by our own services are assumed to be well-formed and without line-breaks (i.e. standard JSON).

### Streaming
For request streaming approaches, like

```java
@PostMapping(path = "/myStreaming", consumes = "application/json", produces = "application/json")
public MyEntity unprotectedPost(HttpServletRequest request) throws IOException;
```

the request-logging will be on the slow path.

[Logbook]:		https://github.com/zalando/logbook
