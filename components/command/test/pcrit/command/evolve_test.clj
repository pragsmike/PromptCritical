(ns pcrit.command.evolve-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.evolve :as evolve]
            [pcrit.command.interface :as command]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            ;; Import the implementation namespaces to mock them directly
            [pcrit.command.vary]
            [pcrit.command.evaluate]
            [pcrit.command.select]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir with-quiet-logging]]))

(use-fixtures :each with-temp-dir with-quiet-logging)

(defn- setup-evolve-test!
  "Creates a bootstrapped experiment and the necessary config file for evolve."
  [exp-dir]
  (let [ctx (exp/new-experiment-context exp-dir)]
    (let [seeds-dir (io/file exp-dir "seeds")]
      (.mkdirs seeds-dir)
      (spit (io/file seeds-dir "seed.txt") "seed {{INPUT_TEXT}}")
      (spit (io/file seeds-dir "refine.txt") "refine {{OBJECT_PROMPT}}"))
    (spit (io/file exp-dir "bootstrap.edn") (pr-str {:seed "seeds/seed.txt" :refine "seeds/refine.txt"}))
    (command/bootstrap! ctx)
    (spit (io/file exp-dir "evolution-parameters.edn")
          (pr-str {:evaluate {:models ["mock-model"]}}))
    ctx))

(deftest evolve-happy-path-test
  (testing "Runs for the specified number of generations"
    (let [ctx (setup-evolve-test! (get-temp-dir))
          inputs-dir (doto (io/file (get-temp-dir) "inputs") .mkdirs)
          vary-calls (atom 0)
          evaluate-calls (atom 0)
          select-calls (atom 0)
          world-state (atom {:current-gen 0})]
      (with-redefs [;; CORRECTED: Mock the implementation vars directly
                    pcrit.command.vary/vary! (fn [_] (swap! vary-calls inc) {:cost 0.1})
                    pcrit.command.evaluate/evaluate! (fn [_ _] (swap! evaluate-calls inc) {:success true :cost 0.2})
                    pcrit.command.select/select! (fn [_ _] (swap! select-calls inc) (swap! world-state update :current-gen inc))
                    ;; Also mock find-latest-generation-number to control the loop
                    expdir/find-latest-generation-number (fn [_] (:current-gen @world-state))]
        (evolve/evolve! ctx {:generations 3 :inputs (.getCanonicalPath inputs-dir)})
        (is (= 3 @vary-calls))
        (is (= 3 @evaluate-calls))
        (is (= 3 @select-calls))))))

(deftest evolve-max-cost-vary-test
  (testing "Halts if max-cost is exceeded during vary step"
    (let [ctx (setup-evolve-test! (get-temp-dir))
          inputs-dir (doto (io/file (get-temp-dir) "inputs") .mkdirs)
          vary-calls (atom 0)
          evaluate-calls (atom 0)]
      (with-redefs [pcrit.command.vary/vary! (fn [_] (swap! vary-calls inc) {:cost 1.5})
                    pcrit.command.evaluate/evaluate! (fn [_ _] (swap! evaluate-calls inc) {:success true :cost 0.2})]
        (evolve/evolve! ctx {:generations 5 :max-cost 1.0 :inputs (.getCanonicalPath inputs-dir)})
        (is (= 1 @vary-calls) "Should call vary once")
        (is (= 0 @evaluate-calls) "Should not call evaluate")))))

(deftest evolve-max-cost-evaluate-test
  (testing "Halts if max-cost is exceeded during evaluate step"
    (let [ctx (setup-evolve-test! (get-temp-dir))
          inputs-dir (doto (io/file (get-temp-dir) "inputs") .mkdirs)
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
    (let [ctx (setup-evolve-test! (get-temp-dir))
          inputs-dir (doto (io/file (get-temp-dir) "inputs") .mkdirs)
          vary-calls (atom 0)
          evaluate-calls (atom 0)
          select-calls (atom 0)]
      (with-redefs [pcrit.command.vary/vary! (fn [_] (swap! vary-calls inc) {:cost 0.1})
                    pcrit.command.evaluate/evaluate! (fn [_ _] (swap! evaluate-calls inc) {:success true :cost 0.2})
                    pcrit.command.select/select! (fn [_ _] (swap! select-calls inc)) ; Note: this mock doesn't advance the generation
                    expdir/find-latest-generation-number (fn [_] 0)] ; Mock to always return 0
        (evolve/evolve! ctx {:generations 5 :inputs (.getCanonicalPath inputs-dir)})
        (is (= 1 @vary-calls))
        (is (= 1 @evaluate-calls))
        (is (= 1 @select-calls) "Should call select once then halt")))))
