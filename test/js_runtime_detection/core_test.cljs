(ns js-runtime-detection.core-test
  (:require [cljs.test :refer [deftest is]]
            [js-runtime-detection.core :as rt]))

(def ^:private expected
  "Rows of the detection matrix, keyed by the JS_RUNTIME_DETECT_ENV
   environment variable the test process runs under."
  {"node"       {:js-runtime                 "node"
                 :js-platform                nil
                 :js-runtime-is-server         true
                 :js-runtime-has-dom        false
                 :js-runtime-has-jsdom      false
                 :js-runtime-has-node-apis  true
                 :js-runtime-is-strict-node true}
   "jsdom-node" {:js-runtime                 "node"
                 :js-platform                nil
                 :js-runtime-is-server         true
                 :js-runtime-has-dom        true
                 :js-runtime-has-jsdom      true
                 :js-runtime-has-node-apis  true
                 :js-runtime-is-strict-node true}})

(def ^:private env-name
  (or (some-> js/process
              (unchecked-get "env")
              (unchecked-get "JS_RUNTIME_DETECT_ENV"))
      "node"))

(def ^:private actual
  {:js-runtime                 rt/js-runtime
   :js-platform                rt/js-platform
   :js-runtime-is-server         rt/js-runtime-is-server
   :js-runtime-has-dom        rt/js-runtime-has-dom
   :js-runtime-has-jsdom      rt/js-runtime-has-jsdom
   :js-runtime-has-node-apis  rt/js-runtime-has-node-apis
   :js-runtime-is-strict-node rt/js-runtime-is-strict-node})

(deftest matches-detection-matrix-row
  (is (contains? expected env-name) (str "Unknown JS_RUNTIME_DETECT_ENV: " env-name))
  (is (= (get expected env-name) actual)))

(deftest predicates-return-booleans
  (doseq [[k v] (dissoc actual :js-runtime :js-platform)]
    (is (boolean? v) (str k " is not a boolean"))))

(deftest runtime-and-platform-are-strings
  (is (string? rt/js-runtime))
  (is (or (nil? rt/js-platform) (string? rt/js-platform))))
