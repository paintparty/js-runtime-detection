(ns js-runtime-detection.core
  "JavaScript runtime and platform detection via `globalThis` sniffing.

   Does not import any Node.js modules, so it is safe to load in any JS
   environment (browser, worker, edge, Node, Deno, Bun). Every value is computed
   once, when the namespace loads.

   Works with ClojureScript, nbb, Cherry, Clojurific and Squint. `js-runtime`
   and `js-platform` are strings, so they are the same value in every dialect.

   Public defs:
     `js-runtime`                - JS runtime (`\"node\"`, `\"deno\"`, ...)
     `js-platform`               - Deployment platform (`\"cloudflare\"`, ...)
     `js-runtime-is-server`      - Am I server-side?
     `js-runtime-has-dom`        - Do I have DOM APIs?
     `js-runtime-has-jsdom`      - Is jsdom active?
     `js-runtime-has-node-apis`  - Can I use `fs`, `process.env`, etc.?
     `js-runtime-is-strict-node` - Am I in actual Node (not Bun/Deno compat)?")


;; PUBLIC API SUMMARY
;; +---------------------------+----------------+----------------------------------------------------+
;; | def                       | Return type    | Purpose                                            |
;; +---------------------------+----------------+----------------------------------------------------+
;; | js-runtime                | string         | Which runtime (Node, Deno, Bun, workerd, browser)  |
;; | js-platform               | string or nil  | Which deployment platform (Cloudflare, Vercel ...) |
;; | js-runtime-is-server      | boolean        | Am I server-side? (convenience composite)          |
;; | js-runtime-has-dom        | boolean        | Do I have DOM APIs?                                |
;; | js-runtime-has-jsdom      | boolean        | Is jsdom active?                                   |
;; | js-runtime-has-node-apis  | boolean        | Can I use fs/process.env/etc.?                     |
;; | js-runtime-is-strict-node | boolean        | Am I in actual Node (not Bun/Deno compat)?         |
;; +---------------------------+----------------+----------------------------------------------------+


;; DETECTION MATRIX for public defs:
;; ╔════════════════════╦═════════════════════╦════════════════╦══════════════════════╦════════════════════╦══════════════════════╦══════════════════════════╦═══════════════════════════╗
;; ║ Environment        ║ js-runtime          ║ js-platform    ║ js-runtime-is-server ║ js-runtime-has-dom ║ js-runtime-has-jsdom ║ js-runtime-has-node-apis ║ js-runtime-is-strict-node ║
;; ╚════════════════════╩═════════════════════╩════════════════╩══════════════════════╩════════════════════╩══════════════════════╩══════════════════════════╩═══════════════════════════╝
;; │ Real browser       │ "browser"           │ nil            │ false                │ true               │ false                │ false                    │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Node               │ "node"              │ nil            │ true                 │ false              │ false                │ true                     │ true                      │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ jsdom in Node      │ "node"              │ nil            │ true                 │ true               │ true                 │ true                     │ true                      │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Deno 2             │ "deno"              │ nil            │ true                 │ false              │ false                │ true                     │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ jsdom in Deno      │ "deno"              │ nil            │ true                 │ true               │ true                 │ true                     │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Bun                │ "bun"               │ nil            │ true                 │ false              │ false                │ true                     │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ jsdom in Bun       │ "bun"               │ nil            │ true                 │ true               │ true                 │ true                     │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Cloudflare Workers │ "workerd"           │ "cloudflare"   │ true                 │ false              │ false                │ false                    │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Vercel Edge*       │ "edge-light"        │ "vercel-edge"  │ true                 │ false              │ false                │ false                    │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Netlify Edge       │ "deno"              │ "netlify-edge" │ true                 │ false              │ false                │ false                    │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Fastly Compute*    │ "fastly"            │ "fastly"       │ true                 │ false              │ false                │ false                    │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Electron renderer  │ "electron-renderer" │ nil            │ true                 │ true               │ false                │ true                     │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Electron main      │ "node"              │ nil            │ true                 │ false              │ false                │ true                     │ true                      │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ React Native       │ "react-native"      │ nil            │ false                │ false              │ false                │ false                    │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Service Worker     │ "service-worker"    │ nil            │ false                │ false              │ false                │ false                    │ false                     │
;; ├────────────────────┼─────────────────────┼────────────────┼──────────────────────┼────────────────────┼──────────────────────┼──────────────────────────┼───────────────────────────┤
;; │ Web Worker         │ "web-worker"        │ nil            │ false                │ false              │ false                │ false                    │ false                     │
;; └────────────────────┴─────────────────────┴────────────────┴──────────────────────┴────────────────────┴──────────────────────┴──────────────────────────┴───────────────────────────┘


;; NOTES on the matrix:
;; - The Electron renderer row assumes `nodeIntegration` is enabled. With
;;   Electron's defaults (contextIsolation on, nodeIntegration off) the
;;   renderer has no `process` global and reports the same values as a real
;;   browser.
;; - * Vercel Edge and Fastly Compute rows are unverified. Vercel Edge is
;;   detected by the `EdgeRuntime` global, which Vercel documents. Fastly
;;   Compute is detected by a `fastly` global, which newer versions of
;;   Fastly's JS runtime may no longer define. Runtime names follow the
;;   WinterTC runtime keys: "edge-light" is Vercel's Edge Runtime.
;; - Cloudflare Workers with the nodejs_compat flag (compatibility date
;;   2025-09-15 or later) report js-runtime-has-node-apis as true. The rest of
;;   the row is the same.
;; - Automated tests cover Node and jsdom in Node (dev and :advanced builds),
;;   Node without `globalThis`, and Cloudflare Workers with and without
;;   nodejs_compat (:advanced build in workerd, via Miniflare). The other rows follow from each platform's
;;   documented globals and have not been run on those platforms.


;; The global object. Engines without `globalThis` (Node before 12, IE11, Safari
;; before 12.1) still have one of the older names for it.
(def ^:private global-ref
  (cond (exists? js/globalThis) js/globalThis
        (exists? js/self)       js/self
        (exists? js/window)     js/window
        (exists? js/global)     js/global))

(defn- gget
  "Reads a property path from `globalThis` by string keys, e.g.
   `(gget \"process\" \"versions\" \"node\")`. Returns `nil` when any step is
   missing.

   Reading through `globalThis` means an undeclared global (`Deno` in Node,
   `process` in a browser) returns `nil` instead of throwing a `ReferenceError`.
   String keys are not renamed by `:advanced` compilation, and `unchecked-get`
   does not trigger `:checked-arrays` warnings the way `aget` does."
  [& ks]
  (reduce (fn [o k] (if (some? o) (unchecked-get o k) (reduced nil)))
          global-ref
          ks))

;; -------------------------------------------------------------------
;; Cached global lookups (one-time at namespace load)
;; -------------------------------------------------------------------

(def ^:private ua                    (gget "navigator" "userAgent"))
(def ^:private win                   (gget "window"))
(def ^:private runtime-is-deno       (boolean (gget "Deno" "version" "deno")))
(def ^:private runtime-is-bun        (boolean (gget "Bun")))
(def ^:private runtime-is-workerd    (= ua "Cloudflare-Workers"))
(def ^:private runtime-is-edge-light (boolean (gget "EdgeRuntime")))
(def ^:private runtime-is-fastly     (boolean (gget "fastly")))

;; -------------------------------------------------------------------
;; Primitive capability flags
;; -------------------------------------------------------------------

(def js-runtime-has-dom
  "True anywhere `window` and `document` globals exist: real browsers, jsdom,
   and the Electron renderer process. Use when your code needs DOM APIs
   regardless of whether they're real or simulated (e.g., rendering components,
   attaching event listeners, measuring element dimensions)."
  (boolean (and win (gget "document"))))

