# js-runtime-detection

A ClojureScript library that detects which JavaScript runtime and deployment platform your code is running in:

- Node
- Deno
- Bun
- Browsers
- Web workers and service workers
- Electron
- React Native
- Cloudflare Workers
- Vercel Edge
- Netlify Edge
- Fastly Compute

Detection works by reading properties of `globalThis`. The library has no dependencies and does not import Node.js built-in modules like `fs` or `os`, which don't exist in browsers or edge runtimes. A missing global gives `nil` instead of an error, so the library loads in any JS environment. Every value is computed once, when `js-runtime-detection.core` loads. If your test setup installs jsdom, load `js-runtime-detection.core` after jsdom is in place.

Properties are read by string key, for example `globalThis["process"]["versions"]["node"]` rather than `globalThis.process.versions.node`. The Closure compiler renames dotted property names in `:advanced` builds but never changes string keys, so results are the same in dev builds and `:advanced` builds.

Detection only looks at which globals exist and what they contain, such as `globalThis.Deno` or `navigator.userAgent`. Other JS code running in the same environment, such as a dependency, a polyfill or a test setup, can add or change those globals. For example, a polyfill can define a `process` global in a browser, and jsdom adds `window` and `document` to Node.

Use `js-runtime`, `js-platform` and the other defs to decide how your program behaves, such as how to print output or which APIs to call. Don't treat these values as a guarantee of where your code is running, as they only report what the globals look like. For example, any code that sets `process.versions.node` in a browser makes `js-server-runtime?` and `js-runtime-has-node-apis?` return `true` there. For this reason, these values should not be relied on for security decisions.

<br>

## Requirements

- ClojureScript 1.9.946 or newer. Tested with `:advanced` builds on 1.9.946 and on 1.10.238 through 1.12.145. ClojureScript 1.7 does not work, because it has no `unchecked-get`.
- A JavaScript engine with `globalThis` (Node 12+, browsers from 2019 on). Without it, every value is `nil`, `false` or `:unknown`.

<br>

## Setup

deps.edn:

```clojure
io.github.paintparty/js-runtime-detection {:mvn/version "0.1.0"}
```

Leiningen:

```clojure
[io.github.paintparty/js-runtime-detection "0.1.0"]
```

<br>

## Usage

```clojure
(ns my.app
  (:require [js-runtime-detection.core :as rt]))

rt/js-runtime          ;; => :node
rt/js-server-runtime?  ;; => true
```

The library is ClojureScript only. From a `.cljc` file, require it inside a reader conditional:

```clojure
(ns my.app
  (:require #?(:cljs [js-runtime-detection.core :as rt])))

(def server? #?(:clj true :cljs rt/js-server-runtime?))
```

<br>

## Public defs

| def                          | Value          | Answers                                             |
|:-----------------------------|:---------------|:----------------------------------------------------|
| `js-runtime`                 | keyword        | Which runtime (Node, Deno, Bun, workerd, browser)   |
| `js-platform`                | keyword or nil | Which deployment platform (Cloudflare, Vercel ...)  |
| `js-server-runtime?`         | boolean        | Is this server-side?                                |
| `js-runtime-has-dom?`        | boolean        | Are `window` and `document` present?                |
| `js-runtime-has-jsdom?`      | boolean        | Is jsdom active?                                    |
| `js-runtime-has-node-apis?`  | boolean        | Can I use `fs`, `process.env` and other Node APIs?  |
| `js-runtime-is-strict-node?` | boolean        | Is this actual Node, not Bun or Deno in Node mode?  |

`js-runtime` is one of:

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

`js-platform` is one of:

`:cloudflare`\
`:vercel-edge`\
`:netlify-edge`\
`:fastly`\
`nil`

<br>

## Detection matrix

| Environment        | `js-runtime`         | `js-platform`   | `js-server-runtime?` | `js-runtime-has-dom?` | `js-runtime-has-jsdom?` | `js-runtime-has-node-apis?` | `js-runtime-is-strict-node?` |
|:-------------------|:---------------------|:----------------|:--------------------:|:---------------------:|:-----------------------:|:---------------------------:|:----------------------------:|
| Real browser       | `:browser`           | `nil`           | false                | true                  | false                   | false                       | false                        |
| Node               | `:node`              | `nil`           | true                 | false                 | false                   | true                        | true                         |
| jsdom in Node      | `:node`              | `nil`           | true                 | true                  | true                    | true                        | true                         |
| Deno 2             | `:deno`              | `nil`           | true                 | false                 | false                   | true                        | false                        |
| jsdom in Deno      | `:deno`              | `nil`           | true                 | true                  | true                    | true                        | false                        |
| Bun                | `:bun`               | `nil`           | true                 | false                 | false                   | true                        | false                        |
| jsdom in Bun       | `:bun`               | `nil`           | true                 | true                  | true                    | true                        | false                        |
| Cloudflare Workers | `:workerd`           | `:cloudflare`   | true                 | false                 | false                   | false                       | false                        |
| Vercel Edge*       | `:workerd`           | `:vercel-edge`  | true                 | false                 | false                   | false                       | false                        |
| Netlify Edge       | `:deno`              | `:netlify-edge` | true                 | false                 | false                   | false                       | false                        |
| Fastly Compute*    | `:unknown`           | `:fastly`       | true                 | false                 | false                   | false                       | false                        |
| Electron renderer  | `:electron-renderer` | `nil`           | true                 | true                  | false                   | true                        | false                        |
| Electron main      | `:node`              | `nil`           | true                 | false                 | false                   | true                        | true                         |
| React Native       | `:react-native`      | `nil`           | false                | false                 | false                   | false                       | false                        |
| Service Worker     | `:service-worker`    | `nil`           | false                | false                 | false                   | false                       | false                        |
| Web Worker         | `:web-worker`        | `nil`           | false                | false                 | false                   | false                       | false                        |

