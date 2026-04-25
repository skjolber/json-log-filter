# Module: api

Public API for json-log-filter. All consumer-facing contracts live here.

## Purpose

Defines the core interfaces and value types that all filter implementations must satisfy. Consumers program to these abstractions; concrete implementations are in `impl/core`, `impl/path`, and `frameworks/jackson`.

## Key types

| Type | Description |
|---|---|
| `JsonFilter` | Primary interface — `process(String)`, `process(byte[])`, `process(char[])` |
| `JsonFilterFactory` | Creates `JsonFilter` instances from a configuration |
| `JsonFilterFactoryProperty` | Configuration keys used by factories |
| `JsonFilterMetrics` | Collects filter statistics (truncations, anonymizations, etc.) |
| `ResizableByteArrayOutputStream` | Utility stream used by filter implementations |

## Build & test

```bash
# from repo root
mvn test -pl api

# or from this directory
mvn test
```

## Notes

- No runtime dependencies outside the JDK.
- Java 17 minimum; compiled with module support via moditect.
- Changes here affect every downstream module — update all implementations after modifying interfaces.
