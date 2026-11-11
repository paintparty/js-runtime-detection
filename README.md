# js-runtime-detection

**Detects JS runtime details. Built for ClojureScript dialects.**\
<sub>Works with ClojureScript, Squint, nbb, Cherry, and Clojurific</sub>

<br />

```clojure
(ns my.app
  (:require [js-runtime-detection.core :as rt]))

;; Public defs are computed once, when js-runtime-detection.core loads
rt/js-runtime           ;; => :node
rt/js-runtime-is-server ;; => true
```

<br />

## Requirements

| Dialect                                                   | Version          |
| :-------------------------------------------------------- | :--------------- |
| [ClojureScript](https://github.com/clojure/clojurescript) | 1.9.854 or newer |
| [nbb](https://github.com/babashka/nbb)                    | 1.6.214          |
| [Cherry](https://github.com/squint-cljs/cherry)           | 0.6.38           |
| [Clojurific](https://github.com/WhimsicalCode/clojurific) | 0.12.15          |
| [Squint](https://github.com/squint-cljs/squint)           | 0.15.212         |

<br />

## Setup

### ClojureScript setup

deps.edn:

```clojure
io.github.paintparty/js-runtime-detection {:mvn/version "0.1.0"}
```

Leiningen:

```clojure
[io.github.paintparty/js-runtime-detection "0.1.0"]
```

### Clojurific setup

deps.edn:

```clojure
io.github.paintparty/js-runtime-detection {:mvn/version "0.1.0"}
```

### nbb setup

nbb.edn:

```clojure
{:deps {io.github.paintparty/js-runtime-detection {:mvn/version "0.1.0"}}}
```

### Squint and Cherry setup

Squint and Cherry read `:deps` from `squint.edn` or `cherry.edn`, and only take git and `:local/root` dependencies, not jars:

```clojure
{:paths ["src"]
 :deps  {io.github.paintparty/js-runtime-detection {:git/tag "v0.1.0" :git/sha "..."}}}
```

[Consuming js-runtime-detection as a git dependency](docs/git-deps.md) has the SHA of the latest release and the other forms the coordinate map can take. The same git coordinates work in deps.edn and nbb.edn.

<br />

## Usage

```clojure
(ns my.app
  (:require [js-runtime-detection.core :as rt]))

;; In Node
rt/js-runtime                ;; => :node
rt/js-platform               ;; => nil
rt/js-runtime-is-server      ;; => true
rt/js-runtime-has-dom        ;; => false
rt/js-runtime-has-jsdom      ;; => false
rt/js-runtime-has-node-apis  ;; => true
rt/js-runtime-is-strict-node ;; => true

;; The same defs in a browser
rt/js-runtime                ;; => :browser
rt/js-platform               ;; => nil
rt/js-runtime-is-server      ;; => false
rt/js-runtime-has-dom        ;; => true
rt/js-runtime-has-jsdom      ;; => false
rt/js-runtime-has-node-apis  ;; => false
rt/js-runtime-is-strict-node ;; => false
```

In Squint, keywords compile to strings, so `rt/js-runtime` is `"node"` and prints as `"node"`. Comparisons such as `(= rt/js-runtime :node)` and `(case rt/js-runtime :node ...)` still work, because `:node` is also `"node"` there.

The library runs on JS only. From a `.cljc` file, require it inside a reader conditional:

```clojure
(ns my.app
  (:require #?(:cljs [js-runtime-detection.core :as rt])))

(def server? #?(:clj true :cljs rt/js-runtime-is-server))
```

<br />

## Public defs

| def                         | Value          | Answers                                            |
| :-------------------------- | :------------- | :------------------------------------------------- |
| `js-runtime`                | keyword        | Which runtime (Node, Deno, Bun, workerd, browser)  |
| `js-platform`               | keyword or nil | Which deployment platform (Cloudflare, Vercel ...) |
| `js-runtime-is-server`      | boolean        | Is this server-side?                               |
| `js-runtime-has-dom`        | boolean        | Are `window` and `document` present?               |
| `js-runtime-has-jsdom`      | boolean        | Is jsdom active?                                   |
| `js-runtime-has-node-apis`  | boolean        | Can I use `fs`, `process.env` and other Node APIs? |
| `js-runtime-is-strict-node` | boolean        | Is this actual Node, not Bun or Deno in Node mode? |

<br />

The value of **`js-runtime`** is one of:

`:node`\
`:deno`\
`:bun`\
`:workerd`\
`:browser`\
`:service-worker`\
`:web-worker`\
`:electron-renderer`\
`:react-native`\
`:unknown`

<br />

The value of **`js-platform`** is one of:

`:cloudflare`\
`:vercel-edge`\
`:netlify-edge`\
`:fastly`\
`nil`

<br />

## Detection matrix

| Environment        | `js-runtime`         | `js-platform`   | `js-runtime-is-server` | `js-runtime-has-dom` | `js-runtime-has-jsdom` | `js-runtime-has-node-apis` | `js-runtime-is-strict-node` |
| :----------------- | :------------------- | :-------------- | :--------------------: | :------------------: | :--------------------: | :------------------------: | :-------------------------: |
| Real browser       | `:browser`           | `nil`           |         `false`        |        `true`        |         `false`        |           `false`          |           `false`           |
| Node               | `:node`              | `nil`           |         `true`         |        `false`       |         `false`        |           `true`           |            `true`           |
| jsdom in Node      | `:node`              | `nil`           |         `true`         |        `true`        |         `true`         |           `true`           |            `true`           |
| Deno 2             | `:deno`              | `nil`           |         `true`         |        `false`       |         `false`        |           `true`           |           `false`           |
| jsdom in Deno      | `:deno`              | `nil`           |         `true`         |        `true`        |         `true`         |           `true`           |           `false`           |
| Bun                | `:bun`               | `nil`           |         `true`         |        `false`       |         `false`        |           `true`           |           `false`           |
| jsdom in Bun       | `:bun`               | `nil`           |         `true`         |        `true`        |         `true`         |           `true`           |           `false`           |
| Cloudflare Workers | `:workerd`           | `:cloudflare`   |         `true`         |        `false`       |         `false`        |           `false`          |           `false`           |
| Vercel Edge\*      | `:workerd`           | `:vercel-edge`  |         `true`         |        `false`       |         `false`        |           `false`          |           `false`           |
| Netlify Edge       | `:deno`              | `:netlify-edge` |         `true`         |        `false`       |         `false`        |           `false`          |           `false`           |
| Fastly Compute\*   | `:unknown`           | `:fastly`       |         `true`         |        `false`       |         `false`        |           `false`          |           `false`           |
| Electron renderer  | `:electron-renderer` | `nil`           |         `true`         |        `true`        |         `false`        |           `true`           |           `false`           |
| Electron main      | `:node`              | `nil`           |         `true`         |        `false`       |         `false`        |           `true`           |            `true`           |
| React Native       | `:react-native`      | `nil`           |         `false`        |        `false`       |         `false`        |           `false`          |           `false`           |
| Service Worker     | `:service-worker`    | `nil`           |         `false`        |        `false`       |         `false`        |           `false`          |           `false`           |
| Web Worker         | `:web-worker`        | `nil`           |         `false`        |        `false`       |         `false`        |           `false`          |           `false`           |

\* Rows marked with an asterisk are unverified. Vercel's Edge Runtime is not workerd, so on Vercel Edge `js-runtime` may be `:unknown` rather than `:workerd`. The `:fastly` platform check relies on a `fastly` global, which newer versions of Fastly's JS runtime may no longer define.

#### jsdom

Under jsdom, `js-runtime-is-server` and `js-runtime-has-node-apis` are true, because jsdom runs on a server runtime. Pair them with `(not js-runtime-has-dom)` to exclude test environments that simulate a browser.

#### Electron renderer

The Electron renderer row assumes `nodeIntegration` is enabled. With Electron's defaults (context isolation on, `nodeIntegration` off), the renderer has no `process` global and reports the same values as a real browser.

#### Cloudflare Workers

Cloudflare Workers with the `nodejs_compat` flag (compatibility date 2025-09-15 or later) report `js-runtime-has-node-apis` as true. The rest of the row is the same.

#### Which rows are tested

These rows of the detection matrix are checked by the automated tests (see [How the tests work](#how-the-tests-work)):

- Node and jsdom in Node, in every dialect: ClojureScript (`:advanced` builds), nbb, Cherry, Clojurific and Squint
- Node without `globalThis`, in ClojureScript and Squint
- Cloudflare Workers with and without `nodejs_compat` (an `:advanced` build run in workerd through Miniflare)

The other rows in the detection matrix above follow from each platform's documented globals and have not been run on those platforms.

<br />

## How it works

The library also has no dependencies and does not import Node.js built-in modules like `fs` or `os`, which don't exist in browsers or edge runtimes, so it will load in any JS environment\
\
Detection works by reading properties of the global object, which is `globalThis` in modern engines. In older engines without `globalThis` (Node before 12, IE11, Safari before 12.1), the library falls back to `self`, `window` or `global`. The exception to this fallback chain is Cherry and Clojurific, which both rely on `globalThis` for their own runtimes.

The values of all the public defs in the library are computed once, when `js-runtime-detection.core` loads. If your test setup installs jsdom, load `js-runtime-detection.core` after jsdom is in place.

Properties are read by string key, for example `globalThis["process"]["versions"]["node"]` rather than `globalThis.process.versions.node`. The Closure compiler, which ClojureScript uses for `:advanced` builds, renames dotted property names but never changes string keys, so results are the same in dev builds and `:advanced` builds.

The library looks for various global vars (`Deno`, `Bun`, `process`, etc.). When they don't exist, the lookup returns `nil` instead of throwing an error. Detection only looks at which globals exist and what they contain, such as `globalThis.Deno` or `navigator.userAgent`. Other JS code running in the same environment, such as a dependency, a polyfill or a test setup, can add or change those globals. For example, a polyfill can define a `process` global in a browser, and jsdom adds `window` and `document` to Node.

`js-runtime`, `js-platform` and the other defs can be used to determine control flow in your program, such as how to print output or which APIs to call. Don't treat these values as a guarantee of where your code is running, as they only report what the globals look like. For example, you could have JS code running in a browser that sets `process.versions.node`, which would result in `js-runtime-is-server` and `js-runtime-has-node-apis` returning `true`. For this reason, the values of the public defs in `js-runtime-detection.core` should not be relied on for security decisions.

<br />

## Development

The library has zero dependencies, and no `package.json`.

```sh
bb test            # same as test:nbb
bb test:nbb        # run the tests on Node under nbb, plain and with jsdom globals
bb test:advanced   # compile the tests with :advanced on the latest ClojureScript, run on Node plain and with jsdom globals
bb test:compat     # same, on the oldest supported ClojureScript (1.9.854)
bb test:cloudflare # optional: run a probe in workerd (Cloudflare Workers) via Miniflare, with and without nodejs_compat
bb test:squint     # compile the tests with Squint, run on Node
bb test:cherry     # compile the tests with Cherry, run on Node
bb test:clojurific # compile the tests with Clojurific, run on Node (needs Java 17 or newer)
bb test:all        # all of the above (test:cloudflare skips if Miniflare is not installed)
bb ci              # run the tests and build the jar
bb install         # install the jar to ~/.m2
bb docs:readme     # after tagging a release: update the version numbers in this README
bb docs:git-deps   # after tagging a release: regenerate docs/git-deps.md
```

### How the tests work

- Each test run checks one row of the detection matrix. The `JS_RUNTIME_DETECT_ENV` env var determines the row: `node` (the default) or `jsdom-node`.

- `test:advanced` and `test:compat` compile the tests with `:advanced` optimizations, then run them three times on Node. The first run is no-frills. The second preloads [test/stubs/jsdom.cjs](test/stubs/jsdom.cjs), which sets the `window`, `document` and jsdom user agent globals that global-jsdom and jest-environment-jsdom install. The third preloads [test/stubs/no-globalthis.cjs](test/stubs/no-globalthis.cjs), which deletes `globalThis` to imitate older engines.

- The `:advanced` runs catch property renaming by the Closure compiler, which dev builds and nbb don't show.

- `test:nbb` runs the tests twice under nbb, the second time with the jsdom stub preloaded through `NODE_OPTIONS`.

- `test:squint` and `test:cherry` compile `src` and `test` to ES modules in `target/squint` and `target/cherry`, then install the compiler's npm package into that folder, because the compiled modules import its runtime. Squint runs all three rows. Cherry skips the run without `globalThis`, because Cherry's runtime needs it.

- `test:clojurific` compiles with the `:clojurific` alias in `deps.edn` and Clojurific's generated test runner (`cljf.esm.node-test/run`), then runs it on Node. Like Cherry, it skips the run without `globalThis`.

- Both tasks use the same build script, [script/build_advanced.clj](script/build_advanced.clj). It calls `cljs.build.api` rather than `cljs.main`, because ClojureScript 1.9.854 predates `cljs.main`.

- `deps.edn` has one alias per ClojureScript version:
  - `:cljs-latest` uses ClojureScript 1.12.145.
  - `:cljs-1.9` uses ClojureScript 1.9.854. It forces Clojure 1.8.0, because under Clojure 1.9 that compiler writes `##Inf` into its output, which the Closure compiler can't parse. It also adds the `jaxb-api` jar, which the ClojureScript 1.9.854 compiler needs to run on Java 11 or newer.

- There are two test entrypoints, plus the one Clojurific generates:
  - [runner.cljs](test/js_runtime_detection/runner.cljs) is for nbb, Squint and Cherry. nbb reports the summary through `cljs.test`'s `:end-run-tests` report method, while Squint and Cherry return it from `run-tests`, so the runner reads both.
  - [advanced_runner.cljs](test/js_runtime_detection/advanced_runner.cljs) is for the ClojureScript builds. It calls `(enable-console-print!)`, which ClojureScript 1.9 needs on Node and which nbb doesn't have.

- A failing test sets the process exit code to 1, so `bb test:all` and `bb ci` stop.

#### Optional workerd (Cloudflare Workers) tests

`bb test:cloudflare` runs the library in workerd, the runtime behind Cloudflare Workers. It needs Miniflare, which you can install into a gitignored folder with:

```sh
npm install --no-save --prefix test/cloudflare miniflare@4.20260730.0
```

This creates only `test/cloudflare/node_modules`. Without it, `bb test:cloudflare` prints SKIPPED and `bb test:all` continues.

The task builds [worker_probe.cljs](test/js_runtime_detection/worker_probe.cljs) with `:advanced` and no `:target`. [test/cloudflare/run.mjs](test/cloudflare/run.mjs) appends a `fetch` handler to the compiled file, loads it into workerd through Miniflare, and compares the returned values against the Cloudflare Workers row. It runs once without flags and once with `nodejs_compat`, with the compatibility date pinned to 2026-07-01.

### Updating docs after a release

After tagging a release (for example `git tag v0.2.0`), run both docs tasks:

- `bb docs:readme` updates the version numbers in the Setup section of this README to match the tag.
- `bb docs:git-deps` regenerates [docs/git-deps.md](docs/git-deps.md) from [docs/git-deps.template.md](docs/git-deps.template.md) with the tag and its commit SHA.

Commit both files and push.

<br />

## License

Copyright © 2026 Jeremiah Coyle

Distributed under the Eclipse Public License version 2.0.
