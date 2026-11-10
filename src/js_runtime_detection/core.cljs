(ns js-runtime-detection.core
  "JavaScript runtime and platform detection via globalThis sniffing.

   Does not import any Node.js modules, so it is safe to load in any JS
   environment (browser, worker, edge, Node, Deno, Bun). Every value is
   computed once, when the namespace loads.

   Public defs:
     js-runtime                 - which JS runtime (:node, :browser, :deno, :bun, etc.)
     js-platform                - which deployment platform (:cloudflare, :vercel-edge, etc.)
     js-server-runtime?         - am I server-side?
     js-runtime-has-dom?        - do I have DOM APIs?
     js-runtime-has-jsdom?      - is jsdom active?
     js-runtime-has-node-apis?  - can I use fs/process.env/etc.?
     js-runtime-is-strict-node? - am I in actual Node (not Bun/Deno compat)?")


;; PUBLIC API SUMMARY
;; +-----------------------------+----------------+----------------------------------------------------+
;; | def                         |  Return type   | Purpose                                            |
;; +-----------------------------+----------------+----------------------------------------------------+
;; | js-runtime                  | keyword        | Which runtime (Node, Deno, Bun, workerd, browser)  |
;; | js-platform                 | keyword or nil | Which deployment platform (Cloudflare, Vercel ...) |
;; | js-server-runtime?          | boolean        | Am I server-side? (convenience composite)          |
;; | js-runtime-has-dom?         | boolean        | Do I have DOM APIs?                                |
;; | js-runtime-has-jsdom?       | boolean        | Is jsdom active?                                   |
;; | js-runtime-has-node-apis?   | boolean        | Can I use fs/process.env/etc.?                     |
;; | js-runtime-is-strict-node?  | boolean        | Am I in actual Node (not Bun/Deno compat)?         |
;; +-----------------------------+----------------+----------------------------------------------------+


;; DETECTION MATRIX for public defs:
;; ╔════════════════════╦════════════════════╦═════════════════╗
;; ║ Environment        ║ js-runtime         ║ js-platform     ║
;; ╚════════════════════╩════════════════════╩═════════════════╝
;; │ Real browser       │ :browser           │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Node               │ :node              │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ jsdom in Node      │ :node              │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Deno 2             │ :deno              │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ jsdom in Deno      │ :deno              │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Bun                │ :bun               │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ jsdom in Bun       │ :bun               │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Cloudflare Workers │ :workerd           │ :cloudflare     │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Vercel Edge*       │ :workerd           │ :vercel-edge    │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Netlify Edge       │ :deno              │ :netlify-edge   │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Fastly Compute*    │ :unknown           │ :fastly         │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Electron renderer  │ :electron-renderer │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Electron main      │ :node              │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ React Native       │ :react-native      │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Service Worker     │ :service-worker    │ nil             │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Web Worker         │ :web-worker        │ nil             │
;; └────────────────────┴────────────────────┴─────────────────┘


;; DETECTION MATRIX for public defs (Expanded):
;; ╔════════════════════╦════════════════════╦═════════════════════╦═══════════════════════╦═══════════════════════════╦══════════════════════════════╗
;; ║ Environment        ║ js-server-runtime? ║ js-runtime-has-dom? ║ js-runtime-has-jsdom? ║ js-runtime-has-node-apis? ║ js-runtime-is-strict-node?   ║
;; ╚════════════════════╩════════════════════╩═════════════════════╩═══════════════════════╩═══════════════════════════╩══════════════════════════════╝
;; │ Real browser       │ false              │ true                │ false                 │ false                     │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Node               │ true               │ false               │ false                 │ true                      │ true                         │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ jsdom in Node      │ true               │ true                │ true                  │ true                      │ true                         │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Deno 2             │ true               │ false               │ false                 │ true                      │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ jsdom in Deno      │ true               │ true                │ true                  │ true                      │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Bun                │ true               │ false               │ false                 │ true                      │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ jsdom in Bun       │ true               │ true                │ true                  │ true                      │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Cloudflare Workers │ true               │ false               │ false                 │ false                     │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Vercel Edge*       │ true               │ false               │ false                 │ false                     │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Netlify Edge       │ true               │ false               │ false                 │ false                     │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Fastly Compute*    │ true               │ false               │ false                 │ false                     │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Electron renderer  │ true               │ true                │ false                 │ true                      │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Electron main      │ true               │ false               │ false                 │ true                      │ true                         │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ React Native       │ false              │ false               │ false                 │ false                     │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Service Worker     │ false              │ false               │ false                 │ false                     │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Web Worker         │ false              │ false               │ false                 │ false                     │ false                        │
;; └────────────────────┴────────────────────┴─────────────────────┴───────────────────────┴───────────────────────────┴──────────────────────────────┘


;; NOTES on the matrices:
;; - Electron renderer rows assume `nodeIntegration` is enabled. With
;;   Electron's defaults (contextIsolation on, nodeIntegration off) the
;;   renderer has no `process` global and reports the same values as a real
;;   browser.
;; - * Vercel Edge and Fastly Compute rows are unverified. Vercel's Edge
;;   Runtime is not workerd, so js-runtime may be :unknown there rather than
;;   :workerd. The :fastly platform check relies on a `fastly` global, which
;;   newer versions of Fastly's JS runtime may no longer define.
;; - Cloudflare Workers with the nodejs_compat flag (compatibility date
;;   2025-09-15 or later) report js-runtime-has-node-apis? as true. The rest of
;;   the row is the same.
;; - Automated tests cover Node and jsdom in Node (dev and :advanced builds),
;;   and Cloudflare Workers with and without nodejs_compat (:advanced build in
;;   workerd, via Miniflare). The other rows follow from each platform's
;;   documented globals and have not been run on those platforms.


(def ^:private global-ref
  (when (exists? js/globalThis) js/globalThis))

(defn- gget
  "Reads a property path from globalThis by string keys, e.g.
   (gget \"process\" \"versions\" \"node\"). Returns nil when any step
   is missing.

   Reading through globalThis means an undeclared global (Deno in Node,
   process in a browser) returns nil instead of throwing a
   ReferenceError. String keys are not renamed by :advanced compilation,
   and unchecked-get does not trigger :checked-arrays warnings the way
   aget does."
  [& ks]
  (reduce (fn [o k] (if (some? o) (unchecked-get o k) (reduced nil)))
          global-ref
          ks))

;; -------------------------------------------------------------------
;; Cached global lookups (one-time at namespace load)
;; -------------------------------------------------------------------

(def ^:private ua       (gget "navigator" "userAgent"))
(def ^:private win      (gget "window"))
(def ^:private deno?    (boolean (gget "Deno" "version" "deno")))
(def ^:private bun?     (boolean (gget "Bun")))
(def ^:private workerd? (= ua "Cloudflare-Workers"))

;; -------------------------------------------------------------------
;; Primitive capability flags
;; -------------------------------------------------------------------

(def js-runtime-has-dom?
  "True anywhere `window` and `document` globals exist - real browsers,
   jsdom, and the Electron renderer process. Use when your code needs DOM
   APIs regardless of whether they're real or simulated (e.g., rendering
   components, attaching event listeners, measuring element dimensions)."
  (boolean (and win (gget "document"))))

(def js-runtime-has-jsdom?
  "True whenever jsdom is active, regardless of underlying runtime
   (Node, Deno, or Bun). Use to detect test environments that simulate
   a browser on top of a server runtime."
  (boolean (some-> ua (.includes "jsdom"))))

(def js-runtime-has-node-apis?
  "Resolves to `true` when Node's `process.versions.node` is present.
   Includes Node itself, Deno 2+ (Node compat mode), Bun, Electron
   (both processes), jsdom (which runs in Node), and Cloudflare Workers
   with the nodejs_compat flag (compatibility date 2025-09-15 or later).
   Use when you need to check for access to Node APIs like `fs`, `path`,
   `process.env`, or native modules. Note: this is true under jsdom, so
   pair with `(not js-runtime-has-dom?)` if you want to exclude test
   envs. For a strict Node-only check, use `(= js-runtime :node)`."
  (boolean (gget "process" "versions" "node")))

(def js-runtime-is-strict-node?
  "True only in actual Node.js. Excludes Bun, Deno and Cloudflare Workers
   even when they run in Node compatibility mode. Checks that
   process.release.name is \"node\", which Deno and Cloudflare's
   nodejs_compat also report, so those runtimes are excluded explicitly."
  (and (= (gget "process" "release" "name") "node")
       (not deno?)
       (not bun?)
       (not workerd?)))


(def js-runtime
  "The JavaScript runtime, independent of deployment platform. One of:

   :node
   :deno
   :bun
   :workerd
   :browser
   :service-worker
   :web-worker
   :electron-renderer
   :react-native
   :unknown

   The :node branch uses `js-runtime-is-strict-node?`, so Deno and Bun
   in Node compatibility mode are not reported as :node. jsdom reports
   the runtime it runs on, not :browser.

   | Environment        | Value                |
   |:-------------------|:---------------------|
   | Real browser       | `:browser`           |
   | Node               | `:node`              |
   | Deno 2             | `:deno`              |
   | Bun                | `:bun`               |
   | Cloudflare Workers | `:workerd`           |
   | Vercel Edge*       | `:workerd`           |
   | Netlify Edge       | `:deno`              |
   | jsdom in Node      | `:node`              |
   | Electron renderer  | `:electron-renderer` |
   | Electron main      | `:node`              |
   | React Native       | `:react-native`      |
   | Service Worker     | `:service-worker`    |
   | Web Worker         | `:web-worker`        |

   * Unverified. On Vercel Edge this may be :unknown.
  "
  (or
   ;; Deno - must be checked b4 Node (Deno 2 shims process.versions.node)
   (when deno? :deno)

   ;; Bun - must be checked b4 Node (Bun shims process.versions.node)
   (when bun? :bun)

   ;; workerd (Cloudflare Workers runtime)
   (when workerd? :workerd)

   ;; Service Worker
   (when (= (gget "self" "constructor" "name")
            "ServiceWorkerGlobalScope")
     :service-worker)

   ;; Dedicated or Shared Web Worker
   (when (and (gget "importScripts") (not win))
     :web-worker)

   ;; Electron renderer (has both window and process)
   (when (and win (gget "process" "versions" "electron"))
     :electron-renderer)

   ;; Browser - jsdom also defines window and document, so it is skipped
   ;; here and falls through to the runtime it runs on
   (when (and js-runtime-has-dom? (not js-runtime-has-jsdom?))
     :browser)

   ;; React Native
   (when (or (gget "HermesInternal")
             (= (gget "navigator" "product") "ReactNative"))
     :react-native)

   ;; Strict Node - uses process.release.name, which Deno/Bun don't fake
   (when js-runtime-is-strict-node? :node)

   :unknown))


(def js-platform
  "The deployment platform, independent of underlying runtime. One of:
   :netlify-edge  - Netlify Edge Functions (runs on Deno)
   :vercel-edge   - Vercel Edge Functions
   :cloudflare    - Cloudflare Workers (runs on workerd)
   :fastly        - Fastly Compute
   nil            - no specific platform detected

   Platforms are distinct from runtimes because they layer
   platform-specific APIs (KV stores, env vars, request contexts) on
   top of the same runtime. A platform can share a runtime with other
   environments: Netlify Edge runs on Deno.

   | Environment        | Value           |
   |:-------------------|:----------------|
   | Cloudflare Workers | `:cloudflare`   |
   | Vercel Edge*       | `:vercel-edge`  |
   | Netlify Edge       | `:netlify-edge` |
   | Fastly Compute*    | `:fastly`       |
   | Everything else    | `nil`           |

   * Unverified.
  "
  (cond
    (gget "Netlify")            :netlify-edge
    (gget "EdgeRuntime")        :vercel-edge
    workerd?                    :cloudflare
    (gget "fastly")             :fastly
    :else                       nil))


(def js-server-runtime?
  "Resolves to `true` in server-side JS runtimes:
   Node, Deno, Bun, and all edge platforms (Cloudflare, Vercel,
   Netlify, Fastly), plus Electron main.

   Resolves to `false` in real browsers, web workers, and React Native.

   Returns `true` under jsdom (jsdom has Node APIs available even
   though it simulates a browser). Pair with `(not js-runtime-has-dom?)`
   if you need to exclude environments where a DOM is present
   (real or simulated).

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

   * Unverified.
   "
  (if (and js-runtime-has-dom? (not (gget "process")))
    false  ; fast path: real browser
    (or (some? js-platform)
        js-runtime-has-node-apis?)))