(def js-runtime-has-jsdom
  "True whenever jsdom is active, regardless of underlying runtime (Node, Deno,
   or Bun). Use to detect test environments that simulate a browser on top of a
   server runtime."
  (boolean (some-> ua (.includes "jsdom"))))

(def js-runtime-has-node-apis
  "Resolves to `true` when Node's `process.versions.node` is present. Includes
   Node itself, Deno 2+ (Node compat mode), Bun, Electron (both processes),
   jsdom (which runs in Node), and Cloudflare Workers with the `nodejs_compat`
   flag (compatibility date 2025-09-15 or later). Use when you need to check for
   access to Node APIs like `fs`, `path`, `process.env`, or native modules.
   Note: this is `true` under jsdom, so pair with `(not js-runtime-has-dom)` if
   you want to exclude test envs. For a strict Node-only check, use
   `(= js-runtime \"node\")`."
  (boolean (gget "process" "versions" "node")))

(def js-runtime-is-strict-node
  "True only in actual Node.js. Excludes Bun, Deno and Cloudflare Workers even
   when they run in Node compatibility mode. Checks that `process.release.name`
   is `\"node\"`, which Deno and Cloudflare's `nodejs_compat` also report, so
   those runtimes are excluded explicitly."
  (and (= (gget "process" "release" "name") "node")
       (not runtime-is-deno)
       (not runtime-is-bun)
       (not runtime-is-workerd)))


