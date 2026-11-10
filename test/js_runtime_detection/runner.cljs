(ns js-runtime-detection.runner
  "Test entrypoint for nbb, Squint and Cherry. ClojureScript builds use
   js-runtime-detection.advanced-runner, and Clojurific builds use its own
   generated runner.

   Under nbb, `run-tests` returns nil and the summary is delivered through the
   `:end-run-tests` report method. Squint and Cherry never call that method,
   and their `run-tests` returns the summary instead. The exit code is set from
   whichever of the two arrives."
  (:require [cljs.test :as t]
            [js-runtime-detection.core-test]))

(defn- set-exit-code! [summary]
  (unchecked-set js/process "exitCode" (if (t/successful? summary) 0 1)))

(defmethod t/report [:cljs.test/default :end-run-tests] [m]
  (set-exit-code! m))

(let [summary (t/run-tests 'js-runtime-detection.core-test)]
  (when (map? summary)
    (set-exit-code! summary)))
