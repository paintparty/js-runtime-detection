(ns js-runtime-detection.core
  "JavaScript runtime and platform detection via globalThis sniffing.

   Does not use any Node.js module imports — safe to load in any JS environment
   (browser, worker, edge, Node, Deno, Bun).

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
;; │ Vercel Edge        │ :workerd           │ :vercel-edge    │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Netlify Edge       │ :deno              │ :netlify-edge   │
;; ├────────────────────┼────────────────────┼─────────────────┤
;; │ Fastly Compute     │ :unknown           │ :fastly         │
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
;; │ Vercel Edge        │ true               │ false               │ false                 │ false                     │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Netlify Edge       │ true               │ false               │ false                 │ false                     │ false                        │
;; ├────────────────────┼────────────────────┼─────────────────────┼───────────────────────┼───────────────────────────┼──────────────────────────────┤
;; │ Fastly Compute     │ true               │ false               │ false                 │ false                     │ false                        │
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


#?(:cljs
   (do
     (def ^:private global-ref
       (when (exists? js/globalThis) js/globalThis))

     (defn- gget
       "Safely reads a property from globalThis."
       [prop]
       (when global-ref
         (aget global-ref prop)))

     ;; -------------------------------------------------------------------
     ;; Cached global lookups (one-time at namespace load)
     ;; -------------------------------------------------------------------

     (def ^:private nav      (gget "navigator"))
     (def ^:private ua       (some-> nav .-userAgent))
     (def ^:private proc     (gget "process"))
     (def ^:private self-ref (gget "self"))
     (def ^:private win      (gget "window"))

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
        (both processes), and jsdom (which runs in Node). Use when you need to
        gate on access to Node APIs like `fs`, `path`, `process.env`, or native
        modules. Note: this is true under jsdom, so pair with
         `(not js-runtime-has-dom?)` if you want to exclude test envs. For a
        strict Node-only check, use `(= js-runtime :node)`."
       (boolean (some-> proc .-versions .-node)))

     (def js-runtime-is-strict-node?
       "True only in actual Node.js. Excludes Bun and Deno even when they're
        running in Node compatibility mode. Uses process.release.name, which
        Deno and Bun don't fake."
       (= (some-> proc .-release .-name) "node"))


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

        Uses `process.release.name` for the :node branch, which Deno and Bun
        don't fake, ensuring strict Node detection.

        | Environment        | Value                |
        |:-------------------|:---------------------|
        | Real browser       | `:browser`           |
        | Node               | `:node`              |
        | Deno 2             | `:deno`              |
        | Bun                | `:bun`               |
        | Cloudflare Workers | `:workerd`           |
        | Vercel Edge        | `:workerd`           |
        | Netlify Edge       | `:deno`              |
        | jsdom in Node      | `:node`              |
        | Electron renderer  | `:electron-renderer` |
        | Electron main      | `:node`              |
        | React Native       | `:react-native`      |
        | Service Worker     | `:service-worker`    |
        | Web Worker         | `:web-worker`        |
       "
       (or
        ;; Deno - must be checked b4 Node (Deno 2 shims process.versions.node)
        (when-let [deno (gget "Deno")]
          (when (some-> deno .-version .-deno) :deno))

        ;; Bun - must be checked b4 Node (Bun shims process.versions.node)
        (when (gget "Bun") :bun)

        ;; workerd (Cloudflare Workers runtime - also used by Vercel Edge)
        (when (= ua "Cloudflare-Workers") :workerd)

        ;; Service Worker
        (when (= (some-> self-ref .-constructor .-name)
                 "ServiceWorkerGlobalScope")
          :service-worker)

        ;; Dedicated or Shared Web Worker
        (when (and (gget "importScripts") (not win))
          :web-worker)

        ;; Electron renderer (has both window and process)
        (when (and win (some-> proc .-versions .-electron))
          :electron-renderer)

        ;; Browser
        (when (and win (gget "document"))
          :browser)

        ;; React Native
        (when (or (gget "HermesInternal")
                  (some-> nav .-product (= "ReactNative")))
          :react-native)

        ;; Strict Node - uses process.release.name, which Deno/Bun don't fake
        (when js-runtime-is-strict-node? :node)

        :unknown))


     (def js-platform
       "The deployment platform, independent of underlying runtime. One of:
        :netlify-edge  - Netlify Edge Functions (runs on Deno)
        :vercel-edge   - Vercel Edge Functions (runs on workerd)
        :cloudflare    - Cloudflare Workers (runs on workerd)
        :fastly        - Fastly Compute@Edge
        nil            - no specific platform detected

        Platforms are distinct from runtimes because they layer
        platform-specific APIs (KV stores, env vars, request contexts) on
        top of the same runtime. Multiple platforms can share a runtime:
        both Vercel Edge and Cloudflare Workers run on workerd.

        | Environment        | Value           |
        |:-------------------|:----------------|
        | Cloudflare Workers | `:cloudflare`   |
        | Vercel Edge        | `:vercel-edge`  |
        | Netlify Edge       | `:netlify-edge` |
        | Fastly Compute     | `:fastly`       |
        | Everything else    | `nil`           |
       "
       (cond
         (gget "Netlify")            :netlify-edge
         (gget "EdgeRuntime")        :vercel-edge
         (= ua "Cloudflare-Workers") :cloudflare
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
        | Vercel Edge        | `true`  |
        | Netlify Edge       | `true`  |
        | Fastly Compute     | `true`  |
        | jsdom in Node      | `true`  |
        | Electron renderer  | `true`  |
        | Electron main      | `true`  |
        | React Native       | `false` |
        | Web Worker         | `false` |
        | Service Worker     | `false` |
        "
       (if (and win (gget "document") (not proc))
         false  ; fast path: real browser
         (or (some? js-platform)
             js-runtime-has-node-apis?)))))
