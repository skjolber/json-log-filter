# Module: impl/core

High-performance, zero-dependency JSON filter for **trusted** input.

## Purpose

Single-pass char/byte streaming filters that assume well-formed JSON. No object model is built. This is the fastest implementation.

## Key types

| Type | Description |
|---|---|
| `DefaultJsonLogFilterBuilder` | Entry point — builds the right filter for the requested feature set |
| `DefaultJsonFilterFactory` | Factory wired into the builder |
| `MaxStringLengthJsonFilter` | Caps string value length |
| `MaxSizeJsonFilter` | Truncates output at a max document size |
| `PathJsonFilter` | Anonymizes/prunes values at exact or wildcard JSON paths |
| `FullPathJsonFilter` | Full `$.a.b.c` path evaluation |
| `AnyPathJsonFilter` | `$..key` deep-scan path evaluation |

Sub-packages:
- `core/pp/` — pretty-print variants
- `core/ws/` — whitespace-removal variants
- `core/util/` — internal helpers

## Dependencies

- `api` (compile)
- `base` (compile)
- `test` (test)

## Build & test

```bash
# from repo root
mvn test -pl impl/core

# or from this directory
mvn test
```

## Notes

- JaCoCo enforces ≥ 80% instruction coverage and 0 missed classes.
- Input is assumed structurally valid; malformed JSON may produce undefined output.
- Use `frameworks/jackson` when input cannot be trusted.
