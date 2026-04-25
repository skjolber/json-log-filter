# Module: base

Shared abstract base classes for filter implementations.

## Purpose

Provides reusable scaffolding — abstract filters, builder DSL, and range-tracking data structures — that both `impl/core` and `frameworks/jackson` extend. Keeps cross-cutting concerns (path matching, string-length capping, max-size truncation) in one place.

## Key types

| Type | Description |
|---|---|
| `AbstractJsonFilter` | Base class for all filters; handles metrics and default `process` overloads |
| `AbstractJsonFilterFactory` | Wires configuration into filter selection logic |
| `AbstractJsonLogFilterBuilder` | Fluent DSL shared by `DefaultJsonLogFilterBuilder` and `JacksonJsonLogFilterBuilder` |
| `AbstractPathJsonFilter` | Adds path-list matching to `AbstractJsonFilter` |
| `AbstractMultiPathJsonFilter` | Handles multiple simultaneous path rules |
| `AbstractRangesFilter` | Tracks byte/char ranges to prune or anonymize |

## Dependencies

- `api` (compile)

## Build & test

```bash
# from repo root
mvn test -pl base

# or from this directory
mvn test
```

## Notes

- No runtime dependencies outside `api` and the JDK.
- Abstract classes here are not public API — do not expose them to end-users.

