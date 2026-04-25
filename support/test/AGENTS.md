# Module: support/test

Shared test utilities for json-log-filter modules.

## Purpose

Provides reusable test infrastructure — JSON generators, directory-based test suites, Truth extensions, and filter test harnesses — used by `base`, `impl/core`, `impl/path`, and `frameworks/jackson`. Compiled as a regular `compile`-scoped jar so other modules can declare it as a `test`-scoped dependency.

## Key types

| Type | Description |
|---|---|
| `AbstractJsonFilterDirectoryUnitTest` | Runs a filter against a directory of input/expected JSON files |
| `JsonFilterDirectoryUnitTestCollection` | Collects test cases from a directory tree |
| `JsonFilterDirectoryUnitTestCollectionRunner` | JUnit 5 extension that drives directory tests |
| `JsonTestSuiteRunner` | Runs the full json-test-suite against a filter |
| `Generator` | Produces synthetic JSON payloads for property-based testing |
| `JsonFilterConstants` | Shared constants (anonymization markers, prune tokens, etc.) |
| `JsonPathFilter` | Helper that applies a JsonPath expression for assertion purposes |
| `cache/` | Caches parsed test fixtures to speed up repeated runs |
| `directory/` | File-system helpers for locating test resource directories |
| `truth/` | Google Truth custom subjects for `JsonFilter` assertions |
| `jackson/` | Jackson-specific test helpers |

## Dependencies

All compile-scoped (so they are available to test classpaths of dependents):
- `api`, `jackson-core`, `jackson-databind`, `json-path`, `classgraph`, `slf4j-api`, `commons-io`, `jimfs`, JUnit 5, Google Truth

## Build & test

```bash
# from repo root
mvn test -pl support/test

# or from this directory
mvn test
```

## Notes

- Sonar analysis is skipped for this module (`sonar.skip=true`).
- Add new shared test helpers here rather than duplicating across modules.
- Do not add production (non-test) filter logic to this module.
