;; Compiles the test suite with :advanced optimizations for Node.
;; Uses cljs.build.api rather than cljs.main so it also runs on
;; ClojureScript 1.9.946, which predates cljs.main.
;;
;; Usage: clojure -M:<cljs alias> script/build_advanced.clj <output-dir>

(require '[cljs.build.api :as api])

(let [[out-dir] *command-line-args*]
  (api/build (api/inputs "src" "test")
             {:main           'js-runtime-detection.advanced-runner
              :target         :nodejs
              :optimizations  :advanced
              :checked-arrays :warn
              :output-dir     out-dir
              :output-to      (str out-dir "/test.js")}))
