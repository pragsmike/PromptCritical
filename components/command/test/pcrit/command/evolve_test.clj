(ns pcrit.command.evolve-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.evolve :as evolve]
            ;; Import the new command-specific test helper
            [pcrit.command.test-helper :as th-cmd]
            ;; Import the generic test helper for fixtures and temp-dir
            [pcrit.test-helper.interface :as th-generic]
            [pcrit.expdir.interface :as expdir]
            ;; Import the implementation namespaces to mock them directly
            [pcrit.command.vary]
            [pcrit.command.evaluate]
            [pcrit.command.select]))

(use-fixtures :each th-generic/with-temp-dir th-generic/with-quiet-logging)

(deftest evolve-happy-path-test
  (testing "Runs for the specified number of generations"
    (let [ctx (th-cmd/setup-configured-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          vary-calls (atom 0)
          evaluate-calls (atom 0)
          select-calls (atom 0)
          world-state (atom {:current-gen 0})]
      (with-redefs [pcrit.command.vary/vary! (fn [_] (swap! vary-calls inc) {:cost 0.1})
                    pcrit.command.evaluate/evaluate! (fn [_ _] (swap! evaluate-calls inc) {:success true :cost 0.2})
                    pcrit.command.select/select! (fn [_ _] (swap! select-calls inc) (swap! world-state update :current-gen inc))
                    expdir/find-latest-generation-number (fn [_] (:current-gen @world-state))]
        (evolve/evolve! ctx {:generations 3 :inputs (.getCanonicalPath inputs-dir)})
        (is (= 3 @vary-calls))
        (is (= 3 @evaluate-calls))
        (is (= 3 @select-calls))))))

(deftest evolve-max-cost-vary-test
  (testing "Halts if max-cost is exceeded during vary step"
    (let [ctx (th-cmd/setup-configured-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          vary-calls (atom 0)
          evaluate-calls (atom 0)]
      (with-redefs [pcrit.command.vary/vary! (fn [_] (swap! vary-calls inc) {:cost 1.5})
                    pcrit.command.evaluate/evaluate! (fn [_ _] (swap! evaluate-calls inc) {:success true :cost 0.2})]
        (evolve/evolve! ctx {:generations 5 :max-cost 1.0 :inputs (.getCanonicalPath inputs-dir)})
        (is (= 1 @vary-calls) "Should call vary once")
        (is (= 0 @evaluate-calls) "Should not call evaluate")))))

(deftest evolve-max-cost-evaluate-test
  (testing "Halts if max-cost is exceeded during evaluate step"
    (let [ctx (th-cmd/setup-configured-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          vary-calls (atom 0)
          evaluate-calls (atom 0)
          select-calls (atom 0)]
      (with-redefs [pcrit.command.vary/vary! (fn [_] (swap! vary-calls inc) {:cost 0.1})
                    pcrit.command.evaluate/evaluate! (fn [_ _] (swap! evaluate-calls inc) {:success true :cost 1.0})
                    pcrit.command.select/select! (fn [_ _] (swap! select-calls inc))]
        (evolve/evolve! ctx {:generations 5 :max-cost 1.0 :inputs (.getCanonicalPath inputs-dir)})
        (is (= 1 @vary-calls))
        (is (= 1 @evaluate-calls))
        (is (= 0 @select-calls) "Should not call select")))))

(deftest evolve-selection-failure-test
  (testing "Halts if selection fails to produce a new generation"
    (let [ctx (th-cmd/setup-configured-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          vary-calls (atom 0)
          evaluate-calls (atom 0)
          select-calls (atom 0)]
      (with-redefs [pcrit.command.vary/vary! (fn [_] (swap! vary-calls inc) {:cost 0.1})
                    pcrit.command.evaluate/evaluate! (fn [_ _] (swap! evaluate-calls inc) {:success true :cost 0.2})
                    pcrit.command.select/select! (fn [_ _] (swap! select-calls inc))
                    expdir/find-latest-generation-number (fn [_] 0)]
        (evolve/evolve! ctx {:generations 5 :inputs (.getCanonicalPath inputs-dir)})
        (is (= 1 @vary-calls))
        (is (= 1 @evaluate-calls))
        (is (= 1 @select-calls) "Should call select once then halt")))))
