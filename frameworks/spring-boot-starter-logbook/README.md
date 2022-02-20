# spring-boot-starter-logbook
Spring Boot starter configuration for high-performance request-response-logging using Logbook:

 * per-path JSON filtering
   * filter only requests / responses with sensitive data, pass through the rest
 * reduced workload
   * detects framework data-binding so to avoid validating of JSON documents

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
In general, incoming requests must be checked for well-formed and compacted before logging. 
If not, the result could be invalid and/or multi-line log statements when logging to console, 
which will cause the logging subsystem to treat structured data as raw text.

This implementation uses the built-in REST service databinding to detect whether the incoming requests are well-formed.
So it avoids parsing the data an additional time just for logging, and can also use the much faster
JSON filters provided within this project. This is a considerable reduction in complexity / cost for request-response logging.

Responses produced by our own services are assumed to be well-formed and without linebreaks (i.e. standard JSON).

### Streaming
For request streaming approaches, like

```java
@PostMapping(path = "/myStreaming", consumes = "application/json", produces = "application/json")
public MyEntity unprotectedPost(HttpServletRequest request) throws IOException;
```

the databinding detector will not work, so the request-logging will be on the slow path.

[Logbook]:		https://github.com/zalando/logbook
