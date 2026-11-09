(ns js-runtime-detection.core-test
  (:require [cljs.test :refer [deftest is testing]]
            [js-runtime-detection.core :as rt]))

(deftest node-row-of-detection-matrix
  (testing "values match the Node row of the detection matrix"
    (is (= :node rt/js-runtime))
    (is (nil? rt/js-platform))
    (is (true? rt/js-server-runtime?))
    (is (false? rt/js-runtime-has-dom?))
    (is (false? rt/js-runtime-has-jsdom?))
    (is (true? rt/js-runtime-has-node-apis?))
    (is (true? rt/js-runtime-is-strict-node?))))

(deftest predicates-return-booleans
  (doseq [v [rt/js-server-runtime?
             rt/js-runtime-has-dom?
             rt/js-runtime-has-jsdom?
             rt/js-runtime-has-node-apis?
             rt/js-runtime-is-strict-node?]]
    (is (boolean? v))))
