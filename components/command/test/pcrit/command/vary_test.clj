(ns pcrit.command.vary-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.core :as cmd]
            [pcrit.command.vary :as vary]
            [pcrit.expdir.interface :as expdir]
            [pcrit.experiment.interface :as exp]
            [pcrit.llm.interface :as llm]
            [pcrit.pop.interface :as pop]
            [pcrit.test-helper.interface :refer [get-temp-dir make-temp-exp-dir! with-temp-dir with-quiet-logging]]))

(use-fixtures :each with-temp-dir with-quiet-logging)

(defn- create-evo-params-file! [exp-dir model-name]
  (spit (io/file exp-dir "evolution-parameters.edn")
        (pr-str {:vary {:model model-name}})))

(deftest vary-command-test
  (let [exp-dir (get-temp-dir)
        ctx (exp/new-experiment-context exp-dir)
        mock-sender-fn (fn [_model-name _prompt-body] {:content "New offspring content"})
        mock-template-caller (fn [model-name template vars] (llm/call-model-template model-name template vars mock-sender-fn))]

    ;; Setup a complete experiment with a gen-0 population
    (make-temp-exp-dir! exp-dir)
    (cmd/bootstrap! ctx)

    (testing "Pre-condition: gen-0 exists and has 1 member"
      (is (= 0 (expdir/find-latest-generation-number ctx)))
      (is (= 1 (count (pop/load-population ctx 0)))))

    (testing "vary! adds offspring to the current generation and does NOT create a new one"
      ;; Run vary! command
      (vary/vary! ctx {:call-template-fn mock-template-caller})

      (testing "A new generation was NOT created"
        (is (= 0 (expdir/find-latest-generation-number ctx)) "Generation number should still be 0."))

      (testing "The current generation's population was expanded"
        (let [varied-pop (pop/load-population ctx 0)]
          (is (= 2 (count varied-pop)) "Population of gen-0 should now have 2 members.")))

      (testing "Offspring has correct ancestry metadata and default model"
        (let [pop-headers (map :header (pop/load-population ctx 0))
              offspring-header (first (filter :parents pop-headers))]
          (is (some? offspring-header) "An offspring with parent metadata should exist.")
          (is (= ["P1"] (:parents offspring-header)) "Offspring should list P1 as its parent.")
          (is (= "mistral" (get-in offspring-header [:generator :model])) "Generator should record the default 'mistral' model.")
          (is (= "P2" (get-in offspring-header [:generator :meta-prompt])) "Generator should record the 'refine' meta-prompt (P2).")
          (is (some? (get-in offspring-header [:generator :vary-run])) "Generator should have a vary-run timestamp.")))

      (testing "vary! uses model specified in evolution-parameters.edn"
        (create-evo-params-file! exp-dir "configured-model")

      ;; Run vary! again on the same generation
        (vary/vary! ctx {:call-template-fn mock-template-caller})

        (let [final-pop (pop/load-population ctx 0)]
          (is (= 4 (count final-pop)) "Population of gen-0 should now have 4 members (1 original + 3 offspring).")

          (let [pop-headers (map :header final-pop)
                configured-offspring (->> pop-headers
                                          (filter #(-> % :generator :model (= "configured-model"))))]
            (is (= 2 (count configured-offspring)) "Two new offspring should have been created with the configured model.")
            (let [one-offspring (first configured-offspring)]
              (is (some? (:parents one-offspring)) "New offspring should have parents."))))))))