(def js-runtime
  "The JavaScript runtime, independent of deployment platform. One of:

   `\"node\"`
   `\"deno\"`
   `\"bun\"`
   `\"workerd\"`
   `\"edge-light\"`
   `\"fastly\"`
   `\"browser\"`
   `\"service-worker\"`
   `\"web-worker\"`
   `\"electron-renderer\"`
   `\"react-native\"`
   `\"unknown\"`

   The `\"node\"` branch uses `js-runtime-is-strict-node`, so Deno, Bun and
   Cloudflare Workers in Node compatibility mode are not reported as `\"node\"`.
   jsdom reports the runtime it runs on, not `\"browser\"`.

   | Environment        | Value                   |
   |:-------------------|:------------------------|
   | Real browser       | `\"browser\"`           |
   | Node               | `\"node\"`              |
   | Deno 2             | `\"deno\"`              |
   | Bun                | `\"bun\"`               |
   | Cloudflare Workers | `\"workerd\"`           |
   | Vercel Edge*       | `\"edge-light\"`        |
   | Fastly Compute*    | `\"fastly\"`            |
   | Netlify Edge       | `\"deno\"`              |
   | jsdom in Node      | `\"node\"`              |
   | Electron renderer  | `\"electron-renderer\"` |
   | Electron main      | `\"node\"`              |
   | React Native       | `\"react-native\"`      |
   | Service Worker     | `\"service-worker\"`    |
   | Web Worker         | `\"web-worker\"`        |

   * Unverified."
  (or
   ;; Deno - must be checked b4 Node (Deno 2 shims process.versions.node)
   (when runtime-is-deno "deno")

   ;; Bun - must be checked b4 Node (Bun shims process.versions.node)
   (when runtime-is-bun "bun")

   ;; workerd (Cloudflare Workers runtime)
   (when runtime-is-workerd "workerd")

   ;; Vercel Edge Runtime
   (when runtime-is-edge-light "edge-light")

   ;; Fastly Compute
   (when runtime-is-fastly "fastly")

   ;; Service Worker
   (when (= (gget "self" "constructor" "name")
            "ServiceWorkerGlobalScope")
     "service-worker")

   ;; Dedicated or Shared Web Worker
   (when (and (gget "importScripts") (not win))
     "web-worker")

   ;; Electron renderer (has both window and process)
   (when (and win (gget "process" "versions" "electron"))
     "electron-renderer")

   ;; Browser - jsdom also defines window and document, so it is skipped
   ;; here and falls through to the runtime it runs on
   (when (and js-runtime-has-dom (not js-runtime-has-jsdom))
     "browser")

   ;; React Native
   (when (or (gget "HermesInternal")
             (= (gget "navigator" "product") "ReactNative"))
     "react-native")

   ;; Strict Node - uses process.release.name, which Deno/Bun don't fake
   (when js-runtime-is-strict-node "node")

   "unknown"))


(def js-platform
  "The deployment platform, independent of underlying runtime. One of:

   `\"netlify-edge\"` - Netlify Edge Functions (runs on Deno)
   `\"vercel-edge\"`  - Vercel Edge Functions
   `\"cloudflare\"`   - Cloudflare Workers (runs on workerd)
   `\"fastly\"`       - Fastly Compute
   `nil`              - no specific platform detected

   Platforms are distinct from runtimes because they layer platform-specific
   APIs (KV stores, env vars, request contexts) on top of the same runtime. A
   platform can share a runtime with other environments: Netlify Edge runs on
   Deno.

   | Environment        | Value              |
   |:-------------------|:-------------------|
   | Cloudflare Workers | `\"cloudflare\"`   |
   | Vercel Edge*       | `\"vercel-edge\"`  |
   | Netlify Edge       | `\"netlify-edge\"` |
   | Fastly Compute*    | `\"fastly\"`       |
   | Everything else    | `nil`              |

   * Unverified."
  (cond
    (gget "Netlify")            "netlify-edge"
    runtime-is-edge-light       "vercel-edge"
    runtime-is-workerd          "cloudflare"
    runtime-is-fastly           "fastly"
    :else                       nil))


(def js-runtime-is-server
  "Resolves to `true` in server-side JS runtimes: Node, Deno, Bun, and all edge
   platforms (Cloudflare, Vercel, Netlify, Fastly), plus Electron main.

   Resolves to `false` in real browsers, web workers, and React Native.

   Returns `true` under jsdom (jsdom has Node APIs available even though it
   simulates a browser). Pair with `(not js-runtime-has-dom)` if you need to
   exclude environments where a DOM is present (real or simulated).

   | Environment        |  Value  |
   |:-------------------|:-------:|
   | Real browser       | `false` |
   | Node               | `true`  |
   | Deno 2             | `true`  |
   | Bun                | `true`  |
   | Cloudflare Workers | `true`  |
   | Vercel Edge*       | `true`  |
   | Netlify Edge       | `true`  |
   | Fastly Compute*    | `true`  |
   | jsdom in Node      | `true`  |
   | Electron renderer  | `true`  |
   | Electron main      | `true`  |
   | React Native       | `false` |
   | Web Worker         | `false` |
   | Service Worker     | `false` |

   * Unverified."
  (if (and js-runtime-has-dom (not (gget "process")))
    false  ; fast path: real browser
    (or (some? js-platform)
        js-runtime-has-node-apis)))
