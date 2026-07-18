# webapp — JSON Log Filter Demo

Interactive browser-based demo of the
[json-log-filter](https://github.com/skjolber/json-log-filter) library,
compiled from Java to JavaScript using [TeaVM](https://teavm.org/) and
served as a **static site on GitHub Pages**.

Live demo: <https://skjolber.github.io/json-log-filter/>

---

## Module layout

```
webapp/teavm/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── …/web/JsonFilterApp.java            Entry point (@JSExport)
    │   └── …/core/util/ByteArrayRangesFilter.java  TeaVM-safe stub (no VarHandle)
    └── webapp/index.html                       Static page served by GitHub Pages
```

---

## Architecture

TeaVM compiles Java **bytecode** to JavaScript, so the webapp depends on the
real library JARs (`api`, `base`, `impl/core`) via ordinary Maven coordinates —
no source JARs or custom build plugins needed.

### TeaVM-safe stub

`ByteArrayRangesFilter` in `impl/core` uses
`java.lang.invoke.MethodHandles.byteArrayViewVarHandle()` in its static
initialiser.  TeaVM has no `VarHandle` implementation, so the class fails to
compile.

A **stub** is placed at the same package/class name in the webapp's own source
tree (`com.github.skjolber.jsonfilter.core.util.ByteArrayRangesFilter`).  Maven
puts project classes before dependency classes on the classpath, so TeaVM picks
up the stub and never sees the original.  All methods in the stub throw
`UnsupportedOperationException`; they are never reachable at runtime because the
`DefaultJsonLogFilterBuilder` code paths that use byte-array filters are not
exercised by the String-based `JsonFilter.process(String)` entry point.

### JS export

`@JSExport` on the static `applyFilter(...)` method causes TeaVM to expose it
as a plain global variable in the IIFE output:

```js
var applyFilter;   // declared at file scope
(function() {
  // … 20 000 lines of compiled Java …
  applyFilter = cgsjw_JsonFilterApp_applyFilter$exported$0;
})();
```

`index.html` captures this reference immediately after loading `app.js`,
before any page script can shadow the global name:

```html
<script src="js/app.js"></script>
<script>
  var _teaVMApplyFilter = applyFilter;
  window.JsonFilterApp = {
    applyFilter: function() { return _teaVMApplyFilter.apply(null, arguments); }
  };
</script>
```

---

## UI features

| Category  | Controls |
|-----------|----------|
| Presets   | 4 built-in examples that populate all fields |
| Anonymize | Keys (any depth), JSONPath expressions, replacement text |
| Prune     | Keys (any depth), JSONPath expressions, replacement text |
| Limits    | Max string length, max document size, max path matches |
| Options   | Remove insignificant whitespace, live filtering toggle |
| Counts    | Input / output character counts and size reduction % |

**Live filtering** (on by default) runs the filter on every keystroke.
Uncheck the "Live filtering" checkbox to show an explicit Apply button instead.

---

## Build requirements

| Tool  | Version |
|-------|---------|
| Java  | 17+     |
| Maven | 3.6.3+  |

---

## Building locally

```bash
# Build the library first, then the webapp
cd /path/to/json-log-filter
mvn install -pl api,base,impl/core --no-transfer-progress -DskipTests
cd webapp/teavm
mvn package --no-transfer-progress -DskipTests
```

The compiled static site lands at:

```
webapp/teavm/target/webapp/
├── index.html
└── js/
    └── app.js   ← ~930 KB compiled JavaScript
```

Serve locally:

```bash
cd webapp/teavm/target/webapp
python3 -m http.server 8080
# → http://localhost:8080/
```

---

## GitHub Pages deployment

`.github/workflows/pages.yml` builds and deploys on every push to
`main`/`master` that touches `webapp/teavm/**`, `api/**`, `base/**`, or
`impl/core/**`.

**One-time repository setup:**

1. Go to **Settings → Pages**.
2. Under **Source**, select **GitHub Actions**.
3. Push a change — the workflow fires and the site goes live at
   `https://<owner>.github.io/<repo>/`.

Use **Actions → Deploy to GitHub Pages → Run workflow** to trigger manually.

---

## Maven deployment

`<maven.deploy.skip>true</maven.deploy.skip>` is set in the module's
`pom.xml` — this module is never published to Maven Central.
