# Module: impl/path

JSONPath-aware request/response filter combining `core` and `jackson`.

## Purpose

Provides `RequestResponseJsonFilter` for scenarios where the same filter must handle both trusted (core) and untrusted (jackson) JSON within a single request/response pair, and `RequestResponseJsonFilterFactory` to build these filters.

## Key types

| Type | Description |
|---|---|
| `RequestResponseJsonFilter` | Applies different filters to request vs response payloads |
| `RequestResponseJsonFilterFactory` | Creates `RequestResponseJsonFilter` instances |
| `matcher/` | Path expression compiler and matching engine |
| `properties/` | Spring/config-property binding for filter configuration |

## Dependencies

- `api` (compile)
- `core` (compile)
- `jackson` (compile)

## Build & test

```bash
# from repo root
mvn test -pl impl/path

# or from this directory
mvn test
```

## Notes

- Depends on both `core` and `jackson`; do not add further framework dependencies.
- Path matcher is shared between core and jackson paths — changes affect both.
