(ns pcrit.command.vary-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.core :as cmd]
            [pcrit.command.vary :as vary]
            [pcrit.expdir.interface :as expdir]
            [pcrit.experiment.interface :as exp]
            [pcrit.llm.interface :as llm]
            [pcrit.pop.interface :as pop]
            [pcrit.test-helper.interface :refer [get-temp-dir make-temp-exp-dir! with-temp-dir]]))

(use-fixtures :each with-temp-dir)

(deftest vary-command-test
  (let [exp-dir (get-temp-dir)
        ctx (exp/new-experiment-context exp-dir)
        ;; Mock LLM response now includes generation metadata
        mock-llm-response {:content "New offspring content"
                           :generation-metadata {:cost 0.001, :usage {:total_tokens 100}, :duration-ms 500}}
        mock-sender-fn (fn [_model-name _prompt-body] mock-llm-response)
        mock-template-caller (fn [model-name template vars] (llm/call-model-template model-name template vars mock-sender-fn))]

    ;; Setup a complete experiment with a gen-0 population
    (make-temp-exp-dir! exp-dir)
    ;; The default scaffold from `init` uses 'improve' as its meta-prompt link name.
    ;; The older `make-temp-exp-dir!` helper uses 'refine'. We rename it here
    ;; to match what the vary! command expects.
    (cmd/bootstrap! (assoc ctx ::bootstrap-spec (io/file exp-dir "bootstrap.edn")))
    (let [link-dir (expdir/get-link-dir ctx)]
      (.renameTo (io/file link-dir "refine") (io/file link-dir "improve")))


    (testing "Pre-condition: gen-0 exists and has 1 member"
      (is (= 0 (expdir/find-latest-generation-number ctx)))
      (is (= 1 (count (pop/load-population ctx 0)))))

    (testing "vary! adds offspring with cost and timing metadata to its header"
      ;; Run vary! command
      (vary/vary! ctx {:call-template-fn mock-template-caller})

      (let [varied-pop (pop/load-population ctx 0)
            pop-headers (map :header varied-pop)
            offspring-header (first (filter :parents pop-headers))]

        (is (some? offspring-header) "An offspring with parent metadata should exist.")
        (is (= ["P1"] (:parents offspring-header)) "Offspring should list P1 as its parent.")

        ;; Verify new generation metadata is present
        (is (= 0.001 (:cost offspring-header)))
        (is (= {:total_tokens 100} (:usage offspring-header)))
        (is (= 500 (:duration-ms offspring-header)))

        ;; Verify existing generator metadata is still present
        (is (= "mistral" (get-in offspring-header [:generator :model])) "Generator should record the default 'mistral' model.")
        (is (= "P2" (get-in offspring-header [:generator :meta-prompt])) "Generator should record the 'improve' meta-prompt (P2).")))))
