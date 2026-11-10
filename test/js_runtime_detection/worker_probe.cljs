(ns js-runtime-detection.worker-probe
  "Loaded inside a Cloudflare worker by test/cloudflare/run.mjs. Stores the
   detected values on globalThis as JSON, for the worker's fetch handler to
   return."
  (:require [js-runtime-detection.core :as rt]))

(unchecked-set js/globalThis "__jsRuntimeDetect"
               (js/JSON.stringify
                #js {"js-runtime"                 (some-> rt/js-runtime name)
                     "js-platform"                (some-> rt/js-platform name)
                     "js-server-runtime?"         rt/js-server-runtime?
                     "js-runtime-has-dom?"        rt/js-runtime-has-dom?
                     "js-runtime-has-jsdom?"      rt/js-runtime-has-jsdom?
                     "js-runtime-has-node-apis?"  rt/js-runtime-has-node-apis?
                     "js-runtime-is-strict-node?" rt/js-runtime-is-strict-node?}))
