# js-runtime-detection

Tells ClojureScript code which JavaScript runtime and deployment platform it is running in: Node, Deno, Bun, a browser, a web or service worker, Electron, React Native, Cloudflare Workers, Vercel Edge, Netlify Edge or Fastly Compute.

Detection works by reading properties of `globalThis`. The namespace imports no Node modules, so it loads in any JS environment. Every value is computed once, when the namespace loads. If your test setup installs jsdom, load this namespace after jsdom is in place.

Properties are read by string key, so results are the same in dev builds and `:advanced` builds.

Any script can define these globals, so don't use these values for security decisions.

The namespace is `.cljc`, so you can require it from shared code, but the defs only exist on the ClojureScript side.

## Install

deps.edn:

```clojure
io.github.paintparty/js-runtime-detection {:mvn/version "0.1.0"}
```

Leiningen:

```clojure
[io.github.paintparty/js-runtime-detection "0.1.0"]
```

## Requirements

- ClojureScript 1.9.946 or newer. Tested with `:advanced` builds on 1.9.946 and on 1.10.238 through 1.12.145. ClojureScript 1.7 does not work, because it has no `unchecked-get`.
- A JavaScript engine with `globalThis` (Node 12+, browsers from 2019 on). Without it, every value is `nil`, `false` or `:unknown`.

## Usage

```clojure
(ns my.app
  (:require [js-runtime-detection.core :as rt]))

rt/js-runtime          ;; => :node
rt/js-server-runtime?  ;; => true
```

From a `.cljc` file, require it for the ClojureScript side only:

```clojure
(ns my.app
  (:require #?(:cljs [js-runtime-detection.core :as rt])))

(def server? #?(:clj true :cljs rt/js-server-runtime?))
```

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

`js-runtime` is one of `:node`, `:deno`, `:bun`, `:workerd`, `:browser`, `:service-worker`, `:web-worker`, `:electron-renderer`, `:react-native` or `:unknown`.

`js-platform` is one of `:cloudflare`, `:vercel-edge`, `:netlify-edge`, `:fastly` or `nil`.

## Detection matrix

| Environment        | `js-runtime`         | `js-platform`   |
|:-------------------|:---------------------|:----------------|
| Real browser       | `:browser`           | `nil`           |
| Node               | `:node`              | `nil`           |
| jsdom in Node      | `:node`              | `nil`           |
| Deno 2             | `:deno`              | `nil`           |
| jsdom in Deno      | `:deno`              | `nil`           |
| Bun                | `:bun`               | `nil`           |
| jsdom in Bun       | `:bun`               | `nil`           |
| Cloudflare Workers | `:workerd`           | `:cloudflare`   |
| Vercel Edge*       | `:workerd`           | `:vercel-edge`  |
| Netlify Edge       | `:deno`              | `:netlify-edge` |
| Fastly Compute*    | `:unknown`           | `:fastly`       |
| Electron renderer  | `:electron-renderer` | `nil`           |
| Electron main      | `:node`              | `nil`           |
| React Native       | `:react-native`      | `nil`           |
| Service Worker     | `:service-worker`    | `nil`           |
| Web Worker         | `:web-worker`        | `nil`           |

| Environment        | `js-server-runtime?` | `has-dom?` | `has-jsdom?` | `has-node-apis?` | `is-strict-node?` |
|:-------------------|:--------------------:|:----------:|:------------:|:----------------:|:-----------------:|
| Real browser       | false                | true       | false        | false            | false             |
| Node               | true                 | false      | false        | true             | true              |
| jsdom in Node      | true                 | true       | true         | true             | true              |
| Deno 2             | true                 | false      | false        | true             | false             |
| jsdom in Deno      | true                 | true       | true         | true             | false             |
| Bun                | true                 | false      | false        | true             | false             |
| jsdom in Bun       | true                 | true       | true         | true             | false             |
| Cloudflare Workers | true                 | false      | false        | false            | false             |
| Vercel Edge*       | true                 | false      | false        | false            | false             |
| Netlify Edge       | true                 | false      | false        | false            | false             |
| Fastly Compute*    | true                 | false      | false        | false            | false             |
| Electron renderer  | true                 | true       | false        | true             | false             |
| Electron main      | true                 | false      | false        | true             | true              |
| React Native       | false                | false      | false        | false            | false             |
| Service Worker     | false                | false      | false        | false            | false             |
| Web Worker         | false                | false      | false        | false            | false             |

The `has-*` and `is-*` columns are short for `js-runtime-has-dom?`, `js-runtime-has-jsdom?`, `js-runtime-has-node-apis?` and `js-runtime-is-strict-node?`.

Under jsdom, `js-server-runtime?` and `js-runtime-has-node-apis?` are true, because jsdom runs on a server runtime. Pair them with `(not js-runtime-has-dom?)` to exclude test environments that simulate a browser.

\* Rows marked with an asterisk are unverified. Vercel's Edge Runtime is not workerd, so on Vercel Edge `js-runtime` may be `:unknown` rather than `:workerd`. The `:fastly` platform check relies on a `fastly` global, which newer versions of Fastly's JavaScript runtime may no longer define.

The Electron renderer rows assume `nodeIntegration` is enabled. With Electron's defaults (context isolation on, `nodeIntegration` off), the renderer has no `process` global and reports the same values as a real browser.

Cloudflare Workers with the `nodejs_compat` flag (compatibility date 2025-09-15 or later) report `js-runtime-has-node-apis?` as true. The rest of the row is the same.

Automated tests cover Node and jsdom in Node (dev and `:advanced` builds), and Cloudflare Workers with and without `nodejs_compat` (an `:advanced` build run in workerd through Miniflare). The other rows follow from each platform's documented globals and have not been run on those platforms.

## Development

Run `npm install` once to get Miniflare, which `test:cloudflare` needs.

```sh
bb test:node       # run the tests on Node (via nbb)
bb test:advanced   # compile the tests with :advanced on the latest ClojureScript, run on Node plain and with jsdom globals
bb test:compat     # same, on the oldest supported ClojureScript (1.9.946)
bb test:cloudflare # run a probe in workerd (Cloudflare Workers) via Miniflare, with and without nodejs_compat
bb test            # all four of the above
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

## License

Copyright © 2026 Jeremiah Coyle

Distributed under the Eclipse Public License version 2.0.
