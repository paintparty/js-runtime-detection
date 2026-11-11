(ns tasks.test-runner
  "Helpers for the test:* tasks in bb.edn. Each one compiles the tests for a
   dialect, runs them on one or more runtimes, and prints each run under a
   label line."
  (:require [babashka.fs :as fs]
            [babashka.tasks :refer [clojure run shell]]
            [clojure.string :as str]))

(defn label-line
  "A profile's label, e.g. `=== Squint 0.15.212 | Node with jsdom globals`.
   SGR styling: `marker` and `|` dim, the dialect bold, the row plain.
   Plain text when NO_COLOR is set."
  [marker dialect row]
  (let [sgr (fn [code s]
              (if (System/getenv "NO_COLOR") s (str "\u001b[" code "m" s "\u001b[0m")))]
    (str (sgr 2 marker) " " (sgr 1 dialect) " " (sgr 2 "|") " " row)))

(defn run-labeled
  "Runs a test command under a label line, e.g.
   `=== Squint 0.15.212 | Node with jsdom globals`, and prints its
   output (stdout, then stderr) indented, without blank lines. Fails the
   task with the command's exit code when it fails, like `shell`."
  [dialect row opts & cmd]
  (println (label-line "===" dialect row))
  (let [{:keys [out err exit]}
        (apply shell (merge {:out :string :err :string :continue true} opts) cmd)]
    (doseq [line  (str/split-lines (str out err))
            :when (not (str/blank? line))]
      (println (str "    " line)))
    (println)
    (when-not (zero? exit)
      (throw (ex-info (str dialect " | " row " failed") {:babashka/exit exit})))))

;; Environment for the runs without globalThis. With NODE_USE_ENV_PROXY
;; set, Node sets up its HTTP proxy after `-r` preloads run, and that
;; setup reads globalThis, so Node itself crashes before the tests start.
(def no-globalthis-env
  (dissoc (into {} (System/getenv)) "NODE_USE_ENV_PROXY"))

(defn run-node-rows
  "Runs a compiled test entrypoint on Node: plain, with jsdom globals
   stubbed in and, unless `no-globalthis?` is false, with globalThis
   deleted. Cherry and Clojurific skip the last run, because their own
   runtimes need globalThis. `dialect` labels each run."
  ([dialect test-js] (run-node-rows dialect test-js true))
  ([dialect test-js no-globalthis?]
   (run-labeled dialect "Node" {}
                "node" test-js)
   (run-labeled dialect "Node with jsdom globals" {:extra-env {"JS_RUNTIME_DETECT_ENV" "jsdom-node"}}
                "node" "-r" "./test/stubs/jsdom.cjs" test-js)
   (when no-globalthis?
     (run-labeled dialect "Node without globalThis" {:env no-globalthis-env}
                  "node" "-r" "./test/stubs/no-globalthis.cjs" test-js))))

(defn run-nbb
  "Runs the test suite on Node under nbb, plain and with jsdom globals."
  []
  (let [args ["npx" "--yes" "nbb@1.6.214" "-cp" "src:test" "test/js_runtime_detection/runner.cljs"]]
    (apply run-labeled "nbb 1.6.214" "Node" {} args)
    (apply run-labeled "nbb 1.6.214" "Node with jsdom globals"
           {:extra-env {"JS_RUNTIME_DETECT_ENV" "jsdom-node"
                        "NODE_OPTIONS"          "-r ./test/stubs/jsdom.cjs"}}
           args)))

(defn run-advanced
  "Compiles the tests with :advanced using the ClojureScript from `alias`,
   then runs them on Node."
  [dialect alias out-dir]
  (clojure (str "-M:" alias) "script/build_advanced.clj" out-dir)
  (run-node-rows dialect (str out-dir "/main.js")))

(defn run-esm-dialect
  "Compiles src and test with the Squint or Cherry CLI from npm `pkg`
   (name@version), installs the same package next to the output for the
   runtime the compiled modules import, then runs the tests on Node."
  [dialect pkg out-dir no-globalthis?]
  (shell "npx" "--yes" pkg "compile" "--paths" "src" "--paths" "test" "--output-dir" out-dir)
  (shell "npm" "install" "--no-save" "--no-package-lock" "--silent" "--prefix" out-dir pkg)
  (run-node-rows dialect (str out-dir "/js_runtime_detection/runner.mjs") no-globalthis?))

(defn run-clojurific
  "Compiles the tests with Clojurific and runs them on Node, plain and with
   jsdom globals."
  []
  (clojure "-M:clojurific" "-m" "cljf.esm" "build"
           (str "{:test-runner {:ns js-runtime-detection.clojurific-runner"
                " :ns-regexp \"-test$\" :runner cljf.esm.node-test/run}"
                " :output-dir \"target/clojurific\"}"))
  (run-node-rows "Clojurific 0.12.15" "target/clojurific/js_runtime_detection/clojurific_runner.js" false))

(defn miniflare-installed? []
  (fs/exists? "test/cloudflare/node_modules/miniflare"))

(defn run-cloudflare
  "Compiles a probe with :advanced and runs it in workerd through Miniflare.
   Prints a skip message instead when Miniflare is not installed."
  []
  (if (miniflare-installed?)
    (do (clojure "-M:cljs-latest" "script/build_advanced.clj" "target/cloudflare-probe"
                 "js-runtime-detection.worker-probe" "browser")
        (run-labeled "ClojureScript 1.12.145, :advanced" "Cloudflare Workers (workerd via Miniflare)" {}
                     "node" "test/cloudflare/run.mjs" "target/cloudflare-probe/main.js"))
    (println (str "SKIPPED test:cloudflare: Miniflare is not installed. To enable it, run:\n"
                  "  npm install --no-save --prefix test/cloudflare miniflare@4.20260730.0"))))

(defn run-all
  "Prints every profile that will run, then runs each test task in order."
  []
  (let [cloudflare? (miniflare-installed?)
        ;; [task labels], in run order. Keep the labels in step with the
        ;; ones each task prints.
        plan [['test:nbb        ["nbb 1.6.214 | Node"
                                 "nbb 1.6.214 | Node with jsdom globals"]]
              ['test:advanced   ["ClojureScript 1.12.145, :advanced | Node"
                                 "ClojureScript 1.12.145, :advanced | Node with jsdom globals"
                                 "ClojureScript 1.12.145, :advanced | Node without globalThis"]]
              ['test:cloudflare [(str "ClojureScript 1.12.145, :advanced | Cloudflare Workers (workerd via Miniflare)"
                                      (when-not cloudflare? " (skipped, Miniflare is not installed)"))]]
              ['test:squint     ["Squint 0.15.212 | Node"
                                 "Squint 0.15.212 | Node with jsdom globals"
                                 "Squint 0.15.212 | Node without globalThis"]]
              ['test:cherry     ["Cherry 0.6.38 | Node"
                                 "Cherry 0.6.38 | Node with jsdom globals"]]
              ['test:clojurific ["Clojurific 0.12.15 | Node"
                                 "Clojurific 0.12.15 | Node with jsdom globals"]]]]
    (println "Preparing to test js-runtime-detection on the following profiles:")
    (doseq [[_ labels] plan
            label      labels]
      (println (apply label-line "---" (str/split label #" [|] " 2))))
    (println)
    (doseq [[task _] plan]
      (run task))))