Under jsdom, `js-server-runtime?` and `js-runtime-has-node-apis?` are true, because jsdom runs on a server runtime. Pair them with `(not js-runtime-has-dom?)` to exclude test environments that simulate a browser.

\* Rows marked with an asterisk are unverified. Vercel's Edge Runtime is not workerd, so on Vercel Edge `js-runtime` may be `:unknown` rather than `:workerd`. The `:fastly` platform check relies on a `fastly` global, which newer versions of Fastly's JavaScript runtime may no longer define.

The Electron renderer row assumes `nodeIntegration` is enabled. With Electron's defaults (context isolation on, `nodeIntegration` off), the renderer has no `process` global and reports the same values as a real browser.

Cloudflare Workers with the `nodejs_compat` flag (compatibility date 2025-09-15 or later) report `js-runtime-has-node-apis?` as true. The rest of the row is the same.

Automated tests cover Node and jsdom in Node (dev and `:advanced` builds), and Cloudflare Workers with and without `nodejs_compat` (an `:advanced` build run in workerd through Miniflare). The other rows follow from each platform's documented globals and have not been run on those platforms.

<br>

## Development

The library has no dependencies, and the repo has no `package.json`. The Cloudflare test is optional. It needs Miniflare, which you can install into a gitignored folder with:

```sh
npm install --no-save --prefix test/cloudflare miniflare@4.20260730.0
```

This creates only `test/cloudflare/node_modules`. Without it, `bb test:cloudflare` prints SKIPPED and `bb test` continues.

```sh
bb test:node       # run the tests on Node (via nbb)
bb test:advanced   # compile the tests with :advanced on the latest ClojureScript, run on Node plain and with jsdom globals
bb test:compat     # same, on the oldest supported ClojureScript (1.9.946)
bb test:cloudflare # optional: run a probe in workerd (Cloudflare Workers) via Miniflare, with and without nodejs_compat
bb test            # all four of the above (test:cloudflare skips if Miniflare is not installed)
bb ci              # run the tests and build the jar
bb install         # install the jar to ~/.m2
```

### How the tests are set up

- Each test run checks one row of the detection matrix. The `JS_RUNTIME_DETECT_ENV` environment variable picks the row: `node` (the default) or `jsdom-node`.
- `test:advanced` and `test:compat` compile the tests with `:advanced` optimizations, then run them on Node twice. The first run is plain. The second preloads [test/stubs/jsdom.cjs](test/stubs/jsdom.cjs), which sets the `window`, `document` and jsdom user agent globals that global-jsdom and jest-environment-jsdom install.
- The `:advanced` runs catch property renaming by the Closure compiler, which dev builds and nbb don't show.
- Both tasks use the same build script, [script/build_advanced.clj](script/build_advanced.clj). It calls `cljs.build.api` rather than `cljs.main`, because ClojureScript 1.9.946 predates `cljs.main`.
- `deps.edn` has one alias per ClojureScript version:
  - `:cljs-latest` uses ClojureScript 1.12.145.
  - `:cljs-1.9` uses ClojureScript 1.9.946. It forces Clojure 1.9.0 and adds the `jaxb-api` jar, which that compiler needs to run on Java 11 or newer.
- There are two test entrypoints:
  - [runner.cljs](test/js_runtime_detection/runner.cljs) is for nbb.
  - [advanced_runner.cljs](test/js_runtime_detection/advanced_runner.cljs) is for the compiled builds. It calls `(enable-console-print!)`, which ClojureScript 1.9 needs on Node and which nbb doesn't have.
- `test:cloudflare` builds [worker_probe.cljs](test/js_runtime_detection/worker_probe.cljs) with `:advanced` and no `:target`. [test/cloudflare/run.mjs](test/cloudflare/run.mjs) appends a `fetch` handler to the compiled file, loads it into workerd through Miniflare, and compares the returned values against the Cloudflare Workers row. It runs once without flags and once with `nodejs_compat`, with the compatibility date pinned to 2026-07-01.
- A failing test sets the process exit code to 1, so `bb test` and `bb ci` stop.

<br>

## License

Copyright © 2026 Jeremiah Coyle

Distributed under the Eclipse Public License version 2.0.
