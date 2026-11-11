(ns js-runtime-detection.advanced-runner
  "Test entrypoint for compiled :advanced builds (`bb test:advanced`), run on
   Node. Setting exitCode by string key keeps the name intact under :advanced
   compilation."
  (:require [cljs.test :as t]
            [js-runtime-detection.core-test]))

(defmethod t/report [:cljs.test/default :end-run-tests] [m]
  (unchecked-set js/process "exitCode" (if (t/successful? m) 0 1)))

(t/run-tests 'js-runtime-detection.core-test)
