(ns pcrit.command.vary-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.core :as cmd]
            [pcrit.command.vary :as vary]
            [pcrit.expdir.interface :as expdir]
            [pcrit.experiment.interface :as exp]
            [pcrit.llm.interface :as llm]
            [pcrit.pop.interface :as pop]
            [pcrit.test-helper.interface :refer [get-temp-dir with-temp-dir with-quiet-logging]]))

(use-fixtures :each with-temp-dir with-quiet-logging)

;; --- CORRECTED TEST SETUP HELPER ---
(defn- make-bootstrap-prereqs!
  "Creates the necessary files for a `bootstrap` command to run successfully.
  Crucially, it does NOT create the `generations/` directory."
  [target-dir]
  (let [seeds-dir (io/file target-dir "seeds")]
    (.mkdirs seeds-dir)
    (spit (io/file seeds-dir "seed-object-prompt.txt") "The seed! {{INPUT_TEXT}}")
    (spit (io/file seeds-dir "improve-meta-prompt.txt") "Refine this: {{OBJECT_PROMPT}}")
    (spit (io/file target-dir "bootstrap.edn")
          (pr-str {:seed "seeds/seed-object-prompt.txt"
                   :refine "seeds/improve-meta-prompt.txt"}))))


(deftest vary-command-test
  (let [exp-dir (get-temp-dir)
        ctx (exp/new-experiment-context exp-dir)
        mock-llm-response {:content "New offspring content"
                           :generation-metadata {:provider          :mock-provider
                                                 :model             "mock-provider/mock-model"
                                                 :token-in          120
                                                 :token-out         30
                                                 :cost-usd-snapshot 0.0025
                                                 :duration-ms       750}}
        mock-sender-fn (fn [_model-name _prompt-body] mock-llm-response)
        mock-template-caller (fn [model-name template vars] (llm/call-model-template model-name template vars mock-sender-fn))]

    ;; CORRECTED: Use the correct, minimal setup function.
    (make-bootstrap-prereqs! exp-dir)
    ;; Now this bootstrap call will succeed.
    (cmd/bootstrap! ctx)


    (testing "Pre-condition: gen-0 exists and has 1 member"
      (is (= 0 (expdir/find-latest-generation-number ctx)))
      (is (= 1 (count (pop/load-population ctx 0)))))

    (testing "vary! adds offspring with full generation metadata to its header"
      (vary/vary! ctx {:call-template-fn mock-template-caller})

      (let [varied-pop (pop/load-population ctx 0)
            pop-headers (map :header varied-pop)
            offspring-header (first (filter :parents pop-headers))]

        (is (some? offspring-header) "An offspring with parent metadata should exist.")
        (is (= ["P1"] (:parents offspring-header)) "Offspring should list P1 as its parent.")

        (is (= "mock-provider" (:provider offspring-header)))
        (is (= "mock-provider/mock-model" (:model offspring-header)))
        (is (= 120 (:token-in offspring-header)))
        (is (= 30 (:token-out offspring-header)))
        (is (= 0.0025 (:cost-usd-snapshot offspring-header)))
        (is (= 750 (:duration-ms offspring-header)))

        (is (some? (get-in offspring-header [:generator :model])) "Generator block should still exist.")))))
