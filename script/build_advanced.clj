;; Compiles ClojureScript with :advanced optimizations.
;;
;; Usage: clojure -M:<cljs alias> script/build_advanced.clj <output-dir> [main-ns] [target]
;;
;; main-ns defaults to js-runtime-detection.advanced-runner. target defaults to
;; nodejs. Pass "browser" for a build with no :target, as used for the
;; Cloudflare worker probe.

(require '[cljs.build.api :as api])

(let [[out-dir main target] *command-line-args*
      main   (symbol (or main "js-runtime-detection.advanced-runner"))
      target (or target "nodejs")]
  (api/build (api/inputs "src" "test")
             (cond-> {:main           main
                      :optimizations  :advanced
                      :checked-arrays :warn
                      :output-dir     out-dir
                      :output-to      (str out-dir "/main.js")}
               (not= target "browser") (assoc :target (keyword target)))))
