# path
Configuration for per-path JSON filtering for request and/or responses.

```
List<JsonFilterPathMatcher> requests = new ArrayList<>();
// your code here
List<JsonFilterPathMatcher> responses = new ArrayList<>();
// your code here

RequestResponseJsonFilter requestResponseJsonFilter = new RequestResponseJsonFilter(requests, responses);
```

then define a max size, if relevant

```
int maxSize = 128_000;
```

upon request, get the path

```

String path = ...;

```

```
JsonFilter requestFilter = requestResponseJsonFilter.getRequestFilter(path, false, maxSize));

// your code here

```
where `false` denotes that input is not known to be valid JSON.

Then for the response

```
JsonFilter responseFilter = requestResponseJsonFilter.getResponseFilter(path, true, maxSize));

// your code here

```
where `true` denotes that input is known to be valid JSON.
