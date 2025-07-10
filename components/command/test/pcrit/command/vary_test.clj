(ns pcrit.command.vary-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.core :as cmd]
            [pcrit.command.vary :as vary]
            [pcrit.experiment.interface :as exp]
            [pcrit.llm.interface :as llm]
            [pcrit.pop.interface :as pop]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir make-temp-exp-dir!]]))

(use-fixtures :each with-temp-dir)

;; Helper to create the config file for tests
(defn- create-evo-params-file! [exp-dir model-name]
  (spit (io/file exp-dir "evolution-parameters.edn")
        (pr-str {:vary {:model model-name}})))

(deftest vary-uses-default-model-test
  (testing "vary! uses default 'mistral' model when config is missing"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          _ (make-temp-exp-dir! exp-dir)
          _ (cmd/bootstrap! ctx) ; bootstrap! now creates gen-0
          captured-model (atom nil)]

      (let [mock-sender-fn (fn [model-name _] (reset! captured-model model-name) {:content "New content"})
            mock-template-caller (fn [model-name template vars] (llm/call-model-template model-name template vars mock-sender-fn))]
        (vary/vary! ctx {:call-template-fn mock-template-caller}))

      (is (= "mistral" @captured-model) "Should use 'mistral' as the default model.")
      ;; The new population (survivors + offspring) is in gen-1
      (let [new-pop (pop/load-population ctx 1)
            offspring (first (filter #(get % :parents) (map :header new-pop)))]
        (is (some? offspring) "An offspring should exist in the new population.")
        (is (= "mistral" (get-in offspring [:generator :model])) "Generator metadata should record the default model.")))))

(deftest vary-uses-configured-model-test
  (testing "vary! uses model specified in evolution-parameters.edn"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          _ (make-temp-exp-dir! exp-dir)
          _ (cmd/bootstrap! ctx) ; bootstrap! now creates gen-0
          captured-model (atom nil)]

      (create-evo-params-file! exp-dir "configured-model")

      (let [mock-sender-fn (fn [model-name _] (reset! captured-model model-name) {:content "New content"})
            mock-template-caller (fn [model-name template vars] (llm/call-model-template model-name template vars mock-sender-fn))]
        (vary/vary! ctx {:call-template-fn mock-template-caller}))

      (is (= "configured-model" @captured-model) "Should use the model from the config file.")
      ;; The new population (survivors + offspring) is in gen-1
      (let [new-pop (pop/load-population ctx 1)
            offspring-header (->> new-pop (map :header) (filter :parents) first)]
        (is (some? offspring-header) "An offspring should exist in the new population.")
        (is (= ["P1"] (:parents offspring-header)) "Offspring should list P1 as its parent.")
        (is (= "configured-model" (get-in offspring-header [:generator :model])) "Generator metadata should record the configured model.")
        (is (= "P2" (get-in offspring-header [:generator :meta-prompt])) "Generator should record the correct meta-prompt.")))))
