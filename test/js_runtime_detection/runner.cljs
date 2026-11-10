(ns js-runtime-detection.runner
  "Test entrypoint for `bb test` (nbb). Compiled builds use
   js-runtime-detection.advanced-runner instead.

   Under nbb, `run-tests` returns nil and the summary is delivered through the
   `:end-run-tests` report method, so the exit code is set from there."
  (:require [cljs.test :as t]
            [js-runtime-detection.core-test]))

(defmethod t/report [:cljs.test/default :end-run-tests] [m]
  (unchecked-set js/process "exitCode" (if (t/successful? m) 0 1)))

(t/run-tests 'js-runtime-detection.core-test)
