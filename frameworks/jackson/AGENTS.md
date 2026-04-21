# Module: frameworks/jackson

Jackson-based JSON filter for **untrusted** input.

## Purpose

Uses Jackson's streaming API (`JsonParser` / `JsonGenerator`) to parse, filter, and re-serialize JSON. Validates structure while applying the same anonymize/prune rules as `impl/core`. Use this when input may be malformed or comes from an external source.

## Key types

| Type | Description |
|---|---|
| `JacksonJsonLogFilterBuilder` | Entry point — fluent builder for Jackson filters |
| `JacksonJsonFilterFactory` | Factory wired into the builder |
| `JacksonJsonFilter` | Core Jackson filter interface |
| `DefaultJacksonJsonFilter` | Pass-through (no-op) Jackson filter |
| `JacksonMaxStringLengthJsonFilter` | Caps string values using Jackson tokenizer |
| `JacksonMaxSizeJsonFilter` | Truncates at a max document size |
| `JacksonAnyPathMaxStringLengthJsonFilter` | `$..key` with string-length cap |
| `JacksonPathMaxStringLengthJsonFilter` | Exact/wildcard path with string-length cap |

## Dependencies

- `api` (compile)
- `base` (compile)
- `tools.jackson.core:jackson-core` (compile)
- `commons-io` (compile)
- `test` (test)

## Build & test

```bash
# from repo root
mvn test -pl frameworks/jackson

# or from this directory
mvn test
```

## Notes

- Jackson version is managed in the root `pom.xml` (`jackson.version` property).
- Jackson validates JSON structure; invalid input returns `null` from `process()`.
- Significantly slower than `impl/core` — prefer `core` for trusted input.
