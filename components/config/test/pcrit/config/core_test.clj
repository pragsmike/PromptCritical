(ns pcrit.config.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.config.core :as config]
            [pcrit.experiment.interface :as exp]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]))

(use-fixtures :each with-temp-dir)

(deftest load-evolution-params-test
  (testing "Returns empty map if config file does not exist"
    (let [ctx (exp/new-experiment-context (get-temp-dir))]
      (is (= {} (config/load-evolution-params ctx)))))

  (testing "Returns empty map and logs error if config file is corrupt"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)]
      (spit (io/file exp-dir "evolution-parameters.edn") "{:vary {:model") ; Corrupt EDN
      (is (= {} (config/load-evolution-params ctx)))))

  (testing "Successfully loads a valid config file"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          config-data {:vary {:model "test-model-123"} :select {:strategy :top-k}}]
      (spit (io/file exp-dir "evolution-parameters.edn") (pr-str config-data))
      (let [loaded-params (config/load-evolution-params ctx)]
        (is (= config-data loaded-params))))))
