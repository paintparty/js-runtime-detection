(ns js-runtime-detection.advanced-runner
  "Test entrypoint for compiled :advanced builds (`bb test:advanced` and
   `bb test:compat`), run on Node.

   ClojureScript before 1.10 does not set up printing for Node on its own, so
   this calls enable-console-print!, which nbb does not have. Setting exitCode
   by string key keeps the name intact under :advanced compilation."
  (:require [cljs.test :as t]
            [js-runtime-detection.core-test]))

(enable-console-print!)

(defmethod t/report [:cljs.test/default :end-run-tests] [m]
  (unchecked-set js/process "exitCode" (if (t/successful? m) 0 1)))

(t/run-tests 'js-runtime-detection.core-test)
